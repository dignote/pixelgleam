# PixelGleam Hybrid Soft Mosaic Architecture V1

## Decision
PixelGleam uses **hidden topology + connected-region gameplay + authored settled gem texture**.

This is the production architecture. The square-grid reference is for comparison/debug only.

## Authoritative layers

### 1. Interaction truth — Cell-ID raster
`cell_id_map.png` maps playable artwork pixels to a stable Cell ID.

Runtime tap pipeline:

```text
screen position
→ inverse pan/zoom transform
→ artwork-local coordinate
→ Cell-ID lookup
→ Cell
→ Region
```

A Cell-ID lookup is deterministic and does not depend on visual shading.

### 2. Gameplay truth — Region graph
Each cell belongs to exactly one gameplay region. A region is a connected semantic/color area represented by one or more hidden cells.

```text
Cell
→ regionId
→ colorId
→ region membership
```

One accepted tap completes the specific connected region containing the tapped cell. Same-color disconnected regions remain separate.

### 3. Visual truth — Settled gem texture
`settled_texture.png` is the approved completed appearance. It can retain soft contours, organic gem/bead microstructure, highlights and shading.

Runtime must not rebuild the completed artwork by stamping thousands of independent G02 PNGs.

### 4. Transient presentation — G02
GEM_G02 and G02 effects are used for transient flight, landing, sparkle and settle feedback. They never define logical completion.

## Unfinished state
The player should see a readable numbered mosaic surface. Cell boundaries may be visible but restrained. The user should understand there are mosaic pieces without seeing a rigid full-screen square lattice.

## Completed state
Completed regions reveal their exact corresponding portion of the settled gem texture. The final artwork must still read as a gem/bead mosaic rather than a flat illustration.

## Negative space
Any preserved background/void pixel has no playable Cell ID. No cell or gem may be generated there.

## Tap forgiveness
Exact lookup is primary. Near boundaries, a small local neighborhood may be searched and candidates matching the selected color may be preferred. Correction radius must remain small and deterministic.

## Runtime prohibitions
Do not:
- runtime flood-fill the RGB artwork,
- run AI segmentation on user taps,
- use the settled texture as gameplay truth,
- merge disconnected same-color regions,
- fill preserved negative-space gaps,
- create a general physics/collision engine for gem effects.
