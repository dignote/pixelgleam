from __future__ import annotations

import json
from collections import defaultdict, deque
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image, ImageDraw, ImageFont
from scipy import ndimage
from skimage.segmentation import slic, find_boundaries
from skimage.color import rgb2lab


CROSS4 = np.array([[0,1,0],[1,1,1],[0,1,0]], dtype=np.uint8)


@dataclass
class GenerationConfig:
    target_cells: int = 347
    compactness: float = 18.0
    sigma: float = 0.6
    min_size_factor: float = 0.25
    max_size_factor: float = 3.0
    seed: int = 1337


@dataclass
class GenerationResult:
    status: str
    cell_count: int
    region_count: int
    output_dir: str
    metrics: dict[str, Any]

    def to_dict(self):
        return asdict(self)


def load_author_color_mask_from_v3(package_dir: Path) -> np.ndarray:
    """Migration helper: derive approved color/semantic raster from a frozen V3 package.

    This consumes ONLY the approved cell->color assignment and raster coverage. The generated
    topology must not reuse old cell boundaries after this raster has been produced.
    """
    art = json.loads((package_dir / 'artwork.json').read_text(encoding='utf-8'))
    cmap = np.array(Image.open(package_dir / 'cell_id_map.png'), dtype=np.int32)
    lookup = {int(c['id']): int(c['colorId']) for c in art['cells']}
    out = np.zeros_like(cmap, dtype=np.uint8)
    for cid, color in lookup.items():
        out[cmap == cid] = color
    return out


def auto_palette_color_mask(master_path: Path, subject_mask_path: Path, palette: list[dict[str, Any]]) -> np.ndarray:
    """Experimental M6 helper: nearest-palette Lab assignment. Not human-approved by itself."""
    rgb = np.array(Image.open(master_path).convert('RGB'), dtype=np.float32) / 255.0
    subject = np.array(Image.open(subject_mask_path).convert('L')) > 0
    lab = rgb2lab(rgb)
    pal_rgb = np.array([[int(p['hex'][1:3],16), int(p['hex'][3:5],16), int(p['hex'][5:7],16)] for p in palette], dtype=np.float32) / 255.0
    pal_lab = rgb2lab(pal_rgb.reshape(1, -1, 3)).reshape(-1, 3)
    d = ((lab[...,None,:] - pal_lab[None,None,:,:]) ** 2).sum(axis=-1)
    ids = np.argmin(d, axis=-1).astype(np.uint8) + 1
    ids[~subject] = 0
    return ids


def _component_records(color_mask: np.ndarray):
    records = []
    for color in sorted(int(v) for v in np.unique(color_mask) if v > 0):
        labels, n = ndimage.label(color_mask == color, structure=CROSS4)
        for k in range(1, n + 1):
            m = labels == k
            records.append({'colorId': color, 'mask': m, 'area': int(m.sum())})
    return records


def _allocate_cells(records, target_cells: int):
    # Every semantic connected component is a gameplay Region and must receive >=1 cell.
    areas = np.array([r['area'] for r in records], dtype=np.float64)
    if target_cells < len(records):
        target_cells = len(records)
    raw = areas / max(1.0, areas.sum()) * target_cells
    alloc = np.maximum(1, np.floor(raw).astype(int))
    diff = int(target_cells - alloc.sum())
    frac = raw - np.floor(raw)
    if diff > 0:
        order = np.argsort(-frac)
        i = 0
        while diff > 0:
            alloc[order[i % len(order)]] += 1
            diff -= 1; i += 1
    elif diff < 0:
        order = np.argsort(frac)
        i = 0
        while diff < 0 and i < len(order) * 4:
            idx = order[i % len(order)]
            if alloc[idx] > 1:
                alloc[idx] -= 1; diff += 1
            i += 1
    return alloc


def _tessellate(master_rgb: np.ndarray, color_mask: np.ndarray, cfg: GenerationConfig):
    recs = _component_records(color_mask)
    alloc = _allocate_cells(recs, cfg.target_cells)
    out = np.zeros(color_mask.shape, dtype=np.int32)
    cell_color: dict[int,int] = {}
    cell_region: dict[int,int] = {}
    next_cell = 1
    for rid, (rec, requested) in enumerate(zip(recs, alloc)):
        m = rec['mask']; area = rec['area']; color = rec['colorId']
        if requested <= 1 or area < 40:
            out[m] = next_cell
            cell_color[next_cell] = color; cell_region[next_cell] = rid
            next_cell += 1
            continue
        ys, xs = np.where(m); y0,y1=ys.min(),ys.max()+1; x0,x1=xs.min(),xs.max()+1
        crop = master_rgb[y0:y1, x0:x1]
        cm = m[y0:y1, x0:x1]
        labels = slic(
            crop, n_segments=int(requested), compactness=cfg.compactness, sigma=cfg.sigma,
            mask=cm, start_label=1, convert2lab=True, enforce_connectivity=True,
            min_size_factor=cfg.min_size_factor, max_size_factor=cfg.max_size_factor,
            slic_zero=False,
        )
        for local in sorted(int(v) for v in np.unique(labels[cm]) if v > 0):
            target = out[y0:y1, x0:x1]
            target[labels == local] = next_cell
            cell_color[next_cell] = color; cell_region[next_cell] = rid
            next_cell += 1
    return out, cell_color, cell_region, recs



