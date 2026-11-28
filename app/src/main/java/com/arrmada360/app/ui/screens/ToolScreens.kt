package com.arrmada360.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arrmada360.app.api.KumaApi
import com.arrmada360.app.api.KumaMonitor
import com.arrmada360.app.api.StashApi
import com.arrmada360.app.api.StashScene
import com.arrmada360.app.api.sceneDetail
import com.arrmada360.app.api.setRating
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import com.arrmada360.app.api.Whisparr
import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.data.display
import com.arrmada360.app.ui.*

// ============================================================ Whisparr gate
/** Works out once whether a Whisparr instance is v2 (Sonarr-based) or v3 (Radarr-based), then shows the normal *arr screens. */
@Composable
fun WhisparrGate(cfg: ServiceConfig, content: @Composable () -> Unit) {
    var ready by remember(cfg.id) { mutableStateOf(Whisparr.known(cfg)) }
    var err by remember(cfg.id) { mutableStateOf<String?>(null) }
    var tries by remember { mutableIntStateOf(0) }
    if (ready) { content(); return }
    LaunchedEffect(cfg.id, tries) {
        err = null
        try { com.arrmada360.app.api.ArrApi(cfg).status(); ready = true } catch (e: Exception) { err = com.arrmada360.app.net.friendlyError(e) }
    }
    AppBackground {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (err == null) CircularProgressIndicator()
            else ErrorBox(err ?: "") { tries++ }
        }
    }
}

// ============================================================ Uptime Kuma
private val KumaGreen = Color(0xFF5CDD8B)

private fun kumaColor(s: Int) = when (s) { 1 -> KumaGreen; 0 -> Bad; 2 -> Warn; 3 -> Accent; else -> Color.Gray }
private fun kumaLabel(s: Int) = when (s) { 1 -> "Up"; 0 -> "Down"; 2 -> "Pending"; 3 -> "Maintenance"; else -> "Unknown" }

