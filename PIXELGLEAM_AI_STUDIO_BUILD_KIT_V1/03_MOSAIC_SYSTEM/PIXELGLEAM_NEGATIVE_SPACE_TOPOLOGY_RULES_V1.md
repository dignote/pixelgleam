# PixelGleam Negative Space & Topology Rules V1

## Core rule
A pixel located inside the subject bounding box is not automatically playable.

Negative spaces between subject parts must remain non-playable when they are real visual gaps.

Examples:
- between hair and arm,
- between a wand/star and the head,
- between arm and torso/dress,
- holes inside handles or ornaments,
- gaps between petals/leaves when visibly open.

## Required masks

### Subject mask
Defines the playable subject domain.

### Semantic/color mask
Defines gameplay color intent within playable pixels.

### Preserved void mask
Defines pixels that must never receive a Cell ID, even when surrounded by subject parts.

## Background-connected validation
Flood-fill background starting from the image border. Background-connected pixels must remain non-playable unless explicitly overridden.

## Hole/gap preservation
Do not run unconditional hole filling, closing or dilation that bridges visible gaps. Any cleanup operation must be constrained by a maximum gap size.

## Topology barriers
Cells may not cross:
- subject → background boundary,
- preserved voids,
- semantic boundaries that define different gameplay colors/regions.

## Suggested authoring behavior
For difficult artwork, allow an assisted `topology_cut_mask` or `preserve_void_mask` to explicitly forbid bridging.

## QA gate
PASS only if:
- no Cell ID exists outside playable subject,
- all protected negative spaces remain empty,
- no semantic boundary is crossed improperly,
- visual gaps remain visually and topologically open,
- coverage inside the playable domain is 100%.
