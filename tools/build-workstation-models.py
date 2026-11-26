"""Author native Blockbench models through the user's running desktop MCP plugin.

Uses vanilla Minecraft textures; exports editable projects and Java models locally.
No server files or existing Blockbench projects are overwritten.
"""
from pathlib import Path
from zipfile import ZipFile
import base64, json, sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT/'.verification'))
from bb_rpc import rpc

OUT = ROOT/'artifacts/workstations-v1'
CLIENT = Path('C:/Users/32633/Desktop/悠梦星辰Minecraft26.1/.minecraft/versions/悠梦星辰Minecraft26.1/悠梦星辰Minecraft26.1.jar')
FACES = ['north','south','east','west','up','down']

def call(tool_name, **args):
    result = rpc('tools/call', {'name':tool_name,'arguments':args})
    if result.get('isError'): raise RuntimeError(str(result))
    return result

def value(result):
    if 'structuredContent' in result: return result['structuredContent']
    return json.loads(next(c['text'] for c in result['content'] if c['type']=='text'))

def cube(name, start, end, rotation=None, origin=None, glow=0):
    result={'name':name,'from':start,'to':end}
    if rotation: result.update(rotation=rotation,origin=origin)
    if glow: result.update(light_emission=glow,shade_direction_override='up')
    return result

