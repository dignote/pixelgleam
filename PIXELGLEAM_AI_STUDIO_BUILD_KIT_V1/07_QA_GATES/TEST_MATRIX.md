# PixelGleam — TEST_MATRIX

## 0. Purpose

This matrix defines minimum gameplay verification.

Priority:

- P0 = release/vertical-slice blocker
- P1 = important correctness/performance
- P2 = polish

Automate pure logic tests wherever possible.

---

# 1. Artwork topology

| ID | Pri | Test | Expected |
|---|---|---|---|
| ART-001 | P0 | Cell references existing region | Load succeeds only for valid reference |
| ART-002 | P0 | Region contains mixed colorIds | Validation fails |
| ART-003 | P0 | Same-color orthogonally connected cells share region | PASS |
| ART-004 | P0 | Same-color diagonal-only cells | Separate regions |
| ART-005 | P0 | Cell outside width/height | Validation fails |
| ART-006 | P1 | Duplicate cell assignment | Validation fails |
| ART-007 | P1 | Empty artwork | Rejected cleanly |
| ART-008 | P1 | Region with zero cells | Rejected cleanly |

---

# 2. Region fill

| ID | Pri | Test | Expected |
|---|---|---|---|
| REG-001 | P0 | Select color and tap matching incomplete region | Exactly that region completes |
| REG-002 | P0 | Two disconnected regions share color; tap first | Only first completes |
| REG-003 | P0 | Wrong selected color | No progress mutation |
| REG-004 | P0 | Tap completed region | No duplicate progress |
| REG-005 | P0 | Tap outside canvas | No progress mutation |
| REG-006 | P0 | Last unfinished region | Completion transition triggered once |
| REG-007 | P0 | Rapid repeated taps on same region | Region counted once |
| REG-008 | P1 | Rapid taps on different valid regions | Each accepted region counted once |
| REG-009 | P1 | Very large region | Logic completes immediately; effect may batch |
| REG-010 | P1 | Effects disabled | Fill still works identically |

---

# 3. Progress

| ID | Pri | Test | Expected |
|---|---|---|---|
| PROG-001 | P0 | Complete 1 of N regions | Count = 1 |
| PROG-002 | P0 | Re-tap completed region | Count unchanged |
| PROG-003 | P0 | Complete all regions of one color | Color marked complete |
| PROG-004 | P0 | All but one region complete | Artwork not complete |
| PROG-005 | P0 | All regions complete | Artwork complete |
| PROG-006 | P1 | Percentage calculation | Derived value is correct |
| PROG-007 | P1 | Reset artwork | Completed set becomes empty intentionally |

---

# 4. State machine

| ID | Pri | Test | Expected |
|---|---|---|---|
| FSM-001 | P0 | LOADING + tap | Ignored |
| FSM-002 | P0 | LOADING + successful load | PLAYING |
| FSM-003 | P0 | PLAYING + pause | PAUSED |
| FSM-004 | P0 | PAUSED + tap | No fill |
| FSM-005 | P0 | PAUSED + resume | PLAYING |
| FSM-006 | P0 | Last region completes | COMPLETING |
| FSM-007 | P0 | Extra taps during COMPLETING | No new resolution |
| FSM-008 | P0 | Completion presentation ends/skips | COMPLETED |
| FSM-009 | P0 | Re-run completion evaluator | No duplicate completion event |
| FSM-010 | P1 | Invalid state transition request | State remains valid |

---

# 5. Canvas transform / collision hit test

Use "collision" here only for pointer-to-grid hit testing.

| ID | Pri | Test | Expected |
|---|---|---|---|
| HIT-001 | P0 | Tap center of cell at zoom 1.0 | Correct cell |
| HIT-002 | P0 | Tap center after pan | Correct transformed cell |
| HIT-003 | P0 | Tap center at min zoom | Correct cell |
| HIT-004 | P0 | Tap center at max zoom | Correct cell |
| HIT-005 | P0 | Tap exact left/top artwork boundary | Defined correct boundary behavior |
| HIT-006 | P0 | Tap just outside boundary | Rejected |
| HIT-007 | P0 | Tap near cell boundary | Deterministic one-cell result |
| HIT-008 | P0 | Pinch gesture over valid cell | No fill tap emitted |
| HIT-009 | P0 | Drag starting over valid cell | No fill tap emitted |
| HIT-010 | P1 | Repeated pan/zoom cycles then tap | Correct cell |
| HIT-011 | P1 | Floating-point extreme transform | No crash; stable result |

---

# 6. Game loop / timing

| ID | Pri | Test | Expected |
|---|---|---|---|
| LOOP-001 | P0 | Same command sequence at different render FPS | Same logical result |
| LOOP-002 | P0 | Animation uses variable dt | Ends at same target |
| LOOP-003 | P0 | Frame skipped / large dt | State remains valid |
| LOOP-004 | P1 | No active effects | No required physics simulation |
| LOOP-005 | P1 | Background/foreground during animation | No duplicate logical completion |
| LOOP-006 | P1 | Navigation away during animation | Effects cleared safely; progress preserved |

---

# 7. Lightweight 2D motion

