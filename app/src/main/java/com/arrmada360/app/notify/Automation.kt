package com.arrmada360.app.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.arrmada360.app.api.DlItem
import com.arrmada360.app.api.DownloadClients
import com.arrmada360.app.api.liveMetrics
import com.arrmada360.app.data.ConfigStore
import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.data.ServiceType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Day/night speed limits applied to every download client. */
object SpeedSchedule {
    var enabled: Boolean
        get() = ConfigStore.flag("speedSched")
        set(v) = ConfigStore.setFlag("speedSched", v)
    /** Minutes after midnight when the limited period starts / ends. */
    var startMin: Int
        get() = ConfigStore.getInt("speedSchedStart", 9 * 60)
        set(v) = ConfigStore.putInt("speedSchedStart", v)
    var endMin: Int
        get() = ConfigStore.getInt("speedSchedEnd", 23 * 60)
        set(v) = ConfigStore.putInt("speedSchedEnd", v)
    var limitDown: Int
        get() = ConfigStore.getInt("speedSchedDown", 2048)
        set(v) = ConfigStore.putInt("speedSchedDown", v)
    var limitUp: Int
        get() = ConfigStore.getInt("speedSchedUp", 512)
        set(v) = ConfigStore.putInt("speedSchedUp", v)
    var fullDown: Int
        get() = ConfigStore.getInt("speedSchedFullDown", 0)
        set(v) = ConfigStore.putInt("speedSchedFullDown", v)
    var fullUp: Int
        get() = ConfigStore.getInt("speedSchedFullUp", 0)
        set(v) = ConfigStore.putInt("speedSchedFullUp", v)
    /** Client ids the schedule applies to (empty = all that support it). */
    var clients: Set<String>
        get() = ConfigStore.getStringSet("speedSchedClients")
        set(v) = ConfigStore.putStringSet("speedSchedClients", v)

    fun inLimitedWindow(now: LocalTime = LocalTime.now()): Boolean {
        val m = now.hour * 60 + now.minute
        return if (startMin <= endMin) m in startMin until endMin else (m >= startMin || m < endMin)
    }

    fun targets(): List<ServiceConfig> = ConfigStore.services.filter { it.type.isDownloader }
        .filter { clients.isEmpty() || it.id in clients }
        .filter { DownloadClients.of(it)?.supportsSpeedLimit == true }

    /** Applies the right limits now. Returns (limited?, clients changed). */
    suspend fun applyNow(force: Boolean = false): Pair<Boolean, Int> = coroutineScope {
        val limited = inLimitedWindow()
        val mode = if (limited) "limited" else "full"
        if (!force && ConfigStore.getString("speedSchedLast") == mode) return@coroutineScope limited to 0
        val (d, u) = if (limited) limitDown to limitUp else fullDown to fullUp
        val n = targets().map { c -> async { try { DownloadClients.of(c)?.setSpeedLimits(d.toLong(), u.toLong()); 1 } catch (e: Exception) { 0 } } }.awaitAll().sum()
        if (n > 0) ConfigStore.putString("speedSchedLast", mode)
        limited to n
    }

    fun fmt(min: Int) = "%02d:%02d".format(min / 60, min % 60)
}

/** Automatic clean-up of finished torrents by ratio or seeding time. */
object SeedRules {
    var autoClean: Boolean
        get() = ConfigStore.flag("seedAuto")
        set(v) = ConfigStore.setFlag("seedAuto", v)
    var minRatio: Float
        get() = ConfigStore.getInt("seedRatioX10", 20) / 10f
        set(v) = ConfigStore.putInt("seedRatioX10", (v * 10).toInt())
    var minDays: Int
        get() = ConfigStore.getInt("seedDays", 14)
        set(v) = ConfigStore.putInt("seedDays", v)
    /** "ratio", "days" or "either" */
    var mode: String
        get() = ConfigStore.getString("seedMode").ifEmpty { "either" }
        set(v) = ConfigStore.putString("seedMode", v)
    var deleteData: Boolean
        get() = ConfigStore.flag("seedDeleteData")
        set(v) = ConfigStore.setFlag("seedDeleteData", v)
    /** Only touch torrents in these categories (empty = every category). */
    var categories: String
        get() = ConfigStore.getString("seedCats")
        set(v) = ConfigStore.putString("seedCats", v)

