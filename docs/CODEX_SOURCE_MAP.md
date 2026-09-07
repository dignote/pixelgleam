# PixelGleam Codex Source Map

## Primary product contract
- `PIXELGLEAM_PRD.md`

## Gameplay source of truth
- `PIXELGLEAM_AI_STUDIO_BUILD_KIT_V1/02_GAMEPLAY_SPECS/GAMEPLAY_SPEC.md`
- `PIXELGLEAM_AI_STUDIO_BUILD_KIT_V1/02_GAMEPLAY_SPECS/ARCHITECTURE.md`
- `PIXELGLEAM_AI_STUDIO_BUILD_KIT_V1/02_GAMEPLAY_SPECS/GAMEPLAY_INVARIANTS.md`

## Mosaic/content source of truth
- `PIXELGLEAM_AI_STUDIO_BUILD_KIT_V1/03_MOSAIC_SYSTEM/PIXELGLEAM_HYBRID_MOSAIC_ARCHITECTURE_V1.md`
- `PIXELGLEAM_AI_STUDIO_BUILD_KIT_V1/03_MOSAIC_SYSTEM/PIXELGLEAM_NEGATIVE_SPACE_TOPOLOGY_RULES_V1.md`
- `PIXELGLEAM_AI_STUDIO_BUILD_KIT_V1/03_MOSAIC_SYSTEM/PIXELGLEAM_SETTLED_GEM_VISUAL_STANDARD_V1.md`
- `PIXELGLEAM_AI_STUDIO_BUILD_KIT_V1/03_MOSAIC_SYSTEM/MOSAIC_TOPOLOGY_COMPILER_V1.md`

## Runtime implementation map
- Gameplay core: `app/src/main/java/com/example/core/engine/`
- Artwork/data: `app/src/main/java/com/example/data/`
- Canvas/render interaction: `app/src/main/java/com/example/ui/canvas/`
- Product shell/UI: `app/src/main/java/com/example/ui/`
- Orchestration/state: `app/src/main/java/com/example/viewmodel/PixelGleamViewModel.kt`
- Audio/haptics: `app/src/main/java/com/example/audio/`

## Key tests
- `app/src/test/java/com/example/GameplayCoreEngineTest.kt`
- `app/src/test/java/com/example/CanvasHitTestTest.kt`
- `app/src/test/java/com/example/ArtworkTransformAndHitResolverTest.kt`
- `app/src/test/java/com/example/ArtworkTopologyTest.kt`
- `app/src/test/java/com/example/VerticalSliceHappyCorgiTest.kt`
- `app/src/test/java/com/example/GemPresentationTest.kt`
- `app/src/test/java/com/example/PersistenceTest.kt`
- `app/src/test/java/com/example/FoundationArchitectureTest.kt`
- `app/src/test/java/com/example/ProductShellUnitTest.kt`

## Content QA
- `fast_audit.py`
- `full_audit.py`
- `audit_artworks.py`
- compiler reference/tests under `compiler_ref/`

## Authority rule
Generated prompts/reports/reference packages are supporting material. They do not override the primary contracts above unless the root explicitly records a new decision in `docs/DECISIONS.md`.
