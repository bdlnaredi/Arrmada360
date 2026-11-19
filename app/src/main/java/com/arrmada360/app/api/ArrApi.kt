package com.arrmada360.app.api

import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.data.ServiceType
import com.arrmada360.app.net.ApiException
import com.arrmada360.app.net.Http
import com.arrmada360.app.net.arr
import com.arrmada360.app.net.bool
import com.arrmada360.app.net.buildUrl
import com.arrmada360.app.net.dbl
import com.arrmada360.app.net.int
import com.arrmada360.app.net.intsJson
import com.arrmada360.app.net.jsonBody
import com.arrmada360.app.net.long
import com.arrmada360.app.net.obj
import com.arrmada360.app.net.objects
import com.arrmada360.app.net.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class MediaItem(
    val id: Int,
    val title: String,
    val year: Int,
    val overview: String,
    val poster: String?,
    val fanart: String?,
    val monitored: Boolean,
    val status: String,
    val sizeOnDisk: Long,
    val progressText: String,
    val complete: Boolean,
    val added: String,
    val network: String,
    val raw: JSONObject,
)

data class ChildItem(
    val id: Int,
    val groupKey: Int,
    val group: String,
    val title: String,
    val subtitle: String,
    val monitored: Boolean,
    val hasFile: Boolean,
    val date: String,
)

data class CalEntry(
    val id: Int,
    val parentId: Int,
    val title: String,
    val subtitle: String,
    val date: String,
    val hasFile: Boolean,
    val poster: String?,
)

data class QueueEntry(
    val id: Int,
    val title: String,
    val subtitle: String,
    val status: String,
    val size: Long,
    val sizeLeft: Long,
    val timeLeft: String,
    val client: String,
    val protocol: String,
    val warning: String,
    val downloadId: String = "",
)

data class HistoryEntry(val title: String, val event: String, val date: String, val quality: String)

data class ReleaseEntry(
    val guid: String,
    val indexerId: Int,
    val title: String,
    val indexer: String,
    val size: Long,
    val seeders: Int?,
    val leechers: Int?,
    val protocol: String,
    val quality: String,
    val ageText: String,
    val rejected: Boolean,
    val rejections: List<String>,
    val customScore: Int,
)

data class Profile(val id: Int, val name: String)
data class ImportItem(
    val path: String,
    val name: String,
    val size: Long,
    val parentId: Int,
    val parentTitle: String,
    val childIds: List<Int>,
    val childText: String,
    val quality: String,
    val rejections: List<String>,
    val raw: JSONObject,
)
data class HealthIssue(val source: String, val type: String, val message: String)
data class TaskInfo(val name: String, val taskName: String, val interval: Int, val last: String, val next: String)
data class RootFolder(val path: String, val free: Long)
data class DiskSpace(val path: String, val label: String, val free: Long, val total: Long)

/**
 * One client for Sonarr, Radarr, Lidarr and Readarr – they share the same REST design.
 */
class ArrApi(val cfg: ServiceConfig) {
    val type: ServiceType = cfg.type
    private val ver = if (type == ServiceType.SONARR || type == ServiceType.RADARR) "v3" else "v1"
    private val client = Http.client(cfg.trustAllCerts)

    val libPath = when (type) {
        ServiceType.SONARR -> "series"
        ServiceType.RADARR -> "movie"
        ServiceType.LIDARR -> "artist"
        else -> "author"
    }
    val itemWord = when (type) {
        ServiceType.SONARR -> "Series"; ServiceType.RADARR -> "Movie"; ServiceType.LIDARR -> "Artist"; else -> "Author"
    }
    val childWord: String? = when (type) {
        ServiceType.SONARR -> "Episodes"; ServiceType.LIDARR -> "Albums"; ServiceType.READARR -> "Books"; else -> null
    }
    private val includeParent = when (type) {
        ServiceType.SONARR -> "includeSeries"; ServiceType.RADARR -> "includeMovie"
        ServiceType.LIDARR -> "includeArtist"; else -> "includeAuthor"
    }
    private val parentKey = when (type) {
        ServiceType.SONARR -> "series"; ServiceType.RADARR -> "movie"; ServiceType.LIDARR -> "artist"; else -> "author"
    }