    val torrentTypes = setOf(ServiceType.QBITTORRENT, ServiceType.TRANSMISSION, ServiceType.DELUGE, ServiceType.RUTORRENT)

    fun matches(d: DlItem): Boolean {
        if (!d.done) return false
        val cats = categories.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (cats.isNotEmpty() && cats.none { it.equals(d.category, true) }) return false
        val r = d.ratio >= 0 && d.ratio >= minRatio
        val t = d.seedTime >= 0 && d.seedTime >= minDays * 86400L
        return when (mode) { "ratio" -> r; "days" -> t; else -> r || t }
    }
}

class AutomationWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    companion object {
        private const val NAME = "arrmada360-automation"
        fun schedule(ctx: Context) {
            val need = SpeedSchedule.enabled || SeedRules.autoClean || ConfigStore.services.any { it.type == ServiceType.UNRAID }
            val wm = WorkManager.getInstance(ctx)
            if (!need) { wm.cancelUniqueWork(NAME); return }
            wm.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<AutomationWorker>(15, TimeUnit.MINUTES).build())
        }
    }

    override suspend fun doWork(): Result {
        ConfigStore.init(applicationContext)
        com.arrmada360.app.net.NetSwitch.start(applicationContext)
        if (SpeedSchedule.enabled) try { SpeedSchedule.applyNow() } catch (e: Exception) { }
        if (SeedRules.autoClean) try { clean() } catch (e: Exception) { }
        ConfigStore.services.filter { it.type == ServiceType.UNRAID }.forEach { c ->
            try { com.arrmada360.app.api.UnraidHistory.add(c, com.arrmada360.app.api.UnraidApi(c).liveMetrics()) } catch (e: Exception) { }
        }
        return Result.success()
    }

    private suspend fun clean() = coroutineScope {
        val clients = ConfigStore.services.filter { it.type in SeedRules.torrentTypes }
        val removed = clients.map { c ->
            async {
                try {
                    val dc = DownloadClients.of(c) ?: return@async emptyList<String>()
                    val hit = dc.snapshot().items.filter { SeedRules.matches(it) }
                    if (hit.isNotEmpty()) dc.delete(hit.map { it.id }, SeedRules.deleteData)
                    hit.map { it.name }
                } catch (e: Exception) { emptyList() }
            }
        }.awaitAll().flatten()
        if (removed.isNotEmpty()) Notifier.post(applicationContext, 777001, "Seeding manager removed ${removed.size} torrent${if (removed.size == 1) "" else "s"}",
            removed.take(6).joinToString("\n") + if (removed.size > 6) "\n…" else "", null)
    }
}

/** One notification every morning: what downloaded, what's on today and anything broken. */
object DailySummary {
    private const val NAME = "arrmada360-daily-summary"
    var enabled: Boolean
        get() = ConfigStore.flag("dailySummary")
        set(v) = ConfigStore.setFlag("dailySummary", v)
    var atMin: Int
        get() = ConfigStore.getInt("dailySummaryAt", 9 * 60)
        set(v) = ConfigStore.putInt("dailySummaryAt", v)

