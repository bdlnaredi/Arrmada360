package com.arrmada360.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.arrmada360.app.MainActivity
import com.arrmada360.app.R
import com.arrmada360.app.api.ArrApi
import com.arrmada360.app.api.DownloadClients
import com.arrmada360.app.api.SeerrApi
import com.arrmada360.app.data.ConfigStore
import com.arrmada360.app.data.Pref
import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.data.ServiceType
import java.util.concurrent.TimeUnit

object Notifier {
    private const val CHANNEL = "arrmada360_media"
    private const val WORK = "arrmada360-notify"

    fun schedule(ctx: Context) {
        val wm = WorkManager.getInstance(ctx)
        if (!ConfigStore.flag(Pref.NOTIFY)) { wm.cancelUniqueWork(WORK); return }
        val req = PeriodicWorkRequestBuilder<NotifyWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun checkNow(ctx: Context) {
        WorkManager.getInstance(ctx).enqueue(OneTimeWorkRequestBuilder<NotifyWorker>().build())
    }

    private fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL) == null) {
                nm.createNotificationChannel(NotificationChannel(CHANNEL, "New downloads & requests", NotificationManager.IMPORTANCE_DEFAULT))
            }
        }
    }

    fun canPost(ctx: Context): Boolean = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun post(ctx: Context, id: Int, title: String, text: String, serviceId: String?, link: String? = null) {
        if (!canPost(ctx)) return
        ensureChannel(ctx)
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (link != null) data = android.net.Uri.parse(link)
            else if (serviceId != null) data = android.net.Uri.parse("arrmada360://service/$serviceId")
        }
        val pi = PendingIntent.getActivity(ctx, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        try { NotificationManagerCompat.from(ctx).notify(id, n) } catch (e: SecurityException) { /* permission revoked */ }
    }
}

/** Runs every ~15 minutes: posts a notification for anything new since the last check. */
class NotifyWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        ConfigStore.init(applicationContext)
        if (!ConfigStore.flag(Pref.NOTIFY)) return Result.success()
        ConfigStore.services.toList().forEach { c ->
            try { check(c) } catch (e: Exception) { /* server offline – try again next time */ }
        }
        try { checkUpdate() } catch (e: Exception) { }
        return Result.success()
    }

    /** At most once every 12 hours: tell the user a new Arrmada360 build is out. */
    private suspend fun checkUpdate() {
        if (!ConfigStore.flag(Pref.AUTO_UPDATE_CHECK)) return
        val now = System.currentTimeMillis()
        if (now - ConfigStore.getString("updateWorkerCheck").toLongOrNull().let { it ?: 0L } < 12 * 3600_000L) return
        ConfigStore.putString("updateWorkerCheck", now.toString())
        val info = com.arrmada360.app.api.Updates.latest()
        val current = com.arrmada360.app.api.Updates.currentBuild(applicationContext)
        if (info.build > current && ConfigStore.getInt("updateNotified", 0) < info.build) {
            ConfigStore.putInt("updateNotified", info.build)
            Notifier.post(applicationContext, 360_360, "Arrmada360 update available", "Build #${info.build} is ready – tap to see what's new and install.", null, "arrmada360://updates")
        }
    }

    private suspend fun check(c: ServiceConfig) {
        val key = "seen_" + c.id
        val seen = ConfigStore.getString(key)
        when {
            c.type.isArr -> {
                val pref = when (c.type) {
                    ServiceType.RADARR -> Pref.NOTIFY_RADARR; ServiceType.SONARR -> Pref.NOTIFY_SONARR
                    ServiceType.LIDARR -> Pref.NOTIFY_LIDARR; else -> Pref.NOTIFY_READARR
                }
                if (!ConfigStore.flag(pref)) return
                val imports = ArrApi(c).history().filter { it.event.contains("Import", true) }
                val newest = imports.maxOfOrNull { it.date } ?: return
                if (seen.isNotEmpty()) {
                    val fresh = imports.filter { it.date > seen }.distinctBy { it.parentTitle + it.detail }
                    fresh.take(5).forEachIndexed { i, h ->
                        val what = when (c.type) { ServiceType.RADARR -> "New movie"; ServiceType.SONARR -> "New episode"; ServiceType.LIDARR -> "New music"; else -> "New book" }
                        Notifier.post(applicationContext, (c.id + h.date).hashCode() + i, "$what downloaded · ${c.name}",
                            listOf(h.parentTitle, h.detail).filter { it.isNotEmpty() }.joinToString(" – ").ifEmpty { h.title }, c.id)
                    }
                    if (fresh.size > 5) Notifier.post(applicationContext, c.id.hashCode(), c.name, "…and ${fresh.size - 5} more downloaded", c.id)
                }
                ConfigStore.putString(key, newest)
            }
            c.type == ServiceType.SEERR -> {
                if (!ConfigStore.flag(Pref.NOTIFY_SEERR)) return
                val reqs = SeerrApi(c).requests("all")
                val newestId = reqs.maxOfOrNull { it.id } ?: return
                val seenId = seen.toIntOrNull()
                if (seenId != null) {
                    reqs.filter { it.id > seenId }.take(5).forEach { r ->
                        Notifier.post(applicationContext, (c.id + r.id).hashCode(), "New request · ${c.name}",
                            "${r.title} – requested by ${r.requestedBy}" + if (r.status == 1) " (needs approval)" else "", c.id)
                    }
                }
                ConfigStore.putString(key, newestId.toString())
            }
            c.type.isDownloader -> {
                if (!ConfigStore.flag(Pref.NOTIFY_DOWNLOADS)) return
                val client = DownloadClients.of(c) ?: return
                val doneNow = client.snapshot().items.filter { it.done }
                val ids = doneNow.map { it.id }.toSet()
                val setKey = "done_" + c.id
                val before = ConfigStore.getStringSet(setKey)
                if (ConfigStore.getString(key).isNotEmpty()) {
                    doneNow.filter { it.id !in before }.take(5).forEach { d ->
                        Notifier.post(applicationContext, (c.id + d.id).hashCode(), "Download finished · ${c.name}", d.name, c.id)
                    }
                }
                ConfigStore.putStringSet(setKey, ids)
                ConfigStore.putString(key, "1")
            }
        }
    }
}
