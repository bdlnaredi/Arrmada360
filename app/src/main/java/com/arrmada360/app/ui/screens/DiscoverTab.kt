package com.arrmada360.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.arrmada360.app.api.ArrApi
import com.arrmada360.app.api.CalEntry
import com.arrmada360.app.api.Discover
import com.arrmada360.app.api.DiscoverItem
import com.arrmada360.app.api.DiscoverPerson
import com.arrmada360.app.api.DiscoverRow
import com.arrmada360.app.api.MediaItem
import com.arrmada360.app.api.SeerrApi
import com.arrmada360.app.api.SeerrMedia
import com.arrmada360.app.data.ConfigStore
import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.data.ServiceType
import com.arrmada360.app.ui.Accent2
import com.arrmada360.app.ui.Good
import com.arrmada360.app.ui.GradientButton
import com.arrmada360.app.ui.Loadable
import com.arrmada360.app.ui.MenuRow
import com.arrmada360.app.ui.Navigator
import com.arrmada360.app.ui.Pill
import com.arrmada360.app.ui.PosterTag
import com.arrmada360.app.ui.MediaStatus
import com.arrmada360.app.ui.MediaStatusIcon
import com.arrmada360.app.ui.Pink
import com.arrmada360.app.ui.Poster
import com.arrmada360.app.ui.Rainbow
import com.arrmada360.app.ui.Route
import com.arrmada360.app.ui.ServiceBadge
import com.arrmada360.app.ui.Warn
import com.arrmada360.app.ui.fmtDate
import com.arrmada360.app.ui.rememberLoadable
import com.arrmada360.app.ui.rememberRunner
import com.arrmada360.app.ui.relativeDay
import com.arrmada360.app.ui.parseDate
import com.arrmada360.app.ui.shimmer

/** Library lookup so discovery tiles can show "in library" and jump straight to the item. */
private class LibIndex(val byTmdb: Map<Int, Int>, val byTvdb: Map<Int, Int>, val items: List<MediaItem>) {
    private val byId = items.associateBy { it.id }
    fun find(i: DiscoverItem): Int? = byTmdb[i.tmdbId] ?: if (i.tvdbId > 0) byTvdb[i.tvdbId] else null
    /** Downloaded / partly downloaded / added but waiting – or null when not in the library. */
    fun status(i: DiscoverItem): MediaStatus? {
        val m = find(i)?.let { byId[it] } ?: return null
        return when {
            m.complete -> MediaStatus.IN_LIBRARY
            m.sizeOnDisk > 0 || m.progressText.substringBefore("/").trim().toIntOrNull()?.let { it > 0 } == true -> MediaStatus.LIB_PARTIAL
            else -> MediaStatus.WAITING
        }
    }
}

