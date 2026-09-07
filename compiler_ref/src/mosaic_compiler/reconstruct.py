from __future__ import annotations
import argparse, json
from pathlib import Path
from .generator import GenerationConfig, generate_from_color_mask, load_author_color_mask_from_v3, auto_palette_color_mask


def main():
    ap=argparse.ArgumentParser()
    ap.add_argument('--source-v3',type=Path,required=True)
    ap.add_argument('--master',type=Path,required=True)
    ap.add_argument('--out',type=Path,required=True)
    ap.add_argument('--target-cells',type=int,default=347)
    ap.add_argument('--auto-color-mask',action='store_true')
    args=ap.parse_args()
    art=json.loads((args.source_v3/'artwork.json').read_text(encoding='utf-8'))
    if args.auto_color_mask:
        color_mask=auto_palette_color_mask(args.master,args.source_v3/'subject_mask.png',art['palette'])
    else:
        color_mask=load_author_color_mask_from_v3(args.source_v3)
    res=generate_from_color_mask(args.master,args.source_v3/'subject_mask.png',color_mask,art['palette'],args.out,
                                 cfg=GenerationConfig(target_cells=args.target_cells))
    print(json.dumps(res.to_dict(),indent=2))
if __name__=='__main__': main()
