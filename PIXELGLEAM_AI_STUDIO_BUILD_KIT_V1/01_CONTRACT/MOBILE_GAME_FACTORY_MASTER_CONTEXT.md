# MOBILE GAME FACTORY — MASTER CONTEXT & PRODUCTION PLAYBOOK

**Project:** App Game / Mobile Game Factory  
**Purpose:** Portable project context for any new ChatGPT chat, Work workspace, Google AI Studio session, Codex session, or Android Studio continuation.  
**Version:** 1.0  
**Last updated:** 2026-08-11  
**Primary language for discussion:** Vietnamese  
**Recommended prompt language for AI build tools:** English  
**Target platform:** Google Play / Android  
**Project scale:** Multi-app / multi-game factory; current direction is to build and publish many distinct mobile apps/games without repeating the same visual/template DNA.

---

# 0. HOW TO USE THIS FILE

This file is the **single source of truth** for the Mobile Game Factory workflow.

When opening a new ChatGPT chat or Work workspace, attach/paste this file and say:

> Treat `MOBILE_GAME_FACTORY_MASTER_CONTEXT.md` as the project contract and default operating context. Continue from it without asking me to restate the rules. I will give you the next game/app description.

When using Google AI Studio, this file is the source for product requirements, visual direction, prompt-generation rules, anti-AI UI rules, audio requirements, functional-control requirements, persistence rules, production QA, and Google Play release readiness.

When the project evolves, update this file instead of relying on conversation memory.

---

# 1. PROJECT MISSION

The goal is not merely to generate many visually attractive prototypes. The goal is to build **multiple production-ready Android games/apps for Google Play** where each title has a clear mechanic, its own recognizable visual identity, real interaction, real audio/haptics, real persistence, and no fake or dead functionality.

Factory pipeline:

```text
GAME IDEA
   ↓
Mechanic Analysis
   ↓
Product Identity
   ↓
Design DNA
   ↓
Visual Differentiation
   ↓
UX Architecture
   ↓
Gameplay-first UI
   ↓
Design System
   ↓
Implementation
   ↓
Audio + Haptics
   ↓
State Persistence
   ↓
Functional UI Audit
   ↓
Navigation Audit
   ↓
Production Readiness Audit
   ↓
Anti-AI Design Audit
   ↓
Store Assets
   ↓
Google Play Release
```

Production definition:

```text
Production Game
=
Gameplay
+ UI/UX
+ Visual Identity
+ Audio
+ Haptics
+ Navigation
+ Persistence
+ Functional Controls
+ Edge States
+ QA / Production Audit
```

A polished screenshot is not a finished game.

---

# 2. NON-NEGOTIABLE PRINCIPLES

## 2.1 Gameplay first

The core mechanic must visually dominate the gameplay screen. The HUD supports the game rather than competing with it.

Typical priority:

```text
Primary Gameplay Object
→ Critical Gameplay State
→ Immediate Feedback
→ Combo / Score
→ Timer / Progress
→ Secondary UI
```

Do not put the gameplay field inside a decorative giant rounded card unless the mechanic genuinely requires a contained board.

## 2.2 Every game needs distinct Design DNA

Never solve differentiation as:

```text
same template + new title + new accent color
```

Instead:

```text
Mechanic
→ Visual metaphor
→ Shape language
→ Color system
→ Motion language
→ UI density
→ Feedback language
```

Examples:
- Rotation mechanic → orbit, radial ticks, angular markers.
- Final-frame timing → calibration lines, thresholds, frame markers.
- Memory grid → modular grid, reveal/hide rhythm.
- Direction inversion → arrows, chevrons, directional cuts.
- Physics climbing → altitude, route markers, rock/chalk/rope.
- Furniture stacking → studio furniture geometry and physical balance.
- Inflation → pressure, surface tension, expanding forms.
- Moving-gap drop → kinetic architectural slabs and alignment.

## 2.3 Default to light visual styles

Current project preference is **LIGHT STYLE by default** unless explicitly requested otherwise.

Do not automatically use:
- black-heavy backgrounds,
- dark cyberpunk,
- tech-noir,
- dark neon,
- glassmorphism,
- purple/cyan AI gradients,
- futuristic HUD shells.

Preferred bases can include paper white, warm ivory, porcelain white, soft cream, light concrete, pale gray, light steel, or pale atmospheric backgrounds.

---

# 3. ANTI-AI / ANTI-SLOP UI RULES

The recurring AI problem is a UI that is **decorated instead of designed**.

Audit every screen for:
- excessive glassmorphism,
- cards nested inside cards,
- every information group inside a separate card,
- excessive corner radius,
- pill controls everywhere,
- generic dashboard composition,
- default purple/blue gradients,
- glow used instead of hierarchy,
- icons inside decorative circles with no function,
- oversized headings,
- excessive empty space,
- excessive symmetry,
- repeated layouts between screens,
- meaningless technical labels,
- fake metrics,
- fake ping/region/build indicators,
- marketing copy inside gameplay,
- generic 2×2 stats grids,
- decorative charts without meaning,
- fake “neural/system/tactical/matrix/analytics” terminology,
- impressive-looking elements with no product purpose.

