# Prompt 12 — Release Candidate Gate

Prepare a release-candidate build without architecture churn.

Verify:
- clean build,
- production app ID/versioning configured appropriately,
- no debug-only UI exposed,
- no placeholder assets/text,
- no dead feature,
- no crash/freeze/blank screen in primary flow,
- all required V1 modes/screens functional,
- persistence survives restart,
- audio/haptics lifecycle correct,
- Android Back correct,
- artwork content package loads deterministically,
- final Happy Corgi and at least one additional content package can be opened if available,
- final regression P0 tests PASS.

Do not add new features in this phase.
Return only release blockers, fixes performed, final test results, and a PASS / REWORK / BLOCKED decision.
