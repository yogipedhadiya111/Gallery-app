package com.aistudio.xplayer.vdpkzm.monetization

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aistudio.xplayer.vdpkzm.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.firebase.analytics.FirebaseAnalytics

@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String = "ca-app-pub-2878814018092038/6658849637"
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    
    // Track if ad was loaded or failed
    var isLoaded by remember { mutableStateOf(false) }
    var adHeight by remember { mutableStateOf(50) } // Default banner height fallback

    // Calculate adaptive banner size
    val adSize = remember(configuration.screenWidthDp) {
        val width = configuration.screenWidthDp
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, width)
    }

    // Set ad height dynamically based on the adaptive size
    LaunchedEffect(adSize) {
        adHeight = adSize.getHeightInPixels(context).let { pixels ->
            val density = context.resources.displayMetrics.density
            if (density > 0) (pixels / density).toInt() else 50
        }
    }

    val firebaseAnalytics = remember { FirebaseAnalytics.getInstance(context) }

    fun logEvent(eventName: String, params: Bundle? = null) {
        try {
            firebaseAnalytics.logEvent(eventName, params)
            Log.d("BannerAdView", "Logged event: $eventName")
        } catch (e: Exception) {
            Log.e("BannerAdView", "Failed to log event: ${e.message}")
        }
    }

    // Create the AdView safely inside remember to avoid recreation on recompositions unless adSize or adUnitId changes
    val adView = remember(adSize, adUnitId) {
        Log.d("BannerAdView", "Creating new AdView instance for Ad Unit: $adUnitId")
        AdView(context).apply {
            this.adUnitId = adUnitId
            setAdSize(adSize)
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    isLoaded = true
                    Log.i("BannerAdView", "Banner Ad Loaded successfully. AdUnitId: $adUnitId, Width: ${adSize.width}, Height: ${adSize.height}")
                    val params = Bundle().apply {
                        putString("ad_unit_id", adUnitId)
                        putString("ad_type", "banner")
                        putString("ad_size", adSize.toString())
                    }
                    logEvent("banner_ad_loaded", params)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    Log.e("BannerAdView", "Banner Ad Failed to load. AdUnitId: $adUnitId, Reason: ${loadAdError.message}, Error Code: ${loadAdError.code}, ResponseInfo: ${loadAdError.responseInfo}")
                    val params = Bundle().apply {
                        putString("ad_unit_id", adUnitId)
                        putString("ad_type", "banner")
                        putInt("error_code", loadAdError.code)
                        putString("error_message", loadAdError.message)
                    }
                    logEvent("banner_ad_failed", params)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    Log.i("BannerAdView", "Banner Ad Clicked. AdUnitId: $adUnitId")
                    val params = Bundle().apply {
                        putString("ad_unit_id", adUnitId)
                        putString("ad_type", "banner")
                    }
                    logEvent("banner_ad_clicked", params)
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    Log.i("BannerAdView", "Banner Ad Impression registered. AdUnitId: $adUnitId")
                    val params = Bundle().apply {
                        putString("ad_unit_id", adUnitId)
                        putString("ad_type", "banner")
                    }
                    logEvent("banner_ad_impression", params)
                }

                override fun onAdOpened() {
                    super.onAdOpened()
                    Log.i("BannerAdView", "Banner Ad Opened. AdUnitId: $adUnitId")
                    val params = Bundle().apply {
                        putString("ad_unit_id", adUnitId)
                        putString("ad_type", "banner")
                    }
                    logEvent("banner_ad_opened", params)
                }

                override fun onAdClosed() {
                    super.onAdClosed()
                    Log.i("BannerAdView", "Banner Ad Closed. AdUnitId: $adUnitId")
                    val params = Bundle().apply {
                        putString("ad_unit_id", adUnitId)
                        putString("ad_type", "banner")
                    }
                    logEvent("banner_ad_closed", params)
                }
            }
        }
    }

    // Keep AdView in sync with the activity/lifecycle states
    DisposableEffect(lifecycleOwner, adView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    Log.d("BannerAdView", "Resuming banner ad.")
                    adView.resume()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    Log.d("BannerAdView", "Pausing banner ad.")
                    adView.pause()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    Log.d("BannerAdView", "Destroying banner ad in ON_DESTROY.")
                    adView.destroy()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        // Load the ad once
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.destroy()
            Log.d("BannerAdView", "Disposed and destroyed banner ad from composition.")
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(adHeight.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                    // Safely handle parent attachment
                    (adView.parent as? android.view.ViewGroup)?.removeView(adView)
                    addView(adView)
                }
            },
            update = {
                // AdView handles updates internally
            }
        )
    }
}
