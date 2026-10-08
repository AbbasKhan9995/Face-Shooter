package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.GameViewModel
import com.example.model.GameColor
import com.example.ui.components.drawGlossyBubble
import com.example.ui.theme.GamePurple
import com.example.ui.theme.LavenderBg

@Composable
fun HomeScreen(
    viewModel: GameViewModel,
    onPlayClicked: () -> Unit,
    onLevelsClicked: () -> Unit,
    onPhotosClicked: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val slotBitmaps by viewModel.photoManager.slotImageBitmaps.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "float_bubbles")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bubble_float"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LavenderBg)
            .testTag("home_screen_root")
    ) {
        // Sound toggle button in top-right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, end = 20.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = { viewModel.toggleSound() },
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.85f))
                    .testTag("home_sound_toggle_button"),
                colors = IconButtonDefaults.iconButtonColors(contentColor = GamePurple)
            ) {
                Icon(
                    imageVector = if (uiState.isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = if (uiState.isSoundEnabled) "Mute Sound" else "Enable Sound",
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Header with 3D Glossy Bubbles
            Box(
                modifier = Modifier
                    .size(260.dp, 130.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = 34.dp.toPx()
                    // Three floating glossy bubbles showcasing colors and face avatars
                    drawGlossyBubble(
                        center = Offset(size.width * 0.22f, size.height * 0.55f + floatOffset),
                        radius = radius,
                        color = GameColor.RED,
                        faceBitmap = slotBitmaps[GameColor.RED.id]
                    )
                    drawGlossyBubble(
                        center = Offset(size.width * 0.50f, size.height * 0.38f - floatOffset),
                        radius = radius * 1.15f,
                        color = GameColor.BLUE,
                        faceBitmap = slotBitmaps[GameColor.BLUE.id]
                    )
                    drawGlossyBubble(
                        center = Offset(size.width * 0.78f, size.height * 0.55f + floatOffset * 0.7f),
                        radius = radius,
                        color = GameColor.YELLOW,
                        faceBitmap = slotBitmaps[GameColor.YELLOW.id]
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Game Title
            Text(
                text = "Face Shooter",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF2E1065),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Shoot & Pop Your Friends' Faces!",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF5B21B6),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            // Main Play Card / Button
            Button(
                onClick = onPlayClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .shadow(8.dp, RoundedCornerShape(24.dp))
                    .testTag("play_button"),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GamePurple,
                    contentColor = Color.White
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Play Level ${uiState.currentLevel}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Secondary Buttons: Levels and Photos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FilledTonalButton(
                    onClick = onLevelsClicked,
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                        .testTag("levels_button"),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.95f),
                        contentColor = Color(0xFF4C1D95)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatListNumbered,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Levels",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                FilledTonalButton(
                    onClick = onPhotosClicked,
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                        .testTag("photos_button"),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.95f),
                        contentColor = Color(0xFF4C1D95)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Photos",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Quick Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFFDDD6FE),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("💡", fontSize = 18.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Tap the shooter to swap bubbles. Drag and release to fire!",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4B5563)
                    )
                }
            }
        }
    }
}
