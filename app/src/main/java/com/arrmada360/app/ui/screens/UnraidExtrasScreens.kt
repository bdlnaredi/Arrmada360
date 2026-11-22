package com.arrmada360.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrmada360.app.api.*
import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.ui.*
import kotlinx.coroutines.delay

// ============================================================ Live graphs
@Composable
fun UnraidLiveScreen(cfg: ServiceConfig, back: () -> Unit) {
    val api = remember(cfg) { UnraidApi(cfg) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val cpu = remember { mutableStateListOf<Float>() }
    val mem = remember { mutableStateListOf<Float>() }
    var last by remember { mutableStateOf<LiveMetrics?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(cfg.id) {
        while (true) {
            try {
                val m = api.liveMetrics()
                last = m; err = null
                cpu += (m.cpu ?: 0.0).toFloat(); mem += (m.mem ?: 0.0).toFloat()
                while (cpu.size > 200) cpu.removeAt(0)
                while (mem.size > 200) mem.removeAt(0)
                UnraidHistory.add(cfg, m)
            } catch (e: Exception) { err = com.arrmada360.app.net.friendlyError(e) }
            delay(3000)
        }
    }
    val hist = remember(last, tab) { UnraidHistory.load(cfg) }
    AppScaffold(title = "Live graphs", isRoot = false, onBack = back, badge = cfg.type, badgeName = cfg.name) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { ChipRow(listOf("Live (10 min)", "Last 24 hours"), tab, { tab = it }) }
            err?.let { e -> item { Text(e, color = Bad, style = MaterialTheme.typography.labelSmall) } }
            if (tab == 0) {
                item { ChartCard("CPU", last?.cpu?.let { "%.0f%%".format(it) } ?: "–", cpu, Accent) }
                item {
                    ChartCard("Memory", last?.mem?.let { "%.0f%%".format(it) } ?: "–", mem, Pink,
                        sub = last?.takeIf { it.memTotal > 0 }?.let { "${humanBytes(it.memUsed)} of ${humanBytes(it.memTotal)}" })
                }
                last?.cores?.takeIf { it.isNotEmpty() }?.let { cores ->
                    item {
                        Panel(Modifier.fillMaxWidth(), tint = Accent2) {
                            Column(Modifier.padding(14.dp)) {
                                Text("CPU threads", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth().height(80.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                                    cores.forEach { c ->
                                        val f = (c / 100.0).toFloat().coerceIn(0.02f, 1f)
                                        Box(Modifier.weight(1f).fillMaxHeight(f).clip(RoundedCornerShape(3.dp))
                                            .background(if (c > 85) Bad else if (c > 50) Warn else Good))
                                    }
                                }
                                Text("${cores.size} threads · busiest %.0f%%".format(cores.maxOrNull() ?: 0.0), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                item { Text("Updates every 3 seconds while this screen is open.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                if (hist.size < 2) item {
                    EmptyBox("Collecting data… Arrmada360 saves a sample about every 15 minutes in the background, so the chart fills in over the day.")
                } else {
                    val span = hist.last().epochMin - hist.first().epochMin
                    item { ChartCard("CPU · 24 h", "avg %.0f%%".format(hist.map { it.cpu }.average()), hist.map { it.cpu }, Accent, sub = "peak %.0f%% · last ${span / 60} h".format(hist.maxOf { it.cpu })) }
                    item { ChartCard("Memory · 24 h", "avg %.0f%%".format(hist.map { it.mem }.average()), hist.map { it.mem }, Pink, sub = "peak %.0f%%".format(hist.maxOf { it.mem })) }
                }
            }
        }
    }
}

@Composable
private fun ChartCard(title: String, value: String, points: List<Float>, color: Color, sub: String? = null) {
    Panel(Modifier.fillMaxWidth(), tint = color) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = color)
            }
            if (sub != null) Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            LineChart(points, color, Modifier.fillMaxWidth().height(130.dp))
        }
    }
}

@Composable
private fun LineChart(points: List<Float>, color: Color, modifier: Modifier) {
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    Canvas(modifier) {
        listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { f -> drawLine(grid, Offset(0f, size.height * f), Offset(size.width, size.height * f), 1f) }
        if (points.size < 2) return@Canvas
        val step = size.width / (points.size - 1)
        fun y(v: Float) = size.height * (1f - (v / 100f).coerceIn(0f, 1f))
        val line = Path().apply { points.forEachIndexed { i, v -> if (i == 0) moveTo(0f, y(v)) else lineTo(i * step, y(v)) } }
        val fill = Path().apply { addPath(line); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0.02f))))
        drawPath(line, color, style = Stroke(width = 3f))
    }
}

