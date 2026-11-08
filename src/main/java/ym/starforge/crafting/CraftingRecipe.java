package ym.starforge.crafting;

import java.util.List;

public record CraftingRecipe(String id, String name, List<Ingredient> ingredients,
                             Ingredient result, double money, long durationSeconds, String group) {
    public CraftingRecipe { ingredients = List.copyOf(ingredients); }
    public String cooldownKey() { return group.isEmpty() ? "recipe/" + id : "group/" + group; }
    public record Ingredient(String source, String id, int amount) {}
}
