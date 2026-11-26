package ym.starforge.crafting;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import ym.starforge.workstation.StationAnchor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/** Schema 1 stores an optional physical crafting station. Command and legacy orders omit the section. */
final class CraftingOrderFile {
    private CraftingOrderFile() {}

    static Map<String, CraftingJob> read(Path file) throws Exception {
        if (!Files.exists(file)) return Map.of();
        var y = new YamlConfiguration();
        y.load(file.toFile());
        if (y.getInt("schema-version") != 1) throw new IllegalArgumentException(file + ":schema-version");
        var section = Objects.requireNonNull(y.getConfigurationSection("jobs"), file + ":jobs");
        var jobs = new HashMap<String, CraftingJob>();
        for (String key : section.getKeys(false)) {
            var record = section.getConfigurationSection(key);
            String path = file + ":jobs." + key;
            if (record == null || !Set.of("prepared", "running", "claiming").contains(record.getString("state", ""))
                    || !record.isLong("started") || !record.isLong("ready") || record.getLong("ready") < record.getLong("started")
                    || record.getInt("amount") < 1 || record.getInt("amount") > 2304 || !record.isString("item") || !record.isString("recipe"))
                throw new IllegalArgumentException(path + " invalid record");
            Base64.getDecoder().decode(record.getString("item"));
            jobs.put(key, new CraftingJob(key, record.getString("recipe"), record.getLong("started"), record.getLong("ready"),
                    record.getString("item"), record.getInt("amount"), record.getString("state"), readStation(record, path)));
        }
        return jobs;
    }

    private static StationAnchor readStation(ConfigurationSection record, String path) {
        if (!record.contains("station")) return null;
        var station = record.getConfigurationSection("station");
        if (station == null) throw new IllegalArgumentException(path + ".station=" + record.get("station"));
        String prefix = path + ".station.";
        double yaw = coordinate(station, "yaw", prefix);
        if (!Float.isFinite((float) yaw)) throw new IllegalArgumentException(prefix + "yaw=" + yaw);
        return new StationAnchor(uuid(station, "furniture", prefix), uuid(station, "world", prefix),
                coordinate(station, "x", prefix), coordinate(station, "y", prefix), coordinate(station, "z", prefix),
                (float) yaw, StationAnchor.Kind.CRAFTING);
    }

    private static UUID uuid(ConfigurationSection station, String key, String path) {
        String value = station.getString(key, "");
        try { return UUID.fromString(value); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException(path + key + "=" + value, ex); }
    }

    private static double coordinate(ConfigurationSection station, String key, String path) {
        Object value = station.get(key);
        if (!(value instanceof Number number) || !Double.isFinite(number.doubleValue()))
            throw new IllegalArgumentException(path + key + "=" + value);
        return number.doubleValue();
    }

    static void write(Path file, Collection<CraftingJob> jobs) throws Exception {
        Files.createDirectories(file.getParent());
        var y = new YamlConfiguration();
        y.set("schema-version", 1);
        var section = y.createSection("jobs");
        for (var job : jobs) {
            var record = section.createSection(job.key());
            record.set("recipe", job.recipe());
            record.set("started", job.started());
            record.set("ready", job.ready());
            record.set("item", job.itemBytes());
            record.set("amount", job.amount());
            record.set("state", job.state());
            if (job.station() != null) {
                var station = record.createSection("station");
                var anchor = job.station();
                station.set("furniture", anchor.furniture().toString());
                station.set("world", anchor.world().toString());
                station.set("x", anchor.x());
                station.set("y", anchor.y());
                station.set("z", anchor.z());
                station.set("yaw", (double) anchor.yaw());
            }
        }
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, y.saveToString());
        Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
}
