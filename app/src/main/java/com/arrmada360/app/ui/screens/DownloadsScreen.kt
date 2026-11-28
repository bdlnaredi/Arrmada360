package com.arrmada360.app.ui.screens

import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import com.arrmada360.app.ui.Rainbow
import com.arrmada360.app.ui.StatusDot
import androidx.compose.foundation.border
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.arrmada360.app.api.SpeedHistory
import com.arrmada360.app.ui.Panel
import com.arrmada360.app.ui.Sparkline
import com.arrmada360.app.ui.animatedLong
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arrmada360.app.api.DlItem
import com.arrmada360.app.api.DownloadClients
import com.arrmada360.app.api.QbitClient
import com.arrmada360.app.api.SabClient
import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.ui.AppScaffold
import com.arrmada360.app.ui.Bad
import com.arrmada360.app.ui.ChipRow
import com.arrmada360.app.ui.EmptyBox
import com.arrmada360.app.ui.Good
import com.arrmada360.app.ui.LoadableContent
import com.arrmada360.app.ui.Pill
import com.arrmada360.app.ui.ProgressBar
import com.arrmada360.app.ui.Warn
import com.arrmada360.app.ui.fmtEpoch
import com.arrmada360.app.ui.humanBytes
import com.arrmada360.app.ui.humanEta
import com.arrmada360.app.ui.humanSpeed
import com.arrmada360.app.ui.rememberLoadable
import com.arrmada360.app.ui.rememberRunner

private val DL_SORTS = listOf("Added (newest)", "Name", "Progress", "Size", "Download speed", "Upload speed", "Ratio")

