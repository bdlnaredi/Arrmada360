package com.arrmada360.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ServiceConfig(
    val id: String = UUID.randomUUID().toString(),
    val type: ServiceType,
    val name: String,
    val url: String,
    val apiKey: String = "",
    val username: String = "",
    val password: String = "",
    val trustAllCerts: Boolean = false,
) {
    val baseUrl: String
        get() {
            var u = url.trim()
            if (!u.startsWith("http://") && !u.startsWith("https://")) u = "http://$u"
            return u.trimEnd('/')
        }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("type", type.name).put("name", name).put("url", url)
        .put("apiKey", apiKey).put("username", username).put("password", password)
        .put("trustAllCerts", trustAllCerts)

    companion object {
        fun fromJson(o: JSONObject): ServiceConfig? = try {
            ServiceConfig(
                id = o.getString("id"),
                type = ServiceType.valueOf(o.getString("type")),
                name = o.optString("name"),
                url = o.optString("url"),
                apiKey = o.optString("apiKey"),
                username = o.optString("username"),
                password = o.optString("password"),
                trustAllCerts = o.optBoolean("trustAllCerts"),
            )
        } catch (e: Exception) {
            null
        }
    }
}

object ConfigStore {
    private lateinit var prefs: SharedPreferences
    val services = mutableStateListOf<ServiceConfig>()
    var onboarded by mutableStateOf(false)
        private set
    var amoled by mutableStateOf(false)
        private set
    var gridColumns by mutableStateOf(3)
        private set

    fun init(ctx: Context) {
        if (::prefs.isInitialized) return
        prefs = ctx.getSharedPreferences("arrmada360", Context.MODE_PRIVATE)
        onboarded = prefs.getBoolean("onboarded", false)
        amoled = prefs.getBoolean("amoled", false)
        gridColumns = prefs.getInt("gridColumns", 3)
        val arr = try { JSONArray(prefs.getString("services", "[]")) } catch (e: Exception) { JSONArray() }
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { ServiceConfig.fromJson(it) }?.let { services.add(it) }
        }
    }

    private fun save() {
        val arr = JSONArray()
        services.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("services", arr.toString()).apply()
    }

    fun get(id: String): ServiceConfig? = services.firstOrNull { it.id == id }

    fun upsert(c: ServiceConfig) {
        val i = services.indexOfFirst { it.id == c.id }
        if (i >= 0) services[i] = c else services.add(c)
        save()
    }

    fun remove(id: String) {
        services.removeAll { it.id == id }
        save()
    }

    fun move(id: String, delta: Int) {
        val i = services.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j < 0 || j >= services.size) return
        val item = services.removeAt(i)
        services.add(j, item)
        save()
    }

    fun ofType(vararg types: ServiceType) = services.filter { it.type in types }
    fun downloaders() = services.filter { it.type.isDownloader }

    fun finishOnboarding() {
        onboarded = true
        prefs.edit().putBoolean("onboarded", true).apply()
    }

    fun resetOnboarding() {
        onboarded = false
        prefs.edit().putBoolean("onboarded", false).apply()
    }

    fun updateAmoled(v: Boolean) {
        amoled = v
        prefs.edit().putBoolean("amoled", v).apply()
    }

    fun updateGridColumns(v: Int) {
        gridColumns = v.coerceIn(2, 5)
        prefs.edit().putInt("gridColumns", gridColumns).apply()
    }

    /** Export everything as JSON (for backup / moving to a new phone). */
    fun exportJson(): String {
        val arr = JSONArray()
        services.forEach { arr.put(it.toJson()) }
        return JSONObject().put("arrmada360", 1).put("services", arr).toString(2)
    }

    fun importJson(text: String): Int {
        val o = JSONObject(text)
        val arr = o.getJSONArray("services")
        var n = 0
        for (i in 0 until arr.length()) {
            val c = arr.optJSONObject(i)?.let { ServiceConfig.fromJson(it) } ?: continue
            val idx = services.indexOfFirst { it.id == c.id }
            if (idx >= 0) services[idx] = c else services.add(c)
            n++
        }
        save()
        return n
    }
}
