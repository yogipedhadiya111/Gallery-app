# XPlayer Pro – Multi-Language Localization Guide

This document describes the design, implementation, and best practices of the production-ready multi-language system engineered for the **XPlayer Pro** Android application.

---

## 1. System Architecture & Real-Time Locale Swapping

Unlike standard Android application localization which requires an activity restart to apply configuration changes, **XPlayer Pro** implements a reactive, state-driven real-time locale engine in Jetpack Compose.

```
                  ┌──────────────────────────────┐
                  │      MainViewModel           │
                  │ (Selected Language StateFlow)│
                  └──────────────┬───────────────┘
                                 │ Updates
                                 ▼
                  ┌──────────────────────────────┐
                  │      MainActivity            │
                  │ (Collects Language StateFlow)│
                  └──────────────┬───────────────┘
                                 │
                                 ▼
         ┌────────────────────────────────────────────────┐
         │ ProvideLocalizedResources (Locale Context)      │
         │  - Sets custom Configuration Context           │
         │  - Maps Display Name -> Standard Locale Code   │
         │  - Sets Layout Direction (supports LTR & RTL)  │
         ┌───────────────────────┬────────────────────────┘
                                 │
                                 ▼
                     ┌───────────────────────┐
                     │   Compose UI Tree     │
                     │  (Instant Re-render)  │
                     └───────────────────────┘
```

### Dynamic Context Injection (`LocaleHelper.kt`)
The core of the dynamic engine is `ProvideLocalizedResources` inside `/app/src/main/java/com/aistudio/xplayer/vdpkzm/ui/LocaleHelper.kt`. It constructs a new configuration context on top of the system `LocalContext`:

```kotlin
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
            "in" -> Locale("in", "ID") // Indonesian
            else -> {
                val parts = localeCode.split("-", "_")
                if (parts.size > 1) Locale(parts[0], parts[1]) else Locale(localeCode)
            }
        }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale) // Real-time RTL support
        context.createConfigurationContext(config)
    }
    CompositionLocalProvider(LocalContext provides localizedContext) {
        content()
    }
}
```

---

## 2. Right-to-Left (RTL) Layout & Arabic Support

To support Right-to-Left (RTL) languages like Arabic (`ar`):
- **Forced Mirroring**: `config.setLayoutDirection(locale)` dynamically tells the Material Design 3 layout engine to render components from right to left immediately when Arabic is chosen.
- **RTL-Friendly Modifiers**: Standard Compose modifiers like `padding(start, end)` mirror automatically. Avoid hardcoded `left` or `right` layouts.
- **AutoMirrored Icons**: Directional icons (like back arrows, next buttons) utilize `Icons.AutoMirrored` so that their vector trajectories mirror automatically depending on the layout direction.

---

## 3. Best Practices for Adding a New Language

Adding a new language (e.g., Japanese, Korean, or a custom dialect) is designed to be completed in **3 simple steps** without touching any business logic:

### Step 1: Create the Translation Resource Folder
Create a standard Android resource folder under `app/src/main/res/` named with the language's ISO 639-1 code (e.g., `values-ja/` for Japanese):
```
app/src/main/res/values-ja/strings.xml
```
*Note: Ensure all string keys exactly match the base `strings.xml` in `values/`.*

### Step 2: Register the Language Item
Open `/app/src/main/java/com/aistudio/xplayer/vdpkzm/ui/LanguageSelectionScreen.kt` and add your new language to the `popularLanguages` list:
```kotlin
LanguageItem(
    code = "ja", 
    name = "Japanese", 
    nativeName = "日本語", 
    flag = "🇯🇵"
)
```

### Step 3: Add Locale Mapping
Open `/app/src/main/java/com/aistudio/xplayer/vdpkzm/ui/LocaleHelper.kt` and update:
1. `mapLanguageNameToCode(name: String)`:
   ```kotlin
   "japanese", "日本語" -> "ja"
   ```
2. `getSystemDefaultLanguage()` (for auto-detecting system default):
   ```kotlin
   "ja" -> "Japanese"
   ```

---

## 4. Multi-Language Testing Strategy

To ensure zero regression, prevent layout overflows, and guarantee high-fidelity text scaling across localized layouts:

### A. Robolectric & Screenshot Testing (Visual Verification)
Instead of expensive emulator runs, utilize standard JVM-based screenshot verification to verify each language's layout alignment.
1. Render individual screens under the custom localized context.
2. Take reference screenshots with **Roborazzi** for languages with extremely long word lengths (e.g., German, Polish) to detect text clips or truncated boxes.
3. Validate Arabic layouts to ensure RTL layout mirroring aligns correctly.

### B. Lint Verification
Run code and resource lints to check for missing translations:
```bash
./gradlew lintDebug
```
*Tip: Ensure `<string name="...">` tags do not contain hardcoded formatting values that differ between languages, which can crash the application.*

---

## 5. Google Play Store Localization Recommendations

To maximize global organic reach and boost app conversions on Google Play:

1. **Localize Store Metadata**: Translate your App Title, Short Description, and Long Description on the Google Play Console for all 20+ supported languages.
2. **Dynamic Delivery**: Keep the APK light. Use **Android App Bundles (.aab)** so Google Play only delivers the translation resources relevant to the user's system language, optimizing device storage.
3. **Graphic Asset Translation**: Translate text-heavy store screenshots and promo graphics for each target region.
4. **Custom Store Listings**: Create unique store listing URLs for specific high-growth countries to deliver localized messaging matching specific local holiday campaigns or search keywords.
