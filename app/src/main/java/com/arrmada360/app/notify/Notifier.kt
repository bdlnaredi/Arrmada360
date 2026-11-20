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
import kotlinx.coroutines.launch

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

    data class Action(val label: String, val intent: PendingIntent)

    /** Downloads an image for a notification (runs on the worker's background thread). */
    fun bitmap(url: String?): android.graphics.Bitmap? {
        if (url.isNullOrEmpty()) return null
        return try {
            com.arrmada360.app.net.Http.images.newCall(okhttp3.Request.Builder().url(url).build()).execute().use { r ->
                if (!r.isSuccessful) null else r.body?.byteStream()?.use { android.graphics.BitmapFactory.decodeStream(it) }
            }
        } catch (e: Exception) { null }
    }

    fun actionIntent(ctx: Context, req: Int, action: String, svc: String, arg: String, notifId: Int): PendingIntent =
        PendingIntent.getBroadcast(ctx, req, Intent(ctx, NotifActionReceiver::class.java).setAction(action)
            .putExtra("svc", svc).putExtra("arg", arg).putExtra("nid", notifId), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    fun linkIntent(ctx: Context, req: Int, link: String): PendingIntent =
        PendingIntent.getActivity(ctx, req, Intent(ctx, MainActivity::class.java).setAction(Intent.ACTION_VIEW).setData(android.net.Uri.parse(link))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    fun post(ctx: Context, id: Int, title: String, text: String, serviceId: String?, link: String? = null,
             poster: String? = null, backdrop: String? = null, actions: List<Action> = emptyList(), color: Int? = null) {
        if (!canPost(ctx)) return
        ensureChannel(ctx)
        val large = bitmap(poster)
        val big = bitmap(backdrop)
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (link != null) data = android.net.Uri.parse(link)
            else if (serviceId != null) data = android.net.Uri.parse("arrmada360://service/$serviceId")
        }
        val pi = PendingIntent.getActivity(ctx, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val b = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pi)
        if (color != null) b.setColor(color)
        if (large != null) b.setLargeIcon(large)
        if (big != null) b.setStyle(NotificationCompat.BigPictureStyle().bigPicture(big).setSummaryText(text).also { s -> if (large != null) s.bigLargeIcon(null as android.graphics.Bitmap?) })
        else b.setStyle(NotificationCompat.BigTextStyle().bigText(text))
        actions.take(3).forEach { a -> b.addAction(0, a.label, a.intent) }
        val n = b.build()
        try { NotificationManagerCompat.from(ctx).notify(id, n) } catch (e: SecurityException) { /* permission revoked */ }
    }
}

/** Runs every ~15 minutes: posts a notification for anything new since the last check. */
class NotifyWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        ConfigStore.init(applicationContext)
        com.arrmada360.app.net.NetSwitch.start(applicationContext)
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
            c.type.isArr && c.type != ServiceType.WHISPARR -> {
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
                        val nid = (c.id + h.date).hashCode() + i
                        val detailLink = if (h.parentId > 0) "arrmada360://item/${c.id}/${h.parentId}" else "arrmada360://service/${c.id}"
                        Notifier.post(applicationContext, nid, "$what downloaded · ${c.name}",
                            listOf(h.parentTitle, h.detail).filter { it.isNotEmpty() }.joinToString(" – ").ifEmpty { h.title }, c.id,
                            link = detailLink, poster = h.poster, backdrop = h.fanart, color = c.type.color.toInt(),
                            actions = listOf(
                                Notifier.Action("Open", Notifier.linkIntent(applicationContext, nid, detailLink)),
                                Notifier.Action("Calendar", Notifier.linkIntent(applicationContext, nid + 1, "arrmada360://calendar")),
                            ))
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
                        val nid = (c.id + r.id).hashCode()
                        val acts = if (r.status == 1) listOf(
                            Notifier.Action("Approve", Notifier.actionIntent(applicationContext, nid, NotifActionReceiver.APPROVE, c.id, r.id.toString(), nid)),
                            Notifier.Action("Decline", Notifier.actionIntent(applicationContext, nid + 1, NotifActionReceiver.DECLINE, c.id, r.id.toString(), nid)),
                        ) else emptyList()
                        Notifier.post(applicationContext, nid, "New request · ${c.name}",
                            "${r.title} – requested by ${r.requestedBy}" + if (r.status == 1) " (needs approval)" else "", c.id,
                            poster = r.poster, actions = acts, color = c.type.color.toInt())
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
                        val nid = (c.id + d.id).hashCode()
                        Notifier.post(applicationContext, nid, "Download finished · ${c.name}", "${d.name}\n${com.arrmada360.app.ui.humanBytes(d.size)}" + if (d.category.isNotEmpty()) " · ${d.category}" else "", c.id,
                            color = c.type.color.toInt(),
                            actions = listOf(Notifier.Action("Open downloads", Notifier.linkIntent(applicationContext, nid, "arrmada360://service/${c.id}"))))
                    }
                }
                ConfigStore.putStringSet(setKey, ids)
                ConfigStore.putString(key, "1")
            }
        }
    }
}


/** Handles buttons on notifications (approve / decline Seerr requests) without opening the app. */
class NotifActionReceiver : android.content.BroadcastReceiver() {
    companion object {
        const val APPROVE = "com.arrmada360.app.NOTIF_APPROVE"
        const val DECLINE = "com.arrmada360.app.NOTIF_DECLINE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        ConfigStore.init(context.applicationContext)
        val cfg = ConfigStore.get(intent.getStringExtra("svc") ?: return) ?: return
        val arg = intent.getStringExtra("arg") ?: return
        val nid = intent.getIntExtra("nid", 0)
        val pending = goAsync()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val msg = try {
                com.arrmada360.app.net.NetSwitch.start(context.applicationContext)
                when (intent.action) {
                    APPROVE -> { SeerrApi(cfg).approve(arg.toInt()); "Request approved" }
                    DECLINE -> { SeerrApi(cfg).decline(arg.toInt()); "Request declined" }
                    else -> null
                }
            } catch (e: Exception) { "Failed: " + com.arrmada360.app.net.friendlyError(e) }
            try {
                if (msg != null) {
                    NotificationManagerCompat.from(context).cancel(nid)
                    Notifier.post(context, nid, msg, cfg.name, cfg.id)
                }
            } finally { pending.finish() }
        }
    }
}