Golden visual rule:

> Every major visual decision must be explainable by product function.

Prefer typography, alignment, spacing, contrast, proximity, information hierarchy, composition, mechanic-driven motion and functional color over glass, glow, decorative gradients, arbitrary shadows and giant cards.

Cards are not the default grouping mechanism. Use proximity, typography, dividers and alignment when possible.

Reusable scales:

```text
Spacing:
4 / 8 / 12 / 16 / 24 / 32

Corner radius:
2 / 4 / 8 / 12
```

Do not default to 20–32px radius everywhere.

---

# 4. DESIGN DNA FRAMEWORK

Before building screens, define:

## Product personality
Examples: precise, playful, controlled, athletic, tactile, editorial, rugged, airy, tense, experimental.

## Visual metaphor
Examples: architectural motion, sports instrumentation, editorial poster, modular puzzle, alpine equipment, product studio, signal decay.

## Typography
Define display, heading, body, numeric/score and caption roles.

## Shape language
Examples: square modular cells, clipped corners, circular orbit, architectural slabs, angled directional shapes, pressure rings, furniture silhouettes.

## Functional color roles
Do not merely list colors. Assign each a role: neutral, primary action, safe, warning, perfect, failure, mode identity, rank/reward.

## Motion language
Motion must express the mechanic:
- physics → settle, recoil, weight,
- jelly → squash/bounce,
- rotation → angular/radial motion,
- timing → snap/threshold,
- direction → slide/reversal,
- memory → reveal/hide/sequence,
- pressure → expansion/tremble/pop.

## Must-never-look-like list
Every game should explicitly state visual anti-goals.

---

# 5. GOOGLE AI STUDIO PROMPT FORMULA

Standard full formula:

```text
BuildPrompt(Game)
=
Product Concept
+ Design Identity
+ Color System
+ Visual Language
+ Shape Language
+ Gameplay
+ Modes
+ Screens
+ Motion
+ Audio
+ Haptics
+ State Persistence
+ Functional UI Contract
+ No Fake Functionality
+ Navigation Audit
+ Button Action Audit
+ Production Readiness
+ Anti-AI Review
```

Do not simply prompt “Create a modern beautiful UI.” AI Studio should be instructed to act as a senior mobile game designer, UX designer, interaction designer, audio designer and production Android engineer.

---

# 6. AI STUDIO ITERATION WORKFLOW FOR AN EXISTING APP

Do not ask AI Studio to redesign everything in one pass after a game is already functional.

Use:

```text
Audit
→ High severity fixes
→ Review
→ Medium severity fixes
→ Screen-specific refinements
→ Design consistency pass
→ Production functionality audit
→ Anti-AI final audit
```

## 6.1 Audit-only prompt

```text
Review the entire current UI/UX.

Do NOT modify code yet.

Identify only the most obvious AI-generated design patterns, especially:
- too many cards
- excessive rounded corners
- unnecessary gradients or glow
- generic dashboard layouts
- weak visual hierarchy
- excessive decoration
- inconsistent spacing
- unnecessary text
- fake or non-functional visual information

For each screen, list High and Medium severity issues.

Preserve all existing features, logic and navigation.
```

## 6.2 High-severity fix prompt

```text
Implement only the HIGH severity issues from the audit.

Do not redesign the app from scratch.
Do not change gameplay logic, navigation, ranking logic, analytics logic, or working functionality.

Prefer typography, spacing, alignment, contrast and subtle borders over glass, glow, heavy gradients and excessive rounded containers.

After implementing, summarize exactly what changed screen by screen.
```

## 6.3 Medium-severity pass

```text
Now fix only the MEDIUM severity issues from the audit.

Preserve all improvements already made.
Do not reintroduce excessive cards, gradients, glow, oversized rounded containers, fake labels, or generic dashboard composition.

Do not change app functionality.
```

## 6.4 Consistency pass

```text
Perform a final UI consistency pass.

Normalize:
- spacing
- typography
- corner radius
- border treatment
- colors
- component states
- button hierarchy
- visual density

Do not redesign screens that already work well.
Do not change functionality.
```

## 6.5 Final anti-AI pass

```text
Perform a final anti-AI design review.

Look specifically for:
- repetitive card layouts
- excessive symmetry
- oversized rounded containers
- unnecessary gradients
- decorative glow
- generic icon circles
- excessive empty space
- overexplained UI
- template-like dashboards
- fake technical wording
- visually impressive but functionally unnecessary elements

Fix only clear problems.

Every visual element must have a functional reason to exist.

Do not change app functionality.
```

---

# 7. AUDIO SYSTEM — MANDATORY