| ID | Pri | Test | Expected |
|---|---|---|---|
| PHY-001 | P0 | Gem effect reaches target | Final transform equals target cell |
| PHY-002 | P0 | Effect crosses unrelated cell | No gameplay mutation |
| PHY-003 | P0 | Two gem effects overlap | No gameplay collision response required |
| PHY-004 | P0 | Cancel effect mid-flight | Logical region remains complete |
| PHY-005 | P1 | Large region effect cap reached | Gameplay succeeds; visual density degrades gracefully |
| PHY-006 | P1 | 30 vs 120 Hz motion | Similar duration/end state |
| PHY-007 | P2 | Settle/overshoot | Does not visibly leave target placement |

---

# 8. Object pool

| ID | Pri | Test | Expected |
|---|---|---|---|
| POOL-001 | P0 | Acquire unused object | Clean initialized state |
| POOL-002 | P0 | Release then reacquire | No old target/color/callback |
| POOL-003 | P0 | Pool object animation finishes | Returned once |
| POOL-004 | P0 | Clear screen with active effects | All safely released/reset |
| POOL-005 | P0 | Pool exhausted | Core fill still succeeds |
| POOL-006 | P1 | Repeated 1000 acquire/release cycles | No state bleed |
| POOL-007 | P1 | Large burst | Allocation/object count stays bounded as designed |
| POOL-008 | P1 | Effects disabled / no pool usage | Gameplay result unchanged |

---

# 9. Persistence

| ID | Pri | Test | Expected |
|---|---|---|---|
| SAVE-001 | P0 | Complete region, save, restart | Region remains complete |
| SAVE-002 | P0 | Wrong tap, restart | No false progress |
| SAVE-003 | P0 | Complete several disconnected regions | Exact set restored |
| SAVE-004 | P0 | Complete artwork, restart | Restores completed |
| SAVE-005 | P0 | Exit during visual animation after logical commit | Region restored complete |
| SAVE-006 | P0 | Save from artwork A then open B | No cross-artwork contamination |
| SAVE-007 | P1 | Save contains unknown region ID | Ignored/recovered safely |
| SAVE-008 | P1 | Empty/no save | Fresh progress created |
| SAVE-009 | P1 | Reset artwork then restart | Reset persists |

---

# 10. Hint

| ID | Pri | Test | Expected |
|---|---|---|---|
| HINT-001 | P0 | Selected color has unfinished region | Targets unfinished region of same color |
| HINT-002 | P0 | Hint invoked | `completedRegionIds` unchanged |
| HINT-003 | P0 | Current color fully complete | Clear fallback/next-color behavior |
| HINT-004 | P1 | Target off-screen | Pan/zoom guidance reaches target |
| HINT-005 | P1 | Hint repeated | No state corruption |

---

# 11. Input stress

| ID | Pri | Test | Expected |
|---|---|---|---|
| INP-001 | P0 | 10 rapid taps same region | One completion |
| INP-002 | P0 | Rapid color switch + tap | Resolution uses authoritative selected color at command handling |
| INP-003 | P1 | Tap during active region effects | Valid independent tap accepted if PLAYING |
| INP-004 | P1 | Simultaneous-ish pan and tap gestures | Gesture classifier prevents accidental fill |
| INP-005 | P1 | Rotate/background app if supported | No duplicated tap command |

---

# 12. Performance

These are practical gates, not a mandate for a complex engine.

| ID | Pri | Test | Expected |
|---|---|---|---|
| PERF-001 | P0 | 48×48 artwork | Responsive interaction |
| PERF-002 | P1 | Largest planned v1 artwork | No interaction-blocking frame stalls |
| PERF-003 | P1 | Fill largest planned region | Logical commit immediate; visuals bounded |
| PERF-004 | P1 | Repeated fills | No unbounded effect-object growth |
| PERF-005 | P1 | Pan/zoom completed artwork | Stable rendering |
| PERF-006 | P1 | Effects fully disabled | Confirms gameplay not dependent on presentation |

Device-specific FPS thresholds should be established after the actual rendering implementation and target-device profiling rather than invented in this specification.

---

# 13. Failure / lifecycle

| ID | Pri | Test | Expected |
|---|---|---|---|
| LIFE-001 | P0 | App backgrounds during PLAYING | Durable progress preserved |
| LIFE-002 | P0 | App backgrounds during COMPLETING | No duplicated completion |
| LIFE-003 | P0 | Screen disposed with active pooled effects | Effects released/cancelled safely |
| LIFE-004 | P1 | Artwork load fails | No interactive broken gameplay state |
| LIFE-005 | P1 | Malformed save | Safe recovery path |
| LIFE-006 | P1 | Malformed artwork topology | Fail early, no silent wrong regions |

---

# 14. Vertical slice release gate

The first production vertical slice is PASS only when all P0 tests below pass:

```text
ART-*
REG-*
PROG-*
FSM-*
HIT-*
LOOP-*
PHY-*
POOL-*
SAVE-*
HINT-*
INP-*
PERF-001
LIFE-*
```

No UI polish can waive a failed P0 gameplay test.

---

# 15. Manual acceptance scenario

Use one artwork containing:

- at least 32×32 cells,
- 6–8 colors,
- at least one color with 3+ disconnected regions,
- one large region,
- small isolated regions,
- edge/corner cells.

Run:

1. open fresh artwork,
2. select a color,
3. wrong-color tap,
4. correct tap,
5. rapid retap,
6. zoom and pan,
7. correct tap after transform,
8. use hint,
9. background/restore app,
10. verify exact progress,
11. complete all colors,
12. verify one completion transition,
13. restart,
14. verify completed state.

Result must match every invariant in `GAMEPLAY_INVARIANTS.md`.