@Composable
fun DownloadsScreen(cfg: ServiceConfig, nav: com.arrmada360.app.ui.Navigator, isRoot: Boolean, back: () -> Unit) {
    val client = remember(cfg) { DownloadClients.of(cfg) } ?: return EmptyBox("Unsupported client")
    val snap = rememberLoadable(cfg, autoRefreshMs = 2500) { DownloadClients.snapshot(client) }
    val run = rememberRunner()
    var tab by rememberSaveable(cfg.id) { mutableIntStateOf(0) }
    var filter by rememberSaveable(cfg.id) { mutableIntStateOf(0) }
    var sort by rememberSaveable(cfg.id) { mutableIntStateOf(0) }
    var selection by remember { mutableStateOf(setOf<String>()) }
    var selectMode by remember { mutableStateOf(false) }
    var visibleIds by remember { mutableStateOf(listOf<String>()) }
    var catFor by remember { mutableStateOf<List<String>?>(null) }
    var catFilter by rememberSaveable(cfg.id + "cat") { mutableStateOf<String?>(null) }
    var selMenu by remember { mutableStateOf(false) }
    val selecting = selectMode || selection.isNotEmpty()
    var action by remember { mutableStateOf<DlItem?>(null) }
    var addOpen by remember { mutableStateOf(false) }
    var deleteIds by remember { mutableStateOf<List<String>?>(null) }
    var menu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }

    val s = snap.data
    val subtitle = if (s != null) "↓ ${humanSpeed(s.dlSpeed)}" + (if (client.isUsenet) "" else "   ↑ ${humanSpeed(s.upSpeed)}") +
        (if (s.globalPaused) "   · PAUSED" else "") + (if (s.freeSpace > 0) "   · ${humanBytes(s.freeSpace)} free" else "") else null
    val refresh = { snap.load(true) }

    AppScaffold(
        title = if (selecting) "${selection.size} selected" else cfg.name,
        subtitle = if (!selecting) subtitle else "Tap items to select",
        isRoot = isRoot && !selecting, onBack = { if (selecting) { selection = emptySet(); selectMode = false } else back() },
        badge = if (!selecting) cfg.type else null, badgeName = cfg.name,
        bottomTabs = if (client.hasHistory) listOf(
            com.arrmada360.app.ui.BottomTab("Queue", androidx.compose.material.icons.Icons.Filled.Download),
            com.arrmada360.app.ui.BottomTab("History", androidx.compose.material.icons.Icons.Filled.History),
            com.arrmada360.app.ui.BottomTab("Server", androidx.compose.material.icons.Icons.Filled.Dns),
        ) else if (client is com.arrmada360.app.api.RssClient) listOf(
            com.arrmada360.app.ui.BottomTab("Torrents", androidx.compose.material.icons.Icons.Filled.Download),
            com.arrmada360.app.ui.BottomTab("RSS", androidx.compose.material.icons.Icons.Filled.RssFeed),
            com.arrmada360.app.ui.BottomTab("Server", androidx.compose.material.icons.Icons.Filled.Dns),
        ) else listOf(
            com.arrmada360.app.ui.BottomTab("Torrents", androidx.compose.material.icons.Icons.Filled.Download),
            com.arrmada360.app.ui.BottomTab("Server", androidx.compose.material.icons.Icons.Filled.Dns),
        ),
        selectedBottom = tab, onBottom = { tab = it },
        actions = {
            if (selecting) {
                val none = selection.isEmpty()
                IconButton(enabled = !none, onClick = { val ids = selection.toList(); run("Resumed ${ids.size}", refresh) { client.resume(ids) } }) { Icon(Icons.Filled.PlayArrow, "Resume") }
                IconButton(enabled = !none, onClick = { val ids = selection.toList(); run("Paused ${ids.size}", refresh) { client.pause(ids) } }) { Icon(Icons.Filled.Pause, "Pause") }
                if (client.supportsCategories) IconButton(enabled = !none, onClick = { catFor = selection.toList() }) { Icon(Icons.AutoMirrored.Filled.Label, "Category") }
                IconButton(enabled = !none, onClick = { deleteIds = selection.toList() }) { Icon(Icons.Filled.Delete, "Delete") }
                Box {
                    IconButton(onClick = { selMenu = true }) { Icon(Icons.Filled.MoreVert, "More") }
                    DropdownMenu(selMenu, { selMenu = false }) {
                        DropdownMenuItem(text = { Text("Select all shown (${visibleIds.size})") }, leadingIcon = { Icon(Icons.Filled.SelectAll, null) }, onClick = { selMenu = false; selection = selection + visibleIds })
                        DropdownMenuItem(text = { Text("Invert selection") }, onClick = { selMenu = false; selection = visibleIds.filter { it !in selection }.toSet() })
                        DropdownMenuItem(text = { Text("Select none") }, onClick = { selMenu = false; selection = emptySet() })
                        val c = client
                        if (c is QbitClient && !none) {
                            DropdownMenuItem(text = { Text("Force start") }, onClick = { selMenu = false; val ids = selection.toList(); run("Force started", refresh) { c.forceStart(ids) } })
                            DropdownMenuItem(text = { Text("Force recheck") }, onClick = { selMenu = false; val ids = selection.toList(); run("Recheck started", refresh) { c.recheck(ids) } })
                        }
                        DropdownMenuItem(text = { Text("Done") }, onClick = { selMenu = false; selection = emptySet(); selectMode = false })
                    }
                }
            } else {
                IconButton(onClick = {
                    if (s?.globalPaused == true) run("Resumed all", refresh) { client.resumeAll() } else run("Paused all", refresh) { client.pauseAll() }
                }) { Icon(if (s?.globalPaused == true) Icons.Filled.PlayArrow else Icons.Filled.Pause, "Pause/resume all") }
                Box {
                    IconButton(onClick = { sortMenu = true }) { Icon(Icons.Filled.Sort, "Sort") }
                    DropdownMenu(sortMenu, { sortMenu = false }) {
                        DL_SORTS.forEachIndexed { i, label ->
                            DropdownMenuItem(text = { Text((if (i == sort) "✓ " else "    ") + label) }, onClick = { sort = i; sortMenu = false })
                        }
                    }
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "More") }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text("Select multiple") }, leadingIcon = { Icon(Icons.Filled.Checklist, null) }, onClick = { menu = false; selectMode = true })
                        if (client.supportsCategories) DropdownMenuItem(text = { Text("Manage categories") }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, null) }, onClick = { menu = false; catFor = emptyList() })
                        DropdownMenuItem(text = { Text("Resume all") }, onClick = { menu = false; run("Resumed all", refresh) { client.resumeAll() } })
                        DropdownMenuItem(text = { Text("Pause all") }, onClick = { menu = false; run("Paused all", refresh) { client.pauseAll() } })
                        if (client is QbitClient) DropdownMenuItem(text = { Text("Toggle alternative speed limits") }, leadingIcon = { Icon(Icons.Filled.Speed, null) },
                            onClick = { menu = false; run("Speed limit mode toggled") { client.toggleAltSpeed() } })
                        if (client is SabClient) listOf(100, 75, 50, 25).forEach { p ->
                            DropdownMenuItem(text = { Text("Speed limit $p%") }, onClick = { menu = false; run("Speed limit set to $p%") { client.setSpeedLimit(p) } })
                        }
                    }
                }
            }
        },
        fab = { if (tab == 0 && !selecting) FloatingActionButton(onClick = { addOpen = true }, containerColor = MaterialTheme.colorScheme.primary) { Icon(Icons.Filled.Add, "Add") } },
    ) { pad ->
        Column(Modifier.padding(pad)) {
            val hasRss = client is com.arrmada360.app.api.RssClient
            val serverTab = if (client.hasHistory || hasRss) 2 else 1
            if (tab == serverTab) {
                DownloaderServerTab(cfg, nav)
            } else if (tab == 1 && hasRss) {
                RssTab(cfg, client)
            } else if (tab == 1) {
                val h = rememberLoadable(cfg, "history") { client.history() }
                LoadableContent(h, empty = { it.isEmpty() }, emptyText = "No history") { list ->
                    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                        items(list) { e ->
                            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Text(e.name, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Pill(e.status, if (e.failed) Bad else Good)
                                    Spacer(Modifier.width(8.dp))
                                    Text("${humanBytes(e.size)} · ${fmtEpoch(e.time)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            } else {
                val filters = if (client.isUsenet) listOf("All", "Downloading", "Paused", "Queued") else listOf("All", "Downloading", "Seeding", "Completed", "Paused", "Active", "Errored")
                val all = s?.items ?: emptyList()
                fun matches(label: String, it: DlItem): Boolean = when (label) {
                    "Downloading" -> !it.done && !it.paused && (it.state.contains("Download", true) || it.state.contains("Stalled", true) || it.dlSpeed > 0 || client.isUsenet)
                    "Seeding" -> it.done && !it.paused
                    "Completed" -> it.done
                    "Paused" -> it.paused
                    "Queued" -> it.state.contains("Queue", true)
                    "Active" -> it.dlSpeed > 0 || it.upSpeed > 0
                    "Errored" -> it.error
                    else -> true
                }
                SpeedHeader(cfg, client.isUsenet, s)
                ChipRow(filters, filter, { filter = it }, Modifier.padding(vertical = 8.dp), counts = filters.map { f -> all.count { matches(f, it) } })
                val cats = all.map { it.category }.filter { it.isNotEmpty() }.distinct().sorted()
                if (cats.isNotEmpty()) {
                    val catNames = listOf("Any category") + cats
                    ChipRow(catNames, catFilter?.let { cats.indexOf(it) + 1 } ?: 0, { i -> catFilter = if (i == 0) null else cats[i - 1] }, Modifier.padding(bottom = 8.dp),
                        counts = listOf(all.size) + cats.map { c -> all.count { it.category == c } })
                }
                LoadableContent(snap, empty = { it.items.isEmpty() }, emptyText = "Nothing downloading") { data ->
                    val label = filters.getOrElse(filter) { "All" }
                    val shown = data.items.filter { matches(label, it) && (catFilter == null || it.category == catFilter) }.let { l ->
                        when (sort) {
                            1 -> l.sortedBy { it.name.lowercase() }
                            2 -> l.sortedBy { it.progress }
                            3 -> l.sortedByDescending { it.size }
                            4 -> l.sortedByDescending { it.dlSpeed }
                            5 -> l.sortedByDescending { it.upSpeed }
                            6 -> l.sortedByDescending { it.ratio }
                            else -> if (client.isUsenet) l else l.sortedByDescending { it.addedOn }
                        }
                    }
                    androidx.compose.runtime.LaunchedEffect(shown) { visibleIds = shown.map { it.id } }
                    LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { com.arrmada360.app.ui.screens.ScreenTip("downloads", androidx.compose.material.icons.Icons.Filled.TouchApp, "Swipe a download right to pause or resume it and left to delete it. Long-press to select several and change category, pause or delete.") }
                        items(shown, key = { it.id }) { item ->
                            val sel = item.id in selection
                            SwipeRow(
                                paused = item.paused,
                                modifier = Modifier.animateItem(),
                                onSwipeRight = { if (item.paused) run("Resumed", refresh) { client.resume(listOf(item.id)) } else run("Paused", refresh) { client.pause(listOf(item.id)) } },
                                onSwipeLeft = { deleteIds = listOf(item.id) },
                            ) {
                            DownloadRow(
                                item, sel, client.isUsenet, selecting,
                                onClick = {
                                    if (selecting) {
                                        selection = if (sel) selection - item.id else selection + item.id
                                    } else {
                                        action = item
                                    }
                                },
                                onLongClick = { selection = if (sel) selection - item.id else selection + item.id },
                                onToggle = { if (item.paused) run(null, refresh) { client.resume(listOf(item.id)) } else run(null, refresh) { client.pause(listOf(item.id)) } },
                            )
                            }
                        }
                    }
                }
            }
        }
    }

    action?.let { item ->
        AlertDialog(
            onDismissRequest = { action = null },
            title = { Text(item.name, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${item.state} · ${(item.progress * 100).toInt()}% of ${humanBytes(item.size)}")
                    if (item.category.isNotEmpty()) Text("Category: ${item.category}")
                    if (item.ratio >= 0 && !client.isUsenet) Text("Ratio: %.2f".format(item.ratio))
                    if (item.addedOn > 0) Text("Added: ${fmtEpoch(item.addedOn)}")
                    Spacer(Modifier.height(8.dp))
                    val c = client
                    if (c is QbitClient) {
                        TextButton(onClick = { action = null; run("Force started", refresh) { c.forceStart(listOf(item.id)) } }) { Text("Force start") }
                        TextButton(onClick = { action = null; run("Recheck started", refresh) { c.recheck(listOf(item.id)) } }) { Text("Force recheck") }
                    }
                    if (client.supportsCategories) TextButton(onClick = { action = null; catFor = listOf(item.id) }) { Text(if (item.category.isEmpty()) "Set category" else "Change category") }
                    TextButton(onClick = { action = null; selection = setOf(item.id) }) { Text("Select (multi-select mode)") }
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = { action = null; deleteIds = listOf(item.id) }) { Text("Delete", color = Bad) }
                    TextButton(onClick = {
                        action = null
                        if (item.paused) run("Resumed", refresh) { client.resume(listOf(item.id)) } else run("Paused", refresh) { client.pause(listOf(item.id)) }
                    }) { Text(if (item.paused) "Resume" else "Pause") }
                }
            },
            dismissButton = { TextButton(onClick = { action = null }) { Text("Close") } },
        )
    }

    deleteIds?.let { ids ->
        var withData by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { deleteIds = null },
            title = { Text(if (ids.size == 1) "Delete download?" else "Delete ${ids.size} downloads?") },
            text = { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(withData, { withData = it }); Text("Also delete downloaded files") } },
            confirmButton = {
                TextButton(onClick = {
                    deleteIds = null; selection = emptySet(); selectMode = false
                    run("Deleted", refresh) { client.delete(ids, withData) }
                }) { Text("Delete", color = Bad) }
            },
            dismissButton = { TextButton(onClick = { deleteIds = null }) { Text("Cancel") } },
        )
    }

    catFor?.let { ids ->
        CategoryDialog(client, ids, s?.items ?: emptyList(), onDismiss = { catFor = null }) {
            catFor = null; selection = emptySet(); selectMode = false; snap.load(true)
        }
    }

    if (addOpen) {
        var link by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addOpen = false },
            title = { Text(if (client.isUsenet) "Add NZB by URL" else "Add torrent") },
            text = {
                OutlinedTextField(link, { link = it }, label = { Text(if (client.isUsenet) "NZB URL" else "Magnet link or .torrent URL") }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(enabled = link.isNotBlank(), onClick = {
                    val u = link.trim(); addOpen = false
                    run("Added", refresh) { client.addUrl(u) }
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { addOpen = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DownloadRow(item: DlItem, selected: Boolean, usenet: Boolean, selecting: Boolean, onClick: () -> Unit, onLongClick: () -> Unit, onToggle: () -> Unit) {
    val color = when {
        item.error -> Bad
        item.paused -> MaterialTheme.colorScheme.onSurfaceVariant
        item.done -> Good
        item.state.contains("Stalled", true) -> Warn
        else -> MaterialTheme.colorScheme.primary
    }
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) SolidColor(MaterialTheme.colorScheme.primaryContainer)
                else Brush.linearGradient(listOf(color.copy(alpha = 0.16f).compositeOver(MaterialTheme.colorScheme.surface), MaterialTheme.colorScheme.surface)),
            )
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(6.dp))
                    Text("${(item.progress * 100).toInt()}%", style = MaterialTheme.typography.titleSmall, color = color)
                }
                Spacer(Modifier.height(6.dp))
                ProgressBar(item.progress, color = color, height = 6.dp)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill(item.state, color)
                    Text("  ${humanBytes(item.size)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    if (!item.done && !item.paused && item.eta != 0L) Text(humanEta(item.eta), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (item.dlSpeed > 0 || item.upSpeed > 0) Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.dlSpeed > 0) { Icon(Icons.Filled.ArrowDownward, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary); Text(humanSpeed(item.dlSpeed), style = MaterialTheme.typography.labelSmall) }
                    if (item.upSpeed > 0 && !usenet) {
                        Spacer(Modifier.width(10.dp))
                        Icon(Icons.Filled.ArrowUpward, null, Modifier.size(12.dp), tint = Good); Text(humanSpeed(item.upSpeed), style = MaterialTheme.typography.labelSmall)
                    }
                    if (item.category.isNotEmpty()) { Spacer(Modifier.width(10.dp)); Pill(item.category, MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            if (selected) Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp))
            else if (selecting) Icon(Icons.Filled.RadioButtonUnchecked, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp))
            else IconButton(onClick = onToggle) { Icon(if (item.paused) Icons.Filled.PlayArrow else Icons.Filled.Pause, if (item.paused) "Resume" else "Pause") }
        }
    }
}

