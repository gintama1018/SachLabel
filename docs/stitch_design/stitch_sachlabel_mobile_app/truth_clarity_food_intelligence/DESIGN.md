---
name: Truth & Clarity Food Intelligence
colors:
  surface: '#f3fbf6'
  surface-dim: '#d3dcd7'
  surface-bright: '#f3fbf6'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#edf6f0'
  surface-container: '#e7f0eb'
  surface-container-high: '#e2eae5'
  surface-container-highest: '#dce5df'
  on-surface: '#151d1a'
  on-surface-variant: '#404942'
  inverse-surface: '#2a322f'
  inverse-on-surface: '#eaf3ee'
  outline: '#707971'
  outline-variant: '#bfc9c0'
  surface-tint: '#226b47'
  primary: '#004328'
  on-primary: '#ffffff'
  primary-container: '#0d5c3a'
  on-primary-container: '#8ad2a7'
  inverse-primary: '#8ed6aa'
  secondary: '#ac3311'
  on-secondary: '#ffffff'
  secondary-container: '#ff6e48'
  on-secondary-container: '#651400'
  tertiary: '#5a2e00'
  on-tertiary: '#ffffff'
  tertiary-container: '#7c4100'
  on-tertiary-container: '#ffb273'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#a9f3c5'
  primary-fixed-dim: '#8ed6aa'
  on-primary-fixed: '#002111'
  on-primary-fixed-variant: '#005232'
  secondary-fixed: '#ffdbd1'
  secondary-fixed-dim: '#ffb4a1'
  on-secondary-fixed: '#3c0800'
  on-secondary-fixed-variant: '#881f00'
  tertiary-fixed: '#ffdcc3'
  tertiary-fixed-dim: '#ffb77d'
  on-tertiary-fixed: '#2f1500'
  on-tertiary-fixed-variant: '#6e3900'
  background: '#f3fbf6'
  on-background: '#151d1a'
  surface-variant: '#dce5df'
typography:
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
  headline-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 26px
    fontWeight: '700'
    lineHeight: 34px
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 22px
    fontWeight: '700'
    lineHeight: 28px
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  title-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 22px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 13px
    fontWeight: '700'
    lineHeight: 16px
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 14px
  data-metric:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '800'
    lineHeight: 28px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  space-2xs: 0.25rem
  space-xs: 0.5rem
  space-sm: 0.75rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
  space-2xl: 3rem
  gutter-mobile: 1rem
  margin-mobile: 1rem
  touch-target-min: 3rem
---

## Brand & Style

The design system establishes a high-trust, investigative, yet instantly accessible consumer utility designed for daily supermarket decisions across India. Its core purpose is radical food label transparency: cross-referencing aggressive front-of-pack marketing assertions ("100% Whole Wheat", "No Added Sugar", "Immunity Boosting") against statutory back-of-pack nutritional facts, ingredient percentages, and regulatory standards.

### Aesthetic Direction: Modern Investigative Utility
The aesthetic balances editorial credibility and surgical consumer utility. It rejects cluttered, clinical medical software conventions in favor of a warm, confident, and tactile civic-utility feel:
- **Warm Editorial Precision:** Off-white, warm cream foundations replace sterile cold greys, reducing screen glare in brightly lit supermarket aisles.
- **Visual Honesty:** Information hierarchy highlights hard metrics—grams of hidden sugars, palm oil percentages, synthetic additives—through distinct tabular cards and clear verdict badges.
- **Decisive Truth Signals:** Rich forest greens validate certified nutritional assertions, while warning ambers and alert corals immediately flag synthetic substitutes, disguised sugars, and regulatory loopholes.
- **Consumer Empowerment:** Designed for rapid one-handed scanning under three seconds while standing in grocery aisles.

## Colors

The color system delivers instantaneous moral clarity regarding food claims. High contrast ratios ensure full legibility on varied mobile displays under direct sunlight and bright store fluorescents.

### Role Tokens & Semantic Intent
- **Primary (`#0D5C3A` - Forest Truth):** Represents verification, certified nutritional integrity, and regulatory compliance. Applied to verified claim badges, primary action triggers, validated ingredient pills, and positive score indicators.
- **Secondary (`#D9532F` - Crimson Alert):** Communicates critical discrepancies, hidden allergens, ultra-processed industrial additives, and deceptive claims. Demands immediate pause before purchasing.
- **Tertiary (`#D97706` - Caution Amber):** Signifies borderline thresholds—such as moderate sodium spikes, synthetic sweeteners, or ambiguous "natural identical" flavorings.
- **Neutral Surface & Content (`#1C2421` - Slate Charcoal):** High-legibility deep carbon-slate for all core typography and structure, preventing the harsh optical vibration of pure black.
- **Canvas Base (`#F9F8F3` - Natural Cream):** Warm, grounded off-white background evoking unbleached parchment and wholesome purity, avoiding digital eye fatigue.
- **Surface Elevation (`#FFFFFF` - Crisp White):** Dedicated card background providing distinct separation against the cream foundation.

## Typography

The design system adopts **Plus Jakarta Sans** as the unified typographic engine. Its wide geometric counters, contemporary terminals, and robust weight distribution ensure extreme legibility when reading complex ingredient manifests, micro-gram nutritional specs, and dynamic percentage comparisons on small mobile viewports.

### Application Guidelines
- **Product & Verdict Headlines (`headline-lg-mobile`, `headline-md`):** Set in bold weights to immediately identify the scanned packaged product name and overall safety rating (e.g., "Misleading Claim Detected").
- **Analytical Metrics (`data-metric`):** Specialized high-density numeric display weight used exclusively for rapid nutritional parsing (e.g., "34g Sugar per 100g", "12% Palm Oil").
- **Evidence Comparison (`body-md`, `body-sm`):** Neutral slate tone with relaxed tracking for reading side-by-side claim disclaimers and microscopic ingredient lists.
- **Status Badges (`label-lg`, `label-md`):** Uppercase or tight small-caps tracking (+0.5px) for clear categorization pills (e.g., "FRONT LABEL", "BACK EVIDENCE", "VERIFIED").

