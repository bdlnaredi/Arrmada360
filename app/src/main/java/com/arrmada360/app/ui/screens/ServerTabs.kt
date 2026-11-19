package com.arrmada360.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.arrmada360.app.api.Conn
import com.arrmada360.app.api.Connections
import com.arrmada360.app.api.DownloadClients
import com.arrmada360.app.api.ProwlarrApi
import com.arrmada360.app.api.QbitClient
import com.arrmada360.app.api.SeerrApi
import com.arrmada360.app.api.TautulliApi
import com.arrmada360.app.api.searchHistory
import com.arrmada360.app.api.stats
import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.ui.Bad
import com.arrmada360.app.ui.Good
import com.arrmada360.app.ui.LoadableContent
import com.arrmada360.app.ui.MenuRow
import com.arrmada360.app.ui.Navigator
import com.arrmada360.app.ui.Panel
import com.arrmada360.app.ui.Pill
import com.arrmada360.app.ui.Poster
import com.arrmada360.app.ui.ProgressBar
import com.arrmada360.app.ui.Rainbow
import com.arrmada360.app.ui.Route
import com.arrmada360.app.ui.SectionTitle
import com.arrmada360.app.ui.StatTile
import com.arrmada360.app.ui.Warn
import com.arrmada360.app.ui.fmtDate
import com.arrmada360.app.ui.fmtEpoch
import com.arrmada360.app.ui.humanEta
import com.arrmada360.app.ui.rememberLoadable
import com.arrmada360.app.ui.rememberRunner

/** The standard Server tab: instance settings, open in browser, connection, plus app-specific rows. */
@Composable
fun GenericServerTab(cfg: ServiceConfig, nav: Navigator, extra: LazyListScope.() -> Unit = {}) {
    val uri = LocalUriHandler.current
    val conn = Connections.of(cfg.id)
    LazyColumn(contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp)) {
        item {
            MenuRow(Icons.Filled.Settings, "${cfg.name} instance settings", cfg.baseUrl, cfg.type.tint) { nav.push(Route.EditService(cfg.id, null)) }
        }
        item { MenuRow(Icons.Filled.Public, "View on web", "Open ${cfg.type.label} in your browser", cfg.type.tint2, external = true) { uri.openUri(cfg.baseUrl) } }
        item {
            MenuRow(
                Icons.Filled.Wifi, "Connection",
                when (conn) { is Conn.Ok -> "Online · ${conn.info} · ${conn.ms} ms"; is Conn.Fail -> conn.msg; is Conn.Checking -> "Checking…"; else -> "Tap to test all connections" },
                when (conn) { is Conn.Ok -> Good; is Conn.Fail -> Bad; else -> Warn },
            ) { nav.push(Route.Health) }
        }
        extra()
    }
}

// ------------------------------------------------------------ download clients
@Composable
fun DownloaderServerTab(cfg: ServiceConfig, nav: Navigator) {
    val client = DownloadClients.of(cfg)
    val run = rememberRunner()
    GenericServerTab(cfg, nav) {
        if (client != null) {
            item { SectionTitle("Actions") }
            item { MenuRow(Icons.Filled.PlayArrow, "Resume everything", null, Good, arrow = false) { run("Resumed all") { client.resumeAll() } } }
            item { MenuRow(Icons.Filled.Pause, "Pause everything", null, Warn, arrow = false) { run("Paused all") { client.pauseAll() } } }
            if (client is QbitClient) item {
                MenuRow(Icons.Filled.Speed, "Toggle alternative speed limits", "Switch between normal and slow (scheduled) limits", Rainbow[1], arrow = false) {
                    run("Speed limit mode toggled") { client.toggleAltSpeed() }
                }
            }
        }
    }
}