A recurring failure of AI Studio is creating Music/SFX/Haptics controls in Settings while not implementing real audio. This is not acceptable.

Implement where appropriate:
- background music,
- UI tap,
- navigation,
- mode selection,
- gameplay action,
- success,
- perfect,
- warning,
- miss,
- failure,
- combo,
- reward,
- rank up,
- game-specific physical interaction.

Examples: hit, drop, slide, impact, bounce, pop, break, rotation reverse, pattern reveal, cell tap, target spawn, timer cue, replay start/pause.

Background music must:
- match the Design DNA,
- remain subtle enough for focus,
- loop cleanly,
- not restart on every screen,
- pause appropriately in background,
- resume according to saved preferences,
- avoid duplicate playback instances.

Settings must have real runtime effects:

```text
Music OFF → background music really stops/mutes.
Music ON → background music resumes.
SFX OFF → UI/gameplay SFX really stop.
SFX ON → SFX return.
Haptics OFF → vibration really stops.
Haptics ON → haptics return.
```

Preferences must persist across app restart.

Audio lifecycle audit:
- no overlapping tracks,
- no duplicate instances,
- no audio leaks after leaving gameplay,
- no incorrect background playback,
- no music restarting every screen.

---

# 8. HAPTICS CONTRACT

Where supported:

```text
Light → normal interaction / target tap / cell tap
Medium → good success
Strong → perfect / major milestone
Warning → mistake / miss / collision / failure
```

Haptics must respect Settings and should not become distracting.

---

# 9. FUNCTIONAL UI CONTRACT

A visible interactive control is a promise to the user.

Every visible interactive element must perform a real action, including Play, Start, mode cards, buttons, icon buttons, navigation items, tabs, toggles, sliders, pause, resume, retry, home, back, tutorial controls, replay controls, rank/statistics navigation, collection items, equipment/upgrade actions and shop actions if present.

Never leave:
- empty callbacks,
- TODO handlers,
- placeholder functions,
- fake responses,
- dead navigation,
- enabled-looking buttons that do nothing.

If a feature is unavailable:

```text
REMOVE IT
```

or:

```text
DISABLE IT CLEARLY + explain the unavailable/locked state.
```

---

# 10. NO FAKE FUNCTIONALITY

Do not expose fake production functionality such as:
- fake leaderboard,
- fake cloud sync,
- fake multiplayer,
- fake login,
- fake online rank,
- fake achievements,
- fake social sharing,
- fake shop,
- fake restore purchase,
- fake analytics,
- fake session history,
- fake replay,
- fake ping,
- fake region,
- fake network status,
- fake currency economy.

If a feature is not implemented, remove it from production UI.

Statistics must come from actual gameplay data. If no data exists, show a real empty state, e.g.:

> No sessions yet. Complete a run to see your performance.

If Coins/currency appear, define how they are earned/spent, persist the balance, actually deduct it, and handle insufficient balance. If that economy is not implemented, remove the currency UI.

---

# 11. STATE PERSISTENCE

Persist appropriate local state:
- Music/SFX/Haptics settings,
- best score,
- best combo,
- best reaction/timing result,
- rank,
- highest level/grid/height/depth,
- mode records,
- tutorial completion,
- unlocked skins,
- equipment,
- local currency if real,
- session history,
- progression.

Meaningful progress should survive app restart.

---

# 12. NAVIGATION AUDIT

Test the game as a graph, not as individual screenshots.

Typical route:

```text
Home
→ Mode Select
→ Gameplay
→ Pause
→ Resume
→ Results
→ Retry
```

Also where present:

```text
Home → Statistics
Home → Rank
Home → Settings
Home → Tutorial
Home → Collection
Home → Equipment
Results → Replay
Round Feedback → Slow-Motion Replay
```

Always test Android system Back. No screen should trap the user or navigate to a blank, placeholder, broken or unfinished screen.

---

# 13. BUTTON ACTION AUDIT

AI must inspect actual code.

Use:

```text
Screen | Control | Expected Behavior | Actual Behavior | Status
```

Every row must end in PASS. If a row is FAIL, fix the implementation before finishing; do not merely report it.

---

# 14. PRODUCTION READINESS RELEASE GATE

Before calling a build complete, verify:

## Stability
- no crashes,
- no freeze,
- no blank screen,
- no broken layout blocking interaction.

## Controls
- no dead buttons,
- no fake toggles,
- no empty callbacks,
- no TODO UI,
- no unreachable feature.

## Gameplay
- every advertised mode works,
- input is reliable,
- scoring works,
- difficulty scaling works,
- pause/resume works,
- retry works.

## Navigation
- all paths work,
- Android Back works,
- no dead-end screen.

## Audio
- music exists,
- SFX exists,
- haptics work,
- toggles are functional,
- lifecycle is correct.

## Persistence
- settings persist,
- records persist,
- progression persists where appropriate.

