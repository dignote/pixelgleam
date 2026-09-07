# PixelGleam — G02 MOSAIC MATERIAL SPEC

## 0. Purpose

G02 has two distinct runtime representations.

This dual representation is mandatory.

---

# 1. Representation A — Flight Gem

Use the existing approved standalone G02 asset.

Role:

- spawn,
- flight,
- approach,
- overshoot,
- landing.

This asset may have:

- transparent padding,
- independent silhouette,
- full jewel depth.

It is transient.

---

# 2. Representation B — Settled Mosaic Material

Do NOT keep the standalone PNG gem as the final settled cell.

Instead, after landing, the target cell polygon becomes a G02 jewel material.

Concept:

```text
cell polygon
→ full-coverage jewel surface
→ facets
→ specular highlight
→ internal reflection
→ subtle bevel/rim
```

The polygon itself defines the visible gem boundary.

---

# 3. Settled Cell Requirements

The material must:

- fill 100% of the cell polygon,
- preserve no visible background gap inside the cell,
- share borders tightly with neighboring cells,
- use a consistent light direction,
- retain G02 facet/refraction character,
- avoid looking like a tiny PNG floating inside the cell.

---

# 4. Facet Generation

For each cell:

```text
polygon
→ deterministic internal points from renderSeed
→ 3–7 facet regions
→ assign controlled luminance variation
→ apply base color
→ apply highlight
→ apply internal reflection
```

Facet geometry should be deterministic.

Same artwork + same renderSeed must reproduce identical appearance.

---

# 5. Lighting

Lock:

```text
key light: top-left
specular: compact
rim: subtle
internal reflection: restrained
```

Avoid:

- global neon bloom,
- oversized white stars on every cell,
- inconsistent highlight direction.

---

# 6. Bevel / Edge Treatment

Recommended:

- very small inner bevel,
- thin contact-darkening line,
- no large padding.

The edge treatment must not create visible seams wider than intended.

---

# 7. Color

The material color comes from `colorId`.

Do not reuse the UI shell palette as artwork content.

White/cream cells require dedicated neutral gem material tuning, not simply yellow tint.

---

# 8. Animation Transition

Recommended:

```text
flight gem approaches target
→ scale compresses
→ target cell crystallizes
→ standalone gem visually merges into polygon
→ settled G02 material becomes final
→ small glint
```

Gameplay state must already be logically complete before this finishes.

---

# 9. Renderer Responsibility

Codex may implement settled material using:

- cached Canvas paths,
- shader/facet gradients,
- clipped texture atlas,
- precomputed facet mesh.

Do not require one PNG per final cell.

Do not redraw flight gem appearance as a flat polygon.

---

# 10. Performance

Cache:

- polygon paths,
- facet layout,
- renderSeed results,
- static settled cells.

Re-render only when needed.

Effects may be reduced for performance; settled mosaic fidelity may not be degraded below the approved visual standard.

---

# 11. Visual QA Gate

PASS requires:

```text
Visible white gaps               = 0
Standalone gem inset appearance  = 0
Cell polygon fill coverage       = 100%
Contour fidelity                 = PASS
Facet readability                = PASS
G02 material identity            = PASS
Dense mosaic softness            = PASS
```
