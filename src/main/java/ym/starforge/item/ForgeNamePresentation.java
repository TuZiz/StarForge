package ym.starforge.item;

import net.kyori.adventure.text.*;
import ym.starforge.text.Texts;
import java.util.*;

/** Component ownership survives ItemHub name preservation and JSON round trips. */
final class ForgeNamePresentation {
    private static final String MARKER="starforge.name.level";
    private ForgeNamePresentation(){}
    static Component tag(Component suffix){
        // ItemHub's quality glyph can be the name root; ordinary suffix text must not inherit its font.
        // A font explicitly configured inside the suffix still overrides this boundary.
        return Texts.plainStyle(Component.translatable(MARKER,"").font(net.kyori.adventure.key.Key.key("minecraft:default")).append(suffix));
    }
    private static boolean own(Component c){return c instanceof TranslatableComponent t&&t.key().equals(MARKER);}
    private static Component clean(Component c){return c.children(c.children().stream().filter(child->!own(child)).map(ForgeNamePresentation::clean).toList());}
    static Component strip(Component current,Component base,Component last,Component legacySuffix){
        if(current==null)return null;
        // Exact output snapshots can restore unnamed vanilla items without inventing a custom name.
        Component result=last!=null&&current.equals(last)?base:current;
        if(result==null)return null;
        result=clean(result);
        // Old releases recorded the unmarked suffix as an exact child. Repair repeated copies,
        // including copies accidentally saved into base_name; never regex-delete '+number' text.
        if(last!=null&&legacySuffix!=null&&!own(legacySuffix)){
            var children=new ArrayList<>(result.children());
            while(!children.isEmpty()&&Texts.plainStyle(children.getLast()).equals(Texts.plainStyle(legacySuffix)))children.removeLast();
            result=result.children(children);
        }
        return result;
    }
}
