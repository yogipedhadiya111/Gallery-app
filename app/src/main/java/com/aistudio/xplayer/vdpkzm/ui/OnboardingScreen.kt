package com.aistudio.xplayer.vdpkzm.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.xplayer.vdpkzm.R

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    var currentStep by remember { mutableStateOf(1) } // 1 or 2
    val isLightTheme = MaterialTheme.colorScheme.background.red > 0.5f
    val backgroundColor = if (isLightTheme) Color(0xFFF8F9FC) else DarkNavyBg
    val textColor = if (isLightTheme) Color(0xFF1B1B1F) else Color.White
    val textMutedColor = if (isLightTheme) Color(0xFF5B5FC7) else Color.White.copy(alpha = 0.7f)
    val cardBg = if (isLightTheme) Color(0xFFFFFFFF) else GlassWhite

    // Background ambient animations
    val infiniteTransition = rememberInfiniteTransition(label = "OnboardingGlow")
    val ambientScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "GlowScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .testTag("onboarding_screen_container")
    ) {
        // Glowing visual element in background
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-50).dp)
                .size(320.dp)
                .scale(ambientScale)
                .blur(80.dp)
                .background(if (isLightTheme) RoyalPurple.copy(alpha = 0.07f) else RoyalPurple.copy(alpha = 0.2f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Section: App Brand & Skip Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Slogan label
                Text(
                    text = "XPLAYER",
                    color = if (isLightTheme) RoyalPurple else PremiumGold,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )

                // Skip Button
                TextButton(
                    onClick = onFinished,
                    modifier = Modifier.testTag("onboarding_skip_button")
                ) {
                    Text(
                        text = stringResource(id = R.string.skip_btn),
                        color = if (isLightTheme) RoyalPurple.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Main Content Area (Animated change on step transition)
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }.using(
                        SizeTransform(clip = false)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                label = "OnboardingStepTransition"
            ) { step ->
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Step illustration within a glassmorphic frame
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(if (step == 1) 0.85f else 0.88f)
                            .aspectRatio(1f)
                            .padding(bottom = 24.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = borderStrokeForStep(step, isLightTheme),
                        elevation = if (isLightTheme) CardDefaults.cardElevation(defaultElevation = 2.dp) else CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val imgRes = if (step == 1) {
                                R.drawable.img_onboarding_playback_1782970133999
                            } else {
                                R.drawable.img_onboarding_adjust_1782970147313
                            }
                            Image(
                                painter = painterResource(id = imgRes),
                                contentDescription = if (step == 1) "Smooth Playback" else "Adjust Video View",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(28.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title
                    Text(
                        text = if (step == 1) stringResource(id = R.string.onboarding_title_1) else stringResource(id = R.string.onboarding_title_2),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .testTag("onboarding_title_step_$step")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Subtitle / Description text
                    Text(
                        text = if (step == 1) {
                            stringResource(id = R.string.onboarding_desc_1)
                        } else {
                            stringResource(id = R.string.onboarding_desc_2)
                        },
                        fontSize = 14.sp,
                        color = textMutedColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .testTag("onboarding_subtitle_step_$step")
                    )
                }
            }

            // Bottom Navigation & Actions Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Indicator dots
                Row(
                    modifier = Modifier.padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(1, 2).forEach { step ->
                        val isSelected = currentStep == step
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (isSelected) 18.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) {
                                        if (isLightTheme) RoyalPurple else PremiumGold
                                    } else {
                                        if (isLightTheme) Color.Black.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.2f)
                                    }
                                )
                                .clickable { currentStep = step }
                        )
                    }
                }

                // CTA Button: Next
                Button(
                    onClick = {
                        if (currentStep == 1) {
                            currentStep = 2
                        } else {
                            onFinished()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .testTag("onboarding_next_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(RoyalPurple, Color(0xFF8252FF), RoyalPurpleDark)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.next_btn),
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun borderStrokeForStep(step: Int, isLightTheme: Boolean): androidx.compose.foundation.BorderStroke {
    return androidx.compose.foundation.BorderStroke(
        1.5.dp,
        if (isLightTheme) {
            Color(0xFFE6E8F0)
        } else {
            if (step == 1) RoyalPurple.copy(alpha = 0.3f) else PremiumGold.copy(alpha = 0.3f)
        }
    )
}
