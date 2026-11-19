package com.arrmada360.app.api

import androidx.compose.runtime.mutableStateMapOf
import com.arrmada360.app.data.ConfigStore
import com.arrmada360.app.data.ServiceConfig
import com.arrmada360.app.net.friendlyError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

sealed interface Conn {
    data object Unknown : Conn
    data object Checking : Conn
    data class Ok(val info: String, val ms: Long) : Conn
    data class Fail(val msg: String) : Conn
}

/** Live connection status for every configured service, shared by the whole UI. */
object Connections {
    val state = mutableStateMapOf<String, Conn>()

    fun of(id: String): Conn = state[id] ?: Conn.Unknown

    suspend fun check(cfg: ServiceConfig) {
        state[cfg.id] = Conn.Checking
        val t = System.currentTimeMillis()
        state[cfg.id] = try {
            Conn.Ok(ServiceTester.test(cfg), System.currentTimeMillis() - t)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Conn.Fail(friendlyError(e))
        }
    }

    suspend fun checkAll() = coroutineScope {
        ConfigStore.services.toList().forEach { c -> launch { check(c) } }
    }
}

/** Remembers recent download / upload speeds per client so we can draw live charts. */
object SpeedHistory {
    val down = mutableStateMapOf<String, List<Long>>()
    val up = mutableStateMapOf<String, List<Long>>()
    val active = mutableStateMapOf<String, Int>()

    fun record(id: String, dl: Long, ul: Long) {
        down[id] = ((down[id] ?: emptyList()) + dl).takeLast(60)
        up[id] = ((up[id] ?: emptyList()) + ul).takeLast(60)
    }

    fun totalDown(): List<Long> = sum(down.values.toList())
    fun totalUp(): List<Long> = sum(up.values.toList())

    private fun sum(lists: List<List<Long>>): List<Long> {
        val n = lists.maxOfOrNull { it.size } ?: 0
        return (0 until n).map { i -> lists.sumOf { l -> l.getOrNull(l.size - n + i) ?: 0L } }
    }
}
