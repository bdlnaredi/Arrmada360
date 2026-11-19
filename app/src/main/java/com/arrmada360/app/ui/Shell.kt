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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.arrmada360.app.api.Conn
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
    data class ArrAdd(val serviceId: String, val term: String = "") : Route
    data object Settings : Route
    data object AddService : Route
    data class EditService(val id: String?, val type: ServiceType?) : Route
    data class Search(val term: String = "") : Route
    data object Health : Route
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
    val nav = remember { Navigator() }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = drawer.isOpen || nav.stack.size > 1 || nav.current != Route.Dashboard) {
        when {
            drawer.isOpen -> scope.launch { drawer.close() }
            nav.stack.size > 1 -> nav.pop()
            else -> nav.root(Route.Dashboard)
        }
    }

    val current = nav.current
    androidx.compose.runtime.LaunchedEffect(ConfigStore.services.size) { Connections.checkAll() }
    CompositionLocalProvider(LocalOpenDrawer provides { scope.launch { drawer.open() } }) {
        ModalNavigationDrawer(
            drawerState = drawer,
            gesturesEnabled = nav.stack.size == 1,
            drawerContent = {
                DrawerContent(current) { r ->
                    nav.root(r)
                    scope.launch { drawer.close() }
                }
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

@Composable
private fun RouteContent(r: Route, nav: Navigator) {
    val back: () -> Unit = { if (!nav.pop()) nav.root(Route.Dashboard) }
    val isRoot = nav.stack.size == 1
    when (r) {
        Route.Dashboard -> DashboardScreen(nav)
        Route.Settings -> SettingsScreen(nav, isRoot, back)
        Route.Health -> com.arrmada360.app.ui.screens.HealthScreen(nav, isRoot, back)
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
        is Route.ArrDetail -> ConfigStore.get(r.serviceId)?.let { cfg ->
            ServiceTheme(cfg.type, ConfigStore.amoled) { ArrDetailScreen(cfg, r.itemId, nav, back) }
        } ?: EmptyBox("Service missing")
        is Route.ArrAdd -> ConfigStore.get(r.serviceId)?.let { cfg ->
            ServiceTheme(cfg.type, ConfigStore.amoled) { ArrAddScreen(cfg, r.term, nav, back) }
        } ?: EmptyBox("Service missing")
    }
}

@Composable
private fun ServiceRoot(cfg: ServiceConfig, nav: Navigator, isRoot: Boolean, back: () -> Unit) {
    when {
        cfg.type.isArr -> ArrScreen(cfg, nav, isRoot, back)
        cfg.type.isDownloader -> DownloadsScreen(cfg, isRoot, back)
        cfg.type.isIndexer -> IndexerScreen(cfg, isRoot, back)
        cfg.type == ServiceType.SEERR -> SeerrScreen(cfg, isRoot, back)
        cfg.type == ServiceType.TAUTULLI -> TautulliScreen(cfg, isRoot, back)
        cfg.type == ServiceType.UNRAID -> UnraidScreen(cfg, isRoot, back)
        else -> EmptyBox("Unsupported")
    }
}

@Composable
private fun DrawerContent(current: Route, go: (Route) -> Unit) {
    ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Box(
                Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Accent.copy(alpha = 0.55f), Accent2.copy(alpha = 0.45f), Pink.copy(alpha = 0.30f))))
                    .padding(horizontal = 20.dp, vertical = 28.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_launcher_foreground), null, tint = Color.Unspecified, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Arrmada360", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                        val online = ConfigStore.services.count { Connections.of(it.id) is Conn.Ok }
                        Text("${ConfigStore.services.size} apps · $online online", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                    }
                }
            }
            Column(Modifier.padding(horizontal = 12.dp)) {
                Spacer(Modifier.height(8.dp))
                val colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = Color.Transparent,
                    selectedContainerColor = Accent.copy(alpha = 0.18f),
                )
                NavigationDrawerItem(
                    label = { Text("Dashboard") }, selected = current == Route.Dashboard,
                    icon = { Icon(Icons.Filled.Dashboard, null, tint = Accent) }, onClick = { go(Route.Dashboard) }, colors = colors,
                )
                NavigationDrawerItem(
                    label = { Text("Search everything") }, selected = current is Route.Search,
                    icon = { Icon(Icons.Filled.Search, null, tint = Accent2) }, onClick = { go(Route.Search()) }, colors = colors,
                )
                NavigationDrawerItem(
                    label = { Text("Connection check") }, selected = current == Route.Health,
                    icon = { Icon(Icons.Filled.Wifi, null, tint = Good) }, onClick = { go(Route.Health) }, colors = colors,
                )
                Category.entries.forEach { cat ->
                    val list = ConfigStore.services.filter { it.type.category == cat }
                    if (list.isNotEmpty()) {
                        SectionTitle(cat.title, Modifier.padding(top = 4.dp))
                        list.forEach { s ->
                            val st = Connections.of(s.id)
                            NavigationDrawerItem(
                                label = { Text(s.name, fontWeight = FontWeight.Medium) },
                                selected = (current as? Route.Service)?.id == s.id,
                                icon = { ServiceBadge(s.type, 32.dp, s.name) },
                                badge = {
                                    when (st) {
                                        is Conn.Ok -> StatusDot(Good)
                                        is Conn.Fail -> StatusDot(Bad)
                                        else -> {}
                                    }
                                },
                                onClick = { go(Route.Service(s.id)) },
                                colors = NavigationDrawerItemDefaults.colors(
                                    unselectedContainerColor = Color.Transparent,
                                    selectedContainerColor = s.type.tint.copy(alpha = 0.20f),
                                ),
                            )
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                NavigationDrawerItem(
                    label = { Text("Settings") }, selected = current == Route.Settings,
                    icon = { Icon(Icons.Filled.Settings, null) }, onClick = { go(Route.Settings) }, colors = colors,
                )
                Spacer(Modifier.height(24.dp))
            }
        }
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
