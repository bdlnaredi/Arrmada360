package com.arrmada360.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrmada360.app.R
import com.arrmada360.app.api.Updates
import com.arrmada360.app.data.ConfigStore
import com.arrmada360.app.data.ServiceType
import com.arrmada360.app.ui.*
import kotlinx.coroutines.delay

/** Brand green used across the first-run flow. */
val OnbGreen = Color(0xFF5CC264)

/** The services we offer in onboarding (everything except adult-only tools, which stay in Add service). */
private val onboardTypes: List<ServiceType> get() = ServiceType.entries.filter { !it.privateArt }

@Composable
fun Onboarding() {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var chosenNames by rememberSaveable { mutableStateOf(listOf<String>()) }
    val chosen = chosenNames.mapNotNull { n -> ServiceType.entries.firstOrNull { it.name == n } }
    fun finish() {
        ConfigStore.putStringSet("setupPending", chosen.filter { t -> ConfigStore.services.none { it.type == t } }.map { it.name }.toSet())
        ConfigStore.finishOnboarding()
    }
    BackHandler(step > 0) { step = (step - 1).coerceAtLeast(0).let { if (it == 2) 1 else it } }
    AnimatedContent(step, transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(250)) }, label = "onb") { s ->
        when (s) {
            0 -> OnbWelcome(onStart = { step = 1 }, onSkip = { finish() })
            1 -> OnbServices(chosen.toSet(), onBack = { step = 0 }, onSkip = { finish() }) { picked ->
                chosenNames = picked.map { it.name }; step = 2
            }
            2 -> OnbPreparing { step = 3 }
            else -> OnbAllSet(chosen, onBack = { step = 1 }, onDone = { finish() })
        }
    }
}

// ------------------------------------------------------------ shared bits
@Composable
private fun OnbHeader(fraction: Float, label: String, onBack: () -> Unit) {
    val anim by animateFloatAsState(fraction, tween(600), label = "prog")
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 4.dp, end = 20.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(anim).clip(RoundedCornerShape(50)).background(OnbGreen))
        }
        Spacer(Modifier.width(16.dp))
        Text(label.uppercase(), style = MaterialTheme.typography.labelLarge, letterSpacing = 2.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BigGreenButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled, shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = OnbGreen, contentColor = Color(0xFF0E1A10)),
        modifier = modifier.fillMaxWidth().height(62.dp),
    ) { Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
}

@Composable
private fun SkipLink(text: String, onClick: () -> Unit) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).clickable(onClick = onClick).padding(14.dp))
}

/** Logo inside a soft rounded tile, like the reference. */
@Composable
fun LogoTile(t: ServiceType, size: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(size * 0.26f)).background(t.tint.copy(alpha = 0.16f).compositeOverSurface()), contentAlignment = Alignment.Center) {
        ServiceLogo(t, size * 0.66f)
    }
}

@Composable
private fun Color.compositeOverSurface(): Color = androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.surface, this.copy(alpha = 1f), this.alpha)

// ------------------------------------------------------------ 1. welcome
@Composable
private fun OnbWelcome(onStart: () -> Unit, onSkip: () -> Unit) {
    val types = onboardTypes
    val t = rememberInfiniteTransition(label = "float")
    val drift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), label = "drift")
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // floating, faded logo wallpaper on a green glow
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.62f).background(Brush.verticalGradient(listOf(OnbGreen.copy(alpha = 0.55f), OnbGreen.copy(alpha = 0.18f), Color.Transparent)))) {
            val cols = 5
            Column(Modifier.fillMaxSize().padding(top = 30.dp), verticalArrangement = Arrangement.SpaceEvenly) {
                for (r in 0 until 4) {
                    Row(Modifier.fillMaxWidth().graphicsLayer { translationX = ((if (r % 2 == 0) drift else 1 - drift) - 0.5f) * 90f }, horizontalArrangement = Arrangement.SpaceEvenly) {
                        for (c in 0 until cols) {
                            val ty = types[(r * cols + c + r) % types.size]
                            Box(Modifier.alpha(0.22f - r * 0.035f).size(86.dp).clip(CircleShape).background(ty.tint.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                                ServiceLogo(ty, 58.dp)
                            }
                        }
                    }
                }
            }
        }
        Column(Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(0.9f))
            Box(Modifier.size(150.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_launcher_foreground), null, tint = Color.Unspecified, modifier = Modifier.size(150.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(buildAnnotatedString {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) { append("Arrmada") }
                withStyle(SpanStyle(color = OnbGreen)) { append("360") }
            }, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text("One app for your entire media stack", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(top = 10.dp))
            Text("Torrents, usenet, the *arrs, requests, Plex stats and your Unraid server – all in one app, every feature free.",
                style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
            Spacer(Modifier.height(22.dp))
            types.chunked(7).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) { row.forEach { LogoTile(it, 42.dp) } }
            }
            Text("${types.size} native integrations + universal search & calendars", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
            Spacer(Modifier.weight(1f))
            BigGreenButton("Get started", onClick = onStart)
            SkipLink("Skip — I'll set up later", onSkip)
        }
    }
}

