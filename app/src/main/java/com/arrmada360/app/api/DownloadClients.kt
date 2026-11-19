package com.arrmada360.app.api

import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.data.ServiceType
import com.arrmada360.app.net.ApiException
import com.arrmada360.app.net.Http
import com.arrmada360.app.net.MemoryCookieJar
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder

data class DlItem(
    val id: String,
    val name: String,
    val progress: Float,
    val state: String,
    val paused: Boolean,
    val done: Boolean,
    val dlSpeed: Long,
    val upSpeed: Long,
    val size: Long,
    val eta: Long,
    val category: String = "",
    val ratio: Double = -1.0,
    val error: Boolean = false,
    val addedOn: Long = 0,
)

data class DlSnapshot(val items: List<DlItem>, val dlSpeed: Long, val upSpeed: Long, val globalPaused: Boolean = false, val freeSpace: Long = -1)

data class DlHistory(val name: String, val status: String, val size: Long, val time: Long, val failed: Boolean)

interface DownloadClient {
    val cfg: ServiceConfig
    suspend fun version(): String
    suspend fun snapshot(): DlSnapshot
    suspend fun pause(ids: List<String>)
    suspend fun resume(ids: List<String>)
    suspend fun delete(ids: List<String>, withData: Boolean)
    suspend fun addUrl(url: String)
    suspend fun pauseAll()
    suspend fun resumeAll()
    suspend fun history(): List<DlHistory> = emptyList()
    val hasHistory: Boolean get() = false
    val isUsenet: Boolean get() = false
}

object DownloadClients {
    private val cache = HashMap<ServiceConfig, DownloadClient>()

    fun of(cfg: ServiceConfig): DownloadClient? = synchronized(cache) {
        cache[cfg] ?: when (cfg.type) {
            ServiceType.QBITTORRENT -> QbitClient(cfg)
            ServiceType.TRANSMISSION -> TransmissionClient(cfg)
            ServiceType.DELUGE -> DelugeClient(cfg)
            ServiceType.RUTORRENT -> RutorrentClient(cfg)
            ServiceType.SABNZBD -> SabClient(cfg)
            ServiceType.NZBGET -> NzbgetClient(cfg)
            else -> null
        }?.also { cache[cfg] = it }
    }

    fun clearCache() = synchronized(cache) { cache.clear() }

    /** Snapshot + record speeds for the live charts. */
    suspend fun snapshot(c: DownloadClient): DlSnapshot {
        val s = c.snapshot()
        SpeedHistory.record(c.cfg.id, s.dlSpeed, s.upSpeed)
        SpeedHistory.active[c.cfg.id] = s.items.count { !it.done && !it.paused }
        return s
    }
}

private fun etaOf(left: Long, speed: Long): Long = if (speed > 0) left / speed else -1

// ======================= qBittorrent =======================
class QbitClient(override val cfg: ServiceConfig) : DownloadClient {
    private val jar = MemoryCookieJar()
    private val client: OkHttpClient = Http.client(cfg.trustAllCerts).newBuilder().cookieJar(jar).build()
    private var loggedIn = false

    private suspend fun login() {
        val body = FormBody.Builder().add("username", cfg.username).add("password", cfg.password).build()
        val r = Http.exec(client, Request.Builder().url("${cfg.baseUrl}/api/v2/auth/login").header("Referer", cfg.baseUrl).post(body).build())
        if (!r.trim().startsWith("Ok") && cfg.username.isNotEmpty()) throw Exception("qBittorrent login failed – check username/password")
        loggedIn = true
    }

    private suspend fun call(path: String, form: Map<String, String>? = null, multipart: Boolean = false): String {
        if (!loggedIn) login()
        fun build(): Request {
            val b = Request.Builder().url("${cfg.baseUrl}/api/v2/$path").header("Referer", cfg.baseUrl)
            if (form != null) {
                if (multipart) {
                    val mb = MultipartBody.Builder().setType(MultipartBody.FORM)
                    form.forEach { (k, v) -> mb.addFormDataPart(k, v) }
                    b.post(mb.build())
                } else {
                    val fb = FormBody.Builder()
                    form.forEach { (k, v) -> fb.add(k, v) }
                    b.post(fb.build())
                }
            }
            return b.build()
        }
        return try {
            Http.exec(client, build())
        } catch (e: ApiException) {
            if (e.code == 403 || e.code == 401) {
                login(); Http.exec(client, build())
            } else throw e
        }
    }

