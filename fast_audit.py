import os
import sys
import json
import zlib
import struct
from collections import defaultdict, deque

def read_png_fast(path):
    with open(path, 'rb') as f:
        data = f.read()
    
    idx = 8
    width = height = bit_depth = color_type = None
    idat_chunks = []
    while idx < len(data):
        length = struct.unpack('>I', data[idx:idx+4])[0]
        chunk_type = data[idx+4:idx+8]
        chunk_data = data[idx+8:idx+8+length]
        idx += 12 + length
        
        if chunk_type == b'IHDR':
            width, height, bit_depth, color_type = struct.unpack('>IIBB', chunk_data[:10])
        elif chunk_type == b'IDAT':
            idat_chunks.append(chunk_data)
        elif chunk_type == b'IEND':
            break
            
    decompressed = zlib.decompress(b''.join(idat_chunks))
    bpp = 2 if (color_type == 0 and bit_depth == 16) else (1 if color_type == 0 else 4)
    stride = width * bpp
    raw = bytearray(width * height * bpp)
    in_ptr = 0
    out_ptr = 0
    
    for y in range(height):
        ft = decompressed[in_ptr]
        in_ptr += 1
        scanline = bytearray(decompressed[in_ptr:in_ptr+stride])
        in_ptr += stride
        
        if ft == 0:
            pass
        elif ft == 1:
            for i in range(bpp, stride):
                scanline[i] = (scanline[i] + scanline[i - bpp]) & 0xFF
        elif ft == 2:
            if y > 0:
                prev = raw[out_ptr - stride : out_ptr]
                for i in range(stride):
                    scanline[i] = (scanline[i] + prev[i]) & 0xFF
        elif ft == 3:
            prev = raw[out_ptr - stride : out_ptr] if y > 0 else bytearray(stride)
            for i in range(stride):
                left = scanline[i - bpp] if i >= bpp else 0
                scanline[i] = (scanline[i] + ((left + prev[i]) >> 1)) & 0xFF
        elif ft == 4:
            prev = raw[out_ptr - stride : out_ptr] if y > 0 else bytearray(stride)
            for i in range(stride):
                left = scanline[i - bpp] if i >= bpp else 0
                up = prev[i]
                left_up = prev[i - bpp] if (i >= bpp and y > 0) else 0
                p = left + up - left_up
                pa = abs(p - left)
                pb = abs(p - up)
                pc = abs(p - left_up)
                pr = left if (pa <= pb and pa <= pc) else (up if pb <= pc else left_up)
                scanline[i] = (scanline[i] + pr) & 0xFF
                
        raw[out_ptr : out_ptr + stride] = scanline
        out_ptr += stride
        
    if color_type == 0 and bit_depth == 16:
        pixels = [0] * (width * height)
        for i in range(width * height):
            pixels[i] = (raw[i*2] << 8) | raw[i*2+1]
        return width, height, pixels
    elif color_type == 0 and bit_depth == 8:
        return width, height, list(raw)
    elif color_type == 6:
        # RGBA
        pixels = []
        for i in range(width * height):
            pixels.append((raw[i*4], raw[i*4+1], raw[i*4+2], raw[i*4+3]))
        return width, height, pixels
    return width, height, raw

