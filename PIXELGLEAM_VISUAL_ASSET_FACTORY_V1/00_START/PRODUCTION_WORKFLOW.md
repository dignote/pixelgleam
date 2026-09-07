# Production Workflow

## Stage F0 — Concept brief
Define subject, category, target difficulty, composition, semantic color budget, approximate cell-count class and expected negative spaces.

Gate: the concept must be readable at mobile thumbnail size and distinct from existing content.

## Stage F1 — Master illustration
Generate/author a clean light-theme illustration. Keep silhouette strong, important parts separated and tiny decorative fragments under control.

Output: `master.png`.

## Stage F2 — Artwork normalization
Normalize background, crop, subject scale, palette complexity, detail density, narrow strips and micro-islands. Do not flatten away all visual richness; this stage prepares a clean topology source.

Outputs: `master_approved.png`, normalization QA notes.

## Stage F3 — Semantic preparation
Create:
- `subject_mask.png`
- `void_mask.png`
- `semantic_color_mask.png`
- `palette.json`

Void has precedence over subject and semantic labels.

## Stage F4 — Topology preflight
Check background connectivity, preserved holes, semantic components, minimum feature size, color count, thin-band ratio and expected topology complexity.

Gate: no obvious invalid bridges or playable cells in negative space.

## Stage F5 — Settled gem texture
Author the completed artwork surface. It must still read as a gem/bead mosaic, but should preserve soft contours and avoid a visible regular square lattice.

Output: `settled_texture.png`.

## Stage F6 — Hidden topology compile
Compile semantic regions into organic hidden Cell-IDs and connected regions. The compiler may merge/split cells for stability, but may not cross preserved voids or semantic barriers.

Outputs: `cell_id_map.png`, `artwork.json`, QA assets.

## Stage F7 — QA
Structural QA:
- 100% playable coverage
- 0 background leakage
- connected Cell-IDs
- symmetric adjacency
- connected/maximal Regions

Runtime-contract QA:
- transform-correct hit testing
- wrong-color tap does not alter progress
- no double completion
- save/restore exact region state

Visual QA:
- subject silhouette preserved
- no gems in voids
- no obvious bridges
- no micro-cell clutter
- final still reads as gem mosaic
- final not reduced to generic pixel grid

## Stage F8 — Freeze package
An artwork is production-ready only when all gates PASS.
