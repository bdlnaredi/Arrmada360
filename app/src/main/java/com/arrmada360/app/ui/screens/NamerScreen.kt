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
                    item {
                        com.arrmada360.app.ui.MenuRow(
                            Icons.Filled.Image, if (showArt(cfg)) "Artwork: shown" else "Artwork: hidden",
                            "Show ThePornDB poster images in match results. Off by default.", cfg.type.tint, arrow = false,
                        ) { ConfigStore.setFlag("namerArt_" + cfg.id, !showArt(cfg)) }
                    }
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
                            Text(f.name.ifEmpty { f.file }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
                        Text(f.name.ifEmpty { f.file }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${f.ext} · ${humanBytes(f.size)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** Cleans a file name into a sensible search phrase: dots/underscores → spaces, drop resolution tags. */
private fun guessQuery(f: NamerFile): String =
    f.name.ifEmpty { f.file.substringAfterLast('/').substringBeforeLast('.') }
        .replace(Regex("[._]+"), " ")
        .replace(Regex("(?i)\\b(2160p|1080p|720p|480p|4k|xxx|mp4|mkv|hevc|h\\.?26[45]|x26[45]|web(-?dl)?|hd|sd)\\b"), " ")
        .replace(Regex("\\s+"), " ").trim()

@Composable
private fun MatchSheet(api: NamerApi, f: NamerFile, onDismiss: () -> Unit, onDone: () -> Unit) {
    val run = rememberRunner()
    var query by remember { mutableStateOf(guessQuery(f)) }
    var submitted by remember { mutableStateOf(query) }
    var type by remember { mutableIntStateOf(0) }
    var confirm by remember { mutableStateOf<NamerMatch?>(null) }
    val results = rememberLoadable(api.cfg, f.file, submitted, type) { if (submitted.isBlank()) emptyList() else api.search(f.file, submitted, SEARCH_TYPES[type]) }
    val art = showArt(api.cfg)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().height(680.dp).imePadding()) {
            Text("Match & rename", style = MaterialTheme.typography.titleLarge, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp))
            Text(f.file, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, modifier = Modifier.padding(horizontal = 16.dp))
            OutlinedTextField(
                query, { query = it }, singleLine = true, leadingIcon = { Icon(Icons.Filled.Search, null) },
                placeholder = { Text("Site, performer, title or date") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { submitted = query.trim() }),
                modifier = Modifier.fillMaxWidth().padding(12.dp), shape = RoundedCornerShape(12.dp),
            )
            ChipRow(SEARCH_TYPES, type, { type = it }, Modifier.padding(bottom = 8.dp))
            LoadableContent(results, empty = { it.isEmpty() }, emptyText = "No matches – try fewer words, e.g. just the site and a performer") { list ->
                LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(list, key = { it.uuid + it.name }) { m ->
                        Panel(Modifier.fillMaxWidth(), tint = if (m.score >= 90) Good else null, onClick = { confirm = m }) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (art && m.poster != null) { Poster(m.poster, Modifier.width(96.dp), ratio = 16f / 9f, corner = 8.dp); Spacer(Modifier.width(10.dp)) }
                                Column(Modifier.weight(1f)) {
                                    Text(m.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
    confirm?.let { m ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Rename to this match?") },
            text = { Text("${m.name}\n${listOf(m.site, m.date).filter { it.isNotEmpty() }.joinToString(" · ")}\n\nNamer will tag, rename and move the file to your destination folder.") },
            confirmButton = { TextButton(onClick = { confirm = null; run("Sent to Namer – renaming", onDone) { api.rename(f.file, m.uuid) } }) { Text("Rename", color = Good, fontWeight = FontWeight.Bold) } },
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
