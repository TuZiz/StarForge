package ym.starforge.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class LevelBalanceTest {
    @Test void existingConfigsInheritBandsWithoutLosingCustomFailureChance() throws Exception {
        var bundled=new YamlConfiguration();
        try(var stream=getClass().getResourceAsStream("/levels.yml")){
            bundled.load(new InputStreamReader(stream,StandardCharsets.UTF_8));
        }
        var stone=new ForgeSettings.MaterialSpec("FLINT",null,null,null);
        var catalog=Map.of("stone",stone,"intermediate_stone",stone,"advanced_stone",stone);
        var live=new YamlConfiguration();
        assertEquals(.25,LevelBalance.read(live,bundled.getConfigurationSection("levels.1"),catalog,1,0).chances().get("stone"));
        assertEquals(.05,LevelBalance.read(live,bundled.getConfigurationSection("levels.10"),catalog,10,0).chances().get("stone"));
        var eleven=LevelBalance.read(live,bundled.getConfigurationSection("levels.11"),catalog,11,.35);
        assertEquals(.20,eleven.downgrade());assertEquals("advanced_stone",eleven.requiredStone());
        assertEquals(.29,LevelBalance.read(live,bundled.getConfigurationSection("levels.11"),catalog,11,.29).downgrade());
        assertEquals(.05,LevelBalance.read(live,bundled.getConfigurationSection("levels.15"),catalog,15,.35).crack());
        assertTrue(live.getKeys(false).isEmpty());
        live.set("stone-chances.stone",1.2);
        assertTrue(assertThrows(IllegalArgumentException.class,()->LevelBalance.read(live,null,catalog,11,.35)).getMessage().contains("levels.11.stone-chances.stone"));
    }
}
