"""Build native cuboid nameplate geometry, not raster artwork.

Names remain configurable CE text_display components. No font/bitmap is baked.
"""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT/'craftengine/星锻工作台/resourcepack/assets/starforge'

def build(kind, accent):
    elements = []
    def box(name, start, end, texture):
        elements.append({'name':name,'from':start,'to':end,'shade':False,
                         'faces':{side:{'uv':[0,0,16,16],'texture':'#'+texture}
                                  for side in ['north','south','east','west','up','down']}})
    # Model is centered at (8,8,8); none display_transform subtracts that center.
    # Layer thickness is small; text lives in front via its local translation.
    box('Stepped dark silhouette',[-5.5,3.5,7.6],[21.5,12.5,8.25],'dark')
    box('Pixel left tab',[-6.5,5,7.6],[-5.5,11,8.25],'dark')
    box('Pixel right tab',[21.5,5,7.6],[22.5,11,8.25],'dark')
    box('Copper header',[-4.5,11.5,8.25],[20.5,12.5,8.45],'copper')
    box('Copper footer',[-4.5,3.5,8.25],[20.5,4.5,8.45],'copper')
    box('Copper left',[-5.5,4.5,8.25],[-4.5,11.5,8.45],'copper')
    box('Copper right',[20.5,4.5,8.25],[21.5,11.5,8.45],'copper')
    box('Dark flat text inset',[-4.5,4.5,8.25],[20.5,11.5,8.35],'inset')
    for x in [-5.5,20.5]:
        for y in [4.5,10.5]:
            box('Pixel corner accent',[x,y,8.45],[x+1,y+1,8.6],'accent')
    for x in [-7.5,22.5]:
        box('Outer diamond center',[x,7.5,7.8],[x+1,8.5,8.5],'accent')
        box('Outer diamond crown',[x+.25,8.5,7.8],[x+.75,9,8.5],'copper')
        box('Outer diamond foot',[x+.25,7,7.8],[x+.75,7.5,8.5],'copper')
    model={'format_version':'26.3','credit':'StarForge native pixel nameplate',
           'textures':{'dark':'minecraft:block/gray_concrete',
                       'inset':'minecraft:block/black_concrete',
                       'copper':'minecraft:block/exposed_copper',
                       'accent':'minecraft:block/'+accent,
                       'particle':'minecraft:block/exposed_copper'},
           'elements':elements,
           'display':{'gui':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[.5,.5,.5]}}}
    path=ASSETS/'models/block/workstations'/(kind+'_nameplate.json')
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(model,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    for e in elements:
        assert all(-16 <= n <= 32 for n in e['from']+e['to'])
    return path

if __name__=='__main__':
    for kind,accent in [('enhancement','diamond_block'),('crafting','raw_copper_block')]:
        print(build(kind,accent).relative_to(ROOT))
