package com.arrmada360.app.ui.screens

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.NewReleases
import com.arrmada360.app.ui.rememberLoadable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.arrmada360.app.R
import com.arrmada360.app.api.Connections
import com.arrmada360.app.api.DownloadClients
import com.arrmada360.app.data.ConfigStore
import com.arrmada360.app.data.Pref
import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.notify.Notifier
import com.arrmada360.app.ui.Accent
import com.arrmada360.app.ui.Accent2
import com.arrmada360.app.ui.AppScaffold
import com.arrmada360.app.ui.Bad
import com.arrmada360.app.ui.Good
import com.arrmada360.app.ui.GradientButton
import com.arrmada360.app.ui.Navigator
import com.arrmada360.app.ui.Pink
import com.arrmada360.app.ui.Pill
import com.arrmada360.app.ui.Rainbow
import com.arrmada360.app.ui.Route
import com.arrmada360.app.ui.ServiceBadge
import com.arrmada360.app.ui.Warn
import com.arrmada360.app.ui.copyText
import com.arrmada360.app.ui.toast
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

// ============================================================ building blocks (nzb360-style grouped cards)
@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title.uppercase(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 10.dp),
    )
    Column(
        Modifier.padding(horizontal = 14.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)),
        content = content,
    )
}

@Composable
private fun GroupDivider() = HorizontalDivider(Modifier.padding(start = 20.dp), color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f))

@Composable
private fun RowIcon(icon: ImageVector, tint: Color) {
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun SettingSwitch(title: String, subtitle: String?, checked: Boolean, divider: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            if (!subtitle.isNullOrEmpty()) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked, onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Good, checkedThumbColor = Color(0xFF111318), uncheckedTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        )
    }
    if (divider) GroupDivider()
}

@Composable
fun SettingLink(
    icon: ImageVector?,
    title: String,
    subtitle: String? = null,
    value: String? = null,
    tint: Color = Good,
    divider: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) { leading(); Spacer(Modifier.width(16.dp)) }
        else if (icon != null) { RowIcon(icon, tint); Spacer(Modifier.width(16.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrEmpty()) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        if (value != null) Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = tint, modifier = Modifier.padding(start = 8.dp))
    }
    if (divider) GroupDivider()
}

@Composable
private fun SettingsPage(title: String, back: () -> Unit, isRoot: Boolean = false, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    AppScaffold(title = title, isRoot = isRoot, onBack = back) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(bottom = 48.dp), content = content)
    }
}

fun appVersion(ctx: Context): Pair<String, Long> = try {
    val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
    (pi.versionName ?: "?") to PackageInfoCompat.getLongVersionCode(pi)
} catch (e: Exception) { "?" to 0L }

// ============================================================ settings lock
@Composable
fun SettingsGate(back: () -> Unit, content: @Composable () -> Unit) {
    if (!ConfigStore.flag(Pref.AUTH_SETTINGS) || ConfigStore.settingsUnlocked) { content(); return }
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) ConfigStore.settingsUnlocked = true
    }
    fun ask() {
        val km = ctx.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        @Suppress("DEPRECATION")
        val intent = km.createConfirmDeviceCredentialIntent("Unlock Arrmada360 settings", "Use your fingerprint, face, PIN or pattern")
        if (intent == null) { ConfigStore.settingsUnlocked = true; ctx.toast("No screen lock set on this phone – settings unlocked") }
        else launcher.launch(intent)
    }
    LaunchedEffect(Unit) { ask() }
    AppScaffold(title = "Settings locked", isRoot = false, onBack = back) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(110.dp).clip(RoundedCornerShape(32.dp)).background(Brush.linearGradient(listOf(Accent.copy(alpha = 0.4f), Accent2.copy(alpha = 0.3f)))), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Fingerprint, null, tint = Color.White, modifier = Modifier.size(60.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("Authenticate to open settings", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(Modifier.height(16.dp))
            GradientButton("Unlock") { ask() }
        }
    }
}

// ============================================================ main settings (nzb360-like layout, Arrmada360 style)
@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.padding(horizontal = 14.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)),
        content = content,
    )
}

@Composable
private fun GroupTitle(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text.uppercase(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color,
        modifier = Modifier.padding(start = 22.dp, top = 26.dp, bottom = 10.dp))
}

/** App tile in the 2-column grid: logo, name, chevron. */
@Composable
private fun AppGridTile(c: ServiceConfig, modifier: Modifier, onClick: () -> Unit, onLong: () -> Unit) {
    val failing = Connections.of(c.id) is com.arrmada360.app.api.Conn.Fail
    Row(
        modifier.height(66.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f))
            .combinedClickable(onClick = onClick, onLongClick = onLong).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            com.arrmada360.app.ui.ServiceLogo(c.type, 30.dp, c.name)
        }
        Spacer(Modifier.width(12.dp))
        Text(c.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (failing) Box(Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Bad))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