def _repair_disconnected_cells(cmap: np.ndarray, cell_color: dict[int,int], cell_region: dict[int,int]):
    """Merge disconnected islands into the best shared-boundary neighbor in the same semantic Region.

    Keeps authoritative Region semantics intact and prevents a single Cell-ID from representing
    multiple spatial islands. If an island has no eligible neighbor (pathological), it becomes a
    new Cell-ID in the same Region.
    """
    cmap = cmap.copy()
    next_id = max(cell_color, default=0) + 1
    original_ids = sorted(list(cell_color))
    for cid in original_ids:
        m = cmap == cid
        labels, n = ndimage.label(m, structure=CROSS4)
        if n <= 1:
            continue
        sizes = [(k, int((labels==k).sum())) for k in range(1,n+1)]
        sizes.sort(key=lambda x: x[1], reverse=True)
        # Largest component keeps cid. Smaller islands merge to same-region neighbor where possible.
        for k,_ in sizes[1:]:
            island = labels == k
            dil = ndimage.binary_dilation(island, structure=CROSS4)
            ring = dil & ~island
            neigh, counts = np.unique(cmap[ring], return_counts=True)
            candidates=[]
            for nid,count in zip(neigh.tolist(),counts.tolist()):
                nid=int(nid)
                if nid>0 and nid!=cid and cell_region.get(nid)==cell_region[cid]:
                    candidates.append((int(count),nid))
            if candidates:
                _,best=max(candidates)
                cmap[island]=best
            else:
                cmap[island]=next_id
                cell_color[next_id]=cell_color[cid]
                cell_region[next_id]=cell_region[cid]
                next_id+=1
    # Drop labels that vanished and compact IDs to 1..N deterministically.
    present=sorted(int(v) for v in np.unique(cmap) if v>0)
    remap={old:i+1 for i,old in enumerate(present)}
    new=np.zeros_like(cmap,dtype=np.int32)
    new_color={}; new_region={}
    for old,newid in remap.items():
        new[cmap==old]=newid
        new_color[newid]=cell_color[old]
        new_region[newid]=cell_region[old]
    return new,new_color,new_region

def _adjacency(cmap: np.ndarray):
    ids = sorted(int(v) for v in np.unique(cmap) if v > 0)
    adj = {cid:set() for cid in ids}
    h,w=cmap.shape
    # Vectorized shared boundaries.
    a=cmap[:,:-1]; b=cmap[:,1:]
    ys,xs=np.where((a>0)&(b>0)&(a!=b))
    for y,x in zip(ys.tolist(),xs.tolist()):
        u,v=int(a[y,x]),int(b[y,x]); adj[u].add(v); adj[v].add(u)
    a=cmap[:-1,:]; b=cmap[1:,:]
    ys,xs=np.where((a>0)&(b>0)&(a!=b))
    for y,x in zip(ys.tolist(),xs.tolist()):
        u,v=int(a[y,x]),int(b[y,x]); adj[u].add(v); adj[v].add(u)
    return adj


def _cell_center(mask: np.ndarray):
    # Distance-transform anchor stays inside cell and away from boundaries.
    dist = ndimage.distance_transform_edt(mask)
    y,x=np.unravel_index(int(np.argmax(dist)), dist.shape)
    return [float(x)+0.5, float(y)+0.5]


def _quality_metrics(cmap: np.ndarray, color_mask: np.ndarray):
    ids = sorted(int(v) for v in np.unique(cmap) if v>0)
    areas = np.array([(cmap==cid).sum() for cid in ids], dtype=np.float64)
    tiny_threshold = max(4.0, float(np.median(areas))*0.20)
    huge_threshold = float(np.median(areas))*3.5
    compact=[]
    for cid in ids:
        m=cmap==cid; area=float(m.sum())
        # 4-neighbor perimeter.
        er=ndimage.binary_erosion(m, structure=CROSS4, border_value=0)
        per=float((m & ~er).sum())
        compact.append(0.0 if per==0 else 4*np.pi*area/(per*per))
    # semantic purity is structurally 1 because each semantic component is tessellated independently.
    return {
        'cellAreaMedian': float(np.median(areas)), 'cellAreaMean': float(areas.mean()),
        'cellAreaCv': float(areas.std()/max(1e-9,areas.mean())),
        'cellAreaMin': int(areas.min()), 'cellAreaMax': int(areas.max()),
        'tinyCellCount': int((areas<tiny_threshold).sum()), 'hugeCellCount': int((areas>huge_threshold).sum()),
        'compactnessMedian': float(np.median(compact)), 'compactnessP10': float(np.percentile(compact,10)),
        'semanticBoundaryCrossingCells': 0,
    }


