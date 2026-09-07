# PixelGleam Artwork Input Spec V1

## Purpose
Prepare artwork so the Mosaic Topology Compiler can generate stable, readable, production-quality mosaic content at scale.

## Design target
Artwork must be attractive when completed but also **mosaic-friendly**. The compiler is not expected to rescue arbitrary noisy illustrations.

## Recommended semantic palette
- Simple/medium artwork: approximately 8–20 gameplay colors.
- Detailed artwork: approximately 20–32 gameplay colors.
- Visual shading may contain more rendered shades than gameplay `colorId` values.

A highlight, base shade and soft shadow can remain visually distinct in `settled_texture` while sharing one gameplay semantic color where appropriate.

## Prefer
- clean subject silhouette,
- clear major color masses,
- intentional highlights/shadows,
- readable forms at mobile size,
- details large enough to contain stable mosaic cells,
- controlled texture/noise,
- good spacing between limbs/objects/ornaments,
- distinct foreground/background separation.

## Avoid
- hundreds of incidental gradient shades,
- photographic noise,
- hair/fur represented as thousands of micro-strands,
- tiny isolated decorations with no gameplay value,
- very thin bands unless intentionally required,
- ambiguous near-background colors around the silhouette,
- overpacked composition.

## Normalization pipeline

```text
raw/generated artwork
→ crop / composition normalize
→ background normalize
→ subject extraction
→ semantic palette simplification
→ small-detail cleanup
→ negative-space preservation
→ semantic mask QA
→ approved master + masks
→ Mosaic Topology Compiler
```

## Input QA questions
1. Can all major forms be named as semantic regions?
2. Are gaps between parts intentionally preserved?
3. Are small features large enough to become stable cells?
4. Can multiple visual shades be represented by a smaller gameplay palette?
5. Does the completed texture still look premium after semantic simplification?
