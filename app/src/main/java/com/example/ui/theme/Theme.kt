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

private val DarkColorScheme =
  darkColorScheme(
    primary = SleekDarkPrimary,
    onPrimary = SleekDarkOnPrimary,
    primaryContainer = SleekDarkSurfaceVariant,
    onPrimaryContainer = SleekDarkOnSurface,
    secondary = SleekDarkSurfaceVariant,
    onSecondary = SleekDarkOnSurface,
    tertiary = SleekDarkTertiary,
    onTertiary = SleekDarkOnTertiary,
    tertiaryContainer = SleekDarkSurfaceVariant,
    onTertiaryContainer = SleekDarkOnSurface,
    background = SleekDarkBg,
    onBackground = SleekDarkOnSurface,
    surface = SleekDarkSurface,
    onSurface = SleekDarkOnSurface,
    surfaceVariant = SleekDarkSurfaceVariant,
    onSurfaceVariant = SleekDarkOnSurfaceVariant,
    outline = SleekDarkOnSurfaceVariant,
    outlineVariant = SleekDarkSurfaceVariant
  )

private val LightColorScheme =
  lightColorScheme(
    primary = SleekLightPrimary,
    onPrimary = SleekLightOnPrimary,
    primaryContainer = SleekLightSurfaceVariant,
    onPrimaryContainer = SleekLightOnSurface,
    secondary = SleekLightSurfaceVariant,
    onSecondary = SleekLightOnSurface,
    tertiary = SleekLightTertiary,
    onTertiary = SleekLightOnTertiary,
    tertiaryContainer = SleekLightSurfaceVariant,
    onTertiaryContainer = SleekLightOnSurface,
    background = SleekLightBg,
    onBackground = SleekLightOnSurface,
    surface = SleekLightSurface,
    onSurface = SleekLightOnSurface,
    surfaceVariant = SleekLightSurfaceVariant,
    onSurfaceVariant = SleekLightOnSurfaceVariant,
    outline = SleekLightOnSurfaceVariant,
    outlineVariant = SleekLightSurfaceVariant
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color by default so our custom design colors are fully preserved
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
