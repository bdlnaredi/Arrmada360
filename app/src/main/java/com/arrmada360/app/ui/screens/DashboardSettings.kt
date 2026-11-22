package com.arrmada360.app.ui.screens

import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip

import android.content.Intent
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.arrmada360.app.ui.BottomTab
import com.arrmada360.app.ui.ServiceTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.arrmada360.app.api.Conn
import com.arrmada360.app.api.Connections
import com.arrmada360.app.api.SpeedHistory
import com.arrmada360.app.ui.Accent
import com.arrmada360.app.ui.Accent2
import com.arrmada360.app.ui.GradientButton
import com.arrmada360.app.ui.Pink
import com.arrmada360.app.ui.PulseDot
import com.arrmada360.app.ui.Rainbow
import com.arrmada360.app.ui.Sparkline
import com.arrmada360.app.ui.StatTile
import com.arrmada360.app.ui.animatedLong
import com.arrmada360.app.ui.shimmer
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arrmada360.app.api.ArrApi
import com.arrmada360.app.api.CalEntry
import com.arrmada360.app.api.DlSnapshot
import com.arrmada360.app.api.DownloadClients
import com.arrmada360.app.api.SeerrApi
import com.arrmada360.app.api.TautulliApi
import com.arrmada360.app.api.UnraidApi
import com.arrmada360.app.data.ConfigStore
import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.data.ServiceType
import com.arrmada360.app.net.int
import com.arrmada360.app.ui.AppScaffold
import com.arrmada360.app.ui.Bad
import com.arrmada360.app.ui.Good
import com.arrmada360.app.ui.Navigator
import com.arrmada360.app.ui.Panel
import com.arrmada360.app.ui.Pill
import com.arrmada360.app.ui.Poster
import com.arrmada360.app.ui.ProgressBar
import com.arrmada360.app.ui.Route
import com.arrmada360.app.ui.SectionTitle
import com.arrmada360.app.ui.ServiceBadge
import com.arrmada360.app.ui.Warn
import com.arrmada360.app.ui.copyText
import com.arrmada360.app.ui.fmtDate
import com.arrmada360.app.ui.humanBytes
import com.arrmada360.app.ui.humanSpeed
import com.arrmada360.app.ui.parseDate
import com.arrmada360.app.ui.relativeDay
import com.arrmada360.app.ui.rememberLoadable
import com.arrmada360.app.ui.toast
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

// ============================================================ Dashboard
@Composable
fun DashboardScreen(nav: Navigator) {
    val services = ConfigStore.services.toList()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var pick by rememberSaveable { mutableIntStateOf(0) }
    val requested = com.arrmada360.app.ui.DashboardNav.tab
    androidx.compose.runtime.LaunchedEffect(requested) {
        if (requested != null) { tab = requested.coerceIn(0, 3); pick = 0; com.arrmada360.app.ui.DashboardNav.tab = null }
    }
    var query by rememberSaveable { mutableStateOf("") }
    var live by rememberSaveable { mutableStateOf("") }
    androidx.compose.runtime.LaunchedEffect(query) {
        kotlinx.coroutines.delay(450) // search automatically once you stop typing
        live = query.trim()
    }
    val searching = live.length >= 2
    val radarrs = services.filter { it.type == ServiceType.RADARR }
    val sonarrs = services.filter { it.type == ServiceType.SONARR }
    val instances = when (tab) { 0 -> radarrs; 1 -> sonarrs; else -> emptyList() }
    AppScaffold(
        title = when (tab) { 0 -> "Movies"; 1 -> "TV Shows"; 2 -> "Calendar"; else -> "Server" },
        subtitle = instances.getOrNull(pick)?.name ?: greeting(),
        isRoot = true, onBack = {},
        tabs = if (instances.size > 1 && !searching) instances.map { it.name } else emptyList(), selectedTab = pick, onTab = { pick = it },
        titleContent = { DashboardSearchBar(query, { query = it }) },
        bottomTabs = listOf(
            BottomTab("Movies", Icons.Filled.Movie),
            BottomTab("TV Shows", Icons.Filled.Tv),
            BottomTab("Calendar", Icons.Filled.CalendarMonth),
        ) + if (com.arrmada360.app.data.Family.active && !com.arrmada360.app.data.Family.showServerTab) emptyList() else listOf(BottomTab("Server", Icons.Filled.Dns)),
        selectedBottom = tab, onBottom = { tab = it; pick = 0 },
        fab = {
            val target = if (searching) null else instances.getOrNull(pick)
            if (target != null && !com.arrmada360.app.data.Family.active) androidx.compose.material3.FloatingActionButton(
                onClick = { nav.push(Route.ArrAdd(target.id)) }, containerColor = target.type.tint, shape = androidx.compose.foundation.shape.CircleShape,
            ) { Icon(Icons.Filled.Add, "Add") }
        },
    ) { pad ->
        Box(Modifier.padding(pad)) {
            if (searching) { com.arrmada360.app.ui.screens.SearchResults(live, nav); return@Box }
            when (tab) {
                0, 1 -> {
                    val c = instances.getOrNull(pick.coerceAtMost((instances.size - 1).coerceAtLeast(0)))
                    if (c != null) ServiceTheme(c.type, ConfigStore.amoled) { DiscoverTab(tab == 0, c, nav) }
                    else DiscoverTab(tab == 0, null, nav)
                }
                2 -> CombinedCalendar(services.filter { it.type.isArr && it.type != ServiceType.WHISPARR }, nav)
                else -> ServerInfoTab(services, nav)
            }
        }
    }
}

