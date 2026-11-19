package com.arrmada360.app.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arrmada360.app.api.ArrApi
import com.arrmada360.app.api.IndexerResult
import com.arrmada360.app.api.JackettApi
import com.arrmada360.app.api.MediaItem
import com.arrmada360.app.api.ProwlarrApi
import com.arrmada360.app.api.SEARCH_CATEGORIES
import com.arrmada360.app.data.ConfigStore
import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.data.ServiceType
import com.arrmada360.app.ui.AppScaffold
import com.arrmada360.app.ui.Bad
import com.arrmada360.app.ui.ChipRow
import com.arrmada360.app.ui.EmptyBox
import com.arrmada360.app.ui.Good
import com.arrmada360.app.ui.LoadableContent
import com.arrmada360.app.ui.Navigator
import com.arrmada360.app.ui.Panel
import com.arrmada360.app.ui.Pill
import com.arrmada360.app.ui.Poster
import com.arrmada360.app.ui.Route
import com.arrmada360.app.ui.SectionTitle
import com.arrmada360.app.ui.SendLinkDialog
import com.arrmada360.app.ui.ServiceBadge
import com.arrmada360.app.ui.StatusDot
import com.arrmada360.app.ui.Warn
import com.arrmada360.app.ui.copyText
import com.arrmada360.app.ui.fmtDate
import com.arrmada360.app.ui.humanBytes
import com.arrmada360.app.ui.rememberLoadable
import com.arrmada360.app.ui.rememberRunner
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private val RESULT_SORTS = listOf("Seeders", "Size", "Newest", "Title")

@Composable
fun IndexerScreen(cfg: ServiceConfig, nav: Navigator, isRoot: Boolean, back: () -> Unit) {
    val prowlarr = remember(cfg) { if (cfg.type == ServiceType.PROWLARR) ProwlarrApi(cfg) else null }
    val jackett = remember(cfg) { if (cfg.type == ServiceType.JACKETT) JackettApi(cfg) else null }
    var tab by rememberSaveable(cfg.id) { mutableIntStateOf(0) }
    AppScaffold(
        title = cfg.name, isRoot = isRoot, onBack = back, badge = cfg.type, badgeName = cfg.name,
        bottomTabs = if (prowlarr != null) listOf(
            com.arrmada360.app.ui.BottomTab("Search", Icons.Filled.Search),
            com.arrmada360.app.ui.BottomTab("Indexers", androidx.compose.material.icons.Icons.Filled.Storage),
            com.arrmada360.app.ui.BottomTab("Stats", androidx.compose.material.icons.Icons.Filled.BarChart),
            com.arrmada360.app.ui.BottomTab("History", androidx.compose.material.icons.Icons.Filled.History),
            com.arrmada360.app.ui.BottomTab("Server", androidx.compose.material.icons.Icons.Filled.Dns),
        ) else listOf(
            com.arrmada360.app.ui.BottomTab("Search", Icons.Filled.Search),
            com.arrmada360.app.ui.BottomTab("Server", androidx.compose.material.icons.Icons.Filled.Dns),
        ),
        selectedBottom = tab, onBottom = { tab = it },
    ) { pad ->
        Box(Modifier.padding(pad)) {
            val search: @Composable () -> Unit = {
                SearchTab(cfg.id) { q, cats -> if (prowlarr != null) prowlarr.search(q, cats) else jackett!!.search(q, cats) }
            }
            if (prowlarr != null) when (tab) {
                1 -> IndexersTab(prowlarr)
                2 -> ProwlarrStatsTab(prowlarr)
                3 -> ProwlarrHistoryTab(prowlarr)
                4 -> ArrServerTab(remember(cfg) { ArrApi(cfg) }, nav)
                else -> search()
            } else when (tab) {
                1 -> GenericServerTab(cfg, nav)
                else -> search()
            }
        }
    }
}

@Composable
private fun SearchTab(key: String, search: suspend (String, List<Int>) -> List<IndexerResult>) {
    var term by rememberSaveable(key) { mutableStateOf("") }
    var submitted by rememberSaveable(key) { mutableStateOf("") }
    var cat by rememberSaveable(key) { mutableIntStateOf(0) }
    var sort by rememberSaveable(key) { mutableIntStateOf(0) }
    var sortMenu by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<IndexerResult?>(null) }
    val results = rememberLoadable(key, submitted, cat) {
        if (submitted.isBlank()) emptyList() else search(submitted, SEARCH_CATEGORIES[cat].second)
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                term, { term = it }, singleLine = true, placeholder = { Text("Search all indexers") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submitted = term.trim() }),
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
            )
            Box {
                IconButton(onClick = { sortMenu = true }) { Icon(Icons.Filled.Sort, "Sort") }
                DropdownMenu(sortMenu, { sortMenu = false }) {
                    RESULT_SORTS.forEachIndexed { i, s -> DropdownMenuItem(text = { Text((if (i == sort) "✓ " else "    ") + s) }, onClick = { sort = i; sortMenu = false }) }
                }
            }
        }
        ChipRow(SEARCH_CATEGORIES.map { it.first }, cat, { cat = it }, Modifier.padding(vertical = 8.dp))
        if (submitted.isBlank()) EmptyBox("Type something and press search")
        else LoadableContent(results, empty = { it.isEmpty() }, emptyText = "No results") { list ->
            val sorted = when (sort) {
                1 -> list.sortedByDescending { it.size }
                2 -> list.sortedByDescending { it.date }
                3 -> list.sortedBy { it.title.lowercase() }
                else -> list.sortedByDescending { it.seeders ?: -1 }
            }
            LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("${list.size} results", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(sorted) { r -> ResultRow(r) { picked = r } }
            }
        }
    }
    picked?.let { r -> ResultDialog(r, key) { picked = null } }
}

