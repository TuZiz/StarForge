package ym.starforge.workstation;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class StationAnimationSettingsTest {
    YamlConfiguration yaml() throws Exception {
        var y=new YamlConfiguration();y.load(Path.of("src/main/resources/workstations.yml").toFile());return y;
    }
    @Test void defaultFitsThirtyTickReopenAndStaysBelowNameplate() throws Exception {
        var cfg=StationAnimationSettings.parse(yaml());
        assertTrue(cfg.enabled());assertEquals(30,cfg.ticks());
        for (var kind : StationAnchor.Kind.values()) {
            var pose = StationMotion.frame(kind, .84, cfg.rise());
            assertTrue(pose.getTranslation().y + pose.getScale().y / 2 < 2.6);
        }
    }
    @Test void invalidSettingsNamePathAndNeverSilentlyDefault() throws Exception {
        for(var entry:java.util.Map.of("schema-version",2,"animation.duration-ticks",30.5,
                "animation.rise-height",Double.NaN,"animation.max-distance",100,"animation.enabled","true").entrySet()) {
            var y=yaml();y.set(entry.getKey(),entry.getValue());
            var ex=assertThrows(IllegalArgumentException.class,()->StationAnimationSettings.parse(y));
            assertTrue(ex.getMessage().contains("workstations.yml:"+entry.getKey()));
        }
    }
}
