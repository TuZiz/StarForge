package ym.starforge.crafting;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

/** Configuration validation is async-safe; item creation requires the viewer's ownership thread. */
final class CraftingGuiIcons {
    private CraftingGuiIcons() {}
    static void validate(ConfigurationSection element, String path) {
        if (element.contains("craft-engine")) { validateId(element.get("craft-engine"), path + ".craft-engine"); return; }
        String value = element.getString("material", "");
        Material material = Material.getMaterial(value);
        if (material == null || material.isLegacy() || java.util.Set.of("AIR", "CAVE_AIR", "VOID_AIR").contains(value))
            throw new IllegalArgumentException(path + ".material=" + value);
    }
    static void validateId(Object id, String path) {
        if (!(id instanceof String value) || !value.matches("[a-z0-9_.-]+:[a-z0-9_/.-]+"))
            throw new IllegalArgumentException(path + "=" + id + " requires namespace:id");
    }
    static ItemStack create(ConfigurationSection element) {
        if (element.contains("craft-engine")) return ce(element.getString("craft-engine"));
        Material material = Material.valueOf(element.getString("material"));
        if (!material.isItem()) throw new IllegalArgumentException(element.getCurrentPath() + ".material=" + material);
        return new ItemStack(material);
    }
    static ItemStack ce(String id) {
        var model = CraftingDecorationModels.model(id);
        var item = new CraftingItems().create(new CraftingRecipe.Ingredient("craft-engine", model == null ? id : "starforge:gui_blank", 1));
        if (model != null) item.editMeta(meta -> {
            meta.setItemModel(model);
            meta.displayName(net.kyori.adventure.text.Component.empty());
            meta.lore(java.util.List.of());
        });
        CraftingMenuRenderer.decorate(item, java.util.List.of(), java.util.Map.of());
        return item;
    }
}
