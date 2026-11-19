package com.arrmada360.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arrmada360.app.api.Conn
import com.arrmada360.app.api.Connections
import com.arrmada360.app.data.ConfigStore
import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.ui.AppScaffold
import com.arrmada360.app.ui.Bad
import com.arrmada360.app.ui.EmptyBox
import com.arrmada360.app.ui.Good
import com.arrmada360.app.ui.GradientButton
import com.arrmada360.app.ui.Navigator
import com.arrmada360.app.ui.Panel
import com.arrmada360.app.ui.Pill
import com.arrmada360.app.ui.PulseDot
import com.arrmada360.app.ui.RingGauge
import com.arrmada360.app.ui.Route
import com.arrmada360.app.ui.ServiceBadge
import com.arrmada360.app.ui.Warn
import kotlinx.coroutines.launch

/** Tests every configured app and explains exactly what's wrong when something fails. */
@Composable
fun HealthScreen(nav: Navigator, isRoot: Boolean, back: () -> Unit) {
    val scope = rememberCoroutineScope()
    val services = ConfigStore.services.toList()
    LaunchedEffect(services.size) { Connections.checkAll() }
    val states = services.map { Connections.of(it.id) }
    val ok = states.count { it is Conn.Ok }
    val bad = states.count { it is Conn.Fail }
    val checking = states.count { it is Conn.Checking }

    AppScaffold(
        title = "Connection check", subtitle = "$ok online · $bad failing" + if (checking > 0) " · $checking checking" else "",
        isRoot = isRoot, onBack = back,
        actions = { IconButton(onClick = { scope.launch { Connections.checkAll() } }) { Icon(Icons.Filled.Refresh, "Test all") } },
    ) { pad ->
        if (services.isEmpty()) {
            Column(Modifier.padding(pad)) { EmptyBox("No services yet – add some in Settings") }
            return@AppScaffold
        }
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Panel(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        RingGauge(if (services.isEmpty()) 0f else ok.toFloat() / services.size, if (bad == 0) Good else Warn, Modifier.size(84.dp)) {
                            Text("$ok/${services.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                when {
                                    checking > 0 -> "Checking your apps…"
                                    bad == 0 -> "Everything is connected 🎉"
                                    else -> "$bad app${if (bad == 1) "" else "s"} need attention"
                                },
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text("Tap an app to fix its address or key.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            GradientButton("Test all again") { scope.launch { Connections.checkAll() } }
                        }
                    }
                }
            }
            items(services, key = { it.id }) { c -> HealthRow(c, Connections.of(c.id), nav, onRetry = { scope.launch { Connections.check(c) } }) }
        }
    }
}

@Composable
private fun HealthRow(c: ServiceConfig, s: Conn, nav: Navigator, onRetry: () -> Unit) {
    Panel(Modifier.fillMaxWidth(), tint = c.type.tint, onClick = { nav.push(Route.EditService(c.id, null)) }) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ServiceBadge(c.type, 44.dp, c.name)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.name, style = MaterialTheme.typography.titleSmall)
                    Text(c.baseUrl, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                when (s) {
                    is Conn.Checking -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = c.type.tint)
                    is Conn.Ok -> PulseDot(Good)
                    is Conn.Fail -> PulseDot(Bad, pulsing = false)
                    else -> PulseDot(MaterialTheme.colorScheme.onSurfaceVariant, pulsing = false)
                }
            }
            AnimatedVisibility(s is Conn.Ok || s is Conn.Fail) {
                Column(Modifier.padding(top = 10.dp)) {
                    when (s) {
                        is Conn.Ok -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Pill("✓ ${s.info}", Good)
                            Spacer(Modifier.width(6.dp))
                            Pill("${s.ms} ms", if (s.ms < 500) Good else Warn)
                        }
                        is Conn.Fail -> {
                            Text(s.msg, style = MaterialTheme.typography.bodySmall, color = Bad)
                            Row {
                                TextButton(onClick = onRetry) { Icon(Icons.Filled.Refresh, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Retry") }
                                TextButton(onClick = { nav.push(Route.EditService(c.id, null)) }) { Icon(Icons.Filled.Edit, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Fix settings") }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