private fun greeting(): String {
    val h = java.time.LocalTime.now().hour
    return when (h) { in 5..11 -> "Good morning"; in 12..16 -> "Good afternoon"; in 17..21 -> "Good evening"; else -> "Up late?" } + " 👋"
}

@Composable
private fun HeroCard(services: List<ServiceConfig>, nav: Navigator) {
    val down = SpeedHistory.totalDown()
    val up = SpeedHistory.totalUp()
    val dl = animatedLong(down.lastOrNull() ?: 0L)
    val ul = animatedLong(up.lastOrNull() ?: 0L)
    val online = services.count { Connections.of(it.id) is Conn.Ok }
    val failing = services.count { Connections.of(it.id) is Conn.Fail }
    val active = SpeedHistory.active.values.sum()
    Box(
        Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Accent.copy(alpha = 0.55f), Accent2.copy(alpha = 0.50f), Pink.copy(alpha = 0.40f))))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(22.dp)),
    ) {
        if (down.size > 1) Sparkline(down, Color.White, Modifier.matchParentSize().padding(top = 60.dp), fill = true)
        Column(Modifier.padding(18.dp)) {
            Text("Total download speed", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(humanSpeed(dl), style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Spacer(Modifier.width(10.dp))
                Icon(Icons.Filled.ArrowUpward, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp).padding(bottom = 2.dp))
                Text(humanSpeed(ul), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.padding(bottom = 4.dp))
            }
            Spacer(Modifier.height(30.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroStat("Online", "$online/${services.size}", Modifier.weight(1f)) { nav.push(Route.Health) }
                HeroStat("Downloading", active.toString(), Modifier.weight(1f)) {
                    ConfigStore.downloaders().firstOrNull()?.let { nav.root(Route.Service(it.id)) }
                }
                HeroStat(if (failing > 0) "Need attention" else "Problems", failing.toString(), Modifier.weight(1f)) { nav.push(Route.Health) }
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = 0.22f)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f), maxLines = 1)
    }
}

