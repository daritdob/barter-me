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
    "Fitness",
    "Music",
    "Pets",
    "Gardening",
    "Moving",
    "Automotive",
    "Childcare",
    "Beauty",
    "Writing",
    "Legal & Admin",
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
    "Fitness" to Color(0xFFEF4444),
    "Music" to Color(0xFFD946EF),
    "Pets" to Color(0xFFF59E0B),
    "Gardening" to Color(0xFF22C55E),
    "Moving" to Color(0xFF0EA5E9),
    "Automotive" to Color(0xFF64748B),
    "Childcare" to Color(0xFFFB7185),
    "Beauty" to Color(0xFFA855F7),
    "Writing" to Color(0xFF0D9488),
    "Legal & Admin" to Color(0xFF475569),
    "Other" to Color(0xFF78716C),
)

fun categoryPinColor(category: String): Color =
    CATEGORY_PIN_COLORS.entries.firstOrNull { it.key.equals(category, ignoreCase = true) }?.value
        ?: CATEGORY_PIN_COLORS["Other"]!!
