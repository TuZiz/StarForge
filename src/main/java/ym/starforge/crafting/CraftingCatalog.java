package ym.starforge.crafting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** One viewer's visible entries and unlock state for a single menu paint. */
final class CraftingCatalog {
    private final Map<String, List<Entry>> categories = new HashMap<>();

    CraftingCatalog(List<CraftingRecipe> recipes, Predicate<CraftingRecipe> unlocked) {
        for (var recipe : recipes) {
            boolean learned = unlocked.test(recipe);
            if (learned || !recipe.requiredHarvest().isEmpty()) {
                categories.computeIfAbsent(recipe.category(), key -> new ArrayList<>()).add(new Entry(recipe, learned));
            }
        }
    }

    List<Entry> entries(String category) {
        return categories.getOrDefault(category, List.of());
    }

    Progress progress(String category) {
        var entries = entries(category);
        return new Progress((int) entries.stream().filter(Entry::unlocked).count(), entries.size());
    }

    record Entry(CraftingRecipe recipe, boolean unlocked) {}

    record Progress(int unlocked, int visible) {
        boolean locked() { return unlocked == 0; }
    }
}
