package ym.starforge.crafting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.bukkit.configuration.file.YamlConfiguration;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class RecipeUnlocksTest {
    @TempDir Path directory;
    private CraftingRecipe recipe(String id, boolean defaults) {
        var item = new CraftingRecipe.Ingredient("vanilla", "IRON_SWORD", 1);
        return new CraftingRecipe(id, id, List.of(item), item, 0, 0, "", defaults, "weapons");
    }
    @Test void defaultsAndLearnedRecipesArePlayerScopedAndSurviveRestart() throws Exception {
        UUID alice = UUID.randomUUID(), bob = UUID.randomUUID(); Path file = directory.resolve("unlocks.yml");
        var starter = recipe("starter", true); var learned = recipe("learned", false);
        try (var store = new RecipeUnlocks(file)) {
            assertTrue(store.unlocked(alice, starter)); assertFalse(store.unlocked(alice, learned));
            store.grant(alice, "learned").get(5, TimeUnit.SECONDS);
            store.grant(alice, "learned").get(5, TimeUnit.SECONDS);
            assertTrue(store.unlocked(alice, learned)); assertFalse(store.unlocked(bob, learned));
        }
        try (var store = new RecipeUnlocks(file)) {
            assertTrue(store.unlocked(alice, learned)); assertFalse(store.unlocked(bob, learned));
        }
    }
    @Test void concurrentGrantsDoNotOverwriteEachOther() throws Exception {
        UUID player = UUID.randomUUID();
        try (var store = new RecipeUnlocks(directory.resolve("unlocks.yml"))) {
            CompletableFuture.allOf(store.grant(player, "a"), store.grant(player, "b")).get(5, TimeUnit.SECONDS);
            assertTrue(store.unlocked(player, recipe("a", false)));
            assertTrue(store.unlocked(player, recipe("b", false)));
        }
    }
    @Test void failedSaveNeverPublishesUnlock() throws Exception {
        var parent = directory.resolve("blocked"); Files.writeString(parent, "file");
        UUID player = UUID.randomUUID();
        try (var store = new RecipeUnlocks(parent.resolve("unlocks.yml"))) {
            assertThrows(ExecutionException.class, () -> store.grant(player, "sword").get(5, TimeUnit.SECONDS));
            assertFalse(store.unlocked(player, recipe("sword", false)));
        }
    }
    @Test void itemhubTestRecipeMatchesAsterStatsDefinition() throws Exception {
        var recipe = new YamlConfiguration(); recipe.load(Path.of("src/main/resources/recipes.yml").toFile());
        assertTrue(recipe.getBoolean("recipes.aster_head.default-unlocked"));
        assertFalse(recipe.getBoolean("recipes.star_sword.default-unlocked"));
        assertEquals("itemhub", recipe.getString("recipes.star_sword.result.source"));
        var item = new YamlConfiguration(); item.load(Path.of("examples/itemhub/锻造测试武器.yml").toFile());
        assertEquals(item.getString("namespace") + ":试作长剑", recipe.getString("recipes.star_sword.result.id"));
        assertEquals(12, item.getDouble("试作长剑.integrations.asterstats.stats.attack"));
        assertEquals(.05, item.getDouble("试作长剑.integrations.asterstats.stats.crit_chance"));
        assertEquals(.5, item.getDouble("试作长剑.integrations.asterstats.stats.crit_damage"));
        assertEquals(List.of("mainhand"), item.getStringList("试作长剑.integrations.asterstats.slots"));
        assertTrue(item.getBoolean("试作长剑.remove-default-attack-damage"));
    }
    @Test void actualItemHubLoaderAcceptsWeaponAndIntegration() {
        // Material registry membership is a live-server check; the loader's schema/text path is real.
        var snapshot = new ym.itemhub.config.ItemConfigLoader(material -> true).load(Path.of("examples/itemhub").toFile());
        var weapon = snapshot.getDefinitions().get("星锻测试:试作长剑");
        assertNotNull(weapon);
        assertEquals(org.bukkit.Material.DIAMOND_SWORD, weapon.getMaterial());
        assertTrue(weapon.getRemoveDefaultAttackDamage());
        assertNotNull(weapon.getPresentation().getName());
        assertEquals(12.0, ((Map<?, ?>) weapon.getIntegrations().get("asterstats").get("stats")).get("attack"));
    }
}
