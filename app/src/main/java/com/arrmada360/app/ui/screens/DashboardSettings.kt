package com.arrmada360.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.arrmada360.app.api.Conn
import com.arrmada360.app.api.Connections
import com.arrmada360.app.api.SpeedHistory
import com.arrmada360.app.ui.Accent
import com.arrmada360.app.ui.Accent2
import com.arrmada360.app.ui.GradientButton
import com.arrmada360.app.ui.Pink
import com.arrmada360.app.ui.PulseDot
import com.arrmada360.app.ui.Rainbow
import com.arrmada360.app.ui.Sparkline
import com.arrmada360.app.ui.StatTile
import com.arrmada360.app.ui.animatedLong
import com.arrmada360.app.ui.shimmer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arrmada360.app.api.ArrApi
import com.arrmada360.app.api.CalEntry
import com.arrmada360.app.api.DlSnapshot
import com.arrmada360.app.api.DownloadClients
import com.arrmada360.app.api.SeerrApi
import com.arrmada360.app.api.TautulliApi
import com.arrmada360.app.api.UnraidApi
import com.arrmada360.app.data.ConfigStore
import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.data.ServiceType
import com.arrmada360.app.net.int
import com.arrmada360.app.ui.AppScaffold
import com.arrmada360.app.ui.Bad
import com.arrmada360.app.ui.Good
import com.arrmada360.app.ui.Navigator
import com.arrmada360.app.ui.Panel
import com.arrmada360.app.ui.Pill
import com.arrmada360.app.ui.Poster
import com.arrmada360.app.ui.ProgressBar
import com.arrmada360.app.ui.Route
import com.arrmada360.app.ui.SectionTitle
import com.arrmada360.app.ui.ServiceBadge
import com.arrmada360.app.ui.Warn
import com.arrmada360.app.ui.copyText
import com.arrmada360.app.ui.fmtDate
import com.arrmada360.app.ui.humanBytes
import com.arrmada360.app.ui.humanSpeed
import com.arrmada360.app.ui.parseDate
import com.arrmada360.app.ui.relativeDay
import com.arrmada360.app.ui.rememberLoadable
import com.arrmada360.app.ui.toast
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

