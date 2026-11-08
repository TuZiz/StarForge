package ym.starforge.crafting;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.*;

/** Read only on an asynchronous scheduler. Existing installations require explicit new files. */
public record CraftingConfig(List<CraftingRecipe> recipes, YamlConfiguration menu) {
    public static CraftingConfig load(JavaPlugin plugin) throws Exception {
        var y = read(plugin, "recipes.yml");
        var menu = read(plugin, "gui/crafting.yml");
        var rows = menu.getStringList("shape");
        if (rows.isEmpty() || rows.size() > 6 || rows.stream().anyMatch(r -> r.length() != 9))
            throw new IllegalArgumentException("gui/crafting.yml:shape must contain 1-6 rows of 9 characters");
        for (String row : rows) for (char symbol : row.toCharArray()) {
            if (symbol != ' ' && !menu.isConfigurationSection("elements." + symbol))
                throw new IllegalArgumentException("gui/crafting.yml:elements missing " + symbol);
            if (symbol != ' ' && !Set.of("none", "recipe", "result", "confirm", "previous", "next", "close", "progress")
                    .contains(menu.getString("elements." + symbol + ".action", "none")))
                throw new IllegalArgumentException("gui/crafting.yml:elements." + symbol + ".action=" + menu.get("elements." + symbol + ".action"));
        }
        for (String required : List.of("recipe", "result", "confirm", "previous", "next", "close", "progress")) {
            boolean found = rows.stream().flatMapToInt(String::chars).anyMatch(c ->
                    required.equals(menu.getString("elements." + (char)c + ".action")));
            if (!found) throw new IllegalArgumentException("gui/crafting.yml:shape missing action " + required);
        }
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
            recipes.add(new CraftingRecipe(id, Objects.requireNonNull(s.getString("name"), "recipes.yml:" + id + ".name"),
                    inputs, ingredient(output.getValues(false), id + ".result"), money, n.longValue(), group));
        }
        return new CraftingConfig(List.copyOf(recipes), menu);
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
    private static YamlConfiguration read(JavaPlugin plugin, String path) throws Exception {
        var file = new File(plugin.getDataFolder(), path);
        if (!file.isFile()) throw new IllegalArgumentException(file + ": missing; install new crafting resource explicitly");
        var y = new YamlConfiguration(); y.load(file);
        if (y.getInt("schema-version") != 1) throw new IllegalArgumentException(file + ":schema-version=" + y.get("schema-version"));
        return y;
    }
}
