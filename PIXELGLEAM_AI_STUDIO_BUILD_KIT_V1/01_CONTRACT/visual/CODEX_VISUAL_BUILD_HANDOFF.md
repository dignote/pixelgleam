# PixelGleam — CODEX VISUAL BUILD HANDOFF

## 0. Required Reading Order

Before modifying code, read completely:

1. `/docs/PIXELGLEAM_PRD.md`
2. `/docs/GAMEPLAY_SPEC.md`
3. `/docs/GAMEPLAY_INVARIANTS.md`
4. `/docs/ARCHITECTURE.md`
5. `/docs/TEST_MATRIX.md`
6. `/docs/visual/UI_MASTER_SPEC.md`
7. `/docs/visual/DESIGN_TOKENS.md`
8. `/docs/visual/SCREEN_SPECS.md`
9. `/docs/visual/COMPONENT_STATE_SPEC.md`
10. `/docs/visual/ASSET_MANIFEST.md`
11. `/docs/visual/VISUAL_SELF_TEST.md`
12. inspect `/reference/PIXELGLEAM_FIGMA_MASTER_CURRENT.png`

Do not treat the reference image as a replacement for the written specifications.

---

# 1. First Task — PLAN ONLY

Do not change production code yet.

Create an implementation plan with this mapping:

```text
Requirement
→ Screen / component
→ Source file(s)
→ State/data dependency
→ Asset dependency
→ Test coverage
→ Visual acceptance criteria
```

Also identify:

- conflicts,
- missing assets,
- ambiguous master details,
- any existing architecture that can be preserved.

Do not invent product features.

---

# 2. Build Order

Implement in this order:

```text
F0 — Design tokens + shared components
F1 — Gameplay
F2 — Home / Gallery
F3 — Category + Artwork Detail
F4 — Artwork Completed
F5 — Daily + My Art
F6 — Settings + Tutorial
F7 — Full visual consistency pass
F8 — Production + screenshot audit
```

Gameplay is the anchor screen.

Do not build every screen in one uncontrolled pass.

---

# 3. F0 — Design System Gate

Implement:

- colors,
- typography,
- spacing,
- radius,
- borders,
- buttons,
- icon buttons,
- color chips,
- progress,
- artwork card primitives.

Gate:

- no unauthorized colors,
- no unauthorized radius scale,
- no glassmorphism,
- no giant rounded cards.

---

# 4. F1 — Gameplay Gate

Must implement and visually match:

- lightweight top bar,
- large artwork canvas,
- readable numbered cells,
- gem-filled cells,
- selected color chip,
- completed color chip,
- progress,
- Hint,
- zoom/pan,
- back/exit.

Must preserve gameplay invariants.

Do not fake visual completion.

---

# 5. Screenshot QA Loop

For each screen family:

```text
build
→ run
→ capture screenshot
→ compare to master/spec
→ record discrepancies
→ fix
→ repeat
```

Check:

- hierarchy,
- proportions,
- spacing,
- typography,
- color,
- radius,
- border,
- icon scale,
- canvas dominance,
- asset fidelity,
- state accuracy.

No phase is PASS because it merely compiles.

---

# 6. Visual Acceptance Tolerances

Use these as practical QA targets:

- major container position/size: within ~4% of intended master proportion,
- spacing uses approved token scale,
- no unauthorized UI element,
- no unauthorized new color,
- no unauthorized large radius,
- primary action hierarchy matches spec,
- gameplay canvas remains visually dominant.

Exact dp may be adjusted responsively by device size.

---

# 7. Structural vs Illustrative Reference

Do not hardcode demo imagery from the master.

The master defines:

- layout grammar,
- hierarchy,
- material direction,
- component styling.

Demo animal artwork is illustrative.

Gameplay data remains authoritative.

---

# 8. Prohibited Codex Behavior

Do not:

- add coin/currency UI,
- add leaderboard/rank,
- add social features,
- add fake favorite/share controls,
- add shop/IAP,
- add analytics panels,
- redesign approved screen hierarchy,
- replace one-tap connected-region logic with per-cell fill,
- make gameplay dependent on animation completion,
- add a physics engine,
- add ECS,
- add architectural layers without requirement.

---

# 9. Required Reporting After Each Phase

Report:

```text
STATUS: PASS / REWORK / BLOCKED

Implemented:
- ...

Preserved:
- ...

Visual QA:
- ...

Gameplay/functional QA:
- ...

Known deviations:
- ...

Files changed:
- ...

Next phase:
- ...
```

Do not proceed to a new visual family if the current family has unresolved blocking deviations.

---

# 10. Final Gate

Before release candidate:

- all P0 gameplay tests PASS,
- all visible controls work,
- settings work and persist,
- audio/haptics work,
- Android Back works,
- exact progress restores,
- no fake features,
- visual audit passes against master,
- no major AI-template patterns remain.


# 11. Mandatory Visual Self-Test

After each implementation family:

1. run the app on the fixed reference device,
2. navigate to required states,
3. capture screenshots,
4. compare against master + visual specs,
5. classify BLOCKER / MAJOR / MINOR,
6. fix all BLOCKER and MAJOR deviations,
7. rebuild and repeat.

Use:

`/docs/visual/VISUAL_SELF_TEST.md`

A phase cannot be marked PASS if:

- BLOCKER > 0,
- MAJOR > 0,
- unauthorized UI > 0,
- token violations > 0,
- gameplay invariant violations > 0.

Do not treat compilation success as visual completion.
