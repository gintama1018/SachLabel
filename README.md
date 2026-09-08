# SachLabel • सच परखें 🔍
### *Truth & Clarity Food Intelligence for Indian Consumers*

> **"We don't score how healthy a product is. We check whether what's printed on the front of the pack actually matches what's printed on the back — and show you the exact words that prove it, in your own language."**

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.23-purple.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.04.01-green.svg)](https://developer.android.com/jetpack/compose)
[![CameraX](https://img.shields.io/badge/CameraX-1.3.3-blue.svg)](https://developer.android.com/training/camerax)
[![ML Kit](https://img.shields.io/badge/ML%20Kit-Latin%20%2B%20Devanagari-orange.svg)](https://developers.google.com/ml-kit/vision/text-recognition/v2)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 🎯 The Core Thesis

Packaged food brands routinely use bold marketing assertions on the front of packages:
* *"100% Natural"*
* *"No Added Sugar"*
* *"No Preservatives"*
* *"Wheat Rusk / Made with Whole Wheat"*

Yet the statutory fine print on the back of the package (mandated by FSSAI) frequently contradicts or qualifies these assertions:
* The *"100% Natural"* juice contains reconstituted concentrate and acidity regulators.
* The *"No Preservatives"* sauce lists **Sodium Benzoate (INS 211)**.
* The *"Wheat Rusk"* contains **68% Refined Flour (Maida)** and only **12% Whole Wheat**.

**SachLabel does NOT assign arbitrary 0–100 health scores.** Instead, it performs a **dual-pack evidence audit**:
1. **Front Photo**: Captures the marketing claim.
2. **Back Photo**: Captures the statutory ingredient list and nutrition table.
3. **On-Device OCR & Rule Engine**: Audits the claim against statutory reality.
4. **Verdict & Proof**: Displays the exact fine-print quotation and provides plain-language voice explanation in regional Indian languages (**Hindi, Tamil, Bengali, English**).

---

## ✨ Features & Capabilities

- **Stitch UI Design System**:
  - Implements the complete Stitch *Truth & Clarity Food Intelligence* visual system.
  - Light mint cream canvas (`#F4FBF7`), forest green primary header (`#004328`), and pure white cards (`#FFFFFF`).
  - Signature `DualEvidenceCard` directly contrasting front claims with statutory back evidence.
  - Moral clarity verdict badges: `VERIFIED ACCURATE`, `NEEDS CONTEXT`, `MISLEADING`, `UNCERTAIN`, `NO CLAIM`.
  - Floating pill bottom navigation with elevated central scan button.
- **100% On-Device & Offline Execution**:
  - CameraX capture with live OCR preview reticle.
  - Google ML Kit On-Device Text Recognition supporting both **Latin** and **Devanagari** scripts.
  - Zero cloud dependencies for scanning or verification — operates with zero network connection.
- **Deterministic Rule Engine (8 Bounded FSSAI Claim Categories)**:
  1. *No Added Sugar* (Checks for maltodextrin, invert syrup, honey, fruit concentrate, high free sugars)
  2. *100% Natural / Pure* (Checks for synthetic additives, acidity regulators, artificial flavoring)
  3. *Sugar-Free* (Checks sugars per 100g > 0.5g limit)
  4. *No Preservatives* (Checks Class II preservatives: sodium benzoate, potassium sorbate, sulfites)
  5. *Organic* (Checks for organic certification tags / synthetic stabilizers)
  6. *High Protein* (Validates protein content against FSSAI RDA thresholds)
  7. *Zero Trans Fat* (Checks trans fat > 0.2g threshold / hydrogenated vegetable oils)
  8. *Vague Wellness Claims* (*"Pure", "Wholesome", "Detox"*)
- **Regional Voice Verdicts (Text-to-Speech)**:
  - Accessible to non-English literate consumers.
  - Explanations synthesized directly in Hindi, Tamil, Bengali, and English using Android's native `TextToSpeech` engine.
- **Personal Health Context (Secondary & Informational)**:
  - Optional relevance check for specific dietary concerns (*Diabetes, High Blood Pressure, Gluten-free, Nut Allergies*).
  - Strictly non-medical and bounded by statutory disclaimers.
- **Interactive Demo Scenarios**:
  - Built-in selector to test all 5 verdict states anytime without physical packaging.

---

## 📱 Screens & User Flow

```
Welcome / Splash
      ↓
Language Selection (English • हिन्दी • தமிழ் • বাংলা)
      ↓
Home Dashboard (Hero Split • Action Center • Latest Investigation)
      ↓
Front Camera Capture (Marketing Claim)
      ↓
Back Camera Capture (Statutory Ingredients & Nutrition)
      ↓
On-Device Processing (Staged 4-Step Checklist)
      ↓
Product Verdict Screen
   ├── Dual Evidence Card (Front Assertion vs Back Reality)
   ├── Voice Verdict (Regional Audio Playback)
   └── Optional Health Context (Dietary Relevance)
```

---

## 🛠️ Architecture & Tech Stack

| Layer | Technologies |
| :--- | :--- |
| **UI Framework** | Jetpack Compose (Material 3 + Stitch Custom Tokens) |
| **Architecture** | Single-Activity Clean MVVM with Kotlin Coroutines & Flow |
| **Camera** | CameraX (`androidx.camera:camera-camera2`, `camera-lifecycle`, `camera-view`) |
| **OCR Vision** | Google ML Kit (`text-recognition`, `text-recognition-devanagari`) |
| **Rule Engine** | Pure Kotlin deterministic pattern matching & regex tokenizer |
| **Persistence** | Jetpack DataStore (Preferences) |
| **Voice Synthesis** | Android `android.speech.tts.TextToSpeech` |
| **Minimum SDK** | API 26 (Android 8.0 Oreo) |
| **Target SDK** | API 34 (Android 14) |

---

## 🚀 Building & Running

### Prerequisites
* **Android Studio Iguana | Hedgehog** (or newer)
* **JDK 17** (e.g. OpenJDK 17 or Microsoft JDK 17)
* **Android SDK 34**

### Command-Line Build

```bash
# Clone the repository
git clone https://github.com/gintama1018/SachLabel.git
cd SachLabel

# Run all 40 unit tests
./gradlew test

# Assemble Debug APK
./gradlew assembleDebug
```

The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🧪 Testing

The repository contains automated unit tests covering the deterministic claim rules, regex ingredient parsing, and edge cases:

```bash
./gradlew testDebugUnitTest
```

Key test suites:
* `RuleEngineTest.kt` (40 unit tests covering all 8 claim categories, preservative detection, sugar synonyms, missing evidence states, and edge cases).
* `ClaimMatcherTest.kt` (Fuzzy matching and claim extraction from raw OCR text blocks).

---

## 📖 Documentation Index

Complete specifications and design assets are available in the [`docs/`](docs/) directory:
* [`docs/PRD.md`](docs/PRD.md) — Product Requirements & Non-Goals
* [`docs/CLAIM_RULES.md`](docs/CLAIM_RULES.md) — The 8 Deterministic FSSAI Rule Specifications
* [`docs/architecture.md`](docs/architecture.md) — Complete System Architecture & Contracts
* [`docs/DEMO_SCRIPT.md`](docs/DEMO_SCRIPT.md) — Hackathon Judge Walkthrough Script
* [`docs/pitch-outline.md`](docs/pitch-outline.md) — 3-Minute Competition Pitch
* [`docs/stitch_design/DESIGN.md`](docs/stitch_design/DESIGN.md) — Stitch UI Tokens & Layout Specs

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