// ============================================================ Dashboard
@Composable
fun DashboardScreen(nav: Navigator) {
    val services = ConfigStore.services.toList()
    AppScaffold(
        title = "Arrmada360", subtitle = greeting(), isRoot = true, onBack = {},
        actions = {
            IconButton(onClick = { nav.push(Route.Health) }) { Icon(Icons.Filled.Wifi, "Connection check") }
            IconButton(onClick = { nav.push(Route.Search()) }) { Icon(Icons.Filled.Search, "Search everything") }
        },
    ) { pad ->
        if (services.isEmpty()) {
            Column(Modifier.padding(pad).padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(80.dp))
                Text("No apps yet", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text("Add Sonarr, Radarr, your download client and more.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
                GradientButton("Add an app") { nav.push(Route.AddService) }
            }
            return@AppScaffold
        }
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(bottom = 40.dp)) {
            item(key = "hero") { HeroCard(services, nav) }
            item(key = "tiles-title") { SectionTitle("Your apps") }
            val rows = services.chunked(2)
            items(rows.size, key = { "row$it" }) { i ->
                Row(Modifier.padding(horizontal = 12.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rows[i].forEach { c -> AppTile(c, Modifier.weight(1f), onClick = { nav.root(Route.Service(c.id)) }, onLong = { nav.push(Route.EditService(c.id, null)) }) }
                    if (rows[i].size == 1) Spacer(Modifier.weight(1f))
                }
            }
            val arrs = services.filter { it.type.isArr }
            if (arrs.isNotEmpty()) item(key = "upcoming") { UpcomingRow(arrs, nav) }
            val downloaders = services.filter { it.type.isDownloader }
            if (downloaders.isNotEmpty()) {
                item { SectionTitle("Live downloads") }
                items(downloaders, key = { "dl" + it.id }) { c -> DownloaderCard(c) { nav.root(Route.Service(c.id)) } }
            }
            val others = services.filter { it.type == ServiceType.SEERR || it.type == ServiceType.TAUTULLI || it.type == ServiceType.UNRAID }
            if (others.isNotEmpty()) {
                item { SectionTitle("At a glance") }
                items(others, key = { "o" + it.id }) { c -> GlanceCard(c) { nav.root(Route.Service(c.id)) } }
            }
        }
    }
}

private fun greeting(): String {
    val h = java.time.LocalTime.now().hour
    return when (h) { in 5..11 -> "Good morning"; in 12..16 -> "Good afternoon"; in 17..21 -> "Good evening"; else -> "Up late?" } + " 👋"
}

@Composable
private fun HeroCard(services: List<ServiceConfig>, nav: Navigator) {
    val down = SpeedHistory.totalDown()
    val up = SpeedHistory.totalUp()
    val dl = animatedLong(down.lastOrNull() ?: 0L)
    val ul = animatedLong(up.lastOrNull() ?: 0L)
    val online = services.count { Connections.of(it.id) is Conn.Ok }
    val failing = services.count { Connections.of(it.id) is Conn.Fail }
    val active = SpeedHistory.active.values.sum()
    Box(
        Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Accent.copy(alpha = 0.55f), Accent2.copy(alpha = 0.50f), Pink.copy(alpha = 0.40f))))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(22.dp)),
    ) {
        if (down.size > 1) Sparkline(down, Color.White, Modifier.matchParentSize().padding(top = 60.dp), fill = true)
        Column(Modifier.padding(18.dp)) {
            Text("Total download speed", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(humanSpeed(dl), style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Spacer(Modifier.width(10.dp))
                Icon(Icons.Filled.ArrowUpward, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp).padding(bottom = 2.dp))
                Text(humanSpeed(ul), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.padding(bottom = 4.dp))
            }
            Spacer(Modifier.height(30.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroStat("Online", "$online/${services.size}", Modifier.weight(1f)) { nav.push(Route.Health) }
                HeroStat("Downloading", active.toString(), Modifier.weight(1f)) {
                    ConfigStore.downloaders().firstOrNull()?.let { nav.root(Route.Service(it.id)) }
                }
                HeroStat(if (failing > 0) "Need attention" else "Problems", failing.toString(), Modifier.weight(1f)) { nav.push(Route.Health) }
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = 0.22f)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f), maxLines = 1)
    }
}

@Composable
private fun AppTile(c: ServiceConfig, modifier: Modifier, onClick: () -> Unit, onLong: () -> Unit) {
    val st = Connections.of(c.id)
    Panel(modifier.height(118.dp), tint = c.type.tint, onClick = onClick, onLongClick = onLong) {
        Box(Modifier.fillMaxWidth().height(118.dp).background(Brush.linearGradient(listOf(c.type.tint.copy(alpha = 0.22f), c.type.tint2.copy(alpha = 0.06f))))) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    ServiceBadge(c.type, 46.dp, c.name)
                    Spacer(Modifier.weight(1f))
                    when (st) {
                        is Conn.Ok -> PulseDot(Good, size = 8.dp)
                        is Conn.Fail -> PulseDot(Bad, pulsing = false, size = 8.dp)
                        is Conn.Checking -> CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = c.type.tint)
                        else -> {}
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(c.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    when (st) { is Conn.Fail -> "Offline – tap & hold to fix"; is Conn.Ok -> c.type.blurb; else -> c.type.blurb },
                    style = MaterialTheme.typography.labelSmall, color = if (st is Conn.Fail) Bad else c.type.tint, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DownloaderCard(c: ServiceConfig, onClick: () -> Unit) {
    val client = remember(c) { DownloadClients.of(c) }
    val l = rememberLoadable(c, "dash", autoRefreshMs = 3000) { client?.let { DownloadClients.snapshot(it) } ?: DlSnapshot(emptyList(), 0, 0) }
    val hist = SpeedHistory.down[c.id] ?: emptyList()
    Panel(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp), tint = c.type.tint, onClick = onClick) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ServiceBadge(c.type, 36.dp, c.name)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.name, style = MaterialTheme.typography.titleSmall)
                    val s = l.data
                    if (s != null) {
                        val a = s.items.count { !it.done && !it.paused }
                        Text("$a active · ${s.items.size} total" + if (s.globalPaused) " · paused" else "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (l.error != null) Text(l.error ?: "", style = MaterialTheme.typography.labelSmall, color = Bad, maxLines = 1)
                }
                val s = l.data
                if (s != null) Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ArrowDownward, null, Modifier.size(14.dp), tint = c.type.tint)
                        Text(humanSpeed(animatedLong(s.dlSpeed)), style = MaterialTheme.typography.titleSmall, color = c.type.tint)
                    }
                    if (client?.isUsenet != true) Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ArrowUpward, null, Modifier.size(12.dp), tint = Good)
                        Text(humanSpeed(animatedLong(s.upSpeed)), style = MaterialTheme.typography.labelSmall, color = Good)
                    }
                }
            }
            if (hist.size > 1) {
                Spacer(Modifier.height(8.dp))
                Sparkline(hist, c.type.tint, Modifier.fillMaxWidth().height(42.dp))
            }
            val s = l.data
            if (s != null) s.items.filter { !it.done && !it.paused }.sortedByDescending { it.dlSpeed }.take(3).forEach { d ->
                Spacer(Modifier.height(8.dp))
                Row {
                    Text(d.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(" ${(d.progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = c.type.tint)
                }
                Spacer(Modifier.height(3.dp))
                ProgressBar(d.progress, color = c.type.tint, height = 4.dp)
            }
        }
    }
}

