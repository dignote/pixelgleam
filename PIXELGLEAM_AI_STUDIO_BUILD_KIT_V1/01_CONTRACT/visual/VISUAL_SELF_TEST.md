# PixelGleam — VISUAL SELF TEST CONTRACT

## 0. Purpose

This document defines the mandatory self-test loop after UI implementation.

The goal is not theoretical 100% pixel identity across every Android device.

The goal is:

- 100% compliance with approved product/UI requirements,
- 0 unauthorized elements,
- 0 BLOCKER deviations,
- 0 MAJOR deviations,
- all design tokens respected,
- approved asset style preserved,
- near-exact geometry on a fixed reference device.

This self-test is mandatory before a screen family can be marked PASS.

---

# 1. Reference Sources

Compare implementation against:

1. `/reference/PIXELGLEAM_FIGMA_MASTER_CURRENT.png`
2. `/docs/visual/UI_MASTER_SPEC.md`
3. `/docs/visual/DESIGN_TOKENS.md`
4. `/docs/visual/SCREEN_SPECS.md`
5. `/docs/visual/COMPONENT_STATE_SPEC.md`
6. `/docs/visual/ASSET_MANIFEST.md`

When the image and written spec differ:

```text
PRD/gameplay truth
→ invariants
→ written visual specs
→ reference image
```

Never add a product feature only because it appears decorative in the image.

---

# 2. Fixed Reference Device

Visual comparison must use one fixed reference configuration.

Default reference target:

```text
Resolution: 1080 × 2400 px
Orientation: portrait
Font scale: 1.0
Display scale / density: fixed and documented
Theme: Light
System language: English
```

If the project already has a stable emulator/device profile, use that profile and record it.

Do not compare screenshots captured from different density/font-scale configurations.

---

# 3. Mandatory Visual QA Loop

For every required screen family:

```text
build
→ launch reference device
→ navigate to exact test state
→ capture screenshot
→ compare against master/spec
→ create discrepancy report
→ classify each discrepancy
→ fix BLOCKER and MAJOR first
→ rebuild
→ recapture
→ compare again
→ repeat until PASS
```

Do not report PASS after only one screenshot review.

---

# 4. Required Screen States to Capture

At minimum capture:

## F1 Gameplay

- Playing
- Selected color
- Hint Active
- Region Filled
- Artwork Completed

## F2 Home / Gallery

- Fresh/no-progress
- In-progress/resume state
- Completed artwork visible

## F3 Category / Artwork Detail

- New artwork
- In-progress artwork
- Completed artwork

## F5 Daily / My Art

- Today available
- Daily completed
- My Art in-progress
- My Art completed

## F6 Settings / Tutorial

- Settings On states
- Settings Off states
- Tutorial first-use state

---

# 5. Comparison Dimensions

Every captured screenshot must be checked in these categories.

## V01 — Screen hierarchy

Check:

- correct major regions,
- correct ordering,
- gameplay/artwork dominance,
- no missing major section.

## V02 — Geometry

Check:

- major container width/height,
- margins,
- top bar height,
- bottom navigation height,
- gameplay canvas proportion,
- artwork preview size,
- palette position.

## V03 — Spacing

Check against token scale:

```text
4 / 8 / 12 / 16 / 24 / 32dp
```

No arbitrary spacing unless documented.

## V04 — Typography

Check:

- role,
- size,
- weight,
- line height,
- alignment,
- numeric legibility.

## V05 — Color

Check:

- shell background,
- text,
- border,
- primary action,
- semantic states,
- artwork colors not polluted by shell palette.

## V06 — Radius / border / shadow

Check:

- approved radius scale,
- 1dp neutral borders,
- restrained elevation,
- no giant rounded cards.

## V07 — Component fidelity

Check:

- buttons,
- color chips,
- progress,
- cards,
- bottom navigation,
- icon buttons,
- settings toggles.

## V08 — Asset fidelity

Check:

- logo,
- icons,
- gem material,
- artwork thumbnails,
- effects.

## V09 — Gameplay visual truth

Check:

- canvas remains dominant,
- numbered cells readable,
- filled cells visually gem-like,
- Hint is subtle,
- no fake gameplay metrics.

## V10 — Unauthorized UI

