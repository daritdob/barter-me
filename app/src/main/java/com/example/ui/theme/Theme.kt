package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BarterDarkPrimary,
    onPrimary = BarterDarkOnPrimary,
    primaryContainer = BarterDarkSurfaceVariant,
    onPrimaryContainer = BarterDarkOnSurface,
    secondary = BarterDarkSecondary,
    onSecondary = BarterDarkOnSecondary,
    tertiary = BarterDarkTertiary,
    onTertiary = BarterDarkOnTertiary,
    tertiaryContainer = BarterDarkSurfaceVariant,
    onTertiaryContainer = BarterDarkOnSurface,
    background = BarterDarkBg,
    onBackground = BarterDarkOnSurface,
    surface = BarterDarkSurface,
    onSurface = BarterDarkOnSurface,
    surfaceVariant = BarterDarkSurfaceVariant,
    onSurfaceVariant = BarterDarkOnSurfaceVariant,
    outline = BarterDarkOutline,
    outlineVariant = BarterDarkOutlineVariant,
)

private val LightColorScheme = lightColorScheme(
    primary = BarterLightPrimary,
    onPrimary = BarterLightOnPrimary,
    primaryContainer = BarterLightSurfaceVariant,
    onPrimaryContainer = BarterLightOnSurface,
    secondary = BarterLightSecondary,
    onSecondary = BarterLightOnSecondary,
    tertiary = BarterLightTertiary,
    onTertiary = BarterLightOnTertiary,
    tertiaryContainer = BarterLightSurfaceVariant,
    onTertiaryContainer = BarterLightOnSurface,
    background = BarterLightBg,
    onBackground = BarterLightOnSurface,
    surface = BarterLightSurface,
    onSurface = BarterLightOnSurface,
    surfaceVariant = BarterLightSurfaceVariant,
    onSurfaceVariant = BarterLightOnSurfaceVariant,
    outline = BarterLightOutline,
    outlineVariant = BarterLightOutlineVariant,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
