package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.game.GameViewModel
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LevelSelectScreen
import com.example.ui.screens.PhotosScreen
import com.example.ui.theme.FaceShooterTheme
import com.example.ui.theme.LavenderBg

sealed class AppScreen {
    object Home : AppScreen()
    object Game : AppScreen()
    object LevelSelect : AppScreen()
    object Photos : AppScreen()
}

class MainActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FaceShooterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = LavenderBg
                ) {
                    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }

                    when (currentScreen) {
                        is AppScreen.Home -> {
                            HomeScreen(
                                viewModel = gameViewModel,
                                onPlayClicked = { currentScreen = AppScreen.Game },
                                onLevelsClicked = { currentScreen = AppScreen.LevelSelect },
                                onPhotosClicked = { currentScreen = AppScreen.Photos }
                            )
                        }
                        is AppScreen.Game -> {
                            GameScreen(
                                viewModel = gameViewModel,
                                onBackClicked = { currentScreen = AppScreen.Home },
                                onLevelsClicked = { currentScreen = AppScreen.LevelSelect }
                            )
                        }
                        is AppScreen.LevelSelect -> {
                            LevelSelectScreen(
                                viewModel = gameViewModel,
                                onBackClicked = { currentScreen = AppScreen.Home },
                                onLevelSelected = { level ->
                                    gameViewModel.startLevel(level)
                                    currentScreen = AppScreen.Game
                                }
                            )
                        }
                        is AppScreen.Photos -> {
                            PhotosScreen(
                                viewModel = gameViewModel,
                                onBackClicked = { currentScreen = AppScreen.Home }
                            )
                        }
                    }
                }
            }
        }
    }
}
