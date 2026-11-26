"""Export the selected generated enhancement concept to the existing chest grid, without API calls."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
from zipfile import ZipFile
from io import BytesIO
import json

ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'craftengine/强化石/resourcepack/assets/starforge'
OUT=ROOT/'artifacts/ui-previews'
raw=Image.open(OUT/'enhancement-iron-copper-v1-generated.png').convert('RGBA')
font_path='C:/Users/32633/Desktop/leaf/plugins/Xerr/generated/resource_pack/assets/xerr_font/font/fusion_pixel_quest_12_sc.ttf'
small_font_path='C:/Users/32633/Desktop/leaf/plugins/Xerr/generated/resource_pack/assets/xerr_font/font/fusion_pixel.ttf'
# Xerr uses this face at 10px and the quest face at 12px. Do not shrink quest glyphs.
font=ImageFont.truetype(small_font_path,10)
background=Image.open(A/'textures/font/gui/craft_orders_six.png').convert('RGBA')
d=ImageDraw.Draw(background);d.fontmode='1'
d.rectangle((5,18,170,105),fill='#303236')

def frame(size,box):
    tile=raw.crop(box).resize(size,Image.Resampling.NEAREST)
    ImageDraw.Draw(tile).rectangle((2,2,size[0]-3,size[1]-3),fill='#292c30')
    return tile

# A shared low material tray: icons and counts align with the two side panels.
# Tier names stay in the real CE hover text; do not rebuild three tall cages.
d.text((88,40),'强化石',font=font,fill='#c9bda6',anchor='mt')
d.rectangle((60,53,115,88),fill='#272a2e',outline='#9e795b')
d.line((61,54,114,54),fill='#c09b74')
d.line((61,87,114,87),fill='#171a1d')
d.line((62,71,113,71),fill='#62564a')
for x in (79,97):d.line((x,78,x,84),fill='#49433b')
# Probability is a compact dynamic status row, without a separate large panel.

# One continuous frame per equipment/action panel; the name is its lower section.
for x,label in [(20,'装备'),(128,'强化')]:
    tile=frame((28,36),(102,296,253,447))
    td=ImageDraw.Draw(tile);td.fontmode='1'
    # Clean the enlarged crop's uneven rim; keep one consistent copper edge.
    td.rectangle((0,0,27,35),fill='#272a2e',outline='#9e795b')
    td.line((1,1,26,1),fill='#c09b74')
    td.line((1,34,26,34),fill='#171a1d')
    td.line((2,18,25,18),fill='#62564a')
    td.text((14,22),label,font=font,fill='#ead6af',anchor='mt')
    background.alpha_composite(tile,(x,53))

hammer=raw.crop((792,323,908,441)).resize((16,16),Image.Resampling.NEAREST)
hp=hammer.load()
for y in range(16):
    for x in range(16):
        r,g,b,a=hp[x,y]
        if max(r,g,b)<90:hp[x,y]=(0,0,0,0)
background.alpha_composite(hammer,(134,54))

# Runes retain their real CE item art and hover text. No baked names or rune duplicates.
rune_frame=frame((18,18),(102,296,253,447))
for x in (115,151):background.alpha_composite(rune_frame,(x,18))
close=raw.crop((875,675,934,728)).resize((8,8),Image.Resampling.NEAREST)
cp=close.load()
for y in range(8):
    for x in range(8):
        r,g,b,a=cp[x,y]
        if max(r,g,b)<95:cp[x,y]=(0,0,0,0)
background.alpha_composite(close,(84,94))
background.save(A/'textures/font/gui/forge_menu.png')

# Shared atlas of native 64x16 cells; display uses exact integer UV sampling.
atlas=Image.new('RGBA',(512,256));rates={};entries=[]
chance_dir=A/'models/item/enhancement';chance_dir.mkdir(parents=True,exist_ok=True)
item_dir=A/'items/enhancement';item_dir.mkdir(parents=True,exist_ok=True)
texture_dir=A/'textures/item/enhancement';texture_dir.mkdir(parents=True,exist_ok=True)
for percent in range(101):
    tile=Image.new('RGBA',(64,16));td=ImageDraw.Draw(tile);td.fontmode='1'
    td.text((0,0),'成功率',font=font,fill='#aaa99e',anchor='lt')
    td.text((35,0),str(percent)+'%',font=font,fill='#bde8cb',anchor='lt')
    td.rectangle((0,13,51,14),fill='#1c2225')
    width=0 if percent==0 else max(1,int(52*percent/100))
    if width:td.rectangle((0,13,width-1,14),fill='#83c99e')
    px=(percent%8)*64;py=(percent//8)*16;atlas.alpha_composite(tile,(px,py));rates[percent]=tile
    uv=[px/32,py/16,(px+64)/32,(py+16)/16]
    geometry={'gui_light':'front','ambientocclusion':False,'textures':{'art':'starforge:item/enhancement/chance_atlas','particle':'starforge:item/enhancement/chance_atlas'},
        'elements':[{'from':[-24,0,8],'to':[40,16,8.01],'shade':False,'faces':{'south':{'uv':uv,'texture':'#art'},'north':{'uv':[uv[2],uv[1],uv[0],uv[3]],'texture':'#art'}}}],
        'display':{'gui':{'rotation':[0,0,0],'translation':[6,10,0],'scale':[1,1,1]}}}
    name=f'chance_{percent:03d}'
    (chance_dir/(name+'.json')).write_text(json.dumps(geometry,separators=(',',':')),encoding='utf-8')
    entries.append({'threshold':percent,'model':{'type':'minecraft:model','model':'starforge:item/enhancement/'+name}})
atlas.save(texture_dir/'chance_atlas.png')
(item_dir/'chance.json').write_text(json.dumps({'oversized_in_gui':True,'model':{'type':'minecraft:range_dispatch','property':'minecraft:custom_model_data','index':0,'entries':entries,
    'fallback':{'type':'minecraft:model','model':'starforge:item/enhancement/chance_000'}}},separators=(',',':')),encoding='utf-8')

# Nine shared count positions center odd lengths. Even lengths need a 2px offset.
# Material selection supports 0..2304, so only positions 2..5 and decimal glyphs are needed.
even=json.loads((A/'items/crafting/count.json').read_text(encoding='utf-8'))
for branch in ('on_true','on_false'):
    models=even['model'][branch]['models'][2:6]
    even['model'][branch]['models']=models
    for dispatch in models:
        dispatch['entries']=[e for e in dispatch['entries'] if e['threshold']<=10]
        for entry in dispatch['entries']:
            old=entry['model']['model'].split('starforge:item/')[1]
            model=json.loads((A/'models/item'/(old+'.json')).read_text(encoding='utf-8'))
            model['display']['gui']['translation']=[2,0,0]
            name=old.replace('crafting/count_','enhancement/count_even_')
            (A/'models/item'/(name+'.json')).write_text(json.dumps(model,separators=(',',':')),encoding='utf-8')
            entry['model']['model']='starforge:item/'+name
(item_dir/'count_even.json').write_text(json.dumps(even,separators=(',',':')),encoding='utf-8')

native=Image.open(OUT/'craft_gallery_six-runtime-preview.png').convert('RGBA').crop((0,550,880,1020)).resize((176,94),Image.Resampling.NEAREST)
preview=Image.new('RGBA',(176,204));preview.alpha_composite(native,(0,110));preview.alpha_composite(background)
pd=ImageDraw.Draw(preview);pd.fontmode='1'
pd.text((25,4),'星锻强化',font=ImageFont.truetype(font_path,12),fill='#ead6af',anchor='lt')
for id,x in [('lucky',116),('protect',152)]:
    icon=Image.open(A/'textures/item'/f'{id}.png').convert('RGBA').resize((16,16),Image.Resampling.NEAREST)
    preview.alpha_composite(icon,(x,18))
preview.alpha_composite(rates[80],(8,26))
for id,x,quantity in [('stone',62,8),('intermediate_stone',80,0),('advanced_stone',98,0)]:
    icon=Image.open(A/'textures/item'/f'{id}.png').convert('RGBA').resize((16,16),Image.Resampling.NEAREST)
    preview.alpha_composite(icon,(x,54))
preview.alpha_composite(Image.open(ROOT/'tools/assets/enhancement-preview-sword.png').convert('RGBA'),(26,54))
pd=ImageDraw.Draw(preview);pd.fontmode='1';pd.text((86,5),'星芽短剑',font=font,fill='#a5d9b7',anchor='lt')
# Render exact runtime 3x5 glyph geometry at 2x so half-pixel coordinates stay exact.
preview2=preview.resize((352,408),Image.Resampling.NEAREST)
atlas_digits=Image.open(A/'textures/item/crafting/count_digits.png').convert('RGBA')
for center,quantity in [(70,8),(88,0),(106,0)]:
    text=str(quantity);pad=(9-len(text))//2
    for i,char in enumerate(text):
        pos=pad+i;theme=0
        name=f'enhancement/count_even_{theme}_{pos}_{char}' if len(text)%2==0 else f'crafting/count_{theme}_{pos}_{char}'
        model=json.loads((A/'models/item'/(name+'.json')).read_text(encoding='utf-8'))
        element=model['elements'][0];translation=model['display']['gui']['translation']
        left=center+element['from'][0]-8+translation[0]
        top=80+8-element['to'][1]-translation[1]
        glyph=atlas_digits.crop((int(char)*4,0,int(char)*4+3,5)).resize((6,10),Image.Resampling.NEAREST)
        preview2.alpha_composite(glyph,(round(left*2),round(top*2)))
preview2.save(OUT/'enhancement-material-tray-v4-logical-2x.png')
preview2.resize((880,1020),Image.Resampling.NEAREST).save(OUT/'enhancement-material-tray-v4-preview.png')
preview2.crop((0,0,352,220)).resize((704,440),Image.Resampling.NEAREST).save(OUT/'enhancement-material-tray-v4-menu.png')
print(json.dumps(dict(background=[176,110],weapon_center=[34,62],stone_centers=[[70+c*18,44+r*18] for r in range(3) for c in range(3)],
                     forge_center=[142,62],forge_hit_slots=[25,34],chance_center=[34,44],chance_atlas=[512,256],chance_entries=101,
                     lucky_center=[124,26],protect_center=[160,26],close_center=[88,98],rune_text=False,game_verified=False)))