@Composable
fun KumaScreen(cfg: ServiceConfig, nav: Navigator, isRoot: Boolean, back: () -> Unit) {
    val api = remember(cfg) { KumaApi(cfg) }
    var tab by rememberSaveable(cfg.id) { mutableIntStateOf(0) }
    var slugVer by remember { mutableIntStateOf(0) }
    AppScaffold(
        title = cfg.display(), isRoot = isRoot, onBack = back, badge = cfg.type, badgeName = cfg.name,
        bottomTabs = listOf(BottomTab("Monitors", Icons.Filled.MonitorHeart), BottomTab("Server", Icons.Filled.Dns)),
        selectedBottom = tab, onBottom = { tab = it },
    ) { pad ->
        Box(Modifier.padding(pad)) {
            if (tab == 0) KumaMonitors(api, slugVer) else GenericServerTab(cfg, nav) {
                item {
                    var slug by remember { mutableStateOf(api.slug) }
                    Panel(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), tint = KumaGreen) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Status page (optional)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Enter a status page slug to show 24-hour uptime and heartbeat bars. The slug is the last part of the page address, e.g. …/status/home → home",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(slug, { slug = it }, singleLine = true, label = { Text("Slug") }, modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { api.slug = slug; slugVer++ }))
                            TextButton(onClick = { api.slug = slug; slugVer++; tab = 0 }) { Text("Save") }
                        }
                    }
                }
                item {
                    Text("Pausing or editing monitors isn't available through Uptime Kuma's public API – use View on web for that.",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}

@Composable
private fun KumaMonitors(api: KumaApi, ver: Int) {
    val l = rememberLoadable(api.cfg, "kuma", ver, autoRefreshMs = 30000) { api.monitors() }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    LoadableContent(l, empty = { it.isEmpty() }, emptyText = "No monitors found") { list ->
        val up = list.count { it.status == 1 }; val down = list.count { it.status == 0 }; val other = list.size - up - down
        val shown = when (filter) { 1 -> list.filter { it.status == 0 }; 2 -> list.filter { it.status == 1 }; 3 -> list.filter { it.status != 0 && it.status != 1 }; else -> list }
        LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Panel(Modifier.fillMaxWidth(), tint = if (down > 0) Bad else KumaGreen) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        RingGauge(if (list.isEmpty()) 0f else up.toFloat() / list.size, if (down > 0) Bad else KumaGreen, Modifier.size(72.dp)) {
                            Text("${if (list.isEmpty()) 0 else up * 100 / list.size}%", fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (down == 0) "All systems operational" else "$down monitor${if (down == 1) "" else "s"} down",
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (down == 0) KumaGreen else Bad)
                            Text("$up up · $down down · $other other", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item { ChipRow(listOf("All", "Down", "Up", "Other"), filter, { filter = it }, counts = listOf(list.size, down, up, other)) }
            items(shown, key = { it.name }) { m -> KumaRow(m) }
        }
    }
}

@Composable
private fun KumaRow(m: KumaMonitor) {
    val c = kumaColor(m.status)
    Panel(Modifier.fillMaxWidth(), tint = c) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PulseDot(c, pulsing = m.status == 0 || m.status == 1)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(m.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val sub = listOf(m.type.uppercase(), m.target).filter { it.isNotBlank() }.joinToString(" · ")
                    if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Pill(kumaLabel(m.status), c)
                    m.responseMs?.let { Text("$it ms", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp)) }
                }
            }
            if (m.beats.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    m.beats.forEach { b -> Box(Modifier.weight(1f).height(16.dp).clip(RoundedCornerShape(3.dp)).background(kumaColor(b))) }
                }
            }
            val extra = listOfNotNull(
                m.uptime24?.let { "24h uptime %.2f%%".format(it * 100) },
                m.certDays?.takeIf { it >= 0 }?.let { "Certificate: $it days" },
                m.lastMsg.takeIf { it.isNotBlank() && m.status != 1 },
            )
            if (extra.isNotEmpty()) Text(extra.joinToString(" · "), style = MaterialTheme.typography.labelSmall,
                color = if (m.certDays != null && m.certDays in 0..13) Warn else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

// ============================================================ Stash / Stasharr
private val StashBlue = Color(0xFF60A5FA)

@Composable
fun StashScreen(cfg: ServiceConfig, nav: Navigator, isRoot: Boolean, back: () -> Unit) {
    val api = remember(cfg) { StashApi(cfg) }
    var tab by rememberSaveable(cfg.id) { mutableIntStateOf(0) }
    AppScaffold(
        title = cfg.display(), isRoot = isRoot, onBack = back, badge = cfg.type, badgeName = cfg.name,
        bottomTabs = listOf(
            BottomTab("Overview", Icons.Filled.Dashboard), BottomTab("Scenes", Icons.Filled.VideoLibrary),
            BottomTab("Tasks", Icons.Filled.PlaylistPlay), BottomTab("Server", Icons.Filled.Dns),
        ), selectedBottom = tab, onBottom = { tab = it },
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                0 -> StashOverview(api)
                1 -> StashScenes(api, nav)
                2 -> StashTasks(api)
                else -> GenericServerTab(cfg, nav) {
                    item { Box(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) { PrivateArtSwitch(cfg) } }
                }
            }
        }
    }
}

