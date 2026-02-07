package com.metrolist.music.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography

// Metrolist brand color
val MetrolistPrimary = Color(0xFFED5564)
val MetrolistOnPrimary = Color.White
val MetrolistBackground = Color.Black
val MetrolistOnBackground = Color.White
val MetrolistSurface = Color(0xFF1A1A1A)
val MetrolistOnSurface = Color.White

private val WearColorScheme = ColorScheme(
    primary = MetrolistPrimary,
    onPrimary = MetrolistOnPrimary,
    primaryContainer = MetrolistPrimary.copy(alpha = 0.3f),
    onPrimaryContainer = MetrolistOnPrimary,
    secondary = MetrolistPrimary.copy(alpha = 0.7f),
    onSecondary = MetrolistOnPrimary,
    secondaryContainer = MetrolistSurface,
    onSecondaryContainer = MetrolistOnSurface,
    tertiary = Color(0xFF4A90D9),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF4A90D9).copy(alpha = 0.3f),
    onTertiaryContainer = Color.White,
    surfaceContainer = MetrolistSurface,
    surfaceContainerLow = MetrolistBackground,
    surfaceContainerHigh = MetrolistSurface,
    onSurface = MetrolistOnSurface,
    onSurfaceVariant = Color(0xFFB0B0B0),
    background = MetrolistBackground,
    onBackground = MetrolistOnBackground,
    error = Color(0xFFCF6679),
    onError = Color.Black,
)

@Composable
fun WearTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WearColorScheme,
        typography = Typography(),
        content = content
    )
}
