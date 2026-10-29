@file:Suppress("DEPRECATION")
package com.aistudio.xplayer.vdpkzm.data

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log

@Suppress("DEPRECATION")
class AudioEffectsManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("audio_effects_prefs", Context.MODE_PRIVATE)

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    var isEqEnabled: Boolean
        get() = prefs.getBoolean("eq_enabled", false)
        set(value) {
            prefs.edit().putBoolean("eq_enabled", value).apply()
            equalizer?.enabled = value
        }

    var bassLevel: Short
        get() = prefs.getInt("bass_level", 0).toShort()
        set(value) {
            prefs.edit().putInt("bass_level", value.toInt()).apply()
            try {
                if (bassBoost?.strengthSupported == true) {
                    bassBoost?.setStrength(value)
                }
            } catch (e: Exception) {
                Log.e("AudioEffectsManager", "Error setting bass strength", e)
            }
        }

    var virtualizerStrength: Short
        get() = prefs.getInt("virt_strength", 0).toShort()
        set(value) {
            prefs.edit().putInt("virt_strength", value.toInt()).apply()
            try {
                if (virtualizer?.strengthSupported == true) {
                    virtualizer?.setStrength(value)
                }
            } catch (e: Exception) {
                Log.e("AudioEffectsManager", "Error setting virtualizer strength", e)
            }
        }

    var selectedPreset: Short
        get() = prefs.getInt("selected_preset", -1).toShort()
        set(value) {
            prefs.edit().putInt("selected_preset", value.toInt()).apply()
            try {
                if (value >= 0 && equalizer != null && value < equalizer!!.numberOfPresets) {
                    equalizer?.usePreset(value)
                    saveBandLevelsFromCurrentPreset()
                }
            } catch (e: Exception) {
                Log.e("AudioEffectsManager", "Error setting preset", e)
            }
        }

    fun getBandLevel(band: Short): Short {
        return prefs.getInt("band_level_$band", 0).toShort()
    }

    fun setBandLevel(band: Short, level: Short) {
        prefs.edit().putInt("band_level_$band", level.toInt()).apply()
        try {
            equalizer?.setBandLevel(band, level)
            // If manual, set preset to -1
            prefs.edit().putInt("selected_preset", -1).apply()
        } catch (e: Exception) {
            Log.e("AudioEffectsManager", "Error setting band level", e)
        }
    }

    fun initEffects(audioSessionId: Int) {
        if (audioSessionId == 0) return
        release()

        try {
            // Initialise native Equalizer
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = isEqEnabled
                val preset = selectedPreset
                if (preset >= 0 && preset < numberOfPresets) {
                    usePreset(preset)
                } else {
                    for (i in 0 until numberOfBands) {
                        setBandLevel(i.toShort(), getBandLevel(i.toShort()))
                    }
                }
            }

            // Initialise BassBoost
            bassBoost = BassBoost(0, audioSessionId).apply {
                if (strengthSupported) {
                    enabled = true
                    setStrength(bassLevel)
                }
            }

            // Initialise Virtualizer
            virtualizer = Virtualizer(0, audioSessionId).apply {
                if (strengthSupported) {
                    enabled = true
                    setStrength(virtualizerStrength)
                }
            }

            Log.d("AudioEffectsManager", "Audio effects successfully initialized on session: $audioSessionId")
        } catch (e: Exception) {
            Log.e("AudioEffectsManager", "Failed to initialize native audio effect modules", e)
        }
    }

    private fun saveBandLevelsFromCurrentPreset() {
        equalizer?.let { eq ->
            for (i in 0 until eq.numberOfBands) {
                val band = i.toShort()
                prefs.edit().putInt("band_level_$band", eq.getBandLevel(band).toInt()).apply()
            }
        }
    }

    fun getPresetNames(): List<String> {
        val names = mutableListOf<String>()
        equalizer?.let { eq ->
            for (i in 0 until eq.numberOfPresets) {
                names.add(eq.getPresetName(i.toShort()) ?: "Preset $i")
            }
        }
        // Custom fallback presets if equalizer is not yet bound
        if (names.isEmpty()) {
            return listOf("Normal", "Classic", "Dance", "Flat", "Folk", "Heavy Metal", "Hip Hop", "Jazz", "Pop", "Rock")
        }
        return names
    }

    fun getBandCount(): Int {
        return equalizer?.numberOfBands?.toInt() ?: 5
    }

    fun getBandCenterFreq(band: Short): Int {
        return equalizer?.getCenterFreq(band) ?: 1000
    }

    fun getBandLevelRange(): Pair<Short, Short> {
        val range = equalizer?.bandLevelRange
        return if (range != null && range.size >= 2) {
            Pair(range[0], range[1])
        } else {
            Pair(-1500, 1500) // Standard +/- 15 dB range in millibels
        }
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (e: Exception) {
            Log.e("AudioEffectsManager", "Error releasing effects", e)
        }
        equalizer = null
        bassBoost = null
        virtualizer = null
    }
}