    private fun url(path: String, q: Map<String, Any?>) = buildUrl("${cfg.baseUrl}/api/$ver/$path", q)
    private fun rb(path: String, q: Map<String, Any?>) = Request.Builder().url(url(path, q)).header("X-Api-Key", cfg.apiKey)

    suspend fun get(path: String, q: Map<String, Any?> = emptyMap()) = Http.exec(client, rb(path, q).get().build())
    suspend fun post(path: String, body: String, q: Map<String, Any?> = emptyMap()) =
        Http.exec(client, rb(path, q).post(body.jsonBody()).build())
    suspend fun put(path: String, body: String, q: Map<String, Any?> = emptyMap()) =
        Http.exec(client, rb(path, q).put(body.jsonBody()).build())
    suspend fun delete(path: String, q: Map<String, Any?> = emptyMap()) = Http.exec(client, rb(path, q).delete().build())

    private suspend fun fetchList(path: String, q: Map<String, Any?> = emptyMap()): List<JSONObject> = withContext(Dispatchers.Default) {
        val s = get(path, q)
        val t = s.trim()
        if (t.startsWith("{")) JSONObject(t).arr("records").objects() else JSONArray(t).objects()
    }

    suspend fun systemStatus(): JSONObject = JSONObject(get("system/status"))
    suspend fun healthIssues(): List<HealthIssue> = fetchList("health").map { HealthIssue(it.str("source"), it.str("type"), it.str("message")) }
    suspend fun tasks(): List<TaskInfo> = fetchList("system/task").map {
        TaskInfo(it.str("name"), it.str("taskName"), it.int("interval"), it.str("lastExecution"), it.str("nextExecution"))
    }.sortedBy { it.name }
    suspend fun restart() { post("system/restart", "{}") }

    // ---------------- manual import ----------------
    suspend fun manualImportList(folder: String?, downloadId: String?): List<ImportItem> {
        val q = mutableMapOf<String, Any?>("filterExistingFiles" to true)
        if (!folder.isNullOrBlank()) q["folder"] = folder
        if (!downloadId.isNullOrBlank()) q["downloadId"] = downloadId
        return fetchList("manualimport", q).map { o ->
            val parent = o.obj("series") ?: o.obj("movie") ?: o.obj("artist") ?: o.obj("author")
            val children = when (type) {
                ServiceType.SONARR -> o.arr("episodes").objects()
                ServiceType.LIDARR -> o.arr("tracks").objects()
                else -> emptyList()
            }
            val childText = when (type) {
                ServiceType.SONARR -> children.joinToString(", ") { "S%02dE%02d".format(it.int("seasonNumber"), it.int("episodeNumber")) }
                ServiceType.LIDARR -> o.obj("album")?.str("title") ?: ""
                ServiceType.READARR -> o.obj("book")?.str("title") ?: ""
                else -> ""
            }
            ImportItem(
                path = o.str("path"), name = o.str("relativePath").ifEmpty { o.str("name") }, size = o.long("size"),
                parentId = parent?.int("id") ?: 0,
                parentTitle = parent?.str("title")?.ifEmpty { null } ?: parent?.str("artistName")?.ifEmpty { null } ?: parent?.str("authorName") ?: "",
                childIds = children.map { it.int("id") }, childText = childText,
                quality = o.obj("quality")?.obj("quality")?.str("name") ?: "",
                rejections = o.arr("rejections").objects().map { it.str("reason") },
                raw = o,
            )
        }
    }

