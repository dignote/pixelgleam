# PixelGleam — GEOMETRY v3 CODEX HANDOFF

## Required Reading

Before geometry implementation, read:

1. `MOSAIC_GEOMETRY_SPEC.md`
2. `G02_MOSAIC_MATERIAL_SPEC.md`
3. `HAPPY_CORGI_V2_TO_V3_MIGRATION.md`
4. `MOSAIC_GEOMETRY_QA_GATE.md`

## First Codex task

PLAN ONLY.

Map:

```text
geometry requirement
→ data model
→ authoring/preprocessing step
→ runtime renderer
→ hit-test
→ QA test
```

Do not implement full app screens yet.

## Required architectural boundary

Authoring/preprocessing may generate:

- final polygons,
- semantic assignments,
- adjacency,
- region graph,
- render seeds.

Runtime should consume validated data.

Do not perform expensive tessellation every frame.

## Runtime truth

Gameplay source of truth:

- validated polygon cells,
- adjacency,
- regionId,
- completedRegionIds.

Visual source of truth:

- approved master,
- G02 settled material spec.

## Prohibited

Do not:

- convert v3 back to a square grid,
- place standalone PNG gems as final settled cells,
- leave visible background gaps,
- infer adjacency by center distance only,
- derive topology from screenshots at runtime.