## States
- empty,
- disabled,
- loading,
- error,
- locked/unlocked,
- first-use,
- max-level/rank,
- long values,
- small-screen behavior.

Principle:

```text
Visual completeness ≠ Production completeness
```

---

# 15. SCREEN ARCHITECTURE DEFAULTS

Normal set:
- Home
- Mode Select
- Gameplay
- Results
- Statistics / Performance
- Rank / Progress
- Settings
- Tutorial

Optional only when useful and implemented:
- Collection
- Equipment
- Level / Route Select
- Slow-Motion Replay
- Records
- Shop
- IAP
- Achievements

Do not add optional screens just because they look good in a mockup.

---

# 16. UI STATE REQUIREMENTS

For relevant components implement Default, Pressed, Selected, Disabled, Loading, Success, Error, Locked and Empty states. Never show an enabled-looking control that cannot be used.

---

# 17. MOTION DESIGN RULES

Motion must communicate state/mechanic.

Examples:
- Timing → marker movement, threshold compression, alignment snap.
- Memory → reveal, hide, sequence rhythm.
- Physics → recoil, settle, sway, collision.
- Rotation → radial movement, angular snap, reversal.
- Direction → slide, snap, counter-motion.
- Inflation → expansion, surface tension, tremble, pop.

Avoid constant decorative loops, particle spam, motion that hides gameplay and excessive spring effects where precision/weight matters.

---

# 18. APP NAMING PROTOCOL

When generating new names:
1. derive from core mechanic + brand identity,
2. avoid generic combinations where possible,
3. check exact-name presence on Google Play/indexed web results,
4. prefer short, memorable and pronounceable names,
5. avoid locking the product to one temporary mode,
6. note that absence from indexed search is not a legal trademark guarantee,
7. do final store/trademark-risk verification before release.

---

# 19. STORE ASSET CONTRACT

## 19.1 Logo/icon concept

```text
512 × 512 px
PNG
transparent background
no text
centered
high contrast
recognizable at small size
```

Avoid tiny details, full scenes, text inside the icon, generic gradient blobs and excessive glow. Motif should directly reference the mechanic.

## 19.2 Feature graphic

```text
1024 × 500 px
PNG
abstract or game-relevant background
clear hero composition
store-friendly
strong silhouette
```

The game name can appear if appropriate, but the image should still communicate the mechanic without relying only on text.

## 19.3 Figma-style concept board

When asked to “render thử ảnh figma”, include:
- major screens,
- color palette,
- typography,
- buttons,
- HUD,
- gameplay states,
- feedback states,
- icons,
- mode indicators,
- rank/progression,
- effects,
- component samples.

## 19.4 Transparent PNG full asset set

When assets should be reusable inside the app, request:
- true transparent alpha background,
- no baked checkerboard,
- no fixed scene background,
- isolated sprites,
- consistent scale,
- enough spacing,
- clean silhouettes,
- cohesive style.

Possible contents:
- gameplay objects,
- UI icons,
- mode icons,
- buttons,
- HUD pieces,
- rank badges,
- status chips,
- feedback effects,
- success/miss states,
- progress elements.

For production usage, individual exported files are preferable to a single presentation sheet when possible.

---

# 20. TECHNICAL DEFAULTS FOR ANDROID / GOOGLE PLAY

Project preference:
- native Android is generally preferred when practical,
- Kotlin + Jetpack Compose is a strong default for app/UI architecture,
- simple 2D gameplay may use Compose Canvas,
- more complex physics/3D can justify a dedicated engine,
- avoid a WebView-only wrapper as final production implementation unless intentional.

Prompts should emphasize production Android behavior, lifecycle, persistence, Android Back, responsive input and release readiness. Do not rewrite an already-stable implementation merely for aesthetics; stability comes before unnecessary architecture churn.

---

# 21. CURRENT GAME / BRAND REGISTRY

This registry prevents visual repetition and naming confusion.

## 21.1 MilliSight

**Previous name/concept:** Reflect Shot  
**Current name:** MilliSight  
**Core mechanic:** reflex + aim + precision training  
**Identity:** precision esports / performance instrument  
**Visual core:** aim, reaction milliseconds, technical training  
**Modes:** Classic, Grid Matrix, Flick Shot, 1-Tap Headshot  
**Logo motif:** eye + target/precision  
**Early AI audit lessons:** excessive glassmorphism; fake “Neural…” labels; fake footer metrics; boxes-in-boxes; noisy scanlines during gameplay.  
**Key rule:** gameplay feedback may be stronger; app shell, analytics and settings should stay restrained.

## 21.2 Jelly Stack: Soft Toy Physics

**Earlier references:** Jelly Tower / Jelly Stack  
**Current store-facing name:** Jelly Stack: Soft Toy Physics  
**Core mechanic:** soft-body stacking physics  
**Identity:** Soft Jelly Toy World  
**Visual core:** tactile soft blocks, squish, bounce, bright physicality  
**Key rule:** gameplay should feel like a toy; result/mode screens must not become utility dashboards.

