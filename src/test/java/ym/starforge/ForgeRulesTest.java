package ym.starforge;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import ym.starforge.config.ForgeSettings;
import ym.starforge.transaction.ForgeRoll;
import ym.starforge.text.Texts;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;

class ForgeRulesTest {
    @Test void lowLevelsNeverDowngrade(){for(int old=0;old<10;old++){var r=ForgeRoll.resolve(old,3,0,1,false,0.5,0);assertEquals(old,r.level());assertEquals(4,r.failures());}}
    @Test void protectionAndSuccessReset(){assertEquals(10,ForgeRoll.resolve(10,4,0,1,true,0.5,0).level());var r=ForgeRoll.resolve(10,4,1,1,false,0.5,0);assertEquals(11,r.level());assertEquals(0,r.failures());}
    @Test void highFailureDropsExactlyOneAndNeverDestroys(){assertEquals(9,ForgeRoll.resolve(10,0,0,0.35,false,0.9,0.1).level());assertEquals(10,ForgeRoll.resolve(10,0,0,0.35,false,0.9,0.8).level());}
    @Test void latestBaseRecalculatesInsteadOfCompounding(){assertEquals(25,ForgeSettings.bonus(100,5,0.2));assertEquals(35,ForgeSettings.bonus(150,5,0.2));assertEquals(35,ForgeSettings.bonus(150,5,0.2));}
    @Test void pityClampsAtOneAndGrowthAccumulates(){var levels=new HashMap<Integer,ForgeSettings.Level>();for(int i=1;i<=15;i++)levels.put(i,new ForgeSettings.Level(0.3,100,"stone",1,0.2,0.05,0.35));var c=new ForgeSettings(15,0.03,0.1,11,"api",levels,Map.of(),Map.of(),Map.of(),null,null);assertEquals(0.46,c.chance(11,2,true),1e-8);assertEquals(1,c.chance(11,100,true));assertEquals(1.5,c.bonus(2,5),1e-8);assertEquals(2,c.bonus(4,5),1e-8);}
    @Test void actualRendererRemovesExplicitNestedItalicAndDoesNotParsePlaceholders(){var c=Texts.parse("<#ABCDEF><i>基础 <i>{value}</i></i>",Map.of("value","<click:run_command:'/stop'>+5</click>"));inspect(c);assertTrue(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(c).contains("<click:"));}
    private void inspect(Component c){assertEquals(TextDecoration.State.FALSE,c.decoration(TextDecoration.ITALIC));assertNull(c.clickEvent());c.children().forEach(this::inspect);}
}
