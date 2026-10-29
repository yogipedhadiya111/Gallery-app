package com.aistudio.xplayer.vdpkzm.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.aistudio.xplayer.vdpkzm.R

data class LanguageItem(
    val code: String,
    val name: String,
    val nativeName: String,
    val flag: String
)

val popularLanguages = listOf(
    LanguageItem("en", "English", "English", "🇺🇸"),
    LanguageItem("es", "Spanish", "Español", "🇪🇸"),
    LanguageItem("hi", "Hindi", "हिन्दी", "🇮🇳")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionScreen(
    currentSelectedLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    var selectedLanguageName by remember { mutableStateOf(currentSelectedLanguage) }
    var searchQuery by remember { mutableStateOf("") }

    val isLightTheme = MaterialTheme.colorScheme.background.red > 0.5f
    val backgroundColor = if (isLightTheme) Color(0xFFF8F9FC) else DarkNavyBg
    val textColor = if (isLightTheme) Color(0xFF1B1B1F) else Color.White
    val textMutedColor = if (isLightTheme) Color(0xFF5B5FC7) else Color.White.copy(alpha = 0.65f)
    val cardBg = if (isLightTheme) Color(0xFFFFFFFF) else GlassWhite
    val borderStrokeColor = if (isLightTheme) Color(0xFFE6E8F0) else GlassWhiteBorder

    val filteredLanguages = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            popularLanguages
        } else {
            popularLanguages.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.nativeName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Background infinite glow transition
    val infiniteTransition = rememberInfiniteTransition(label = "LanguageGlow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "GlowScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .testTag("language_screen_container")
    ) {
        // Aesthetic ambient aura background glow
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-40).dp)
                .size(300.dp)
                .scale(glowScale)
                .blur(90.dp)
                .background(if (isLightTheme) RoyalPurple.copy(alpha = 0.06f) else RoyalPurple.copy(alpha = 0.18f), CircleShape)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-60).dp, y = 80.dp)
                .size(320.dp)
                .scale(glowScale)
                .blur(100.dp)
                .background(if (isLightTheme) RoyalPurpleDark.copy(alpha = 0.05f) else RoyalPurpleDark.copy(alpha = 0.15f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Title
            Text(
                text = stringResource(id = R.string.choose_language),
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            Text(
                text = stringResource(id = R.string.choose_language_sub),
                fontSize = 14.sp,
                color = textMutedColor,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
            )

            // Glassmorphic Search Bar
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(BorderStroke(1.dp, borderStrokeColor), RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .testTag("language_search_input"),
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.search_language),
                        color = textColor.copy(alpha = 0.4f),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = textColor.copy(alpha = 0.5f)
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = if (isLightTheme) RoyalPurple else PremiumGold
                ),
                singleLine = true
            )

            // Section Label
            Text(
                text = stringResource(id = R.string.popular_languages),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = if (isLightTheme) RoyalPurple else PremiumGold.copy(alpha = 0.85f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                textAlign = TextAlign.Start
            )

            // Language Scrollable List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredLanguages) { lang ->
                    val isSelected = selectedLanguageName.equals(lang.name, ignoreCase = true)
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedLanguageName = lang.name }
                            .testTag("language_item_${lang.code}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                if (isLightTheme) Color(0xFFE8E4FF) else Color(0xFF191738)
                            } else {
                                cardBg
                            }
                        ),
                        border = BorderStroke(
                            width = 1.2.dp,
                            color = if (isSelected) {
                                if (isLightTheme) RoyalPurple else PremiumGold
                            } else {
                                borderStrokeColor
                            }
                        ),
                        elevation = if (isLightTheme) CardDefaults.cardElevation(defaultElevation = 1.dp) else CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Flag Emoji
                                Text(
                                    text = lang.flag,
                                    fontSize = 24.sp
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Column {
                                    Text(
                                        text = lang.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isSelected) {
                                            if (isLightTheme) RoyalPurple else PremiumGold
                                        } else {
                                            textColor
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = lang.nativeName,
                                        fontSize = 12.sp,
                                        color = textColor.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(if (isLightTheme) RoyalPurple else PremiumGold, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = if (isLightTheme) Color.White else DarkNavyBg,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Continue CTA Button
            Button(
                onClick = {
                    onLanguageSelected(selectedLanguageName)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .testTag("language_continue_button"),
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
                    Text(
                        text = stringResource(id = R.string.continue_btn),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