## 21.3 Depth Illusion: Reflex Tap

**Earlier name:** DepthTrap  
**Current name:** Depth Illusion: Reflex Tap  
**Core mechanic:** judge nearest/farthest despite misleading depth cues  
**Traps:** Size Trap, Blur Trap, Layer Swap  
**Identity:** Optical Depth Lab  
**Palette direction:** light cool neutrals + mist blue + aqua + coral  
**Visual core:** spatial layering, perspective ambiguity, geometric objects  
**Key rule:** challenge comes from perception, not messy UI.

## 21.4 Fast Color: Neuro Reaction

**Earlier name:** Color Lie: Cognitive Reflex Challenge  
**Current name:** Fast Color: Neuro Reaction  
**Core mechanic:** Stroop conflict — color vs word meaning  
**Modes:** Classic, Switch, Chaos, Speed, Mirror  
**Identity:** Editorial Cognitive Lab  
**Visual inspiration:** Swiss graphic design + editorial poster + cognitive test  
**Palette:** warm ivory / black / cobalt / vermilion / mustard / green  
**Key rule:** gameplay colors are functional cognitive signals; do not overuse them decoratively.

## 21.5 Mind Switch: Direction Puzzle

**Earlier name:** Opposite Tap: Reflex Challenge  
**Current name:** Mind Switch: Direction Puzzle  
**Core mechanic:** SAME vs OPPOSITE directional response  
**Modes:** Classic, Switch, Chaos  
**Identity:** Kinetic Signal System  
**Visual core:** arrows, chevrons, directional cuts, kinetic typography  
**Key rule:** do not reduce the game to four arrow buttons + a score card; identity comes from direction/reversal.

## 21.6 Predictive Hue

**Core mechanic:** delayed input affects a future color step; predict instead of react  
**Modes:** Sequence, Entropy  
**Visual core:** predictive color path / future sequence  
**Logo motif:** colored nodes moving forward  
**Key rule:** communicate “input now → effect later” clearly.

## 21.7 Almost Climb: Physics Climbing Challenge

**Core mechanic:** physics climbing with force, direction, balance and stamina  
**Grip types:** Standard, Slippery, Cracked, Moving, Fake  
**Identity:** Alpine Survival Minimalism  
**Palette:** cold stone gray, snow white, rescue orange, moss green, ice blue  
**Visual core:** topographic lines, route markers, chalk, rope, outdoor gear  
**Screens:** Home, Route Select, Gameplay, Results, Equipment, Progress, Settings, Tutorial  
**Key rule:** climbing wall is the screen; UI behaves like instrumentation.

## 21.8 Chair Stack Fail: Physics Stacking Challenge

**Core mechanic:** unstable furniture stacking, delayed settling, collapse  
**Modes:** Classic, Slippery, Wind, optional Challenge  
**Identity:** Playful Modern Workshop  
**Theme:** LIGHT only  
**Palette:** warm cream, terracotta, teal, mustard, sage, sky blue, coral  
**Visual inspiration:** modern furniture studio + Scandinavian interior + tabletop physics  
**Key rule:** physics creates the entertainment, not UI effects.

## 21.9 Inflate Too Much: Reflex Timing Challenge

**Core mechanic:** hold to inflate, release near safe maximum, avoid burst  
**Modes:** Classic, Speed, Chain, Multi, Chaos  
**Identity:** Airy Pop Lab  
**Theme:** LIGHT only  
**Palette:** warm white, sky blue, mint, lemon, apricot, coral, soft violet  
**Visual core:** expanding forms, pressure, tension, surface behavior  
**Key rule:** the object itself should become the primary HUD rather than a generic progress bar.

## 21.10 FastAim: Reflex Precision Challenge

**Core mechanic:** fast target acquisition and precise tapping  
**Modes:** Classic, Precision, Chaos  
**Identity:** Precision Sport Lab  
**Theme:** LIGHT only  
**Palette:** off-white, cobalt, electric orange, lime, sky blue, graphite  
**Visual core:** sports timing, clean targets, reaction metrics  
**Important differentiation:** FastAim = athletic performance trainer; MilliSight = esports/aim performance instrument.

## 21.11 Pattern Burst

**Core mechanic:** brief pattern exposure → memory recall on grid  
**Modes:** CORE static recall; FLOW sequence; VOID decoys; FLASH ultra-short exposure  
**Difficulty:** 3×3 → 4×4 → 5×5 → 6×6  
**Identity:** Modular Memory Studio  
**Theme:** LIGHT only  
**Visual inspiration:** Bauhaus + Swiss grid + modern puzzle systems  
**Palette:** paper white, ultramarine, vermilion, sunflower, mint, lilac, graphite  
**Visual core:** grid + rhythm + sequence  
**Key rule:** avoid the neon “brain trainer” cliché.

## 21.12 NeuroTarget: Spin Loop

