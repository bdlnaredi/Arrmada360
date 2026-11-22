package com.arrmada360.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Public
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.arrmada360.app.api.Conn
import com.arrmada360.app.data.display
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.arrmada360.app.api.Connections
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arrmada360.app.R
import com.arrmada360.app.data.Category
import com.arrmada360.app.data.ConfigStore
import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.data.ServiceType
import com.arrmada360.app.ui.screens.ArrAddScreen
import com.arrmada360.app.ui.screens.ArrDetailScreen
import com.arrmada360.app.ui.screens.ArrScreen
import com.arrmada360.app.ui.screens.DashboardScreen
import com.arrmada360.app.ui.screens.DownloadsScreen
import com.arrmada360.app.ui.screens.IndexerScreen
import com.arrmada360.app.ui.screens.SeerrScreen
import com.arrmada360.app.ui.screens.ServiceEditorScreen
import com.arrmada360.app.ui.screens.ServicePickerScreen
import com.arrmada360.app.ui.screens.SettingsScreen
import com.arrmada360.app.ui.screens.TautulliScreen
import com.arrmada360.app.ui.screens.UnraidScreen
import com.arrmada360.app.ui.screens.UniversalSearchScreen
import kotlinx.coroutines.launch

sealed interface Route {
    data object Dashboard : Route
    data class Service(val id: String) : Route
    data class ArrDetail(val serviceId: String, val itemId: Int) : Route
    data object YearReview : Route
    data class ClipFinder(val uri: String, val mime: String?) : Route
    data class LinkLookup(val link: String) : Route
    data object SettingsFamily : Route
    data class UnraidLive(val serviceId: String) : Route
    data class UnraidLogs(val serviceId: String, val containerId: String?, val containerName: String?) : Route
    data object SpeedScheduler : Route
    data object SeedingManager : Route
    data class ArrCollections(val serviceId: String, val focusTmdb: Int?) : Route
    data class ArrQuality(val serviceId: String) : Route
    data class ArrAdd(val serviceId: String, val term: String = "") : Route
    data object Settings : Route
    data object AddService : Route
    data class EditService(val id: String?, val type: ServiceType?) : Route
    data class Search(val term: String = "") : Route
    data object Health : Route
    data class ArrSystem(val serviceId: String) : Route
    data class ManualImport(val serviceId: String) : Route
    data object SettingsGeneral : Route
    data object SettingsGestures : Route
    data object SettingsBackup : Route
    data object SettingsNotifications : Route
    data object SettingsDeepLinks : Route
    data object SettingsAppearance : Route
    data object SettingsDiscovery : Route
    data object Updates : Route
    data object SettingsConnection : Route
    data object Changelog : Route
}

/** Lets deep links pick a dashboard bottom tab. */
object DashboardNav {
    var tab by androidx.compose.runtime.mutableStateOf<Int?>(null)
}

class Navigator {
    val stack = mutableStateListOf<Route>(Route.Dashboard)
    val current: Route get() = stack.last()
    fun push(r: Route) { stack.add(r) }
    fun pop(): Boolean = if (stack.size > 1) { stack.removeAt(stack.size - 1); true } else false
    fun root(r: Route) { stack.clear(); stack.add(r) }
}

