import os
import sys
import json
import zlib
import struct
import binascii
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
        pixels = []
        for i in range(width * height):
            pixels.append((raw[i*4], raw[i*4+1], raw[i*4+2], raw[i*4+3]))
        return width, height, pixels
    return width, height, raw

def write_png_16bit_grayscale(path, width, height, pixels):
    raw = bytearray()
    for y in range(height):
        raw.append(0) # Filter None
        row_offset = y * width
        for x in range(width):
            val = pixels[row_offset + x]
            raw.append((val >> 8) & 0xFF)
            raw.append(val & 0xFF)
            
    compressed = zlib.compress(raw, level=6)
    
    def make_chunk(chunk_type, data):
        length = struct.pack('>I', len(data))
        crc = struct.pack('>I', binascii.crc32(chunk_type + data) & 0xffffffff)
        return length + chunk_type + data + crc
        
    ihdr_data = struct.pack('>IIBBBBB', width, height, 16, 0, 0, 0, 0)
    ihdr_chunk = make_chunk(b'IHDR', ihdr_data)
    idat_chunk = make_chunk(b'IDAT', compressed)
    iend_chunk = make_chunk(b'IEND', b'')
    
    with open(path, 'wb') as f:
        f.write(b'\x89PNG\r\n\x1a\n')
        f.write(ihdr_chunk)
        f.write(idat_chunk)
        f.write(iend_chunk)

def compute_safe_anchor(pixels, width, height, is_boundary_check_fn):
    """
    Computes safe interior point using distance transform.
    pixels: list of 1D pixel indices
    is_boundary_check_fn: function(x, y) -> bool
    """
    if not pixels:
        return 0.5, 0.5, 0.0
        
    min_x = width
    max_x = 0
    min_y = height
    max_y = 0
    
    for p in pixels:
        x = p % width
        y = p // width
        if x < min_x: min_x = x
        if x > max_x: max_x = x
        if y < min_y: min_y = y
        if y > max_y: max_y = y
        
    bw = max_x - min_x + 1
    bh = max_y - min_y + 1
    grid = [-1] * (bw * bh)
    
    for p in pixels:
        x = p % width - min_x
        y = p // width - min_y
        grid[y * bw + x] = 999999
        
    queue = deque()
    for p in pixels:
        x = p % width
        y = p // width
        lx = x - min_x
        ly = y - min_y
        
        is_boundary = False
        if x == 0 or x == width - 1 or y == 0 or y == height - 1:
            is_boundary = True
        elif is_boundary_check_fn(x, y):
            is_boundary = True
            
        if is_boundary:
            grid[ly * bw + lx] = 0
            queue.append((ly, lx))
            
    max_dist = 0
    best_x = pixels[0] % width
    best_y = pixels[0] // width
    
    while queue:
        cy, cx = queue.popleft()
        d = grid[cy * bw + cx]
        if d > max_dist:
            max_dist = d
            best_x = cx + min_x
            best_y = cy + min_y
            
        for nx, ny in [(cx - 1, cy), (cx + 1, cy), (cx, cy - 1), (cx, cy + 1)]:
            if 0 <= nx < bw and 0 <= ny < bh:
                gidx = ny * bw + nx
                if grid[gidx] == 999999:
                    grid[gidx] = d + 1
                    queue.append((ny, nx))
                    
    return best_x + 0.5, best_y + 0.5, float(max_dist)

