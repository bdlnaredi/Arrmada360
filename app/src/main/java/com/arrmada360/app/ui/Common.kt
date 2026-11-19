package com.arrmada360.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ripple
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
            if (!silent || data == null) error = com.arrmada360.app.net.friendlyError(e)
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
                ctx.toast(com.arrmada360.app.net.friendlyError(e))
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

// ------------------------------------------------------------------ backgrounds
/** Soft coloured glow behind every screen, tinted by the current app's colours. */
@Composable
fun AppBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val p = MaterialTheme.colorScheme.primary
    val s = MaterialTheme.colorScheme.secondary
    val bg = MaterialTheme.colorScheme.background
    val t = rememberInfiniteTransition(label = "aurora")
    val drift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), label = "drift")
    Box(
        modifier.fillMaxSize().background(bg).drawBehind {
            val w = size.width
            val h = size.height
            drawCircle(
                brush = Brush.radialGradient(listOf(p.copy(alpha = 0.30f), Color.Transparent), center = Offset(w * (0.1f + 0.25f * drift), 0f), radius = w * 0.95f),
                radius = w * 0.95f, center = Offset(w * (0.1f + 0.25f * drift), 0f),
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(s.copy(alpha = 0.22f), Color.Transparent), center = Offset(w * (0.95f - 0.2f * drift), h * 0.22f), radius = w * 0.8f),
                radius = w * 0.8f, center = Offset(w * (0.95f - 0.2f * drift), h * 0.22f),
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(p.copy(alpha = 0.10f), Color.Transparent), center = Offset(w * 0.3f, h * (0.85f - 0.1f * drift)), radius = w * 0.9f),
                radius = w * 0.9f, center = Offset(w * 0.3f, h * (0.85f - 0.1f * drift)),
            )
        },
    ) {
        // Make every plain Text light by default (a transparent background otherwise falls back to black).
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onBackground,
        ) { content() }
    }
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
    badgeName: String? = null,
    actions: @Composable () -> Unit = {},
    fab: @Composable () -> Unit = {},
    tabs: List<String> = emptyList(),
    selectedTab: Int = 0,
    onTab: (Int) -> Unit = {},
    bottomTabs: List<BottomTab> = emptyList(),
    selectedBottom: Int = 0,
    onBottom: (Int) -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val openDrawer = LocalOpenDrawer.current
    AppBackground {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (badge != null) {
                                    ServiceBadge(badge, 34.dp, badgeName)
                                    Spacer(Modifier.width(12.dp))
                                }
                                Column {
                                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                                    if (!subtitle.isNullOrEmpty()) Text(
                                        subtitle, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            if (isRoot) IconButton(onClick = openDrawer) { Icon(Icons.Filled.Menu, "Menu") }
                            else IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                        },
                        actions = { actions() },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    )
                    if (tabs.isNotEmpty()) PillTabs(tabs, selectedTab, onTab)
                }
            },
            bottomBar = { if (bottomTabs.isNotEmpty()) BottomTabs(bottomTabs, selectedBottom, onBottom) },
            floatingActionButton = fab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            content = content,
        )
    }
}

/** A tappable settings-style row with a coloured icon and an arrow, used on every Server tab. */
@Composable
fun MenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    external: Boolean = false,
    arrow: Boolean = true,
    onClick: () -> Unit,
) {
    Panel(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), tint = tint, onClick = onClick) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.40f), tint.copy(alpha = 0.15f)))),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                if (!subtitle.isNullOrEmpty()) Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            if (arrow) Icon(
                if (external) androidx.compose.material.icons.Icons.AutoMirrored.Filled.OpenInNew else androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null, tint = tint,
            )
        }
    }
}

data class BottomTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