@Composable
fun AppShell(pendingLink: String?, onLinkHandled: () -> Unit) {
    val nav = remember {
        Navigator().also { n ->
            val sid = ConfigStore.getString("startupService")
            if (sid.isNotEmpty() && ConfigStore.get(sid) != null) n.root(Route.Service(sid))
        }
    }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val sharedClip = com.arrmada360.app.data.ShareInbox.clip
    LaunchedEffect(sharedClip) { sharedClip?.let { (u, m) -> nav.push(Route.ClipFinder(u, m)); com.arrmada360.app.data.ShareInbox.clip = null } }
    val sharedLink = com.arrmada360.app.data.ShareInbox.titleLink
    LaunchedEffect(sharedLink) { sharedLink?.let { nav.push(Route.LinkLookup(it)); com.arrmada360.app.data.ShareInbox.titleLink = null } }

    BackHandler(enabled = drawer.isOpen || nav.stack.size > 1 || (nav.current != Route.Dashboard && ConfigStore.flag(com.arrmada360.app.data.Pref.BACK_TO_DASHBOARD))) {
        when {
            drawer.isOpen -> scope.launch { drawer.close() }
            nav.stack.size > 1 -> nav.pop()
            else -> nav.root(Route.Dashboard)
        }
    }

    val current = nav.current
    androidx.compose.runtime.LaunchedEffect(ConfigStore.services.size, com.arrmada360.app.net.NetSwitch.version) { Connections.checkAll() }
    // Volume rocker: cycle through apps
    androidx.compose.runtime.LaunchedEffect(Unit) {
        com.arrmada360.app.data.AppEvents.cycleService.collect { d ->
            val list = ConfigStore.services.toList()
            if (list.isEmpty()) return@collect
            val cur = (nav.current as? Route.Service)?.id
            val idx = list.indexOfFirst { it.id == cur }
            val next = if (idx < 0) (if (d > 0) 0 else list.size - 1) else ((idx + d) % list.size + list.size) % list.size
            nav.root(Route.Service(list[next].id))
        }
    }
    // Deep links: arrmada360://service/<name or id>, arrmada360://movies …
    androidx.compose.runtime.LaunchedEffect(Unit) {
        com.arrmada360.app.data.AppEvents.deepLink.collect { uri ->
            when (uri.host) {
                "service" -> {
                    val key = uri.pathSegments.firstOrNull() ?: ""
                    ConfigStore.services.firstOrNull { it.id == key || it.name.equals(key, true) || it.type.name.equals(key, true) }
                        ?.let { nav.root(Route.Service(it.id)) }
                }
                "item" -> {
                    val seg = uri.pathSegments
                    val svc = seg.getOrNull(0)?.let { ConfigStore.get(it) }
                    val iid = seg.getOrNull(1)?.toIntOrNull()
                    if (svc != null && iid != null) { nav.root(Route.Service(svc.id)); nav.push(Route.ArrDetail(svc.id, iid)) }
                }
                "dashboard" -> nav.root(Route.Dashboard)
                "movies" -> { DashboardNav.tab = 0; nav.root(Route.Dashboard) }
                "tv" -> { DashboardNav.tab = 1; nav.root(Route.Dashboard) }
                "calendar" -> { DashboardNav.tab = 2; nav.root(Route.Dashboard) }
                "server" -> { DashboardNav.tab = 3; nav.root(Route.Dashboard) }
                "search" -> nav.root(Route.Search())
                "health" -> nav.root(Route.Health)
                "settings" -> nav.root(Route.Settings)
                "updates" -> { nav.root(Route.Dashboard); nav.push(Route.Updates) }
                else -> nav.root(Route.Dashboard)
            }
            com.arrmada360.app.data.AppEvents.deepLink.resetReplayCache()
        }
    }
    com.arrmada360.app.ui.screens.WhatsNewGate()
    com.arrmada360.app.ui.screens.UpdateGate(nav)
    val wide = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 840
    if (wide) CompositionLocalProvider(LocalOpenDrawer provides {}, com.arrmada360.app.ui.LocalWideLayout provides true) {
        // Tablets, foldables and TVs: side menu always visible next to the content.
        androidx.compose.foundation.layout.Row(Modifier.fillMaxSize()) {
            Box(Modifier.width(300.dp).fillMaxHeight()) {
                DrawerContent(current, go = { r -> nav.root(r) }, push = { r -> nav.push(r) })
            }
            Box(Modifier.weight(1f).fillMaxHeight()) { RouteContent(current, nav) }
        }
    } else CompositionLocalProvider(LocalOpenDrawer provides { scope.launch { drawer.open() } }) {
        ModalNavigationDrawer(
            drawerState = drawer,
            gesturesEnabled = (nav.stack.size == 1 && ConfigStore.flag(com.arrmada360.app.data.Pref.DRAWER_GESTURE)) || drawer.isOpen,
            drawerContent = {
                DrawerContent(current, go = { r ->
                    nav.root(r)
                    scope.launch { drawer.close() }
                }, push = { r ->
                    nav.push(r)
                    scope.launch { drawer.close() }
                })
            },
        ) {
            Box(Modifier.fillMaxSize()) {
                RouteContent(current, nav)
            }
        }
    }

    if (pendingLink != null) {
        SendLinkDialog(pendingLink, onDismiss = onLinkHandled)
    }
}

