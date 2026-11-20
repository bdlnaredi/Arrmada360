package com.arrmada360.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arrmada360.app.api.ArrApi
import com.arrmada360.app.api.TautulliApi
import com.arrmada360.app.api.TopGroup
import com.arrmada360.app.data.ConfigStore
import com.arrmada360.app.data.ServiceType
import com.arrmada360.app.ui.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private class YearData(
    val year: Int,
    val moviesPerMonth: List<Int>,
    val episodesPerMonth: List<Int>,
    val grabbedBytes: Long,
    val grabs: Int,
    val moviesInLibrary: Int,
    val showsInLibrary: Int,
    val librarySize: Long,
    val moviesAddedThisYear: Int,
    val showsAddedThisYear: Int,
    val topGenres: List<Pair<String, Int>>,
    val diskUsed: Long,
    val diskTotal: Long,
    val plays: Long?,
    val top: List<TopGroup>,
    val errors: List<String>,
)

private suspend fun loadYear(year: Int): YearData = coroutineScope {
    val from = LocalDate.of(year, 1, 1)
    val to = minOf(LocalDate.of(year, 12, 31), LocalDate.now())
    val radarrs = ConfigStore.services.filter { it.type == ServiceType.RADARR }
    val sonarrs = ConfigStore.services.filter { it.type == ServiceType.SONARR }
    val errors = mutableListOf<String>()
    fun perMonth(dates: List<LocalDate>) = (1..12).map { m -> dates.count { it.monthValue == m } }

    val movieImports = radarrs.map { c -> async { try { ArrApi(c).historyBetween(3, from, to).map { it.first } } catch (e: Exception) { errors += c.name; emptyList() } } }
    val epImports = sonarrs.map { c -> async { try { ArrApi(c).historyBetween(3, from, to).map { it.first } } catch (e: Exception) { errors += c.name; emptyList() } } }
    val grabs = (radarrs + sonarrs).map { c -> async { try { ArrApi(c).historyBetween(1, from, to) } catch (e: Exception) { emptyList() } } }
    val libs = (radarrs + sonarrs).map { c -> async { c.type to try { ArrApi(c).library() } catch (e: Exception) { emptyList() } } }
    val disks = (radarrs + sonarrs).map { c -> async { try { ArrApi(c).diskSpace() } catch (e: Exception) { emptyList() } } }
    val taut = ConfigStore.services.firstOrNull { it.type == ServiceType.TAUTULLI }
    val days = ChronoUnit.DAYS.between(from, LocalDate.now()).toInt().coerceIn(1, 366)
    val isThisYear = year == LocalDate.now().year
    val playsJob = async {
        if (taut == null || !isThisYear) null else try { TautulliApi(taut).playsByDate(days).series.sumOf { it.second.sum() } } catch (e: Exception) { null }
    }
    val topJob = async { if (taut == null || !isThisYear) emptyList() else try { TautulliApi(taut).topStats(days) } catch (e: Exception) { emptyList() } }

    val mi = movieImports.awaitAll().flatten()
    val ei = epImports.awaitAll().flatten()
    val g = grabs.awaitAll().flatten()
    val l = libs.awaitAll()
    val movies = l.filter { it.first == ServiceType.RADARR }.flatMap { it.second }
    val shows = l.filter { it.first == ServiceType.SONARR }.flatMap { it.second }
    fun addedIn(added: String) = added.take(4) == year.toString()
    val genreCount = (movies + shows).filter { addedIn(it.added) }.flatMap { m ->
        m.raw.optJSONArray("genres")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList()
    }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(8).map { it.key to it.value }
    val dk = disks.awaitAll().flatten().distinctBy { it.path }
    YearData(
        year, perMonth(mi), perMonth(ei), g.sumOf { it.second }, g.size,
        movies.size, shows.size, (movies + shows).sumOf { it.sizeOnDisk },
        movies.count { addedIn(it.added) }, shows.count { addedIn(it.added) }, genreCount,
        dk.sumOf { it.total - it.free }, dk.sumOf { it.total }, playsJob.await(), topJob.await(), errors,
    )
}

@Composable
fun YearReviewScreen(back: () -> Unit) {
    val now = LocalDate.now().year
    var year by rememberSaveable { mutableIntStateOf(now) }
    val l = rememberLoadable("year", year) { loadYear(year) }
    AppScaffold(title = "Year in review", isRoot = false, onBack = back) { pad ->
        Box(Modifier.padding(pad)) {
            Column {
                ChipRow(listOf("$now", "${now - 1}"), if (year == now) 0 else 1, { year = if (it == 0) now else now - 1 })
                LoadableContent(l) { d -> YearBody(d) }
            }
        }
    }
    if (l.loading && l.data == null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Text("Reading a year of history… this can take a moment", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(40.dp))
    }
}

