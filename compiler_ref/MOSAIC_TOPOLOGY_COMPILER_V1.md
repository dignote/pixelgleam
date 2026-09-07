# PixelGleam — MOSAIC TOPOLOGY COMPILER V1

**Document:** MOSAIC_TOPOLOGY_COMPILER_V1.md  
**Product:** PixelGleam  
**Role:** Offline artwork-to-gameplay topology production specification  
**Status:** Proposed production contract / benchmark plan  
**Version:** 1.0  
**Primary runtime target:** Android / Kotlin / Jetpack Compose  
**Compiler execution:** Offline content-production pipeline only  

---

# 0. PURPOSE

The Mosaic Topology Compiler converts an approved PixelGleam artwork into a deterministic, production-ready gameplay package.

It exists to solve the product's hardest content problem:

> Preserve a soft, authored, high-fidelity completed mosaic while giving every player tap an exact and deterministic gameplay meaning.

The compiler does **not** render gameplay at runtime. It produces authoritative content artifacts that the Android game consumes.

Core separation:

```text
ART ASSET FACTORY
= author visual truth + interaction topology

GAME RUNTIME
= consume frozen topology + run deterministic gameplay
```

The runtime must never need to infer cell boundaries, colors, regions, or artwork semantics from the completed image.

---

# 1. SOURCE-OF-TRUTH HIERARCHY

This specification preserves the existing PixelGleam V3 production decision.

## 1.1 Gameplay topology truth

```text
cell_id_map.png
```

A single integer raster map:

```text
0      = outside playable artwork
1..N   = unique playable Cell-ID
```

Every playable subject pixel belongs to exactly one Cell-ID.

## 1.2 Gameplay data truth

```text
artwork.json
```

Contains at minimum:

```text
artwork ID
canvas dimensions
palette
cell records
region records
cell centers
color IDs
region IDs
adjacency
asset references
```

## 1.3 Settled visual truth

```text
settled_texture.png
```

The approved completed artwork.

The runtime reveals this authored texture through Cell-ID / Region masks after logical completion.

## 1.4 Subject truth

```text
subject_mask.png
```

Defines the exact playable silhouette.

## 1.5 Optional authoring/debug truth

```text
semantic_mask_*.png
numbered_reference.png
cell_contours.json
qa/*.png
```

These support generation and QA but are not runtime gameplay truth unless explicitly promoted.

---

# 2. PRODUCTION ARCHITECTURE

```text
APPROVED MASTER ARTWORK
        │
        ├──────────────► settled_texture.png
        │
        ▼
SUBJECT SEGMENTATION
        │
        ▼
SEMANTIC / COLOR SEGMENTATION
        │
        ▼
EDGE-AWARE SEED GENERATION
        │
        ▼
CONSTRAINED TESSELLATION
        │
        ▼
CELL CLEANUP / MERGE / SPLIT
        │
        ▼
CELL-ID RASTERIZATION
        │
        ▼
ADJACENCY GRAPH
        │
        ▼
CONNECTED SAME-COLOR REGIONS
        │
        ▼
LABEL / CENTER GENERATION
        │
        ▼
NUMBERED REFERENCE
        │
        ▼
STRUCTURAL QA
        │
        ▼
VISUAL / GAMEPLAY QA
        │
        ▼
FROZEN ARTWORK PACKAGE
```

The compiler may use AI-assisted segmentation during authoring, but the final package must be deterministic and frozen.

AI inference is prohibited as runtime gameplay truth.

---

# 3. NON-NEGOTIABLE DESIGN PRINCIPLES

## 3.1 Hidden Interaction Tessellation

The player should not perceive the gameplay topology as a rigid technical grid.

The Cell-ID structure exists underneath the visual artwork.

```text
Visible completed artwork ≠ visible Cell-ID geometry
```

The topology may be irregular and organic while runtime hit testing remains exact.

## 3.2 Logical state is independent of animation

```text
accepted tap
→ logical region complete immediately
→ presentation animation
→ settled texture reveal
```

A dropped animation frame must never corrupt gameplay.

## 3.3 Cell and Region are different concepts

```text
Region
└── one or more Cells
```

A tap resolves a Cell-ID, but the signature mechanic completes the Region.

