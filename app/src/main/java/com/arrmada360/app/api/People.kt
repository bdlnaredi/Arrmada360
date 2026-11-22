package com.arrmada360.app.api

import com.arrmada360.app.net.arr
import com.arrmada360.app.net.dbl
import com.arrmada360.app.net.int
import com.arrmada360.app.net.obj
import com.arrmada360.app.net.objects
import com.arrmada360.app.net.str
import org.json.JSONObject

data class PersonCredit(
    val tmdbId: Int, val title: String, val year: Int, val isTv: Boolean, val poster: String?,
    val role: String, val crew: Boolean, val popularity: Double, val date: String, val episodes: Int,
)
data class PersonDetail(
    val id: Int, val name: String, val photo: String?, val bio: String, val birthday: String, val deathday: String,
    val birthplace: String, val dept: String, val imdbId: String, val credits: List<PersonCredit>, val source: String,
)

/** Cast lists, biographies and filmographies – from TMDB when you have a key, otherwise from Seerr. */
object People {
    private fun img(p: String, size: String = "w342") = if (p.isEmpty()) null else "https://image.tmdb.org/t/p/$size$p"
    val available: Boolean get() = Discover.tmdbKey().isNotEmpty() || Discover.seerr() != null

    suspend fun person(id: Int): PersonDetail {
        if (Discover.tmdbKey().isNotEmpty()) {
            val o = Discover.tmdb("person/$id", mapOf("append_to_response" to "combined_credits"))
            val cc = o.obj("combined_credits") ?: JSONObject()
            return detail(o, id, "TMDB", cc.arr("cast").objects().map { credit(it, false) } + cc.arr("crew").objects().map { credit(it, true) },
                photoKey = "profile_path", bioKey = "biography", placeKey = "place_of_birth", deptKey = "known_for_department", imdbKey = "imdb_id")
        }
        if (Discover.seerr() != null) {
            val o = Discover.seerrGet("person/$id")
            val cc = try { Discover.seerrGet("person/$id/combined_credits") } catch (e: Exception) { JSONObject() }
            return detail(o, id, "Seerr", cc.arr("cast").objects().map { credit(it, false) } + cc.arr("crew").objects().map { credit(it, true) },
                photoKey = "profilePath", bioKey = "biography", placeKey = "placeOfBirth", deptKey = "knownForDepartment", imdbKey = "imdbId")
        }
        throw Exception("Add a free TMDB key in Settings → Discovery sources (or connect Overseerr/Jellyseerr) to see filmographies")
    }

    private fun detail(o: JSONObject, id: Int, src: String, credits: List<PersonCredit>, photoKey: String, bioKey: String, placeKey: String, deptKey: String, imdbKey: String) =
        PersonDetail(
            id, o.str("name"), img(o.str(photoKey), "h632"), o.str(bioKey), o.str("birthday"), o.str("deathday"), o.str(placeKey), o.str(deptKey), o.str(imdbKey),
            // one entry per title per role type; merge jobs for crew
            credits.filter { it.tmdbId > 0 && it.title.isNotBlank() }
                .groupBy { Triple(it.tmdbId, it.isTv, it.crew) }
                .map { (_, list) -> list.first().copy(role = list.map { it.role }.filter { it.isNotBlank() }.distinct().joinToString(", ")) },
            src,
        )

    private fun credit(o: JSONObject, crew: Boolean): PersonCredit {
        val tv = (o.str("media_type").ifEmpty { o.str("mediaType") }) == "tv"
        val date = if (tv) o.str("first_air_date").ifEmpty { o.str("firstAirDate") } else o.str("release_date").ifEmpty { o.str("releaseDate") }
        return PersonCredit(
            tmdbId = o.int("id"), title = if (tv) o.str("name") else o.str("title"), year = date.take(4).toIntOrNull() ?: 0, isTv = tv,
            poster = img(o.str("poster_path").ifEmpty { o.str("posterPath") }), role = if (crew) o.str("job") else o.str("character"), crew = crew,
            popularity = o.dbl("popularity"), date = date, episodes = o.int("episode_count"),
        )
    }

    /** Cast of a TV show (Sonarr doesn't keep cast). */
    suspend fun tvCast(m: MediaItem): List<ArrApi.Credit> {
        val id = MediaExtras.tmdbId(m, false) ?: return emptyList()
        if (Discover.tmdbKey().isNotEmpty()) {
            return Discover.tmdb("tv/$id/aggregate_credits").arr("cast").objects().take(30).map { c ->
                ArrApi.Credit(c.str("name"), c.arr("roles").objects().firstOrNull()?.str("character").orEmpty(), img(c.str("profile_path"), "w185"), false, c.int("id"))
            }
        }
        if (Discover.seerr() != null) {
            return Discover.seerrGet("tv/$id").obj("credits")?.arr("cast")?.objects()?.take(30)?.map { c ->
                ArrApi.Credit(c.str("name"), c.str("character"), img(c.str("profilePath"), "w185"), false, c.int("id"))
            } ?: emptyList()
        }
        return emptyList()
    }
}
