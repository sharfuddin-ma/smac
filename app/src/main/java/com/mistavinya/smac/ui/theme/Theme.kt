package com.mistavinya.smac.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = Color(0xFF003258),
    secondary = Color(0xFF4CAF50),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8E6C9),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF5F5F5F),
    error = Color(0xFFD32F2F),
    onError = Color.White,
    outline = Color(0xFFDDDDDD),
    outlineVariant = Color(0xFFEEEEEE)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF1565C0),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFF81C784),
    onSecondary = Color(0xFF003822),
    secondaryContainer = Color(0xFF2E7D32),
    background = Color(0xFF0F0F0F),
    onBackground = Color(0xFFE8E8E8),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFE8E8E8),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFB0B0B0),
    error = Color(0xFFEF5350),
    onError = Color.White,
    outline = Color(0xFF3A3A3A),
    outlineVariant = Color(0xFF2C2C2C)
)

@Composable
fun CallSyncTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
