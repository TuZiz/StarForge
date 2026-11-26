package ym.starforge.crafting;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Viewer-thread item models for the catalogue's two-row pixel cards. */
final class CraftingUnlockArt {
    private static final String ROOT = "starforge:crafting/unlock/";
    private static final int COUNT_COLUMNS = 7;

    private CraftingUnlockArt() {}

    static boolean galleryCard(CraftingSession session, int slot) {
        return pixelCardAnchor(slot) && "starforge:craft_gallery_six".equals(
                session.revision.menu(CraftingScreen.RECIPES).getString("background-image"))
                && CraftingRecipeCards.cells(session.revision.menu(CraftingScreen.RECIPES), slot).size() == 6;
    }

    static boolean categoryCard(CraftingSession session, int slot, String category) {
        if (!pixelCardAnchor(slot) || !"starforge:craft_types_six".equals(
                session.revision.menu(CraftingScreen.CATEGORIES).getString("background-image"))) return false;
        for (int offset : new int[]{-1, 0, 1, 8, 9, 10}) {
            if (!category.equals(session.revision.categoryAt(slot + offset))) return false;
        }
        return true;
    }

    private static boolean pixelCardAnchor(int slot) {
        return slot >= 0 && (slot / 9 == 0 || slot / 9 == 2) && slot % 9 % 3 == 1;
    }

    static ItemStack recipeIcon(ConfigurationSection element, String recipeId) {
        String model = element.getString("locked-models." + recipeId,
                ROOT + (recipeId.equals("fuel_press") ? "recipe_fuel_press" : "recipe"));
        return decoration(model);
    }

    static void categoryIcon(ItemStack item, ConfigurationSection element, int slot, String category) {
        String builtIn = switch (slot) {
            case 1 -> category.equals(CraftingCategories.DEFAULT) ? "category_equipment" : "category";
            case 4 -> category.equals("machines") ? "category_machines" : "category";
            default -> "category";
        };
        item.editMeta(meta -> meta.setItemModel(NamespacedKey.fromString(element.getString("locked-card-model", ROOT + builtIn))));
    }

    static void lockedCaption(ItemStack caption, String id) {
        if (CraftingDecorationModels.model(id) == null) return;
        caption.editMeta(meta -> {
            var data = meta.getCustomModelDataComponent();
            data.setFlags(List.of(true));
            meta.setCustomModelDataComponent(data);
        });
    }

    static void count(ItemStack item, CraftingCatalog.Progress progress) {
        item.editMeta(meta -> {
            meta.setItemModel(NamespacedKey.fromString(ROOT + "count"));
            var data = meta.getCustomModelDataComponent();
            data.setFloats(digits(progress));
            meta.setCustomModelDataComponent(data);
        });
    }

    static List<Float> digits(CraftingCatalog.Progress progress) {
        // Exact counts remain available in the tooltip when an unusually large catalogue exceeds the card.
        String text = progress.visible() > 999 ? "..." : progress.unlocked() + "/" + progress.visible();
        var digits = new ArrayList<Float>(COUNT_COLUMNS);
        for (int i = 0; i < COUNT_COLUMNS; i++) {
            int index = i - COUNT_COLUMNS + text.length();
            if (index < 0) digits.add(0f);
            else {
                char value = text.charAt(index);
                digits.add(value == '/' ? 11f : value == '.' ? 12f : value - '0' + 1f);
            }
        }
        return List.copyOf(digits);
    }

    private static ItemStack decoration(String model) {
        var item = CraftingGuiIcons.ce("starforge:gui_blank");
        item.editMeta(meta -> meta.setItemModel(NamespacedKey.fromString(model)));
        return item;
    }
}