/** Colourful bottom navigation bar, like nzb360's per-app tabs. */
@Composable
fun BottomTabs(tabs: List<BottomTab>, selected: Int, onSelect: (Int) -> Unit) {
    val p = MaterialTheme.colorScheme.primary
    val s = MaterialTheme.colorScheme.secondary
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, p.copy(alpha = 0.6f), s.copy(alpha = 0.6f), Color.Transparent))))
        androidx.compose.material3.NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f), tonalElevation = 0.dp) {
            tabs.forEachIndexed { i, t ->
                val on = i == selected
                NavigationBarItem(
                    selected = on,
                    onClick = { onSelect(i) },
                    icon = { Icon(t.icon, t.label) },
                    label = { Text(t.label, maxLines = 1, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.labelSmall) },
                    colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0B0D10),
                        selectedTextColor = p,
                        indicatorColor = p,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

@Composable
fun PillTabs(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val p = MaterialTheme.colorScheme.primary
    val s = MaterialTheme.colorScheme.secondary
    LazyRow(
        modifier = modifier.fillMaxWidth().padding(bottom = 6.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(tabs.size) { i ->
            val on = i == selected
            val textColor by animateColorAsState(if (on) Color(0xFF0B0D10) else MaterialTheme.colorScheme.onSurfaceVariant, label = "tab")
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (on) Brush.horizontalGradient(listOf(p, s)) else SolidColor(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)))
                    .clickable { onSelect(i) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(tabs[i], color = textColor, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ------------------------------------------------------------------ logos & widgets
/** The app's official logo on a tinted tile. Falls back to initials while offline. */
@Composable
fun ServiceBadge(type: ServiceType, size: Dp = 36.dp, name: String? = null) {
    var loaded by remember(type, name) { mutableStateOf(false) }
    val shape = RoundedCornerShape(size * 0.28f)
    Box(
        Modifier.size(size).clip(shape)
            .background(Brush.linearGradient(listOf(type.tint.copy(alpha = 0.32f), type.tint2.copy(alpha = 0.14f))))
            .border(1.dp, type.tint.copy(alpha = 0.45f), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (!loaded) Text(type.short, color = type.tint, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.36f).sp)
        AsyncImage(
            model = type.logoUrl(name), contentDescription = type.label, contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(size * 0.15f),
            onSuccess = { loaded = true }, onError = { loaded = false },
        )
    }
}

@Composable
fun Poster(url: String?, modifier: Modifier = Modifier, ratio: Float = 2f / 3f, corner: Dp = 10.dp, fallback: String = "") {
    Box(
        modifier.aspectRatio(ratio).clip(RoundedCornerShape(corner))
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)))),
        contentAlignment = Alignment.Center,
    ) {
        if (fallback.isNotEmpty()) Text(
            fallback.take(24), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(6.dp),
        )
        if (url != null) AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun StatusDot(color: Color, size: Dp = 8.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}

/** A status dot with a soft pulsing halo, used for "online / live". */
@Composable
fun PulseDot(color: Color, pulsing: Boolean = true, size: Dp = 10.dp) {
    val t = rememberInfiniteTransition(label = "pulse")
    val a by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "a")
    Box(Modifier.size(size * 2.2f), contentAlignment = Alignment.Center) {
        if (pulsing) Box(
            Modifier.size(size * 2.2f).graphicsLayer { scaleX = 0.45f + a * 0.55f; scaleY = 0.45f + a * 0.55f; alpha = 1f - a }
                .clip(CircleShape).background(color.copy(alpha = 0.6f)),
        )
        Box(Modifier.size(size).clip(CircleShape).background(color))
    }
}

@Composable
fun Pill(text: String, color: Color = MaterialTheme.colorScheme.primary, solid: Boolean = false) {
    Box(
        Modifier.clip(RoundedCornerShape(50))
            .background(if (solid) SolidColor(color) else Brush.horizontalGradient(listOf(color.copy(alpha = 0.26f), color.copy(alpha = 0.12f))))
            .padding(horizontal = 9.dp, vertical = 3.dp),
    ) { Text(text, color = if (solid) Color(0xFF0B0D10) else color, style = MaterialTheme.typography.labelSmall, maxLines = 1, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Row(modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height(14.dp).clip(RoundedCornerShape(50))
            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))))
        Spacer(Modifier.width(8.dp))
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
    }
}

/** A tinted glass card that springs a little when pressed. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    tint: Color? = null,
    content: @Composable () -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "press")
    val t = tint ?: MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(Brush.linearGradient(listOf(t.copy(alpha = 0.13f).compositeOver(surface), surface.copy(alpha = 0.92f))))
            .border(1.dp, Brush.linearGradient(listOf(t.copy(alpha = 0.35f), t.copy(alpha = 0.05f))), shape)
            .let {
                if (onClick != null) it.combinedClickable(interactionSource = src, indication = ripple(), onLongClick = onLongClick, onClick = onClick) else it
            },
    ) { content() }
}

@Composable
fun ProgressBar(progress: Float, color: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier, height: Dp = 5.dp) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), animationSpec = tween(700), label = "progress")
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (p > 0f) Box(
            Modifier.fillMaxWidth(p).height(height).clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.65f), color))),
        )
    }
}

/** Ring gauge for percentages (disk used, CPU…). */
@Composable
fun RingGauge(value: Float, color: Color, modifier: Modifier = Modifier, stroke: Dp = 8.dp, content: @Composable () -> Unit = {}) {
    val v by animateFloatAsState(value.coerceIn(0f, 1f), tween(900), label = "ring")
    val track = MaterialTheme.colorScheme.surfaceVariant
    val c2 = MaterialTheme.colorScheme.secondary
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val inset = sw / 2
            val arcSize = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw)
            drawArc(track, -90f, 360f, false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(sw, cap = StrokeCap.Round))
            drawArc(Brush.sweepGradient(listOf(color, c2, color)), -90f, 360f * v, false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(sw, cap = StrokeCap.Round))
        }
        content()
    }
}

