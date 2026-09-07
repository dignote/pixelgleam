# PixelGleam — DESIGN TOKENS

## 0. Purpose

These tokens reduce visual guesswork during implementation.

Values are implementation targets derived from the current master direction. Fine tuning is allowed only after screenshot QA.

---

# 1. Color Tokens

```text
color.bg.app              = #FBFAF6
color.bg.surface          = #FFFDF9
color.bg.subtle           = #F3EEE5

color.text.primary        = #2D2925
color.text.secondary      = #756E66
color.text.muted          = #8B8176

color.border.default      = #DDD6CD
color.border.strong       = #CFC6BA

color.action.primary      = #6D57DB
color.action.primaryText  = #FFFFFF

color.accent.coral        = #FF7C67
color.accent.aqua         = #66B9C8
color.accent.sunflower    = #F2CA55
color.accent.green        = #70BF7D
color.accent.cobalt       = #65A9E9
color.accent.lavender     = #D986C9

color.state.success       = #70BF7D
color.state.warning       = #F2A64B
color.state.error         = #E85E5E
```

Artwork colors remain content-driven and may exceed this palette.

Do not recolor artwork to match the shell.

---

# 2. Spacing Tokens

```text
space.1 = 4dp
space.2 = 8dp
space.3 = 12dp
space.4 = 16dp
space.5 = 24dp
space.6 = 32dp
```

Default screen horizontal padding:

```text
16dp
```

---

# 3. Radius Tokens

```text
radius.xs = 8dp
radius.sm = 10dp
radius.md = 12dp
radius.lg = 14dp
```

Do not add larger radii without review.

---

# 4. Border Tokens

```text
border.default = 1dp
border.selected = 2–3dp
```

Selected palette chip may use a 2–3dp primary outline.

---

# 5. Typography Tokens

Use Android-accessible scaling.

```text
type.display       = 28sp / 32sp / Bold
type.screenTitle   = 20sp / 24sp / Bold
type.sectionTitle  = 17sp / 22sp / Semibold
type.body          = 15sp / 21sp / Regular
type.label         = 14sp / 18sp / Medium
type.caption       = 12sp / 16sp / Regular
type.numberChip    = 15–16sp / Bold
```

Use tabular or stable-width numerals where possible for progress/counts.

---

# 6. Component Size Tokens

```text
button.height.primary      = 52–56dp
button.height.secondary    = 48–54dp

icon.hitTarget.minimum     = 44dp
palette.chip.visual        = 44–50dp
palette.chip.hitTarget     = 52–56dp

bottomNav.height           = 64–72dp
topBar.height              = 56–64dp
```

---

# 7. Elevation / Shadow

Use minimal elevation.

Preferred:

```text
surface elevation: 0–1
hero artwork/card: 1–2
modal only if required: 2–3
```

Do not simulate material using large blurred colored shadows.

---

# 8. Gameplay Layout Targets

```text
gameplay.canvasHeight:
approximately 75–85% of usable area before secondary controls

gameplay.palette:
compact single horizontal row where feasible

gameplay.secondaryControls:
one compact row

gameplay.progress:
small top-bar or inline indicator
```

---

# 9. Gem Visual Tokens

```text
gem.highlight      = crisp, compact
gem.depth          = subtle
gem.glow           = none / extremely restrained
gem.edge           = clean faceted silhouette
gem.shadow         = soft and short
```

Gem color is driven by artwork palette.

---

# 10. Accessibility

- interactive hit targets >= 44dp,
- selected state cannot rely only on color,
- progress text remains readable,
- number chips require sufficient contrast,
- zoom controls remain reachable one-handed.