/** The Movies / TV Shows tab on the dashboard: rows of things to discover, like nzb360. */
@Composable
fun DiscoverTab(isMovie: Boolean, arr: ServiceConfig?, nav: Navigator) {
    val type = if (isMovie) "movie" else "tv"
    val api = remember(arr) { arr?.let { ArrApi(it) } }
    val lib = rememberLoadable(arr, "discover-lib") {
        val items = api?.library() ?: emptyList()
        LibIndex(
            items.associate { it.raw.optInt("tmdbId") to it.id }.filterKeys { it > 0 },
            items.associate { it.raw.optInt("tvdbId") to it.id }.filterKeys { it > 0 },
            items,
        )
    }
    // Upcoming = what's coming in *your* library (Radarr movies / Sonarr episodes)
    val libUpcoming = rememberLoadable(arr, "discover-libup") {
        api?.calendar(0, 90)?.filter { !it.hasFile && (parseDate(it.date)?.let { d -> !d.isBefore(java.time.LocalDate.now()) } ?: true) }?.take(40) ?: emptyList()
    }
    val recent = rememberLoadable(arr, "discover-recent") {
        when {
            api == null -> emptyList()
            isMovie -> api.library().filter { it.complete }
                .sortedByDescending { it.raw.optJSONObject("movieFile")?.optString("dateAdded") ?: it.added }
                .take(20).map { CalEntry(it.id, it.id, it.title, it.year.toString(), it.raw.optJSONObject("movieFile")?.optString("dateAdded") ?: "", true, it.poster) }
            else -> try { api.recentImports() } catch (e: Exception) { emptyList() }
        }
    }
    val trending = rememberLoadable(type, "trending", ConfigStore.tmdbKey, ConfigStore.traktClientId) { Discover.trending(type) }
    val anticipated = rememberLoadable(type, "anticipated", ConfigStore.tmdbKey, ConfigStore.traktClientId) { Discover.anticipated(type) }
    val upcoming = rememberLoadable(type, "upcoming", ConfigStore.tmdbKey, ConfigStore.traktClientId) { Discover.upcoming(type) }
    val popular = rememberLoadable(type, "popular", ConfigStore.tmdbKey, ConfigStore.traktClientId) { Discover.popular(type) }
    val people = rememberLoadable("people", ConfigStore.tmdbKey) { Discover.people() }
    val libData = lib.data
    val recommended = rememberLoadable(type, "recommended", libData?.items?.size, ConfigStore.tmdbKey) {
        val l = libData ?: return@rememberLoadable DiscoverRow(emptyList<DiscoverItem>(), "")
        val seeds = l.items.sortedByDescending { it.added }.map { it.raw.optInt("tmdbId") }.filter { it > 0 }
        Discover.recommended(type, seeds, l.byTmdb.keys)
    }
    var openItem by remember { mutableStateOf<DiscoverItem?>(null) }
    var openPerson by remember { mutableStateOf<DiscoverPerson?>(null) }

    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
        if (!Discover.hasAnySource()) item { SourcesHint(nav) }
        item(key = "guide") { DashboardGuideCards(nav) }
        item(key = "legend") { com.arrmada360.app.ui.StatusLegend(Modifier.padding(start = 16.dp, top = 8.dp)) }
        item(key = "hero") { HeroRow(trending, if (isMovie) "Trending movies" else "Trending shows", lib.data) { openItem = it } }
        if (arr != null) item(key = "recent") {
            EntryRow("Recently downloaded", Icons.Filled.DownloadDone, Good, recent, arr) { e -> nav.push(Route.ArrDetail(arr.id, e.parentId)) }
        }
        if (arr != null) item(key = "libup") {
            EntryRow(if (isMovie) "Upcoming movies" else "Upcoming episodes", Icons.Filled.CalendarMonth, Warn, libUpcoming, arr, datePrefix = "📅 ",
                emptyHint = if (isMovie) "No monitored movies releasing in the next 90 days" else "No monitored episodes airing in the next 90 days") { e ->
                nav.push(Route.ArrDetail(arr.id, e.parentId))
            }
        }
        item(key = "rec") { ItemRow("Recommended for you", Icons.Filled.AutoAwesome, Accent2, recommended, lib.data, hideIfEmpty = true) { openItem = it } }
        item(key = "soon") { ItemRow("Releasing soon", Icons.Filled.NewReleases, Rainbow[0], upcoming, lib.data, showDate = true) { openItem = it } }
        item(key = "ant") { ItemRow("Most anticipated", Icons.Filled.Whatshot, Pink, anticipated, lib.data) { openItem = it } }
        item(key = "pop") { ItemRow(if (isMovie) "Popular movies" else "Popular shows", Icons.Filled.LocalFireDepartment, Rainbow[5], popular, lib.data) { openItem = it } }
        item(key = "people") { PeopleRow(people) { openPerson = it } }
        if (arr != null) item(key = "lib") {
            Spacer(Modifier.height(8.dp))
            MenuRow(Icons.Filled.VideoLibrary, "Browse your full library", "${lib.data?.items?.size ?: "…"} ${if (isMovie) "movies" else "shows"} in ${arr.name}", arr.type.tint) {
                nav.root(Route.Service(arr.id))
            }
        }
    }

    openItem?.let { i -> DiscoverSheet(i, isMovie, lib.data, nav) { openItem = null } }
    openPerson?.let { p -> LaunchedEffect(p) { openPerson = null; nav.push(Route.Person(p.id, p.name)) } }
}

