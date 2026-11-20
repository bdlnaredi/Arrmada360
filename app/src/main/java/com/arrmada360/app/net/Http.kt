package com.arrmada360.app.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class ApiException(val code: Int, body: String) :
    Exception(if (code == 401 || code == 403) "HTTP $code – check your API key / login" else "HTTP $code ${body.take(200)}".trim())

object Http {
    private val base: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val trusting: OkHttpClient by lazy {
        val tm = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
        val ctx = SSLContext.getInstance("TLS")
        ctx.init(null, arrayOf<TrustManager>(tm), SecureRandom())
        base.newBuilder()
            .sslSocketFactory(ctx.socketFactory, tm)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    fun client(trustAll: Boolean): OkHttpClient = if (trustAll) trusting else base

    /** Client for images: accepts self-signed certs so posters from local servers always load. */
    val images: OkHttpClient get() = trusting

    suspend fun exec(client: OkHttpClient, req: Request): String = withContext(Dispatchers.IO) {
        val key = OfflineCache.keyFor(req)
        try {
            client.newCall(req).execute().use { r ->
                val body = r.body?.string().orEmpty()
                if (!r.isSuccessful) throw ApiException(r.code, body)
                if (key != null) { OfflineCache.put(key, body); OfflineCache.online() }
                body
            }
        } catch (e: java.io.IOException) {
            // Server unreachable: fall back to the last copy we saw, if any.
            val cached = key?.let { OfflineCache.get(it) } ?: throw e
            OfflineCache.served(cached.second)
            cached.first
        }
    }
}

val JSON_TYPE = "application/json; charset=utf-8".toMediaType()
fun String.jsonBody(): RequestBody = toRequestBody(JSON_TYPE)

fun buildUrl(base: String, query: Map<String, Any?> = emptyMap()): HttpUrl {
    val b = base.toHttpUrl().newBuilder()
    query.forEach { (k, v) -> if (v != null) b.addQueryParameter(k, v.toString()) }
    return b.build()
}

class MemoryCookieJar : CookieJar {
    private val store = mutableListOf<Cookie>()
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        synchronized(store) {
            cookies.forEach { c ->
                store.removeAll { it.name == c.name }
                store.add(c)
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> = synchronized(store) { store.toList() }
    fun clear() = synchronized(store) { store.clear() }
}

// ---------- JSON helpers (Android's org.json returns "null" strings otherwise) ----------
fun JSONObject.str(k: String, def: String = ""): String = if (has(k) && !isNull(k)) optString(k, def) else def
fun JSONObject.int(k: String, def: Int = 0): Int = if (has(k) && !isNull(k)) optInt(k, def) else def
fun JSONObject.long(k: String, def: Long = 0L): Long = if (has(k) && !isNull(k)) optLong(k, def) else def
fun JSONObject.dbl(k: String, def: Double = 0.0): Double = if (has(k) && !isNull(k)) optDouble(k, def) else def
fun JSONObject.bool(k: String, def: Boolean = false): Boolean = if (has(k) && !isNull(k)) optBoolean(k, def) else def
fun JSONObject.obj(k: String): JSONObject? = optJSONObject(k)
fun JSONObject.arr(k: String): JSONArray = optJSONArray(k) ?: JSONArray()
fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }
fun JSONArray.strings(): List<String> = (0 until length()).map { optString(it) }
fun jsonArrayOf(vararg v: Any?): JSONArray = JSONArray().apply { v.forEach { put(it) } }
fun JSONArray.ints(): List<Int> = (0 until length()).map { optInt(it) }
fun intsJson(ids: List<Int>): JSONArray = JSONArray().apply { ids.forEach { put(it) } }

/** Turns low-level network errors into plain-English hints. */
fun friendlyError(e: Throwable): String = when (e) {
    is ApiException -> when (e.code) {
        401, 403 -> "Wrong API key or login (HTTP ${e.code})"
        404 -> "Not found (HTTP 404) – check the address, port and URL base (e.g. /sonarr)"
        407 -> "A proxy is asking for a login (HTTP 407)"
        502, 503, 504 -> "Server or tunnel error (HTTP ${e.code}) – is the app running?"
        else -> e.message ?: "HTTP ${e.code}"
    }
    is java.net.UnknownHostException -> "Can't find that address – check the IP or domain"
    is java.net.ConnectException -> "Connection refused – check the port and that the app is running"
    is java.net.NoRouteToHostException -> "No route to host – is your phone on the same network or VPN?"
    is java.net.SocketTimeoutException -> "Timed out – is your phone on the same network or Tailscale?"
    is javax.net.ssl.SSLException -> "HTTPS problem – try http:// or tick 'Allow self-signed HTTPS certificates'"
    is org.json.JSONException -> "Unexpected reply (not JSON) – is this the right app, port and URL base?"
    is IllegalArgumentException -> "Invalid address – use something like http://192.168.1.10:8989"
    else -> e.message ?: e.javaClass.simpleName
}


/** Keeps the last good answer of every read request so the app still shows your library and calendar when a server is unreachable. */
object OfflineCache {
    var dir: java.io.File? = null
    /** When the screen is showing saved data, the time that data was saved. */
    private val since = androidx.compose.runtime.mutableStateOf<Long?>(null)
    var offlineSince: Long?
        get() = since.value
        set(v) { since.value = v }
    private const val MAX_BYTES = 60L * 1024 * 1024

    val enabled: Boolean get() = !com.arrmada360.app.data.ConfigStore.flag("offlineCacheOff")

    fun keyFor(req: Request): String? {
        if (!enabled || dir == null) return null
        val isRead = when (req.method) {
            "GET" -> true
            "POST" -> {
                val b = req.body ?: return null
                if ((b.contentLength()) > 20_000) return null
                val buf = okio.Buffer(); b.writeTo(buf)
                val text = buf.readUtf8()
                req.url.encodedPath.endsWith("/graphql") && text.isNotEmpty() && !text.contains("mutation")
            }
            else -> false
        }
        if (!isRead) return null
        val extra = if (req.method == "POST") { val buf = okio.Buffer(); req.body?.writeTo(buf); buf.readUtf8() } else ""
        val raw = req.url.toString() + "|" + extra
        val md = java.security.MessageDigest.getInstance("SHA-1").digest(raw.toByteArray())
        return md.joinToString("") { "%02x".format(it) }
    }

    fun put(key: String, body: String) {
        if (body.length > 3_000_000) return
        val d = dir ?: return
        try {
            d.mkdirs()
            java.io.File(d, key).writeText(body)
            trim(d)
        } catch (e: Exception) { }
    }

    fun get(key: String): Pair<String, Long>? {
        val f = java.io.File(dir ?: return null, key)
        return if (f.exists()) try { f.readText() to f.lastModified() } catch (e: Exception) { null } else null
    }

    fun served(savedAt: Long) { offlineSince = savedAt }
    fun online() { if (offlineSince != null) offlineSince = null }

    fun clear() { dir?.listFiles()?.forEach { it.delete() }; offlineSince = null }
    fun sizeBytes(): Long = dir?.listFiles()?.sumOf { it.length() } ?: 0

    private var writes = 0
    private fun trim(d: java.io.File) {
        if (++writes % 25 != 0) return
        val files = d.listFiles()?.sortedBy { it.lastModified() } ?: return
        var total = files.sumOf { it.length() }
        for (f in files) { if (total <= MAX_BYTES) break; total -= f.length(); f.delete() }
    }
}
