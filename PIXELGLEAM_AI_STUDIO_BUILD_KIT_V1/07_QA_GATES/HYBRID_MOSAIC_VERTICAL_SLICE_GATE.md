# Hybrid Mosaic Vertical Slice Gate

PASS requires all of the following on-device/in-app:

## Content loading
- Happy Corgi V3 package loads without rewriting topology.
- Cell-ID map and artwork coordinate space are aligned.
- Invalid/missing content fails safely.

## Gameplay
- selected color is explicit,
- valid tap completes exactly the tapped connected region,
- disconnected same-color region remains unfinished,
- wrong-color tap does not mutate progress,
- completed region cannot double-count,
- completion fires once.

## Geometry
- pan works,
- pinch zoom works,
- drag/pinch do not become fill taps,
- Cell-ID hit testing remains correct after transforms,
- preserved background/void has no playable hit.

## Visual
- numbered unfinished state is readable,
- G02 is used only for transient feedback,
- settled texture is revealed as completed visual truth,
- final state retains gem/mosaic identity,
- no visible rigid global square grid unless explicitly part of unfinished guidance.

## Persistence
- exact completedRegionIds persist,
- kill/restart restores exact logical progress,
- completed artwork remains completed,
- animation state is not required for restore.

## Navigation
- exit/back works,
- Android system Back works,
- completion actions work,
- no dead-end screen.
