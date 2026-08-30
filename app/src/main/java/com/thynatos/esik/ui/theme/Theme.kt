package com.thynatos.esik.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = EsikGreen,
    onPrimary = EsikWarmSurface,
    primaryContainer = EsikGreenContainer,
    onPrimaryContainer = EsikInk,
    secondary = EsikInkMuted,
    onSecondary = EsikWarmSurface,
    secondaryContainer = EsikWarmSurfaceVariant,
    onSecondaryContainer = EsikInk,
    background = EsikWarmBackground,
    onBackground = EsikInk,
    surface = EsikWarmSurface,
    onSurface = EsikInk,
    surfaceVariant = EsikWarmSurfaceVariant,
    onSurfaceVariant = EsikInkMuted,
    outline = EsikWarmOutline,
    error = EsikError,
    onError = EsikWarmSurface,
    errorContainer = EsikErrorContainer,
    onErrorContainer = EsikInk,
)

private val DarkColorScheme = darkColorScheme(
    primary = EsikGreenDark,
    onPrimary = EsikDarkBackground,
    primaryContainer = EsikGreenContainerDark,
    onPrimaryContainer = EsikDarkInk,
    secondary = EsikDarkInkMuted,
    onSecondary = EsikDarkBackground,
    secondaryContainer = EsikDarkSurfaceVariant,
    onSecondaryContainer = EsikDarkInk,
    background = EsikDarkBackground,
    onBackground = EsikDarkInk,
    surface = EsikDarkSurface,
    onSurface = EsikDarkInk,
    surfaceVariant = EsikDarkSurfaceVariant,
    onSurfaceVariant = EsikDarkInkMuted,
    outline = EsikDarkOutline,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun EsikTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = EsikTypography,
        shapes = EsikShapes,
        content = content,
    )
}
