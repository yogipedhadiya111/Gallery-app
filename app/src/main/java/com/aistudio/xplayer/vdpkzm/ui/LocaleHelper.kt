package com.aistudio.xplayer.vdpkzm.ui

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

fun mapLanguageNameToCode(name: String): String {
    return when (name.trim().lowercase()) {
        "english" -> "en"
        "spanish", "español" -> "es"
        "hindi", "हिन्दी" -> "hi"
        else -> {
            if (name.contains("Español", ignoreCase = true) || name.contains("Spanish", ignoreCase = true)) "es"
            else if (name.contains("हिन्दी", ignoreCase = true) || name.contains("Hindi", ignoreCase = true)) "hi"
            else "en"
        }
    }
}

fun getSystemDefaultLanguage(): String {
    val locale = Locale.getDefault()
    return when (locale.language) {
        "es" -> "Spanish"
        "hi" -> "Hindi"
        else -> "English"
    }
}

@Suppress("DEPRECATION")
@Composable
fun ProvideLocalizedResources(
    localeCode: String,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val localizedContext = remember(context, localeCode) {
        val locale = when (localeCode) {
            "zh-rCN" -> Locale.SIMPLIFIED_CHINESE
            "zh-rTW" -> Locale.TRADITIONAL_CHINESE
            "in" -> Locale("in", "ID")
            else -> {
                val parts = localeCode.split("-", "_")
                if (parts.size > 1) {
                    Locale(parts[0], parts[1])
                } else {
                    Locale(localeCode)
                }
            }
        }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        context.createConfigurationContext(config)
    }
    CompositionLocalProvider(LocalContext provides localizedContext) {
        content()
    }
}
