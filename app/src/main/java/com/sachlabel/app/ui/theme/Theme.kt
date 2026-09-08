package com.sachlabel.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Stitch Truth & Clarity Color Scheme
private val StitchColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = AlertCrimson,
    onSecondary = Color.White,
    secondaryContainer = AlertCrimsonContainer,
    tertiary = CautionAmber,
    onTertiary = Color.White,
    background = BackgroundSurface,
    onBackground = TextPrimary,
    surface = BackgroundSurface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceContainerLow,
    onSurfaceVariant = TextSecondary,
    outline = OutlineColor,
    outlineVariant = OutlineVariant,
    error = AlertCrimson,
    onError = Color.White,
    errorContainer = AlertCrimsonLow
)

@Composable
fun SachLabelTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StitchColorScheme,
        typography = Typography,
        content = content
    )
}
