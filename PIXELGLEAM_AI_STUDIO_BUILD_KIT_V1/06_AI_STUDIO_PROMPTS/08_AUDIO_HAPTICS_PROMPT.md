# Prompt 08 — Real Audio & Haptics

Implement the PRD audio/haptics contract without changing gameplay logic.

Required real audio categories:
- background music,
- UI tap/navigation,
- palette selection,
- gem placement,
- region completion,
- color completion,
- hint,
- wrong tap,
- artwork completion.

Required settings:
- Music On/Off,
- SFX On/Off,
- Haptics On/Off.

Each toggle must have immediate runtime effect and persist across restart.

Lifecycle requirements:
- no duplicate music instances,
- no overlapping tracks caused by navigation,
- correct background/foreground handling,
- no leaked audio after leaving gameplay.

Use restrained tactile mappings consistent with the PRD.

End with phase report.