def design(kind):
    groups={}
    def add(material,name,start,end,**extra):
        groups.setdefault(material,[]).append(cube(name,start,end,**extra))
    stone='polished_deepslate'; trim='exposed_copper'; dark='deepslate_tiles'
    add(stone,'Foundation beveled foot',[0,0,0],[16,1.5,16])
    add(dark,'Inset foundation',[.75,1.5,.75],[15.25,2.25,15.25])
    for x in (0.5,13.5):
        for z in (.5,13.5):
            add(trim,f'Copper corner shoe {x} {z}',[x,0.5,z],[x+2,2.5,z+2])
    if kind=='enhancement_station':
        add(stone,'Upper base step',[1.5,2.25,1.5],[14.5,4,14.5])
        add(dark,'Altar pedestal',[3,4,3],[13,9.5,13])
        add(trim,'Lower pedestal belt',[2.75,4.25,2.75],[13.25,5,13.25])
        add(stone,'Altar crown',[1,9.5,1],[15,11,15])
        add(dark,'Obsidian working surface',[2,11,2],[14,11.5,14])
        for x in (1,14):
            add(trim,f'Crown edge X{x}',[x,10.75,1],[x+.5,11.5,15])
        for z in (1,14.5):
            add(trim,f'Crown edge Z{z}',[1,10.75,z],[15,11.5,z+.5])
        for x in (2,12):
            for z in (2,12):
                add(stone,f'Rune pylon {x} {z}',[x,11,z],[x+2,15,z+2])
                add(trim,f'Pylon cap {x} {z}',[x-.25,14.5,z-.25],[x+2.25,15.25,z+2.25])
                add('sea_lantern',f'Pylon light {x} {z}',[x+.5,15.25,z+.5],[x+1.5,16.25,z+1.5],glow=10)
        for z in (5,10.5):
            add(trim,f'Crystal cradle Z{z}',[5,11.5,z],[11,12.25,z+.5])
        for x in (5,10.5):
            add(trim,f'Crystal cradle X{x}',[x,11.5,5],[x+.5,12.25,11])
        add('sea_lantern','Arcane core socket',[6,11.5,6],[10,12.75,10],glow=12)
        for tier,width in enumerate((.6,1.4,2.4,3.3,3.3,2.4,1.4,.6)):
            y=17+tier
            add('diamond_block',f'Floating crystal facet tier {tier}',[8-width/2,y,8-width/2],[8+width/2,y+1,8+width/2],
                rotation=[0,45,0],origin=[8,y+.5,8],glow=12)
        for i,(x,y,z) in enumerate([(4.5,16,8),(11,17.5,8),(8,15.5,4.5),(8,16.5,11)]):
            add('sea_lantern',f'Floating energy mote {i}',[x,y,z],[x+.65,y+.65,z+.65],rotation=[0,0,45],origin=[x+.325,y+.325,z+.325],glow=10)
        # Geometry inlays remain legible at normal block viewing distance.
        for side in ('north','south','east','west'):
            for i,(a,b,c,d) in enumerate([(7,6,8,8.5),(8,8,9,9),(6,7,7,8),(8,6,10,6.5)]):
                if side in ('north','south'):
                    z=2.97 if side=='north' else 13.01
                    add('diamond_block',f'{side} cyan rune {i}',[a,b,z],[c,d,z+.04],glow=8)
                else:
                    x=2.97 if side=='west' else 13.01
                    add('diamond_block',f'{side} cyan rune {i}',[x,b,a],[x+.04,d,c],glow=8)
        for x in (4,11):
            for z in (2.8,13.05):
                add(trim,f'Altar rivet {x} {z}',[x,5.6,z],[x+.55,6.15,z+.15])
    else:
        add('deepslate_bricks','Furnace masonry',[1,2.25,1],[10.5,11.75,15])
        add(dark,'Furnace face inset',[2.25,3.25,.8],[9.25,10.25,1.05])
        add('magma','Hot forge chamber',[2.75,3.75,.65],[8.75,9.75,.85],glow=12)
        for x in (2.5,8.75):
            add(trim,f'Furnace door jamb {x}',[x,3.5,.35],[x+.5,10,.85])
        for y in (3.25,9.75):
            add(trim,f'Furnace door lintel {y}',[2.5,y,.35],[9.25,y+.5,.85])
        for x in (3.5,5,6.5,8):
            add('anvil',f'Forged iron fire grate {x}',[x,3.5,.1],[x+.35,9.75,.55])
        add('anvil','Ash drawer',[2.25,2.5,.15],[9.5,3.25,1])
        add(trim,'Drawer handle',[5,2.55,-.05],[6.75,3,0.25])
        add('dark_oak_planks','Side workbench cabinet',[11,2.25,2],[15.5,11.75,14.5])
        for x in (10.75,15):
            for z in (1.75,13.75):
                add('stripped_dark_oak_log',f'Workbench oak leg {x} {z}',[x,2.25,z],[x+.75,12,z+.75])
        for y in (4,8.5):
            add(trim,f'Cabinet iron strap {y}',[11,y,1.65],[15.5,y+.5,2])
        add('anvil','Cabinet handle',[12.5,7,1.35],[14,7.5,1.8])
        add(stone,'Overhanging worktop',[0,11.75,0],[16,13.25,16])
        for z in (.1,15.4):
            add(trim,f'Workbench copper rim {z}',[0,12.5,z],[16,13.35,z+.5])
        add('anvil','Anvil broad foot',[3.25,13.25,5.25],[11.25,14.5,11.25])
        add('anvil','Anvil tapered heel',[4.5,14.5,6.25],[10,15.75,10.25])
        add('anvil','Anvil waist',[6,15.75,7],[8.5,18.75,9.5])
        add('anvil','Anvil shoulder',[4.5,18.75,6],[11.25,19.5,10.5])
        add('anvil','Anvil steel head',[3,19.5,5.75],[12.25,21.5,10.75])
        add('iron_block','Anvil polished face',[3,21.5,5.75],[12.25,21.85,10.75])
        add('anvil','Horn wide root',[1.5,20,6.25],[3,21.35,10.25])
        add('anvil','Horn tapered tip',[.25,20.35,7],[1.5,21,9.5])
        add('coal_block','Hardy hole',[10.25,21.86,6.5],[11.25,21.88,7.5])
        add('deepslate_bricks','Forge chimney',[1,13.25,12],[5.5,25.5,16])
        for y in (16.5,22):
            add(trim,f'Chimney band {y}',[.75,y,11.75],[5.75,y+.6,16.25])
        add(stone,'Chimney crown',[.5,25.5,11.5],[6,26.5,16.5])
        add('coal_block','Chimney soot opening',[1.25,26.51,12.25],[5.25,26.55,15.75])
        add('dark_oak_planks','Tool rack upright',[14,13.25,12.75],[15,22,13.75])
        add(trim,'Tool rack cross bar',[11.5,21,12.75],[16,21.65,13.75])
        add('stripped_dark_oak_log','Hammer handle',[13.3,13.5,11.3],[14,19.75,12])
        add('anvil','Hammer heavy head',[12,19.25,10.5],[15.5,21,12.75])
        add('iron_block','Hammer striking face',[11.75,19.25,10.5],[12.1,21,12.75])
        for x in (11,11.65):
            add('anvil',f'Hanging tongs arm {x}',[x,14,12],[x+.25,19,12.35])
        add(trim,'Tongs rivet',[11.2,16.75,11.8],[11.75,17.25,12.45])
        for i,x in enumerate((12,13.4,14.8)):
            add('iron_block',f'Spare metal billet {i}',[x,13.25,3],[x+1,14,6])
        for x in (1,15):
            for z in (.05,15.3):
                add('iron_block',f'Worktop rivet {x} {z}',[x,12.65,z],[x+.45,13.1,z+.15])
    return groups

