package com.arrmada360.app.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import com.arrmada360.app.data.ConfigStore
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.arrmada360.app.data.ServiceType

val Accent = Color(0xFF35C5F4)
val Accent2 = Color(0xFFA855F7)
val Good = Color(0xFF4ADE80)
val Warn = Color(0xFFFBBF24)
val Bad = Color(0xFFF87171)
val Info = Color(0xFF60A5FA)
val Pink = Color(0xFFF472B6)

/** A bright palette used for charts, chips and the dashboard hero. */
val Rainbow = listOf(Color(0xFF35C5F4), Color(0xFFA855F7), Color(0xFFF472B6), Color(0xFFFBBF24), Color(0xFF4ADE80), Color(0xFFF97316))

private fun scheme(amoled: Boolean, primary: Color, secondary: Color): ColorScheme {
    val bg = if (amoled) Color(0xFF000000) else Color(0xFF0E1117)
    val surface = if (amoled) Color(0xFF0C0D12) else Color(0xFF171B24)
    val high = if (amoled) Color(0xFF181A22) else Color(0xFF222837)
    return darkColorScheme(
        primary = primary,
        onPrimary = Color(0xFF0B0D10),
        primaryContainer = primary.copy(alpha = 0.24f),
        onPrimaryContainer = Color(0xFFF2F5F9),
        secondary = secondary,
        onSecondary = Color.White,
        secondaryContainer = secondary.copy(alpha = 0.22f),
        onSecondaryContainer = Color(0xFFF2F5F9),
        tertiary = Pink,
        background = bg,
        onBackground = Color(0xFFEDEFF3),
        surface = surface,
        onSurface = Color(0xFFEDEFF3),
        surfaceVariant = high,
        onSurfaceVariant = Color(0xFFA1ABBA),
        surfaceContainerLowest = bg,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = high,
        surfaceContainerHighest = high,
        outline = Color(0xFF3A4252),
        outlineVariant = Color(0xFF262C38),
        error = Bad,
    )
}

private fun lightScheme(primary: Color, secondary: Color): ColorScheme {
    val bg = Color(0xFFF3F5F9); val surface = Color(0xFFFFFFFF); val high = Color(0xFFE4E8EF)
    return lightColorScheme(
        primary = primary, onPrimary = Color(0xFF0B0D10), primaryContainer = primary.copy(alpha = 0.22f), onPrimaryContainer = Color(0xFF10141B),
        secondary = secondary, onSecondary = Color.White, secondaryContainer = secondary.copy(alpha = 0.2f), onSecondaryContainer = Color(0xFF10141B),
        tertiary = Pink, background = bg, onBackground = Color(0xFF141821), surface = surface, onSurface = Color(0xFF141821),
        surfaceVariant = high, onSurfaceVariant = Color(0xFF586273),
        surfaceContainerLowest = surface, surfaceContainerLow = surface, surfaceContainer = surface, surfaceContainerHigh = high, surfaceContainerHighest = high,
        outline = Color(0xFFB9C1CE), outlineVariant = Color(0xFFDDE2EA), error = Color(0xFFDC2626),
    )
}

/** True when the app should draw in dark colours right now. */
@Composable
fun isDarkUi(): Boolean = when (ConfigStore.themeMode) { "light" -> false; "system" -> isSystemInDarkTheme(); else -> true }

@Composable
private fun resolve(amoled: Boolean, primary: Color, secondary: Color): ColorScheme {
    val dark = isDarkUi()
    if (ConfigStore.materialYou && android.os.Build.VERSION.SDK_INT >= 31) {
        val ctx = LocalContext.current
        val d = if (dark) androidx.compose.material3.dynamicDarkColorScheme(ctx) else androidx.compose.material3.dynamicLightColorScheme(ctx)
        return if (dark && amoled) d.copy(background = Color.Black, surface = Color(0xFF0C0D12)) else d
    }
    return if (dark) scheme(amoled, primary, secondary) else lightScheme(primary, secondary)
}

private val type = Typography(
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun AppTheme(amoled: Boolean, content: @Composable () -> Unit) {
    val dark = isDarkUi()
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) androidx.compose.runtime.SideEffect {
        val w = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        androidx.core.view.WindowCompat.getInsetsController(w, view).apply {
            isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(colorScheme = resolve(amoled, Accent, Accent2), typography = type, content = content)
}

/** Re-tints the theme with a service's brand colours, like each section in nzb360. */
@Composable
fun ServiceTheme(type: ServiceType, amoled: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = resolve(amoled, type.tint, type.tint2), typography = MaterialTheme.typography, content = content)
}
