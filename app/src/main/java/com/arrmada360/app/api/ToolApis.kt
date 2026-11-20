package com.arrmada360.app.api

import com.arrmada360.app.data.ConfigStore
import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.net.ApiException
import com.arrmada360.app.net.Http
import com.arrmada360.app.net.arr
import com.arrmada360.app.net.bool
import com.arrmada360.app.net.dbl
import com.arrmada360.app.net.int
import com.arrmada360.app.net.jsonBody
import com.arrmada360.app.net.long
import com.arrmada360.app.net.obj
import com.arrmada360.app.net.objects
import com.arrmada360.app.net.str
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

// ======================= Uptime Kuma =======================
data class KumaMonitor(
    val name: String,
    val type: String,
    val target: String,
    /** 1 up, 0 down, 2 pending, 3 maintenance, -1 unknown */
    val status: Int,
    val responseMs: Long?,
    val certDays: Int?,
    val uptime24: Double?,
    val beats: List<Int>,
    val lastMsg: String,
)

class KumaApi(val cfg: ServiceConfig) {
    private val client = Http.client(cfg.trustAllCerts)
    var slug: String
        get() = ConfigStore.getString("kumaSlug_" + cfg.id)
        set(v) = ConfigStore.putString("kumaSlug_" + cfg.id, v.trim())

    private suspend fun getText(path: String, auth: Boolean): String {
        val b = Request.Builder().url("${cfg.baseUrl}$path")
        if (auth && cfg.apiKey.isNotBlank()) b.header("Authorization", Credentials.basic("", cfg.apiKey))
        return Http.exec(client, b.build())
    }

    suspend fun status(): String {
        if (cfg.apiKey.isNotBlank()) {
            val n = parseMetrics(getText("/metrics", true)).size
            return "Uptime Kuma · $n monitors"
        }
        if (slug.isNotEmpty()) { JSONObject(getText("/api/status-page/$slug", false)); return "Uptime Kuma · status page $slug" }
        getText("/api/entry-page", false)
        return "Uptime Kuma reachable – add an API key to see monitors"
    }

    private val lineRx = Regex("""^(\w+)\{(.*)\}\s+(\S+)""")
    private val labelRx = Regex("""(\w+)="((?:[^"\\]|\\.)*)"""")

    private data class Metric(val name: String, val labels: Map<String, String>, val value: Double)

    private fun parseMetrics(text: String): Map<String, MutableMap<String, Any>> {
        val out = linkedMapOf<String, MutableMap<String, Any>>()
        text.lineSequence().filter { it.startsWith("monitor_") }.forEach { line ->
            val m = lineRx.find(line) ?: return@forEach
            val labels = labelRx.findAll(m.groupValues[2]).associate { it.groupValues[1] to it.groupValues[2].replace("\\\"", "\"") }
            val name = labels["monitor_name"] ?: return@forEach
            val row = out.getOrPut(name) { mutableMapOf("labels" to labels) }
            row[m.groupValues[1]] = m.groupValues[3].toDoubleOrNull() ?: return@forEach
        }
        return out
    }

    suspend fun monitors(): List<KumaMonitor> = coroutineScope {
        val metricsJob = if (cfg.apiKey.isNotBlank()) async { parseMetrics(getText("/metrics", true)) } else null
        val pageJob = if (slug.isNotEmpty()) async { try { JSONObject(getText("/api/status-page/$slug", false)) } catch (e: Exception) { null } } else null
        val beatJob = if (slug.isNotEmpty()) async { try { JSONObject(getText("/api/status-page/heartbeat/$slug", false)) } catch (e: Exception) { null } } else null
        val metrics = metricsJob?.await() ?: emptyMap()
        val page = pageJob?.await()
        val beats = beatJob?.await()
        // status-page monitors by name → id
        val idByName = mutableMapOf<String, Int>()
        val typeByName = mutableMapOf<String, String>()
        page?.arr("publicGroupList")?.objects()?.forEach { g ->
            g.arr("monitorList").objects().forEach { m -> idByName[m.str("name")] = m.int("id"); typeByName[m.str("name")] = m.str("type") }
        }
        val names = (metrics.keys + idByName.keys).distinct()
        if (names.isEmpty() && cfg.apiKey.isBlank() && slug.isEmpty()) throw Exception("Add an API key (Server tab → instance settings) or a status page slug to see monitors")
        names.map { name ->
            val row = metrics[name]
            @Suppress("UNCHECKED_CAST") val labels = (row?.get("labels") as? Map<String, String>).orEmpty()
            val id = idByName[name] ?: labels["monitor_id"]?.toIntOrNull()
            val hb = id?.let { beats?.obj("heartbeatList")?.arr(it.toString())?.objects() }.orEmpty()
            val metricStatus = (row?.get("monitor_status") as? Double)?.toInt()
            val status = metricStatus ?: hb.lastOrNull()?.int("status", -1) ?: -1
            val target = listOf(labels["monitor_url"], labels["monitor_hostname"]).firstOrNull { !it.isNullOrBlank() && it != "null" && it != "https://" } ?: ""
            KumaMonitor(
                name = name,
                type = labels["monitor_type"] ?: typeByName[name] ?: "",
                target = target,
                status = status,
                responseMs = (row?.get("monitor_response_time") as? Double)?.toLong()?.takeIf { it >= 0 } ?: hb.lastOrNull()?.long("ping")?.takeIf { it > 0 },
                certDays = (row?.get("monitor_cert_days_remaining") as? Double)?.toInt(),
                uptime24 = id?.let { beats?.obj("uptimeList")?.optDouble("${it}_24", Double.NaN) }?.takeIf { !it.isNaN() },
                beats = hb.takeLast(40).map { it.int("status") },
                lastMsg = hb.lastOrNull()?.str("msg").orEmpty(),
            )
        }.sortedWith(compareBy({ if (it.status == 0) 0 else 1 }, { it.name.lowercase() }))
    }
}

// ======================= Stash / Stasharr (GraphQL) =======================
data class StashStats(
    val scenes: Int, val size: Double, val duration: Double, val images: Int, val galleries: Int,
    val performers: Int, val studios: Int, val tags: Int, val plays: Int, val oCount: Int, val version: String,
)
data class StashScene(
    val id: String, val title: String, val date: String, val studio: String, val performers: List<String>,
    val duration: Double, val size: Long, val height: Int, val screenshot: String?, val plays: Int, val rating: Int?, val organized: Boolean, val path: String,
)
data class StashJob(val id: String, val status: String, val description: String, val progress: Double?, val subTasks: List<String>)

class StashApi(val cfg: ServiceConfig) {
    private val client = Http.client(cfg.trustAllCerts)

