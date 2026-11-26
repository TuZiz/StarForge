package ym.starforge.crafting;

import ym.starforge.workstation.StationAnchor;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

/** One durable writer. Ambiguous prepared/claiming records are retained for manual recovery. */
public final class CraftingOrders implements AutoCloseable {
    private final Path file;
    private volatile Snapshot committed;
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> new Thread(r, "StarForge-crafting-store"));
    private volatile boolean healthy = true;
    public CraftingOrders(Path file) throws Exception {
        this.file = file;
        committed = Snapshot.of(CraftingOrderFile.read(file));
    }

    private record Snapshot(Map<String, CraftingJob> jobs, Map<UUID, Long> stations) {
        static Snapshot of(Map<String, CraftingJob> jobs) {
            var stations = new HashMap<UUID, Long>();
            long now = System.currentTimeMillis();
            for (var job : jobs.values()) {
                if (job.station() != null && job.state().equals("running") && job.ready() > now)
                    stations.merge(job.station().furniture(), job.ready(), Math::max);
            }
            return new Snapshot(Map.copyOf(jobs), Map.copyOf(stations));
        }
    }

    /** Last completion time per physical station. Compare with wall-clock time even when no writes occur. */
    public Map<UUID, Long> activeStations() {
        return committed.stations();
    }
    public CraftingJob blocker(UUID player, CraftingRecipe recipe) {
        return recipe.group().isEmpty() ? null : committed.jobs().get(player + "/" + recipe.cooldownKey());
    }
    public CraftingJob job(UUID player, String key) {
        return key != null && key.startsWith(player + "/") ? committed.jobs().get(key) : null;
    }
    public List<CraftingJob> jobs(UUID player) {
        return committed.jobs().values().stream().filter(j -> j.key().startsWith(player + "/"))
                .sorted(Comparator.comparingLong(CraftingJob::started).thenComparing(CraftingJob::key)).toList();
    }
    public long count(UUID player) {
        return committed.jobs().keySet().stream().filter(key -> key.startsWith(player + "/")).count();
    }
    public CompletableFuture<CraftingJob> reserve(UUID player, CraftingRecipe recipe, String bytes, int limit) {
        return reserve(player, recipe, bytes, limit, null);
    }
    public CompletableFuture<CraftingJob> reserve(UUID player, CraftingRecipe recipe, String bytes, int limit, StationAnchor station) {
        return write(jobs -> {
            if (limit < 1 || limit > 54) throw new IllegalArgumentException("Invalid crafting limit " + limit);
            if (station != null && station.kind() != StationAnchor.Kind.CRAFTING)
                throw new IllegalArgumentException("Invalid crafting station " + station);
            if (count(player) >= limit) throw new CapacityException();
            String key = player + "/" + (recipe.group().isEmpty() ? "order/" + UUID.randomUUID() : recipe.cooldownKey());
            long now = System.currentTimeMillis();
            var job = new CraftingJob(key, recipe.id(), now, now + recipe.durationSeconds() * 1000, bytes, recipe.result().amount(), "prepared", station);
            if (jobs.putIfAbsent(key, job) != null) throw new IllegalStateException("Existing crafting order " + key);
            return job;
        });
    }
    public static final class CapacityException extends IllegalStateException {
        public CapacityException() { super("Player crafting order limit reached"); }
    }
    public CompletableFuture<Void> activate(CraftingJob expected) {
        return write(jobs -> {
            if (!expected.state().equals("prepared") || !jobs.replace(expected.key(), expected, expected.state("running")))
                throw new IllegalStateException("Crafting reservation changed " + expected.key());
            return null;
        });
    }
    public CompletableFuture<Void> release(CraftingJob expected) {
        return write(jobs -> {
            if (!expected.state().equals("prepared") || !jobs.remove(expected.key(), expected))
                throw new IllegalStateException("Crafting reservation changed " + expected.key());
            return null;
        });
    }
    public CompletableFuture<CraftingJob> beginClaim(CraftingJob expected) {
        return write(jobs -> {
            if (!expected.state().equals("running") || expected.remaining() > 0 || !jobs.replace(expected.key(), expected, expected.state("claiming")))
                throw new IllegalStateException("Crafting order not claimable " + expected.key());
            return expected;
        });
    }
    public CompletableFuture<Void> complete(CraftingJob job) {
        return write(jobs -> {
            if (!jobs.remove(job.key(), job.state("claiming"))) throw new IllegalStateException("Crafting claim changed " + job.key());
            return null;
        });
    }
    public CompletableFuture<Void> restore(CraftingJob job) {
        return write(jobs -> {
            if (!jobs.replace(job.key(), job.state("claiming"), job.state("running")))
                throw new IllegalStateException("Crafting claim changed " + job.key());
            return null;
        });
    }
    private <T> CompletableFuture<T> write(Function<Map<String, CraftingJob>, T> change) {
        return CompletableFuture.supplyAsync(() -> {
            if (!healthy) throw new IllegalStateException("Crafting store unavailable " + file);
            var jobs = new HashMap<>(committed.jobs());
            T value = change.apply(jobs);
            try {
                CraftingOrderFile.write(file, jobs.values());
                committed = Snapshot.of(jobs);
                return value;
            } catch (Exception ex) { healthy = false; throw new CompletionException("Crafting store save failed file=" + file + " schema=1", ex); }
        }, io);
    }
    public void close() { io.shutdown(); }
}
