# Prompt 02 — Pure Gameplay Core

Implement only Phase A2.

Read GAMEPLAY_SPEC and GAMEPLAY_INVARIANTS again before coding.

Implement pure testable gameplay logic with rendering removed from authoritative state.

Required concepts:
- Artwork content model
- Cell ID
- Region ID
- Color ID
- selected color
- completedRegionIds
- region lookup
- tap validation result
- completion evaluation
- idempotent completion

Rules:
- one valid tap completes only the connected region containing the tapped Cell ID,
- disconnected same-color areas remain separate,
- wrong-color input does not mutate progress,
- a completed region cannot increment progress twice,
- artwork completion is based on logical state, never animation timing.

Use the Happy Corgi V3 `artwork.json` contract.

Add unit tests for every P0 invariant before proceeding.

Do not write presentation animation yet.
Do not infer regions from pixels/RGB at runtime.

End with phase report.
