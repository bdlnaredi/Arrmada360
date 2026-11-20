package com.arrmada360.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arrmada360.app.api.NamerApi
import com.arrmada360.app.api.NamerFile
import com.arrmada360.app.api.NamerMatch
import com.arrmada360.app.data.ConfigStore
import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.data.display
import com.arrmada360.app.ui.AppScaffold
import com.arrmada360.app.ui.Bad
import com.arrmada360.app.ui.BottomTab
import com.arrmada360.app.ui.ChipRow
import com.arrmada360.app.ui.Good
import com.arrmada360.app.ui.LoadableContent
import com.arrmada360.app.ui.Navigator
import com.arrmada360.app.ui.Panel
import com.arrmada360.app.ui.Pill
import com.arrmada360.app.ui.Poster
import com.arrmada360.app.ui.StatTile
import com.arrmada360.app.ui.Warn
import com.arrmada360.app.ui.fmtEpoch
import com.arrmada360.app.ui.humanBytes
import com.arrmada360.app.ui.rememberLoadable
import com.arrmada360.app.ui.rememberRunner

private val SEARCH_TYPES = listOf("Any", "Scenes", "Movies", "JAV")

/** Artwork from ThePornDB is hidden unless the user turns it on for this Namer instance. */
private fun showArt(cfg: ServiceConfig) = ConfigStore.flag("namerArt_" + cfg.id)

@Composable
private fun ArtworkSwitch(cfg: ServiceConfig, compact: Boolean = false) {
    val on = showArt(cfg)
    Panel(Modifier.fillMaxWidth(), tint = if (on) Good else cfg.type.tint, onClick = { ConfigStore.setFlag("namerArt_" + cfg.id, !on) }) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = if (compact) 6.dp else 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Image, null, tint = if (on) Good else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Show ThePornDB artwork", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (!compact) Text(if (on) "Poster images are shown in match results" else "Off – match results are text only", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            androidx.compose.material3.Switch(on, { ConfigStore.setFlag("namerArt_" + cfg.id, it) })
        }
    }
}

@Composable
fun NamerScreen(cfg: ServiceConfig, nav: Navigator, isRoot: Boolean, back: () -> Unit) {
    val api = remember(cfg) { NamerApi(cfg) }
    var tab by rememberSaveable(cfg.id) { mutableIntStateOf(0) }
    AppScaffold(
        title = cfg.display(), subtitle = listOf("Failed to match", "Queue", "Server")[tab], isRoot = isRoot, onBack = back, badge = cfg.type, badgeName = cfg.name,
        bottomTabs = listOf(BottomTab("Failed", Icons.Filled.ErrorOutline), BottomTab("Queue", Icons.Filled.Queue), BottomTab("Server", Icons.Filled.Dns)),
        selectedBottom = tab, onBottom = { tab = it },
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                0 -> FailedTab(api)
                1 -> QueueTab(api)
                else -> GenericServerTab(cfg, nav) {
                    item { Box(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) { ArtworkSwitch(cfg) } }
                }
            }
        }
    }
}

@Composable
private fun FailedTab(api: NamerApi) {
    val l = rememberLoadable(api.cfg, "namerfailed", autoRefreshMs = 30000) { api.failed() }
    var matching by remember { mutableStateOf<NamerFile?>(null) }
    var actions by remember { mutableStateOf<NamerFile?>(null) }
    LoadableContent(l, empty = { it.isEmpty() }, emptyText = "No failed files 🎉\nEverything Namer saw was matched.") { list ->
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { ArtworkSwitch(api.cfg) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Need matching", list.size.toString(), Warn, Modifier.weight(1f))
                    StatTile("Total size", humanBytes(list.sumOf { it.size }), api.cfg.type.tint, Modifier.weight(1f))
                }
            }
            items(list, key = { it.file }) { f ->
                Panel(Modifier.fillMaxWidth(), tint = Warn, onClick = { matching = f }, onLongClick = { actions = f }) {
                    Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(f.name.ifEmpty { f.file }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (f.ext.isNotEmpty()) Pill(f.ext, api.cfg.type.tint)
                                Pill(humanBytes(f.size), MaterialTheme.colorScheme.outline)
                                if (f.percentage > 0) Pill("Best match ${f.percentage.toInt()}%", if (f.percentage >= 80) Good else Warn)
                            }
                            if (f.updated > 0) Text(fmtEpoch(f.updated), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { matching = f }) { Icon(Icons.Filled.DriveFileRenameOutline, "Match & rename", tint = Good) }
                    }
                }
            }
        }
    }
    matching?.let { f -> MatchSheet(api, f, onDismiss = { matching = null }) { matching = null; l.load(true) } }
    actions?.let { f -> FileActions(api, f, onDismiss = { actions = null }) { actions = null; l.load(true) } }
}

