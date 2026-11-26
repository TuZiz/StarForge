package ym.starforge.crafting;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ym.starforge.workstation.StationAnchor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CraftingStationOrdersTest {
    @TempDir Path directory;

    private static CraftingRecipe recipe(long seconds) {
        var item = new CraftingRecipe.Ingredient("vanilla", "IRON_SWORD", 1);
        return new CraftingRecipe("sword", "sword", List.of(item), item, 0, seconds, "");
    }

    private static StationAnchor station(UUID world, double x, float yaw) {
        return new StationAnchor(UUID.randomUUID(), world, x, 63.5, -3.25, yaw, StationAnchor.Kind.CRAFTING);
    }

    @Test void loadsAnEmptyLegacyOrderFileWithoutRewritingIt() throws Exception {
        Path file = directory.resolve("orders.yml");
        String legacy = "schema-version: 1\njobs: {}\n";
        Files.writeString(file, legacy);
        try (var store = new CraftingOrders(file)) {
            assertTrue(store.activeStations().isEmpty());
            assertTrue(store.jobs(UUID.randomUUID()).isEmpty());
        }
        assertEquals(legacy, Files.readString(file));
    }

    @Test void stationsTrackTheirLastDeadlineAcrossOwnersMovesAndRestarts() throws Exception {
        Path file = directory.resolve("orders.yml");
        UUID alice = UUID.randomUUID(), bob = UUID.randomUUID(), world = UUID.randomUUID();
        var forge = station(world, 2.25, -90);
        var moved = new StationAnchor(forge.furniture(), world, 32.5, 69, 12, 90, StationAnchor.Kind.CRAFTING);
        var other = station(world, 10.5, 180);
        Map<UUID, Long> expected;
        String commandKey;
        try (var store = new CraftingOrders(file)) {
            var shortJob = store.reserve(alice, recipe(300), "AQ==", 6, forge).get(5, TimeUnit.SECONDS);
            var longJob = store.reserve(bob, recipe(3600), "Ag==", 6, moved).get(5, TimeUnit.SECONDS);
            var otherJob = store.reserve(alice, recipe(900), "Aw==", 6, other).get(5, TimeUnit.SECONDS);
            var commandJob = store.reserve(bob, recipe(7200), "BA==", 6).get(5, TimeUnit.SECONDS);
            commandKey = commandJob.key();
            var prepared = store.activeStations();
            assertTrue(prepared.isEmpty());
            CompletableFuture.allOf(store.activate(longJob), store.activate(shortJob),
                    store.activate(otherJob), store.activate(commandJob)).get(5, TimeUnit.SECONDS);
            expected = Map.of(forge.furniture(), longJob.ready(), other.furniture(), otherJob.ready());
            assertEquals(expected, store.activeStations());
            assertTrue(prepared.isEmpty());
            assertThrows(UnsupportedOperationException.class, () -> store.activeStations().clear());
        }
        try (var store = new CraftingOrders(file)) {
            assertEquals(expected, store.activeStations());
            assertNull(store.job(bob, commandKey).station());
            assertEquals(2, store.count(alice));
            assertEquals(2, store.count(bob));
        }
    }

    @Test void readyAndClaimingOrdersRemainClaimableWithoutKeepingTheHammerActive() throws Exception {
        Path file = directory.resolve("orders.yml");
        UUID player = UUID.randomUUID();
        var forge = station(UUID.randomUUID(), -4, 90);
        String key;
        try (var store = new CraftingOrders(file)) {
            var reservation = store.reserve(player, recipe(0), "AQ==", 6, forge).get(5, TimeUnit.SECONDS);
            key = reservation.key();
            store.activate(reservation).get(5, TimeUnit.SECONDS);
            assertTrue(store.activeStations().isEmpty());
            store.beginClaim(store.job(player, key)).get(5, TimeUnit.SECONDS);
        }
        try (var store = new CraftingOrders(file)) {
            var claiming = store.job(player, key);
            assertEquals("claiming", claiming.state());
            assertEquals(forge, claiming.station());
            assertTrue(store.activeStations().isEmpty());
            store.restore(claiming).get(5, TimeUnit.SECONDS);
            assertTrue(store.activeStations().isEmpty());
            var job = store.job(player, key);
            store.beginClaim(job).get(5, TimeUnit.SECONDS);
            store.complete(job).get(5, TimeUnit.SECONDS);
            assertNull(store.job(player, key));
        }
    }

    @Test void failedActivationCannotPublishTheHammerOrLoseTheReservation() throws Exception {
        Path file = directory.resolve("orders.yml");
        UUID player = UUID.randomUUID();
        var forge = station(UUID.randomUUID(), 0, 0);
        String key;
        try (var store = new CraftingOrders(file)) {
            var job = store.reserve(player, recipe(600), "AQ==", 6, forge).get(5, TimeUnit.SECONDS);
            key = job.key();
            Files.createDirectory(file.resolveSibling("orders.yml.tmp"));
            assertThrows(ExecutionException.class, () -> store.activate(job).get(5, TimeUnit.SECONDS));
            assertTrue(store.activeStations().isEmpty());
            assertEquals(job, store.job(player, key));
        }
        try (var store = new CraftingOrders(file)) {
            assertEquals("prepared", store.job(player, key).state());
            assertTrue(store.activeStations().isEmpty());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"reserve", "beginClaim", "complete"})
    void otherFailedWritesPreserveActiveStationsAndTheExactClaimState(String operation) throws Exception {
        Path file = directory.resolve("orders.yml");
        UUID player = UUID.randomUUID(), world = UUID.randomUUID();
        var activeForge = station(world, 0, 0);
        var idleForge = station(world, 16, 90);
        CraftingJob previous;
        Map<UUID, Long> deadlines;
        try (var store = new CraftingOrders(file)) {
            var active = store.reserve(player, recipe(600), "AQ==", 6, activeForge).get(5, TimeUnit.SECONDS);
            store.activate(active).get(5, TimeUnit.SECONDS);
            var ready = store.reserve(player, recipe(0), "Ag==", 6, idleForge).get(5, TimeUnit.SECONDS);
            store.activate(ready).get(5, TimeUnit.SECONDS);
            var running = store.job(player, ready.key());
            if (operation.equals("complete")) store.beginClaim(running).get(5, TimeUnit.SECONDS);
            previous = store.job(player, ready.key());
            deadlines = Map.of(activeForge.furniture(), active.ready());
            Files.createDirectory(file.resolveSibling("orders.yml.tmp"));
            CompletableFuture<?> attempt = switch (operation) {
                case "reserve" -> store.reserve(player, recipe(600), "Aw==", 6, idleForge);
                case "beginClaim" -> store.beginClaim(running);
                case "complete" -> store.complete(running);
                default -> throw new AssertionError(operation);
            };
            assertThrows(ExecutionException.class, () -> attempt.get(5, TimeUnit.SECONDS));
            assertEquals(2, store.count(player));
            assertEquals(deadlines, store.activeStations());
            assertEquals(previous, store.job(player, previous.key()));
        }
        try (var store = new CraftingOrders(file)) {
            assertEquals(2, store.count(player));
            assertEquals(deadlines, store.activeStations());
            assertEquals(previous, store.job(player, previous.key()));
        }
    }

    @Test void invalidSavedStationCoordinateReportsItsRecordAndField() throws Exception {
        Path file = directory.resolve("orders.yml");
        String key;
        try (var store = new CraftingOrders(file)) {
            var job = store.reserve(UUID.randomUUID(), recipe(30), "AQ==", 6, station(UUID.randomUUID(), 2, 90))
                    .get(5, TimeUnit.SECONDS);
            key = job.key();
        }
        var y = YamlConfiguration.loadConfiguration(file.toFile());
        y.set("jobs." + key + ".station.x", "invalid");
        y.save(file.toFile());
        String error = assertThrows(IllegalArgumentException.class, () -> new CraftingOrders(file)).getMessage();
        assertTrue(error.contains(file + ":jobs." + key + ".station.x=invalid"));
    }
}
