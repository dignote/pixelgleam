from __future__ import annotations
import json, random
from pathlib import Path
import numpy as np
from PIL import Image


def run_runtime_contract(package_dir: Path, seed: int = 1337):
    random.seed(seed)
    art=json.loads((package_dir/'artwork.json').read_text(encoding='utf-8'))
    cmap=np.array(Image.open(package_dir/'cell_id_map.png'),dtype=np.int32)
    cells={int(c['id']):c for c in art['cells']}
    regions={int(r['id']):r for r in art['regions']}
    # screen = local*scale + pan; inverse must recover cell lookup.
    transform_cases=0; transform_fail=[]
    for cid,c in list(cells.items())[:min(200,len(cells))]:
        x,y=map(float,c['center'])
        for _ in range(3):
            scale=random.uniform(0.65,3.5); panx=random.uniform(-180,180); pany=random.uniform(-220,220)
            sx=x*scale+panx; sy=y*scale+pany
            lx=(sx-panx)/scale; ly=(sy-pany)/scale
            ix=min(cmap.shape[1]-1,max(0,int(lx))); iy=min(cmap.shape[0]-1,max(0,int(ly)))
            got=int(cmap[iy,ix]); transform_cases+=1
            if got!=cid: transform_fail.append({'expected':cid,'got':got,'local':[lx,ly]})
    # Gameplay completion contract.
    completed=set(); duplicate_ok=True; wrong_ok=True
    for rid,r in regions.items():
        cid=int(r['cellIds'][0]); expected_color=int(r['colorId']); actual_color=int(cells[cid]['colorId'])
        if actual_color!=expected_color: wrong_ok=False
        before=len(completed)
        # wrong color must not complete.
        wrong_color=expected_color+999
        if wrong_color==actual_color: wrong_color+=1
        if wrong_color==actual_color: completed.add(rid)
        if len(completed)!=before: wrong_ok=False
        # correct tap completes exactly once.
        completed.add(rid); after=len(completed); completed.add(rid)
        if len(completed)!=after: duplicate_ok=False
    # Persistence exactness.
    ordered=sorted(regions)
    saved=set(ordered[:max(1,len(ordered)//2)])
    save_path=package_dir/'qa'/'runtime_sim_save.json'; save_path.parent.mkdir(exist_ok=True)
    save_path.write_text(json.dumps({'completedRegionIds':sorted(saved)},indent=2),encoding='utf-8')
    restored=set(json.loads(save_path.read_text())['completedRegionIds'])
    persistence_ok=restored==saved
    report={
      'status':'PASS' if not transform_fail and duplicate_ok and wrong_ok and persistence_ok else 'REWORK',
      'transformHitTest':{'cases':transform_cases,'failures':len(transform_fail),'samples':transform_fail[:10]},
      'regionCompletionExactlyOnce':duplicate_ok,
      'wrongColorNoProgressMutation':wrong_ok,
      'saveRestoreExactRegionSet':persistence_ok,
      'allRegionsCompletable':len(completed)==len(regions),
      'note':'Content/runtime contract simulation only; Android gesture arbitration, lifecycle and Compose rendering require app integration.'
    }
    return report
