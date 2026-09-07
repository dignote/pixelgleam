import os
import json
from collections import defaultdict, deque
from repair_topology import read_png_fast, compute_safe_anchor

def generate_anchors_for_artwork(folder_name):
    folder_path = os.path.join("app/src/main/assets/artworks", folder_name)
    art_json_path = os.path.join(folder_path, "artwork.json")
    cell_map_path = os.path.join(folder_path, "cell_id_map.png")
    safe_anchors_path = os.path.join(folder_path, "safe_anchors.json")
    
    if not os.path.exists(art_json_path) or not os.path.exists(cell_map_path):
        return
        
    with open(art_json_path, 'r', encoding='utf-8') as f:
        art = json.load(f)
        
    w, h, cell_data = read_png_fast(cell_map_path)
    total_pixels = w * h
    
    cells = art.get('cells', [])
    regions = art.get('regions', [])
    cell_map = {c['id']: c for c in cells}
    
    # Collect cell pixels
    cell_pixels = defaultdict(list)
    for i in range(total_pixels):
        cid = cell_data[i]
        if cid > 0:
            cell_pixels[cid].append(i)
            
    print(f"Generating anchors for {folder_name} ({len(cells)} cells, {len(regions)} regions)...")
    
    cell_anchors_dict = {}
    for cell in cells:
        cid = cell['id']
        pixels = cell_pixels.get(cid, [])
        if not pixels:
            anc = {"x": cell.get('centerX', 0.5), "y": cell.get('centerY', 0.5), "clearance": 0.0}
        else:
            def is_cell_boundary(x, y):
                idx = y * w + x
                return (cell_data[idx - 1] != cid or
                        cell_data[idx + 1] != cid or
                        cell_data[idx - w] != cid or
                        cell_data[idx + w] != cid)
            ax, ay, clearance = compute_safe_anchor(pixels, w, h, is_cell_boundary)
            anc = {"x": round(ax, 2), "y": round(ay, 2), "clearance": round(clearance, 2)}
        cell['safeAnchor'] = anc
        cell_anchors_dict[str(cid)] = anc
        
    # Region raster
    region_raster = [0] * total_pixels
    for cell in cells:
        rid = cell['regionId']
        for p in cell_pixels.get(cell['id'], []):
            region_raster[p] = rid
            
    region_anchors_dict = {}
    for reg in regions:
        rid = reg['id']
        reg_pixels = []
        for cid in reg.get('cellIds', []):
            reg_pixels.extend(cell_pixels.get(cid, []))
            
        if not reg_pixels:
            anc = {"x": 0.5, "y": 0.5, "clearance": 0.0}
        else:
            def is_region_boundary(x, y):
                idx = y * w + x
                return (region_raster[idx - 1] != rid or
                        region_raster[idx + 1] != rid or
                        region_raster[idx - w] != rid or
                        region_raster[idx + w] != rid)
            ax, ay, clearance = compute_safe_anchor(reg_pixels, w, h, is_region_boundary)
            anc = {"x": round(ax, 2), "y": round(ay, 2), "clearance": round(clearance, 2)}
        reg['safeAnchor'] = anc
        region_anchors_dict[str(rid)] = anc
        
    with open(safe_anchors_path, 'w', encoding='utf-8') as f:
        json.dump({
            "artworkId": art.get("id", folder_name),
            "cells": cell_anchors_dict,
            "regions": region_anchors_dict
        }, f, indent=2)
        
    with open(art_json_path, 'w', encoding='utf-8') as f:
        json.dump(art, f, indent=2)

if __name__ == '__main__':
    for art in ["happy_corgi_v3", "B2_flower", "B3_mandala", "B4_food", "B5_character"]:
        generate_anchors_for_artwork(art)
