package com.example.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.model.FallingBubble
import com.example.model.GameColor
import com.example.model.GridBubble
import com.example.model.GridCell
import com.example.model.LevelConfig
import com.example.model.LevelGenerator
import com.example.model.PopParticle
import com.example.model.Projectile
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class GameEngine {

    var boardWidth: Float = 0f
    var boardHeight: Float = 0f
    var bubbleRadius: Float = 0f
    var rowSpacing: Float = 0f

    val grid = mutableMapOf<GridCell, GameColor>()

    var currentBubbleColor: GameColor = GameColor.RED
    var nextBubbleColor: GameColor = GameColor.BLUE

    var activeProjectile: Projectile? = null
    val fallingBubbles = mutableListOf<FallingBubble>()
    val popParticles = mutableListOf<PopParticle>()

    var ceilingPixelOffset: Float = 0f
    var shotsUntilCeilingDrop: Int = 8
    var comboCount: Int = 0

    var isGameOver: Boolean = false
    var isLevelComplete: Boolean = false
    var isAiming: Boolean = false
    var aimAngle: Float = -90f // degrees, -90 is straight up

    lateinit var levelConfig: LevelConfig

    val shooterPos: Offset
        get() = Offset(boardWidth / 2f, boardHeight - bubbleRadius * 2.8f)

    val nextBubblePos: Offset
        get() = Offset(shooterPos.x - bubbleRadius * 3.4f, shooterPos.y)

    val dangerLineY: Float
        get() = shooterPos.y - bubbleRadius * 2.5f

    fun initDimensions(width: Float, height: Float) {
        boardWidth = width
        boardHeight = height
        bubbleRadius = width / (LevelGenerator.COLS * 2f)
        rowSpacing = bubbleRadius * sqrt(3.0f)
    }

    fun startLevel(level: Int) {
        levelConfig = LevelGenerator.getLevelConfig(level)
        grid.clear()
        grid.putAll(LevelGenerator.generateBoard(level))

        fallingBubbles.clear()
        popParticles.clear()
        activeProjectile = null

        ceilingPixelOffset = 0f
        shotsUntilCeilingDrop = levelConfig.ceilingShots
        comboCount = 0
        isGameOver = false
        isLevelComplete = false
        isAiming = false
        aimAngle = -90f

        currentBubbleColor = pickRandomBubbleColor()
        nextBubbleColor = pickRandomBubbleColor()
    }

    fun swapBubbles(): Boolean {
        if (activeProjectile != null || isGameOver || isLevelComplete) return false
        val temp = currentBubbleColor
        currentBubbleColor = nextBubbleColor
        nextBubbleColor = temp
        return true
    }

    fun pickRandomBubbleColor(): GameColor {
        val colorsOnBoard = grid.values.distinct()
        return if (colorsOnBoard.isNotEmpty()) {
            colorsOnBoard.random()
        } else if (::levelConfig.isInitialized) {
            levelConfig.availableColors.random()
        } else {
            GameColor.RED
        }
    }

    /**
     * Compute cell center in pixel coordinates with ceilingPixelOffset applied
     */
    fun getCellCenter(row: Int, col: Int): Offset {
        val isEven = (row % 2 == 0)
        val x = if (isEven) {
            bubbleRadius + col * 2f * bubbleRadius
        } else {
            bubbleRadius * 2f + col * 2f * bubbleRadius
        }
        val y = bubbleRadius + ceilingPixelOffset + row * rowSpacing
        return Offset(x, y)
    }

    /**
     * Update aim angle from touch point. Returns true if aim is active.
     */
    fun updateAim(touchX: Float, touchY: Float): Boolean {
        if (activeProjectile != null || isGameOver || isLevelComplete) {
            isAiming = false
            return false
        }
        val dx = touchX - shooterPos.x
        val dy = touchY - shooterPos.y

        // Disallow shooting downwards or too close to shooter
        if (dy >= -bubbleRadius * 0.5f) {
            isAiming = false
            return false
        }

        var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        // Clamp angle between -165° (left) and -15° (right)
        angleDeg = angleDeg.coerceIn(-165f, -15f)
        aimAngle = angleDeg
        isAiming = true
        return true
    }

    /**
     * Calculates trajectory dots with 1 wall bounce
     */
    fun calculateTrajectory(): List<Offset> {
        val points = mutableListOf<Offset>()
        val rad = Math.toRadians(aimAngle.toDouble())
        var dirX = cos(rad).toFloat()
        var dirY = sin(rad).toFloat()

        var currX = shooterPos.x
        var currY = shooterPos.y
        val stepSize = bubbleRadius * 0.6f
        var bounces = 0
        val maxSteps = 140

        for (i in 0 until maxSteps) {
            currX += dirX * stepSize
            currY += dirY * stepSize

            // Check wall bounce
            if (currX <= bubbleRadius) {
                currX = bubbleRadius
                dirX = -dirX
                bounces++
                if (bounces > 1) break
            } else if (currX >= boardWidth - bubbleRadius) {
                currX = boardWidth - bubbleRadius
                dirX = -dirX
                bounces++
                if (bounces > 1) break
            }

            points.add(Offset(currX, currY))

            // Check ceiling collision
            if (currY <= bubbleRadius + ceilingPixelOffset) break

            // Check bubble collision
            var hitBubble = false
            for ((cell, _) in grid) {
                val center = getCellCenter(cell.row, cell.col)
                if (hypot(currX - center.x, currY - center.y) <= bubbleRadius * 1.88f) {
                    hitBubble = true
                    break
                }
            }
            if (hitBubble) break
        }

        return points
    }

    fun fireBubble(): Boolean {
        if (!isAiming || activeProjectile != null || isGameOver || isLevelComplete) return false
        isAiming = false

        val rad = Math.toRadians(aimAngle.toDouble())
        val speed = bubbleRadius * 38f // Fast, crisp projectile speed
        val vx = (cos(rad) * speed).toFloat()
        val vy = (sin(rad) * speed).toFloat()

        activeProjectile = Projectile(
            x = shooterPos.x,
            y = shooterPos.y,
            vx = vx,
            vy = vy,
            color = currentBubbleColor
        )
        return true
    }

    sealed class GameEvent {
        data class Popped(val count: Int, val combo: Int, val orphanCount: Int = 0) : GameEvent()
        data class OrphansDropped(val count: Int) : GameEvent()
        object LevelWon : GameEvent()
        object LevelLost : GameEvent()
        object CeilingDropped : GameEvent()
        object None : GameEvent()
    }

    /**
     * Update physics using micro-substeps to guarantee zero tunneling
     */
    fun update(dt: Float): GameEvent {
        var event: GameEvent = GameEvent.None

        // 1. Update falling orphan bubbles
        val fallingIterator = fallingBubbles.iterator()
        while (fallingIterator.hasNext()) {
            val fb = fallingIterator.next()
            fb.vy += 1400f * dt // gravity
            fb.x += fb.vx * dt
            fb.y += fb.vy * dt
            fb.rotation += fb.rotationSpeed * dt
            fb.alpha = (fb.alpha - 0.5f * dt).coerceAtLeast(0f)
            if (fb.y > boardHeight + bubbleRadius * 2 || fb.alpha <= 0.05f) {
                fallingIterator.remove()
            }
        }

        // 2. Update pop particles
        val particleIterator = popParticles.iterator()
        while (particleIterator.hasNext()) {
            val p = particleIterator.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.life -= p.decay
            if (p.life <= 0f) {
                particleIterator.remove()
            }
        }

        // 3. Update projectile with sub-stepping
        val proj = activeProjectile ?: return event
        val maxStep = 0.005f
        var remaining = dt

        while (remaining > 0.0001f && activeProjectile != null) {
            val step = remaining.coerceAtMost(maxStep)
            remaining -= step

            proj.x += proj.vx * step
            proj.y += proj.vy * step

            // Left wall bounce
            if (proj.x <= bubbleRadius) {
                proj.x = bubbleRadius
                proj.vx = -proj.vx
            }
            // Right wall bounce
            if (proj.x >= boardWidth - bubbleRadius) {
                proj.x = boardWidth - bubbleRadius
                proj.vx = -proj.vx
            }

            // Ceiling hit
            var collided = false
            if (proj.y <= bubbleRadius + ceilingPixelOffset) {
                collided = true
            } else {
                // Check collision with existing grid bubbles
                for ((cell, _) in grid) {
                    val center = getCellCenter(cell.row, cell.col)
                    if (hypot(proj.x - center.x, proj.y - center.y) <= bubbleRadius * 1.88f) {
                        collided = true
                        break
                    }
                }
            }

            if (collided) {
                activeProjectile = null
                event = snapAndResolve(proj)
                break
            }
        }

        return event
    }

    private fun snapAndResolve(proj: Projectile): GameEvent {
        // Find closest empty cell that is attached (row 0 or adjacent to an existing bubble)
        var bestCell: GridCell? = null
        var minDistance = Float.MAX_VALUE

        // Search through plausible rows
        for (r in 0 until 24) {
            val cols = LevelGenerator.getColsInRow(r)
            for (c in 0 until cols) {
                val cell = GridCell(r, c)
                if (cell in grid) continue // must be empty

                // Must be connected to ceiling (row 0) or adjacent to an existing bubble
                val isAttached = (r == 0) || LevelGenerator.getNeighbors(r, c).any { it in grid }
                if (!isAttached) continue

                val center = getCellCenter(r, c)
                val dist = hypot(proj.x - center.x, proj.y - center.y)
                if (dist < minDistance) {
                    minDistance = dist
                    bestCell = cell
                }
            }
        }

        val landedCell = bestCell ?: run {
            // Fallback: row 0 nearest empty column
            var fallbackCol = ((proj.x - bubbleRadius) / (bubbleRadius * 2f)).toInt()
                .coerceIn(0, LevelGenerator.COLS - 1)
            var cand = GridCell(0, fallbackCol)
            if (cand in grid) {
                // Find nearest empty column
                val emptyCol = (0 until LevelGenerator.COLS).minByOrNull { c ->
                    if (GridCell(0, c) in grid) 999 else kotlin.math.abs(c - fallbackCol)
                } ?: 0
                cand = GridCell(0, emptyCol)
            }
            cand
        }

        grid[landedCell] = proj.color

        // Match 3+ flood fill
        val matches = findConnectedMatches(landedCell, proj.color)
        var returnEvent: GameEvent = GameEvent.None

        if (matches.size >= 3) {
            comboCount++
            // Pop matched bubbles
            for (cell in matches) {
                grid.remove(cell)
                spawnPopParticles(cell, proj.color)
            }

            // Orphan check: any bubbles disconnected from ceiling must fall!
            val orphans = findOrphans()
            if (orphans.isNotEmpty()) {
                for (orphan in orphans) {
                    val color = grid.remove(orphan) ?: continue
                    val center = getCellCenter(orphan.row, orphan.col)
                    fallingBubbles.add(
                        FallingBubble(
                            x = center.x,
                            y = center.y,
                            vx = Random.nextFloat() * 160f - 80f,
                            vy = Random.nextFloat() * -140f - 40f,
                            color = color,
                            rotationSpeed = Random.nextFloat() * 200f - 100f
                        )
                    )
                }
            }
            returnEvent = GameEvent.Popped(matches.size, comboCount, orphans.size)
        } else {
            // No match
            comboCount = 0
            shotsUntilCeilingDrop--
            if (shotsUntilCeilingDrop <= 0) {
                ceilingPixelOffset += rowSpacing
                shotsUntilCeilingDrop = levelConfig.ceilingShots
                returnEvent = GameEvent.CeilingDropped
            }
        }

        // Win check: grid is completely clear!
        if (grid.isEmpty()) {
            isLevelComplete = true
            return GameEvent.LevelWon
        }

        // Lose check: any remaining bubble crossed the danger line
        for ((cell, _) in grid) {
            val center = getCellCenter(cell.row, cell.col)
            if (center.y + bubbleRadius >= dangerLineY) {
                isGameOver = true
                return GameEvent.LevelLost
            }
        }

        // Cycle next bubble
        currentBubbleColor = nextBubbleColor
        nextBubbleColor = pickRandomBubbleColor()

        return returnEvent
    }

    private fun findConnectedMatches(start: GridCell, color: GameColor): Set<GridCell> {
        val matches = mutableSetOf<GridCell>()
        val queue = ArrayDeque<GridCell>()
        queue.add(start)
        matches.add(start)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val neighbors = LevelGenerator.getNeighbors(current.row, current.col)
            for (n in neighbors) {
                if (n !in matches && grid[n] == color) {
                    matches.add(n)
                    queue.add(n)
                }
            }
        }
        return matches
    }

    private fun findOrphans(): Set<GridCell> {
        val supported = mutableSetOf<GridCell>()
        val queue = ArrayDeque<GridCell>()

        // All bubbles in row 0 are anchored to the top wall
        for ((cell, _) in grid) {
            if (cell.row == 0) {
                supported.add(cell)
                queue.add(cell)
            }
        }

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            for (neighbor in LevelGenerator.getNeighbors(current.row, current.col)) {
                if (neighbor in grid && neighbor !in supported) {
                    supported.add(neighbor)
                    queue.add(neighbor)
                }
            }
        }

        return grid.keys.filter { it !in supported }.toSet()
    }

    private fun spawnPopParticles(cell: GridCell, color: GameColor) {
        val center = getCellCenter(cell.row, cell.col)
        val particleCount = 14
        for (i in 0 until particleCount) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 240f + 70f
            popParticles.add(
                PopParticle(
                    x = center.x,
                    y = center.y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    color = if (i % 2 == 0) color.baseColor else color.highlightColor,
                    radius = bubbleRadius * (Random.nextFloat() * 0.24f + 0.12f),
                    life = 1f,
                    decay = Random.nextFloat() * 0.035f + 0.035f
                )
            )
        }
    }
}
