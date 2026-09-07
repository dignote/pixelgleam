# Full Build Sequence

| Phase | Goal | Deliverable | Gate |
|---|---|---|---|
| A0 | Context lock | Plan only | Requirements mapped to code/tests |
| A1 | Android foundation | Kotlin/Compose project | Builds/runs, no fake feature |
| A2 | Pure gameplay logic | Artwork model + region controller | Invariants PASS |
| A3 | Canvas geometry | Render + pan/zoom + Cell-ID hit test | Transform hit test PASS |
| A4 | Mosaic presentation | G02 flight + settled reveal | Visual state never gameplay truth |
| A5 | Persistence | Exact region save/restore | Kill/restart exact restore |
| A6 | Vertical slice | Happy Corgi full flow | All P0 vertical-slice tests PASS |
| A7 | Product shell | Gallery, Daily, My Art, Settings, Tutorial | No dead controls |
| A8 | Audio/haptics | Real BGM/SFX/haptics | Toggles/lifecycle/persistence PASS |
| A9 | Visual production | Figma-master aligned UI | Visual self-test PASS |
| A10 | Production audit | Button/nav/state audit | Zero P0 failure |
| A11 | Content pipeline | Accept compiler packages | B1–B5 contract compatible |
| A12 | Release | Release candidate | Final regression PASS |

## Stop conditions
- Do not expand features while A2–A6 has P0 failures.
- Do not replace approved artwork or G02 assets without explicit instruction.
- Do not infer geometry from final texture at runtime.
- Do not show fake leaderboard, account, cloud sync, currency, shop, analytics, or social systems.
