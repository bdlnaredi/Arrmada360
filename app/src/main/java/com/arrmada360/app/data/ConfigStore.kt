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
    /** Optional address used away from home (Tailscale, domain, reverse proxy…). */
    val remoteUrl: String = "",
) {
    val localBaseUrl: String get() = normalize(url)
    val remoteBaseUrl: String? get() = remoteUrl.trim().takeIf { it.isNotEmpty() }?.let { normalize(it) }
    val hasRemote: Boolean get() = remoteBaseUrl != null

    /** The address to use right now – local at home, remote elsewhere (see [com.arrmada360.app.net.NetSwitch]). */
    val baseUrl: String
        get() = if (hasRemote && !com.arrmada360.app.net.NetSwitch.useLocal(this)) remoteBaseUrl!! else localBaseUrl

    private fun normalize(raw: String): String {
        var u = raw.trim()
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "http://$u"
        return u.trimEnd('/')
    }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("type", type.name).put("name", name).put("url", url)
        .put("apiKey", apiKey).put("username", username).put("password", password)
        .put("trustAllCerts", trustAllCerts).put("remoteUrl", remoteUrl)

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
                remoteUrl = o.optString("remoteUrl"),
            )
        } catch (e: Exception) {
            null
        }
    }
}

/** All on/off preferences with their defaults. */
object Pref {
    const val VOLUME_NAV = "volumeNav"
    const val DRAWER_GESTURE = "drawerGesture"
    const val SWIPE_DOWNLOADS = "swipeDownloads"
    const val BACK_TO_DASHBOARD = "backToDashboard"
    const val FRIENDLY_NAMES = "friendlyNames"
    const val SPEED_BITS = "speedBits"
    const val AUTO_CLEAR_SEARCH = "autoClearSearch"
    const val WHATS_NEW = "whatsNew"
    const val AUTH_SETTINGS = "authSettings"
    const val CRASH_LOGS = "crashLogs"
    const val NOTIFY = "notify"
    const val NOTIFY_RADARR = "notify_RADARR"
    const val NOTIFY_SONARR = "notify_SONARR"
    const val NOTIFY_LIDARR = "notify_LIDARR"
    const val NOTIFY_READARR = "notify_READARR"
    const val NOTIFY_SEERR = "notify_SEERR"
    const val NOTIFY_DOWNLOADS = "notify_DOWNLOADS"
    const val AUTO_UPDATE_CHECK = "autoUpdateCheck"

    val defaults: Map<String, Boolean> = mapOf(
        VOLUME_NAV to false, DRAWER_GESTURE to true, SWIPE_DOWNLOADS to true, BACK_TO_DASHBOARD to true,
        FRIENDLY_NAMES to false, SPEED_BITS to false, AUTO_CLEAR_SEARCH to true, WHATS_NEW to true,
        AUTH_SETTINGS to false, CRASH_LOGS to true,
        NOTIFY to false, NOTIFY_RADARR to true, NOTIFY_SONARR to true, NOTIFY_LIDARR to true,
        NOTIFY_READARR to true, NOTIFY_SEERR to true, NOTIFY_DOWNLOADS to false, AUTO_UPDATE_CHECK to true,
    )
}

object ConfigStore {
    private lateinit var prefs: SharedPreferences
    private val flags = androidx.compose.runtime.mutableStateMapOf<String, Boolean>()
    var imdbOpenWith by mutableStateOf("browser")
        private set
    /** Settings lock: unlocked for the rest of this app session once the user authenticates. */
    var settingsUnlocked by mutableStateOf(false)
    val services = mutableStateListOf<ServiceConfig>()
    var onboarded by mutableStateOf(false)
        private set
    var amoled by mutableStateOf(false)
    /** "dark", "light" or "system" */
    var themeMode by mutableStateOf("dark")
    var materialYou by mutableStateOf(false)
    fun updateTheme(mode: String) { themeMode = mode; prefs.edit().putString("themeMode", mode).apply() }
    fun updateMaterialYou(v: Boolean) { materialYou = v; prefs.edit().putBoolean("materialYou", v).apply() }
        private set
    var gridColumns by mutableStateOf(3)
        private set
    var tmdbKey by mutableStateOf("")
        private set
    var traktClientId by mutableStateOf("")
        private set
    /** AUTO, LOCAL or REMOTE. */
    var connMode by mutableStateOf("AUTO")
        private set
    var homeSsids by mutableStateOf(setOf<String>())
        private set