A Region is a connected component of same-color Cells using orthogonal/shared-boundary adjacency.

## 3.4 Final gem material is authored, not procedurally reconstructed

Approved standalone `GEM_G02` is for:

- flight,
- landing,
- short crystallization feedback.

It is **not** the final settled mosaic renderer.

The final surface comes from `settled_texture.png`.

## 3.5 Runtime simplicity over authoring simplicity

Content compilation may be computationally expensive offline.

Runtime must remain predictable:

```text
tap
→ inverse transform
→ Cell-ID lookup
→ Region lookup
→ validate
→ update state
```

---

# 4. INPUT CONTRACT

The compiler accepts one artwork job.

## 4.1 Required input

```text
input/
  master_artwork.png
  metadata.json
```

`metadata.json` minimum:

```json
{
  "id": "artwork_id",
  "title": "Artwork Title",
  "category": "animals",
  "targetCellCount": 350,
  "targetColorCount": 7
}
```

## 4.2 Preferred input

```text
input/
  master_artwork.png
  subject_mask.png
  semantic_masks/
  numbered_color_reference.png
  metadata.json
```

Author-provided masks override automatically inferred masks after validation.

## 4.3 Optional constraints

```json
{
  "detailZones": [],
  "protectedContours": [],
  "minimumFeatureSizePx": 0,
  "cellDensityOverrides": [],
  "paletteOverrides": []
}
```

These exist for art-direction control, not for runtime.

---

# 5. OUTPUT CONTRACT

A successful compile produces:

```text
output/<artwork_id>/
  artwork.json
  cell_id_map.png
  subject_mask.png
  settled_texture.png
  thumbnail.webp
  qa/
    numbered_canvas.png
    cell_map_debug.png
    region_map.png
    adjacency_debug.png
    semantic_debug.png
    contour_closeup.png
    coverage_heatmap.png
    progress_000.png
    progress_025.png
    progress_050.png
    progress_075.png
    progress_100.png
    qa_report.json
```

Optional:

```text
  semantic_masks/
  authoring_manifest.json
  cell_contours.json
```

No package may be marked production-ready without `qa_report.json`.

---

# 6. PIPELINE STAGES

# C0 — Intake Validation

Validate:

- readable image,
- supported color mode,
- non-zero dimensions,
- alpha/background consistency,
- stable artwork ID,
- no accidental resize between related input masks.

**BLOCKER:** mask dimensions differ from master coordinate system.

---

# C1 — Subject Mask

Goal:

```text
master artwork
→ exact playable silhouette
```

Allowed methods:

- manually authored alpha/mask,
- AI-assisted foreground segmentation,
- threshold/matte extraction when reliable,
- hybrid automatic + human correction.

Final requirement:

```text
subject_mask ∈ {0,1}
```

No uncertain runtime pixels.

QA:

- no holes caused by highlights,
- no accidental background inclusion,
- thin details preserved where important,
- contour visually matches approved master.

---

# C2 — Semantic / Color Segmentation

Goal:

Prevent playable Cells from crossing meaningful artwork boundaries.

Possible semantic classes:

```text
fur orange
fur white
cream
black eye
nose
tongue
collar
tag
background exclusion
```

For flowers:

```text
petal family
center
stem
leaf
accent
```

The segmentation model is artwork-dependent.

The compiler must support both:

```text
semantic class
and
playable colorId
```

They may be the same but are not required to be identical.

Example:

Two visually different semantic fur zones may intentionally share the same numbered palette color while remaining topology-separated.

---

# C3 — Detail Importance Map

Generate a scalar importance field used to allocate cell density.

Higher importance near:

- eyes,
- mouth,
- nose,
- thin petals,
- character facial features,
- small decorative symbols,
- sharp silhouette turns,
- high-curvature contour zones,
- semantic boundaries.

Lower importance in:

- broad flat areas,
- large uniform fur/body zones,
- low-detail fills.

Conceptual formula:

```text
importance =
    semantic_boundary_weight
  + edge_weight
  + contour_curvature_weight
  + authored_detail_zone_weight
  + local_visual_detail_weight
```

The exact weights must be configuration-driven.

---

# C4 — Seed Generation

Generate cell seeds based on target density.

Requirements:

- no seed outside subject,
- no seed on invalid semantic boundary,
- more seeds in high-detail zones,
- fewer seeds in broad low-detail zones,
- deterministic result for a fixed `renderSeed` / compiler seed.

Recommended strategy:

```text
weighted Poisson-disk / blue-noise sampling
+ semantic-mask constraints
+ detail importance field
```

Regular square lattice is prohibited for production artwork.

---

# C5 — Constrained Tessellation

Recommended starting family:

```text
seeded Voronoi / weighted Voronoi / CVT-like partition
```

but the output is not required to remain vector-first.

Critical production rule:

> The authoritative output is the gapless Cell-ID raster, not an ideal mathematical polygon set.

Constraints:

- subject coverage = complete,
- one owner per subject pixel,
- no cell crosses forbidden semantic boundary,
- disconnected fragments with same Cell-ID are prohibited,
- contour-conforming boundary cells,
- topology should visually avoid a repetitive regular grid.

---

# C6 — Cell Cleanup

Automatically repair topology.

## Merge tiny cells

A tiny cell should be merged with a compatible neighbor when it is too small for:

- reliable tap targeting,
- useful numbering,
- readable progression.

Preferred merge candidate:

1. same playable color,
2. same semantic class,
3. longest shared boundary,
4. lowest shape distortion.

## Split oversized cells

An oversized cell may be split when it:

- removes too much interaction rhythm,
- creates an excessively large region chunk,
- makes the mosaic look under-segmented.

## Remove disconnected Cell-ID islands

Each Cell-ID must represent exactly one connected component.

If a raster Cell-ID becomes disconnected:

```text
split into unique Cell-IDs
or
repair ownership
```

Never leave one ID with disconnected islands.

---

# C7 — Boundary Smoothing

Purpose:

Reduce visually mechanical cell boundaries while preserving topology.

The smoothing step may operate on:

- raster ownership boundary,
- vector contour approximation,
- seed relaxation.

It must not:

- create overlap,
- create gaps,
- cross semantic boundaries,
- erase small meaningful features,
- materially alter master silhouette.

Remember:

The completed visual comes from the master texture; topology smoothing primarily improves numbered/unfilled-state readability and interaction feel.

---

# C8 — Cell-ID Rasterization

Generate a single integer label image.

Contract:

```text
background = 0
subject     = 1..N
```

Properties by construction:

- mutually exclusive ownership,
- no overlap representation,
- O(1)-style coordinate lookup,
- exact pan/zoom transform compatibility.

Recommended storage:

- lossless integer-safe format,
- 16-bit when Cell-ID count fits,
- no color-management transform that can change label values.

The implementation must verify that exported/reloaded IDs are bit-exact.

---

# C9 — Adjacency Graph

Derive adjacency from Cell-ID raster boundaries.

Two Cells are adjacent when they share a non-trivial boundary.

Required:

```text
A neighbor B ⇔ B neighbor A
```

Do not derive adjacency only from centroid distance.

Recommended raster derivation:

- scan horizontal label transitions,
- scan vertical label transitions,
- collect unique ID pairs,
- exclude 0/background,
- optionally record shared-boundary length.

Diagonal-only contact does not define connected-region adjacency for gameplay.

---

# C10 — Region Compilation

A Region is:

```text
connected component of same-color Cells
```

Inputs:

```text
Cell adjacency graph
+ colorId per Cell
```

Output:

```text
Region {
  id,
  colorId,
  cellIds[]
}
```

Hard requirement:

Two disconnected same-color areas must remain separate Regions.

This preserves the PixelGleam signature mechanic.

---

# C11 — Cell Centers and Label Anchors

For each Cell compute:

```text
center
labelAnchor
bounds
area
```

The animation target and number label need not be identical.

Preferred label anchor:

- inside the Cell-ID mask,
- maximizes local clearance from boundary,
- stable at normal gameplay zoom,
- avoids awkward placement in thin cells.

A distance-transform maximum is a strong default for label placement.

Centroid may be used only when it is valid and visually suitable.

---

# C12 — Numbered / Unfinished-State Renderer

The numbered canvas should use gameplay data, not the jewel highlights from the settled texture.

Recommended components:

```text
flat/subtle base fill
soft Cell boundary indication
number label
selected-color emphasis
completed-state suppression
```

Rules:

