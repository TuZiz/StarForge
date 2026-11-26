package ym.starforge.integration;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import ym.starforge.text.Texts;
import java.util.Map;

/** Called on viewer's entity scheduler. No resource-pack IO or cached CE definitions. */
public final class MenuArt {
    private MenuArt() {}
    public static Component title(Component text,String imageId) {
        if(imageId==null||!Bukkit.getPluginManager().isPluginEnabled("CraftEngine"))return text;
        return CraftEngineMenuArt.title(text,imageId);
    }
    static Component compose(Component image,Component title) {
        // Offset glyphs belong only to the background. Unstyled dynamic names must not
        // inherit that private-use font; preserve any explicitly configured title font.
        if(title.font()==null)title=title.font(Key.key("minecraft:default"));
        return Texts.plainStyle(Component.text("\uE100").font(Key.key("starforge:offsets"))
            .append(image).append(Component.text("\uE101").font(Key.key("starforge:offsets"))).append(title));
    }
}
