package com.aistudio.xplayer.vdpkzm.monetization

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.aistudio.xplayer.vdpkzm.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.analytics.FirebaseAnalytics
import java.util.Date

object InterstitialAdManager : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private const val TAG = "InterstitialAdManager"

    // Production unit ID from prompt
    private const val PROD_AD_UNIT_ID = "ca-app-pub-2878814018092038/8873984822"

    // Configuration/Bypass states (Synchronized with AppOpenAdManager logic)
    var isSplashActive: Boolean = true
    var isLanguageActive: Boolean = false
    var isPaywallActive: Boolean = false
    var isOnboardingActive: Boolean = false
    var isVideoPlaying: Boolean = false
    var isPremiumUser: Boolean = false
    var isVaultUnlockActive: Boolean = false

    // Ad structures
    private var interstitialAd: InterstitialAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    
    // Throttle / Frequency Control
    private var lastShowTime: Long = 0
    private const val MIN_SHOW_INTERVAL_MS = 180_000 // 3 minutes
    private var transitionCount = 0
    private const val TRANSITION_THRESHOLD = 4

    private var currentActivity: Activity? = null
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    fun initialize(application: Application) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(application)
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // Preload our first Interstitial
        loadAd(application)
    }

    private fun logEvent(eventName: String, params: Bundle? = null) {
        try {
            firebaseAnalytics.logEvent(eventName, params)
            Log.d(TAG, "Logged event to Firebase: $eventName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log event to Firebase: ${e.message}")
        }
    }

    /** Preloads/Caches the interstitial ad */
    fun loadAd(application: Application) {
        if (isLoadingAd || interstitialAd != null) {
            return
        }

        isLoadingAd = true
        val adUnitId = PROD_AD_UNIT_ID
        Log.d(TAG, "Loading Interstitial Ad with Unit ID: $adUnitId")

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            application,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoadingAd = false
                    Log.d(TAG, "Interstitial Ad loaded successfully.")
                    logEvent("ad_loaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    isLoadingAd = false
                    Log.e(TAG, "Interstitial Ad failed to load: ${loadAdError.message}")
                    val params = Bundle().apply {
                        putInt("error_code", loadAdError.code)
                        putString("error_message", loadAdError.message)
                    }
                    logEvent("ad_failed", params)
                }
            }
        )
    }

    /** Increment transition and show if threshold / interval met */
    fun onScreenTransition(activity: Activity) {
        transitionCount++
        Log.d(TAG, "Screen Transition Registered. Count: $transitionCount")
        
        if (transitionCount >= TRANSITION_THRESHOLD) {
            Log.d(TAG, "Transition count threshold reached ($TRANSITION_THRESHOLD). Attempting to show ad.")
            val shown = showAdIfAvailable(activity)
            if (shown) {
                transitionCount = 0
            }
        }
    }

    /** Reset transition count (e.g., if needed by app flow) */
    fun resetTransitionCount() {
        transitionCount = 0
    }

    /** Show Interstitial Ad if available, not throttled, and all rules pass */
    fun showAdIfAvailable(activity: Activity): Boolean {
        if (isShowingAd) {
            Log.d(TAG, "Interstitial is already showing.")
            return false
        }

        // Policy / UX Bypasses
        if (isPremiumUser) {
            Log.d(TAG, "Bypassed ad: User is premium.")
            return false
        }

        if (isSplashActive || isLanguageActive || isPaywallActive || isOnboardingActive || isVideoPlaying || isVaultUnlockActive) {
            Log.d(TAG, "Bypassed ad: Critical/sensitive UI active. " +
                    "Splash=$isSplashActive, Language=$isLanguageActive, Paywall=$isPaywallActive, Onboarding=$isOnboardingActive, Video=$isVideoPlaying, Vault=$isVaultUnlockActive")
            return false
        }

        // Frequency Cap
        val currentTime = Date().time
        if (currentTime - lastShowTime < MIN_SHOW_INTERVAL_MS) {
            Log.d(TAG, "Bypassed ad: Throttled by frequency cap. Wait ${((MIN_SHOW_INTERVAL_MS - (currentTime - lastShowTime)) / 1000)}s more.")
            return false
        }

        val ad = interstitialAd
        if (ad == null) {
            Log.d(TAG, "Bypassed ad: No loaded interstitial available. Requesting one now.")
            loadAd(activity.application)
            return false
        }

        // Show policy compliance confirmation dialog before display
        activity.runOnUiThread {
            android.app.AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Advertisement")
                .setMessage("The next action may show an advertisement.\n\nWatching ads helps support XPlayer Pro.\n\nContinue?")
                .setPositiveButton("Continue") { dialog, _ ->
                    dialog.dismiss()
                    showAdNow(activity, ad)
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(false)
                .show()
        }
        return true
    }

    private fun showAdNow(activity: Activity, ad: InterstitialAd) {
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                isShowingAd = false
                Log.d(TAG, "Interstitial dismissed.")
                logEvent("ad_closed")
                loadAd(activity.application)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                isShowingAd = false
                Log.e(TAG, "Interstitial failed to show: ${adError.message}")
                val params = Bundle().apply {
                    putInt("error_code", adError.code)
                    putString("error_message", adError.message)
                }
                logEvent("ad_failed", params)
                loadAd(activity.application)
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
                lastShowTime = Date().time
                Log.d(TAG, "Interstitial showed successfully.")
                logEvent("ad_opened")
                logEvent("interstitial_impression")
            }

            override fun onAdClicked() {
                Log.d(TAG, "Interstitial clicked.")
                logEvent("ad_clicked")
            }
        }

        ad.show(activity)
    }

    // ActivityLifecycleCallbacks implementations
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }
    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }
}
