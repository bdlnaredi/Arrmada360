package com.arrmada360.app.api

import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.data.ServiceType
import com.arrmada360.app.net.Http
import com.arrmada360.app.net.arr
import com.arrmada360.app.net.bool
import com.arrmada360.app.net.buildUrl
import com.arrmada360.app.net.dbl
import com.arrmada360.app.net.int
import com.arrmada360.app.net.jsonBody
import com.arrmada360.app.net.long
import com.arrmada360.app.net.obj
import com.arrmada360.app.net.objects
import com.arrmada360.app.net.str
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

// ======================= Indexer search (Prowlarr + Jackett) =======================
data class IndexerResult(
    val title: String,
    val indexer: String,
    val size: Long,
    val seeders: Int?,
    val peers: Int?,
    val date: String,
    val downloadUrl: String?,
    val magnet: String?,
    val infoUrl: String?,
    val guid: String,
    val indexerId: Int,
    val protocol: String,
    val categories: String,
)

data class IndexerInfo(val id: Int, val name: String, val enabled: Boolean, val protocol: String, val privacy: String, val failing: Boolean)

val SEARCH_CATEGORIES = listOf(
    "All" to emptyList(),
    "Movies" to listOf(2000),
    "TV" to listOf(5000),
    "Music" to listOf(3000),
    "Books" to listOf(7000),
    "Anime" to listOf(5070),
    "PC" to listOf(4000),
)

class ProwlarrApi(val cfg: ServiceConfig) {
    private val client = Http.client(cfg.trustAllCerts)
    private fun rb(path: String, q: Map<String, Any?> = emptyMap()) =
        Request.Builder().url(buildUrl("${cfg.baseUrl}/api/v1/$path", q)).header("X-Api-Key", cfg.apiKey)

    suspend fun status(): String = "Prowlarr " + JSONObject(Http.exec(client, rb("system/status").build())).str("version")

    suspend fun search(query: String, cats: List<Int>): List<IndexerResult> {
        val u = buildUrl("${cfg.baseUrl}/api/v1/search", mapOf("query" to query, "type" to "search", "limit" to 100)).newBuilder()
        cats.forEach { u.addQueryParameter("categories", it.toString()) }
        val s = Http.exec(client, Request.Builder().url(u.build()).header("X-Api-Key", cfg.apiKey).build())
        return JSONArray(s).objects().map { o ->
            IndexerResult(
                title = o.str("title"), indexer = o.str("indexer"), size = o.long("size"),
                seeders = if (o.has("seeders") && !o.isNull("seeders")) o.int("seeders") else null,
                peers = if (o.has("leechers") && !o.isNull("leechers")) o.int("leechers") else null,
                date = o.str("publishDate"), downloadUrl = o.str("downloadUrl").ifEmpty { null },
                magnet = o.str("magnetUrl").ifEmpty { null }, infoUrl = o.str("infoUrl").ifEmpty { null },
                guid = o.str("guid"), indexerId = o.int("indexerId"), protocol = o.str("protocol"),
                categories = o.arr("categories").objects().joinToString { it.str("name") },
            )
        }
    }

    /** Sends the release to the download client configured inside Prowlarr. */
    suspend fun grab(r: IndexerResult) {
        Http.exec(client, rb("search").post(JSONObject().put("guid", r.guid).put("indexerId", r.indexerId).toString().jsonBody()).build())
    }

    suspend fun indexers(): List<IndexerInfo> {
        val failing = try {
            JSONArray(Http.exec(client, rb("indexerstatus").build())).objects().map { it.int("indexerId") }.toSet()
        } catch (e: Exception) { emptySet() }
        return JSONArray(Http.exec(client, rb("indexer").build())).objects().map {
            IndexerInfo(it.int("id"), it.str("name"), it.bool("enable"), it.str("protocol"), it.str("privacy"), it.int("id") in failing)
        }.sortedBy { it.name.lowercase() }
    }

    suspend fun testIndexer(id: Int) {
        val o = Http.exec(client, rb("indexer/$id").build())
        Http.exec(client, rb("indexer/test").post(o.jsonBody()).build())
    }

    suspend fun setIndexerEnabled(id: Int, enabled: Boolean) {
        val o = JSONObject(Http.exec(client, rb("indexer/$id").build()))
        o.put("enable", enabled)
        Http.exec(client, rb("indexer/$id").put(o.toString().jsonBody()).build())
    }
}

