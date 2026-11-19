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
        client.newCall(req).execute().use { r ->
            val body = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw ApiException(r.code, body)
            body
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
