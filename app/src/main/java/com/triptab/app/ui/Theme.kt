package com.triptab.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Brown = Color(0xFF5B3A1E)
private val Olive = Color(0xFF6E8F4E)
private val Sand = Color(0xFFF4EBDD)
private val Sun = Color(0xFFE0A93B)

private val Light = lightColorScheme(
    primary = Brown, onPrimary = Color.White,
    primaryContainer = Color(0xFFEAD9C3), onPrimaryContainer = Color(0xFF2E1B0A),
    secondary = Olive, onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE8CC), onSecondaryContainer = Color(0xFF1E2E10),
    tertiary = Sun,
    background = Sand, onBackground = Color(0xFF2B2117),
    surface = Color(0xFFFBF6EE), onSurface = Color(0xFF2B2117),
    surfaceVariant = Color(0xFFEDE2D2), onSurfaceVariant = Color(0xFF5E5145),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFE6C29E), onPrimary = Color(0xFF3A230F),
    primaryContainer = Color(0xFF5B3A1E), onPrimaryContainer = Color(0xFFFBE7D1),
    secondary = Color(0xFFB5D196), onSecondary = Color(0xFF213511),
    secondaryContainer = Color(0xFF3F5A2A), onSecondaryContainer = Color(0xFFDCE8CC),
    tertiary = Sun,
    background = Color(0xFF1C1712), onBackground = Color(0xFFEDE2D2),
    surface = Color(0xFF241E18), onSurface = Color(0xFFEDE2D2),
    surfaceVariant = Color(0xFF3A3129), onSurfaceVariant = Color(0xFFD3C4B3),
)

@Composable
fun TripTabTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
