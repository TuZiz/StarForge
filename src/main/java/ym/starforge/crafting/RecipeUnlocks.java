package ym.starforge.crafting;

import org.bukkit.configuration.file.YamlConfiguration;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Async single-writer storage; publish immutable grants only after an atomic durable replacement. */
public final class RecipeUnlocks implements AutoCloseable {
    private final Path file;
    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> new Thread(r, "StarForge-recipe-unlocks"));
    private volatile Map<UUID, Set<String>> grants;
    private volatile java.util.function.BiPredicate<UUID,String> harvests = (player,crop) -> false;

    public void harvestMilestones(java.util.function.BiPredicate<UUID,String> lookup) { harvests = lookup; }
    public RecipeUnlocks(Path file) throws Exception {
        this.file = file;
        Map<UUID, Set<String>> loaded = new HashMap<>();
        if (Files.exists(file)) {
            var y = new YamlConfiguration(); y.load(file.toFile());
            if (y.getInt("schema-version") != 1) throw new IllegalArgumentException(file + ":schema-version=" + y.get("schema-version"));
            var players = y.getConfigurationSection("players");
            if (players == null) throw new IllegalArgumentException(file + ":players missing");
            for (String uuid : players.getKeys(false)) {
                Object raw = players.get(uuid);
                if (!(raw instanceof List<?> values) || values.stream().anyMatch(v -> !(v instanceof String id) || !id.matches("[a-z0-9_-]+")))
                    throw new IllegalArgumentException(file + ":players." + uuid + "=" + raw);
                loaded.put(UUID.fromString(uuid), Set.copyOf(players.getStringList(uuid)));
            }
        }
        grants = Map.copyOf(loaded);
    }
    public boolean unlocked(UUID player, CraftingRecipe recipe) {
        return unlockState(player).test(recipe);
    }

    CraftingCatalog catalog(UUID player, List<CraftingRecipe> recipes) {
        return new CraftingCatalog(recipes, unlockState(player));
    }

    private java.util.function.Predicate<CraftingRecipe> unlockState(UUID player) {
        var granted = grants.getOrDefault(player, Set.of());
        var harvestLookup = harvests;
        return recipe -> recipe.defaultUnlocked() || granted.contains(recipe.id())
                || !recipe.requiredHarvest().isEmpty() && harvestLookup.test(player, recipe.requiredHarvest());
    }

    static String requirementKey(CraftingRecipe recipe) {
        return "golden_rice".equals(recipe.requiredHarvest()) ? "crafting-harvest-rice" : "crafting-locked";
    }

    public CompletableFuture<Void> grant(UUID player, String recipe) {
        if (!recipe.matches("[a-z0-9_-]+")) return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid recipe " + recipe));
        return CompletableFuture.runAsync(() -> {
            Map<UUID, Set<String>> next = new HashMap<>(grants);
            Set<String> values = new TreeSet<>(next.getOrDefault(player, Set.of()));
            if (!values.add(recipe)) return;
            next.put(player, Set.copyOf(values));
            try {
                var y = new YamlConfiguration(); y.set("schema-version", 1); y.createSection("players");
                next.forEach((uuid, ids) -> y.set("players." + uuid, new ArrayList<>(new TreeSet<>(ids))));
                Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
                Files.createDirectories(file.getParent()); Files.writeString(tmp, y.saveToString());
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                grants = Map.copyOf(next);
            } catch (Exception ex) { throw new CompletionException("Recipe unlock save failed file=" + file + " schema=1 player=" + player + " recipe=" + recipe, ex); }
        }, writer);
    }
    public void close() { writer.shutdown(); }
}
