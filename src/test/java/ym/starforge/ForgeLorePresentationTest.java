package ym.starforge;

import org.junit.jupiter.api.Test;
import net.kyori.adventure.text.*;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import ym.starforge.item.ForgeLorePresentation;
import ym.starforge.config.ForgeSettings;
import ym.starforge.text.Texts;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ForgeLorePresentationTest {
    ForgeSettings config(){var c=new StoneTierTest().settings();return new ForgeSettings(c.max(),c.pity(),c.lucky(),c.announce(),c.backend(),c.levels(),c.materials(),c.weapons(),Map.of("weapon-attack-suffix"," <#4ADE80>(+{bonus})","weapon-bonus-line","<#CBD5E1>攻击力 <#4ADE80>+{bonus}"),null,null);}
    @Test void zeroLevelLeavesOriginalLoreAndNoExtraLine(){var original=List.of(Texts.parse("<#FFFFFF>⚔ 攻击力 » +2",Map.of()));assertEquals(original,ForgeLorePresentation.decorate(original,0,0,config()));}
    @Test void decorateAndStripPreserveOriginalAttackAndOtherLore(){
        var original=List.of(Texts.parse("<#FFFFFF>⚔ 攻击力 » +2",Map.of()),Texts.parse("<#FDE047>暴击几率 » +5%",Map.of()));
        var decorated=ForgeLorePresentation.decorate(original,5,1.5,config());assertEquals(2,decorated.size());assertEquals(original.get(1),decorated.get(1));
        assertTrue(visible(decorated.getFirst()).contains("(+1.5)"),()->GsonComponentSerializer.gson().serialize(decorated.getFirst())+" visible="+visible(decorated.getFirst()));
        assertEquals(original,ForgeLorePresentation.strip(decorated));assertEquals(decorated,ForgeLorePresentation.decorate(decorated,5,1.5,config()));
    }
    @Test void noAttributeLineUsesCompactFallbackAndNeverMatchesFlavorText(){
        var original=List.of(Texts.parse("<#FFFFFF>这把武器的攻击力很强",Map.of()));var decorated=ForgeLorePresentation.decorate(original,3,.9,config());
        assertEquals(2,decorated.size());assertEquals(original,ForgeLorePresentation.strip(decorated));
    }
    @Test void changingBaseLineOrBonusRetainsOnlyOneSuffix(){
        var original=List.of(Texts.parse("<#FFFFFF>攻击力 » +4",Map.of()));var output=ForgeLorePresentation.decorate(original,5,2,config());
        var changed=ForgeLorePresentation.decorate(output,5,3,config());assertEquals(original,ForgeLorePresentation.strip(changed));
        String text=visible(changed.getFirst());assertFalse(text.contains("(+2)"));assertTrue(text.contains("(+3)"));
    }
    private String visible(Component c){return PlainTextComponentSerializer.plainText().serialize(net.kyori.adventure.translation.GlobalTranslator.render(c,Locale.ROOT));}
}
