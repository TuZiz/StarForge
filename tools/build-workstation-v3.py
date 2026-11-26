"""Rebuild full-size modular workstations with native geometry and texel scale.

No nonuniform furniture scale and no AI/raster-generated texture assets.
"""
from pathlib import Path
from zipfile import ZipFile
import base64,copy,itertools,json,math,uuid

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'artifacts/workstations-v3'
LIVE=ROOT/'craftengine/星锻工作台/resourcepack/assets/starforge/models/block/workstations'
CLIENT=Path('C:/Users/32633/Desktop/悠梦星辰Minecraft26.1/.minecraft/versions/悠梦星辰Minecraft26.1/悠梦星辰Minecraft26.1.jar')
SIDES=['north','south','east','west','up','down']

class Model:
    def __init__(self,kind):self.kind=kind;self.parts=[]
    def box(self,group,mat,name,start,end,rotation=None):
        # Splitting into <=16-unit cuboids permits 1 texture pixel/model unit.
        cuts=[]
        for a,b in zip(start,end):
            points=[a]
            while points[-1]+16<b-1e-7:points.append(points[-1]+16)
            points.append(b);cuts.append(list(zip(points,points[1:])))
        for n,sections in enumerate(itertools.product(*cuts)):
            a=[s[0] for s in sections];b=[s[1] for s in sections]
            dx,dy,dz=[b[i]-a[i] for i in range(3)]
            dims={'north':(dx,dy),'south':(dx,dy),'east':(dz,dy),'west':(dz,dy),'up':(dx,dz),'down':(dx,dz)}
            e={'name':name+(' '+str(n) if n else ''),'from':a,'to':b,
               'faces':{side:{'uv':[0,0,*dims[side]],'texture':'#'+mat} for side in SIDES}}
            if rotation:e['rotation']=rotation
            self.parts.append((group,mat,e))
    def export(self):
        mats=list(dict.fromkeys(m for _,m,_ in self.parts))
        doc={'format_version':'26.3','credit':'StarForge v3 modular native geometry',
             'textures':{m:'minecraft:block/'+m for m in mats},'elements':[e for _,_,e in self.parts],
             'display':{'gui':{'rotation':[25,225,0],'translation':[0,-2,0],'scale':[.35,.35,.35]},
                        'ground':{'rotation':[0,0,0],'translation':[0,2,0],'scale':[.18,.18,.18]},
                        'fixed':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[1,1,1]}}}
        source={'meta':{'format_version':'5.0','model_format':'java_block','box_uv':False},'name':self.kind+' v3',
                'resolution':{'width':16,'height':16},'elements':[],'outliner':[],'textures':[]}
        groups={}
        with ZipFile(CLIENT) as z:
            for i,mat in enumerate(mats):
                data=z.read('assets/minecraft/textures/block/'+mat+'.png')
                source['textures'].append({'path':'','name':mat,'namespace':'minecraft','folder':'block','id':str(i),
                    'uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,self.kind+'/'+mat)),'mode':'bitmap','visible':True,
                    'render_mode':'default','width':16,'height':16,'uv_width':16,'uv_height':16,
                    'source':'data:image/png;base64,'+base64.b64encode(data).decode()})
        for i,(group,mat,e) in enumerate(self.parts):
            p=copy.deepcopy(e);p['type']='cube';p['uuid']=str(uuid.uuid5(uuid.NAMESPACE_URL,self.kind+'/'+str(i)))
            for face in p['faces'].values():face['texture']=mats.index(mat)
            if 'rotation' in p:
                r=p['rotation'];p['origin']=r['origin'];p['rotation']=[r['angle'] if k==r['axis'] else 0 for k in 'xyz']
            else:p['rotation']=[0,0,0];p['origin']=[8,8,8]
            source['elements'].append(p);groups.setdefault(group,[]).append(p['uuid'])
        source['outliner']=[{'name':name,'origin':[8,8,8],'uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,self.kind+'/group/'+name)),
                            'children':children} for name,children in groups.items()]
        for e in doc['elements']:
            assert all(-16<=n<=32 for n in e['from']+e['to']),e['name']
        for p,data in [(OUT/'models'/(self.kind+'.bbmodel'),source),
                       (OUT/'resourcepack/assets/starforge/models/block/workstations'/(self.kind+'.json'),doc),
                       (LIVE/(self.kind+'.json'),doc)]:
            p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        return dict(id=self.kind,cubes=len(self.parts),materials=len(mats),min=[min(e['from'][i] for _,_,e in self.parts) for i in range(3)],max=[max(e['to'][i] for _,_,e in self.parts) for i in range(3)])

def foundation(m):
    # Actual 3x2 footprint built from flagstones, not a stretched one-block cube.
    for x in [-15,0,15]:
        for z in [-7,8]:
            m.box('Foundation','polished_deepslate','Separate paving stone',[x,0,z],[x+14.8,1.5,z+14.8])
    for x in [-15,27]:
        for z in [-7,20]:m.box('Foundation','exposed_copper','Corner foot',[x,1.5,z],[x+2.8,2.1,z+2.8])

