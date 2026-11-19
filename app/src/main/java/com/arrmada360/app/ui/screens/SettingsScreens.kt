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

// ============================================================ main settings
@Composable
fun SettingsScreen(nav: Navigator, isRoot: Boolean, back: () -> Unit) {
    val ctx = LocalContext.current
    var manage by remember { mutableStateOf<ServiceConfig?>(null) }
    val (ver, _) = appVersion(ctx)
    SettingsPage("Settings", back, isRoot) {
        item {
            Row(
                Modifier.padding(14.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Accent.copy(alpha = 0.45f), Accent2.copy(alpha = 0.40f), Pink.copy(alpha = 0.25f)))).padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_launcher_foreground), null, tint = Color.Unspecified, modifier = Modifier.size(64.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Arrmada360", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Text("Version $ver · every feature free", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                }
            }
        }
        item {
            SettingsGroup("Your apps") {
                ConfigStore.services.forEach { c ->
                    SettingLink(null, c.name, c.baseUrl, tint = c.type.tint, leading = { ServiceBadge(c.type, 44.dp, c.name) },
                        onLongClick = { manage = c }) { nav.push(Route.EditService(c.id, null)) }
                }
                SettingLink(Icons.Filled.Add, "Add an app", "Sonarr, Radarr, Prowlarr, qBittorrent, Unraid…", tint = Accent) { nav.push(Route.AddService) }
                SettingLink(Icons.Filled.Wifi, "Test all connections", "See which apps are online and fix any that aren't", tint = Good, divider = false) { nav.push(Route.Health) }
            }
            Text("Tip: long-press an app to reorder or remove it.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, top = 6.dp))
        }
        item {
            SettingsGroup("Settings") {
                SettingLink(Icons.Filled.Tune, "General settings", "Navigation, interface, IMDb links, security", tint = Rainbow[0]) { nav.push(Route.SettingsGeneral) }
                SettingLink(Icons.Filled.Wifi, "Connection switching", com.arrmada360.app.net.NetSwitch.describe(), tint = Good) { nav.push(Route.SettingsConnection) }
                SettingLink(Icons.Filled.Notifications, "Notifications", if (ConfigStore.flag(Pref.NOTIFY)) "On – checking your servers in the background" else "Off", tint = Rainbow[3]) { nav.push(Route.SettingsNotifications) }
                SettingLink(Icons.Filled.Inventory2, "Backup & restore", "Save everything to a .zip or move to a new phone", tint = Good) { nav.push(Route.SettingsBackup) }
                SettingLink(Icons.Filled.Palette, "Appearance", if (ConfigStore.amoled) "AMOLED black · ${ConfigStore.gridColumns} poster columns" else "Dark · ${ConfigStore.gridColumns} poster columns", tint = Rainbow[2]) { nav.push(Route.SettingsAppearance) }
                SettingLink(Icons.Filled.Explore, "Discovery sources", "Trakt, TMDB and Seerr for the Movies & TV tabs", tint = Accent2) { nav.push(Route.SettingsDiscovery) }
                SettingLink(Icons.Filled.SystemUpdate, "Updates & changelog", "Version $ver – check for a new build, see what's changed", tint = Accent, divider = false) { nav.push(Route.Updates) }
            }
        }
        item {
            Text("Arrmada360 is free and open source. No subscriptions, no paywalls, nothing sent anywhere except to your own servers and the discovery sources you choose.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(20.dp))
            TextButton(onClick = { ConfigStore.resetOnboarding() }, modifier = Modifier.padding(horizontal = 8.dp)) { Text("Run first-time setup again") }
        }
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
                    TextButton(onClick = { confirm = true }) { Text(if (confirm) "Tap again to remove" else "Remove from Arrmada360", color = Bad) }
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
