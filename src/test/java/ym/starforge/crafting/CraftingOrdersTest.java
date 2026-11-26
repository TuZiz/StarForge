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
        String key;
        try (var store = new CraftingOrders(file)) {
            var reservation = store.reserve(player, recipe, "AQ==", 6).get(5, TimeUnit.SECONDS);
            key = reservation.key();
            assertEquals("prepared", store.job(player, key).state());
            store.activate(reservation).get(5, TimeUnit.SECONDS);
        }
        try (var store = new CraftingOrders(file)) {
            var job = store.job(player, key);
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
            var reservation = store.reserve(player, a, "AQ==", 6).get(5, TimeUnit.SECONDS);
            assertThrows(ExecutionException.class, () -> store.reserve(player, b, "AQ==", 6).get(5, TimeUnit.SECONDS));
            assertEquals("sword", store.blocker(player, b).recipe());
            store.reserve(UUID.randomUUID(), b, "AQ==", 6).get(5, TimeUnit.SECONDS);
            store.activate(reservation).get(5, TimeUnit.SECONDS);
            assertThrows(ExecutionException.class, () -> store.beginClaim(store.blocker(player, a)).get(5, TimeUnit.SECONDS));
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
            assertThrows(ExecutionException.class, () -> store.reserve(player, recipe, "AQ==", 6).get(5, TimeUnit.SECONDS));
            assertThrows(ExecutionException.class, () -> store.reserve(player, recipe, "Ag==", 6).get(5, TimeUnit.SECONDS));
        }
    }
    @Test void menuResourceUsesMappedNineCharacterRows() throws Exception {
        for (String file : List.of("lang/zh_cn.yml", "plugin.yml", "recipes.yml")) {
            var resource = new YamlConfiguration(); resource.load(Path.of("src/main/resources/" + file).toFile());
        }
        var images = new YamlConfiguration(); images.load(Path.of("craftengine/强化石/configuration/gui-images.yml").toFile());
        for (var screen : CraftingScreen.values()) {
            var y = new YamlConfiguration(); y.load(Path.of("src/main/resources/" + screen.file).toFile());
            CraftingConfig.validateMenu(screen, y);
            var rows = y.getStringList("shape");
            assertTrue(y.contains("background-image"));
            assertTrue(y.getString("title").contains("<font:"));
            for (var symbol : y.getConfigurationSection("elements").getKeys(false)) {
                var element = y.getConfigurationSection("elements." + symbol);
                if (Set.of("previous", "next", "back", "orders", "info", "confirm", "close").contains(element.getString("action"))) {
                    if (element.contains("craft-engine")) {
                        CraftingGuiIcons.validateId(element.getString("craft-engine"), screen.file + ":elements." + symbol);
                        continue;
                    }
                    var material = org.bukkit.Material.getMaterial(element.getString("material", ""));
                    assertNotNull(material); assertFalse(material.isLegacy());
                    assertFalse(Set.of("AIR", "CAVE_AIR", "VOID_AIR").contains(material.name()));
                }
            }
            if (screen == CraftingScreen.DETAIL) {
                assertEquals(6, rows.size());
                assertEquals(3, rows.stream().flatMapToInt(String::chars).filter(c -> c == 'C').count());
                assertEquals(1, rows.stream().flatMapToInt(String::chars).filter(c -> c == 'B').count());
                assertEquals('O', rows.get(1).charAt(7));
                assertEquals(6, CraftingOrderLayout.slots(y,"ingredient").size());
                assertEquals(CraftingOrderLayout.slots(y,"ingredient").stream().map(s -> s + 9).toList(),
                        CraftingOrderLayout.slots(y,"ingredient-count"));
            } else if (screen == CraftingScreen.RECIPES) {
                assertEquals(5, rows.size());
                assertEquals(6, CraftingOrderLayout.slots(y,"recipe").size());
            } else if (screen == CraftingScreen.ORDERS) {
                var cells = CraftingOrderLayout.cells(y); assertEquals(6, cells.size());
                assertEquals(5, rows.size());
                assertEquals(List.of(1,4,7,19,22,25), cells.stream().map(CraftingOrderLayout.Cell::itemSlot).toList());
                assertEquals(List.of(10,13,16,28,31,34), cells.stream().map(CraftingOrderLayout.Cell::progressSlot).toList());
            }
        }
        assertEquals(Set.of("starforge:forge_menu", "starforge:craft_orders_six", "starforge:craft_types_six",
                "starforge:craft_gallery_six", "starforge:craft_detail_six"), images.getConfigurationSection("images").getKeys(false));
        var recipes = new YamlConfiguration(); recipes.load(Path.of("src/main/resources/recipes.yml").toFile());
        assertFalse(recipes.contains("recipes.iron_sword"));
        assertEquals(60, recipes.getInt("recipes.star_sword.duration-seconds"));
        var inputs = recipes.getMapList("recipes.star_sword.ingredients");
        assertEquals(List.of("starforge:stone", "starforge:intermediate_stone"), inputs.stream().map(i -> i.get("id")).toList());
        assertTrue(inputs.stream().allMatch(i -> "craft-engine".equals(i.get("source"))));
        assertEquals("itemhub", recipes.getString("recipes.star_sword.result.source"));
        assertEquals("星锻测试:试作长剑", recipes.getString("recipes.star_sword.result.id"));
    }
    @Test void layoutValidationReportsMissingAndUnsupportedActions() throws Exception {
        var y = new YamlConfiguration(); y.load(Path.of("src/main/resources/gui/crafting.yml").toFile());
        y.set("elements.R", null);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> CraftingConfig.validateMenu(CraftingScreen.RECIPES, y))
                .getMessage().contains("gui/crafting.yml:elements.R"));
        y.load(Path.of("src/main/resources/gui/crafting.yml").toFile()); y.set("elements.R.action", "result");
        assertTrue(assertThrows(IllegalArgumentException.class, () -> CraftingConfig.validateMenu(CraftingScreen.RECIPES, y))
                .getMessage().contains("action=result"));
        y.load(Path.of("src/main/resources/gui/crafting.yml").toFile()); y.set("elements.R.action", "none");
        assertTrue(assertThrows(IllegalArgumentException.class, () -> CraftingConfig.validateMenu(CraftingScreen.RECIPES, y))
                .getMessage().contains("missing action recipe"));
    }
    @Test void buttonMaterialValidationReportsFileAndInvalidValue() throws Exception {
        var y = new YamlConfiguration(); y.load(Path.of("src/main/resources/gui/crafting.yml").toFile());
        for (String value : List.of("", "AIR", "CAVE_AIR", "VOID_AIR", "NOT_A_MATERIAL")) {
            y.set("elements.T.craft-engine", null); y.set("elements.T.material", value);
            assertTrue(assertThrows(IllegalArgumentException.class, () -> CraftingConfig.validateMenu(CraftingScreen.RECIPES, y))
                    .getMessage().contains("gui/crafting.yml:elements.T.material="));
        }
    }
}
