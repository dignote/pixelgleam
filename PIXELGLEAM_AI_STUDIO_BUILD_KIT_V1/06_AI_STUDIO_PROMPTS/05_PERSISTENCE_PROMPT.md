# Prompt 05 — Exact Save / Resume

Implement only Phase A5.

Persist at minimum:
- artwork ID,
- completedRegionIds,
- completed state,
- selected color if useful and predictable,
- tutorial/settings state later through the same persistence layer.

Required behavior:
- accepted region completion becomes durable promptly,
- kill/restart restores exact logical progress,
- transient flight/sparkle animation does not need to resume,
- stale/invalid region IDs are handled safely,
- corrupt/missing save must not crash the app.

Add tests for serialization/restore and idempotent completion after restore.

End with phase report.
