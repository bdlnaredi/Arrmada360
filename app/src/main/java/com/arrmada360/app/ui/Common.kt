package com.arrmada360.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.arrmada360.app.data.ServiceType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ------------------------------------------------------------------ async state
class Loadable<T>(private val scope: CoroutineScope, private val block: suspend () -> T) {
    var data by mutableStateOf<T?>(null)
    var error by mutableStateOf<String?>(null)
    var loading by mutableStateOf(false)

    fun load(silent: Boolean = false) {
        scope.launch { loadNow(silent) }
    }

    suspend fun loadNow(silent: Boolean = false) {
        if (!silent) loading = true
        try {
            data = block()
            error = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!silent || data == null) error = e.message ?: e.toString()
        }
        loading = false
    }
}

@Composable
fun <T> rememberLoadable(vararg keys: Any?, autoRefreshMs: Long = 0, block: suspend () -> T): Loadable<T> {
    val scope = rememberCoroutineScope()
    val l = remember(*keys) { Loadable(scope, block) }
    LaunchedEffect(l) {
        l.loadNow()
        if (autoRefreshMs > 0) {
            while (true) {
                delay(autoRefreshMs)
                l.loadNow(silent = true)
            }
        }
    }
    return l
}

class Runner(private val scope: CoroutineScope, private val ctx: Context) {
    operator fun invoke(success: String?, after: (() -> Unit)? = null, block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
                if (success != null) ctx.toast(success)
                after?.invoke()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ctx.toast(e.message ?: "Something went wrong")
            }
        }
    }
}

@Composable
fun rememberRunner(): Runner {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    return remember { Runner(scope, ctx) }
}

fun Context.toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

fun Context.copyText(text: String) {
    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Arrmada360", text))
    toast("Copied")
}

// ------------------------------------------------------------------ navigation shell
val LocalOpenDrawer = staticCompositionLocalOf<() -> Unit> { {} }

@Composable
fun AppScaffold(
    title: String,
    subtitle: String? = null,
    isRoot: Boolean,
    onBack: () -> Unit,
    badge: ServiceType? = null,
    actions: @Composable () -> Unit = {},
    fab: @Composable () -> Unit = {},
    tabs: List<String> = emptyList(),
    selectedTab: Int = 0,
    onTab: (Int) -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val openDrawer = LocalOpenDrawer.current
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (badge != null) {
                                ServiceBadge(badge, 28.dp)
                                Spacer(Modifier.width(10.dp))
                            }
                            Column {
                                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (!subtitle.isNullOrEmpty()) Text(
                                    subtitle, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        if (isRoot) IconButton(onClick = openDrawer) { Icon(Icons.Filled.Menu, "Menu") }
                        else IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    },
                    actions = { actions() },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                )
                if (tabs.isNotEmpty()) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        edgePadding = 8.dp,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {},
                    ) {
                        tabs.forEachIndexed { i, t ->
                            Tab(
                                selected = i == selectedTab, onClick = { onTab(i) },
                                text = { Text(t, fontWeight = if (i == selectedTab) FontWeight.SemiBold else FontWeight.Normal) },
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = fab,
        containerColor = MaterialTheme.colorScheme.background,
        content = content,
    )
}

// ------------------------------------------------------------------ small widgets
@Composable
fun ServiceBadge(type: ServiceType, size: Dp = 36.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size / 4)).background(type.tint.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(type.short, color = type.tint, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.38f).sp)
    }
}

@Composable
fun Poster(url: String?, modifier: Modifier = Modifier, ratio: Float = 2f / 3f, corner: Dp = 8.dp, fallback: String = "") {
    Box(
        modifier.aspectRatio(ratio).clip(RoundedCornerShape(corner)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (fallback.isNotEmpty()) Text(
            fallback.take(24), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(6.dp),
        )
        if (url != null) AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun StatusDot(color: Color, size: Dp = 8.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}

@Composable
fun Pill(text: String, color: Color = MaterialTheme.colorScheme.primary) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.16f)).padding(horizontal = 8.dp, vertical = 2.dp),
    ) { Text(text, color = color, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp),
    )
}

