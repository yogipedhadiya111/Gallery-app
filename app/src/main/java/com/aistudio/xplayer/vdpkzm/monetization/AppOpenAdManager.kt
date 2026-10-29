package com.aistudio.xplayer.vdpkzm.monetization

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.firebase.analytics.FirebaseAnalytics
import java.util.Date

object AppOpenAdManager : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private const val TAG = "AppOpenAdManager"
    
    // Production unit ID from prompt
    private const val PROD_AD_UNIT_ID = "ca-app-pub-2878814018092038/1406558344"

    // Configuration flags
    var isSplashActive: Boolean = true
    var isLanguageActive: Boolean = false
    var isPaywallActive: Boolean = false
    var isOnboardingActive: Boolean = false
    var isVideoPlaying: Boolean = false
    var isPremiumUser: Boolean = false

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var isColdStartShown = false
    private var loadTime: Long = 0

    private var currentActivity: Activity? = null
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    fun initialize(application: Application) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(application)
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        
        // Preload the first ad
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

    /** Request an ad */
    fun loadAd(application: Application) {
        if (isLoadingAd || isAdAvailable()) {
            return
        }

        isLoadingAd = true
        val adUnitId = PROD_AD_UNIT_ID
        Log.d(TAG, "Loading App Open Ad with Unit ID: $adUnitId")

        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            application,
            adUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                    Log.d(TAG, "App Open Ad loaded successfully.")
                    logEvent("ad_loaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                    Log.e(TAG, "App Open Ad failed to load: ${loadAdError.message}")
                    val params = Bundle().apply {
                        putInt("error_code", loadAdError.code)
                        putString("error_message", loadAdError.message)
                    }
                    logEvent("ad_failed", params)
                }
            }
        )
    }

    /** Check if ad exists and has not expired */
    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    /** Utility method to check if ad has expired */
    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < (numMilliSecondsPerHour * numHours)
    }

    /** Show the ad if available */
    fun showAdIfAvailable(activity: Activity) {
        // Only show once on cold start
        if (isColdStartShown) {
            Log.d(TAG, "Bypassed ad: Already shown on Cold Start once per session.")
            return
        }

        // If the app open ad is already showing, do not show again
        if (isShowingAd) {
            Log.d(TAG, "Ad is already showing.")
            return
        }

        // Check our bypass states - user context safety
        if (isPremiumUser) {
            Log.d(TAG, "User is premium, ads bypassed.")
            return
        }
        if (isSplashActive || isLanguageActive || isPaywallActive || isOnboardingActive || isVideoPlaying) {
            Log.d(TAG, "Ads bypassed due to active foreground restriction: " +
                    "Splash=$isSplashActive, Language=$isLanguageActive, Paywall=$isPaywallActive, Onboarding=$isOnboardingActive, VideoPlaying=$isVideoPlaying")
            return
        }

        if (!isAdAvailable()) {
            Log.d(TAG, "Ad is not available. Loading a new one...")
            val params = Bundle().apply { putString("reason", "ad_not_available") }
            logEvent("ad_failed", params)
            loadAd(activity.application)
            return
        }

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                // Set the reference to null so isAdAvailable() returns false.
                appOpenAd = null
                isShowingAd = false
                isColdStartShown = true
                Log.d(TAG, "Ad dismissed full screen content.")
                logEvent("ad_closed")
                loadAd(activity.application)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAd = false
                Log.e(TAG, "Ad failed to show: ${adError.message}")
                val params = Bundle().apply {
                    putInt("error_code", adError.code)
                    putString("error_message", adError.message)
                }
                logEvent("ad_failed", params)
                loadAd(activity.application)
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
                isColdStartShown = true
                Log.d(TAG, "Ad showed full screen content.")
                logEvent("ad_opened")
            }

            override fun onAdClicked() {
                Log.d(TAG, "Ad clicked.")
                logEvent("ad_clicked")
            }
        }

        appOpenAd?.show(activity)
    }

    // DefaultLifecycleObserver method
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (isColdStartShown) {
            Log.d(TAG, "Bypassed ad: Already shown on Cold Start once per session.")
            return
        }
        Log.d(TAG, "App moved to foreground, showing ad if available.")
        currentActivity?.let {
            showAdIfAvailable(it)
        }
    }

    // ActivityLifecycleCallbacks methods
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