def enhancement():
    m=Model('enhancement_station');b=m.box;foundation(m)
    b('Central altar','deepslate_tiles','Altar foot',[0,1.5,0],[16,3.5,16])
    b('Central altar','polished_deepslate','Compact square pedestal',[2,3.5,2],[14,9.5,14])
    b('Central altar','exposed_copper','Pedestal belt',[1.5,4,1.5],[14.5,4.75,14.5])
    b('Central altar','polished_deepslate','Overhanging altar crown',[-1,9.5,-1],[17,11.5,17])
    b('Central altar','deepslate_tiles','Rune working surface',[0,11.5,0],[16,12,16])
    for x in [0,15.5]:b('Central altar','exposed_copper','Thin altar rim',[x,11.5,0],[x+.5,12.25,16])
    for z in [0,15.5]:b('Central altar','exposed_copper','Thin altar rim',[0,11.5,z],[16,12.25,z+.5])
    for i,(x,y,w,h) in enumerate([(6,6,1,2),(7,7.5,3,.5),(8,5,1,2.5),(10,6.5,1,.5)]):
        b('Central altar','diamond_block','Front rune '+str(i),[x,y,1.94],[x+w,y+h,2])
    # The four pylons sit outside the central altar and keep compact square sections.
    for x in [-11,23]:
        for z in [-3,15]:
            high=16 if z<0 else 23
            b('Corner pylons','polished_deepslate','Pylon foot',[x-1,1.5,z-1],[x+5,4,z+5])
            b('Corner pylons','deepslate_bricks','Pylon shaft',[x,4,z],[x+4,high,z+4])
            b('Corner pylons','exposed_copper','Pylon lower collar',[x-.25,5,z-.25],[x+4.25,5.75,z+4.25])
            b('Corner pylons','exposed_copper','Pylon capital',[x-.5,high,z-.5],[x+4.5,high+1,z+4.5])
            b('Corner pylons','sea_lantern','Pylon cyan inset',[x+1,high+1,z+1],[x+3,high+2,z+3])
            b('Corner pylons','diamond_block','Vertical light slit',[x+1.5,8,z-.06],[x+2.5,high-2,z])
    # Rear gate: stepped beam and blue inserts; open space around the crystal.
    b('Rear rune gate','polished_deepslate','Gate left shoulder',[-7,22,15],[0,24,19])
    b('Rear rune gate','polished_deepslate','Gate right shoulder',[16,22,15],[23,24,19])
    b('Rear rune gate','deepslate_tiles','Gate raised bridge',[-1,24,15],[17,26,19])
    b('Rear rune gate','exposed_copper','Gate copper eyebrow',[-1,26,15],[17,26.6,19])
    for x in [1,7,13]:b('Rear rune gate','sea_lantern','Gate light',[x,24.6,14.9],[x+2,25.4,15])
    # Side shelves and connecting channels use their own proportions.
    for x in [-13,21]:
        b('Side rune shelves','polished_deepslate','Side console leg',[x+1,1.5,5],[x+7,7,11])
        b('Side rune shelves','deepslate_tiles','Side console slab',[x,7,4],[x+9,9,13])
        b('Side rune shelves','exposed_copper','Side console trim',[x,8.5,3.75],[x+9,9.25,4.25])
        b('Side rune shelves','diamond_block','Side rune center',[x+3,9,7],[x+6,9.5,10])
    b('Crystal','exposed_copper','Crystal socket copper',[4,12,4],[12,13,12])
    b('Crystal','sea_lantern','Crystal socket light',[5,13,5],[11,13.75,11])
    # A narrow, tall new crystal; width is intentionally independent of platform width.
    for i,width in enumerate([.8,1.8,2.8,3.8,4.2,3.8,2.8,1.8,.8]):
        y=18+i*1.4
        b('Crystal','diamond_block','Slender crystal facet '+str(i),[8-width/2,y,8-width/2],[8+width/2,y+1.4,8+width/2],
          {'axis':'y','angle':45,'origin':[8,y+.7,8]})
    for x,y,z in [(4,16,8),(11.5,18,8),(8,16.5,4.5)]:
        b('Crystal','sea_lantern','Energy mote',[x,y,z],[x+.6,y+.6,z+.6])
    return m