**Core mechanic:** targets inside continuously rotating arena  
**Modes:** Classic, Reverse, Chaos, Precision  
**Identity:** Rotational Sports Lab  
**Theme:** LIGHT only  
**Palette:** warm white, cobalt, coral, acid lime, lavender, sky blue, graphite  
**Visual core:** orbit, concentric rings, radial ticks, angle markers  
**Critical rule:** gameplay arena may rotate; HUD remains fixed and readable.

## 21.13 Last Frame Click

**Core mechanic:** tap at the final possible safe moment before target disappears  
**Modes:** Classic, Precision, Chaos, Combo Rush  
**Identity:** Editorial Timing Lab  
**Theme:** LIGHT only  
**Palette:** paper white, ultramarine, signal red, amber, mint, graphite  
**Visual core:** timing tracks, calibration ticks, final-frame markers  
**Key rule:** it is about waiting, not reacting ASAP; tension should increase through timeline state rather than visual noise.

## 21.14 DropVanta

**Earlier name:** Falling Point  
**Current name:** DropVanta  
**Core mechanic:** drop object through moving gaps/barriers  
**Modes:** Classic, Tight Gap, Shift, Double Drop, optional Rhythm  
**Identity:** Kinetic Architecture  
**Theme:** LIGHT only  
**Palette:** chalk white, warm ivory, pale concrete, cobalt, terracotta, saffron, sage, graphite  
**Visual core:** moving slabs, architectural gaps, alignment lines, vertical physics  
**Key rule:** moving structures define identity more than the falling object.

## 21.15 FinalNerve

**Earlier concept/name:** Critical Tap: Last Chance  
**Current name:** FinalNerve  
**Core mechanic:** watch object decay and tap at the final valid moment  
**Signature feature:** real Slow-Motion Replay using actual recorded timing data  
**Modes:** Classic, Last Chance, Fade Shift, Nerve Rush, optional Decoy  
**Identity:** Tension Signal Studio  
**Theme:** LIGHT only  
**Palette:** porcelain white, deep teal, coral, saffron, pale mint, lavender, graphite  
**Visual core:** signal decay, fading objects, final-frame threshold, replay timeline  
**Important differentiation:** Last Frame Click centers on timing track/calibration; FinalNerve centers on visual decay + real replay analysis.

---

# 22. FIXED STORE-ASSET MAPPING

For the standardized first five titles:

1. **MilliSight**
2. **Jelly Stack: Soft Toy Physics**
3. **Depth Illusion: Reflex Tap**
4. **Fast Color: Neuro Reaction**
5. **Mind Switch: Direction Puzzle**

Preserve this mapping when labeling/generating store assets.

---

# 23. KNOWN LOGO MOTIF MAPPING

1. MilliSight → eye + target/precision motif  
2. Jelly Stack → stacked soft jelly blocks  
3. Depth Illusion → layered depth/cube geometry  
4. Fast Color → conflicting colored geometric forms  
5. Mind Switch → opposing arrows

Use as conceptual continuity unless redesign is requested.

---

# 24. FULL PRODUCTION BUILD PROMPT TEMPLATE

