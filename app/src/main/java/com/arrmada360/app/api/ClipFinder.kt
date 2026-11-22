package com.arrmada360.app.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Base64
import com.arrmada360.app.data.ConfigStore
import com.arrmada360.app.net.ApiException
import com.arrmada360.app.net.Http
import com.arrmada360.app.net.arr
import com.arrmada360.app.net.dbl
import com.arrmada360.app.net.int
import com.arrmada360.app.net.obj
import com.arrmada360.app.net.objects
import com.arrmada360.app.net.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

data class ClipGuess(
    val title: String,
    val year: Int?,
    val isTv: Boolean,
    val season: Int?,
    val episode: Int?,
    val confidence: Int,
    val reason: String,
    val source: String,
)

/** Works out which movie or show a shared clip or screenshot comes from. */
object ClipFinder {
    var geminiKey: String
        get() = ConfigStore.getString("geminiKey")
        set(v) = ConfigStore.putString("geminiKey", v.trim())

    private val client by lazy { Http.client(false) }

    /** Pulls a few evenly spaced frames out of a video (or loads an image). */
    suspend fun frames(ctx: Context, uri: Uri, mime: String?): List<Bitmap> = withContext(Dispatchers.IO) {
        val isImage = mime?.startsWith("image") == true
        if (isImage) {
            val b = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            return@withContext listOfNotNull(b?.let { scale(it) })
        }
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(ctx, uri)
            val durMs = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val points = if (durMs <= 0) listOf(0L) else listOf(0.12, 0.35, 0.6, 0.85).map { (durMs * it).toLong() }
            points.mapNotNull { ms ->
                val us = ms * 1000
                val f = if (Build.VERSION.SDK_INT >= 27) r.getScaledFrameAtTime(us, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 768, 768)
                else r.getFrameAtTime(us, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                f?.let { scale(it) }
            }.filter { !isMostlyBlack(it) }.ifEmpty {
                listOfNotNull(r.getFrameAtTime(0)?.let { scale(it) })
            }
        } finally { try { r.release() } catch (e: Exception) { } }
    }

    private fun scale(b: Bitmap): Bitmap {
        val max = 768
        val s = max.toFloat() / maxOf(b.width, b.height)
        return if (s >= 1f) b else Bitmap.createScaledBitmap(b, (b.width * s).toInt(), (b.height * s).toInt(), true)
    }

    private fun isMostlyBlack(b: Bitmap): Boolean {
        var dark = 0; var n = 0
        val stepX = (b.width / 12).coerceAtLeast(1); val stepY = (b.height / 12).coerceAtLeast(1)
        var y = 0
        while (y < b.height) { var x = 0; while (x < b.width) { val p = b.getPixel(x, y); val l = ((p shr 16 and 255) + (p shr 8 and 255) + (p and 255)) / 3; if (l < 18) dark++; n++; x += stepX }; y += stepY }
        return n > 0 && dark.toFloat() / n > 0.9f
    }

    private fun jpeg(b: Bitmap, q: Int = 82): ByteArray = ByteArrayOutputStream().also { b.compress(Bitmap.CompressFormat.JPEG, q, it) }.toByteArray()

