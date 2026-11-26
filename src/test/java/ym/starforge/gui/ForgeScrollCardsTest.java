package ym.starforge.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import ym.starforge.config.ForgeSettings;
import ym.starforge.text.Texts;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ForgeScrollCardsTest {
    static void nonItalic(Component c){assertEquals(TextDecoration.State.FALSE,c.decoration(TextDecoration.ITALIC));c.children().forEach(ForgeScrollCardsTest::nonItalic);}
    @Test void bothScrollCardsShowTheirOwnCountAndExactUseWithoutLosingOriginalLore()throws Exception{
        var y=new YamlConfiguration();try(var r=new InputStreamReader(getClass().getResourceAsStream("/gui/main.yml"),StandardCharsets.UTF_8)){y.load(r);}
        var texts=new Texts(()->new ForgeSettings(15,0,0,11,"api",Map.of(),Map.of(),Map.of(),Map.of("option-enabled","<#86EFAC>已启用","option-disabled","<#94A3B8>未启用","option-waiting","<#FBBF24>等待补充"),y,null));
        for(String key:List.of("L","P"))for(int owned:new int[]{0,1,64})for(boolean enabled:new boolean[]{false,true})for(boolean ready:new boolean[]{false,true}){
            var original=Component.text("CE原说明").decorate(TextDecoration.ITALIC).append(Component.text("嵌套说明").decorate(TextDecoration.ITALIC));
            var lines=ForgeScrollCards.lore(List.of(original),y.getConfigurationSection("elements."+key),texts,Map.of(),owned,enabled,ready);
            lines.forEach(ForgeScrollCardsTest::nonItalic);
            var visible=lines.stream().map(PlainTextComponentSerializer.plainText()::serialize).toList();
            assertEquals("CE原说明嵌套说明",visible.getFirst());assertTrue(visible.contains("可用数量 "+owned+" 张 · 本次使用 "+(owned>0&&enabled&&ready?1:0)+" 张"));
            assertTrue(visible.contains(!enabled?"未启用":owned>0?"已启用":"等待补充"));
            assertTrue(visible.getLast().contains(key.equals("L")?"幸运符":"保护符"));
            assertFalse(String.join("",visible).contains("{"));
        }
    }
    @Test void percentGaugeNeverRoundsIncompleteChanceUpToGuaranteed(){
        assertEquals(0,ForgeChanceDisplay.percent(Double.NaN));assertEquals(0,ForgeChanceDisplay.percent(-.1));
        assertEquals(0,ForgeChanceDisplay.percent(0));assertEquals(80,ForgeChanceDisplay.percent(.8));
        assertEquals(85,ForgeChanceDisplay.percent(.85));assertEquals(99,ForgeChanceDisplay.percent(.999999));
        assertEquals(100,ForgeChanceDisplay.percent(1));
    }
}
