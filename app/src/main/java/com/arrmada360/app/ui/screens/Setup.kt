package com.arrmada360.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import com.arrmada360.app.ui.Accent
import com.arrmada360.app.ui.Accent2
import com.arrmada360.app.ui.AppBackground
import com.arrmada360.app.ui.GradientButton
import com.arrmada360.app.ui.Pink
import com.arrmada360.app.ui.Warn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrmada360.app.R
import com.arrmada360.app.api.ServiceTester
import com.arrmada360.app.data.AuthKind
import com.arrmada360.app.data.Category
import com.arrmada360.app.data.ConfigStore
import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.data.ServiceType
import com.arrmada360.app.ui.AppScaffold
import com.arrmada360.app.ui.Bad
import com.arrmada360.app.ui.Good
import com.arrmada360.app.ui.ServiceBadge
import com.arrmada360.app.ui.ServiceTheme
import kotlinx.coroutines.launch

// ============================================================ onboarding flow
@Composable
fun Onboarding() {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var chosen by remember { mutableStateOf(listOf<ServiceType>()) }
    var index by rememberSaveable { mutableIntStateOf(0) }

    when (step) {
        0 -> WelcomeScreen { step = 1 }
        1 -> ServicePickerScreen(
            title = "Which apps do you use?",
            subtitle = "Pick everything you run. You can add more later.",
            multi = true,
            initial = chosen.toSet(),
            onBack = { step = 0 },
            onDone = { list ->
                chosen = list
                index = 0
                if (list.isEmpty()) { ConfigStore.finishOnboarding() } else step = 2
            },
        )
        else -> {
            val type = chosen.getOrNull(index)
            if (type == null) {
                androidx.compose.runtime.LaunchedEffect(Unit) { ConfigStore.finishOnboarding() }
            } else {
                val next: () -> Unit = {
                    if (index + 1 >= chosen.size) { ConfigStore.finishOnboarding() } else { index += 1 }
                }
                ServiceEditorScreen(
                    type = type,
                    existing = null,
                    progress = "${index + 1} of ${chosen.size}",
                    onBack = { if (index > 0) index-- else step = 1 },
                    onSaved = { next() },
                    skipLabel = "Skip",
                    onSkip = { next() },
                )
            }
        }
    }
}

@Composable
private fun WelcomeScreen(onStart: () -> Unit) {
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "welcome")
    val bob by t.animateFloat(
        0f, 1f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(2200), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "bob",
    )
    val spin by t.animateFloat(
        0f, 360f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(14000, easing = androidx.compose.animation.core.LinearEasing)),
        label = "spin",
    )
    AppBackground {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(170.dp).graphicsLayer { rotationZ = spin }.clip(CircleShape)
                        .background(Brush.sweepGradient(listOf(Accent, Accent2, Pink, Warn, Accent))),
                )
                Box(Modifier.size(158.dp).clip(CircleShape).background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_launcher_foreground), null, tint = Color.Unspecified, modifier = Modifier.size(170.dp))
                }
            }
            Spacer(Modifier.height(28.dp))
            Text("Arrmada360", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(
                "Your whole media server in one app.\nEvery feature unlocked — free, forever.",
                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(30.dp))
            val rows = ServiceType.entries.chunked(5)
            rows.forEachIndexed { r, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 5.dp)) {
                    row.forEachIndexed { i, type ->
                        val phase = ((i + r) % 2) * 2 - 1
                        Box(Modifier.graphicsLayer { translationY = (bob - 0.5f) * 14f * phase }) { ServiceBadge(type, 46.dp) }
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
            GradientButton("Get started", Modifier.fillMaxWidth().height(56.dp), onClick = onStart)
        }
    }
}

// ============================================================ service picker grid
@Composable
fun ServicePickerScreen(
    title: String,
    subtitle: String,
    multi: Boolean,
    initial: Set<ServiceType>,
    onBack: () -> Unit,
    onDone: (List<ServiceType>) -> Unit,
) {
    var selected by remember { mutableStateOf(initial) }
    AppScaffold(title = title, subtitle = subtitle, isRoot = false, onBack = onBack) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Category.entries.forEach { cat ->
                    val types = ServiceType.entries.filter { it.category == cat }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(cat.title.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 10.dp, start = 4.dp))
                    }
                    types.forEach { t ->
                        item {
                            val on = t in selected
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (on) Brush.linearGradient(listOf(t.tint.copy(alpha = 0.34f), t.tint2.copy(alpha = 0.14f))) else Brush.linearGradient(listOf(t.tint.copy(alpha = 0.08f).compositeOver(MaterialTheme.colorScheme.surface), MaterialTheme.colorScheme.surface)))
                                    .border(BorderStroke(if (on) 2.dp else 1.dp, if (on) t.tint else t.tint.copy(alpha = 0.18f)), RoundedCornerShape(14.dp))
                                    .clickable {
                                        if (multi) selected = if (on) selected - t else selected + t
                                        else onDone(listOf(t))
                                    }
                                    .padding(12.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ServiceBadge(t, 46.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(t.label, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                        Text(t.blurb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                }
                                if (on) Icon(Icons.Filled.CheckCircle, null, tint = t.tint, modifier = Modifier.align(Alignment.TopEnd).size(18.dp))
                            }
                        }
                    }
                }
            }
            if (multi) {
                GradientButton(
                    if (selected.isEmpty()) "Skip for now" else "Continue with ${selected.size} app${if (selected.size == 1) "" else "s"}",
                    Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding().height(56.dp),
                ) { onDone(ServiceType.entries.filter { it in selected }) }
            }
        }
    }
}