    fun schedule(ctx: Context) {
        val wm = WorkManager.getInstance(ctx)
        if (!enabled) { wm.cancelUniqueWork(NAME); return }
        val now = java.time.LocalDateTime.now()
        var next = now.toLocalDate().atTime(atMin / 60, atMin % 60)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = java.time.Duration.between(now, next).toMinutes()
        wm.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<DailySummaryWorker>(24, TimeUnit.HOURS).setInitialDelay(delay, TimeUnit.MINUTES).build())
    }

    suspend fun build(): Pair<String, String> = coroutineScope {
        val arrs = ConfigStore.services.filter { (it.type == ServiceType.SONARR || it.type == ServiceType.RADARR) }
        val since = java.time.Instant.now().minusSeconds(24 * 3600)
        val imports = arrs.map { c ->
            async {
                try {
                    com.arrmada360.app.api.ArrApi(c).history().filter { h ->
                        h.event.contains("import", true) && try { java.time.OffsetDateTime.parse(h.date).toInstant().isAfter(since) } catch (e: Exception) { false }
                    }.map { listOf(it.parentTitle, it.detail).filter { s -> s.isNotEmpty() }.joinToString(" – ").ifEmpty { it.title } }
                } catch (e: Exception) { emptyList() }
            }
        }
        val today = java.time.LocalDate.now()
        val airing = arrs.map { c ->
            async {
                try {
                    com.arrmada360.app.api.ArrApi(c).calendar(0, 1).filter { e ->
                        val d = try { if (e.date.length > 10) java.time.OffsetDateTime.parse(e.date).atZoneSameInstant(java.time.ZoneId.systemDefault()).toLocalDate() else java.time.LocalDate.parse(e.date.take(10)) } catch (x: Exception) { null }
                        d == today
                    }.map { "${it.title} ${it.subtitle}".trim() }
                } catch (e: Exception) { emptyList() }
            }
        }
        val health = (arrs + ConfigStore.services.filter { it.type == ServiceType.PROWLARR || it.type == ServiceType.LIDARR || it.type == ServiceType.READARR }).map { c ->
            async { try { com.arrmada360.app.api.ArrApi(c).healthIssues().filter { it.type.equals("error", true) || it.type.equals("warning", true) }.size.let { if (it > 0) "${c.name}: $it health warning${if (it == 1) "" else "s"}" else null } } catch (e: Exception) { "${c.name} is offline" } }
        }
        val kuma = ConfigStore.services.filter { it.type == ServiceType.UPTIME_KUMA }.map { c ->
            async { try { com.arrmada360.app.api.KumaApi(c).monitors().filter { it.status == 0 }.map { "${it.name} is down" } } catch (e: Exception) { emptyList() } }
        }
        val disks = arrs.map { c -> async { try { com.arrmada360.app.api.ArrApi(c).diskSpace() } catch (e: Exception) { emptyList() } } }
        val got = imports.awaitAll().flatten().distinct()
        val on = airing.awaitAll().flatten().distinct()
        val problems = (health.awaitAll().filterNotNull() + kuma.awaitAll().flatten()).toMutableList()
        disks.awaitAll().flatten().distinctBy { it.path }.filter { it.total > 0 && it.free.toDouble() / it.total < 0.1 }.forEach {
            problems += "${it.path} only ${com.arrmada360.app.ui.humanBytes(it.free)} free"
        }
        val title = buildString {
            append("Good morning · ")
            append(if (got.isEmpty()) "nothing new" else "${got.size} new download${if (got.size == 1) "" else "s"}")
            if (on.isNotEmpty()) append(" · ${on.size} airing today")
            if (problems.isNotEmpty()) append(" · ${problems.size} issue${if (problems.size == 1) "" else "s"}")
        }
        val body = buildString {
            if (got.isNotEmpty()) { append("Downloaded:\n"); got.take(8).forEach { append("• ").append(it).append('\n') }; if (got.size > 8) append("…and ${got.size - 8} more\n") }
            if (on.isNotEmpty()) { append("Today:\n"); on.take(6).forEach { append("• ").append(it).append('\n') } }
            if (problems.isNotEmpty()) { append("Needs attention:\n"); problems.take(6).forEach { append("⚠ ").append(it).append('\n') } }
            if (isEmpty()) append("All quiet on your server.")
        }.trim()
        title to body
    }
}

class DailySummaryWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        ConfigStore.init(applicationContext)
        if (!DailySummary.enabled) return Result.success()
        com.arrmada360.app.net.NetSwitch.start(applicationContext)
        val (t, b) = try { DailySummary.build() } catch (e: Exception) { return Result.retry() }
        Notifier.post(applicationContext, 777100, t, b, null, link = "arrmada360://dashboard",
            actions = listOf(Notifier.Action("Calendar", Notifier.linkIntent(applicationContext, 777101, "arrmada360://calendar"))))
        return Result.success()
    }
}
