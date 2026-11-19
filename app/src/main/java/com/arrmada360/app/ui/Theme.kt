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
val Good = Color(0xFF4ADE80)
val Warn = Color(0xFFFBBF24)
val Bad = Color(0xFFF87171)

private fun scheme(amoled: Boolean, primary: Color): ColorScheme {
    val bg = if (amoled) Color(0xFF000000) else Color(0xFF14171D)
    val surface = if (amoled) Color(0xFF0B0C0F) else Color(0xFF1B1F27)
    val high = if (amoled) Color(0xFF16181D) else Color(0xFF242A34)
    return darkColorScheme(
        primary = primary,
        onPrimary = Color(0xFF0B0D10),
        primaryContainer = primary.copy(alpha = 0.22f),
        onPrimaryContainer = Color(0xFFEFF3F7),
        secondary = primary,
        secondaryContainer = primary.copy(alpha = 0.18f),
        onSecondaryContainer = Color(0xFFEFF3F7),
        background = bg,
        onBackground = Color(0xFFE7EAEE),
        surface = surface,
        onSurface = Color(0xFFE7EAEE),
        surfaceVariant = high,
        onSurfaceVariant = Color(0xFF9AA4B2),
        surfaceContainerLowest = bg,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = high,
        surfaceContainerHighest = high,
        outline = Color(0xFF3A4250),
        outlineVariant = Color(0xFF2A303B),
        error = Bad,
    )
}

private val type = Typography(
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun AppTheme(amoled: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme(amoled, Accent), typography = type, content = content)
}

/** Re-tints the theme with a service's brand colour, like each section in nzb360. */
@Composable
fun ServiceTheme(type: ServiceType, amoled: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme(amoled, type.tint), typography = MaterialTheme.typography, content = content)
}
