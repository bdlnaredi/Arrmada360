package com.arrmada360.app.api

import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.net.Http
import com.arrmada360.app.net.arr
import com.arrmada360.app.net.dbl
import com.arrmada360.app.net.jsonBody
import com.arrmada360.app.net.long
import com.arrmada360.app.net.obj
import com.arrmada360.app.net.objects
import com.arrmada360.app.net.str
import okhttp3.Credentials
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

data class NamerFile(val file: String, val name: String, val ext: String, val size: Long, val updated: Long, val percentage: Double)

data class NamerMatch(
    val uuid: String,
    val type: String,
    val name: String,
    val date: String,
    val site: String,
    val network: String,
    val performers: String,
    val poster: String?,
    val score: Double,
)

/** Namer (ThePornDB renamer) web UI API – see namer/web/routes/api.py. */
class NamerApi(val cfg: ServiceConfig) {
    private val client = Http.client(cfg.trustAllCerts)

    private suspend fun post(path: String, body: JSONObject = JSONObject()): Any? {
        val b = Request.Builder().url("${cfg.baseUrl}/api/v1/$path").post(body.toString().jsonBody())
        if (cfg.username.isNotEmpty()) b.header("Authorization", Credentials.basic(cfg.username, cfg.password))
        val s = Http.exec(client, b.build()).trim()
        return if (s.isEmpty()) null else JSONTokener(s).nextValue()
    }

    suspend fun status(): String {
        val s = Http.exec(client, Request.Builder().url("${cfg.baseUrl}/api/healthcheck").build())
        return if (s.contains("OK", true)) "Namer OK" else "Namer"
    }

    private fun files(v: Any?): List<NamerFile> = (v as? JSONArray ?: JSONArray()).objects().map {
        NamerFile(it.str("file"), it.str("name"), it.str("ext"), it.long("size"), it.long("update_time"), it.dbl("percentage"))
    }

    suspend fun failed(): List<NamerFile> = files(post("get_files")).sortedByDescending { it.updated }
    suspend fun queued(): List<NamerFile> = files(post("get_queued"))
    suspend fun queueSize(): String = post("get_queue")?.toString() ?: "0"

    /** Raw reply of the last search, shown when nothing is found so problems are visible. */
    var lastReply: String = ""
        private set

    /** type: Any | Scenes | Movies | JAV (Namer's SearchType values). */
    suspend fun search(file: String, query: String, type: String, page: Int = 1): List<NamerMatch> =
        parseMatches(post("get_search", JSONObject().put("file", file).put("query", query).put("type", type).put("page", page)))

    /** Match by video fingerprint (phash) – works even when the file name is useless. */
    suspend fun phash(file: String, type: String): List<NamerMatch> =
        parseMatches(post("get_phash", JSONObject().put("file", file).put("type", type)))

    private fun parseMatches(v: Any?): List<NamerMatch> {
        lastReply = when (v) { null -> "(empty reply)"; is JSONObject -> v.toString().take(600); is JSONArray -> v.toString().take(600); else -> v.toString() }
        val list = when (v) {
            is JSONObject -> v.optJSONArray("files") ?: v.optJSONArray("results") ?: JSONArray()
            is JSONArray -> v
            false -> throw Exception("Namer rejected the search request (it replied “false”). Check Namer's log.")
            else -> throw Exception("Unexpected reply from Namer: ${v.toString().take(200)}")
        }
        return list.objects().map { m ->
            val l = m.obj("looked_up") ?: m
            val perf = l.opt("performers")
            val names = when (perf) {
                is JSONArray -> (0 until perf.length()).mapNotNull { i ->
                    perf.optJSONObject(i)?.let { p -> p.str("name").ifEmpty { p.obj("parent")?.str("name") ?: "" } } ?: perf.optString(i)
                }.filter { it.isNotEmpty() }
                is String -> listOf(perf)
                else -> emptyList()
            }
            val pct = listOf("name_match", "percentage", "match", "score").map { m.dbl(it, -1.0) }.firstOrNull { it >= 0 } ?: -1.0
            NamerMatch(
                uuid = l.str("uuid").ifEmpty { l.str("id") }, type = l.str("type"), name = l.str("name").ifEmpty { l.str("title") }, date = l.str("date"),
                site = l.str("site"), network = l.str("network"), performers = names.joinToString(", "),
                poster = l.str("poster_url").ifEmpty { null }, score = if (pct in 0.0..1.0 && pct > 0) pct * 100 else pct,
            )
        }.sortedByDescending { it.score }
    }

    suspend fun rename(file: String, sceneId: String) { post("rename", JSONObject().put("file", file).put("scene_id", sceneId)) }

    suspend fun delete(file: String): Boolean = post("delete", JSONObject().put("file", file)) == true

    suspend fun failedLog(file: String): String {
        val r = post("read_failed_log", JSONObject().put("file", file)) as? JSONObject ?: return ""
        val d = r.opt("data")
        return when (d) { null, JSONObject.NULL -> ""; is String -> d; is JSONObject -> d.toString(2); is JSONArray -> d.toString(2); else -> d.toString() }
    }
}
