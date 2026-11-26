"""Export the approved generated preview into grid-aligned CE assets; never calls an image API."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import json

ROOT = Path(__file__).resolve().parents[1]
CE = ROOT / 'craftengine/强化石'
ASSETS = CE / 'resourcepack/assets/starforge'
SOURCE = ROOT / 'artifacts/ui-previews/orders-six-pixel-v1-generated.png'
FOOTER_SOURCE = ROOT / 'artifacts/ui-previews/orders-six-footer-v2-generated.png'
FONT = Path('C:/Users/32633/Desktop/leaf/plugins/Xerr/generated/resource_pack/assets/xerr_font/font/fusion_pixel_quest_12_sc.ttf')
font = ImageFont.truetype(str(FONT), 12)
raw = Image.open(SOURCE).convert('RGBA')
background = raw.crop((70, 49, 955, 824)).resize((176, 122), Image.Resampling.NEAREST)
bottom_edge = background.crop((0, 118, 176, 122))
background = background.crop((0, 0, 176, 110))
background.alpha_composite(bottom_edge, (0, 106))
d = ImageDraw.Draw(background); d.fontmode = '1'
d.rectangle((5, 3, 170, 105), fill='#303236')
d.line((5, 17, 170, 17), fill='#6c6257')
d.line((5, 89, 170, 89), fill='#6c6257')
header_anvil = raw.crop((116, 71, 177, 120)).resize((13, 11), Image.Resampling.NEAREST)
background.alpha_composite(header_anvil, (10, 5))
card = raw.crop((103, 153, 365, 424)).resize((52, 36), Image.Resampling.NEAREST)
cd = ImageDraw.Draw(card); cd.rectangle((2, 2, 49, 33), fill='#36383b')
for top in (18, 54):
    for left in (8, 62, 116): background.alpha_composite(card, (left, top))
d = ImageDraw.Draw(background); d.fontmode = '1'
# Remove protruding card corners and trim flecks at the toolbar boundary.
d.rectangle((5, 87, 170, 90), fill='#303236')
d.line((8, 88, 167, 88), fill='#55575a', width=1)
footer = Image.open(FOOTER_SOURCE).convert('RGBA')
def footer_icon(box):
    icon = footer.crop(box).resize((9, 9), Image.Resampling.NEAREST)
    icon.putdata([(234,214,175,255) if r > 150 and g > 140 and b > 110 else (0,0,0,0)
                  for r,g,b,a in [icon.getpixel((x,y)) for y in range(9) for x in range(9)]])
    return icon
background.alpha_composite(footer_icon((207,682,256,729)), (84,94))
bg_path = ASSETS / 'textures/font/gui/craft_orders_six.png'
bg_path.parent.mkdir(parents=True, exist_ok=True); background.save(bg_path)

DIGITS = {'0':['111','101','101','101','111'], '1':['010','110','010','010','111'],
 '2':['111','001','111','100','111'], '3':['111','001','111','001','111'],
 '4':['101','101','111','001','001'], '5':['111','100','111','001','111'],
 '6':['111','100','111','101','111'], '7':['111','001','010','010','010'],
 '8':['111','101','111','101','111'], '9':['111','101','111','001','111'],
 '%':['101','001','010','100','101']}
texture_dir = ASSETS / 'textures/item/crafting'
model_dir = ASSETS / 'models/item/crafting'
texture_dir.mkdir(parents=True, exist_ok=True); model_dir.mkdir(parents=True, exist_ok=True)
logical_bars = {}
def asset(name, image, label, wide):
    image.save(texture_dir / (name + '.png'))
    model = {'gui_light':'front', 'ambientocclusion':False,
             'textures':{'art':'starforge:item/crafting/' + name, 'particle':'starforge:item/crafting/' + name},
             'elements':[{'from':[-16 if wide else 0, 0, 8], 'to':[32 if wide else 16, 16, 8.01], 'shade':False,
               'faces':{'south':{'uv':[0,0,16,16], 'texture':'#art'},
                        'north':{'uv':[16,0,0,16], 'texture':'#art'}}}],
             'display':{'gui':{'rotation':[0,0,0], 'translation':[0,0,0], 'scale':[1,1,1]}}}
    (model_dir / (name + '.json')).write_text(json.dumps(model, separators=(',',':')), encoding='utf-8')
    item_dir = ASSETS / 'items/crafting'
    item_dir.mkdir(parents=True, exist_ok=True)
    (item_dir / (name + '.json')).write_text(json.dumps({'oversized_in_gui': wide, 'model': {'type': 'minecraft:model', 'model': 'starforge:item/crafting/' + name}}, separators=(',', ':')), encoding='utf-8')
for progress in range(101):
    tile = Image.new('RGBA', (48,16)); td = ImageDraw.Draw(tile)
    td.rectangle((3,0,44,5), fill='#161a1d', outline='#666059')
    fill = 0 if progress == 0 else max(1, int(40*progress/100))
    color = '#6ade45' if progress == 100 else '#e6ac36'
    if fill: td.rectangle((4,1,3+fill,3), fill=color)
    for x in range(7,44,4): td.line((x,1,x,4), fill='#414140')
    text = str(progress) + '%'; start = (48 - (len(text)*4-1))//2
    for i, c in enumerate(text):
        for row, pixels in enumerate(DIGITS[c]):
            for col, pixel in enumerate(pixels):
                if pixel == '1': td.point((start+i*4+col, 9+row), fill=color if progress == 100 else '#e8dcc1')
    name = f'craft_progress_{progress:03d}'; logical_bars[name] = tile
    asset(name, tile.resize((64,32), Image.Resampling.NEAREST), f'<#86EFAC>锻造进度 · {progress}%', True)
for state, label, color in [('empty','空闲','#7f8283'), ('locked','未开放','#636668'), ('pending','处理中','#d9ad57')]:
    tile = Image.new('RGBA',(48,16)); td = ImageDraw.Draw(tile); td.fontmode='1'
    td.text((24,2), label, font=font, fill=color, anchor='mt')
    name = 'craft_progress_' + state; logical_bars[name] = tile
    asset(name, tile.resize((64,32),Image.Resampling.NEAREST), '<#94A3B8>' + label, True)
anvil = raw.crop((460,502,562,591)).resize((22,18),Image.Resampling.NEAREST)
for state, label in [('empty','空闲锻造位'), ('locked','未开放锻造位')]:
    tile = Image.new('RGBA',(32,32)); tile.alpha_composite(anvil,(5,7))
    # The selected source's silhouette is flat gray; remove its original dark card backdrop.
    pixels = tile.load()
    for y in range(32):
        for x in range(32):
            r,g,b,a = pixels[x,y]
            if a and max(r,g,b) < 82: pixels[x,y] = (0,0,0,0)
            elif state == 'locked' and a: pixels[x,y] = (r//2,g//2,b//2,a)
    asset('craft_slot_' + state,tile,'<#94A3B8>'+label,False)

# Nine pages cover the configured maximum of 54 orders with the default six-cell Shape.
nav_tiles = {}
def nav(name, tile, label):
    nav_tiles[name] = tile
    asset(name, tile.resize((32,32),Image.Resampling.NEAREST),label,False)
for name, direction, label in [('previous',-1,'上一页'),('next',1,'下一页')]:
    tile = Image.new('RGBA',(16,16)); td = ImageDraw.Draw(tile)
    td.line([(8-direction*2,4),(8+direction*2,8),(8-direction*2,12)],fill='#bda77d',width=1)
    nav('craft_nav_'+name,tile,'<#EAD6AF>'+label)
page_digits = {**DIGITS, '/':['001','001','010','100','100']}
for pages in range(1,10):
    for page in range(1,pages+1):
        tile = Image.new('RGBA',(16,16)); td = ImageDraw.Draw(tile)
        for i,c in enumerate(f'{page}/{pages}'):
            for y,line in enumerate(page_digits[c]):
                for x,pixel in enumerate(line):
                    if pixel=='1': td.point((2+i*4+x,5+y),fill='#ead6af')
        nav(f'craft_page_{page}_{pages}',tile,f'<#EAD6AF>第 {page}/{pages} 页')
tile = Image.new('RGBA',(16,16)); td = ImageDraw.Draw(tile)
for x in (4,8,12): td.point((x,8),fill='#ead6af')
nav('craft_page_more',tile,'<#EAD6AF>查看页码')

native = Image.open(ROOT/'artifacts/ui-previews/chest-layout-dense-native.png').convert('RGBA')
preview = Image.new('RGBA',(176,204))
preview.alpha_composite(native.crop((0,126,176,222)),(0,108))
preview.alpha_composite(background,(0,0))
# Native inventory label is drawn after the menu title; keep its full band visible in the preview.
inventory_draw = ImageDraw.Draw(preview); inventory_draw.fontmode = '1'
inventory_draw.rectangle((5,110,170,121),fill='#c6c6c6')
inventory_draw.text((8,110),'物品栏',font=font,fill='#404040',anchor='lt')
pd = ImageDraw.Draw(preview); pd.fontmode='1'; pd.text((25,4),'我的锻造 4/6',font=font,fill='#ead6af',anchor='lt')
from zipfile import ZipFile
from io import BytesIO
client = Path('C:/Users/32633/Desktop/悠梦星辰Minecraft26.1/.minecraft/versions/悠梦星辰Minecraft26.1/悠梦星辰Minecraft26.1.jar')
with ZipFile(client) as archive:
    for index, (item, progress) in enumerate([('iron_sword',65),('iron_sword',32),('diamond_sword',100),('iron_axe',12),(None,None),(None,None)]):
        x = 34 + index%3*54; y = 26 + index//3*36
        icon = Image.open(BytesIO(archive.read('assets/minecraft/textures/item/'+item+'.png'))).convert('RGBA') if item else Image.open(texture_dir/'craft_slot_empty.png').resize((16,16),Image.Resampling.NEAREST)
        preview.alpha_composite(icon,(x-8,y-8))
        preview.alpha_composite(logical_bars[f'craft_progress_{progress:03d}' if progress is not None else 'craft_progress_empty'],(x-24,y+10))
preview.save(ROOT/'artifacts/ui-previews/orders-six-runtime-logical.png')
preview.resize((880,1020),Image.Resampling.NEAREST).save(ROOT/'artifacts/ui-previews/orders-six-runtime-preview.png')
report = {'background':[176,110], 'texture_count':106, 'navigation_count':48, 'progress_size':[64,32], 'progress_display':[48,16],
 'item_centers':[[34+c*54,26+r*36] for r in range(2) for c in range(3)], 'inventory_first_row_y':122,
 'inventory_label_covered':False, 'reused_generated_art':[str(SOURCE),str(FOOTER_SOURCE)], 'game_verified':False}
(ROOT/'.verification/six-orders-assets.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False))
