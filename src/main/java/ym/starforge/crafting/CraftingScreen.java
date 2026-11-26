package ym.starforge.crafting;

import java.util.Set;

/** One YAML per independently opened chest screen. */
public enum CraftingScreen {
    CATEGORIES("gui/crafting-types.yml", "category", Set.of("category", "orders"), Set.of("close")),
    RECIPES("gui/crafting.yml", "recipe", Set.of("recipe", "previous", "next", "orders"), Set.of("close", "info", "back", "recipe-caption")),
    DETAIL("gui/crafting-detail.yml", "ingredient", Set.of("ingredient", "result", "confirm", "back"), Set.of("previous", "next", "close", "ingredient-count")),
    ORDERS("gui/crafting-orders.yml", "order", Set.of("order", "previous", "next", "back"), Set.of("close", "progress", "info"));

    final String file;
    final String contentAction;
    final Set<String> requiredActions;
    final Set<String> allowedActions;

    CraftingScreen(String file, String contentAction, Set<String> requiredActions) {
        this(file, contentAction, requiredActions, Set.of());
    }
    CraftingScreen(String file, String contentAction, Set<String> requiredActions, Set<String> optionalActions) {
        this.file = file; this.contentAction = contentAction; this.requiredActions = requiredActions;
        var allowed = new java.util.HashSet<>(requiredActions); allowed.addAll(optionalActions);
        this.allowedActions = Set.copyOf(allowed);
    }
}