@Composable
private fun YearBody(d: YearData) {
    val movies = d.moviesPerMonth.sum(); val eps = d.episodesPerMonth.sum()
    val monthTotals = d.moviesPerMonth.zip(d.episodesPerMonth) { a, b -> a + b }
    val best = monthTotals.withIndex().maxByOrNull { it.value }
    LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Brush.linearGradient(listOf(Accent.copy(alpha = 0.85f), Accent2.copy(alpha = 0.85f), Pink.copy(alpha = 0.8f))))) {
                Column(Modifier.padding(22.dp)) {
                    Text("Your ${d.year}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White)
                    Text("on your server", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.85f))
                    Spacer(Modifier.height(16.dp))
                    Row {
                        BigNum("$movies", "movies downloaded", Modifier.weight(1f))
                        BigNum("$eps", "episodes downloaded", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row {
                        BigNum(humanBytes(d.grabbedBytes), "grabbed in ${d.grabs} releases", Modifier.weight(1f))
                        BigNum(d.plays?.toString() ?: "–", if (d.plays != null) "plays on Plex" else "plays (add Tautulli)", Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth(), tint = Accent) {
                Column(Modifier.padding(14.dp)) {
                    Text("Downloads per month", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (best != null && best.value > 0) Text("Busiest month: ${Month.of(best.index + 1).getDisplayName(TextStyle.FULL, Locale.getDefault())} with ${best.value}",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    MonthBars(d.moviesPerMonth, d.episodesPerMonth, Modifier.fillMaxWidth().height(140.dp))
                    Row(Modifier.fillMaxWidth()) {
                        (1..12).forEach { m -> Text(Month.of(m).getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { StatusDot(ServiceType.RADARR.tint); Spacer(Modifier.width(4.dp)); Text("Movies", style = MaterialTheme.typography.labelSmall) }
                        Row(verticalAlignment = Alignment.CenterVertically) { StatusDot(ServiceType.SONARR.tint); Spacer(Modifier.width(4.dp)); Text("Episodes", style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Movies added", "${d.moviesAddedThisYear}", ServiceType.RADARR.tint, Modifier.weight(1f))
                StatTile("Shows added", "${d.showsAddedThisYear}", ServiceType.SONARR.tint, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("In library", "${d.moviesInLibrary} movies · ${d.showsInLibrary} shows", Good, Modifier.weight(1.4f))
                StatTile("Library size", humanBytes(d.librarySize), Warn, Modifier.weight(1f))
            }
        }
        if (d.diskTotal > 0) item {
            Panel(Modifier.fillMaxWidth(), tint = Good) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    val f = d.diskUsed.toFloat() / d.diskTotal
                    RingGauge(f, if (f > 0.9f) Bad else Good, Modifier.size(64.dp)) { Text("${(f * 100).toInt()}%", fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Disk space used", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("${humanBytes(d.diskUsed)} of ${humanBytes(d.diskTotal)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (d.topGenres.isNotEmpty()) item {
            Panel(Modifier.fillMaxWidth(), tint = Pink) {
                Column(Modifier.padding(14.dp)) {
                    Text("Top genres you added", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    val max = d.topGenres.first().second.coerceAtLeast(1)
                    d.topGenres.forEachIndexed { i, (g, n) ->
                        Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(g, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(110.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            ProgressBar(n.toFloat() / max, Rainbow[i % Rainbow.size], Modifier.weight(1f), height = 8.dp)
                            Text(" $n", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        d.top.filter { it.id == "top_movies" || it.id == "top_tv" || it.id == "top_users" }.forEach { g ->
            item(key = g.id) {
                Column {
                    SectionTitle(g.title + " this year")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(g.rows.take(10)) { i, e ->
                            Column(Modifier.width(if (g.id == "top_users") 80.dp else 104.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                if (g.id == "top_users") Box(Modifier.size(64.dp).clip(CircleShape).background(Rainbow[i % Rainbow.size]), contentAlignment = Alignment.Center) {
                                    Text(e.title.take(1), fontWeight = FontWeight.Bold, color = Color.White)
                                    if (e.thumb != null) coil.compose.AsyncImage(e.thumb, null, modifier = Modifier.matchParentSize().clip(CircleShape))
                                } else Poster(e.thumb, Modifier.fillMaxWidth(), fallback = e.title)
                                Text(e.title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${e.plays} plays", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        if (d.errors.isNotEmpty()) item {
            Text("Couldn't read history from: ${d.errors.distinct().joinToString()}", style = MaterialTheme.typography.labelSmall, color = Bad)
        }
    }
}

@Composable
private fun BigNum(value: String, label: String, modifier: Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f), maxLines = 2)
    }
}

@Composable
private fun MonthBars(a: List<Int>, b: List<Int>, modifier: Modifier) {
    val max = a.zip(b) { x, y -> x + y }.maxOrNull()?.coerceAtLeast(1) ?: 1
    val track = MaterialTheme.colorScheme.surfaceVariant
    val ca = ServiceType.RADARR.tint; val cb = ServiceType.SONARR.tint
    Canvas(modifier) {
        val slot = size.width / 12
        val w = slot * 0.62f
        for (i in 0 until 12) {
            val x = i * slot + (slot - w) / 2
            drawRoundRect(track, Offset(x, 0f), Size(w, size.height), CornerRadius(w / 4))
            val hb = size.height * b[i] / max
            val ha = size.height * a[i] / max
            if (hb > 0) drawRect(cb, Offset(x, size.height - hb), Size(w, hb))
            if (ha > 0) drawRect(ca, Offset(x, size.height - hb - ha), Size(w, ha))
        }
    }
}
