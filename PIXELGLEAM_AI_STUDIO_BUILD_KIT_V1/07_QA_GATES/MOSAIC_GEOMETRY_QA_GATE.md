# PixelGleam — MOSAIC GEOMETRY QA GATE

## BLOCKER

- any uncovered interior area,
- any interior overlap,
- invalid/self-intersecting polygon,
- semantic boundary crossed,
- hit-test ambiguity on a playable point,
- incorrect region connectivity,
- final subject contour visibly wrong.

## MAJOR

- visible seam/gap,
- cell density harms readability,
- settled material appears inset,
- incorrect facet direction,
- contour visibly simplified beyond approved master.

## PASS

```text
BLOCKER = 0
MAJOR = 0
Coverage = 100%
Overlap = 0
Semantic crossing = 0
Adjacency symmetry = 100%
Region connectivity = 100%
```

## Required Review Artifacts

Produce:

- silhouette mask,
- semantic masks,
- tessellation map,
- adjacency debug map,
- numbered canvas,
- settled G02 mosaic,
- 0/25/50/75/100 progression,
- contour close-up,
- gap/overlap heatmap,
- screenshot comparison to approved master.
