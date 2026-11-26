package ym.starforge.crafting;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CraftingMaterialLoreTest {
    private YamlConfiguration menu() throws Exception {
        var y = new YamlConfiguration(); y.load(Path.of("src/main/resources/gui/crafting.yml").toFile()); return y;
    }
    @Test void showsActualNamesRequiredOwnedAndShortageWithoutStackCap() throws Exception {
        var y = menu();
        var lines = CraftingMaterialLore.render(List.of(
                new CraftingMaterialLore.Requirement(Component.text("初级强化石").decorate(TextDecoration.ITALIC), 8, 56),
                new CraftingMaterialLore.Requirement(Component.text("中级强化石"), 128, 2)),
                y.getString("elements.R.material-enough"), y.getString("elements.R.material-missing"));
        assertEquals(2, lines.size());
        var plain = PlainTextComponentSerializer.plainText();
        String enough = plain.serialize(lines.get(0)), missing = plain.serialize(lines.get(1));
        for (String value : List.of("初级强化石", "×8", "拥有 56", "充足")) assertTrue(enough.contains(value), enough);
        for (String value : List.of("中级强化石", "×128", "拥有 2", "缺", "126")) assertTrue(missing.contains(value), missing);
        assertFalse(enough.contains("{required}")); assertFalse(missing.contains("{missing}"));
        lines.forEach(this::noItalic);
    }
    @Test void preservesVanillaTranslationAndLiteralCustomName() throws Exception {
        var y = menu();
        var lines = CraftingMaterialLore.render(List.of(
                new CraftingMaterialLore.Requirement(Component.translatable("item.minecraft.iron_ingot"), 3, 3),
                new CraftingMaterialLore.Requirement(Component.text("<red>自定义{required}"), 2, 0)),
                y.getString("elements.R.material-enough"), y.getString("elements.R.material-missing"));
        assertTrue(hasTranslation(lines.get(0)));
        assertTrue(PlainTextComponentSerializer.plainText().serialize(lines.get(1)).contains("<red>自定义{required}"));
        lines.forEach(this::noItalic);
    }
    @Test void validatesMissingMaterialTemplatesAndInsertionPoint() throws Exception {
        for (String key : List.of("material-enough", "material-missing")) {
            var y = menu(); y.set("elements.R." + key, "");
            assertTrue(assertThrows(IllegalArgumentException.class, () -> CraftingConfig.validateMenu(CraftingScreen.RECIPES, y))
                    .getMessage().contains("elements.R." + key));
        }
        var y = menu(); y.set("elements.R.lore", List.of(""));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> CraftingConfig.validateMenu(CraftingScreen.RECIPES, y))
                .getMessage().contains("lore missing {materials}"));
    }
    @Test void detailAllowsOnlyConfirmAndBackButStillRequiresBoth() throws Exception {
        var y = new YamlConfiguration(); y.load(Path.of("src/main/resources/gui/crafting-detail.yml").toFile());
        assertDoesNotThrow(() -> CraftingConfig.validateMenu(CraftingScreen.DETAIL, y));
        y.createSection("elements.X", java.util.Map.of("action", "close", "material", "BARRIER"));
        y.set("shape", List.of("FFFFFFFFC", "FIIIFFFFF", "FIIIFFFOF", "FIIIFFFFF", "BFPFCFNFX"));
        assertDoesNotThrow(() -> CraftingConfig.validateMenu(CraftingScreen.DETAIL, y));
        y.set("shape", List.of("FFFFFFFFF", "FIIIFFFFF", "FIIIFFFOF", "FIIIFFFFF", "FFFFBFFFF"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> CraftingConfig.validateMenu(CraftingScreen.DETAIL, y))
                .getMessage().contains("missing action confirm"));
    }
    private boolean hasTranslation(Component c) {
        return c instanceof TranslatableComponent t && t.key().equals("item.minecraft.iron_ingot")
                || c.children().stream().anyMatch(this::hasTranslation);
    }
    private void noItalic(Component c) {
        assertEquals(TextDecoration.State.FALSE, c.decoration(TextDecoration.ITALIC)); c.children().forEach(this::noItalic);
    }
}