```text
Act as a senior mobile game designer, UI/UX designer, interaction designer, audio designer, and production Android engineer.

Your task is to design and build a polished mobile game called:

[GAME NAME]

IMPORTANT:
Use a LIGHT visual style only unless explicitly overridden.

Do NOT use:
- dark theme
- cyberpunk
- neon gamer aesthetics
- glassmorphism
- generic AI dashboard cards
- excessive gradients
- excessive glow
- excessive rounded containers
- fake technical terminology
- decorative controls with no function

==================================================
PRODUCT CONCEPT
==================================================

[Explain the core loop, what the player sees, what they do, what can go wrong, what skill is tested.]

Core skills:
[SKILL 1]
[SKILL 2]
[SKILL 3]

Sessions should be short, clear, replayable and progressively challenging.

==================================================
DESIGN IDENTITY
==================================================

Use this concept:

“[DESIGN DNA NAME]”

Visual inspiration:
[REFERENCE CATEGORIES, not copied products]

Personality:
[ADJECTIVES]

Identity must come from:
[MECHANIC-DRIVEN VISUAL LANGUAGE]

==================================================
COLOR SYSTEM
==================================================

Base:
[LIGHT BASES]

Primary:
[PRIMARY ACCENTS]

Secondary:
[SECONDARY ACCENTS]

Assign functional color roles.
Do not use all colors simultaneously.
Do not use gameplay colors decoratively when that harms comprehension.

==================================================
VISUAL LANGUAGE
==================================================

Use:
[SHAPES / LINES / MOTIFS]

Prefer:
[LAYOUT PRINCIPLES]

Avoid:
[AI-SLOP PATTERNS]

==================================================
GAME MODES
==================================================

[MODE 1]
[MODE 2]
[MODE 3]
[...]

==================================================
SCREEN STRUCTURE
==================================================

Build only useful, functional screens:
Home
Mode Select
Gameplay
Results
Statistics / Performance
Rank / Progress
Settings
Tutorial
[optional screens if the mechanic truly needs them]

==================================================
GAMEPLAY SCREEN
==================================================

Define priority order.
The gameplay mechanic must dominate the screen.
HUD must be lightweight.
Do not frame gameplay inside a giant decorative card.

==================================================
MOTION
==================================================

Motion must express the mechanic.
Avoid decorative looping animation.

==================================================
AUDIO SYSTEM — MUST BE IMPLEMENTED
==================================================

Implement real:
- background music
- UI sounds
- gameplay sounds
- success/perfect
- warning/miss/failure
- combo/reward
- game-specific interaction sounds

Music must loop correctly and respect lifecycle.
Music/SFX/Haptics settings must actually work and persist.

==================================================
HAPTICS
==================================================

Implement meaningful light/medium/strong/warning haptics where supported.
Respect Settings.

==================================================
STATE PERSISTENCE
==================================================

Persist meaningful:
- settings
- best records
- mode records
- progression
- unlocks
- tutorial completion
- session history if present

==================================================
FUNCTIONAL UI CONTRACT
==================================================

Every visible interactive control must work.

No:
- empty callbacks
- TODO handlers
- fake navigation
- active-looking dead buttons
- placeholder screens

Remove or visibly disable unavailable features.

==================================================
NO FAKE FUNCTIONALITY
==================================================

Do not show fake leaderboard, online rank, cloud sync, login, analytics, replay, currency, shop, network status, or other systems unless actually implemented.

==================================================
NAVIGATION AUDIT
==================================================

Test every actual route, pause/resume, retry, back, tutorial and Android system Back.
No dead-end screens.

==================================================
BUTTON ACTION AUDIT
==================================================

Inspect actual code.
For every interactive element verify:
Screen
Control
Expected Behavior
Actual Behavior
Status

Every row must PASS.
Fix failed controls.

==================================================
PRODUCTION READINESS
==================================================

Verify:
- no crashes
- no freeze
- no blank screens
- all modes playable
- scoring works
- persistence works
- audio works
- settings work
- pause/resume works
- retry works
- Android Back works
- no fake metrics
- no dead controls

==================================================
ANTI-AI REVIEW
==================================================

Remove:
- excessive cards
- glassmorphism
- random gradients
- excessive rounded containers
- fake technical labels
- unnecessary decorative effects
- repetitive layouts
- controls with no function

Ask:
Does the mechanic define the visual identity?
Is gameplay dominant?
Does every visible control actually work?
Does the game feel commercially designed rather than AI-generated?

Refine if necessary.

==================================================
FINAL TARGET
==================================================

[3-part product identity statement]
```

---

# 25. SHORT BUILD PROMPT TEMPLATE

```text
Build a polished mobile game called [NAME].

Core mechanic:
[CORE LOOP]

Modes:
[MODES]

Design identity:
“[DESIGN DNA]”

LIGHT THEME ONLY.

Palette:
[COLORS]

Do NOT use:
dark theme
cyberpunk
neon
glassmorphism
generic dashboards
excessive rounded cards

Visual language:
[MECHANIC-DRIVEN MOTIFS]

Gameplay priority:
[ORDER]

Implement REAL:
background music
UI sounds
gameplay SFX
success/failure
combo
haptics

Music/SFX/Haptics toggles must actually work and persist.

Persist:
[MEANINGFUL DATA]

CRITICAL:
Audit every button, tab, toggle and navigation control.

No empty callbacks.
No TODO handlers.
No fake screens.
No fake analytics.
No active-looking control that does nothing.

Test:
Play
Modes
Pause
Resume
Retry
Settings
Tutorial
Android Back
[other real paths]

Final target:
[IDENTITY STATEMENT]

Before finishing, remove anything generic or AI-generated.
```

---

# 26. FINAL PRODUCTION AUDIT PROMPT

```text
Perform a Production Readiness Audit of the entire application.

Do NOT redesign the UI.
Inspect the actual implementation.

1. Find every visible button, icon button, tab, toggle, slider and interactive component.
2. Verify that every one has a real working action.
3. Find and fix empty callbacks, placeholder handlers, TODO actions, fake navigation, buttons pointing to unimplemented screens, and enabled controls that do nothing.
4. Audit audio: real BGM, gameplay SFX, UI sounds, functional Music/SFX/Haptics toggles, persisted settings, correct lifecycle.
5. Test every navigation route and Android system Back.
6. Remove visible mockup/placeholder functionality.
7. Check crash, freeze, blank screen, unresponsive controls, unreachable screens, dead-end navigation, overlapping dialogs, duplicate audio and fake metrics.
8. Produce a final audit table:

Screen | Control | Expected Behavior | Actual Behavior | Status

Every row must end with PASS.
If any row is FAIL, fix the implementation and test again before finishing.
```

