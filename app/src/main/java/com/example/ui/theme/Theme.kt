package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = ForestPrimary,
    onPrimary = Color.White,
    primaryContainer = ForestContainer,
    onPrimaryContainer = OnForestContainer,
    secondary = LeafGreen,
    onSecondary = Color.White,
    secondaryContainer = LeafContainer,
    onSecondaryContainer = OnLeafContainer,
    tertiary = EarthLoam,
    onTertiary = Color.White,
    tertiaryContainer = EarthSand,
    onTertiaryContainer = EarthWarm,
    background = DewBackground,
    onBackground = OnForestContainer,
    surface = DewSurface,
    onSurface = OnForestContainer,
    surfaceVariant = StoneSurfaceVariant,
    onSurfaceVariant = Color(0xFF414941),
    outline = SlateOutline,
    outlineVariant = SlateOutlineVariant,
    error = AlertCrimson,
    onError = Color.White,
    errorContainer = AlertCrimsonContainer,
    onErrorContainer = Color(0xFF410002)
)

private val DarkColorScheme = darkColorScheme(
    primary = MintPrimary,
    onPrimary = OnMintPrimary,
    primaryContainer = ForestPrimary,
    onPrimaryContainer = ForestContainer,
    secondary = MintSecondary,
    onSecondary = OnForestContainer,
    secondaryContainer = Color(0xFF1D4D32),
    onSecondaryContainer = LeafContainer,
    tertiary = Color(0xFFD7CCC8),
    onTertiary = EarthWarm,
    tertiaryContainer = Color(0xFF4E342E),
    onTertiaryContainer = EarthSand,
    background = CanopyBackground,
    onBackground = Color(0xFFE3ECE4),
    surface = CanopySurface,
    onSurface = Color(0xFFE3ECE4),
    surfaceVariant = CanopySurfaceVariant,
    onSurfaceVariant = Color(0xFFC0C9C0),
    outline = Color(0xFF8A948A),
    outlineVariant = Color(0xFF3E4A40),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

val AgroGuardShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AgroGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AgroGuardTypography,
        shapes = AgroGuardShapes,
        content = content
    )
}
