package ym.starforge.crafting;

import org.bukkit.configuration.file.YamlConfiguration;
import java.util.*;

/** Maps paired item/progress cells from Shape, never from fixed inventory indexes. */
final class CraftingOrderLayout {
    record Cell(int itemSlot, int progressSlot) {}
    private CraftingOrderLayout() {}
    static List<Integer> slots(YamlConfiguration y, String action) {
        var result = new ArrayList<Integer>(); int slot = 0;
        for (String row : y.getStringList("shape")) for (char symbol : row.toCharArray()) {
            if (action.equals(y.getString("elements." + symbol + ".action"))) result.add(slot);
            slot++;
        }
        return List.copyOf(result);
    }
    static List<Cell> cells(YamlConfiguration y) {
        var items = slots(y, "order"); var bars = slots(y, "progress");
        if (items.size() != bars.size()) throw new IllegalArgumentException("gui/crafting-orders.yml:shape order/progress count must match");
        var cells = new ArrayList<Cell>();
        for (int i = 0; i < items.size(); i++) {
            if (bars.get(i) != items.get(i) + 9)
                throw new IllegalArgumentException("gui/crafting-orders.yml:shape progress must be directly below its order");
            cells.add(new Cell(items.get(i), bars.get(i)));
        }
        return List.copyOf(cells);
    }
    static String symbol(YamlConfiguration y, int slot) { return "" + y.getStringList("shape").get(slot / 9).charAt(slot % 9); }
}
