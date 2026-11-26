package ym.starforge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ym.starforge.config.InitialResourceInstaller;
import ym.starforge.config.GuiResourceInstaller;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class InitialResourceInstallerTest {
    @TempDir Path directory;

    @Test void absentAndAlreadyEmptyDirectoriesReceiveEveryBundledConfiguration() throws Exception {
        for (Path root : new Path[]{directory.resolve("new/StarForge"), directory.resolve("empty")}) {
            if (root.getFileName().toString().equals("empty")) Files.createDirectory(root);
            assertEquals(6, InitialResourceInstaller.install(root, name -> getClass().getResourceAsStream("/" + name)).size());
            assertEquals(6, GuiResourceInstaller.installMissing(root, name -> getClass().getResourceAsStream("/" + name)).size());
            Set<String> expected;
            try (var paths = Files.walk(Path.of("src/main/resources"))) {
                expected = paths.filter(Files::isRegularFile).map(Path.of("src/main/resources")::relativize)
                        .map(p -> p.toString().replace('\\', '/')).filter(s -> s.endsWith(".yml") && !s.equals("plugin.yml")).collect(Collectors.toSet());
            }
            Set<String> actual;
            try (var paths = Files.walk(root)) {
                actual = paths.filter(Files::isRegularFile).map(root::relativize).map(p -> p.toString().replace('\\', '/')).collect(Collectors.toSet());
            }
            assertEquals(expected, actual);
            for (String name : expected) try (var stream = getClass().getResourceAsStream("/" + name)) {
                assertNotNull(stream); assertArrayEquals(stream.readAllBytes(), Files.readAllBytes(root.resolve(name)));
            }
        }
    }

    @Test void existingAndInvalidCustomConfigurationIsNeverReplacedOrCompleted() throws Exception {
        InitialResourceInstaller.install(directory, name -> getClass().getResourceAsStream("/" + name));
        var file = directory.resolve("config.yml");
        String custom = "# 自定义中文注释\r\nschema-version: 99\r\nunknown: '保留'\r\n";
        Files.writeString(file, custom); Files.delete(directory.resolve("levels.yml"));
        assertTrue(InitialResourceInstaller.install(directory, name -> { fail("Existing installation must not read defaults"); return null; }).isEmpty());
        assertEquals(custom, Files.readString(file)); assertFalse(Files.exists(directory.resolve("levels.yml")));
        assertFalse(Files.exists(directory.resolve("materials.yml")));
        assertFalse(Files.exists(directory.resolve("crafting-orders.yml")));
    }

    @Test void missingDistributionResourceLeavesNoPartialDefaultsAndCanRetry() throws Exception {
        var error = assertThrows(IOException.class, () -> InitialResourceInstaller.install(directory,
                name -> name.equals("workstations.yml") ? null : getClass().getResourceAsStream("/" + name)));
        assertTrue(error.getMessage().contains("workstations.yml"));
        try (var children = Files.list(directory)) { assertEquals(0, children.count()); }
        assertEquals(6, InitialResourceInstaller.install(directory, name -> getClass().getResourceAsStream("/" + name)).size());
    }
}
