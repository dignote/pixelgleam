# PixelGleam — Google AI Studio Build Kit V1

## Purpose
This package is the build handoff for implementing PixelGleam in Google AI Studio as a production Android game.

The defining production architecture is **Hybrid Soft Mosaic**:

```text
approved artwork
→ artwork normalization
→ limited semantic palette
→ subject + negative-space masks
→ hidden Cell-ID topology
→ connected-region graph
→ numbered unfinished mosaic
→ one valid tap completes one connected region
→ G02 gem flight / landing
→ authored settled gem texture reveal
```

Do not replace this architecture with a visible square pixel grid, runtime image flood-fill, SVG-only gameplay truth, or AI segmentation at tap time.

## Read order
1. `01_CONTRACT/MOBILE_GAME_FACTORY_MASTER_CONTEXT.md`
2. `01_CONTRACT/PIXELGLEAM_PRD.md`
3. `02_GAMEPLAY_SPECS/GAMEPLAY_SPEC.md`
4. `02_GAMEPLAY_SPECS/GAMEPLAY_INVARIANTS.md`
5. `02_GAMEPLAY_SPECS/ARCHITECTURE.md`
6. `03_MOSAIC_SYSTEM/PIXELGLEAM_HYBRID_MOSAIC_ARCHITECTURE_V1.md`
7. `03_MOSAIC_SYSTEM/PIXELGLEAM_ARTWORK_INPUT_SPEC_V1.md`
8. `03_MOSAIC_SYSTEM/PIXELGLEAM_NEGATIVE_SPACE_TOPOLOGY_RULES_V1.md`
9. `03_MOSAIC_SYSTEM/PIXELGLEAM_SETTLED_GEM_VISUAL_STANDARD_V1.md`
10. `07_QA_GATES/TEST_MATRIX.md`
11. `06_AI_STUDIO_PROMPTS/00_MASTER_BOOT_PROMPT.md`

## Recommended AI Studio workflow
Do not upload the whole package and ask for a finished app in one request.

Use the prompts in numerical order. Each phase has an acceptance gate. A phase marked REWORK or BLOCKED must be fixed before proceeding.

## First build target
The first production target is **Happy Corgi V3 vertical slice**, because it already has frozen production geometry/assets:

```text
Gallery
→ open Happy Corgi
→ select color
→ valid region tap
→ wrong-color tap
→ pan / zoom
→ hint
→ G02 flight / landing
→ settled texture reveal
→ save
→ kill app
→ restore exact state
→ complete artwork
→ reopen completed artwork
```

Only after this vertical slice passes should AI Studio build Daily, My Art, Settings, Tutorial, and the rest of the product shell.
