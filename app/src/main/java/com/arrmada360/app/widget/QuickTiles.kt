package com.arrmada360.app.widget

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.arrmada360.app.R
import com.arrmada360.app.data.ConfigStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object QuickTiles {
    fun requestUpdate(ctx: Context) {
        try { TileService.requestListeningState(ctx, ComponentName(ctx, DownloadsTileService::class.java)) } catch (e: Exception) { }
    }
}

/** Quick Settings tile: pause or resume every download client with one tap. */
class DownloadsTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var busy = false

    override fun onStartListening() {
        super.onStartListening()
        ConfigStore.init(applicationContext)
        render()
    }

    private fun render(label: String? = null) {
        val t = qsTile ?: return
        val paused = Widgets.allPaused
        val hasClients = ConfigStore.services.any { it.type.isDownloader }
        t.icon = Icon.createWithResource(this, if (paused) R.drawable.ic_w_play else R.drawable.ic_w_pause)
        t.label = "Downloads"
        if (android.os.Build.VERSION.SDK_INT >= 29) t.subtitle = label ?: when {
            !hasClients -> "No clients"
            paused -> "Paused – tap to resume"
            else -> "Running – tap to pause"
        }
        t.state = when { !hasClients -> Tile.STATE_UNAVAILABLE; paused -> Tile.STATE_INACTIVE; else -> Tile.STATE_ACTIVE }
        t.updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (busy) return
        busy = true
        val target = !Widgets.allPaused
        render(if (target) "Pausing…" else "Resuming…")
        scope.launch {
            com.arrmada360.app.net.NetSwitch.start(applicationContext)
            val n = withContext(Dispatchers.IO) { Widgets.setAllPaused(target) }
            busy = false
            render(if (n == 0) "Couldn't reach clients" else null)
            Widgets.refresh(applicationContext)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }
}
