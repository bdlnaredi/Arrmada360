package com.arrmada360.app.ui.screens

import android.content.Intent
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
        title = "Dashboard", isRoot = true, onBack = {},
        actions = { IconButton(onClick = { nav.push(Route.Search()) }) { Icon(Icons.Filled.Search, "Search everything") } },
    ) { pad ->
        if (services.isEmpty()) {
            Column(Modifier.padding(pad).padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(80.dp))
                Text("No services yet", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("Add Sonarr, Radarr, your download client and more.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
                Button(onClick = { nav.push(Route.AddService) }) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Add a service") }
            }
            return@AppScaffold
        }
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(bottom = 32.dp)) {
            val downloaders = services.filter { it.type.isDownloader }
            if (downloaders.isNotEmpty()) {
                item { SectionTitle("Downloads") }
                items(downloaders, key = { "dl" + it.id }) { c -> DownloaderCard(c) { nav.root(Route.Service(c.id)) } }
            }
            val arrs = services.filter { it.type.isArr }
            if (arrs.isNotEmpty()) item(key = "upcoming") { UpcomingRow(arrs, nav) }
            val others = services.filter { it.type == ServiceType.SEERR || it.type == ServiceType.TAUTULLI || it.type == ServiceType.UNRAID }
            if (others.isNotEmpty()) {
                item { SectionTitle("At a glance") }
                items(others, key = { "o" + it.id }) { c -> GlanceCard(c) { nav.root(Route.Service(c.id)) } }
            }
            item { SectionTitle("All services") }
            items(services, key = { "s" + it.id }) { c ->
                Row(Modifier.fillMaxWidth().clickable { nav.root(Route.Service(c.id)) }.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ServiceBadge(c.type, 32.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name)
                        Text(c.type.blurb, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun DownloaderCard(c: ServiceConfig, onClick: () -> Unit) {
    val client = remember(c) { DownloadClients.of(c) }
    val l = rememberLoadable(c, "dash", autoRefreshMs = 4000) { client?.snapshot() ?: DlSnapshot(emptyList(), 0, 0) }
    Panel(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), onClick = onClick) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ServiceBadge(c.type, 30.dp)
                Spacer(Modifier.width(10.dp))
                Text(c.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                val s = l.data
                if (s != null) {
                    Icon(Icons.Filled.ArrowDownward, null, Modifier.size(14.dp), tint = c.type.tint)
                    Text(humanSpeed(s.dlSpeed), style = MaterialTheme.typography.labelSmall)
                    if (client?.isUsenet != true) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Filled.ArrowUpward, null, Modifier.size(14.dp), tint = Good)
                        Text(humanSpeed(s.upSpeed), style = MaterialTheme.typography.labelSmall)
                    }
                } else if (l.error != null) Pill("Offline", Bad)
            }
            val s = l.data
            if (s != null) {
                val active = s.items.filter { !it.done && !it.paused }
                Spacer(Modifier.height(6.dp))
                Text("${active.size} active · ${s.items.size} total" + if (s.globalPaused) " · paused" else "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                active.sortedByDescending { it.dlSpeed }.take(3).forEach { d ->
                    Spacer(Modifier.height(8.dp))
                    Text(d.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(3.dp))
                    ProgressBar(d.progress, color = c.type.tint, height = 3.dp)
                }
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
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(list) { (c, e) ->
                Column(Modifier.width(110.dp).clickable { nav.push(Route.ArrDetail(c.id, e.parentId)) }) {
                    Box {
                        Poster(e.poster, Modifier.fillMaxWidth(), fallback = e.title)
                        Box(Modifier.align(Alignment.TopStart).padding(4.dp)) { Pill(relativeDay(parseDate(e.date)), c.type.tint) }
                    }
                    Text(e.title, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                    Text(e.subtitle, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                "${n.int("pending")} pending · ${n.int("processing")} processing · ${n.int("available")} available"
            }
            ServiceType.TAUTULLI -> {
                val (s, bw) = TautulliApi(c).activity()
                if (s.isEmpty()) "Nobody watching" else "${s.size} streaming: " + s.joinToString { it.user } + " · ${"%.1f".format(bw / 1000.0)} Mbps"
            }
            else -> {
                val a = UnraidApi(c).array()
                val free = if (a.total > 0) " · ${humanBytes(a.total - a.used)} free" else ""
                "Array ${a.state.lowercase()}$free"
            }
        }
    }
    Panel(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), onClick = onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ServiceBadge(c.type, 30.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(c.name, style = MaterialTheme.typography.titleSmall)
                Text(l.data ?: l.error?.let { "Offline – $it" } ?: "Loading…", style = MaterialTheme.typography.bodySmall,
                    color = if (l.error != null && l.data == null) Warn else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
