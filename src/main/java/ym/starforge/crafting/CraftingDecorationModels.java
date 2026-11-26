package ym.starforge.crafting;

import org.bukkit.NamespacedKey;

/** Reserved GUI decoration IDs remain valid without registering collectible CE items. */
final class CraftingDecorationModels {
    private CraftingDecorationModels() {}

    static NamespacedKey model(String id) {
        if (id == null || !id.startsWith("starforge:")) return null;
        String name = id.substring("starforge:".length());
        boolean supported = name.matches("craft_progress_(00[0-9]|0[1-9][0-9]|100|empty|locked|pending)")
                || name.matches("craft_slot_(empty|locked)")
                || name.matches("craft_nav_(previous|next)") || name.equals("craft_page_more");
        if (name.matches("craft_page_[1-9]_[1-9]"))
            supported = name.charAt(11) <= name.charAt(13);
        if (name.matches("craft_name_[a-z0-9_]+")) {
            return new NamespacedKey("starforge", "crafting/name_" + name.substring(11));
        }
        return supported ? new NamespacedKey("starforge", "crafting/" + name) : null;
    }
}
