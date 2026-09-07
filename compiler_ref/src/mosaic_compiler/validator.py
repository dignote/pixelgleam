from __future__ import annotations

import json
from collections import defaultdict, deque
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Any

from PIL import Image, ImageChops


@dataclass
class ValidationResult:
    status: str
    geometry: dict[str, Any]
    checks: dict[str, bool]
    blockers: list[str]
    majors: list[str]
    fixture: dict[str, Any]

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)


def _load_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def _neighbors4(x: int, y: int, w: int, h: int):
    if x > 0: yield x - 1, y
    if x + 1 < w: yield x + 1, y
    if y > 0: yield x, y - 1
    if y + 1 < h: yield x, y + 1


def _component_count_for_label(data: list[int], w: int, h: int, label: int, pixels: list[int]) -> int:
    if not pixels:
        return 0
    remaining = set(pixels)
    components = 0
    while remaining:
        components += 1
        start = remaining.pop()
        q = deque([start])
        while q:
            idx = q.popleft()
            x, y = idx % w, idx // w
            for nx, ny in _neighbors4(x, y, w, h):
                nidx = ny * w + nx
                if nidx in remaining and data[nidx] == label:
                    remaining.remove(nidx)
                    q.append(nidx)
    return components


def validate_artwork(package_dir: Path, expected_qa_path: Path | None = None, master_path: Path | None = None) -> ValidationResult:
    artwork_path = package_dir / "artwork.json"
    cell_map_path = package_dir / "cell_id_map.png"
    subject_path = package_dir / "subject_mask.png"
    settled_path = package_dir / "settled_texture.png"
    for p in (artwork_path, cell_map_path, subject_path, settled_path):
        if not p.exists():
            raise FileNotFoundError(p)

    artwork = _load_json(artwork_path)
    cell_map_im = Image.open(cell_map_path)
    subject_im = Image.open(subject_path).convert("L")
    settled_im = Image.open(settled_path).convert("RGBA")

    if cell_map_im.size != subject_im.size or settled_im.size != subject_im.size:
        raise ValueError("cell_id_map, subject_mask, and settled_texture dimensions must match")

    w, h = cell_map_im.size
    cmap = [int(v) for v in cell_map_im.getdata()]
    smask = [int(v) for v in subject_im.getdata()]

    subject_pixels = sum(1 for v in smask if v > 0)
    labeled_subject_pixels = sum(1 for m, c in zip(smask, cmap) if m > 0 and c > 0)
    uncovered = sum(1 for m, c in zip(smask, cmap) if m > 0 and c == 0)
    background_labeled = sum(1 for m, c in zip(smask, cmap) if m == 0 and c > 0)
    coverage = 100.0 if subject_pixels == 0 else labeled_subject_pixels * 100.0 / subject_pixels

    cells = artwork.get("cells", [])
    json_cell_ids = {int(c["id"]) for c in cells}
    raster_cell_ids = {v for v in cmap if v > 0}
    cell_ids_match = json_cell_ids == raster_cell_ids
    consecutive_cell_ids = raster_cell_ids == set(range(1, max(raster_cell_ids, default=0) + 1))

    pixels_by_cell: dict[int, list[int]] = defaultdict(list)
    for idx, cid in enumerate(cmap):
        if cid > 0:
            pixels_by_cell[cid].append(idx)

    disconnected_cells: list[int] = []
    for cid, pixels in pixels_by_cell.items():
        if _component_count_for_label(cmap, w, h, cid, pixels) != 1:
            disconnected_cells.append(cid)
    cell_connectivity = not disconnected_cells

    # Derive true raster adjacency from shared 4-neighbor boundaries.
    raster_adj: dict[int, set[int]] = {cid: set() for cid in raster_cell_ids}
    for y in range(h):
        row = y * w
        for x in range(w):
            a = cmap[row + x]
            if a <= 0:
                continue
            if x + 1 < w:
                b = cmap[row + x + 1]
                if b > 0 and b != a:
                    raster_adj[a].add(b); raster_adj[b].add(a)
            if y + 1 < h:
                b = cmap[row + w + x]
                if b > 0 and b != a:
                    raster_adj[a].add(b); raster_adj[b].add(a)

    json_adj: dict[int, set[int]] = {int(c["id"]): {int(n) for n in c.get("neighbors", [])} for c in cells}
    adjacency_symmetry = all(a in json_adj.get(b, set()) for a, ns in json_adj.items() for b in ns)
    adjacency_exact = all(json_adj.get(cid, set()) == raster_adj.get(cid, set()) for cid in raster_cell_ids) and set(json_adj) == raster_cell_ids

    # Region validation from cell graph, not polygon data.
    cell_by_id = {int(c["id"]): c for c in cells}
    region_cells: dict[int, list[int]] = defaultdict(list)
    region_color_consistency = True
    for c in cells:
        region_cells[int(c["regionId"])].append(int(c["id"]))
    disconnected_regions: list[int] = []
    for rid, ids in region_cells.items():
        colors = {int(cell_by_id[cid]["colorId"]) for cid in ids}
        if len(colors) != 1:
            region_color_consistency = False
        allowed = set(ids)
        seen = set()
        q = deque([ids[0]]) if ids else deque()
        while q:
            cid = q.popleft()
            if cid in seen: continue
            seen.add(cid)
            for n in raster_adj.get(cid, set()):
                if n in allowed and n not in seen:
                    q.append(n)
        if seen != allowed:
            disconnected_regions.append(rid)
    region_connectivity = not disconnected_regions and region_color_consistency

    # Validate that declared regions are exactly maximal same-color connected components.
    maximal_regions = True
    for cid, ns in raster_adj.items():
        c = cell_by_id.get(cid)
        if not c: continue
        for n in ns:
            nc = cell_by_id.get(n)
            if not nc: continue
            same_color = int(c["colorId"]) == int(nc["colorId"])
            same_region = int(c["regionId"]) == int(nc["regionId"])
            if same_color != same_region:
                maximal_regions = False
                break
        if not maximal_regions: break

    settled_exact = True
    settled_background_transparent = True
    if master_path is not None:
        master_im = Image.open(master_path).convert("RGBA")
        if master_im.size != settled_im.size:
            settled_exact = False
        else:
            master_px = list(master_im.getdata())
            settled_px = list(settled_im.getdata())
            # V3 package deliberately clears alpha outside subject_mask.
            # "Exact master" therefore means every PLAYABLE subject pixel is bit-exact
            # to the approved master; transparent packaging outside the subject is allowed.
            settled_exact = all(
                s == m for s, m, maskv in zip(settled_px, master_px, smask) if maskv > 0
            )
            settled_background_transparent = all(
                s[3] == 0 for s, maskv in zip(settled_px, smask) if maskv == 0
            )

    expected = _load_json(expected_qa_path) if expected_qa_path and expected_qa_path.exists() else None
    expected_match = True
    if expected:
        eg = expected.get("geometry", {})
        expected_match = (
            int(eg.get("cellCount", len(raster_cell_ids))) == len(raster_cell_ids)
            and int(eg.get("regionCount", len(region_cells))) == len(region_cells)
            and int(eg.get("uncoveredPixels", uncovered)) == uncovered
            and int(eg.get("backgroundLabeledPixels", background_labeled)) == background_labeled
        )

    checks = {
        "cellIdSetMatchesArtworkJson": cell_ids_match,
        "cellIdsConsecutive": consecutive_cell_ids,
        "cellConnectivity": cell_connectivity,
        "adjacencySymmetry": adjacency_symmetry,
        "adjacencyExactRasterVsJson": adjacency_exact,
        "regionColorConsistency": region_color_consistency,
        "regionConnectivity": region_connectivity,
        "regionsAreMaximalSameColorComponents": maximal_regions,
        "settledTextureMasterExact": settled_exact,
        "settledBackgroundTransparent": settled_background_transparent,
        "expectedGoldenMetricsMatch": expected_match,
    }

    blockers: list[str] = []
    if uncovered: blockers.append(f"uncovered subject pixels: {uncovered}")
    if background_labeled: blockers.append(f"background labeled pixels: {background_labeled}")
    if not cell_ids_match: blockers.append("Cell-ID set differs between raster and artwork.json")
    if not consecutive_cell_ids: blockers.append("Cell-IDs are not consecutive 1..N")
    if disconnected_cells: blockers.append(f"disconnected cells: {disconnected_cells[:20]}")
    if not adjacency_symmetry: blockers.append("artwork.json adjacency is not symmetric")
    if not adjacency_exact: blockers.append("artwork.json adjacency does not exactly match raster shared boundaries")
    if not region_color_consistency: blockers.append("one or more regions contain multiple colorIds")
    if disconnected_regions: blockers.append(f"disconnected regions: {disconnected_regions[:20]}")
    if not maximal_regions: blockers.append("regions are not maximal same-color connected components")
    if not settled_exact: blockers.append("settled_texture playable subject pixels are not an exact match to approved master")
    if not settled_background_transparent: blockers.append("settled_texture contains non-transparent pixels outside subject_mask")
    if not expected_match: blockers.append("computed metrics differ from golden expected QA")

    result = ValidationResult(
        status="PASS" if not blockers else "REWORK",
        geometry={
            "width": w,
            "height": h,
            "subjectPixels": subject_pixels,
            "coveragePercent": round(coverage, 6),
            "uncoveredPixels": uncovered,
            "backgroundLabeledPixels": background_labeled,
            "cellCount": len(raster_cell_ids),
            "regionCount": len(region_cells),
            "overlapPixels": 0,
        },
        checks=checks,
        blockers=blockers,
        majors=[],
        fixture={
            "artworkId": artwork.get("id"),
            "version": artwork.get("version"),
            "topologyType": artwork.get("topologyType"),
        },
    )
    return result
