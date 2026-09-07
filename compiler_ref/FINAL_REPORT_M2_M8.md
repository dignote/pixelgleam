# PixelGleam — Mosaic Topology Compiler V1 Final Execution Report

**Execution scope:** M2 → M8 after M0/M1 golden validation  
**Date:** 2026-08-28  
**Overall status:** **BLOCKED at Factory Gate (M8)** — compiler core is validated on the real Happy Corgi production fixture; four additional approved production benchmark artworks are not present in the supplied source pack.

## Executive result

The core technical hypothesis is validated:

```text
approved visual/semantic mask
→ organic raster tessellation
→ Cell-ID topology
→ adjacency
→ connected same-color Regions
→ exact runtime hit lookup
→ authored settled texture reveal
```

The reconstructed Happy Corgi uses completely new cell boundaries while retaining the approved semantic/color region raster derived from the frozen V3 package.

### Happy Corgi reconstruction

| Metric | Golden V3 | Compiler reconstruction |
|---|---:|---:|
| Cells | 347 | 343 |
| Regions | 43 | 43 |
| Subject coverage | 100% | 100% |
| Uncovered pixels | 0 | 0 |
| Background labeled pixels | 0 | 0 |
| Cell connectivity | PASS | PASS |
| Adjacency exact | PASS | PASS |
| Region connectivity | PASS | PASS |
| Maximal same-color regions | PASS | PASS |
| Settled texture subject match | PASS | PASS |
| Settled background transparency | PASS | PASS |

The 343-cell output is within ~1.2% of the golden 347-cell density and preserves the exact 43-region semantic structure. Exact old Cell-ID equivalence was intentionally not required.

## M2 — Author-assisted masks → Cell-ID topology

**Status: PASS**

Implemented:
- deterministic configuration;
- author-assisted color/semantic raster input;
- proportional per-region cell allocation;
- SLIC-based edge-aware organic raster tessellation;
- exact subject clipping;
- disconnected Cell-ID island repair;
- consecutive Cell-ID compaction;
- adjacency extraction from true shared raster boundaries;
- Region compilation;
- distance-transform cell anchors;
- Cell-ID PNG, subject mask, settled texture and artwork JSON export;
- QA overlays and generation metrics.

Important constraint: M2 deliberately does not infer gameplay topology at runtime.

## M3 — Happy Corgi reconstruction

**Status: PASS**

First run exposed 11 disconnected Cell-IDs. The compiler was corrected rather than relaxing validation. The second run passes all M1 structural checks.

Generation quality metrics:
- target cells: 347;
- generated cells: 343;
- generated regions: 43;
- cell area median: 324 px;
- cell area CV: ~0.396;
- tiny cells detected: 5 (mostly forced by tiny semantic components inherited from the approved semantic raster);
- huge cells: 0;
- median compactness: ~0.561;
- semantic-boundary crossing cells: 0.

Visual review of the boundary overlay confirms irregular/organic cells and no fallback to a square grid.

## M4/M5 — Multi-shape technical stress tests

**Technical status: PASS**  
**Production benchmark status: NOT SATISFIED**

Because the supplied pack contains only the approved Animal/Fur benchmark (Happy Corgi), synthetic deterministic stress fixtures were created for:
- Flower/Botanical;
- Mandala/Repeated geometry;
- Food/Object;
- Human/Character-like narrow features.

All four pass the same structural validator. These fixtures test compiler robustness only and MUST NOT be counted as the production B2–B5 benchmark artworks required by the spec.

## M6 — Automatic segmentation assistance

**Status: REWORK / intentionally not promoted**

An experimental nearest-palette Lab segmentation helper was implemented. On Happy Corgi, naive per-pixel palette classification produces **5,526 connected color components** instead of the approved 43 semantic regions.

Conclusion:

```text
raw automatic palette segmentation
≠ production semantic mask
```

Therefore the original architecture decision is confirmed: automatic vision/segmentation may propose masks, but human approval/correction is required before topology compilation. The compiler should not silently convert raw palette classification into gameplay truth.

## M7 — Runtime integration proof

**Content/runtime contract simulation: PASS**  
**Actual Android/Google AI Studio vertical slice: NOT EXECUTED in this environment**

The runtime simulation loaded the generated package and validated:
- 600 transformed center hit-tests across random zoom/pan transforms: 600 PASS / 0 FAIL;
- Cell-ID lookup after inverse transform;
- correct Region/color lookup;
- wrong-color taps do not mutate progress;
- Region completion is idempotent / no double count;
- exact completedRegionIds save/restore;
- every Region can be completed.

This is not a substitute for Android gesture arbitration, Compose rendering, lifecycle/background behavior, audio, or actual app persistence. Those remain part of the Google AI Studio app vertical slice.

## M8 — Factory Gate

**Status: BLOCKED**

The spec requires five real approved benchmark artworks compiled through one documented pipeline. The source pack currently provides only:

- B1 Animal/Fur: Happy Corgi — PASS.

Missing approved production inputs:
- B2 Flower/Botanical;
- B3 Mandala/Geometric Ornament;
- B4 Food/Everyday Object;
- B5 Human/Character.

The compiler must not claim production factory approval using synthetic stress fixtures.

## Engineering conclusion

The central PixelGleam mosaic architecture is technically viable and should be retained:

```text
Hidden interaction topology = Cell-ID raster
Gameplay grouping          = Region graph
Final visual truth         = settled_texture
Transient gem feedback     = GEM_G02
```

The hardest remaining problem is no longer runtime hit testing. It is **production-quality semantic mask generation at content-factory scale**.

The correct next gate is to create/approve four real benchmark artworks plus their subject/semantic masks, then run this same compiler unchanged as much as possible. Excessive per-artwork tuning should be treated as a compiler-quality warning.

## Files produced

- `src/mosaic_compiler/generator.py`
- `src/mosaic_compiler/reconstruct.py`
- `src/mosaic_compiler/stress_bench.py`
- `src/mosaic_compiler/runtime_sim.py`
- `outputs/happy_corgi_reconstructed_v1/`
- `reports/m3_happy_corgi_reconstruction_validation.json`
- `reports/m4_m5_technical_stress.json`
- `reports/m7_runtime_contract_simulation.json`
- `reports/happy_corgi_golden_vs_reconstructed.png`

## Release decision

**Compiler core:** READY FOR REAL MULTI-ARTWORK BENCHMARK  
**PixelGleam Factory Gate:** BLOCKED pending 4 approved benchmark artworks  
**Do not start mass content production yet.**