// ------------------------------------------------------------ 2. pick services
@Composable
private fun OnbServices(initial: Set<ServiceType>, onBack: () -> Unit, onSkip: () -> Unit, onNext: (List<ServiceType>) -> Unit) {
    var sel by remember { mutableStateOf(initial) }
    val types = onboardTypes
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding()) {
        OnbHeader(0.45f, "Your services", onBack)
        LazyVerticalGrid(GridCells.Adaptive(104.dp), Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text("Which of these do you use?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text("Choose what you run and Arrmada360 will prepare your starting experience. You can add more at any time.",
                        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                    Row(Modifier.padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${sel.size} selected", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Text(if (sel.isEmpty()) "Select all" else "Clear all", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = OnbGreen,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { sel = if (sel.isEmpty()) types.toSet() else emptySet() }.padding(6.dp))
                    }
                }
            }
            items(types, key = { it.name }) { t ->
                val on = t in sel
                val scale by animateFloatAsState(if (on) 1f else 0.96f, spring(dampingRatio = 0.5f), label = "tile")
                Box(
                    Modifier.graphicsLayer { scaleX = scale; scaleY = scale }.clip(RoundedCornerShape(22.dp))
                        .background(if (on) OnbGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface)
                        .border(2.dp, if (on) OnbGreen else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                        .clickable { sel = if (on) sel - t else sel + t }.padding(vertical = 16.dp, horizontal = 6.dp),
                ) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        LogoTile(t, 56.dp)
                        Text(t.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1,
                            color = if (on) OnbGreen else MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 10.dp))
                    }
                    if (on) Box(Modifier.align(Alignment.TopEnd).size(22.dp).clip(CircleShape).background(OnbGreen), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Check, null, tint = Color(0xFF0E1A10), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        Column(Modifier.padding(horizontal = 20.dp)) {
            BigGreenButton(if (sel.isEmpty()) "Continue" else "Continue with ${sel.size}") { onNext(types.filter { it in sel }) }
            SkipLink("Skip — I'll set everything up later", onSkip)
        }
    }
}

