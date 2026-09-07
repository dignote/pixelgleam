# Negative Space & Topology Rules V1

## Golden rule
A pixel being inside the subject bounding box does NOT make it playable.

## Required rules
1. Background connected to the image border remains non-playable.
2. Meaningful inter-part gaps must remain void.
3. Tessellation occurs only inside validated playable subject minus void.
4. No cell may bridge a protected void.
5. No cell may cross an important semantic barrier unless explicitly permitted.
6. Cleanup operations such as dilation/closing must not silently seal visible gaps.

## Character examples
Preserve where visually open:
- hair ↔ arm gap
- wand/star ↔ head gap
- arm ↔ torso/dress gap
- leg ↔ dress gap

## Botanical/object examples
Preserve meaningful holes between petals/leaves, handles, loops and disconnected structural parts.

## Suggested validation sequence
background seed from image border → flood fill → subject subtraction → void confirmation → semantic-barrier check → tessellation.

## Assisted override
For difficult art, an author may provide a topology cut/keep-open mask. Manual assistance is allowed and preferred over silently corrupting topology.