    /** Asks Google Gemini (free API key) to recognise the frames. */
    suspend fun askGemini(frames: List<Bitmap>, hint: String): ClipGuess? {
        val key = geminiKey
        if (key.isEmpty() || frames.isEmpty()) return null
        val prompt = """These images are frames from one short clip of a movie or TV show${if (hint.isNotBlank()) " (the person adds: \"$hint\")" else ""}.
Identify the movie or TV show. Use faces of actors, on-screen text, logos, sets and style.
Reply ONLY with JSON: {"title": string, "year": number|null, "type": "movie"|"tv", "season": number|null, "episode": number|null, "confidence": 0-100, "reason": "one short sentence"}.
If you are not sure, still give your best guess with a low confidence."""
        val parts = JSONArray().put(JSONObject().put("text", prompt))
        frames.take(4).forEach { f ->
            parts.put(JSONObject().put("inline_data", JSONObject().put("mime_type", "image/jpeg").put("data", Base64.encodeToString(jpeg(f), Base64.NO_WRAP))))
        }
        val body = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", parts)))
            .put("generationConfig", JSONObject().put("responseMimeType", "application/json").put("temperature", 0.2))
            .toString()
        var lastErr: Exception? = null
        for (model in listOf(ConfigStore.getString("geminiModel").ifEmpty { "gemini-2.5-flash" }, "gemini-flash-latest", "gemini-2.0-flash")) {
            try {
                val req = Request.Builder().url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
                    .header("x-goog-api-key", key).post(body.toRequestBody("application/json".toMediaType())).build()
                val res = JSONObject(Http.exec(client, req))
                val text = res.arr("candidates").objects().firstOrNull()?.obj("content")?.arr("parts")?.objects()
                    ?.joinToString("") { it.str("text") }.orEmpty()
                val j = JSONObject(text.substringAfter("```json").substringAfter("```").let { if (it.contains("{")) it.substring(it.indexOf('{'), it.lastIndexOf('}') + 1) else text })
                return ClipGuess(
                    title = j.str("title"), year = j.optInt("year").takeIf { it > 1800 },
                    isTv = j.str("type").equals("tv", true), season = j.optInt("season").takeIf { it > 0 }, episode = j.optInt("episode").takeIf { it > 0 },
                    confidence = j.optInt("confidence").coerceIn(0, 100), reason = j.str("reason"), source = "Gemini",
                ).takeIf { it.title.isNotBlank() }
            } catch (e: ApiException) {
                lastErr = e
                if (e.code != 404) throw Exception(if (e.code == 400 || e.code == 403) "Gemini rejected the key – check it in Settings → Discovery sources" else "Gemini error ${e.code}")
            }
        }
        throw lastErr ?: Exception("Gemini didn't answer")
    }

    /** trace.moe recognises anime scenes from a single frame, no key needed. */
    suspend fun askTraceMoe(frame: Bitmap): ClipGuess? {
        val req = Request.Builder().url("https://api.trace.moe/search?anilistInfo&cutBorders")
            .post(jpeg(frame, 90).toRequestBody("image/jpeg".toMediaType())).build()
        val r = JSONObject(Http.exec(client, req)).arr("result").objects().firstOrNull() ?: return null
        val sim = r.dbl("similarity")
        if (sim < 0.87) return null
        val a = r.obj("anilist")
        val t = a?.obj("title")
        val title = t?.str("english")?.ifEmpty { null } ?: t?.str("romaji").orEmpty()
        return ClipGuess(title, null, true, null, r.optInt("episode").takeIf { it > 0 }, (sim * 100).toInt(),
            "Matched an anime scene", "trace.moe")
    }

    /** Turns an IMDb or TMDB link into a title to search for. */
    suspend fun titleFromLink(link: String): Pair<String, Boolean?>? {
        val imdb = Regex("tt\\d{5,10}").find(link)?.value
        val tmdb = Regex("themoviedb\\.org/(movie|tv)/(\\d+)").find(link)
        if (tmdb != null) {
            val tv = tmdb.groupValues[1] == "tv"
            if (MediaExtras.hasTmdb) {
                val o = Discover.tmdb("${tmdb.groupValues[1]}/${tmdb.groupValues[2]}")
                return (o.str("title").ifEmpty { o.str("name") }) to tv
            }
            val slug = link.substringAfter(tmdb.groupValues[2]).trimStart('-').substringBefore('?').substringBefore('/').replace('-', ' ')
            if (slug.isNotBlank()) return slug to tv
        }
        if (imdb != null) {
            if (MediaExtras.hasTmdb) {
                val f = Discover.tmdb("find/$imdb", mapOf("external_source" to "imdb_id"))
                f.arr("movie_results").objects().firstOrNull()?.let { return it.str("title") to false }
                f.arr("tv_results").objects().firstOrNull()?.let { return it.str("name") to true }
            }
            ConfigStore.services.firstOrNull { it.type == com.arrmada360.app.data.ServiceType.RADARR }?.let { c ->
                try { ArrApi(c).lookup("imdb:$imdb").firstOrNull()?.let { return it.title to false } } catch (e: Exception) { }
            }
            ConfigStore.services.firstOrNull { it.type == com.arrmada360.app.data.ServiceType.SONARR }?.let { c ->
                try { ArrApi(c).lookup("imdb:$imdb").firstOrNull()?.let { return it.title to true } } catch (e: Exception) { }
            }
        }
        return null
    }

    fun isTitleLink(s: String) = Regex("imdb\\.com/title/tt\\d+|themoviedb\\.org/(movie|tv)/\\d+").containsMatchIn(s)
}

@Suppress("unused") private fun keep(i: Int) = i
