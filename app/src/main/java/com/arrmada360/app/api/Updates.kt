package com.arrmada360.app.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.arrmada360.app.net.ApiException
import com.arrmada360.app.net.Http
import com.arrmada360.app.net.arr
import com.arrmada360.app.net.long
import com.arrmada360.app.net.objects
import com.arrmada360.app.net.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.io.File

data class UpdateInfo(
    val build: Int,
    val title: String,
    val apkUrl: String,
    val size: Long,
    val published: String,
    val pageUrl: String,
)

/** Checks the GitHub "latest" release of this repo and installs newer builds. */
object Updates {
    const val REPO = "bdlnaredi/Arrmada360"
    private const val API = "https://api.github.com/repos/$REPO/releases/tags/latest"
    private const val CHANGELOG = "https://raw.githubusercontent.com/$REPO/main/CHANGELOG.md"

    fun currentBuild(ctx: Context): Int = try {
        PackageInfoCompat.getLongVersionCode(ctx.packageManager.getPackageInfo(ctx.packageName, 0)).toInt()
    } catch (e: Exception) { 0 }

    fun currentVersionName(ctx: Context): String = try {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?"
    } catch (e: Exception) { "?" }

    suspend fun latest(): UpdateInfo {
        val req = Request.Builder().url(API).header("Accept", "application/vnd.github+json").build()
        val o = try { JSONObject(Http.exec(Http.client(false), req)) } catch (e: ApiException) {
            if (e.code == 404) throw Exception("No release published yet") else throw e
        }
        val title = o.str("name")
        val build = Regex("#(\\d+)").find(title)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("(\\d+)\\s*$").find(o.str("body"))?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val apk = o.arr("assets").objects().firstOrNull { it.str("name").endsWith(".apk") } ?: throw Exception("The latest release has no APK yet")
        return UpdateInfo(build, title, apk.str("browser_download_url"), apk.long("size"), o.str("published_at").ifEmpty { apk.str("updated_at") }, o.str("html_url"))
    }

    suspend fun changelog(): String = Http.exec(Http.client(false), Request.Builder().url(CHANGELOG).build())

    /** The CHANGELOG sections for builds newer than [current]. */
    fun notesSince(changelog: String, current: Int): String {
        val sections = changelog.split(Regex("(?m)^## ")).drop(1)
        return sections.filter { sec ->
            val n = Regex("^\\[1\\.0\\.(\\d+)]").find(sec)?.groupValues?.get(1)?.toIntOrNull()
            n == null && sec.startsWith("[Unreleased") || (n != null && n > current)
        }.joinToString("\n") { "## $it" }.trim()
    }

    suspend fun download(ctx: Context, info: UpdateInfo, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(ctx.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val out = File(dir, "Arrmada360-${info.build}.apk")
        Http.client(false).newCall(Request.Builder().url(info.apkUrl).build()).execute().use { r ->
            if (!r.isSuccessful) throw ApiException(r.code, "")
            val body = r.body ?: throw Exception("Empty download")
            val total = body.contentLength().takeIf { it > 0 } ?: info.size
            body.byteStream().use { input ->
                out.outputStream().use { os ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        os.write(buf, 0, n)
                        done += n
                        if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
        }
        out
    }

    /** Android must allow this app to install packages; returns false (and opens the setting) if not. */
    fun canInstall(ctx: Context): Boolean = Build.VERSION.SDK_INT < 26 || ctx.packageManager.canRequestPackageInstalls()

    fun openInstallPermission(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            ctx.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun install(ctx: Context, apk: File) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", apk)
        val i = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
    }
}
