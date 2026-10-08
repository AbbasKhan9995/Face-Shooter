package com.example.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

data class GridCell(
    val row: Int,
    val col: Int
)

data class GridBubble(
    val row: Int,
    val col: Int,
    val color: GameColor
)

data class Projectile(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: GameColor,
    var active: Boolean = true
)

data class FallingBubble(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: GameColor,
    var rotation: Float = 0f,
    var rotationSpeed: Float = 0f,
    var alpha: Float = 1f
)

data class PopParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val radius: Float,
    var life: Float = 1f, // 1f down to 0f
    val decay: Float = 0.035f
)
