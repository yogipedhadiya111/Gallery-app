package com.aistudio.xplayer.vdpkzm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PolishLightPrimary,
    onPrimary = PolishLightOnPrimary,
    secondary = PolishLightSecondary,
    onSecondary = PolishLightOnSecondary,
    tertiary = PolishLightTertiary,
    onTertiary = PolishLightOnTertiary,
    background = PolishLightBg,
    onBackground = PolishLightOnSurface,
    surface = PolishLightSurface,
    onSurface = PolishLightOnSurface,
    surfaceVariant = PolishLightVariant,
    onSurfaceVariant = PolishLightOnSurface,
    outline = PolishLightBorder,
    outlineVariant = PolishLightGrey
)

private val DarkColorScheme = darkColorScheme(
    primary = PolishDarkPrimary,
    onPrimary = PolishDarkOnPrimary,
    secondary = PolishDarkSecondary,
    onSecondary = PolishDarkOnSecondary,
    tertiary = PolishDarkTertiary,
    onTertiary = PolishDarkOnTertiary,
    background = PolishDarkBg,
    onBackground = PolishDarkOnSurface,
    surface = PolishDarkSurface,
    onSurface = PolishDarkOnSurface,
    surfaceVariant = PolishDarkVariant,
    onSurfaceVariant = PolishDarkOnSurface,
    outline = PolishDarkBorder,
    outlineVariant = PolishDarkGrey
)

private val AmoledColorScheme = darkColorScheme(
    primary = PolishAmoledPrimary,
    onPrimary = PolishAmoledOnPrimary,
    secondary = PolishAmoledSecondary,
    onSecondary = PolishAmoledOnSecondary,
    tertiary = PolishAmoledTertiary,
    onTertiary = PolishAmoledOnTertiary,
    background = PolishAmoledBg,
    onBackground = PolishAmoledOnSurface,
    surface = PolishAmoledSurface,
    onSurface = PolishAmoledOnSurface,
    surfaceVariant = PolishAmoledVariant,
    onSurfaceVariant = PolishAmoledOnSurface,
    outline = PolishAmoledBorder,
    outlineVariant = PolishAmoledGrey
)

@Composable
fun XPlayerTheme(
    modeValue: Int = 0, // 0=Light, 1=Dark, 2=AMOLED
    content: @Composable () -> Unit
) {
    val selectedScheme = when (modeValue) {
        0 -> LightColorScheme
        1 -> DarkColorScheme
        else -> AmoledColorScheme
    }

    MaterialTheme(
        colorScheme = selectedScheme,
        typography = Typography,
        content = content
    )
}
