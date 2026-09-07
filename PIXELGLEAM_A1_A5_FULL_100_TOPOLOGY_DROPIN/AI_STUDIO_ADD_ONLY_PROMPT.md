Use this pack as authoritative A1–A5 topology.

Integration only. Do not regenerate or modify topology.

Replace the matching A1–A5 assets in app/src/main/assets/artworks/.

Required invariant:
every canvas pixel is playable and has a Cell ID; coverage = 100%.

Keep supplied artwork.json, cell_id_map.png, region_id_map.png, subject_mask.png,
safe_anchors.json, settled_texture.png.

Run validator and playtest all five.
Reject any integration result where coverage is below 100% or any Cell/Region/anchor invariant fails.
