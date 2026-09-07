import os
import json
import zlib
import struct
from collections import defaultdict, deque

def read_png(path):
    with open(path, 'rb') as f:
        data = f.read()
    if data[:8] != b'\x89PNG\r\n\x1a\n':
        raise ValueError(f"Not a valid PNG: {path}")
    
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
    
    # Check bit depth & color type
    # Grayscale 16-bit: color_type 0, bit_depth 16 -> 2 bytes per pixel
    # Grayscale 8-bit: color_type 0, bit_depth 8 -> 1 byte per pixel
    # RGBA 8-bit: color_type 6, bit_depth 8 -> 4 bytes per pixel
    # RGB 8-bit: color_type 2, bit_depth 8 -> 3 bytes per pixel
    
    bpp = 0
    if color_type == 0:
        bpp = 2 if bit_depth == 16 else 1
    elif color_type == 2:
        bpp = 3 * (bit_depth // 8)
    elif color_type == 3:
        bpp = 1
    elif color_type == 4:
        bpp = 2 * (bit_depth // 8)
    elif color_type == 6:
        bpp = 4 * (bit_depth // 8)
    else:
        raise ValueError(f"Unsupported color_type: {color_type}")
        
    stride = width * bpp
    raw_pixels = bytearray(width * height * bpp)
    
    in_idx = 0
    prev_row = bytearray(stride)
    curr_row = bytearray(stride)
    
    for y in range(height):
        filter_type = decompressed[in_idx]
        in_idx += 1
        curr_raw = decompressed[in_idx:in_idx+stride]
        in_idx += stride
        
        if filter_type == 0:
            curr_row[:] = curr_raw
        elif filter_type == 1: # Sub
            for i in range(stride):
                left = curr_row[i - bpp] if i >= bpp else 0
                curr_row[i] = (curr_raw[i] + left) & 0xFF
        elif filter_type == 2: # Up
            for i in range(stride):
                up = prev_row[i]
                curr_row[i] = (curr_raw[i] + up) & 0xFF
        elif filter_type == 3: # Average
            for i in range(stride):
                left = curr_row[i - bpp] if i >= bpp else 0
                up = prev_row[i]
                curr_row[i] = (curr_raw[i] + ((left + up) // 2)) & 0xFF
        elif filter_type == 4: # Paeth
            for i in range(stride):
                left = curr_row[i - bpp] if i >= bpp else 0
                up = prev_row[i]
                left_up = prev_row[i - bpp] if i >= bpp else 0
                p = left + up - left_up
                pa = abs(p - left)
                pb = abs(p - up)
                pc = abs(p - left_up)
                if pa <= pb and pa <= pc:
                    pr = left
                elif pb <= pc:
                    pr = up
                else:
                    pr = left_up
                curr_row[i] = (curr_raw[i] + pr) & 0xFF
        else:
            raise ValueError(f"Unknown filter: {filter_type}")
            
        raw_pixels[y*stride:(y+1)*stride] = curr_row
        prev_row[:] = curr_row
        
    if color_type == 0 and bit_depth == 16:
        # Convert to 1D int array of 16-bit IDs
        pixels = [0] * (width * height)
        for i in range(width * height):
            pixels[i] = (raw_pixels[i*2] << 8) | raw_pixels[i*2+1]
        return width, height, pixels, 16
    elif color_type == 0 and bit_depth == 8:
        return width, height, list(raw_pixels), 8
    elif color_type == 6 and bit_depth == 8: # RGBA
        # Return list of (r, g, b, a)
        pixels = []
        for i in range(width * height):
            pixels.append((raw_pixels[i*4], raw_pixels[i*4+1], raw_pixels[i*4+2], raw_pixels[i*4+3]))
        return width, height, pixels, 32
    else:
        return width, height, raw_pixels, bit_depth

def audit_artwork(folder_path):
    print(f"\n==========================================")
    print(f"AUDITING: {folder_path}")
    print(f"==========================================")
    
    art_path = os.path.join(folder_path, 'artwork.json')
    cell_map_path = os.path.join(folder_path, 'cell_id_map.png')
    mask_path = os.path.join(folder_path, 'subject_mask.png')
    
    if not os.path.exists(art_path) or not os.path.exists(cell_map_path):
        print(f"Missing essential files in {folder_path}")
        return None
        
    with open(art_path, 'r', encoding='utf-8') as f:
        art = json.load(f)
        
    w, h, cell_data, bit_depth = read_png(cell_map_path)
    
    mask_data = None
    if os.path.exists(mask_path):
        mw, mh, mdata, mdepth = read_png(mask_path)
        mask_data = mdata
        
    json_cells = art.get('cells', [])
    json_regions = art.get('regions', [])
    json_cell_ids = {c['id'] for c in json_cells}
    json_cell_map = {c['id']: c for c in json_cells}
    json_region_map = {r['id']: r for r in json_regions}
    
    raster_cell_ids = {cid for cid in cell_data if cid > 0}
    
    ghost_json_cells = json_cell_ids - raster_cell_ids
    missing_json_cells = raster_cell_ids - json_cell_ids
    
    # Check 4-connected components for each raster cell
    cell_pixels = defaultdict(list)
    for idx, cid in enumerate(cell_data):
        if cid > 0:
            cell_pixels[cid].append(idx)
            
    disconnected_cells = []
    for cid, pixels in cell_pixels.items():
        # BFS connected components
        rem = set(pixels)
        comps = 0
        while rem:
            comps += 1
            start = rem.pop()
            q = deque([start])
            while q:
                curr = q.popleft()
                cx = curr % w
                cy = curr // w
                for nx, ny in [(cx-1, cy), (cx+1, cy), (cx, cy-1), (cx, cy+1)]:
                    if 0 <= nx < w and 0 <= ny < h:
                        nidx = ny * w + nx
                        if nidx in rem and cell_data[nidx] == cid:
                            rem.remove(nidx)
                            q.append(nidx)
        if comps > 1:
            disconnected_cells.append((cid, comps))
            
    # Check raster 4-neighbor adjacency between cells
    cell_adj = defaultdict(set)
    for y in range(h):
        for x in range(w):
            idx = y * w + x
            cid1 = cell_data[idx]
            if cid1 <= 0:
                continue
            if x + 1 < w:
                cid2 = cell_data[idx + 1]
                if cid2 > 0 and cid2 != cid1:
                    cell_adj[cid1].add(cid2)
                    cell_adj[cid2].add(cid1)
            if y + 1 < h:
                cid3 = cell_data[idx + w]
                if cid3 > 0 and cid3 != cid1:
                    cell_adj[cid1].add(cid3)
                    cell_adj[cid3].add(cid1)
                    
    # Check Region connectivity and same-color maximal components
    region_cells = defaultdict(list)
    for c in json_cells:
        region_cells[c['regionId']].append(c['id'])
        
    disconnected_regions = []
    for rid, cids in region_cells.items():
        allowed = set(cids)
        seen = set()
        if cids:
            q = deque([cids[0]])
            while q:
                curr = q.popleft()
                if curr in seen:
                    continue
                seen.add(curr)
                for n in cell_adj.get(curr, set()):
                    if n in allowed and n not in seen:
                        q.append(n)
        if seen != allowed:
            disconnected_regions.append((rid, len(allowed), len(seen)))
            
    # Check same color touching cells in different regions
    same_color_touching_diff_regions = []
    for cid1, neighbors in cell_adj.items():
        c1 = json_cell_map.get(cid1)
        if not c1:
            continue
        for cid2 in neighbors:
            c2 = json_cell_map.get(cid2)
            if not c2:
                continue
            if c1['colorId'] == c2['colorId'] and c1['regionId'] != c2['regionId']:
                if (min(cid1, cid2), max(cid1, cid2)) not in same_color_touching_diff_regions:
                    same_color_touching_diff_regions.append((min(cid1, cid2), max(cid1, cid2)))
                    
    # Subject mask check
    subject_violations = 0
    if mask_data:
        for idx, cid in enumerate(cell_data):
            if cid > 0:
                mv = mask_data[idx]
                if isinstance(mv, tuple): # RGBA
                    if mv[3] == 0 or (mv[0] + mv[1] + mv[2] == 0):
                        subject_violations += 1
                else:
                    if mv < 128:
                        subject_violations += 1
                        
    print(f"Canvas size: {w}x{h}")
    print(f"JSON Cells count: {len(json_cells)}, Raster Cells count: {len(raster_cell_ids)}")
    print(f"Declared Regions count: {len(json_regions)}")
    print(f"Ghost JSON Cells: {sorted(list(ghost_json_cells))}")
    print(f"Missing JSON Cells: {sorted(list(missing_json_cells))}")
    print(f"Disconnected Cells: {disconnected_cells}")
    print(f"Disconnected Regions: {len(disconnected_regions)} -> {disconnected_regions[:10]}")
    print(f"Same-color touching different regions (Maximal violation): {len(same_color_touching_diff_regions)} pairs")
    print(f"Subject mask violations: {subject_violations}")

if __name__ == '__main__':
    artworks = [
        "A1_blossom_bicycle",
        "A2_royal_koi_pair",
        "A3_cozy_tea_corner",
        "A4_moon_fairy_portrait",
        "A5_lucky_shiba_garden",
        "happy_corgi_v3",
        "B2_flower",
        "B3_mandala",
        "B4_food",
        "B5_character",
        "gentle_bear_v2"
    ]
    for art in artworks:
        path = os.path.join("app/src/main/assets/artworks", art)
        if os.path.exists(path):
            audit_artwork(path)
