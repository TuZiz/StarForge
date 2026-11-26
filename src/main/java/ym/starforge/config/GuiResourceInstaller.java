package ym.starforge.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Async-only installation: restore missing GUI resources, never replace user files. */
public final class GuiResourceInstaller {
    private static final List<String> MENUS = List.of("gui/main.yml", "gui/admin.yml", "gui/crafting.yml",
            "gui/crafting-detail.yml", "gui/crafting-orders.yml", "gui/crafting-types.yml");

    private GuiResourceInstaller() {}

    public static List<String> installMissing(Path directory, Function<String, InputStream> resources) throws IOException {
        var installed = new ArrayList<String>();
        for (String resource : MENUS) {
            Path target = directory.resolve(resource);
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                if (!Files.isRegularFile(target)) throw new IOException(target + ": GUI path exists but is not a regular file");
                continue;
            }
            Files.createDirectories(target.getParent());
            Path temporary = Files.createTempFile(target.getParent(), ".starforge-gui-", ".tmp");
            try {
                try (InputStream input = resources.apply(resource)) {
                    if (input == null) throw new IOException(target + ": bundled GUI resource missing: " + resource);
                    Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
                }
                try {
                    // No REPLACE_EXISTING: an existing or concurrently created user file wins.
                    Files.move(temporary, target);
                    installed.add(resource);
                } catch (FileAlreadyExistsException ex) {
                    if (!Files.isRegularFile(target)) throw new IOException(target + ": GUI path is not a regular file", ex);
                }
            } catch (IOException ex) {
                throw new IOException(target + ": unable to restore missing GUI resource " + resource, ex);
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
        return List.copyOf(installed);
    }
}