// ============================================================ Logs
@Composable
fun UnraidLogsScreen(cfg: ServiceConfig, containerId: String?, containerName: String?, back: () -> Unit) {
    val api = remember(cfg) { UnraidApi(cfg) }
    var open by remember { mutableStateOf<Pair<String, String>?>(containerId?.let { (containerName ?: "Container") to it }) }
    val files = rememberLoadable(cfg, "logfiles") { api.logFiles() }
    val o = open
    if (o != null) { LogViewer(api, o.first, isContainer = containerId != null && o.second == containerId, path = o.second) { if (containerId != null) back() else open = null }; return }
    AppScaffold(title = "Logs", isRoot = false, onBack = back, badge = cfg.type, badgeName = cfg.name) { pad ->
        LoadableContent(files, Modifier.padding(pad), empty = { it.isEmpty() }, emptyText = "No log files reported by Unraid") { list ->
            LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item { Text("For a container's own log, open Docker and pick Logs from its ⋮ menu.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(list, key = { it.path }) { f ->
                    Panel(Modifier.fillMaxWidth(), tint = Accent, onClick = { open = f.name to f.path }) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Description, null, tint = Accent)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(f.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("${humanBytes(f.size)} · ${fmtDate(f.modified, true)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogViewer(api: UnraidApi, title: String, isContainer: Boolean, path: String, back: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var lines by rememberSaveable { mutableIntStateOf(300) }
    var q by rememberSaveable { mutableStateOf("") }
    var wrap by rememberSaveable { mutableStateOf(true) }
    var errorsOnly by rememberSaveable { mutableStateOf(false) }
    val l = rememberLoadable(api.cfg, path, lines, autoRefreshMs = 10000) { if (isContainer) api.containerLog(path, lines) else api.readLog(path, lines) }
    val state = rememberLazyListState()
    AppScaffold(title = title, isRoot = false, onBack = back, actions = {
        IconButton(onClick = {
            val t = l.data?.text ?: return@IconButton
            ctx.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, t.takeLast(90_000)), "Share log"))
        }) { Icon(Icons.Filled.Share, "Share") }
    }) { pad ->
        Column(Modifier.padding(pad)) {
            OutlinedTextField(q, { q = it }, singleLine = true, placeholder = { Text("Search in log") }, leadingIcon = { Icon(Icons.Filled.Search, null) },
                shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp))
            Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(100, 300, 1000).forEach { n -> FilterChip(lines == n, { lines = n }, label = { Text("$n lines") }) }
                FilterChip(errorsOnly, { errorsOnly = !errorsOnly }, label = { Text("Errors") })
                FilterChip(wrap, { wrap = !wrap }, label = { Text("Wrap") })
            }
            LoadableContent(l) { c ->
                val all = c.text.lines()
                val rx = Regex("error|fail|fatal|exception|warn", RegexOption.IGNORE_CASE)
                val shown = all.filter { (q.isBlank() || it.contains(q, true)) && (!errorsOnly || rx.containsMatchIn(it)) }
                LaunchedEffect(c.text) { if (shown.isNotEmpty()) state.scrollToItem(shown.size - 1) }
                if (shown.isEmpty()) EmptyBox(if (c.text.isBlank()) "Log is empty" else "No lines match")
                else SelectionContainer {
                    LazyColumn(state = state, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF0B0D12))) {
                        items(shown.size) { i ->
                            val line = shown[i]
                            val col = when {
                                Regex("error|fail|fatal|exception", RegexOption.IGNORE_CASE).containsMatchIn(line) -> Color(0xFFFF7A7A)
                                line.contains("warn", true) -> Color(0xFFFFC857)
                                else -> Color(0xFFD7DCE5)
                            }
                            val mod = if (wrap) Modifier.fillMaxWidth() else Modifier.horizontalScroll(rememberScrollState())
                            Text(line, color = col, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp, softWrap = wrap,
                                maxLines = if (wrap) Int.MAX_VALUE else 1, overflow = TextOverflow.Clip, modifier = mod.padding(vertical = 1.dp))
                        }
                    }
                }
            }
        }
    }
}
