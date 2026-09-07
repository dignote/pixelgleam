# PixelGleam — Durable Decisions

## D001 — Root/worker authority
Root GPT-5.6 Sol/high owns architecture, gameplay interpretation, integration review and final acceptance.
Workers execute bounded tasks and cannot self-accept work.

## D002 — Hit testing
`HitResolver` is the authoritative gameplay hit-resolution path. Do not introduce parallel gameplay hit semantics in UI/ViewModel.

## D003 — Gameplay topology
Gameplay must preserve hidden topology, connected-region semantics and negative-space behavior defined by the gameplay/mosaic specifications.

## D004 — Context strategy
Repository contracts and state files preserve long-term project context. Worker contexts should remain task-bounded.

## D005 — Imported AI Studio baseline
The current codebase is treated as the migration baseline. Cleanup/refactor is not automatically authorized merely because code originated from AI Studio.
