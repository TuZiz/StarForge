package ym.starforge.crafting;

import java.util.List;

public record CraftingRecipe(String id, String name, List<Ingredient> ingredients,
                             Ingredient result, double money, long durationSeconds, String group,
                             boolean defaultUnlocked, String category, String requiredHarvest) {
    public CraftingRecipe(String id, String name, List<Ingredient> ingredients, Ingredient result,
                          double money, long durationSeconds, String group, boolean defaultUnlocked, String category) {
        this(id, name, ingredients, result, money, durationSeconds, group, defaultUnlocked, category, "");
    }
    public CraftingRecipe { ingredients = List.copyOf(ingredients); }
    public CraftingRecipe(String id, String name, List<Ingredient> ingredients, Ingredient result,
                          double money, long durationSeconds, String group) {
        this(id, name, ingredients, result, money, durationSeconds, group, true, CraftingCategories.DEFAULT);
    }
    public String cooldownKey() { return group.isEmpty() ? "recipe/" + id : "group/" + group; }
    public record Ingredient(String source, String id, int amount) {}
}
