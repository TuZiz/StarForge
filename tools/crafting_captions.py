"""Export recipe captions at the shared workshop card dimensions."""

import argparse
import json
from pathlib import Path

from PIL import Image, ImageDraw, ImageEnhance, ImageFont, ImageOps

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'craftengine/强化石/resourcepack/assets/starforge'
FRAME = ROOT / 'artifacts/ui-previews/framed-navigation-v1-generated.png'
FONT = Path('C:/Users/32633/Desktop/leaf/plugins/Xerr/generated/resource_pack/assets/xerr_font/font/fusion_pixel.ttf')
COLOR = '#ead6af'
ARMOR_CAPTIONS = {
    'aster_head': '星芽护额',
    'aster_chest': '星芽轻甲',
    'aster_legs': '星芽护腿',
    'aster_feet': '星芽短靴',
}
CAPTIONS = {
    'iron_sword': '铁剑',
    'star_sword': '试作星辉长剑',
    **ARMOR_CAPTIONS,
    'fuel_press': '燃料压制机',
}


def nameplate(name, texture_scale=2):
    frame = Image.open(FRAME).convert('RGBA')
    frame = frame.crop((116, 250, 267, 295)).resize((48, 14), Image.Resampling.NEAREST)
    ImageDraw.Draw(frame).rectangle((2, 2, 45, 11), fill='#303236')
    label = Image.new('RGBA', (48, 16))
    label.alpha_composite(frame, (0, 1))
    label = label.resize((48 * texture_scale, 16 * texture_scale), Image.Resampling.NEAREST)

    font = ImageFont.truetype(str(FONT), 10)
    left, top, right, bottom = font.getbbox(name, anchor='lt')
    glyph = Image.new('RGBA', (max(round(font.getlength(name)), right) - left, bottom - top))
    draw = ImageDraw.Draw(glyph)
    draw.fontmode = '1'
    draw.text((-left, -top), name, font=font, fill=COLOR, anchor='lt')
    glyph = glyph.resize((glyph.width * texture_scale, glyph.height * texture_scale), Image.Resampling.NEAREST)
    max_width = 44 * texture_scale

    # Keep the native height. At 2x atlas density, this limit preserves each
    # source column and prevents very long labels from becoming illegibly thin.
    if glyph.width > max_width:
        if texture_scale != 2 or glyph.width * 7 > max_width * 10:
            raise ValueError(f'铭牌名称过长，无法保留像素笔画：{name}')
        glyph = glyph.resize((max_width, glyph.height), Image.Resampling.NEAREST)
    label.alpha_composite(glyph, ((label.width - glyph.width) // 2, (label.height - glyph.height) // 2))
    return label


def export_caption(recipe_id, name):
    label = nameplate(name)
    atlas = Image.new('RGBA', (128, 64))
    atlas.alpha_composite(label)
    key = f'crafting/name_{recipe_id}'
    atlas.save(ASSETS / f'textures/item/{key}.png')
    locked_key = f'{key}_locked'
    locked_palette(atlas, 0.9).save(ASSETS / f'textures/item/{locked_key}.png')

    model = json.loads((ASSETS / 'models/item/crafting/craft_progress_000.json').read_text(encoding='utf-8'))
    model['elements'][0]['faces']['south']['uv'] = [0, 0, 12, 8]
    model['elements'][0]['faces']['north']['uv'] = [12, 0, 0, 8]
    for variant in (key, locked_key):
        model['textures'] = {field: f'starforge:item/{variant}' for field in ('art', 'particle')}
        write_json(f'models/item/{variant}.json', model)
    entry = {'type': 'minecraft:condition', 'property': 'minecraft:custom_model_data', 'index': 0,
             'on_true': model_ref(locked_key), 'on_false': model_ref(key)}
    write_json(f'items/{key}.json', {'oversized_in_gui': True, 'model': entry})
    return label


def locked_palette(image, brightness):
    gray = ImageOps.colorize(ImageOps.grayscale(image), '#1d2024', '#bfc0b9').convert('RGBA')
    shaded = ImageEnhance.Brightness(Image.blend(image, gray, 0.87)).enhance(brightness)
    shaded.putalpha(image.getchannel('A'))
    return shaded


def model_ref(key):
    return {'type': 'minecraft:model', 'model': f'starforge:item/{key}'}


def write_json(relative, data):
    path = ASSETS / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    if not path.exists() or json.loads(path.read_text(encoding='utf-8')) != data:
        path.write_text(json.dumps(data, separators=(',', ':')), encoding='utf-8')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='导出指定锻造配方的 CE 铭牌，不重建其他菜单素材。')
    parser.add_argument('recipe_id', nargs='+', choices=CAPTIONS)
    for recipe_id in parser.parse_args().recipe_id:
        export_caption(recipe_id, CAPTIONS[recipe_id])
        print(f'已导出铭牌 {recipe_id}')
