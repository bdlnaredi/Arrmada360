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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arrmada360.app.api.ArrApi
import com.arrmada360.app.api.ImportItem
import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.data.display
import com.arrmada360.app.ui.AppScaffold
import com.arrmada360.app.ui.Bad
import com.arrmada360.app.ui.ChipRow
import com.arrmada360.app.ui.EmptyBox
import com.arrmada360.app.ui.Good
import com.arrmada360.app.ui.GradientButton
import com.arrmada360.app.ui.LoadableContent
import com.arrmada360.app.ui.Panel
import com.arrmada360.app.ui.Pill
import com.arrmada360.app.ui.SectionTitle
import com.arrmada360.app.ui.Warn
import com.arrmada360.app.ui.humanBytes
import com.arrmada360.app.ui.rememberLoadable
import com.arrmada360.app.ui.rememberRunner

private sealed interface ImportSource {
    data class Folder(val path: String) : ImportSource
    data class Download(val id: String, val title: String) : ImportSource
}

/** Manual import: pick a stuck download from the queue or type a folder, review files, import. */
@Composable
fun ManualImportScreen(cfg: ServiceConfig, back: () -> Unit) {
    val api = remember(cfg) { ArrApi(cfg) }
    var source by remember { mutableStateOf<ImportSource?>(null) }
    var folder by remember { mutableStateOf("") }

    AppScaffold(title = "Manual import", subtitle = cfg.display(), isRoot = false, onBack = { if (source != null) source = null else back() }, badge = cfg.type, badgeName = cfg.name) { pad ->
        Box(Modifier.padding(pad).imePadding()) {
            val s = source
            if (s == null) {
                val queue = rememberLoadable(cfg, "miq") { api.queue() }
                Column {
                    Panel(Modifier.fillMaxWidth().padding(12.dp)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Import from a folder", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            OutlinedTextField(
                                folder, { folder = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("/downloads/complete/tv") },
                                leadingIcon = { Icon(Icons.Filled.Folder, null) },
                                supportingText = { Text("The path as ${cfg.type.label} sees it (inside its container)") },
                                shape = RoundedCornerShape(12.dp),
                            )
                            GradientButton("Scan folder", Modifier.fillMaxWidth(), enabled = folder.isNotBlank()) { source = ImportSource.Folder(folder.trim()) }
                        }
                    }
                    SectionTitle("Or pick a download from the queue")
                    LoadableContent(queue, empty = { it.isEmpty() }, emptyText = "Queue is empty") { list ->
                        LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(list.filter { it.downloadId.isNotEmpty() }, key = { it.id }) { q ->
                                val blocked = q.status.contains("import", true) || q.warning.isNotEmpty()
                                Panel(Modifier.fillMaxWidth(), tint = if (blocked) Warn else null, onClick = { source = ImportSource.Download(q.downloadId, q.title) }) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(q.subtitle.ifEmpty { q.title }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(q.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Spacer(Modifier.height(4.dp))
                                        Pill(q.status, if (blocked) Warn else MaterialTheme.colorScheme.primary)
                                        if (q.warning.isNotEmpty()) Text(q.warning, style = MaterialTheme.typography.labelSmall, color = Warn, maxLines = 3)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                ImportFiles(api, s) { source = null; back() }
            }
        }
    }
}

@Composable
private fun ImportFiles(api: ArrApi, source: ImportSource, done: () -> Unit) {
    val l = rememberLoadable(api.cfg, source) {
        when (source) {
            is ImportSource.Folder -> api.manualImportList(source.path, null)
            is ImportSource.Download -> api.manualImportList(null, source.id)
        }
    }
    val run = rememberRunner()
    var selected by remember { mutableStateOf<Set<String>?>(null) }
    var mode by remember { mutableStateOf(0) }
    val modes = listOf("Move" to "move", "Copy" to "copy", "Auto" to "auto")
    LoadableContent(l, empty = { it.isEmpty() }, emptyText = "No importable video files found there") { items ->
        val sel = selected ?: items.filter { it.parentId > 0 && it.rejections.isEmpty() }.map { it.path }.toSet().also { selected = it }
        Column {
            ChipRow(modes.map { it.first }, mode, { mode = it }, Modifier.padding(vertical = 8.dp))
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { it.path }) { f -> FileRow(f, f.path in sel) { on -> selected = if (on) sel + f.path else sel - f.path } }
            }
            val chosen = items.filter { it.path in sel && it.parentId > 0 }
            GradientButton(
                "Import ${chosen.size} file${if (chosen.size == 1) "" else "s"} (${modes[mode].first.lowercase()})",
                Modifier.fillMaxWidth().padding(12.dp).navigationBarsPadding(), enabled = chosen.isNotEmpty(),
            ) { run("Import started", done) { api.manualImport(chosen, modes[mode].second) } }
        }
    }
}

@Composable
private fun FileRow(f: ImportItem, checked: Boolean, onCheck: (Boolean) -> Unit) {
    val matched = f.parentId > 0
    Panel(Modifier.fillMaxWidth(), tint = if (!matched || f.rejections.isNotEmpty()) Warn else Good, onClick = { if (matched) onCheck(!checked) }) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked && matched, { onCheck(it) }, enabled = matched)
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(f.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (matched) Pill(f.parentTitle, Good) else Pill("Not matched", Bad)
                    if (f.childText.isNotEmpty()) Pill(f.childText)
                    if (f.quality.isNotEmpty()) Pill(f.quality, MaterialTheme.colorScheme.secondary)
                    Pill(humanBytes(f.size), MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!matched) Text("Match this file in the ${"web UI"} first", style = MaterialTheme.typography.labelSmall, color = Warn)
                f.rejections.forEach { Text("• $it", style = MaterialTheme.typography.labelSmall, color = Warn) }
            }
        }
    }
}

