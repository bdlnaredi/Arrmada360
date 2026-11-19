package com.arrmada360.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arrmada360.app.api.DownloadClient
import com.arrmada360.app.api.QbitClient
import com.arrmada360.app.api.RssArticle
import com.arrmada360.app.api.RssClient
import com.arrmada360.app.api.RssFeed
import com.arrmada360.app.api.RssRule
import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.ui.Bad
import com.arrmada360.app.ui.ChipRow
import com.arrmada360.app.ui.Dropdown
import com.arrmada360.app.ui.EmptyBox
import com.arrmada360.app.ui.Good
import com.arrmada360.app.ui.GradientButton
import com.arrmada360.app.ui.LoadableContent
import com.arrmada360.app.ui.Panel
import com.arrmada360.app.ui.Pill
import com.arrmada360.app.ui.PillTabs
import com.arrmada360.app.ui.Rainbow
import com.arrmada360.app.ui.SendLinkDialog
import com.arrmada360.app.ui.StatusDot
import com.arrmada360.app.ui.Warn
import com.arrmada360.app.ui.fmtDate
import com.arrmada360.app.ui.rememberLoadable
import com.arrmada360.app.ui.rememberRunner
import org.json.JSONArray
import org.json.JSONObject

/** RSS reader for qBittorrent and ruTorrent: articles, feeds and (qBittorrent) auto-download rules. */
@Composable
fun RssTab(cfg: ServiceConfig, client: DownloadClient) {
    val rss = client as? RssClient ?: return EmptyBox("This client has no RSS support")
    var view by rememberSaveable(cfg.id + "rssview") { mutableIntStateOf(0) }
    val feeds = rememberLoadable(cfg, "rssfeeds", autoRefreshMs = 60_000) { rss.rssFeeds() }
    var feedFilter by rememberSaveable(cfg.id + "rssfeed") { mutableStateOf<String?>(null) }
    val tabs = if (rss.hasRules) listOf("Articles", "Feeds", "Rules") else listOf("Articles", "Feeds")
    Column {
        PillTabs(tabs, view, { view = it })
        when (view) {
            0 -> ArticlesView(cfg, client, rss, feeds, feedFilter) { feedFilter = it }
            1 -> FeedsView(rss, feeds) { f -> feedFilter = f.key; view = 0 }
            else -> RulesView(cfg, client, rss, feeds.data ?: emptyList())
        }
    }
}