def _save_debug(master_rgba: Image.Image, cmap: np.ndarray, color_mask: np.ndarray, out_dir: Path):
    out_dir.mkdir(parents=True, exist_ok=True)
    rgb=np.array(master_rgba.convert('RGB'))
    boundary=find_boundaries(cmap, mode='inner') & (cmap>0)
    overlay=rgb.copy(); overlay[boundary]=[30,30,30]
    Image.fromarray(overlay).save(out_dir/'cell_boundary_overlay.png')
    # Color-id diagnostic, deterministic pseudo colors.
    lut=np.array([[0,0,0],[243,154,33],[247,236,221],[217,184,135],[55,45,39],[240,102,95],[236,182,61],[90,166,191]],dtype=np.uint8)
    cm_rgb=lut[np.clip(color_mask,0,len(lut)-1)]
    Image.fromarray(cm_rgb).save(out_dir/'semantic_color_mask.png')
    # cell ID debug colors
    ids=np.unique(cmap)
    dbg=np.zeros((*cmap.shape,3),dtype=np.uint8)
    for cid in ids:
        if cid<=0: continue
        dbg[cmap==cid]=[(cid*73)%256,(cid*151)%256,(cid*199)%256]
    Image.fromarray(dbg).save(out_dir/'cell_id_debug.png')


def generate_from_color_mask(master_path: Path, subject_mask_path: Path, color_mask: np.ndarray,
                             palette: list[dict[str,Any]], output_dir: Path,
                             artwork_id: str='happy_corgi_reconstructed_v1', title: str='Happy Corgi Reconstructed',
                             cfg: GenerationConfig|None=None) -> GenerationResult:
    cfg=cfg or GenerationConfig()
    np.random.seed(cfg.seed)
    master=Image.open(master_path).convert('RGBA')
    rgb=np.array(master.convert('RGB'), dtype=np.float32)/255.0
    subject=np.array(Image.open(subject_mask_path).convert('L'))>0
    if color_mask.shape != subject.shape: raise ValueError('color_mask dimensions differ from subject')
    color_mask=color_mask.copy(); color_mask[~subject]=0
    if np.any(subject & (color_mask==0)): raise ValueError('color mask leaves subject pixels unlabeled')
    cmap, cell_color, cell_region, recs = _tessellate(rgb, color_mask, cfg)
    cmap, cell_color, cell_region = _repair_disconnected_cells(cmap, cell_color, cell_region)
    # Guarantee exact subject coverage and no background leakage.
    cmap[~subject]=0
    if np.any(subject & (cmap==0)): raise RuntimeError('tessellation left uncovered subject pixels')
    adj=_adjacency(cmap)
    cells=[]
    for cid in sorted(cell_color):
        m=cmap==cid
        cells.append({'id':cid,'center':_cell_center(m),'colorId':cell_color[cid],
                      'regionId':cell_region[cid],'polygon':[], 'neighbors':sorted(adj[cid])})
    regions=[]
    byreg=defaultdict(list)
    for cid,rid in cell_region.items(): byreg[rid].append(cid)
    for rid in sorted(byreg):
        ids=sorted(byreg[rid]); regions.append({'id':rid,'colorId':cell_color[ids[0]],'cellIds':ids})
    output_dir.mkdir(parents=True,exist_ok=True)
    Image.fromarray(cmap.astype(np.uint16), mode='I;16').save(output_dir/'cell_id_map.png')
    Image.fromarray((subject.astype(np.uint8)*255), mode='L').save(output_dir/'subject_mask.png')
    settled=np.array(master)
    settled[~subject,3]=0
    Image.fromarray(settled,'RGBA').save(output_dir/'settled_texture.png')
    art={
        'id':artwork_id,'title':title,'version':'compiler-v1.0','category':'benchmark',
        'canvas':{'width':int(cmap.shape[1]),'height':int(cmap.shape[0])},
        'topologyType':'cell-id-raster-v1','hitTest':'cell-id-texture-lookup','settledRender':'authored-texture-reveal',
        'flightRepresentation':'GEM_G02','palette':palette,'cells':cells,'regions':regions,
        'assets':{'cellIdMap':'cell_id_map.png','subjectMask':'subject_mask.png','settledTexture':'settled_texture.png'},
        'colorSource':'author-assisted-semantic-color-mask'
    }
    (output_dir/'artwork.json').write_text(json.dumps(art,ensure_ascii=False,indent=2),encoding='utf-8')
    metrics=_quality_metrics(cmap,color_mask)
    metrics.update({'requestedCellTarget':cfg.target_cells,'actualCellCount':len(cells),'actualRegionCount':len(regions),
                    'coveragePercent':100.0,'backgroundLabeledPixels':0})
    qa_dir=output_dir/'qa'; _save_debug(master,cmap,color_mask,qa_dir)
    (qa_dir/'generation_metrics.json').write_text(json.dumps(metrics,indent=2),encoding='utf-8')
    return GenerationResult('PASS',len(cells),len(regions),str(output_dir),metrics)