@Composable
private fun SpeedHeader(cfg: ServiceConfig, usenet: Boolean, s: com.arrmada360.app.api.DlSnapshot?) {
    val down = SpeedHistory.down[cfg.id] ?: emptyList()
    val up = SpeedHistory.up[cfg.id] ?: emptyList()
    val dl = animatedLong(s?.dlSpeed ?: 0)
    val ul = animatedLong(s?.upSpeed ?: 0)
    Panel(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Box(Modifier.fillMaxWidth().height(112.dp)) {
            if (!usenet && up.size > 1) Sparkline(up, Good, Modifier.matchParentSize().padding(top = 40.dp), fill = true)
            if (down.size > 1) Sparkline(down, MaterialTheme.colorScheme.primary, Modifier.matchParentSize().padding(top = 30.dp), fill = true)
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ArrowDownward, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(humanSpeed(dl), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Text("Download", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!usenet) Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ArrowUpward, null, Modifier.size(14.dp), tint = Good)
                        Text(humanSpeed(ul), style = MaterialTheme.typography.titleMedium, color = Good)
                    }
                    Text("Upload", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (s != null && s.freeSpace > 0) {
                    Spacer(Modifier.width(14.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(humanBytes(s.freeSpace), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                        Text("Free space", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** Swipe right to pause / resume, swipe left to delete. */
@Composable
private fun SwipeRow(paused: Boolean, modifier: Modifier, onSwipeRight: () -> Unit, onSwipeLeft: () -> Unit, content: @Composable () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { v ->
            when (v) {
                SwipeToDismissBoxValue.StartToEnd -> { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onSwipeRight() }
                SwipeToDismissBoxValue.EndToStart -> { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onSwipeLeft() }
                else -> {}
            }
            false
        },
    )
    if (!com.arrmada360.app.data.ConfigStore.flag(com.arrmada360.app.data.Pref.SWIPE_DOWNLOADS)) {
        Box(modifier) { content() }
        return
    }
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        backgroundContent = {
            val dir = state.dismissDirection
            val color = when (dir) { SwipeToDismissBoxValue.StartToEnd -> if (paused) Good else Warn; SwipeToDismissBoxValue.EndToStart -> Bad; else -> Color.Transparent }
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(Brush.horizontalGradient(
                    if (dir == SwipeToDismissBoxValue.EndToStart) listOf(Color.Transparent, color.copy(alpha = 0.6f)) else listOf(color.copy(alpha = 0.6f), Color.Transparent),
                )).padding(horizontal = 20.dp),
                contentAlignment = if (dir == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                when (dir) {
                    SwipeToDismissBoxValue.StartToEnd -> Icon(if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause, null, tint = Color.White)
                    SwipeToDismissBoxValue.EndToStart -> Icon(Icons.Filled.Delete, null, tint = Color.White)
                    else -> {}
                }
            }
        },
    ) { content() }
}

/** Pick, create or clear a category (qBittorrent) / label (ruTorrent) for one or many torrents. */
@Composable
private fun CategoryDialog(client: com.arrmada360.app.api.DownloadClient, ids: List<String>, items: List<DlItem>, onDismiss: () -> Unit, onDone: () -> Unit) {
    val run = rememberRunner()
    val cats = rememberLoadable(client.cfg, "cats") { client.categories() }
    var newName by remember { mutableStateOf("") }
    var newPath by remember { mutableStateOf("") }
    val manageOnly = ids.isEmpty()
    val current = items.filter { it.id in ids }.map { it.category }.distinct()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (manageOnly) "Categories" else "Category for ${ids.size} torrent${if (ids.size == 1) "" else "s"}") },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                val list = cats.data
                if (list == null) Text("Loading…") else {
                    if (list.isEmpty()) Text("No categories yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    list.forEachIndexed { i, c ->
                        val color = Rainbow[i % Rainbow.size]
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .let { m -> if (manageOnly) m else m.clickable { run("Category set to $c", onDone) { client.setCategory(ids, c) } } }
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            StatusDot(color, 10.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(c, fontWeight = if (c in current) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                            Text("${items.count { it.category == c }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (c in current && current.size == 1) { Spacer(Modifier.width(6.dp)); Icon(Icons.Filled.CheckCircle, null, tint = color) }
                        }
                    }
                    if (!manageOnly && current.any { it.isNotEmpty() }) TextButton(onClick = { run("Category removed", onDone) { client.setCategory(ids, "") } }) {
                        Text("Remove category", color = Bad)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(if (client is QbitClient) "New category" else "New label", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(newName, { newName = it }, singleLine = true, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                if (client is QbitClient) OutlinedTextField(newPath, { newPath = it }, singleLine = true, label = { Text("Save path (optional)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = newName.isNotBlank() && !(manageOnly && client !is QbitClient), onClick = {
                val n = newName.trim(); val pth = newPath.trim()
                if (manageOnly) run("Category $n created", onDone) { client.createCategory(n, pth) }
                else run("Category set to $n", onDone) { client.createCategory(n, pth); client.setCategory(ids, n) }
            }) { Text(if (manageOnly) "Create" else "Create & apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
