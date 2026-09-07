# PixelGleam — GAMEPLAY_SPEC

## 0. Purpose

This document is the gameplay contract for PixelGleam.

It intentionally covers only the systems required to make the core mechanic correct, deterministic, testable, responsive, and pleasant:

- game/update loop
- connected-region fill
- input hit testing
- limited 2D motion for gem-placement feedback
- lightweight collision/hit detection
- transient object pooling
- gameplay state machine
- progress / completion rules

It does **not** define app-level UI architecture, backend services, accounts, monetization, cloud sync, leaderboards, shops, ECS, a custom physics engine, or other systems not required by the core game.

## 1. Reference basis

Primary product sources:

- `MOBILE_GAME_FACTORY_MASTER_CONTEXT.md`
- `Jewelscapes.txt`

External technical reference index:

- Build Your Own X: https://github.com/codecrafters-io/build-your-own-x
- Game references indexed there include Handmade Hero.
- Physics references indexed there include:
  - Toptal — Video Game Physics Tutorial
  - Allen Chou — Game Physics Series
  - Tuts+ — How to Create a Custom Physics Engine

### Project interpretation

Build Your Own X is a learning index, not a PixelGleam architecture specification.

The relevant lesson is to understand the loop, motion, collision, and simulation boundaries well enough to implement only what the game needs.

PixelGleam does **not** require a general rigid-body physics engine.

---

# 2. Core gameplay identity

PixelGleam is a relaxing pixel-gem coloring game.

Signature interaction:

> Select a numbered color, tap one matching cell, and fill the entire connected region represented by that cell with animated gem beads.

Signature visual transformation:

> Numbered pixel canvas → completed gem mosaic.

The gameplay is not "tap every pixel individually."

The connected-region one-tap fill is the defining mechanic.

---

# 3. Core loop

## 3.1 Player loop

1. Open an unfinished artwork.
2. Select a numbered color.
3. Find a visible cell matching that color.
4. Tap the cell.
5. Resolve the cell to its connected region.
6. Validate that the selected color matches the region.
7. Fill the entire unresolved region exactly once.
8. Play gem-placement feedback.
9. Update color progress and artwork progress.
10. Persist progress.
11. Continue until no unresolved region remains.
12. Transition to completion.

## 3.2 No time pressure

Default gameplay has:

- no timer,
- no lives,
- no score penalty,
- no fail state caused by incorrect taps.

Incorrect input produces subtle feedback but does not destroy progress.

---

# 4. Artwork data contract

Each artwork MUST contain enough precomputed data to avoid runtime ambiguity.

```text
Artwork
- id
- title
- width
- height
- palette[]
- cells[]
- regions[]
```

## 4.1 Cell

```text
Cell
- x
- y
- colorId
- regionId
```

A cell is immutable with respect to artwork topology.

Completion state is stored separately from the source artwork definition.

## 4.2 Region

```text
Region
- id
- colorId
- cellIndices[]
```

A region is a maximal orthogonally connected set of cells of the same `colorId`.

Default connectivity:

- up
- down
- left
- right

Diagonal-only contact does NOT merge regions.

Example:

```text
A A . A
A A . A
```

The left and right groups are separate regions even if they share the same color.

## 4.3 Progress state

Runtime/persisted progress:

```text
ArtworkProgress
- artworkId
- completedRegionIds
- selectedColorId
- completionState
- updatedAt
```

Do not persist per-frame animation objects.

---

# 5. Region-fill resolution

On tap:

```text
screen position
→ canvas transform
→ grid coordinate
→ cell
→ regionId
→ region
→ validation
→ resolve region
```

## 5.1 Valid tap

A tap is valid when all are true:

- gameplay state accepts input,
- tap resolves to a valid cell,
- cell belongs to the currently selected `colorId`,
- region is not already completed.

Result:

1. mark logical region completed,
2. enqueue visual placement feedback for its cells,
3. update counters,
4. persist logical state,
5. test artwork completion.

Logical completion MUST NOT wait for the final animation frame.

## 5.2 Wrong-color tap

If tapped cell's `colorId != selectedColorId`:

- no cells are changed,
- no region is completed,
- progress is unchanged,
- play subtle non-punitive feedback.

## 5.3 Already-completed tap

If the region is already complete:

- do nothing to gameplay state,
- optional subtle visual acknowledgement,
- never duplicate progress or rewards.

---

# 6. Game/update loop

PixelGleam gameplay is primarily **event-driven**.

A continuous frame loop is needed only while visual motion/animation is active.

Recommended conceptual loop:

```text
receive input/events
→ update gameplay state
→ update active animations with delta time
→ recycle finished transient objects
→ render current state
```

Rules:

- gameplay logic must not depend on device frame rate,
- animation movement must use `deltaTime`,
- logical region resolution happens once per accepted tap,
- persistence is triggered by logical events, not each frame,
- when there is no active animation, the game must not run unnecessary simulation work.

No fixed-step rigid-body solver is required for v1.

---

# 7. 2D motion scope

## 7.1 Required

Only lightweight visual motion is required:

- gem moves toward target cell,
- optional small arrival overshoot,
- short settle,
- sparkle or brightness pulse,
- region completion shimmer.

The movement may use interpolation/easing or simple velocity integration.

## 7.2 Not required

Do NOT implement:

- gravity simulation for every gem,
- rigid-body rotation,
- impulse solver,
- mass/inertia system,
- friction solver,
- continuous physical contacts,
- constraints/joints,
- broad-phase physics world.

These add complexity without improving the core mechanic.

## 7.3 Deterministic destination

Regardless of animation path, every gem ends at the exact center/placement transform of its target cell.

Visual motion can vary; logical placement cannot.

---

# 8. Collision / hit testing

"Collision" in PixelGleam has two intentionally narrow meanings.

