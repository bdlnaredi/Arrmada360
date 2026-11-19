package com.arrmada360.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.arrmada360.app.api.UpdateInfo
import com.arrmada360.app.api.Updates
import com.arrmada360.app.data.ConfigStore
import com.arrmada360.app.data.Pref
import com.arrmada360.app.net.friendlyError
import com.arrmada360.app.ui.Accent
import com.arrmada360.app.ui.Accent2
import com.arrmada360.app.ui.AppScaffold
import com.arrmada360.app.ui.Bad
import com.arrmada360.app.ui.Good
import com.arrmada360.app.ui.GradientButton
import com.arrmada360.app.ui.Navigator
import com.arrmada360.app.ui.Panel
import com.arrmada360.app.ui.Pill
import com.arrmada360.app.ui.Pink
import com.arrmada360.app.ui.ProgressBar
import com.arrmada360.app.ui.Rainbow
import com.arrmada360.app.ui.Route
import com.arrmada360.app.ui.Warn
import com.arrmada360.app.ui.fmtDate
import com.arrmada360.app.ui.humanBytes
import com.arrmada360.app.ui.rememberLoadable
import com.arrmada360.app.ui.toast
import kotlinx.coroutines.launch

/** Shared download/install state so the dialog and the Updates page show the same progress. */
private object UpdateState {
    var progress by mutableFloatStateOf(-1f) // -1 = idle
    var error by mutableStateOf<String?>(null)
}

@Composable
private fun rememberInstaller(): (UpdateInfo) -> Unit {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember {
        { info: UpdateInfo ->
            if (!Updates.canInstall(ctx)) {
                ctx.toast("Allow Arrmada360 to install apps, then tap Update again")
                Updates.openInstallPermission(ctx)
            } else if (UpdateState.progress < 0f) {
                UpdateState.error = null
                UpdateState.progress = 0f
                scope.launch {
                    try {
                        val file = Updates.download(ctx, info) { UpdateState.progress = it }
                        UpdateState.progress = -1f
                        Updates.install(ctx, file)
                    } catch (e: Exception) {
                        UpdateState.progress = -1f
                        UpdateState.error = friendlyError(e)
                        ctx.toast("Download failed: ${friendlyError(e)}")
                    }
                }
            }
        }
    }
}

// ============================================================ Updates page
@Composable
fun UpdatesScreen(nav: Navigator, back: () -> Unit) {
    val ctx = LocalContext.current
    val uri = LocalUriHandler.current
    val current = Updates.currentBuild(ctx)
    var refresh by remember { mutableStateOf(0) }
    val latest = rememberLoadable(refresh, "update-latest") { Updates.latest() }
    val log = rememberLoadable(refresh, "update-log") { Updates.changelog() }
    val install = rememberInstaller()
    AppScaffold(title = "Updates", subtitle = "Version ${Updates.currentVersionName(ctx)}", isRoot = false, onBack = back) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                val info = latest.data
                val newer = info != null && info.build > current
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(if (newer) listOf(Accent.copy(alpha = 0.5f), Accent2.copy(alpha = 0.45f), Pink.copy(alpha = 0.3f)) else listOf(Good.copy(alpha = 0.35f), Accent.copy(alpha = 0.2f))))
                        .padding(20.dp),
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                                Icon(if (newer) Icons.Filled.NewReleases else Icons.Filled.CheckCircle, null, tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    when {
                                        info == null && latest.error != null -> "Couldn't check for updates"
                                        info == null -> "Checking for updates…"
                                        newer -> "Build #${info.build} is available"
                                        else -> "You're up to date"
                                    },
                                    style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold,
                                )
                                Text("Installed: build #$current", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                                if (info != null) Text("Latest: build #${info.build} · ${humanBytes(info.size)} · ${fmtDate(info.published, true)}",
                                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                                if (info == null && latest.error != null) Text(latest.error ?: "", style = MaterialTheme.typography.bodySmall, color = Color.White)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        val p = UpdateState.progress
                        when {
                            p >= 0f -> {
                                Text("Downloading… ${(p * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp)); ProgressBar(p, color = Color.White, height = 8.dp)
                            }
                            newer && info != null -> GradientButton("Download & install build #${info.build}", Modifier.fillMaxWidth()) { install(info) }
                            else -> GradientButton("Check again", Modifier.fillMaxWidth()) { refresh++ }
                        }
                        UpdateState.error?.let { Text(it, color = Bad, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) }
                    }
                }
            }
            item {
                Column(Modifier.clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))) {
                    SettingSwitch("Check for updates automatically", "Look for a new build when the app opens (and in the background if notifications are on).",
                        ConfigStore.flag(Pref.AUTO_UPDATE_CHECK)) { ConfigStore.setFlag(Pref.AUTO_UPDATE_CHECK, it) }
                    SettingLink(Icons.Filled.SystemUpdate, "Full changelog", "Every change in every build", tint = Accent2) { nav.push(Route.Changelog) }
                    SettingLink(Icons.Filled.NewReleases, "Open release on GitHub", "github.com/${Updates.REPO}", tint = Accent, divider = false) {
                        uri.openUri(latest.data?.pageUrl ?: "https://github.com/${Updates.REPO}/releases/latest")
                    }
                }
            }
            val notes = log.data?.let { Updates.notesSince(it, current) }
            if (!notes.isNullOrBlank()) {
                item { Text("What's new since your version", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(start = 8.dp, top = 6.dp)) }
                item { Panel(Modifier.fillMaxWidth()) { Box(Modifier.padding(16.dp)) { MarkdownText(notes) } } }
            }
        }
    }
}

