package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A beautiful iOS-like glassmorphic modifier that adds translucent gradient background,
 * subtle high-contrast gloss border, and dynamic light reflections.
 */
@Composable
fun Modifier.glassmorphic(
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 1.dp,
    isDarkTheme: Boolean = true
): Modifier {
    val roundedShape = RoundedCornerShape(cornerRadius)
    
    // Glass background color depending on theme dark/light
    val glassBgColor = if (isDarkTheme) {
        Color(0xFF2B2930).copy(alpha = 0.35f)
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.55f)
    }

    // High-contrast, refraction-simulating glossy white-to-translucent border gradient
    val borderBrush = if (isDarkTheme) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.03f),
                Color(0xFFD0BCFF).copy(alpha = 0.15f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.60f),
                Color.Black.copy(alpha = 0.05f),
                Color(0xFF6750A4).copy(alpha = 0.20f)
            )
        )
    }

    return this
        .clip(roundedShape)
        .background(glassBgColor)
        .border(
            width = borderWidth,
            brush = borderBrush,
            shape = roundedShape
        )
}

/**
 * A premium Glass Card container that matches the dynamic iOS look-and-feel.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    borderWidth: Dp = 1.dp,
    isDarkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .glassmorphic(
                cornerRadius = cornerRadius,
                borderWidth = borderWidth,
                isDarkTheme = isDarkTheme
            )
    ) {
        content()
    }
}
