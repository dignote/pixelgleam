# App-ready Artwork Package Contract V1

Recommended package:

artwork_<id>/
- master_preview.png
- thumbnail.png
- settled_texture.png
- subject_mask.png
- void_mask.png
- semantic_color_mask.png
- cell_id_map.png
- palette.json
- artwork.json
- qa_report.json

## Runtime truth
- Cell-ID raster: interaction lookup.
- Region graph in artwork.json: gameplay completion truth.
- settled_texture: completed visual truth.

The Android app must consume this frozen package and must not re-segment or regenerate topology at runtime.
