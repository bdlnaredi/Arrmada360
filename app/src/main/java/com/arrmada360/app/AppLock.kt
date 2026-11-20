package com.arrmada360.app

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.CancellationSignal
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arrmada360.app.data.ConfigStore

/** Fingerprint / face / screen-lock gate for the whole app. */
object AppLock {
    const val PREF = "appLock"
    const val TIMEOUT = "appLockTimeoutSec"
    var locked by mutableStateOf(false)
    private var lastBackground = 0L
    private var authInProgress = false

    val enabled: Boolean get() = ConfigStore.flag(PREF)
    val timeoutSec: Int get() = ConfigStore.getInt(TIMEOUT, 0)

    fun onStart() {
        if (!enabled) { locked = false; return }
        if (authInProgress) return
        val away = (System.currentTimeMillis() - lastBackground) / 1000
        if (lastBackground == 0L || away >= timeoutSec) locked = true
    }

    fun onStop() { if (!authInProgress) lastBackground = System.currentTimeMillis() }

    fun deviceSecure(ctx: Context): Boolean = (ctx.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure

    class Prompter(private val activity: ComponentActivity) {
        private val credential = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            authInProgress = false
            if (r.resultCode == Activity.RESULT_OK) locked = false
        }

        fun unlock() {
            if (!deviceSecure(activity)) {
                locked = false
                Toast.makeText(activity, "Set a screen lock on your phone to use app lock", Toast.LENGTH_LONG).show()
                return
            }
            authInProgress = true
            if (Build.VERSION.SDK_INT >= 28) biometric() else keyguard()
        }

        private fun keyguard() {
            val km = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            @Suppress("DEPRECATION")
            val i = km.createConfirmDeviceCredentialIntent("Unlock Arrmada360", "Confirm it's you")
            if (i == null) { authInProgress = false; locked = false; return }
            credential.launch(i)
        }

        @Suppress("DEPRECATION")
        private fun biometric() {
            val b = android.hardware.biometrics.BiometricPrompt.Builder(activity)
                .setTitle("Unlock Arrmada360")
                .setSubtitle("Use your fingerprint, face or screen lock")
            when {
                Build.VERSION.SDK_INT >= 30 -> b.setAllowedAuthenticators(
                    android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                )
                Build.VERSION.SDK_INT == 29 -> b.setDeviceCredentialAllowed(true)
                else -> b.setNegativeButton("Use PIN", activity.mainExecutor) { _, _ -> keyguard() }
            }
            b.build().authenticate(CancellationSignal(), activity.mainExecutor, object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                    authInProgress = false; locked = false
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    val noBio = errorCode == android.hardware.biometrics.BiometricPrompt.BIOMETRIC_ERROR_HW_NOT_PRESENT ||
                        errorCode == android.hardware.biometrics.BiometricPrompt.BIOMETRIC_ERROR_NO_BIOMETRICS ||
                        errorCode == android.hardware.biometrics.BiometricPrompt.BIOMETRIC_ERROR_HW_UNAVAILABLE
                    if (noBio && Build.VERSION.SDK_INT < 30) keyguard() else authInProgress = false
                }
            })
        }
    }
}