/** Small live line chart with a soft gradient fill. */
@Composable
fun Sparkline(values: List<Long>, color: Color, modifier: Modifier = Modifier, fill: Boolean = true) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val max = (values.maxOrNull() ?: 1L).coerceAtLeast(1L).toFloat()
        val stepX = size.width / (values.size - 1)
        val line = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height - (v / max) * size.height * 0.92f
            if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
        }
        if (fill) {
            val area = Path()
            area.addPath(line)
            area.lineTo(size.width, size.height)
            area.lineTo(0f, size.height)
            area.close()
            drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.40f), Color.Transparent)))
        }
        drawPath(line, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** Animated shimmering placeholder shown while data loads. */
@Composable
fun Modifier.shimmer(): Modifier {
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(-400f, 1400f, infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "x")
    val base = MaterialTheme.colorScheme.surfaceVariant
    val hi = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f).compositeOver(base)
    return this.background(Brush.linearGradient(listOf(base, hi, base), start = Offset(x, 0f), end = Offset(x + 400f, 200f)))
}

@Composable
fun CenterBox(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { content() }
}

@Composable
fun LoadingBox() {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(6) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp, 70.dp).clip(RoundedCornerShape(10.dp)).shimmer())
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.fillMaxWidth(0.7f).height(14.dp).clip(RoundedCornerShape(6.dp)).shimmer())
                    Box(Modifier.fillMaxWidth(0.45f).height(10.dp).clip(RoundedCornerShape(6.dp)).shimmer())
                }
            }
        }
    }
}

@Composable
fun ErrorBox(msg: String, onRetry: () -> Unit) = CenterBox {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(84.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Bad.copy(alpha = 0.35f), Pink.copy(alpha = 0.15f)))), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.CloudOff, null, tint = Bad, modifier = Modifier.size(40.dp))
        }
        Text("Couldn't connect", style = MaterialTheme.typography.titleMedium)
        Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        GradientButton("Try again", onClick = onRetry)
    }
}

@Composable
fun EmptyBox(text: String) = CenterBox {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(84.dp).clip(CircleShape).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)))), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Inbox, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(38.dp))
        }
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun GradientButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val p = MaterialTheme.colorScheme.primary
    val s = MaterialTheme.colorScheme.secondary
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier, shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant),
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            Modifier.background(if (enabled) Brush.horizontalGradient(listOf(p, s)) else SolidColor(Color.Transparent)).padding(horizontal = 22.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) { Text(text, color = if (enabled) Color(0xFF0B0D10) else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) }
    }
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
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Box {
            OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
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

/** Colourful filter chips; each chip gets its own colour from the rainbow palette. */
@Composable
fun ChipRow(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, counts: List<Int>? = null) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options.size) { i ->
            val on = i == selected
            val c = if (i == 0) MaterialTheme.colorScheme.primary else Rainbow[(i - 1) % Rainbow.size]
            val bgAlpha by animateFloatAsState(if (on) 0.9f else 0.14f, label = "chip")
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(c.copy(alpha = bgAlpha))
                    .border(1.dp, c.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .clickable { onSelect(i) }.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(options[i], color = if (on) Color(0xFF0B0D10) else c, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                val n = counts?.getOrNull(i)
                if (n != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(n.toString(), color = if (on) Color(0xFF0B0D10) else c.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/** A number that smoothly counts to its new value. */
@Composable
fun animatedLong(target: Long): Long {
    val v by animateFloatAsState(target.toFloat(), tween(800), label = "num")
    return v.toLong()
}

/** Big stat tile with gradient (dashboard / headers). */
@Composable
fun StatTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0.08f))))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
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