/** What family members are allowed to open. */
private fun familyAllows(r: Route): Boolean = when (r) {
    Route.Dashboard, Route.YearReview, Route.Changelog -> true
    is Route.ClipFinder, is Route.LinkLookup, is Route.ArrDetail -> true
    is Route.Service -> com.arrmada360.app.data.Family.canOpen(r.id)
    is Route.Search -> true
    else -> false
}

@Composable
private fun RouteContent(r: Route, nav: Navigator) {
    val back: () -> Unit = { if (!nav.pop()) nav.root(Route.Dashboard) }
    val isRoot = nav.stack.size == 1
    if (com.arrmada360.app.data.Family.active && !familyAllows(r)) { com.arrmada360.app.ui.screens.FamilyBlockedScreen(nav, back); return }
    when (r) {
        Route.Dashboard -> DashboardScreen(nav)
        is Route.ClipFinder -> com.arrmada360.app.ui.screens.ClipFinderScreen(r.uri, r.mime, nav, back)
        is Route.LinkLookup -> com.arrmada360.app.ui.screens.LinkLookupScreen(r.link, nav, back)
        Route.SettingsFamily -> com.arrmada360.app.ui.screens.SettingsGate(back) { com.arrmada360.app.ui.screens.FamilySettingsScreen(back) }
        Route.Settings -> com.arrmada360.app.ui.screens.SettingsGate(back) { SettingsScreen(nav, isRoot, back) }
        Route.SettingsGeneral -> com.arrmada360.app.ui.screens.SettingsGate(back) { com.arrmada360.app.ui.screens.GeneralSettingsScreen(nav, back) }
        Route.SettingsGestures -> com.arrmada360.app.ui.screens.SettingsGate(back) { com.arrmada360.app.ui.screens.GestureSettingsScreen(back) }
        Route.SettingsBackup -> com.arrmada360.app.ui.screens.SettingsGate(back) { com.arrmada360.app.ui.screens.BackupScreen(back) }
        Route.SettingsNotifications -> com.arrmada360.app.ui.screens.SettingsGate(back) { com.arrmada360.app.ui.screens.NotificationsScreen(nav, back) }
        Route.SettingsDeepLinks -> com.arrmada360.app.ui.screens.DeepLinksScreen(back)
        Route.Updates -> com.arrmada360.app.ui.screens.UpdatesScreen(nav, back)
        Route.SettingsConnection -> com.arrmada360.app.ui.screens.SettingsGate(back) { com.arrmada360.app.ui.screens.ConnectionSwitchScreen(back) }
        Route.Changelog -> com.arrmada360.app.ui.screens.ChangelogScreen(back)
        Route.SettingsAppearance -> com.arrmada360.app.ui.screens.SettingsGate(back) { com.arrmada360.app.ui.screens.AppearanceScreen(back) }
        Route.SettingsDiscovery -> com.arrmada360.app.ui.screens.SettingsGate(back) { com.arrmada360.app.ui.screens.DiscoverySettingsScreen(back) }
        Route.Health -> com.arrmada360.app.ui.screens.HealthScreen(nav, isRoot, back)
        Route.YearReview -> com.arrmada360.app.ui.screens.YearReviewScreen(back)
        Route.SpeedScheduler -> com.arrmada360.app.ui.screens.SpeedSchedulerScreen(back)
        Route.SeedingManager -> com.arrmada360.app.ui.screens.SeedingManagerScreen(back)
        Route.AddService -> ServicePickerScreen(
            title = "Add a service", subtitle = "Pick an app to connect",
            multi = false, initial = emptySet(), onBack = back,
            onDone = { types -> types.firstOrNull()?.let { nav.pop(); nav.push(Route.EditService(null, it)) } },
        )
        is Route.EditService -> {
            val existing = r.id?.let { ConfigStore.get(it) }
            val type = existing?.type ?: r.type ?: ServiceType.SONARR
            ServiceEditorScreen(type, existing, onBack = back, onSaved = { back() }, skipLabel = null)
        }
        is Route.Search -> UniversalSearchScreen(nav, r.term, isRoot, back)
        is Route.Service -> {
            val cfg = ConfigStore.get(r.id)
            if (cfg == null) {
                EmptyBox("This service was removed.")
            } else ServiceTheme(cfg.type, ConfigStore.amoled) { ServiceRoot(cfg, nav, isRoot, back) }
        }
        is Route.ArrCollections -> ConfigStore.get(r.serviceId)?.let { cfg ->
            ServiceTheme(cfg.type, ConfigStore.amoled) { com.arrmada360.app.ui.screens.CollectionsScreen(cfg, r.focusTmdb, nav, back) }
        } ?: EmptyBox("Service missing")
        is Route.UnraidLive -> ConfigStore.get(r.serviceId)?.let { cfg ->
            ServiceTheme(cfg.type, ConfigStore.amoled) { com.arrmada360.app.ui.screens.UnraidLiveScreen(cfg, back) }
        } ?: EmptyBox("Service missing")
        is Route.UnraidLogs -> ConfigStore.get(r.serviceId)?.let { cfg ->
            ServiceTheme(cfg.type, ConfigStore.amoled) { com.arrmada360.app.ui.screens.UnraidLogsScreen(cfg, r.containerId, r.containerName, back) }
        } ?: EmptyBox("Service missing")
        is Route.ArrQuality -> ConfigStore.get(r.serviceId)?.let { cfg ->
            ServiceTheme(cfg.type, ConfigStore.amoled) { com.arrmada360.app.ui.screens.QualityFinderScreen(cfg, nav, back) }
        } ?: EmptyBox("Service missing")
        is Route.ArrDetail -> ConfigStore.get(r.serviceId)?.let { cfg ->
            ServiceTheme(cfg.type, ConfigStore.amoled) { ArrDetailScreen(cfg, r.itemId, nav, back) }
        } ?: EmptyBox("Service missing")
        is Route.ArrSystem -> ConfigStore.get(r.serviceId)?.let { cfg ->
            ServiceTheme(cfg.type, ConfigStore.amoled) { com.arrmada360.app.ui.screens.ArrSystemScreen(cfg, back) }
        } ?: EmptyBox("Service missing")
        is Route.ManualImport -> ConfigStore.get(r.serviceId)?.let { cfg ->
            ServiceTheme(cfg.type, ConfigStore.amoled) { com.arrmada360.app.ui.screens.ManualImportScreen(cfg, back) }
        } ?: EmptyBox("Service missing")
        is Route.ArrAdd -> ConfigStore.get(r.serviceId)?.let { cfg ->
            ServiceTheme(cfg.type, ConfigStore.amoled) { ArrAddScreen(cfg, r.term, nav, back) }
        } ?: EmptyBox("Service missing")
    }
}

