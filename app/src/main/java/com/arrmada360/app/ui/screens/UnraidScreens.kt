package com.arrmada360.app.ui.screens

import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Description

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arrmada360.app.api.UnraidApi
import com.arrmada360.app.api.UnraidArray
import com.arrmada360.app.api.UnraidContainer
import com.arrmada360.app.api.UnraidDisk
import com.arrmada360.app.api.UnraidNotification
import com.arrmada360.app.api.UnraidSystem
import com.arrmada360.app.api.UnraidVm
import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.data.display
import com.arrmada360.app.ui.Accent
import com.arrmada360.app.ui.Accent2
import com.arrmada360.app.ui.AppScaffold
import com.arrmada360.app.ui.Bad
import com.arrmada360.app.ui.BottomTab
import com.arrmada360.app.ui.ChipRow
import com.arrmada360.app.ui.Good
import com.arrmada360.app.ui.LoadableContent
import com.arrmada360.app.ui.Navigator
import com.arrmada360.app.ui.Panel
import com.arrmada360.app.ui.Pill
import com.arrmada360.app.ui.ProgressBar
import com.arrmada360.app.ui.PulseDot
import com.arrmada360.app.ui.RingGauge
import com.arrmada360.app.ui.Route
import com.arrmada360.app.ui.SectionTitle
import com.arrmada360.app.ui.StatTile
import com.arrmada360.app.ui.StatusDot
import com.arrmada360.app.ui.Warn
import com.arrmada360.app.ui.fmtDate
import com.arrmada360.app.ui.humanBytes
import com.arrmada360.app.ui.rememberLoadable
import com.arrmada360.app.ui.rememberRunner

/** Confirmation for anything that changes the server. */
private data class Confirm(val title: String, val text: String, val button: String, val action: suspend () -> Unit, val done: String)

@Composable
private fun ConfirmDialog(c: Confirm?, dismiss: () -> Unit, after: () -> Unit) {
    val run = rememberRunner()
    if (c == null) return
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(c.title) },
        text = { Text(c.text) },
        confirmButton = { TextButton(onClick = { dismiss(); run(c.done, after) { c.action() } }) { Text(c.button, color = Bad, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } },
    )
}

@Composable
fun UnraidScreen(cfg: ServiceConfig, nav: Navigator, isRoot: Boolean, back: () -> Unit) {
    val api = remember(cfg) { UnraidApi(cfg) }
    val uri = LocalUriHandler.current
    var tab by rememberSaveable(cfg.id) { mutableIntStateOf(0) }
    AppScaffold(
        title = cfg.display(), isRoot = isRoot, onBack = back, badge = cfg.type, badgeName = cfg.name,
        subtitle = listOf("Dashboard", "Array", "Docker", "VMs", "Shares")[tab],
        actions = {
            IconButton(onClick = { uri.openUri(cfg.baseUrl) }) { Icon(Icons.Filled.Public, "Open Unraid web UI") }
            IconButton(onClick = { nav.push(Route.EditService(cfg.id, null)) }) { Icon(Icons.Filled.Settings, "Server settings") }
        },
        bottomTabs = listOf(
            BottomTab("Dashboard", Icons.Filled.Dashboard), BottomTab("Array", Icons.Filled.Storage),
            BottomTab("Docker", Icons.Filled.ViewInAr), BottomTab("VMs", Icons.Filled.Computer), BottomTab("Shares", Icons.Filled.Folder),
        ),
        selectedBottom = tab, onBottom = { tab = it },
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                0 -> UnraidDashboard(api, nav) { tab = it }
                1 -> UnraidArrayTab(api)
                2 -> UnraidDockerTab(api, nav)
                3 -> UnraidVmTab(api)
                else -> UnraidSharesTab(api)
            }
        }
    }
}

// ------------------------------------------------------------ Dashboard
private class UnraidDash(
    val sys: UnraidSystem?, val arr: UnraidArray?, val containers: List<UnraidContainer>?, val vms: List<UnraidVm>?,
    val notes: List<UnraidNotification>?, val error: String?,
)

