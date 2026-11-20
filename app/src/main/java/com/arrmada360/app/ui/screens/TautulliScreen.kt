package com.arrmada360.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.arrmada360.app.api.PlayHistory
import com.arrmada360.app.api.StreamSession
import com.arrmada360.app.api.TautulliApi
import com.arrmada360.app.api.TopGroup
import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.data.display
import com.arrmada360.app.ui.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TautColor = Color(0xFFE5A00D)

@Composable
fun TautulliScreen(cfg: ServiceConfig, nav: Navigator, isRoot: Boolean, back: () -> Unit) {
    val api = remember(cfg) { TautulliApi(cfg) }
    var tab by rememberSaveable(cfg.id) { mutableIntStateOf(0) }
    AppScaffold(
        title = cfg.display(), isRoot = isRoot, onBack = back, badge = cfg.type, badgeName = cfg.name,
        bottomTabs = listOf(
            BottomTab("Activity", Icons.Filled.PlayCircle), BottomTab("History", Icons.Filled.History),
            BottomTab("Stats", Icons.Filled.BarChart), BottomTab("Users", Icons.Filled.People), BottomTab("Server", Icons.Filled.Dns),
        ), selectedBottom = tab, onBottom = { tab = it },
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                0 -> TActivityTab(api)
                1 -> THistoryTab(api)
                2 -> TStatsTab(api)
                3 -> TUsersTab(api)
                else -> TautulliServerTab(cfg, nav)
            }
        }
    }
}