// ------------------------------------------------------------ Prowlarr
@Composable
fun ProwlarrStatsTab(api: ProwlarrApi) {
    val l = rememberLoadable(api.cfg, "pstats") { api.stats() }
    LoadableContent(l, empty = { it.isEmpty() }, emptyText = "No stats yet") { list ->
        val maxQ = (list.maxOfOrNull { it.queries } ?: 1).coerceAtLeast(1)
        LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Queries", list.sumOf { it.queries }.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    StatTile("Grabs", list.sumOf { it.grabs }.toString(), Good, Modifier.weight(1f))
                    StatTile("Failed", list.sumOf { it.failed }.toString(), Bad, Modifier.weight(1f))
                }
            }
            items(list) { s ->
                Panel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(s.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                            Pill("${s.avgMs} ms", if (s.avgMs < 1500) Good else Warn)
                        }
                        Spacer(Modifier.size(6.dp))
                        ProgressBar(s.queries.toFloat() / maxQ, height = 6.dp)
                        Spacer(Modifier.size(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Pill("${s.queries} queries")
                            Pill("${s.grabs} grabs", Good)
                            if (s.failed > 0) Pill("${s.failed} failed", Bad)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProwlarrHistoryTab(api: ProwlarrApi) {
    val l = rememberLoadable(api.cfg, "phist") { api.searchHistory() }
    LoadableContent(l, empty = { it.isEmpty() }, emptyText = "No history yet") { list ->
        LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(list) { h ->
                Panel(Modifier.fillMaxWidth(), tint = if (h.ok) null else Bad) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Pill(h.event, if (!h.ok) Bad else if (h.event.contains("Grab", true)) Good else MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            if (h.indexer.isNotEmpty()) Pill(h.indexer, MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.weight(1f))
                            Text(fmtDate(h.date, true), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (h.query.isNotEmpty()) Text(h.query, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 4.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------ Seerr
@Composable
fun SeerrUsersTab(api: SeerrApi) {
    val l = rememberLoadable(api.cfg, "susers") { api.users() }
    LoadableContent(l, empty = { it.isEmpty() }, emptyText = "No users") { list ->
        LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list, key = { it.id }) { u -> UserRow(u.avatar, u.name, u.email, "${u.requests} requests", Rainbow[u.id % Rainbow.size]) }
        }
    }
}

@Composable
fun SeerrServerTab(cfg: ServiceConfig, nav: Navigator) {
    val api = SeerrApi(cfg)
    val run = rememberRunner()
    val status = rememberLoadable(cfg, "sstatus") { api.statusObj() }
    val jobs = rememberLoadable(cfg, "sjobs") { api.jobs() }
    GenericServerTab(cfg, nav) {
        status.data?.let { st ->
            item {
                MenuRow(Icons.Filled.Info, "Version ${st.optString("version")}",
                    if (st.optBoolean("updateAvailable")) "Update available" else "Up to date", if (st.optBoolean("updateAvailable")) Warn else Good, arrow = false) {}
            }
        }
        val j = jobs.data
        if (!j.isNullOrEmpty()) {
            item { SectionTitle("Jobs · tap to run now") }
            items(j, key = { it.id }) { job ->
                MenuRow(Icons.Filled.PlayArrow, job.name, (if (job.running) "Running now · " else "") + "Next: ${fmtDate(job.next, true)}",
                    if (job.running) Good else Rainbow[job.name.length % Rainbow.size], arrow = false) {
                    run("${job.name} started", { jobs.load(true) }) { api.runJob(job.id) }
                }
            }
        }
    }
}

// ------------------------------------------------------------ Tautulli
@Composable
fun TautulliUsersTab(api: TautulliApi) {
    val l = rememberLoadable(api.cfg, "tusers") { api.users() }
    LoadableContent(l, empty = { it.isEmpty() }, emptyText = "No users") { list ->
        LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list) { u ->
                UserRow(u.thumb, u.name, if (u.lastPlayed.isNotEmpty()) "Last: ${u.lastPlayed}" else "", "${u.plays} plays · ${humanEta(u.duration)} · seen ${fmtEpoch(u.lastSeen)}",
                    Rainbow[u.name.length % Rainbow.size])
            }
        }
    }
}

@Composable
fun TautulliServerTab(cfg: ServiceConfig, nav: Navigator) {
    val api = TautulliApi(cfg)
    val info = rememberLoadable(cfg, "tinfo") { api.serverInfo() }
    val libs = rememberLoadable(cfg, "tlibs") { api.libraries() }
    GenericServerTab(cfg, nav) {
        info.data?.let { d ->
            item {
                MenuRow(Icons.Filled.Info, "Plex: ${d.optString("pms_name")}",
                    "v${d.optString("pms_version")} · ${d.optString("pms_platform")} · ${d.optString("pms_ip")}:${d.optString("pms_port")}", Good, arrow = false) {}
            }
        }
        val l = libs.data
        if (!l.isNullOrEmpty()) {
            item { SectionTitle("Plex libraries") }
            items(l) { lib ->
                val detail = when (lib.type) {
                    "show" -> "${lib.count} shows · ${lib.parents} seasons · ${lib.children} episodes"
                    "artist" -> "${lib.count} artists · ${lib.parents} albums · ${lib.children} tracks"
                    else -> "${lib.count} items"
                }
                MenuRow(Icons.Filled.Info, lib.name, detail, Rainbow[lib.name.length % Rainbow.size], arrow = false) {}
            }
        }
    }
}

@Composable
private fun UserRow(avatar: String?, name: String, line1: String, line2: String, color: Color) {
    Panel(Modifier.fillMaxWidth(), tint = color) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box44(avatar, name, color)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                if (line1.isNotEmpty()) Text(line1, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(line2, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun Box44(url: String?, name: String, color: Color) {
    androidx.compose.foundation.layout.Box(
        Modifier.size(44.dp).clip(CircleShape).androidxBackground(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
        if (url != null) AsyncImage(model = url, contentDescription = null, modifier = Modifier.size(44.dp).clip(CircleShape))
    }
}

private fun Modifier.androidxBackground(c: Color): Modifier =
    this.then(Modifier.background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(c.copy(alpha = 0.7f), c.copy(alpha = 0.3f)))))

@Suppress("unused")
private val keepIcon: @Composable () -> Unit = { IconButton(onClick = {}) { Icon(Icons.Filled.Info, null) } }
@Suppress("unused")
private val keepPoster: @Composable () -> Unit = { Poster(null) }