## Layout & Spacing

The layout model is anchored on an 8pt base grid optimized for mobile portrait ergonomics, thumb navigation, and quick handheld interactions.

### Layout Rules
- **Mobile Container Structure:** Fluid 4-column structure with strict `16px` (`1rem`) outer screen padding and `12px` inter-card column gutters.
- **3-Column Quick Action Matrix:** For product detail screens and dashboard summaries, key metrics are structured as a balanced 3-column comparative strip:
  1. *Claimed Benefit* (Left)
  2. *Actual Reality* (Center)
  3. *Verdict / Delta* (Right)
- **Ergonomic Touch Targets:** Primary interactive targets (barcode triggers, scan buttons, claim breakdown toggles) maintain a strict minimum bounding box of `48px` (`3rem`) for rapid one-handed tap precision.
- **Persistent Bottom Navigation Clearance:** Content lists automatically apply `80px` (`5rem`) bottom padding buffer to ensure full visibility above the persistent navigation dock.

## Elevation & Depth

Visual hierarchy uses physical, tactile card structures combined with warm tinted ambient shadows rather than stark digital blurs.

### Surface Elevation System
- **Level 0 (App Canvas):** Unbleached cream canvas (`#F9F8F3`), flat, zero elevation.
- **Level 1 (Card Baseline):** Pure white cards (`#FFFFFF`) with a subtle border outline (`1px solid rgba(28, 36, 33, 0.08)`) and soft diffuse drop-shadow: `0 2px 8px rgba(13, 92, 58, 0.04)`.
- **Level 2 (Active Evidence Overlays & Floating Verdicts):** Elevated comparison popovers and verdict modules use: `0 8px 24px rgba(28, 36, 33, 0.10)`.
- **Level 3 (Persistent Bottom Bar & Floating Barcode Action):** Pure white anchored dock with top border separation and an upward lift shadow: `0 -4px 16px rgba(28, 36, 33, 0.06)`.

## Shapes

The design system implements balanced geometric softness (`roundedness: 2`), reflecting the approachability of a daily consumer companion alongside the clinical rigor of an evidence tool.

### Geometry Specifications
- **Standard Structural Cards:** `16px` (`1rem`) corner radius for content groupings, scan comparison boxes, and score summaries.
- **Interactive Action Buttons:** `12px` (`0.75rem`) corner radius to provide a stable, pressable foundation.
- **Evidence Status Pills & Nutritional Chips:** Full pill radius (`9999px`) for compact categorical tags, allergens, and certification badges.
- **Modal Sheets & Bottom Trays:** `24px` (`1.5rem`) top-left and top-right radii for upward-sliding back-of-pack ingredient analysis panels.

## Components

### 1. Evidence Comparison Cards (Front vs. Back)
The central signature component of the app. A dual-panel card showing:
- **Left/Top Flank:** The front-of-pack claim highlighted with quote styling and promotional packaging snippet.
- **Right/Bottom Flank:** The statutory back-of-pack verification line, citing exact ingredient order, lab-tested sugar/fat percentages, and regulatory citation.
- **Divider:** Subtle dotted line with an embedded truth verdict chip (`VERIFIED`, `EXAGGERATED`, `MISLEADING`).

### 2. Action Grid (3-Column Nutritional Matrix)
- Three equal-width card modules placed horizontally showing key consumer flags:
  1. **Sugar Reality** (e.g., "Contains 4.2 tsp sugar")
  2. **Oil / Fat Base** (e.g., "78% Refined Palm Oil")
  3. **Synthetic Additives** (e.g., "INS 150d / Class IV Caramel")
- Tinted background fills (`rgba(13, 92, 58, 0.08)` for safe items, `rgba(217, 83, 47, 0.08)` for warning items) with bold colored metric texts.

### 3. Verdict Badges & Chips
- High-contrast, dense visual tags using full-pill geometry (`roundedness: 9999px`).
- **Pass (Truth):** `#0D5C3A` background with crisp white label, accompanied by a checkmark icon.
- **Alert (Misleading):** `#D9532F` background with crisp white label, accompanied by a warning triangle.
- **Caution (Ambiguous):** `#D97706` background with crisp white label.

### 4. Interactive Buttons
- **Primary Verification Button:** Rich Forest Green background (`#0D5C3A`), white label, `48px` minimum height, tactile active press state (`scale(0.98)`).
- **Secondary Action Button:** Transparent surface with `1.5px` border in `#0D5C3A`, neutral slate typography.
- **Danger Action Button:** Light crimson wash (`rgba(217, 83, 47, 0.12)`) with `#D9532F` text for reporting deceptive labels or submitting manual scans.

### 5. Input Fields & Search Bars
- Background set to crisp `#FFFFFF` with a `1px` neutral border (`rgba(28, 36, 33, 0.16)`).
- Dedicated barcode scan button integrated directly into the trailing edge of search inputs for rapid store lookups.
- Focused state: `2px` ring in `#0D5C3A` with no offset.

### 6. Persistent Bottom Navigation
- Anchored mobile bottom navigation dock with 4 primary destinations: **Scan**, **Compare**, **Pantry Alerts**, and **Truth Feed**.
- Active icons use filled Forest Green (`#0D5C3A`) with an underlying active dot indicator; inactive icons remain low-contrast slate gray (`rgba(28, 36, 33, 0.45)`).
- Center floating scan button elevated with high-contrast forest green circular container and camera glyph.