- boundaries must be readable but restrained,
- the canvas must not resemble a rigid spreadsheet/grid,
- semantic shape readability must survive at gameplay zoom,
- numbers must not collide excessively,
- tiny cells may use alternative guidance behavior if numbers are unreadable.

---

# C13 — Progress Preview Generation

Generate deterministic reference renders:

```text
0%
25%
50%
75%
100%
```

Completed regions reveal the corresponding `settled_texture` pixels.

QA must confirm:

```text
100% preview == approved settled visual
```

within exact expected pixel rules after agreed color/alpha handling.

---

# C14 — Gameplay Tap-Forgiveness Metadata

Runtime exact lookup remains primary.

Optional metadata may support a small mobile-friendly correction radius.

Runtime behavior:

```text
1. exact Cell-ID lookup
2. if exact target is invalid/background or ambiguous at an edge:
3. search a small artwork-space neighborhood
4. prefer Cells matching selected color
5. choose nearest valid candidate
```

Constraints:

- correction radius must be small,
- correction must never jump across a large visible distance,
- completed/disabled targets remain respected,
- behavior must be deterministic.

Tap forgiveness improves touch UX; it must not change topology truth.

---

# 7. ARTWORK JSON V1 SCHEMA TARGET

Conceptual structure:

```json
{
  "id": "happy_corgi_v3",
  "title": "Happy Corgi",
  "version": "compiler-1.x",
  "category": "animals",
  "canvas": {
    "width": 365,
    "height": 455
  },
  "topologyType": "MASTER_DERIVED_CELL_ID_MAP",
  "hitTest": "cell_id_map_lookup",
  "settledRender": "preauthored_master_texture_reveal",
  "flightRepresentation": "GEM_G02",
  "palette": [],
  "cells": [],
  "regions": [],
  "assets": {
    "cellIdMap": "cell_id_map.png",
    "subjectMask": "subject_mask.png",
    "settledTexture": "settled_texture.png"
  }
}
```

Cell record target:

```json
{
  "id": 101,
  "center": [120.5, 85.0],
  "labelAnchor": [122.0, 83.0],
  "colorId": 3,
  "regionId": 12,
  "neighbors": [99, 100, 102, 117],
  "bounds": [110, 74, 132, 96],
  "area": 241
}
```

Optional debug contour:

```json
"polygon": []
```

Polygon data is not hit-test truth.

---

# 8. QA SYSTEM

QA is divided into structural, gameplay, visual, and scalability gates.

# 8.1 Structural QA — BLOCKER LEVEL

Must PASS:

```text
Coverage of subject pixels        = 100%
Uncovered subject pixels          = 0
Background pixels with Cell-ID    = 0
Overlapping ownership             = impossible / 0
Invalid Cell-ID                   = 0
Disconnected Cell-ID              = 0
Adjacency symmetry                = 100%
Region connectivity               = 100%
Semantic forbidden crossing       = 0
Settled texture alignment         = PASS
```

Any failure is `BLOCKED`.

---

# 8.2 Cell Quality QA

Measure:

- Cell area distribution,
- minimum cell area,
- maximum cell area,
- aspect/extremeness indicators,
- narrow-neck cells,
- label-anchor clearance,
- contour complexity,
- local density variation.

Do not hardcode one universal aesthetic threshold for all art styles.

Instead define:

```text
hard safety thresholds
+ artwork-class target bands
```

Example:

Character faces require stricter detail preservation than broad mandala fills.

---

# 8.3 Gameplay QA

Automated tests:

```text
tap cell center
→ resolves expected Cell-ID

tap after scale/pan transform
→ resolves same Cell-ID

select correct color + tap
→ completes exactly expected Region

wrong color + tap
→ no logical change

completed Region + tap
→ no double count

same-color disconnected Regions
→ remain separate

hint target
→ always unfinished valid Region
```

Fuzz test random valid subject points and compare lookup consistency.

---

# 8.4 Visual QA

Required review artifacts:

- numbered canvas,
- Cell-ID debug map,
- Region map,
- semantic map,
- contour close-ups,
- 0/25/50/75/100 progression,
- exact 100% settled reference,
- zoomed face/detail review where applicable.

Human review questions:

1. Does the artwork remain recognizable in numbered state?
2. Does topology feel organic rather than square-grid mechanical?
3. Are important facial/semantic features protected?
4. Are any Cells too small to interact with comfortably?
5. Are any Cells so large that the mosaic interaction becomes dull?
6. Does 100% exactly recover the approved master?
7. Do progression reveals look continuous without seams?

---

# 8.5 Runtime Performance QA

Compiler output should permit runtime to avoid expensive geometry work.

Benchmark:

- Cell-ID map load,
- tap lookup,
- Region lookup,
- reveal-mask update,
- save/restore identifiers.

No runtime point-in-polygon requirement for primary hit testing.

---

# 9. BENCHMARK ARTWORK SET

Do not approve the compiler using Happy Corgi alone.

V1 must be tested across five structurally different artwork classes.

## B1 — Animal / Fur

Representative structure:

- soft silhouette,
- eyes/nose detail,
- broad fur zones,
- curved ears,
- small facial features.

Reference class:

```text
Happy Corgi V3
```

Benchmark objective:

- preserve expression,
- avoid blocky face,
- variable density.

Existing production reference:

```text
347 Cells
43 Regions
Coverage 100%
Settled exact-master match PASS
```

This becomes the regression baseline.

---

## B2 — Flower / Botanical

Stress cases:

- thin petals,
- overlapping shapes,
- narrow stems,
- leaves,
- repeated curved structures.

Objective:

- prevent petal bridging,
- retain narrow stem topology,
- preserve petal separation.

---

## B3 — Mandala / Geometric Ornament

Stress cases:

- repeated radial symmetry,
- many small adjacent shapes,
- precise boundaries,
- high repetition.

Objective:

- avoid accidental region merging,
- maintain symmetry without creating a visually boring regular grid,
- test large Cell count.

---

## B4 — Food / Everyday Object

Stress cases:

- simple broad shapes,
- highlights,
- multiple material zones,
- moderate detail.

Objective:

- verify compiler does not over-segment simple artwork,
- preserve efficient content scale.

---

## B5 — Human / Character

Stress cases:

- face,
- eyes,
- mouth,
- hair strands/clusters,
- clothing/accessories,
- high semantic sensitivity.

Objective:

- prove detail allocation,
- avoid facial distortion,
- protect thin meaningful features.

This is expected to be the hardest quality benchmark.

---

# 10. BENCHMARK PASS POLICY

Compiler V1 is not production-approved until all five benchmark classes meet:

```text
BLOCKER = 0
MAJOR structural issue = 0
Coverage = 100%
Background labeling = 0
Disconnected Cell-ID = 0
Adjacency symmetry = 100%
Region connectivity = 100%
Settled master match = PASS
Tap transform tests = PASS
Persistence ID stability = PASS
```

Additionally:

```text
3/5 benchmark artworks minimum
must achieve visual approval without manual topology repainting.
```

The remaining two may permit limited author correction in V1, but all corrections must be fed back into compiler heuristics/configuration.

Long-term factory target:

```text
5/5 automatic or semi-automatic PASS
with no hand-edited Cell-ID raster.
```

---

# 11. AUTO / ASSISTED / MANUAL BOUNDARIES

V1 should explicitly support three modes.

## AUTO

```text
master
→ compiler
→ QA
```

Use when confidence is high.

## ASSISTED

Human supplies:

- subject mask,
- semantic masks,
- protected detail zones,
- density hints.

Compiler still produces final topology.

This should be the default production mode during V1 development.

## MANUAL OVERRIDE

Allowed only for:

- correcting segmentation,
- marking semantic boundaries,
- adjusting protected zones,
- forcing merge/split constraints.

Avoid direct painting of final Cell-ID maps as normal workflow because it prevents scalability.

---

# 12. DETERMINISM AND VERSIONING

Every compile must record:

```text
compilerVersion
algorithmVersion
configuration hash
input artwork hash
input mask hashes
random seed
output hash
```

Same inputs + same compiler/configuration must produce the same authoritative topology.

Artwork IDs must remain stable across game updates unless topology intentionally changes.

If topology changes after release, save migration must be explicitly addressed.

---

# 13. SAVE-COMPATIBILITY RULE

Runtime persists `completedRegionIds`.

Therefore a released artwork topology is effectively versioned gameplay content.

