# Prompt 00 — Master Boot / Plan Only

Act as a senior Android game engineer, gameplay systems engineer, mobile UI/UX designer, interaction designer, audio designer and production QA engineer.

Read all attached PixelGleam source documents completely before responding.

Treat these documents as authoritative contracts. Do not reinterpret the signature mechanic or replace the mosaic architecture.

## Product architecture to preserve
PixelGleam uses:

```text
approved artwork
→ normalized semantic palette + masks
→ hidden Cell-ID topology
→ connected-region graph
→ numbered unfinished mosaic
→ one valid tap completes one connected region
→ transient G02 gem flight / landing
→ authored settled gem texture reveal
```

The final gameplay truth is NOT the rendered image. The settled texture is NOT used to infer completion. Runtime RGB flood-fill and runtime AI segmentation are prohibited.

## Current task
DO NOT write or modify code yet.

Produce an implementation plan only.

Map every P0 requirement to:

```text
Requirement
→ implementation location/module
→ authoritative state/data
→ affected assets
→ tests
→ acceptance gate
```

Plan phases in this order:
A1 Android foundation
A2 pure region/gameplay logic
A3 Canvas + pan/zoom + Cell-ID hit testing
A4 G02 presentation + settled texture reveal
A5 persistence
A6 Happy Corgi V3 vertical slice
A7 product shell
A8 audio/haptics
A9 visual production
A10 production audit
A11 content-package integration
A12 release readiness

Explicitly list:
- protected files/assets you will not regenerate,
- assumptions,
- missing information that is truly blocking,
- P0 tests to run before expanding features.

Do not add fake product systems.
Do not redesign the game.
Do not build a visible global square-grid architecture.