    suspend fun manualImport(items: List<ImportItem>, mode: String) {
        val files = JSONArray()
        items.forEach { i ->
            val f = JSONObject().put("path", i.path)
            i.raw.opt("quality")?.let { f.put("quality", it) }
            i.raw.opt("languages")?.let { f.put("languages", it) }
            f.put("releaseGroup", i.raw.str("releaseGroup"))
            f.put("downloadId", i.raw.str("downloadId"))
            when (type) {
                ServiceType.SONARR -> f.put("seriesId", i.parentId).put("episodeIds", intsJson(i.childIds))
                ServiceType.RADARR -> f.put("movieId", i.parentId)
                ServiceType.LIDARR -> f.put("artistId", i.parentId).put("albumId", i.raw.obj("album")?.int("id") ?: 0).put("trackIds", intsJson(i.childIds))
                else -> f.put("authorId", i.parentId).put("bookId", i.raw.obj("book")?.int("id") ?: 0)
            }
            files.put(f)
        }
        command(JSONObject().put("name", "ManualImport").put("files", files).put("importMode", mode))
    }

    suspend fun status(): String {
        val o = JSONObject(get("system/status"))
        return "${o.str("appName", type.label)} ${o.str("version")}"
    }

    // ---------------- images ----------------
    private val origin: String by lazy {
        val u = cfg.baseUrl.toHttpUrl()
        "${u.scheme}://${u.host}:${u.port}"
    }

    fun image(o: JSONObject?, vararg types: String): String? {
        if (o == null) return null
        val imgs = o.arr("images").objects()
        for (t in types) {
            val img = imgs.firstOrNull { it.str("coverType").equals(t, true) } ?: continue
            val remote = img.str("remoteUrl")
            if (remote.isNotEmpty()) return remote
            val local = img.str("url")
            if (local.isNotEmpty()) {
                val full = if (local.startsWith("http")) local else origin + local
                return full + (if (full.contains("?")) "&" else "?") + "apikey=" + cfg.apiKey
            }
        }
        val remotePoster = o.str("remotePoster")
        return remotePoster.ifEmpty { null }
    }

    // ---------------- library ----------------
    fun parseItem(o: JSONObject): MediaItem {
        val stats = o.obj("statistics")
        val title = when (type) {
            ServiceType.LIDARR -> o.str("artistName")
            ServiceType.READARR -> o.str("authorName")
            else -> o.str("title")
        }
        val (progress, complete, size) = when (type) {
            ServiceType.SONARR -> {
                val have = stats?.int("episodeFileCount") ?: 0
                val total = stats?.int("episodeCount") ?: 0
                Triple("$have/$total", total in 1..have, stats?.long("sizeOnDisk") ?: 0L)
            }
            ServiceType.RADARR -> {
                val has = o.bool("hasFile")
                Triple(if (has) "Downloaded" else if (o.bool("monitored")) "Missing" else "Unmonitored", has, o.long("sizeOnDisk"))
            }
            ServiceType.LIDARR -> {
                val have = stats?.int("trackFileCount") ?: 0
                val total = stats?.int("totalTrackCount") ?: stats?.int("trackCount") ?: 0
                Triple("$have/$total tracks", total in 1..have, stats?.long("sizeOnDisk") ?: 0L)
            }
            else -> {
                val have = stats?.int("bookFileCount") ?: 0
                val total = stats?.int("bookCount") ?: 0
                Triple("$have/$total books", total in 1..have, stats?.long("sizeOnDisk") ?: 0L)
            }
        }
        val year = o.int("year").let { y ->
            if (y > 0) y else o.str("firstAired").take(4).toIntOrNull() ?: 0
        }
        return MediaItem(
            id = o.int("id"),
            title = title,
            year = year,
            overview = o.str("overview"),
            poster = image(o, "poster", "cover", "headshot"),
            fanart = image(o, "fanart", "banner"),
            monitored = o.bool("monitored"),
            status = o.str("status"),
            sizeOnDisk = size,
            progressText = progress,
            complete = complete,
            added = o.str("added"),
            network = o.str("network").ifEmpty { o.str("studio") }.ifEmpty { o.str("genres") },
            raw = o,
        )
    }

    suspend fun library(): List<MediaItem> = fetchList(libPath).map { parseItem(it) }

