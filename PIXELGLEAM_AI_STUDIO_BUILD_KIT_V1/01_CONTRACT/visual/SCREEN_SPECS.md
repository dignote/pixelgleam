# PixelGleam — SCREEN SPECS

## 0. Scope

This document specifies the screens visible in the current master plus required supporting screens from the PRD.

---

# S01 — HOME / GALLERY

## Goal

Get the player into artwork quickly.

## Required regions

1. top app identity / settings access,
2. Daily feature,
3. Categories,
4. My Art / resume area,
5. bottom navigation.

## Required controls

- Daily artwork card,
- category cards,
- artwork cards,
- Gallery tab,
- Daily tab,
- My Art tab,
- Settings tab.

## States

- fresh install,
- in-progress artwork exists,
- completed artwork exists,
- no local progress.

## Forbidden

- coin balance,
- premium currency,
- rank,
- leaderboard,
- account avatar,
- fake notification badge.

---

# S02 — CATEGORY

## Goal

Browse artwork within one category.

## Required

- back,
- category name,
- artwork grid,
- per-artwork status.

## Artwork statuses

- New,
- In Progress,
- Completed.

## Card content

- thumbnail,
- title,
- progress/state.

Avoid metadata overload.

---

# S03 — ARTWORK DETAIL

## Goal

Confirm artwork and enter/resume gameplay.

## Required

- back,
- artwork preview,
- artwork title,
- useful compact metadata,
- progress,
- Play or Continue.

## Button rule

Use:

- `Play` for fresh artwork,
- `Continue` for in-progress artwork.

Completed artwork may offer Replay only if reset/replay behavior is fully implemented.

---

# S04 — GAMEPLAY / PLAYING

## Goal

Keep the canvas dominant and make the core mechanic obvious.

## Top bar

- Back,
- artwork title,
- compact overall progress.

## Canvas

Must support:

- numbered cells,
- completed gem cells,
- pan,
- pinch zoom,
- transform-correct tap hit testing.

## Palette

Each color chip shows:

- number,
- color,
- remaining state/count if used,
- selected state,
- completed state.

## Secondary controls

- Hint,
- Zoom if explicit control retained,
- Back / exit.

Do not add:

- Undo unless actual undo system exists,
- score,
- lives,
- timer,
- coins.

---

# S05 — GAMEPLAY / HINT ACTIVE

Not a new navigation destination.

Same gameplay screen with:

- one unfinished region highlighted,
- optional viewport guidance.

Hint must not:

- fill the region,
- change progress,
- change region ownership.

---

# S06 — ARTWORK COMPLETED

## Required

- completed artwork,
- completed state,
- 100% progress,
- Next Artwork,
- Back to Gallery.

Optional:

- replay only if actual reset/replay flow exists.

No fake rewards.

---

# S07 — DAILY

## Required

- current date/month context,
- today's artwork,
- past daily entries,
- completed/incomplete state.

V1 may be fully local/deterministic.

No backend-dependent UI.

---

# S08 — MY ART

## Required

Sections may include:

- In Progress,
- Completed.

Artwork data must come from actual local progress.

---

# S09 — SETTINGS

## Required real controls

- Music,
- SFX,
- Haptics.

These must have immediate runtime effect and persist after restart.

Optional:

- reset tutorial,
- legal/about if actually implemented.

---

# S10 — TUTORIAL / FIRST USE

Teach only:

1. select numbered color,
2. tap matching cell,
3. one tap fills connected region,
4. zoom / pan,
5. hint.

Use visual teaching rather than long text.

Tutorial completion persists.

---

# Cross-Screen Navigation Requirements

```text
Launch
→ Home

Home
→ Category
→ Daily
→ My Art
→ Settings
→ Artwork

Artwork
→ Gameplay

Gameplay
→ Complete

Complete
→ Next Artwork
→ Gallery
```

Android Back must be valid from every screen.
