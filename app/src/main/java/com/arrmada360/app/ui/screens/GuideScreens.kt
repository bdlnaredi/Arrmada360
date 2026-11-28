package com.arrmada360.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrmada360.app.data.ConfigStore
import com.arrmada360.app.data.Family
import com.arrmada360.app.data.ServiceType
import com.arrmada360.app.ui.*

/** Remembers which tips have been seen. */
object Tips {
    private val version = mutableIntStateOf(0)
    fun seen(id: String): Boolean { version.intValue; return ConfigStore.flag("tip_$id") }
    fun markSeen(id: String) { ConfigStore.setFlag("tip_$id", true); version.intValue++ }
    val all = listOf("welcome", "carousel", "library", "detail", "downloads", "unraid", "stash", "person", "settings", "search")
    fun resetAll() { all.forEach { ConfigStore.setFlag("tip_$it", false) }; ConfigStore.putInt("tipIndex", 0); version.intValue++ }
}

private val DlOrange = Color(0xFFE8A23A)

/** Opens the first download client (or Add service when there is none). */
fun openDownloads(nav: Navigator) {
    val dl = ConfigStore.services.firstOrNull { it.type.isDownloader && Family.canOpen(it.id) }
    if (dl != null) nav.push(Route.Service(dl.id)) else if (!Family.active) nav.push(Route.AddService)
}

// ============================================================ dashboard cards
@Composable
fun DashboardGuideCards(nav: Navigator) {
    val pending = ConfigStore.getStringSet("setupPending").mapNotNull { n -> ServiceType.entries.firstOrNull { it.name == n } }
        .filter { t -> ConfigStore.services.none { it.type == t } }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 6.dp)) {
        AnimatedVisibility(!Tips.seen("welcome"), enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) { WelcomeSwipeCard(nav) }
        if (pending.isNotEmpty() && !Family.active) FinishSetupCard(pending, nav)
        AnimatedVisibility(Tips.seen("welcome") && !Tips.seen("carousel"), enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) { TipCarousel(nav) }
    }
}

