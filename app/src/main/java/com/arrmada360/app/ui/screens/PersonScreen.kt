package com.arrmada360.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.arrmada360.app.api.ArrApi
import com.arrmada360.app.api.People
import com.arrmada360.app.api.PersonCredit
import com.arrmada360.app.data.ConfigStore
import com.arrmada360.app.data.Family
import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.data.ServiceType
import com.arrmada360.app.ui.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.time.Period

private data class LibHit(val cfg: ServiceConfig, val itemId: Int, val complete: Boolean, val hasFiles: Boolean)

/** Which of your Radarr / Sonarr instances already have a TMDB id. */
private suspend fun libraryByTmdb(): Pair<Map<Int, LibHit>, Map<Int, LibHit>> = coroutineScope {
    val arrs = ConfigStore.services.filter { it.type == ServiceType.RADARR || it.type == ServiceType.SONARR }
    val all = arrs.map { c -> async { try { ArrApi(c).library().map { c to it } } catch (e: Exception) { emptyList() } } }.awaitAll().flatten()
    fun hit(p: Pair<ServiceConfig, com.arrmada360.app.api.MediaItem>) = LibHit(p.first, p.second.id, p.second.complete, p.second.sizeOnDisk > 0)
    val movies = all.filter { it.first.type == ServiceType.RADARR }.mapNotNull { p -> p.second.raw.optInt("tmdbId").takeIf { it > 0 }?.let { it to hit(p) } }.toMap()
    val shows = all.filter { it.first.type == ServiceType.SONARR }.mapNotNull { p -> p.second.raw.optInt("tmdbId").takeIf { it > 0 }?.let { it to hit(p) } }.toMap()
    movies to shows
}

@Composable
fun PersonScreen(personId: Int, name: String, nav: Navigator, back: () -> Unit) {
    val uri = LocalUriHandler.current
    val p = rememberLoadable("person", personId) { People.person(personId) }
    val lib = rememberLoadable("person-lib") { libraryByTmdb() }
    var kind by rememberSaveable { mutableIntStateOf(0) }   // all / movies / tv
    var role by rememberSaveable { mutableIntStateOf(0) }   // acting / crew
    var sort by rememberSaveable { mutableIntStateOf(0) }   // popular / newest / oldest
    var hideOwned by rememberSaveable { mutableStateOf(false) }
    var bioOpen by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf<PersonCredit?>(null) }

    AppScaffold(title = p.data?.name ?: name, isRoot = false, onBack = back) { pad ->
        LoadableContent(p, Modifier.padding(pad)) { d ->
            val (movies, shows) = lib.data ?: (emptyMap<Int, LibHit>() to emptyMap())
            fun owned(c: PersonCredit) = if (c.isTv) shows[c.tmdbId] else movies[c.tmdbId]
            val hasCrew = d.credits.any { it.crew }
            val acting = d.credits.filter { !it.crew }
            val base = if (role == 1 && hasCrew) d.credits.filter { it.crew } else if (acting.isEmpty()) d.credits else acting
            val shown = base
                .filter { kind == 0 || (kind == 1 && !it.isTv) || (kind == 2 && it.isTv) }
                .filter { !hideOwned || owned(it) == null }
                .filter { !(it.isTv && it.role.contains("Self", true) && kind != 2) }
                .let { list ->
                    when (sort) {
                        1 -> list.sortedByDescending { it.date.ifEmpty { "0000" } }
                        2 -> list.filter { it.date.isNotEmpty() }.sortedBy { it.date }
                        else -> list.sortedByDescending { it.popularity }
                    }
                }
            val ownedCount = d.credits.count { owned(it) != null }
            LazyVerticalGrid(GridCells.Adaptive(108.dp), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(150.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(Rainbow[0], Accent2, Pink, Rainbow[0]))).padding(4.dp).clip(CircleShape),
                            contentAlignment = Alignment.Center) {
                            Text(d.name.take(1), style = MaterialTheme.typography.headlineLarge, color = Color.White)
                            if (d.photo != null) AsyncImage(d.photo, d.name, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize().clip(CircleShape))
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(d.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                            if (d.dept.isNotEmpty()) Pill(d.dept, Accent2)
                            Pill("${d.credits.distinctBy { it.tmdbId to it.isTv }.size} titles", Accent)
                            if (ownedCount > 0) Pill("$ownedCount in library", Good)
                        }
                        val life = buildString {
                            val b = runCatching { LocalDate.parse(d.birthday) }.getOrNull()
                            val dd = runCatching { LocalDate.parse(d.deathday) }.getOrNull()
                            if (b != null) {
                                append("Born ").append(fmtDate(d.birthday))
                                if (dd == null) append(" (age ${Period.between(b, LocalDate.now()).years})")
                            }
                            if (dd != null) append(" · Died ").append(fmtDate(d.deathday)).append(b?.let { " (aged ${Period.between(it, dd).years})" } ?: "")
                        }
                        if (life.isNotEmpty()) Text(life, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                        if (d.birthplace.isNotEmpty()) Text(d.birthplace, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        if (d.bio.isNotBlank()) Text(
                            d.bio, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface,
                            maxLines = if (bioOpen) 200 else 5, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 12.dp).animateContentSize().clickable { bioOpen = !bioOpen },
                        )
                        if (d.bio.length > 300) TextButton(onClick = { bioOpen = !bioOpen }) { Text(if (bioOpen) "Show less" else "Read full biography") }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                            if (d.imdbId.isNotEmpty()) OutlinedButton(onClick = { uri.openUri("https://www.imdb.com/name/${d.imdbId}") }) { Text("IMDb") }
                            OutlinedButton(onClick = { uri.openUri("https://www.themoviedb.org/person/${d.id}") }) { Text("TMDB") }
                        }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        SectionTitle("Filmography")
                        ChipRow(listOf("All", "Movies", "TV"), kind, { kind = it },
                            counts = listOf(base.size, base.count { !it.isTv }, base.count { it.isTv }))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (hasCrew && acting.isNotEmpty()) ChipRow(listOf("Acting", "Crew"), role, { role = it }, Modifier.weight(1f))
                            else Spacer(Modifier.weight(1f))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ChipRow(listOf("Popular", "Newest", "Oldest"), sort, { sort = it }, Modifier.weight(1f))
                            FilterChip(hideOwned, { hideOwned = !hideOwned }, label = { Text("Not in library") })
                        }
                        if (lib.loading && lib.data == null) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 8.dp))
                    }
                }
                if (shown.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { EmptyBox("Nothing here") }
                items(shown, key = { "${it.tmdbId}-${it.isTv}-${it.crew}" }) { c ->
                    val hit = owned(c)
                    CreditTile(c, hit) {
                        if (hit != null) nav.push(Route.ArrDetail(hit.cfg.id, hit.itemId)) else if (!Family.active) adding = c
                    }
                }
            }
        }
    }
    adding?.let { c -> AddCreditDialog(c, nav) { adding = null } }
}

