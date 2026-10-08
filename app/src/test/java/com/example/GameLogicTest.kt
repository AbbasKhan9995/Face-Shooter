package com.example

import com.example.game.GameEngine
import com.example.model.GameColor
import com.example.model.GridCell
import com.example.model.LevelGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameLogicTest {

    @Test
    fun testLevelDifficultyRamp() {
        val lvl1 = LevelGenerator.getLevelConfig(1)
        assertEquals(3, lvl1.availableColors.size)
        assertEquals(5, lvl1.startingRows)
        assertEquals(8, lvl1.ceilingShots)

        val lvl20 = LevelGenerator.getLevelConfig(20)
        assertEquals(4, lvl20.availableColors.size)

        val lvl40 = LevelGenerator.getLevelConfig(40)
        assertEquals(5, lvl40.availableColors.size)

        val lvl100 = LevelGenerator.getLevelConfig(100)
        assertEquals(6, lvl100.availableColors.size)
        assertEquals(11, lvl100.startingRows)
        assertEquals(3, lvl100.ceilingShots)
    }

    @Test
    fun testBoardGenerationDeterministicAndNoImmediate3Match() {
        val board1 = LevelGenerator.generateBoard(1)
        val board2 = LevelGenerator.generateBoard(1)
        assertEquals(board1, board2)

        // Verify no 3+ same-color clusters exist at start
        for ((cell, color) in board1) {
            val visited = mutableSetOf<GridCell>()
            val queue = ArrayDeque<GridCell>()
            queue.add(cell)
            visited.add(cell)
            while (queue.isNotEmpty()) {
                val curr = queue.removeFirst()
                for (neighbor in LevelGenerator.getNeighbors(curr.row, curr.col)) {
                    if (neighbor !in visited && board1[neighbor] == color) {
                        visited.add(neighbor)
                        queue.add(neighbor)
                    }
                }
            }
            assertTrue("Expected initial cluster size < 3 for cell $cell, got ${visited.size}", visited.size < 3)
        }
    }

    @Test
    fun testAimAngleClamping() {
        val engine = GameEngine()
        engine.initDimensions(600f, 1000f)
        engine.startLevel(1)

        // Aiming almost horizontal to left: should be clamped to -165
        engine.updateAim(0f, engine.shooterPos.y - 50f)
        assertEquals(-165f, engine.aimAngle, 0.1f)

        // Aiming straight up: should be -90
        engine.updateAim(engine.shooterPos.x, engine.shooterPos.y - 200f)
        assertEquals(-90f, engine.aimAngle, 0.1f)

        // Aiming almost horizontal to right: should be clamped to -15
        engine.updateAim(engine.boardWidth, engine.shooterPos.y - 50f)
        assertEquals(-15f, engine.aimAngle, 0.1f)

        // Aiming downward: should disallow
        val aimResult = engine.updateAim(engine.shooterPos.x, engine.shooterPos.y + 100f)
        assertFalse(aimResult)
    }

    @Test
    fun testTrajectoryWallBounce() {
        val engine = GameEngine()
        engine.initDimensions(600f, 1000f)
        engine.startLevel(1)

        // Aim towards upper-left wall
        engine.updateAim(50f, engine.shooterPos.y - 300f)
        val points = engine.calculateTrajectory()
        assertTrue("Trajectory should have multiple points", points.isNotEmpty())
    }

    @Test
    fun testSwapBubbles() {
        val engine = GameEngine()
        engine.initDimensions(600f, 1000f)
        engine.startLevel(1)

        val current = engine.currentBubbleColor
        val next = engine.nextBubbleColor

        engine.swapBubbles()
        assertEquals(next, engine.currentBubbleColor)
        assertEquals(current, engine.nextBubbleColor)
    }

    @Test
    fun testFireAndWallBounce() {
        val engine = GameEngine()
        engine.initDimensions(600f, 1000f)
        engine.startLevel(1)

        engine.updateAim(engine.shooterPos.x, engine.shooterPos.y - 400f)
        assertTrue(engine.fireBubble())
        assertNotNull(engine.activeProjectile)

        val proj = engine.activeProjectile!!
        // Simulate step
        engine.update(0.016f)
        assertTrue(proj.y < engine.shooterPos.y)
    }

    @Test
    fun testCeilingDrop() {
        val engine = GameEngine()
        engine.initDimensions(600f, 1000f)
        engine.startLevel(1)

        val initialOffset = engine.ceilingPixelOffset
        assertEquals(0f, initialOffset, 0.01f)
        assertEquals(8, engine.shotsUntilCeilingDrop)

        var shots = 0
        while (engine.ceilingPixelOffset == 0f && shots < 25) {
            shots++
            val proj = com.example.model.Projectile(
                x = engine.bubbleRadius * 2f * (shots % 8 + 1f),
                y = engine.ceilingPixelOffset + engine.bubbleRadius * 1.5f,
                vx = 0f,
                vy = -200f,
                color = if (shots % 2 == 0) GameColor.YELLOW else GameColor.PINK
            )
            engine.activeProjectile = proj
            engine.update(0.05f)
        }

        assertTrue("Ceiling offset should increase after ceiling drop", engine.ceilingPixelOffset > 0f)
    }
}
