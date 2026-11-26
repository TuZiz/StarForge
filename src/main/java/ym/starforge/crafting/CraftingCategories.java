package ym.starforge.crafting;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Validated navigation and titles for one crafting configuration snapshot. */
record CraftingCategories(Map<Integer, String> slots, Map<String, String> titles) {
    static final String DEFAULT = "weapons"; // 装备
    private static final Pattern ID = Pattern.compile("[a-z0-9_-]+");

    CraftingCategories {
        slots = Map.copyOf(slots);
        titles = Map.copyOf(titles);
    }

    static boolean validId(Object value) {
        return value instanceof String id && ID.matcher(id).matches();
    }

    static String resolve(ConfigurationSection button, Set<String> recipeCategories) {
        String action = button.getString("action", "none");
        if (action.equals("category")) {
            Object target = button.get("category");
            if (!validId(target)) {
                throw new IllegalArgumentException(CraftingScreen.CATEGORIES.file + ":" + button.getCurrentPath()
                        + ".category=" + target + " requires a category ID");
            }
            return (String) target;
        }
        if (action.equals("equipment")) return DEFAULT;
        if (action.equals("machines") || action.equals(DEFAULT)) return action;
        if (action.equals("none") || CraftingScreen.CATEGORIES.allowedActions.contains(action)) return null;
        return recipeCategories.contains(action) ? action : null;
    }

    static Map<String, String> readTitles(YamlConfiguration gallery) {
        var titles = new LinkedHashMap<String, String>();
        // The old gallery key remains a fallback for existing servers.
        if (gallery.isString("machines-title")) titles.put("machines", gallery.getString("machines-title"));
        if (!gallery.contains("category-titles")) return titles;
        var section = gallery.getConfigurationSection("category-titles");
        if (section == null) throw invalidTitle("category-titles", gallery.get("category-titles"));
        for (String id : section.getKeys(false)) {
            Object value = section.get(id);
            if (!validId(id) || !(value instanceof String title)) throw invalidTitle("category-titles." + id, value);
            titles.put(id, title);
        }
        return titles;
    }

    private static IllegalArgumentException invalidTitle(String path, Object value) {
        return new IllegalArgumentException(CraftingScreen.RECIPES.file + ":" + path + "=" + value
                + " requires a category ID and a title string");
    }
}
