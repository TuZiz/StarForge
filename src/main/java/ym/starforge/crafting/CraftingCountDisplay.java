package ym.starforge.crafting;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** Native item-model digits. Called only from the viewer's ownership thread. */
final class CraftingCountDisplay {
    private CraftingCountDisplay() {}
    static List<Float> digits(int owned, int required) {
        String text = Math.min(owned, 9999) + "/" + required;
        var values = new ArrayList<Float>();
        int padding = (9 - text.length()) / 2;
        for (int i = 0; i < 9; i++) {
            int at = i - padding;
            char c = at >= 0 && at < text.length() ? text.charAt(at) : ' ';
            values.add(c == ' ' ? 0f : c == '/' ? 11f : (float)(c - '0' + 1));
        }
        return List.copyOf(values);
    }
    static ItemStack create(int owned, int required) {
        var item = CraftingGuiIcons.ce("starforge:gui_blank");
        item.editMeta(meta -> {
            meta.setItemModel(new NamespacedKey("starforge", "crafting/count"));
            var data = meta.getCustomModelDataComponent();
            data.setFloats(digits(owned, required)); data.setFlags(List.of(owned >= required));
            meta.setCustomModelDataComponent(data);
        });
        return item;
    }
}