    suspend fun item(id: Int): MediaItem = parseItem(JSONObject(get("$libPath/$id")))

    suspend fun lookup(term: String): List<MediaItem> = fetchList("$libPath/lookup", mapOf("term" to term)).map { parseItem(it) }

    suspend fun setMonitored(id: Int, monitored: Boolean) {
        val o = JSONObject(get("$libPath/$id"))
        o.put("monitored", monitored)
        put("$libPath/$id", o.toString())
    }

    /** Sonarr only – toggle one season's monitoring. */
    suspend fun setSeasonMonitored(seriesId: Int, season: Int, monitored: Boolean) {
        val o = JSONObject(get("series/$seriesId"))
        o.arr("seasons").objects().forEach { if (it.int("seasonNumber") == season) it.put("monitored", monitored) }
        put("series/$seriesId", o.toString())
    }

    suspend fun deleteItem(id: Int, deleteFiles: Boolean) {
        delete("$libPath/$id", mapOf("deleteFiles" to deleteFiles, "addImportExclusion" to false))
    }

    suspend fun command(body: JSONObject) {
        post("command", body.toString())
    }

    suspend fun searchItem(id: Int) = command(
        when (type) {
            ServiceType.SONARR -> JSONObject().put("name", "SeriesSearch").put("seriesId", id)
            ServiceType.RADARR -> JSONObject().put("name", "MoviesSearch").put("movieIds", intsJson(listOf(id)))
            ServiceType.LIDARR -> JSONObject().put("name", "ArtistSearch").put("artistId", id)
            else -> JSONObject().put("name", "AuthorSearch").put("authorId", id)
        }
    )

    suspend fun refreshItem(id: Int) = command(
        when (type) {
            ServiceType.SONARR -> JSONObject().put("name", "RefreshSeries").put("seriesId", id)
            ServiceType.RADARR -> JSONObject().put("name", "RefreshMovie").put("movieIds", intsJson(listOf(id)))
            ServiceType.LIDARR -> JSONObject().put("name", "RefreshArtist").put("artistId", id)
            else -> JSONObject().put("name", "RefreshAuthor").put("authorId", id)
        }
    )

    suspend fun searchChildren(ids: List<Int>) = command(
        when (type) {
            ServiceType.SONARR -> JSONObject().put("name", "EpisodeSearch").put("episodeIds", intsJson(ids))
            ServiceType.LIDARR -> JSONObject().put("name", "AlbumSearch").put("albumIds", intsJson(ids))
            ServiceType.READARR -> JSONObject().put("name", "BookSearch").put("bookIds", intsJson(ids))
            else -> JSONObject().put("name", "MoviesSearch").put("movieIds", intsJson(ids))
        }
    )

    suspend fun searchSeason(seriesId: Int, season: Int) =
        command(JSONObject().put("name", "SeasonSearch").put("seriesId", seriesId).put("seasonNumber", season))

    suspend fun runTask(name: String) = command(JSONObject().put("name", name))

    suspend fun children(parentId: Int): List<ChildItem> = when (type) {
        ServiceType.SONARR -> fetchList("episode", mapOf("seriesId" to parentId)).map {
            val s = it.int("seasonNumber")
            ChildItem(
                id = it.int("id"), groupKey = s,
                group = if (s == 0) "Specials" else "Season $s",
                title = "${it.int("episodeNumber")}. ${it.str("title")}",
                subtitle = it.str("airDate"),
                monitored = it.bool("monitored"), hasFile = it.bool("hasFile"), date = it.str("airDateUtc"),
            )
        }.sortedWith(compareByDescending<ChildItem> { if (it.groupKey == 0) -1 else it.groupKey }.thenBy { it.title.substringBefore('.').toIntOrNull() ?: 0 })
        ServiceType.LIDARR -> fetchList("album", mapOf("artistId" to parentId)).map {
            val st = it.obj("statistics")
            val pct = st?.dbl("percentOfTracks") ?: 0.0
            ChildItem(
                id = it.int("id"), groupKey = 0, group = it.str("albumType", "Album"),
                title = it.str("title"),
                subtitle = it.str("releaseDate").take(10) + "  ·  ${st?.int("trackFileCount") ?: 0}/${st?.int("totalTrackCount") ?: 0} tracks",
                monitored = it.bool("monitored"), hasFile = pct >= 100.0, date = it.str("releaseDate"),
            )
        }.sortedByDescending { it.date }
        ServiceType.READARR -> fetchList("book", mapOf("authorId" to parentId)).map {
            val st = it.obj("statistics")
            ChildItem(
                id = it.int("id"), groupKey = 0, group = "Books",
                title = it.str("title"),
                subtitle = it.str("releaseDate").take(10),
                monitored = it.bool("monitored"), hasFile = (st?.int("bookFileCount") ?: 0) > 0, date = it.str("releaseDate"),
            )
        }.sortedByDescending { it.date }
        else -> emptyList()
    }