@Composable
private fun QueueTab(api: NamerApi) {
    val l = rememberLoadable(api.cfg, "namerqueue", autoRefreshMs = 5000) { api.queueSize() to api.queued() }
    LoadableContent(l) { (size, list) ->
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { StatTile("Waiting in queue", size, api.cfg.type.tint, Modifier.fillMaxWidth()) }
            if (list.isEmpty()) item { Text("Nothing queued – Namer is idle.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp)) }
            items(list, key = { "q" + it.file + it.updated }) { f ->
                Panel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(f.name.ifEmpty { f.file }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${f.ext} · ${humanBytes(f.size)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** Search phrases to try, best first: the original file name (ThePornDB parses it), then a cleaned version. */
private fun queryCandidates(f: NamerFile): List<String> {
    val videoExt = setOf("mp4", "mkv", "avi", "wmv", "mov", "m4v", "webm", "flv", "ts", "mpg", "mpeg") + f.ext.lowercase()
    val stem = f.name.ifEmpty { f.file.substringAfterLast('/') }.let { n ->
        if (n.contains('.') && n.substringAfterLast('.').lowercase() in videoExt) n.substringBeforeLast('.') else n
    }
    val cleaned = stem.replace(Regex("[._]+"), " ")
        .replace(Regex("(?i)\\b(2160p|1080p|720p|480p|4k|xxx|mp4|mkv|hevc|h\\.?26[45]|x26[45]|web(-?dl)?|hd|sd)\\b"), " ")
        .replace(Regex("\\s+"), " ").trim()
    return listOf(stem, cleaned).filter { it.isNotBlank() }.distinct()
}

private class SearchOutcome(val matches: List<NamerMatch>, val usedQuery: String, val reply: String)

@Composable
private fun MatchSheet(api: NamerApi, f: NamerFile, onDismiss: () -> Unit, onDone: () -> Unit) {
    val run = rememberRunner()
    val candidates = remember(f) { queryCandidates(f) }
    var query by remember { mutableStateOf(candidates.firstOrNull() ?: "") }
    // null = automatic (try every candidate until something matches); otherwise exactly what the user typed
    var submitted by remember { mutableStateOf<String?>(null) }
    var type by remember { mutableIntStateOf(0) }
    var usePhash by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<NamerMatch?>(null) }
    var showReply by remember { mutableStateOf(false) }
    val results = rememberLoadable(api.cfg, f.file, submitted, type, usePhash) {
        val t = SEARCH_TYPES[type]
        if (usePhash) SearchOutcome(api.phash(f.file, t), "video fingerprint", api.lastReply)
        else {
            val tries = submitted?.let { listOf(it) } ?: candidates
            var out = SearchOutcome(emptyList(), tries.firstOrNull() ?: "", "")
            for (q in tries) {
                val m = api.search(f.file, q, t)
                out = SearchOutcome(m, q, api.lastReply)
                if (m.isNotEmpty()) break
            }
            out
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().height(700.dp).imePadding()) {
            Text("Match & rename", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 16.dp))
            Text(f.file, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, modifier = Modifier.padding(horizontal = 16.dp))
            OutlinedTextField(
                query, { query = it }, singleLine = true, leadingIcon = { Icon(Icons.Filled.Search, null) },
                placeholder = { Text("File name, or site + performer / title / date") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { usePhash = false; submitted = query.trim() }),
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp), shape = RoundedCornerShape(12.dp),
            )
            Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { usePhash = false; submitted = query.trim() }) { Icon(Icons.Filled.Search, null); Text(" Search") }
                TextButton(onClick = { usePhash = true }) { Icon(Icons.Filled.Fingerprint, null); Text(" Match by fingerprint") }
            }
            ChipRow(SEARCH_TYPES, type, { type = it }, Modifier.padding(bottom = 6.dp))
            Box(Modifier.padding(horizontal = 12.dp, vertical = 2.dp)) { ArtworkSwitch(api.cfg, compact = true) }
            val d = results.data
            when {
                d == null && results.error != null -> Column(Modifier.padding(16.dp)) {
                    Text("Search failed", style = MaterialTheme.typography.titleMedium, color = Bad)
                    Text(results.error ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    Text("If this keeps happening, check that a ThePornDB API token is set in namer.cfg (porndb_token) and look at Namer's log.",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { results.load() }) { Text("Try again") }
                }
                d == null -> Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.CircularProgressIndicator(color = api.cfg.type.tint)
                    Spacer(Modifier.height(10.dp))
                    Text(if (usePhash) "Fingerprinting the video – this can take a minute…" else "Searching ThePornDB…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                d.matches.isEmpty() -> Column(Modifier.padding(16.dp)) {
                    Text("No matches", style = MaterialTheme.typography.titleMedium, color = Warn)
                    Text("Searched for: “${d.usedQuery}” (${SEARCH_TYPES[type]})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    Text("Try: just the site and a performer, the date as YY.MM.DD, another type (Scenes / Movies), or Match by fingerprint.",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    TextButton(onClick = { showReply = !showReply }) { Text(if (showReply) "Hide Namer's reply" else "Show Namer's reply") }
                    if (showReply) Text(d.reply.ifEmpty { "(nothing)" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> {
                    Text("${d.matches.size} matches for “${d.usedQuery}”", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
                    LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(d.matches, key = { it.uuid + "|" + it.name + "|" + it.date }) { m ->
                            Panel(Modifier.fillMaxWidth(), tint = if (m.score >= 90) Good else null, onClick = { confirm = m }) {
                                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (showArt(api.cfg) && m.poster != null) { Poster(m.poster, Modifier.width(96.dp), ratio = 16f / 9f, corner = 8.dp); Spacer(Modifier.width(10.dp)) }
                                    Column(Modifier.weight(1f)) {
                                        Text(m.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Text(listOf(m.site, m.date).filter { it.isNotEmpty() }.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (m.performers.isNotEmpty()) Text(m.performers, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Spacer(Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (m.type.isNotEmpty()) Pill(m.type.lowercase().replaceFirstChar { it.uppercase() }, api.cfg.type.tint)
                                            if (m.score >= 0) Pill("${m.score.toInt()}% match", if (m.score >= 90) Good else if (m.score >= 70) Warn else Bad)
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
    confirm?.let { m ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Rename to this match?") },
            text = { Text("${m.name}\n${listOf(m.site, m.date).filter { it.isNotEmpty() }.joinToString(" · ")}\n\nNamer will tag, rename and move the file to your destination folder.") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    if (m.uuid.isEmpty()) run(null) { throw Exception("This result has no ThePornDB id – pick another") }
                    else run("Sent to Namer – renaming", onDone) { api.rename(f.file, m.uuid) }
                }) { Text("Rename", color = Good, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun FileActions(api: NamerApi, f: NamerFile, onDismiss: () -> Unit, onDone: () -> Unit) {
    val run = rememberRunner()
    var log by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(f.name.ifEmpty { f.file }, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text("${f.ext} · ${humanBytes(f.size)}", style = MaterialTheme.typography.labelSmall)
                TextButton(onClick = { run(null) { log = api.failedLog(f.file).ifEmpty { "No log for this file." } } }) { Text("Show why it failed") }
                log?.let { Text(it.take(4000), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (!confirmDelete) TextButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, null, tint = Bad); Text(" Delete file", color = Bad) }
                else TextButton(onClick = { run("Deleted", onDone) { if (!api.delete(f.file)) throw Exception("Namer refused to delete it (check allow_delete_files in namer.cfg)") } }) {
                    Text("Tap to permanently delete", color = Bad, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Suppress("unused")
private val keepFill = Modifier.fillMaxSize()
