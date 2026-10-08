package com.example.game

import android.app.Application
import androidx.compose.runtime.mutableLongStateOf
import androidx.lifecycle.AndroidViewModel
import com.example.audio.SoundManager
import com.example.data.PhotoManager
import com.example.data.PreferencesManager
import com.example.model.GameColor
import com.example.model.LevelGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GameUiState(
    val currentLevel: Int = 1,
    val shotsUntilDrop: Int = 8,
    val totalCeilingShots: Int = 8,
    val comboCount: Int = 0,
    val isGameOver: Boolean = false,
    val isLevelWon: Boolean = false,
    val isGameCompletedAll: Boolean = false,
    val isSoundEnabled: Boolean = true,
    val maxUnlockedLevel: Int = 1,
    val activeBubblesCount: Int = 0,
    val currentBubbleColor: GameColor = GameColor.RED,
    val nextBubbleColor: GameColor = GameColor.BLUE
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    val preferencesManager = PreferencesManager(application)
    val photoManager = PhotoManager(application)
    val soundManager = SoundManager(preferencesManager)
    val engine = GameEngine()

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // Reactive ticker read by Compose Canvas to force redraw on every physics frame or aim drag
    val frameTick = mutableLongStateOf(0L)

    private var hasInitializedBoard = false

    init {
        _uiState.value = _uiState.value.copy(
            currentLevel = preferencesManager.currentLevel,
            maxUnlockedLevel = preferencesManager.maxUnlockedLevel,
            isSoundEnabled = preferencesManager.isSoundEnabled
        )
    }

    fun initDimensions(width: Float, height: Float) {
        engine.initDimensions(width, height)
        if (!hasInitializedBoard) {
            hasInitializedBoard = true
            startLevel(preferencesManager.currentLevel)
        }
    }

    fun startLevel(level: Int) {
        val targetLevel = level.coerceIn(1, LevelGenerator.MAX_LEVELS)
        preferencesManager.currentLevel = targetLevel
        engine.startLevel(targetLevel)
        syncUiState()
        frameTick.longValue++
    }

    fun restartCurrentLevel() {
        startLevel(_uiState.value.currentLevel)
    }

    fun nextLevel() {
        val next = _uiState.value.currentLevel + 1
        if (next > LevelGenerator.MAX_LEVELS) {
            _uiState.value = _uiState.value.copy(isGameCompletedAll = true)
        } else {
            preferencesManager.unlockNextLevel(_uiState.value.currentLevel)
            startLevel(next)
        }
    }

    fun updateAim(x: Float, y: Float): Boolean {
        val active = engine.updateAim(x, y)
        frameTick.longValue++
        return active
    }

    fun cancelAim() {
        engine.isAiming = false
        frameTick.longValue++
    }

    fun fireBubble(): Boolean {
        val fired = engine.fireBubble()
        if (fired) {
            soundManager.playShoot()
            syncUiState()
            frameTick.longValue++
        }
        return fired
    }

    fun swapBubbles(): Boolean {
        val swapped = engine.swapBubbles()
        if (swapped) {
            soundManager.playSwap()
            syncUiState()
            frameTick.longValue++
        }
        return swapped
    }

    fun toggleSound(): Boolean {
        val enabled = soundManager.toggleSound()
        _uiState.value = _uiState.value.copy(isSoundEnabled = enabled)
        return enabled
    }

    fun tick(dt: Float) {
        val event = engine.update(dt)
        // Always increment frameTick to drive smooth 60fps rendering of projectile, particles & falling bubbles
        frameTick.longValue++

        when (event) {
            is GameEngine.GameEvent.Popped -> {
                soundManager.playPop(event.combo)
                if (event.orphanCount > 0) {
                    soundManager.playOrphanDrop(event.orphanCount)
                }
                syncUiState()
            }
            is GameEngine.GameEvent.OrphansDropped -> {
                soundManager.playOrphanDrop(event.count)
                syncUiState()
            }
            is GameEngine.GameEvent.CeilingDropped -> {
                syncUiState()
            }
            is GameEngine.GameEvent.LevelWon -> {
                soundManager.playWin()
                val completedLevel = _uiState.value.currentLevel
                if (completedLevel >= LevelGenerator.MAX_LEVELS) {
                    _uiState.value = _uiState.value.copy(
                        isLevelWon = true,
                        isGameCompletedAll = true
                    )
                } else {
                    preferencesManager.unlockNextLevel(completedLevel)
                    _uiState.value = _uiState.value.copy(
                        isLevelWon = true,
                        maxUnlockedLevel = preferencesManager.maxUnlockedLevel
                    )
                }
            }
            is GameEngine.GameEvent.LevelLost -> {
                soundManager.playLose()
                _uiState.value = _uiState.value.copy(isGameOver = true)
            }
            GameEngine.GameEvent.None -> {
                // Continuous frame update
            }
        }
    }

    fun syncUiState() {
        _uiState.value = _uiState.value.copy(
            currentLevel = engine.levelConfig.levelNumber,
            shotsUntilDrop = engine.shotsUntilCeilingDrop,
            totalCeilingShots = engine.levelConfig.ceilingShots,
            comboCount = engine.comboCount,
            isGameOver = engine.isGameOver,
            isLevelWon = engine.isLevelComplete,
            isSoundEnabled = preferencesManager.isSoundEnabled,
            maxUnlockedLevel = preferencesManager.maxUnlockedLevel,
            activeBubblesCount = engine.grid.size,
            currentBubbleColor = engine.currentBubbleColor,
            nextBubbleColor = engine.nextBubbleColor
        )
    }
}
