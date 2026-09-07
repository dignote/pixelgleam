# PixelGleam — ASSET MANIFEST

## 0. Purpose

This document tells Codex what should be code-drawn and what should be delivered as reusable visual assets.

Do not redraw production art with generic Compose geometry when a dedicated asset is required.

---

# 1. CODE-DRAWN UI

Implement in code:

- app backgrounds,
- cards/surfaces,
- dividers,
- progress bars,
- palette chip containers,
- button containers,
- selection outlines,
- screen layout,
- typography,
- simple loading/empty states.

---

# 2. VECTOR ICONS

Required icon family:

```text
ic_back
ic_gallery
ic_daily
ic_my_art
ic_settings
ic_hint
ic_zoom
ic_check
ic_close / exit if needed
```

Style:

- simple line icons,
- consistent stroke,
- no mixed visual families.

Use standard Material symbols only if they match the approved master; otherwise use custom vectors.

---

# 3. BRAND ASSETS

Required:

```text
brand/pixelgleam_logo
brand/pixelgleam_mark
```

The master currently shows a faceted gem mark plus PixelGleam wordmark.

Final export may be produced after brand approval.

Do not recreate the wordmark using an arbitrary font if a locked logo asset exists.

---

# 4. GEM ASSETS

Required production family:

```text
gems/gem_base_template
gems/gem_red
gems/gem_orange
gems/gem_yellow
gems/gem_green
gems/gem_blue
gems/gem_purple
gems/gem_pink
```

Exact colors may be dynamically tinted if visual quality remains consistent.

Required qualities:

- faceted,
- resin/crystal,
- compact highlight,
- clean silhouette,
- transparent alpha,
- no baked scene background.

Preferred exports:

- SVG/vector if material remains convincing,
- otherwise transparent PNG/WebP at appropriate densities.

---

# 5. EFFECT ASSETS

Potential reusable assets:

```text
effects/sparkle_small
effects/sparkle_cluster
effects/region_complete_shimmer
effects/artwork_complete_confetti_or_sparkle
```

Effects must remain restrained.

If code particles can match the master exactly, asset files are optional.

---

# 6. ARTWORK CONTENT

Each artwork package should contain:

```text
artwork/<id>/metadata.json
artwork/<id>/thumbnail.webp
artwork/<id>/artwork.json
```

Optional final preview:

```text
artwork/<id>/completed_preview.webp
```

Gameplay topology comes from data, not a flat screenshot.

Artwork categories may include:

- animals,
- nature,
- food,
- people,
- mandala,
- fantasy,
- seasonal.

---

# 7. MASTER REFERENCE

Current master:

```text
/reference/PIXELGLEAM_FIGMA_MASTER_CURRENT.png
```

This file is a visual reference, not a runtime asset.

Do not ship it inside the app.

---

# 8. DO NOT CREATE

Do not create production assets for:

- coins,
- premium gems/currency,
- leaderboard,
- ranks,
- shop,
- social likes,
- fake rewards,

because these are outside V1 scope.

---

# 9. Asset QA

Every reusable production asset must be checked for:

- transparent background where required,
- consistent style,
- correct scale,
- no baked checkerboard,
- no unwanted text,
- no excessive glow,
- no inconsistent perspective,
- no visual mismatch with master.