def crafting():
    m=Model('crafting_station');b=m.box;foundation(m)
    # Left third: independent furnace mass and a tall square chimney.
    b('Left furnace','deepslate_bricks','Furnace base',[-14,1.5,-5],[-1,4,20])
    b('Left furnace','deepslate_bricks','Furnace body',[-13,4,-4],[-2,20,19])
    b('Left furnace','polished_deepslate','Furnace crown',[-14,20,-5],[-1,22,20])
    b('Left furnace','coal_block','Dark recessed mouth',[-11.5,6,-4.15],[-3.5,17,-4.01])
    b('Left furnace','magma','Hot inner mouth',[-10.5,7,-4.3],[-4.5,15,-4.16])
    for x in [-12,-3.5]:b('Left furnace','exposed_copper','Mouth jamb',[x,5,-4.9],[x+1,18,-4.25])
    for y in [5,17]:b('Left furnace','exposed_copper','Mouth lintel',[-12,y,-4.9],[-2.5,y+1,-4.25])
    for x in [-10.5,-8.5,-6.5,-4.5]:b('Left furnace','anvil','Door iron bar',[x,6,-5.15],[x+.5,17,-4.8])
    b('Left furnace','anvil','Ash tray',[-12,3.5,-5.1],[-3,5,-4])
    b('Left furnace','exposed_copper','Ash tray grip',[-9.5,4,-5.6],[-6,4.6,-5.1])
    b('Chimney','deepslate_bricks','Square chimney',[-12,22,11],[-5,30,19])
    b('Chimney','exposed_copper','Chimney collar',[-12.5,25,10.5],[-4.5,25.75,19.5])
    b('Chimney','polished_deepslate','Chimney crown',[-13,30,10],[-4,31.5,20])
    b('Chimney','coal_block','Chimney opening',[-11.5,31.5,11.5],[-5.5,31.6,18.5])
    # Center: a properly sized anvil on its own squat stand, separated from the furnace.
    b('Central anvil','stripped_dark_oak_log','Anvil stump',[2,1.5,2],[14,7,14])
    for y in [2,5.5]:b('Central anvil','anvil','Stump iron strap',[1.7,y,1.7],[14.3,y+.7,14.3])
    b('Central anvil','anvil','Broad foot',[1,7,3],[15,8.5,13])
    b('Central anvil','anvil','Tapered lower heel',[3,8.5,4],[13,10,12])
    b('Central anvil','anvil','Narrow waist',[6,10,5.5],[10,14.5,10.5])
    b('Central anvil','anvil','Shoulder',[3.5,14.5,4],[12.5,15.75,12])
    b('Central anvil','anvil','Head',[1.5,15.75,4],[15,18,12])
    b('Central anvil','iron_block','Polished face',[1.5,18,4],[15,18.4,12])
    b('Central anvil','anvil','Horn root',[-.5,16.25,5],[1.5,17.75,11])
    b('Central anvil','anvil','Horn tip',[-2,16.75,6],[-.5,17.5,10])
    b('Central anvil','coal_block','Hardy hole',[12.25,18.4,5],[13.5,18.45,6.25])
    # Right third: a timber bench with air underneath, separate tool board behind it.
    for x in [18,28]:
        for z in [-3,16]:b('Right workbench','stripped_dark_oak_log','Bench leg',[x,1.5,z],[x+2,14,z+2])
    b('Right workbench','dark_oak_planks','Bench apron',[18,11,-3],[30,14,-1])
    b('Right workbench','dark_oak_planks','Workbench top',[17,14,-4],[31,16,19])
    b('Right workbench','exposed_copper','Front edge binding',[17,14,-4.35],[31,15,-4])
    b('Right workbench','dark_oak_planks','Small drawer',[19,10,-2.8],[29,13,7])
    b('Right workbench','anvil','Drawer handle',[23,11,-3.25],[26,11.6,-2.8])
    b('Right workbench','dark_oak_planks','Lower shelf',[18,4,2],[30,5.5,17])
    for x in [19,28]:b('Tool rack','stripped_dark_oak_log','Rack upright',[x,16,17],[x+1.5,28,19])
    b('Tool rack','dark_oak_planks','Rack crosspiece',[18,26.5,17],[30.5,29,19])
    for x in [20,24,28]:b('Tool rack','exposed_copper','Tool hook',[x,25.5,16.4],[x+.5,27,17.2])
    b('Tool rack','stripped_dark_oak_log','Hammer handle',[20.5,17.5,15.5],[21.25,24.5,16.25])
    b('Tool rack','anvil','Hammer head',[19,23.5,15],[23,25.5,17])
    b('Tool rack','iron_block','Hammer striking cap',[18.75,23.5,15],[19.25,25.5,17])
    for x in [25,26]:b('Tool rack','anvil','Tongs arm',[x,18,16],[x+.35,25,16.5])
    b('Tool rack','exposed_copper','Tongs joint',[25,21,15.85],[26.35,21.6,16.55])
    for i,x in enumerate([20,23.5,27]):b('Right workbench','iron_block','Metal billet '+str(i),[x,16,1],[x+2.5,17.1,6])
    b('Right workbench','coal_block','Coal basket',[19,5.5,10],[24,8.5,16])
    return m

if __name__=='__main__':
    result=[enhancement().export(),crafting().export()]
    (OUT/'validation.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
    print(json.dumps(result))