## 8.1 Pointer-to-cell hit test — REQUIRED

Convert screen coordinates through the current pan/zoom transform into canvas-local coordinates.

Then:

```text
gridX = floor(localX / cellSize)
gridY = floor(localY / cellSize)
```

Reject when outside artwork bounds.

Touch hit testing must remain correct at every supported zoom and pan position.

## 8.2 Visual gem collision — NOT REQUIRED for v1

Flying gem effects do not need to collide with:

- each other,
- UI,
- canvas borders,
- other completed gems.

They are presentation objects with deterministic destinations.

If visual overlap occurs during flight, that is acceptable unless it materially harms readability.

---

# 9. Input model

Required gestures:

- single tap: attempt region fill,
- drag: pan canvas when zoomed,
- pinch: zoom,
- optional double tap: quick zoom.

## 9.1 Gesture arbitration

A pointer sequence classified as drag/pinch MUST NOT also generate a region-fill tap.

Tap acceptance requires movement below the platform-appropriate touch-slop threshold.

## 9.2 Input lock

Do not globally lock all input while a region animation runs.

A short per-tap debounce is allowed to prevent accidental duplicate activation.

The player should remain able to:

- pan,
- zoom,
- select another color,
- fill another region,

unless a completion transition is in progress.

---

# 10. Object pooling

Object pooling is limited to short-lived objects created in bursts.

Pool candidates:

- flying gem visual instances,
- sparkle particles,
- small completion effects.

Do not pool:

- artwork cells,
- region definitions,
- screen state,
- persisted progress objects,
- ordinary UI elements unless profiling proves a need.

## 10.1 Pool lifecycle

```text
acquire
→ initialize completely
→ activate
→ update/render
→ finish
→ reset completely
→ release
```

A reused object MUST NOT retain:

- old target,
- old velocity,
- old alpha,
- old scale,
- old callback,
- old color,
- old completion flag.

Pool exhaustion may allocate a bounded fallback object or degrade the effect count; it must never block core gameplay.

---

# 11. Gameplay state machine

Use a small explicit state machine.

```text
LOADING
  ↓
PLAYING
  ↓
COMPLETING
  ↓
COMPLETED
```

Optional temporary app-level interruption:

```text
PLAYING ↔ PAUSED
```

## 11.1 LOADING

Allowed:

- load artwork,
- validate data,
- restore progress.

Not allowed:

- region fill input.

Exit condition:

- valid artwork + progress loaded.

## 11.2 PLAYING

Allowed:

- select color,
- tap region,
- pan,
- zoom,
- hint,
- persist progress.

## 11.3 PAUSED

Allowed:

- resume,
- exit.

No new region fills.

## 11.4 COMPLETING

Entered exactly once when all regions are logically completed.

Allowed:

- final completion presentation.

Blocked:

- new region-resolution commands.

## 11.5 COMPLETED

Artwork is finalized.

Allowed:

- continue / return to gallery / replay if product design exposes it.

Progress must remain complete after restart.

---

# 12. Hint behavior

Hint is a gameplay helper, not an auto-solver.

Default behavior:

1. read currently selected color,
2. find an unfinished region of that color,
3. choose a deterministic or nearest suitable region,
4. pan/zoom toward it if needed,
5. pulse/highlight the region briefly.

Hint MUST NOT:

- alter completion state,
- fill cells,
- corrupt selected color,
- create duplicate region completion.

If no unfinished region remains for the selected color, select/indicate the next unfinished color or return a clear no-target state.

---

# 13. Color progression

For each color track:

```text
totalRegions
completedRegions
remainingRegions
```

A color is complete when:

```text
completedRegions == totalRegions
```

Optional convenience:

- visually mark completed color,
- move completed color later in palette order,
- auto-select next unfinished color.

Any automatic color change must happen only after the current region's logical completion is committed.

---

# 14. Artwork completion

Artwork is complete exactly when:

```text
completedRegionIds.size == totalRegionCount
```

Do not infer completion from:

- animation count,
- visual occupancy,
- percentage rounding,
- current selected color.

Completion transition must be idempotent.

Calling completion evaluation multiple times must still produce one completion event.

---

# 15. Save/resume contract

Persist after every successful region completion or equivalent safe batched transaction.

On restore:

- every completed region appears filled,
- every incomplete region remains unresolved,
- selected color may be restored,
- no old flight/sparkle animation resumes,
- completion state is recalculated/validated.

A crash/restart may lose a transient animation, but MUST NOT lose an already committed region completion.

---

# 16. Performance boundaries

Gameplay correctness has priority over decorative density.

For large regions:

- cap simultaneous gem-flight visuals,
- stagger or batch visual emissions,
- immediately commit all logical cells/region state,
- use pooled transient effects.

If a region contains hundreds of cells, the implementation must not create hundreds of heavyweight UI nodes.

Prefer canvas/batched drawing for the artwork.

---

# 17. Explicit non-goals

Do not add these to the gameplay core unless requirements change:

- custom physics engine,
- ECS,
- spatial partitioning tree,
- collision broad phase,
- rigid-body solver,
- entity hierarchy framework,
- scripting engine,
- online authoritative state,
- multiplayer rollback,
- replay simulation,
- procedural artwork generation,
- generalized plugin system.

---

# 18. Definition of Done — vertical slice

One test artwork must support:

- 32×32 or larger grid,
- multiple colors,
- multiple disconnected regions sharing one color,
- color selection,
- correct region fill,
- wrong-color rejection,
- already-completed rejection,
- zoom,
- pan,
- reliable hit test after transformations,
- hint,
- visual gem placement,
- object reuse for transient effects,
- save/restart/restore,
- exactly-once completion transition.

All corresponding cases in `TEST_MATRIX.md` must PASS.
