package com.arrmada360.app.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
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
    MaterialTheme(colorScheme = scheme(amoled, Accent, Accent2), typography = type, content = content)
}

/** Re-tints the theme with a service's brand colours, like each section in nzb360. */
@Composable
fun ServiceTheme(type: ServiceType, amoled: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme(amoled, type.tint, type.tint2), typography = MaterialTheme.typography, content = content)
}
