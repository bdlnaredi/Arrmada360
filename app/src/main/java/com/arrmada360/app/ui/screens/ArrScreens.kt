package com.arrmada360.app.ui.screens

import com.arrmada360.app.api.arrType
import com.arrmada360.app.ui.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.key

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.arrmada360.app.ui.StatTile
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ManageSearch
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.arrmada360.app.api.ArrApi
import com.arrmada360.app.api.CalEntry
import com.arrmada360.app.api.ChildItem
import com.arrmada360.app.api.MediaItem
import com.arrmada360.app.api.Profile
import com.arrmada360.app.api.QueueEntry
import com.arrmada360.app.api.ReleaseEntry
import com.arrmada360.app.api.RootFolder
import com.arrmada360.app.data.ConfigStore
import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.data.display
import com.arrmada360.app.data.ServiceType
import com.arrmada360.app.ui.AppScaffold
import com.arrmada360.app.ui.BottomTab
import com.arrmada360.app.ui.MenuRow
import com.arrmada360.app.ui.Rainbow
import com.arrmada360.app.ui.Bad
import com.arrmada360.app.ui.ChipRow
import com.arrmada360.app.ui.Dropdown
import com.arrmada360.app.ui.EmptyBox
import com.arrmada360.app.ui.ErrorBox
import com.arrmada360.app.ui.Good
import com.arrmada360.app.ui.LoadableContent
import com.arrmada360.app.ui.LoadingBox
import com.arrmada360.app.ui.Navigator
import com.arrmada360.app.ui.Panel
import com.arrmada360.app.ui.Pill
import com.arrmada360.app.ui.Poster
import com.arrmada360.app.ui.ProgressBar
import com.arrmada360.app.ui.Route
import com.arrmada360.app.ui.SectionTitle
import com.arrmada360.app.ui.StatusDot
import com.arrmada360.app.ui.Warn
import com.arrmada360.app.ui.fmtDate
import com.arrmada360.app.ui.humanBytes
import com.arrmada360.app.ui.parseDate
import com.arrmada360.app.ui.relativeDay
import com.arrmada360.app.ui.rememberLoadable
import com.arrmada360.app.ui.rememberRunner

private val SORTS = listOf("Title", "Recently added", "Year", "Size on disk")

@Composable
fun ArrScreen(cfg: ServiceConfig, nav: Navigator, isRoot: Boolean, back: () -> Unit) {
    val api = remember(cfg) { ArrApi(cfg) }
    var tab by rememberSaveable(cfg.id) { mutableIntStateOf(0) }
    var libView by rememberSaveable(cfg.id + "lib") { mutableIntStateOf(0) }
    val libWord = if (cfg.arrType == ServiceType.RADARR) "Movies" else api.itemWord.let { if (it == "Series") "Series" else it + "s" }
    val bottom = listOf(
        BottomTab("Library", Icons.Filled.VideoLibrary),
        BottomTab("Queue", Icons.Filled.Download),
        BottomTab("Add", Icons.Filled.AddCircle),
        BottomTab("History", Icons.Filled.History),
        BottomTab("Server", Icons.Filled.Dns),
    )
    var menu by remember { mutableStateOf(false) }
    var showDisk by remember { mutableStateOf(false) }
    val run = rememberRunner()

    AppScaffold(
        title = cfg.display(), subtitle = bottom[tab].label, isRoot = isRoot, onBack = back, badge = cfg.type, badgeName = cfg.name,
        tabs = if (tab == 0) listOf(libWord, "Calendar", "Missing") else emptyList(), selectedTab = libView, onTab = { libView = it },
        bottomTabs = bottom, selectedBottom = tab, onBottom = { tab = it },
        actions = {
            IconButton(onClick = { tab = 2 }) { Icon(Icons.Filled.Search, "Add new") }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "More") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text("RSS sync") }, onClick = { menu = false; run("RSS sync started") { api.runTask("RssSync") } })
                    DropdownMenuItem(text = { Text("Search all missing") }, onClick = {
                        menu = false
                        val n = when (cfg.arrType) { ServiceType.SONARR -> "MissingEpisodeSearch"; ServiceType.RADARR -> "MissingMoviesSearch"; ServiceType.LIDARR -> "MissingAlbumSearch"; else -> "MissingBookSearch" }
                        run("Searching for all missing") { api.runTask(n) }
                    })
                    DropdownMenuItem(text = { Text("Refresh & scan all") }, onClick = {
                        menu = false
                        val n = when (cfg.arrType) { ServiceType.SONARR -> "RefreshSeries"; ServiceType.RADARR -> "RefreshMovie"; ServiceType.LIDARR -> "RefreshArtist"; else -> "RefreshAuthor" }
                        run("Refresh started") { api.runTask(n) }
                    })
                    DropdownMenuItem(text = { Text("Refresh downloads") }, onClick = { menu = false; run("Checking downloads") { api.runTask("RefreshMonitoredDownloads") } })
                    DropdownMenuItem(text = { Text("Disk space") }, onClick = { menu = false; showDisk = true })
                }
            }
        },
        fab = {
            if (tab == 0 && libView == 0) FloatingActionButton(onClick = { tab = 2 }, containerColor = MaterialTheme.colorScheme.primary, shape = androidx.compose.foundation.shape.CircleShape) {
                Icon(Icons.Filled.Add, "Add")
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                0 -> when (libView) {
                    1 -> CalendarTab(api, nav, wanted = false)
                    2 -> CalendarTab(api, nav, wanted = true)
                    else -> {
                        val twoPane = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 1000
                        var picked by rememberSaveable(cfg.id + "pane") { mutableStateOf<Int?>(null) }
                        if (!twoPane) LibraryTab(api, nav)
                        else Row(Modifier.fillMaxSize()) {
                            Box(Modifier.weight(1f)) { LibraryTab(api, nav) { picked = it } }
                            Box(Modifier.weight(1f)) {
                                val id = picked
                                if (id == null) EmptyBox("Pick something from your library to see it here")
                                else key(id) { ArrDetailScreen(cfg, id, nav) { picked = null } }
                            }
                        }
                    }
                }
                1 -> QueueTab(api)
                2 -> ArrAddContent(cfg, "", nav) { id -> if (id > 0) nav.push(Route.ArrDetail(cfg.id, id)) }
                3 -> HistoryTab(api)
                else -> ArrServerTab(api, nav)
            }
        }
    }
    if (showDisk) DiskDialog(api) { showDisk = false }
}

/** System status, health warnings, disk space and scheduled tasks for any *arr app (incl. Prowlarr). */
private class ArrServerData(val status: org.json.JSONObject, val health: List<com.arrmada360.app.api.HealthIssue>, val disks: List<com.arrmada360.app.api.DiskSpace>, val tasks: List<com.arrmada360.app.api.TaskInfo>)

