package com.example.data

import androidx.compose.ui.graphics.Color

val BARTER_CATEGORIES = listOf(
    "Design",
    "Photography",
    "Cleaning",
    "Education",
    "Tech",
    "Catering",
    "Home & Repair",
    "Other",
)

val CATEGORY_PIN_COLORS: Map<String, Color> = mapOf(
    "Design" to Color(0xFF6366F1),
    "Photography" to Color(0xFFEC4899),
    "Cleaning" to Color(0xFF14B8A6),
    "Education" to Color(0xFF3B82F6),
    "Tech" to Color(0xFF8B5CF6),
    "Catering" to Color(0xFFF97316),
    "Home & Repair" to Color(0xFF84CC16),
    "Other" to Color(0xFF78716C),
)

fun categoryPinColor(category: String): Color =
    CATEGORY_PIN_COLORS.entries.firstOrNull { it.key.equals(category, ignoreCase = true) }?.value
        ?: CATEGORY_PIN_COLORS["Other"]!!
