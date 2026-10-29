package com.aistudio.xplayer.vdpkzm.monetization

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.firebase.analytics.FirebaseAnalytics

@Composable
fun NativeAdCard(
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }

    val firebaseAnalytics = remember { FirebaseAnalytics.getInstance(context) }

    fun logEvent(eventName: String, params: Bundle? = null) {
        try {
            firebaseAnalytics.logEvent(eventName, params)
            Log.d("NativeAdCard", "Firebase Logged event: $eventName, params: $params")
        } catch (e: Exception) {
            Log.e("NativeAdCard", "Failed to log event: ${e.message}", e)
        }
    }

    DisposableEffect(adUnitId) {
        isLoading = true
        loadFailed = false
        Log.d("NativeAdCard", "Loading Native Ad from Unit ID: $adUnitId")
        
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad ->
                nativeAd = ad
                isLoading = false
                Log.d("NativeAdCard", "Native ad loaded successfully from: $adUnitId")
                
                val params = Bundle().apply {
                    putString("ad_type", "native")
                    putString("ad_unit_id", adUnitId)
                }
                logEvent("ad_loaded", params)
            }
            .withAdListener(object : AdListener() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    Log.d("NativeAdCard", "AdListener: onAdLoaded called")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    super.onAdFailedToLoad(error)
                    isLoading = false
                    loadFailed = true
                    Log.e("NativeAdCard", "AdListener: Native ad failed to load: Code ${error.code}, Message: ${error.message}", Exception(error.message))
                    
                    val params = Bundle().apply {
                        putString("ad_type", "native")
                        putString("ad_unit_id", adUnitId)
                        putInt("error_code", error.code)
                        putString("error_message", error.message)
                    }
                    logEvent("ad_failed", params)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    Log.d("NativeAdCard", "AdListener: onAdClicked called")
                    val params = Bundle().apply {
                        putString("ad_type", "native")
                        putString("ad_unit_id", adUnitId)
                    }
                    logEvent("ad_clicked", params)
                }

                override fun onAdOpened() {
                    super.onAdOpened()
                    Log.d("NativeAdCard", "AdListener: onAdOpened called")
                    val params = Bundle().apply {
                        putString("ad_type", "native")
                        putString("ad_unit_id", adUnitId)
                    }
                    logEvent("ad_opened", params)
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    Log.d("NativeAdCard", "AdListener: onAdImpression recorded")
                    val params = Bundle().apply {
                        putString("ad_type", "native")
                        putString("ad_unit_id", adUnitId)
                    }
                    logEvent("ad_impression", params)
                }

                override fun onAdClosed() {
                    super.onAdClosed()
                    Log.d("NativeAdCard", "AdListener: onAdClosed called")
                    val params = Bundle().apply {
                        putString("ad_type", "native")
                        putString("ad_unit_id", adUnitId)
                    }
                    logEvent("ad_closed", params)
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())

        onDispose {
            Log.d("NativeAdCard", "Disposing Native Ad")
            nativeAd?.destroy()
        }
    }

    if (isLoading) {
        // Render a visually clean Material 3 card skeleton while loading
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(150.dp)
        )
    } else if (loadFailed || nativeAd == null) {
        Spacer(modifier = Modifier.size(0.dp))
    } else {
        val currentAd = nativeAd!!
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { ctx ->
                // NativeAdView MUST be the root container
                val nativeAdView = NativeAdView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }

                // Main card layout container styled as an elegant Material 3 card
                val cardLayout = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 8, 0, 8)
                    }
                    setPadding(16, 16, 16, 16)
                    
                    // Custom Dark Themed Background with Rounded Corners
                    val drawable = android.graphics.drawable.GradientDrawable().apply {
                        setColor(Color.parseColor("#1C1A2E")) // Slate-colored dark theme card
                        cornerRadius = 24f
                    }
                    background = drawable
                }

                // Row for Ad badge + Headline
                val headerRow = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                // Ad Badge (clearly attribute ad under Google Play Policy)
                val adBadge = TextView(ctx).apply {
                    text = "Ad"
                    setTextColor(Color.parseColor("#FFD700")) // Gold color matching app's premium visual design
                    textSize = 11f
                    setTypeface(null, Typeface.BOLD)
                    setPadding(12, 4, 12, 4)
                    val badgeDrawable = android.graphics.drawable.GradientDrawable().apply {
                        setStroke(2, Color.parseColor("#FFD700"))
                        cornerRadius = 8f
                    }
                    background = badgeDrawable
                }

                // Title Headline View
                val headlineView = TextView(ctx).apply {
                    text = currentAd.headline
                    setTextColor(Color.WHITE)
                    textSize = 15f
                    setTypeface(null, Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {
                        leftMargin = 16
                    }
                }

                headerRow.addView(adBadge)
                headerRow.addView(headlineView)
                cardLayout.addView(headerRow)

                // Optional Media View (renders video or big image asset if available)
                val mediaContainer = FrameLayout(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        380
                    ).apply {
                        topMargin = 16
                        bottomMargin = 16
                    }
                }
                val mediaView = MediaView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                }
                mediaContainer.addView(mediaView)
                cardLayout.addView(mediaContainer)
                
                if (currentAd.mediaContent != null) {
                    mediaView.setMediaContent(currentAd.mediaContent!!)
                    mediaContainer.visibility = View.VISIBLE
                } else {
                    mediaContainer.visibility = View.GONE
                }

                // Body text view
                val bodyView = TextView(ctx).apply {
                    text = currentAd.body
                    setTextColor(Color.parseColor("#B0B0C0"))
                    textSize = 13f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = 8
                        bottomMargin = 16
                    }
                }
                cardLayout.addView(bodyView)
                if (!currentAd.body.isNullOrEmpty()) {
                    bodyView.visibility = View.VISIBLE
                } else {
                    bodyView.visibility = View.GONE
                }

                // Row for Icon, Advertiser/Rating, and CTA
                val footerRow = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                // Optional App Icon view
                val iconView = ImageView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(80, 80).apply {
                        rightMargin = 16
                    }
                }
                footerRow.addView(iconView)
                if (currentAd.icon != null) {
                    iconView.setImageDrawable(currentAd.icon?.drawable)
                    iconView.visibility = View.VISIBLE
                } else {
                    iconView.visibility = View.GONE
                }

                // Advertiser, store, or rating text
                val advertiserView = TextView(ctx).apply {
                    val starRating = currentAd.starRating
                    val advertiserText = currentAd.advertiser ?: currentAd.store ?: ""
                    text = if (starRating != null && starRating > 0) {
                        "$advertiserText (★ $starRating)"
                    } else {
                        advertiserText
                    }
                    setTextColor(Color.parseColor("#808090"))
                    textSize = 12f
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                }
                footerRow.addView(advertiserView)
                if (!currentAd.advertiser.isNullOrEmpty() || !currentAd.store.isNullOrEmpty() || (currentAd.starRating != null && currentAd.starRating!! > 0)) {
                    advertiserView.visibility = View.VISIBLE
                } else {
                    advertiserView.visibility = View.GONE
                }

                // Dynamic Call To Action (CTA) Button
                val callToActionView = Button(ctx).apply {
                    text = currentAd.callToAction ?: "Install"
                    setTextColor(Color.parseColor("#0F0A1C"))
                    textSize = 13f
                    setTypeface(null, Typeface.BOLD)
                    setAllCaps(false)
                    val btnDrawable = android.graphics.drawable.GradientDrawable().apply {
                        setColor(Color.parseColor("#FFD700")) // Gold color primary CTA button
                        cornerRadius = 24f
                    }
                    background = btnDrawable
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        leftMargin = 16
                    }
                }
                footerRow.addView(callToActionView)
                if (!currentAd.callToAction.isNullOrEmpty()) {
                    callToActionView.visibility = View.VISIBLE
                } else {
                    callToActionView.visibility = View.GONE
                }

                cardLayout.addView(footerRow)
                nativeAdView.addView(cardLayout)

                // CRITICAL REQUIREMENT under AdMob: register each component with nativeAdView for clicks & impressions tracking!
                nativeAdView.headlineView = headlineView
                nativeAdView.mediaView = mediaView
                nativeAdView.bodyView = bodyView
                nativeAdView.iconView = iconView
                nativeAdView.advertiserView = advertiserView
                nativeAdView.callToActionView = callToActionView

                // Bind the loaded native ad
                nativeAdView.setNativeAd(currentAd)

                nativeAdView
            },
            update = { view ->
                view.setNativeAd(currentAd)
            }
        )
    }
}