    suspend fun gql(query: String, vars: JSONObject = JSONObject()): JSONObject {
        val body = JSONObject().put("query", query).put("variables", vars).toString()
        val b = Request.Builder().url("${cfg.baseUrl}/graphql").header("Content-Type", "application/json").post(body.jsonBody())
        if (cfg.apiKey.isNotBlank()) b.header("ApiKey", cfg.apiKey)
        val o = JSONObject(Http.exec(client, b.build()))
        val errs = o.optJSONArray("errors")
        if (errs != null && errs.length() > 0) throw Exception("Stash: " + (errs.optJSONObject(0)?.str("message") ?: "error"))
        return o.obj("data") ?: JSONObject()
    }

    suspend fun status(): String = "Stash " + (gql("{ version { version } }").obj("version")?.str("version") ?: "")

    suspend fun stats(): StashStats {
        val d = gql("{ version { version } stats { scene_count scenes_size scenes_duration image_count gallery_count performer_count studio_count tag_count total_play_count total_o_count } }")
        val s = d.obj("stats") ?: JSONObject()
        return StashStats(s.int("scene_count"), s.dbl("scenes_size"), s.dbl("scenes_duration"), s.int("image_count"), s.int("gallery_count"),
            s.int("performer_count"), s.int("studio_count"), s.int("tag_count"), s.int("total_play_count"), s.int("total_o_count"),
            d.obj("version")?.str("version").orEmpty())
    }

    private fun fixUrl(u: String): String? {
        if (u.isEmpty()) return null
        return try {
            val src = u.toHttpUrl(); val base = cfg.baseUrl.toHttpUrl()
            val b = src.newBuilder().scheme(base.scheme).host(base.host).port(base.port)
            if (cfg.apiKey.isNotBlank()) b.addQueryParameter("apikey", cfg.apiKey)
            b.build().toString()
        } catch (e: Exception) { u }
    }