@Composable
private fun UpcomingRow(arrs: List<ServiceConfig>, nav: Navigator) {
    val l = rememberLoadable(arrs.map { it.id }, "upcoming") {
        coroutineScope {
            arrs.map { c -> async { try { ArrApi(c).calendar(0, 14).map { c to it } } catch (e: Exception) { emptyList<Pair<ServiceConfig, CalEntry>>() } } }
                .awaitAll().flatten().sortedBy { it.second.date }.take(20)
        }
    }
    val list = l.data ?: return
    if (list.isEmpty()) return
    Column {
        SectionTitle("Coming up")
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(list) { (c, e) ->
                Panel(Modifier.width(128.dp), tint = c.type.tint, onClick = { nav.push(Route.ArrDetail(c.id, e.parentId)) }) {
                    Column {
                        Box {
                            Poster(e.poster, Modifier.fillMaxWidth(), corner = 0.dp, fallback = e.title)
                            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))))
                            Box(Modifier.align(Alignment.TopStart).padding(6.dp)) { Pill(relativeDay(parseDate(e.date)), c.type.tint, solid = true) }
                            Box(Modifier.align(Alignment.TopEnd).padding(6.dp)) { ServiceBadge(c.type, 22.dp, c.name) }
                            Column(Modifier.align(Alignment.BottomStart).padding(8.dp)) {
                                Text(e.title, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(e.subtitle, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlanceCard(c: ServiceConfig, onClick: () -> Unit) {
    val l = rememberLoadable(c, "glance", autoRefreshMs = 15000) {
        when (c.type) {
            ServiceType.SEERR -> {
                val n = SeerrApi(c).counts()
                listOf("Pending" to n.int("pending").toString(), "Processing" to n.int("processing").toString(), "Available" to n.int("available").toString())
            }
            ServiceType.TAUTULLI -> {
                val (s, bw) = TautulliApi(c).activity()
                listOf("Streams" to s.size.toString(), "Bandwidth" to "%.1f Mbps".format(bw / 1000.0), "Transcoding" to s.count { it.decision.contains("Transcode", true) }.toString())
            }
            else -> {
                val a = UnraidApi(c).array()
                val pct = if (a.total > 0) (a.used * 100 / a.total) else 0
                listOf("Array" to a.state.lowercase().replaceFirstChar { it.uppercase() }, "Used" to "$pct%", "Free" to humanBytes(a.total - a.used))
            }
        }
    }
    Panel(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp), tint = c.type.tint, onClick = onClick) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ServiceBadge(c.type, 36.dp, c.name)
                Spacer(Modifier.width(10.dp))
                Text(c.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, null, tint = c.type.tint)
            }
            Spacer(Modifier.height(10.dp))
            val d = l.data
            when {
                d != null -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    d.forEachIndexed { i, (label, value) ->
                        StatTile(label, value, if (i == 0) c.type.tint else if (i == 1) c.type.tint2 else Rainbow[(i + 2) % Rainbow.size], Modifier.weight(1f))
                    }
                }
                l.error != null -> Text("Offline – ${l.error}", style = MaterialTheme.typography.bodySmall, color = Warn, maxLines = 2)
                else -> Box(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).shimmer())
            }
        }
    }
}