    fun init(ctx: Context) {
        if (::prefs.isInitialized) return
        prefs = ctx.getSharedPreferences("arrmada360", Context.MODE_PRIVATE)
        com.arrmada360.app.net.OfflineCache.dir = java.io.File(ctx.cacheDir, "offline")
        onboarded = prefs.getBoolean("onboarded", false)
        amoled = prefs.getBoolean("amoled", false)
        themeMode = prefs.getString("themeMode", "dark") ?: "dark"
        materialYou = prefs.getBoolean("materialYou", false)
        gridColumns = prefs.getInt("gridColumns", 3)
        tmdbKey = prefs.getString("tmdbKey", "") ?: ""
        Pref.defaults.forEach { (k, d) -> flags[k] = prefs.getBoolean(k, d) }
        imdbOpenWith = prefs.getString("imdbOpenWith", "browser") ?: "browser"
        connMode = prefs.getString("connMode", "AUTO") ?: "AUTO"
        homeSsids = prefs.getStringSet("homeSsids", emptySet())?.toSet() ?: emptySet()
        traktClientId = prefs.getString("traktClientId", "") ?: ""
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

    fun flag(key: String): Boolean = flags[key] ?: Pref.defaults[key] ?: false

    fun setFlag(key: String, v: Boolean) {
        flags[key] = v
        prefs.edit().putBoolean(key, v).apply()
    }

    fun updateConnMode(v: String) {
        connMode = v
        prefs.edit().putString("connMode", v).apply()
        com.arrmada360.app.net.NetSwitch.refresh()
    }

    fun updateHomeSsids(v: Set<String>) {
        homeSsids = v
        prefs.edit().putStringSet("homeSsids", v).apply()
        com.arrmada360.app.net.NetSwitch.refresh()
    }

    fun updateImdbOpenWith(v: String) {
        imdbOpenWith = v
        prefs.edit().putString("imdbOpenWith", v).apply()
    }

    fun getString(key: String, def: String = ""): String = prefs.getString(key, def) ?: def
    fun putString(key: String, v: String) = prefs.edit().putString(key, v).apply()
    fun getStringSet(key: String): Set<String> = prefs.getStringSet(key, emptySet()) ?: emptySet()
    fun putStringSet(key: String, v: Set<String>) = prefs.edit().putStringSet(key, v).apply()
    fun getInt(key: String, def: Int = 0): Int = prefs.getInt(key, def)
    fun putInt(key: String, v: Int) = prefs.edit().putInt(key, v).apply()

    /** Full backup: every service plus every preference. */
    fun exportFullJson(): String {
        val arr = JSONArray()
        services.forEach { arr.put(it.toJson()) }
        val p = JSONObject()
        Pref.defaults.keys.forEach { p.put(it, flag(it)) }
        return JSONObject()
            .put("app", "Arrmada360").put("backupVersion", 2).put("services", arr).put("prefs", p)
            .put("amoled", amoled).put("gridColumns", gridColumns)
            .put("tmdbKey", tmdbKey).put("traktClientId", traktClientId).put("imdbOpenWith", imdbOpenWith)
            .put("connMode", connMode).put("homeSsids", JSONArray(homeSsids.toList()))
            .toString(2)
    }

    /** Replaces all current settings with the backup. Returns the number of services restored. */
    fun restoreFullJson(text: String): Int {
        val o = JSONObject(text)
        val arr = o.getJSONArray("services")
        val list = (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.let { j -> ServiceConfig.fromJson(j) } }
        services.clear()
        services.addAll(list)
        save()
        o.optJSONObject("prefs")?.let { p -> Pref.defaults.keys.forEach { k -> if (p.has(k)) setFlag(k, p.optBoolean(k)) } }
        if (o.has("amoled")) updateAmoled(o.optBoolean("amoled"))
        if (o.has("gridColumns")) updateGridColumns(o.optInt("gridColumns", 3))
        if (o.has("tmdbKey") || o.has("traktClientId")) updateDiscoverKeys(o.optString("tmdbKey"), o.optString("traktClientId"))
        if (o.has("imdbOpenWith")) updateImdbOpenWith(o.optString("imdbOpenWith", "browser"))
        if (o.has("connMode")) updateConnMode(o.optString("connMode", "AUTO"))
        o.optJSONArray("homeSsids")?.let { a -> updateHomeSsids((0 until a.length()).map { a.optString(it) }.toSet()) }
        if (list.isNotEmpty()) finishOnboarding()
        return list.size
    }

    fun updateDiscoverKeys(tmdb: String, trakt: String) {
        tmdbKey = tmdb.trim()
        traktClientId = trakt.trim()
        prefs.edit().putString("tmdbKey", tmdbKey).putString("traktClientId", traktClientId).apply()
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

/** "Movies" instead of "Radarr" etc. when friendly names are on (and there's only one of that app). */
fun ServiceConfig.display(): String {
    if (!ConfigStore.flag(Pref.FRIENDLY_NAMES)) return name
    if (ConfigStore.services.count { it.type == type } > 1) return name
    return when (type) {
        ServiceType.RADARR -> "Movies"; ServiceType.SONARR -> "TV Shows"; ServiceType.LIDARR -> "Music"; ServiceType.READARR -> "Books"
        ServiceType.PROWLARR, ServiceType.JACKETT -> "Indexers"; ServiceType.SEERR -> "Requests"; ServiceType.TAUTULLI -> "Plex activity"
        ServiceType.UNRAID -> "Server"; ServiceType.NAMER -> "Renamer"; ServiceType.WHISPARR -> "Scenes"
        ServiceType.UPTIME_KUMA -> "Uptime"; ServiceType.STASH -> "Stash"; else -> "Downloads"
    }
}

/** Simple app-wide events (volume keys, deep links). */
object AppEvents {
    val cycleService = kotlinx.coroutines.flow.MutableSharedFlow<Int>(extraBufferCapacity = 8)
    val deepLink = kotlinx.coroutines.flow.MutableSharedFlow<android.net.Uri>(replay = 1, extraBufferCapacity = 4)
}
