package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.model.GameColor

/**
 * Draws a 3D glossy bubble with a clipped face photo inside, colored rim,
 * darker rim shading, and bright specular highlights on top.
 */
fun DrawScope.drawGlossyBubble(
    center: Offset,
    radius: Float,
    color: GameColor,
    faceBitmap: ImageBitmap?,
    scale: Float = 1f,
    alpha: Float = 1f
) {
    if (scale <= 0.01f || alpha <= 0.01f) return
    val r = radius * scale

    // 1. Subtle soft shadow under bubble
    drawCircle(
        color = Color(0x33000000).copy(alpha = 0.2f * alpha),
        radius = r * 1.02f,
        center = Offset(center.x, center.y + r * 0.06f)
    )

    // 2. Base 3D sphere radial gradient
    val sphereGradient = Brush.radialGradient(
        colors = listOf(
            color.highlightColor.copy(alpha = 0.95f * alpha),
            color.baseColor.copy(alpha = alpha),
            color.darkShade.copy(alpha = alpha)
        ),
        center = Offset(center.x - r * 0.3f, center.y - r * 0.3f),
        radius = r * 1.3f
    )
    drawCircle(
        brush = sphereGradient,
        radius = r,
        center = center
    )

    // 3. Clipped Face Photo (Clipped strictly inside bubble sphere)
    val innerRadius = r * 0.82f
    if (faceBitmap != null && innerRadius > 2f) {
        val clipPath = Path().apply {
            addOval(
                Rect(
                    center.x - innerRadius,
                    center.y - innerRadius,
                    center.x + innerRadius,
                    center.y + innerRadius
                )
            )
        }
        clipPath(clipPath) {
            drawImage(
                image = faceBitmap,
                dstOffset = IntOffset(
                    (center.x - innerRadius).toInt(),
                    (center.y - innerRadius).toInt()
                ),
                dstSize = IntSize(
                    (innerRadius * 2f).toInt(),
                    (innerRadius * 2f).toInt()
                ),
                alpha = alpha
            )
        }
    }

    // 4. Colored Accent Ring around the photo
    drawCircle(
        color = color.ringColor.copy(alpha = 0.85f * alpha),
        radius = innerRadius,
        center = center,
        style = Stroke(width = r * 0.12f)
    )

    // 5. 3D Dark Rim Vignette (darker rim around the edges)
    val rimVignette = Brush.radialGradient(
        0.0f to Color.Transparent,
        0.7f to Color.Transparent,
        0.95f to color.darkShade.copy(alpha = 0.55f * alpha),
        1.0f to Color(0x99000000).copy(alpha = 0.65f * alpha),
        center = center,
        radius = r
    )
    drawCircle(
        brush = rimVignette,
        radius = r,
        center = center
    )

    // 6. Secondary soft light bounce at bottom-right
    val bounceLight = Brush.radialGradient(
        colors = listOf(
            color.highlightColor.copy(alpha = 0.40f * alpha),
            Color.Transparent
        ),
        center = Offset(center.x + r * 0.35f, center.y + r * 0.4f),
        radius = r * 0.55f
    )
    drawCircle(
        brush = bounceLight,
        radius = r * 0.9f,
        center = center
    )

    // 7. Bright White Specular Highlights in the top-left (3D glass sheen)
    rotate(degrees = -30f, pivot = center) {
        // Main specular glossy oval
        drawOval(
            color = Color.White.copy(alpha = 0.88f * alpha),
            topLeft = Offset(center.x - r * 0.52f, center.y - r * 0.62f),
            size = Size(r * 0.46f, r * 0.24f)
        )
    }

    // Secondary specular pin-point glint
    drawCircle(
        color = Color.White.copy(alpha = 0.75f * alpha),
        radius = r * 0.08f,
        center = Offset(center.x - r * 0.50f, center.y - r * 0.15f)
    )
}
