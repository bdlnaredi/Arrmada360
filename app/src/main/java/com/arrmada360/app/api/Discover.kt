package com.arrmada360.app.api

import com.arrmada360.app.data.ConfigStore
import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.data.ServiceType
import com.arrmada360.app.net.Http
import com.arrmada360.app.net.arr
import com.arrmada360.app.net.buildUrl
import com.arrmada360.app.net.dbl
import com.arrmada360.app.net.int
import com.arrmada360.app.net.obj
import com.arrmada360.app.net.objects
import com.arrmada360.app.net.str
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class DiscoverItem(
    val tmdbId: Int,
    val mediaType: String, // "movie" or "tv"
    val title: String,
    val year: String,
    val poster: String?,
    val backdrop: String?,
    val overview: String,
    val rating: Double,
    val source: String,
    val tvdbId: Int = 0,
    val extra: String = "",
    val date: String = "",
    val seerrStatus: Int = 0,
)

data class DiscoverPerson(val id: Int, val name: String, val photo: String?, val knownFor: List<String>, val dept: String, val source: String)

data class DiscoverRow<T>(val items: List<T>, val source: String)

/**
 * Discovery feeds for the Movies / TV Shows tabs.
 * Each row tries Trakt, then TMDB, then Seerr (whichever the user has set up) and uses the first that answers.
 */
object Discover {
    private val client get() = Http.client(false)
    fun seerr(): ServiceConfig? = ConfigStore.services.firstOrNull { it.type == ServiceType.SEERR }
    private fun tmdbKey() = ConfigStore.tmdbKey.trim()
    private fun traktId() = ConfigStore.traktClientId.trim()
    fun hasAnySource() = seerr() != null || tmdbKey().isNotEmpty() || traktId().isNotEmpty()

    private fun img(p: String, size: String = "w342") = if (p.isEmpty() || p == "null") null else "https://image.tmdb.org/t/p/$size$p"
    private fun traktPath(type: String) = if (type == "movie") "movies" else "shows"

    // ------------------------------------------------------------ raw clients
    private suspend fun tmdb(path: String, q: Map<String, Any?> = emptyMap()): JSONObject {
        val key = tmdbKey()
        val bearer = key.length > 40
        val qq = q.toMutableMap()
        if (!bearer) qq["api_key"] = key
        val b = Request.Builder().url(buildUrl("https://api.themoviedb.org/3/$path", qq))
        if (bearer) b.header("Authorization", "Bearer $key")
        return JSONObject(Http.exec(client, b.build()))
    }

    private suspend fun seerrGet(path: String, q: Map<String, Any?> = emptyMap()): JSONObject {
        val c = seerr() ?: throw Exception("no seerr")
        return JSONObject(Http.exec(Http.client(c.trustAllCerts), Request.Builder().url(buildUrl("${c.baseUrl}/api/v1/$path", q)).header("X-Api-Key", c.apiKey).build()))
    }

    private suspend fun trakt(path: String, q: Map<String, Any?> = mapOf("limit" to 24, "extended" to "full")): JSONArray =
        JSONArray(
            Http.exec(
                client,
                Request.Builder().url(buildUrl("https://api.trakt.tv/$path", q))
                    .header("trakt-api-version", "2").header("trakt-api-key", traktId()).header("Content-Type", "application/json").build(),
            ),
        )

    // ------------------------------------------------------------ parsers
    private fun tmdbItem(o: JSONObject, type: String): DiscoverItem {
        val d = o.str("release_date").ifEmpty { o.str("first_air_date") }
        return DiscoverItem(
            o.int("id"), type, o.str("title").ifEmpty { o.str("name") }, d.take(4),
            img(o.str("poster_path")), img(o.str("backdrop_path"), "w780"), o.str("overview"), o.dbl("vote_average"), "TMDB", date = d,
        )
    }

    private fun seerrItem(o: JSONObject, defType: String): DiscoverItem? {
        val t = o.str("mediaType").ifEmpty { defType }
        if (t != "movie" && t != "tv") return null
        val d = o.str("releaseDate").ifEmpty { o.str("firstAirDate") }
        return DiscoverItem(
            o.int("id"), t, o.str("title").ifEmpty { o.str("name") }, d.take(4),
            img(o.str("posterPath")), img(o.str("backdropPath"), "w780"), o.str("overview"), o.dbl("voteAverage"), "Seerr",
            date = d, seerrStatus = o.obj("mediaInfo")?.int("status") ?: 0,
        )
    }

