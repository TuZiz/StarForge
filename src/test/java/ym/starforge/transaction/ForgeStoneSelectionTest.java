package ym.starforge.transaction;

import org.junit.jupiter.api.Test;
import ym.starforge.config.ForgeSettings;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ForgeStoneSelectionTest {
    @Test void guaranteedHighTierStillDebitsRequiredStoneAndHonorsMinimum(){
        var c=settings(4);
        c.levels().put(11,new ForgeSettings.Level(.45,1100,"advanced_stone",4,.2,.05,.2,
            .2,.05,Map.of("stone",.02,"intermediate_stone",.08,"advanced_stone",.17),"advanced_stone",0));
        var result=ForgeStoneSelection.prepare(c,ForgeSettings.defaults(),10,100,true,quantities(64,0,5),quantities(64,0,5));
        assertTrue(result.ready());assertEquals(1,result.plan().chance());
        assertEquals(quantities(3,0,1),result.consumed());
        assertFalse(ForgeStoneSelection.prepare(c,ForgeSettings.defaults(),10,100,true,quantities(64,0,0),quantities(64,0,5)).ready());
        var mixed=ForgeStoneSelection.prepare(c,ForgeSettings.defaults(),10,0,false,quantities(1,2,1),quantities(64,5,5));
        assertEquals(.35,mixed.plan().chance(),1e-10);assertEquals(quantities(1,2,1),mixed.consumed());
    }
    @Test void crackIsExclusiveAndPreviewMatchesConditionalFailureBoundaries(){
        var plan=new ForgePlan(15,"advanced_stone",5,1500,.70,.35,"advanced_stone",.05);
        var odds=ForgeRoll.odds(14,plan,false);
        assertEquals(.105,odds.downgrade(),1e-12);assertEquals(.015,odds.crack(),1e-12);assertEquals(.18,odds.unchanged(),1e-12);
        var cracked=ForgeRoll.resolveTarget(14,15,5,.7,.35,.05,false,.8,.36);
        assertTrue(cracked.cracked());assertFalse(cracked.downgraded());assertEquals(14,cracked.level());assertEquals(6,cracked.failures());
        var down=ForgeRoll.resolveTarget(14,15,5,.7,.35,.05,false,.8,.34);
        assertTrue(down.downgraded());assertFalse(down.cracked());assertEquals(13,down.level());
        var protectedRoll=ForgeRoll.resolveTarget(14,15,5,.7,.35,.05,true,.8,.01);
        assertFalse(protectedRoll.cracked());assertFalse(protectedRoll.downgraded());assertEquals(.30,ForgeRoll.odds(14,plan,true).unchanged(),1e-12);
        assertFalse(ForgeRoll.resolveTarget(13,15,5,.7,.35,.05,false,.8,.36).cracked());
        assertEquals(0,ForgeRoll.resolveTarget(14,15,5,.7,.35,.05,false,.1,.36).failures());
    }
    static ForgeSettings settings(int minimum){
        var levels=new LinkedHashMap<Integer,ForgeSettings.Level>();
        for(int i=1;i<=15;i++)levels.put(i,new ForgeSettings.Level(.1,100,"stone",minimum,.2,.05,.35));
        var materials=new LinkedHashMap<String,ForgeSettings.MaterialSpec>();
        materials.put("stone",new ForgeSettings.MaterialSpec(null,"starforge:stone",null,new ForgeSettings.StoneRule(1,15,1,.1)));
        materials.put("intermediate_stone",new ForgeSettings.MaterialSpec(null,"starforge:intermediate_stone",null,new ForgeSettings.StoneRule(1,15,1,.2)));
        materials.put("advanced_stone",new ForgeSettings.MaterialSpec(null,"starforge:advanced_stone",null,new ForgeSettings.StoneRule(1,15,1,.35)));
        return new ForgeSettings(15,.03,.1,11,"api",levels,materials,Map.of(),Map.of(),null,null);
    }
    static Map<String,Integer> quantities(int a,int b,int c){
        var map=new LinkedHashMap<String,Integer>();map.put("stone",a);map.put("intermediate_stone",b);map.put("advanced_stone",c);return map;
    }
    static ForgeStoneSelection plan(int minimum,int failures,boolean lucky,Map<String,Integer> chosen,Map<String,Integer> owned){
        return ForgeStoneSelection.prepare(settings(minimum),ForgeSettings.defaults(),0,failures,lucky,chosen,owned);
    }
    @Test void overSelectionStopsAtOneHundredAndDoesNotChangeChoice(){
        var chosen=quantities(15,0,0);var result=plan(1,0,false,chosen,quantities(64,0,0));
        assertTrue(result.ready());assertEquals(1,result.plan().chance());assertEquals(10,result.consumed().get("stone"));
        assertEquals(15,result.totalSelected());assertEquals(15,chosen.get("stone"));
        assertEquals(9,plan(1,0,true,chosen,quantities(64,0,0)).plan().amount());
        assertEquals(7,plan(1,10,false,chosen,quantities(64,0,0)).plan().amount());
    }
    @Test void mixesThreeKindsAndDebitsOnlyEarlierKindsOnceCapped(){
        var mixed=plan(1,0,false,quantities(3,1,1),quantities(10,10,10));
        assertEquals(.85,mixed.plan().chance(),1e-12);assertEquals(quantities(3,1,1),mixed.consumed());
        var capped=plan(1,0,false,quantities(9,5,8),quantities(10,10,10));
        assertEquals(quantities(9,1,0),capped.consumed());assertEquals(1,capped.plan().chance());
        var reversed=new LinkedHashMap<String,Integer>();reversed.put("advanced_stone",8);reversed.put("stone",9);
        var result=plan(1,0,false,reversed,quantities(10,10,10));
        assertEquals(3,result.consumed().get("advanced_stone"));assertEquals(0,result.consumed().get("stone"));
    }
    @Test void minimumCountStillAppliesToPityAndGuaranteedSelections(){
        assertFalse(plan(4,100,true,quantities(0,0,3),quantities(10,10,10)).ready());
        var result=plan(4,100,true,quantities(0,0,8),quantities(10,10,10));
        assertEquals(4,result.plan().amount());assertEquals(1,result.plan().chance());
        assertFalse(plan(1,100,true,quantities(0,0,0),quantities(10,10,10)).ready());
    }
    @Test void shortageRetainsChoiceAndCannotPromiseDebit(){
        var selected=quantities(15,0,0);var result=plan(1,0,false,selected,quantities(12,10,10));
        assertFalse(result.ready());assertFalse(result.enough());assertEquals(15,result.selected().get("stone"));
        assertEquals(quantities(0,0,0),result.consumed());
        assertEquals(15,ForgeStoneSelection.adjusted(15,1,12));
        assertEquals(10,ForgeStoneSelection.adjusted(15,-5,12));
    }
    @Test void quantitiesAreBoundedAndSnapshotsImmutable(){
        assertThrows(IllegalArgumentException.class,()->plan(1,0,false,quantities(-1,0,0),quantities(64,64,64)));
        assertThrows(IllegalArgumentException.class,()->plan(1,0,false,quantities(2305,0,0),quantities(2305,64,64)));
        assertThrows(IllegalArgumentException.class,()->plan(1,0,false,Map.of("lucky",1),Map.of("lucky",1)));
        var selected=quantities(1,0,0);var result=plan(1,0,false,selected,quantities(64,64,64));selected.put("stone",50);
        assertEquals(1,result.totalSelected());assertThrows(UnsupportedOperationException.class,()->result.selected().put("stone",2));
        assertEquals(0,ForgeStoneSelection.adjusted(3,-5,64));assertEquals(64,ForgeStoneSelection.adjusted(63,5,64));
        assertEquals(2304,ForgeStoneSelection.adjusted(2303,5,4096));
    }
    @Test void maxLevelOrWrongTierDoesNotProduceAnExecutablePlan(){
        assertFalse(ForgeStoneSelection.prepare(settings(1),ForgeSettings.defaults(),15,0,false,quantities(10,0,0),quantities(64,64,64)).ready());
        var cfg=settings(1);var specs=new LinkedHashMap<>(cfg.materials());
        specs.put("advanced_stone",new ForgeSettings.MaterialSpec(null,"starforge:advanced_stone",null,new ForgeSettings.StoneRule(11,15,1,.35)));
        var restricted=new ForgeSettings(15,.03,.1,11,"api",cfg.levels(),specs,Map.of(),Map.of(),null,null);
        assertFalse(ForgeStoneSelection.prepare(restricted,ForgeSettings.defaults(),0,0,false,quantities(0,0,1),quantities(64,64,64)).ready());
    }
}
