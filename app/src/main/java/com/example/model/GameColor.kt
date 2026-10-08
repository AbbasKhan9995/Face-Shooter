package com.example.model

import androidx.compose.ui.graphics.Color

enum class GameColor(
    val id: Int,
    val displayName: String,
    val baseColor: Color,
    val ringColor: Color,
    val darkShade: Color,
    val highlightColor: Color
) {
    RED(
        id = 0,
        displayName = "Red",
        baseColor = Color(0xFFEF4444),
        ringColor = Color(0xFFDC2626),
        darkShade = Color(0xFF991B1B),
        highlightColor = Color(0xFFFCA5A5)
    ),
    BLUE(
        id = 1,
        displayName = "Blue",
        baseColor = Color(0xFF3B82F6),
        ringColor = Color(0xFF2563EB),
        darkShade = Color(0xFF1E40AF),
        highlightColor = Color(0xFF93C5FD)
    ),
    GREEN(
        id = 2,
        displayName = "Green",
        baseColor = Color(0xFF10B981),
        ringColor = Color(0xFF059669),
        darkShade = Color(0xFF065F46),
        highlightColor = Color(0xFF6EE7B7)
    ),
    YELLOW(
        id = 3,
        displayName = "Yellow",
        baseColor = Color(0xFFF59E0B),
        ringColor = Color(0xFFD97706),
        darkShade = Color(0xFF92400E),
        highlightColor = Color(0xFFFDE68A)
    ),
    PURPLE(
        id = 4,
        displayName = "Purple",
        baseColor = Color(0xFF8B5CF6),
        ringColor = Color(0xFF7C3AED),
        darkShade = Color(0xFF5B21B6),
        highlightColor = Color(0xFFC4B5FD)
    ),
    PINK(
        id = 5,
        displayName = "Pink",
        baseColor = Color(0xFFEC4899),
        ringColor = Color(0xFFDB2777),
        darkShade = Color(0xFF9D174D),
        highlightColor = Color(0xFFFBCFE8)
    );

    companion object {
        fun fromId(id: Int): GameColor = entries.firstOrNull { it.id == id } ?: RED

        fun getAvailableColorsForLevel(level: Int): List<GameColor> = when {
            level <= 15 -> listOf(RED, BLUE, GREEN)
            level <= 30 -> listOf(RED, BLUE, GREEN, YELLOW)
            level <= 50 -> listOf(RED, BLUE, GREEN, YELLOW, PURPLE)
            else -> entries
        }
    }
}
