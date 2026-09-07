from __future__ import annotations
from pathlib import Path
import json, math
import numpy as np
from PIL import Image, ImageDraw
from .generator import GenerationConfig, generate_from_color_mask
from .validator import validate_artwork

PALETTE=[
 {'id':1,'name':'a','hex':'#F39A21'},{'id':2,'name':'b','hex':'#F7ECDD'},{'id':3,'name':'c','hex':'#D9B887'},
 {'id':4,'name':'d','hex':'#372D27'},{'id':5,'name':'e','hex':'#F0665F'},{'id':6,'name':'f','hex':'#ECB63D'},{'id':7,'name':'g','hex':'#5AA6BF'}]
RGB={i:tuple(int(PALETTE[i-1]['hex'][j:j+2],16) for j in (1,3,5)) for i in range(1,8)}

def _base(size=256): return Image.new('RGBA',(size,size),(255,250,242,255)), Image.new('L',(size,size),0), Image.new('L',(size,size),0)

def make_fixture(kind:str, root:Path):
    master,subject,color=_base(); d=ImageDraw.Draw(master); s=ImageDraw.Draw(subject); c=ImageDraw.Draw(color)
    if kind=='flower':
        cx=cy=128
        for k in range(10):
            a=2*math.pi*k/10; px=cx+55*math.cos(a); py=cy+55*math.sin(a)
            box=(px-38,py-22,px+38,py+22); col=1+(k%3)
            d.ellipse(box,fill=RGB[col]+(255,)); s.ellipse(box,fill=255); c.ellipse(box,fill=col)
        d.ellipse((96,96,160,160),fill=RGB[6]+(255,)); s.ellipse((96,96,160,160),fill=255); c.ellipse((96,96,160,160),fill=6)
    elif kind=='mandala':
        cx=cy=128
        for r,col in [(105,1),(82,2),(60,5),(38,6),(18,4)]:
            d.ellipse((cx-r,cy-r,cx+r,cy+r),fill=RGB[col]+(255,)); s.ellipse((cx-r,cy-r,cx+r,cy+r),fill=255); c.ellipse((cx-r,cy-r,cx+r,cy+r),fill=col)
        # radial spokes cut repeated narrow features
        for k in range(16):
            a=2*math.pi*k/16; x=cx+105*math.cos(a); y=cy+105*math.sin(a)
            d.line((cx,cy,x,y),fill=RGB[3]+(255,),width=5); c.line((cx,cy,x,y),fill=3,width=5); s.line((cx,cy,x,y),fill=255,width=5)
    elif kind=='food':
        d.ellipse((55,55,200,220),fill=RGB[5]+(255,)); s.ellipse((55,55,200,220),fill=255); c.ellipse((55,55,200,220),fill=5)
        d.polygon([(125,55),(145,15),(162,65)],fill=RGB[7]+(255,)); s.polygon([(125,55),(145,15),(162,65)],fill=255); c.polygon([(125,55),(145,15),(162,65)],fill=7)
        d.ellipse((95,100,125,130),fill=RGB[2]+(255,)); c.ellipse((95,100,125,130),fill=2)
    elif kind=='character':
        d.ellipse((58,28,198,222),fill=RGB[2]+(255,)); s.ellipse((58,28,198,222),fill=255); c.ellipse((58,28,198,222),fill=2)
        # hair silhouette / narrow strands
        d.pieslice((48,18,208,180),180,360,fill=RGB[4]+(255,)); s.pieslice((48,18,208,180),180,360,fill=255); c.pieslice((48,18,208,180),180,360,fill=4)
        for x in [92,164]: d.ellipse((x-10,105,x+10,125),fill=RGB[7]+(255,)); c.ellipse((x-10,105,x+10,125),fill=7)
        d.arc((100,135,156,175),0,180,fill=RGB[5]+(255,),width=5); c.arc((100,135,156,175),0,180,fill=5,width=5)
    else: raise ValueError(kind)
    root.mkdir(parents=True,exist_ok=True)
    master.save(root/'master.png'); subject.save(root/'subject_mask.png'); color.save(root/'color_mask.png')
    return root/'master.png',root/'subject_mask.png',np.array(color,dtype=np.uint8)

def run(root:Path):
    rows=[]
    for kind,target in [('flower',220),('mandala',260),('food',180),('character',240)]:
        fixture=root/'fixtures'/kind; master,subject,cm=make_fixture(kind,fixture)
        out=root/'outputs'/kind
        res=generate_from_color_mask(master,subject,cm,PALETTE,out,artwork_id=f'stress_{kind}',title=kind,cfg=GenerationConfig(target_cells=target))
        val=validate_artwork(out,master_path=master)
        rows.append({'kind':kind,'generation':res.to_dict(),'validation':val.to_dict()})
    status='PASS' if all(r['validation']['status']=='PASS' for r in rows) else 'REWORK'
    return {'status':status,'classification':'TECHNICAL_STRESS_ONLY_NOT_PRODUCTION_BENCHMARK','cases':rows}
