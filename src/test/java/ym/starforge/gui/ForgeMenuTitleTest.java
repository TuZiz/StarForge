package ym.starforge.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ForgeMenuTitleTest {
    private String template()throws Exception{
        var y=new YamlConfiguration();y.load(Path.of("src/main/resources/gui/main.yml").toFile());return y.getString("title");
    }
    private void noItalic(Component c){assertEquals(TextDecoration.State.FALSE,c.decoration(TextDecoration.ITALIC));c.children().forEach(this::noItalic);}
    private boolean translated(Component c){return c instanceof TranslatableComponent||c.children().stream().anyMatch(this::translated);}
    @Test void realWeaponNameIsLiteralAndNestedStylesAreNotItalic()throws Exception{
        var name=Component.text("星辉长剑 +2 <red>").decorate(TextDecoration.ITALIC)
                .append(Component.text("★").decorate(TextDecoration.ITALIC));
        var title=ForgeMenuTitle.replace(template(),name);noItalic(title);
        var plain=PlainTextComponentSerializer.plainText().serialize(title);
        assertTrue(plain.contains("星锻强化"));assertTrue(plain.contains("星辉长剑 +2 <red>★"));assertFalse(plain.contains("{weapon}"));
    }
    @Test void vanillaNameRemainsClientTranslatedAndEmptySlotHasNoPlaceholder()throws Exception{
        assertTrue(translated(ForgeMenuTitle.replace(template(),Component.translatable("item.minecraft.iron_sword"))));
        var empty=ForgeMenuTitle.replace(template(),Component.empty());noItalic(empty);
        assertFalse(PlainTextComponentSerializer.plainText().serialize(empty).contains("{weapon}"));
    }
}
