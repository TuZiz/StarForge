"""Export StarForge's locked-card sheets and native catalogue counter models."""

from PIL import Image, ImageDraw, ImageFont
from crafting_captions import ASSETS, CAPTIONS, FONT, ROOT, export_caption, locked_palette, model_ref, write_json

UNLOCK = 'crafting/unlock'
DIGITS = ('111101101101111', '010110010010111', '111001111100111', '111001111001111',
          '101101111001001', '111100111001111', '111100111101111', '111001010010010',
          '111101111101111', '111101111001111', '001001010100100', '000000000000010')


def geometry(key, bounds, uv):
    return {'gui_light': 'front', 'textures': {'art': f'starforge:item/{key}', 'particle': f'starforge:item/{key}'},
            'elements': [{'from': bounds[:3], 'to': bounds[3:], 'shade': False,
                          'faces': {'south': {'uv': uv, 'texture': '#art'},
                                    'north': {'uv': [uv[2], uv[1], uv[0], uv[3]], 'texture': '#art'}}}],
            'display': {'gui': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]}}}


def item(name, key):
    write_json(f'items/{UNLOCK}/{name}.json', {'oversized_in_gui': True, 'model': model_ref(key)})


def export_cards():
    bg = ASSETS / 'textures/font/gui'
    categories = Image.open(bg / 'craft_types_six.png').convert('RGBA')
    gallery = Image.open(bg / 'craft_gallery_six.png').convert('RGBA')
    press = Image.open(ROOT.parent / 'Genlite/art/machines/fuel-press/preview.png').convert('RGBA')
    press = locked_palette(press.crop(press.getbbox()).resize((28, 32), Image.Resampling.NEAREST), 0.77)
    font = ImageFont.truetype(str(FONT), 10)
    stamp = Image.new('RGBA', (52, 36))
    draw = ImageDraw.Draw(stamp)
    draw.fontmode = '1'
    draw.text((27, 10), '未解锁', font=font, fill='#252628', anchor='mt')
    draw.text((26, 9), '未解锁', font=font, fill='#d2cbb8', anchor='mt')
    stamp = stamp.resize((104, 72), Image.Resampling.NEAREST)

    cards = [('category_equipment', categories.crop((8, 18, 60, 54)), False, False),
             ('category_machines', categories.crop((62, 18, 114, 54)), False, False),
             ('category', gallery.crop((8, 18, 60, 54)), True, False),
             ('recipe', gallery.crop((8, 18, 60, 54)), True, False),
             ('recipe_fuel_press', gallery.crop((8, 18, 60, 54)), True, True)]
    atlas = Image.new('RGBA', (256, 256))
    for index, (name, source, leave_name, machine) in enumerate(cards):
        panel = locked_palette(source, 0.85).resize((104, 72), Image.Resampling.NEAREST)
        if leave_name:
            # The caption is an independent item in the row below. Keep its footprint transparent.
            panel.paste((0, 0, 0, 0), (4, 36, 100, 68))
            # Shared recipe art can sit in the last row, where the menu draws its own divider.
            panel.paste((0, 0, 0, 0), (0, 68, 104, 72))
        if machine:
            panel.alpha_composite(press, (38, 4))
        panel.alpha_composite(stamp)
        x, y = index % 2 * 128, index // 2 * 80
        atlas.alpha_composite(panel, (x, y))
        key = f'{UNLOCK}/{name}'
        uv = [x / 16, y / 16, (x + 104) / 16, (y + 72) / 16]
        # The client limits element coordinates to -16..32. Scale around item centre to reach 52×36.
        model = geometry(f'{UNLOCK}/cards', [-5, -6, 8, 21, 12, 8.01], uv)
        model['display']['gui']['scale'] = [2, 2, 1]
        write_json(f'models/item/{key}.json', model)
        item(name, key)
    atlas.save(ASSETS / f'textures/item/{UNLOCK}/cards.png')


def export_counter():
    atlas = Image.new('RGBA', (64, 8))
    draw = ImageDraw.Draw(atlas)
    for glyph, bits in enumerate(DIGITS):
        for index, bit in enumerate(bits):
            if bit == '1':
                draw.point((glyph * 4 + index % 3, index // 3), fill='#d7cbb2')
    atlas.save(ASSETS / f'textures/item/{UNLOCK}/digits.png')

    slots = []
    for position in range(7):
        entries = []
        x = 9 - 4 * (6 - position)
        for glyph in range(len(DIGITS)):
            key = f'{UNLOCK}/digit_{position}_{glyph}'
            uv = [glyph, 0, glyph + .75, 10]
            write_json(f'models/item/{key}.json', geometry(f'{UNLOCK}/digits', [x, 7, 8, x + 3, 12, 8.01], uv))
            entries.append({'threshold': glyph + 1, 'model': model_ref(key)})
        slots.append({'type': 'minecraft:range_dispatch', 'property': 'minecraft:custom_model_data',
                      'index': position, 'entries': entries, 'fallback': {'type': 'minecraft:empty'}})
    write_json(f'items/{UNLOCK}/count.json',
               {'oversized_in_gui': True, 'model': {'type': 'minecraft:composite', 'models': slots}})


if __name__ == '__main__':
    (ASSETS / f'textures/item/{UNLOCK}').mkdir(parents=True, exist_ok=True)
    export_cards()
    export_counter()
    for recipe_id, name in CAPTIONS.items():
        export_caption(recipe_id, name)