Production rule:

```text
Never silently regenerate and replace region IDs for a shipped artwork.
```

Options for topology change:

1. keep existing version,
2. publish new artwork version/ID,
3. supply explicit old→new Region migration.

Compiler must support stable deterministic ID assignment when inputs remain unchanged.

---

# 14. ANDROID RUNTIME HANDOFF

Google AI Studio / Codex implementation must consume compiler output as follows.

## Load

```text
load artwork.json
load cell_id_map
load settled_texture
restore completedRegionIds
```

## Tap

```text
screen point
→ inverse canvas transform
→ artwork-local x/y
→ Cell-ID lookup
→ optional small tap forgiveness
→ cell.colorId / regionId
→ gameplay validation
```

## Completion

```text
mark Region logically complete
→ persist Region ID
→ play G02 flight/landing presentation
→ reveal settled_texture through Region mask
```

## Prohibited runtime behavior

- infer Region by flood-filling the visual texture,
- recolor based on sampled jewel highlight color,
- generate Voronoi topology on device,
- run AI segmentation for player taps,
- use vector polygon hit testing as primary truth,
- leave standalone G02 sprites as settled artwork.

---

# 15. COMPILER IMPLEMENTATION MODULES

Recommended decomposition:

```text
mosaic_compiler/
  intake/
  segmentation/
  importance/
  seeds/
  tessellation/
  topology/
  regions/
  labels/
  rendering/
  qa/
  export/
  config/
  tests/
```

Possible responsibilities:

## segmentation

- subject mask,
- semantic masks,
- validation.

## importance

- semantic boundary distance,
- edge/curvature maps,
- density map.

## tessellation

- seed placement,
- constrained assignment,
- cleanup.

## topology

- Cell-ID raster,
- connectivity,
- adjacency.

## regions

- same-color connected components.

## labels

- centers,
- maximum-clearance label anchors.

## qa

- structural assertions,
- debug renderers,
- benchmark runner.

## export

- artwork.json,
- PNG assets,
- QA report.

---

# 16. RECOMMENDED DEVELOPMENT ORDER

## M0 — Freeze Happy Corgi V3 as golden fixture

Do not regenerate it yet.

Use its existing V3 package as expected-behavior reference.

Deliver:

- test fixture loader,
- QA metric reader,
- expected 347 Cells / 43 Regions baseline,
- visual reference snapshots.

Gate:

`PASS` existing package exactly.

---

## M1 — Compiler skeleton + validation

Deliver:

- CLI/job runner,
- input/output structure,
- deterministic configuration,
- QA report schema.

No automatic segmentation requirement yet.

---

## M2 — Author-assisted masks → Cell-ID topology

Start with human-approved:

- subject mask,
- semantic/color masks.

Implement:

- seed generation,
- raster tessellation,
- connectivity repair,
- adjacency,
- Regions,
- centers,
- export.

This isolates the topology problem from AI segmentation quality.

---

## M3 — Happy Corgi reconstruction experiment

Run compiler on Corgi source/masks.

Do not require exact 347-cell equivalence.

Require:

- same visual/semantic quality class,
- zero structural errors,
- comparable gameplay density,
- exact settled texture recovery.

Compare new output against approved V3 baseline.

---

## M4 — Benchmark B2/B3

Add:

- Flower,
- Mandala.

Tune:

- density map,
- tiny Cell repair,
- narrow-feature handling,
- repeated-pattern behavior.

---

## M5 — Benchmark B4/B5

Add:

- Food/Object,
- Human/Character.

Tune adaptive detail system.

Human/Character becomes quality ceiling test.

---

## M6 — Automatic segmentation assistance

Only after topology compiler is stable.

Add optional:

```text
AI/vision segmentation
→ proposed masks
→ QA
→ human approval/correction
→ topology compiler
```

Do not entangle segmentation-model quality with core topology code.

---

## M7 — Runtime integration proof

Google AI Studio builds one minimal PixelGleam vertical slice consuming compiler output.

Required proof:

```text
Gallery
→ artwork load
→ numbered canvas
→ select color
→ tap
→ Region completion
→ zoom/pan
→ tap after transform
→ save
→ restart
→ exact restore
→ complete
→ exact master reveal
```

---

## M8 — Factory gate

