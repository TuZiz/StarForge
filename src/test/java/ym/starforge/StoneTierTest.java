package ym.starforge;

import org.junit.jupiter.api.Test;
import org.bukkit.configuration.file.YamlConfiguration;
import ym.starforge.config.ForgeSettings;
import ym.starforge.transaction.ForgePlan;
import ym.starforge.transaction.ForgeRoll;
import java.util.*;
import java.io.InputStreamReader;
import static org.junit.jupiter.api.Assertions.*;

class StoneTierTest {
    ForgeSettings settings(){
        var levels=new LinkedHashMap<Integer,ForgeSettings.Level>();
        for(int i=1;i<=15;i++)levels.put(i,new ForgeSettings.Level(.4,i*100,i==1?"stone":i<=10?"intermediate_stone":"advanced_stone",1,.2,.05,.35));
        var materials=Map.of("stone",new ForgeSettings.MaterialSpec(null,"starforge:stone",null,new ForgeSettings.StoneRule(1,1,1,1.0)),
            "intermediate_stone",new ForgeSettings.MaterialSpec(null,"starforge:intermediate_stone",null,new ForgeSettings.StoneRule(2,10,1,null)),
            "advanced_stone",new ForgeSettings.MaterialSpec(null,"starforge:advanced_stone",null,new ForgeSettings.StoneRule(11,15,1,null)));
        return new ForgeSettings(15,.03,.1,11,"api",levels,materials,Map.of(),Map.of(),null,null);
    }
    @Test void beginnerGuaranteedOnlyForFirstLevel(){
        var cfg=settings();var first=ForgePlan.prepare(cfg,0,0,false);
        assertEquals("stone",first.material());assertEquals(1,first.target());assertEquals(1,first.chance());
        assertFalse(cfg.materials().get("stone").stone().allows(2));
        assertEquals("intermediate_stone",ForgePlan.prepare(cfg,1,0,false).material());
        assertEquals("advanced_stone",ForgePlan.prepare(cfg,10,0,false).material());
        assertNull(ForgePlan.prepare(cfg,15,0,false));
    }
    @Test void tierAndLevelChanceSharePityCalculation(){
        var cfg=settings();assertEquals(.56,ForgePlan.prepare(cfg,5,2,true).chance(),1e-8);
        assertEquals(1,ForgePlan.prepare(cfg,0,99,true).chance());
    }
    @Test void multiStepSuccessUsesConfiguredTargetAndNeverExceedsCap(){
        var cfg=settings();var materials=new HashMap<>(cfg.materials());
        materials.put("advanced_stone",new ForgeSettings.MaterialSpec(null,"starforge:advanced_stone",null,new ForgeSettings.StoneRule(11,15,2,.7)));
        var changed=new ForgeSettings(15,.03,.1,11,"api",cfg.levels(),materials,Map.of(),Map.of(),null,null);
        assertEquals(12,ForgePlan.prepare(changed,10,0,false).target());
        assertEquals(12,ForgeRoll.resolveTarget(10,12,0,.7,.35,false,.1,.9).level());
        assertNull(ForgePlan.prepare(changed,14,0,false));
    }
    @Test void stackedStonesIncreaseChanceAndNeverExceedOne(){
        var cfg=settings();
        var rule=ForgeSettings.defaults();
        assertEquals(.8,ForgePlan.prepare(cfg,rule,1,0,false,2).chance(),1e-8);
        assertEquals(1,ForgePlan.prepare(cfg,rule,1,0,false,64).chance());
        assertEquals(1,ForgePlan.prepare(cfg,rule,0,0,false,3).chance());
    }
    @Test void mixedStoneProbabilitiesAddAndRespectConfiguredRanges(){
        var cfg=settings();
        var specs=new HashMap<>(cfg.materials());
        specs.put("stone",new ForgeSettings.MaterialSpec(null,"starforge:stone",null,new ForgeSettings.StoneRule(1,15,1,.1)));
        specs.put("intermediate_stone",new ForgeSettings.MaterialSpec(null,"starforge:intermediate_stone",null,new ForgeSettings.StoneRule(1,15,1,.2)));
        var mixed=new ForgeSettings(15,cfg.pity(),cfg.lucky(),11,"api",cfg.levels(),specs,Map.of(),Map.of(),null,null);
        var weapon=ForgeSettings.defaults();
        var plan=ForgePlan.prepare(mixed,weapon,0,0,false);
        assertEquals(.4,plan.withStones(mixed,weapon,0,0,false,List.of(Map.entry("stone",2),Map.entry("intermediate_stone",1))).chance(),1e-8);
        var capped=plan.withStones(mixed,weapon,0,0,false,List.of(Map.entry("stone",64)));
        assertEquals(1,capped.chance());
        assertEquals(10,capped.amount());
        assertEquals(9,plan.withStones(mixed,weapon,0,0,true,List.of(Map.entry("stone",64))).amount());
        assertEquals(6,plan.withStones(mixed,weapon,0,0,false,List.of(Map.entry("intermediate_stone",4),Map.entry("stone",64))).amount());
        assertNull(plan.withStones(mixed,weapon,0,0,false,List.of(Map.entry("advanced_stone",1))));
    }
    @Test void defaultMenuSeparatesWeaponStonesAndScrollButtons()throws Exception{
        var y=new YamlConfiguration();try(var r=new InputStreamReader(getClass().getResourceAsStream("/gui/main.yml"),java.nio.charset.StandardCharsets.UTF_8)){y.load(r);}
        var shape=String.join("",y.getStringList("shape"));
        assertEquals(19,shape.indexOf('W'));
        assertEquals(38,shape.indexOf('L'));
        assertEquals(42,shape.indexOf('P'));
        assertEquals(25,shape.indexOf('C'));
        assertEquals(List.of(12,13,14,21,22,23,30,31,32),java.util.stream.IntStream.range(0,shape.length())
                .filter(i->shape.charAt(i)=='S').boxed().toList());
        assertEquals("weapon",y.getString("elements.W.action"));
        assertEquals("AIR",y.getString("elements.W.material"));
        assertFalse(y.getBoolean("elements.W.pixel-button"));
        for(String key:List.of("C","X")){assertEquals("AIR",y.getString("elements."+key+".material"));assertTrue(y.getBoolean("elements."+key+".pixel-button"));}
        assertEquals("stone",y.getString("elements.S.action"));assertEquals("lucky",y.getString("elements.L.craft-engine"));assertEquals("protect",y.getString("elements.P.craft-engine"));
        assertFalse(y.getString("title").contains("{chance}"));
    }
}