Check for any unapproved:

- coins,
- premium currency,
- ranks,
- leaderboard,
- shop,
- social/favorite/share controls,
- fake notification badges,
- decorative technical panels.

Any unauthorized product feature is a BLOCKER.

---

# 6. Severity Classification

## BLOCKER

Examples:

- wrong screen hierarchy,
- gameplay canvas no longer dominant,
- missing required control,
- dead control visible,
- unauthorized product feature,
- wrong navigation,
- wrong gameplay state shown,
- incorrect master asset family,
- dark/glass/neon redesign,
- connected-region visual behavior contradicted.

PASS is impossible with any BLOCKER.

## MAJOR

Examples:

- major section size/position visibly wrong,
- spacing system inconsistent,
- wrong primary color,
- typography hierarchy wrong,
- asset material significantly different,
- incorrect button/chip state,
- major icon family inconsistency,
- artwork/detail composition substantially different.

PASS is impossible with any MAJOR.

## MINOR

Examples:

- 1–3dp local spacing mismatch,
- tiny icon optical alignment issue,
- subtle shadow difference,
- small color delta not affecting hierarchy,
- minor text wrap difference.

MINOR issues should be fixed when practical.

---

# 7. Geometry Tolerances

On the fixed reference device:

## Major layout

Target:

```text
major container position/size deviation <= 3%
```

Preferred for anchor elements:

```text
<= 2%
```

## Token spacing

Must use approved token values.

If responsive composition requires deviation, document it.

## Text

Exact pixel match is not required due to Android font rasterization.

Required:

- correct type role,
- correct sp range,
- correct weight,
- correct alignment,
- no unintended wrapping/truncation.

---

# 8. PASS Definition

A screen family is PASS only when:

```text
Required elements present       = 100%
Unauthorized elements           = 0
BLOCKER deviations              = 0
MAJOR deviations                = 0
Design token violations         = 0
Dead/placeholder controls       = 0
Incorrect component states      = 0
Gameplay invariant violations   = 0
```

MINOR deviations:

- should be listed,
- should not exceed reasonable visual tolerance,
- should be fixed when low-risk.

---

# 9. Self-Test Report Format

After every comparison cycle produce:

```text
VISUAL SELF-TEST — [SCREEN/FAMILY]

Reference device:
- resolution:
- density:
- font scale:

Screenshot:
- path:

STATUS:
PASS / REWORK / BLOCKED

BLOCKER:
- ...

MAJOR:
- ...

MINOR:
- ...

Token violations:
- ...

Unauthorized elements:
- ...

Asset deviations:
- ...

Fixes applied:
- ...

Remaining deviations:
- ...

Next action:
- ...
```

---

# 10. Required Iteration Behavior

If STATUS = REWORK:

Codex must:

1. fix identified BLOCKER/MAJOR issues,
2. rebuild,
3. capture a new screenshot,
4. re-run comparison.

Do not stop after writing a discrepancy report.

Do not ask for approval for ordinary corrections already defined by the approved master/spec.

Escalate only when:

- the master and spec conflict materially,
- a required asset does not exist,
- platform limitations prevent faithful implementation,
- fixing the issue would change gameplay/product behavior.

---

# 11. Asset-Specific Self-Test

Gem assets must be checked separately for:

- faceted silhouette,
- compact highlight,
- subtle depth,
- no neon bloom,
- no flat generic polygon look,
- consistent scale,
- transparent background where applicable.

Artwork assets must be checked for:

- pixel/mosaic readability,
- coherent style,
- no baked UI chrome,
- correct thumbnail crop.

---

# 12. Responsive Follow-Up

The fixed reference device is used for master fidelity.

After master PASS, verify at least:

- smaller supported device,
- larger supported device.

Responsive checks ensure:

- no clipping,
- no overlap,
- hit targets remain accessible,
- canvas remains dominant.

Do not change the master design language to solve one device-specific issue.

---

# 13. Final Production Gate

Before declaring visual completion:

- all required screen families have PASS self-test reports,
- all master screenshots are archived,
- all P0 functional tests PASS,
- no fake UI remains,
- Android Back works,
- persistence works,
- settings/audio/haptics work,
- visual consistency pass is complete.

Visual completeness and functional completeness are both required.
