# PixelGleam — ARCHITECTURE

## 0. Architecture principle

Implement the smallest architecture that guarantees the gameplay invariants.

PixelGleam is not a physics game.

It is an event-driven grid/region game with lightweight animated feedback.

Therefore:

- no custom physics engine,
- no general collision engine,
- no ECS,
- no scene graph framework built from scratch,
- no generalized gameplay framework,
- no unnecessary repository/domain/use-case layer multiplication.

The core must remain independently testable.

---

# 1. Technical reference boundary

Build Your Own X indexes learning material for both game construction and physics engines:

- https://github.com/codecrafters-io/build-your-own-x
- Game: Handmade Hero and other from-scratch game resources
- Physics Engine:
  - Toptal rigid-body physics introduction
  - Allen Chou game physics series
  - Tuts+ custom physics engine series

Relevant concepts:

- explicit update/render loop,
- time-based motion,
- separation of logical state from presentation,
- collision/hit-test boundaries,
- deterministic state transitions.

Not adopted:

- full rigid-body simulation,
- general collision response,
- full engine architecture.

Object pooling and gameplay state machines are used here as narrow implementation patterns because PixelGleam has bursty transient effects and mutually exclusive gameplay phases. They are not justification for a larger engine.

---

# 2. Minimal runtime components

```text
GameplayScreen
    |
    v
GameplayController
    |
    +--> ArtworkDefinition
    +--> GameplayState
    +--> RegionResolver
    +--> CanvasTransform
    +--> EffectSystem
    |       \--> VisualEffectPool
    |
    +--> ProgressStore
```

This is conceptual responsibility separation, not a requirement to create a class for every box.

Merge tiny responsibilities when the code is clearer.

---

# 3. ArtworkDefinition

Immutable loaded content.

Responsibilities:

- width/height,
- palette,
- cells,
- regions,
- lookup cell → region,
- validate topology during load/debug tests.

Suggested structures:

```text
cells: flat array indexed by y * width + x
regions: map/array keyed by regionId
```

Avoid runtime flood fill if `regionId` and region membership are already authored/precomputed.

Why:

- deterministic,
- fast,
- simple to test,
- save state references stable region IDs.

Runtime flood-fill may be kept only as an authoring/import validation tool.

---

# 4. GameplayState

Authoritative mutable gameplay data.

```text
GameplayState
- phase
- selectedColorId
- completedRegionIds
- perColorProgress
```

Optional derived values:

- completion percentage,
- remaining regions,
- artwork complete.

Do not store the same truth in many mutable forms.

Preferred source of truth:

```text
completedRegionIds
```

Counters may be cached for performance but must stay derivable.

---

# 5. GameplayController

Owns command handling and state transitions.

Example commands:

```text
SelectColor(colorId)
Tap(screenPoint)
RequestHint
Pause
Resume
ResetArtwork
```

On `Tap`:

```text
if phase != PLAYING: return

cell = transform.screenToCell(point)
if cell invalid: return

region = artwork.region(cell.regionId)

if region.colorId != selectedColorId:
    feedback.wrongTap(...)
    return

if region.id already completed:
    return

commitRegion(region.id)
effects.playRegionFill(region)
progressStore.save(...)
evaluateCompletion()
```

The controller must not wait for individual gem effects.

---

# 6. RegionResolver

Keep this very small.

Responsibilities:

- resolve grid coordinate to cell,
- resolve cell to region,
- validate region/color/completion status.

No physics.

No rendering.

No persistence.

If the logic remains trivial, it may be a pure set of functions rather than a class.

---

# 7. CanvasTransform

Single authority for coordinate conversion.

Responsibilities:

```text
screen ↔ canvas local
canvas local ↔ grid
zoom
pan
bounds
```

This prevents hit-test math from being duplicated between rendering, gesture handling, and hints.

The renderer and hit tester must use the same transform state.

---

# 8. Game/update loop

## 8.1 Logical loop

Gameplay state changes are command/event driven.

No continuously ticking core gameplay simulation is required.

## 8.2 Frame update

Frame updates exist while transient effects are active:

```text
onFrame(deltaTime):
    effectSystem.update(deltaTime)
    requestRender()
```

If using Jetpack Compose, let the platform animation/frame facilities drive visual updates rather than creating a competing permanent engine thread.

Do not build a bespoke Android game thread unless profiling/functionality proves Compose frame driving insufficient.

## 8.3 Logical vs presentation time

Authoritative state:

- instant/event based.

Presentation state:

- time based.

This separation is mandatory.

---

# 9. 2D physics decision

## Decision

Do not integrate or implement a general-purpose 2D physics engine for v1.

## Required approximation

For gem-flight feedback:

```text
position = interpolate(start, target, easedT)
scale = curve(t)
alpha = curve(t)
optional small arrival offset / settle
```

Alternative simple integration is acceptable:

```text
velocity += acceleration * dt
position += velocity * dt
```

but only if it stays deterministic enough for presentation.

No physical response is needed.

## Why

The player does not manipulate dynamic rigid bodies.

The final gem position is predetermined by grid topology.

A physics solver would add:

