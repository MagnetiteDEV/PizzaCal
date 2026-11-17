# Pizza Dough Calculator — Android (v1 🤖)

Native Android port of the Pizza Dough Calculator PWA (web v5.3), built with **Kotlin + Jetpack Compose**, 100% offline.

## Features (exact parity with web v5.3)

- **Baker's-percentage engine**: three modes — total dough weight, dough balls (count × weight), flour weight
- **Dough loss %** (default 2%): gross = target × (1 + loss/100)
- **Yeast auto-conversion**: IDY ×1, ADY ×1.25, Fresh ×3 — changing type preserves leavening power, converting from a canonical IDY value so repeated switches don't accumulate rounding drift
- **Poolish / Biga preferments**: auto traditional defaults (100/50 hydration, 100% yeast share), deducted from totals with three result tables (Total / Preferment / Final)
- **Smart rounding**: <10 g → 0.1 g (so 1.2 g yeast doesn't become 1 g), else whole grams
- **5 style presets** (Neapolitan, New York, Detroit, Roman/Teglia, Thin) + user custom presets
- **Pro Tools slide panel** (closed by default, remembers state): fermentation yeast suggestion (equivalent-hours-at-20°C model), Regola del 55 water temperature (with a too-cold warning below 1 °C), pan-size calculator with one-tap apply
- **Copy recipe** (all three sections) + reset with confirmation (keeps language, theme and custom presets)
- **Liquid Glass UI**: frosted-glass cards/buttons/menus, fast simple animations (≤200 ms)
- **Italian trattoria theme** + dark mode (system default, toggle cycles system → light → dark)
- **Bilingual FA/EN** with RTL/LTR switching, Vazirmatn font (subsetted, ~53 KB per weight)
- **Persistence**: DataStore (fields, language, mode, preset, theme, panel state, custom presets)
- **Zero network**: no INTERNET permission in the manifest

## Project structure

```
app/src/main/
├── AndroidManifest.xml
├── java/com/pizzadough/app/
│   ├── MainActivity.kt
│   ├── MainViewModel.kt          # all state + actions (mirrors web app.js logic)
│   ├── domain/
│   │   ├── Calculator.kt         # pure engine: calculate(), suggestYeastIDY(), waterTempC(), panWeight(), formatting
│   │   └── Models.kt             # enums, Preset, BUILTIN_PRESETS
│   ├── data/
│   │   └── PrefsRepository.kt    # DataStore persistence
│   └── ui/
│       ├── theme/Theme.kt        # Italian palette, dark mode, Liquid Glass tokens, Vazirmatn
│       ├── components/Glass.kt   # GlassCard, GlassButton, GlassSegmentedControl, GlassNumberField, pills
│       └── screens/
│           ├── CalculatorScreen.kt  # header, presets, mode, formula, preferment, results
│           ├── ProToolsPanel.kt     # slide panel (fermentation / water temp / pan)
│           └── ResultsTable.kt      # result tables + clipboard text builder
└── res/
    ├── values/strings.xml        # English (103 strings)
    ├── values-fa/strings.xml     # Persian (103 strings)
    ├── font/vazirmatn_{regular,medium,bold}.ttf  # subsetted Latin+Persian
    └── mipmap-anydpi-v26/        # adaptive launcher icon
```

## Build

Requirements: Android Studio (Hedgehog+) or command line with Android SDK 34 + JDK 17.

```bash
# generate the Gradle wrapper jar (not shipped in the zip)
gradle wrapper
./gradlew assembleDebug      # APK in app/build/outputs/apk/debug/
./gradlew assembleRelease    # minified with R8 + resource shrinking
```

Open the folder in Android Studio and press Run — that's it.

## GitHub Actions (automatic APK builds)

A workflow at `.github/workflows/build-apk.yml` builds an installable APK on
every push to `main` (uploaded as an Actions artifact) and attaches it to a
GitHub Release on version tags (`v*`). Fonts are binary, so they are not
committed — CI downloads Vazirmatn TTFs from the official release. To use it:

1. Create an empty public repo (e.g. `PizzaCal`) and push this folder.
2. Push a tag: `git tag v1.0 && git push origin v1.0`
3. Download `pizza-dough-v1.apk` from the Releases page and install it
   (allow "install unknown apps" when prompted).

## Verification

- Calculation engine (`domain/Calculator.kt`) compiled with kotlinc 2.0.20 and run against the regression suite:
  - 1000 g Neapolitan → sums to exactly 1000.00 (flour 606.06, water 375.76, salt 16.97, yeast 1.21)
  - 4×250 g balls @2% loss → gross 1020.0
  - 1000 g flour mode → 1650.0 g gross
  - Poolish 30%/100%/100% flour split reconciles
  - All 5 invalid inputs throw the correct error (prefFlourPct 120, prefYeastShare 150, negative preferment flour, hydration −5, salt 99)
  - Fermentation suggestions: 24 h fridge → 0.34%, 4 h room + 24 h fridge → 0.19%, 48 h fridge + 4 h room → 0.12%
  - Regola del 55: room 20 + flour 20 → water 15.0°C
  - Pan: 38×28 @0.6 → 638 g; Ø30 @0.6 → 424 g
- UI layer (Compose) is written per the spec but **not compiled here** — this machine has no Android SDK. Build it once in Android Studio to confirm.

## Performance notes (per the design brief)

- No WebView, no heavy dependencies; release builds use R8 + resource shrinking
- Glass = static translucent layers + hairline borders (no per-frame blur); animations capped at 180–200 ms, transform/alpha only
- Compose: `derivedStateOf`-style recompute via a single combined `StateFlow`, state hoisting, keyed lists, coroutines tied to `viewModelScope`
- Fonts subsetted to Latin + Persian glyphs (~53 KB each); vector-only icons

## Changelog

- **v5.3 parity** (2026-11-17): canonical-IDY yeast conversion, language-aware % sign, water-temperature warning, reset confirmation preserving settings — calculation engine and defaults unchanged
- **v1** (2026-10-30): initial Android port from web v5.2 — all features above, Liquid Glass UI, badge «v1 🤖 Android»
