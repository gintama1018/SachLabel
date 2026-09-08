package com.sachlabel.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// SachLabel — Truth & Clarity Food Intelligence Design Tokens (Stitch UI)
// ─────────────────────────────────────────────────────────────────────────────

// Primary Brand Colors (Forest Truth)
val PrimaryGreen = Color(0xFF004328)
val PrimaryContainer = Color(0xFF0D5C3A)
val PrimaryFixed = Color(0xFFA9F3C5)
val PrimaryFixedDim = Color(0xFF8ED6AA)
val OnPrimary = Color(0xFFFFFFFF)
val OnPrimaryContainer = Color(0xFF8AD2A7)
val OnPrimaryFixed = Color(0xFF002111)

// Canvas & Surfaces
val BackgroundSurface = Color(0xFFF4FBF7)         // Natural mint cream canvas
val SurfaceContainerLowest = Color(0xFFFFFFFF)    // Crisp white card surface
val SurfaceContainerLow = Color(0xFFEDF6F0)       // Subtle elevated surface
val SurfaceContainer = Color(0xFFE6EFE9)          // Pill / icon background
val SurfaceContainerHigh = Color(0xFFDCE6E0)      // Borders / dividers
val SurfaceInverse = Color(0xFF151D1A)            // Dark viewfinder container
val InverseOnSurface = Color(0xFFEAF3EE)

// Typography & Content
val TextPrimary = Color(0xFF151D1A)               // Slate charcoal (no pure black)
val TextSecondary = Color(0xFF4D5B54)             // Medium slate
val TextMuted = Color(0xFF707971)                 // Muted gray-green
val OutlineVariant = Color(0xFFBFC9C0)            // Subtle card border
val OutlineColor = Color(0xFF707971)

// Secondary & Alert Tokens (Crimson Alert & Warning)
val AlertCrimson = Color(0xFFAC3311)
val AlertCrimsonContainer = Color(0xFFFF6E48)
val AlertCrimsonLow = Color(0xFFFFDAD6)
val CautionAmber = Color(0xFFD97706)
val CautionAmberContainer = Color(0xFFFFB273)
val CautionAmberLow = Color(0xFFFFDCC3)

// Header Gradients (Warm Forest Green)
val HeaderGradient = Brush.verticalGradient(
    listOf(
        Color(0xFF00472B),
        Color(0xFF0B5336),
        Color(0xFF166342)
    )
)

val CardGradientGreen = Brush.verticalGradient(
    listOf(
        Color(0xFF00472B),
        Color(0xFF0F593B)
    )
)

// Verdict Colors (Stitch Badge System)
val VerdictMisleadingColor = Color(0xFFFF6E48)
val VerdictMisleadingBg = Color(0xFFFFDAD6)
val VerdictNeedsContextColor = Color(0xFFD97706)
val VerdictNeedsContextBg = Color(0xFFFFDCC3)
val VerdictConsistentColor = Color(0xFF0D5C3A)
val VerdictConsistentBg = Color(0xFFA9F3C5)
val VerdictNeutralColor = Color(0xFF707971)
val VerdictNeutralBg = Color(0xFFE6EFE9)

// Floating Bottom Nav
val BottomNavBg = Color(0xFFFFFFFF)
val BottomNavBorder = Color(0x33BFC9C0)
