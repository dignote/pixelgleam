# Semantic Mask Spec V1

The semantic map is a production-control layer, not the final artwork.

## Goals
- keep the number of gameplay colors bounded,
- preserve meaningful visual regions,
- remove RGB noise from shadows/highlights,
- create deterministic connected-region logic.

## Rules
- Similar shades may share one semantic color ID.
- Visually different materials may use separate semantic IDs even when RGB is close.
- Tiny highlight/specular marks usually remain visual-only in settled_texture.
- A semantic region may be disconnected; connected-region compilation will split it into separate Region IDs.
- Diagonal-only contact does not merge connected regions.
