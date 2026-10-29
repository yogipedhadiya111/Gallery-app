package com.aistudio.xplayer.vdpkzm

import android.app.Application
import android.util.Log
import com.aistudio.xplayer.vdpkzm.data.AppDatabase
import com.aistudio.xplayer.vdpkzm.data.AudioEffectsManager
import com.aistudio.xplayer.vdpkzm.data.VideoRepository
import com.aistudio.xplayer.vdpkzm.monetization.AppOpenAdManager
import com.aistudio.xplayer.vdpkzm.monetization.InterstitialAdManager
import com.aistudio.xplayer.vdpkzm.monetization.RewardedAdManager
import com.google.android.gms.ads.MobileAds

class XPlayerApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: VideoRepository
        private set

    lateinit var audioEffectsManager: AudioEffectsManager
        private set

    override fun onCreate() {
        super.onCreate()
        
        // Centralized Google Mobile Ads SDK initialization (runs exactly once)
        MobileAds.initialize(this) { status ->
            Log.d("XPlayerApplication", "Google Mobile Ads SDK Initialized successfully: ${status.adapterStatusMap.keys}")
        }

        database = AppDatabase.getDatabase(this)
        repository = VideoRepository(this, database.videoDao())
        audioEffectsManager = AudioEffectsManager(this)

        // Initialize App Open Ads System
        AppOpenAdManager.initialize(this)
        
        // Initialize Interstitial Ads System
        InterstitialAdManager.initialize(this)

        // Initialize Rewarded Ads System
        RewardedAdManager.initialize(this)
    }
}
