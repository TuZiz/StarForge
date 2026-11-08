package ym.starforge.crafting;

import org.bukkit.configuration.file.YamlConfiguration;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/** One durable writer. Ambiguous prepared/claiming records are retained for manual recovery. */
public final class CraftingOrders implements AutoCloseable {
    private final Path file;
    private final Map<String, CraftingJob> jobs = new ConcurrentHashMap<>();
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> new Thread(r, "StarForge-crafting-store"));
    private volatile boolean healthy = true;
    public CraftingOrders(Path file) throws Exception {
        this.file = file;
        if (!Files.exists(file)) return;
        var y = new YamlConfiguration(); y.load(file.toFile());
        if (y.getInt("schema-version") != 1) throw new IllegalArgumentException(file + ":schema-version");
        var s = Objects.requireNonNull(y.getConfigurationSection("jobs"), file + ":jobs");
        for (String key : s.getKeys(false)) {
            var v = s.getConfigurationSection(key);
            if (v == null || !Set.of("prepared", "running", "claiming").contains(v.getString("state", ""))
                    || !v.isLong("started") || !v.isLong("ready") || v.getLong("ready") < v.getLong("started")
                    || v.getInt("amount") < 1 || v.getInt("amount") > 2304 || !v.isString("item") || !v.isString("recipe"))
                throw new IllegalArgumentException(file + ":jobs." + key + " invalid record");
            Base64.getDecoder().decode(v.getString("item"));
            jobs.put(key, new CraftingJob(key, v.getString("recipe"), v.getLong("started"), v.getLong("ready"),
                    v.getString("item"), v.getInt("amount"), v.getString("state")));
        }
    }
    private String key(UUID player, CraftingRecipe recipe) { return player + "/" + recipe.cooldownKey(); }
    public CraftingJob job(UUID player, CraftingRecipe recipe) { return jobs.get(key(player, recipe)); }
    public List<CraftingJob> jobs(UUID player) {
        return jobs.values().stream().filter(j -> j.key().startsWith(player + "/")).sorted(Comparator.comparingLong(CraftingJob::started)).toList();
    }
    public CompletableFuture<Void> reserve(UUID player, CraftingRecipe recipe, String bytes) {
        return write(() -> {
            String key = key(player, recipe); long now = System.currentTimeMillis();
            var job = new CraftingJob(key, recipe.id(), now, now + recipe.durationSeconds() * 1000, bytes, recipe.result().amount(), "prepared");
            if (jobs.putIfAbsent(key, job) != null) throw new IllegalStateException("Existing crafting order " + key);
            return null;
        });
    }
    public CompletableFuture<Void> activate(UUID player, CraftingRecipe recipe) {
        return write(() -> { String key = key(player, recipe); jobs.compute(key, (k, j) -> Objects.requireNonNull(j).state("running")); return null; });
    }
    public CompletableFuture<Void> release(UUID player, CraftingRecipe recipe) {
        return write(() -> { jobs.remove(key(player, recipe)); return null; });
    }
    public CompletableFuture<CraftingJob> beginClaim(CraftingJob expected) {
        return write(() -> {
            if (!expected.state().equals("running") || expected.remaining() > 0 || !jobs.replace(expected.key(), expected, expected.state("claiming")))
                throw new IllegalStateException("Crafting order not claimable " + expected.key());
            return expected;
        });
    }
    public CompletableFuture<Void> complete(CraftingJob job) { return write(() -> { jobs.remove(job.key()); return null; }); }
    public CompletableFuture<Void> restore(CraftingJob job) { return write(() -> { jobs.put(job.key(), job.state("running")); return null; }); }
    private <T> CompletableFuture<T> write(Supplier<T> change) {
        return CompletableFuture.supplyAsync(() -> {
            if (!healthy) throw new IllegalStateException("Crafting store unavailable " + file);
            T value = change.get();
            try {
                Files.createDirectories(file.getParent());
                var y = new YamlConfiguration(); y.set("schema-version", 1); y.createSection("jobs");
                for (var job : jobs.values()) {
                    String p = "jobs." + job.key() + ".";
                    y.set(p + "recipe", job.recipe()); y.set(p + "started", job.started()); y.set(p + "ready", job.ready());
                    y.set(p + "item", job.itemBytes()); y.set(p + "amount", job.amount()); y.set(p + "state", job.state());
                }
                Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
                Files.writeString(tmp, y.saveToString());
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                return value;
            } catch (Exception ex) { healthy = false; throw new CompletionException("Crafting store save failed file=" + file + " schema=1", ex); }
        }, io);
    }
    public void close() { io.shutdown(); }
}
