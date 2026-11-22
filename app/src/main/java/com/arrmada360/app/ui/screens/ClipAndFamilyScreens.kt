package com.arrmada360.app.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.arrmada360.app.api.ClipFinder
import com.arrmada360.app.api.ClipGuess
import com.arrmada360.app.data.ConfigStore
import com.arrmada360.app.data.Family
import com.arrmada360.app.data.ServiceType
import com.arrmada360.app.data.display
import com.arrmada360.app.ui.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

// ============================================================ Clip finder
@Composable
fun ClipFinderScreen(uri: String, mime: String?, nav: Navigator, back: () -> Unit) {
    val ctx = LocalContext.current
    var hint by remember { mutableStateOf("") }
    var attempt by remember { mutableIntStateOf(0) }
    var searchFor by remember { mutableStateOf<String?>(null) }
    val frames = rememberLoadable("clipframes", uri) { ClipFinder.frames(ctx, Uri.parse(uri), mime) }
    val guess = rememberLoadable("clipguess", uri, attempt, frames.data?.size ?: -1) {
        val f = frames.data ?: return@rememberLoadable null
        if (f.isEmpty()) throw Exception("Couldn't read any frames from this file")
        coroutineScope {
            val anime = async { try { ClipFinder.askTraceMoe(f[f.size / 2]) } catch (e: Exception) { null } }
            val gem = async { if (ClipFinder.geminiKey.isEmpty()) null else ClipFinder.askGemini(f, hint) }
            listOfNotNull(gem.await(), anime.await()).sortedByDescending { it.confidence }
        }
    }
    AppScaffold(title = "What is this?", isRoot = false, onBack = back) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            frames.data?.let { f ->
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                    items(f) { b: Bitmap ->
                        Image(b.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.height(80.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)))
                    }
                }
            }
            val s = searchFor
            if (s != null) {
                Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Results for “$s”", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { searchFor = null }) { Text("Back") }
                }
                SearchResults(s, nav)
                return@Column
            }
            if (ClipFinder.geminiKey.isEmpty()) Panel(Modifier.fillMaxWidth().padding(12.dp), tint = Warn, onClick = { nav.push(Route.SettingsDiscovery) }) {
                Column(Modifier.padding(14.dp)) {
                    Text("Add a free Gemini key for movies and TV", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Without it only anime can be recognised. Get one at aistudio.google.com → Get API key, then paste it in Settings → Discovery sources.",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            when {
                frames.error != null -> ErrorBox(frames.error ?: "") { frames.load() }
                frames.data == null || (guess.loading && guess.data == null) -> Column(Modifier.fillMaxWidth().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(); Spacer(Modifier.height(10.dp))
                    Text(if (frames.data == null) "Reading the clip…" else "Looking at the frames…", style = MaterialTheme.typography.bodySmall)
                }
                guess.error != null -> ErrorBox(guess.error ?: "") { attempt++ }
                else -> {
                    val list = guess.data.orEmpty()
                    androidx.compose.foundation.lazy.LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (list.isEmpty()) item { EmptyBox("Couldn't recognise this clip. Try adding a hint below, or a clearer clip with faces or titles.") }
                        items(list) { g -> GuessCard(g) { searchFor = g.title } }
                        item {
                            Panel(Modifier.fillMaxWidth(), tint = Accent) {
                                Column(Modifier.padding(14.dp)) {
                                    Text("Not right?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(hint, { hint = it }, label = { Text("Add a hint (actor, language, year…)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                                        Button(onClick = { attempt++ }, enabled = ClipFinder.geminiKey.isNotEmpty()) { Text("Try again") }
                                        OutlinedButton(onClick = { if (hint.isNotBlank()) searchFor = hint }) { Text("Search my hint") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuessCard(g: ClipGuess, onFind: () -> Unit) {
    val c = when { g.confidence >= 75 -> Good; g.confidence >= 45 -> Warn; else -> Bad }
    Panel(Modifier.fillMaxWidth(), tint = c, onClick = onFind) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (g.isTv) Icons.Filled.Tv else Icons.Filled.Movie, null, tint = c)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(g.title + (g.year?.let { " ($it)" } ?: ""), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    val ep = listOfNotNull(g.season?.let { "Season $it" }, g.episode?.let { "Episode $it" }).joinToString(" · ")
                    Text(listOf(if (g.isTv) "TV show" else "Movie", ep).filter { it.isNotEmpty() }.joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                RingGauge(g.confidence / 100f, c, Modifier.size(48.dp), stroke = 5.dp) { Text("${g.confidence}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium) }
            }
            if (g.reason.isNotBlank()) Text(g.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("by ${g.source}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Button(onClick = onFind) { Icon(Icons.Filled.Search, null); Text(" View or add") }
            }
        }
    }
}

// ============================================================ IMDb / TMDB link
@Composable
fun LinkLookupScreen(link: String, nav: Navigator, back: () -> Unit) {
    val l = rememberLoadable("linklookup", link) { ClipFinder.titleFromLink(link) ?: throw Exception("Couldn't read a title from this link. A TMDB key in Settings → Discovery sources helps.") }
    AppScaffold(title = "Open link", isRoot = false, onBack = back) { pad ->
        Column(Modifier.padding(pad)) {
            Text(link, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, modifier = Modifier.padding(horizontal = 16.dp))
            LoadableContent(l) { (title, _) -> SearchResults(title, nav) }
        }
    }
}

// ============================================================ Family mode
@Composable
fun FamilySettingsScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    var allowed by remember { mutableStateOf(Family.allowed.ifEmpty { ConfigStore.services.filter { it.type == ServiceType.SEERR || it.type == ServiceType.TAUTULLI }.map { it.id }.toSet() }) }
    var serverTab by remember { mutableStateOf(Family.showServerTab) }
    var pin by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    AppScaffold(title = "Family mode", isRoot = false, onBack = back) { pad ->
        androidx.compose.foundation.lazy.LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("Hand your phone or tablet to family without worrying. In family mode they can browse movies, shows and the calendar, and open only the apps you pick. Nothing can be deleted, edited or added, and Settings is locked behind your PIN.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(4.dp))
            }
            item { SectionTitle("Apps family can open") }
            items(ConfigStore.services.filter { !it.type.privateArt }, key = { it.id }) { c ->
                Panel(Modifier.fillMaxWidth(), tint = c.type.tint, onClick = { allowed = if (c.id in allowed) allowed - c.id else allowed + c.id }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ServiceLogo(c.type, 28.dp, c.name); Spacer(Modifier.width(10.dp))
                        Text(c.display(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Checkbox(c.id in allowed, { allowed = if (it) allowed + c.id else allowed - c.id })
                    }
                }
            }
            item {
                Panel(Modifier.fillMaxWidth(), tint = Accent) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Show the Server tab", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Disk space, download history and health on the dashboard", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(serverTab, { serverTab = it })
                    }
                }
            }
            item { SectionTitle(if (Family.hasPin) "Change PIN (optional)" else "Set a PIN to leave family mode") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(pin, { pin = it.filter { c -> c.isDigit() }.take(8) }, label = { Text("PIN (4–8 digits)") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(pin2, { pin2 = it.filter { c -> c.isDigit() }.take(8) }, label = { Text("Repeat PIN") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth())
                }
            }
            item {
                GradientButton("Save and switch to family mode", Modifier.fillMaxWidth()) {
                    when {
                        !Family.hasPin && pin.length < 4 -> ctx.toast("Set a PIN of at least 4 digits first")
                        pin.isNotEmpty() && pin != pin2 -> ctx.toast("The two PINs don't match")
                        pin.isNotEmpty() && pin.length < 4 -> ctx.toast("PIN needs at least 4 digits")
                        else -> {
                            if (pin.isNotEmpty()) Family.setPin(pin)
                            Family.allowed = allowed; Family.showServerTab = serverTab
                            Family.active = true
                            ctx.toast("Family mode on")
                            back()
                        }
                    }
                }
            }
            if (Family.hasPin) item {
                TextButton(onClick = {
                    if (pin.isNotEmpty() && pin != pin2) ctx.toast("The two PINs don't match")
                    else { if (pin.length >= 4) Family.setPin(pin); Family.allowed = allowed; Family.showServerTab = serverTab; ctx.toast("Saved") }
                }, modifier = Modifier.fillMaxWidth()) { Text("Just save, don't switch") }
            }
        }
    }
}

/** Asks for the PIN to leave family mode. */
@Composable
fun FamilyExitDialog(onDone: () -> Unit) {
    val ctx = LocalContext.current
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDone,
        icon = { Icon(Icons.Filled.FamilyRestroom, null) },
        title = { Text("Leave family mode") },
        text = {
            OutlinedTextField(pin, { pin = it.filter { c -> c.isDigit() }.take(8) }, label = { Text("PIN") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
        },
        confirmButton = {
            TextButton(onClick = {
                if (Family.checkPin(pin)) { Family.active = false; ctx.toast("Family mode off"); onDone() } else { ctx.toast("Wrong PIN"); pin = "" }
            }) { Text("Unlock") }
        },
        dismissButton = { TextButton(onClick = onDone) { Text("Cancel") } },
    )
}

@Composable
fun FamilyBlockedScreen(nav: Navigator, back: () -> Unit) {
    var exit by remember { mutableStateOf(false) }
    val seerr = ConfigStore.services.firstOrNull { it.type == ServiceType.SEERR && it.id in Family.allowed }
    AppScaffold(title = "Family mode", isRoot = false, onBack = back) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Filled.FamilyRestroom, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(12.dp))
            Text("This needs the admin", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Adding, changing and deleting are switched off in family mode.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            if (seerr != null) { GradientButton("Request it in ${seerr.display()}") { nav.root(Route.Service(seerr.id)) }; Spacer(Modifier.height(10.dp)) }
            OutlinedButton(onClick = { exit = true }) { Icon(Icons.Filled.Lock, null); Text(" Leave family mode") }
        }
    }
    if (exit) FamilyExitDialog { exit = false }
}
