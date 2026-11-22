package com.arrmada360.app.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.data.ServiceType
import com.arrmada360.app.net.arr
import com.arrmada360.app.net.int
import com.arrmada360.app.net.obj
import com.arrmada360.app.net.objects
import com.arrmada360.app.net.str
import org.json.JSONObject
import java.util.Locale

data class Provider(val name: String, val logo: String?)
data class WatchProviders(val region: String, val link: String, val stream: List<Provider>, val rent: List<Provider>, val buy: List<Provider>, val free: List<Provider>)

/** Trailers and "where to stream" through TMDB. */
object MediaExtras {
    val hasTmdb: Boolean get() = Discover.tmdbKey().isNotEmpty()

    fun region(): String = com.arrmada360.app.data.ConfigStore.getString("watchRegion").ifEmpty { Locale.getDefault().country.ifEmpty { "IN" } }

    /** Finds the TMDB id of a library item (Sonarr may only know the TVDB id). */
    suspend fun tmdbId(m: MediaItem, movie: Boolean): Int? {
        m.raw.optInt("tmdbId").takeIf { it > 0 }?.let { return it }
        if (!hasTmdb) return null
        val tvdb = m.raw.optInt("tvdbId").takeIf { it > 0 } ?: return null
        val r = Discover.tmdb("find/$tvdb", mapOf("external_source" to "tvdb_id"))
        return r.arr(if (movie) "movie_results" else "tv_results").objects().firstOrNull()?.int("id")
    }

    suspend fun trailerKey(m: MediaItem, movie: Boolean): String? {
        m.raw.optString("youTubeTrailerId").takeIf { it.isNotBlank() }?.let { return it }
        if (!hasTmdb) return null
        val id = tmdbId(m, movie) ?: return null
        val vids = Discover.tmdb("${if (movie) "movie" else "tv"}/$id/videos").arr("results").objects()
            .filter { it.str("site") == "YouTube" }
        val best = vids.firstOrNull { it.str("type") == "Trailer" && it.optBoolean("official") }
            ?: vids.firstOrNull { it.str("type") == "Trailer" } ?: vids.firstOrNull { it.str("type") == "Teaser" } ?: vids.firstOrNull()
        return best?.str("key")
    }

    fun openTrailer(ctx: Context, key: String?, fallbackQuery: String) {
        val intents = if (key != null) listOf(Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$key")), Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$key")))
        else listOf(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode("$fallbackQuery trailer"))))
        for (i in intents) {
            try { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return } catch (e: Exception) { }
        }
    }

    suspend fun providers(m: MediaItem, movie: Boolean): WatchProviders? {
        if (!hasTmdb) return null
        val id = tmdbId(m, movie) ?: return null
        val all = Discover.tmdb("${if (movie) "movie" else "tv"}/$id/watch/providers").obj("results") ?: return null
        val reg = region()
        val r = all.obj(reg) ?: return WatchProviders(reg, "", emptyList(), emptyList(), emptyList(), emptyList())
        fun list(k: String) = r.arr(k).objects().map { p -> Provider(p.str("provider_name"), p.str("logo_path").takeIf { it.isNotEmpty() }?.let { "https://image.tmdb.org/t/p/w92$it" }) }
        return WatchProviders(reg, r.str("link"), list("flatrate"), list("rent"), list("buy"), list("free") + list("ads"))
    }
}

// ------------------------------------------------------------ Radarr collections
data class CollMovie(val tmdbId: Int, val title: String, val year: Int, val poster: String?)
data class ArrCollection(val id: Int, val title: String, val monitored: Boolean, val poster: String?, val movies: List<CollMovie>, val raw: JSONObject)

suspend fun ArrApi.collections(): List<ArrCollection> = fetchList("collection").map { c ->
    ArrCollection(
        id = c.int("id"), title = c.str("title"), monitored = c.optBoolean("monitored"),
        poster = image(c, "poster"),
        movies = c.arr("movies").objects().map { m -> CollMovie(m.int("tmdbId"), m.str("title"), m.int("year"), image(m, "poster")) }.sortedBy { it.year },
        raw = c,
    )
}.filter { it.movies.isNotEmpty() }.sortedBy { it.title }