@Composable
fun ArrServerTab(api: ArrApi, nav: Navigator) {
    val run = rememberRunner()
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    val cfg = api.cfg
    val t = cfg.type
    val at = cfg.arrType
    val st = rememberLoadable(cfg, "srvstatus") { api.systemStatus() }
    val health = rememberLoadable(cfg, "srvhealth") { try { api.healthIssues() } catch (e: Exception) { emptyList() } }
    val (missingTask, refreshTask, libWord) = when (at) {
        ServiceType.SONARR -> Triple("MissingEpisodeSearch", "RefreshSeries", "series")
        ServiceType.RADARR -> Triple("MissingMoviesSearch", "RefreshMovie", "movies")
        ServiceType.LIDARR -> Triple("MissingAlbumSearch", "RefreshArtist", "artists")
        ServiceType.READARR -> Triple("MissingBookSearch", "RefreshAuthor", "authors")
        else -> Triple("", "", "")
    }
    LazyColumn(contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp)) {
        item {
            val v = st.data?.optString("version") ?: ""
            MenuRow(Icons.Filled.Settings, "${cfg.name} instance settings",
                listOf(cfg.baseUrl, if (v.isNotEmpty()) "v$v" else "").filter { it.isNotEmpty() }.joinToString(" · "), t.tint) {
                nav.push(Route.EditService(cfg.id, null))
            }
        }
        item { MenuRow(Icons.Filled.Public, "View on web", "Open ${t.label} in your browser", t.tint2, external = true) { uri.openUri(cfg.baseUrl) } }
        if (t.privateArt) item { Box(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) { com.arrmada360.app.ui.PrivateArtSwitch(cfg) } }
        if (at == ServiceType.RADARR && t != ServiceType.WHISPARR) item {
            MenuRow(Icons.Filled.CollectionsBookmark, "Collections", "Franchises you own – add the missing movies in one tap", Rainbow[3]) { nav.push(Route.ArrCollections(cfg.id, null)) }
        }
        if ((at == ServiceType.RADARR || at == ServiceType.SONARR) && t != ServiceType.WHISPARR) item {
            MenuRow(Icons.Filled.HighQuality, "Quality & duplicates", "Below cutoff, low quality, missing subtitles and duplicates", Rainbow[4]) { nav.push(Route.ArrQuality(cfg.id)) }
        }
        if (t.isArr) {
            item { MenuRow(Icons.Filled.DriveFileMove, "Manual import", "Import files from a folder or a stuck download", Rainbow[1]) { nav.push(Route.ManualImport(cfg.id)) } }
            item { MenuRow(Icons.Filled.RssFeed, "RSS sync", "Check your indexers for new releases now", Rainbow[3], arrow = false) { run("RSS sync started") { api.runTask("RssSync") } } }
            item { MenuRow(Icons.Filled.Sync, "Update library", "Refresh & rescan all $libWord", Rainbow[4], arrow = false) { run("Library update started") { api.runTask(refreshTask) } } }
            item { MenuRow(Icons.Filled.TravelExplore, "Search all missing", "Search every monitored missing item", Rainbow[2], arrow = false) { run("Searching for all missing") { api.runTask(missingTask) } } }
            item { MenuRow(Icons.Filled.CloudSync, "Refresh downloads", "Check download clients for finished items", Rainbow[0], arrow = false) { run("Checking downloads") { api.runTask("RefreshMonitoredDownloads") } } }
        }
        item {
            val h = health.data
            MenuRow(Icons.Filled.MonitorHeart, "System status",
                when { h == null -> "Health, disk space, scheduled tasks"; h.isEmpty() -> "All healthy · disk space · tasks"; else -> "${h.size} health warning${if (h.size == 1) "" else "s"} · disk space · tasks" },
                if (h != null && h.isNotEmpty()) Warn else Good) { nav.push(Route.ArrSystem(cfg.id)) }
        }
    }
}

@Composable
fun ArrSystemScreen(cfg: ServiceConfig, back: () -> Unit) {
    val api = remember(cfg) { ArrApi(cfg) }
    AppScaffold(title = "System status", subtitle = cfg.display(), isRoot = false, onBack = back, badge = cfg.type, badgeName = cfg.name) { pad ->
        Box(Modifier.padding(pad)) { ArrSystemContent(api) }
    }
}

