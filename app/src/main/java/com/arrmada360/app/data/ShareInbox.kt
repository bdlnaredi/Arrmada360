package com.arrmada360.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Things shared into the app from other apps, waiting to be opened. */
object ShareInbox {
    /** (content uri, mime type) of a shared clip or screenshot */
    var clip by mutableStateOf<Pair<String, String?>?>(null)
    /** an IMDb / TMDB link */
    var titleLink by mutableStateOf<String?>(null)
}