def repair_artwork(folder_name):
    folder_path = os.path.join("app/src/main/assets/artworks", folder_name)
    art_json_path = os.path.join(folder_path, "artwork.json")
    cell_map_path = os.path.join(folder_path, "cell_id_map.png")
    mask_path = os.path.join(folder_path, "subject_mask.png")
    safe_anchors_path = os.path.join(folder_path, "safe_anchors.json")
    meta_path = os.path.join(folder_path, "metadata.json")
    
    with open(art_json_path, 'r', encoding='utf-8') as f:
        art = json.load(f)
        
    w, h, cell_data = read_png_fast(cell_map_path)
    total_pixels = w * h
    
    original_cells = art.get('cells', [])
    original_cell_map = {c['id']: c for c in original_cells}
    
    # 1. Audit before
    before_cell_count = len(original_cells)
    before_regions_count = len(art.get('regions', []))
    
    # Extract connected components from the raster
    # Group connected components of raster cells
    visited = bytearray(total_pixels)
    components = [] # list of dict(orig_cid, pixels)
    
    for i in range(total_pixels):
        cid = cell_data[i]
        if cid > 0 and not visited[i]:
            comp_pixels = [i]
            visited[i] = 1
            q = deque([i])
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
            components.append({
                'orig_cid': cid,
                'pixels': comp_pixels,
                'min_pixel': min(comp_pixels),
                'count': len(comp_pixels)
            })
            
    # Sort components deterministically:
    # First by orig_cid, then largest component first (count descending), then min_pixel ascending
    components.sort(key=lambda c: (c['orig_cid'], -c['count'], c['min_pixel']))
    
    # Assign new consecutive Cell IDs (1..N)
    new_cell_data = [0] * total_pixels
    new_cells_list = []
    
    for new_id, comp in enumerate(components, start=1):
        orig_cid = comp['orig_cid']
        orig_cell = original_cell_map.get(orig_cid, {})
        color_id = orig_cell.get('colorId', 1)
        semantic_part = orig_cell.get('semanticPartId', orig_cell.get('semanticGroup', f"part_{orig_cid}"))
        
        # Paint raster with new Cell ID
        for p in comp['pixels']:
            new_cell_data[p] = new_id
            
        # Compute centroid
        cx_sum = sum(p % w for p in comp['pixels'])
        cy_sum = sum(p // w for p in comp['pixels'])
        centroid_x = cx_sum / len(comp['pixels']) + 0.5
        centroid_y = cy_sum / len(comp['pixels']) + 0.5
        
        new_cells_list.append({
            'id': new_id,
            'orig_cid': orig_cid,
            'colorId': color_id,
            'semanticPartId': semantic_part,
            'centerX': centroid_x,
            'centerY': centroid_y,
            'pixels': comp['pixels']
        })
        
    # 2. Derive true 4-neighbor adjacency between newly numbered cells
    cell_adj = defaultdict(set)
    for y in range(h):
        row = y * w
        for x in range(w):
            idx = row + x
            c1 = new_cell_data[idx]
            if c1 <= 0:
                continue
            if x + 1 < w:
                c2 = new_cell_data[idx + 1]
                if c2 > 0 and c2 != c1:
                    cell_adj[c1].add(c2)
                    cell_adj[c2].add(c1)
            if y + 1 < h:
                c3 = new_cell_data[idx + w]
                if c3 > 0 and c3 != c1:
                    cell_adj[c1].add(c3)
                    cell_adj[c3].add(c1)
                    
    for c in new_cells_list:
        c['neighbors'] = sorted(list(cell_adj.get(c['id'], set())))
        
    # 3. Algorithmically derive maximal same-color connected components (Regions)
    # BFS across orthogonally adjacent cells with same colorId
    cell_by_id = {c['id']: c for c in new_cells_list}
    visited_cells = set()
    regions_list = []
    
    # Sort cells by ID to ensure deterministic region discovery
    for cell in new_cells_list:
        cid = cell['id']
        if cid in visited_cells:
            continue
        
        color = cell['colorId']
        region_cell_ids = []
        q = deque([cid])
        visited_cells.add(cid)
        
        while q:
            curr_id = q.popleft()
            region_cell_ids.append(curr_id)
            for n_id in cell_adj.get(curr_id, set()):
                if n_id not in visited_cells and cell_by_id[n_id]['colorId'] == color:
                    visited_cells.add(n_id)
                    q.append(n_id)
                    
        region_cell_ids.sort()
        regions_list.append({
            'colorId': color,
            'cellIds': region_cell_ids,
            'min_cell_id': min(region_cell_ids)
        })
        
    # Sort regions deterministically by min_cell_id
    regions_list.sort(key=lambda r: r['min_cell_id'])
    
    # Assign region IDs 1..R
    for r_idx, reg in enumerate(regions_list, start=1):
        reg['id'] = r_idx
        for cid in reg['cellIds']:
            cell_by_id[cid]['regionId'] = r_idx
            
    # 4. Compute Safe Anchors for all Cells and Regions
    print(f"Computing safe anchors for {len(new_cells_list)} cells and {len(regions_list)} regions...")
    
    # Cell anchors
    cell_anchors_dict = {}
    for cell in new_cells_list:
        cid = cell['id']
        def is_cell_boundary(x, y):
            idx = y * w + x
            return (new_cell_data[idx - 1] != cid or
                    new_cell_data[idx + 1] != cid or
                    new_cell_data[idx - w] != cid or
                    new_cell_data[idx + w] != cid)
                    
        ax, ay, clearance = compute_safe_anchor(cell['pixels'], w, h, is_cell_boundary)
        anc = {
            "x": round(ax, 2),
            "y": round(ay, 2),
            "clearance": round(clearance, 2)
        }
        cell['safeAnchor'] = anc
        cell_anchors_dict[str(cid)] = anc
        
    # Region raster for quick region boundary checking
    region_raster = [0] * total_pixels
    for cell in new_cells_list:
        rid = cell['regionId']
        for p in cell['pixels']:
            region_raster[p] = rid
            
    # Region anchors
    region_anchors_dict = {}
    for reg in regions_list:
        rid = reg['id']
        reg_pixels = []
        for cid in reg['cellIds']:
            reg_pixels.extend(cell_by_id[cid]['pixels'])
            
        def is_region_boundary(x, y):
            idx = y * w + x
            return (region_raster[idx - 1] != rid or
                    region_raster[idx + 1] != rid or
                    region_raster[idx - w] != rid or
                    region_raster[idx + w] != rid)
                    
        ax, ay, clearance = compute_safe_anchor(reg_pixels, w, h, is_region_boundary)
        anc = {
            "x": round(ax, 2),
            "y": round(ay, 2),
            "clearance": round(clearance, 2)
        }
        reg['safeAnchor'] = anc
        region_anchors_dict[str(rid)] = anc
        
    # 5. Build cleaned artwork.json
    final_cells_json = []
    for c in new_cells_list:
        cell_obj = {
            "id": c['id'],
            "centerX": round(c['centerX'], 2),
            "centerY": round(c['centerY'], 2),
            "colorId": c['colorId'],
            "regionId": c['regionId'],
            "neighbors": c['neighbors'],
            "safeAnchor": c['safeAnchor']
        }
        if 'semanticPartId' in c:
            cell_obj['semanticPartId'] = c['semanticPartId']
        final_cells_json.append(cell_obj)
        
    final_regions_json = []
    for r in regions_list:
        final_regions_json.append({
            "id": r['id'],
            "colorId": r['colorId'],
            "cellIds": r['cellIds'],
            "safeAnchor": r['safeAnchor']
        })
        
    repaired_art = {
        "id": art.get("id", folder_name),
        "title": art.get("title", folder_name),
        "version": "4.0-topology-repaired",
        "category": art.get("category", "repaired"),
        "canvas": {
            "width": w,
            "height": h
        },
        "topologyType": "MASTER_DERIVED_CELL_ID_MAP",
        "hitTest": "cell_id_map_lookup",
        "settledRender": "preauthored_master_texture_reveal",
        "flightRepresentation": art.get("flightRepresentation", "GEM_G02"),
        "palette": art.get("palette", []),
        "cells": final_cells_json,
        "regions": final_regions_json
    }
    
    # Save artwork.json
    with open(art_json_path, 'w', encoding='utf-8') as f:
        json.dump(repaired_art, f, indent=2)
        
    # Save safe_anchors.json
    anchors_file_content = {
        "artworkId": art.get("id", folder_name),
        "cells": cell_anchors_dict,
        "regions": region_anchors_dict
    }
    with open(safe_anchors_path, 'w', encoding='utf-8') as f:
        json.dump(anchors_file_content, f, indent=2)
        
    # Save cell_id_map.png
    write_png_16bit_grayscale(cell_map_path, w, h, new_cell_data)
    
    # Save metadata.json if present
    if os.path.exists(meta_path):
        try:
            with open(meta_path, 'r', encoding='utf-8') as f:
                meta = json.load(f)
            meta['totalCells'] = len(final_cells_json)
            meta['totalRegions'] = len(final_regions_json)
            with open(meta_path, 'w', encoding='utf-8') as f:
                json.dump(meta, f, indent=2)
        except Exception:
            pass
            
    print(f"\n=======================================================")
    print(f"REPAIR COMPLETE FOR: {folder_name}")
    print(f"Cells: {before_cell_count} -> {len(final_cells_json)}")
    print(f"Regions: {before_regions_count} -> {len(final_regions_json)}")
    print(f"=======================================================\n")
    
    return {
        "folder_name": folder_name,
        "before_cells": before_cell_count,
        "before_regions": before_regions_count,
        "after_cells": len(final_cells_json),
        "after_regions": len(final_regions_json)
    }

if __name__ == '__main__':
    target = sys.argv[1] if len(sys.argv) > 1 else "A1_blossom_bicycle"
    repair_artwork(target)
