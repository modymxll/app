package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val BubblesLightColorScheme = lightColorScheme(
    primary = BubblesCyan,
    onPrimary = BubblesWhite,
    primaryContainer = BubblesCyanSoft,
    onPrimaryContainer = BubblesCyanDark,
    secondary = BubblesGray,
    onSecondary = BubblesWhite,
    secondaryContainer = BubblesSurfaceVariant,
    onSecondaryContainer = BubblesSlateDark,
    tertiary = BubblesCyanDark,
    onTertiary = BubblesWhite,
    background = BubblesBackground,
    onBackground = BubblesSlateDark,
    surface = BubblesWhite,
    onSurface = BubblesSlateDark,
    surfaceVariant = BubblesSurfaceVariant,
    onSurfaceVariant = BubblesGray,
    outline = BubblesDivider,
    outlineVariant = BubblesCyanBorder,
    error = StatusRed,
    onError = BubblesWhite,
    errorContainer = StatusRedBg,
    onErrorContainer = StatusRed
)

val BubblesShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BubblesDeliveryTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BubblesLightColorScheme,
        typography = BubblesTypography,
        shapes = BubblesShapes,
        content = content
    )
}