    suspend fun scenes(q: String, sort: String = "created_at", page: Int = 1): Pair<Int, List<StashScene>> {
        val query = """query(${'$'}f: FindFilterType) { findScenes(filter: ${'$'}f) { count scenes {
            id title date organized rating100 play_count
            files { path size duration height basename }
            paths { screenshot } studio { name } performers { name } } } }"""
        val f = JSONObject().put("per_page", 40).put("page", page).put("sort", sort).put("direction", "DESC")
        if (q.isNotBlank()) f.put("q", q)
        val r = gql(query, JSONObject().put("f", f)).obj("findScenes") ?: JSONObject()
        val showArt = cfg.artAllowed()
        return r.int("count") to r.arr("scenes").objects().map { s ->
            val file = s.arr("files").objects().firstOrNull()
            StashScene(
                id = s.str("id"),
                title = s.str("title").ifEmpty { file?.str("basename").orEmpty() },
                date = s.str("date"), studio = s.obj("studio")?.str("name").orEmpty(),
                performers = s.arr("performers").objects().map { it.str("name") },
                duration = file?.dbl("duration") ?: 0.0, size = file?.long("size") ?: 0, height = file?.int("height") ?: 0,
                screenshot = if (showArt) fixUrl(s.obj("paths")?.str("screenshot").orEmpty()) else null,
                plays = s.int("play_count"), rating = if (s.has("rating100") && !s.isNull("rating100")) s.int("rating100") else null,
                organized = s.bool("organized"), path = file?.str("path").orEmpty(),
            )
        }
    }

    suspend fun jobs(): List<StashJob> =
        gql("{ jobQueue { id status description progress subTasks } }").arr("jobQueue").objects().map { j ->
            StashJob(j.str("id"), j.str("status"), j.str("description"), if (j.isNull("progress")) null else j.dbl("progress"),
                j.arr("subTasks").let { a -> (0 until a.length()).map { a.optString(it) } })
        }

    suspend fun scan() { gql("mutation { metadataScan(input: {}) }") }
    suspend fun generate() {
        gql("mutation { metadataGenerate(input: { covers: true, sprites: true, previews: true, imagePreviews: false, markers: true, phashes: true, interactiveHeatmapsSpeeds: false }) }")
    }
    suspend fun autoTag() { gql("""mutation { metadataAutoTag(input: { performers: ["*"], studios: ["*"], tags: ["*"] }) }""") }
    suspend fun clean() { gql("mutation { metadataClean(input: { dryRun: false }) }") }
    suspend fun optimise() { gql("mutation { optimiseDatabase }") }
    suspend fun stopJob(id: String) { gql("mutation(${'$'}id: ID!) { stopJob(job_id: ${'$'}id) }", JSONObject().put("id", id)) }
    suspend fun stopAll() { gql("mutation { stopAllJobs }") }

    /** Identify with the sources set as defaults in Stash, falling back to every configured stash-box. */
    suspend fun identify() {
        val c = gql("{ configuration { general { stashBoxes { endpoint } } defaults { identify { sources { source { stash_box_endpoint scraper_id } } } } } }").obj("configuration")
        val srcs = JSONArray()
        c?.obj("defaults")?.obj("identify")?.arr("sources")?.objects()?.forEach { s ->
            val src = s.obj("source") ?: return@forEach
            val o = JSONObject()
            src.str("stash_box_endpoint").takeIf { it.isNotEmpty() }?.let { o.put("stash_box_endpoint", it) }
            src.str("scraper_id").takeIf { it.isNotEmpty() }?.let { o.put("scraper_id", it) }
            if (o.length() > 0) srcs.put(JSONObject().put("source", o))
        }
        if (srcs.length() == 0) c?.obj("general")?.arr("stashBoxes")?.objects()?.forEach { b ->
            srcs.put(JSONObject().put("source", JSONObject().put("stash_box_endpoint", b.str("endpoint"))))
        }
        if (srcs.length() == 0) throw Exception("No identify sources – add a stash-box (e.g. ThePornDB) in Stash → Settings → Metadata providers")
        gql("mutation(${'$'}i: IdentifyMetadataInput!) { metadataIdentify(input: ${'$'}i) }", JSONObject().put("i", JSONObject().put("sources", srcs)))
    }

    suspend fun setOrganized(id: String, v: Boolean) {
        gql("mutation(${'$'}i: SceneUpdateInput!) { sceneUpdate(input: ${'$'}i) { id } }", JSONObject().put("i", JSONObject().put("id", id).put("organized", v)))
    }
    suspend fun deleteScene(id: String, deleteFile: Boolean) {
        gql("mutation(${'$'}i: ScenesDestroyInput!) { scenesDestroy(input: ${'$'}i) }",
            JSONObject().put("i", JSONObject().put("ids", JSONArray().put(id)).put("delete_file", deleteFile).put("delete_generated", true)))
    }
}

@Suppress("unused") private fun keepEx(e: ApiException) = e