data class IndexerStat(val name: String, val queries: Int, val grabs: Int, val failed: Int, val avgMs: Int)
data class SearchHistory(val event: String, val query: String, val indexer: String, val date: String, val ok: Boolean)

suspend fun ProwlarrApi.stats(): List<IndexerStat> {
    val s = Http.exec(Http.client(cfg.trustAllCerts), Request.Builder().url(buildUrl("${cfg.baseUrl}/api/v1/indexerstats")).header("X-Api-Key", cfg.apiKey).build())
    return JSONObject(s).arr("indexers").objects().map {
        IndexerStat(it.str("indexerName"), it.int("numberOfQueries"), it.int("numberOfGrabs"), it.int("numberOfFailedQueries"), it.int("averageResponseTime"))
    }.sortedByDescending { it.queries }
}

suspend fun ProwlarrApi.searchHistory(): List<SearchHistory> {
    val names = try { indexers().associate { it.id to it.name } } catch (e: Exception) { emptyMap() }
    val u = buildUrl("${cfg.baseUrl}/api/v1/history", mapOf("pageSize" to 100, "sortKey" to "date", "sortDirection" to "descending"))
    val s = Http.exec(Http.client(cfg.trustAllCerts), Request.Builder().url(u).header("X-Api-Key", cfg.apiKey).build())
    return JSONObject(s).arr("records").objects().map {
        val d = it.obj("data")
        SearchHistory(
            event = it.str("eventType").replace(Regex("([a-z])([A-Z])"), "$1 $2").replaceFirstChar { c -> c.uppercase() },
            query = d?.str("query")?.ifEmpty { null } ?: d?.str("grabTitle")?.ifEmpty { null } ?: d?.str("title") ?: "",
            indexer = names[it.int("indexerId")] ?: "",
            date = it.str("date"), ok = it.bool("successful", true),
        )
    }
}

class JackettApi(val cfg: ServiceConfig) {
    private val client = Http.client(cfg.trustAllCerts)

    suspend fun status(): String {
        Http.exec(client, Request.Builder().url(buildUrl("${cfg.baseUrl}/api/v2.0/indexers/all/results/torznab/api", mapOf("apikey" to cfg.apiKey, "t" to "caps"))).build())
        return "Jackett OK"
    }

    suspend fun search(query: String, cats: List<Int>): List<IndexerResult> {
        val u = buildUrl("${cfg.baseUrl}/api/v2.0/indexers/all/results", mapOf("apikey" to cfg.apiKey, "Query" to query)).newBuilder()
        cats.forEach { u.addQueryParameter("Category[]", it.toString()) }
        val o = JSONObject(Http.exec(client, Request.Builder().url(u.build()).build()))
        return o.arr("Results").objects().map { r ->
            IndexerResult(
                title = r.str("Title"), indexer = r.str("Tracker"), size = r.long("Size"),
                seeders = if (r.isNull("Seeders")) null else r.int("Seeders"),
                peers = if (r.isNull("Peers")) null else r.int("Peers"),
                date = r.str("PublishDate"), downloadUrl = r.str("Link").ifEmpty { null },
                magnet = r.str("MagnetUri").ifEmpty { null }, infoUrl = r.str("Details").ifEmpty { null },
                guid = r.str("Guid"), indexerId = 0, protocol = "torrent", categories = r.str("CategoryDesc"),
            )
        }
    }
}

// ======================= Overseerr / Jellyseerr =======================
data class SeerrMedia(
    val tmdbId: Int,
    val mediaType: String,
    val title: String,
    val year: String,
    val poster: String?,
    val backdrop: String?,
    val overview: String,
    val status: Int,
)

data class SeerrRequest(
    val id: Int,
    val status: Int,
    val type: String,
    val tmdbId: Int,
    val title: String,
    val poster: String?,
    val requestedBy: String,
    val created: String,
    val mediaStatus: Int,
    val seasons: String,
    val is4k: Boolean,
)

data class SeerrUser(val id: Int, val name: String, val email: String, val requests: Int, val avatar: String?)
data class SeerrJob(val id: String, val name: String, val type: String, val next: String, val running: Boolean)

