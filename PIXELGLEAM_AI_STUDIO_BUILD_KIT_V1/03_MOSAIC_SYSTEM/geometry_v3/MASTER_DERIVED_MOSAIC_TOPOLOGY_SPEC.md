# PixelGleam — MASTER-DERIVED MOSAIC TOPOLOGY SPEC

## 0. Production Decision

For high-fidelity artwork such as Happy Corgi, the authoritative production model is:

```text
approved completed mosaic master
→ Cell-ID segmentation map
→ numbered/color reference mapping
→ connected region graph
```

This replaces attempts to procedurally recreate the final jewel surface from generic polygon shaders.

---

# 1. Three Sources of Truth

## Gameplay topology truth

`cell_id_map.png`

A 16-bit integer map.

```text
0      = outside artwork
1..N   = playable cell ID
```

Every subject pixel belongs to exactly one Cell-ID.

## Gameplay data truth

`artwork.json`

Contains:

- cell ID,
- center,
- colorId,
- regionId,
- neighbors,
- optional vector contour.

## Settled visual truth

`settled_texture.png`

This is the approved authored completed mosaic appearance.

When a cell/region is completed, the runtime reveals the corresponding pixels of `settled_texture` through the Cell-ID mask.

---

# 2. G02 Dual Representation

## Flight / landing

Use approved standalone `GEM_G02`.

## Settled state

Do not leave the standalone G02 sprite inside the target cell.

Instead:

```text
flight gem reaches target
→ logical cell already complete
→ short crystallize transition
→ reveal settled_texture inside target Cell-ID mask
```

This guarantees that completed artwork matches the approved master.

---

# 3. Coverage Contract

The Cell-ID map must satisfy:

```text
subject pixels with cellId = 0   => 0
background pixels with cellId>0  => 0
overlap                          => impossible by representation
coverage                         => 100%
```

A single integer raster map makes cell ownership exclusive by construction.

---

# 4. Hit Testing

Preferred runtime hit test:

```text
screen point
→ inverse pan/zoom
→ artwork-local coordinate
→ Cell-ID map lookup
→ cellId
→ regionId
```

This is simpler and more exact than testing hundreds of arbitrary polygons.

Optional vector polygons are for:

- debug,
- label placement,
- highlight contours.

They are not required as hit-test truth.

---

# 5. Region Logic

Adjacency is derived from shared horizontal/vertical Cell-ID boundaries.

Region:

```text
connected same-color Cell-IDs
```

Disconnected same-color components remain separate regions.

---

# 6. Numbered State

Color IDs should be authored from an approved numbered/color reference, not inferred from jewel highlights in the completed texture.

The unfilled renderer may:

- render flat cell colors + numbers from data, or
- use an approved numbered texture if exact alignment is guaranteed.

---

# 7. Progress Rendering

For each completed cell:

```text
mask = cell_id_map == cellId
output[mask] = settled_texture[mask]
```

For an entire completed region:

```text
mask = Cell-ID in region.cellIds
```

No white gaps may appear between completed cells because settled pixels come from one continuous authored master.

---

# 8. Performance

Recommended:

- load Cell-ID map once,
- cache region masks or cell bounds,
- reveal via Canvas/bitmap mask,
- cache completed bitmap progressively.

Do not rescan the full bitmap per frame if profiling shows cost.

---

# 9. QA

PASS requires:

```text
Coverage                       100%
Uncovered subject pixels       0
Background-labeled pixels      0
Overlap                        0
Disconnected Cell-IDs          0
Adjacency symmetry             PASS
Region connectivity            PASS
Settled texture master match   exact
```

---

# 10. Generalization

Each new production artwork may have:

```text
artwork.json
cell_id_map.png
subject_mask.png
settled_texture.png
thumbnail.webp
```

This makes artwork quality authorable independently of the runtime renderer while preserving deterministic gameplay topology.
