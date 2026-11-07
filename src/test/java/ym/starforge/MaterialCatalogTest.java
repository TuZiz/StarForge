package ym.starforge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.bukkit.configuration.file.YamlConfiguration;
import ym.starforge.config.MaterialCatalogLoader;
import java.nio.file.*;
import java.io.InputStreamReader;
import static org.junit.jupiter.api.Assertions.*;

class MaterialCatalogTest {
    @TempDir Path temporary;
    private String rules="""
        schema-version: 1
        materials:
          stone:
            craft-engine: 'starforge:stone'
            enhancement:
              min-target: 1
              max-target: 1
              steps: 1
              chance: 1.0
          lucky:
            craft-engine: 'starforge:lucky'
          protect:
            craft-engine: 'starforge:protect'
        """;
    @Test void externalCatalogContainsOnlyReferencesAndRules()throws Exception{
        var file=temporary.resolve("starforge-materials.yml");Files.writeString(file,rules);
        var materials=new MaterialCatalogLoader().load(file.toFile(),15);
        assertEquals("starforge:stone",materials.get("stone").ce());assertEquals(1,materials.get("stone").stone().chance());
        assertNull(getClass().getResourceAsStream("/materials.yml"),"StarForge must not package duplicate material definitions");
    }
    @Test void duplicateDisplayMetadataAndInvalidChanceAreRejectedWithExternalPath()throws Exception{
        var file=temporary.resolve("starforge-materials.yml");
        for(String broken:new String[]{rules.replace("    craft-engine: 'starforge:stone'","    name: '重复名称'\n    craft-engine: 'starforge:stone'"),rules.replace("chance: 1.0","chance: 1.5")}){
            Files.writeString(file,broken);var error=assertThrows(IllegalArgumentException.class,()->new MaterialCatalogLoader().load(file.toFile(),15));
            assertTrue(error.getMessage().contains("starforge-materials.yml"));
        }
    }
    @Test void materialButtonsDoNotOverrideCENamesOrLore()throws Exception{
        var gui=new YamlConfiguration();try(var r=new InputStreamReader(getClass().getResourceAsStream("/gui/main.yml"),java.nio.charset.StandardCharsets.UTF_8)){gui.load(r);}
        for(String key:new String[]{"S","L","P"}){assertFalse(gui.contains("elements."+key+".name"));assertFalse(gui.contains("elements."+key+".lore"));}
    }
}
