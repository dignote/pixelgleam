# Artwork Input Spec V1

## Required input files
- `master_approved.png`
- `subject_mask.png`
- `void_mask.png`
- `semantic_color_mask.png`
- `palette.json`

## Subject mask
- White/1 = playable subject candidate.
- Black/0 = background.
- Must preserve meaningful gaps.
- Do not blindly fill all holes.

## Void mask
- White/1 = protected negative space.
- Void always overrides subject/semantic labels.
- No Cell-ID may occupy a protected void pixel.

## Semantic map
- Flat integer/color labels only.
- No antialias ambiguity as gameplay truth.
- One semantic color may represent multiple RGB shades in the master.
- Do not derive gameplay regions from raw rendered RGB values.

## Palette
Store stable IDs, display number, representative color and optional role name.

## Preflight targets
- semantic colors: normally 8–28
- no accidental 1–3 pixel islands after normalization
- no uncontrolled long one-pixel strips
- important gaps remain open
- subject/background classification complete
- artwork remains legible when reduced to thumbnail size

These are production targets, not universal mathematical constants. QA may allow justified exceptions.