@Composable
private fun ServiceRoot(cfg: ServiceConfig, nav: Navigator, isRoot: Boolean, back: () -> Unit) {
    when {
        cfg.type == ServiceType.WHISPARR -> com.arrmada360.app.ui.screens.WhisparrGate(cfg) { ArrScreen(cfg, nav, isRoot, back) }
        cfg.type.isArr -> ArrScreen(cfg, nav, isRoot, back)
        cfg.type == ServiceType.UPTIME_KUMA -> com.arrmada360.app.ui.screens.KumaScreen(cfg, nav, isRoot, back)
        cfg.type == ServiceType.STASH -> com.arrmada360.app.ui.screens.StashScreen(cfg, nav, isRoot, back)
        cfg.type.isDownloader -> DownloadsScreen(cfg, nav, isRoot, back)
        cfg.type.isIndexer -> IndexerScreen(cfg, nav, isRoot, back)
        cfg.type == ServiceType.SEERR -> SeerrScreen(cfg, nav, isRoot, back)
        cfg.type == ServiceType.TAUTULLI -> TautulliScreen(cfg, nav, isRoot, back)
        cfg.type == ServiceType.UNRAID -> UnraidScreen(cfg, nav, isRoot, back)
        cfg.type == ServiceType.NAMER -> com.arrmada360.app.ui.screens.NamerScreen(cfg, nav, isRoot, back)
        else -> EmptyBox("Unsupported")
    }
}