- unnecessary state,
- timing sensitivity,
- collision complexity,
- harder tests,
- lifecycle risk,

without improving gameplay correctness.

---

# 10. Collision decision

## Required collision system

Only touch hit testing:

```text
screen point
→ inverse canvas transform
→ grid coordinate
→ cell bounds
```

Optional hit tolerance may be used for accessibility, but it must never resolve an adjacent wrong cell ambiguously.

## Not required

- SAT,
- AABB pair checks between gem objects,
- spatial hash,
- quadtree,
- impulse/contact solver,
- continuous collision detection.

If future gameplay introduces moving interactive physical objects, revisit this decision then.

---

# 11. EffectSystem

Presentation-only.

Responsibilities:

- spawn gem-flight effects,
- update active effects,
- spawn sparkle/completion effects,
- release finished effects back to pool,
- apply density/performance caps.

Interface idea:

```text
playRegionFill(region, viewportInfo)
playWrongTap(cell)
playColorComplete(colorId)
playArtworkComplete()
update(dt)
clear()
```

No authoritative progress lives here.

---

# 12. VisualEffectPool

Use one or a few simple pools for bursty transient visual objects.

Example:

```text
GemFlightEffect
- active
- start
- target
- elapsed
- duration
- color
- cellIndex
```

Lifecycle:

```text
acquire()
resetAndConfigure(...)
active = true

...

active = false
reset()
release()
```

Pool size should be bounded.

If a region is larger than available effect capacity:

- keep logical completion immediate,
- visualize a representative capped subset or batch effects over time.

Never allocate one heavyweight view/composable per logical cell solely for a burst animation.

---

# 13. Gameplay state machine

Implement phase as a small enum/sealed type:

```text
LOADING
PLAYING
PAUSED
COMPLETING
COMPLETED
```

Transition table:

| From | Event | To |
|---|---|---|
| LOADING | artwork+progress ready | PLAYING |
| PLAYING | pause | PAUSED |
| PAUSED | resume | PLAYING |
| PLAYING | last region completed | COMPLETING |
| COMPLETING | completion presentation done/skipped | COMPLETED |

Invalid transitions are ignored or rejected explicitly.

Do not create states like:

- GEM_FLYING,
- SPARKLING,
- COLOR_FINISH_ANIMATING,
- PANNING,
- ZOOMING,

because those are presentation/input substates and do not change the fundamental validity of gameplay commands.

---

# 14. Persistence boundary

`ProgressStore` persists only durable logical data.

Minimal API:

```text
load(artworkId)
save(progress)
reset(artworkId)
```

Persist:

- completedRegionIds,
- selectedColorId if desired,
- completion marker,
- last update timestamp if needed.

Do not persist:

- pool objects,
- effect positions,
- animation progress,
- gesture state,
- frame timing.

---

# 15. Rendering

Preferred model:

- artwork drawn in a Canvas/batched surface,
- completed/incomplete cells rendered from `ArtworkDefinition + GameplayState`,
- transient effects rendered as a separate overlay.

Avoid representing every pixel cell as a heavyweight independent UI component.

The exact Android rendering primitive may be selected during implementation, but the architecture must preserve the same state boundaries.

---

# 16. Threading

Default:

- gameplay commands/state mutations serialized on the UI/main coroutine context,
- content loading/storage may use background dispatchers,
- immutable artwork data passed back to gameplay.

Do not introduce locks or concurrent gameplay mutation unless there is a proven need.

---

# 17. Error handling

Invalid artwork topology must fail early during load/development validation.

Examples:

- missing region,
- region with mixed colors,
- out-of-bounds cell,
- duplicate cell assignment.

Runtime save mismatch:

- ignore unknown persisted region IDs safely,
- recompute progress from surviving valid IDs,
- never crash the gameplay screen solely because an old save references removed content.

---

# 18. Suggested package shape

Keep package structure shallow.

```text
gameplay/
    ArtworkDefinition.kt
    GameplayState.kt
    GameplayController.kt
    RegionLogic.kt
    CanvasTransform.kt
    EffectSystem.kt
    VisualEffectPool.kt
    ProgressStore.kt
```

Tests:

```text
gameplay/
    RegionLogicTest.kt
    GameplayControllerTest.kt
    CanvasTransformTest.kt
    EffectPoolTest.kt
    ProgressRestoreTest.kt
```

Do not create extra layers purely to match a template.

---

# 19. Architecture gates

Implementation may proceed only if:

1. region logic works without rendering,
2. rendering works without owning gameplay truth,
3. disabling effects does not break filling,
4. disabling pooling changes performance only, not outcomes,
5. no physics engine is required for core mechanic,
6. all state transitions are explicit,
7. transformed hit tests are deterministic,
8. persistence restores exact completed regions.

---

# 20. Revisit triggers

Only reconsider architecture if one of these becomes a real requirement:

- moving interactive bodies,
- physical collisions affect win/progress,
- hundreds/thousands of simultaneously simulated entities,
- user-generated artwork requires runtime region generation,
- performance profiling proves current rendering inadequate.

Until then, do not add corresponding systems.