    suspend fun setChildrenMonitored(ids: List<Int>, monitored: Boolean) {
        val (path, key) = when (type) {
            ServiceType.SONARR -> "episode/monitor" to "episodeIds"
            ServiceType.LIDARR -> "album/monitor" to "albumIds"
            else -> "book/monitor" to "bookIds"
        }
        put(path, JSONObject().put(key, intsJson(ids)).put("monitored", monitored).toString())
    }

    // ---------------- calendar / queue / history / wanted ----------------
    suspend fun calendar(daysBack: Long = 1, daysAhead: Long = 30): List<CalEntry> {
        val start = LocalDate.now().minusDays(daysBack).toString()
        val end = LocalDate.now().plusDays(daysAhead).toString()
        val q = mapOf("start" to start, "end" to end, includeParent to true, "unmonitored" to false)
        return fetchList("calendar", q).map { o -> toEntry(o) }.sortedBy { it.date }
    }

    private fun toEntry(o: JSONObject): CalEntry = when (type) {
        ServiceType.SONARR -> {
            val s = o.obj("series")
            CalEntry(
                o.int("id"), o.int("seriesId"), s?.str("title") ?: "Unknown",
                "S%02dE%02d · %s".format(o.int("seasonNumber"), o.int("episodeNumber"), o.str("title")),
                o.str("airDateUtc").ifEmpty { o.str("airDate") }, o.bool("hasFile"), image(s, "poster"),
            )
        }
        ServiceType.RADARR -> {
            val dates = listOf("digitalRelease", "physicalRelease", "inCinemas").map { o.str(it) }.filter { it.isNotEmpty() }
            val today = LocalDate.now().toString()
            val d = dates.filter { it >= today }.minOrNull() ?: dates.maxOrNull() ?: ""
            CalEntry(o.int("id"), o.int("id"), o.str("title"), "${o.int("year")} · ${o.str("studio")}", d, o.bool("hasFile"), image(o, "poster"))
        }
        ServiceType.LIDARR -> {
            val a = o.obj("artist")
            CalEntry(o.int("id"), o.int("artistId"), a?.str("artistName") ?: "", o.str("title"), o.str("releaseDate"),
                (o.obj("statistics")?.dbl("percentOfTracks") ?: 0.0) >= 100.0, image(o, "cover") ?: image(a, "poster"))
        }
        else -> {
            val a = o.obj("author")
            CalEntry(o.int("id"), o.int("authorId"), a?.str("authorName") ?: "", o.str("title"), o.str("releaseDate"),
                (o.obj("statistics")?.int("bookFileCount") ?: 0) > 0, image(o, "cover") ?: image(a, "poster"))
        }
    }

