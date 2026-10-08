package com.example.model

import kotlin.random.Random

data class LevelConfig(
    val levelNumber: Int,
    val startingRows: Int,
    val availableColors: List<GameColor>,
    val ceilingShots: Int
)

object LevelGenerator {
    const val MAX_LEVELS = 100
    const val COLS = 10

    fun getLevelConfig(level: Int): LevelConfig {
        val clampedLevel = level.coerceIn(1, MAX_LEVELS)
        val startingRows = (5 + (clampedLevel - 1) / 12).coerceAtMost(11)
        val availableColors = GameColor.getAvailableColorsForLevel(clampedLevel)
        val ceilingShots = (8 - ((clampedLevel - 1) * 5) / 99).coerceIn(3, 8)
        return LevelConfig(
            levelNumber = clampedLevel,
            startingRows = startingRows,
            availableColors = availableColors,
            ceilingShots = ceilingShots
        )
    }

    /**
     * Number of columns in a given row (even row = COLS, odd row = COLS - 1)
     */
    fun getColsInRow(row: Int): Int = if (row % 2 == 0) COLS else COLS - 1

    /**
     * Deterministically generate the starting board for the given level.
     * Guarantees no pre-existing 3+ connected matches of same color.
     */
    fun generateBoard(level: Int): Map<GridCell, GameColor> {
        val config = getLevelConfig(level)
        val rng = Random(level * 31337L + 777L)
        val board = mutableMapOf<GridCell, GameColor>()

        for (r in 0 until config.startingRows) {
            val cols = getColsInRow(r)
            for (c in 0 until cols) {
                val cell = GridCell(r, c)
                board[cell] = config.availableColors[rng.nextInt(config.availableColors.size)]
            }
        }

        // Iteratively resolve any clusters of size >= 3
        var hasMatch = true
        var passes = 0
        while (hasMatch && passes < 30) {
            hasMatch = false
            passes++
            for ((cell, color) in board.entries.toList()) {
                val count = countMatchesForCell(board, cell, color)
                if (count >= 3) {
                    hasMatch = true
                    // Choose an alternate color among availableColors that produces minimal cluster
                    val otherColors = config.availableColors.filter { it != color }
                    val bestColor = otherColors.minByOrNull { cand ->
                        board[cell] = cand
                        countMatchesForCell(board, cell, cand)
                    } ?: config.availableColors.random(rng)
                    board[cell] = bestColor
                }
            }
        }
        return board
    }

    /**
     * Helper to find size of same-color connected component for a cell in the given board.
     */
    private fun countMatchesForCell(
        board: Map<GridCell, GameColor>,
        startCell: GridCell,
        color: GameColor
    ): Int {
        val visited = mutableSetOf<GridCell>()
        val queue = ArrayDeque<GridCell>()
        queue.add(startCell)
        visited.add(startCell)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val neighbors = getNeighbors(current.row, current.col)
            for (neighbor in neighbors) {
                if (neighbor !in visited && board[neighbor] == color) {
                    visited.add(neighbor)
                    queue.add(neighbor)
                }
            }
        }
        return visited.size
    }

    /**
     * Hex grid neighbors for (row, col)
     */
    fun getNeighbors(row: Int, col: Int): List<GridCell> {
        val list = mutableListOf<GridCell>()
        val colsInCurrent = getColsInRow(row)

        // Same row: left and right
        if (col > 0) list.add(GridCell(row, col - 1))
        if (col < colsInCurrent - 1) list.add(GridCell(row, col + 1))

        val isEven = (row % 2 == 0)
        // Upper row neighbors (if row > 0)
        if (row > 0) {
            val topCols = getColsInRow(row - 1)
            if (isEven) {
                // Even row: (row-1, col-1) and (row-1, col)
                if (col - 1 in 0 until topCols) list.add(GridCell(row - 1, col - 1))
                if (col in 0 until topCols) list.add(GridCell(row - 1, col))
            } else {
                // Odd row: (row-1, col) and (row-1, col+1)
                if (col in 0 until topCols) list.add(GridCell(row - 1, col))
                if (col + 1 in 0 until topCols) list.add(GridCell(row - 1, col + 1))
            }
        }

        // Lower row neighbors
        val bottomCols = getColsInRow(row + 1)
        if (isEven) {
            if (col - 1 in 0 until bottomCols) list.add(GridCell(row + 1, col - 1))
            if (col in 0 until bottomCols) list.add(GridCell(row + 1, col))
        } else {
            if (col in 0 until bottomCols) list.add(GridCell(row + 1, col))
            if (col + 1 in 0 until bottomCols) list.add(GridCell(row + 1, col + 1))
        }

        return list
    }
}
