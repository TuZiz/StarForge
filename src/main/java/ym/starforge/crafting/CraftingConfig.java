package ym.starforge.crafting;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.nio.file.Path;
import java.util.*;

/** Read only on an asynchronous scheduler; SettingsLoader installs missing GUI resources first. */
public record CraftingConfig(List<CraftingRecipe> recipes, Map<CraftingScreen, YamlConfiguration> menus,
                             int maxActiveOrders, CraftingCategories categories) {
    public CraftingConfig {
        recipes = List.copyOf(recipes); menus = Map.copyOf(menus);
    }
    public YamlConfiguration menu(CraftingScreen screen) { return menus.get(screen); }
    String categoryAt(int slot) { return categories.slots().get(slot); }
    String galleryTitle(String category) { return categories.titles().getOrDefault(category, menu(CraftingScreen.RECIPES).getString("title")); }
    public static CraftingConfig load(JavaPlugin plugin) throws Exception {
        return load(plugin.getDataFolder().toPath());
    }
    static CraftingConfig load(Path directory) throws Exception {
        var y = read(directory, "recipes.yml");
        var recipes = readRecipes(y);
        var recipeCategories = new HashSet<String>();
        recipes.forEach(recipe -> recipeCategories.add(recipe.category()));
        var menus = new EnumMap<CraftingScreen, YamlConfiguration>(CraftingScreen.class);
        Map<Integer, String> categorySlots = Map.of();
        for (var screen : CraftingScreen.values()) {
            var menu = read(directory, screen.file);
            var slots = validateMenu(screen, menu, recipeCategories);
            if (screen == CraftingScreen.CATEGORIES) categorySlots = slots;
            menus.put(screen, menu);
        }
        var categories = new CraftingCategories(categorySlots, CraftingCategories.readTitles(menus.get(CraftingScreen.RECIPES)));
        return new CraftingConfig(recipes, menus, orderLimit(y), categories);
    }
    private static List<CraftingRecipe> readRecipes(YamlConfiguration y) {
        List<CraftingRecipe> recipes = new ArrayList<>();
        var section = Objects.requireNonNull(y.getConfigurationSection("recipes"), "recipes.yml:recipes");
        for (String id : section.getKeys(false)) {
            var s = section.getConfigurationSection(id);
            if (s == null || !id.matches("[a-z0-9_-]+")) throw new IllegalArgumentException("recipes.yml:recipes." + id);
            List<CraftingRecipe.Ingredient> inputs = new ArrayList<>();
            for (var raw : s.getMapList("ingredients")) inputs.add(ingredient(raw, id + ".ingredients"));
            if (inputs.isEmpty()) throw new IllegalArgumentException("recipes.yml:" + id + ".ingredients empty");
            var output = s.getConfigurationSection("result");
            if (output == null) throw new IllegalArgumentException("recipes.yml:" + id + ".result missing");
            double money = s.getDouble("money", -1);
            Object duration = s.get("duration-seconds");
            if (!Double.isFinite(money) || money < 0 || money > 1e9 || !(duration instanceof Number n)
                    || n.doubleValue() != n.longValue() || n.longValue() < 0 || n.longValue() > 31536000)
                throw new IllegalArgumentException("recipes.yml:" + id + " invalid money/duration-seconds: " + s.getValues(false));
            String group = s.getString("cooldown-group", "");
            if (!group.isEmpty() && !group.matches("[a-z0-9_-]+")) throw new IllegalArgumentException("recipes.yml:" + id + ".cooldown-group=" + group);
            Object category = s.get("category", CraftingCategories.DEFAULT);
            if (!CraftingCategories.validId(category)) throw new IllegalArgumentException("recipes.yml:recipes." + id + ".category=" + category);
            String harvest = s.getString("required-harvest", "");
            if (s.contains("required-harvest") && (!s.isString("required-harvest") || !harvest.matches("[a-z0-9_]+")
                    || defaultUnlocked(s, id))) {
                throw new IllegalArgumentException("recipes.yml:recipes." + id + ".required-harvest=" + s.get("required-harvest")
                    + " requires a crop ID and default-unlocked: false");
            }
            recipes.add(new CraftingRecipe(id, Objects.requireNonNull(s.getString("name"), "recipes.yml:" + id + ".name"),
                    inputs, ingredient(output.getValues(false), id + ".result"), money, n.longValue(), group,
                    defaultUnlocked(s, id), (String) category, harvest));
        }
        return recipes;
    }
    static int orderLimit(YamlConfiguration y) {
        Object raw = y.get("max-active-orders", 6);
        if (!(raw instanceof Number n) || n.doubleValue() != n.intValue() || n.intValue() < 1 || n.intValue() > 54)
            throw new IllegalArgumentException("recipes.yml:max-active-orders=" + raw + " must be an integer from 1 to 54");
        return n.intValue();
    }
    static void validateMenu(CraftingScreen screen, YamlConfiguration menu) {
        validateMenu(screen, menu, Set.of());
    }
    private static Map<Integer, String> validateMenu(CraftingScreen screen, YamlConfiguration menu, Set<String> recipeCategories) {
        var rows = menu.getStringList("shape");
        if (rows.isEmpty() || rows.size() > 6 || rows.stream().anyMatch(r -> r.length() != 9))
            throw new IllegalArgumentException(screen.file + ":shape must contain 1-6 rows of 9 characters");
        Set<String> found = new HashSet<>();
        var categorySlots = new LinkedHashMap<Integer, String>();
        int slot = -1;
        for (String row : rows) for (char symbol : row.toCharArray()) {
            slot++;
            if (symbol == ' ') continue;
            String path = "elements." + symbol;
            if (!menu.isConfigurationSection(path)) throw new IllegalArgumentException(screen.file + ":" + path + " missing");
            Object rawAction = menu.get(path + ".action", "none");
            if (!(rawAction instanceof String action))
                throw new IllegalArgumentException(screen.file + ":" + path + ".action=" + rawAction + " must be a string");
            if (screen == CraftingScreen.CATEGORIES) {
                String target = CraftingCategories.resolve(menu.getConfigurationSection(path), recipeCategories);
                if (target != null) {
                    categorySlots.put(slot, target);
                    action = "category";
                }
            }
            if (!action.equals("none") && !screen.allowedActions.contains(action))
                throw new IllegalArgumentException(screen.file + ":" + path + ".action=" + action);
            if (action.equals("recipe")) {
                String models = path + ".locked-models";
                if (menu.contains(models)) {
                    var variants = menu.getConfigurationSection(models);
                    if (variants == null) throw new IllegalArgumentException(screen.file + ":" + models
                            + "=" + menu.get(models) + " requires a recipe-to-model mapping");
                    for (String id : variants.getKeys(false))
                        CraftingGuiIcons.validateId(variants.get(id), screen.file + ":" + models + "." + id);
                }
                for (String key : List.of("material-enough", "material-missing")) {
                    String template = menu.getString(path + "." + key, "");
                    if (!template.contains("{material}") || !template.contains("{required}") || !template.contains("{owned}"))
                        throw new IllegalArgumentException(screen.file + ":" + path + "." + key + " must include material/required/owned placeholders");
                }
                if (!menu.getStringList(path + ".lore").contains("{materials}"))
                    throw new IllegalArgumentException(screen.file + ":" + path + ".lore missing {materials}");
            }
            if (action.equals("category") && menu.contains(path + ".locked-card-model"))
                CraftingGuiIcons.validateId(menu.get(path + ".locked-card-model"), screen.file + ":" + path + ".locked-card-model");
            if (action.equals("recipe-caption") && menu.isConfigurationSection(path + ".captions")) {
                var captions = menu.getConfigurationSection(path + ".captions");
                for (String id : captions.getKeys(false)) CraftingGuiIcons.validateId(captions.get(id), path + ".captions." + id);
            }
            if (!Set.of("none", "recipe", "order", "ingredient", "result", "progress", "ingredient-count").contains(action))
                CraftingGuiIcons.validate(menu.getConfigurationSection(path), screen.file + ":" + path);
            if (action.equals("order") && menu.contains("background-image"))
                for (String key : List.of("empty-craft-engine", "locked-craft-engine"))
                    CraftingGuiIcons.validateId(menu.get(path + "." + key), screen.file + ":" + path + "." + key);
            if (action.equals("info") && menu.contains(path + ".page-craft-engine-prefix"))
                for (String key : List.of("page-craft-engine-prefix", "overflow-craft-engine"))
                    CraftingGuiIcons.validateId(menu.get(path + "." + key), screen.file + ":" + path + "." + key);
            if (action.equals("progress"))
                for (String key : List.of("craft-engine-prefix", "pending-craft-engine", "empty-craft-engine", "locked-craft-engine"))
                    CraftingGuiIcons.validateId(menu.get(path + "." + key), screen.file + ":" + path + "." + key);
            found.add(action);
        }
        for (String action : screen.requiredActions) if (!found.contains(action))
            throw new IllegalArgumentException(screen.file + ":shape missing action " + action);
        if (screen == CraftingScreen.ORDERS && found.contains("progress")) CraftingOrderLayout.cells(menu);
        if (found.contains("ingredient-count")) validatePairs(menu, screen, "ingredient", "ingredient-count");
        if (found.contains("recipe-caption")) validatePairs(menu, screen, "recipe", "recipe-caption");
        return categorySlots;
    }
    private static void validatePairs(YamlConfiguration menu, CraftingScreen screen, String item, String label) {
        var slots = CraftingOrderLayout.slots(menu, item); var labels = CraftingOrderLayout.slots(menu, label);
        if (!labels.equals(slots.stream().map(s -> s + 9).toList()))
            throw new IllegalArgumentException(screen.file + ":shape " + label + " must be directly below every " + item);
    }
    private static boolean defaultUnlocked(ConfigurationSection s, String id) {
        if (s.contains("default-unlocked") && !(s.get("default-unlocked") instanceof Boolean))
            throw new IllegalArgumentException("recipes.yml:recipes." + id + ".default-unlocked=" + s.get("default-unlocked"));
        return s.getBoolean("default-unlocked", true);
    }
    private static CraftingRecipe.Ingredient ingredient(Map<?, ?> raw, String path) {
        String source = String.valueOf(raw.get("source")), id = String.valueOf(raw.get("id"));
        Object amount = raw.get("amount");
        if (!Set.of("vanilla", "craft-engine", "itemhub").contains(source) || id.equals("null") || id.isBlank()
                || !(amount instanceof Number n) || n.doubleValue() != n.intValue() || n.intValue() < 1 || n.intValue() > 2304)
            throw new IllegalArgumentException("recipes.yml:" + path + " invalid " + raw);
        if (source.equals("vanilla")) {
            var material = org.bukkit.Material.matchMaterial(id);
            if (material == null || material.isAir() || !material.isItem()) throw new IllegalArgumentException("recipes.yml:" + path + ".id=" + id);
        }
        return new CraftingRecipe.Ingredient(source, id, n.intValue());
    }
    private static YamlConfiguration read(Path directory, String path) throws Exception {
        var file = directory.resolve(path).toFile();
        if (!file.isFile()) throw new IllegalArgumentException(file + ": missing; install new crafting resource explicitly");
        var y = new YamlConfiguration(); y.load(file);
        if (y.getInt("schema-version") != 1) throw new IllegalArgumentException(file + ":schema-version=" + y.get("schema-version"));
        return y;
    }
}
