"""Export the four starter armor captions using the shared recipe card renderer."""

from crafting_captions import ARMOR_CAPTIONS, export_caption

if __name__ == '__main__':
    for recipe_id, name in ARMOR_CAPTIONS.items():
        export_caption(recipe_id, name)