/** Dashed "add" tile. */
@Composable
private fun AddTile(label: String, modifier: Modifier, onClick: () -> Unit) {
    val c = Good
    Row(
        modifier.height(66.dp).clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick)
            .drawBehind {
                drawRoundRect(
                    c.copy(alpha = 0.7f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 1.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
                    ),
                )
            }.padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Add, null, tint = c)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = c, maxLines = 1)
    }
}

@Composable
private fun TileGrid(apps: List<ServiceConfig>, addLabel: String, onOpen: (ServiceConfig) -> Unit, onLong: (ServiceConfig) -> Unit, onAdd: () -> Unit) {
    val cells: List<ServiceConfig?> = apps + listOf(null)
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        cells.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { c ->
                    if (c == null) AddTile(addLabel, Modifier.weight(1f), onAdd)
                    else AppGridTile(c, Modifier.weight(1f), { onOpen(c) }, { onLong(c) })
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun SettingsScreen(nav: Navigator, isRoot: Boolean, back: () -> Unit) {
    val ctx = LocalContext.current
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    var manage by remember { mutableStateOf<ServiceConfig?>(null) }
    var startupPick by remember { mutableStateOf(false) }
    var crashText by remember { mutableStateOf<String?>(null) }
    val (ver, build) = appVersion(ctx)
    val latest = rememberLoadable("settings-latest") { try { com.arrmada360.app.api.Updates.latest() } catch (e: Exception) { null } }
    val updateReady = (latest.data?.build ?: 0) > build
    val apps = ConfigStore.services.filter { !it.type.isIndexer }
    val indexers = ConfigStore.services.filter { it.type.isIndexer }
    val online = ConfigStore.services.count { Connections.of(it.id) is com.arrmada360.app.api.Conn.Ok }
    val startupId = ConfigStore.getString("startupService")
    val startupName = ConfigStore.get(startupId)?.name ?: "Dashboard"

    AppScaffold(
        title = "", isRoot = isRoot, onBack = back,
        titleContent = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(end = 12.dp)) {
                Text("Settings", style = MaterialTheme.typography.headlineMedium, color = Color.White, modifier = Modifier.weight(1f))
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { nav.push(if (updateReady) Route.Updates else Route.Changelog) }
                        .padding(start = 8.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box {
                        Icon(painterResource(R.drawable.ic_launcher_foreground), null, tint = Color.Unspecified, modifier = Modifier.size(36.dp))
                        if (updateReady) Box(Modifier.align(Alignment.TopEnd).size(10.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Pink))
                    }
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(if (updateReady) "Update ready" else "What's New", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("v$ver", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Accent)
                    }
                }
            }
        },
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
            item {
                GroupTitle("App settings")
                SettingsCard {
                    SettingLink(Icons.Filled.Tune, "General", tint = MaterialTheme.colorScheme.onSurface) { nav.push(Route.SettingsGeneral) }
                    SettingLink(Icons.Filled.Notifications, "Notifications", if (ConfigStore.flag(Pref.NOTIFY)) "On" else null, tint = MaterialTheme.colorScheme.onSurface) { nav.push(Route.SettingsNotifications) }
                    SettingLink(Icons.Filled.CloudUpload, "Backup & restore", tint = MaterialTheme.colorScheme.onSurface) { nav.push(Route.SettingsBackup) }
                    SettingLink(Icons.Filled.Palette, "Appearance", tint = MaterialTheme.colorScheme.onSurface) { nav.push(Route.SettingsAppearance) }
                    SettingLink(Icons.Filled.Explore, "Discovery sources", "Trakt, TMDB and Seerr", tint = MaterialTheme.colorScheme.onSurface) { nav.push(Route.SettingsDiscovery) }
                    SettingLink(Icons.Filled.SystemUpdate, "Updates", if (updateReady) "Build #${latest.data?.build} is ready to install" else "Build #$build · up to date", tint = MaterialTheme.colorScheme.onSurface, divider = false) { nav.push(Route.Updates) }
                }
            }
            item {
                GroupTitle("Your server", Good)
                Column(
                    Modifier.padding(horizontal = 14.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp))
                        .background(Brush.linearGradient(listOf(Good.copy(alpha = 0.22f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)))),
                ) {
                    Row(Modifier.padding(start = 22.dp, end = 18.dp, top = 20.dp, bottom = 12.dp), verticalAlignment = Alignment.Bottom) {
                        Text("$online", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = Color.White)
                        Text(" / ${ConfigStore.services.size} apps online", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp).weight(1f))
                        Row(
                            Modifier.clip(RoundedCornerShape(50)).background(Good.copy(alpha = 0.85f)).clickable { nav.push(Route.Health) }.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) { Icon(Icons.Filled.Wifi, null, tint = Color.White, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Test", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                    Text("Every feature is free – no credits, no subscriptions.", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 22.dp, bottom = 12.dp))
                    GroupDivider()
                    SettingLink(Icons.Filled.Wifi, "Connection check", "Test every app and fix what's offline", tint = Good) { nav.push(Route.Health) }
                    SettingLink(Icons.Filled.Sync, "Connection switching", com.arrmada360.app.net.NetSwitch.describe(), tint = Accent, divider = false) { nav.push(Route.SettingsConnection) }
                }
            }
            item {
                GroupTitle("Services")
                SettingsCard {
                    Row(Modifier.fillMaxWidth().clickable { startupPick = true }.padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        RowIcon(Icons.Filled.RocketLaunch, Good)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("STARTUP SCREEN", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                            Text(startupName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    GroupDivider()
                    TileGrid(apps, "Add a service", onOpen = { nav.push(Route.EditService(it.id, null)) }, onLong = { manage = it }, onAdd = { nav.push(Route.AddService) })
                }
                Text("Tap an app to edit it, long-press to reorder or remove.", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 22.dp, top = 8.dp))
            }
            item {
                GroupTitle("Indexers")
                SettingsCard {
                    TileGrid(indexers, "Add an indexer", onOpen = { nav.push(Route.EditService(it.id, null)) }, onLong = { manage = it },
                        onAdd = { nav.push(Route.EditService(null, com.arrmada360.app.data.ServiceType.PROWLARR)) })
                }
            }
            item {
                GroupTitle("Help")
                SettingsCard {
                    SettingLink(Icons.Filled.HelpOutline, "Help center", "Setup guide, API keys, troubleshooting", tint = MaterialTheme.colorScheme.onSurface) {
                        uri.openUri("https://github.com/${com.arrmada360.app.api.Updates.REPO}#readme")
                    }
                    SettingLink(Icons.Filled.Article, "Logging center", "Last crash log on this phone", tint = MaterialTheme.colorScheme.onSurface) {
                        val f = File(ctx.filesDir, "crash.txt"); crashText = if (f.exists()) f.readText() else ""
                    }
                    SettingLink(Icons.Filled.Feedback, "Send feedback", "Report a bug or ask for a feature on GitHub", tint = MaterialTheme.colorScheme.onSurface, divider = false) {
                        uri.openUri("https://github.com/${com.arrmada360.app.api.Updates.REPO}/issues/new")
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.Center) {
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF24292F)).clickable { uri.openUri("https://github.com/${com.arrmada360.app.api.Updates.REPO}") }
                            .padding(horizontal = 22.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) { Icon(Icons.Filled.Code, null, tint = Color.White); Spacer(Modifier.width(8.dp)); Text("GitHub", color = Color.White, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(12.dp))
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(Accent2.copy(alpha = 0.85f)).clickable { nav.push(Route.Changelog) }
                            .padding(horizontal = 22.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) { Icon(Icons.Filled.NewReleases, null, tint = Color.White); Spacer(Modifier.width(8.dp)); Text("Changelog", color = Color.White, fontWeight = FontWeight.Bold) }
                }
                Text("Privacy: nothing leaves your phone except to your own servers →", style = MaterialTheme.typography.labelLarge, color = Good, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 18.dp)
                        .clickable { uri.openUri("https://github.com/${com.arrmada360.app.api.Updates.REPO}#privacy") })
                TextButton(onClick = { ConfigStore.resetOnboarding() }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("Run first-time setup again", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (startupPick) AlertDialog(
        onDismissRequest = { startupPick = false },
        title = { Text("Open on startup") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                (listOf<ServiceConfig?>(null) + ConfigStore.services).forEach { c ->
                    Row(Modifier.fillMaxWidth().clickable { ConfigStore.putString("startupService", c?.id ?: ""); startupPick = false }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(startupId == (c?.id ?: ""), { ConfigStore.putString("startupService", c?.id ?: ""); startupPick = false })
                        if (c != null) { com.arrmada360.app.ui.ServiceLogo(c.type, 26.dp, c.name); Spacer(Modifier.width(10.dp)) }
                        Text(c?.name ?: "Dashboard")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { startupPick = false }) { Text("Close") } },
    )

    crashText?.let { t ->
        AlertDialog(
            onDismissRequest = { crashText = null },
            title = { Text("Logging center") },
            text = { Text(t.ifEmpty { "No crashes recorded 🎉" }.take(3000), style = MaterialTheme.typography.labelSmall) },
            confirmButton = {
                if (t.isNotEmpty()) TextButton(onClick = {
                    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, t), "Share crash log"))
                }) { Text("Share") }
            },
            dismissButton = { TextButton(onClick = { crashText = null }) { Text("Close") } },
        )
    }

    manage?.let { c ->
        var confirm by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { manage = null },
            title = { Text(c.name) },
            text = {
                Column {
                    TextButton(onClick = { ConfigStore.move(c.id, -1) }) { Text("Move up") }
                    TextButton(onClick = { ConfigStore.move(c.id, 1) }) { Text("Move down") }
                    TextButton(onClick = { manage = null; nav.push(Route.EditService(c.id, null)) }) { Text("Edit") }
                    TextButton(onClick = { confirm = true }) { Text("Remove from Arrmada360", color = Bad) }
                    if (confirm) TextButton(onClick = { ConfigStore.remove(c.id); manage = null }) { Text("Yes, remove ${c.name}", color = Bad, fontWeight = FontWeight.Bold) }
                }
            },
            confirmButton = { TextButton(onClick = { manage = null }) { Text("Done") } },
        )
    }
}

// ============================================================ General settings
@OptIn(ExperimentalCoilApi::class)
@Composable
fun GeneralSettingsScreen(nav: Navigator, back: () -> Unit) {
    val ctx = LocalContext.current
    var imdbDialog by remember { mutableStateOf(false) }
    var crashText by remember { mutableStateOf<String?>(null) }
    SettingsPage("General Settings", back) {
        item {
            SettingsGroup("Navigation") {
                SettingSwitch("Volume rocker navigation", "Press the volume buttons to quickly switch between your apps.", ConfigStore.flag(Pref.VOLUME_NAV)) { ConfigStore.setFlag(Pref.VOLUME_NAV, it) }
                SettingLink(Icons.Filled.Swipe, "Gesture navigation settings", "Side-menu swipe, swipe actions on downloads, back button behaviour.", divider = false) { nav.push(Route.SettingsGestures) }
            }
        }
        item {
            SettingsGroup("Interface") {
                SettingSwitch("Friendly service names", "Show \"Movies, TV Shows, Music\" instead of \"Radarr, Sonarr, Lidarr\".", ConfigStore.flag(Pref.FRIENDLY_NAMES)) { ConfigStore.setFlag(Pref.FRIENDLY_NAMES, it) }
                SettingSwitch("Show speeds as bits", "Display download speeds as bits instead of bytes (500 Mbps vs 62 MB/s).", ConfigStore.flag(Pref.SPEED_BITS)) { ConfigStore.setFlag(Pref.SPEED_BITS, it) }
                SettingSwitch("Auto-clear searches in *arr views", "Clear the library filter after opening a searched item.", ConfigStore.flag(Pref.AUTO_CLEAR_SEARCH)) { ConfigStore.setFlag(Pref.AUTO_CLEAR_SEARCH, it) }
                SettingSwitch("Show What's New on update", "Show a screen with all the changes when Arrmada360 updates to a new version.", ConfigStore.flag(Pref.WHATS_NEW)) { ConfigStore.setFlag(Pref.WHATS_NEW, it) }
                SettingSwitch("Check for updates automatically", "Tell me when a new Arrmada360 build is available and install it in one tap.", ConfigStore.flag(Pref.AUTO_UPDATE_CHECK), divider = false) { ConfigStore.setFlag(Pref.AUTO_UPDATE_CHECK, it) }
            }
        }
        item {
            var lockOn by remember { mutableStateOf(com.arrmada360.app.AppLock.enabled) }
            var timeout by remember { mutableIntStateOf(com.arrmada360.app.AppLock.timeoutSec) }
            var timeoutDlg by remember { mutableStateOf(false) }
            val opts = listOf(0 to "Immediately", 30 to "After 30 seconds", 60 to "After 1 minute", 300 to "After 5 minutes", 900 to "After 15 minutes")
            SettingsGroup("Security") {
                SettingSwitch("App lock", "Unlock Arrmada360 with your fingerprint, face or screen lock every time you open it.", lockOn) {
                    if (it && !com.arrmada360.app.AppLock.deviceSecure(ctx)) ctx.toast("Set a screen lock (PIN, pattern or fingerprint) on your phone first")
                    else { lockOn = it; ConfigStore.setFlag(com.arrmada360.app.AppLock.PREF, it) }
                }
                SettingLink(Icons.Filled.Timer, "Lock after", value = opts.firstOrNull { it.first == timeout }?.second ?: "Immediately", divider = false) { timeoutDlg = true }
            }
            if (timeoutDlg) AlertDialog(
                onDismissRequest = { timeoutDlg = false }, title = { Text("Lock after leaving the app") },
                text = { Column { opts.forEach { (sec, label) -> TextButton(onClick = { timeout = sec; ConfigStore.putInt(com.arrmada360.app.AppLock.TIMEOUT, sec); timeoutDlg = false }) { Text(label) } } } },
                confirmButton = { TextButton(onClick = { timeoutDlg = false }) { Text("Close") } },
            )
        }
        item {
            SettingsGroup("Third-party app integrations") {
                SettingLink(Icons.Filled.Link, "Open IMDb links with", value = if (ConfigStore.imdbOpenWith == "app") "IMDb app" else "Device browser", tint = Warn, divider = false) { imdbDialog = true }
            }
        }
        item {
            SettingsGroup("General actions") {
                SettingSwitch("Require authentication for settings", "Require fingerprint, face or PIN to view and manage Arrmada360 settings.", ConfigStore.flag(Pref.AUTH_SETTINGS)) {
                    ConfigStore.setFlag(Pref.AUTH_SETTINGS, it); if (it) ConfigStore.settingsUnlocked = true
                }
                SettingSwitch("Crash logs", "Keep a log of the last crash on this phone so you can share it. Nothing is ever uploaded.", ConfigStore.flag(Pref.CRASH_LOGS)) { ConfigStore.setFlag(Pref.CRASH_LOGS, it) }
                SettingLink(Icons.Filled.BugReport, "View last crash log", "Share it with whoever is fixing the app.", tint = Bad) {
                    val f = File(ctx.filesDir, "crash.txt")
                    crashText = if (f.exists()) f.readText() else ""
                }
                SettingLink(Icons.Filled.HideImage, "Clear image cache", "Useful if Radarr, Sonarr, etc. show the wrong poster art or logos.") {
                    ctx.imageLoader.memoryCache?.clear()
                    ctx.imageLoader.diskCache?.clear()
                    ctx.toast("Image cache cleared")
                }
                SettingLink(Icons.Filled.DeleteSweep, "Clear cached app data", "Logs out of download clients and reloads every app. Useful if lists look out of date.", divider = false) {
                    DownloadClients.clearCache()
                    Connections.state.clear()
                    ctx.toast("Cache cleared – everything will reload")
                }
            }
        }
    }

    if (imdbDialog) AlertDialog(
        onDismissRequest = { imdbDialog = false },
        title = { Text("Open IMDb links with") },
        text = {
            Column {
                listOf("browser" to "Device browser", "app" to "IMDb app (if installed)").forEach { (k, label) ->
                    Row(Modifier.fillMaxWidth().clickable { ConfigStore.updateImdbOpenWith(k); imdbDialog = false }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(ConfigStore.imdbOpenWith == k, { ConfigStore.updateImdbOpenWith(k); imdbDialog = false })
                        Text(label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { imdbDialog = false }) { Text("Close") } },
    )

    crashText?.let { t ->
        AlertDialog(
            onDismissRequest = { crashText = null },
            title = { Text("Last crash log") },
            text = { Text(t.ifEmpty { "No crashes recorded 🎉" }.take(3000), style = MaterialTheme.typography.labelSmall) },
            confirmButton = {
                if (t.isNotEmpty()) TextButton(onClick = {
                    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, t), "Share crash log"))
                }) { Text("Share") }
            },
            dismissButton = {
                TextButton(onClick = { File(ctx.filesDir, "crash.txt").delete(); crashText = null }) { Text("Delete") }
            },
        )
    }
}

@Composable
fun GestureSettingsScreen(back: () -> Unit) {
    SettingsPage("Gesture navigation", back) {
        item {
            SettingsGroup("Gestures") {
                SettingSwitch("Swipe to open the side menu", "Swipe in from the left edge on main screens to open the app menu.", ConfigStore.flag(Pref.DRAWER_GESTURE)) { ConfigStore.setFlag(Pref.DRAWER_GESTURE, it) }
                SettingSwitch("Swipe actions on downloads", "Swipe a download right to pause or resume it, left to delete it.", ConfigStore.flag(Pref.SWIPE_DOWNLOADS)) { ConfigStore.setFlag(Pref.SWIPE_DOWNLOADS, it) }
                SettingSwitch("Back goes to the dashboard", "Pressing back on an app's main screen returns to the dashboard instead of closing.", ConfigStore.flag(Pref.BACK_TO_DASHBOARD), divider = false) { ConfigStore.setFlag(Pref.BACK_TO_DASHBOARD, it) }
            }
        }
    }
}

// ============================================================ Backup & restore
@Composable
fun BackupScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    var pasteOpen by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf<String?>(null) }
    val createLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) try {
            ctx.contentResolver.openOutputStream(uri)?.use { os ->
                ZipOutputStream(os).use { zip ->
                    zip.putNextEntry(ZipEntry("arrmada360-settings.json"))
                    zip.write(ConfigStore.exportFullJson().toByteArray())
                    zip.closeEntry()
                }
            }
            ctx.toast("Backup saved")
        } catch (e: Exception) { ctx.toast("Couldn't save backup: ${e.message}") }
    }
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) try {
            val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
            val text = if (bytes.size > 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()) {
                var found: String? = null
                ZipInputStream(bytes.inputStream()).use { z ->
                    var e = z.nextEntry
                    while (e != null && found == null) {
                        if (e.name.endsWith(".json")) found = z.readBytes().decodeToString()
                        e = z.nextEntry
                    }
                }
                found ?: throw Exception("no settings file inside the .zip")
            } else bytes.decodeToString()
            confirmRestore = text
        } catch (e: Exception) { ctx.toast("That isn't an Arrmada360 backup (${e.message})") }
    }
    SettingsPage("Backup & Restore", back) {
        item {
            Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(140.dp).clip(RoundedCornerShape(40.dp)).background(Brush.linearGradient(listOf(Good.copy(alpha = 0.35f), Accent.copy(alpha = 0.2f)))), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Inventory2, null, tint = Good, modifier = Modifier.size(84.dp))
                }
            }
        }
        item {
            SettingsGroup("Transfer settings") {
                SettingLink(Icons.Filled.CloudUpload, "Create a backup", "Save all apps and settings to a .zip file to store locally or move to another device.", tint = MaterialTheme.colorScheme.onSurface) {
                    createLauncher.launch("arrmada360-backup-${java.time.LocalDate.now()}.zip")
                }
                SettingLink(Icons.Filled.Restore, "Restore from a file", "Pick an Arrmada360 backup .zip on your device to replace all current settings.", tint = MaterialTheme.colorScheme.onSurface) {
                    openLauncher.launch(arrayOf("application/zip", "application/json", "application/octet-stream", "text/plain"))
                }
                SettingLink(Icons.Filled.ContentCopy, "Copy settings as text", "Paste them into a note or message.", tint = MaterialTheme.colorScheme.onSurface) { ctx.copyText(ConfigStore.exportFullJson()) }
                SettingLink(Icons.Filled.Sync, "Restore from pasted text", "Paste settings you copied earlier.", tint = MaterialTheme.colorScheme.onSurface, divider = false) { pasteOpen = true }
            }
        }
        item {
            Text(
                "Got a new device? Back up here, move the .zip to the new device, and restore to get up and running fast. Backups include your API keys and passwords – keep them private.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(20.dp),
            )
        }
    }

    if (pasteOpen) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { pasteOpen = false },
            title = { Text("Paste settings") },
            text = { OutlinedTextField(text, { text = it }, minLines = 4, maxLines = 8, label = { Text("Exported settings") }) },
            confirmButton = { TextButton(onClick = { pasteOpen = false; confirmRestore = text }) { Text("Continue") } },
            dismissButton = { TextButton(onClick = { pasteOpen = false }) { Text("Cancel") } },
        )
    }

    confirmRestore?.let { text ->
        AlertDialog(
            onDismissRequest = { confirmRestore = null },
            title = { Text("Replace all current settings?") },
            text = { Text("Your current apps and settings will be replaced by the backup.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = null
                    try {
                        val n = ConfigStore.restoreFullJson(text)
                        ctx.toast("Restored $n apps")
                        Notifier.schedule(ctx)
                    } catch (e: Exception) {
                        // older text exports (just the services list)
                        try { ctx.toast("Imported ${ConfigStore.importJson(text)} apps") } catch (e2: Exception) { ctx.toast("That doesn't look like an Arrmada360 backup") }
                    }
                }) { Text("Restore", color = Bad) }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = null }) { Text("Cancel") } },
        )
    }
}

