package com.arrmada360.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arrmada360.app.api.SeerrApi
import com.arrmada360.app.api.SeerrMedia
import com.arrmada360.app.api.SeerrRequest
import com.arrmada360.app.api.StreamSession
import com.arrmada360.app.api.TautulliApi
import com.arrmada360.app.api.UnraidApi
import com.arrmada360.app.api.UnraidArray
import com.arrmada360.app.api.UnraidSystem
import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.ui.AppScaffold
import com.arrmada360.app.ui.Bad
import com.arrmada360.app.ui.ChipRow
import com.arrmada360.app.ui.EmptyBox
import com.arrmada360.app.ui.Good
import com.arrmada360.app.ui.LoadableContent
import com.arrmada360.app.ui.Panel
import com.arrmada360.app.ui.Pill
import com.arrmada360.app.ui.Poster
import com.arrmada360.app.ui.ProgressBar
import com.arrmada360.app.ui.SectionTitle
import com.arrmada360.app.ui.StatusDot
import com.arrmada360.app.ui.Warn
import com.arrmada360.app.ui.fmtDate
import com.arrmada360.app.ui.fmtEpoch
import com.arrmada360.app.ui.humanBytes
import com.arrmada360.app.ui.humanEta
import com.arrmada360.app.ui.rememberLoadable
import com.arrmada360.app.ui.rememberRunner

// ============================================================ Overseerr / Jellyseerr
private val REQ_FILTERS = listOf("Pending" to "pending", "Approved" to "approved", "Processing" to "processing", "Available" to "available", "All" to "all")

@Composable
fun SeerrScreen(cfg: ServiceConfig, isRoot: Boolean, back: () -> Unit) {
    val api = remember(cfg) { SeerrApi(cfg) }
    var tab by rememberSaveable(cfg.id) { mutableIntStateOf(0) }
    AppScaffold(title = cfg.name, isRoot = isRoot, onBack = back, badge = cfg.type, badgeName = cfg.name,
        tabs = listOf("Requests", "Discover"), selectedTab = tab, onTab = { tab = it }) { pad ->
        Box(Modifier.padding(pad)) {
            if (tab == 0) RequestsTab(api) else DiscoverTab(api)
        }
    }
}

private fun requestStatus(r: SeerrRequest): Pair<String, androidx.compose.ui.graphics.Color> = when {
    r.status == 1 -> "Pending" to Warn
    r.status == 3 -> "Declined" to Bad
    r.status == 4 -> "Failed" to Bad
    r.mediaStatus == 5 -> "Available" to Good
    r.mediaStatus == 4 -> "Partially available" to Good
    r.mediaStatus == 3 -> "Processing" to Warn
    else -> "Approved" to Good
}