/** Turns on "monitor collection" so Radarr adds every missing movie, then refreshes collections. */
suspend fun ArrApi.monitorCollection(c: ArrCollection, qualityProfileId: Int, rootFolder: String, search: Boolean) {
    val o = JSONObject(c.raw.toString()).put("monitored", true).put("qualityProfileId", qualityProfileId)
        .put("rootFolderPath", rootFolder).put("searchOnAdd", search)
    if (!o.has("minimumAvailability") || o.optString("minimumAvailability").isEmpty()) o.put("minimumAvailability", "released")
    put("collection/${c.id}", o.toString())
    command(JSONObject().put("name", "RefreshCollections"))
}

/** Adds one movie by TMDB id with the given profile and folder. */
suspend fun ArrApi.addByTmdb(tmdbId: Int, qualityProfileId: Int, rootFolder: String, search: Boolean): Int {
    val raw = JSONObject(get("movie/lookup/tmdb", mapOf("tmdbId" to tmdbId)))
    return add(raw, qualityProfileId, rootFolder, null, null, "movieOnly", search, "released", "")
}

// ------------------------------------------------------------ quality & duplicates
data class QualityRow(val itemId: Int, val childId: Int?, val title: String, val subtitle: String, val quality: String, val size: Long, val poster: String?, val instance: ServiceConfig)

suspend fun ArrApi.cutoffUnmet(): List<QualityRow> {
    val q = mutableMapOf<String, Any?>("page" to 1, "pageSize" to 500, "sortKey" to if (type == ServiceType.RADARR) "title" else "airDateUtc", "sortDirection" to "descending")
    if (type == ServiceType.SONARR) { q["includeSeries"] = true; q["includeEpisodeFile"] = true }
    val recs = JSONObject(get("wanted/cutoff", q)).arr("records").objects()
    return recs.map { r ->
        if (type == ServiceType.RADARR) {
            val f = r.obj("movieFile")
            QualityRow(r.int("id"), null, r.str("title"), r.int("year").toString(), f?.obj("quality")?.obj("quality")?.str("name").orEmpty(), f?.optLong("size") ?: 0, image(r, "poster"), cfg)
        } else {
            val s = r.obj("series")
            QualityRow(r.int("seriesId"), r.int("id"), s?.str("title").orEmpty(), "S%02dE%02d · %s".format(r.int("seasonNumber"), r.int("episodeNumber"), r.str("title")),
                r.obj("episodeFile")?.obj("quality")?.obj("quality")?.str("name").orEmpty(), r.obj("episodeFile")?.optLong("size") ?: 0, image(s, "poster"), cfg)
        }
    }
}

suspend fun ArrApi.searchCutoff(rows: List<QualityRow>) {
    if (type == ServiceType.RADARR) command(JSONObject().put("name", "MoviesSearch").put("movieIds", org.json.JSONArray(rows.map { it.itemId })))
    else command(JSONObject().put("name", "EpisodeSearch").put("episodeIds", org.json.JSONArray(rows.mapNotNull { it.childId })))
}

fun movieHeight(m: MediaItem): Int {
    val f = m.raw.optJSONObject("movieFile") ?: return -1
    val res = f.optJSONObject("mediaInfo")?.optString("resolution").orEmpty()
    res.substringAfter('x', "").toIntOrNull()?.let { return it }
    val q = f.optJSONObject("quality")?.optJSONObject("quality")
    return q?.optInt("resolution")?.takeIf { it > 0 } ?: 0
}

fun movieQuality(m: MediaItem): String = m.raw.optJSONObject("movieFile")?.optJSONObject("quality")?.optJSONObject("quality")?.optString("name").orEmpty()
fun movieSubs(m: MediaItem): String = m.raw.optJSONObject("movieFile")?.optJSONObject("mediaInfo")?.optString("subtitles").orEmpty()
