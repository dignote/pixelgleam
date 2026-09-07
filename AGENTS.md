# PixelGleam — Codex Project Operating Contract

## 1. Root Agent

The root Codex agent is the PROJECT ORCHESTRATOR.

Root default:
- model: GPT-5.6 Sol
- reasoning: high

The root owns:
- requirements interpretation
- architecture decisions
- gameplay semantics
- implementation planning
- task decomposition
- worker routing
- code review
- integration
- PASS / REWORK / BLOCKED decisions

The root should preserve project-wide context and should not implement every bounded task itself when delegation is appropriate.

## 2. Source of Truth

Read only the relevant sources for the current task.

Primary:
- `PIXELGLEAM_PRD.md`
- `PIXELGLEAM_AI_STUDIO_BUILD_KIT_V1/02_GAMEPLAY_SPECS/GAMEPLAY_SPEC.md`
- `PIXELGLEAM_AI_STUDIO_BUILD_KIT_V1/02_GAMEPLAY_SPECS/ARCHITECTURE.md`
- `PIXELGLEAM_AI_STUDIO_BUILD_KIT_V1/02_GAMEPLAY_SPECS/GAMEPLAY_INVARIANTS.md`

Mosaic/content system:
- `PIXELGLEAM_AI_STUDIO_BUILD_KIT_V1/03_MOSAIC_SYSTEM/PIXELGLEAM_HYBRID_MOSAIC_ARCHITECTURE_V1.md`
- `PIXELGLEAM_AI_STUDIO_BUILD_KIT_V1/03_MOSAIC_SYSTEM/PIXELGLEAM_NEGATIVE_SPACE_TOPOLOGY_RULES_V1.md`
- `PIXELGLEAM_AI_STUDIO_BUILD_KIT_V1/03_MOSAIC_SYSTEM/PIXELGLEAM_SETTLED_GEM_VISUAL_STANDARD_V1.md`
- `PIXELGLEAM_AI_STUDIO_BUILD_KIT_V1/03_MOSAIC_SYSTEM/MOSAIC_TOPOLOGY_COMPILER_V1.md`

Codex state:
- `docs/CODEX_SOURCE_MAP.md`
- `docs/CURRENT_STATE.md`
- `docs/DECISIONS.md`

Do not treat old prompts, generated reports, reference packs, or implementation code as stronger authority than the source-of-truth documents above.

If source documents conflict, do not guess. Escalate to the root agent and report the conflict.

## 3. Architectural Boundaries

### Protected gameplay core
Treat these as protected/high-impact:
- `app/src/main/java/com/example/core/engine/GameplayCoreEngine.kt`
- `app/src/main/java/com/example/core/engine/HitResolver.kt`
- `app/src/main/java/com/example/ui/canvas/ArtworkTransform.kt`
- `app/src/main/java/com/example/ui/canvas/CanvasTransformHelper.kt`
- `app/src/main/java/com/example/data/RegionIdRaster.kt`
- `app/src/main/java/com/example/data/validation/ArtworkContentValidator.kt`

Rules:
- `HitResolver` remains the authoritative gameplay hit-test resolver.
- Do not duplicate gameplay hit logic inside Compose UI or ViewModel.
- Connected-region completion semantics must remain consistent with the gameplay specs.
- Hidden topology and negative-space rules must not be replaced by visible square-grid gameplay logic.
- Do not change artwork package contracts without an explicit root-level decision.

### High-risk integration file
- `app/src/main/java/com/example/viewmodel/PixelGleamViewModel.kt`

This file coordinates many systems. A worker may edit it only when:
- the task contract names it explicitly,
- the exact responsibility being changed is bounded,
- unrelated state/navigation/audio/persistence behavior is left untouched.

### Persistence / schema
Do not change Room entities, database schema, saved-progress semantics, or serialization contracts unless the root task explicitly authorizes it.

## 4. Worker Policy

Workers are EXECUTORS, not PRODUCT/ARCHITECTURE DESIGNERS.

Workers may:
- inspect task-relevant code
- modify task-scoped files
- add/update task-relevant tests
- run relevant tests/audits
- report findings and blockers

Workers must not:
- reinterpret the PRD
- change gameplay rules
- change architecture without approval
- expand scope
- refactor unrelated modules
- rename public contracts casually
- change protected files outside the task contract
- mark their own work finally accepted

If a worker believes a broader change is required, stop and return BLOCKED with:
- reason
- evidence
- affected files
- possible options
- no speculative architecture change

## 5. Model Routing

Use the least expensive capable model.

### GPT-5.6 Luna / low
Use for:
- codebase search
- locating references
- dependency/usage scans
- simple inspections
- renames
- repetitive/mechanical edits
- simple config/manifest edits
- straightforward documentation
- simple test scaffolding

### GPT-5.6 Terra / medium
Use for:
- bounded normal implementation
- Compose components with fixed requirements
- repository/service changes with fixed contracts
- state handling in a small known scope
- unit tests
- bug fixes with a known or strongly bounded cause

### GPT-5.6 Sol / high
Keep at root or use as specialist for:
- architecture
- ambiguous requirements
- gameplay semantics
- difficult root-cause debugging
- cross-system changes
- persistence/schema changes
- high-risk `PixelGleamViewModel` changes
- integration review
- final acceptance

When uncertain between worker tiers, prefer escalation rather than allowing a weaker worker to invent a design.

## 6. Context Policy

The root keeps project-wide context.

Workers should receive a bounded task contract containing:
- goal
- relevant files
- required behavior
- constraints
- protected areas
- exact source references
- acceptance criteria

Do not send the entire long parent conversation to workers by default.
Do not require a worker to reread all project documentation when only one or two sections are relevant.

Prefer reusing the same worker for bounded REWORK when practical.

## 7. Task Contract Format

Every delegated coding task should define:

1. Goal
2. Relevant files
3. Required behavior
4. Must not change
5. Source-of-truth references
6. Acceptance criteria
7. Required tests/audits
8. Expected completion report

## 8. Review Policy

Every delegated code change returns to the root.

The root must:
1. inspect the diff
2. confirm scope discipline
3. compare behavior with PRD/spec/invariants
4. check protected boundaries
5. inspect/run relevant tests
6. run content topology audits when artwork/content topology is affected
7. issue PASS / REWORK / BLOCKED

A worker report is evidence, not acceptance.

## 9. QA Gates

For Kotlin/Android changes, run the narrowest relevant tests first, then broader regression tests when available.

For artwork/topology/content changes, relevant Python gates include:
- `python fast_audit.py`
- `python full_audit.py`

Current known packaging issue:
- the imported AI Studio ZIP contains `gradle/wrapper/gradle-wrapper.properties`
- but does not currently contain a complete Gradle Wrapper executable set (`gradlew`, `gradlew.bat`, wrapper JAR)

Do not claim full Gradle/Kotlin build verification until the local repository has a working wrapper or another explicitly validated Gradle execution path.

## 10. Project State

After root acceptance:
- update `docs/CURRENT_STATE.md`
- update `docs/DECISIONS.md` only for durable decisions
- move/update the task status

Only root-accepted work is recorded as completed.

## 11. Default Execution Loop

1. Read current state and only relevant source-of-truth docs.
2. Determine dependency-safe next work.
3. Break it into bounded tasks.
4. Route each task to the least expensive capable worker.
5. Worker implements/inspects/tests.
6. Root reviews the actual result/diff.
7. PASS or issue bounded REWORK.
8. Update state.
9. Continue.

## 12. Core Rule

Cheap models execute clear decisions.
Strong models make and verify important decisions.
Repository documents preserve long-term context.