    override suspend fun version() = "qBittorrent " + call("app/version")

    override suspend fun snapshot(): DlSnapshot {
        val list = JSONArray(call("torrents/info")).objects().map { o ->
            val st = o.str("state")
            val paused = st.startsWith("paused") || st.startsWith("stopped")
            DlItem(
                id = o.str("hash"), name = o.str("name"), progress = o.dbl("progress").toFloat(),
                state = qbState(st), paused = paused, done = o.dbl("progress") >= 1.0,
                dlSpeed = o.long("dlspeed"), upSpeed = o.long("upspeed"), size = o.long("size"),
                eta = o.long("eta").let { if (it >= 8640000) -1 else it }, category = o.str("category"),
                ratio = o.dbl("ratio"), error = st == "error" || st == "missingFiles", addedOn = o.long("added_on"),
            )
        }
        val t = JSONObject(call("transfer/info"))
        val free = try { JSONObject(call("sync/maindata")).obj("server_state")?.long("free_space_on_disk", -1) ?: -1 } catch (e: Exception) { -1L }
        return DlSnapshot(list, t.long("dl_info_speed"), t.long("up_info_speed"), freeSpace = free)
    }

    private fun qbState(s: String) = when (s) {
        "downloading" -> "Downloading"; "forcedDL" -> "Downloading [F]"; "metaDL", "forcedMetaDL" -> "Fetching metadata"
        "stalledDL" -> "Stalled"; "uploading" -> "Seeding"; "forcedUP" -> "Seeding [F]"; "stalledUP" -> "Seeding"
        "pausedDL", "stoppedDL" -> "Paused"; "pausedUP", "stoppedUP" -> "Completed"
        "queuedDL" -> "Queued"; "queuedUP" -> "Queued (seed)"; "checkingDL", "checkingUP", "checkingResumeData" -> "Checking"
        "moving" -> "Moving"; "error" -> "Error"; "missingFiles" -> "Missing files"; else -> s
    }

    private suspend fun twoWay(newPath: String, oldPath: String, hashes: String) {
        try { call(newPath, mapOf("hashes" to hashes)) } catch (e: ApiException) {
            if (e.code == 404) call(oldPath, mapOf("hashes" to hashes)) else throw e
        }
    }

    override suspend fun pause(ids: List<String>) = twoWay("torrents/stop", "torrents/pause", ids.joinToString("|"))
    override suspend fun resume(ids: List<String>) = twoWay("torrents/start", "torrents/resume", ids.joinToString("|"))
    override suspend fun pauseAll() = twoWay("torrents/stop", "torrents/pause", "all")
    override suspend fun resumeAll() = twoWay("torrents/start", "torrents/resume", "all")
    override suspend fun delete(ids: List<String>, withData: Boolean) {
        call("torrents/delete", mapOf("hashes" to ids.joinToString("|"), "deleteFiles" to withData.toString()))
    }
    override suspend fun addUrl(url: String) {
        call("torrents/add", mapOf("urls" to url), multipart = true)
    }

    suspend fun categories(): List<String> = try { JSONObject(call("torrents/categories")).keys().asSequence().toList() } catch (e: Exception) { emptyList() }
    suspend fun setCategory(ids: List<String>, cat: String) { call("torrents/setCategory", mapOf("hashes" to ids.joinToString("|"), "category" to cat)) }
    suspend fun recheck(ids: List<String>) { call("torrents/recheck", mapOf("hashes" to ids.joinToString("|"))) }
    suspend fun forceStart(ids: List<String>) { call("torrents/setForceStart", mapOf("hashes" to ids.joinToString("|"), "value" to "true")) }
    suspend fun toggleAltSpeed() { call("transfer/toggleSpeedLimitsMode", emptyMap()) }
    suspend fun altSpeedOn(): Boolean = call("transfer/speedLimitsMode").trim() == "1"
}

// ======================= Transmission =======================
class TransmissionClient(override val cfg: ServiceConfig) : DownloadClient {
    private val client = Http.client(cfg.trustAllCerts)
    private var sid = ""
    private val rpcUrl = cfg.baseUrl.let { if (it.endsWith("/rpc")) it else "$it/transmission/rpc" }

