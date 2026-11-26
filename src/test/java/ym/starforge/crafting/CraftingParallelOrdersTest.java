package ym.starforge.crafting;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class CraftingParallelOrdersTest {
    @TempDir Path directory;
    private CraftingRecipe recipe(long duration, int amount) {
        var item = new CraftingRecipe.Ingredient("vanilla", "IRON_SWORD", amount);
        return new CraftingRecipe("sword", "sword", List.of(item), item, 0, duration, "");
    }
    @Test void concurrentSameRecipeCreatesSeparateOrdersAndSurvivesRestart() throws Exception {
        Path file = directory.resolve("orders.yml"); UUID player = UUID.randomUUID();
        List<CraftingJob> expected = new ArrayList<>();
        try (var store = new CraftingOrders(file)) {
            var requests = new ArrayList<CompletableFuture<CraftingJob>>();
            for (int i = 0; i < 32; i++) requests.add(store.reserve(player, recipe(60, 1), "AQ==", 54));
            for (var request : requests) {
                var reservation = request.get(5, TimeUnit.SECONDS);
                store.activate(reservation).get(5, TimeUnit.SECONDS);
            }
            expected.addAll(store.jobs(player));
            assertEquals(32, expected.size());
            assertEquals(32, expected.stream().map(CraftingJob::key).distinct().count());
            assertNull(store.blocker(player, recipe(60, 1)));
            assertTrue(store.jobs(UUID.randomUUID()).isEmpty());
            for (var job : expected) {
                assertEquals(60_000, job.ready() - job.started());
                assertEquals("running", job.state());
                assertNull(store.job(UUID.randomUUID(), job.key()));
            }
        }
        try (var store = new CraftingOrders(file)) { assertEquals(expected, store.jobs(player)); }
    }
    @Test void eachOrderKeepsItsDeadlineAndFrozenResult() throws Exception {
        UUID player = UUID.randomUUID(); Path file = directory.resolve("orders.yml");
        CraftingJob first, second;
        try (var store = new CraftingOrders(file)) {
            first = store.reserve(player, recipe(0, 1), "AQ==", 6).get(5, TimeUnit.SECONDS);
            second = store.reserve(player, recipe(3600, 2), "Ag==", 6).get(5, TimeUnit.SECONDS);
            store.activate(first).get(5, TimeUnit.SECONDS); store.activate(second).get(5, TimeUnit.SECONDS);
        }
        try (var store = new CraftingOrders(file)) {
            var ready = store.job(player, first.key()); var running = store.job(player, second.key());
            assertEquals(0, ready.remaining()); assertEquals(100, ready.progress());
            assertTrue(running.remaining() > 0); assertTrue(running.progress() < 100);
            assertEquals(3_600_000, running.ready() - running.started());
            assertEquals("AQ==", ready.itemBytes()); assertEquals(1, ready.amount());
            assertEquals("Ag==", running.itemBytes()); assertEquals(2, running.amount());
            assertThrows(ExecutionException.class, () -> store.beginClaim(running).get(5, TimeUnit.SECONDS));
            store.beginClaim(ready).get(5, TimeUnit.SECONDS);
            assertThrows(ExecutionException.class, () -> store.beginClaim(ready).get(5, TimeUnit.SECONDS));
            store.complete(ready).get(5, TimeUnit.SECONDS);
            assertNull(store.job(player, ready.key()));
            assertEquals(running, store.job(player, running.key()));
        }
    }
    @Test void releasingOneReservationDoesNotRemoveAnotherOrAnActivatedOrder() throws Exception {
        UUID player = UUID.randomUUID();
        try (var store = new CraftingOrders(directory.resolve("orders.yml"))) {
            var a = store.reserve(player, recipe(60, 1), "AQ==", 6).get(5, TimeUnit.SECONDS);
            var b = store.reserve(player, recipe(60, 1), "AQ==", 6).get(5, TimeUnit.SECONDS);
            store.release(a).get(5, TimeUnit.SECONDS);
            assertNull(store.job(player, a.key())); assertEquals(b, store.job(player, b.key()));
            store.activate(b).get(5, TimeUnit.SECONDS);
            assertThrows(ExecutionException.class, () -> store.release(b).get(5, TimeUnit.SECONDS));
            assertThrows(ExecutionException.class, () -> store.activate(b).get(5, TimeUnit.SECONDS));
            assertEquals(b.state("running"), store.job(player, b.key()));
        }
    }
    @Test void failedDeliveryRestoresOnlyItsOrderAndCannotReviveCompletedClaim() throws Exception {
        UUID player = UUID.randomUUID();
        try (var store = new CraftingOrders(directory.resolve("orders.yml"))) {
            var a = store.reserve(player, recipe(0, 1), "AQ==", 6).get(5, TimeUnit.SECONDS);
            var b = store.reserve(player, recipe(0, 1), "Ag==", 6).get(5, TimeUnit.SECONDS);
            store.activate(a).get(5, TimeUnit.SECONDS); store.activate(b).get(5, TimeUnit.SECONDS);
            var running = a.state("running");
            assertThrows(ExecutionException.class, () -> store.complete(running).get(5, TimeUnit.SECONDS));
            store.beginClaim(running).get(5, TimeUnit.SECONDS);
            store.restore(running).get(5, TimeUnit.SECONDS);
            assertEquals(running, store.job(player, a.key()));
            assertEquals(b.state("running"), store.job(player, b.key()));
            store.beginClaim(running).get(5, TimeUnit.SECONDS); store.complete(running).get(5, TimeUnit.SECONDS);
            assertThrows(ExecutionException.class, () -> store.restore(running).get(5, TimeUnit.SECONDS));
            assertNull(store.job(player, a.key())); assertEquals(1, store.jobs(player).size());
        }
    }
    @Test void legacyRecipeOrderCanCoexistWithNewOrdersAndBeClaimed() throws Exception {
        UUID player = UUID.randomUUID(); Path file = directory.resolve("orders.yml");
        String oldKey = player + "/recipe/sword";
        var yaml = new YamlConfiguration(); yaml.set("schema-version", 1);
        String path = "jobs." + oldKey + ".";
        long started = System.currentTimeMillis() - 2_000;
        yaml.set(path + "recipe", "sword"); yaml.set(path + "started", started); yaml.set(path + "ready", started + 1_000);
        yaml.set(path + "item", "AQ=="); yaml.set(path + "amount", 1); yaml.set(path + "state", "running");
        yaml.save(file.toFile());
        try (var store = new CraftingOrders(file)) {
            var old = store.job(player, oldKey); assertNotNull(old);
            var newer = store.reserve(player, recipe(60, 1), "Ag==", 6).get(5, TimeUnit.SECONDS);
            store.activate(newer).get(5, TimeUnit.SECONDS); assertEquals(2, store.jobs(player).size());
            store.beginClaim(old).get(5, TimeUnit.SECONDS); store.complete(old).get(5, TimeUnit.SECONDS);
            assertEquals(List.of(newer.state("running")), store.jobs(player));
        }
    }
}
