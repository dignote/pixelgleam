# Prompt 11 — Production Readiness Audit

Do not redesign the application.
Inspect and test the actual implementation.

Audit:
1. every visible button, card, icon button, tab, toggle and gesture,
2. every navigation route and Android system Back,
3. gameplay connected-region invariants,
4. pan/zoom hit accuracy,
5. persistence and restart restore,
6. Daily/My Art states,
7. audio/haptics toggles and lifecycle,
8. empty/error/locked/loading states where relevant,
9. content-load failure,
10. duplicate completion / duplicate audio / dead navigation,
11. fake or placeholder production functionality,
12. visual anti-AI rules.

Produce:

```text
Screen | Control / Behavior | Expected | Actual | Status
```

Every P0 row must end PASS.
If a row fails, fix it and retest before finishing.
