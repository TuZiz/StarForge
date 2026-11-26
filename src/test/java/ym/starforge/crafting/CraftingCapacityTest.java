package ym.starforge.crafting;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class CraftingCapacityTest {
    @TempDir Path directory;
    private final CraftingRecipe recipe = new CraftingRecipe("ce", "ce", List.of(),
            new CraftingRecipe.Ingredient("craft-engine", "test:weapon", 1), 0, 0, "");
    @Test void concurrentReservationsRespectLimitAndDoNotAffectOtherPlayers() throws Exception {
        UUID player = UUID.randomUUID();
        try (var store = new CraftingOrders(directory.resolve("orders.yml"))) {
            var requests = new ArrayList<CompletableFuture<CraftingJob>>();
            for (int i = 0; i < 20; i++) requests.add(store.reserve(player, recipe, "AQ==", 6));
            int accepted = 0;
            for (var request : requests) {
                try { request.get(5, TimeUnit.SECONDS); accepted++; }
                catch (ExecutionException ex) { assertInstanceOf(CraftingOrders.CapacityException.class, ex.getCause()); }
            }
            assertEquals(6, accepted); assertEquals(6, store.count(player));
            store.reserve(UUID.randomUUID(), recipe, "AQ==", 6).get(5, TimeUnit.SECONDS);
            assertEquals(6, store.count(player));
        }
    }
    @Test void preparedRunningReadyAndClaimingAllOccupyUntilReleasedOrCompleted() throws Exception {
        UUID player = UUID.randomUUID();
        try (var store = new CraftingOrders(directory.resolve("orders.yml"))) {
            var a = store.reserve(player, recipe, "AQ==", 1).get(5, TimeUnit.SECONDS);
            assertFull(store, player);
            store.release(a).get(5, TimeUnit.SECONDS);
            var b = store.reserve(player, recipe, "Ag==", 1).get(5, TimeUnit.SECONDS);
            store.activate(b).get(5, TimeUnit.SECONDS); assertFull(store, player);
            var ready = b.state("running"); store.beginClaim(ready).get(5, TimeUnit.SECONDS); assertFull(store, player);
            store.restore(ready).get(5, TimeUnit.SECONDS); assertFull(store, player);
            store.beginClaim(ready).get(5, TimeUnit.SECONDS); store.complete(ready).get(5, TimeUnit.SECONDS);
            assertEquals(0, store.count(player));
            store.reserve(player, recipe, "Aw==", 1).get(5, TimeUnit.SECONDS);
        }
    }
    @Test void loweringLimitKeepsExistingOrdersClaimableAfterRestart() throws Exception {
        UUID player = UUID.randomUUID(); Path file = directory.resolve("orders.yml");
        try (var store = new CraftingOrders(file)) {
            for (int i = 0; i < 8; i++) {
                var order = store.reserve(player, recipe, "AQ==", 8).get(5, TimeUnit.SECONDS);
                store.activate(order).get(5, TimeUnit.SECONDS);
            }
        }
        try (var store = new CraftingOrders(file)) {
            assertEquals(8, store.jobs(player).size()); assertFull(store, player);
            for (var order : store.jobs(player)) {
                store.beginClaim(order).get(5, TimeUnit.SECONDS); store.complete(order).get(5, TimeUnit.SECONDS);
            }
            assertEquals(0, store.count(player));
        }
    }
    @Test void limitDefaultsToSixAndRejectsInvalidValues() {
        var y = new YamlConfiguration(); assertEquals(6, CraftingConfig.orderLimit(y));
        for (Object value : List.of(0, -1, 55, 1.5, "6", true)) {
            y.set("max-active-orders", value);
            assertTrue(assertThrows(IllegalArgumentException.class, () -> CraftingConfig.orderLimit(y)).getMessage().contains("recipes.yml:max-active-orders="));
        }
        y.set("max-active-orders", 1); assertEquals(1, CraftingConfig.orderLimit(y));
        y.set("max-active-orders", 54); assertEquals(54, CraftingConfig.orderLimit(y));
    }
    private void assertFull(CraftingOrders store, UUID player) {
        var error = assertThrows(ExecutionException.class, () -> store.reserve(player, recipe, "AQ==", 1).get(5, TimeUnit.SECONDS));
        assertInstanceOf(CraftingOrders.CapacityException.class, error.getCause());
    }
}