@Composable
private fun AppTile(c: ServiceConfig, modifier: Modifier, onClick: () -> Unit, onLong: () -> Unit) {
    val st = Connections.of(c.id)
    Panel(modifier.height(118.dp), tint = c.type.tint, onClick = onClick, onLongClick = onLong) {
        Box(Modifier.fillMaxWidth().height(118.dp).background(Brush.linearGradient(listOf(c.type.tint.copy(alpha = 0.22f), c.type.tint2.copy(alpha = 0.06f))))) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    ServiceBadge(c.type, 46.dp, c.name)
                    Spacer(Modifier.weight(1f))
                    when (st) {
                        is Conn.Ok -> PulseDot(Good, size = 8.dp)
                        is Conn.Fail -> PulseDot(Bad, pulsing = false, size = 8.dp)
                        is Conn.Checking -> CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = c.type.tint)
                        else -> {}
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(c.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    when (st) { is Conn.Fail -> "Offline – tap & hold to fix"; is Conn.Ok -> c.type.blurb; else -> c.type.blurb },
                    style = MaterialTheme.typography.labelSmall, color = if (st is Conn.Fail) Bad else c.type.tint, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DownloaderCard(c: ServiceConfig, onClick: () -> Unit) {
    val client = remember(c) { DownloadClients.of(c) }
    val l = rememberLoadable(c, "dash", autoRefreshMs = 3000) { client?.let { DownloadClients.snapshot(it) } ?: DlSnapshot(emptyList(), 0, 0) }
    val hist = SpeedHistory.down[c.id] ?: emptyList()
    Panel(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp), tint = c.type.tint, onClick = onClick) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ServiceBadge(c.type, 36.dp, c.name)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.name, style = MaterialTheme.typography.titleSmall)
                    val s = l.data
                    if (s != null) {
                        val a = s.items.count { !it.done && !it.paused }
                        Text("$a active · ${s.items.size} total" + if (s.globalPaused) " · paused" else "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (l.error != null) Text(l.error ?: "", style = MaterialTheme.typography.labelSmall, color = Bad, maxLines = 1)
                }
                val s = l.data
                if (s != null) Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ArrowDownward, null, Modifier.size(14.dp), tint = c.type.tint)
                        Text(humanSpeed(animatedLong(s.dlSpeed)), style = MaterialTheme.typography.titleSmall, color = c.type.tint)
                    }
                    if (client?.isUsenet != true) Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ArrowUpward, null, Modifier.size(12.dp), tint = Good)
                        Text(humanSpeed(animatedLong(s.upSpeed)), style = MaterialTheme.typography.labelSmall, color = Good)
                    }
                }
            }
            if (hist.size > 1) {
                Spacer(Modifier.height(8.dp))
                Sparkline(hist, c.type.tint, Modifier.fillMaxWidth().height(42.dp))
            }
            val s = l.data
            if (s != null) s.items.filter { !it.done && !it.paused }.sortedByDescending { it.dlSpeed }.take(3).forEach { d ->
                Spacer(Modifier.height(8.dp))
                Row {
                    Text(d.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(" ${(d.progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = c.type.tint)
                }
                Spacer(Modifier.height(3.dp))
                ProgressBar(d.progress, color = c.type.tint, height = 4.dp)
            }
        }
    }
}

@Composable
private fun UpcomingRow(arrs: List<ServiceConfig>, nav: Navigator) {
    val l = rememberLoadable(arrs.map { it.id }, "upcoming") {
        coroutineScope {
            arrs.map { c -> async { try { ArrApi(c).calendar(0, 14).map { c to it } } catch (e: Exception) { emptyList<Pair<ServiceConfig, CalEntry>>() } } }
                .awaitAll().flatten().sortedBy { it.second.date }.take(20)
        }
    }
    val list = l.data ?: return
    if (list.isEmpty()) return
    Column {
        SectionTitle("Coming up")
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(list) { (c, e) ->
                Panel(Modifier.width(128.dp), tint = c.type.tint, onClick = { nav.push(Route.ArrDetail(c.id, e.parentId)) }) {
                    Column {
                        Box {
                            Poster(e.poster, Modifier.fillMaxWidth(), corner = 0.dp, fallback = e.title)
                            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))))
                            Box(Modifier.align(Alignment.TopStart).padding(6.dp)) { Pill(relativeDay(parseDate(e.date)), c.type.tint, solid = true) }
                            Box(Modifier.align(Alignment.TopEnd).padding(6.dp)) { ServiceBadge(c.type, 22.dp, c.name) }
                            Column(Modifier.align(Alignment.BottomStart).padding(8.dp)) {
                                Text(e.title, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(e.subtitle, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlanceCard(c: ServiceConfig, onClick: () -> Unit) {
    val l = rememberLoadable(c, "glance", autoRefreshMs = 15000) {
        when (c.type) {
            ServiceType.SEERR -> {
                val n = SeerrApi(c).counts()
                listOf("Pending" to n.int("pending").toString(), "Processing" to n.int("processing").toString(), "Available" to n.int("available").toString())
            }
            ServiceType.TAUTULLI -> {
                val (s, bw) = TautulliApi(c).activity()
                listOf("Streams" to s.size.toString(), "Bandwidth" to "%.1f Mbps".format(bw / 1000.0), "Transcoding" to s.count { it.decision.contains("Transcode", true) }.toString())
            }
            else -> {
                val a = UnraidApi(c).array()
                val pct = if (a.total > 0) (a.used * 100 / a.total) else 0
                listOf("Array" to a.state.lowercase().replaceFirstChar { it.uppercase() }, "Used" to "$pct%", "Free" to humanBytes(a.total - a.used))
            }
        }
    }
    Panel(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp), tint = c.type.tint, onClick = onClick) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ServiceBadge(c.type, 36.dp, c.name)
                Spacer(Modifier.width(10.dp))
                Text(c.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, null, tint = c.type.tint)
            }
            Spacer(Modifier.height(10.dp))
            val d = l.data
            when {
                d != null -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    d.forEachIndexed { i, (label, value) ->
                        StatTile(label, value, if (i == 0) c.type.tint else if (i == 1) c.type.tint2 else Rainbow[(i + 2) % Rainbow.size], Modifier.weight(1f))
                    }
                }
                l.error != null -> Text("Offline – ${l.error}", style = MaterialTheme.typography.bodySmall, color = Warn, maxLines = 2)
                else -> Box(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).shimmer())
            }
        }
    }
}

