package com.arrmada360.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.arrmada360.app.api.*
import com.arrmada360.app.data.ConfigStore
import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.data.ServiceType
import com.arrmada360.app.ui.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

// ============================================================ Trailer + where to stream (detail page)
@Composable
fun TrailerAndStreaming(api: ArrApi, m: MediaItem, nav: Navigator) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val movie = api.type == ServiceType.RADARR
    val prov = rememberLoadable(api.cfg, m.id, "providers") { if (movie || api.type == ServiceType.SONARR) MediaExtras.providers(m, movie) else null }
    var loadingTrailer by remember { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = {
                loadingTrailer = true
                scope.launch {
                    val key = try { MediaExtras.trailerKey(m, movie) } catch (e: Exception) { null }
                    loadingTrailer = false
                    MediaExtras.openTrailer(ctx, key, "${m.title} ${if (m.year > 0) m.year else ""}")
                }
            }) {
                if (loadingTrailer) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Icon(Icons.Filled.SmartDisplay, null, tint = Color(0xFFFF4E45))
                Spacer(Modifier.width(6.dp)); Text("Trailer")
            }
            val coll = m.raw.optJSONObject("collection")
            if (movie && coll != null && coll.optString("title").isNotEmpty()) FilledTonalButton(onClick = { nav.push(Route.ArrCollections(api.cfg.id, coll.optInt("tmdbId"))) }) {
                Icon(Icons.Filled.CollectionsBookmark, null); Spacer(Modifier.width(6.dp))
                Text(coll.optString("title"), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (movie || api.type == ServiceType.SONARR) {
            if (!MediaExtras.hasTmdb) Text("Add a free TMDB key in Settings → Discovery sources to see where this streams.",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            else prov.data?.let { p -> WhereToStream(p) }
        }
    }
}

@Composable
private fun WhereToStream(p: WatchProviders) {
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    Column(Modifier.padding(top = 10.dp)) {
        Text("Where to watch · ${p.region}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        val groups = listOf("Stream" to p.stream, "Free" to p.free, "Rent" to p.rent, "Buy" to p.buy).filter { it.second.isNotEmpty() }
        if (groups.isEmpty()) Text("Not streaming anywhere in your country right now", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        groups.forEach { (label, list) ->
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(52.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(list.distinctBy { it.name }) { pr ->
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(enabled = p.link.isNotEmpty()) { uri.openUri(p.link) }, contentAlignment = Alignment.Center) {
                            Text(pr.name.take(2), style = MaterialTheme.typography.labelSmall)
                            if (pr.logo != null) AsyncImage(pr.logo, pr.name, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                        }
                    }
                }
            }
        }
        if (p.link.isNotEmpty() && groups.isNotEmpty()) Text("Data from JustWatch via TMDB · tap a logo for links", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    }
}

// ============================================================ Collections (Radarr)
@Composable
fun CollectionsScreen(cfg: ServiceConfig, focusTmdb: Int?, nav: Navigator, back: () -> Unit) {
    val api = remember(cfg) { ArrApi(cfg) }
    val l = rememberLoadable(cfg, "collections") {
        coroutineScope {
            val c = async { api.collections() }
            val lib = async { api.library() }
            c.await() to lib.await().associateBy { it.raw.optInt("tmdbId") }
        }
    }
    val profiles = rememberLoadable(cfg, "collqp") { api.qualityProfiles() }
    val roots = rememberLoadable(cfg, "collroot") { api.rootFolders() }
    val run = rememberRunner()
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var q by rememberSaveable { mutableStateOf("") }
    var confirm by remember { mutableStateOf<ArrCollection?>(null) }
    AppScaffold(title = "Collections", isRoot = false, onBack = back, badge = cfg.type, badgeName = cfg.name) { pad ->
        Box(Modifier.padding(pad)) {
            LoadableContent(l, empty = { it.first.isEmpty() }, emptyText = "No collections yet – Radarr builds them from the movies you own") { (cols, owned) ->
                val withCounts = cols.map { c -> c to c.movies.count { it.tmdbId in owned } }
                var shown = when (filter) {
                    1 -> withCounts.filter { (c, n) -> n < c.movies.size }
                    2 -> withCounts.filter { (c, n) -> n == c.movies.size }
                    else -> withCounts
                }.filter { q.isBlank() || it.first.title.contains(q, true) }
                if (focusTmdb != null && focusTmdb > 0) shown = shown.sortedByDescending { it.first.raw.optInt("tmdbId") == focusTmdb }
                LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        OutlinedTextField(q, { q = it }, singleLine = true, placeholder = { Text("Search collections") }, leadingIcon = { Icon(Icons.Filled.Search, null) },
                            shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                    }
                    item {
                        ChipRow(listOf("All", "Missing movies", "Complete"), filter, { filter = it },
                            counts = listOf(withCounts.size, withCounts.count { it.second < it.first.movies.size }, withCounts.count { it.second == it.first.movies.size }))
                    }
                    items(shown, key = { it.first.id }) { (c, have) ->
                        CollectionCard(c, have, owned, onAddMissing = { confirm = c }, onOpen = { id -> nav.push(Route.ArrDetail(cfg.id, id)) }) { movie ->
                            val qp = profiles.data?.firstOrNull()?.id; val root = roots.data?.firstOrNull()?.path
                            if (qp == null || root == null) return@CollectionCard
                            run("Added ${movie.title}", { l.load(true) }) { api.addByTmdb(movie.tmdbId, qp, root, true) }
                        }
                    }
                }
            }
        }
    }
    confirm?.let { c ->
        var qp by remember { mutableStateOf(profiles.data?.firstOrNull()) }
        var root by remember { mutableStateOf(roots.data?.firstOrNull()) }
        var search by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Add missing from ${c.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Radarr will monitor this collection and add every movie you don't have yet.", style = MaterialTheme.typography.bodySmall)
                    profiles.data?.let { p -> Dropdown("Quality profile", p, qp, { it.name }, { qp = it }) }
                    roots.data?.let { r -> Dropdown("Root folder", r, root, { "${it.path} (${humanBytes(it.free)} free)" }, { root = it }) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(search, { search = it }); Text("Start searching right away") }
                }
            },
            confirmButton = {
                TextButton(enabled = qp != null && root != null, onClick = {
                    val p = qp!!; val r = root!!; confirm = null
                    run("${c.title}: adding missing movies", { l.load(true) }) { api.monitorCollection(c, p.id, r.path, search) }
                }) { Text("Add missing") }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun CollectionCard(c: ArrCollection, have: Int, owned: Map<Int, MediaItem>, onAddMissing: () -> Unit, onOpen: (Int) -> Unit, onAddOne: (CollMovie) -> Unit) {
    val complete = have == c.movies.size
    Panel(Modifier.fillMaxWidth(), tint = if (complete) Good else Warn) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(c.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("$have of ${c.movies.size} in your library" + if (c.monitored) " · monitored" else "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!complete && !c.monitored) TextButton(onClick = onAddMissing) { Icon(Icons.Filled.LibraryAdd, null); Spacer(Modifier.width(4.dp)); Text("Add missing") }
                else if (complete) Icon(Icons.Filled.CheckCircle, null, tint = Good)
            }
            ProgressBar(have.toFloat() / c.movies.size.coerceAtLeast(1), if (complete) Good else Warn, height = 4.dp)
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(c.movies, key = { it.tmdbId }) { mv ->
                    val lib = owned[mv.tmdbId]
                    var asking by remember { mutableStateOf(false) }
                    Column(Modifier.width(86.dp).clickable { if (lib != null) onOpen(lib.id) else asking = true }) {
                        Box {
                            Poster(mv.poster, Modifier.fillMaxWidth(), fallback = mv.title)
                            if (lib == null) Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Add, "Add", tint = Color.White, modifier = Modifier.size(30.dp))
                            } else Box(Modifier.align(Alignment.TopEnd).padding(4.dp)) { MediaStatusIcon(if (lib.complete) MediaStatus.AVAILABLE else MediaStatus.WAITING) }
                        }
                        Text(mv.title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (mv.year > 0) Text("${mv.year}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (asking) AlertDialog(
                        onDismissRequest = { asking = false }, title = { Text("Add ${mv.title}?") },
                        text = { Text("Adds it with your first quality profile and root folder, and starts searching.") },
                        confirmButton = { TextButton(onClick = { asking = false; onAddOne(mv) }) { Text("Add") } },
                        dismissButton = { TextButton(onClick = { asking = false }) { Text("Cancel") } },
                    )
                }
            }
        }
    }
}

// ============================================================ Quality & duplicate finder
@Composable
fun QualityFinderScreen(cfg: ServiceConfig, nav: Navigator, back: () -> Unit) {
    val api = remember(cfg) { ArrApi(cfg) }
    val movie = api.type == ServiceType.RADARR
    val tabs = if (movie) listOf("Below cutoff", "Low quality", "No subtitles", "Duplicates") else listOf("Below cutoff", "Duplicates")
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val run = rememberRunner()
    val cutoff = rememberLoadable(cfg, "cutoff") { api.cutoffUnmet() }
    val lib = rememberLoadable(cfg, "qlib") { api.library() }
    val dupes = rememberLoadable(cfg, "dupes") { findDuplicates(cfg) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    AppScaffold(title = "Quality & duplicates", isRoot = false, onBack = back, badge = cfg.type, badgeName = cfg.name) { pad ->
        Column(Modifier.padding(pad)) {
            ChipRow(tabs, tab, { tab = it; selected = emptySet() })
            when (tabs[tab]) {
                "Below cutoff" -> LoadableContent(cutoff, empty = { it.isEmpty() }, emptyText = "Everything meets your quality cutoff 🎉") { rows ->
                    val key = { r: QualityRow -> "${r.itemId}-${r.childId}" }
                    Column {
                        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${rows.size} waiting for a better release", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                            TextButton(onClick = { selected = if (selected.size == rows.size) emptySet() else rows.map(key).toSet() }) { Text(if (selected.size == rows.size) "Clear" else "Select all") }
                            Button(enabled = selected.isNotEmpty(), onClick = {
                                val pick = rows.filter { key(it) in selected }
                                run("Searching ${pick.size} for upgrades") { api.searchCutoff(pick) }; selected = emptySet()
                            }) { Icon(Icons.Filled.Upgrade, null); Text(" Search ${selected.size}") }
                        }
                        LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(rows, key = key) { r ->
                                QRow(r.poster, r.title, r.subtitle, r.quality, r.size, key(r) in selected,
                                    onClick = { selected = if (key(r) in selected) selected - key(r) else selected + key(r) },
                                    onOpen = { nav.push(Route.ArrDetail(cfg.id, r.itemId)) })
                            }
                        }
                    }
                }
                "Low quality", "No subtitles" -> LoadableContent(lib) { items ->
                    var maxH by rememberSaveable { mutableIntStateOf(720) }
                    val rows = if (tabs[tab] == "Low quality") items.filter { it.raw.has("movieFile") && movieHeight(it) in 1..maxH }.sortedBy { movieHeight(it) }
                    else items.filter { it.raw.has("movieFile") && movieSubs(it).isBlank() }.sortedBy { it.title }
                    Column {
                        if (tabs[tab] == "Low quality") ChipRow(listOf("SD & below", "720p & below", "1080p & below"), when (maxH) { 576 -> 0; 720 -> 1; else -> 2 }, { maxH = listOf(576, 720, 1080)[it] })
                        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${rows.size} movies", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                            TextButton(onClick = { selected = if (selected.size == rows.size) emptySet() else rows.map { it.id.toString() }.toSet() }) { Text(if (selected.size == rows.size && rows.isNotEmpty()) "Clear" else "Select all") }
                            Button(enabled = selected.isNotEmpty(), onClick = {
                                val ids = rows.filter { it.id.toString() in selected }.map { it.id }
                                run("Searching ${ids.size} movies") { api.bulkSearch(ids) }; selected = emptySet()
                            }) { Icon(Icons.Filled.Search, null); Text(" Search ${selected.size}") }
                        }
                        if (rows.isEmpty()) EmptyBox(if (tabs[tab] == "Low quality") "No movies at or below this quality" else "Every movie file has subtitles")
                        LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(rows, key = { it.id }) { m ->
                                val h = movieHeight(m)
                                QRow(m.poster, m.title, if (m.year > 0) "${m.year}" else "", listOf(movieQuality(m), if (h > 0) "${h}p" else "").filter { it.isNotEmpty() }.joinToString(" · "),
                                    m.sizeOnDisk, m.id.toString() in selected,
                                    onClick = { val k = m.id.toString(); selected = if (k in selected) selected - k else selected + k },
                                    onOpen = { nav.push(Route.ArrDetail(cfg.id, m.id)) })
                            }
                        }
                    }
                }
                else -> LoadableContent(dupes, empty = { it.isEmpty() }, emptyText = "No duplicates across your ${cfg.type.label} instances") { groups ->
                    LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { Text("Titles that exist in more than one instance (for example Radarr and Radarr 4K)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        items(groups, key = { it.first }) { (_, list) ->
                            Panel(Modifier.fillMaxWidth(), tint = Warn) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(list.first().second.title + if (list.first().second.year > 0) " (${list.first().second.year})" else "", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("Using ${humanBytes(list.sumOf { it.second.sizeOnDisk })} in total", style = MaterialTheme.typography.labelSmall, color = Warn)
                                    list.forEach { (inst, m) ->
                                        Row(Modifier.fillMaxWidth().clickable { nav.push(Route.ArrDetail(inst.id, m.id)) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            ServiceLogo(inst.type, 22.dp, inst.name); Spacer(Modifier.width(8.dp))
                                            Text(inst.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                            val q = if (inst.type == ServiceType.RADARR) movieQuality(m) else m.progressText
                                            Text(listOf(q, humanBytes(m.sizeOnDisk)).filter { it.isNotEmpty() }.joinToString(" · "), style = MaterialTheme.typography.labelSmall)
                                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun findDuplicates(cfg: ServiceConfig): List<Pair<String, List<Pair<ServiceConfig, MediaItem>>>> = coroutineScope {
    val same = ConfigStore.services.filter { it.type == cfg.type }
    val idKey = if (cfg.type == ServiceType.RADARR) "tmdbId" else "tvdbId"
    val all = same.map { c -> async { try { ArrApi(c).library().map { c to it } } catch (e: Exception) { emptyList() } } }.awaitAll().flatten()
    all.groupBy { it.second.raw.optInt(idKey).takeIf { id -> id > 0 }?.toString() ?: ("t:" + it.second.title.lowercase() + it.second.year) }
        .filter { (_, v) -> v.map { it.first.id }.distinct().size > 1 }
        .toList().sortedByDescending { (_, v) -> v.sumOf { it.second.sizeOnDisk } }
}

@Composable
private fun QRow(poster: String?, title: String, sub: String, quality: String, size: Long, selected: Boolean, onClick: () -> Unit, onOpen: () -> Unit) {
    Panel(Modifier.fillMaxWidth(), tint = if (selected) MaterialTheme.colorScheme.primary else Accent, onClick = onClick) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(selected, { onClick() })
            Poster(poster, Modifier.width(38.dp), corner = 6.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (quality.isNotEmpty()) Pill(quality, Warn)
                    if (size > 0) Pill(humanBytes(size), Accent2)
                }
            }
            IconButton(onClick = onOpen) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Open") }
        }
    }
}