    private suspend fun rpc(method: String, args: JSONObject = JSONObject()): JSONObject = withContext(Dispatchers.IO) {
        val body = JSONObject().put("method", method).put("arguments", args).toString()
        var attempt = 0
        var result: JSONObject? = null
        while (attempt < 2 && result == null) {
            attempt++
            val b = Request.Builder().url(rpcUrl).header("X-Transmission-Session-Id", sid).post(body.jsonBody())
            if (cfg.username.isNotEmpty()) b.header("Authorization", Credentials.basic(cfg.username, cfg.password))
            client.newCall(b.build()).execute().use { r ->
                if (r.code == 409) {
                    sid = r.header("X-Transmission-Session-Id") ?: ""
                } else {
                    val s = r.body?.string().orEmpty()
                    if (!r.isSuccessful) throw ApiException(r.code, s)
                    val o = JSONObject(s)
                    if (o.str("result") != "success") throw Exception("Transmission: " + o.str("result"))
                    result = o.obj("arguments") ?: JSONObject()
                }
            }
        }
        result ?: throw Exception("Transmission session handshake failed")
    }

    private fun ids(ids: List<String>) = JSONArray().apply { ids.forEach { put(it.toIntOrNull() ?: it) } }

    override suspend fun version() = "Transmission " + rpc("session-get").str("version")

    override suspend fun snapshot(): DlSnapshot {
        val fields = JSONArray(listOf("id", "name", "percentDone", "status", "rateDownload", "rateUpload", "totalSize", "eta", "uploadRatio", "error", "labels", "addedDate", "leftUntilDone"))
        val list = rpc("torrent-get", JSONObject().put("fields", fields)).arr("torrents").objects().map { o ->
            val st = o.int("status")
            DlItem(
                id = o.int("id").toString(), name = o.str("name"), progress = o.dbl("percentDone").toFloat(),
                state = when (st) { 0 -> if (o.dbl("percentDone") >= 1.0) "Completed" else "Paused"; 1 -> "Queued check"; 2 -> "Checking"; 3 -> "Queued"; 4 -> "Downloading"; 5 -> "Queued (seed)"; 6 -> "Seeding"; else -> "?" },
                paused = st == 0, done = o.dbl("percentDone") >= 1.0,
                dlSpeed = o.long("rateDownload"), upSpeed = o.long("rateUpload"), size = o.long("totalSize"),
                eta = o.long("eta"), category = o.arr("labels").optString(0, ""), ratio = o.dbl("uploadRatio"),
                error = o.int("error") != 0, addedOn = o.long("addedDate"),
            )
        }
        val s = rpc("session-stats")
        return DlSnapshot(list, s.long("downloadSpeed"), s.long("uploadSpeed"))
    }

    override suspend fun pause(ids: List<String>) { rpc("torrent-stop", JSONObject().put("ids", ids(ids))) }
    override suspend fun resume(ids: List<String>) { rpc("torrent-start", JSONObject().put("ids", ids(ids))) }
    override suspend fun pauseAll() { rpc("torrent-stop") }
    override suspend fun resumeAll() { rpc("torrent-start") }
    override suspend fun delete(ids: List<String>, withData: Boolean) {
        rpc("torrent-remove", JSONObject().put("ids", ids(ids)).put("delete-local-data", withData))
    }
    override suspend fun addUrl(url: String) { rpc("torrent-add", JSONObject().put("filename", url)) }
}

// ======================= Deluge (Web UI JSON-RPC) =======================
class DelugeClient(override val cfg: ServiceConfig) : DownloadClient {
    private val jar = MemoryCookieJar()
    private val client = Http.client(cfg.trustAllCerts).newBuilder().cookieJar(jar).build()
    private var reqId = 1
    private var loggedIn = false
    private val url = cfg.baseUrl.let { if (it.endsWith("/json")) it else "$it/json" }

    private suspend fun raw(method: String, params: JSONArray): JSONObject {
        val body = JSONObject().put("method", method).put("params", params).put("id", reqId++).toString()
        val s = Http.exec(client, Request.Builder().url(url).post(body.jsonBody()).build())
        return JSONObject(s)
    }

    private suspend fun login() {
        val r = raw("auth.login", JSONArray().put(cfg.password))
        if (!r.bool("result")) throw Exception("Deluge login failed – check password")
        loggedIn = true
        val connected = raw("web.connected", JSONArray()).bool("result")
        if (!connected) {
            val hosts = raw("web.get_hosts", JSONArray()).optJSONArray("result")
            val first = hosts?.optJSONArray(0)?.optString(0)
            if (first != null) raw("web.connect", JSONArray().put(first))
        }
    }

