@file:Suppress("DEPRECATION")
package com.aistudio.xplayer.vdpkzm

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.aistudio.xplayer.vdpkzm.data.Video
import com.aistudio.xplayer.vdpkzm.ui.DashboardScreen
import com.aistudio.xplayer.vdpkzm.ui.MainViewModel
import com.aistudio.xplayer.vdpkzm.ui.MainViewModelFactory
import com.aistudio.xplayer.vdpkzm.ui.VideoPlayerScreen
import com.aistudio.xplayer.vdpkzm.ui.SplashScreen
import com.aistudio.xplayer.vdpkzm.ui.PremiumPaywallScreen
import com.aistudio.xplayer.vdpkzm.ui.OnboardingScreen
import com.aistudio.xplayer.vdpkzm.ui.LanguageSelectionScreen
import com.aistudio.xplayer.vdpkzm.ui.ProvideLocalizedResources
import com.aistudio.xplayer.vdpkzm.ui.mapLanguageNameToCode
import com.aistudio.xplayer.vdpkzm.ui.SecureScreen
import com.aistudio.xplayer.vdpkzm.ui.SecureScreenManager
import com.aistudio.xplayer.vdpkzm.ui.theme.XPlayerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as XPlayerApplication
        MainViewModelFactory(app, app.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request runtime permissions gracefully on start
        requestPermissionsIfNeeded()

        setContent {
            val originalContext = androidx.compose.ui.platform.LocalContext.current
            val attributionContext = remember(originalContext) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    originalContext.createAttributionContext("google")
                } else {
                    originalContext
                }
            }
            CompositionLocalProvider(androidx.compose.ui.platform.LocalContext provides attributionContext) {
                val themeMode by viewModel.themeState.collectAsState()
                val isPremium by viewModel.isPremium.collectAsState()
            val selectedLanguage by viewModel.selectedLanguage.collectAsState()

            val protectScreenshots by viewModel.protectScreenshots.collectAsState()
            val preventScreenRecording by viewModel.preventScreenRecording.collectAsState()
            val hideRecentApps by viewModel.hideRecentApps.collectAsState()

            // Reactively sync preferences to the window security manager
            LaunchedEffect(protectScreenshots, preventScreenRecording, hideRecentApps) {
                SecureScreenManager.updateSettings(
                    activity = this@MainActivity,
                    protectScreenshots = protectScreenshots,
                    preventScreenRecording = preventScreenRecording,
                    hideRecentApps = hideRecentApps
                )
            }

            var showSplash by remember { mutableStateOf(true) }
            var showLanguage by remember { mutableStateOf(false) }
            var showPaywall by remember { mutableStateOf(false) }
            var showOnboarding by remember { mutableStateOf(false) }
            var playingVideo by remember { mutableStateOf<Video?>(null) }

            // Sync state flags with AppOpenAdManager and InterstitialAdManager
            LaunchedEffect(showSplash, showLanguage, showPaywall, showOnboarding, playingVideo, isPremium) {
                com.aistudio.xplayer.vdpkzm.monetization.AppOpenAdManager.isSplashActive = showSplash
                com.aistudio.xplayer.vdpkzm.monetization.AppOpenAdManager.isLanguageActive = showLanguage
                com.aistudio.xplayer.vdpkzm.monetization.AppOpenAdManager.isPaywallActive = showPaywall
                com.aistudio.xplayer.vdpkzm.monetization.AppOpenAdManager.isOnboardingActive = showOnboarding
                com.aistudio.xplayer.vdpkzm.monetization.AppOpenAdManager.isVideoPlaying = (playingVideo != null)
                com.aistudio.xplayer.vdpkzm.monetization.AppOpenAdManager.isPremiumUser = isPremium

                com.aistudio.xplayer.vdpkzm.monetization.InterstitialAdManager.isSplashActive = showSplash
                com.aistudio.xplayer.vdpkzm.monetization.InterstitialAdManager.isLanguageActive = showLanguage
                com.aistudio.xplayer.vdpkzm.monetization.InterstitialAdManager.isPaywallActive = showPaywall
                com.aistudio.xplayer.vdpkzm.monetization.InterstitialAdManager.isOnboardingActive = showOnboarding
                com.aistudio.xplayer.vdpkzm.monetization.InterstitialAdManager.isVideoPlaying = (playingVideo != null)
                com.aistudio.xplayer.vdpkzm.monetization.InterstitialAdManager.isPremiumUser = isPremium
            }

            XPlayerTheme(modeValue = themeMode) {
                ProvideLocalizedResources(localeCode = mapLanguageNameToCode(selectedLanguage)) {
                    SecureScreen(viewModel = viewModel)
                    Surface(modifier = Modifier.fillMaxSize()) {
                        if (showSplash) {
                            SplashScreen(
                                onSplashFinished = {
                                    showSplash = false
                                    if (!viewModel.hasSelectedLanguage()) {
                                        showLanguage = true
                                    } else if (!isPremium && !viewModel.hasShownPaywall()) {
                                        showPaywall = true
                                        viewModel.setHasShownPaywall(true)
                                    } else if (!viewModel.hasCompletedOnboarding()) {
                                        showOnboarding = true
                                    }
                                }
                            )
                        } else if (showLanguage) {
                            LanguageSelectionScreen(
                                currentSelectedLanguage = selectedLanguage,
                                onLanguageSelected = { lang ->
                                    viewModel.selectLanguage(lang)
                                    showLanguage = false
                                    if (!isPremium && !viewModel.hasShownPaywall()) {
                                        showPaywall = true
                                        viewModel.setHasShownPaywall(true)
                                    } else if (!viewModel.hasCompletedOnboarding()) {
                                        showOnboarding = true
                                    }
                                }
                            )
                        } else if (showPaywall) {
                            SecureScreen(isSensitive = true, viewModel = viewModel)
                            PremiumPaywallScreen(
                                onDismiss = {
                                    showPaywall = false
                                    if (!viewModel.hasCompletedOnboarding()) {
                                        showOnboarding = true
                                    }
                                },
                                onSuccess = {
                                    viewModel.startPremiumTrial()
                                    showPaywall = false
                                    if (!viewModel.hasCompletedOnboarding()) {
                                        showOnboarding = true
                                    }
                                }
                            )
                        } else if (showOnboarding) {
                            OnboardingScreen(
                                onFinished = {
                                    viewModel.setCompletedOnboarding(true)
                                    showOnboarding = false
                                }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (playingVideo != null) {
                                    SecureScreen(isSensitive = true, viewModel = viewModel)
                                    // Full-bleed ExoPlayer screen
                                    VideoPlayerScreen(
                                        video = playingVideo!!,
                                        onBack = {
                                            playingVideo = null
                                            com.aistudio.xplayer.vdpkzm.monetization.InterstitialAdManager.showAdIfAvailable(this@MainActivity)
                                        }
                                    )
                                } else {
                                    // Bottom Nav Dashboard Screen
                                    DashboardScreen(
                                        viewModel = viewModel,
                                        onPlayVideo = { clicked ->
                                            playingVideo = clicked
                                        },
                                        onLaunchPaywall = {
                                            showPaywall = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        val requiredPerms = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_VIDEO) 
                != PackageManager.PERMISSION_GRANTED) {
                requiredPerms.add(android.Manifest.permission.READ_MEDIA_VIDEO)
            }
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                requiredPerms.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                requiredPerms.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (requiredPerms.isNotEmpty()) {
            requestPermissions(requiredPerms.toTypedArray(), 101)
        }
    }

    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            val videoPermissionIdx = permissions.indexOfFirst {
                it == android.Manifest.permission.READ_MEDIA_VIDEO || 
                it == android.Manifest.permission.READ_EXTERNAL_STORAGE
            }
            val videoPermissionGranted = if (videoPermissionIdx != -1) {
                grantResults.getOrNull(videoPermissionIdx) == PackageManager.PERMISSION_GRANTED
            } else {
                grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
            }

            if (videoPermissionGranted) {
                viewModel.scanVideos()
                Toast.makeText(this, "Storage Access Granted! Indexing local video library...", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Permission denied. Please grant storage access to scan local media files.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        SecureScreenManager.onActivityResumed(this)
    }

    override fun onPause() {
        super.onPause()
        SecureScreenManager.onActivityPaused(this)
    }
}
