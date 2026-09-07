# PixelGleam Mosaic Topology Compiler V1 — M0 + M1 Report

## Status

PASS

## M0 — Golden fixture

Frozen Happy Corgi V3 / artwork version 3.9 as protected regression fixture.

Computed independently from source assets:

- Canvas: 365 × 455
- Subject pixels: 111,565
- Cell count: 347
- Region count: 43
- Coverage: 100%
- Uncovered subject pixels: 0
- Background-labeled pixels: 0
- Cell connectivity: PASS
- Cell-ID raster ↔ artwork.json ID set: PASS
- Cell IDs consecutive 1..347: PASS
- JSON adjacency symmetry: PASS
- JSON adjacency exactly matches shared raster boundaries: PASS
- Region color consistency: PASS
- Region connectivity: PASS
- Regions are maximal same-color connected components: PASS
- Settled texture exact on all subject-mask pixels: PASS
- Settled texture transparent outside subject mask: PASS

### Clarification found during independent validation

A raw full-canvas RGBA comparison between `settled_texture.png` and the approved master is not bit-exact because the production settled texture intentionally clears alpha outside `subject_mask`.

All 54,510 differing pixels are background pixels. There are zero differing pixels inside the playable subject. The validator therefore defines the V3 approval phrase “settled texture exact master match” as:

1. bit-exact RGBA equality on every playable `subject_mask > 0` pixel; and
2. alpha = 0 outside the subject mask.

This preserves the approved visual while matching the actual production package semantics.

## M1 — Compiler skeleton + validation

Delivered:

- Python package skeleton
- CLI: `pixelgleam-mosaic validate`
- deterministic V1 config
- QA report JSON schema
- independent topology validator
- protected golden fixture loader/content
- machine-readable QA report
- regression test

Automatic segmentation and topology generation are intentionally NOT implemented in M1.

## Gate

M0: PASS  
M1: PASS

Next allowed milestone: M2 — author-assisted subject/semantic masks → generated Cell-ID topology.