    private suspend fun call(method: String, params: JSONArray): Any? {
        if (!loggedIn) login()
        var r = raw(method, params)
        val err = r.obj("error")
        if (err != null && err.int("code") == 1) {
            login(); r = raw(method, params)
        }
        r.obj("error")?.let { throw Exception("Deluge: " + it.str("message")) }
        return r.opt("result")
    }

    private suspend fun tryBoth(a: String, aParams: JSONArray, b: String, bParams: JSONArray) {
        try { call(a, aParams) } catch (e: Exception) { call(b, bParams) }
    }

    override suspend fun version() = "Deluge " + (call("daemon.info", JSONArray())?.toString() ?: "")

    override suspend fun snapshot(): DlSnapshot {
        val fields = JSONArray(listOf("name", "progress", "state", "download_payload_rate", "upload_payload_rate", "total_wanted", "eta", "ratio", "label", "time_added", "message"))
        val res = call("web.update_ui", JSONArray().put(fields).put(JSONObject())) as? JSONObject ?: JSONObject()
        val torrents = res.obj("torrents") ?: JSONObject()
        val list = torrents.keys().asSequence().mapNotNull { hash ->
            val o = torrents.optJSONObject(hash) ?: return@mapNotNull null
            val st = o.str("state")
            DlItem(
                id = hash, name = o.str("name"), progress = (o.dbl("progress") / 100.0).toFloat(), state = st,
                paused = st == "Paused", done = o.dbl("progress") >= 100.0,
                dlSpeed = o.long("download_payload_rate"), upSpeed = o.long("upload_payload_rate"),
                size = o.long("total_wanted"), eta = o.long("eta").let { if (it <= 0) -1 else it },
                category = o.str("label"), ratio = o.dbl("ratio"), error = st == "Error", addedOn = o.long("time_added"),
            )
        }.toList()
        val stats = res.obj("stats")
        return DlSnapshot(list, stats?.long("download_rate") ?: 0, stats?.long("upload_rate") ?: 0, freeSpace = stats?.long("free_space", -1) ?: -1)
    }

    private fun idsArr(ids: List<String>) = JSONArray(ids)

    override suspend fun pause(ids: List<String>) =
        tryBoth("core.pause_torrents", JSONArray().put(idsArr(ids)), "core.pause_torrent", JSONArray().put(idsArr(ids)))
    override suspend fun resume(ids: List<String>) =
        tryBoth("core.resume_torrents", JSONArray().put(idsArr(ids)), "core.resume_torrent", JSONArray().put(idsArr(ids)))
    override suspend fun pauseAll() { call("core.pause_session", JSONArray()) }
    override suspend fun resumeAll() { call("core.resume_session", JSONArray()) }
    override suspend fun delete(ids: List<String>, withData: Boolean) {
        try {
            call("core.remove_torrents", JSONArray().put(idsArr(ids)).put(withData))
        } catch (e: Exception) {
            ids.forEach { call("core.remove_torrent", JSONArray().put(it).put(withData)) }
        }
    }
    override suspend fun addUrl(url: String) {
        if (url.startsWith("magnet:")) call("core.add_torrent_magnet", JSONArray().put(url).put(JSONObject()))
        else call("core.add_torrent_url", JSONArray().put(url).put(JSONObject()))
    }
}

// ======================= ruTorrent (httprpc plugin) =======================
class RutorrentClient(override val cfg: ServiceConfig) : DownloadClient {
    private val client = Http.client(cfg.trustAllCerts)

    private suspend fun post(path: String, form: List<Pair<String, String>>): String {
        val fb = FormBody.Builder()
        form.forEach { (k, v) -> fb.add(k, v) }
        val b = Request.Builder().url("${cfg.baseUrl}/$path").post(fb.build())
        if (cfg.username.isNotEmpty()) b.header("Authorization", Credentials.basic(cfg.username, cfg.password))
        return Http.exec(client, b.build())
    }

    private suspend fun action(mode: String, ids: List<String> = emptyList()): String =
        post("plugins/httprpc/action.php", listOf("mode" to mode) + ids.map { "hash" to it })

    override suspend fun version(): String {
        snapshot(); return "ruTorrent (httprpc OK)"
    }

