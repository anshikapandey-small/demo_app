package com.wonderart.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = WonderPurple,
    onPrimary = Color.White,
    primaryContainer = WonderPurpleSoft,
    onPrimaryContainer = WonderPurpleDark,
    secondary = WonderCoral,
    onSecondary = Color.White,
    secondaryContainer = WonderCoralSoft,
    onSecondaryContainer = WonderCoralDark,
    tertiary = WonderMint,
    onTertiary = Color.White,
    tertiaryContainer = WonderMintSoft,
    onTertiaryContainer = WonderMint,
    background = WonderLavenderBg,
    onBackground = WonderTextPrimary,
    surface = WonderSurfaceWhite,
    onSurface = WonderTextPrimary,
    surfaceVariant = WonderSurfaceMuted,
    onSurfaceVariant = WonderTextSecondary,
    outline = WonderBorderSubtle,
    outlineVariant = WonderBorderSubtle,
    error = WonderErrorRed,
    onError = Color.White,
    errorContainer = WonderErrorSoft,
    onErrorContainer = WonderErrorRed
)

private val DarkColorScheme = darkColorScheme(
    primary = WonderPurpleLight,
    onPrimary = Color.White,
    primaryContainer = WonderPurpleDark,
    onPrimaryContainer = WonderPurpleSoft,
    secondary = WonderCoral,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A2118),
    onSecondaryContainer = WonderCoralSoft,
    tertiary = WonderMint,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF143D24),
    onTertiaryContainer = WonderMintSoft,
    background = WonderDarkBg,
    onBackground = Color(0xFFF3F4FA),
    surface = WonderDarkSurface,
    onSurface = Color(0xFFF3F4FA),
    surfaceVariant = WonderDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB5B8CE),
    outline = WonderDarkBorder,
    outlineVariant = WonderDarkBorder,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = WonderErrorSoft
)

object WonderartThemeTokens {
    val spacing: WonderartSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalWonderartSpacing.current
}

@Composable
fun WonderartTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    CompositionLocalProvider(LocalWonderartSpacing provides WonderartSpacing()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WonderartTypography,
            shapes = WonderartShapes,
            content = content
        )
    }
}