    suspend fun queue(): List<QueueEntry> {
        val q = mapOf("pageSize" to 200, includeParent to true, "includeUnknownSeriesItems" to true, "includeEpisode" to true)
        return fetchList("queue", q).map { o ->
            val parent = o.obj(parentKey)
            val parentTitle = parent?.str("title")?.ifEmpty { null } ?: parent?.str("artistName")?.ifEmpty { null }
                ?: parent?.str("authorName") ?: ""
            val warn = o.arr("statusMessages").objects().flatMap { m -> m.arr("messages").let { a -> (0 until a.length()).map { a.optString(it) } } }
                .joinToString("; ")
            QueueEntry(
                id = o.int("id"), title = o.str("title"), subtitle = parentTitle,
                status = o.str("trackedDownloadState").ifEmpty { o.str("status") },
                size = o.dbl("size").toLong(), sizeLeft = o.dbl("sizeleft").toLong(),
                timeLeft = o.str("timeleft"), client = o.str("downloadClient"), protocol = o.str("protocol"),
                warning = warn.ifEmpty { o.str("errorMessage") },
                downloadId = o.str("downloadId"),
            )
        }
    }

    suspend fun removeQueue(id: Int, removeFromClient: Boolean, blocklist: Boolean) {
        delete("queue/$id", mapOf("removeFromClient" to removeFromClient, "blocklist" to blocklist))
    }

    suspend fun history(): List<HistoryEntry> {
        val q = mapOf("pageSize" to 100, "sortKey" to "date", "sortDirection" to "descending", includeParent to true)
        return fetchList("history", q).map { o ->
            val ev = o.str("eventType")
            HistoryEntry(
                title = o.str("sourceTitle"),
                event = ev.replace(Regex("([a-z])([A-Z])"), "$1 $2").replaceFirstChar { it.uppercase() },
                date = o.str("date"),
                quality = o.obj("quality")?.obj("quality")?.str("name") ?: "",
            )
        }
    }

    /** Missing items. Falls back to library filtering on older Radarr builds without /wanted. */
    suspend fun wanted(): List<CalEntry> {
        return try {
            val q = mapOf("pageSize" to 200, includeParent to true, "monitored" to true, "sortDirection" to "descending",
                "sortKey" to if (type == ServiceType.SONARR) "episodes.airDateUtc" else "releaseDate")
            fetchList("wanted/missing", q).map { toEntry(it) }
        } catch (e: ApiException) {
            if (type == ServiceType.RADARR && e.code == 404) {
                library().filter { it.monitored && !it.complete }.map {
                    CalEntry(it.id, it.id, it.title, it.year.toString(), it.raw.str("inCinemas"), false, it.poster)
                }
            } else throw e
        }
    }

    // ---------------- releases (interactive search) ----------------
    suspend fun releases(q: Map<String, Any?>): List<ReleaseEntry> = fetchList("release", q).map { o ->
        val age = o.dbl("ageHours")
        ReleaseEntry(
            guid = o.str("guid"), indexerId = o.int("indexerId"), title = o.str("title"), indexer = o.str("indexer"),
            size = o.long("size"),
            seeders = if (o.has("seeders") && !o.isNull("seeders")) o.int("seeders") else null,
            leechers = if (o.has("leechers") && !o.isNull("leechers")) o.int("leechers") else null,
            protocol = o.str("protocol"),
            quality = o.obj("quality")?.obj("quality")?.str("name") ?: "",
            ageText = if (age < 48) "${age.toInt()}h" else "${o.int("age")}d",
            rejected = o.bool("rejected"),
            rejections = o.arr("rejections").let { a -> (0 until a.length()).map { a.optString(it) } },
            customScore = o.int("customFormatScore"),
        )
    }.sortedWith(compareBy<ReleaseEntry> { it.rejected }.thenByDescending { it.customScore }.thenByDescending { it.seeders ?: 0 })

    fun releaseQueryForItem(id: Int): Map<String, Any?> = when (type) {
        ServiceType.RADARR -> mapOf("movieId" to id)
        ServiceType.SONARR -> mapOf("seriesId" to id)
        ServiceType.LIDARR -> mapOf("artistId" to id)
        else -> mapOf("authorId" to id)
    }