class SeerrApi(val cfg: ServiceConfig) {
    private val client = Http.client(cfg.trustAllCerts)
    private fun rb(path: String, q: Map<String, Any?> = emptyMap()) =
        Request.Builder().url(buildUrl("${cfg.baseUrl}/api/v1/$path", q)).header("X-Api-Key", cfg.apiKey)

    private suspend fun get(path: String, q: Map<String, Any?> = emptyMap()) = JSONObject(Http.exec(client, rb(path, q).build()))
    private suspend fun post(path: String, body: JSONObject = JSONObject()) =
        Http.exec(client, rb(path).post(body.toString().jsonBody()).build())

    suspend fun status(): String = "Seerr " + get("status").str("version")

    private fun img(p: String, size: String = "w342") = if (p.isEmpty()) null else "https://image.tmdb.org/t/p/$size$p"

    private val titleCache = HashMap<String, Pair<String, String?>>()

    suspend fun requests(filter: String): List<SeerrRequest> = coroutineScope {
        val res = get("request", mapOf("take" to 50, "skip" to 0, "sort" to "added", "filter" to filter)).arr("results").objects()
        res.map { r ->
            async {
                val media = r.obj("media") ?: JSONObject()
                val tmdb = media.int("tmdbId")
                val type = r.str("type").ifEmpty { media.str("mediaType") }
                val key = "$type/$tmdb"
                val info = titleCache[key] ?: try {
                    val d = get(if (type == "tv") "tv/$tmdb" else "movie/$tmdb")
                    val t = d.str("title").ifEmpty { d.str("name") }
                    val y = (d.str("releaseDate").ifEmpty { d.str("firstAirDate") }).take(4)
                    Pair(if (y.isNotEmpty()) "$t ($y)" else t, img(d.str("posterPath")))
                } catch (e: Exception) {
                    Pair("TMDB $tmdb", null)
                }.also { titleCache[key] = it }
                val seasons = r.arr("seasons").objects().joinToString(", ") { "S" + it.int("seasonNumber") }
                SeerrRequest(
                    id = r.int("id"), status = r.int("status"), type = type, tmdbId = tmdb,
                    title = info.first, poster = info.second,
                    requestedBy = r.obj("requestedBy")?.let { u -> u.str("displayName").ifEmpty { u.str("plexUsername") }.ifEmpty { u.str("email") } } ?: "",
                    created = r.str("createdAt"), mediaStatus = media.int("status"), seasons = seasons, is4k = r.bool("is4k"),
                )
            }
        }.awaitAll()
    }

    suspend fun approve(id: Int) { post("request/$id/approve") }
    suspend fun decline(id: Int) { post("request/$id/decline") }
    suspend fun deleteRequest(id: Int) { Http.exec(client, rb("request/$id").delete().build()) }
    suspend fun retry(id: Int) { post("request/$id/retry") }

    private fun parseMedia(o: JSONObject): SeerrMedia? {
        val type = o.str("mediaType")
        if (type != "movie" && type != "tv") return null
        return SeerrMedia(
            tmdbId = o.int("id"), mediaType = type,
            title = o.str("title").ifEmpty { o.str("name") },
            year = o.str("releaseDate").ifEmpty { o.str("firstAirDate") }.take(4),
            poster = img(o.str("posterPath")), backdrop = img(o.str("backdropPath"), "w780"),
            overview = o.str("overview"), status = o.obj("mediaInfo")?.int("status") ?: 0,
        )
    }

    suspend fun search(q: String): List<SeerrMedia> =
        get("search", mapOf("query" to q, "page" to 1)).arr("results").objects().mapNotNull { parseMedia(it) }

    suspend fun discover(kind: String): List<SeerrMedia> {
        val path = when (kind) {
            "movies" -> "discover/movies"; "tv" -> "discover/tv"; "upcoming" -> "discover/movies/upcoming"
            else -> "discover/trending"
        }
        return get(path, mapOf("page" to 1)).arr("results").objects().mapNotNull {
            if (!it.has("mediaType")) it.put("mediaType", if (kind == "tv") "tv" else "movie")
            parseMedia(it)
        }
    }

    suspend fun request(m: SeerrMedia, is4k: Boolean = false) {
        val body = JSONObject().put("mediaType", m.mediaType).put("mediaId", m.tmdbId).put("is4k", is4k)
        if (m.mediaType == "tv") body.put("seasons", "all")
        post("request", body)
    }