// ============================================================ Changelog viewer
@Composable
fun ChangelogScreen(back: () -> Unit) {
    val log = rememberLoadable("changelog") { Updates.changelog() }
    AppScaffold(title = "Changelog", isRoot = false, onBack = back) { pad ->
        com.arrmada360.app.ui.LoadableContent(log, Modifier.padding(pad)) { text ->
            val body = text.substringAfter("\n## ", text).let { if (it == text) text else "## $it" }
            LazyColumn(contentPadding = PaddingValues(12.dp)) {
                item { Panel(Modifier.fillMaxWidth()) { Box(Modifier.padding(16.dp)) { MarkdownText(body) } } }
            }
        }
    }
}

/** Renders the simple Markdown used in CHANGELOG.md: headings, bullets, **bold**, `code`. */
@Composable
fun MarkdownText(md: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        var sectionIndex = 0
        md.lines().forEach { raw ->
            val line = raw.trimEnd()
            when {
                line.isBlank() || line == "---" -> Spacer(Modifier.height(4.dp))
                line.startsWith("## ") -> {
                    val c = Rainbow[sectionIndex++ % Rainbow.size]
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(50)).background(c))
                        Spacer(Modifier.width(8.dp))
                        Text(line.removePrefix("## ").replace("[", "").replace("]", ""), style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }
                line.startsWith("### ") -> {
                    val t = line.removePrefix("### ")
                    Spacer(Modifier.height(6.dp))
                    Pill(t, when (t.lowercase()) { "added" -> Good; "fixed" -> Bad; "changed" -> Warn; else -> Accent })
                }
                line.startsWith("# ") -> {}
                line.trimStart().startsWith("- ") -> {
                    val indent = line.indexOf("- ")
                    Row(Modifier.padding(start = (8 + indent * 6).dp)) {
                        Text(if (indent > 0) "◦  " else "•  ", color = if (indent > 0) MaterialTheme.colorScheme.onSurfaceVariant else Accent)
                        Text(inline(line.substring(indent + 2)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                else -> Text(inline(line), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun inline(s: String) = buildAnnotatedString {
    var i = 0
    val text = s.replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end > 0) { withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) { append(text.substring(i + 2, end)) }; i = end + 2 } else { append(text[i]); i++ }
            }
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end > 0) { withStyle(SpanStyle(color = Accent, fontWeight = FontWeight.SemiBold)) { append(text.substring(i + 1, end)) }; i = end + 1 } else { append(text[i]); i++ }
            }
            text[i] == '*' -> {
                val end = text.indexOf('*', i + 1)
                if (end > 0) { withStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) { append(text.substring(i + 1, end)) }; i = end + 1 } else { append(text[i]); i++ }
            }
            else -> { append(text[i]); i++ }
        }
    }
}

// ============================================================ automatic check on launch
@Composable
fun UpdateGate(nav: Navigator) {
    val ctx = LocalContext.current
    var found by remember { mutableStateOf<Pair<UpdateInfo, String>?>(null) }
    val install = rememberInstaller()
    LaunchedEffect(Unit) {
        if (!ConfigStore.flag(Pref.AUTO_UPDATE_CHECK)) return@LaunchedEffect
        val last = ConfigStore.getString("lastUpdateCheck").toLongOrNull() ?: 0L
        if (System.currentTimeMillis() - last < 6 * 3600_000L) return@LaunchedEffect
        ConfigStore.putString("lastUpdateCheck", System.currentTimeMillis().toString())
        try {
            val info = Updates.latest()
            val current = Updates.currentBuild(ctx)
            if (info.build > current && ConfigStore.getInt("skipBuild", 0) != info.build) {
                val notes = try { Updates.notesSince(Updates.changelog(), current) } catch (e: Exception) { "" }
                found = info to notes
            }
        } catch (e: Exception) { /* offline – try next time */ }
    }
    val f = found ?: return
    val (info, notes) = f
    AlertDialog(
        onDismissRequest = { found = null },
        icon = { Icon(Icons.Filled.NewReleases, null, tint = Accent) },
        title = { Text("Update available – build #${info.build}", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text("You have build #${Updates.currentBuild(ctx)}. Download ${humanBytes(info.size)} and install?", style = MaterialTheme.typography.bodyMedium)
                val p = UpdateState.progress
                if (p >= 0f) { Spacer(Modifier.height(10.dp)); Text("Downloading… ${(p * 100).toInt()}%"); ProgressBar(p, height = 8.dp) }
                if (notes.isNotBlank()) { Spacer(Modifier.height(12.dp)); MarkdownText(notes) }
            }
        },
        confirmButton = { GradientButton(if (UpdateState.progress >= 0f) "Downloading…" else "Update now", enabled = UpdateState.progress < 0f) { install(info) } },
        dismissButton = {
            Row {
                TextButton(onClick = { ConfigStore.putInt("skipBuild", info.build); found = null }) { Text("Skip this build") }
                TextButton(onClick = { found = null; nav.push(Route.Updates) }) { Text("Later") }
            }
        },
    )
}
