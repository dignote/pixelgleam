#!/usr/bin/env python3
"""
PixelGleam Build-Time Artwork Catalog & Thumbnail Generator.

Extracts metadata and topology counts directly from validated artwork.json packages,
generates 256x256 deterministic thumbnail.webp images, and outputs a canonical catalog.json.
"""

import hashlib
import json
import os
import subprocess
import sys

BASE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
ARTWORKS_DIR = os.path.join(BASE_DIR, "app", "src", "main", "assets", "artworks")
CATALOG_PATH = os.path.join(ARTWORKS_DIR, "catalog.json")

def map_category_to_label(category_id: str, raw_category: str) -> str:
    key = (category_id if category_id else raw_category).lower()
    if "animal" in key or "pet" in key:
        return "Animals & Companions"
    if "object" in key or "scene" in key:
        return "Objects & Scenes"
    if "symmetry" in key or "pattern" in key:
        return "Symmetry & Patterns"
    if "food" in key:
        return "Food & Still Life"
    if "character" in key or "avatar" in key:
        return "Characters & Avatars"
    if "botanical" in key or "nature" in key or "flower" in key:
        return "Botanical & Nature"
    if "benchmark" in key or "geometric" in key:
        return "Symmetric Geometric"
    return "General"

def generate_thumbnail(texture_path: str, thumbnail_path: str) -> bool:
    """Generates a deterministic 256x256 WebP thumbnail using convert or ffmpeg."""
    if not os.path.exists(texture_path):
        print(f"ERROR: Settled texture missing: {texture_path}", file=sys.stderr)
        return False

    cmd = [
        "convert",
        texture_path,
        "-resize", "256x256",
        "-quality", "85",
        "-define", "webp:method=6",
        thumbnail_path
    ]
    try:
        subprocess.run(cmd, check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        return True
    except Exception as e:
        print(f"ERROR generating thumbnail for {texture_path}: {e}", file=sys.stderr)
        return False

def main():
    if not os.path.isdir(ARTWORKS_DIR):
        print(f"ERROR: Artworks directory not found: {ARTWORKS_DIR}", file=sys.stderr)
        sys.exit(1)

    folders = sorted(os.listdir(ARTWORKS_DIR))
    artworks_list = []
    
    for folder in folders:
        folder_path = os.path.join(ARTWORKS_DIR, folder)
        if not os.path.isdir(folder_path):
            continue

        artwork_json_path = os.path.join(folder_path, "artwork.json")
        if not os.path.exists(artwork_json_path):
            # Skip non-artwork directories or legacy incomplete packages
            print(f"INFO: Skipping {folder} (no artwork.json)")
            continue

        # Incomplete legacy package check
        if folder == "gentle_bear":
            print("INFO: Skipping legacy incomplete package 'gentle_bear'")
            continue

        try:
            with open(artwork_json_path, "r", encoding="utf-8") as f:
                data = json.load(f)
        except Exception as e:
            print(f"ERROR: Failed to read {artwork_json_path}: {e}", file=sys.stderr)
            sys.exit(1)

        aid = data.get("id", folder)
        if aid != folder:
            print(f"ERROR: Folder name '{folder}' does not match artwork id '{aid}'", file=sys.stderr)
            sys.exit(1)

        title = data.get("title", aid.replace("_", " ").title())
        category_id = data.get("categoryId", data.get("category", "general"))
        category_label = data.get("categoryLabel", map_category_to_label(category_id, data.get("category", "")))
        sort_order = data.get("sortOrder", 2147483647)
        release_order = data.get("releaseOrder", 2147483647)
        version = data.get("version", "1.0")

        cells = data.get("cells", [])
        regions = data.get("regions", [])
        total_cells = len(cells)
        total_regions = len(regions)

        if total_cells == 0 or total_regions == 0:
            print(f"ERROR: Package {aid} has 0 cells or regions", file=sys.stderr)
            sys.exit(1)

        # Generate thumbnail.webp if missing or outdated
        texture_path = os.path.join(folder_path, "settled_texture.png")
        thumbnail_path = os.path.join(folder_path, "thumbnail.webp")
        if not os.path.exists(thumbnail_path) or os.path.getmtime(texture_path) > os.path.getmtime(thumbnail_path):
            print(f"Generating thumbnail for {aid}...")
            ok = generate_thumbnail(texture_path, thumbnail_path)
            if not ok:
                sys.exit(1)

        artworks_list.append({
            "id": aid,
            "title": title,
            "categoryId": category_id,
            "categoryLabel": category_label,
            "sortOrder": sort_order,
            "releaseOrder": release_order,
            "totalCells": total_cells,
            "totalRegions": total_regions,
            "assetFolder": f"artworks/{aid}",
            "thumbnail": "thumbnail.webp",
            "version": version
        })

    # Deterministic sorting: sortOrder -> releaseOrder -> id
    artworks_list.sort(key=lambda item: (item["sortOrder"], item["releaseOrder"], item["id"]))

    # Use deterministic schema representation
    catalog_obj = {
        "schemaVersion": 1,
        "artworks": artworks_list
    }

    catalog_content = json.dumps(catalog_obj, indent=2, ensure_ascii=False) + "\n"
    with open(CATALOG_PATH, "w", encoding="utf-8") as f:
        f.write(catalog_content)

    print(f"SUCCESS: Generated catalog.json with {len(artworks_list)} artworks at {CATALOG_PATH}")

if __name__ == "__main__":
    main()
