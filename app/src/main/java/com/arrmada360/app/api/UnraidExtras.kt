package com.arrmada360.app.api

import com.arrmada360.app.data.ConfigStore
import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.net.arr
import com.arrmada360.app.net.dbl
import com.arrmada360.app.net.int
import com.arrmada360.app.net.long
import com.arrmada360.app.net.obj
import com.arrmada360.app.net.objects
import com.arrmada360.app.net.str
import org.json.JSONArray
import org.json.JSONObject

data class LiveMetrics(val cpu: Double?, val mem: Double?, val cores: List<Double>, val memUsed: Long, val memTotal: Long)
data class LogFileInfo(val name: String, val path: String, val size: Long, val modified: String)
data class LogContent(val text: String, val totalLines: Int)

private fun gqlStr(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

suspend fun UnraidApi.liveMetrics(): LiveMetrics {
    val m = try { gql("query { metrics { cpu { percentTotal cpus { percentTotal } } memory { total used percentTotal } } }").obj("metrics") }
    catch (e: Exception) { gql("query { metrics { cpu { percentTotal } memory { total used } } }").obj("metrics") }
    val cpu = m?.obj("cpu"); val mem = m?.obj("memory")
    val total = mem?.long("total") ?: 0; val used = mem?.long("used") ?: 0
    val memPct = mem?.optDouble("percentTotal")?.takeIf { !it.isNaN() } ?: if (total > 0) used * 100.0 / total else null
    return LiveMetrics(cpu?.optDouble("percentTotal")?.takeIf { !it.isNaN() }, memPct,
        cpu?.arr("cpus")?.objects()?.map { it.dbl("percentTotal") } ?: emptyList(), used, total)
}

suspend fun UnraidApi.logFiles(): List<LogFileInfo> =
    gql("query { logFiles { name path size modifiedAt } }").arr("logFiles").objects()
        .map { LogFileInfo(it.str("name"), it.str("path"), it.long("size"), it.str("modifiedAt")) }
        .sortedByDescending { it.modified }

suspend fun UnraidApi.readLog(path: String, lines: Int): LogContent {
    // Ask for the total first so we can read the last N lines.
    val first = gql("query { logFile(path: ${gqlStr(path)}, lines: 1) { totalLines } }").obj("logFile")
    val total = first?.int("totalLines") ?: 0
    val start = (total - lines + 1).coerceAtLeast(1)
    val o = gql("query { logFile(path: ${gqlStr(path)}, lines: $lines, startLine: $start) { content totalLines } }").obj("logFile")
    return LogContent(o?.str("content").orEmpty(), o?.int("totalLines") ?: total)
}

/** Docker writes each container's output as JSON lines; Unraid's API can read it like any other log file. */
suspend fun UnraidApi.containerLog(containerId: String, lines: Int): LogContent {
    val cid = containerId.substringAfterLast(':')
    val raw = readLog("/var/lib/docker/containers/$cid/$cid-json.log", lines)
    val text = raw.text.lineSequence().filter { it.isNotBlank() }.joinToString("\n") { l ->
        try {
            val o = JSONObject(l)
            val t = o.str("time").take(19).replace('T', ' ')
            "$t  ${o.str("log").trimEnd()}"
        } catch (e: Exception) { l }
    }
    return LogContent(text, raw.totalLines)
}

/** CPU / RAM samples kept on the phone for the 24-hour chart. */
object UnraidHistory {
    data class Sample(val epochMin: Long, val cpu: Float, val mem: Float)

    fun load(cfg: ServiceConfig): List<Sample> = try {
        val a = JSONArray(ConfigStore.getString("unraidHist_" + cfg.id).ifEmpty { "[]" })
        (0 until a.length()).mapNotNull { i -> a.optJSONArray(i)?.let { Sample(it.optLong(0), it.optDouble(1).toFloat(), it.optDouble(2).toFloat()) } }
    } catch (e: Exception) { emptyList() }

    fun add(cfg: ServiceConfig, m: LiveMetrics) {
        val now = System.currentTimeMillis() / 60000
        val list = load(cfg).filter { now - it.epochMin <= 24 * 60 }.toMutableList()
        if (list.lastOrNull()?.epochMin == now) return
        // keep at most one sample every 5 minutes
        if (list.isNotEmpty() && now - list.last().epochMin < 5) return
        list += Sample(now, (m.cpu ?: 0.0).toFloat(), (m.mem ?: 0.0).toFloat())
        val a = JSONArray()
        list.forEach { a.put(JSONArray().put(it.epochMin).put(it.cpu.toDouble()).put(it.mem.toDouble())) }
        ConfigStore.putString("unraidHist_" + cfg.id, a.toString())
    }
}
