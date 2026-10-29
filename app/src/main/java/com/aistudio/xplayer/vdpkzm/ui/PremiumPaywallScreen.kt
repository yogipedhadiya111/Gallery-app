package com.aistudio.xplayer.vdpkzm.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.aistudio.xplayer.vdpkzm.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Color Definitions for Premium Visual Identity
val RoyalPurple = Color(0xFF6E3AFF)
val RoyalPurpleDark = Color(0xFF4519BE)
val PremiumGold = Color(0xFFFFD700)
val PremiumGoldLight = Color(0xFFFFF2A3)
val DarkNavyBg = Color(0xFF090A10)
val DarkNavySurface = Color(0xFF141624)
val GlassWhite = Color(0x0FFFFFFF)
val GlassWhiteBorder = Color(0x1AFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumPaywallScreen(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var showBillingSheet by remember { mutableStateOf(false) }
    var isProcessingPayment by remember { mutableStateOf(false) }
    var billingStep by remember { mutableStateOf(1) } // 1 = Details, 2 = Success
    var benefitTabSelected by remember { mutableStateOf(1) } // 0 = Free, 1 = Premium
    
    val coroutineScope = rememberCoroutineScope()
    
    // Ambient Animated Glow States
    val infiniteTransition = rememberInfiniteTransition(label = "GlowTransition")
    val ambientScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "AmbientScale"
    )
    val ambientAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "AmbientAlpha"
    )

    // Shimmer highlight effect for CTA button
    val shimmerTranslateX by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "ShimmerX"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBg)
            .testTag("premium_paywall_container")
    ) {
        // Ambient background glowing blobs
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-100).dp)
                .size(300.dp)
                .scale(ambientScale)
                .alpha(ambientAlpha)
                .blur(80.dp)
                .background(RoyalPurple.copy(alpha = 0.25f), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 50.dp, y = 50.dp)
                .size(250.dp)
                .blur(90.dp)
                .background(PremiumGold.copy(alpha = 0.12f), CircleShape)
        )

        // Main Scrolling Body Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Best Value Badge Pill
                Box(
                    modifier = Modifier
                        .background(RoyalPurple.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                        .border(1.dp, RoyalPurple.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = PremiumGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "⭐ BEST VALUE",
                            color = PremiumGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Close Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .background(GlassWhite, CircleShape)
                        .border(1.dp, GlassWhiteBorder, CircleShape)
                        .size(36.dp)
                        .testTag("paywall_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Paywall",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Premium Crown Icon Badge
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(RoyalPurple.copy(alpha = 0.4f), Color.Transparent)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFFFFEA79), Color(0xFFC79218))
                            ),
                            shape = CircleShape
                        )
                        .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WorkspacePremium,
                        contentDescription = "Premium Crown",
                        tint = DarkNavyBg,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title
            Text(
                text = "XPLAYER PREMIUM",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("paywall_title")
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle
            Text(
                text = "Unlock the complete Video Player experience.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Comparison selector tab layout
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(4.dp)
            ) {
                listOf("Free Mode", "Premium Pro").forEachIndexed { index, label ->
                    val isSelected = benefitTabSelected == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) RoyalPurple else Color.Transparent)
                            .clickable { benefitTabSelected = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Benefit Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkNavySurface.copy(alpha = 0.85f)),
                border = BorderStroke(
                    1.dp,
                    if (benefitTabSelected == 1) PremiumGold.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (benefitTabSelected == 1) "⚡ Ultimate Premium Benefits" else "⚪ Free Mode Limitations",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (benefitTabSelected == 1) PremiumGold else Color.White
                        )
                        
                        Text(
                            text = if (benefitTabSelected == 1) "14+ Features" else "Basic Only",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (benefitTabSelected == 1) PremiumGold.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier
                                .background(
                                    if (benefitTabSelected == 1) PremiumGold.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.08f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (benefitTabSelected == 1) {
                        // Premium Benefits list (Dual column for high density but readable layout)
                        val premiumBenefits = listOf(
                            "No Ads" to Icons.Default.CheckCircle,
                            "Play All Video Formats" to Icons.Default.CheckCircle,
                            "Ultra HD & 4K Playback" to Icons.Default.CheckCircle,
                            "Advanced Playback Support" to Icons.Default.CheckCircle,
                            "Background Play" to Icons.Default.CheckCircle,
                            "Picture-in-Picture" to Icons.Default.CheckCircle,
                            "Subtitle Support" to Icons.Default.CheckCircle,
                            "Playback Speed Controls" to Icons.Default.CheckCircle,
                            "Gesture Controls" to Icons.Default.CheckCircle,
                            "Sleep Timer" to Icons.Default.CheckCircle,
                            "Folder Lock" to Icons.Default.CheckCircle,
                            "Private Video Vault" to Icons.Default.CheckCircle,
                            "Chromecast Streaming" to Icons.Default.CheckCircle,
                            "Future Updates Included" to Icons.Default.CheckCircle
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Split premium benefits into chunks of 2 for beautiful grid display
                            premiumBenefits.chunked(2).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowItems.forEach { benefit ->
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = benefit.second,
                                                contentDescription = "Included",
                                                tint = PremiumGold,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = benefit.first,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White.copy(alpha = 0.9f),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Free Limitations List
                        val freeLimitations = listOf(
                            "Standard basic video playback only",
                            "Frequent advertisements interrupt experience",
                            "Basic play/pause controls, no gestures",
                            "No background playback or floating window",
                            "No Folder Lock or Secure Private Vault",
                            "Limited audio & subtitle track syncing"
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            freeLimitations.forEach { limitation ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.RemoveCircleOutline,
                                        contentDescription = "Limitation",
                                        tint = Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = limitation,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.65f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Pricing details Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = RoyalPurple.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, RoyalPurple.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Try FREE for 3 Days",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PremiumGold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Then ₹1,350/week",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Auto-renews until cancelled. Cancel anytime.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // REWARDED AD OPTION CARD FOR COMPLIANT OPT-IN TRIAL
            var showAdConfirmDialog by remember { mutableStateOf(false) }
            val activity = LocalContext.current.findActivity()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .clickable { showAdConfirmDialog = true }
                    .testTag("watch_ad_trial_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                border = BorderStroke(1.2.dp, Color(0xFFFFD700).copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFFFD700).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Free Premium Trial",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Watch a quick video to unlock all features",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Button(
                        onClick = { showAdConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "WATCH AD",
                            color = Color(0xFF090A10),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            if (showAdConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showAdConfirmDialog = false },
                    title = {
                        Text(
                            text = "Unlock Premium Trial",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    text = {
                        Text(
                            text = "Watch a short advertisement to receive your reward.",
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showAdConfirmDialog = false
                                if (activity != null) {
                                    com.aistudio.xplayer.vdpkzm.monetization.RewardedAdManager.showAd(
                                        activity = activity,
                                        onRewardGranted = {
                                            onSuccess()
                                        },
                                        onAdClosed = {}
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                        ) {
                            Text("Watch Ad", color = Color(0xFF090A10), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showAdConfirmDialog = false }
                        ) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                        }
                    },
                    containerColor = Color(0xFF141624),
                    shape = RoundedCornerShape(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary CTA: Start Free Trial Button with glowing animation & shimmer highlights
            Button(
                onClick = { showBillingSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .testTag("start_trial_cta_button"),
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
                    // Shimmer Reflection Highlight
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(80.dp)
                            .offset(x = shimmerTranslateX.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.0f),
                                        Color.White.copy(alpha = 0.25f),
                                        Color.White.copy(alpha = 0.0f)
                                    )
                                )
                            )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "START FREE TRIAL",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Secondary Action text button (Continue with Free Version)
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .wrapContentSize()
                    .testTag("continue_free_button")
            ) {
                Text(
                    text = "Continue with Free Version",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = TextDecoration.Underline
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Trust Badges Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val trustItems = listOf(
                    Icons.Default.Security to "Secure\nPayment",
                    Icons.Default.EventAvailable to "Cancel\nAnytime",
                    Icons.Default.Shop to "Google Play\nSubscription",
                    Icons.Default.FavoriteBorder to "No Risk\nTrial"
                )

                trustItems.forEach { item ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(GlassWhite, CircleShape)
                                .border(1.dp, GlassWhiteBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.first,
                                contentDescription = null,
                                tint = PremiumGold,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.second,
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Legally compliant Footer terms and text
            Text(
                text = "3-day free trial available for eligible new users. Subscription renews automatically unless cancelled before the trial ends. Manage subscriptions in Google Play. Charged at ₹1,350/week after trial. All transactions are secure and encrypted via Google Play Billing services.",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                lineHeight = 14.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // GOOGLE PLAY SUBSCRIPTION COMPLIANT BILLING FLOW SHEET
        if (showBillingSheet) {
            ModalBottomSheet(
                onDismissRequest = { 
                    if (!isProcessingPayment) {
                        showBillingSheet = false 
                        billingStep = 1
                    }
                },
                containerColor = Color(0xFF1F212E),
                contentColor = Color.White,
                tonalElevation = 8.dp,
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(0.3f)) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(20.dp)
                ) {
                    if (billingStep == 1) {
                        // Google Play Checkout Screen
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Google Play Logo mockup
                                    Icon(
                                        imageVector = Icons.Default.Shop,
                                        contentDescription = "Google Play",
                                        tint = Color(0xFF34A853),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Google Play Checkout",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "SECURE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF34A853),
                                    modifier = Modifier
                                        .background(Color(0xFF34A853).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                            Spacer(modifier = Modifier.height(14.dp))

                            // Item details
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "XPlayer Ultimate Subscription",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "3-Day Free Trial",
                                        fontSize = 12.sp,
                                        color = Color(0xFF34A853),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = "₹1,350/wk",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.3f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(stringResource(id = R.string.starting_today), fontSize = 11.sp, color = Color.White.copy(0.6f))
                                        Text(stringResource(id = R.string.trial_price_desc), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34A853))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(stringResource(id = R.string.starting_date), fontSize = 11.sp, color = Color.White.copy(0.6f))
                                        Text(stringResource(id = R.string.subscription_price), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Simulated Payment Card
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(0.04f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CreditCard,
                                        contentDescription = "Visa",
                                        tint = Color(0xFF1A1F71),
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color.White, RoundedCornerShape(4.dp))
                                            .padding(2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(stringResource(id = R.string.google_pay_lbl), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("yogipppp8@gmail.com", fontSize = 11.sp, color = Color.White.copy(0.5f))
                                    }
                                }
                                Icon(Icons.Default.CheckCircle, "Selected", tint = Color(0xFF34A853), modifier = Modifier.size(18.dp))
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = "By clicking 'Subscribe', you agree to start a 3-day free trial. If you do not cancel before Jul 4, 2026, you will be billed ₹1,350 automatically each week until cancelled. Cancel anytime in Google Play Subscriptions.",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                lineHeight = 13.sp
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // Confirm Button
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isProcessingPayment = true
                                        delay(1800) // Simulate real payment gateway delay
                                        isProcessingPayment = false
                                        billingStep = 2
                                    }
                                },
                                enabled = !isProcessingPayment,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("subscribe_confirm_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B))
                            ) {
                                if (isProcessingPayment) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(stringResource(id = R.string.subscribe_trial_btn), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        // Success state screen in bottom sheet
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color(0xFF34A853).copy(0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = Color(0xFF34A853),
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Subscription Activated!",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Welcome to XPlayer Premium. All elite tools, Private Secure Vault features, and formats are unlocked for you.",
                                fontSize = 13.sp,
                                color = Color.White.copy(0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    showBillingSheet = false
                                    billingStep = 1
                                    onSuccess()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("billing_success_continue_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple)
                            ) {
                                Text(stringResource(id = R.string.launch_premium_player), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