// ============================================================ Notifications
@Composable
fun NotificationsScreen(nav: Navigator, back: () -> Unit) {
    val ctx = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { ConfigStore.setFlag(Pref.NOTIFY, true); Notifier.schedule(ctx); Notifier.checkNow(ctx) }
        else ctx.toast("Allow notifications for Arrmada360 in Android settings to get alerts")
    }
    fun setEnabled(on: Boolean) {
        if (on && Build.VERSION.SDK_INT >= 33 && !Notifier.canPost(ctx)) { permission.launch(Manifest.permission.POST_NOTIFICATIONS); return }
        ConfigStore.setFlag(Pref.NOTIFY, on)
        Notifier.schedule(ctx)
        if (on) Notifier.checkNow(ctx)
    }
    val enabled = ConfigStore.flag(Pref.NOTIFY)
    SettingsPage("Notifications", back) {
        item {
            Text(
                "Arrmada360 checks your servers in the background about every 15 minutes and posts a notification when something new is downloaded or requested. It's built to be very light on battery.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
            )
        }
        item {
            SettingsGroup("Notify service") {
                Row(Modifier.fillMaxWidth().clickable { setEnabled(!enabled) }.padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    RowIcon(if (enabled) Icons.Filled.NotificationsActive else Icons.Filled.Notifications, if (enabled) Good else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Enable notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Turn the background media-notifier on.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(enabled, { setEnabled(it) }, colors = SwitchDefaults.colors(checkedTrackColor = Good, checkedThumbColor = Color(0xFF111318), uncheckedTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
                }
            }
        }
        item {
            SettingsGroup("Per-service alerts") {
                SettingSwitch("Radarr", "Notify when new movies are downloaded.", ConfigStore.flag(Pref.NOTIFY_RADARR)) { ConfigStore.setFlag(Pref.NOTIFY_RADARR, it) }
                SettingSwitch("Sonarr", "Notify when new episodes are downloaded.", ConfigStore.flag(Pref.NOTIFY_SONARR)) { ConfigStore.setFlag(Pref.NOTIFY_SONARR, it) }
                SettingSwitch("Lidarr", "Notify when new songs or albums are downloaded.", ConfigStore.flag(Pref.NOTIFY_LIDARR)) { ConfigStore.setFlag(Pref.NOTIFY_LIDARR, it) }
                SettingSwitch("Readarr", "Notify when new books are downloaded.", ConfigStore.flag(Pref.NOTIFY_READARR)) { ConfigStore.setFlag(Pref.NOTIFY_READARR, it) }
                SettingSwitch("Seerr", "Notify when new requests are made.", ConfigStore.flag(Pref.NOTIFY_SEERR)) { ConfigStore.setFlag(Pref.NOTIFY_SEERR, it) }
                SettingSwitch("Download clients", "Notify when a torrent or NZB finishes in qBittorrent, SABnzbd, etc.", ConfigStore.flag(Pref.NOTIFY_DOWNLOADS), divider = false) { ConfigStore.setFlag(Pref.NOTIFY_DOWNLOADS, it) }
            }
        }
        item {
            Spacer(Modifier.height(16.dp))
            Column(Modifier.padding(horizontal = 14.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))) {
                SettingLink(Icons.Filled.Sync, "Check now", if (enabled) "Look for new downloads right away" else "Turn notifications on first", divider = true) {
                    if (enabled) { Notifier.checkNow(ctx); ctx.toast("Checking your servers…") } else ctx.toast("Turn on notifications first")
                }
                SettingLink(Icons.Filled.Link, "Arrmada360 deep links", "Launch Arrmada360 screens straight from Notifiarr, Ntfy, Tasker and more.", tint = MaterialTheme.colorScheme.onSurface, divider = false) {
                    nav.push(Route.SettingsDeepLinks)
                }
            }
        }
    }
}

@Composable
fun DeepLinksScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    val links = listOf(
        "arrmada360://dashboard" to "Dashboard",
        "arrmada360://movies" to "Movies tab",
        "arrmada360://tv" to "TV Shows tab",
        "arrmada360://calendar" to "Calendar tab",
        "arrmada360://server" to "Server tab",
        "arrmada360://search" to "Search everything",
        "arrmada360://health" to "Connection check",
        "arrmada360://settings" to "Settings",
        "arrmada360://updates" to "Check for updates",
    ) + ConfigStore.services.map { "arrmada360://service/${android.net.Uri.encode(it.name)}" to it.name }
    SettingsPage("Deep links", back) {
        item {
            Text("Use these links in Notifiarr, Ntfy, Tasker, a browser bookmark or a home-screen shortcut to jump straight to a screen. Tap to copy.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(20.dp))
        }
        item {
            SettingsGroup("Links") {
                links.forEachIndexed { i, (url, label) ->
                    Row(Modifier.fillMaxWidth().clickable { ctx.copyText(url) }.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(url, style = MaterialTheme.typography.bodySmall, color = Rainbow[i % Rainbow.size])
                        }
                        IconButton(onClick = { ctx.copyText(url) }) { Icon(Icons.Filled.ContentCopy, "Copy", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    if (i < links.size - 1) GroupDivider()
                }
            }
        }
    }
}

// ============================================================ Appearance & discovery
@Composable
fun AppearanceScreen(back: () -> Unit) {
    SettingsPage("Appearance", back) {
        item {
            SettingsGroup("Theme") {
                SettingSwitch("AMOLED black", "Pure black background to save battery on OLED screens.", ConfigStore.amoled, divider = false) { ConfigStore.updateAmoled(it) }
            }
        }
        item {
            SettingsGroup("Library") {
                Column(Modifier.padding(20.dp)) {
                    Text("Poster columns: ${ConfigStore.gridColumns}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Slider(value = ConfigStore.gridColumns.toFloat(), onValueChange = { ConfigStore.updateGridColumns(Math.round(it)) }, valueRange = 2f..5f, steps = 2)
                }
            }
        }
    }
}

@Composable
fun DiscoverySettingsScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    var tmdb by remember { mutableStateOf(ConfigStore.tmdbKey) }
    var trakt by remember { mutableStateOf(ConfigStore.traktClientId) }
    SettingsPage("Discovery sources", back) {
        item {
            Text(
                "The Movies and TV Shows tabs use Trakt first, then TMDB, then your Overseerr/Jellyseerr. Seerr works with no key. Free keys add more rows (popular people needs TMDB or Seerr).",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(20.dp),
            )
        }
        item {
            SettingsGroup("Keys") {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(tmdb, { tmdb = it }, label = { Text("TMDB API key or read access token") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("themoviedb.org → Settings → API (free)") })
                    OutlinedTextField(trakt, { trakt = it }, label = { Text("Trakt Client ID") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("trakt.tv/oauth/applications → New application → Client ID (free)") })
                    GradientButton("Save", Modifier.fillMaxWidth()) {
                        ConfigStore.updateDiscoverKeys(tmdb, trakt)
                        ctx.toast("Saved – Movies & TV tabs will refresh")
                    }
                }
            }
        }
    }
}

// ============================================================ What's New
private val CHANGELOG = listOf(
    "Settings rebuilt: General settings, Backup & Restore (.zip), Notifications",
    "Background notifications for new movies, episodes, music, books, Seerr requests and finished downloads",
    "Volume rocker navigation, gesture options, friendly names, speeds in bits",
    "Settings lock with fingerprint / PIN, local crash logs, cache clearing",
    "Deep links (arrmada360://…) for Notifiarr, Ntfy and Tasker",
    "Movies & TV Shows tabs with trending, upcoming, popular, recommended and people rows",
)

@Composable
fun WhatsNewGate() {
    val ctx = LocalContext.current
    val (ver, code) = appVersion(ctx)
    var show by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    LaunchedEffect(code) {
        val last = ConfigStore.getInt("lastVersionCode", 0)
        ConfigStore.putInt("lastVersionCode", code.toInt())
        if (last != 0 && code > last && ConfigStore.flag(Pref.WHATS_NEW)) {
            notes = try {
                val all = com.arrmada360.app.api.Updates.notesSince(com.arrmada360.app.api.Updates.changelog(), last)
                all.lines().filterNot { it.startsWith("## [Unreleased") }.joinToString("\n")
            } catch (e: Exception) { "" }
            show = true
        }
    }
    if (show) AlertDialog(
        onDismissRequest = { show = false },
        title = { Text("What's new in $ver", fontWeight = FontWeight.Bold) },
        text = {
            if (notes.isNotBlank()) Column(Modifier.heightIn(max = 460.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) { MarkdownText(notes) }
            else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CHANGELOG.forEachIndexed { i, line ->
                    Row {
                        Text("●  ", color = Rainbow[i % Rainbow.size])
                        Text(line, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { GradientButton("Nice!") { show = false } },
    )
}

@Suppress("unused")
private val keepLock = Icons.Filled.Lock
@Suppress("unused")
private fun keepAlign() = TextAlign.Center

// ============================================================ Connection switching (local ⇄ remote)
@Composable
fun ConnectionSwitchScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    val ns = com.arrmada360.app.net.NetSwitch
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { ns.refresh(); ctx.toast("Tap “Add current Wi-Fi” again") } else ctx.toast("Without location access Android hides the Wi-Fi name – Auto will test local addresses instead")
    }
    val v = ns.version
    if (v < 0) Text("")
    SettingsPage("Connection switching", back) {
        item {
            Box(
                Modifier.padding(14.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Good.copy(alpha = 0.35f), Accent.copy(alpha = 0.25f)))).padding(18.dp),
            ) {
                Column {
                    Text(if (ConfigStore.services.filter { it.hasRemote }.all { ns.useLocal(it) }) "🏠  Local connection" else "🌍  Remote connection",
                        style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Text(ns.describe(), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                    Spacer(Modifier.height(10.dp))
                    GradientButton("Re-check network now") { ns.refresh(); ctx.toast("Re-checking…") }
                }
            }
        }
        item {
            SettingsGroup("Mode") {
                listOf(
                    Triple("AUTO", "Automatic (recommended)", "Local on your home Wi-Fi, remote everywhere else"),
                    Triple("LOCAL", "Always local", "Use local addresses even on mobile data (e.g. Tailscale subnet routing)"),
                    Triple("REMOTE", "Always remote", "Always use the remote address when one is set"),
                ).forEachIndexed { i, (k, t, sub) ->
                    Row(Modifier.fillMaxWidth().clickable { ConfigStore.updateConnMode(k) }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(ConfigStore.connMode == k, { ConfigStore.updateConnMode(k) })
                        Column(Modifier.weight(1f)) {
                            Text(t, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (i < 2) GroupDivider()
                }
            }
        }
        item {
            SettingsGroup("Home Wi-Fi networks") {
                Text(
                    if (ConfigStore.homeSsids.isEmpty()) "None added – on Wi-Fi, Arrmada360 quickly tests each local address and uses it if it answers."
                    else "On these networks local addresses are used; on any other network the remote address is used.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(20.dp),
                )
                ConfigStore.homeSsids.sorted().forEach { s ->
                    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Wifi, null, tint = Good)
                        Spacer(Modifier.width(12.dp))
                        Text(s, style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.weight(1f))
                        if (s == ns.ssid) Pill("Connected", Good)
                        IconButton(onClick = { ConfigStore.updateHomeSsids(ConfigStore.homeSsids - s) }) { Icon(Icons.Filled.DeleteSweep, "Remove", tint = Bad) }
                    }
                }
                GroupDivider()
                SettingLink(Icons.Filled.Add, "Add current Wi-Fi network", ns.ssid?.let { "Connected to “$it”" } ?: if (!ns.onWifi) "Connect to your home Wi-Fi first" else "Needs location permission to read the network name", tint = Accent, divider = false) {
                    when {
                        !ns.onWifi -> ctx.toast("You're not on Wi-Fi")
                        !ns.hasLocationPermission() -> perm.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        ns.ssid == null -> { ns.refresh(); ctx.toast("Couldn't read the Wi-Fi name – make sure Location is turned on") }
                        else -> { ConfigStore.updateHomeSsids(ConfigStore.homeSsids + ns.ssid!!); ctx.toast("Added ${ns.ssid}") }
                    }
                }
            }
        }
        item {
            SettingsGroup("Your apps right now") {
                val list = ConfigStore.services.toList()
                if (list.none { it.hasRemote }) Text("No app has a remote address yet. Edit an app and fill in “Remote address” to enable switching.",
                    style = MaterialTheme.typography.bodySmall, color = Warn, modifier = Modifier.padding(20.dp))
                list.forEachIndexed { i, c ->
                    val local = !c.hasRemote || ns.useLocal(c)
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ServiceBadge(c.type, 36.dp, c.name)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(c.baseUrl, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        when {
                            !c.hasRemote -> Pill("Local only", MaterialTheme.colorScheme.outline)
                            local -> Pill("🏠 Local", Good)
                            else -> Pill("🌍 Remote", Accent2)
                        }
                    }
                    if (i < list.size - 1) GroupDivider()
                }
            }
        }
    }
}
