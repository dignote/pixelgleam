# PixelGleam — Factory Gate 5/5 Benchmark Report

**Status:** PASS (compiler/content-contract gate)  
**Scope:** B1 Happy Corgi + four newly generated production-style benchmark artworks.

## Result table

| Benchmark | Cells | Regions | Structural validator | Runtime contract | Notes |
|---|---:|---:|---|---|---|
| B1 Happy Corgi | 343 | 43 | PASS | PASS | Golden reconstructed fixture |
| B2_flower | 412 | 47 | PASS | PASS | minCell=148, P10compact=0.243 |
| B3_mandala | 459 | 93 | PASS | PASS | minCell=477, P10compact=0.031 |
| B4_food | 359 | 32 | PASS | PASS | minCell=495, P10compact=0.234 |
| B5_character | 398 | 34 | PASS | PASS | minCell=375, P10compact=0.202 |

## Gates verified for B2–B5

- subject coverage = 100%
- uncovered subject pixels = 0
- background-labeled pixels = 0
- Cell-ID set matches artwork.json
- Cell-IDs consecutive
- every Cell-ID is 4-connected
- raster adjacency exactly matches JSON
- region color consistency PASS
- region connectivity PASS
- regions are maximal same-color connected components
- settled texture matches the benchmark master on playable subject pixels
- settled background is transparent
- transform hit-test simulation: 600/600 per artwork, 0 failures
- wrong-color taps do not mutate progress
- region completion is idempotent
- save/restore of completedRegionIds is exact
- all regions are completable

## Benchmark assets

B2: flower / botanical bouquet  
B3: symmetric mandala  
B4: food/object cupcake  
B5: stylized human/character

The artwork masters were generated specifically for this benchmark. Their semantic masks were produced offline with an author-assisted pipeline:
subject extraction → SLIC boundary-aware segmentation → Lab color grouping → micro-component cleanup → topology compiler.
This is not runtime AI segmentation.

## Compiler changes discovered by the benchmark

### 1. Cell-center performance
The old implementation ran a full-canvas distance transform once per Cell-ID. At 1254×1254 this became a significant authoring-time bottleneck.

V1.1 computes the distance transform only inside each cell bounding box.

### 2. Quality-metric performance
Per-cell full-canvas binary erosion was replaced by vectorized shared-edge perimeter accounting.

### 3. Pathological micro-cells
B3/B4 exposed 1–30 px cells created by edge cases after segmentation/tessellation.
V1.1 merges cells smaller than 64 px into a same-region shared-boundary neighbor where possible.

After repair:
- B3 minimum cell area: 477 px
- B4 minimum cell area: 495 px
- B5 minimum cell area: 375 px
- B2 minimum cell area: 148 px

## Visual QA

The generated contact sheet contains, for each B2–B5 artwork:
1. approved benchmark master
2. semantic/color mask
3. Cell-ID boundary overlay

Mandala remains the hardest topology class. Its compactness P10 is low because several intentional ornamental bands are long and narrow. Structural correctness is PASS, but a future visual-quality optimizer should add an elongation-aware allocation rule if the desired final gem style requires every cell to be more bead-like.

## Decision

The previous M8 blocker (only one real benchmark artwork) is cleared for the compiler/content-contract scope.

**Factory Gate B1–B5: PASS.**

This does not yet prove Android/Compose production behavior. App integration must still validate:
- real touch gesture arbitration
- pinch/pan + tap cancellation
- rendering and reveal masks
- lifecycle/backgrounding
- persistence implementation
- frame-time/memory on target devices

## Recommended next phase

Integrate the frozen V1.1 content contract into a Google AI Studio PixelGleam vertical slice using one benchmark artwork first, then rotate through B1–B5 as regression content.
