package com.example.furniturefactory.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val FfColorScheme = lightColorScheme(
    primary = FfColors.Ink,
    onPrimary = FfColors.White,
    primaryContainer = FfColors.Beige,
    onPrimaryContainer = FfColors.Ink,
    secondary = FfColors.Sand,
    onSecondary = FfColors.Ink,
    secondaryContainer = FfColors.Beige,
    onSecondaryContainer = FfColors.Ink,
    tertiary = FfColors.Sand,
    background = FfColors.White,
    onBackground = FfColors.Ink,
    surface = FfColors.White,
    onSurface = FfColors.Ink,
    surfaceVariant = FfColors.Beige,
    onSurfaceVariant = FfColors.Gray,
    surfaceContainerLowest = FfColors.White,
    surfaceContainerLow = FfColors.White,
    surfaceContainer = FfColors.White,
    surfaceContainerHigh = FfColors.White,
    surfaceContainerHighest = FfColors.Beige,
    outline = FfColors.Divider,
    outlineVariant = FfColors.Divider,
    error = FfColors.Error,
    scrim = FfColors.Scrim,
)

val FfShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Тема приложения. Каталог намеренно всегда светлый — как печатный каталог бренда. */
@Composable
fun FurnitureFactoryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FfColorScheme,
        typography = FfTypography,
        shapes = FfShapes,
        content = content,
    )
}
