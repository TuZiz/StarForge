package ym.starforge.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.LinkOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

/** Async-only first install. A pre-created empty directory is also a fresh installation. */
public final class InitialResourceInstaller {
    private static final List<String> FILES = List.of(
            "config.yml", "levels.yml", "weapons.yml", "lang/zh_cn.yml", "recipes.yml", "workstations.yml");

    private InitialResourceInstaller() {}

    public static List<String> install(Path directory, Function<String, InputStream> resources) throws IOException {
        Files.createDirectories(directory);
        try (var children = Files.walk(directory)) {
            if (children.anyMatch(path -> !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))) return List.of();
        }
        // Read the complete distribution first; a missing/broken resource must not leave a partial install.
        var contents = new LinkedHashMap<String, byte[]>();
        for (String name : FILES) {
            try (InputStream input = resources.apply(name)) {
                if (input == null) throw new IOException("bundled resource missing");
                contents.put(name, input.readAllBytes());
            } catch (IOException error) {
                throw new IOException(directory.resolve(name) + ": first-install schema=1 failed: " + error.getMessage(), error);
            }
        }
        var installed = new ArrayList<String>();
        try {
            for (var entry : contents.entrySet()) {
                Path target = directory.resolve(entry.getKey());
                Files.createDirectories(target.getParent());
                Path temporary = Files.createTempFile(target.getParent(), ".starforge-initial-", ".tmp");
                try {
                    Files.write(temporary, entry.getValue());
                    Files.move(temporary, target); // No replacement: concurrent administrator writes win.
                    installed.add(entry.getKey());
                } finally { Files.deleteIfExists(temporary); }
            }
            return List.copyOf(installed);
        } catch (IOException error) {
            for (String name : installed.reversed()) {
                Path target = directory.resolve(name);
                try {
                    if (Files.isRegularFile(target) && Arrays.equals(Files.readAllBytes(target), contents.get(name)))
                        Files.delete(target);
                } catch (IOException rollback) { error.addSuppressed(rollback); }
            }
            throw new IOException(directory + ": first-install schema=1 failed; existing files preserved: " + error.getMessage(), error);
        }
    }
}