@Composable
private fun SourcesHint(nav: Navigator) {
    Column(
        Modifier.padding(12.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Accent2.copy(alpha = 0.35f), Pink.copy(alpha = 0.18f)))).padding(16.dp),
    ) {
        Text("Turn on discovery", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(
            "Add Overseerr/Jellyseerr, or a free TMDB API key or Trakt Client ID in Settings, to see trending, upcoming, popular and recommended titles here.",
            style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f),
        )
        Spacer(Modifier.height(10.dp))
        GradientButton("Open Settings") { nav.push(Route.Settings) }
    }
}

@Composable
private fun RowHeader(title: String, icon: ImageVector, color: Color, source: String?) {
    Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(Brush.linearGradient(listOf(color.copy(alpha = 0.6f), color.copy(alpha = 0.25f)))), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color.White, modifier = Modifier.weight(1f))
        if (!source.isNullOrEmpty()) Pill(source, color)
    }
}

@Composable
private fun ShimmerRow(width: Int = 120, height: Int = 180) {
    Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(4) { Box(Modifier.size(width.dp, height.dp).clip(RoundedCornerShape(12.dp)).shimmer()) }
    }
}

@Composable
private fun HeroRow(l: Loadable<DiscoverRow<DiscoverItem>>, title: String, lib: LibIndex?, onClick: (DiscoverItem) -> Unit) {
    val d = l.data
    if (d != null && d.items.isEmpty()) return
    RowHeader(title, Icons.Filled.TrendingUp, MaterialTheme.colorScheme.primary, d?.source)
    if (d == null) { ShimmerRow(290, 165); return }
    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(d.items.take(15), key = { "h" + it.tmdbId }) { i ->
            Box(
                Modifier.width(290.dp).height(165.dp).clip(RoundedCornerShape(18.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp)).clickable { onClick(i) },
            ) {
                Poster(i.backdrop ?: i.poster, Modifier.fillMaxWidth(), ratio = 290f / 165f, corner = 0.dp, fallback = i.title)
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))))
                Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                    Text(i.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (i.year.isNotEmpty()) Text(i.year, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
                        if (i.rating > 0) PosterTag("★ %.1f".format(i.rating), Warn)
                        if (i.extra.isNotEmpty()) PosterTag(i.extra, MaterialTheme.colorScheme.primary)
                        val st = lib?.status(i) ?: MediaStatus.fromSeerr(i.seerrStatus)
                        if (st != null) MediaStatusIcon(st, 24.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemRow(
    title: String,
    icon: ImageVector,
    color: Color,
    l: Loadable<DiscoverRow<DiscoverItem>>,
    lib: LibIndex?,
    showDate: Boolean = false,
    hideIfEmpty: Boolean = false,
    onClick: (DiscoverItem) -> Unit,
) {
    val d = l.data
    if (d != null && d.items.isEmpty()) {
        if (!hideIfEmpty && Discover.hasAnySource()) {
            RowHeader(title, icon, color, null)
            Text("Nothing to show right now", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
        }
        return
    }
    if (d == null && l.error != null) return
    RowHeader(title, icon, color, d?.source)
    if (d == null) { ShimmerRow(); return }
    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(d.items, key = { title + it.tmdbId + it.title }) { i -> PosterCard(i, color, lib?.status(i), showDate) { onClick(i) } }
    }
}

@Composable
private fun PosterCard(i: DiscoverItem, color: Color, libStatus: MediaStatus?, showDate: Boolean, onClick: () -> Unit) {
    Column(Modifier.width(120.dp).clickable(onClick = onClick)) {
        Box(Modifier.clip(RoundedCornerShape(12.dp)).border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp))) {
            Poster(i.poster, Modifier.fillMaxWidth(), corner = 12.dp, fallback = i.title)
            if (i.rating > 0) Box(Modifier.align(Alignment.TopStart).padding(5.dp)) { PosterTag("★ %.1f".format(i.rating), Warn) }
            val st = libStatus ?: MediaStatus.fromSeerr(i.seerrStatus)
            if (st != null) Box(Modifier.align(Alignment.TopEnd).padding(5.dp)) { MediaStatusIcon(st) }
            if (showDate && i.date.isNotEmpty()) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))).padding(6.dp)) {
                Text(relativeDay(parseDate(i.date)), style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(i.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(listOf(i.year, i.extra).filter { it.isNotEmpty() }.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun EntryRow(
    title: String,
    icon: ImageVector,
    color: Color,
    l: Loadable<List<CalEntry>>,
    arr: ServiceConfig,
    datePrefix: String = "⬇ ",
    emptyHint: String? = null,
    onClick: (CalEntry) -> Unit,
) {
    val d = l.data
    if (d != null && d.isEmpty()) {
        if (emptyHint != null) {
            RowHeader(title, icon, color, arr.name)
            Text(emptyHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
        }
        return
    }
    RowHeader(title, icon, color, arr.name)
    if (d == null) { ShimmerRow(); return }
    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(d, key = { "r" + it.id + it.date }) { e ->
            Column(Modifier.width(120.dp).clickable { onClick(e) }) {
                Box(Modifier.clip(RoundedCornerShape(12.dp)).border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(12.dp))) {
                    Poster(e.poster, Modifier.fillMaxWidth(), corner = 12.dp, fallback = e.title)
                    Box(Modifier.align(Alignment.TopEnd).padding(5.dp)) { ServiceBadge(arr.type, 22.dp, arr.name) }
                    if (e.date.isNotEmpty()) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))).padding(6.dp)) {
                        Text(datePrefix + relativeDay(parseDate(e.date)), style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(e.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(e.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun PeopleRow(l: Loadable<DiscoverRow<DiscoverPerson>>, onClick: (DiscoverPerson) -> Unit) {
    val d = l.data
    if (d != null && d.items.isEmpty()) return
    if (d == null && l.error != null) return
    RowHeader("Popular people", Icons.Filled.People, Rainbow[0], d?.source)
    if (d == null) { ShimmerRow(84, 84); return }
    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(d.items, key = { "p" + it.id }) { p ->
            Column(Modifier.width(88.dp).clickable { onClick(p) }, horizontalAlignment = Alignment.CenterHorizontally) {
                val c = Rainbow[p.id % Rainbow.size]
                Box(
                    Modifier.size(84.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(c, Accent2, Pink, c))).padding(3.dp)
                        .clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(p.name.take(1), style = MaterialTheme.typography.titleLarge, color = c, fontWeight = FontWeight.Bold)
                    if (p.photo != null) AsyncImage(model = p.photo, contentDescription = p.name, contentScale = ContentScale.Crop, modifier = Modifier.size(78.dp).clip(CircleShape))
                }
                Spacer(Modifier.height(6.dp))
                Text(p.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (p.dept.isNotEmpty()) Text(p.dept, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

// ------------------------------------------------------------ detail sheets
@Composable
private fun DiscoverSheet(i: DiscoverItem, isMovie: Boolean, lib: LibIndex?, nav: Navigator, onDismiss: () -> Unit) {
    val uri = LocalUriHandler.current
    val run = rememberRunner()
    val arrs = ConfigStore.services.filter { it.type == if (isMovie) ServiceType.RADARR else ServiceType.SONARR }
    val seerr = Discover.seerr()
    val libId = lib?.find(i)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Box(Modifier.fillMaxWidth().height(200.dp)) {
                if (i.backdrop != null) AsyncImage(model = i.backdrop, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background))))
                Row(Modifier.align(Alignment.BottomStart).padding(16.dp), verticalAlignment = Alignment.Bottom) {
                    Poster(i.poster, Modifier.width(96.dp), fallback = i.title)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(i.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (i.year.isNotEmpty()) Pill(i.year, MaterialTheme.colorScheme.primary)
                            if (i.rating > 0) Pill("★ %.1f".format(i.rating), Warn)
                            Pill(if (isMovie) "Movie" else "TV", Accent2)
                        }
                        if (i.date.isNotEmpty()) Text(fmtDate(i.date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (i.overview.isNotEmpty()) Text(i.overview, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val firstArr = arrs.firstOrNull()
                if (libId != null && firstArr != null) {
                    val st = lib?.status(i) ?: MediaStatus.IN_LIBRARY
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MediaStatusIcon(st, 24.dp); Spacer(Modifier.width(8.dp))
                        Text(st.label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                    GradientButton("Open in ${firstArr.name}", Modifier.fillMaxWidth()) { onDismiss(); nav.push(Route.ArrDetail(firstArr.id, libId)) }
                } else {
                    arrs.forEach { a ->
                        GradientButton("+ Add to ${a.name}", Modifier.fillMaxWidth()) {
                            onDismiss()
                            val term = if (isMovie && i.tmdbId > 0) "tmdb:${i.tmdbId}" else if (!isMovie && i.tvdbId > 0) "tvdb:${i.tvdbId}" else i.title
                            nav.push(Route.ArrAdd(a.id, term))
                        }
                    }
                    if (seerr != null && i.tmdbId > 0) {
                        val seerrSt = MediaStatus.fromSeerr(i.seerrStatus)
                        if (seerrSt != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) { MediaStatusIcon(seerrSt, 22.dp); Spacer(Modifier.width(8.dp)); Text(seerrSt.label + " in Seerr", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }
                        } else OutlinedButton(onClick = {
                            onDismiss()
                            run("Requested ${i.title}") {
                                SeerrApi(seerr).request(SeerrMedia(i.tmdbId, i.mediaType, i.title, i.year, i.poster, i.backdrop, i.overview, i.seerrStatus))
                            }
                        }, modifier = Modifier.fillMaxWidth()) {
                            ServiceBadge(ServiceType.SEERR, 22.dp, seerr.name); Spacer(Modifier.width(8.dp)); Text("Request with ${seerr.name}")
                        }
                    }
                    if (arrs.isEmpty() && seerr == null) Text("Add ${if (isMovie) "Radarr" else "Sonarr"} or Seerr to add this.", style = MaterialTheme.typography.bodySmall, color = Warn)
                }
                if (i.tmdbId > 0) OutlinedButton(onClick = { uri.openUri(Discover.webUrl(i)) }, modifier = Modifier.fillMaxWidth()) { Text("Open on TMDB") }
            }
        }
    }
}

@Composable
private fun PersonSheet(p: DiscoverPerson, onDismiss: () -> Unit) {
    val uri = LocalUriHandler.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(130.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(Rainbow[0], Accent2, Pink, Rainbow[0]))).padding(4.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                if (p.photo != null) AsyncImage(model = p.photo, contentDescription = p.name, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize().clip(CircleShape))
                else Text(p.name.take(1), style = MaterialTheme.typography.headlineMedium, color = Color.White)
            }
            Spacer(Modifier.height(12.dp))
            Text(p.name, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            if (p.dept.isNotEmpty()) Pill(p.dept, Accent2)
            if (p.knownFor.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text("Known for", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                p.knownFor.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = { uri.openUri(Discover.personUrl(p)) }, modifier = Modifier.fillMaxWidth()) { Text("Open on TMDB") }
        }
    }
}

@Suppress("unused")
private val keepStar = Icons.Filled.Star