@Composable
private fun CombinedCalendar(arrs: List<ServiceConfig>, nav: Navigator) {
    if (arrs.isEmpty()) { com.arrmada360.app.ui.EmptyBox("Add Sonarr, Radarr, Lidarr or Readarr to fill your calendar"); return }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val l = rememberLoadable(arrs.map { it.id }, "combinedcal") {
        coroutineScope {
            arrs.map { c -> async { try { ArrApi(c).calendar(1, 30).map { c to it } } catch (e: Exception) { emptyList<Pair<ServiceConfig, CalEntry>>() } } }
                .awaitAll().flatten().sortedBy { it.second.date }
        }
    }
    val names = listOf("All") + arrs.map { it.name }
    Column {
        com.arrmada360.app.ui.ChipRow(names, filter, { filter = it }, Modifier.padding(vertical = 8.dp))
        com.arrmada360.app.ui.LoadableContent(l, empty = { it.isEmpty() }, emptyText = "Nothing coming up in the next 30 days") { all ->
            val list = if (filter == 0) all else all.filter { it.first.id == arrs[filter - 1].id }
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                list.groupBy { parseDate(it.second.date) }.forEach { (day, entries) ->
                    item(key = "d$day") { SectionTitle(relativeDay(day)) }
                    items(entries, key = { it.first.id + it.second.id + it.second.date }) { (c, e) ->
                        Panel(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), tint = c.type.tint, onClick = { nav.push(Route.ArrDetail(c.id, e.parentId)) }) {
                            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Poster(e.poster, Modifier.width(50.dp), corner = 8.dp, fallback = e.title)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(e.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(e.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.height(3.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Pill(fmtDate(e.date, withTime = e.date.length > 10).ifEmpty { "TBA" }, c.type.tint)
                                        if (e.hasFile) Pill("✓ Downloaded", Good)
                                    }
                                }
                                ServiceBadge(c.type, 30.dp, c.name)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================ Server tab (nzb360 style)
@Composable
private fun BigHeader(title: String, color: Color) {
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 12.dp)) {
        Box(Modifier.width(72.dp).height(4.dp).clip(RoundedCornerShape(50)).background(color))
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun InfoCard(
    tint: Color,
    label: String,
    title: String,
    icon: @Composable () -> Unit,
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit = {},
) {
    Column(
        Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.30f), MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) { icon() }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = tint, fontWeight = FontWeight.SemiBold)
                Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (onClick != null) Icon(Icons.Filled.ChevronRight, null, tint = Color.White, modifier = Modifier.size(30.dp))
        }
        content()
    }
}

@Composable
private fun PlainIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color = Color.White) = Icon(icon, null, tint = tint, modifier = Modifier.size(34.dp))

@Composable
fun ServerInfoTab(services: List<ServiceConfig>, nav: Navigator) {
    if (services.isEmpty()) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(60.dp))
            Text("No apps yet", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(16.dp))
            GradientButton("Add an app") { nav.push(Route.AddService) }
        }
        return
    }
    val arrs = services.filter { it.type.isArr && it.type != ServiceType.WHISPARR }
    val unraids = services.filter { it.type == ServiceType.UNRAID }
    val downloaders = services.filter { it.type.isDownloader }
    LazyColumn(contentPadding = PaddingValues(bottom = 40.dp)) {
        item { BigHeader("Server Info", Good) }
        item(key = "year") {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Accent.copy(alpha = 0.7f), Accent2.copy(alpha = 0.7f), Pink.copy(alpha = 0.6f))))
                    .clickable { nav.push(Route.YearReview) }.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Your ${java.time.LocalDate.now().year} in review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Downloads per month, data grabbed, top genres and Plex stats", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White)
            }
        }
        if (downloaders.isNotEmpty()) item(key = "dltools") {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val sched = com.arrmada360.app.notify.SpeedSchedule
                Panel(Modifier.weight(1f), tint = Warn, onClick = { nav.push(Route.SpeedScheduler) }) {
                    Column(Modifier.padding(14.dp)) {
                        Icon(Icons.Filled.Schedule, null, tint = Warn)
                        Text("Speed scheduler", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(if (sched.enabled) "${sched.fmt(sched.startMin)}–${sched.fmt(sched.endMin)} limited" else "Off", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Panel(Modifier.weight(1f), tint = Good, onClick = { nav.push(Route.SeedingManager) }) {
                    Column(Modifier.padding(14.dp)) {
                        Icon(Icons.Filled.CloudUpload, null, tint = Good)
                        Text("Seeding manager", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(if (com.arrmada360.app.notify.SeedRules.autoClean) "Auto-clean on" else "Ratio & seed time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item(key = "disk") { DiskSpaceCard(arrs, nav) }
        item(key = "dlhist") { DownloadHistoryCard(arrs, downloaders) }
        item(key = "health") { ArrHealthCard(services.filter { it.type.isArr || it.type == ServiceType.PROWLARR }, nav) }
        if (unraids.isNotEmpty()) {
            item { BigHeader("Unraid", ServiceType.UNRAID.tint) }
            items(unraids, key = { "un" + it.id }) { u -> UnraidSummaryCard(u) { nav.root(Route.Service(u.id)) } }
        }
        if (downloaders.isNotEmpty()) {
            item { BigHeader("Live Downloads", Accent) }
            items(downloaders, key = { "dl" + it.id }) { c -> DownloaderCard(c) { nav.root(Route.Service(c.id)) } }
        }
        item { BigHeader("Other Services", Pink) }
        item(key = "books") { RecentCard(ServiceType.READARR, "Recently Downloaded Books", nav) }
        item(key = "albums") { RecentCard(ServiceType.LIDARR, "Recently Downloaded Albums", nav) }
        item { BigHeader("External Server Use", Warn) }
        item(key = "taut") { TautulliCard(nav) }
        item(key = "seerr") { SeerrCard(nav) }
        item { BigHeader("Your Apps", Accent2) }
        val rows = services.chunked(2)
        items(rows.size, key = { "row$it" }) { i ->
            Row(Modifier.padding(horizontal = 12.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rows[i].forEach { c -> AppTile(c, Modifier.weight(1f), onClick = { nav.root(Route.Service(c.id)) }, onLong = { nav.push(Route.EditService(c.id, null)) }) }
                if (rows[i].size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DiskSpaceCard(arrs: List<ServiceConfig>, nav: Navigator) {
    val l = rememberLoadable(arrs.map { it.id }, "srvdisk") {
        coroutineScope {
            arrs.map { c -> async { try { ArrApi(c).diskSpace() } catch (e: Exception) { emptyList() } } }.awaitAll().flatten()
                .filter { it.total > 0 }.distinctBy { it.path }.sortedBy { it.path }
        }
    }
    InfoCard(Good, "Server", "Disk Space", { PlainIcon(Icons.Filled.Storage) }) {
        val d = l.data
        when {
            arrs.isEmpty() -> Text("Add Sonarr or Radarr to see disk space", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
            d == null -> Box(Modifier.padding(top = 14.dp).fillMaxWidth().height(80.dp).clip(RoundedCornerShape(12.dp)).shimmer())
            d.isEmpty() -> Text("No disks reported", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
            else -> d.forEach { s ->
                val used = 1f - s.free.toFloat() / s.total
                Spacer(Modifier.height(16.dp))
                Text(s.path, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressBar(used, color = if (used > 0.9f) Bad else Good, modifier = Modifier.weight(1f), height = 7.dp)
                    Spacer(Modifier.width(14.dp))
                    Text("${humanBytes(s.free)} free", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(100.dp))
                }
            }
        }
    }
}

@Composable
private fun DownloadHistoryCard(arrs: List<ServiceConfig>, downloaders: List<ServiceConfig>) {
    val l = rememberLoadable(arrs.map { it.id } + downloaders.map { it.id }, "srvhist") {
        val days = (13 downTo 0).map { java.time.LocalDate.now().minusDays(it.toLong()) }
        val totals = HashMap<java.time.LocalDate, Long>()
        coroutineScope {
            val a = arrs.map { c -> async { try { ArrApi(c).grabbedPerDay(14) } catch (e: Exception) { emptyMap() } } }
            val u = downloaders.map { c ->
                async {
                    try {
                        val client = DownloadClients.of(c)
                        if (client == null || !client.hasHistory) emptyMap() else client.history().filter { !it.failed && it.time > 0 }
                            .groupBy { java.time.Instant.ofEpochSecond(it.time).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
                            .mapValues { e -> e.value.sumOf { it.size } }
                    } catch (e: Exception) { emptyMap() }
                }
            }
            (a.awaitAll() + u.awaitAll()).forEach { m -> m.forEach { (d, b) -> totals[d] = (totals[d] ?: 0L) + b } }
        }
        days.map { it to (totals[it] ?: 0L) }
    }
    val data = l.data
    val total = data?.sumOf { it.second } ?: 0L
    InfoCard(Warn, "Download History", if (data == null) "Loading…" else "${humanBytes(total).replace("0 B", "0 KB")} in last 2 weeks", { PlainIcon(Icons.Filled.Download) }) {
        Spacer(Modifier.height(16.dp))
        if (data == null) Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)).shimmer())
        else BarChart(data.map { it.second }, data.first().first, data.last().first)
    }
}

@Composable
private fun BarChart(values: List<Long>, first: java.time.LocalDate, last: java.time.LocalDate) {
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    val grid = MaterialTheme.colorScheme.outline
    val c1 = Warn
    val c2 = Pink
    Row(Modifier.fillMaxWidth().height(170.dp)) {
        Column(Modifier.fillMaxHeight().width(64.dp), verticalArrangement = Arrangement.SpaceBetween) {
            (4 downTo 0).forEach { i -> Text(if (values.all { it == 0L }) "0 GB" else humanBytes(max * i / 4), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface) }
        }
        androidx.compose.foundation.Canvas(Modifier.weight(1f).fillMaxHeight()) {
            val w = size.width
            val h = size.height
            for (i in 0..4) drawLine(grid, androidx.compose.ui.geometry.Offset(w * i / 4f, 0f), androidx.compose.ui.geometry.Offset(w * i / 4f, h), strokeWidth = 1.5f)
            drawLine(grid, androidx.compose.ui.geometry.Offset(0f, h), androidx.compose.ui.geometry.Offset(w, h), strokeWidth = 1.5f)
            val bw = w / values.size
            values.forEachIndexed { i, v ->
                if (v > 0) {
                    val bh = h * v / max.toFloat()
                    drawRoundRect(
                        Brush.verticalGradient(listOf(c1, c2), startY = h - bh, endY = h),
                        topLeft = androidx.compose.ui.geometry.Offset(i * bw + bw * 0.18f, h - bh),
                        size = androidx.compose.ui.geometry.Size(bw * 0.64f, bh),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                    )
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(start = 64.dp, top = 6.dp)) {
        Text(fmtDate(first.toString()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(fmtDate(last.toString()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ArrHealthCard(apps: List<ServiceConfig>, nav: Navigator) {
    val l = rememberLoadable(apps.map { it.id }, "srvhealth") {
        coroutineScope {
            apps.map { c -> async { try { ArrApi(c).healthIssues().map { c to it } } catch (e: Exception) { emptyList() } } }.awaitAll().flatten()
        }
    }
    val d = l.data
    InfoCard(
        Bad, "From the *arrs",
        when { apps.isEmpty() -> "No *arr apps added"; d == null -> "Checking…"; d.isEmpty() -> "No Server Issues"; else -> "${d.size} Server Issue${if (d.size == 1) "" else "s"}" },
        { PlainIcon(Icons.Filled.Info) },
    ) {
        d?.take(8)?.forEach { (c, h) ->
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().clickable { nav.root(Route.Service(c.id)) }, verticalAlignment = Alignment.Top) {
                ServiceBadge(c.type, 28.dp, c.name)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.name + " · " + h.type.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall, color = if (h.type.equals("error", true)) Bad else Warn, fontWeight = FontWeight.Bold)
                    Text(h.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun UnraidSummaryCard(u: ServiceConfig, onClick: () -> Unit) {
    val api = remember(u) { UnraidApi(u) }
    val l = rememberLoadable(u, "srvunraid", autoRefreshMs = 15000) {
        Triple(
            try { api.system() } catch (e: Exception) { null },
            try { api.array() } catch (e: Exception) { null },
            try { api.containers() } catch (e: Exception) { null },
        )
    }
    val d = l.data
    InfoCard(u.type.tint, "From ${u.name}", d?.first?.hostname?.ifEmpty { null } ?: u.name, { ServiceBadge(u.type, 40.dp, u.name) }, onClick) {
        if (d == null) { Spacer(Modifier.height(12.dp)); Box(Modifier.fillMaxWidth().height(70.dp).clip(RoundedCornerShape(12.dp)).shimmer()); return@InfoCard }
        val (sys, arr, cont) = d
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (arr != null) StatTile("Array", arr.state.lowercase().replaceFirstChar { it.uppercase() }, if (arr.state.equals("STARTED", true)) Good else Warn, Modifier.weight(1f))
            if (arr != null && arr.total > 0) StatTile("Used", "${arr.used * 100 / arr.total}%", u.type.tint, Modifier.weight(1f))
            if (cont != null) StatTile("Docker", "${cont.count { it.state.equals("RUNNING", true) }}/${cont.size}", Accent, Modifier.weight(1f))
        }
        if (sys != null && (sys.cpuPercent != null || sys.memTotal > 0)) {
            Spacer(Modifier.height(10.dp))
            sys.cpuPercent?.let { c -> Text("CPU ${"%.0f".format(c)}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface); ProgressBar((c / 100).toFloat(), color = u.type.tint) }
            if (sys.memTotal > 0) { Spacer(Modifier.height(6.dp)); Text("RAM ${humanBytes(sys.memUsed)} / ${humanBytes(sys.memTotal)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface); ProgressBar(sys.memUsed.toFloat() / sys.memTotal, color = Accent2) }
        }
    }
}

@Composable
private fun RecentCard(type: ServiceType, title: String, nav: Navigator) {
    val c = ConfigStore.services.firstOrNull { it.type == type }
    val l = rememberLoadable(c, "srvrecent") { c?.let { ArrApi(it).recentByHistory() } ?: emptyList() }
    InfoCard(type.tint, "From ${c?.name ?: type.label}", title, { ServiceBadge(type, 40.dp, c?.name) }, if (c != null) ({ nav.root(Route.Service(c.id)) }) else null) {
        Spacer(Modifier.height(16.dp))
        val d = l.data
        if (c == null || (d != null && d.isEmpty())) {
            Box(Modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    repeat(3) { i -> Box(Modifier.weight(1f).height(190.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.08f - i * 0.02f))) }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (c == null) "Enable ${type.label} to Populate" else "Nothing downloaded yet", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.75f))
                    if (c == null) {
                        Spacer(Modifier.height(10.dp))
                        androidx.compose.material3.Button(
                            onClick = { nav.push(Route.EditService(null, type)) },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(50),
                        ) { Text("Go to Settings", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        } else if (d == null) {
            Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(16.dp)).shimmer())
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(d, key = { it.id }) { e ->
                    Column(Modifier.width(118.dp).clickable { nav.push(Route.ArrDetail(c.id, e.parentId)) }) {
                        Poster(e.poster, Modifier.fillMaxWidth(), ratio = if (type == ServiceType.LIDARR) 1f else 2f / 3f, corner = 14.dp, fallback = e.title)
                        Spacer(Modifier.height(5.dp))
                        Text(e.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(e.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun TautulliCard(nav: Navigator) {
    val c = ConfigStore.services.firstOrNull { it.type == ServiceType.TAUTULLI }
    val l = rememberLoadable(c, "srvtaut", autoRefreshMs = 15000) { c?.let { TautulliApi(it).activity() } }
    val d = l.data
    val n = d?.first?.size ?: 0
    InfoCard(
        ServiceType.TAUTULLI.tint, "From ${c?.name ?: "Tautulli"}",
        when { c == null -> "Enable Tautulli to see streams"; d == null && l.error != null -> "Offline"; d == null -> "Checking…"; n == 0 -> "No Active Streams"; else -> "$n Active Stream${if (n == 1) "" else "s"}" },
        { ServiceBadge(ServiceType.TAUTULLI, 40.dp, c?.name) },
        { if (c != null) nav.root(Route.Service(c.id)) else nav.push(Route.EditService(null, ServiceType.TAUTULLI)) },
    ) {
        d?.first?.take(5)?.forEach { s ->
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Poster(s.thumb, Modifier.width(36.dp), corner = 6.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${s.user} · ${s.decision} · ${s.progress}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun SeerrCard(nav: Navigator) {
    val c = ConfigStore.services.firstOrNull { it.type == ServiceType.SEERR }
    val l = rememberLoadable(c, "srvseerr", autoRefreshMs = 30000) { c?.let { SeerrApi(it).counts() } }
    val d = l.data
    val pending = d?.int("pending") ?: 0
    InfoCard(
        ServiceType.SEERR.tint, "From ${c?.name ?: "Seerr"}",
        when { c == null -> "Enable Seerr to see requests"; d == null && l.error != null -> "Offline"; d == null -> "Checking…"; pending == 0 -> "No Pending Requests"; else -> "$pending Pending Request${if (pending == 1) "" else "s"}" },
        { ServiceBadge(ServiceType.SEERR, 40.dp, c?.name) },
        { if (c != null) nav.root(Route.Service(c.id)) else nav.push(Route.EditService(null, ServiceType.SEERR)) },
    ) {
        if (d != null) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Pending", pending.toString(), Warn, Modifier.weight(1f))
                StatTile("Processing", d.int("processing").toString(), ServiceType.SEERR.tint, Modifier.weight(1f))
                StatTile("Available", d.int("available").toString(), Good, Modifier.weight(1f))
            }
        }
    }
}

/** nzb360-style pill search bar in the dashboard's top bar. Searches automatically as you type. */
@Composable
private fun DashboardSearchBar(value: String, onChange: (String) -> Unit) {
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val voice = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { r ->
        val said = r.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: return@rememberLauncherForActivityResult
        // "add dune part two" / "search for dune" → "dune part two"
        onChange(said.replace(Regex("^(please )?(add|search( for)?|find|download|get)\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+(to|in|on) (radarr|sonarr|my library)$", RegexOption.IGNORE_CASE), "").trim())
    }
    val pickClip = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { u ->
        if (u != null) com.arrmada360.app.data.ShareInbox.clip = u.toString() to ctx.contentResolver.getType(u)
    }
    Row(
        Modifier.padding(end = 12.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text("Search movies, TV…", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
            androidx.compose.foundation.text.BasicTextField(
                value, onChange, singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { focus.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) IconButton(onClick = { onChange(""); focus.clearFocus() }, modifier = Modifier.size(32.dp)) {
            Icon(androidx.compose.material.icons.Icons.Filled.Close, "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            IconButton(onClick = { pickClip.launch("video/*") }, modifier = Modifier.size(34.dp)) {
                Icon(androidx.compose.material.icons.Icons.Filled.ImageSearch, "Identify a clip", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = {
                val i = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    .putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Say a movie or show, e.g. \"add Dune Part Two\"")
                try { voice.launch(i) } catch (e: Exception) { android.widget.Toast.makeText(ctx, "Voice search isn't available on this device", android.widget.Toast.LENGTH_SHORT).show() }
            }, modifier = Modifier.size(34.dp)) {
                Icon(androidx.compose.material.icons.Icons.Filled.Mic, "Voice search", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
