"""Export selected generated art to native chest grids; no image API or server writes."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
from zipfile import ZipFile
from io import BytesIO
import json
from crafting_captions import CAPTIONS, export_caption, nameplate

ROOT = Path(__file__).resolve().parents[1]
CE = ROOT / 'craftengine/强化石'
A = CE / 'resourcepack/assets/starforge'
OUT = ROOT / 'artifacts/ui-previews'
font = ImageFont.truetype('C:/Users/32633/Desktop/leaf/plugins/Xerr/generated/resource_pack/assets/xerr_font/font/fusion_pixel_quest_12_sc.ttf',12)
small_font = ImageFont.truetype('C:/Users/32633/Desktop/leaf/plugins/Xerr/generated/resource_pack/assets/xerr_font/font/fusion_pixel.ttf',10)
gallery = Image.open(OUT/'gallery-six-v1-generated.png').convert('RGBA')
detail = Image.open(OUT/'detail-workbench-v1-generated.png').convert('RGBA')
framed = Image.open(OUT/'framed-navigation-v1-generated.png').convert('RGBA')
base = Image.open(A/'textures/font/gui/craft_orders_six.png').convert('RGBA')

def text(im,xy,value,color='#ead6af',anchor='mt'):
    d=ImageDraw.Draw(im); d.fontmode='1'; d.text(xy,value,font=font,fill=color,anchor=anchor)

def panel(height):
    im=Image.new('RGBA',(176,height)); im.alpha_composite(base.crop((0,0,176,18)))
    # Preserve the generated iron/copper outer frame and replace only its contents.
    im.alpha_composite(base.crop((0,18,176,102)).resize((176,height-26),Image.Resampling.NEAREST),(0,18))
    im.alpha_composite(base.crop((0,102,176,110)),(0,height-8))
    d=ImageDraw.Draw(im); d.rectangle((5,18,170,height-5),fill='#303236')
    d.line((8,height-22,167,height-22),fill='#55575a')
    return im

# One card edge from the approved gallery, stripped of all example icons and labels.
card=gallery.crop((378,242,650,504)).resize((52,36),Image.Resampling.NEAREST)
ImageDraw.Draw(card).rectangle((2,2,49,33),fill='#36383b')
grid=panel(110)
for y in (18,54):
    for x in (8,62,116):grid.alpha_composite(card,(x,y))
d=ImageDraw.Draw(grid);d.rectangle((5,87,170,90),fill='#303236');d.line((8,88,167,88),fill='#55575a')
# Reuse the approved gray/copper button bevels, replacing generated lettering at
# exact native pixel sizes. B is centered at x88, T is centered at x142.
def framed_button(box,size):
    im=framed.crop(box).resize(size,Image.Resampling.NEAREST)
    ImageDraw.Draw(im).rectangle((2,2,size[0]-3,size[1]-3),fill='#303236')
    return im

return_button=framed_button((319,532,476,585),(40,14))
rd=ImageDraw.Draw(return_button);rd.fontmode='1'
rd.line([(10,4),(6,7),(10,10)],fill='#ead6af',width=1)
rd.text((25,2),'返回',font=small_font,fill='#ead6af',anchor='mt')
orders_button=framed_button((534,532,705,585),(52,14))
anvil=framed.crop((545,543,576,573)).resize((6,6),Image.Resampling.NEAREST)
ap=anvil.load()
for y in range(anvil.height):
    for x in range(anvil.width):
        r,g,b,alpha=ap[x,y]
        if max(r,g,b)<85:ap[x,y]=(0,0,0,0)
orders_button.alpha_composite(anvil,(3,4))
od=ImageDraw.Draw(orders_button);od.fontmode='1'
od.text((10,2),'我的锻造',font=small_font,fill='#ead6af',anchor='lt')
grid.alpha_composite(return_button,(68,92))
grid.alpha_composite(orders_button,(116,92))

types=grid.copy()
# Equipment and machines occupy the first two cards.
equipment=gallery.crop((149,269,303,430)).resize((16,16),Image.Resampling.NEAREST)
pixels=equipment.load()
for y in range(16):
    for x in range(16):
        r,g,b,a=pixels[x,y]
        if max(r,g,b)<78:pixels[x,y]=(0,0,0,0)
# Center the visible silhouette, not the padded source crop. Keep the icon/name group
# vertically centered inside the first 52x36 card (x8..59, y18..53).
equipment=equipment.crop(equipment.getbbox())
types.alpha_composite(equipment,(34-equipment.width//2,21+(16-equipment.height)//2))
types.alpha_composite(nameplate('装备', texture_scale=1),(10,36))
machine_icon_path=ROOT.parent/'Genlite/craftengine/genlite/resourcepack/assets/genlite/textures/item/gear.png'
with Image.open(machine_icon_path) as source:
    machine=source.convert('RGBA').resize((16,16),Image.Resampling.NEAREST)
types.alpha_composite(machine,(80,21))
types.alpha_composite(nameplate('机器', texture_scale=1),(64,36))

confirm=panel(128)
text(confirm,(61,18),'拥有 / 需要',color='#929799')
for y in (36,72):
    for x in (26,62,98):
        # Reuse the selected detail's simple frame, with input icons removed.
        tile=card.resize((20,20),Image.Resampling.NEAREST)
        ImageDraw.Draw(tile).rectangle((2,2,17,17),fill='#36383b')
        confirm.alpha_composite(tile,(x-2,y-2))
text(confirm,(142,18),'成品')
text(confirm,(88,111),'返回')
text(confirm,(142,111),'制作')
# Arrow crop from the approved detail; mask its dark source backdrop.
arrow=detail.crop((537,251,676,289)).resize((13,4),Image.Resampling.NEAREST)
arrow.putdata([(r,g,b,a) if min(r,g,b)>110 else (0,0,0,0) for r,g,b,a in arrow.getdata()])
confirm.alpha_composite(arrow,(124,42))

images={'craft_types_six':types,'craft_gallery_six':grid,'craft_detail_six':confirm}
for name,im in images.items():im.save(A/'textures/font/gui'/f'{name}.png')
p=CE/'configuration/gui-images.yml';s=p.read_text(encoding='utf-8')
for name,im in images.items():
    if f'  starforge:{name}:' not in s:s+=f'  starforge:{name}:\n    height: {im.height}\n    ascent: 13\n    font: minecraft:default\n    file: starforge:font/gui/{name}.png\n'
p.write_text(s,encoding='utf-8')

def write_json(path,obj):
    path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(obj,separators=(',',':')),encoding='utf-8')

# 3x5 bitmap digits, one shared 64x32 RGBA atlas. No per-value texture explosion.
digits=['111101101101111','010110010010111','111001111100111','111001111001111',
        '101101111001001','111100111001111','111100111101111','111001010010010',
        '111101111101111','111101111001111','001001010100100']
atlas=Image.new('RGBA',(64,32));ad=ImageDraw.Draw(atlas)
for theme,color in enumerate(('#86efac','#f87171')):
    for glyph,bits in enumerate(digits):
        for i,bit in enumerate(bits):
            if bit=='1':ad.point((glyph*4+i%3,theme*8+i//3),fill=color)
atlas.save(A/'textures/item/crafting/count_digits.png')
branches=[]
for theme in range(2):
    models=[]
    for pos in range(9):
        entries=[]
        for g in range(11):
            name=f'crafting/count_{theme}_{pos}_{g}'
            x=-9.5+pos*4
            # Flat geometry follows the existing working order progress models.
            model={'gui_light':'front','textures':{'art':'starforge:item/crafting/count_digits','particle':'starforge:item/crafting/count_digits'},
                   'elements':[{'from':[x,5.5,8],'to':[x+3,10.5,8.01],'shade':False,
                    'faces':{'south':{'uv':[g,theme*4,g+.75,theme*4+2.5],'texture':'#art'},
                             'north':{'uv':[g+.75,theme*4,g,theme*4+2.5],'texture':'#art'}}}],
                   'display':{'gui':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[1,1,1]}}}
            write_json(A/'models/item'/f'{name}.json',model)
            entries.append({'threshold':g+1,'model':{'type':'minecraft:model','model':'starforge:item/'+name}})
        models.append({'type':'minecraft:range_dispatch','property':'minecraft:custom_model_data','index':pos,
                       'entries':entries,'fallback':{'type':'minecraft:empty'}})
    branches.append({'type':'minecraft:composite','models':models})
write_json(A/'items/crafting/count.json',{'oversized_in_gui':True,'model':{'type':'minecraft:condition','property':'minecraft:custom_model_data','index':0,
           'on_true':branches[0],'on_false':branches[1]}})

labels = {recipe_id: export_caption(recipe_id, name) for recipe_id, name in CAPTIONS.items()}

# Slot-exact engineering previews, distinct from generated concepts or game screenshots.
with ZipFile('C:/Users/32633/Desktop/悠梦星辰Minecraft26.1/.minecraft/versions/悠梦星辰Minecraft26.1/悠梦星辰Minecraft26.1.jar') as z:
    sword=Image.open(BytesIO(z.read('assets/minecraft/textures/item/iron_sword.png'))).convert('RGBA')
native=Image.open(OUT/'chest-layout-dense-native.png').convert('RGBA')
for name,background in images.items():
    im=Image.new('RGBA',(176,background.height+94)); im.alpha_composite(native.crop((0,126,176,220)),(0,background.height))
    im.alpha_composite(background); text(im,(25,4), '配方图鉴' if 'types' in name else '装备配方' if 'gallery' in name else '制作确认',anchor='lt')
    text(im,(8,background.height),'物品栏',color='#404040',anchor='lt')
    if 'gallery' in name: im.alpha_composite(sword,(26,18));im.alpha_composite(labels['iron_sword'].resize((48,16),Image.Resampling.NEAREST),(10,36))
    if 'detail' in name:
        im.alpha_composite(sword,(134,36))
        for x,id,value in [(26,'stone','8/8'),(62,'intermediate_stone','2/2')]:
            # Existing CE textures are the sole material art source.
            icon=Image.open(A/'textures/item'/f'{id}.png').convert('RGBA').resize((16,16),Image.Resampling.NEAREST)
            im.alpha_composite(icon,(x,36))
            for at,c in enumerate(value):
                glyph=10 if c=='/' else int(c)
                im.alpha_composite(atlas.crop((glyph*4,0,glyph*4+3,5)),(x+8-(len(value)*4-1)//2+at*4,60))
    im.resize((880,im.height*5),Image.Resampling.NEAREST).save(OUT/f'{name}-runtime-preview.png')
print('Exported three backgrounds, shared count atlas, 198 glyph models, count item and caption; no server writes.')
