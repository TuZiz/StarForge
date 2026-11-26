package ym.starforge.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import ym.starforge.text.Texts;
import java.util.*;

/** Appends live virtual usage to CE text; quantities count only real owned scrolls. */
final class ForgeScrollCards {
    private ForgeScrollCards(){}
    static int used(int owned,boolean enabled,boolean ready){return owned>0&&enabled&&ready?1:0;}
    static List<Component> lore(List<Component> original,ConfigurationSection button,Texts texts,
                                Map<String,String> values,int owned,boolean enabled,boolean ready){
        var lore=new ArrayList<Component>();if(original!=null)lore.addAll(original);lore.add(Component.empty());
        var vars=new HashMap<>(values);vars.put("scroll-owned",Integer.toString(owned));
        vars.put("scroll-used",Integer.toString(used(owned,enabled,ready)));
        String status=!enabled?"option-disabled":owned>0?"option-enabled":"option-waiting";
        for(String line:button.getStringList("availability-lore"))
            lore.add(line.equals("{scroll-status}")?texts.get(status,Map.of()):Texts.parse(line,vars));
        button.getStringList("toggle-lore").forEach(line->lore.add(Texts.parse(line,vars)));
        return lore.stream().map(Texts::plainStyle).toList();
    }
}