def export(kind):
    for codec,suffix in [('project','.bbmodel'),('java_block','.json')]:
        result=value(call('export_model',codec_id=codec,max_content_length=2000000,result_format='text'))
        if result['truncated']: raise RuntimeError('Truncated export')
        raw=result['content']
        if isinstance(raw,str):
            try: doc=json.loads(raw)
            except json.JSONDecodeError: doc=json.loads(json.loads('"'+raw+'"'))
        else: doc=raw
        if isinstance(doc,str): doc=json.loads(doc)
        if codec=='project':
            target=OUT/'models'/(kind+suffix)
        else:
            # Replace temporary editor identifiers with stable vanilla resource references.
            texture_names=[t['name'] for t in json.loads((OUT/'models'/(kind+'.bbmodel')).read_text())['textures']]
            for key,old in list(doc['textures'].items()):
                if key=='particle':continue
                name=texture_names[int(key)] if key.isdigit() else Path(old).name
                doc['textures'][key]='minecraft:block/'+name
            doc['textures']['particle']='minecraft:block/polished_deepslate'
            doc['display']={'fixed':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[1,1,1]},
                            'gui':{'rotation':[25,225,0],'translation':[0,-3,0],'scale':[.55,.55,.55]},
                            'ground':{'rotation':[0,0,0],'translation':[0,3,0],'scale':[.25,.25,.25]}}
            target=OUT/'resourcepack/assets/starforge/models/block/workstations'/(kind+suffix)
        target.parent.mkdir(parents=True,exist_ok=True)
        if target.exists():raise RuntimeError('Refusing to overwrite '+str(target))
        target.write_text(json.dumps(doc,ensure_ascii=False,indent=2),encoding='utf-8')
    entry=OUT/'resourcepack/assets/starforge/items/workstations'/(kind+'.json');entry.parent.mkdir(parents=True,exist_ok=True)
    entry.write_text(json.dumps({'model':{'type':'minecraft:model','model':'starforge:block/workstations/'+kind}},indent=2),encoding='utf-8')

def build(kind,create):
    if create: call('create_project',name='StarForge - '+('锻造台' if kind=='crafting_station' else '强化台'),format='java_block')
    groups=design(kind)
    with ZipFile(CLIENT) as z:
        for mat,elements in groups.items():
            if mat!='polished_deepslate' or create:
                data='data:image/png;base64,'+base64.b64encode(z.read('assets/minecraft/textures/block/'+mat+'.png')).decode()
                call('create_texture',name=mat,data=data,width=16,height=16)
            call('add_group',name=mat)
            call('place_cube',elements=elements,texture=mat,group=mat,faces=True)
    export(kind)
    # Camera is a genuine Blockbench render; separate from generated concepts.
    call('set_camera_angle',position=[45,32,-50],target=[0,12,0],projection='orthographic',zoom=.55,max_size=1200)
    call('capture_screenshot',max_size=1400)
    dest=OUT/'previews'/(kind+'.png');dest.parent.mkdir(parents=True,exist_ok=True)
    import shutil
    shutil.copy2(ROOT/'.verification/bb-image-0.png',dest)
    print(kind, 'cubes',sum(map(len,groups.values())), 'exported',flush=True)

if __name__=='__main__': build(sys.argv[1], '--create' in sys.argv)
