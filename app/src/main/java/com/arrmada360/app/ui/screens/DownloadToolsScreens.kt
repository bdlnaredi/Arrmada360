package com.arrmada360.app.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arrmada360.app.api.DlItem
import com.arrmada360.app.api.DownloadClients
import com.arrmada360.app.data.ConfigStore
import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.notify.AutomationWorker
import com.arrmada360.app.notify.SeedRules
import com.arrmada360.app.notify.SpeedSchedule
import com.arrmada360.app.ui.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

// ============================================================ Speed scheduler
@Composable
fun SpeedSchedulerScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    val run = rememberRunner()
    var enabled by remember { mutableStateOf(SpeedSchedule.enabled) }
    var start by remember { mutableIntStateOf(SpeedSchedule.startMin) }
    var end by remember { mutableIntStateOf(SpeedSchedule.endMin) }
    var down by remember { mutableStateOf(SpeedSchedule.limitDown.toString()) }
    var up by remember { mutableStateOf(SpeedSchedule.limitUp.toString()) }
    var fullDown by remember { mutableStateOf(SpeedSchedule.fullDown.toString()) }
    var fullUp by remember { mutableStateOf(SpeedSchedule.fullUp.toString()) }
    var picked by remember { mutableStateOf(SpeedSchedule.clients) }
    val all = ConfigStore.services.filter { it.type.isDownloader }
    val supported = all.filter { DownloadClients.of(it)?.supportsSpeedLimit == true }

    fun save() {
        SpeedSchedule.startMin = start; SpeedSchedule.endMin = end
        SpeedSchedule.limitDown = down.toIntOrNull() ?: 0; SpeedSchedule.limitUp = up.toIntOrNull() ?: 0
        SpeedSchedule.fullDown = fullDown.toIntOrNull() ?: 0; SpeedSchedule.fullUp = fullUp.toIntOrNull() ?: 0
        SpeedSchedule.clients = picked
        SpeedSchedule.enabled = enabled
        AutomationWorker.schedule(ctx.applicationContext)
    }
    fun pickTime(cur: Int, set: (Int) -> Unit) {
        TimePickerDialog(ctx, { _, h, m -> set(h * 60 + m) }, cur / 60, cur % 60, true).show()
    }

    AppScaffold(title = "Speed scheduler", isRoot = false, onBack = back) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Panel(Modifier.fillMaxWidth(), tint = if (enabled) Good else Accent) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Schedule, null, tint = if (enabled) Good else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Limit speeds during the day", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(if (enabled) "Now: ${if (SpeedSchedule.inLimitedWindow()) "limited" else "full speed"}" else "Off",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(enabled, { enabled = it; save(); if (it) run("Schedule on – limits applied") { SpeedSchedule.applyNow(true) } })
                    }
                }
            }
            item {
                Panel(Modifier.fillMaxWidth(), tint = Warn) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Limited period", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Warn)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { pickTime(start) { start = it } }, modifier = Modifier.weight(1f)) { Text("From ${SpeedSchedule.fmt(start)}") }
                            OutlinedButton(onClick = { pickTime(end) { end = it } }, modifier = Modifier.weight(1f)) { Text("Until ${SpeedSchedule.fmt(end)}") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NumField("Download KB/s", down, { down = it }, Modifier.weight(1f))
                            NumField("Upload KB/s", up, { up = it }, Modifier.weight(1f))
                        }
                        Text("1 MB/s = 1024 KB/s. 0 means unlimited.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Panel(Modifier.fillMaxWidth(), tint = Good) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Rest of the day", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Good)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NumField("Download KB/s", fullDown, { fullDown = it }, Modifier.weight(1f))
                            NumField("Upload KB/s", fullUp, { fullUp = it }, Modifier.weight(1f))
                        }
                        Text("Leave at 0 for full speed.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { SectionTitle("Apply to") }
            if (all.isEmpty()) item { Text("Add a download client first.", style = MaterialTheme.typography.bodySmall) }
            items(all, key = { it.id }) { c ->
                val ok = c in supported
                Panel(Modifier.fillMaxWidth(), tint = c.type.tint) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ServiceLogo(c.type, 28.dp, c.name); Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            if (!ok) Text("Speed limits aren't available for ${c.type.label}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Checkbox(ok && (picked.isEmpty() || c.id in picked), enabled = ok, onCheckedChange = { on ->
                            val base = if (picked.isEmpty()) supported.map { it.id }.toSet() else picked
                            picked = if (on) base + c.id else base - c.id
                        })
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GradientButton("Save", Modifier.weight(1f)) { save(); ctx.toast("Schedule saved") }
                    OutlinedButton(onClick = { save(); run("Limits applied now") { SpeedSchedule.applyNow(true) } }, modifier = Modifier.weight(1f).height(52.dp)) { Text("Apply now") }
                }
            }
            item {
                Text("Arrmada360 checks every 15 minutes and switches limits for you, even when the app is closed. Android may delay this a little when your phone is asleep.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun NumField(label: String, v: String, set: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(v, { set(it.filter { c -> c.isDigit() }.take(7)) }, label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = modifier)
}

// ============================================================ Ratio & seeding manager
private data class SeedRow(val cfg: ServiceConfig, val item: DlItem)

@Composable
fun SeedingManagerScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    val run = rememberRunner()
    val l = rememberLoadable("seeding") {
        coroutineScope {
            ConfigStore.services.filter { it.type in SeedRules.torrentTypes }.map { c ->
                async { try { DownloadClients.of(c)?.snapshot()?.items?.filter { it.done }?.map { SeedRow(c, it) } ?: emptyList() } catch (e: Exception) { emptyList() } }
            }.awaitAll().flatten()
        }
    }
    var ratio by remember { mutableFloatStateOf(SeedRules.minRatio) }
    var days by remember { mutableIntStateOf(SeedRules.minDays) }
    var mode by remember { mutableStateOf(SeedRules.mode) }
    var cats by remember { mutableStateOf(SeedRules.categories) }
    var auto by remember { mutableStateOf(SeedRules.autoClean) }
    var delData by remember { mutableStateOf(SeedRules.deleteData) }
    var onlyMatching by rememberSaveable { mutableStateOf(true) }
    var sort by rememberSaveable { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var confirm by remember { mutableStateOf(false) }
    fun save() {
        SeedRules.minRatio = ratio; SeedRules.minDays = days; SeedRules.mode = mode; SeedRules.categories = cats
        SeedRules.autoClean = auto; SeedRules.deleteData = delData
        AutomationWorker.schedule(ctx.applicationContext)
    }
    val key = { r: SeedRow -> r.cfg.id + "/" + r.item.id }
    LaunchedEffect(ratio, days, mode, cats, auto, delData) { save() }

    AppScaffold(title = "Seeding manager", isRoot = false, onBack = back) { pad ->
        LoadableContent(l, Modifier.padding(pad)) { rows ->
            val matching = rows.filter { SeedRules.matches(it.item) }
            val shown = (if (onlyMatching) matching else rows).let { r ->
                when (sort) { 1 -> r.sortedByDescending { it.item.seedTime }; 2 -> r.sortedByDescending { it.item.size }; else -> r.sortedByDescending { it.item.ratio } }
            }
            LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Panel(Modifier.fillMaxWidth(), tint = Accent) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Rules", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Ratio at least %.1f".format(ratio), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                            Slider(ratio, { ratio = (it * 10).toInt() / 10f }, valueRange = 0.1f..10f)
                            Text("Seeding for at least $days days", style = MaterialTheme.typography.labelLarge)
                            Slider(days.toFloat(), { days = it.toInt() }, valueRange = 1f..90f)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("either" to "Either rule", "ratio" to "Ratio only", "days" to "Time only").forEach { (k, lbl) ->
                                    FilterChip(mode == k, { mode = k }, label = { Text(lbl) })
                                }
                            }
                            OutlinedTextField(cats, { cats = it }, singleLine = true, label = { Text("Only categories (comma separated, blank = all)") }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text("Remove automatically", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("Every 15 minutes, finished torrents that match are removed and you get a notification.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(auto, { auto = it })
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(delData, { delData = it })
                                Text("Also delete the downloaded files", style = MaterialTheme.typography.bodySmall, color = if (delData) Bad else MaterialTheme.colorScheme.onSurface)
                            }
                            if (delData) Text("Only use this if Sonarr/Radarr copy (not hard-link) files, or you're sure they've been imported.", style = MaterialTheme.typography.labelSmall, color = Bad)
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("Finished", "${rows.size}", Accent, Modifier.weight(1f))
                        StatTile("Match rules", "${matching.size}", Warn, Modifier.weight(1f))
                        StatTile("Space", humanBytes(matching.sumOf { it.item.size }), Good, Modifier.weight(1f))
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(onlyMatching, { onlyMatching = !onlyMatching; selected = emptySet() }, label = { Text("Only matching") })
                        Spacer(Modifier.width(6.dp))
                        ChipRow(listOf("Ratio", "Seed time", "Size"), sort, { sort = it }, Modifier.weight(1f))
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { selected = if (selected.size == shown.size) emptySet() else shown.map(key).toSet() }) { Text(if (selected.size == shown.size && shown.isNotEmpty()) "Clear" else "Select all") }
                        Spacer(Modifier.weight(1f))
                        Button(enabled = selected.isNotEmpty(), onClick = { confirm = true }, colors = ButtonDefaults.buttonColors(containerColor = Bad)) {
                            Icon(Icons.Filled.Delete, null); Text(" Remove ${selected.size}")
                        }
                    }
                }
                if (shown.isEmpty()) item { EmptyBox(if (onlyMatching) "Nothing matches your rules yet" else "No finished torrents") }
                items(shown, key = key) { r ->
                    val sel = key(r) in selected
                    val hit = SeedRules.matches(r.item)
                    Panel(Modifier.fillMaxWidth(), tint = if (sel) MaterialTheme.colorScheme.primary else if (hit) Warn else Accent, onClick = { selected = if (sel) selected - key(r) else selected + key(r) }) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(sel, { selected = if (sel) selected - key(r) else selected + key(r) })
                            Column(Modifier.weight(1f)) {
                                Text(r.item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(top = 4.dp)) {
                                    Pill("Ratio %.2f".format(r.item.ratio.coerceAtLeast(0.0)), if (r.item.ratio >= ratio) Good else Accent)
                                    if (r.item.seedTime >= 0) Pill("${r.item.seedTime / 86400}d seeding", if (r.item.seedTime >= days * 86400L) Good else Accent2)
                                    Pill(humanBytes(r.item.size), Accent2)
                                    if (r.item.category.isNotEmpty()) Pill(r.item.category, Pink)
                                }
                                Text(r.cfg.name + " · " + r.item.state, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Remove ${selected.size} torrent${if (selected.size == 1) "" else "s"}?") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(delData, { delData = it }); Text("Also delete files") }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                confirm = false
                val pick = l.data.orEmpty().filter { key(it) in selected }
                val withData = delData
                selected = emptySet()
                run("Removed ${pick.size}", { l.load(true) }) {
                    pick.groupBy { it.cfg }.forEach { (c, list) -> DownloadClients.of(c)?.delete(list.map { it.item.id }, withData) }
                }
            }) { Text("Remove", color = Bad) }
        },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } },
    )
}
