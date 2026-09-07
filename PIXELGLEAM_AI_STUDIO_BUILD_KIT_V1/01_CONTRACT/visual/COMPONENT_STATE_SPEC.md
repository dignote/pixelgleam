# PixelGleam — COMPONENT STATE SPEC

## 0. Rule

No production component is considered finished with only a default state.

---

# 1. PrimaryButton

States:

- Default
- Pressed
- Disabled
- Loading

Rules:

- primary violet fill,
- white label,
- 52–56dp height,
- 12–14dp radius.

---

# 2. SecondaryButton

States:

- Default
- Pressed
- Disabled

Rules:

- neutral/white fill,
- violet or graphite label,
- visible border.

---

# 3. NumberedColorChip

States:

## Available
- color fill,
- number visible,
- remaining count optionally visible.

## Selected
- strong primary outline,
- selection must be visible without relying only on color.

## Completed
- visually receded,
- optional check state,
- no misleading active styling.

## Disabled
Only if a real disabled condition exists.

---

# 4. ArtworkCard

States:

## New
- artwork thumbnail,
- title,
- New indicator.

## InProgress
- thumbnail,
- title,
- real progress.

## Completed
- thumbnail,
- title,
- completed state.

## Loading
- neutral placeholder.

## Error
- retry only if retry action works.

---

# 5. BottomNavigationItem

States:

- Default
- Selected
- Pressed

No badges unless backed by real state.

---

# 6. IconButton

States:

- Default
- Pressed
- Disabled.

Minimum hit target:

- 44dp.

Visual icon may remain smaller.

---

# 7. ProgressBar

States:

- Empty,
- Partial,
- Complete.

Must reflect actual logical progress.

Never animate fake values.

---

# 8. GameplayCell

States:

## Unfilled
- numbered cell,
- neutral background,
- readable number.

## HintHighlighted
- subtle outline/pulse only.

## Filled
- gem material,
- no number.

## WrongTapFeedback
- temporary visual feedback only,
- no logical mutation.

---

# 9. GemFlightEffect

Presentation-only states:

- Acquired
- Active
- Settling
- Finished
- Released

Must not own gameplay truth.

---

# 10. SettingsToggle

States:

- On,
- Off,
- Pressed,
- Disabled only when technically unavailable.

Toggles must update real Music/SFX/Haptics behavior immediately.

---

# 11. DailyCell / CalendarEntry

States:

- No artwork,
- Available,
- In Progress,
- Completed,
- Today.

Do not suggest online freshness if Daily is local.

---

# 12. CompletionActions

`Next Artwork`

- visible only when a next valid artwork exists.

`Back to Gallery`

- always functional.

`Replay`

- visible only if replay/reset is implemented.
