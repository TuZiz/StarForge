package ym.starforge;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import ym.starforge.config.*;
import ym.starforge.item.*;
import ym.starforge.text.Texts;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

class ArmorEnhancementTest {
    ForgeSettings config(){
        var c=new StoneTierTest().settings();
        return new ForgeSettings(c.max(),c.pity(),c.lucky(),c.announce(),c.backend(),c.levels(),c.materials(),c.weapons(),
            Map.of("armor-defense-suffix"," <#4ADE80>(+{bonus})","armor-bonus-line","<#94A3B8>防御力 <#4ADE80>+{bonus}"),null,null);
    }
    String visible(Component c){return PlainTextComponentSerializer.plainText().serialize(net.kyori.adventure.translation.GlobalTranslator.render(c,Locale.ROOT));}
    @Test void defenseGrowthDoesNotReuseAttackOverridesOrCompound()throws Exception{
        var c=config();var yaml=new YamlConfiguration();yaml.loadFromString("""
            schema-version: 2
            weapons:
              防具:
                vanilla: 'LEATHER_CHESTPLATE'
                growth-multiplier: 2.0
                levels:
                  2:
                    defense-flat: 0.4
                    defense-percent: 0.1
                    attack-flat: 20.0
            """);
        var rule=new WeaponRulesLoader().load(yaml,c.max(),c.levels(),c.materials()).get("防具");
        assertEquals(3.0,c.bonus(6,2,rule,true),1e-9);
        assertEquals(4.2,c.bonus(10,2,rule,true),1e-9);
        assertEquals(41.6,c.bonus(6,2,rule,false),1e-9);
        yaml.set("weapons.防具.levels.2.defense-percent",-1);
        assertThrows(IllegalArgumentException.class,()->new WeaponRulesLoader().load(yaml,c.max(),c.levels(),c.materials()));
    }
    @Test void defenseLoreIsInlineIdempotentAndPreservesHealthAndMagicDefense(){
        var input=List.of(Texts.parse("<#FFFFFF>◆ 防御力 » +6",Map.of()),Texts.parse("<#FFFFFF>最大生命 » +2",Map.of()),Texts.parse("<#FFFFFF>法术防御 » +4",Map.of()));
        var decorated=ForgeLorePresentation.decorate(input,1,.5,config(),EnhancementStat.DEFENSE);
        assertEquals(3,decorated.size());assertTrue(visible(decorated.getFirst()).endsWith("(+0.5)"));
        assertEquals(input.get(1),decorated.get(1));assertEquals(input.get(2),decorated.get(2));
        assertEquals(decorated,ForgeLorePresentation.decorate(decorated,1,.5,config(),EnhancementStat.DEFENSE));
        assertEquals(input,ForgeLorePresentation.strip(decorated));
        assertFalse(config().loreStyle().defenseLine().matcher("法术防御 » +4").matches());
        assertFalse(config().loreStyle().defenseLine().matcher("魔法防御力 » +4").matches());
    }
    @Test void noDefenseLineUsesRemovableFallbackAndZeroAddsNothing(){
        var input=List.of(Texts.parse("<#FFFFFF>最大生命 » +2",Map.of()));
        var decorated=ForgeLorePresentation.decorate(input,1,.5,config(),EnhancementStat.DEFENSE);
        assertEquals(2,decorated.size());assertEquals("防御力 +0.5",visible(decorated.get(1)));
        assertEquals(input,ForgeLorePresentation.strip(decorated));
        assertEquals(input,ForgeLorePresentation.decorate(input,0,0,config(),EnhancementStat.DEFENSE));
    }
}