    fun releaseQueryForChild(id: Int): Map<String, Any?> = when (type) {
        ServiceType.SONARR -> mapOf("episodeId" to id)
        ServiceType.LIDARR -> mapOf("albumId" to id)
        else -> mapOf("bookId" to id)
    }

    suspend fun grab(r: ReleaseEntry) {
        post("release", JSONObject().put("guid", r.guid).put("indexerId", r.indexerId).toString())
    }

    // ---------------- adding ----------------
    suspend fun qualityProfiles(): List<Profile> = fetchList("qualityprofile").map { Profile(it.int("id"), it.str("name")) }
    suspend fun metadataProfiles(): List<Profile> =
        if (type == ServiceType.LIDARR || type == ServiceType.READARR)
            fetchList("metadataprofile").map { Profile(it.int("id"), it.str("name")) }.filter { it.name != "None" }
        else emptyList()
    suspend fun languageProfiles(): List<Profile> =
        if (type == ServiceType.SONARR) try { fetchList("languageprofile").map { Profile(it.int("id"), it.str("name")) } } catch (e: Exception) { emptyList() }
        else emptyList()
    suspend fun rootFolders(): List<RootFolder> = fetchList("rootfolder").map { RootFolder(it.str("path"), it.long("freeSpace")) }
    suspend fun diskSpace(): List<DiskSpace> = fetchList("diskspace").map {
        DiskSpace(it.str("path"), it.str("label"), it.long("freeSpace"), it.long("totalSpace"))
    }

    suspend fun add(
        raw: JSONObject,
        qualityProfileId: Int,
        rootFolder: String,
        metadataProfileId: Int?,
        languageProfileId: Int?,
        monitor: String,
        searchNow: Boolean,
        minimumAvailability: String,
        seriesType: String,
    ): Int {
        val o = JSONObject(raw.toString())
        o.put("qualityProfileId", qualityProfileId)
        o.put("rootFolderPath", rootFolder)
        o.put("monitored", monitor != "none")
        when (type) {
            ServiceType.SONARR -> {
                o.put("seasonFolder", true)
                o.put("seriesType", seriesType)
                if (languageProfileId != null) o.put("languageProfileId", languageProfileId)
                o.put("addOptions", JSONObject().put("monitor", monitor).put("searchForMissingEpisodes", searchNow)
                    .put("searchForCutoffUnmetEpisodes", false))
            }
            ServiceType.RADARR -> {
                o.put("minimumAvailability", minimumAvailability)
                o.put("addOptions", JSONObject().put("searchForMovie", searchNow).put("monitor", if (monitor == "none") "none" else "movieOnly"))
            }
            ServiceType.LIDARR -> {
                if (metadataProfileId != null) o.put("metadataProfileId", metadataProfileId)
                o.put("addOptions", JSONObject().put("monitor", monitor).put("searchForMissingAlbums", searchNow))
            }
            else -> {
                if (metadataProfileId != null) o.put("metadataProfileId", metadataProfileId)
                o.put("addOptions", JSONObject().put("monitor", monitor).put("searchForMissingBooks", searchNow))
            }
        }
        val res = JSONObject(post(libPath, o.toString()))
        return res.int("id")
    }

    val monitorOptions: List<Pair<String, String>> = when (type) {
        ServiceType.SONARR -> listOf(
            "all" to "All episodes", "future" to "Future episodes", "missing" to "Missing episodes",
            "existing" to "Existing episodes", "pilot" to "Pilot only", "firstSeason" to "First season",
            "latestSeason" to "Latest season", "none" to "None",
        )
        ServiceType.RADARR -> listOf("movieOnly" to "Monitored", "none" to "Not monitored")
        ServiceType.LIDARR -> listOf("all" to "All albums", "future" to "Future albums", "missing" to "Missing albums",
            "existing" to "Existing albums", "latest" to "Latest album", "first" to "First album", "none" to "None")
        else -> listOf("all" to "All books", "future" to "Future books", "missing" to "Missing books",
            "existing" to "Existing books", "latest" to "Latest book", "first" to "First book", "none" to "None")
    }
}
