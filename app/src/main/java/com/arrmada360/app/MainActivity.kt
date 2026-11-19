package com.arrmada360.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import coil.Coil
import coil.ImageLoader
import com.arrmada360.app.data.ConfigStore
import com.arrmada360.app.net.Http
import com.arrmada360.app.ui.AppRoot

class MainActivity : ComponentActivity() {
    private var pendingLink by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ConfigStore.init(applicationContext)
        Coil.setImageLoader(
            ImageLoader.Builder(applicationContext)
                .okHttpClient { Http.images }
                .crossfade(true)
                .build()
        )
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
        val link = when (i.action) {
            Intent.ACTION_VIEW -> i.dataString
            Intent.ACTION_SEND -> i.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        }?.trim()
        if (!link.isNullOrEmpty() && (link.startsWith("magnet:") || link.startsWith("http"))) pendingLink = link
    }
}
