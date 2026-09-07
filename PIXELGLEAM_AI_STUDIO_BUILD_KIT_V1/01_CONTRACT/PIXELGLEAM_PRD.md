# PixelGleam — PRODUCT REQUIREMENTS DOCUMENT (PRD)

**Document:** PIXELGLEAM_PRD.md  
**Product:** PixelGleam  
**Platform:** Android / Google Play  
**Primary language for build prompts:** English  
**Primary discussion language:** Vietnamese  
**Status:** Product definition for implementation planning  
**Scope:** Production-ready V1 mobile game  
**Source basis:** MOBILE_GAME_FACTORY_MASTER_CONTEXT.md, Jewelscapes.txt, GAMEPLAY_SPEC.md, GAMEPLAY_INVARIANTS.md, ARCHITECTURE.md, TEST_MATRIX.md

---

# 0. DOCUMENT ROLE

This PRD is the product-level source of truth for PixelGleam.

It defines:

- what the product is,
- what problem/experience it delivers,
- what the player does,
- which screens and systems belong in V1,
- how progress/content/daily play behave,
- what visual and interaction identity must be preserved,
- what production behaviors are mandatory,
- what is explicitly out of scope,
- what acceptance criteria Codex/AI Studio must satisfy.

This PRD does **not** replace the implementation-facing documents:

```text
PIXELGLEAM_PRD.md
        ↓
GAMEPLAY_SPEC.md
        ↓
GAMEPLAY_INVARIANTS.md
        ↓
ARCHITECTURE.md
        ↓
TEST_MATRIX.md
        ↓
CODEX IMPLEMENTATION PLAN
        ↓
BUILD
```

If a technical implementation decision conflicts with this PRD, the implementation must be changed unless the PRD is explicitly revised.

---

# 1. PRODUCT SUMMARY

## 1.1 Product name

**PixelGleam**

Working concept source: Jewelscapes.

PixelGleam is the production-facing working name for this build unless explicitly renamed later.

## 1.2 Product category

Relaxing casual mobile game combining:

- color-by-number,
- pixel art,
- connected-region filling,
- gem/bead mosaic presentation,
- lightweight collection/progress.

## 1.3 Product promise

PixelGleam turns a numbered pixel canvas into a colorful gem mosaic through a low-friction one-tap interaction.

Instead of requiring the player to tap every pixel individually, the game allows a matching connected region to be completed with one tap.

## 1.4 Signature mechanic

> Select a numbered color, tap a matching pixel/cell, and fill the entire connected region represented by that cell.

This mechanic is the product's primary gameplay differentiator and must remain visually and functionally dominant.

## 1.5 Signature visual transformation

> Numbered pixel grid → gem placement animation → completed jewel-like mosaic artwork.

## 1.6 Session character

PixelGleam should feel:

- relaxing,
- easy to resume,
- visually satisfying,
- low-pressure,
- suitable for short or long sessions,
- clear without a long tutorial.

Default V1 gameplay has no timer, no lives, no score penalty, and no fail state caused by incorrect taps.

---

# 2. PRODUCT GOALS

## 2.1 Primary goals

V1 must:

1. deliver a satisfying one-tap connected-region coloring mechanic,
2. make each successful action visibly transform the artwork,
3. support a scalable library of artwork,
4. preserve exact progress across app restarts,
5. provide a Daily artwork experience,
6. feel commercially designed rather than like a generic AI-generated utility,
7. provide real audio, SFX, haptics, controls, and navigation,
8. be production-ready for Android / Google Play.

## 2.2 Secondary goals

V1 should:

- make unfinished artwork easy to resume,
- make completed artwork feel collectible,
- keep UI secondary to the artwork,
- allow zoom/pan without compromising tap accuracy,
- allow hints without auto-solving the game,
- support future content expansion without rewriting gameplay code.

## 2.3 Non-goals

V1 does not require:

