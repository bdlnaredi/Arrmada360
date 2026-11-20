package com.arrmada360.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.arrmada360.app.MainActivity
import com.arrmada360.app.R
import com.arrmada360.app.api.ArrApi
import com.arrmada360.app.api.DownloadClients
import com.arrmada360.app.data.ConfigStore
import com.arrmada360.app.data.ServiceType
import com.arrmada360.app.ui.humanBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

object Widgets {
    const val ACTION_REFRESH = "com.arrmada360.app.WIDGET_REFRESH"
    const val ACTION_TOGGLE_PAUSE = "com.arrmada360.app.WIDGET_TOGGLE_PAUSE"
    private const val WORK = "arrmada360-widgets"
    private const val PERIODIC = "arrmada360-widgets-periodic"

    fun anyInstalled(ctx: Context): Boolean {
        val m = AppWidgetManager.getInstance(ctx)
        return listOf(DownloadsWidget::class.java, UpcomingWidget::class.java, ActionsWidget::class.java)
            .any { m.getAppWidgetIds(ComponentName(ctx, it)).isNotEmpty() }
    }

    fun refresh(ctx: Context) {
        if (!anyInstalled(ctx)) return
        WorkManager.getInstance(ctx).enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<WidgetWorker>().build())
    }

    fun schedule(ctx: Context) {
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WidgetWorker>(30, TimeUnit.MINUTES).build())
    }

    fun cancelIfNone(ctx: Context) { if (!anyInstalled(ctx)) WorkManager.getInstance(ctx).cancelUniqueWork(PERIODIC) }

    fun link(ctx: Context, uri: String, req: Int): PendingIntent =
        PendingIntent.getActivity(ctx, req, Intent(ctx, MainActivity::class.java).setAction(Intent.ACTION_VIEW).setData(Uri.parse(uri))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    fun broadcast(ctx: Context, action: String, req: Int): PendingIntent =
        PendingIntent.getBroadcast(ctx, req, Intent(ctx, WidgetActionReceiver::class.java).setAction(action), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    /** Pause or resume every download client. Returns how many clients were changed. */
    suspend fun setAllPaused(paused: Boolean): Int = coroutineScope {
        ConfigStore.services.filter { it.type.isDownloader }.map { c ->
            async { try { DownloadClients.of(c)?.let { if (paused) it.pauseAll() else it.resumeAll() }; 1 } catch (e: Exception) { 0 } }
        }.awaitAll().sum().also { ConfigStore.setFlag("allDownloadsPaused", paused) }
    }

    val allPaused: Boolean get() = ConfigStore.flag("allDownloadsPaused")

    fun speed(bps: Long): String = if (ConfigStore.flag(com.arrmada360.app.data.Pref.SPEED_BITS)) "%.1f Mbps".format(bps * 8 / 1_000_000.0) else humanBytes(bps) + "/s"
}

abstract class BaseWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        ConfigStore.init(context.applicationContext)
        appWidgetIds.forEach { id -> appWidgetManager.updateAppWidget(id, skeleton(context)) }
        Widgets.refresh(context)
    }
    override fun onEnabled(context: Context) { Widgets.schedule(context) }
    override fun onDisabled(context: Context) { Widgets.cancelIfNone(context) }
    abstract fun skeleton(ctx: Context): RemoteViews
}

class DownloadsWidget : BaseWidget() {
    override fun skeleton(ctx: Context) = RemoteViews(ctx.packageName, R.layout.widget_downloads).also { bindDownloadClicks(ctx, it) }
}

class UpcomingWidget : BaseWidget() {
    override fun skeleton(ctx: Context) = RemoteViews(ctx.packageName, R.layout.widget_upcoming).also {
        it.setOnClickPendingIntent(R.id.u_refresh, Widgets.broadcast(ctx, Widgets.ACTION_REFRESH, 11))
        it.setOnClickPendingIntent(R.id.u_root, Widgets.link(ctx, "arrmada360://calendar", 12))
    }
}

