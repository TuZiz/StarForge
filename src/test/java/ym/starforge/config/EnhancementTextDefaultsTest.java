package ym.starforge.config;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import ym.starforge.text.Texts;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EnhancementTextDefaultsTest {
    @Test void oldShippedSuffixLosesStarWhileCustomTemplateIsPreserved() throws Exception {
        var language=new YamlConfiguration();
        try(var stream=getClass().getResourceAsStream("/lang/zh_cn.yml")){
            language.load(new InputStreamReader(stream,StandardCharsets.UTF_8));
        }
        var bundled=Map.of("level-suffix",language.getString("level-suffix"));
        var loaded=new HashMap<>(Map.of("level-suffix"," <#FDE047>✦ +{level}"));
        EnhancementTextDefaults.upgrade(loaded,bundled);
        var suffix=Texts.parse(loaded.get("level-suffix"),Map.of("level","2"));
        assertEquals(" +2",PlainTextComponentSerializer.plainText().serialize(suffix));

        String custom=" <#8DE4FF>[+{level}]";
        loaded.put("level-suffix",custom);
        EnhancementTextDefaults.upgrade(loaded,bundled);
        assertEquals(custom,loaded.get("level-suffix"));
    }
}
