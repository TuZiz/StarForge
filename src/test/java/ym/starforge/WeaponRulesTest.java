package ym.starforge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.bukkit.configuration.file.YamlConfiguration;
import ym.starforge.config.*;
import ym.starforge.transaction.ForgePlan;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WeaponRulesTest {
    @TempDir Path temp;
    ForgeSettings cfg(){return new StoneTierTest().settings();}
    Map<String,ForgeSettings.Weapon> load(String content)throws Exception{
        var y=new YamlConfiguration();y.loadFromString(content);var c=cfg();return new WeaponRulesLoader().load(y,c.max(),c.levels(),c.materials());
    }
    @Test void emptyRulesAllowDefaultPolicy()throws Exception{
        assertTrue(load("schema-version: 2\nweapons:\n").isEmpty());
        assertNotNull(ForgePlan.prepare(cfg(),ForgeSettings.defaults(),0,0,false));
    }
    @Test void overridesInheritAndShareCapAndGrowth()throws Exception{
        var w=load("""
            schema-version: 2
            weapons:
              中文武器:
                marker: '测试武器'
                max-level: 5
                growth-multiplier: 2.0
                levels:
                  2:
                    money: 7.0
                    chance: 0.8
                    attack-flat: 3.0
            """).get("中文武器");
        var plan=ForgePlan.prepare(cfg(),w,1,0,false);
        assertEquals(7,plan.money());assertEquals(.8,plan.chance());assertEquals("intermediate_stone",plan.material());assertNull(ForgePlan.prepare(cfg(),w,5,0,false));
        assertEquals((.2+3+2*.1)*2,cfg().bonus(2,2,w),1e-8);
    }
    @Test void legacyFallbackOnlyBelongsToExplicitSelector()throws Exception{
        var old=load("schema-version: 1\nweapons:\n  旧武器:\n    itemhub: '星芽:短剑'\n    base-attack: 2.0\n").get("旧武器");
        assertEquals(2,old.base());assertEquals(1,old.growth());assertEquals(15,old.max(15));assertEquals(0,ForgeSettings.defaults().base());
    }
    @Test void blacklistsAndRuleValuesValidated()throws Exception{
        assertTrue(load("schema-version: 2\nweapons:\n  禁止:\n    vanilla: 'WOODEN_SWORD'\n    blacklist: true\n").get("禁止").blacklisted());
        for(String invalid:List.of("max-level: 16","max-level: 2.5","growth-multiplier: -1","blacklist: 'yes'"))
            assertThrows(IllegalArgumentException.class,()->load("schema-version: 2\nweapons:\n  无效:\n    vanilla: 'IRON_SWORD'\n    "+invalid+"\n"));
        assertThrows(IllegalArgumentException.class,()->load("schema-version: 2\nweapons:\n  无效:\n    itemhub: '测试:刀'\n    vanilla: 'PAPER'\n"));
        assertThrows(IllegalArgumentException.class,()->load("schema-version: 2\nweapons:\n  无效:\n    marker: '测试'\n    levels:\n      2:\n        material: '不存在'\n"));
    }
    @Test void multiStepStoneNeverCrossesPerWeaponCap(){
        var c=cfg();var materials=new HashMap<>(c.materials());materials.put("intermediate_stone",new ForgeSettings.MaterialSpec("FLINT",null,null,new ForgeSettings.StoneRule(2,10,2,.8)));
        var changed=new ForgeSettings(15,c.pity(),c.lucky(),11,"api",c.levels(),materials,Map.of(),Map.of(),null,null);
        var w=new ForgeSettings.Weapon(null,"IRON_SWORD",0,null,false,5,1,Map.of());
        assertNull(ForgePlan.prepare(changed,w,4,0,false));assertEquals(5,ForgePlan.prepare(changed,w,3,0,false).target());
    }
    @Test void migrationIsIdempotentAndPreservesCommentsAndUnknownFields()throws Exception{
        Path p=temp.resolve("weapons.yml");String text="\uFEFFschema-version: 1 # 保留中文\r\n# 管理员注释\r\nweapons:\r\n  星芽:\r\n    itemhub: '星芽:短剑'\r\n    base-attack: 2.0\r\n    custom-extra: '保留'\r\n";Files.writeString(p,text);
        WeaponConfigMigration.migrate(p);String actual=Files.readString(p);assertEquals(text.replace("schema-version: 1","schema-version: 2"),actual);
        assertEquals(text,Files.readString(temp.resolve("weapons.yml.v1.bak")));WeaponConfigMigration.migrate(p);assertEquals(actual,Files.readString(p));
    }
}