    suspend fun counts(): JSONObject = get("request/count")

    suspend fun users(): List<SeerrUser> = get("user", mapOf("take" to 100, "skip" to 0, "sort" to "requests")).arr("results").objects().map {
        val av = it.str("avatar")
        SeerrUser(
            id = it.int("id"),
            name = it.str("displayName").ifEmpty { it.str("plexUsername") }.ifEmpty { it.str("username") }.ifEmpty { it.str("email") },
            email = it.str("email"), requests = it.int("requestCount"),
            avatar = if (av.startsWith("/")) cfg.baseUrl + av else av.ifEmpty { null },
        )
    }

    suspend fun statusObj(): JSONObject = get("status")

    suspend fun jobs(): List<SeerrJob> = JSONArray(Http.exec(client, rb("settings/jobs").build())).objects().map {
        SeerrJob(it.str("id"), it.str("name"), it.str("type"), it.str("nextExecutionTime"), it.bool("running"))
    }

    suspend fun runJob(id: String) { post("settings/jobs/$id/run") }
}

// ======================= Tautulli =======================
data class StreamSession(
    val sessionId: String,
    val user: String,
    val title: String,
    val subtitle: String,
    val thumb: String?,
    val progress: Int,
    val state: String,
    val player: String,
    val decision: String,
    val quality: String,
    val bandwidthKbps: Long,
    val location: String,
)

data class PlayHistory(val user: String, val title: String, val date: Long, val duration: Long, val percent: Int, val player: String, val thumb: String?)

data class TautulliUser(val name: String, val plays: Int, val lastSeen: Long, val duration: Long, val thumb: String?, val lastPlayed: String)
data class TautulliLibrary(val name: String, val type: String, val count: Int, val parents: Int, val children: Int)

class TautulliApi(val cfg: ServiceConfig) {
    private val client = Http.client(cfg.trustAllCerts)

    private suspend fun cmd(cmd: String, extra: Map<String, Any?> = emptyMap()): JSONObject {
        val q = mutableMapOf<String, Any?>("apikey" to cfg.apiKey, "cmd" to cmd)
        q.putAll(extra)
        val o = JSONObject(Http.exec(client, Request.Builder().url(buildUrl("${cfg.baseUrl}/api/v2", q)).build())).obj("response") ?: JSONObject()
        if (o.str("result") != "success") throw Exception("Tautulli: " + o.str("message", "error"))
        return o
    }

    fun imageUrl(thumb: String): String? = if (thumb.isEmpty()) null
    else buildUrl("${cfg.baseUrl}/api/v2", mapOf("apikey" to cfg.apiKey, "cmd" to "pms_image_proxy", "img" to thumb, "width" to 300, "height" to 450, "fallback" to "poster")).toString()

    suspend fun status(): String = "Tautulli " + (cmd("get_tautulli_info").obj("data")?.str("tautulli_version") ?: "")

    suspend fun activity(): Pair<List<StreamSession>, Long> {
        val d = cmd("get_activity").obj("data") ?: JSONObject()
        val list = d.arr("sessions").objects().map { s ->
            val type = s.str("media_type")
            val title = if (type == "episode") s.str("grandparent_title") else s.str("title")
            val sub = when (type) {
                "episode" -> "S%s · E%s · %s".format(s.str("parent_media_index"), s.str("media_index"), s.str("title"))
                "track" -> "${s.str("grandparent_title")} — ${s.str("parent_title")}"
                else -> s.str("year")
            }
            val thumb = if (type == "episode") s.str("grandparent_thumb") else s.str("thumb")
            StreamSession(
                sessionId = s.str("session_id"), user = s.str("friendly_name"), title = if (type == "track") s.str("title") else title,
                subtitle = sub, thumb = imageUrl(thumb), progress = s.int("progress_percent"), state = s.str("state"),
                player = "${s.str("player")} · ${s.str("platform")}",
                decision = s.str("transcode_decision").replaceFirstChar { it.uppercase() },
                quality = s.str("quality_profile"), bandwidthKbps = s.long("bandwidth"), location = s.str("location").uppercase(),
            )
        }
        return list to d.long("total_bandwidth")
    }