    private fun traktItem(o: JSONObject, type: String, extra: String = "", date: String = ""): DiscoverItem {
        val ids = o.obj("ids")
        val y = o.int("year")
        return DiscoverItem(
            ids?.int("tmdb") ?: 0, type, o.str("title"), if (y > 0) y.toString() else "", null, null,
            o.str("overview"), o.dbl("rating"), "Trakt", tvdbId = ids?.int("tvdb") ?: 0, extra = extra,
            date = date.ifEmpty { o.str("released").ifEmpty { o.str("first_aired") } },
        )
    }

    private val artCache = HashMap<String, Triple<String?, String?, String>>()

    /** Trakt has no artwork, so fill posters from TMDB (if keyed) or Seerr. */
    private suspend fun withArt(items: List<DiscoverItem>): List<DiscoverItem> = coroutineScope {
        items.map { i ->
            async {
                if (i.poster != null || i.tmdbId == 0) return@async i
                val key = "${i.mediaType}/${i.tmdbId}"
                val art = artCache[key] ?: try {
                    if (tmdbKey().isNotEmpty()) {
                        val o = tmdb(key)
                        Triple(img(o.str("poster_path")), img(o.str("backdrop_path"), "w780"), o.str("overview"))
                    } else if (seerr() != null) {
                        val o = seerrGet(key)
                        Triple(img(o.str("posterPath")), img(o.str("backdropPath"), "w780"), o.str("overview"))
                    } else Triple(null, null, "")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Triple(null, null, "")
                }.also { artCache[key] = it }
                i.copy(poster = art.first, backdrop = art.second, overview = i.overview.ifEmpty { art.third })
            }
        }.awaitAll()
    }

    private suspend fun <T> firstOf(vararg tries: Pair<String, suspend () -> List<T>>): DiscoverRow<T> {
        var lastError: Exception? = null
        for ((name, f) in tries) {
            try {
                val r = f()
                if (r.isNotEmpty()) return DiscoverRow(r, name)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
            }
        }
        if (lastError != null && !hasAnySource()) throw lastError
        return DiscoverRow(emptyList(), "")
    }

    private fun tr(type: String, f: suspend () -> List<DiscoverItem>): Pair<String, suspend () -> List<DiscoverItem>> =
        "Trakt" to suspend { if (traktId().isEmpty()) emptyList<DiscoverItem>() else withArt(f()) }
    private fun tm(f: suspend () -> List<DiscoverItem>): Pair<String, suspend () -> List<DiscoverItem>> =
        "TMDB" to suspend { if (tmdbKey().isEmpty()) emptyList<DiscoverItem>() else f() }
    private fun se(f: suspend () -> List<DiscoverItem>): Pair<String, suspend () -> List<DiscoverItem>> =
        "Seerr" to suspend { if (seerr() == null) emptyList<DiscoverItem>() else f() }

    // ------------------------------------------------------------ rows
    suspend fun trending(type: String): DiscoverRow<DiscoverItem> = firstOf(
        tr(type) {
            trakt("${traktPath(type)}/trending").objects().mapNotNull { w ->
                w.obj(if (type == "movie") "movie" else "show")?.let { traktItem(it, type, "${w.int("watchers")} watching") }
            }
        },
        tm { tmdb("trending/$type/week").arr("results").objects().map { tmdbItem(it, type) } },
        se { seerrGet("discover/trending", mapOf("page" to 1)).arr("results").objects().mapNotNull { seerrItem(it, "") }.filter { it.mediaType == type } },
    )

    suspend fun popular(type: String): DiscoverRow<DiscoverItem> = firstOf(
        tr(type) { trakt("${traktPath(type)}/popular").objects().map { traktItem(it, type) } },
        tm { tmdb("$type/popular").arr("results").objects().map { tmdbItem(it, type) } },
        se { seerrGet(if (type == "movie") "discover/movies" else "discover/tv", mapOf("page" to 1)).arr("results").objects().mapNotNull { seerrItem(it, type) } },
    )