def audit_artwork(folder_name):
    folder_path = os.path.join("app/src/main/assets/artworks", folder_name)
    art_json_path = os.path.join(folder_path, "artwork.json")
    cell_map_path = os.path.join(folder_path, "cell_id_map.png")
    mask_path = os.path.join(folder_path, "subject_mask.png")
    safe_anchors_path = os.path.join(folder_path, "safe_anchors.json")
    
    with open(art_json_path, 'r', encoding='utf-8') as f:
        art = json.load(f)
        
    w, h, cell_data = read_png_fast(cell_map_path)
    total_pixels = w * h
    
    mask_data = None
    if os.path.exists(mask_path):
        mw, mh, mask_data = read_png_fast(mask_path)
        
    anchors_data = None
    if os.path.exists(safe_anchors_path):
        with open(safe_anchors_path, 'r', encoding='utf-8') as f:
            anchors_data = json.load(f)

    json_cells = art.get('cells', [])
    json_regions = art.get('regions', [])
    json_cell_ids = {c['id'] for c in json_cells}
    json_cell_map = {c['id']: c for c in json_cells}
    json_region_map = {r['id']: r for r in json_regions}
    
    raster_cell_counts = defaultdict(int)
    for v in cell_data:
        if v > 0:
            raster_cell_counts[v] += 1
            
    raster_cell_ids = set(raster_cell_counts.keys())
    
    ghost_json_cells = json_cell_ids - raster_cell_ids
    missing_json_cells = raster_cell_ids - json_cell_ids
    
    # 1-pass fast 4-connected component count for all cells in raster
    visited = bytearray(total_pixels)
    cell_component_counts = defaultdict(int)
    cell_components_map = defaultdict(list) # cid -> list of components (each is list of pixel indices or count)
    
    for i in range(total_pixels):
        if cell_data[i] > 0 and not visited[i]:
            cid = cell_data[i]
            cell_component_counts[cid] += 1
            # BFS to mark component
            q = deque([i])
            visited[i] = 1
            comp_pixels = [i]
            while q:
                curr = q.popleft()
                cx = curr % w
                cy = curr // w
                if cx > 0:
                    n = curr - 1
                    if not visited[n] and cell_data[n] == cid:
                        visited[n] = 1; q.append(n); comp_pixels.append(n)
                if cx < w - 1:
                    n = curr + 1
                    if not visited[n] and cell_data[n] == cid:
                        visited[n] = 1; q.append(n); comp_pixels.append(n)
                if cy > 0:
                    n = curr - w
                    if not visited[n] and cell_data[n] == cid:
                        visited[n] = 1; q.append(n); comp_pixels.append(n)
                if cy < h - 1:
                    n = curr + w
                    if not visited[n] and cell_data[n] == cid:
                        visited[n] = 1; q.append(n); comp_pixels.append(n)
            cell_components_map[cid].append(comp_pixels)
            
    disconnected_cells = [(cid, cnt) for cid, cnt in cell_component_counts.items() if cnt > 1]
    
    # Raster 4-neighbor adjacency between cells
    cell_adj = defaultdict(set)
    for y in range(h):
        row = y * w
        for x in range(w):
            idx = row + x
            c1 = cell_data[idx]
            if c1 <= 0:
                continue
            if x + 1 < w:
                c2 = cell_data[idx + 1]
                if c2 > 0 and c2 != c1:
                    cell_adj[c1].add(c2)
                    cell_adj[c2].add(c1)
            if y + 1 < h:
                c3 = cell_data[idx + w]
                if c3 > 0 and c3 != c1:
                    cell_adj[c1].add(c3)
                    cell_adj[c3].add(c1)
                    
    # Check Region connectivity
    region_cells = defaultdict(list)
    for c in json_cells:
        region_cells[c['regionId']].append(c['id'])
        
    disconnected_regions = []
    for rid, cids in region_cells.items():
        if not cids:
            continue
        allowed = set(cids)
        seen = set()
        q = deque([cids[0]])
        seen.add(cids[0])
        while q:
            curr = q.popleft()
            for n in cell_adj.get(curr, set()):
                if n in allowed and n not in seen:
                    seen.add(n); q.append(n)
        if seen != allowed:
            disconnected_regions.append(rid)
            
    # Same color touching cells in different regions
    same_color_touching_diff_regions = []
    for c1_id, neighbors in cell_adj.items():
        c1 = json_cell_map.get(c1_id)
        if not c1:
            continue
        for c2_id in neighbors:
            c2 = json_cell_map.get(c2_id)
            if not c2:
                continue
            if c1['colorId'] == c2['colorId'] and c1['regionId'] != c2['regionId']:
                pair = (min(c1_id, c2_id), max(c1_id, c2_id))
                if pair not in same_color_touching_diff_regions:
                    same_color_touching_diff_regions.append(pair)
                    
    # Check anchors
    cell_anchor_failures = []
    region_anchor_failures = []
    
    for c in json_cells:
        anc = None
        if anchors_data and 'cells' in anchors_data and str(c['id']) in anchors_data['cells']:
            anc = anchors_data['cells'][str(c['id'])]
        elif 'safeAnchor' in c and c['safeAnchor']:
            anc = c['safeAnchor']
        if anc:
            ax = int(anc['x'])
            ay = int(anc['y'])
            if ax < 0 or ax >= w or ay < 0 or ay >= h:
                cell_anchor_failures.append((c['id'], 'out_of_bounds', ax, ay))
            else:
                actual_cid = cell_data[ay * w + ax]
                if actual_cid != c['id']:
                    cell_anchor_failures.append((c['id'], 'mismatch', actual_cid, ax, ay))
        else:
            cell_anchor_failures.append((c['id'], 'missing'))
            
    for r in json_regions:
        anc = None
        if anchors_data and 'regions' in anchors_data and str(r['id']) in anchors_data['regions']:
            anc = anchors_data['regions'][str(r['id'])]
        elif 'safeAnchor' in r and r['safeAnchor']:
            anc = r['safeAnchor']
        if anc:
            ax = int(anc['x'])
            ay = int(anc['y'])
            if ax < 0 or ax >= w or ay < 0 or ay >= h:
                region_anchor_failures.append((r['id'], 'out_of_bounds', ax, ay))
            else:
                actual_cid = cell_data[ay * w + ax]
                c_obj = json_cell_map.get(actual_cid)
                if not c_obj or c_obj['regionId'] != r['id']:
                    region_anchor_failures.append((r['id'], 'mismatch', actual_cid, ax, ay))
        else:
            region_anchor_failures.append((r['id'], 'missing'))
            
    # Subject mask
    subject_mask_violations = 0
    if mask_data:
        for idx, cid in enumerate(cell_data):
            if cid > 0:
                mv = mask_data[idx]
                if isinstance(mv, (list, tuple)):
                    if mv[3] == 0 or (mv[0] + mv[1] + mv[2] == 0):
                        subject_mask_violations += 1
                else:
                    if mv < 128:
                        subject_mask_violations += 1
                        
    print(f"\n=======================================================")
    print(f"AUDIT FOR: {folder_name}")
    print(f"Dimensions: {w}x{h}")
    print(f"JSON Cells: {len(json_cells)}, Raster Cells: {len(raster_cell_ids)}")
    print(f"Declared Regions: {len(json_regions)}")
    print(f"Ghost JSON Cells (in JSON but not raster): {sorted(list(ghost_json_cells))}")
    print(f"Missing JSON Cells (in raster but not JSON): {sorted(list(missing_json_cells))}")
    print(f"Disconnected Cells: {disconnected_cells}")
    print(f"Disconnected Regions count: {len(disconnected_regions)} -> {disconnected_regions[:10]}")
    print(f"Maximal-Region touching same-color violations: {len(same_color_touching_diff_regions)} pairs")
    print(f"Cell Anchor failures: {len(cell_anchor_failures)}")
    print(f"Region Anchor failures: {len(region_anchor_failures)}")
    print(f"Subject Mask violations: {subject_mask_violations}")
    print(f"=======================================================\n")
    
    return {
        "folder_name": folder_name,
        "width": w,
        "height": h,
        "json_cells_count": len(json_cells),
        "raster_cells_count": len(raster_cell_ids),
        "declared_regions_count": len(json_regions),
        "ghost_json_cells": sorted(list(ghost_json_cells)),
        "missing_json_cells": sorted(list(missing_json_cells)),
        "disconnected_cells": disconnected_cells,
        "disconnected_regions": disconnected_regions,
        "same_color_touching_diff_regions": len(same_color_touching_diff_regions),
        "cell_anchor_failures": len(cell_anchor_failures),
        "region_anchor_failures": len(region_anchor_failures),
        "subject_mask_violations": subject_mask_violations,
    }

if __name__ == '__main__':
    target = sys.argv[1] if len(sys.argv) > 1 else None
    if target:
        audit_artwork(target)
    else:
        for a in ["A1_blossom_bicycle", "A2_royal_koi_pair", "A3_cozy_tea_corner", "A4_moon_fairy_portrait", "A5_lucky_shiba_garden"]:
            audit_artwork(a)