    suspend fun history(): List<PlayHistory> =
        (cmd("get_history", mapOf("length" to 50)).obj("data")?.arr("data") ?: JSONArray()).objects().map { h ->
            PlayHistory(
                user = h.str("friendly_name"), title = h.str("full_title"), date = h.long("date"), duration = h.long("play_duration", h.long("duration")),
                percent = h.int("percent_complete"), player = h.str("player"),
                thumb = imageUrl(if (h.str("media_type") == "episode") h.str("grandparent_thumb") else h.str("thumb")),
            )
        }

    suspend fun users(): List<TautulliUser> =
        (cmd("get_users_table", mapOf("length" to 100, "order_column" to "plays", "order_dir" to "desc")).obj("data")?.arr("data") ?: JSONArray()).objects().map {
            TautulliUser(it.str("friendly_name"), it.int("plays"), it.long("last_seen"), it.long("duration"), it.str("user_thumb").ifEmpty { null }, it.str("last_played"))
        }

    suspend fun serverInfo(): JSONObject = cmd("get_server_info").obj("data") ?: JSONObject()

    suspend fun libraries(): List<TautulliLibrary> =
        (cmd("get_libraries").optJSONArray("data") ?: JSONArray()).objects().map {
            TautulliLibrary(it.str("section_name"), it.str("section_type"), it.int("count"), it.int("parent_count"), it.int("child_count"))
        }

    suspend fun terminate(sessionId: String, message: String) {
        cmd("terminate_session", mapOf("session_id" to sessionId, "message" to message))
    }

    suspend fun homeStats(): List<Pair<String, List<String>>> =
        (cmd("get_home_stats", mapOf("time_range" to 30, "stats_count" to 5)).optJSONArray("data") ?: JSONArray()).objects().mapNotNull { st ->
            val id = st.str("stat_id")
            val title = when (id) {
                "top_movies" -> "Most watched movies"; "top_tv" -> "Most watched shows"; "top_users" -> "Most active users"
                "top_platforms" -> "Top platforms"; "popular_movies" -> "Popular movies"; "popular_tv" -> "Popular shows"
                else -> return@mapNotNull null
            }
            val rows = st.arr("rows").objects().map { r ->
                val name = r.str("title").ifEmpty { r.str("friendly_name") }.ifEmpty { r.str("platform") }
                val plays = r.int("total_plays").let { if (it > 0) "$it plays" else "${r.int("users_watched")} users" }
                "$name  ·  $plays"
            }
            title to rows
        }
}

// ======================= Unraid (official GraphQL API, Unraid 7.2+) =======================
data class UnraidSystem(val hostname: String, val version: String, val uptime: String, val cpu: String, val cpuPercent: Double?, val memUsed: Long, val memTotal: Long)
data class UnraidDisk(val name: String, val status: String, val temp: Int?, val size: Long, val kind: String, val fsFree: Long, val fsSize: Long, val errors: Int = 0, val spinning: Boolean? = null, val fsType: String = "")
data class UnraidArray(val state: String, val used: Long, val total: Long, val disks: List<UnraidDisk>)
data class UnraidContainer(val id: String, val name: String, val state: String, val status: String, val image: String, val autoStart: Boolean)
data class UnraidVm(val id: String, val name: String, val state: String)
data class UnraidShare(val name: String, val comment: String, val used: Long, val free: Long, val size: Long, val cache: String)
data class UnraidNotification(val id: String, val title: String, val subject: String, val description: String, val importance: String, val timestamp: String)
data class ParityStatus(val status: String, val progress: Double?, val errors: Int?, val running: Boolean, val speed: String, val date: String)

class UnraidApi(val cfg: ServiceConfig) {
    private val client = Http.client(cfg.trustAllCerts)

    suspend fun gql(query: String): JSONObject {
        val body = JSONObject().put("query", query).toString()
        val s = Http.exec(client, Request.Builder().url("${cfg.baseUrl}/graphql").header("x-api-key", cfg.apiKey)
            .header("Content-Type", "application/json").post(body.jsonBody()).build())
        val o = JSONObject(s)
        val data = o.obj("data")
        val errs = o.arr("errors").objects()
        if (errs.isNotEmpty() && (data == null || data.length() == 0)) throw Exception("Unraid: " + errs.first().str("message"))
        return data ?: JSONObject()
    }