class ActionsWidget : BaseWidget() {
    override fun skeleton(ctx: Context) = RemoteViews(ctx.packageName, R.layout.widget_actions).also { v ->
        v.setOnClickPendingIntent(R.id.a_movies, Widgets.link(ctx, "arrmada360://movies", 21))
        v.setOnClickPendingIntent(R.id.a_tv, Widgets.link(ctx, "arrmada360://tv", 22))
        v.setOnClickPendingIntent(R.id.a_search, Widgets.link(ctx, "arrmada360://search", 23))
        val dl = ConfigStore.services.firstOrNull { it.type.isDownloader }
        v.setOnClickPendingIntent(R.id.a_downloads, Widgets.link(ctx, if (dl != null) "arrmada360://service/${dl.id}" else "arrmada360://server", 24))
    }
}

private fun bindDownloadClicks(ctx: Context, v: RemoteViews) {
    v.setOnClickPendingIntent(R.id.w_refresh, Widgets.broadcast(ctx, Widgets.ACTION_REFRESH, 1))
    v.setOnClickPendingIntent(R.id.w_pause, Widgets.broadcast(ctx, Widgets.ACTION_TOGGLE_PAUSE, 2))
    val dl = ConfigStore.services.firstOrNull { it.type.isDownloader }
    v.setOnClickPendingIntent(R.id.w_root, Widgets.link(ctx, if (dl != null) "arrmada360://service/${dl.id}" else "arrmada360://server", 3))
    v.setImageViewResource(R.id.w_pause, if (Widgets.allPaused) R.drawable.ic_w_play else R.drawable.ic_w_pause)
}

class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ConfigStore.init(context.applicationContext)
        when (intent.action) {
            Widgets.ACTION_REFRESH -> Widgets.refresh(context)
            Widgets.ACTION_TOGGLE_PAUSE -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try { Widgets.setAllPaused(!Widgets.allPaused) } finally {
                        Widgets.refresh(context)
                        QuickTiles.requestUpdate(context)
                        pending.finish()
                    }
                }
            }
        }
    }
}

class WidgetWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    private val dayFmt = DateTimeFormatter.ofPattern("EEE d")

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        ConfigStore.init(ctx)
        com.arrmada360.app.net.NetSwitch.start(ctx)
        val m = AppWidgetManager.getInstance(ctx)
        val dlIds = m.getAppWidgetIds(ComponentName(ctx, DownloadsWidget::class.java))
        val upIds = m.getAppWidgetIds(ComponentName(ctx, UpcomingWidget::class.java))
        val actIds = m.getAppWidgetIds(ComponentName(ctx, ActionsWidget::class.java))
        if (actIds.isNotEmpty()) { val v = ActionsWidget().skeleton(ctx); actIds.forEach { m.updateAppWidget(it, v) } }
        if (dlIds.isNotEmpty()) { val v = downloads(ctx); dlIds.forEach { m.updateAppWidget(it, v) } }
        if (upIds.isNotEmpty()) { val v = upcoming(ctx); upIds.forEach { m.updateAppWidget(it, v) } }
        return Result.success()
    }

    private suspend fun downloads(ctx: Context): RemoteViews = coroutineScope {
        val v = RemoteViews(ctx.packageName, R.layout.widget_downloads)
        bindDownloadClicks(ctx, v)
        val clients = ConfigStore.services.filter { it.type.isDownloader }
        if (clients.isEmpty()) { v.setTextViewText(R.id.w_status, "Add a download client in Arrmada360"); return@coroutineScope v }
        val snaps = clients.map { c -> async { try { DownloadClients.of(c)?.snapshot() } catch (e: Exception) { null } } }.awaitAll()
        val ok = snaps.filterNotNull()
        val items = ok.flatMap { it.items }
        val active = items.filter { !it.done && !it.paused }.sortedByDescending { it.dlSpeed }
        v.setTextViewText(R.id.w_down, "↓ " + Widgets.speed(ok.sumOf { it.dlSpeed }))
        v.setTextViewText(R.id.w_up, "↑ " + Widgets.speed(ok.sumOf { it.upSpeed }))
        val offline = snaps.count { it == null }
        v.setTextViewText(R.id.w_status, buildString {
            append("${active.size} active · ${items.size} total")
            if (offline > 0) append(" · $offline offline")
            if (Widgets.allPaused) append(" · paused")
            append(" · ").append(java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")))
        })
        val rows = listOf(R.id.w_row0, R.id.w_row1, R.id.w_row2)
        val names = listOf(R.id.w_name0, R.id.w_name1, R.id.w_name2)
        val metas = listOf(R.id.w_meta0, R.id.w_meta1, R.id.w_meta2)
        val progs = listOf(R.id.w_prog0, R.id.w_prog1, R.id.w_prog2)
        for (i in 0 until 3) {
            val d = active.getOrNull(i)
            v.setViewVisibility(rows[i], if (d == null) View.GONE else View.VISIBLE)
            if (d != null) {
                v.setTextViewText(names[i], d.name)
                v.setTextViewText(metas[i], "${(d.progress * 100).toInt()}% · ${Widgets.speed(d.dlSpeed)}")
                v.setProgressBar(progs[i], 1000, (d.progress * 1000).toInt(), false)
            }
        }
        v
    }

    private suspend fun upcoming(ctx: Context): RemoteViews = coroutineScope {
        val v = RemoteViews(ctx.packageName, R.layout.widget_upcoming)
        v.setOnClickPendingIntent(R.id.u_refresh, Widgets.broadcast(ctx, Widgets.ACTION_REFRESH, 11))
        v.setOnClickPendingIntent(R.id.u_root, Widgets.link(ctx, "arrmada360://calendar", 12))
        val arrs = ConfigStore.services.filter { it.type == ServiceType.SONARR || it.type == ServiceType.RADARR }
        if (arrs.isEmpty()) { v.setTextViewText(R.id.u_status, "Add Sonarr or Radarr in Arrmada360"); return@coroutineScope v }
        val today = LocalDate.now()
        val list = arrs.map { c -> async { try { ArrApi(c).calendar(0, 14) } catch (e: Exception) { emptyList() } } }.awaitAll().flatten()
            .mapNotNull { e -> parse(e.date)?.let { it to e } }
            .filter { !it.first.isBefore(today) }
            .sortedBy { it.first }
        v.setTextViewText(R.id.u_status, if (list.isEmpty()) "Nothing in the next 2 weeks" else "${list.size} in the next 2 weeks")
        val rows = listOf(R.id.u_row0, R.id.u_row1, R.id.u_row2, R.id.u_row3, R.id.u_row4)
        val days = listOf(R.id.u_day0, R.id.u_day1, R.id.u_day2, R.id.u_day3, R.id.u_day4)
        val titles = listOf(R.id.u_title0, R.id.u_title1, R.id.u_title2, R.id.u_title3, R.id.u_title4)
        val subs = listOf(R.id.u_sub0, R.id.u_sub1, R.id.u_sub2, R.id.u_sub3, R.id.u_sub4)
        for (i in 0 until 5) {
            val p = list.getOrNull(i)
            v.setViewVisibility(rows[i], if (p == null) View.GONE else View.VISIBLE)
            if (p != null) {
                val (d, e) = p
                v.setTextViewText(days[i], when (d) { today -> "Today"; today.plusDays(1) -> "Tomorrow"; else -> d.format(dayFmt) })
                v.setTextViewText(titles[i], e.title)
                v.setTextViewText(subs[i], e.subtitle)
            }
        }
        v
    }

    private fun parse(s: String): LocalDate? = try {
        if (s.length > 10) java.time.OffsetDateTime.parse(s).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate() else LocalDate.parse(s.take(10))
    } catch (e: Exception) { try { LocalDate.parse(s.take(10)) } catch (e2: Exception) { null } }
}