private fun fmtMs(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

private fun mbps(kbps: Long) = if (kbps >= 1000) "%.1f Mbps".format(kbps / 1000.0) else "$kbps kbps"

@Composable
private fun Avatar(url: String?, name: String, size: androidx.compose.ui.unit.Dp) {
    val c = Rainbow[name.length % Rainbow.size]
    Box(Modifier.size(size).clip(CircleShape).background(Brush.linearGradient(listOf(c, c.copy(alpha = 0.5f)))), contentAlignment = Alignment.Center) {
        Text(name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        if (url != null) AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize().clip(CircleShape))
    }
}

// ------------------------------------------------------------ Activity
@Composable
private fun TActivityTab(api: TautulliApi) {
    val l = rememberLoadable(api.cfg, "tactivity", autoRefreshMs = 5000) { api.activityFull() }
    val run = rememberRunner()
    var stop by remember { mutableStateOf<StreamSession?>(null) }
    LoadableContent(l) { a ->
        LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Streams", "${a.sessions.size}", TautColor, Modifier.weight(1f))
                    StatTile("Bandwidth", mbps(a.total), Accent, Modifier.weight(1.3f))
                    StatTile("Transcoding", "${a.transcode}", if (a.transcode > 0) Warn else Good, Modifier.weight(1f))
                }
            }
            if (a.sessions.isNotEmpty()) item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill("Direct play ${a.direct}", Good); Pill("Direct stream ${a.directStream}", Accent); Pill("Transcode ${a.transcode}", Warn)
                    if (a.wan > 0) Pill("WAN ${mbps(a.wan)}", Pink)
                }
            }
            if (a.sessions.isEmpty()) item {
                Panel(Modifier.fillMaxWidth().padding(top = 40.dp), tint = TautColor) {
                    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Weekend, null, tint = TautColor, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("Nobody is watching right now", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Streams show up here live", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(a.sessions, key = { it.sessionId }) { s -> SessionCard(s) { stop = s } }
        }
    }
    stop?.let { s ->
        var msg by remember { mutableStateOf("The server owner has ended this stream.") }
        AlertDialog(
            onDismissRequest = { stop = null },
            title = { Text("Stop ${s.user}'s stream?") },
            text = { OutlinedTextField(msg, { msg = it }, label = { Text("Message shown to the user") }) },
            confirmButton = { TextButton(onClick = { stop = null; run("Stream stopped", { l.load(true) }) { api.terminate(s.sessionId, msg) } }) { Text("Stop", color = Bad) } },
            dismissButton = { TextButton(onClick = { stop = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SessionCard(s: StreamSession, onStop: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    val decisionColor = when {
        s.decision.contains("Transcode", true) -> Warn
        s.decision.contains("Stream", true) -> Accent
        else -> Good
    }
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).clickable { open = !open }.animateContentSize()) {
        if (s.art != null) AsyncImage(s.art, null, contentScale = ContentScale.Crop, alpha = 0.45f, modifier = Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.85f), Color.Black.copy(alpha = 0.45f)))))
        Column(Modifier.padding(12.dp)) {
            Row {
                Poster(s.thumb, Modifier.width(78.dp), fallback = s.title)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (s.state == "paused") Icons.Filled.Pause else if (s.state == "buffering") Icons.Filled.HourglassTop else Icons.Filled.PlayArrow, null,
                            tint = if (s.state == "playing") Good else Warn, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(s.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                    Text(s.subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(s.userThumb, s.user, 22.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(s.user, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                        Text("  ·  ${s.player}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Pill(s.decision.ifEmpty { "Direct Play" }, decisionColor)
                        if (s.location.isNotEmpty()) Pill(s.location, if (s.location == "WAN") Pink else Accent2)
                        if (s.bandwidthKbps > 0) Pill(mbps(s.bandwidthKbps), Accent)
                    }
                }
                IconButton(onClick = onStop) { Icon(Icons.Filled.StopCircle, "Stop stream", tint = Bad) }
            }
            Spacer(Modifier.height(10.dp))
            ProgressBar(s.progress / 100f, TautColor)
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(if (s.durationMs > 0) "${fmtMs(s.offsetMs)} / ${fmtMs(s.durationMs)}" else "${s.progress}%", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.weight(1f))
                if (s.durationMs > 0) Text("${fmtMs((s.durationMs - s.offsetMs).coerceAtLeast(0))} left", style = MaterialTheme.typography.labelSmall, color = TautColor, fontWeight = FontWeight.Bold)
            }
            if (open) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                Spacer(Modifier.height(8.dp))
                DetailLine(Icons.Filled.Devices, "Player", s.product)
                if (s.quality.isNotEmpty()) DetailLine(Icons.Filled.HighQuality, "Quality", s.quality)
                if (s.video.isNotEmpty()) DetailLine(Icons.Filled.Videocam, "Video", s.video)
                if (s.audio.isNotEmpty()) DetailLine(Icons.Filled.MusicNote, "Audio", s.audio)
                if (s.container.isNotEmpty()) DetailLine(Icons.Filled.Inventory2, "Container", s.container)
                if (s.ip.isNotEmpty()) DetailLine(Icons.Filled.Public, "IP address", s.ip)
            } else Text("Tap for stream details", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun DetailLine(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = TautColor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.width(84.dp))
        Text(value, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ------------------------------------------------------------ History
private val dayFmt = DateTimeFormatter.ofPattern("EEEE, d MMM")
private val timeFmt = DateTimeFormatter.ofPattern("h:mm a")

@Composable
private fun THistoryTab(api: TautulliApi) {
    val l = rememberLoadable(api.cfg, "thist2") { api.historyFull(150) }
    var who by rememberSaveable { mutableIntStateOf(0) }
    LoadableContent(l, empty = { it.isEmpty() }, emptyText = "No plays yet") { all ->
        val users = listOf("Everyone") + all.map { it.user }.distinct()
        val list = if (who == 0) all else all.filter { it.user == users.getOrNull(who) }
        val zone = ZoneId.systemDefault()
        val groups = list.groupBy { Instant.ofEpochSecond(it.date).atZone(zone).toLocalDate() }
        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
            item { ChipRow(users, who, { who = it }, Modifier.padding(vertical = 6.dp)) }
            groups.forEach { (day, rows) ->
                item(key = "h$day") {
                    val today = LocalDate.now()
                    val label = when (day) { today -> "Today"; today.minusDays(1) -> "Yesterday"; else -> day.format(dayFmt) }
                    Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TautColor, modifier = Modifier.weight(1f))
                        Text("${rows.size} play${if (rows.size == 1) "" else "s"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(rows) { h -> HistoryRow(h) }
            }
        }
    }
}

@Composable
private fun HistoryRow(h: PlayHistory) {
    Panel(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp), tint = TautColor) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Poster(h.thumb, Modifier.width(42.dp), corner = 8.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(h.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(null, h.user, 16.dp); Spacer(Modifier.width(5.dp))
                    Text(h.user, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("  ·  ${h.player}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("${Instant.ofEpochSecond(h.date).atZone(ZoneId.systemDefault()).format(timeFmt)} · watched ${humanEta(h.duration)}",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RingGauge(h.percent / 100f, if (h.percent >= 90) Good else TautColor, Modifier.size(38.dp), stroke = 4.dp) {
                Text("${h.percent}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ------------------------------------------------------------ Stats
private val SeriesColors = mapOf("TV" to Accent, "Movies" to TautColor, "Music" to Pink, "Live TV" to Good)

@Composable
private fun TStatsTab(api: TautulliApi) {
    val ranges = listOf(7, 30, 90, 365)
    var r by rememberSaveable { mutableIntStateOf(1) }
    val days = ranges[r]
    val plays = rememberLoadable(api.cfg, "tplays$days") { api.playsByDate(days) }
    val top = rememberLoadable(api.cfg, "ttop$days") { api.topStats(days) }
    LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
        item { ChipRow(listOf("7 days", "30 days", "90 days", "1 year"), r, { r = it }, Modifier.padding(vertical = 6.dp)) }
        item {
            Panel(Modifier.fillMaxWidth().padding(horizontal = 12.dp), tint = TautColor) {
                Column(Modifier.padding(14.dp)) {
                    val p = plays.data
                    val total = p?.series?.sumOf { it.second.sum() } ?: 0
                    Text("$total plays", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    Text("in the last $days days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    if (p == null) Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { if (plays.error != null) Text(plays.error ?: "", color = Bad) else CircularProgressIndicator() }
                    else {
                        StackedBars(p.series, Modifier.fillMaxWidth().height(120.dp))
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            p.series.filter { it.second.sum() > 0 }.forEach { (name, v) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StatusDot(SeriesColors[name] ?: Accent2); Spacer(Modifier.width(4.dp))
                                    Text("$name ${v.sum()}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
        val groups = top.data.orEmpty()
        if (top.loading && groups.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        groups.forEach { g -> item(key = g.id) { TopGroupView(g) } }
    }
}

@Composable
private fun StackedBars(series: List<Pair<String, List<Long>>>, modifier: Modifier) {
    val n = series.maxOfOrNull { it.second.size } ?: 0
    val totals = (0 until n).map { i -> series.sumOf { it.second.getOrElse(i) { 0 } } }
    val max = (totals.maxOrNull() ?: 0).coerceAtLeast(1)
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier) {
        if (n == 0) return@Canvas
        val slot = size.width / n
        val w = (slot * 0.7f).coerceAtLeast(1f)
        for (i in 0 until n) {
            val x = i * slot + (slot - w) / 2
            drawRoundRect(track, Offset(x, 0f), Size(w, size.height), androidx.compose.ui.geometry.CornerRadius(w / 3))
            var y = size.height
            series.forEach { (name, v) ->
                val h = size.height * v.getOrElse(i) { 0 } / max
                if (h > 0) { y -= h; drawRect(SeriesColors[name] ?: Accent2, Offset(x, y), Size(w, h)) }
            }
        }
    }
}

@Composable
private fun TopGroupView(g: TopGroup) {
    Column {
        SectionTitle(g.title)
        when (g.id) {
            "top_users", "top_platforms" -> {
                val max = g.rows.maxOf { it.plays }.coerceAtLeast(1)
                Panel(Modifier.fillMaxWidth().padding(horizontal = 12.dp), tint = TautColor) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        g.rows.take(6).forEachIndexed { i, e ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${i + 1}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = TautColor, modifier = Modifier.width(22.dp))
                                if (g.id == "top_users") { Avatar(e.thumb, e.title, 30.dp); Spacer(Modifier.width(10.dp)) }
                                else { Icon(Icons.Filled.Devices, null, tint = Rainbow[i % Rainbow.size], modifier = Modifier.size(26.dp)); Spacer(Modifier.width(10.dp)) }
                                Column(Modifier.weight(1f)) {
                                    Row {
                                        Text(e.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${e.plays} plays", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    ProgressBar(e.plays.toFloat() / max, Rainbow[i % Rainbow.size], height = 4.dp)
                                }
                            }
                        }
                    }
                }
            }
            else -> LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(g.rows) { i, e ->
                    Column(Modifier.width(110.dp)) {
                        Box {
                            Poster(e.thumb, Modifier.fillMaxWidth(), fallback = e.title)
                            Box(Modifier.padding(6.dp).size(26.dp).clip(CircleShape).background(TautColor), contentAlignment = Alignment.Center) {
                                Text("${i + 1}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = Color.Black)
                            }
                        }
                        Text(e.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                        Text(if (g.id.startsWith("popular")) "${e.users} users" else "${e.plays} plays · ${humanEta(e.duration)}",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------ Users
@Composable
private fun TUsersTab(api: TautulliApi) {
    val l = rememberLoadable(api.cfg, "tusers2") { api.users() }
    LoadableContent(l, empty = { it.isEmpty() }, emptyText = "No users") { list ->
        val max = list.maxOf { it.plays }.coerceAtLeast(1)
        LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(list) { i, u ->
                val c = Rainbow[i % Rainbow.size]
                Panel(Modifier.fillMaxWidth(), tint = c) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(u.thumb, u.name, 46.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(u.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1)
                                Text("${u.plays}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = c)
                                Text(" plays", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (u.lastPlayed.isNotEmpty()) Text("Last watched ${u.lastPlayed}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${humanEta(u.duration)} watched · seen ${relativeEpoch(u.lastSeen)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            Spacer(Modifier.height(5.dp))
                            ProgressBar(u.plays.toFloat() / max, c, height = 4.dp)
                        }
                    }
                }
            }
        }
    }
}

private fun relativeEpoch(sec: Long): String {
    if (sec <= 0) return "never"
    val d = (System.currentTimeMillis() / 1000 - sec)
    return when {
        d < 3600 -> "${d / 60} min ago"
        d < 86400 -> "${d / 3600} h ago"
        d < 86400 * 30 -> "${d / 86400} days ago"
        else -> fmtEpoch(sec)
    }
}