    private suspend fun firstOk(vararg queries: String): JSONObject {
        var last: Exception? = null
        for (q in queries) {
            try { return gql(q) } catch (e: Exception) { last = e }
        }
        throw last ?: Exception("Unraid query failed")
    }

    suspend fun status(): String {
        val d = firstOk("query { info { os { hostname } } }", "query { online }")
        return "Unraid " + (d.obj("info")?.obj("os")?.str("hostname") ?: "online")
    }

    suspend fun system(): UnraidSystem {
        val d = firstOk(
            "query { info { os { hostname uptime } cpu { brand cores threads } } vars { version } }",
            "query { info { os { hostname uptime } cpu { brand cores threads } } }",
            "query { info { os { hostname } } }",
        )
        val info = d.obj("info") ?: JSONObject()
        val os = info.obj("os") ?: JSONObject()
        val cpu = info.obj("cpu")
        var cpuPct: Double? = null
        var memUsed = 0L
        var memTotal = 0L
        try {
            val m = gql("query { metrics { cpu { percentTotal } memory { total used } } }").obj("metrics")
            cpuPct = m?.obj("cpu")?.dbl("percentTotal")
            memUsed = m?.obj("memory")?.long("used") ?: 0
            memTotal = m?.obj("memory")?.long("total") ?: 0
        } catch (e: Exception) { /* older API */ }
        return UnraidSystem(
            hostname = os.str("hostname"), version = d.obj("vars")?.str("version") ?: "", uptime = os.str("uptime"),
            cpu = cpu?.let { "${it.str("brand")} · ${it.int("cores")}C/${it.int("threads")}T" } ?: "",
            cpuPercent = cpuPct, memUsed = memUsed, memTotal = memTotal,
        )
    }

    suspend fun array(): UnraidArray {
        val d = firstOk(
            "query { array { state capacity { kilobytes { free used total } } parities { name status temp size numErrors isSpinning } disks { name status temp size fsFree fsSize fsType numErrors isSpinning } caches { name status temp size fsFree fsSize fsType numErrors isSpinning } } }",
            "query { array { state capacity { kilobytes { free used total } } parities { name status temp size } disks { name status temp size fsFree fsSize } caches { name status temp size fsFree fsSize } } }",
            "query { array { state capacity { kilobytes { free used total } } parities { name status temp } disks { name status temp } caches { name status temp } } }",
            "query { array { state capacity { kilobytes { free used total } } } }",
        )
        val a = d.obj("array") ?: JSONObject()
        val kb = a.obj("capacity")?.obj("kilobytes")
        fun disks(key: String, kind: String) = a.arr(key).objects().map {
            UnraidDisk(it.str("name"), it.str("status"), if (it.isNull("temp") || !it.has("temp")) null else it.int("temp"),
                it.long("size") * 1024, kind, it.long("fsFree") * 1024, it.long("fsSize") * 1024,
                errors = it.int("numErrors"), spinning = if (it.has("isSpinning") && !it.isNull("isSpinning")) it.bool("isSpinning") else null, fsType = it.str("fsType"))
        }
        return UnraidArray(
            state = a.str("state"),
            used = (kb?.str("used")?.toLongOrNull() ?: 0L) * 1024,
            total = (kb?.str("total")?.toLongOrNull() ?: 0L) * 1024,
            disks = disks("parities", "Parity") + disks("disks", "Data") + disks("caches", "Cache"),
        )
    }

    suspend fun containers(): List<UnraidContainer> {
        val d = firstOk(
            "query { docker { containers { id names state status image autoStart } } }",
            "query { docker { containers { id names state } } }",
        )
        return (d.obj("docker")?.arr("containers") ?: JSONArray()).objects().map {
            val names = it.optJSONArray("names")
            val n = (names?.optString(0) ?: it.str("name")).trimStart('/')
            UnraidContainer(it.str("id"), n, it.str("state"), it.str("status"), it.str("image"), it.bool("autoStart"))
        }.sortedBy { it.name.lowercase() }
    }