@Composable
private fun RequestsTab(api: SeerrApi) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val l = rememberLoadable(api.cfg, filter) { api.requests(REQ_FILTERS[filter].second) }
    val run = rememberRunner()
    Column {
        ChipRow(REQ_FILTERS.map { it.first }, filter, { filter = it }, Modifier.padding(vertical = 8.dp))
        LoadableContent(l, empty = { it.isEmpty() }, emptyText = "No requests") { list ->
            LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { r ->
                    Panel(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(10.dp)) {
                            Poster(r.poster, Modifier.width(64.dp), fallback = r.title)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(r.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("${if (r.type == "tv") "TV" else "Movie"}${if (r.is4k) " · 4K" else ""}${if (r.seasons.isNotEmpty()) " · " + r.seasons else ""}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                Text("by ${r.requestedBy} · ${fmtDate(r.created)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(6.dp))
                                val (label, color) = requestStatus(r)
                                Pill(label, color)
                            }
                            Column {
                                if (r.status == 1) {
                                    IconButton(onClick = { run("Approved", { l.load(true) }) { api.approve(r.id) } }) { Icon(Icons.Filled.Check, "Approve", tint = Good) }
                                    IconButton(onClick = { run("Declined", { l.load(true) }) { api.decline(r.id) } }) { Icon(Icons.Filled.Close, "Decline", tint = Bad) }
                                } else {
                                    if (r.status == 4) IconButton(onClick = { run("Retrying", { l.load(true) }) { api.retry(r.id) } }) { Icon(Icons.Filled.Refresh, "Retry") }
                                    IconButton(onClick = { run("Request deleted", { l.load(true) }) { api.deleteRequest(r.id) } }) { Icon(Icons.Filled.Delete, "Delete") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val DISCOVER = listOf("Trending" to "trending", "Popular movies" to "movies", "Popular TV" to "tv", "Upcoming" to "upcoming")

@Composable
private fun DiscoverTab(api: SeerrApi) {
    var kind by rememberSaveable { mutableIntStateOf(0) }
    var term by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf("") }
    val l = rememberLoadable(api.cfg, kind, submitted) { if (submitted.isNotBlank()) api.search(submitted) else api.discover(DISCOVER[kind].second) }
    var pick by remember { mutableStateOf<SeerrMedia?>(null) }
    val run = rememberRunner()
    Column(Modifier.imePadding()) {
        OutlinedTextField(
            term, { term = it; if (it.isBlank()) submitted = "" }, singleLine = true, placeholder = { Text("Search movies & TV") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submitted = term.trim() }),
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp), shape = RoundedCornerShape(12.dp),
        )
        if (submitted.isBlank()) ChipRow(DISCOVER.map { it.first }, kind, { kind = it }, Modifier.padding(vertical = 8.dp))
        else Spacer(Modifier.height(8.dp))
        LoadableContent(l, empty = { it.isEmpty() }, emptyText = "Nothing found") { list ->
            LazyVerticalGrid(GridCells.Fixed(3), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(list, key = { it.mediaType + it.tmdbId }) { m ->
                    Column(Modifier.clickable { pick = m }) {
                        Box {
                            Poster(m.poster, Modifier.fillMaxWidth(), fallback = m.title)
                            if (m.status >= 2) Box(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                                Pill(when (m.status) { 5 -> "Available"; 4 -> "Partial"; 3 -> "Processing"; else -> "Requested" }, if (m.status >= 4) Good else Warn)
                            }
                        }
                        Text(m.title, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${m.year} · ${if (m.mediaType == "tv") "TV" else "Movie"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
    pick?.let { m ->
        AlertDialog(
            onDismissRequest = { pick = null },
            title = { Text("${m.title} (${m.year})") },
            text = { Text(m.overview.ifEmpty { "No overview." }, maxLines = 10, overflow = TextOverflow.Ellipsis) },
            confirmButton = {
                Row {
                    if (m.status < 2) {
                        TextButton(onClick = { pick = null; run("Requested 4K ${m.title}", { l.load(true) }) { api.request(m, true) } }) { Text("Request 4K") }
                        TextButton(onClick = { pick = null; run("Requested ${m.title}", { l.load(true) }) { api.request(m) } }) { Text("Request") }
                    } else Text("Already requested", modifier = Modifier.padding(12.dp))
                }
            },
            dismissButton = { TextButton(onClick = { pick = null }) { Text("Close") } },
        )
    }
}

// ============================================================ Tautulli
@Composable
fun TautulliScreen(cfg: ServiceConfig, isRoot: Boolean, back: () -> Unit) {
    val api = remember(cfg) { TautulliApi(cfg) }
    var tab by rememberSaveable(cfg.id) { mutableIntStateOf(0) }
    AppScaffold(title = cfg.name, isRoot = isRoot, onBack = back, badge = cfg.type, badgeName = cfg.name,
        tabs = listOf("Activity", "History", "Stats"), selectedTab = tab, onTab = { tab = it }) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                0 -> ActivityTab(api)
                1 -> {
                    val l = rememberLoadable(cfg, "hist") { api.history() }
                    LoadableContent(l, empty = { it.isEmpty() }, emptyText = "No plays yet") { list ->
                        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                            items(list) { h ->
                                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Poster(h.thumb, Modifier.width(40.dp), corner = 6.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(h.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${h.user} · ${h.player}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${fmtEpoch(h.date)} · ${humanEta(h.duration)} · ${h.percent}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    val l = rememberLoadable(cfg, "stats") { api.homeStats() }
                    LoadableContent(l, empty = { it.isEmpty() }, emptyText = "No stats") { list ->
                        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                            list.forEach { (title, rows) ->
                                item { SectionTitle("$title · last 30 days") }
                                items(rows) { r -> Text(r, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityTab(api: TautulliApi) {
    val l = rememberLoadable(api.cfg, "activity", autoRefreshMs = 5000) { api.activity() }
    val run = rememberRunner()
    var stop by remember { mutableStateOf<StreamSession?>(null) }
    LoadableContent(l, empty = { it.first.isEmpty() }, emptyText = "Nobody is watching right now") { (list, bw) ->
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("${list.size} stream${if (list.size == 1) "" else "s"} · ${"%.1f".format(bw / 1000.0)} Mbps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(list, key = { it.sessionId }) { s ->
                Panel(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(10.dp)) {
                        Poster(s.thumb, Modifier.width(64.dp), fallback = s.title)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(s.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${s.user} · ${s.player}", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Pill(s.state.replaceFirstChar { it.uppercase() }, if (s.state == "playing") Good else Warn)
                                Pill(s.decision, if (s.decision.contains("Transcode", true)) Warn else Good)
                                if (s.location.isNotEmpty()) Pill(s.location, MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(6.dp))
                            ProgressBar(s.progress / 100f)
                            Text("${s.progress}% · ${s.quality}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { stop = s }) { Icon(Icons.Filled.Stop, "Stop stream", tint = Bad) }
                    }
                }
            }
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

// ============================================================ Unraid
@Composable
fun UnraidScreen(cfg: ServiceConfig, isRoot: Boolean, back: () -> Unit) {
    val api = remember(cfg) { UnraidApi(cfg) }
    var tab by rememberSaveable(cfg.id) { mutableIntStateOf(0) }
    AppScaffold(title = cfg.name, isRoot = isRoot, onBack = back, badge = cfg.type, badgeName = cfg.name,
        tabs = listOf("Overview", "Docker", "VMs"), selectedTab = tab, onTab = { tab = it }) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                0 -> UnraidOverview(api)
                1 -> UnraidDocker(api)
                else -> UnraidVms(api)
            }
        }
    }
}

private class UnraidOverviewData(val system: UnraidSystem?, val array: UnraidArray?, val sysError: String?, val arrError: String?, val unread: Int)

@Composable
private fun UnraidOverview(api: UnraidApi) {
    val l = rememberLoadable(api.cfg, "overview", autoRefreshMs = 10000) {
        var se: String? = null
        var ae: String? = null
        val s = try { api.system() } catch (e: Exception) { se = e.message; null }
        val a = try { api.array() } catch (e: Exception) { ae = e.message; null }
        if (s == null && a == null) throw Exception(se ?: ae ?: "Unraid API not reachable")
        UnraidOverviewData(s, a, se, ae, api.notificationsUnread())
    }
    LoadableContent(l) { d ->
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            d.system?.let { s ->
                item {
                    Panel(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(s.hostname.ifEmpty { "Unraid" }, style = MaterialTheme.typography.titleMedium)
                            if (s.version.isNotEmpty()) Text("Unraid ${s.version}", style = MaterialTheme.typography.bodySmall)
                            if (s.cpu.isNotEmpty()) Text(s.cpu, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (s.uptime.isNotEmpty()) Text("Up since ${fmtDate(s.uptime, true).ifEmpty { s.uptime }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (d.unread > 0) Pill("${d.unread} unread notifications", Warn)
                            s.cpuPercent?.let { c ->
                                Spacer(Modifier.height(6.dp)); Text("CPU ${"%.0f".format(c)}%", style = MaterialTheme.typography.labelSmall)
                                ProgressBar((c / 100.0).toFloat())
                            }
                            if (s.memTotal > 0) {
                                Spacer(Modifier.height(6.dp)); Text("RAM ${humanBytes(s.memUsed)} / ${humanBytes(s.memTotal)}", style = MaterialTheme.typography.labelSmall)
                                ProgressBar(s.memUsed.toFloat() / s.memTotal)
                            }
                        }
                    }
                }
            }
            d.array?.let { a ->
                item {
                    Panel(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Array", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                Pill(a.state, if (a.state.equals("STARTED", true)) Good else Warn)
                            }
                            if (a.total > 0) {
                                val p = a.used.toFloat() / a.total
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    com.arrmada360.app.ui.RingGauge(p, if (p > 0.9f) Bad else MaterialTheme.colorScheme.primary, Modifier.size(96.dp), stroke = 10.dp) {
                                        Text("${(p * 100).toInt()}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        com.arrmada360.app.ui.StatTile("Used", humanBytes(a.used), MaterialTheme.colorScheme.primary)
                                        com.arrmada360.app.ui.StatTile("Free", humanBytes(a.total - a.used), Good)
                                    }
                                }
                            }
                        }
                    }
                }
                if (a.disks.isNotEmpty()) item { SectionTitle("Disks") }
                items(a.disks) { disk ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(if (disk.status.contains("OK", true)) Good else if (disk.status.contains("NP", true) || disk.status.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else Bad)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${disk.name}  ·  ${disk.kind}", style = MaterialTheme.typography.bodyMedium)
                            val sizeText = if (disk.fsSize > 0) "${humanBytes(disk.fsSize - disk.fsFree)} / ${humanBytes(disk.fsSize)}" else if (disk.size > 0) humanBytes(disk.size) else ""
                            Text(listOf(disk.status, sizeText).filter { it.isNotEmpty() }.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (disk.fsSize > 0) ProgressBar(1f - disk.fsFree.toFloat() / disk.fsSize, modifier = Modifier.padding(top = 4.dp))
                        }
                        disk.temp?.let { t -> Text("$t°C", style = MaterialTheme.typography.labelSmall, color = if (t >= 50) Bad else if (t >= 45) Warn else MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
            if (d.sysError != null || d.arrError != null) item {
                Text(listOfNotNull(d.sysError, d.arrError).joinToString("\n"), style = MaterialTheme.typography.labelSmall, color = Warn)
            }
        }
    }
}

@Composable
private fun UnraidDocker(api: UnraidApi) {
    val l = rememberLoadable(api.cfg, "docker", autoRefreshMs = 10000) { api.containers() }
    val run = rememberRunner()
    var filter by rememberSaveable { mutableIntStateOf(0) }
    Column {
        ChipRow(listOf("All", "Running", "Stopped"), filter, { filter = it }, Modifier.padding(vertical = 8.dp))
        LoadableContent(l, empty = { it.isEmpty() }, emptyText = "No containers") { list ->
            val shown = list.filter { when (filter) { 1 -> it.state.equals("RUNNING", true); 2 -> !it.state.equals("RUNNING", true); else -> true } }
            LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item { Text("${list.count { it.state.equals("RUNNING", true) }} of ${list.size} running", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(shown, key = { it.id }) { c ->
                    val running = c.state.equals("RUNNING", true)
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(if (running) Good else Bad)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                            Text(c.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(c.status.ifEmpty { c.state.lowercase() }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            if (c.image.isNotEmpty()) Text(c.image, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (running) {
                            IconButton(onClick = { run("Restarting ${c.name}", { l.load(true) }) { api.containerAction(c.id, "stop"); api.containerAction(c.id, "start") } }) { Icon(Icons.Filled.Refresh, "Restart") }
                            IconButton(onClick = { run("Stopped ${c.name}", { l.load(true) }) { api.containerAction(c.id, "stop") } }) { Icon(Icons.Filled.Stop, "Stop", tint = Bad) }
                        } else IconButton(onClick = { run("Started ${c.name}", { l.load(true) }) { api.containerAction(c.id, "start") } }) { Icon(Icons.Filled.PlayArrow, "Start", tint = Good) }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnraidVms(api: UnraidApi) {
    val l = rememberLoadable(api.cfg, "vms") { api.vms() }
    val run = rememberRunner()
    LoadableContent(l, empty = { it.isEmpty() }, emptyText = "No virtual machines") { list ->
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(list, key = { it.id }) { v ->
                val running = v.state.equals("RUNNING", true)
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(if (running) Good else if (v.state.equals("PAUSED", true)) Warn else Bad)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                        Text(v.name, style = MaterialTheme.typography.bodyMedium)
                        Text(v.state.lowercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (running) {
                        IconButton(onClick = { run("Rebooting ${v.name}", { l.load(true) }) { api.vmAction(v.id, "reboot") } }) { Icon(Icons.Filled.Refresh, "Reboot") }
                        IconButton(onClick = { run("Stopping ${v.name}", { l.load(true) }) { api.vmAction(v.id, "stop") } }) { Icon(Icons.Filled.Stop, "Stop", tint = Bad) }
                    } else IconButton(onClick = { run("Starting ${v.name}", { l.load(true) }) { api.vmAction(v.id, if (v.state.equals("PAUSED", true)) "resume" else "start") } }) {
                        Icon(Icons.Filled.PlayArrow, "Start", tint = Good)
                    }
                }
            }
        }
    }
}

@Suppress("unused")
private fun keep() = listOf(Modifier.size(1.dp), Modifier.fillMaxSize())