    override suspend fun snapshot(): DlSnapshot {
        val o = JSONObject(action("list"))
        val t = o.optJSONObject("t") ?: JSONObject()
        var dl = 0L
        var ul = 0L
        val list = t.keys().asSequence().mapNotNull { hash ->
            val a = t.optJSONArray(hash) ?: return@mapNotNull null
            fun s(i: Int) = a.optString(i, "")
            fun l(i: Int) = s(i).toLongOrNull() ?: 0L
            val size = l(5)
            val done = l(8)
            val upRate = l(11)
            val downRate = l(12)
            dl += downRate; ul += upRate
            val started = s(3) == "1"
            val active = s(28) == "1"
            val complete = size > 0 && l(19) == 0L
            val msg = s(29)
            DlItem(
                id = hash, name = s(4), progress = if (size > 0) done.toFloat() / size else 0f,
                state = when {
                    s(1) == "1" -> "Checking"
                    !started -> if (complete) "Completed" else "Paused"
                    !active -> "Paused"
                    complete -> "Seeding"
                    else -> "Downloading"
                },
                paused = !started || !active, done = complete, dlSpeed = downRate, upSpeed = upRate, size = size,
                eta = etaOf(l(19), downRate), category = try { URLDecoder.decode(s(14), "UTF-8") } catch (e: Exception) { s(14) },
                ratio = l(10) / 1000.0, error = msg.isNotEmpty() && !msg.contains("Tracker: [Success", true), addedOn = l(26),
            )
        }.toList()
        return DlSnapshot(list, dl, ul)
    }

    override suspend fun pause(ids: List<String>) { action("stop", ids) }
    override suspend fun resume(ids: List<String>) { action("start", ids) }
    override suspend fun pauseAll() { pause(snapshot().items.map { it.id }) }
    override suspend fun resumeAll() { resume(snapshot().items.map { it.id }) }
    override suspend fun delete(ids: List<String>, withData: Boolean) { action(if (withData) "removewithdata" else "remove", ids) }
    override suspend fun addUrl(url: String) { post("php/addtorrent.php", listOf("url" to url)) }
}

// ======================= SABnzbd =======================
class SabClient(override val cfg: ServiceConfig) : DownloadClient {
    private val client = Http.client(cfg.trustAllCerts)
    override val hasHistory = true
    override val isUsenet = true

    private suspend fun api(mode: String, extra: Map<String, Any?> = emptyMap()): JSONObject {
        val q = mutableMapOf<String, Any?>("mode" to mode, "output" to "json", "apikey" to cfg.apiKey)
        q.putAll(extra)
        val s = Http.exec(client, Request.Builder().url(buildUrl("${cfg.baseUrl}/api", q)).build())
        val o = JSONObject(s)
        if (o.has("error") && !o.isNull("error") && o.str("error").isNotEmpty()) throw Exception("SABnzbd: " + o.str("error"))
        return o
    }

    override suspend fun version() = "SABnzbd " + api("version").str("version")

    private fun parseEta(t: String): Long {
        val p = t.split(":").mapNotNull { it.toLongOrNull() }
        return when (p.size) { 4 -> p[0] * 86400 + p[1] * 3600 + p[2] * 60 + p[3]; 3 -> p[0] * 3600 + p[1] * 60 + p[2]; 2 -> p[0] * 60 + p[1]; else -> -1 }
    }

    override suspend fun snapshot(): DlSnapshot {
        val q = api("queue", mapOf("limit" to 200)).obj("queue") ?: JSONObject()
        val list = q.arr("slots").objects().map { o ->
            val mb = o.dbl("mb"); val left = o.dbl("mbleft")
            val st = o.str("status")
            DlItem(
                id = o.str("nzo_id"), name = o.str("filename"), progress = (o.str("percentage").toFloatOrNull() ?: 0f) / 100f,
                state = st, paused = st == "Paused", done = false, dlSpeed = 0, upSpeed = 0,
                size = (mb * 1048576).toLong(), eta = parseEta(o.str("timeleft")), category = o.str("cat"),
            )
        }
        val kbps = q.str("kbpersec").toDoubleOrNull() ?: 0.0
        val free = ((q.str("diskspace1").toDoubleOrNull() ?: -1.0) * 1073741824).toLong()
        return DlSnapshot(list, (kbps * 1024).toLong(), 0, q.bool("paused"), free)
    }