@Composable
private fun WelcomeSwipeCard(nav: Navigator) {
    val openDrawer = LocalOpenDrawer.current
    val t = rememberInfiniteTransition(label = "hand")
    val x by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "x")
    var drag by remember { mutableFloatStateOf(0f) }
    Box(
        Modifier.padding(horizontal = 12.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1F2A2E), MaterialTheme.colorScheme.surface, Color(0xFF3A2E22).copy(alpha = 0.7f))))
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (drag > 120) { openDrawer(); Tips.markSeen("welcome") }
                        else if (drag < -120) { openDownloads(nav); Tips.markSeen("welcome") }
                        drag = 0f
                    },
                ) { _, d -> drag += d }
            },
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                    Icon(androidx.compose.ui.res.painterResource(com.arrmada360.app.R.drawable.ic_launcher_foreground), null, tint = Color.Unspecified, modifier = Modifier.size(60.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("WELCOME", style = MaterialTheme.typography.labelLarge, letterSpacing = 2.sp, color = OnbGreen, fontWeight = FontWeight.Bold)
                    Text("Welcome to Arrmada360", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                }
                IconButton(onClick = { Tips.markSeen("welcome") }) { Icon(Icons.Filled.Close, "Dismiss", tint = Color.White.copy(alpha = 0.6f)) }
            }
            Text("Open the menu and your downloads from anywhere. Swipe left or right on this card to try it.",
                style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.75f), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp))
            // menu ◀ · · · hand · · · ▶ downloads
            Box(Modifier.fillMaxWidth().height(110.dp).padding(top = 14.dp)) {
                val dotColor = Color.White.copy(alpha = 0.35f)
                Canvas(Modifier.fillMaxWidth().height(4.dp).align(Alignment.Center).padding(horizontal = 80.dp)) {
                    drawLine(dotColor, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 14f)))
                }
                Box(Modifier.align(Alignment.CenterStart).size(72.dp).clip(CircleShape).background(OnbGreen.copy(alpha = 0.3f)).border(2.dp, OnbGreen.copy(alpha = 0.6f), CircleShape)
                    .clickable { openDrawer(); Tips.markSeen("welcome") }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Menu, "Menu", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Box(Modifier.align(Alignment.CenterEnd).size(72.dp).clip(CircleShape).background(DlOrange.copy(alpha = 0.15f)).border(2.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    .clickable { openDownloads(nav); Tips.markSeen("welcome") }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Downloading, "Downloads", tint = Color.White, modifier = Modifier.size(30.dp))
                }
                BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 90.dp)) {
                    val travel = maxWidth - 44.dp
                    Icon(Icons.Filled.TouchApp, null, tint = Color.White, modifier = Modifier.size(44.dp).align(Alignment.CenterStart)
                        .graphicsLayer { translationX = (travel.toPx() * x) + drag.coerceIn(-200f, 200f) * 0.4f; translationY = 18f })
                }
            }
            Row(Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp).clip(RoundedCornerShape(50)).border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.05f)).padding(horizontal = 18.dp, vertical = 10.dp)) {
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(color = OnbGreen, fontWeight = FontWeight.Bold)) { append("Swipe right") }
                    withStyle(SpanStyle(color = Color.White)) { append(" for menu  /  ") }
                    withStyle(SpanStyle(color = DlOrange, fontWeight = FontWeight.Bold)) { append("left") }
                    withStyle(SpanStyle(color = Color.White)) { append(" for downloads") }
                }, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun FinishSetupCard(pending: List<ServiceType>, nav: Navigator) {
    Panel(Modifier.padding(horizontal = 12.dp).fillMaxWidth(), tint = OnbGreen) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Cable, null, tint = OnbGreen)
                Spacer(Modifier.width(10.dp))
                Text("Finish connecting your apps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { ConfigStore.putStringSet("setupPending", emptySet()) }) { Text("Hide") }
            }
            Text("Tap one to add its address and API key.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pending.forEach { t ->
                    Row(Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { nav.push(Route.EditService(null, t)) }
                        .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        LogoTile(t, 30.dp); Spacer(Modifier.width(8.dp))
                        Text(t.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private data class TipData(val icon: ImageVector, val color: Color, val title: String, val body: String, val action: String? = null, val go: ((Navigator) -> Unit)? = null)

private val tipList = listOf(
    TipData(Icons.Filled.Search, Accent, "Search everything", "Type in the bar at the top to search your libraries and add new movies and shows. Tap the mic to speak, or the picture icon to identify a video clip."),
    TipData(Icons.Filled.Share, OnbGreen, "Share into Arrmada360", "Share a video clip, screenshot, IMDb/TMDB link or magnet link from any app to Arrmada360 – it works out what it is."),
    TipData(Icons.Filled.TouchApp, Accent2, "Long-press to select", "In any library, long-press posters to select several and monitor, search, tag or delete them all at once."),
    TipData(Icons.Filled.Person, Pink, "Tap a person", "On a movie or show, tap anyone in the cast to see their biography and filmography – and add their other titles."),
    TipData(Icons.Filled.Dns, Warn, "The Server tab", "Speed scheduler, seeding manager, year in review, disk space and health all live on the dashboard's Server tab."),
    TipData(Icons.Filled.Wifi, Rainbow[0], "Home or away", "Give each app a remote address (Tailscale or Cloudflare) and Arrmada360 switches automatically when you leave home Wi-Fi.",
        "Connection switching") { it.push(Route.SettingsConnection) },
    TipData(Icons.Filled.Widgets, Rainbow[4], "Widgets & quick tile", "Long-press your home screen → Widgets for speeds and upcoming episodes. Add the Downloads tile to Quick Settings to pause everything in one tap."),
    TipData(Icons.Filled.Notifications, Rainbow[3], "Stay in the loop", "Turn on download alerts and a morning summary in Settings → Notifications.", "Notification settings") { it.push(Route.SettingsNotifications) },
)

@Composable
private fun TipCarousel(nav: Navigator) {
    var i by remember { mutableIntStateOf(ConfigStore.getInt("tipIndex", 0).coerceIn(0, tipList.size - 1)) }
    val tip = tipList[i]
    Box(Modifier.padding(horizontal = 12.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))
        .background(Brush.linearGradient(listOf(tip.color.copy(alpha = 0.22f), MaterialTheme.colorScheme.surface)))
        .border(1.dp, tip.color.copy(alpha = 0.3f), RoundedCornerShape(24.dp))) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("TIP ${i + 1} OF ${tipList.size}", style = MaterialTheme.typography.labelMedium, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold, color = tip.color, modifier = Modifier.weight(1f))
                TextButton(onClick = { Tips.markSeen("carousel") }) { Text("Hide tips") }
            }
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(tip.color.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                    Icon(tip.icon, null, tint = tip.color)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(tip.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(tip.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    if (tip.action != null && tip.go != null) TextButton(onClick = { tip.go.invoke(nav) }, contentPadding = PaddingValues(0.dp)) { Text(tip.action + " →", color = tip.color) }
                }
            }
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    tipList.indices.forEach { k ->
                        Box(Modifier.height(6.dp).width(if (k == i) 18.dp else 6.dp).clip(RoundedCornerShape(50)).background(if (k == i) tip.color else MaterialTheme.colorScheme.outlineVariant))
                    }
                }
                if (i > 0) TextButton(onClick = { i--; ConfigStore.putInt("tipIndex", i) }) { Text("Back") }
                Button(onClick = {
                    if (i + 1 >= tipList.size) Tips.markSeen("carousel") else { i++; ConfigStore.putInt("tipIndex", i) }
                }, colors = ButtonDefaults.buttonColors(containerColor = tip.color, contentColor = Color.Black)) {
                    Text(if (i + 1 >= tipList.size) "Got it" else "Next tip")
                }
            }
        }
    }
}

