package com.arrmada360.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Family mode: a restricted, read-only profile for other people in the house. */
object Family {
    private var activeState by mutableStateOf<Boolean?>(null)

    var active: Boolean
        get() = activeState ?: ConfigStore.flag("familyActive").also { activeState = it }
        set(v) { activeState = v; ConfigStore.setFlag("familyActive", v) }

    val hasPin: Boolean get() = ConfigStore.getString("familyPin").isNotEmpty()

    fun setPin(pin: String) = ConfigStore.putString("familyPin", hash(pin))
    fun checkPin(pin: String): Boolean = hash(pin) == ConfigStore.getString("familyPin")

    /** Apps family members may open (by service id). */
    var allowed: Set<String>
        get() = ConfigStore.getStringSet("familyApps")
        set(v) = ConfigStore.putStringSet("familyApps", v)

    var showServerTab: Boolean
        get() = ConfigStore.flag("familyServerTab")
        set(v) = ConfigStore.setFlag("familyServerTab", v)

    fun canOpen(serviceId: String) = !active || serviceId in allowed

    private fun hash(s: String): String =
        java.security.MessageDigest.getInstance("SHA-256").digest(("arrmada360:" + s).toByteArray()).joinToString("") { "%02x".format(it) }
}