    private fun esc(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")

    suspend fun containerAction(id: String, action: String) {
        // action: start | stop
        gql("mutation { docker { $action(id: \"${esc(id)}\") { id state } } }")
    }

    suspend fun vms(): List<UnraidVm> {
        val d = firstOk(
            "query { vms { domains { id name state } } }",
            "query { vms { domains { uuid name state } } }",
            "query { vms { domain { uuid name state } } }",
        )
        val v = d.obj("vms") ?: JSONObject()
        val list = v.optJSONArray("domains") ?: v.optJSONArray("domain") ?: JSONArray()
        return list.objects().map { UnraidVm(it.str("id").ifEmpty { it.str("uuid") }, it.str("name"), it.str("state")) }
    }

    suspend fun vmAction(id: String, action: String) {
        // action: start | stop | pause | resume | forceStop | reboot
        gql("mutation { vm { $action(id: \"${esc(id)}\") } }")
    }

    suspend fun shares(): List<UnraidShare> {
        val d = firstOk(
            "query { shares { name comment free used size cache } }",
            "query { shares { name free used size } }",
            "query { shares { name free used } }",
        )
        return d.arr("shares").objects().map {
            UnraidShare(it.str("name"), it.str("comment"), it.long("used") * 1024, it.long("free") * 1024, it.long("size") * 1024, it.str("cache"))
        }.sortedBy { it.name.lowercase() }
    }

    suspend fun setArrayState(start: Boolean) {
        gql("mutation { array { setState(input: { desiredState: ${if (start) "START" else "STOP"} }) { state } } }")
    }

    suspend fun parityStatus(): ParityStatus? = try {
        val d = firstOk(
            "query { array { parityCheckStatus { status progress errors running speed date } } }",
            "query { parityHistory { date status errors speed } }",
        )
        val live = d.obj("array")?.obj("parityCheckStatus")
        if (live != null) ParityStatus(live.str("status"), if (live.has("progress") && !live.isNull("progress")) live.dbl("progress") else null,
            if (live.has("errors") && !live.isNull("errors")) live.int("errors") else null, live.bool("running"), live.str("speed"), live.str("date"))
        else d.arr("parityHistory").objects().firstOrNull()?.let {
            ParityStatus("Last check: " + it.str("status"), null, it.int("errors"), false, it.str("speed"), it.str("date"))
        }
    } catch (e: Exception) { null }

    /** action: start | startCorrect | pause | resume | cancel */
    suspend fun parity(action: String) {
        when (action) {
            "start" -> gql("mutation { parityCheck { start(correct: false) } }")
            "startCorrect" -> gql("mutation { parityCheck { start(correct: true) } }")
            else -> gql("mutation { parityCheck { $action } }")
        }
    }

    suspend fun notifications(): List<UnraidNotification> {
        val d = firstOk(
            "query { notifications { list(filter: { type: UNREAD, offset: 0, limit: 30 }) { id title subject description importance timestamp } } }",
            "query { notifications { list(filter: { type: UNREAD, offset: 0, limit: 30 }) { id title subject description importance } } }",
        )
        return (d.obj("notifications")?.arr("list") ?: JSONArray()).objects().map {
            UnraidNotification(it.str("id"), it.str("title"), it.str("subject"), it.str("description"), it.str("importance"), it.str("timestamp"))
        }
    }

    suspend fun archiveNotification(id: String) {
        gql("mutation { archiveNotification(id: \"${esc(id)}\") { id } }")
    }

    /** action: reboot | shutdown */
    suspend fun power(action: String) { gql("mutation { $action }") }

    suspend fun notificationsUnread(): Int = try {
        gql("query { notifications { overview { unread { total } } } }").obj("notifications")?.obj("overview")?.obj("unread")?.int("total") ?: 0
    } catch (e: Exception) { 0 }
}

// ======================= Connection test =======================
object ServiceTester {
    suspend fun test(cfg: ServiceConfig): String = when (cfg.type) {
        ServiceType.SONARR, ServiceType.RADARR, ServiceType.LIDARR, ServiceType.READARR -> ArrApi(cfg).status()
        ServiceType.PROWLARR -> ProwlarrApi(cfg).status()
        ServiceType.JACKETT -> JackettApi(cfg).status()
        ServiceType.SEERR -> SeerrApi(cfg).status()
        ServiceType.TAUTULLI -> TautulliApi(cfg).status()
        ServiceType.UNRAID -> UnraidApi(cfg).status()
        ServiceType.NAMER -> NamerApi(cfg).status()
        else -> DownloadClients.of(cfg)?.version() ?: "OK"
    }
}
