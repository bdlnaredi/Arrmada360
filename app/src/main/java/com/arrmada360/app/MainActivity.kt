package com.arrmada360.app

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import coil.Coil
import coil.ImageLoader
import com.arrmada360.app.data.AppEvents
import com.arrmada360.app.data.ConfigStore
import com.arrmada360.app.data.Pref
import com.arrmada360.app.net.Http
import com.arrmada360.app.notify.Notifier
import com.arrmada360.app.ui.AppRoot
import java.io.File

class MainActivity : ComponentActivity() {
    private var pendingLink by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ConfigStore.init(applicationContext)
        com.arrmada360.app.net.NetSwitch.start(applicationContext)
        installCrashLogger()
        Coil.setImageLoader(
            ImageLoader.Builder(applicationContext)
                .okHttpClient { Http.images }
                .crossfade(true)
                .build()
        )
        Notifier.schedule(applicationContext)
        handle(intent)
        setContent {
            AppRoot(pendingLink) { pendingLink = null }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(i: Intent?) {
        if (i == null) return
        val data = i.data
        if (i.action == Intent.ACTION_VIEW && data?.scheme == "arrmada360") {
            AppEvents.deepLink.tryEmit(data)
            return
        }
        val link = when (i.action) {
            Intent.ACTION_VIEW -> i.dataString
            Intent.ACTION_SEND -> i.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        }?.trim()
        if (!link.isNullOrEmpty() && (link.startsWith("magnet:") || link.startsWith("http"))) pendingLink = link
    }

    /** Volume rocker navigation: volume down = next app, volume up = previous app. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (ConfigStore.onboarded && ConfigStore.flag(Pref.VOLUME_NAV) && ConfigStore.services.isNotEmpty()) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN -> { AppEvents.cycleService.tryEmit(1); return true }
                KeyEvent.KEYCODE_VOLUME_UP -> { AppEvents.cycleService.tryEmit(-1); return true }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    /** Keeps the last crash on this phone only (Settings → General → Crash logs). Nothing is uploaded. */
    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                if (ConfigStore.flag(Pref.CRASH_LOGS)) {
                    File(filesDir, "crash.txt").writeText("Arrmada360 crash at ${java.util.Date()}\n\n" + e.stackTraceToString())
                }
            } catch (ignored: Exception) {
            }
            previous?.uncaughtException(t, e)
        }
    }
}
