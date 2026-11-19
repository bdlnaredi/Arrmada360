package com.arrmada360.app.net

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.arrmada360.app.data.ConfigStore
import com.arrmada360.app.data.ServiceConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Local ⇄ remote connection switching, like nzb360.
 *  - Always local / always remote, or
 *  - Auto: on one of your home Wi-Fi networks → local. Otherwise, if no home networks are set (or the
 *    network name can't be read), the app quickly checks whether each local address answers and uses it if so.
 */
object NetSwitch {
    private lateinit var app: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var probeJob: Job? = null

    var onWifi by mutableStateOf(false)
        private set
    var ssid by mutableStateOf<String?>(null)
        private set
    /** Bumped every time the network or the decision changes. */
    var version by mutableIntStateOf(0)
        private set
    /** serviceId → local address reachable on the current network. */
    val reachable = mutableStateMapOf<String, Boolean>()

    fun start(ctx: Context) {
        if (::app.isInitialized) return
        app = ctx.applicationContext
        readNetwork()
        val cm = app.getSystemService(ConnectivityManager::class.java)
        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = changed()
            override fun onLost(network: Network) = changed()
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                val wifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                if (wifi != onWifi) changed()
            }
        })
        changed()
    }

    private fun changed() {
        scope.launch {
            delay(400) // let the network settle
            readNetwork()
            refresh()
        }
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun readNetwork() {
        if (!::app.isInitialized) return
        val cm = app.getSystemService(ConnectivityManager::class.java)
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        onWifi = caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        ssid = if (onWifi && hasLocationPermission()) {
            try {
                val raw = (app.getSystemService(Context.WIFI_SERVICE) as WifiManager).connectionInfo?.ssid
                raw?.trim('"')?.takeIf { it.isNotEmpty() && it != "<unknown ssid>" }
            } catch (e: Exception) { null }
        } else null
    }

    fun hasLocationPermission(): Boolean = ::app.isInitialized &&
        ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Re-read the network and re-test local addresses (after settings change, or a pull to refresh). */
    fun refresh() {
        if (!::app.isInitialized) return
        probeJob?.cancel()
        probeJob = scope.launch {
            readNetwork()
            reachable.clear()
            version++
            if (ConfigStore.connMode == "AUTO" && onWifi && !homeKnown()) {
                ConfigStore.services.toList().filter { it.hasRemote }.forEach { c ->
                    launch { reachable[c.id] = probe(c.localBaseUrl); version++ }
                }
            }
        }
    }

    /** True when the Wi-Fi name is known and matches (or doesn't match) a saved home network. */
    private fun homeKnown(): Boolean = ConfigStore.homeSsids.isNotEmpty() && ssid != null

    fun atHome(): Boolean? = when {
        !onWifi -> false
        homeKnown() -> ssid in ConfigStore.homeSsids
        else -> null
    }

    fun useLocal(c: ServiceConfig): Boolean = when (ConfigStore.connMode) {
        "LOCAL" -> true
        "REMOTE" -> false
        else -> when (atHome()) {
            true -> true
            false -> false
            null -> reachable[c.id] ?: true // still testing → try local first on Wi-Fi
        }
    }

    /** Fast TCP check (1.5 s) that the local host:port answers. */
    private suspend fun probe(base: String): Boolean = withContext(Dispatchers.IO) {
        val u = base.toHttpUrlOrNull() ?: return@withContext false
        try {
            Socket().use { it.connect(InetSocketAddress(u.host, u.port), 1500) }
            true
        } catch (e: Exception) { false }
    }

    fun describe(): String = when (ConfigStore.connMode) {
        "LOCAL" -> "Always using local addresses"
        "REMOTE" -> "Always using remote addresses"
        else -> when {
            !onWifi -> "Mobile data – using remote addresses"
            atHome() == true -> "Home Wi-Fi${ssid?.let { " ($it)" } ?: ""} – using local addresses"
            atHome() == false -> "Away${ssid?.let { " ($it)" } ?: ""} – using remote addresses"
            else -> "Wi-Fi${ssid?.let { " ($it)" } ?: ""} – testing local addresses"
        }
    }
}