- account/login,
- cloud sync,
- multiplayer,
- online leaderboard,
- online rank,
- user-generated content,
- social feed,
- fake analytics,
- shop,
- coin economy,
- IAP,
- achievements,
- backend-driven Daily system,
- generalized physics engine,
- ECS,
- generalized collision engine,
- custom game-engine framework.

Any future addition must be introduced only when it is actually implemented.

---

# 3. TARGET PLAYER

## 3.1 Intended audience

Source material explicitly positions the experience as relaxing color-by-number / pixel-art content, including artwork intended for adults.

The product therefore targets players who enjoy:

- casual relaxing games,
- color-by-number,
- pixel art,
- decorative mosaic/bead art,
- low-pressure completion,
- collectible artwork.

## 3.2 Player motivation

Primary motivations:

- relaxation,
- visual completion,
- satisfying repeated interaction,
- collecting finished art,
- daily routine.

## 3.3 Skill requirement

Low mechanical skill.

The product challenge is primarily:

- visual searching,
- completion,
- light attention,
- navigation of the canvas.

The product must not introduce unnecessary dexterity difficulty.

---

# 4. CORE GAMEPLAY LOOP

## 4.1 Main loop

```text
Choose artwork
    ↓
Open numbered canvas
    ↓
Select color number
    ↓
Find matching area
    ↓
Tap one matching cell
    ↓
Resolve connected region
    ↓
Gem placement animation
    ↓
Region completes
    ↓
Color/artwork progress updates
    ↓
Repeat
    ↓
Complete artwork
    ↓
Completion reveal
    ↓
Save to completed collection / choose next artwork
```

## 4.2 Core interaction rules

The player must be able to:

- select a numbered color,
- tap a valid matching cell,
- fill the whole connected region,
- zoom,
- pan,
- request a hint,
- leave and resume later.

## 4.3 Wrong input

Wrong-color tap:

- must not change logical progress,
- must not remove progress,
- must not create a punitive fail state,
- may trigger subtle feedback.

## 4.4 Completed region tap

Tapping a completed region:

- must not duplicate completion,
- must not duplicate progress,
- may give subtle acknowledgement.

## 4.5 Completion

Artwork completes only when all regions are logically complete.

Completion must be determined from gameplay state, not from visual animation timing.

---

# 5. CONNECTED-REGION RULE

## 5.1 Region definition

A region is a maximal group of cells that:

- share the same color ID,
- are connected orthogonally:
  - up,
  - down,
  - left,
  - right.

Diagonal-only contact does not merge regions.

## 5.2 One-tap rule

One accepted tap may complete only the specific connected region of the tapped cell.

Two disconnected areas sharing the same number/color must remain separate.

## 5.3 Source of truth

Artwork topology must be represented explicitly through:

- `colorId`,
- `regionId`,
- region membership.

Do not infer completion from rendered appearance.

---

# 6. GAMEPLAY SCREEN REQUIREMENTS

## 6.1 Priority order

Gameplay screen hierarchy:

```text
Artwork canvas
→ current target/color state
→ immediate feedback
→ progress
→ hint / zoom support
→ secondary navigation
```

The canvas is the hero.

The HUD must remain quiet.

## 6.2 Canvas

The gameplay canvas should occupy the majority of the usable screen.

Requirements:

- readable numbered cells,
- completed gem appearance,
- pan,
- pinch zoom,
- reliable tap hit testing at all supported zoom levels.

## 6.3 Palette

The palette must show numbered colors.

Each palette item should communicate:

- color,
- number,
- completion/remaining state.

A completed color may:

- show a completion mark,
- visually recede,
- move later in palette order.

Auto-select-next-color is allowed if behavior remains predictable.

## 6.4 Hint

Default hint behavior:

1. use selected color,
2. locate an unfinished region,
3. guide pan/zoom to it,
4. highlight/pulse that region.

Hint must not auto-complete the region in V1.

## 6.5 Progress

The player should be able to understand:

- overall artwork progress,
- color completion,
- what remains unfinished.

Avoid oversized dashboards or generic stat cards.

---