@Composable
private fun DrawerContent(current: Route, go: (Route) -> Unit, push: (Route) -> Unit) {
    ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surface, drawerShape = androidx.compose.foundation.shape.RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)) {
        Column(Modifier.fillMaxSize()) {
            // ---- header: logo + wordmark, thin rule, edit button
            Row(Modifier.padding(start = 24.dp, end = 20.dp, top = 28.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_launcher_foreground), null, tint = Color.Unspecified, modifier = Modifier.size(44.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    androidx.compose.ui.text.buildAnnotatedString {
                        append("Arrmada")
                        pushStyle(androidx.compose.ui.text.SpanStyle(color = Accent)); append("360"); pop()
                    },
                    style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold,
                )
            }
            Row(Modifier.padding(start = 24.dp, end = 16.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier.size(38.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).background(Accent.copy(alpha = 0.16f))
                        .clickable { if (!com.arrmada360.app.data.Family.active) push(Route.Settings) },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Edit, "Edit apps", tint = Accent, modifier = Modifier.size(18.dp)) }
            }
            // ---- flat, calm list
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
                DrawerRow("Dashboard", current == Route.Dashboard, Accent, onClick = { go(Route.Dashboard) }) {
                    Icon(Icons.Filled.Dashboard, null, tint = if (current == Route.Dashboard) Accent else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(26.dp))
                }
                ConfigStore.services.filter { com.arrmada360.app.data.Family.canOpen(it.id) }.forEach { s ->
                    val sel = (current as? Route.Service)?.id == s.id
                    val failing = Connections.of(s.id) is Conn.Fail
                    DrawerRow(s.display(), sel, s.type.tint, warn = failing, onClick = { go(Route.Service(s.id)) }) { ServiceLogo(s.type, 30.dp, s.name) }
                }
                Spacer(Modifier.height(12.dp))
            }
            // ---- footer: settings + connection pill
            var familyExit by remember { mutableStateOf(false) }
            if (familyExit) com.arrmada360.app.ui.screens.FamilyExitDialog { familyExit = false }
            if (com.arrmada360.app.data.Family.active) Row(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(Accent2.copy(alpha = 0.18f)).clickable { familyExit = true }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.FamilyRestroom, null, tint = Accent2); Spacer(Modifier.width(10.dp))
                Text("Family mode · tap to leave", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(56.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                        .background(if (current == Route.Settings) Accent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.background)
                        .clickable { if (com.arrmada360.app.data.Family.active) familyExit = true else go(Route.Settings) },
                    contentAlignment = Alignment.Center,
                ) { Icon(if (com.arrmada360.app.data.Family.active) Icons.Filled.Lock else Icons.Filled.Settings, "Settings", tint = MaterialTheme.colorScheme.onSurface) }
                Spacer(Modifier.width(10.dp))
                val remoteApps = ConfigStore.services.filter { it.hasRemote }
                val local = remoteApps.isEmpty() || remoteApps.all { com.arrmada360.app.net.NetSwitch.useLocal(it) }
                val online = ConfigStore.services.count { Connections.of(it.id) is Conn.Ok }
                Row(
                    Modifier.weight(1f).height(56.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.background).clickable { push(Route.SettingsConnection) }.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(if (local) Icons.Filled.Home else Icons.Filled.Public, null, tint = if (local) Good else Accent2)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("CONNECTION", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (local) "Local" else "Remote", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text("$online/${ConfigStore.services.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun DrawerRow(label: String, selected: Boolean, color: Color, warn: Boolean = false, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp).height(56.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
            .background(if (selected) color.copy(alpha = 0.14f) else Color.Transparent).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(4.dp).height(32.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(50)).background(if (selected) color else Color.Transparent))
        Spacer(Modifier.width(14.dp))
        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(18.dp))
        Text(
            label, style = MaterialTheme.typography.titleMedium, maxLines = 1,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
            modifier = Modifier.weight(1f),
        )
        if (warn) Box(Modifier.padding(end = 16.dp).size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Bad))
    }
}

@Composable
fun AppRoot(pendingLink: String?, onLinkHandled: () -> Unit) {
    val onboarded = ConfigStore.onboarded
    AppTheme(ConfigStore.amoled) {
        if (!onboarded) com.arrmada360.app.ui.screens.Onboarding()
        else AppShell(pendingLink, onLinkHandled)
    }
}
