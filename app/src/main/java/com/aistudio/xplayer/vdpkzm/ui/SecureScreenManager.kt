package com.aistudio.xplayer.vdpkzm.ui

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Enterprise-grade screen security manager for XPlayer Pro.
 * Provides APIs to protect sensitive screens against screenshots, screen recordings,
 * recent app preview leaks, and external screen casting.
 *
 * ### Android Platform Security Boundaries & Limitations:
 * 1. **Screenshots & Capture (FLAG_SECURE):**
 *    - `FLAG_SECURE` is the only officially supported Android API to prevent screen capture.
 *    - When enabled, it instructs the OS `SurfaceFlinger` to exclude the window's content from screenshots,
 *      native screen recorders, and non-secure displays (e.g., Chromecast mirroring, Miracast).
 *    - **Limitation:** System-level debug tools (such as `adb shell screencap` or `screenrecord` on rooted/developer builds)
 *      or specialized OEM ROM-specific captures might bypass this flag in developer environments.
 * 2. **Screen Recording:**
 *    - Standard screen recorder apps that capture the screen buffers will receive black frames for windows protected by `FLAG_SECURE`.
 *    - **Limitation:** Optical capture (e.g., recording the device screen with an external physical camera) or hardware-based
 *      HDMI capture cards on devices where HDCP is disabled cannot be prevented via software APIs.
 * 3. **Recent Apps Switcher (App Switcher Previews):**
 *    - On Android 13+ (API 33+), we utilize `Activity.setRecentsScreenshotEnabled(false)` to reliably instruct the OS
 *      to not take previews of the active window for the Recents screen.
 *    - For backward compatibility and maximum robust fallback across all Android versions, we dynamically inject `FLAG_SECURE`
 *      during `onPause` if "Hide App Content in Recent Apps" is enabled, and remove it on `onResume` if foreground capture is permitted.
 */
object SecureScreenManager {

    // Count of currently active sensitive screens (e.g., Vault, Playback, Paywall)
    private var sensitiveScreenCount = 0

    // Cached user preferences for standard security controls
    var protectScreenshots = false
        private set
    var preventScreenRecording = false
        private set
    var hideRecentApps = false
        private set

    // Track whether the activity is currently paused/stopped (in the background)
    private var isActivityPaused = false

    /**
     * Call when a sensitive screen starts/composes (e.g. Vault, Playback, Paywall).
     */
    fun onSensitiveScreenStart(activity: Activity) {
        sensitiveScreenCount++
        updateActualWindowFlags(activity)
    }

    /**
     * Call when a sensitive screen finishes/disposes.
     */
    fun onSensitiveScreenDispose(activity: Activity) {
        sensitiveScreenCount = maxOf(0, sensitiveScreenCount - 1)
        updateActualWindowFlags(activity)
    }

    /**
     * Call when the main activity transitions to Resumed.
     */
    fun onActivityResumed(activity: Activity) {
        isActivityPaused = false
        updateActualWindowFlags(activity)
    }

    /**
     * Call when the main activity transitions to Paused.
     */
    fun onActivityPaused(activity: Activity) {
        isActivityPaused = true
        updateActualWindowFlags(activity)
    }

    /**
     * Updates the global settings and applies window flags instantly.
     */
    fun updateSettings(
        activity: Activity,
        protectScreenshots: Boolean,
        preventScreenRecording: Boolean,
        hideRecentApps: Boolean
    ) {
        this.protectScreenshots = protectScreenshots
        this.preventScreenRecording = preventScreenRecording
        this.hideRecentApps = hideRecentApps
        updateActualWindowFlags(activity)
    }

    /**
     * Determines whether FLAG_SECURE should be applied to the active window.
     * Always returns true for strictly sensitive screens (e.g. Vault, Paywall, Playback),
     * otherwise falls back to user settings.
     */
    fun shouldApplySecureFlag(
        protectScreenshots: Boolean,
        preventScreenRecording: Boolean,
        hideRecentApps: Boolean,
        isSensitiveScreen: Boolean
    ): Boolean {
        if (isSensitiveScreen) return true
        return protectScreenshots || preventScreenRecording || (isActivityPaused && hideRecentApps)
    }

    /**
     * Evaluates security rules and toggles Window flags.
     */
    private fun updateActualWindowFlags(activity: Activity) {
        activity.runOnUiThread {
            try {
                // Apply FLAG_SECURE if:
                // 1. Any sensitive screen is currently active
                // 2. The user has enabled "Protect Screenshots"
                // 3. The user has enabled "Prevent Screen Recording"
                // 4. The user has enabled "Hide App Content in Recent Apps" and the app is paused (backgrounded)
                val shouldSecure = sensitiveScreenCount > 0 ||
                        protectScreenshots ||
                        preventScreenRecording ||
                        (isActivityPaused && hideRecentApps)

                if (shouldSecure) {
                    activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }

                // Also call setRecentsScreenshotEnabled(false) on API 33+ (Android 13+) for robust Recent Apps preview blocking
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hideRecents = hideRecentApps || sensitiveScreenCount > 0 || protectScreenshots || preventScreenRecording
                    activity.setRecentsScreenshotEnabled(!hideRecents)
                }
            } catch (e: Exception) {
                // Defensive catch to prevent crashes during rapid configuration/lifecycle shifts
            }
        }
    }
}

/**
 * A reusable lifecycle-aware Composable to enforce screen protection.
 * Auto-cleans up on dispose to prevent FLAG_SECURE leakage onto non-protected screens.
 */
@Composable
fun SecureScreen(
    enabled: Boolean = true,
    isSensitive: Boolean = false,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return

    DisposableEffect(enabled, isSensitive) {
        if (enabled && isSensitive) {
            SecureScreenManager.onSensitiveScreenStart(activity)
        }
        onDispose {
            if (enabled && isSensitive) {
                SecureScreenManager.onSensitiveScreenDispose(activity)
            }
        }
    }
}