// ============================================================ connection editor
@Composable
fun ServiceEditorScreen(
    type: ServiceType,
    existing: ServiceConfig?,
    onBack: () -> Unit,
    onSaved: (ServiceConfig) -> Unit,
    skipLabel: String?,
    onSkip: () -> Unit = {},
    progress: String? = null,
) {
    // key everything on the type so the form resets when onboarding moves to the next app
    val k = existing?.id ?: type.name
    var name by remember(k) { mutableStateOf(existing?.name ?: type.label) }
    var url by remember(k) { mutableStateOf(existing?.url ?: "") }
    var apiKey by remember(k) { mutableStateOf(existing?.apiKey ?: "") }
    var user by remember(k) { mutableStateOf(existing?.username ?: if (type == ServiceType.QBITTORRENT) "admin" else if (type == ServiceType.NZBGET) "nzbget" else "") }
    var pass by remember(k) { mutableStateOf(existing?.password ?: "") }
    var trust by remember(k) { mutableStateOf(existing?.trustAllCerts ?: false) }
    var show by remember(k) { mutableStateOf(false) }
    var testing by remember(k) { mutableStateOf(false) }
    var result by remember(k) { mutableStateOf<Pair<Boolean, String>?>(null) }
    val scope = rememberCoroutineScope()

    fun build() = ServiceConfig(
        id = existing?.id ?: java.util.UUID.randomUUID().toString(),
        type = type, name = name.trim().ifEmpty { type.label }, url = url.trim(),
        apiKey = apiKey.trim(), username = user.trim(), password = pass, trustAllCerts = trust,
    )

    ServiceTheme(type, ConfigStore.amoled) {
        AppScaffold(
            title = if (existing != null) "Edit ${existing.name}" else "Set up ${type.label}",
            subtitle = progress, isRoot = false, onBack = onBack, badge = type, badgeName = name,
            actions = { if (skipLabel != null) TextButton(onClick = onSkip) { Text(skipLabel) } },
        ) { pad ->
            Column(
                Modifier.fillMaxSize().padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(type.tint.copy(alpha = 0.30f), type.tint2.copy(alpha = 0.10f)))).padding(16.dp)) {
                    ServiceBadge(type, 64.dp, name)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(type.label, style = MaterialTheme.typography.titleLarge)
                        Text("Default port ${type.defaultPort} · ${type.blurb}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedTextField(name, { name = it }, label = { Text("Display name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    url, { url = it }, label = { Text("Address") },
                    placeholder = { Text("http://192.168.1.10:${type.defaultPort}") },
                    supportingText = { Text("IP or domain with port. Include a URL base if you use one, e.g. /sonarr") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                when (type.auth) {
                    AuthKind.API_KEY -> OutlinedTextField(
                        apiKey, { apiKey = it }, label = { Text("API key") }, singleLine = true,
                        supportingText = { Text(type.keyHint) }, modifier = Modifier.fillMaxWidth(),
                    )
                    AuthKind.USER_PASS -> {
                        OutlinedTextField(user, { user = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        PasswordField(pass, { pass = it }, show, { show = !show }, type.keyHint)
                    }
                    AuthKind.PASSWORD_ONLY -> PasswordField(pass, { pass = it }, show, { show = !show }, type.keyHint)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { trust = !trust }) {
                    Checkbox(trust, { trust = it })
                    Text("Allow self-signed HTTPS certificates", style = MaterialTheme.typography.bodyMedium)
                }
                result?.let { (ok, msg) ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background((if (ok) Good else Bad).copy(alpha = 0.14f)).padding(12.dp),
                    ) { Text((if (ok) "✓ Connected: " else "✗ ") + msg, color = if (ok) Good else Bad, style = MaterialTheme.typography.bodySmall) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            testing = true; result = null
                            scope.launch {
                                result = try { true to ServiceTester.test(build()) } catch (e: Exception) { false to com.arrmada360.app.net.friendlyError(e) }
                                testing = false
                            }
                        },
                        enabled = url.isNotBlank() && !testing, modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        if (testing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Test")
                    }
                    GradientButton("Save", Modifier.weight(1f).height(48.dp), enabled = url.isNotBlank()) {
                        val c = build(); ConfigStore.upsert(c)
                        scope.launch { com.arrmada360.app.api.Connections.check(c) }
                        onSaved(c)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, show: Boolean, toggle: () -> Unit, hint: String) {
    OutlinedTextField(
        value, onChange, label = { Text("Password") }, singleLine = true,
        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = { IconButton(onClick = toggle) { Icon(if (show) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null) } },
        supportingText = { Text(hint) }, modifier = Modifier.fillMaxWidth(),
    )
}