@Composable
private fun ResultRow(r: IndexerResult, onClick: () -> Unit) {
    Panel(Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(12.dp)) {
            Text(r.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Pill(humanBytes(r.size))
                if (r.seeders != null) Pill("▲${r.seeders} ▼${r.peers ?: 0}", if (r.seeders > 5) Good else if (r.seeders > 0) Warn else Bad)
                else Pill(r.protocol.ifEmpty { "usenet" }, MaterialTheme.colorScheme.onSurfaceVariant)
                Text(fmtDate(r.date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            Text(listOf(r.indexer, r.categories).filter { it.isNotEmpty() }.joinToString(" · "), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ResultDialog(r: IndexerResult, serviceId: String, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val uri = LocalUriHandler.current
    val link = r.magnet ?: r.downloadUrl ?: ""
    val cfg = ConfigStore.get(serviceId)
    val extra = mutableListOf<Pair<String, suspend () -> Unit>>()
    if (cfg?.type == ServiceType.PROWLARR) extra += "⚡ Grab with Prowlarr's download client" to suspend { ProwlarrApi(cfg).grab(r) }
    extra += "Copy link" to suspend { ctx.copyText(link) }
    if (r.infoUrl != null) extra += "Open details page" to suspend { uri.openUri(r.infoUrl) }
    SendLinkDialog(link, extra, onDismiss)
}

@Composable
private fun IndexersTab(api: ProwlarrApi) {
    val l = rememberLoadable(api.cfg, "indexers") { api.indexers() }
    val run = rememberRunner()
    LoadableContent(l, empty = { it.isEmpty() }, emptyText = "No indexers configured") { list ->
        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
            item {
                Text("${list.count { it.enabled }} enabled · ${list.count { it.failing }} failing", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
            }
            items(list, key = { it.id }) { ix ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(when { !ix.enabled -> MaterialTheme.colorScheme.onSurfaceVariant; ix.failing -> Bad; else -> Good })
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(ix.name, style = MaterialTheme.typography.bodyMedium)
                        Text("${ix.protocol} · ${ix.privacy}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { run("${ix.name}: test passed") { api.testIndexer(ix.id) } }) { Text("Test") }
                    Switch(ix.enabled, { on -> run(if (on) "Enabled" else "Disabled", { l.load(true) }) { api.setIndexerEnabled(ix.id, on) } })
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

// ============================================================ Search everything
private class UniversalResults(val library: List<Pair<ServiceConfig, List<MediaItem>>>, val lookups: List<Pair<ServiceConfig, List<MediaItem>>>)

@Composable
fun UniversalSearchScreen(nav: Navigator, initial: String, isRoot: Boolean, back: () -> Unit) {
    var term by rememberSaveable { mutableStateOf(initial) }
    var submitted by rememberSaveable { mutableStateOf(initial) }
    val arrs = ConfigStore.services.filter { it.type.isArr }
    val indexers = ConfigStore.services.filter { it.type.isIndexer }
    val res = rememberLoadable(submitted) {
        if (submitted.isBlank()) UniversalResults(emptyList(), emptyList())
        else coroutineScope {
            val lib = arrs.map { c -> async { c to try { ArrApi(c).library().filter { it.title.contains(submitted, true) }.take(10) } catch (e: Exception) { emptyList() } } }
            val look = arrs.map { c -> async { c to try { ArrApi(c).lookup(submitted).filter { it.id == 0 }.take(10) } catch (e: Exception) { emptyList() } } }
            UniversalResults(lib.awaitAll().filter { it.second.isNotEmpty() }, look.awaitAll().filter { it.second.isNotEmpty() })
        }
    }
    AppScaffold(title = "Search everything", isRoot = isRoot, onBack = back) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().imePadding()) {
            OutlinedTextField(
                term, { term = it }, singleLine = true, placeholder = { Text("Movies, shows, artists, authors…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submitted = term.trim() }),
                modifier = Modifier.fillMaxWidth().padding(12.dp), shape = RoundedCornerShape(12.dp),
            )
            if (submitted.isBlank()) {
                EmptyBox(if (arrs.isEmpty()) "Add Sonarr, Radarr, Lidarr or Readarr to search here" else "Searches your libraries and finds new things to add")
            } else LoadableContent(res, empty = { it.library.isEmpty() && it.lookups.isEmpty() && indexers.isEmpty() }, emptyText = "Nothing found") { r ->
                LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                    r.library.forEach { (c, items) ->
                        item { SectionTitle("In ${c.name}") }
                        item { MediaRow(items) { m -> nav.push(Route.ArrDetail(c.id, m.id)) } }
                    }
                    r.lookups.forEach { (c, items) ->
                        item { SectionTitle("Add to ${c.name}") }
                        item { MediaRow(items) { m -> nav.push(Route.ArrAdd(c.id, m.title)) } }
                    }
                    if (indexers.isNotEmpty()) {
                        item { SectionTitle("Search indexers directly") }
                        items(indexers) { c ->
                            Row(Modifier.fillMaxWidth().clickable { nav.push(Route.Service(c.id)) }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                ServiceBadge(c.type, 30.dp); Spacer(Modifier.width(12.dp)); Text("Open ${c.name}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaRow(items: List<MediaItem>, onClick: (MediaItem) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(items) { m ->
            Column(Modifier.width(104.dp).clickable { onClick(m) }) {
                Poster(m.poster, Modifier.fillMaxWidth(), fallback = m.title)
                Text(m.title, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                if (m.year > 0) Text(m.year.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