Compile all five benchmark artworks using one documented pipeline.

Output one comparative report.

Only after this gate should the project scale content production.

---

# 17. FAILURE CLASSIFICATION

## BLOCKED

Examples:

- coverage <100%,
- background mislabeled,
- disconnected Cell-ID,
- incorrect Region graph,
- save-ID instability,
- settled texture mismatch,
- tap resolves wrong topology after transform.

Do not proceed.

## REWORK

Examples:

- topology technically valid but face distorted,
- too many tiny Cells,
- overly coarse broad regions,
- numbered canvas unreadable,
- visible regular-grid feeling,
- poor mobile tap ergonomics.

Tune compiler and repeat.

## PASS

Structural QA + gameplay QA + visual review all pass.

---

# 18. REPORT FORMAT AFTER EACH COMPILER RUN

Every run should end with:

```text
STATUS: PASS / REWORK / BLOCKED

Artwork:
Compiler version:
Input hash:
Seed:

Cells:
Regions:
Colors:

Coverage:
Background labeling:
Disconnected cells:
Adjacency symmetry:
Region connectivity:
Settled master match:

Minimum Cell area:
Median Cell area:
Maximum Cell area:
Label-anchor failures:

Gameplay transform test:
Tap forgiveness test:

Visual QA:
- silhouette
- detail preservation
- density
- numbered readability
- progression continuity

Files produced:

Required rework:

Next step:
```

---

# 19. HAPPY CORGI V3 REGRESSION CONTRACT

Current approved V3 baseline must remain protected.

Known approved properties:

```text
Topology: Master-Derived 16-bit Cell-ID map
Hit test: Cell-ID lookup
Settled visual: exact approved master texture
Flight representation: GEM_G02
Color source: numbered master reference
Cells: 347
Regions: 43
Coverage: 100%
Uncovered subject pixels: 0
Overlap: 0
Background-labeled pixels: 0
Cell connectivity: PASS
Adjacency symmetry: PASS
Region connectivity: PASS
Settled exact-master match: PASS
BLOCKER: 0
MAJOR: 0
```

The compiler must not overwrite the approved V3 production package during development.

Use a separate experiment ID, e.g.:

```text
happy_corgi_compiler_v1_experiment
```

until formally approved.

---

# 20. KEY ENGINEERING DECISION SUMMARY

PixelGleam should **not** solve the core problem as:

```text
show a regular grid
+ place gem sprites into grid cells
```

It should solve it as:

```text
authored artwork
+ hidden organic interaction tessellation
+ deterministic Cell-ID lookup
+ connected Region logic
+ authored settled texture reveal
```

The compiler is therefore not a generic image filter.

It is a content compiler that produces gameplay topology.

---

# 21. FINAL V1 ACCEPTANCE TARGET

`MOSAIC_TOPOLOGY_COMPILER_V1` is ready for production use only when:

1. Happy Corgi V3 remains a protected regression reference.
2. Five artwork classes have been compiled and reviewed.
3. Structural QA is fully deterministic and automated.
4. Cell-ID / Region topology has zero blocker errors.
5. Settled 100% artwork reproduces the approved master.
6. Runtime hit testing works after arbitrary supported pan/zoom transforms.
7. Tap forgiveness improves touch UX without violating topology.
8. Released Region IDs are treated as persistent/versioned content.
9. Google AI Studio can consume the output without generating geometry itself.
10. New artwork can be added without changing gameplay-engine logic.

Factory success criterion:

> The difficult work of making a soft, premium mosaic becomes a repeatable offline content-production problem rather than a fragile runtime-rendering problem.

---

# 22. NEXT EXECUTION STEP

Start with **M0 + M1**, not with automatic AI segmentation.

Immediate deliverables:

```text
1. Freeze Happy Corgi V3 as golden fixture.
2. Define compiler CLI/job contract.
3. Define configuration schema.
4. Define QA report schema.
5. Build Cell-ID validation utilities.
6. Build adjacency/Region reconstruction utilities.
7. Verify the compiler toolchain can fully read and validate the existing V3 package.
```

Gate:

```text
Existing Happy Corgi V3
→ parsed
→ independently validated
→ all known QA metrics reproduced
→ PASS
```

Only then implement new tessellation generation.

---

END OF SPEC