// ------------------------------------------------------------ 3. preparing
@Composable
private fun OnbPreparing(onDone: () -> Unit) {
    val uri = LocalUriHandler.current
    var p by remember { mutableFloatStateOf(0f) }
    val stages = listOf("Preparing your dashboard…", "Loading app logos…", "Setting up notifications…", "Finishing up…")
    LaunchedEffect(Unit) {
        while (p < 1f) { delay(60); p = (p + 0.012f).coerceAtMost(1f) }
        delay(500); onDone()
    }
    val anim by animateFloatAsState(p, label = "p")
    val pulse = rememberInfiniteTransition(label = "pulse")
    val rot by pulse.animateFloat(0f, 360f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "rot")
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()
        .verticalScroll(rememberScrollState()).padding(horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(40.dp))
        Box(Modifier.size(64.dp).graphicsLayer { rotationZ = rot }.clip(RoundedCornerShape(40)).background(OnbGreen))
        Text("Getting things set up for you", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(top = 26.dp))
        Text(stages[(p * stages.size).toInt().coerceAtMost(stages.size - 1)], style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        Box(Modifier.padding(top = 18.dp).width(200.dp).height(8.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(anim).clip(RoundedCornerShape(50)).background(OnbGreen))
        }
        HorizontalDivider(Modifier.padding(vertical = 28.dp))
        Text("WHILE YOU WAIT — A LITTLE ABOUT ARRMADA360", style = MaterialTheme.typography.labelLarge, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FactTile("${ServiceType.entries.size}", "apps supported", Modifier.weight(1f))
            FactTile("100%", "free, no ads", Modifier.weight(1f))
            FactTile("0", "paywalls", Modifier.weight(1f))
        }
        Row(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(OnbGreen.copy(alpha = 0.14f)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(OnbGreen.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.LockOpen, null, tint = OnbGreen)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(buildAnnotatedString { append("Every feature "); withStyle(SpanStyle(color = OnbGreen)) { append("unlocked") } },
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text("Widgets, Unraid, RSS, notifications – no credits or subscriptions.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("Built around your feedback", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp))
        Text("Tell us what's working, what's missing and what you'd like next – updates land in the app automatically.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        FeedbackLine(Icons.Filled.Code, "GitHub", "Source code, releases and the changelog") { uri.openUri("https://github.com/${Updates.REPO}") }
        FeedbackLine(Icons.Filled.BugReport, "Report an issue", "Bugs and feature requests on GitHub") { uri.openUri("https://github.com/${Updates.REPO}/issues/new") }
        FeedbackLine(Icons.Filled.Feedback, "In-app feedback", "Settings → Help → Send feedback, any time") {}
        FeedbackLine(Icons.Filled.SystemUpdate, "In-app updates", "New builds install from Settings → Updates") {}
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun FactTile(big: String, small: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
        .background(MaterialTheme.colorScheme.surface).padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(big, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(small, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FeedbackLine(icon: ImageVector, title: String, sub: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 14.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = OnbGreen, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ------------------------------------------------------------ 4. all set
@Composable
private fun OnbAllSet(chosen: List<ServiceType>, onBack: () -> Unit, onDone: () -> Unit) {
    var editing by remember { mutableStateOf<ServiceType?>(null) }
    val e = editing
    if (e != null) {
        ServiceEditorScreen(type = e, existing = null, onBack = { editing = null }, onSaved = { editing = null }, skipLabel = null)
        return
    }
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 300f)) }
    val connected = chosen.filter { t -> ConfigStore.services.any { it.type == t } }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding()) {
        OnbHeader(1f, "All set", onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.padding(top = 20.dp).size(110.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }.clip(CircleShape)
                        .background(OnbGreen.copy(alpha = 0.18f)).border(3.dp, OnbGreen, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Check, null, tint = OnbGreen, modifier = Modifier.size(60.dp))
                    }
                    Text("You're all set", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 22.dp))
                    Text(if (chosen.isEmpty()) "Start exploring – you can add your apps any time from the menu or Settings."
                        else "Connect your apps now, or start exploring first – you can connect them any time later.",
                        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp, bottom = 10.dp))
                }
            }
            items(chosen, key = { it.name }) { t ->
                val done = t in connected
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    LogoTile(t, 46.dp)
                    Spacer(Modifier.width(14.dp))
                    Text(t.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1)
                    if (done) Row(Modifier.clip(RoundedCornerShape(50)).background(OnbGreen).padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Check, null, tint = Color(0xFF0E1A10), modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp))
                        Text("Connected", color = Color(0xFF0E1A10), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    } else Text("Connect your instance", color = OnbGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(OnbGreen.copy(alpha = 0.16f)).clickable { editing = t }.padding(horizontal = 14.dp, vertical = 9.dp))
                }
            }
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            BigGreenButton(when {
                chosen.isEmpty() || connected.size == chosen.size -> "Start using Arrmada360"
                connected.isEmpty() -> "Explore now, I'll connect these later"
                else -> "Continue – connect the rest later"
            }, onClick = onDone)
        }
    }
}
