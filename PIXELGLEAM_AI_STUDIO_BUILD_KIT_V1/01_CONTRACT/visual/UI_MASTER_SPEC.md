# PixelGleam — UI MASTER SPEC

## 0. Role

This document converts the current PixelGleam master visual into implementation rules.

The reference image is:

`/reference/PIXELGLEAM_FIGMA_MASTER_CURRENT.png`

The image is not sufficient by itself. This document identifies which parts are structural truth, visual truth, illustrative reference, and prohibited invention.

---

# 1. Master Visual Direction

**Design DNA:** Gem Mosaic Atelier  
**UI Direction:** Editorial Craft Game UI  
**Theme:** LIGHT ONLY  
**Platform:** Android mobile  
**Primary principle:** Artwork-first, gameplay-first, restrained shell.

PixelGleam should feel:

- tactile,
- crafted,
- cheerful,
- relaxing,
- visually satisfying,
- commercial rather than template-generated.

The app shell should remain neutral enough for artwork and gem colors to dominate.

---

# 2. Truth Layers

## 2.1 Structural truth

The following are implementation requirements:

- gameplay canvas dominates the gameplay screen,
- top app bar is lightweight,
- numbered palette is directly below the canvas,
- hint / zoom / navigation are secondary,
- gallery/home prioritizes playable artwork,
- category grid is simple and content-first,
- artwork detail is visually dominated by artwork preview,
- completed screen is visually dominated by final mosaic,
- bottom navigation is compact,
- no fake dashboard modules.

## 2.2 Visual truth

Treat these as visual rules:

- porcelain / warm ivory shell,
- graphite text,
- purple-violet primary action accent,
- functional artwork colors,
- thin neutral borders,
- moderate radii,
- restrained shadows,
- crisp faceted gem materials,
- simple line icons,
- no neon bloom.

## 2.3 Illustrative reference

These are examples, not hard requirements:

- Happy Corgi artwork,
- Blue Bird artwork,
- exact animal thumbnails,
- exact category illustration content,
- exact pixel pattern visible in the master,
- exact gem placement path.

Do not hardcode demo artwork merely because it appears in the master.

---

# 3. Layout Grammar

## 3.1 Spacing

Use the spacing scale from `DESIGN_TOKENS.md`.

Prefer:

- 8–12dp internal compact spacing,
- 16dp normal screen padding,
- 20–24dp between major groups,
- 24–32dp only for major separation.

Avoid large empty vertical gaps that reduce gameplay area.

## 3.2 Radius

Cards and controls should use restrained radii.

Preferred:

- 8dp,
- 10dp,
- 12dp,
- 14dp.

Do not default to 20–32dp rounded containers.

## 3.3 Borders

Default border:

- 1dp,
- neutral warm gray,
- used instead of excessive shadows.

## 3.4 Shadows

Use only for:

- important raised artwork cards,
- selected/hero artwork,
- subtle gem depth.

No glassy floating panels.

---

# 4. Screen Hierarchy

## Home / Gallery

Priority:

1. Daily / resume artwork,
2. category discovery,
3. My Art preview,
4. navigation.

Do not add:

- coins,
- account avatar,
- rankings,
- shop,
- fake activity counters.

## Category

Priority:

1. category title,
2. artwork grid,
3. progress states.

Artwork cards show only useful data:

- title,
- progress or New / Completed state.

## Artwork Detail

Priority:

1. artwork preview,
2. artwork name,
3. compact metadata if meaningful,
4. progress,
5. Play / Continue.

Do not add social or economy controls.

## Gameplay

Priority:

1. artwork canvas,
2. numbered palette,
3. current progress,
4. Hint / Zoom / navigation.

Canvas must visually occupy approximately 75–85% of useful gameplay height before bottom controls.

## Hint Active

Same gameplay screen.

Do not navigate to a separate screen.

Only add:

- subtle region highlight,
- optional viewport guidance.

Hint never auto-fills.

## Artwork Completed

Priority:

1. completed artwork,
2. completion state,
3. Next Artwork,
4. Back to Gallery.

No fake rewards or currencies.

---

# 5. Gem Material

Gem style is a core identity asset.

Required qualities:

- faceted,
- polished resin/crystal feel,
- crisp compact highlight,
- subtle internal tonal variation,
- readable small silhouette,
- no heavy glow,
- no metallic chrome,
- no photoreal jewelry rendering.

Placed gems should still read as pixel/mosaic units.

---

# 6. Typography

Use a friendly geometric sans with strong numeric legibility.

Required hierarchy:

- Display / screen hero: bold,
- screen title: semibold/bold,
- body: regular/medium,
- labels/buttons: medium/semibold,
- palette numbers: bold and highly legible,
- micro counts: regular/medium.

Do not use oversized marketing headings inside gameplay.

---

# 7. Iconography

Use simple outline icons.

Required style:

- consistent stroke weight,
- compact view box,
- no decorative circle unless required for hit target,
- no filled cartoon icon family mixed with line icons.

Core icons:

- back,
- gallery/home,
- daily,
- my art,
- settings,
- hint,
- zoom,
- completion/check.

---

# 8. Motion Language

Core sequence:

`tap → region validates → gem travel → snap → settle → subtle sparkle`

Motion should be:

- short,
- tactile,
- clear,
- subordinate to interaction.

Do not use:

- endless loops,
- particle storms,
- bouncing whole-screen UI,
- exaggerated springs.

---

# 9. Anti-AI / Prohibited Patterns

Never introduce:

- dark/cyberpunk styling,
- glassmorphism,
- purple/cyan AI gradients,
- nested card stacks,
- giant pill controls,
- fake metrics,
- fake currency,
- fake leaderboard,
- fake social metrics,
- decorative technical terminology,
- generic SaaS dashboard layout,
- excessive centered symmetry,
- excessive glow.

---

# 10. Master Fidelity Rule

When implementation conflicts with the reference image:

1. preserve product behavior from PRD/gameplay specs,
2. preserve structural truth from this document,
3. preserve design tokens,
4. use the image for visual intent,
5. never invent a new product feature to imitate a decorative image detail.
