package ym.starforge.crafting;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CraftingCategoriesTest {
    @TempDir Path directory;

    @Test void changedFilesPublishOneSnapshotWithNewCategoryAndRecipe() throws Exception {
        copyDefaults();
        var old = CraftingConfig.load(directory);
        assertEquals("machines", old.categoryAt(3));
        assertNull(old.categoryAt(6));

        var types = read(CraftingScreen.CATEGORIES.file);
        types.set("shape", List.of("MMMCCCFFF", "MMMCCCFFF", "FFFFFFFFF", "FFFFFFFFF", "FFFBBBTTT"));
        types.set("elements.M.action", "machines");
        types.set("elements.M.category", null);
        types.set("elements.C.action", "consumables");
        types.set("elements.C.material", "BREAD");
        types.save(directory.resolve(CraftingScreen.CATEGORIES.file).toFile());
        var recipes = read("recipes.yml");
        recipes.set("recipes.fuel_press.category", "consumables");
        recipes.save(directory.resolve("recipes.yml").toFile());
        var gallery = read(CraftingScreen.RECIPES.file);
        gallery.set("category-titles.machines", null);
        gallery.set("machines-title", "<#EAD6AF>机器配方");
        gallery.set("category-titles.consumables", "<#EAD6AF>消耗品配方");
        gallery.save(directory.resolve(CraftingScreen.RECIPES.file).toFile());

        var next = CraftingConfig.load(directory);
        for (int slot : List.of(3, 4, 5, 12, 13, 14)) assertEquals("consumables", next.categoryAt(slot));
        assertEquals("machines", next.categoryAt(0));
        assertEquals("<#EAD6AF>消耗品配方", next.galleryTitle("consumables"));
        assertEquals("<#EAD6AF>机器配方", next.galleryTitle("machines"));
        assertEquals(List.of("fuel_press"), next.recipes().stream().filter(r -> r.category().equals("consumables")).map(CraftingRecipe::id).toList());
        assertEquals("weapons", old.categoryAt(0));
        assertEquals("machines", old.recipes().getFirst().category());
    }

    @Test void explicitEmptyCategoryWorksButMistypedShorthandAndMalformedIdsFail() throws Exception {
        copyDefaults();
        var types = read(CraftingScreen.CATEGORIES.file);
        types.set("shape", List.of("CCCFFFFFF","FFFFFFFFF","FFFFFFFFF","FFFFFFFFF","FFFBBBTTT"));
        types.set("elements.C.action", "category");
        types.set("elements.C.category", "future-category");
        types.set("elements.C.material", "BOOK");
        types.save(directory.resolve(CraftingScreen.CATEGORIES.file).toFile());
        assertEquals("future-category", CraftingConfig.load(directory).categoryAt(0));

        types.set("elements.C.category", 42);
        types.save(directory.resolve(CraftingScreen.CATEGORIES.file).toFile());
        assertTrue(assertThrows(IllegalArgumentException.class, () -> CraftingConfig.load(directory))
                .getMessage().contains("gui/crafting-types.yml:elements.C.category=42"));
        types.set("elements.C.action", "future-categry");
        types.save(directory.resolve(CraftingScreen.CATEGORIES.file).toFile());
        assertTrue(assertThrows(IllegalArgumentException.class, () -> CraftingConfig.load(directory))
                .getMessage().contains("gui/crafting-types.yml:elements.C.action=future-categry"));
    }

    private void copyDefaults() throws Exception {
        Files.createDirectories(directory.resolve("gui"));
        Files.copy(Path.of("src/main/resources/recipes.yml"), directory.resolve("recipes.yml"));
        for (var screen : CraftingScreen.values()) {
            Files.copy(Path.of("src/main/resources", screen.file), directory.resolve(screen.file));
        }
    }

    private YamlConfiguration read(String path) throws Exception {
        var yaml = new YamlConfiguration();
        yaml.load(directory.resolve(path).toFile());
        return yaml;
    }
}