@Composable
private fun UnraidDashboard(api: UnraidApi, nav: Navigator, goTab: (Int) -> Unit) {
    var confirm by remember { mutableStateOf<Confirm?>(null) }
    val run = rememberRunner()
    val l = rememberLoadable(api.cfg, "udash", autoRefreshMs = 8000) {
        var err: String? = null
        val sys = try { api.system() } catch (e: Exception) { err = e.message; null }
        val arr = try { api.array() } catch (e: Exception) { err = err ?: e.message; null }
        if (sys == null && arr == null) throw Exception(err ?: "Unraid API not reachable – Unraid 7.2+ with an API key is needed")
        UnraidDash(sys, arr, try { api.containers() } catch (e: Exception) { null }, try { api.vms() } catch (e: Exception) { null },
            try { api.notifications() } catch (e: Exception) { null }, err)
    }
    LoadableContent(l) { d ->
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { com.arrmada360.app.ui.screens.ScreenTip("unraid", androidx.compose.material.icons.Icons.Filled.Dns, "Use the tabs below for the array, Docker, VMs and shares. Live graphs and Logs are right here; container logs are in each container's ⋮ menu.") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Panel(Modifier.weight(1f), tint = Accent, onClick = { nav.push(Route.UnraidLive(api.cfg.id)) }) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ShowChart, null, tint = Accent); Spacer(Modifier.width(8.dp))
                            Text("Live graphs", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Panel(Modifier.weight(1f), tint = Warn, onClick = { nav.push(Route.UnraidLogs(api.cfg.id, null, null)) }) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Description, null, tint = Warn); Spacer(Modifier.width(8.dp))
                            Text("Logs", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
            d.sys?.let { s ->
                item {
                    Panel(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PulseDot(Good)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(s.hostname.ifEmpty { "Unraid" }, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                                    Text(listOf(if (s.version.isNotEmpty()) "Unraid ${s.version}" else "", s.cpu).filter { it.isNotEmpty() }.joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (s.uptime.isNotEmpty()) Text("Up since ${fmtDate(s.uptime, true).ifEmpty { s.uptime }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                                s.cpuPercent?.let { c ->
                                    Gauge("CPU", (c / 100).toFloat(), "${"%.0f".format(c)}%", MaterialTheme.colorScheme.primary)
                                }
                                if (s.memTotal > 0) Gauge("RAM", s.memUsed.toFloat() / s.memTotal, "${s.memUsed * 100 / s.memTotal}%", Accent2)
                                d.arr?.let { a -> if (a.total > 0) Gauge("Array", a.used.toFloat() / a.total, "${a.used * 100 / a.total}%", if (a.used.toFloat() / a.total > 0.9f) Bad else Good) }
                            }
                            if (s.memTotal > 0) Text("RAM ${humanBytes(s.memUsed)} of ${humanBytes(s.memTotal)}", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    d.arr?.let { a -> StatTile("Array", a.state.lowercase().replaceFirstChar { it.uppercase() }, if (a.state.equals("STARTED", true)) Good else Warn, Modifier.weight(1f).clickableNoRipple { goTab(1) }) }
                    d.containers?.let { c -> StatTile("Docker", "${c.count { it.state.equals("RUNNING", true) }}/${c.size}", Accent, Modifier.weight(1f).clickableNoRipple { goTab(2) }) }
                    d.vms?.let { v -> StatTile("VMs", "${v.count { it.state.equals("RUNNING", true) }}/${v.size}", Accent2, Modifier.weight(1f).clickableNoRipple { goTab(3) }) }
                }
            }
            d.arr?.let { a ->
                val hot = a.disks.filter { (it.temp ?: 0) >= 45 }
                val bad = a.disks.filter { it.errors > 0 || (it.status.isNotEmpty() && !it.status.contains("OK", true) && !it.status.contains("NP", true)) }
                if (hot.isNotEmpty() || bad.isNotEmpty()) item {
                    Panel(Modifier.fillMaxWidth(), tint = Bad) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Disk warnings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Bad)
                            bad.forEach { Text("• ${it.name}: ${it.status}${if (it.errors > 0) " · ${it.errors} errors" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface) }
                            hot.forEach { Text("• ${it.name} is ${it.temp}°C", style = MaterialTheme.typography.bodySmall, color = Warn) }
                        }
                    }
                }
            }
            val notes = d.notes
            if (notes != null) {
                item { SectionTitle(if (notes.isEmpty()) "Notifications · none unread" else "Notifications · ${notes.size} unread") }
                items(notes, key = { "n" + it.id }) { n ->
                    val c = when (n.importance.uppercase()) { "ALERT" -> Bad; "WARNING" -> Warn; else -> Accent }
                    Panel(Modifier.fillMaxWidth(), tint = c) {
                        Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Pill(n.importance.lowercase().replaceFirstChar { it.uppercase() }.ifEmpty { "Info" }, c)
                                    Spacer(Modifier.width(6.dp))
                                    if (n.timestamp.isNotEmpty()) Text(fmtDate(n.timestamp, true).ifEmpty { n.timestamp }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(n.subject.ifEmpty { n.title }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                if (n.description.isNotEmpty()) Text(n.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
                            }
                            IconButton(onClick = { run("Dismissed", { l.load(true) }) { api.archiveNotification(n.id) } }) { Icon(Icons.Filled.Close, "Dismiss") }
                        }
                    }
                }
            }
            item { SectionTitle("Power") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PowerButton("Reboot", Icons.Filled.RestartAlt, Warn, Modifier.weight(1f)) {
                        confirm = Confirm("Reboot ${api.cfg.name}?", "Every container, VM and share will go offline while the server restarts.", "Reboot", { api.power("reboot") }, "Rebooting…")
                    }
                    PowerButton("Shut down", Icons.Filled.PowerSettingsNew, Bad, Modifier.weight(1f)) {
                        confirm = Confirm("Shut down ${api.cfg.name}?", "You'll need physical or IPMI access to turn it back on.", "Shut down", { api.power("shutdown") }, "Shutting down…")
                    }
                }
            }
        }
    }
    ConfirmDialog(confirm, { confirm = null }) { l.load(true) }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)

@Composable
private fun Gauge(label: String, v: Float, text: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RingGauge(v, color, Modifier.size(88.dp), stroke = 9.dp) { Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PowerButton(label: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Panel(modifier, tint = color, onClick = onClick) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color)
            Spacer(Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// ------------------------------------------------------------ Array
@Composable
private fun UnraidArrayTab(api: UnraidApi) {
    var confirm by remember { mutableStateOf<Confirm?>(null) }
    val run = rememberRunner()
    val l = rememberLoadable(api.cfg, "uarray", autoRefreshMs = 10000) { api.array() to api.parityStatus() }
    LoadableContent(l) { (a, parity) ->
        val started = a.state.equals("STARTED", true)
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Panel(Modifier.fillMaxWidth(), tint = if (started) Good else Warn) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (a.total > 0) {
                                val p = a.used.toFloat() / a.total
                                RingGauge(p, if (p > 0.9f) Bad else MaterialTheme.colorScheme.primary, Modifier.size(96.dp), stroke = 10.dp) {
                                    Text("${(p * 100).toInt()}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Spacer(Modifier.width(16.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Pill(a.state, if (started) Good else Warn)
                                Spacer(Modifier.height(6.dp))
                                if (a.total > 0) {
                                    Text("${humanBytes(a.used)} used", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                    Text("${humanBytes(a.total - a.used)} free of ${humanBytes(a.total)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("${a.disks.count { it.kind == "Data" }} data · ${a.disks.count { it.kind == "Parity" }} parity · ${a.disks.count { it.kind == "Cache" }} cache",
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        if (started) PowerButton("Stop array", Icons.Filled.Stop, Bad, Modifier.fillMaxWidth()) {
                            confirm = Confirm("Stop the array?", "All shares, Docker containers and VMs that use the array will stop.", "Stop array", { api.setArrayState(false) }, "Stopping array…")
                        } else PowerButton("Start array", Icons.Filled.PlayArrow, Good, Modifier.fillMaxWidth()) {
                            confirm = Confirm("Start the array?", "Unraid will mount the disks and start shares, Docker and VMs.", "Start array", { api.setArrayState(true) }, "Starting array…")
                        }
                    }
                }
            }
            item {
                Panel(Modifier.fillMaxWidth(), tint = Accent2) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Parity check", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        if (parity != null) {
                            Text(listOf(parity.status, if (parity.speed.isNotEmpty()) parity.speed else "", parity.errors?.let { "$it errors" } ?: "",
                                if (parity.date.isNotEmpty()) fmtDate(parity.date, true) else "").filter { it.isNotEmpty() }.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            parity.progress?.let { p -> Spacer(Modifier.height(8.dp)); ProgressBar((p / 100).toFloat(), color = Accent2, height = 7.dp); Text("${"%.1f".format(p)}%", style = MaterialTheme.typography.labelSmall) }
                        } else Text("Status not available from this Unraid version", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(10.dp))
                        val running = parity?.running == true
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!running) {
                                TextButton(onClick = { confirm = Confirm("Start parity check?", "Read-only check; your disks will be busy for many hours.", "Start check", { api.parity("start") }, "Parity check started") }) { Text("Check") }
                                TextButton(onClick = { confirm = Confirm("Start correcting parity check?", "Writes corrections to parity if errors are found.", "Start", { api.parity("startCorrect") }, "Correcting check started") }) { Text("Check & correct") }
                            } else {
                                TextButton(onClick = { run("Paused", { l.load(true) }) { api.parity("pause") } }) { Text("Pause") }
                                TextButton(onClick = { run("Resumed", { l.load(true) }) { api.parity("resume") } }) { Text("Resume") }
                                TextButton(onClick = { confirm = Confirm("Cancel parity check?", "Progress will be lost.", "Cancel check", { api.parity("cancel") }, "Parity check cancelled") }) { Text("Cancel", color = Bad) }
                            }
                        }
                    }
                }
            }
            listOf("Parity", "Data", "Cache").forEach { kind ->
                val disks = a.disks.filter { it.kind == kind }
                if (disks.isNotEmpty()) {
                    item { SectionTitle("$kind disks") }
                    items(disks, key = { kind + it.name }) { DiskRow(it) }
                }
            }
        }
    }
    ConfirmDialog(confirm, { confirm = null }) { l.load(true) }
}

@Composable
private fun DiskRow(d: UnraidDisk) {
    val ok = d.status.contains("OK", true)
    val color = when { d.errors > 0 -> Bad; ok -> Good; d.status.contains("NP", true) || d.status.isEmpty() -> MaterialTheme.colorScheme.onSurfaceVariant; else -> Bad }
    Panel(Modifier.fillMaxWidth(), tint = color) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(color, 10.dp)
                Spacer(Modifier.width(10.dp))
                Text(d.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                d.spinning?.let { Pill(if (it) "Spinning" else "Spun down", if (it) Good else MaterialTheme.colorScheme.onSurfaceVariant) }
                Spacer(Modifier.width(6.dp))
                d.temp?.let { t -> Pill("$t°C", if (t >= 50) Bad else if (t >= 45) Warn else Accent) }
            }
            Spacer(Modifier.height(6.dp))
            Text(listOf(d.status, d.fsType, if (d.size > 0) humanBytes(d.size) else "", if (d.errors > 0) "${d.errors} errors" else "").filter { it.isNotEmpty() }.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (d.fsSize > 0) {
                val used = 1f - d.fsFree.toFloat() / d.fsSize
                Spacer(Modifier.height(6.dp))
                ProgressBar(used, color = if (used > 0.9f) Bad else MaterialTheme.colorScheme.primary, height = 6.dp)
                Text("${humanBytes(d.fsSize - d.fsFree)} used · ${humanBytes(d.fsFree)} free", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ------------------------------------------------------------ Docker
@Composable
private fun UnraidDockerTab(api: UnraidApi, nav: Navigator) {
    val l = rememberLoadable(api.cfg, "udocker", autoRefreshMs = 8000) { api.containers() }
    val run = rememberRunner()
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var confirm by remember { mutableStateOf<Confirm?>(null) }
    Column {
        OutlinedTextField(query, { query = it }, singleLine = true, placeholder = { Text("Filter containers") },
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 4.dp), shape = RoundedCornerShape(12.dp))
        val all = l.data ?: emptyList()
        ChipRow(listOf("All", "Running", "Stopped"), filter, { filter = it }, Modifier.padding(vertical = 8.dp),
            counts = listOf(all.size, all.count { it.state.equals("RUNNING", true) }, all.count { !it.state.equals("RUNNING", true) }))
        LoadableContent(l, empty = { it.isEmpty() }, emptyText = "No containers") { list ->
            val shown = list.filter { when (filter) { 1 -> it.state.equals("RUNNING", true); 2 -> !it.state.equals("RUNNING", true); else -> true } }
                .filter { query.isBlank() || it.name.contains(query.trim(), true) || it.image.contains(query.trim(), true) }
            LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            confirm = Confirm("Start all stopped containers?", "${list.count { !it.state.equals("RUNNING", true) }} containers will start.", "Start all",
                                { list.filter { !it.state.equals("RUNNING", true) }.forEach { api.containerAction(it.id, "start") } }, "Starting containers…")
                        }) { Icon(Icons.Filled.PlayArrow, null, tint = Good); Text(" Start all") }
                        TextButton(onClick = {
                            confirm = Confirm("Stop all running containers?", "Every running container (including Arrmada360's own apps) will stop.", "Stop all",
                                { list.filter { it.state.equals("RUNNING", true) }.forEach { api.containerAction(it.id, "stop") } }, "Stopping containers…")
                        }) { Icon(Icons.Filled.Stop, null, tint = Bad); Text(" Stop all") }
                    }
                }
                items(shown, key = { it.id }) { c ->
                    val running = c.state.equals("RUNNING", true)
                    var menu by remember { mutableStateOf(false) }
                    Panel(Modifier.fillMaxWidth(), tint = if (running) Good else Bad) {
                        Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (running) PulseDot(Good, size = 8.dp) else StatusDot(Bad, 8.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                                Text(c.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(c.status.ifEmpty { c.state.lowercase() }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                if (c.image.isNotEmpty()) Text(c.image, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (c.autoStart) Pill("Autostart", Accent)
                            }
                            if (running) IconButton(onClick = { run("Restarting ${c.name}", { l.load(true) }) { api.containerAction(c.id, "stop"); api.containerAction(c.id, "start") } }) { Icon(Icons.Filled.Refresh, "Restart") }
                            if (running) IconButton(onClick = { run("Stopped ${c.name}", { l.load(true) }) { api.containerAction(c.id, "stop") } }) { Icon(Icons.Filled.Stop, "Stop", tint = Bad) }
                            else IconButton(onClick = { run("Started ${c.name}", { l.load(true) }) { api.containerAction(c.id, "start") } }) { Icon(Icons.Filled.PlayArrow, "Start", tint = Good) }
                            Box {
                                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, null) }
                                DropdownMenu(menu, { menu = false }) {
                                    DropdownMenuItem(text = { Text("Logs") }, leadingIcon = { Icon(Icons.Filled.Description, null) }, onClick = { menu = false; nav.push(Route.UnraidLogs(api.cfg.id, c.id, c.name)) })
                                    DropdownMenuItem(text = { Text("Pause") }, leadingIcon = { Icon(Icons.Filled.Pause, null) }, onClick = { menu = false; run("Paused ${c.name}", { l.load(true) }) { api.gql("mutation { docker { pause(id: \"${c.id}\") { id state } } }") } })
                                    DropdownMenuItem(text = { Text("Unpause") }, leadingIcon = { Icon(Icons.Filled.PlayArrow, null) }, onClick = { menu = false; run("Unpaused ${c.name}", { l.load(true) }) { api.gql("mutation { docker { unpause(id: \"${c.id}\") { id state } } }") } })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    ConfirmDialog(confirm, { confirm = null }) { l.load(true) }
}

// ------------------------------------------------------------ VMs
@Composable
private fun UnraidVmTab(api: UnraidApi) {
    val l = rememberLoadable(api.cfg, "uvms", autoRefreshMs = 10000) { api.vms() }
    val run = rememberRunner()
    var confirm by remember { mutableStateOf<Confirm?>(null) }
    LoadableContent(l, empty = { it.isEmpty() }, emptyText = "No virtual machines") { list ->
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list, key = { it.id }) { v ->
                val st = v.state.uppercase()
                val running = st == "RUNNING"
                val paused = st == "PAUSED"
                val color = when { running -> Good; paused -> Warn; else -> Bad }
                Panel(Modifier.fillMaxWidth(), tint = color) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Computer, null, tint = color) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(v.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Pill(v.state.lowercase().replaceFirstChar { it.uppercase() }, color)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            when {
                                running -> {
                                    TextButton(onClick = { run("Pausing ${v.name}", { l.load(true) }) { api.vmAction(v.id, "pause") } }) { Text("Pause") }
                                    TextButton(onClick = { run("Rebooting ${v.name}", { l.load(true) }) { api.vmAction(v.id, "reboot") } }) { Text("Reboot") }
                                    TextButton(onClick = { run("Stopping ${v.name}", { l.load(true) }) { api.vmAction(v.id, "stop") } }) { Text("Stop") }
                                    TextButton(onClick = { confirm = Confirm("Force stop ${v.name}?", "Like pulling the power plug – unsaved work in the VM is lost.", "Force stop", { api.vmAction(v.id, "forceStop") }, "Force stopped") }) { Text("Force stop", color = Bad) }
                                }
                                paused -> {
                                    TextButton(onClick = { run("Resuming ${v.name}", { l.load(true) }) { api.vmAction(v.id, "resume") } }) { Text("Resume") }
                                    TextButton(onClick = { confirm = Confirm("Force stop ${v.name}?", "Unsaved work in the VM is lost.", "Force stop", { api.vmAction(v.id, "forceStop") }, "Force stopped") }) { Text("Force stop", color = Bad) }
                                }
                                else -> TextButton(onClick = { run("Starting ${v.name}", { l.load(true) }) { api.vmAction(v.id, "start") } }) { Text("Start", color = Good, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
        }
    }
    ConfirmDialog(confirm, { confirm = null }) { l.load(true) }
}

// ------------------------------------------------------------ Shares
@Composable
private fun UnraidSharesTab(api: UnraidApi) {
    val l = rememberLoadable(api.cfg, "ushares") { api.shares() }
    LoadableContent(l, empty = { it.isEmpty() }, emptyText = "No shares") { list ->
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Shares", list.size.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    StatTile("Used", humanBytes(list.sumOf { it.used }), Accent2, Modifier.weight(1f))
                }
            }
            items(list, key = { it.name }) { s ->
                val total = if (s.size > 0) s.size else s.used + s.free
                Panel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Folder, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                if (s.comment.isNotEmpty()) Text(s.comment, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                            if (s.cache.isNotEmpty()) Pill("Cache: ${s.cache}", Accent)
                        }
                        if (total > 0) {
                            Spacer(Modifier.height(8.dp))
                            ProgressBar(s.used.toFloat() / total, height = 6.dp)
                            Text("${humanBytes(s.used)} used · ${humanBytes(s.free)} free", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
