# PixelGleam — MOSAIC GEOMETRY SPEC

## 0. Purpose

This document replaces the earlier independent-polygon mosaic approach.

The production geometry target is a **gapless, contour-conforming tessellation** of the artwork silhouette.

The completed mosaic must not look like standalone gems placed inside loose cells.

---

# 1. Geometry Objective

For the subject silhouette S and all playable cells C1..Cn:

```text
Union(Ci) = S
InteriorGap(S, Union(Ci)) = 0
InteriorOverlap(Ci, Cj) = 0 for i != j
```

Cells may share boundaries, but must not overlap in area.

The outer boundary of the union of all cells must match the artwork silhouette.

---

# 2. Recommended Construction

Preferred pipeline:

```text
source artwork
→ semantic masks
→ silhouette mask
→ seeded constrained Voronoi / CVT
→ clip to semantic masks
→ boundary-conforming relaxation
→ topology graph
→ region grouping
→ geometry QA
```

A regular square grid is prohibited for production artwork.

---

# 3. Semantic Segmentation First

Create semantic masks before tessellation:

- orange fur,
- white fur,
- cream fur,
- eyes,
- nose,
- ears,
- tongue,
- collar,
- tag,
- other meaningful regions.

Tessellation must respect semantic boundaries.

A cell cannot cross a semantic color boundary.

---

# 4. Cell Model

```text
Cell
- id
- polygon[]
- center
- colorId
- regionId
- neighbors[]
- renderSeed
```

## polygon

A closed ordered polygon in artwork-local coordinates.

Requirements:

- non-self-intersecting,
- positive area,
- contained within semantic mask,
- participates in gapless coverage.

## center

Used for:

- hit target fallback,
- animation target,
- label placement.

The center must be inside its polygon.

## renderSeed

Deterministic integer used for facet variation.

---

# 5. Region Model

```text
Region
- id
- colorId
- cellIds[]
- contour[]
```

A region is a connected group of same-color cells according to the adjacency graph.

Two same-color regions that do not touch remain separate.

---

# 6. Adjacency

Two cells are neighbors when they share a non-trivial boundary segment.

Do not infer adjacency only from center distance.

Adjacency must be symmetric:

```text
A in neighbors(B) ⇔ B in neighbors(A)
```

---

# 7. Boundary Cells

Boundary cells must conform to the artwork silhouette.

Do not use:

- clipped octagons that leave visible gaps,
- arbitrary inset polygons,
- cells that stop short of the silhouette.

The outer edge of boundary cells collectively forms the subject contour.

---

# 8. Density and Cell Sizing

Cell density may vary by region.

Recommended:

- larger cells in broad fur areas,
- smaller cells near facial detail,
- compact cells around eyes/nose/mouth,
- medium cells along ears and contour.

Avoid uniform density if it harms subject readability.

---

# 9. Hit Testing

Use point-in-polygon after transforming the pointer into artwork-local coordinates.

Pipeline:

```text
screen point
→ inverse canvas transform
→ artwork-local point
→ candidate cell lookup
→ point-in-polygon
```

A spatial acceleration structure is optional only if profiling proves it necessary.

---

# 10. Geometry QA Gate

PASS requires:

```text
Silhouette coverage        = 100%
Interior uncovered area    = 0
Interior overlap area      = 0
Boundary conformity        = PASS
Cell center inside polygon = 100%
Polygon validity           = 100%
Adjacency symmetry         = 100%
Region connectivity        = 100%
Semantic boundary crossing = 0
```

Any visible white gap inside the subject is a BLOCKER.

Any standalone-cell inset look caused by geometry is a MAJOR issue.

---

# 11. Deprecated Models

Deprecated for production:

- square grid artwork v1,
- independent loose polygon model v2.1.

These may remain as historical prototypes only.
