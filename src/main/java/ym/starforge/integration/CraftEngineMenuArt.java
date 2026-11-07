package ym.starforge.integration;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.api.CraftEngineImages;
import net.momirealms.craftengine.core.util.Key;
import ym.starforge.text.Texts;
import java.util.Map;

/** Class is reached only when CE is enabled; it does not bind optional CE classes on startup. */
final class CraftEngineMenuArt {
    static Component title(Component title,String id) {
        var image=CraftEngineImages.byId(Key.of(id));
        if(image==null)return title;
        var glyph=Texts.parse("<#FFFFFF>"+image.miniMessageAt(0,0),Map.of());
        return MenuArt.compose(glyph,title);
    }
}