@Composable
fun ArrSystemContent(api: ArrApi) {
    val run = rememberRunner()
    var confirmRestart by remember { mutableStateOf(false) }
    val l = rememberLoadable(api.cfg, "server") {
        ArrServerData(
            api.systemStatus(),
            try { api.healthIssues() } catch (e: Exception) { emptyList() },
            try { api.diskSpace() } catch (e: Exception) { emptyList() },
            try { api.tasks() } catch (e: Exception) { emptyList() },
        )
    }
    LoadableContent(l) { d ->
        LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Panel(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        com.arrmada360.app.ui.ServiceBadge(api.type, 56.dp, api.cfg.name)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${d.status.optString("appName", api.type.label)} ${d.status.optString("version")}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            val os = listOf(d.status.optString("osName"), d.status.optString("osVersion")).filter { it.isNotEmpty() && it != "null" }.joinToString(" ")
                            if (os.isNotEmpty()) Text(os, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val branch = d.status.optString("branch")
                            if (branch.isNotEmpty()) Text("Branch: $branch", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val started = d.status.optString("startTime")
                            if (started.isNotEmpty()) Text("Running since ${fmtDate(started, true)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Health", if (d.health.isEmpty()) "All good" else "${d.health.size} issue${if (d.health.size == 1) "" else "s"}", if (d.health.isEmpty()) Good else Warn, Modifier.weight(1f))
                    StatTile("Disks", d.disks.size.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    StatTile("Tasks", d.tasks.size.toString(), MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                }
            }
            if (d.health.isNotEmpty()) {
                item { SectionTitle("Health") }
                items(d.health) { h ->
                    Panel(Modifier.fillMaxWidth(), tint = if (h.type.equals("error", true)) Bad else Warn) {
                        Column(Modifier.padding(12.dp)) {
                            Pill(h.type.replaceFirstChar { it.uppercase() }, if (h.type.equals("error", true)) Bad else Warn)
                            Spacer(Modifier.height(4.dp))
                            Text(h.message, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (d.disks.isNotEmpty()) {
                item { SectionTitle("Disk space") }
                items(d.disks) { s ->
                    val used = if (s.total > 0) 1f - s.free.toFloat() / s.total else 0f
                    Panel(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row {
                                Text(s.label.ifEmpty { s.path }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${(used * 100).toInt()}%", style = MaterialTheme.typography.titleSmall, color = if (used > 0.9f) Bad else MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.height(6.dp))
                            ProgressBar(used, color = if (used > 0.9f) Bad else MaterialTheme.colorScheme.primary, height = 7.dp)
                            Spacer(Modifier.height(4.dp))
                            Text("${humanBytes(s.free)} free of ${humanBytes(s.total)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (d.tasks.isNotEmpty()) {
                item { SectionTitle("Scheduled tasks") }
                items(d.tasks) { t ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(t.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Every ${t.interval} min · last ${fmtDate(t.last, true)} · next ${fmtDate(t.next, true)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { run("${t.name} started") { api.runTask(t.taskName) } }) { Icon(Icons.Filled.PlayArrow, "Run now", tint = MaterialTheme.colorScheme.primary) }
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.OutlinedButton(onClick = { confirmRestart = true }, modifier = Modifier.fillMaxWidth()) { Text("Restart ${api.type.label}", color = Bad) }
            }
        }
    }
    if (confirmRestart) AlertDialog(
        onDismissRequest = { confirmRestart = false },
        title = { Text("Restart ${api.cfg.name}?") },
        text = { Text("It will be unavailable for a few seconds.") },
        confirmButton = { TextButton(onClick = { confirmRestart = false; run("Restarting…") { api.restart() } }) { Text("Restart", color = Bad) } },
        dismissButton = { TextButton(onClick = { confirmRestart = false }) { Text("Cancel") } },
    )
}

@Composable
private fun DiskDialog(api: ArrApi, onDismiss: () -> Unit) {
    val l = rememberLoadable(api) { api.diskSpace() }
    AlertDialog(
        onDismissRequest = onDismiss, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Disk space") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val d = l.data
                when {
                    d == null && l.error != null -> Text(l.error ?: "")
                    d == null -> Text("Loading…")
                    else -> d.forEach { s ->
                        val used = if (s.total > 0) 1f - s.free.toFloat() / s.total else 0f
                        Text(s.label.ifEmpty { s.path }, style = MaterialTheme.typography.titleSmall)
                        ProgressBar(used, color = if (used > 0.9f) Bad else MaterialTheme.colorScheme.primary)
                        Text("${humanBytes(s.free)} free of ${humanBytes(s.total)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
    )
}

// ------------------------------------------------------------ Library
@Composable
fun LibraryTab(api: ArrApi, nav: Navigator, onOpen: ((Int) -> Unit)? = null) {
    val l = rememberLoadable(api.cfg) { api.library() }
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableIntStateOf(0) }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var sortMenu by remember { mutableStateOf(false) }
    val filters = listOf("All", "Monitored", "Missing", "Complete", "Unmonitored")
    var selected by remember { mutableStateOf(setOf<Int>()) }
    var bulk by remember { mutableStateOf<String?>(null) }
    val run = rememberRunner()
    androidx.activity.compose.BackHandler(selected.isNotEmpty()) { selected = emptySet() }

    Column {
        ScreenTip("library", Icons.Filled.TouchApp, "Tap a poster to open it. Long-press posters to select several and monitor, search, tag or delete them together.")
        if (selected.isNotEmpty()) BulkBar(selected.size,
            onAll = { selected = (l.data ?: emptyList()).map { it.id }.toSet() },
            onClear = { selected = emptySet() },
            onAction = { bulk = it },
        ) else
        Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                query, { query = it }, placeholder = { Text("Filter library") }, singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null) }, modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            )
            Box {
                IconButton(onClick = { sortMenu = true }) { Icon(Icons.Filled.Sort, "Sort") }
                DropdownMenu(sortMenu, { sortMenu = false }) {
                    SORTS.forEachIndexed { i, s ->
                        DropdownMenuItem(text = { Text((if (i == sort) "✓ " else "    ") + s) }, onClick = { sort = i; sortMenu = false })
                    }
                }
            }
        }
        val all = l.data ?: emptyList()
        val counts = listOf(all.size, all.count { it.monitored }, all.count { it.monitored && !it.complete }, all.count { it.complete }, all.count { !it.monitored })
        if (all.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(api.itemWord.let { if (it == "Series") "Series" else it + "s" }, all.size.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            StatTile("Missing", counts[2].toString(), Bad, Modifier.weight(1f))
            StatTile("On disk", humanBytes(all.sumOf { it.sizeOnDisk }), MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
        }
        ChipRow(filters, filter, { filter = it }, Modifier.padding(vertical = 6.dp), counts = if (all.isEmpty()) null else counts)
        LoadableContent(l, empty = { it.isEmpty() }, emptyText = "Your library is empty.\nTap + to add something.") { items ->
            val shown = remember(items, query, sort, filter) {
                items.asSequence()
                    .filter { query.isBlank() || it.title.contains(query.trim(), true) }
                    .filter {
                        when (filter) {
                            1 -> it.monitored; 2 -> it.monitored && !it.complete; 3 -> it.complete; 4 -> !it.monitored; else -> true
                        }
                    }
                    .let { s ->
                        when (sort) {
                            1 -> s.sortedByDescending { it.added }
                            2 -> s.sortedByDescending { it.year }
                            3 -> s.sortedByDescending { it.sizeOnDisk }
                            else -> s.sortedBy { it.raw.optString("sortTitle", it.title).ifEmpty { it.title }.lowercase() }
                        }
                    }.toList()
            }
            LazyVerticalGrid(
                columns = if (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600) GridCells.Adaptive(118.dp) else GridCells.Fixed(ConfigStore.gridColumns),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(shown, key = { it.id }) { m ->
                    PosterTile(m, selected = m.id in selected, onLong = if (com.arrmada360.app.data.Family.active) null else ({ selected = if (m.id in selected) selected - m.id else selected + m.id })) {
                        if (selected.isNotEmpty()) selected = if (m.id in selected) selected - m.id else selected + m.id
                        else { if (ConfigStore.flag(com.arrmada360.app.data.Pref.AUTO_CLEAR_SEARCH)) query = ""; if (onOpen != null) onOpen(m.id) else nav.push(Route.ArrDetail(api.cfg.id, m.id)) }
                    }
                }
            }
        }
    }
    BulkDialogs(api, selected, bulk) { reload -> bulk = null; if (reload) { selected = emptySet(); l.load(true) } }
}

@Composable
private fun BulkBar(count: Int, onAll: () -> Unit, onClear: () -> Unit, onAction: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)).padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClear) { Icon(Icons.Filled.Close, "Clear selection") }
            Text("$count selected", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = onAll) { Text("Select all") }
        }
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val acts = listOf(
                Triple("monitor", "Monitor", Icons.Filled.Bookmark), Triple("unmonitor", "Unmonitor", Icons.Filled.BookmarkBorder),
                Triple("search", "Search", Icons.Filled.Search), Triple("refresh", "Refresh", Icons.Filled.Refresh),
                Triple("quality", "Quality", Icons.Filled.HighQuality), Triple("tag", "Tags", Icons.Filled.LocalOffer),
                Triple("delete", "Delete", Icons.Filled.Delete),
            )
            items(acts) { (k, label, icon) ->
                androidx.compose.material3.AssistChip(onClick = { onAction(k) }, label = { Text(label) },
                    leadingIcon = { Icon(icon, null, tint = if (k == "delete") Bad else MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) })
            }
        }
    }
}

@Composable
private fun BulkDialogs(api: ArrApi, ids: Set<Int>, action: String?, done: (Boolean) -> Unit) {
    val run = rememberRunner()
    val n = ids.size
    val word = api.itemWord.lowercase().let { if (n == 1) it else if (it == "series") it else it + "s" }
    when (action) {
        "monitor", "unmonitor", "search", "refresh" -> LaunchedEffect(action) {
            val list = ids.toList()
            done(false)
            run(when (action) { "monitor" -> "$n $word monitored"; "unmonitor" -> "$n $word unmonitored"; "search" -> "Searching $n $word"; else -> "Refreshing $n $word" }, { done(true) }) {
                when (action) {
                    "monitor" -> api.bulkEdit(list, monitored = true)
                    "unmonitor" -> api.bulkEdit(list, monitored = false)
                    "search" -> api.bulkSearch(list)
                    else -> api.bulkRefresh(list)
                }
            }
        }
        "quality" -> {
            val profiles = rememberLoadable(api.cfg, "bulkqp") { api.qualityProfiles() }
            AlertDialog(
                onDismissRequest = { done(false) }, title = { Text("Quality profile for $n $word") },
                text = {
                    Column {
                        val p = profiles.data
                        if (p == null) Text(profiles.error ?: "Loading…")
                        else p.forEach { pr -> TextButton(onClick = { done(false); run("Quality profile set to ${pr.name}", { done(true) }) { api.bulkEdit(ids.toList(), qualityProfileId = pr.id) } }) { Text(pr.name) } }
                    }
                },
                confirmButton = { TextButton(onClick = { done(false) }) { Text("Cancel") } },
            )
        }
        "tag" -> {
            val tags = rememberLoadable(api.cfg, "bulktags") { api.tags() }
            var newTag by remember { mutableStateOf("") }
            var remove by remember { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = { done(false) }, title = { Text("Tags for $n $word") },
                text = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.FilterChip(!remove, { remove = false }, label = { Text("Add tag") })
                            Spacer(Modifier.width(8.dp))
                            androidx.compose.material3.FilterChip(remove, { remove = true }, label = { Text("Remove tag") })
                        }
                        val t = tags.data
                        if (t == null) Text(tags.error ?: "Loading…")
                        else androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            t.forEach { tag ->
                                androidx.compose.material3.AssistChip(onClick = {
                                    done(false)
                                    run(if (remove) "Removed tag ${tag.name}" else "Added tag ${tag.name}", { done(true) }) {
                                        if (remove) api.bulkEdit(ids.toList(), removeTags = listOf(tag.id)) else api.bulkEdit(ids.toList(), addTags = listOf(tag.id))
                                    }
                                }, label = { Text(tag.name) })
                            }
                        }
                        if (!remove) {
                            OutlinedTextField(newTag, { newTag = it }, singleLine = true, label = { Text("New tag") }, modifier = Modifier.fillMaxWidth())
                            TextButton(enabled = newTag.isNotBlank(), onClick = {
                                val label = newTag; done(false)
                                run("Added tag $label", { done(true) }) { val tg = api.createTag(label); api.bulkEdit(ids.toList(), addTags = listOf(tg.id)) }
                            }) { Text("Create & add") }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { done(false) }) { Text("Cancel") } },
            )
        }
        "delete" -> {
            var files by remember { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = { done(false) }, title = { Text("Delete $n $word?") },
                text = { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(files, { files = it }); Text("Also delete files from disk") } },
                confirmButton = { TextButton(onClick = { done(false); run("Deleted $n $word", { done(true) }) { api.bulkDelete(ids.toList(), files) } }) { Text("Delete", color = Bad) } },
                dismissButton = { TextButton(onClick = { done(false) }) { Text("Cancel") } },
            )
        }
    }
}

@Composable
private fun PosterTile(m: MediaItem, selected: Boolean = false, onLong: (() -> Unit)? = null, onClick: () -> Unit) {
    val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val focused by src.collectIsFocusedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(if (pressed) 0.94f else if (focused) 1.06f else 1f, label = "tile")
    val color = when { m.complete -> Good; !m.monitored -> Color.Gray; m.raw.optString("status") == "continuing" -> MaterialTheme.colorScheme.primary; else -> Bad }
    Column(
        Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
            .combinedClickable(interactionSource = src, indication = androidx.compose.material3.ripple(), onLongClick = onLong, onClick = onClick),
    ) {
        Box(Modifier.clip(RoundedCornerShape(12.dp)).border(if (selected || focused) 3.dp else 1.dp, if (selected || focused) MaterialTheme.colorScheme.primary else color.copy(alpha = 0.45f), RoundedCornerShape(12.dp))) {
            Poster(m.poster, Modifier.fillMaxWidth(), ratio = if (m.raw.has("artistName")) 1f else 2f / 3f, corner = 12.dp, fallback = m.title)
            if (!m.monitored) Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.5f)))
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.7f)))))
            if (selected) Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.CheckCircle, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
            Box(Modifier.align(Alignment.TopEnd).padding(5.dp)) {
                Icon(if (m.monitored) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, null, tint = if (m.monitored) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            }
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(6.dp)) {
                Text(m.progressText, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1)
                Spacer(Modifier.height(3.dp))
                val prog = when {
                    m.complete -> 1f
                    m.progressText.contains("/") -> m.progressText.substringBefore(" ").split("/").let { p -> val h = p[0].toFloatOrNull() ?: 0f; val t = p.getOrNull(1)?.toFloatOrNull() ?: 0f; if (t > 0) h / t else 0f }
                    else -> 0f
                }
                ProgressBar(prog, color = color, height = 3.dp)
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(m.title, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        if (m.year > 0) Text(m.year.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

// ------------------------------------------------------------ Calendar & Wanted
@Composable
fun CalendarTab(api: ArrApi, nav: Navigator, wanted: Boolean) {
    val l = rememberLoadable(api.cfg, wanted) { if (wanted) api.wanted() else api.calendar() }
    val run = rememberRunner()
    var interactive by remember { mutableStateOf<Pair<String, Map<String, Any?>>?>(null) }
    interactive?.let { (t, q) -> ReleasesSheet(api, t, q) { interactive = null } }
    LoadableContent(l, empty = { it.isEmpty() }, emptyText = if (wanted) "Nothing missing 🎉" else "Nothing coming up in the next 30 days") { list ->
        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
            if (wanted) {
                items(list, key = { "w" + it.id }) { e ->
                    CalRow(e, onClick = { nav.push(Route.ArrDetail(api.cfg.id, e.parentId)) }) {
                        Row {
                            IconButton(onClick = { interactive = e.title + " " + e.subtitle to (if (api.type == ServiceType.RADARR) api.releaseQueryForItem(e.id) else api.releaseQueryForChild(e.id)) }) {
                                Icon(Icons.Filled.ManageSearch, "Interactive search")
                            }
                            IconButton(onClick = {
                                run("Search started") { if (api.type == ServiceType.RADARR) api.searchItem(e.id) else api.searchChildren(listOf(e.id)) }
                            }) { Icon(Icons.Filled.Search, "Search") }
                        }
                    }
                }
            } else {
                val groups = list.groupBy { parseDate(it.date) }
                groups.forEach { (day, entries) ->
                    item(key = "h$day") { SectionTitle(relativeDay(day)) }
                    items(entries, key = { "c" + it.id + it.date }) { e ->
                        CalRow(e, onClick = { nav.push(Route.ArrDetail(api.cfg.id, e.parentId)) }) {
                            if (e.hasFile) Icon(Icons.Filled.CheckCircle, null, tint = Good, modifier = Modifier.padding(12.dp))
                            else IconButton(onClick = { interactive = e.title + " " + e.subtitle to (if (api.type == ServiceType.RADARR) api.releaseQueryForItem(e.id) else api.releaseQueryForChild(e.id)) }) {
                                Icon(Icons.Filled.ManageSearch, "Interactive search")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalRow(e: CalEntry, onClick: () -> Unit, trailing: @Composable () -> Unit) {
    Panel(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), tint = if (e.hasFile) Good else MaterialTheme.colorScheme.primary, onClick = onClick) {
    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Poster(e.poster, Modifier.width(48.dp), corner = 8.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(e.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(e.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Pill(fmtDate(e.date, withTime = e.date.length > 10).ifEmpty { "TBA" }, if (e.hasFile) Good else MaterialTheme.colorScheme.primary)
        }
        trailing()
    }
    }
}

// ------------------------------------------------------------ Queue
@Composable
private fun QueueTab(api: ArrApi) {
    val l = rememberLoadable(api.cfg, autoRefreshMs = 5000) { api.queue() }
    var selected by remember { mutableStateOf<QueueEntry?>(null) }
    LoadableContent(l, empty = { it.isEmpty() }, emptyText = "Queue is empty") { list ->
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list, key = { it.id }) { q ->
                Panel(Modifier.fillMaxWidth(), onClick = { selected = q }) {
                    Column(Modifier.padding(12.dp)) {
                        Text(q.subtitle.ifEmpty { q.title }, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        if (q.subtitle.isNotEmpty()) Text(q.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(8.dp))
                        val p = if (q.size > 0) 1f - q.sizeLeft.toFloat() / q.size else 0f
                        ProgressBar(p, color = if (q.warning.isNotEmpty()) Warn else MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(6.dp))
                        Row {
                            Text("${q.status} · ${(p * 100).toInt()}% of ${humanBytes(q.size)}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                            Text(listOf(q.timeLeft, q.client).filter { it.isNotEmpty() }.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (q.warning.isNotEmpty()) Text(q.warning, style = MaterialTheme.typography.labelSmall, color = Warn, maxLines = 3)
                    }
                }
            }
        }
    }
    selected?.let { q ->
        val run = rememberRunner()
        var block by remember { mutableStateOf(false) }
        var fromClient by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("Remove from queue?") },
            text = {
                Column {
                    Text(q.title, style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(fromClient, { fromClient = it }); Text("Remove from download client") }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(block, { block = it }); Text("Blocklist release (search again)") }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = q.id; selected = null
                    run("Removed", { l.load(true) }) { api.removeQueue(id, fromClient, block) }
                }) { Text("Remove", color = Bad) }
            },
            dismissButton = { TextButton(onClick = { selected = null }) { Text("Cancel") } },
        )
    }
}

// ------------------------------------------------------------ History
@Composable
private fun HistoryTab(api: ArrApi) {
    val l = rememberLoadable(api.cfg) { api.history() }
    LoadableContent(l, empty = { it.isEmpty() }, emptyText = "No history yet") { list ->
        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
            items(list) { h ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val c = when {
                            h.event.contains("Import", true) || h.event.contains("Downloaded", true) -> Good
                            h.event.contains("Fail", true) || h.event.contains("Delet", true) -> Bad
                            h.event.contains("Grab", true) -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Pill(h.event, c)
                        if (h.quality.isNotEmpty()) { Spacer(Modifier.width(6.dp)); Pill(h.quality, MaterialTheme.colorScheme.onSurfaceVariant) }
                        Spacer(Modifier.weight(1f))
                        Text(fmtDate(h.date, true), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(h.title, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

// ============================================================ Detail
@Composable
fun ArrDetailScreen(cfg: ServiceConfig, itemId: Int, nav: Navigator, back: () -> Unit) {
    val api = remember(cfg) { ArrApi(cfg) }
    val item = rememberLoadable(cfg, itemId) { api.item(itemId) }
    val kids = rememberLoadable(cfg, itemId, "kids") { if (api.childWord != null) api.children(itemId) else emptyList() }
    val run = rememberRunner()
    var releases by remember { mutableStateOf<Pair<String, Map<String, Any?>>?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(setOf<Int>()) }
    var overviewOpen by remember { mutableStateOf(false) }
    var seasonPick by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    AppScaffold(
        title = item.data?.title ?: "Loading…", isRoot = false, onBack = back,
        actions = {
            IconButton(onClick = { run("Refreshing") { api.refreshItem(itemId) } }) { Icon(Icons.Filled.Refresh, "Refresh") }
            if (!com.arrmada360.app.data.Family.active) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Delete") }
        },
    ) { pad ->
        val m = item.data
        when {
            m == null && item.error != null -> Box(Modifier.padding(pad)) { ErrorBox(item.error ?: "") { item.load() } }
            m == null -> Box(Modifier.padding(pad)) { LoadingBox() }
            else -> LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
                item {
                    Box(Modifier.fillMaxWidth().height(220.dp)) {
                        AsyncImage(model = m.fanart ?: m.poster, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background))))
                        Row(Modifier.align(Alignment.BottomStart).padding(16.dp), verticalAlignment = Alignment.Bottom) {
                            Poster(m.poster, Modifier.width(100.dp), ratio = if (cfg.arrType == ServiceType.LIDARR) 1f else 2f / 3f, fallback = m.title)
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(m.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                                Text(listOf(if (m.year > 0) m.year.toString() else "", m.network, m.status.replaceFirstChar { it.uppercase() })
                                    .filter { it.isNotEmpty() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Pill(m.progressText, if (m.complete) Good else MaterialTheme.colorScheme.primary)
                                    if (m.sizeOnDisk > 0) Pill(humanBytes(m.sizeOnDisk), MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                if (!com.arrmada360.app.data.Family.active) item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ActionButton(if (m.monitored) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, if (m.monitored) "Monitored" else "Unmonitored") {
                            run(if (m.monitored) "Unmonitored" else "Monitored", { item.load(true) }) { api.setMonitored(itemId, !m.monitored) }
                        }
                        ActionButton(Icons.Filled.Search, "Auto search") { run("Search started") { api.searchItem(itemId) } }
                        ActionButton(Icons.Filled.ManageSearch, "Interactive") {
                            if (cfg.arrType == ServiceType.SONARR) seasonPick = true else releases = m.title to api.releaseQueryForItem(itemId)
                        }
                        ActionButton(Icons.Filled.Edit, "Edit") { editing = true }
                        ActionButton(Icons.Filled.Refresh, "Refresh") { run("Refresh & scan started") { api.refreshItem(itemId) } }
                    }
                }
                item { FactsRow(cfg, m) }
                item { ScreenTip("detail", Icons.Filled.Lightbulb, "Search finds the best release automatically, Interactive lets you pick one yourself, and Edit changes quality or tags. Tap anyone in the cast for their filmography.") }
                if (cfg.type != ServiceType.WHISPARR) item { com.arrmada360.app.ui.screens.TrailerAndStreaming(api, m, nav) }
                if (m.overview.isNotEmpty()) item {
                    Text(
                        m.overview, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (overviewOpen) 100 else 4, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 16.dp).clickable { overviewOpen = !overviewOpen },
                    )
                }
                item {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        InfoLine("Path", m.raw.optString("path"))
                        if (cfg.arrType == ServiceType.RADARR) {
                            val f = m.raw.optJSONObject("movieFile")
                            if (f != null) {
                                InfoLine("File", f.optString("relativePath"))
                                InfoLine("Quality", f.optJSONObject("quality")?.optJSONObject("quality")?.optString("name") ?: "")
                            }
                            InfoLine("Availability", m.raw.optString("minimumAvailability"))
                        }
                        if (cfg.arrType == ServiceType.SONARR) {
                            InfoLine("Type", m.raw.optString("seriesType"))
                            InfoLine("Next airing", fmtDate(m.raw.optString("nextAiring"), true))
                        }
                        val imdb = m.raw.optString("imdbId")
                        if (imdb.startsWith("tt")) {
                            val ctx = androidx.compose.ui.platform.LocalContext.current
                            androidx.compose.material3.OutlinedButton(onClick = { openImdb(ctx, imdb) }, modifier = Modifier.padding(top = 8.dp)) {
                                Text("Open on IMDb", color = Warn, fontWeight = FontWeight.Bold)
                            }
                        }
                        val genres = m.raw.optJSONArray("genres")
                        if (genres != null && genres.length() > 0) InfoLine("Genres", (0 until genres.length()).joinToString { genres.optString(it) })
                    }
                }
                if (cfg.arrType == ServiceType.RADARR) item { MovieFileCard(m) }
                if ((cfg.arrType == ServiceType.RADARR || cfg.arrType == ServiceType.SONARR) && cfg.type != ServiceType.WHISPARR) item { CastRow(api, m, nav) }
                item {
                    Row(Modifier.fillMaxWidth().clickable { showHistory = !showHistory }.padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle("History", Modifier.weight(1f))
                        Icon(if (showHistory) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
                    }
                }
                if (showHistory) item { ItemHistory(api, itemId) }
                if (api.childWord != null) {
                    val list = kids.data
                    item { SectionTitle(api.childWord ?: "") }
                    when {
                        list == null && kids.error != null -> item { Text(kids.error ?: "", color = Bad, modifier = Modifier.padding(16.dp)) }
                        list == null -> item { Box(Modifier.fillMaxWidth().height(80.dp)) { LoadingBox() } }
                        else -> {
                            val groups = list.groupBy { it.groupKey to it.group }
                            val singleGroup = groups.size == 1 && cfg.arrType != ServiceType.SONARR
                            groups.forEach { (g, children) ->
                                val (key, label) = g
                                val open = singleGroup || key in expanded
                                if (!singleGroup) item(key = "g$key$label") {
                                    GroupHeader(
                                        label = label, children = children, open = open,
                                        onToggle = { expanded = if (open) expanded - key else expanded + key },
                                        seasonMonitored = if (cfg.arrType == ServiceType.SONARR) seasonMonitored(m, key) else null,
                                        onMonitor = { mon ->
                                            if (cfg.arrType == ServiceType.SONARR) run(if (mon) "Season monitored" else "Season unmonitored", { item.load(true); kids.load(true) }) { api.setSeasonMonitored(itemId, key, mon) }
                                            else run("Updated", { kids.load(true) }) { api.setChildrenMonitored(children.map { it.id }, mon) }
                                        },
                                        onSearch = {
                                            if (cfg.arrType == ServiceType.SONARR) run("Season search started") { api.searchSeason(itemId, key) }
                                            else run("Search started") { api.searchChildren(children.map { it.id }) }
                                        },
                                        onInteractive = if (cfg.arrType == ServiceType.SONARR) ({ releases = "${m.title} – $label" to mapOf("seriesId" to itemId, "seasonNumber" to key) }) else null,
                                    )
                                }
                                if (open) items(children, key = { "k" + it.id }) { c ->
                                    ChildRow(c,
                                        onMonitor = { run(null, { kids.load(true) }) { api.setChildrenMonitored(listOf(c.id), !c.monitored) } },
                                        onSearch = { run("Search started") { api.searchChildren(listOf(c.id)) } },
                                        onInteractive = { releases = c.title to api.releaseQueryForChild(c.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    releases?.let { (title, q) ->
        ReleasesSheet(api, title, q) { releases = null }
    }

    if (seasonPick) item.data?.let { m ->
        val seasons = (m.raw.optJSONArray("seasons")?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it)?.optInt("seasonNumber") } } ?: emptyList()).sortedDescending()
        AlertDialog(
            onDismissRequest = { seasonPick = false }, title = { Text("Interactive search") },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                    Text("Pick a season – or open a season below to search single episodes.", style = MaterialTheme.typography.bodySmall)
                    seasons.forEach { sn ->
                        TextButton(onClick = { seasonPick = false; releases = "${m.title} – ${if (sn == 0) "Specials" else "Season $sn"}" to mapOf("seriesId" to itemId, "seasonNumber" to sn) }) {
                            Text(if (sn == 0) "Specials" else "Season $sn")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { seasonPick = false }) { Text("Cancel") } },
        )
    }

    if (editing) item.data?.let { m -> EditItemDialog(api, m) { changed -> editing = false; if (changed) item.load(true) } }

    if (confirmDelete) {
        var files by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${item.data?.title ?: ""}?") },
            text = { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(files, { files = it }); Text("Also delete files from disk") } },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    run("Deleted", { back() }) { api.deleteItem(itemId, files) }
                }) { Text("Delete", color = Bad) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

private fun seasonMonitored(m: MediaItem, season: Int): Boolean? {
    val arr = m.raw.optJSONArray("seasons") ?: return null
    for (i in 0 until arr.length()) {
        val s = arr.optJSONObject(i) ?: continue
        if (s.optInt("seasonNumber") == season) return s.optBoolean("monitored")
    }
    return null
}

@Composable
private fun InfoLine(label: String, value: String) {
    if (value.isEmpty()) return
    Row {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(96.dp))
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(48.dp).clip(androidx.compose.foundation.shape.CircleShape)
                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.20f))))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun GroupHeader(
    label: String,
    children: List<ChildItem>,
    open: Boolean,
    onToggle: () -> Unit,
    seasonMonitored: Boolean?,
    onMonitor: (Boolean) -> Unit,
    onSearch: () -> Unit,
    onInteractive: (() -> Unit)?,
) {
    val have = children.count { it.hasFile }
    val mon = seasonMonitored ?: children.any { it.monitored }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface).clickable(onClick = onToggle).padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onMonitor(!mon) }) {
            Icon(if (mon) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, "Monitor", tint = if (mon) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("$have / ${children.size}", style = MaterialTheme.typography.labelSmall, color = if (have == children.size && have > 0) Good else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, "Search") }
        if (onInteractive != null) IconButton(onClick = onInteractive) { Icon(Icons.Filled.ManageSearch, "Interactive") }
        Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, modifier = Modifier.padding(end = 12.dp))
    }
}

@Composable
private fun ChildRow(c: ChildItem, onMonitor: () -> Unit, onSearch: () -> Unit, onInteractive: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onMonitor) {
            Icon(if (c.monitored) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, "Monitor",
                tint = if (c.monitored) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(c.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(fmtDate(c.subtitle.take(10)).ifEmpty { c.subtitle }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val aired = parseDate(c.date)?.let { !it.isAfter(java.time.LocalDate.now()) } ?: false
        Icon(
            if (c.hasFile) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null,
            tint = when { c.hasFile -> Good; aired && c.monitored -> Bad; else -> MaterialTheme.colorScheme.onSurfaceVariant },
            modifier = Modifier.size(18.dp),
        )
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, null) }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem(text = { Text("Automatic search") }, onClick = { menu = false; onSearch() })
                DropdownMenuItem(text = { Text("Interactive search") }, onClick = { menu = false; onInteractive() })
                DropdownMenuItem(text = { Text(if (c.monitored) "Unmonitor" else "Monitor") }, onClick = { menu = false; onMonitor() })
            }
        }
    }
}

// ============================================================ Interactive search sheet
@Composable
fun ReleasesSheet(api: ArrApi, title: String, query: Map<String, Any?>, onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val l = rememberLoadable(api.cfg, query) { api.releases(query) }
    val run = rememberRunner()
    var hideRejected by rememberSaveable { mutableStateOf(false) }
    var sort by rememberSaveable { mutableIntStateOf(0) }
    var proto by rememberSaveable { mutableIntStateOf(0) }
    var quality by rememberSaveable { mutableStateOf("All") }
    var text by rememberSaveable { mutableStateOf("") }
    var confirm by remember { mutableStateOf<ReleaseEntry?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().height(680.dp)) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Interactive search", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                IconButton(onClick = { l.load() }) { Icon(Icons.Filled.Refresh, "Search again") }
            }
            val d = l.data
            when {
                d == null && l.error != null -> ErrorBox(l.error ?: "") { l.load() }
                d == null -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(40.dp)); LoadingBox()
                    Text("Searching your indexers… this can take a minute", style = MaterialTheme.typography.bodySmall)
                }
                d.isEmpty() -> EmptyBox("No releases found")
                else -> {
                    val qualities = listOf("All") + d.map { it.quality }.filter { it.isNotEmpty() }.distinct()
                    OutlinedTextField(text, { text = it }, singleLine = true, placeholder = { Text("Filter by name, group, indexer") },
                        leadingIcon = { Icon(Icons.Filled.FilterList, null) }, shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp))
                    ChipRow(listOf("Best", "Seeders", "Size ↓", "Size ↑", "Newest"), sort, { sort = it })
                    Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ChipRow(listOf("All", "Torrent", "Usenet"), proto, { proto = it }, Modifier.weight(1f))
                    }
                    Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Dropdown("Quality", qualities, quality, { it }, { quality = it }, Modifier.weight(1f))
                        Spacer(Modifier.width(10.dp))
                        Text("Hide rejected", style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.width(4.dp))
                        Switch(hideRejected, { hideRejected = it })
                    }
                    val shown = d.asSequence()
                        .filter { !hideRejected || !it.rejected }
                        .filter { proto == 0 || it.protocol.equals(if (proto == 1) "torrent" else "usenet", true) }
                        .filter { quality == "All" || it.quality == quality }
                        .filter { text.isBlank() || it.title.contains(text.trim(), true) || it.indexer.contains(text.trim(), true) }
                        .let { sq ->
                            when (sort) {
                                1 -> sq.sortedByDescending { it.seeders ?: -1 }
                                2 -> sq.sortedByDescending { it.size }
                                3 -> sq.sortedBy { it.size }
                                4 -> sq.sortedBy { it.ageHours }
                                else -> sq
                            }
                        }.toList()
                    Text("${shown.size} of ${d.size} releases · tap to grab", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(shown, key = { it.guid + it.indexerId }) { r ->
                            ReleaseRow(r) { if (r.rejected) confirm = r else run("Grabbed – sent to download client") { api.grab(r) } }
                        }
                    }
                }
            }
        }
    }
    confirm?.let { r ->
        AlertDialog(
            onDismissRequest = { confirm = null }, title = { Text("Grab a rejected release?") },
            text = { Column { Text(r.title, style = MaterialTheme.typography.bodySmall); Spacer(Modifier.height(6.dp)); r.rejections.forEach { Text("• $it", color = Bad, style = MaterialTheme.typography.labelSmall) } } },
            confirmButton = { TextButton(onClick = { confirm = null; run("Grabbed – sent to download client") { api.grab(r) } }) { Text("Grab anyway") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ReleaseRow(r: ReleaseEntry, onGrab: () -> Unit) {
    var showWhy by remember { mutableStateOf(false) }
    Panel(Modifier.fillMaxWidth(), onClick = onGrab) {
        Column(Modifier.padding(12.dp)) {
            Text(r.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 3, overflow = TextOverflow.Ellipsis,
                color = if (r.rejected) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Pill(r.quality.ifEmpty { "?" })
                Pill(humanBytes(r.size), MaterialTheme.colorScheme.onSurfaceVariant)
                if (r.seeders != null) Pill("▲${r.seeders} ▼${r.leechers ?: 0}", if (r.seeders > 5) Good else Warn)
                else Pill(r.protocol, MaterialTheme.colorScheme.onSurfaceVariant)
                Pill(r.ageText, MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(r.indexer, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                if (r.customScore != 0) Text("Score ${r.customScore}", style = MaterialTheme.typography.labelSmall)
                if (r.rejected) TextButton(onClick = { showWhy = !showWhy }) { Text("Rejected", color = Bad, style = MaterialTheme.typography.labelSmall) }
            }
            if (showWhy) r.rejections.forEach { Text("• $it", style = MaterialTheme.typography.labelSmall, color = Bad) }
        }
    }
}

// ============================================================ Add new
@Composable
fun ArrAddScreen(cfg: ServiceConfig, initialTerm: String, nav: Navigator, back: () -> Unit) {
    val api = remember(cfg) { ArrApi(cfg) }
    AppScaffold(title = "Add ${api.itemWord.lowercase()}", subtitle = cfg.display(), isRoot = false, onBack = back) { pad ->
        Box(Modifier.padding(pad)) {
            ArrAddContent(cfg, initialTerm, nav) { newId ->
                nav.pop()
                if (newId > 0) nav.push(Route.ArrDetail(cfg.id, newId))
            }
        }
    }
}

@Composable
fun ArrAddContent(cfg: ServiceConfig, initialTerm: String, nav: Navigator, onAdded: (Int) -> Unit) {
    val api = remember(cfg) { ArrApi(cfg) }
    var term by rememberSaveable(cfg.id + "addterm") { mutableStateOf(initialTerm) }
    var submitted by rememberSaveable(cfg.id + "addsub") { mutableStateOf(initialTerm) }
    val results = rememberLoadable(cfg, submitted) { if (submitted.isBlank()) emptyList() else api.lookup(submitted) }
    var adding by remember { mutableStateOf<MediaItem?>(null) }

    Column(Modifier.fillMaxSize().imePadding()) {
        OutlinedTextField(
            term, { term = it }, singleLine = true,
            placeholder = { Text(when (cfg.arrType) { ServiceType.SONARR -> "Show name or tvdb:12345"; ServiceType.RADARR -> "Movie name, tmdb:123 or imdb:tt…"; ServiceType.LIDARR -> "Artist name"; else -> "Author name" }) },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submitted = term.trim() }),
            modifier = Modifier.fillMaxWidth().padding(12.dp), shape = RoundedCornerShape(12.dp),
        )
        if (submitted.isBlank()) EmptyBox("Search for something new to add to ${cfg.name}")
        else LoadableContent(results, empty = { it.isEmpty() }, emptyText = "No results") { list ->
            LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list) { m ->
                    val inLib = m.id > 0
                    Panel(Modifier.fillMaxWidth(), tint = if (inLib) Good else null, onClick = { if (inLib) nav.push(Route.ArrDetail(cfg.id, m.id)) else adding = m }) {
                        Row(Modifier.padding(10.dp)) {
                            Poster(m.poster, Modifier.width(70.dp), ratio = if (cfg.arrType == ServiceType.LIDARR) 1f else 2f / 3f, fallback = m.title)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(m.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(listOf(if (m.year > 0) m.year.toString() else "", m.network).filter { it.isNotEmpty() }.joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                if (inLib) Pill("✓ In library", Good) else Pill("+ Tap to add", MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(4.dp))
                                Text(m.overview, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    adding?.let { m ->
        AddSheet(api, m, onDismiss = { adding = null }) { newId ->
            adding = null
            onAdded(newId)
        }
    }
}

private class AddOptions(val quality: List<Profile>, val roots: List<RootFolder>, val metadata: List<Profile>, val language: List<Profile>)

@Composable
private fun AddSheet(api: ArrApi, m: MediaItem, onDismiss: () -> Unit, onAdded: (Int) -> Unit) {
    val opts = rememberLoadable(api.cfg, "addopts") { AddOptions(api.qualityProfiles(), api.rootFolders(), api.metadataProfiles(), api.languageProfiles()) }
    val run = rememberRunner()
    var quality by remember { mutableStateOf<Profile?>(null) }
    var root by remember { mutableStateOf<RootFolder?>(null) }
    var meta by remember { mutableStateOf<Profile?>(null) }
    var monitor by remember { mutableStateOf(api.monitorOptions.first()) }
    var avail by remember { mutableStateOf("released") }
    var seriesType by remember { mutableStateOf("standard") }
    var search by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }

    val o = opts.data
    LaunchedEffect(o) {
        if (o != null) {
            if (quality == null) quality = o.quality.firstOrNull()
            if (root == null) root = o.roots.firstOrNull()
            if (meta == null) meta = o.metadata.firstOrNull()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Poster(m.poster, Modifier.width(56.dp), ratio = if (api.type == ServiceType.LIDARR) 1f else 2f / 3f)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(m.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    if (m.year > 0) Text(m.year.toString(), style = MaterialTheme.typography.bodySmall)
                }
            }
            when {
                o == null && opts.error != null -> Text(opts.error ?: "", color = Bad)
                o == null -> Text("Loading profiles…")
                else -> {
                    Dropdown("Root folder", o.roots, root, { "${it.path}  (${humanBytes(it.free)} free)" }, { root = it })
                    Dropdown("Quality profile", o.quality, quality, { it.name }, { quality = it })
                    if (o.metadata.isNotEmpty()) Dropdown("Metadata profile", o.metadata, meta, { it.name }, { meta = it })
                    Dropdown("Monitor", api.monitorOptions, monitor, { it.second }, { monitor = it })
                    if (api.type == ServiceType.RADARR) Dropdown(
                        "Minimum availability", listOf("announced", "inCinemas", "released"), avail,
                        { when (it) { "announced" -> "Announced"; "inCinemas" -> "In cinemas"; else -> "Released" } }, { avail = it },
                    )
                    if (api.type == ServiceType.SONARR) Dropdown("Series type", listOf("standard", "daily", "anime"), seriesType, { it.replaceFirstChar { c -> c.uppercase() } }, { seriesType = it })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Start search for missing", modifier = Modifier.weight(1f))
                        Switch(search, { search = it })
                    }
                    Button(
                        enabled = !busy && quality != null && root != null,
                        onClick = {
                            busy = true
                            run("Added ${m.title}") {
                                try {
                                    val id = api.add(m.raw, quality!!.id, root!!.path, meta?.id, o.language.firstOrNull()?.id,
                                        if (api.type == ServiceType.RADARR && monitor.first == "movieOnly") "movieOnly" else monitor.first,
                                        search, avail, seriesType)
                                    onAdded(id)
                                } finally { busy = false }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                    ) { Text(if (busy) "Adding…" else "Add ${m.title}") }
                }
            }
        }
    }
}

/** Opens an IMDb title in the IMDb app or the browser, per General settings. */
fun openImdb(ctx: android.content.Context, imdbId: String) {
    val web = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.imdb.com/title/$imdbId/"))
    if (ConfigStore.imdbOpenWith == "app") {
        val app = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("imdb:///title/$imdbId/"))
        try { ctx.startActivity(app); return } catch (e: Exception) { /* app not installed */ }
    }
    try { ctx.startActivity(web) } catch (e: Exception) { }
}


// ------------------------------------------------------------ detail extras
@Composable
private fun FactsRow(cfg: ServiceConfig, m: MediaItem) {
    val r = m.raw
    val chips = mutableListOf<Pair<String, Color>>()
    val runtime = r.optInt("runtime")
    if (runtime > 0) chips += (if (runtime >= 60) "${runtime / 60}h ${runtime % 60}m" else "${runtime}m") to Accent
    r.optString("certification").takeIf { it.isNotEmpty() }?.let { chips += it to Accent2 }
    r.optString("studio").takeIf { it.isNotEmpty() }?.let { chips += it to Pink }
    val ratings = r.optJSONObject("ratings")
    if (ratings != null) {
        ratings.optJSONObject("imdb")?.optDouble("value")?.takeIf { it > 0 }?.let { chips += "IMDb %.1f".format(it) to Warn }
        ratings.optJSONObject("tmdb")?.optDouble("value")?.takeIf { it > 0 }?.let { chips += "TMDB %.1f".format(it) to Good }
        ratings.optJSONObject("rottenTomatoes")?.optDouble("value")?.takeIf { it > 0 }?.let { chips += "🍅 ${it.toInt()}%" to Bad }
        ratings.optJSONObject("metacritic")?.optDouble("value")?.takeIf { it > 0 }?.let { chips += "Metacritic ${it.toInt()}" to Accent }
        if (!ratings.has("imdb") && !ratings.has("tmdb")) ratings.optDouble("value").takeIf { it > 0 }?.let { chips += "★ %.1f".format(it) to Warn }
    }
    val tags = r.optJSONArray("tags")
    if (tags != null && tags.length() > 0) chips += "${tags.length()} tag${if (tags.length() == 1) "" else "s"}" to MaterialTheme.colorScheme.onSurfaceVariant
    if (chips.isEmpty()) return
    androidx.compose.foundation.layout.FlowRow(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        chips.forEach { (t, c) -> Pill(t, c) }
    }
}

@Composable
private fun MovieFileCard(m: MediaItem) {
    val f = m.raw.optJSONObject("movieFile") ?: return
    val mi = f.optJSONObject("mediaInfo")
    Panel(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), tint = Good) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Movie, null, tint = Good)
                Spacer(Modifier.width(8.dp))
                Text("File on disk", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(humanBytes(f.optLong("size")), style = MaterialTheme.typography.labelMedium, color = Good, fontWeight = FontWeight.Bold)
            }
            Text(f.optString("relativePath"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp))
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                f.optJSONObject("quality")?.optJSONObject("quality")?.optString("name")?.takeIf { it.isNotEmpty() }?.let { Pill(it, Accent) }
                if (mi != null) {
                    mi.optString("resolution").takeIf { it.isNotEmpty() }?.let { Pill(it, Accent2) }
                    mi.optString("videoCodec").takeIf { it.isNotEmpty() }?.let { Pill(it, Pink) }
                    mi.optString("videoDynamicRangeType").takeIf { it.isNotEmpty() }?.let { Pill(it, Warn) }
                    val audio = listOf(mi.optString("audioCodec"), mi.optDouble("audioChannels").takeIf { !it.isNaN() && it > 0 }?.toString() ?: "").filter { it.isNotEmpty() }.joinToString(" ")
                    if (audio.isNotEmpty()) Pill(audio, Good)
                    mi.optString("audioLanguages").takeIf { it.isNotEmpty() }?.let { Pill(it.take(30), MaterialTheme.colorScheme.onSurfaceVariant) }
                    mi.optString("subtitles").takeIf { it.isNotEmpty() }?.let { Pill("Subs: ${it.take(24)}", MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}

@Composable
private fun CastRow(api: ArrApi, m: MediaItem, nav: Navigator) {
    val l = rememberLoadable(api.cfg, m.id, "credits2") {
        if (api.type == ServiceType.RADARR) api.credits(m.id).ifEmpty { emptyList() } else com.arrmada360.app.api.People.tvCast(m)
    }
    val all = l.data.orEmpty()
    val cast = all.filter { !it.crew }.take(30)
    val crew = all.filter { it.crew && it.role in setOf("Director", "Screenplay", "Writer", "Producer", "Original Music Composer", "Director of Photography", "Novel", "Creator") }
        .groupBy { it.personId to it.name }.map { (_, v) -> v.first().copy(role = v.map { it.role }.distinct().joinToString(", ")) }.take(12)
    if (cast.isEmpty() && crew.isEmpty()) {
        if (api.type == ServiceType.SONARR && !com.arrmada360.app.api.People.available && l.data != null)
            Text("Add a TMDB key in Settings → Discovery sources to see the cast.", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        return
    }
    val open = { c: ArrApi.Credit -> if (c.personId > 0) nav.push(Route.Person(c.personId, c.name)) }
    Column {
        if (cast.isNotEmpty()) {
            SectionTitle("Cast")
            PeopleStrip(cast, open)
        }
        if (crew.isNotEmpty()) {
            SectionTitle("Crew")
            PeopleStrip(crew, open)
        }
        Text("Tap a person for their biography and filmography", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
    }
}

@Composable
private fun PeopleStrip(list: List<ArrApi.Credit>, onClick: (ArrApi.Credit) -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(list) { c ->
            Column(Modifier.width(84.dp).clip(RoundedCornerShape(12.dp)).clickable(enabled = c.personId > 0) { onClick(c) }.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(72.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Text(c.name.take(1), fontWeight = FontWeight.Bold)
                    if (c.image != null) AsyncImage(c.image, null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize().clip(androidx.compose.foundation.shape.CircleShape))
                }
                Text(c.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                Text(c.role, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ItemHistory(api: ArrApi, id: Int) {
    val l = rememberLoadable(api.cfg, id, "ihist") { api.itemHistory(id) }
    val d = l.data
    Column(Modifier.padding(horizontal = 12.dp)) {
        when {
            d == null && l.error != null -> Text(l.error ?: "", color = Bad)
            d == null -> LinearProgressIndicator(Modifier.fillMaxWidth())
            d.isEmpty() -> Text("No history yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> d.take(30).forEach { h ->
                val c = when { h.event.contains("Import", true) -> Good; h.event.contains("Grab", true) -> Accent; h.event.contains("Fail", true) -> Bad; h.event.contains("Delet", true) -> Warn; else -> Accent2 }
                Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                    StatusDot(c, 10.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Row {
                            Text(h.event, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = c, modifier = Modifier.weight(1f))
                            Text(fmtDate(h.date, true), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (h.detail.isNotEmpty()) Text(h.detail, style = MaterialTheme.typography.labelSmall)
                        Text(h.title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (h.quality.isNotEmpty()) Text(h.quality, style = MaterialTheme.typography.labelSmall, color = Accent)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditItemDialog(api: ArrApi, m: MediaItem, done: (Boolean) -> Unit) {
    val run = rememberRunner()
    val profiles = rememberLoadable(api.cfg, "editqp") { api.qualityProfiles() }
    val tagsL = rememberLoadable(api.cfg, "edittags") { api.tags() }
    var qp by remember { mutableIntStateOf(m.raw.optInt("qualityProfileId")) }
    var avail by remember { mutableStateOf(m.raw.optString("minimumAvailability")) }
    var stype by remember { mutableStateOf(m.raw.optString("seriesType")) }
    var tags by remember { mutableStateOf(m.raw.optJSONArray("tags")?.let { a -> (0 until a.length()).map { a.optInt(it) }.toSet() } ?: emptySet()) }
    AlertDialog(
        onDismissRequest = { done(false) }, title = { Text("Edit ${m.title}") },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                profiles.data?.let { p -> Dropdown("Quality profile", p, p.firstOrNull { it.id == qp }, { it.name }, { qp = it.id }) }
                if (api.type == ServiceType.RADARR) Dropdown("Minimum availability", listOf("announced", "inCinemas", "released"), avail,
                    { when (it) { "inCinemas" -> "In cinemas"; "announced" -> "Announced"; else -> "Released" } }, { avail = it })
                if (api.type == ServiceType.SONARR) Dropdown("Series type", listOf("standard", "daily", "anime"), stype, { it.replaceFirstChar { c -> c.uppercase() } }, { stype = it })
                val t = tagsL.data
                if (!t.isNullOrEmpty()) {
                    Text("Tags", style = MaterialTheme.typography.labelLarge)
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        t.forEach { tg -> androidx.compose.material3.FilterChip(tg.id in tags, { tags = if (tg.id in tags) tags - tg.id else tags + tg.id }, label = { Text(tg.name) }) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                run("Saved", { done(true) }) {
                    api.editItem(m.id, qualityProfileId = qp.takeIf { it > 0 },
                        minimumAvailability = avail.takeIf { api.type == ServiceType.RADARR && it.isNotEmpty() },
                        seriesType = stype.takeIf { api.type == ServiceType.SONARR && it.isNotEmpty() },
                        tags = tags.toList())
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = { done(false) }) { Text("Cancel") } },
    )
}
