package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import com.example.data.ArabicFontScale
import com.example.data.ThemeMode

val LocalArabicFontMultiplier = staticCompositionLocalOf { 1.0f }
val LocalIsDarkTheme = staticCompositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = EmeraldOnPrimaryDark,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = EmeraldOnContainerDark,
    secondary = GoldSecondaryDark,
    onSecondary = GoldOnSecondaryDark,
    secondaryContainer = GoldContainerDark,
    onSecondaryContainer = GoldOnContainerDark,
    tertiary = IndigoContainer,
    background = DarkInkBackground,
    onBackground = LightInkOnSurface,
    surface = DarkInkSurface,
    onSurface = LightInkOnSurface,
    surfaceVariant = DarkInkSurfaceVariant,
    onSurfaceVariant = LightInkMuted,
    outline = DarkManuscriptOutline
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = GoldSecondary,
    onSecondary = GoldOnSecondary,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = GoldOnContainer,
    tertiary = IndigoTertiary,
    tertiaryContainer = IndigoContainer,
    background = ParchmentBackground,
    onBackground = InkOnSurface,
    surface = ParchmentSurface,
    onSurface = InkOnSurface,
    surfaceVariant = ParchmentSurfaceVariant,
    onSurfaceVariant = InkMuted,
    outline = ManuscriptOutline
)

private val MueribShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MueribTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    fontScale: ArabicFontScale = ArabicFontScale.NORMAL,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (useDark) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalArabicFontMultiplier provides fontScale.multiplier,
        LocalIsDarkTheme provides useDark
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = MueribShapes,
            content = content
        )
    }
}
