# PixelGleam Visual Asset Factory V1

Purpose: production pipeline for creating scalable, high-quality artwork content for PixelGleam before handoff to Google AI Studio runtime.

## Locked product direction
PixelGleam uses Hybrid Soft Mosaic:
- hidden Cell-ID raster = interaction truth
- connected Region graph = gameplay truth
- G02 / transient gems = placement feedback only
- authored settled_texture = final completed visual truth
- preserved negative space = non-playable void

Do not convert the product into a visible square-grid coloring game by default.

## Required production order
1. Generate or illustrate master artwork.
2. Normalize composition, detail density and semantic palette.
3. Produce subject mask.
4. Produce preserved void / negative-space mask.
5. Produce semantic color map.
6. QA semantic topology and minimum feature size.
7. Author settled gem texture using the approved gem material language.
8. Compile hidden topology.
9. Run structural, runtime-contract and visual QA.
10. Freeze app-ready artwork package.

Start with 01_SPECS/ART_STYLE_BIBLE.md and 00_START/PRODUCTION_WORKFLOW.md.