# 7. GAMEPLAY GESTURES

Required:

- tap → attempt connected-region fill,
- drag → pan,
- pinch → zoom.

Optional:

- double tap → quick zoom.

Gesture arbitration must prevent a drag/pinch from also being interpreted as a fill tap.

---

# 8. GAMEPLAY FEEDBACK

## 8.1 Successful region fill

Recommended feedback sequence:

```text
Tap
↓
region validates
↓
gem visuals travel toward target cells
↓
placement snap
↓
small settle
↓
sparkle / region pulse
```

## 8.2 Visual state vs logical state

Logical completion occurs immediately when the region is accepted.

The animation is presentation only.

The game must remain correct if:

- animation is skipped,
- frame drops occur,
- app backgrounds,
- user navigates away.

## 8.3 Wrong tap feedback

Use restrained feedback:

- subtle wobble,
- brief pulse,
- light warning haptic.

Do not use aggressive red overlays or punitive feedback.

---

# 9. 2D MOTION / PHYSICS SCOPE

## 9.1 Required

PixelGleam needs lightweight visual motion only:

- gem flight,
- arrival,
- optional overshoot,
- settle,
- sparkle,
- completion shimmer.

## 9.2 Not required

Do not build a general 2D physics system.

V1 does not need:

- mass,
- inertia,
- gravity simulation for all gems,
- impulse solver,
- friction,
- rigid-body collisions,
- constraints,
- joints,
- broad-phase collision world.

## 9.3 Requirement

Every gem visual must end at its deterministic target cell.

---

# 10. COLLISION / HIT TESTING SCOPE

The only required gameplay collision concept is pointer-to-cell hit testing.

Pipeline:

```text
screen position
→ inverse pan/zoom transform
→ canvas local
→ grid coordinate
→ cell
→ region
```

Do not implement gem-to-gem collision.

Flying visual gems do not alter gameplay because of overlap.

---

# 11. CONTENT SYSTEM

## 11.1 Artwork categories

Source description includes categories such as:

- animals,
- flowers and nature,
- food and everyday objects,
- people and characters,
- mandala,
- festivals / emoji,
- fantasy,
- relaxing pixel art.

These categories may be refined later, but V1 content architecture must support multiple categories.

## 11.2 Artwork package

Each artwork should be loadable as content rather than hardcoded Kotlin logic.

Conceptual package:

```text
artwork/
    metadata
    thumbnail
    palette
    cell topology
    region topology
```

## 11.3 Scalability requirement

Adding a new artwork should not require editing gameplay engine logic.

## 11.4 V1 content quantity

The source material does not specify an exact artwork count.

Therefore this PRD does **not** invent a mandatory store-launch number.

Implementation must support a scalable library; the final launch quantity is a content-production decision.

---

# 12. DAILY EXPERIENCE

## 12.1 Source requirement

The source concept explicitly includes a Daily page with:

- today's artwork,
- access to previous days,
- a calendar-like history,
- new artwork/themes over time.

## 12.2 V1 implementation direction

V1 may implement Daily fully locally.

Recommended behavior:

```text
calendar date
→ deterministic artwork selection
→ daily completion state
```

No server is required for V1.

## 12.3 Daily screen

Must allow:

- open today's artwork,
- identify completed/uncompleted daily entry,
- view previous daily entries.

Do not expose future content as playable unless intentionally supported.

## 12.4 Daily persistence

Daily completion must survive app restart.

---

# 13. MY ART / COLLECTION

## 13.1 Purpose

Provide a useful place to revisit:

- in-progress artwork,
- completed artwork.

## 13.2 Minimum states

Artwork card/list item should distinguish:

- new,
- in progress,
- completed.

## 13.3 Do not fake

Do not add:

- fake likes,
- fake online stats,
- fake community counts,
- fake rarity,
- fake user rankings.

---

# 14. SCREEN ARCHITECTURE

V1 should contain only useful screens.

## 14.1 Required screens