@Composable
fun Panel(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
    ) { content() }
}

@Composable
fun ProgressBar(progress: Float, color: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier, height: Dp = 4.dp) {
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(height).clip(RoundedCornerShape(50)).background(color))
    }
}

@Composable
fun CenterBox(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { content() }
}

@Composable
fun LoadingBox() = CenterBox { CircularProgressIndicator() }

@Composable
fun ErrorBox(msg: String, onRetry: () -> Unit) = CenterBox {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Filled.CloudOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
        Text("Couldn't connect", style = MaterialTheme.typography.titleMedium)
        Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Button(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
fun EmptyBox(text: String) = CenterBox {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
}

/** Shows loading / error / content for a [Loadable], with pull-to-refresh. */
@Composable
fun <T> LoadableContent(l: Loadable<T>, modifier: Modifier = Modifier, empty: (T) -> Boolean = { false }, emptyText: String = "Nothing here", content: @Composable (T) -> Unit) {
    val d = l.data
    PullToRefreshBox(isRefreshing = l.loading && d != null, onRefresh = { l.load() }, modifier = modifier.fillMaxSize()) {
        when {
            d == null && l.error != null -> ErrorBox(l.error ?: "") { l.load() }
            d == null -> LoadingBox()
            empty(d) -> EmptyBox(emptyText)
            else -> content(d)
        }
    }
}

@Composable
fun <T> Dropdown(label: String, options: List<T>, selected: T?, text: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected?.let(text) ?: "Select…", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Filled.ArrowDropDown, null)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { o ->
                    DropdownMenuItem(text = { Text(text(o)) }, onClick = { onSelect(o); open = false })
                }
            }
        }
    }
}

@Composable
fun ChipRow(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options.size) { i ->
            FilterChip(selected = i == selected, onClick = { onSelect(i) }, label = { Text(options[i]) })
        }
    }
}

// ------------------------------------------------------------------ formatting
fun humanBytes(b: Long): String {
    if (b < 0) return "–"
    if (b < 1024) return "$b B"
    val units = listOf("KB", "MB", "GB", "TB", "PB")
    var v = b.toDouble() / 1024
    var i = 0
    while (v >= 1024 && i < units.size - 1) { v /= 1024; i++ }
    return String.format(Locale.US, if (v >= 100) "%.0f %s" else "%.1f %s", v, units[i])
}

fun humanSpeed(bps: Long): String = if (bps <= 0) "0 B/s" else humanBytes(bps) + "/s"

fun humanEta(sec: Long): String {
    if (sec < 0) return "∞"
    if (sec == 0L) return "–"
    val d = sec / 86400; val h = (sec % 86400) / 3600; val m = (sec % 3600) / 60; val s = sec % 60
    return when {
        d > 0 -> "${d}d ${h}h"
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m ${s}s"
        else -> "${s}s"
    }
}

private val dayFmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
private val dayTimeFmt = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.getDefault())

fun parseDate(iso: String): LocalDate? = try {
    if (iso.length > 10) Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalDate() else LocalDate.parse(iso.take(10))
} catch (e: Exception) {
    try { LocalDate.parse(iso.take(10)) } catch (e2: Exception) { null }
}

fun fmtDate(iso: String, withTime: Boolean = false): String {
    if (iso.isEmpty()) return ""
    return try {
        if (iso.length > 10) {
            val z = Instant.parse(iso).atZone(ZoneId.systemDefault())
            if (withTime) z.format(dayTimeFmt) else z.format(dayFmt)
        } else LocalDate.parse(iso).format(dayFmt)
    } catch (e: Exception) {
        iso.take(10)
    }
}

fun fmtEpoch(sec: Long): String = if (sec <= 0) "" else Instant.ofEpochSecond(sec).atZone(ZoneId.systemDefault()).format(dayTimeFmt)

fun relativeDay(d: LocalDate?): String {
    if (d == null) return "Unknown"
    val today = LocalDate.now()
    return when (d) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        today.minusDays(1) -> "Yesterday"
        else -> d.format(dayFmt)
    }
}
