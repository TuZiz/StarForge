package ym.starforge.integration;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;
import ym.starforge.text.Texts;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MenuArtTest {
    @Test void backgroundAndTextKeepIndependentFontsAndNonitalicStyle(){
        var image=Component.text("\uE999").font(Key.key("minecraft:default"));
        var title=Texts.parse("<font:starforge:ui><#E0D8FF>星锻 +{level}</font>",Map.of("level","5"));
        var combined=MenuArt.compose(image,title);
        assertEquals(Key.key("starforge:offsets"),combined.font());
        assertEquals("\uE100",((TextComponent)combined).content());
        assertEquals(Key.key("minecraft:default"),combined.children().get(0).font());
        assertEquals("\uE101",((TextComponent)combined.children().get(1)).content());
        assertEquals(Key.key("starforge:ui"),combined.children().get(2).font());
        check(combined);
    }
    private void check(Component component){assertEquals(TextDecoration.State.FALSE,component.decoration(TextDecoration.ITALIC));component.children().forEach(this::check);}
}