---

# 27. FIGMA / ASSET REQUEST TEMPLATES

## 27.1 Figma concept board

> Render a Figma-style concept board for this game showing all major screens and the design system.

Expected: Home, Mode Select, Gameplay, Results, Analytics, Rank, Settings, Tutorial, palette, typography, buttons, HUD, icons, gameplay states, motion/feedback examples.

## 27.2 Transparent full asset set

> Render the full icon and gameplay asset set as PNG with true transparent alpha background, isolated reusable sprites, no baked checkerboard, no scene background, consistent style and spacing.

Expected: gameplay objects, target states, mode icons, nav icons, buttons, badges, rank icons, success/error effects and progress elements.

## 27.3 Logo

```text
512×512
PNG
transparent
no text
single strong motif
centered
high contrast
```

## 27.4 Feature graphic

```text
1024×500
PNG
abstract/game-relevant background
strong hero composition
clear visual identity
```

---

# 28. QUALITY BAR FOR NEW GAMES

Before approving a new concept:

## Product
- Is the mechanic understandable quickly?
- Is there a reason to replay?
- Does difficulty scale through the mechanic rather than clutter?

## Visual
- Does it look different from existing registry entries?
- Is identity mechanic-driven?
- Is the palette genuinely different?
- Is the composition different, not just recolored?

## UX
- Is primary action obvious?
- Is gameplay dominant?
- Is HUD quiet?
- Are result screens compact and meaningful?

## Production
- Does every control work?
- Is audio real?
- Is persistence real?
- Are stats real?
- Are optional features implemented or removed?

## Store
- Does the icon communicate the mechanic at small size?
- Does feature graphic look distinct from other apps?
- Is app name sufficiently distinctive?

---

# 29. UPDATE PROTOCOL

When adding a new game, append this record:

```text
Current name:
Previous/working name:
Core mechanic:
Modes:
Design identity:
Theme:
Palette:
Visual core:
Motion language:
Audio identity:
Signature feature:
Key UX rule:
Important differentiation from similar existing games:
Logo motif:
Feature graphic direction:
```

When a title changes:
- preserve old name as alias,
- mark new name as current,
- update asset mapping,
- update store-facing references.

When a global rule changes:
- update the relevant core section,
- increment version,
- add changelog entry.

---

# 30. CHANGELOG

## v1.0 — 2026-08-11

Created the first comprehensive portable project context containing:
- factory philosophy,
- prompt formula,
- anti-AI UI rules,
- light-theme default,
- Design DNA method,
- AI Studio iteration workflow,
- audio/haptics contract,
- functional UI contract,
- no-fake-functionality policy,
- persistence requirements,
- button/navigation audits,
- production release gate,
- asset specifications,
- naming protocol,
- current game/style registry,
- full/short AI Studio prompt templates,
- final production audit prompt,
- update protocol.

---

# 31. BOOTSTRAP MESSAGE FOR A NEW CHAT / WORK

```text
Use MOBILE_GAME_FACTORY_MASTER_CONTEXT.md as the authoritative project context for this conversation.

Do not ask me to repeat the previously established rules.

For every new game:
1. analyze the mechanic,
2. choose a Design DNA different from the existing registry,
3. use a light style unless I explicitly say otherwise,
4. choose a distinct palette and motion language,
5. write a production-ready Google AI Studio prompt,
6. require real audio/haptics,
7. require real persistence,
8. require every visible control to work,
9. prohibit fake functionality/metrics,
10. include navigation/button audits and production readiness,
11. apply the anti-AI UI rules,
12. preserve the current naming/asset registry.

I will now give you the next game or task.
```

---

# 32. QUICK REFERENCE — ONE-PAGE FACTORY RULE

```text
NEW GAME
   ↓
MECHANIC
   ↓
DISTINCT DESIGN DNA
   ↓
LIGHT PALETTE
   ↓
GAMEPLAY-FIRST UI
   ↓
SMALL DESIGN SYSTEM
   ↓
MECHANIC-DRIVEN MOTION
   ↓
REAL AUDIO + HAPTICS
   ↓
REAL NAVIGATION
   ↓
REAL PERSISTENCE
   ↓
NO FAKE FEATURES
   ↓
BUTTON AUDIT
   ↓
NAVIGATION AUDIT
   ↓
PRODUCTION AUDIT
   ↓
ANTI-AI AUDIT
   ↓
FIGMA / ICONS / STORE ASSETS
   ↓
RELEASE
```

Golden rule:

> Do not optimize for “AI wow.” Optimize for gameplay clarity, distinct identity, functional completeness, consistency and production realism.

Functional golden rule:

> A visible interactive control is a promise to the user. If the app cannot fulfill the action, the control must not appear enabled in production.

Visual golden rule:

> Every major visual decision must be explainable by product function.

Factory golden rule:

> A new game must not be the previous game with a new color palette.

---

END OF MASTER CONTEXT