// ============================================================ Settings
@Composable
fun SettingsScreen(nav: Navigator, isRoot: Boolean, back: () -> Unit) {
    val ctx = LocalContext.current
    var confirmDelete by remember { mutableStateOf<ServiceConfig?>(null) }
    var importOpen by remember { mutableStateOf(false) }
    AppScaffold(title = "Settings", isRoot = isRoot, onBack = back) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(bottom = 40.dp)) {
            item { SectionTitle("Services") }
            item {
                GradientButton("Test all connections", Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth()) { nav.push(Route.Health) }
            }
            items(ConfigStore.services.toList(), key = { it.id }) { c ->
                Row(Modifier.fillMaxWidth().clickable { nav.push(Route.EditService(c.id, null)) }.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    ServiceBadge(c.type, 32.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name)
                        Text(c.baseUrl, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { ConfigStore.move(c.id, -1) }) { Icon(Icons.Filled.KeyboardArrowUp, "Move up") }
                    IconButton(onClick = { ConfigStore.move(c.id, 1) }) { Icon(Icons.Filled.KeyboardArrowDown, "Move down") }
                    IconButton(onClick = { nav.push(Route.EditService(c.id, null)) }) { Icon(Icons.Filled.Edit, "Edit") }
                    IconButton(onClick = { confirmDelete = c }) { Icon(Icons.Filled.Delete, "Delete") }
                }
            }
            item {
                TextButton(onClick = { nav.push(Route.AddService) }, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Add service")
                }
            }
            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
            item { SectionTitle("Appearance") }
            item {
                Row(Modifier.fillMaxWidth().clickable { ConfigStore.updateAmoled(!ConfigStore.amoled) }.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("AMOLED black")
                        Text("Pure black background to save battery", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(ConfigStore.amoled, { ConfigStore.updateAmoled(it) })
                }
            }
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Poster columns: ${ConfigStore.gridColumns}")
                    Slider(value = ConfigStore.gridColumns.toFloat(), onValueChange = { ConfigStore.updateGridColumns(Math.round(it)) }, valueRange = 2f..5f, steps = 2)
                }
            }
            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
            item { SectionTitle("Backup") }
            item {
                Row(Modifier.padding(horizontal = 8.dp)) {
                    TextButton(onClick = {
                        val text = ConfigStore.exportJson()
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                        ctx.startActivity(Intent.createChooser(send, "Export Arrmada360 settings"))
                    }) { Text("Export / share") }
                    TextButton(onClick = { ctx.copyText(ConfigStore.exportJson()) }) { Text("Copy") }
                    TextButton(onClick = { importOpen = true }) { Text("Import") }
                }
            }
            item {
                Text("Exports contain your API keys and passwords – keep them private.", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
            }
            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
            item { SectionTitle("About") }
            item {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Arrmada360", fontWeight = FontWeight.SemiBold)
                    Text("Free and open source. Every feature unlocked – no subscriptions, no paywalls.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { ConfigStore.resetOnboarding() }) { Text("Run first-time setup again") }
                }
            }
        }
    }

    confirmDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Remove ${c.name}?") },
            text = { Text("This only removes it from Arrmada360. Nothing changes on your server.") },
            confirmButton = { TextButton(onClick = { ConfigStore.remove(c.id); confirmDelete = null }) { Text("Remove", color = Bad) } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
        )
    }

    if (importOpen) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { importOpen = false },
            title = { Text("Import settings") },
            text = { OutlinedTextField(text, { text = it }, label = { Text("Paste exported JSON") }, minLines = 4, maxLines = 8) },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        val n = ConfigStore.importJson(text)
                        ctx.toast("Imported $n services")
                        importOpen = false
                    } catch (e: Exception) {
                        ctx.toast("That doesn't look like an Arrmada360 export")
                    }
                }) { Text("Import") }
            },
            dismissButton = { TextButton(onClick = { importOpen = false }) { Text("Cancel") } },
        )
    }
}

@Suppress("unused")
private fun unusedRefs() = listOf(fmtDate(""), Warn)