    override suspend fun pause(ids: List<String>) { api("queue", mapOf("name" to "pause", "value" to ids.joinToString(","))) }
    override suspend fun resume(ids: List<String>) { api("queue", mapOf("name" to "resume", "value" to ids.joinToString(","))) }
    override suspend fun pauseAll() { api("pause") }
    override suspend fun resumeAll() { api("resume") }
    override suspend fun delete(ids: List<String>, withData: Boolean) {
        api("queue", mapOf("name" to "delete", "value" to ids.joinToString(","), "del_files" to if (withData) 1 else 0))
    }
    override suspend fun addUrl(url: String) { api("addurl", mapOf("name" to url)) }

    override suspend fun history(): List<DlHistory> =
        (api("history", mapOf("limit" to 100)).obj("history")?.arr("slots") ?: JSONArray()).objects().map { o ->
            DlHistory(o.str("name"), o.str("status"), o.long("bytes"), o.long("completed"), o.str("status") == "Failed")
        }

    suspend fun setSpeedLimit(percent: Int) { api("config", mapOf("name" to "speedlimit", "value" to percent)) }
}

// ======================= NZBGet =======================
class NzbgetClient(override val cfg: ServiceConfig) : DownloadClient {
    private val client = Http.client(cfg.trustAllCerts)
    override val hasHistory = true
    override val isUsenet = true
    private var reqId = 1

    private suspend fun rpc(method: String, params: JSONArray = JSONArray()): Any? {
        val body = JSONObject().put("method", method).put("params", params).put("id", reqId++).toString()
        val b = Request.Builder().url("${cfg.baseUrl}/jsonrpc").post(body.jsonBody())
        if (cfg.username.isNotEmpty()) b.header("Authorization", Credentials.basic(cfg.username, cfg.password))
        val o = JSONObject(Http.exec(client, b.build()))
        o.obj("error")?.let { throw Exception("NZBGet: " + it.str("message")) }
        return o.opt("result")
    }

    override suspend fun version() = "NZBGet " + rpc("version").toString()

    override suspend fun snapshot(): DlSnapshot {
        val groups = (rpc("listgroups", JSONArray().put(0)) as? JSONArray ?: JSONArray()).objects()
        val st = rpc("status") as? JSONObject ?: JSONObject()
        val rate = st.long("DownloadRate")
        val list = groups.map { o ->
            val total = o.long("FileSizeMB") * 1048576
            val left = o.long("RemainingSizeMB") * 1048576
            val s = o.str("Status")
            DlItem(
                id = o.int("NZBID").toString(), name = o.str("NZBName"),
                progress = if (total > 0) (total - left).toFloat() / total else 0f,
                state = s.lowercase().replaceFirstChar { it.uppercase() }, paused = s.startsWith("PAUSED"), done = false,
                dlSpeed = 0, upSpeed = 0, size = total, eta = etaOf(left, rate), category = o.str("Category"),
            )
        }
        return DlSnapshot(list, rate, 0, st.bool("DownloadPaused"), st.long("FreeDiskSpaceMB", -1).let { if (it < 0) -1 else it * 1048576 })
    }

    private suspend fun edit(cmd: String, ids: List<String>) {
        rpc("editqueue", JSONArray().put(cmd).put("").put(JSONArray().apply { ids.forEach { put(it.toInt()) } }))
    }

    override suspend fun pause(ids: List<String>) = edit("GroupPause", ids)
    override suspend fun resume(ids: List<String>) = edit("GroupResume", ids)
    override suspend fun pauseAll() { rpc("pausedownload") }
    override suspend fun resumeAll() { rpc("resumedownload") }
    override suspend fun delete(ids: List<String>, withData: Boolean) = edit(if (withData) "GroupFinalDelete" else "GroupDelete", ids)
    override suspend fun addUrl(url: String) {
        rpc("append", JSONArray().put("").put(url).put("").put(0).put(false).put(false).put("").put(0).put("SCORE").put(JSONArray()))
    }

    override suspend fun history(): List<DlHistory> =
        (rpc("history", JSONArray().put(false)) as? JSONArray ?: JSONArray()).objects().take(100).map { o ->
            val s = o.str("Status")
            DlHistory(o.str("Name"), s, o.long("FileSizeMB") * 1048576, o.long("HistoryTime"), s.startsWith("FAILURE"))
        }
}