@Composable
private fun CreditTile(c: PersonCredit, hit: LibHit?, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick)) {
        Box {
            Poster(c.poster, Modifier.fillMaxWidth(), fallback = c.title)
            Box(Modifier.align(Alignment.TopStart).padding(5.dp).clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                Text(if (c.isTv) "TV" else "MOVIE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Box(Modifier.align(Alignment.TopEnd).padding(5.dp)) {
                when {
                    hit == null -> if (!Family.active) Box(Modifier.size(26.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, "Add", tint = Color.Black, modifier = Modifier.size(18.dp))
                    }
                    hit.complete -> MediaStatusIcon(MediaStatus.IN_LIBRARY)
                    hit.hasFiles -> MediaStatusIcon(MediaStatus.LIB_PARTIAL)
                    else -> MediaStatusIcon(MediaStatus.WAITING)
                }
            }
        }
        Text(c.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
        val sub = listOf(if (c.year > 0) "${c.year}" else "TBA", c.role).filter { it.isNotBlank() }.joinToString(" · ")
        Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AddCreditDialog(c: PersonCredit, nav: Navigator, done: () -> Unit) {
    val targets = ConfigStore.services.filter { it.type == if (c.isTv) ServiceType.SONARR else ServiceType.RADARR }
    if (targets.isEmpty()) {
        AlertDialog(onDismissRequest = done, title = { Text("No ${if (c.isTv) "Sonarr" else "Radarr"} added") },
            text = { Text("Add ${if (c.isTv) "Sonarr" else "Radarr"} in Settings to add ${c.title}.") },
            confirmButton = { TextButton(onClick = done) { Text("OK") } })
        return
    }
    val term = if (c.isTv) c.title else "tmdb:${c.tmdbId}"
    if (targets.size == 1) { LaunchedEffect(Unit) { done(); nav.push(Route.ArrAdd(targets[0].id, term)) }; return }
    AlertDialog(
        onDismissRequest = done,
        title = { Text("Add ${c.title}${if (c.year > 0) " (${c.year})" else ""}") },
        text = {
            Column {
                targets.forEach { t ->
                    Row(Modifier.fillMaxWidth().clickable { done(); nav.push(Route.ArrAdd(t.id, term)) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        ServiceLogo(t.type, 28.dp, t.name); Spacer(Modifier.width(10.dp)); Text(t.name, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = done) { Text("Cancel") } },
    )
}