// ============================================================ one-time screen tips
/** A small "first time here" banner shown once per screen. */
@Composable
fun ScreenTip(id: String, icon: ImageVector, text: String, color: Color = OnbGreen) {
    AnimatedVisibility(!Tips.seen(id), enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.16f)).border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(18.dp)).padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            TextButton(onClick = { Tips.markSeen(id) }) { Text("Got it", color = color, fontWeight = FontWeight.Bold) }
        }
    }
}

// ============================================================ Help & guides
@Composable
fun HelpScreen(nav: Navigator, back: () -> Unit) {
    val uri = LocalUriHandler.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var open by remember { mutableStateOf<ServiceType?>(null) }
    AppScaffold(title = "Help & guides", isRoot = false, onBack = back) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Panel(Modifier.weight(1f), tint = OnbGreen, onClick = { ConfigStore.resetOnboarding() }) {
                        Column(Modifier.padding(14.dp)) {
                            Icon(Icons.Filled.PlayCircle, null, tint = OnbGreen)
                            Text("Replay welcome tour", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Your apps stay connected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Panel(Modifier.weight(1f), tint = Accent, onClick = { Tips.resetAll(); ctx.toast("Tips will show again"); nav.root(Route.Dashboard) }) {
                        Column(Modifier.padding(14.dp)) {
                            Icon(Icons.Filled.Lightbulb, null, tint = Accent)
                            Text("Show all tips again", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Welcome card and screen tips", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item { SectionTitle("Getting around") }
            items(tipList) { t ->
                Panel(Modifier.fillMaxWidth(), tint = t.color, onClick = { t.go?.invoke(nav) }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(t.icon, null, tint = t.color); Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(t.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item { SectionTitle("Connecting each app") }
            items(ServiceType.entries.toList(), key = { it.name }) { t ->
                val expanded = open == t
                Panel(Modifier.fillMaxWidth(), tint = t.tint, onClick = { open = if (expanded) null else t }) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LogoTile(t, 36.dp); Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(t.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(t.blurb, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
                        }
                        if (expanded) {
                            Spacer(Modifier.height(8.dp))
                            GuideLine("Address", "http://<server-ip>:${t.defaultPort} at home, or your Tailscale / Cloudflare address as the remote address")
                            GuideLine("Login", when (t.auth) {
                                com.arrmada360.app.data.AuthKind.API_KEY -> "API key – ${t.keyHint}"
                                com.arrmada360.app.data.AuthKind.USER_PASS -> "Username and password – the ones you use for its web page"
                                com.arrmada360.app.data.AuthKind.PASSWORD_ONLY -> "Password – the one you use for its web page"
                                com.arrmada360.app.data.AuthKind.NONE -> "No login needed"
                            })
                            GuideLine("Test", "Tap Test connection before saving. Green means it works; red tells you what to fix.")
                            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!Family.active) Button(onClick = { nav.push(Route.EditService(null, t)) }) { Text("Add ${t.label}") }
                            }
                        }
                    }
                }
            }
            item { SectionTitle("More help") }
            item { MenuRow(Icons.Filled.MenuBook, "Full guide on GitHub", "README with screenshots and troubleshooting", Accent, external = true) { uri.openUri("https://github.com/${com.arrmada360.app.api.Updates.REPO}#readme") } }
            item { MenuRow(Icons.Filled.Wifi, "Connection check", "Test every app and see what's wrong", OnbGreen) { nav.push(Route.Health) } }
            item { MenuRow(Icons.Filled.Feedback, "Ask a question or report a bug", "Opens a new GitHub issue", Pink, external = true) { uri.openUri("https://github.com/${com.arrmada360.app.api.Updates.REPO}/issues/new") } }
        }
    }
}

@Composable
private fun GuideLine(label: String, text: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(64.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Suppress("unused") private val keepArrow = Icons.AutoMirrored.Filled.KeyboardArrowRight
