package ym.starforge.crafting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.bukkit.configuration.file.YamlConfiguration;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class CraftingOrdersTest {
    @TempDir Path directory;
    private CraftingRecipe recipe(String id, String group, long duration) {
        var item = new CraftingRecipe.Ingredient("vanilla", "IRON_SWORD", 1);
        return new CraftingRecipe(id, id, List.of(item), item, 0, duration, group);
    }
    @Test void restartRetainsProgressAndOneClaimOnly() throws Exception {
        Path file = directory.resolve("orders.yml"); UUID player = UUID.randomUUID();
        var recipe = recipe("sword", "", 0);
        try (var store = new CraftingOrders(file)) {
            store.reserve(player, recipe, "AQ==").get(5, TimeUnit.SECONDS);
            assertEquals("prepared", store.job(player, recipe).state());
            store.activate(player, recipe).get(5, TimeUnit.SECONDS);
        }
        try (var store = new CraftingOrders(file)) {
            var job = store.job(player, recipe);
            assertEquals(100, job.progress()); assertEquals(0, job.remaining());
            store.beginClaim(job).get(5, TimeUnit.SECONDS);
            assertThrows(ExecutionException.class, () -> store.beginClaim(job).get(5, TimeUnit.SECONDS));
            store.complete(job).get(5, TimeUnit.SECONDS);
        }
        try (var store = new CraftingOrders(file)) { assertTrue(store.jobs(player).isEmpty()); }
    }
    @Test void groupBlocksParallelRecipeButPlayersAreIndependent() throws Exception {
        var a = recipe("sword", "weapons", 3600); var b = recipe("axe", "weapons", 60);
        UUID player = UUID.randomUUID();
        try (var store = new CraftingOrders(directory.resolve("orders.yml"))) {
            store.reserve(player, a, "AQ==").get(5, TimeUnit.SECONDS);
            assertThrows(ExecutionException.class, () -> store.reserve(player, b, "AQ==").get(5, TimeUnit.SECONDS));
            assertEquals("sword", store.job(player, b).recipe());
            store.reserve(UUID.randomUUID(), b, "AQ==").get(5, TimeUnit.SECONDS);
            store.activate(player, a).get(5, TimeUnit.SECONDS);
            assertThrows(ExecutionException.class, () -> store.beginClaim(store.job(player, a)).get(5, TimeUnit.SECONDS));
        }
    }
    @Test void corruptedOrderFailsWithPath() throws Exception {
        var path = directory.resolve("orders.yml"); Files.writeString(path, "schema-version: 4\n");
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new CraftingOrders(path)).getMessage().contains("schema-version"));
    }
    @Test void failedSaveStopsFurtherMutations() throws Exception {
        Path parent = directory.resolve("blocked"); Files.writeString(parent, "not a directory");
        try (var store = new CraftingOrders(parent.resolve("orders.yml"))) {
            UUID player = UUID.randomUUID(); var recipe = recipe("sword", "", 60);
            assertThrows(ExecutionException.class, () -> store.reserve(player, recipe, "AQ==").get(5, TimeUnit.SECONDS));
            assertThrows(ExecutionException.class, () -> store.activate(player, recipe).get(5, TimeUnit.SECONDS));
        }
    }
    @Test void menuResourceUsesMappedNineCharacterRows() throws Exception {
        for (String file : List.of("lang/zh_cn.yml", "plugin.yml", "gui/crafting.yml", "recipes.yml")) {
            var resource = new YamlConfiguration(); resource.load(Path.of("src/main/resources/" + file).toFile());
        }
        var y = new YamlConfiguration();
        y.load(Path.of("src/main/resources/gui/crafting.yml").toFile());
        for (String row : y.getStringList("shape")) {
            assertEquals(9, row.length());
            for (char c : row.toCharArray()) assertNotNull(y.getConfigurationSection("elements." + c));
        }
        var rows = y.getStringList("shape");
        // Pixel background uses native chest centers, not arbitrary positions in generated artwork.
        assertEquals("BFTFCFNFX", rows.get(4));
        assertEquals('O', rows.get(2).charAt(7));
        for (int row = 1; row <= 3; row++) assertEquals("RRR", rows.get(row).substring(1, 4));
        var recipes = new YamlConfiguration(); recipes.load(Path.of("src/main/resources/recipes.yml").toFile());
        assertEquals(60, recipes.getInt("recipes.iron_sword.duration-seconds"));
        var inputs = recipes.getMapList("recipes.iron_sword.ingredients");
        assertEquals(List.of("starforge:stone", "starforge:intermediate_stone"), inputs.stream().map(i -> i.get("id")).toList());
        assertTrue(inputs.stream().allMatch(i -> "craft-engine".equals(i.get("source"))));
        assertEquals("vanilla", recipes.getString("recipes.iron_sword.result.source"));
        assertEquals("IRON_SWORD", recipes.getString("recipes.iron_sword.result.id"));
    }
}
