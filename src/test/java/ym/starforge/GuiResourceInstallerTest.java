package ym.starforge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ym.starforge.config.GuiResourceInstaller;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class GuiResourceInstallerTest {
    @TempDir Path directory;
    private static final List<String> MENUS = List.of("gui/main.yml", "gui/admin.yml", "gui/crafting.yml",
            "gui/crafting-detail.yml", "gui/crafting-orders.yml", "gui/crafting-types.yml");

    @Test void restoresMissingDirectoryFromActualBundledResourcesAndIsIdempotent() throws Exception {
        Path data = directory.resolve("StarForge"); Files.createDirectories(data);
        Files.writeString(data.resolve("config.yml"), "# 用户配置\n");
        assertEquals(MENUS, GuiResourceInstaller.installMissing(data, name -> getClass().getResourceAsStream("/" + name)));
        for (String name : MENUS) {
            try (var source = getClass().getResourceAsStream("/" + name)) {
                assertNotNull(source); assertArrayEquals(source.readAllBytes(), Files.readAllBytes(data.resolve(name)));
            }
        }
        assertTrue(GuiResourceInstaller.installMissing(data, name -> getClass().getResourceAsStream("/" + name)).isEmpty());
        assertEquals("# 用户配置\n", Files.readString(data.resolve("config.yml")));
    }

    @Test void preservesExistingUserFilesBytesAndOnlyRestoresDeletedGui() throws Exception {
        GuiResourceInstaller.installMissing(directory, name -> getClass().getResourceAsStream("/" + name));
        Path main = directory.resolve("gui/main.yml");
        byte[] custom = "# 用户菜单\r\nunknown-field: '保留'\r\n".getBytes(StandardCharsets.UTF_8);
        Files.write(main, custom); Files.delete(directory.resolve("gui/crafting.yml"));
        assertEquals(List.of("gui/crafting.yml"), GuiResourceInstaller.installMissing(directory,
                name -> getClass().getResourceAsStream("/" + name)));
        assertArrayEquals(custom, Files.readAllBytes(main));
        assertTrue(GuiResourceInstaller.installMissing(directory, name -> { fail("Must not read existing resources"); return null; }).isEmpty());
        assertFalse(Files.exists(directory.resolve("recipes.yml")));
    }

    @Test void missingOrBrokenBundledResourceLeavesNoPartialTarget() throws Exception {
        IOException error = assertThrows(IOException.class, () -> GuiResourceInstaller.installMissing(directory, name -> null));
        assertTrue(error.getMessage().contains("gui")); assertTrue(error.getCause().getMessage().contains("gui/main.yml"));
        assertFalse(Files.exists(directory.resolve("gui/main.yml")));
        try (var children = Files.list(directory.resolve("gui"))) { assertEquals(0, children.count()); }
        assertThrows(IOException.class, () -> GuiResourceInstaller.installMissing(directory, name -> new java.io.InputStream() {
            @Override public int read() throws IOException { throw new IOException("simulated read failure"); }
        }));
        assertFalse(Files.exists(directory.resolve("gui/main.yml")));
    }

    @Test void directoryAtMenuPathIsDiagnosedWithoutDeletingIt() throws Exception {
        Files.createDirectories(directory.resolve("gui/main.yml"));
        var error = assertThrows(IOException.class, () -> GuiResourceInstaller.installMissing(directory,
                name -> new ByteArrayInputStream(new byte[0])));
        assertTrue(error.getMessage().contains("main.yml")); assertTrue(Files.isDirectory(directory.resolve("gui/main.yml")));
    }
}
