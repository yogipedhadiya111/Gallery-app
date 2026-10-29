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
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.firebase.analytics.FirebaseAnalytics

object RewardedAdManager : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private const val TAG = "RewardedAdManager"

    // Production unit ID from prompt
    private const val PROD_AD_UNIT_ID = "ca-app-pub-2878814018092038/8678543701"

    private var rewardedAd: RewardedAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false

    private var currentActivity: Activity? = null
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    fun initialize(application: Application) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(application)
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // Preload our first Rewarded Ad
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

    /** Preloads/Caches the rewarded ad */
    fun loadAd(application: Application) {
        if (isLoadingAd || rewardedAd != null) {
            return
        }

        isLoadingAd = true
        val adUnitId = PROD_AD_UNIT_ID
        Log.d(TAG, "Loading Rewarded Ad with Unit ID: $adUnitId")

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            application,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoadingAd = false
                    Log.d(TAG, "Rewarded Ad loaded successfully.")
                    logEvent("ad_loaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedAd = null
                    isLoadingAd = false
                    Log.e(TAG, "Rewarded Ad failed to load: ${loadAdError.message}")
                    val params = Bundle().apply {
                        putInt("error_code", loadAdError.code)
                        putString("error_message", loadAdError.message)
                    }
                    logEvent("ad_failed", params)
                }
            }
        )
    }

    fun isAdAvailable(): Boolean {
        return rewardedAd != null
    }

    /**
     * Show the rewarded ad.
     * @param activity The current activity hosting the ad.
     * @param onRewardGranted Callback executed immediately when the user finishes watching the ad and receives the reward.
     * @param onAdClosed Callback executed when the ad is dismissed/closed (whether completed or skipped).
     */
    fun showAd(activity: Activity, onRewardGranted: () -> Unit, onAdClosed: () -> Unit) {
        if (isShowingAd) {
            Log.d(TAG, "Rewarded ad is already showing.")
            return
        }

        val ad = rewardedAd
        if (ad == null) {
            Log.d(TAG, "No cached Rewarded Ad available. Fetching a new one.")
            loadAd(activity.application)
            onAdClosed()
            return
        }

        // Show policy compliance confirmation dialog before display
        activity.runOnUiThread {
            android.app.AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Advertisement")
                .setMessage("The next action may show an advertisement.\n\nWatching ads helps support XPlayer Pro.\n\nContinue?")
                .setPositiveButton("Continue") { dialog, _ ->
                    dialog.dismiss()
                    showAdNow(activity, ad, onRewardGranted, onAdClosed)
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                    onAdClosed()
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun showAdNow(activity: Activity, ad: RewardedAd, onRewardGranted: () -> Unit, onAdClosed: () -> Unit) {
        isShowingAd = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                isShowingAd = false
                Log.d(TAG, "Rewarded ad dismissed.")
                logEvent("ad_closed")
                loadAd(activity.application)
                onAdClosed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                isShowingAd = false
                Log.e(TAG, "Rewarded ad failed to show: ${adError.message}")
                val params = Bundle().apply {
                    putInt("error_code", adError.code)
                    putString("error_message", adError.message)
                }
                logEvent("ad_failed", params)
                loadAd(activity.application)
                onAdClosed()
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
                Log.d(TAG, "Rewarded ad displayed.")
                logEvent("ad_opened")
                logEvent("rewarded_impression")
            }

            override fun onAdClicked() {
                Log.d(TAG, "Rewarded ad clicked.")
                logEvent("ad_clicked")
            }
        }

        ad.show(activity) { rewardItem ->
            Log.d(TAG, "User completed watching the ad. Reward item: ${rewardItem.type}, Amount: ${rewardItem.amount}")
            logEvent("reward_earned")
            onRewardGranted()
        }
    }

    // ActivityLifecycleCallbacks implementation
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
