package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.GameViewModel
import com.example.ui.components.drawGlossyBubble
import com.example.ui.theme.GamePurple
import com.example.ui.theme.LavenderBg
import kotlin.math.hypot

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onBackClicked: () -> Unit,
    onLevelsClicked: () -> Unit
) {
    BackHandler { onBackClicked() }

    val uiState by viewModel.uiState.collectAsState()
    val slotBitmaps by viewModel.photoManager.slotImageBitmaps.collectAsState()
    val frameTick = viewModel.frameTick.longValue

    // Smooth 60fps Game Animation Loop
    LaunchedEffect(Unit) {
        var lastTime = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastTime != 0L) {
                    val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.033f)
                    viewModel.tick(dt)
                }
                lastTime = now
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LavenderBg)
            .testTag("game_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 34.dp, bottom = 10.dp, start = 12.dp, end = 12.dp)
        ) {
            // HUD Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button
                IconButton(
                    onClick = onBackClicked,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.9f))
                        .testTag("game_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = GamePurple
                    )
                }

                // Level & Ceiling Pressure Badge
                Surface(
                    color = Color.White.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 3.dp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Level ${uiState.currentLevel}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF2E1065)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        // Shots until ceiling drop
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Ceiling: ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF6B7280)
                            )
                            Text(
                                text = "${uiState.shotsUntilDrop} shots",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.shotsUntilDrop <= 2) Color(0xFFEF4444) else GamePurple
                            )
                        }
                    }
                }

                // Controls: Restart & Sound
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { viewModel.restartCurrentLevel() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f))
                            .testTag("restart_level_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restart Level",
                            tint = Color(0xFF4B5563)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleSound() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f))
                            .testTag("game_sound_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (uiState.isSoundEnabled) "Mute" else "Unmute",
                            tint = GamePurple
                        )
                    }
                }
            }

            // Play Area (Inside a Rounded-Rectangle Card)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("play_area_card"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val widthPx = constraints.maxWidth.toFloat()
                    val heightPx = constraints.maxHeight.toFloat()

                    // Initialize dimensions once in the game engine
                    LaunchedEffect(widthPx, heightPx) {
                        viewModel.initDimensions(widthPx, heightPx)
                    }

                    val engine = viewModel.engine

                    // Unified Touch Input: Supports both drag-aim-release AND tap-to-shoot & swap
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val downPos = down.position
                                    val shooterDist = hypot(downPos.x - engine.shooterPos.x, downPos.y - engine.shooterPos.y)
                                    val nextDist = hypot(downPos.x - engine.nextBubblePos.x, downPos.y - engine.nextBubblePos.y)

                                    if (shooterDist <= engine.bubbleRadius * 2.4f || nextDist <= engine.bubbleRadius * 2.4f) {
                                        // Tapped on shooter or next bubble -> swap!
                                        viewModel.swapBubbles()
                                        waitForUpOrCancellation()
                                    } else {
                                        // Aiming
                                        var isAiming = viewModel.updateAim(downPos.x, downPos.y)
                                        val pointerId = down.id

                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                            if (!change.pressed) {
                                                // Pointer released
                                                if (isAiming) {
                                                    viewModel.fireBubble()
                                                }
                                                break
                                            } else {
                                                // Pointer dragged
                                                val currentPos = change.position
                                                isAiming = viewModel.updateAim(currentPos.x, currentPos.y)
                                                change.consume()
                                            }
                                        }
                                    }
                                }
                            }
                    ) {
                        // Main Canvas
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Register frameTick to trigger recomposition every frame!
                            val _tick = frameTick
                            val radius = engine.bubbleRadius
                            if (radius <= 0f) return@Canvas

                            // 1. Ceiling block (visual ceiling that lowers with pressure)
                            val ceilingY = engine.ceilingPixelOffset
                            if (ceilingY > 0f) {
                                drawRect(
                                    color = Color(0xFFDDD6FE).copy(alpha = 0.65f),
                                    topLeft = Offset(0f, 0f),
                                    size = Size(size.width, ceilingY)
                                )
                            }
                            drawLine(
                                color = Color(0xFF8B5CF6),
                                start = Offset(0f, ceilingY),
                                end = Offset(size.width, ceilingY),
                                strokeWidth = 8f
                            )

                            // 2. Danger line near shooter
                            val dangerY = engine.dangerLineY
                            drawLine(
                                color = Color(0xFFF43F5E).copy(alpha = 0.65f),
                                start = Offset(8f, dangerY),
                                end = Offset(size.width - 8f, dangerY),
                                strokeWidth = 3f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                            )

                            // 3. Settled grid bubbles with photos and 3D gloss
                            for ((cell, color) in engine.grid) {
                                val center = engine.getCellCenter(cell.row, cell.col)
                                drawGlossyBubble(
                                    center = center,
                                    radius = radius,
                                    color = color,
                                    faceBitmap = slotBitmaps[color.id]
                                )
                            }

                            // 4. Falling orphan bubbles with gravity and rotation
                            for (fb in engine.fallingBubbles) {
                                drawGlossyBubble(
                                    center = Offset(fb.x, fb.y),
                                    radius = radius,
                                    color = fb.color,
                                    faceBitmap = slotBitmaps[fb.color.id],
                                    alpha = fb.alpha
                                )
                            }

                            // 5. Pop Particles
                            for (p in engine.popParticles) {
                                drawCircle(
                                    color = p.color.copy(alpha = p.life.coerceIn(0f, 1f)),
                                    radius = p.radius * p.life,
                                    center = Offset(p.x, p.y)
                                )
                            }

                            // 6. Aiming Trajectory Line (Dotted with wall reflection preview)
                            if (engine.isAiming) {
                                val trajectoryPoints = engine.calculateTrajectory()
                                val dotColor = engine.currentBubbleColor.ringColor.copy(alpha = 0.85f)
                                for (i in trajectoryPoints.indices step 2) {
                                    val pt = trajectoryPoints[i]
                                    drawCircle(
                                        color = dotColor,
                                        radius = radius * 0.16f,
                                        center = pt
                                    )
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.9f),
                                        radius = radius * 0.08f,
                                        center = pt
                                    )
                                }
                            }

                            // 7. Shooter Dock Base (Pedestal)
                            drawCircle(
                                color = Color(0xFFF1F5F9),
                                radius = radius * 1.35f,
                                center = engine.shooterPos
                            )
                            drawCircle(
                                color = Color(0xFFCBD5E1),
                                radius = radius * 1.35f,
                                center = engine.shooterPos,
                                style = Stroke(width = 3f)
                            )

                            // 8. Next Bubble Dock
                            drawCircle(
                                color = Color(0xFFF8FAFC),
                                radius = radius * 1.15f,
                                center = engine.nextBubblePos
                            )
                            drawCircle(
                                color = Color(0xFFE2E8F0),
                                radius = radius * 1.15f,
                                center = engine.nextBubblePos,
                                style = Stroke(width = 2.5f)
                            )

                            // Draw Next bubble
                            drawGlossyBubble(
                                center = engine.nextBubblePos,
                                radius = radius * 0.85f,
                                color = engine.nextBubbleColor,
                                faceBitmap = slotBitmaps[engine.nextBubbleColor.id]
                            )

                            // 9. Current Loaded Bubble in Shooter
                            drawGlossyBubble(
                                center = engine.shooterPos,
                                radius = radius,
                                color = engine.currentBubbleColor,
                                faceBitmap = slotBitmaps[engine.currentBubbleColor.id]
                            )

                            // 10. Flying Active Projectile
                            engine.activeProjectile?.let { proj ->
                                drawGlossyBubble(
                                    center = Offset(proj.x, proj.y),
                                    radius = radius,
                                    color = proj.color,
                                    faceBitmap = slotBitmaps[proj.color.id]
                                )
                            }
                        }

                        // Explicit Clickable Swap Button Badge near the dock
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(
                                    start = 14.dp,
                                    bottom = 8.dp
                                )
                        ) {
                            Surface(
                                onClick = { viewModel.swapBubbles() },
                                color = Color(0xFFEDE9FE),
                                shape = RoundedCornerShape(12.dp),
                                shadowElevation = 2.dp,
                                modifier = Modifier.testTag("swap_bubbles_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = "Swap Bubbles",
                                        tint = GamePurple,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "SWAP",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = GamePurple
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Win Overlay
        AnimatedVisibility(
            visible = uiState.isLevelWon,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .testTag("win_overlay"),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .padding(20.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = Color(0xFFFEF08A),
                            shape = CircleShape,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Celebration,
                                    contentDescription = null,
                                    tint = Color(0xFFCA8A04),
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (uiState.isGameCompletedAll) {
                            Text(
                                text = "All 100 Levels Conquered!",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1E1B4B),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "More levels coming soon! You are a master Face Shooter!",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF4B5563),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "Level Complete!",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1E1B4B),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "All face bubbles popped cleanly!",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF6B7280),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        if (!uiState.isGameCompletedAll) {
                            Button(
                                onClick = { viewModel.nextLevel() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("next_level_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GamePurple)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Next Level", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { viewModel.restartCurrentLevel() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("replay_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Replay", fontWeight = FontWeight.SemiBold)
                            }

                            FilledTonalButton(
                                onClick = onLevelsClicked,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("win_levels_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Levels", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // Lose Overlay ("Try Again")
        AnimatedVisibility(
            visible = uiState.isGameOver,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .testTag("lose_overlay"),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .padding(20.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = Color(0xFFFFE4E6),
                            shape = CircleShape,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SentimentDissatisfied,
                                    contentDescription = null,
                                    tint = Color(0xFFE11D48),
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Try Again!",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E1B4B),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "The bubbles crossed the danger line.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { viewModel.restartCurrentLevel() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("retry_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        FilledTonalButton(
                            onClick = onLevelsClicked,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("lose_levels_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Levels", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