    suspend fun anticipated(type: String): DiscoverRow<DiscoverItem> {
        val today = LocalDate.now().toString()
        return firstOf(
            tr(type) {
                trakt("${traktPath(type)}/anticipated").objects().mapNotNull { w ->
                    w.obj(if (type == "movie") "movie" else "show")?.let { traktItem(it, type, "${w.int("list_count")} lists") }
                }
            },
            tm {
                val dateKey = if (type == "movie") "primary_release_date.gte" else "first_air_date.gte"
                tmdb("discover/$type", mapOf(dateKey to today, "sort_by" to "popularity.desc")).arr("results").objects().map { tmdbItem(it, type) }
            },
            se { seerrGet(if (type == "movie") "discover/movies/upcoming" else "discover/tv/upcoming", mapOf("page" to 1)).arr("results").objects().mapNotNull { seerrItem(it, type) } },
        )
    }

    suspend fun upcoming(type: String): DiscoverRow<DiscoverItem> {
        val today = LocalDate.now().toString()
        val row = firstOf(
            tr(type) {
                if (type == "movie") trakt("calendars/all/movies/$today/45", emptyMap()).objects().mapNotNull { w ->
                    w.obj("movie")?.let { traktItem(it, type, date = w.str("released")) }
                } else trakt("calendars/all/shows/premieres/$today/45", emptyMap()).objects().mapNotNull { w ->
                    w.obj("show")?.let { traktItem(it, type, extra = "Premiere", date = w.str("first_aired")) }
                }.distinctBy { it.tmdbId }
            },
            tm { tmdb(if (type == "movie") "movie/upcoming" else "tv/on_the_air").arr("results").objects().map { tmdbItem(it, type) } },
            se { seerrGet(if (type == "movie") "discover/movies/upcoming" else "discover/tv/upcoming", mapOf("page" to 1)).arr("results").objects().mapNotNull { seerrItem(it, type) } },
        )
        return row.copy(items = row.items.filter { it.date.isEmpty() || it.date >= today }.sortedBy { it.date.ifEmpty { "9999" } }.take(30))
    }

    suspend fun people(): DiscoverRow<DiscoverPerson> = firstOf(
        "TMDB" to suspend {
            if (tmdbKey().isEmpty()) emptyList<DiscoverPerson>() else tmdb("person/popular").arr("results").objects().map { p ->
                DiscoverPerson(p.int("id"), p.str("name"), img(p.str("profile_path"), "w185"),
                    p.arr("known_for").objects().map { it.str("title").ifEmpty { it.str("name") } }.filter { it.isNotEmpty() },
                    p.str("known_for_department"), "TMDB")
            }
        },
        "Seerr" to suspend {
            if (seerr() == null) emptyList<DiscoverPerson>() else (1..2).flatMap { page ->
                seerrGet("discover/trending", mapOf("page" to page)).arr("results").objects().filter { it.str("mediaType") == "person" }.map { p ->
                    DiscoverPerson(p.int("id"), p.str("name"), img(p.str("profilePath"), "w185"),
                        p.arr("knownFor").objects().map { it.str("title").ifEmpty { it.str("name") } }.filter { it.isNotEmpty() },
                        p.str("knownForDepartment"), "Seerr")
                }
            }
        },
    )

    /** Recommendations based on the newest titles in the user's library. */
    suspend fun recommended(type: String, seeds: List<Int>, exclude: Set<Int>): DiscoverRow<DiscoverItem> {
        if (seeds.isEmpty()) return DiscoverRow(emptyList(), "")
        suspend fun collect(fetch: suspend (Int) -> List<DiscoverItem>): List<DiscoverItem> = coroutineScope {
            val lists = seeds.take(5).map { id -> async { try { fetch(id) } catch (e: CancellationException) { throw e } catch (e: Exception) { emptyList() } } }.awaitAll()
            val out = LinkedHashMap<Int, DiscoverItem>()
            val maxLen = lists.maxOfOrNull { it.size } ?: 0
            for (i in 0 until maxLen) for (l in lists) l.getOrNull(i)?.let { if (it.tmdbId !in exclude && it.tmdbId !in out) out[it.tmdbId] = it }
            out.values.take(30)
        }
        return firstOf(
            tm { collect { id -> tmdb("$type/$id/recommendations").arr("results").objects().map { tmdbItem(it, type) } } },
            se { collect { id -> seerrGet("$type/$id/recommendations", mapOf("page" to 1)).arr("results").objects().mapNotNull { seerrItem(it, type) } } },
        )
    }

    fun webUrl(i: DiscoverItem) = "https://www.themoviedb.org/${i.mediaType}/${i.tmdbId}"
    fun personUrl(p: DiscoverPerson) = "https://www.themoviedb.org/person/${p.id}"

    @Suppress("unused")
    private fun unused(t: String) = traktPath(t)
}