// ------------------------------------------------------------ articles
@Composable
private fun ArticlesView(
    cfg: ServiceConfig,
    client: DownloadClient,
    rss: RssClient,
    feeds: com.arrmada360.app.ui.Loadable<List<RssFeed>>,
    feedFilter: String?,
    setFilter: (String?) -> Unit,
) {
    val run = rememberRunner()
    val uri = LocalUriHandler.current
    var query by rememberSaveable { mutableStateOf("") }
    var unreadOnly by rememberSaveable { mutableStateOf(false) }
    var pick by remember { mutableStateOf<RssArticle?>(null) }
    var sendTo by remember { mutableStateOf<RssArticle?>(null) }
    val all = feeds.data ?: emptyList()
    val names = listOf("All feeds") + all.map { it.name }
    val selectedIdx = if (feedFilter == null) 0 else (all.indexOfFirst { it.key == feedFilter } + 1).coerceAtLeast(0)
    Column {
        Row(Modifier.padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(query, { query = it }, singleLine = true, placeholder = { Text("Search articles") }, leadingIcon = { Icon(Icons.Filled.Search, null) },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
            IconButton(onClick = { run("Refreshing feeds", { feeds.load(true) }) { rss.rssRefresh(all.firstOrNull { it.key == feedFilter }) } }) { Icon(Icons.Filled.Refresh, "Refresh") }
            if (client is QbitClient) IconButton(onClick = { run("Marked as read", { feeds.load(true) }) { rss.rssMarkRead(all.firstOrNull { it.key == feedFilter }, null) } }) { Icon(Icons.Filled.DoneAll, "Mark all read") }
        }
        ChipRow(names, selectedIdx, { i -> setFilter(if (i == 0) null else all.getOrNull(i - 1)?.key) }, Modifier.padding(vertical = 8.dp),
            counts = listOf(all.sumOf { f -> f.articles.count { !it.read } }) + all.map { f -> f.articles.count { !it.read } })
        if (client is QbitClient) Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Unread only", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Switch(unreadOnly, { unreadOnly = it })
        }
        LoadableContent(feeds, empty = { it.isEmpty() }, emptyText = "No RSS feeds yet.\nOpen the Feeds tab to add one.") { list ->
            val arts = list.filter { feedFilter == null || it.key == feedFilter }.flatMap { it.articles }
                .filter { query.isBlank() || it.title.contains(query.trim(), true) }
                .filter { !unreadOnly || !it.read }
                .sortedByDescending { it.date }
            if (arts.isEmpty()) EmptyBox("No articles match")
            else LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("${arts.size} articles", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(arts, key = { it.feedKey + "|" + it.id + it.title }) { a ->
                    val color = Rainbow[(list.indexOfFirst { it.key == a.feedKey }.coerceAtLeast(0)) % Rainbow.size]
                    Panel(Modifier.fillMaxWidth(), tint = color, onClick = { pick = a }) {
                        Row(Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (!a.read && client is QbitClient) { StatusDot(color, 8.dp); Spacer(Modifier.width(8.dp)) }
                            Column(Modifier.weight(1f)) {
                                Text(a.title, style = MaterialTheme.typography.bodyMedium, fontWeight = if (a.read) FontWeight.Normal else FontWeight.Bold,
                                    color = if (a.read) MaterialTheme.colorScheme.onSurfaceVariant else Color.White, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Pill(a.feedName, color)
                                    if (a.date.isNotEmpty()) Text(fmtDate(a.date, true), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { run("Sent to ${cfg.name}", { feeds.load(true) }) { rss.rssDownload(a); if (client is QbitClient) rss.rssMarkRead(null, a) } }) {
                                Icon(Icons.Filled.Download, "Download", tint = Good)
                            }
                        }
                    }
                }
            }
        }
    }

    pick?.let { a ->
        AlertDialog(
            onDismissRequest = { pick = null },
            title = { Text(a.title, style = MaterialTheme.typography.titleSmall, maxLines = 4, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    Text("${a.feedName} · ${fmtDate(a.date, true)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (a.description.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(android.text.Html.fromHtml(a.description, android.text.Html.FROM_HTML_MODE_COMPACT).toString().trim().take(1200), style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = { pick = null; sendTo = a }) { Text("Send to another download client…") }
                    if (a.page.startsWith("http")) TextButton(onClick = { uri.openUri(a.page) }) { Text("Open web page") }
                    if (client is QbitClient && !a.read) TextButton(onClick = { pick = null; run("Marked as read", { feeds.load(true) }) { rss.rssMarkRead(null, a) } }) { Text("Mark as read") }
                }
            },
            confirmButton = {
                TextButton(onClick = { pick = null; run("Sent to ${cfg.name}", { feeds.load(true) }) { rss.rssDownload(a); if (client is QbitClient) rss.rssMarkRead(null, a) } }) {
                    Text("Download", color = Good, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { pick = null }) { Text("Close") } },
        )
    }
    sendTo?.let { a -> SendLinkDialog(a.torrentUrl) { sendTo = null } }
}

// ------------------------------------------------------------ feeds
@Composable
private fun FeedsView(rss: RssClient, feeds: com.arrmada360.app.ui.Loadable<List<RssFeed>>, open: (RssFeed) -> Unit) {
    val run = rememberRunner()
    var adding by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<RssFeed?>(null) }
    val settings = rememberLoadable(rss, "rsssettings") { rss.rssSettings() }
    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = { FloatingActionButton(onClick = { adding = true }, containerColor = MaterialTheme.colorScheme.primary, shape = CircleShape) { Icon(Icons.Filled.Add, "Add feed") } },
    ) { pad ->
        LoadableContent(feeds, Modifier.padding(pad)) { list ->
            LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                settings.data?.let { (reader, auto) ->
                    item {
                        Panel(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("RSS reader", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("Fetch feeds in qBittorrent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(reader, { on -> run(if (on) "RSS reader on" else "RSS reader off", { settings.load(true) }) { rss.rssSetSettings(on, auto) } })
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Auto-download", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("Let your rules download matches automatically", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(auto, { on -> run(if (on) "Auto-download on" else "Auto-download off", { settings.load(true) }) { rss.rssSetSettings(reader, on) } })
                                }
                            }
                        }
                    }
                }
                if (list.isEmpty()) item { Text("No feeds yet – tap + to add your tracker's RSS link.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp)) }
                items(list, key = { it.key }) { f ->
                    val i = list.indexOf(f)
                    val c = Rainbow[i % Rainbow.size]
                    Panel(Modifier.fillMaxWidth(), tint = if (f.error) Bad else c, onClick = { open(f) }) {
                        Row(Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(c.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.RssFeed, null, tint = c)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(f.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(f.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Pill("${f.articles.size} articles", c)
                                    val unread = f.articles.count { !it.read }
                                    if (unread > 0 && rss is QbitClient) Pill("$unread unread", Good)
                                    if (f.error) Pill("Error", Bad)
                                    if (f.loading) Pill("Updating…", Warn)
                                }
                            }
                            IconButton(onClick = { run("Refreshing ${f.name}", { feeds.load(true) }) { rss.rssRefresh(f) } }) { Icon(Icons.Filled.Refresh, "Refresh") }
                            IconButton(onClick = { removing = f }) { Icon(Icons.Filled.Delete, "Remove", tint = Bad) }
                        }
                    }
                }
            }
        }
    }

    if (adding) {
        var url by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text("Add RSS feed") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(url, { url = it }, label = { Text("Feed URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(name, { name = it }, label = { Text("Name (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(enabled = url.isNotBlank(), onClick = {
                    val u = url.trim(); val n = name.trim().ifEmpty { android.net.Uri.parse(u).host ?: "Feed" }
                    adding = false
                    run("Feed added", { feeds.load(true) }) { rss.rssAddFeed(u, n) }
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("Cancel") } },
        )
    }
    removing?.let { f ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("Remove ${f.name}?") },
            confirmButton = { TextButton(onClick = { removing = null; run("Feed removed", { feeds.load(true) }) { rss.rssRemoveFeed(f) } }) { Text("Remove", color = Bad) } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Cancel") } },
        )
    }
}

// ------------------------------------------------------------ qBittorrent auto-download rules
@Composable
private fun RulesView(cfg: ServiceConfig, client: DownloadClient, rss: RssClient, feeds: List<RssFeed>) {
    val run = rememberRunner()
    val rules = rememberLoadable(cfg, "rssrules") { rss.rssRules() }
    val cats = rememberLoadable(cfg, "qbcats") { (client as? QbitClient)?.categories() ?: emptyList() }
    var editing by remember { mutableStateOf<RssRule?>(null) }
    var creating by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = { FloatingActionButton(onClick = { creating = true }, containerColor = MaterialTheme.colorScheme.primary, shape = CircleShape) { Icon(Icons.Filled.Add, "New rule") } },
    ) { pad ->
        LoadableContent(rules, Modifier.padding(pad), empty = { it.isEmpty() }, emptyText = "No download rules yet.\nTap + to auto-download matching torrents.") { list ->
            LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.name }) { r ->
                    Panel(Modifier.fillMaxWidth(), tint = if (r.enabled) Good else null, onClick = { editing = r }) {
                        Row(Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Rule, null, tint = if (r.enabled) Good else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(r.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                if (r.mustContain.isNotEmpty()) Text("Must contain: ${r.mustContain}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                if (r.mustNotContain.isNotEmpty()) Text("Must not contain: ${r.mustNotContain}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Pill("${r.feeds.size} feed${if (r.feeds.size == 1) "" else "s"}")
                                    if (r.category.isNotEmpty()) Pill(r.category, MaterialTheme.colorScheme.secondary)
                                    if (r.episodeFilter.isNotEmpty()) Pill(r.episodeFilter, Warn)
                                }
                            }
                            Switch(r.enabled, { on ->
                                run(if (on) "Rule enabled" else "Rule disabled", { rules.load(true) }) { rss.rssSaveRule(r.name, JSONObject(r.raw.toString()).put("enabled", on)) }
                            })
                        }
                    }
                }
            }
        }
    }
    if (creating || editing != null) {
        RuleEditor(rss, editing, feeds, cats.data ?: emptyList(), onDone = { creating = false; editing = null; rules.load(true) })
    }
}

@Composable
private fun RuleEditor(rss: RssClient, rule: RssRule?, feeds: List<RssFeed>, cats: List<String>, onDone: () -> Unit) {
    val run = rememberRunner()
    var name by remember { mutableStateOf(rule?.name ?: "") }
    var must by remember { mutableStateOf(rule?.mustContain ?: "") }
    var mustNot by remember { mutableStateOf(rule?.mustNotContain ?: "") }
    var regex by remember { mutableStateOf(rule?.useRegex ?: false) }
    var episode by remember { mutableStateOf(rule?.episodeFilter ?: "") }
    var category by remember { mutableStateOf(rule?.category ?: "") }
    var savePath by remember { mutableStateOf(rule?.savePath ?: "") }
    var enabled by remember { mutableStateOf(rule?.enabled ?: true) }
    var chosen by remember { mutableStateOf(rule?.feeds?.toSet() ?: feeds.map { it.url }.toSet()) }
    var matches by remember { mutableStateOf<List<String>?>(null) }
    ModalBottomSheet(onDismissRequest = onDone, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (rule == null) "New download rule" else "Edit rule", style = MaterialTheme.typography.titleLarge, color = Color.White)
            OutlinedTextField(name, { name = it }, label = { Text("Rule name") }, singleLine = true, enabled = rule == null, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(must, { must = it }, label = { Text("Must contain") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                supportingText = { Text(if (regex) "Regular expression" else "Words separated by spaces; use | for OR, * as wildcard") })
            OutlinedTextField(mustNot, { mustNot = it }, label = { Text("Must not contain") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(episode, { episode = it }, label = { Text("Episode filter (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("e.g. 1x01- for season 1 onwards") })
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Use regular expressions", modifier = Modifier.weight(1f)); Switch(regex, { regex = it }) }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Rule enabled", modifier = Modifier.weight(1f)); Switch(enabled, { enabled = it }) }
            if (cats.isNotEmpty()) Dropdown("Category", listOf("") + cats, category, { it.ifEmpty { "None" } }, { category = it })
            OutlinedTextField(savePath, { savePath = it }, label = { Text("Save path (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("Apply to feeds", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            feeds.forEach { f ->
                Row(Modifier.fillMaxWidth().clickable { chosen = if (f.url in chosen) chosen - f.url else chosen + f.url }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(f.url in chosen, { on -> chosen = if (on) chosen + f.url else chosen - f.url })
                    Text(f.name)
                }
            }
            matches?.let { m ->
                Panel(Modifier.fillMaxWidth(), tint = if (m.isEmpty()) Warn else Good) {
                    Column(Modifier.padding(12.dp)) {
                        Text(if (m.isEmpty()) "Nothing in your feeds matches yet" else "${m.size} matching articles", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        m.take(15).forEach { Text("• $it", style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                    }
                }
            }
            fun def(): JSONObject {
                val o = JSONObject(rule?.raw?.toString() ?: "{}")
                o.put("enabled", enabled).put("mustContain", must).put("mustNotContain", mustNot).put("useRegex", regex)
                    .put("episodeFilter", episode).put("affectedFeeds", JSONArray(chosen.toList()))
                    .put("assignedCategory", category).put("savePath", savePath)
                return o
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    val n = name.trim()
                    run(null) { rss.rssSaveRule(n, def()); matches = rss.rssRuleMatches(n) }
                }) { Text("Save & preview matches") }
                if (rule != null) TextButton(onClick = { run("Rule removed", onDone) { rss.rssRemoveRule(rule.name) } }) { Text("Delete", color = Bad) }
            }
            GradientButton("Save rule", Modifier.fillMaxWidth(), enabled = name.isNotBlank()) {
                val n = name.trim()
                run("Rule saved", onDone) { rss.rssSaveRule(n, def()) }
            }
        }
    }
}

@Suppress("unused")
private val keepEdit = Icons.Filled.Edit
@Suppress("unused")
private val keepFill = Modifier.fillMaxSize()
