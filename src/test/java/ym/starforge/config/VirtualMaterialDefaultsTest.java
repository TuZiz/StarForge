package ym.starforge.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import ym.starforge.gui.ForgeMenus;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class VirtualMaterialDefaultsTest {
    @Test void oldDefaultPreviewUpgradesInMemoryButCustomTextAndPositionsStay()throws Exception{
        var y=bundled();var shape=y.getStringList("shape");
        y.set("elements.C.selection-lore",List.of("<#CBD5E1>攻击力 <#F8FAFC>{current} <#64748B>→ <#4ADE80>{next-attack}","保留自定义说明"));
        SettingsLoader.virtualDefaults(y,bundled());
        assertTrue(y.getStringList("elements.C.selection-lore").getFirst().contains("{stat-name}"));
        assertEquals("保留自定义说明",y.getStringList("elements.C.selection-lore").get(1));assertEquals(shape,y.getStringList("shape"));
    }
    YamlConfiguration bundled()throws Exception{
        var y=new YamlConfiguration();try(var r=new InputStreamReader(getClass().getResourceAsStream("/gui/main.yml"),StandardCharsets.UTF_8)){y.load(r);}return y;
    }
    @Test void existingCustomMenuInheritsOnlyNewFieldsWithoutWritingOrChangingLayout()throws Exception{
        var y=bundled();y.set("virtual-materials",null);y.set("elements.V",y.get("elements.C"));
        y.set("elements.C",null);y.set("elements.V.selection-lore",null);
        for(String field:List.of("repair-name","repair-lore","risk-lore"))y.set("elements.V."+field,null);
        y.set("elements.V.lore",List.of("自定义旧说明"));y.set("title","自定义标题");
        y.set("shape",y.getStringList("shape").stream().map(s->s.replace('C','V')).toList());
        y.set("unknown-user-field","保留");String before=y.saveToString();
        SettingsLoader.virtualDefaults(y,bundled());ForgeMenus.validate(y);ForgeMenus.validateInputs(y);
        assertEquals(List.of("stone","intermediate_stone","advanced_stone"),y.getStringList("virtual-materials.materials"));
        assertTrue(y.getStringList("elements.V.selection-lore").contains("{selection-status}"));
        assertTrue(y.getString("elements.V.repair-name").contains("修复裂纹"));
        assertTrue(y.getString("virtual-materials.target-chance").contains("{per-stone}"));
        assertTrue(y.getStringList("elements.V.risk-lore").getFirst().contains("{crack-chance}"));
        assertEquals("自定义标题",y.getString("title"));assertEquals(before,y.saveToString());
        assertEquals(List.of("自定义旧说明"),y.getStringList("elements.V.lore"));
    }
    @Test void userOverridesArePreservedAndBrokenLayoutsAreNotFilledIn()throws Exception{
        var y=bundled();y.set("virtual-materials.materials",List.of("advanced_stone","stone"));
        y.set("elements.C.selection-lore",List.of("自定义新说明"));SettingsLoader.virtualDefaults(y,bundled());
        assertEquals(List.of("advanced_stone","stone"),y.getStringList("virtual-materials.materials"));
        assertEquals(List.of("自定义新说明"),y.getStringList("elements.C.selection-lore"));
        y.set("elements.W",null);SettingsLoader.virtualDefaults(y,bundled());
        assertThrows(IllegalArgumentException.class,()->ForgeMenus.validate(y));
    }
    @Test void oldScrollButtonsInheritLiveCountsAndKeepCustomToggleLore()throws Exception{
        var y=bundled();y.set("elements.L.availability-lore",null);y.set("elements.P.availability-lore",null);
        y.set("elements.L.toggle-lore",List.of("保留用户提示"));String before=y.saveToString();
        SettingsLoader.virtualDefaults(y,bundled());
        for(String key:List.of("L","P"))assertTrue(y.getStringList("elements."+key+".availability-lore").stream().anyMatch(s->s.contains("{scroll-owned}")&&s.contains("{scroll-used}")));
        assertEquals(List.of("保留用户提示"),y.getStringList("elements.L.toggle-lore"));assertEquals(before,y.saveToString());
    }
}