@Composable
private fun StashOverview(api: StashApi) {
    val st = rememberLoadable(api.cfg, "stashstats") { api.stats() }
    val jobs = rememberLoadable(api.cfg, "stashjobs", autoRefreshMs = 4000) { api.jobs() }
    val run = rememberRunner()
    LoadableContent(st) { s ->
        LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { ScreenTip("stash", Icons.Filled.Image, "Screenshots and performer photos are hidden until you turn on Show artwork in the Server tab. Tap a scene for its details.", StashBlue) }
            item {
                Panel(Modifier.fillMaxWidth(), tint = StashBlue) {
                    Column(Modifier.padding(16.dp)) {
                        Text("${s.scenes}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                        Text("scenes · ${humanBytes(s.size.toLong())} · ${humanEta(s.duration.toLong())}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (s.version.isNotEmpty()) Text("Stash ${s.version}", style = MaterialTheme.typography.labelSmall, color = StashBlue)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Performers", "${s.performers}", Rainbow[2], Modifier.weight(1f))
                    StatTile("Studios", "${s.studios}", Rainbow[3], Modifier.weight(1f))
                    StatTile("Tags", "${s.tags}", Rainbow[4], Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Images", "${s.images}", Rainbow[0], Modifier.weight(1f))
                    StatTile("Galleries", "${s.galleries}", Rainbow[1], Modifier.weight(1f))
                    StatTile("Plays", "${s.plays}", Rainbow[5], Modifier.weight(1f))
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Running jobs", Modifier.weight(1f))
                    if (!jobs.data.isNullOrEmpty()) TextButton(onClick = { run("Stopping all jobs", { jobs.load(true) }) { api.stopAll() } }) { Text("Stop all", color = Bad) }
                }
            }
            val j = jobs.data.orEmpty()
            if (j.isEmpty()) item { Text("Nothing running", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 6.dp)) }
            items(j, key = { it.id }) { job ->
                Panel(Modifier.fillMaxWidth(), tint = StashBlue) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(job.description, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(job.status.lowercase().replaceFirstChar { it.uppercase() } + (job.subTasks.firstOrNull()?.let { " · $it" } ?: ""),
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                            }
                            IconButton(onClick = { run("Stopping job", { jobs.load(true) }) { api.stopJob(job.id) } }) { Icon(Icons.Filled.StopCircle, "Stop", tint = Bad) }
                        }
                        job.progress?.let { ProgressBar(it.toFloat(), StashBlue); Text("${(it * 100).toInt()}%", style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StashScenes(api: StashApi, nav: Navigator) {
    var q by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf("") }
    val sorts = listOf("created_at" to "Newest", "date" to "Release date", "play_count" to "Most played", "rating" to "Rating", "filesize" to "Size")
    var sort by rememberSaveable { mutableIntStateOf(0) }
    val l = rememberLoadable(api.cfg, "stashscenes", submitted, sort) { api.scenes(submitted, sorts[sort].first) }
    var pick by remember { mutableStateOf<StashScene?>(null) }
    val run = rememberRunner()
    Column {
        OutlinedTextField(q, { q = it }, singleLine = true, placeholder = { Text("Search scenes, performers, studios") },
            leadingIcon = { Icon(Icons.Filled.Search, null) }, shape = RoundedCornerShape(50),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { submitted = q }),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp))
        ChipRow(sorts.map { it.second }, sort, { sort = it })
        LoadableContent(l, empty = { it.second.isEmpty() }, emptyText = "No scenes") { (count, list) ->
            LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("$count scenes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(list, key = { it.id }) { s ->
                    Panel(Modifier.fillMaxWidth(), tint = StashBlue, onClick = { nav.push(Route.StashScene(api.cfg.id, s.id)) }) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (s.screenshot != null) { Poster(s.screenshot, Modifier.width(110.dp), ratio = 16f / 9f, corner = 8.dp); Spacer(Modifier.width(10.dp)) }
                            Column(Modifier.weight(1f)) {
                                Text(s.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                val meta = listOf(s.studio, s.date).filter { it.isNotEmpty() }.joinToString(" · ")
                                if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.labelSmall, color = StashBlue, maxLines = 1)
                                if (s.performers.isNotEmpty()) Text(s.performers.joinToString(", "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(top = 4.dp)) {
                                    if (s.height > 0) Pill("${s.height}p", Accent2)
                                    if (s.duration > 0) Pill(humanEta(s.duration.toLong()), Accent)
                                    if (s.organized) Pill("Organized", Good)
                                    s.rating?.let { Pill("★ ${it / 20.0}".removeSuffix(".0"), Warn) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StashTasks(api: StashApi) {
    val run = rememberRunner()
    LazyColumn(contentPadding = PaddingValues(top = 6.dp, bottom = 32.dp)) {
        item { SectionTitle("Library") }
        item { MenuRow(Icons.Filled.Refresh, "Scan", "Find new and changed files", Rainbow[0], arrow = false) { run("Scan started") { api.scan() } } }
        item { MenuRow(Icons.Filled.AutoFixHigh, "Identify", "Match scenes using your default identify sources", Rainbow[1], arrow = false) { run("Identify started") { api.identify() } } }
        item { MenuRow(Icons.Filled.LocalOffer, "Auto tag", "Tag performers, studios and tags from file names", Rainbow[2], arrow = false) { run("Auto tag started") { api.autoTag() } } }
        item { MenuRow(Icons.Filled.Image, "Generate", "Covers, previews, sprites, markers and phashes", Rainbow[3], arrow = false) { run("Generate started") { api.generate() } } }
        item { SectionTitle("Maintenance") }
        item { MenuRow(Icons.Filled.CleaningServices, "Clean", "Remove entries for files that no longer exist", Rainbow[4], arrow = false) { run("Clean started") { api.clean() } } }
        item { MenuRow(Icons.Filled.Storage, "Optimise database", "Compact and speed up the database", Rainbow[5], arrow = false) { run("Optimising database") { api.optimise() } } }
        item { Text("Progress shows on the Overview tab.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp)) }
    }
}

@Suppress("unused") private val keepShape = CircleShape


// ------------------------------------------------------------ scene detail
@Composable
fun StashSceneScreen(cfg: ServiceConfig, sceneId: String, nav: Navigator, back: () -> Unit) {
    val api = remember(cfg) { StashApi(cfg) }
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    val l = rememberLoadable(cfg, "stashscene", sceneId) { api.sceneDetail(sceneId) }
    val run = rememberRunner()
    var synopsisOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    AppScaffold(title = l.data?.scene?.title ?: "Scene", isRoot = false, onBack = back, badge = cfg.type, badgeName = cfg.name) { pad ->
        LoadableContent(l, Modifier.padding(pad)) { d ->
            val s = d.scene
            LazyColumn(contentPadding = PaddingValues(bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                        if (s.screenshot != null) coil.compose.AsyncImage(s.screenshot, null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.matchParentSize())
                        else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.HideImage, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                            Text("Artwork hidden – turn on Show artwork in the Server tab", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text(s.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        val meta = listOf(d.studio, s.date, d.code).filter { it.isNotEmpty() }.joinToString(" · ")
                        if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.titleSmall, color = StashBlue, fontWeight = FontWeight.Bold)
                        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                            if (s.duration > 0) Pill(humanEta(s.duration.toLong()), Accent)
                            if (d.video.isNotEmpty()) Pill(d.video, Accent2)
                            if (d.audio.isNotEmpty()) Pill(d.audio, Pink)
                            if (s.size > 0) Pill(humanBytes(s.size), MaterialTheme.colorScheme.onSurfaceVariant)
                            s.rating?.let { Pill("★ " + (it / 20.0).toString().removeSuffix(".0"), Warn) }
                            if (s.plays > 0) Pill("Played ${s.plays}×", Good)
                            if (s.organized) Pill("Organized", Good)
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { uri.openUri("${cfg.baseUrl}/scenes/${s.id}") }, modifier = Modifier.weight(1f)) { Icon(Icons.AutoMirrored.Filled.OpenInNew, null); Text(" Open") }
                        FilledTonalButton(onClick = { run(if (s.organized) "Marked as not organized" else "Marked as organized", { l.load(true) }) { api.setOrganized(s.id, !s.organized) } }, modifier = Modifier.weight(1f)) {
                            Icon(if (s.organized) Icons.Filled.CheckCircle else Icons.Filled.TaskAlt, null); Text(if (s.organized) " Organized" else " Organize")
                        }
                        FilledTonalButton(onClick = { confirmDelete = true }, colors = ButtonDefaults.filledTonalButtonColors(contentColor = Bad)) { Icon(Icons.Filled.Delete, "Delete") }
                    }
                }
                item {
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Rating", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(60.dp))
                        (1..5).forEach { n ->
                            val on = (s.rating ?: 0) >= n * 20
                            IconButton(onClick = { run("Rated $n★", { l.load(true) }) { api.setRating(s.id, if (s.rating == n * 20) null else n * 20) } }, modifier = Modifier.size(36.dp)) {
                                Icon(if (on) Icons.Filled.Star else Icons.Filled.StarBorder, "$n stars", tint = Warn)
                            }
                        }
                    }
                }
                if (d.details.isNotBlank()) item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text("Synopsis", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(d.details, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (synopsisOpen) 200 else 5, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp).clickable { synopsisOpen = !synopsisOpen })
                    }
                }
                if (d.performers.isNotEmpty()) item {
                    Column {
                        SectionTitle("Performers")
                        androidx.compose.foundation.lazy.LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(d.performers, key = { it.id }) { p ->
                                Column(Modifier.width(96.dp).clickable { uri.openUri("${cfg.baseUrl}/performers/${p.id}") }, horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(Modifier.size(width = 96.dp, height = 128.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                        Text(p.name.take(1), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                        if (p.image != null) coil.compose.AsyncImage(p.image, p.name, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.matchParentSize())
                                    }
                                    Text(p.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                                    val age = runCatching { java.time.Period.between(java.time.LocalDate.parse(p.birthdate), runCatching { java.time.LocalDate.parse(s.date) }.getOrDefault(java.time.LocalDate.now())).years }.getOrNull()
                                    val sub = listOfNotNull(p.gender.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }.takeIf { it.isNotEmpty() }, age?.let { "$it in scene" }).joinToString(" · ")
                                    if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                            }
                        }
                    }
                }
                if (d.studio.isNotEmpty()) item {
                    Panel(Modifier.fillMaxWidth().padding(horizontal = 12.dp), tint = StashBlue) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(width = 96.dp, height = 48.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                if (d.studioImage != null) coil.compose.AsyncImage(d.studioImage, d.studio, contentScale = androidx.compose.ui.layout.ContentScale.Fit, modifier = Modifier.matchParentSize().padding(4.dp))
                                else Icon(Icons.Filled.Business, null)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Studio", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(d.studio, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (d.director.isNotEmpty()) Text("Director: ${d.director}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                if (d.tags.isNotEmpty()) item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text("Tags", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                            d.tags.forEach { Pill(it, Accent2) }
                        }
                    }
                }
                item {
                    Panel(Modifier.fillMaxWidth().padding(horizontal = 12.dp), tint = Accent) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("File", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            if (s.path.isNotEmpty()) Text(s.path, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val specs = listOfNotNull(
                                if (d.width > 0 && s.height > 0) "${d.width}×${s.height}" else null,
                                if (d.fps > 0) "%.2f fps".format(d.fps) else null,
                                if (d.bitrate > 0) "%.1f Mbps".format(d.bitrate / 1_000_000.0) else null,
                            )
                            if (specs.isNotEmpty()) Text(specs.joinToString(" · "), style = MaterialTheme.typography.labelSmall)
                            d.urls.forEach { u -> Text(u, style = MaterialTheme.typography.labelSmall, color = StashBlue, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.clickable { uri.openUri(u) }) }
                        }
                    }
                }
            }
        }
    }
    if (confirmDelete) l.data?.scene?.let { s ->
        AlertDialog(
            onDismissRequest = { confirmDelete = false }, title = { Text("Delete ${s.title}?") },
            text = { Text("Remove it from Stash only, or delete the video file too.") },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = { confirmDelete = false; run("Removed from Stash", { back() }) { api.deleteScene(s.id, false) } }) { Text("Remove from library", color = Bad) }
                    TextButton(onClick = { confirmDelete = false; run("Scene and file deleted", { back() }) { api.deleteScene(s.id, true) } }) { Text("Delete scene AND file", color = Bad, fontWeight = FontWeight.Bold) }
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
