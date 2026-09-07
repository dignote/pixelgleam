# PixelGleam — GAMEPLAY_INVARIANTS

## 0. Purpose

These invariants are non-negotiable gameplay truths.

If implementation behavior violates an invariant, the implementation is wrong even if the screen looks correct.

---

# 1. Artwork topology

### INV-001 — Immutable topology

For a loaded artwork, a cell's:

- x
- y
- colorId
- regionId

never change during play.

### INV-002 — One region per cell

Every playable cell belongs to exactly one region.

### INV-003 — Region color consistency

Every cell in a region has the same `colorId` as the region.

### INV-004 — Orthogonal connectivity

Cells in one region are connected through up/down/left/right adjacency.

Diagonal-only adjacency does not merge regions.

### INV-005 — Region separation

Two cells of the same color that are not orthogonally connected belong to different region IDs.

---

# 2. Resolution

### INV-010 — One accepted tap resolves at most one region

A single tap command can never complete multiple disconnected regions.

### INV-011 — Selected color must match

A region can only be completed by a player tap when:

```text
selectedColorId == tappedRegion.colorId
```

### INV-012 — Completed region is idempotent

Resolving an already completed region changes no logical gameplay data.

### INV-013 — Wrong input never modifies progress

Wrong-color taps and out-of-bounds taps do not change:

- completed regions,
- color progress,
- artwork progress,
- completion state.

### INV-014 — Logical resolution precedes presentation

Once a valid region fill is accepted, logical completion does not depend on the animation finishing.

### INV-015 — Region completion is atomic

A region is logically either:

- incomplete, or
- complete.

There is no persisted partially completed region in v1.

---

# 3. Progress

### INV-020 — Region set is the source of truth

Artwork progress is derived from completed region IDs.

### INV-021 — No duplicate counting

A region ID contributes to completion count at most once.

### INV-022 — Exact color completion

A color is complete iff all regions of that color are complete.

### INV-023 — Exact artwork completion

Artwork is complete iff all artwork regions are complete.

### INV-024 — Percentage is derived

Displayed percentage must be derived from logical progress and never be the source of truth.

### INV-025 — Completion is monotonic

Within a normal playthrough, logical completion only moves forward.

Completed regions do not revert to incomplete unless the user explicitly resets/restarts the artwork.

---

# 4. State machine

### INV-030 — Valid state

Gameplay always has exactly one primary state:

```text
LOADING
PLAYING
PAUSED
COMPLETING
COMPLETED
```

### INV-031 — Fill only in PLAYING

New region-fill commands are accepted only in `PLAYING`.

### INV-032 — Completion transition exactly once

`PLAYING → COMPLETING` occurs at most once per completed artwork session.

### INV-033 — Completed persistence

After `COMPLETED` is persisted, reopening the artwork cannot restore it as incomplete unless the player explicitly resets it.

### INV-034 — No animation-owned state transition

A dropped/cancelled visual effect cannot reverse or create logical gameplay transitions.

---

# 5. Input and transforms

### INV-040 — Transform-correct hit test

The same artwork cell must resolve correctly regardless of current pan/zoom, provided the screen point maps to that cell.

### INV-041 — Out-of-bounds safety

Any screen point mapping outside artwork bounds produces no region-fill mutation.

### INV-042 — Gesture exclusivity

A drag or pinch gesture must not also resolve a tap region from the same pointer sequence.

### INV-043 — No double activation

One physical tap gesture cannot enqueue the same logical region completion twice.

---

# 6. Timing and game loop

### INV-050 — Frame-rate independent logical outcome

At 30, 60, 90, or 120 Hz, the same accepted input sequence produces the same completed-region state.

### INV-051 — Delta-time visual update

Transient movement uses elapsed time rather than fixed pixels-per-frame.

### INV-052 — No required physics tick

Core region-fill correctness does not depend on a rigid-body physics simulation step.

### INV-053 — Idle simulation restraint

When no transient effect requires updating, gameplay does not require continuous physics work.

---

# 7. Motion / lightweight physics

### INV-060 — Target is exact

Every gem placement effect that completes normally ends at its assigned target cell transform.

### INV-061 — Visual path does not change ownership

A flying visual crossing another cell never fills, modifies, or collides logically with that cell.

### INV-062 — No gem-to-gem gameplay collision

Transient gem visuals cannot modify gameplay because of contact with each other.

### INV-063 — Animation cancellation is safe

If an animation is cancelled by navigation, lifecycle change, or performance fallback, already committed gameplay state remains correct.

---

# 8. Object pooling

### INV-070 — Reuse is behaviorally invisible

A pooled object behaves the same as a newly created object after acquisition.

### INV-071 — Complete reset

A released pooled object retains no active gameplay-affecting references or callbacks.

### INV-072 — Pool failure cannot block gameplay

Exhausting a visual-effect pool cannot prevent valid region completion.

### INV-073 — Pool contains presentation objects only

No authoritative artwork progress is owned solely by pooled transient objects.

---

# 9. Persistence

### INV-080 — Committed region survives restart

If a region completion transaction succeeds, reopening the artwork shows that region complete.

### INV-081 — Transients are not persisted

In-flight gem particles, sparkle ages, velocities, interpolation times, and animation callbacks are not part of saved gameplay progress.

### INV-082 — Restore validates IDs

Persisted region IDs not present in the current artwork definition are rejected/ignored safely rather than crashing or corrupting progress.

### INV-083 — Save is artwork-scoped

Progress for artwork A cannot mutate or appear in artwork B.

---

# 10. Hint

### INV-090 — Hint cannot complete

Hint never changes `completedRegionIds`.

### INV-091 — Hint target is unresolved

A hint target must be incomplete at the moment it is selected.

### INV-092 — Hint respects selected color

When a valid unfinished region exists for the selected color, hint targets that color.

---

# 11. Completion effect

### INV-100 — Presentation follows truth

Completion animation starts only after logical artwork completion is true.

### INV-101 — Completion effect is non-authoritative

Skipping, cancelling, or replaying completion animation does not alter completed-region data.

---

# 12. Anti-overengineering invariants

### INV-110 — No general physics dependency

The core fill mechanic must remain functional with all decorative physics/motion effects disabled.

### INV-111 — No general collision world dependency

Pointer hit testing and region resolution must not require a full physics/collision engine.

### INV-112 — No pooled-state authority

Disabling object pooling may reduce performance but must not alter gameplay outcomes.

### INV-113 — State machine remains small

Do not add gameplay states solely to mirror UI components or animation phases.

Add a state only when it changes which gameplay commands are valid.