# --- compiler-v1.1 factory benchmark optimizations ---
# Added after B2-B5 production benchmark:
# 1) bbox-local distance transform for cell centers
# 2) vectorized perimeter metrics
# 3) merge pathological micro-cells (<64 px) into same-region shared-boundary neighbors

_repair_disconnected_cells_v10 = _repair_disconnected_cells

def _cell_center(mask: np.ndarray):
    ys, xs = np.where(mask)
    if len(xs) == 0:
        return [0.5, 0.5]
    y0, y1 = int(ys.min()), int(ys.max()) + 1
    x0, x1 = int(xs.min()), int(xs.max()) + 1
    crop = mask[y0:y1, x0:x1]
    dist = ndimage.distance_transform_edt(crop)
    y, x = np.unravel_index(int(np.argmax(dist)), dist.shape)
    return [float(x + x0) + 0.5, float(y + y0) + 0.5]

def _quality_metrics(cmap: np.ndarray, color_mask: np.ndarray):
    maxid = int(cmap.max())
    areas = np.bincount(cmap.ravel(), minlength=maxid + 1)[1:].astype(np.float64)
    p = np.pad(cmap, 1, constant_values=0)
    center = p[1:-1, 1:-1]
    per = np.zeros(maxid + 1, dtype=np.float64)
    for neigh in (p[:-2,1:-1], p[2:,1:-1], p[1:-1,:-2], p[1:-1,2:]):
        exposed = (center > 0) & (center != neigh)
        bc = np.bincount(center[exposed].ravel(), minlength=maxid + 1)
        per[:len(bc)] += bc
    per = per[1:]
    compact = np.where(per > 0, 4*np.pi*areas/(per*per), 0.0)
    tiny_threshold = max(4.0, float(np.median(areas))*0.20)
    huge_threshold = float(np.median(areas))*3.5
    return {
        'cellAreaMedian': float(np.median(areas)),
        'cellAreaMean': float(areas.mean()),
        'cellAreaCv': float(areas.std()/max(1e-9,areas.mean())),
        'cellAreaMin': int(areas.min()),
        'cellAreaMax': int(areas.max()),
        'tinyCellCount': int((areas<tiny_threshold).sum()),
        'hugeCellCount': int((areas>huge_threshold).sum()),
        'compactnessMedian': float(np.median(compact)),
        'compactnessP10': float(np.percentile(compact,10)),
        'semanticBoundaryCrossingCells': 0,
    }

def _repair_disconnected_cells(cmap: np.ndarray, cell_color: dict[int,int], cell_region: dict[int,int]):
    cmap, cell_color, cell_region = _repair_disconnected_cells_v10(cmap, cell_color, cell_region)
    min_abs = 64
    for _ in range(4):
        maxid = int(cmap.max())
        areas = np.bincount(cmap.ravel(), minlength=maxid+1)
        tiny = [cid for cid in range(1,maxid+1) if 0 < areas[cid] < min_abs]
        if not tiny:
            break
        changed = False
        for cid in tiny:
            m = cmap == cid
            if not m.any():
                continue
            ring = ndimage.binary_dilation(m, structure=CROSS4) & ~m
            vals, counts = np.unique(cmap[ring], return_counts=True)
            candidates = []
            for nid, count in zip(vals.tolist(), counts.tolist()):
                nid = int(nid)
                if nid > 0 and nid != cid and cell_region.get(nid) == cell_region.get(cid):
                    candidates.append((int(count), int(areas[nid]), nid))
            if candidates:
                _, _, best = max(candidates)
                cmap[m] = best
                changed = True
        if not changed:
            break
        present = sorted(int(v) for v in np.unique(cmap) if v > 0)
        remap = {old:i+1 for i,old in enumerate(present)}
        new = np.zeros_like(cmap, dtype=np.int32)
        new_color, new_region = {}, {}
        for old, newid in remap.items():
            new[cmap == old] = newid
            new_color[newid] = cell_color[old]
            new_region[newid] = cell_region[old]
        cmap, cell_color, cell_region = new, new_color, new_region
    return cmap, cell_color, cell_region

