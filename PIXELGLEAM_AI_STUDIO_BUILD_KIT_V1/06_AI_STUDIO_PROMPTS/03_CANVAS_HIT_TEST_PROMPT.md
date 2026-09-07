# Prompt 03 — Canvas, Pan/Zoom and Cell-ID Hit Testing

Implement only Phase A3.

Use the protected Happy Corgi V3 files:
- cell_id_map.png
- subject_mask.png
- artwork.json
- settled_texture.png

Requirements:
- render the unfinished artwork coordinate space,
- pan gesture,
- pinch zoom,
- correct screen → artwork inverse transform,
- Cell-ID lookup at transformed coordinate,
- no gameplay hit outside playable subject,
- drag/pinch must not also trigger a fill tap,
- optional small deterministic tap-forgiveness near boundaries.

Important:
- do not use point-in-polygon as the primary hit-test architecture,
- do not runtime flood-fill RGB colors,
- do not generate a rigid 160×160 visible square grid,
- Cell-ID topology remains hidden interaction truth.

Add transform/hit-test tests where practical.
Provide a debug mode that can visualize Cell IDs/regions without changing production visuals.

End with phase report.