```text
Splash / launch handling
Home / Gallery
Artwork Detail or direct artwork launch
Gameplay
Artwork Complete / Results
Daily
My Art
Settings
Tutorial / first-use help
```

`Artwork Detail` may be omitted if direct gallery-to-gameplay flow is clearer.

## 14.2 Navigation graph

```text
Launch
  ↓
Home / Gallery
  ├── Artwork → Gameplay
  ├── Daily → Daily Artwork → Gameplay
  ├── My Art → Artwork → Gameplay
  ├── Settings
  └── Tutorial

Gameplay
  ├── Pause / Exit
  └── Complete → Completion screen
                    ├── Next artwork
                    └── Gallery
```

## 14.3 Android Back

Android system Back must work correctly.

No screen may trap the user.

---

# 15. HOME / GALLERY

## 15.1 Purpose

Home is primarily an artwork discovery/resume surface.

Do not turn it into a metrics dashboard.

## 15.2 Content priorities

Suggested order:

1. Resume current artwork, if one exists.
2. Featured/recommended artwork.
3. Categories.
4. Daily entry.
5. My Art access.

Exact composition may evolve during UI design, but gameplay/content remains the focus.

---

# 16. ARTWORK COMPLETION EXPERIENCE

When the final region completes:

1. block duplicate logical completion,
2. show controlled completion reveal,
3. show completed artwork clearly,
4. persist completed state,
5. provide next useful actions.

Useful actions:

- next artwork,
- return to gallery,
- revisit artwork.

Do not expose buttons without real behavior.

---

# 17. DESIGN DNA

## 17.1 Proposed production identity

**Gem Mosaic Atelier**

This is a design proposal derived from the core mechanic.

It is not an explicit phrase from the source description.

## 17.2 Personality

- relaxing,
- tactile,
- crafted,
- cheerful,
- satisfying,
- light.

## 17.3 Visual metaphor

A refined mosaic / bead-art workspace.

## 17.4 Shape language

Use:

- pixel cells,
- square/rectangular grid geometry,
- small faceted gem forms,
- subtle tile/mosaic structure,
- restrained UI containers.

Avoid making the UI look like:

- a generic dashboard,
- a casino,
- a neon cyber game,
- a glassmorphism demo.

---

# 18. THEME AND COLOR SYSTEM

## 18.1 Theme

LIGHT STYLE is required unless explicitly changed later.

## 18.2 Base surfaces

Recommended:

- porcelain white,
- warm ivory,
- paper white,
- very pale neutral gray.

## 18.3 Functional principle

Artwork colors are content.

They must not all be reused decoratively across the app shell.

Keep the UI neutral enough for artwork to dominate.

## 18.4 Proposed accent family

Possible accents:

- coral,
- aqua,
- sunflower,
- green,
- cobalt,
- lavender.

Use accents functionally.

---

# 19. ANTI-AI VISUAL RULES

Do not use:

- excessive glassmorphism,
- cards nested inside cards,
- huge rounded containers,
- pills everywhere,
- purple/cyan AI gradients,
- decorative glow,
- fake technical terminology,
- fake metrics,
- generic 2×2 stat grids,
- oversized headings,
- excessive empty space,
- repetitive screen layouts.

Every major visual element must have a product reason.

---

# 20. MOTION LANGUAGE

Motion should communicate:

- placement,
- completion,
- tactile satisfaction.

Recommended:

- controlled travel,
- snap,
- settle,
- small sparkle,
- subtle shimmer.

Avoid:

- endless looping decorations,
- particle spam,
- strong spring effects everywhere,
- animation that slows gameplay.

---

# 21. AUDIO IDENTITY

## 21.1 Proposed direction

**Soft Gem Workshop**

This is a proposed product direction, not explicit source wording.

## 21.2 Required real audio

Implement:

- background music,
- UI tap,
- navigation,
- palette selection,
- gem placement,
- region completion,
- color completion,
- hint,
- artwork completion,
- subtle wrong-tap feedback.

## 21.3 Audio character

Recommended:

- soft crystalline clicks,
- delicate chimes,
- restrained sparkling tones,
- low-energy ambient/plucked music.

Avoid arcade-heavy audio.

---

# 22. HAPTICS

Required where supported.

Suggested mapping:

- normal valid tap → light,
- region completion → light/medium,
- color completion → medium,
- artwork completion → stronger but restrained,
- wrong tap → subtle warning.

Haptics must obey Settings.

---

# 23. SETTINGS

Minimum real controls:

- Music On/Off,
- SFX On/Off,
- Haptics On/Off.

Settings must:

- change runtime behavior immediately,
- persist across restart.

Do not expose non-functional controls.

---

# 24. STATE PERSISTENCE

PixelGleam must persist meaningful local state.

## 24.1 Artwork progress

Persist:

- artwork ID,
- completed region IDs,
- selected color if desired,
- completed state.

## 24.2 Product state

Persist:

- settings,
- tutorial completion,
- daily history,
- artwork completion,
- in-progress artwork identity.

## 24.3 Restore requirement

If the player exits at 62% progress and reopens the artwork, it must restore the exact logical progress.

Transient visual animations do not need to resume.

---

# 25. OBJECT POOLING

Object pooling exists only as a performance technique for transient effects.

Pool candidates:

- gem flight objects,
- sparkle effects,
- small completion effects.

Do not pool authoritative gameplay state.

Pool failure/exhaustion must never block region completion.

---

# 26. GAMEPLAY STATE MACHINE

Required primary states:

```text
LOADING
PLAYING
PAUSED
COMPLETING
COMPLETED
```

Do not create gameplay states for every animation.

Examples that should remain presentation states, not primary gameplay states:

- gem flying,
- sparkling,
- panning,
- zooming,
- color complete animation.

---

# 27. PERFORMANCE REQUIREMENTS

## 27.1 Functional target

Gameplay must remain responsive for intended V1 artwork sizes.

At minimum, vertical slice testing must cover:

- 32×32 artwork,
- 48×48 artwork,
- multiple disconnected regions,
- large regions.

## 27.2 Large-region strategy

If a region contains many cells:

- logical completion occurs immediately,
- visual effects may be capped,
- effects may be staggered/batched,
- gameplay must not create a heavyweight UI node for every transient gem.

## 27.3 No invented FPS requirement

This PRD does not invent a fixed FPS target without target-device profiling.

The implementation must prioritize:

- responsive input,
- no interaction-blocking stalls,
- stable motion,
- no unbounded allocation growth.

---

# 28. TECHNICAL DIRECTION

## 28.1 Platform direction

Preferred production stack:

- Kotlin,
- native Android,
- Jetpack Compose,
- Compose Canvas or equivalent efficient 2D rendering,
- local persistence,
- coroutines / platform lifecycle.

## 28.2 Gameplay architectural rule

Core gameplay logic must be testable without rendering.

## 28.3 Architecture restraint

Do not introduce architecture solely because it is common in larger games.

Specifically do not add unless a future requirement proves the need:

- custom engine loop thread,
- ECS,
- scene framework,
- physics engine,
- spatial partitioning,
- generalized collision system,
- scripting engine,
- multiplayer state layer.

---

# 29. FUNCTIONAL UI CONTRACT

Every visible interactive element is a promise.

Every visible:

- button,
- icon button,
- tab,
- toggle,
- artwork card,
- hint control,
- palette item,
- pause/resume control,
- retry/reset action,
- back action

must have real behavior.

No:

- TODO callbacks,
- fake screens,
- enabled dead buttons,
- placeholder navigation.

If a feature is unavailable:

- remove it, or
- clearly disable it.

---

# 30. NO FAKE FUNCTIONALITY

Do not expose fake:

- leaderboard,
- cloud sync,
- account,
- community,
- rank,
- shop,
- currency,
- online status,
- network status,
- analytics,
- achievements,
- replay.

Statistics, if later added, must come from real player data.

---

# 31. UI STATES

Relevant controls/components must support appropriate states:

- default,
- pressed,
- selected,
- disabled,
- loading,
- empty,
- completed,
- error where needed.

No control should look active if it cannot be used.

---

# 32. ERROR / EDGE STATES

Required handling:

- artwork load failure,
- invalid/malformed artwork data,
- empty gallery/content state,
- missing/corrupt local save,
- save containing outdated region IDs,
- app background during animation,
- app background during completion,
- small-screen layout,
- long artwork names where applicable.

Malformed artwork topology must not silently produce incorrect gameplay.

---

# 33. TUTORIAL / FIRST USE

Tutorial should explain only what is necessary.

Minimum teaching sequence:

1. select a number/color,
2. tap a matching cell,
3. explain that one tap fills the connected region,
4. show zoom/pan,
5. show hint.

Avoid long text-heavy onboarding.

Tutorial completion must persist.

---

# 34. CONTENT / PROGRESSION MODEL

V1 progression is primarily content completion, not competitive scoring.

Useful states:

```text
NEW
IN_PROGRESS
COMPLETED
```

No artificial XP/rank system is required.

A more complex progression system may be added later only with a clear product purpose.

---

# 35. STORE IDENTITY

## 35.1 Icon direction

The icon should communicate:

- pixel/grid,
- gem/bead,
- transformation/completion.

Requirements inherited from the factory contract:

- 512×512,
- PNG,
- transparent background where appropriate for asset production,
- no tiny text,
- strong silhouette,
- readable at small size.

## 35.2 Feature graphic direction

Recommended concept:

- partially completed numbered pixel mosaic transitioning into a bright finished gem mosaic,
- light background,
- clear hero artwork,
- restrained UI.

The mechanic should be understandable without relying solely on text.

---

# 36. V1 RELEASE SCOPE

V1 includes:

- artwork gallery,
- categories,
- one-tap connected-region gameplay,
- palette,
- zoom/pan,
- hint,
- progress,
- save/resume,
- completed artwork state,
- Daily,
- My Art,
- settings,
- tutorial,
- real audio,
- real SFX,
- real haptics,
- Android Back support,
- production QA.

V1 excludes all non-goals listed earlier.

---

# 37. VERTICAL SLICE REQUIREMENT

Before building full content/UI depth, the implementation must prove one complete production-quality artwork flow.

Test artwork:

- at least 32×32,
- 6–8 colors,
- 20+ connected regions,
- multiple disconnected regions sharing a color,
- at least one large region.

Vertical slice flow:

```text
Gallery
→ open artwork
→ select color
→ fill region
→ wrong tap
→ zoom/pan
→ fill after transform
→ hint
→ save
→ kill/restart app
→ exact restore
→ complete artwork
→ completion reveal
→ reopen completed artwork
```

Do not continue into broad feature expansion while core gameplay is failing.

---

# 38. PRODUCT ACCEPTANCE CRITERIA

PixelGleam V1 is product-complete only when:

## Gameplay

- one-tap connected-region fill is correct,
- disconnected same-color areas do not merge,
- wrong-color taps do not corrupt progress,
- completed regions cannot double-count,
- zoom/pan do not break tap accuracy,
- hints find valid unfinished regions,
- completion occurs exactly once.

## Persistence

- successful fills survive restart,
- exact artwork progress restores,
- completed artwork remains complete,
- settings persist,
- Daily state persists,
- tutorial state persists.

## Visual

- artwork is visually dominant,
- UI is light and restrained,
- gem-placement feedback is satisfying,
- no generic AI dashboard treatment,
- no excessive glass/glow/cards.

## Audio/Haptics

- BGM exists,
- SFX exist,
- haptics exist where supported,
- settings actually mute/enable them,
- lifecycle does not create duplicate audio.

## Navigation

- all visible navigation works,
- pause/resume works,
- Android Back works,
- no dead-end screen.

## Functional completeness

- no dead buttons,
- no TODO handlers,
- no fake production features,
- no blank/placeholder production screens.

## QA

