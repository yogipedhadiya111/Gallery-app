package com.aistudio.xplayer.vdpkzm.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.aistudio.xplayer.vdpkzm.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // Animations for logo scale & fade
    val scale = remember { Animatable(0.7f) }
    val alpha = remember { Animatable(0f) }
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        // Animate scale & alpha concurrently
        scale.animateTo(
            targetValue = 1.05f,
            animationSpec = tween(1000, easing = EaseOutCubic)
        )
        // Gentle bounce back to 1f
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(300, easing = EaseInOutSine)
        )
    }

    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(800, easing = EaseInCubic)
        )
    }

    // Simulate progress bar filling
    LaunchedEffect(Unit) {
        val steps = 20
        for (i in 1..steps) {
            delay(60)
            progress = i.toFloat() / steps
        }
        delay(200)
        onSplashFinished()
    }

    val isLightTheme = androidx.compose.material3.MaterialTheme.colorScheme.background.red > 0.5f
    val backgroundColorColors = if (isLightTheme) {
        listOf(
            Color(0xFFFFFFFF),
            Color(0xFFF8F9FC),
            Color(0xFFE8E4FF)
        )
    } else {
        listOf(
            Color(0xFF0F0A1C),
            Color(0xFF07050E),
            Color(0xFF020104)
        )
    }
    val textColor = if (isLightTheme) Color(0xFF1B1B1F) else Color.White
    val sloganColor = if (isLightTheme) Color(0xFF6E3AFF) else Color(0xFFFFD700)
    val logoBg = if (isLightTheme) Color(0xFFFFFFFF) else Color(0xFF141624).copy(alpha = 0.8f)
    val logoBorder = if (isLightTheme) Color(0xFF6E3AFF).copy(alpha = 0.6f) else Color(0xFFFFD700).copy(alpha = 0.4f)
    val logoTint = if (isLightTheme) Color(0xFF6E3AFF) else Color(0xFFFFD700)
    val progressColor = if (isLightTheme) Color(0xFF6E3AFF) else Color(0xFFFFD700)
    val progressTrackColor = if (isLightTheme) Color(0xFF6E3AFF).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = backgroundColorColors
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Brand Logo container with double halo glow
            Box(
                modifier = Modifier
                    .scale(scale.value)
                    .alpha(alpha.value)
                    .size(100.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = if (isLightTheme) {
                                listOf(Color(0xFF6E3AFF).copy(alpha = 0.12f), Color.Transparent)
                            } else {
                                listOf(Color(0xFF6E3AFF).copy(alpha = 0.25f), Color.Transparent)
                            }
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Secondary glow border
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .border(1.5.dp, logoBorder, CircleShape)
                        .background(logoBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "XPlayer Logo",
                        tint = logoTint,
                        modifier = Modifier
                            .size(42.dp)
                            .offset(x = 3.dp) // Optical center correction for triangle play button
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Display Title
            Text(
                text = "XPLAYER",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                color = textColor,
                modifier = Modifier.alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Premium Slogan
            Text(
                text = stringResource(id = R.string.splash_slogan),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = sloganColor.copy(alpha = 0.85f),
                modifier = Modifier.alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Luxury horizontal linear progress bar
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(3.dp)
                    .alpha(alpha.value)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = progressColor,
                    trackColor = progressTrackColor,
                )
            }
        }
    }
}