All P0 cases in `TEST_MATRIX.md` must PASS.

---

# 39. IMPLEMENTATION ORDER

Recommended build order:

## P0 — Product/spec lock

Inputs:

- this PRD,
- GAMEPLAY_SPEC,
- GAMEPLAY_INVARIANTS,
- ARCHITECTURE,
- TEST_MATRIX.

Output:

- Codex implementation plan.

## P1 — Artwork topology + pure region logic

Deliver:

- artwork loader/model,
- region lookup,
- progress model,
- unit tests.

## P2 — Core gameplay controller

Deliver:

- selected color,
- tap validation,
- region completion,
- completion evaluation.

## P3 — Canvas interaction

Deliver:

- rendering,
- pan,
- zoom,
- transform-correct hit testing.

## P4 — Feedback

Deliver:

- gem placement,
- sparkle,
- object pooling,
- haptics,
- gameplay SFX.

## P5 — Persistence

Deliver:

- exact save/restore,
- lifecycle safety.

## P6 — Product shell

Deliver:

- Home/Gallery,
- Daily,
- My Art,
- Settings,
- Tutorial,
- navigation.

## P7 — Visual production pass

Deliver:

- Gem Mosaic Atelier identity,
- polished states,
- anti-AI audit.

## P8 — Production audit

Deliver:

- button audit,
- navigation audit,
- persistence audit,
- audio audit,
- P0 test completion.

## P9 — Store/release preparation

Deliver:

- icon,
- feature graphic,
- screenshots,
- Android release readiness.

---

# 40. CODEX HANDOFF RULES

Before writing implementation code, Codex must read:

```text
PIXELGLEAM_PRD.md
GAMEPLAY_SPEC.md
GAMEPLAY_INVARIANTS.md
ARCHITECTURE.md
TEST_MATRIX.md
```

Codex must then produce an implementation plan containing:

```text
Requirement
→ implementation location
→ affected state/data
→ test coverage
→ acceptance gate
```

Codex must not:

- reinterpret the one-tap mechanic,
- replace connected-region logic with per-cell coloring,
- add a physics engine,
- add unused abstractions,
- create fake product systems,
- skip required persistence,
- use UI animation as gameplay truth.

---

# 41. SOURCE-DERIVED VS PROPOSED DECISIONS

## Directly supported by source material

- color-by-number / pixel art concept,
- one tap fills a connected pixel region,
- numbered color selection,
- gem/bead placement visual,
- zoom,
- hint,
- no time pressure,
- artwork categories,
- Daily page/history,
- relaxing experience.

## Directly supported by Mobile Game Factory contract

- gameplay-first UI,
- light style default,
- distinct Design DNA,
- real audio/haptics,
- real persistence,
- every visible control must work,
- no fake functionality,
- navigation/button audits,
- production readiness,
- anti-AI design rules.

## Proposed product decisions in this PRD

The following are design/implementation recommendations derived from those requirements rather than explicit source wording:

- production name PixelGleam,
- Design DNA name “Gem Mosaic Atelier”,
- audio identity “Soft Gem Workshop”,
- exact V1 screen composition,
- local deterministic Daily implementation,
- state names,
- exact suggested technical stack/package responsibilities,
- object-pooling boundaries,
- exact vertical-slice size,
- proposed palette family.

These may be revised without contradicting the original concept, provided the core mechanic and factory contract remain intact.

---

# 42. FINAL PRODUCT TARGET

PixelGleam should ship as:

> A light, tactile, relaxing pixel-gem coloring game where one tap completes a connected numbered region, every action visibly transforms the artwork, progress is safely preserved, and the entire product feels intentionally designed rather than template-generated.

Product success depends first on:

1. correct connected-region gameplay,
2. satisfying gem feedback,
3. frictionless artwork completion,
4. reliable save/resume,
5. scalable content,
6. production completeness.

A polished screenshot is not sufficient.

The game is complete only when gameplay, interaction, persistence, audio, navigation, states, and production QA all work together.
