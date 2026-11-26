package ym.starforge.gui;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import ym.starforge.text.Texts;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Presents the current action on existing forge button slots. */
final class ForgeButtonLore {
    private ForgeButtonLore(){}

    static void repair(ItemStack icon,ConfigurationSection button,Texts texts,Map<String,String> values,boolean ready){
        icon.editMeta(meta->{
            meta.customName(Texts.parse(button.getString("repair-name"),values));
            var lore=new ArrayList<>(button.getStringList("repair-lore").stream().map(line->Texts.parse(line,values)).toList());
            lore.add(texts.get(ready?"crack-repair-ready":"crack-repair-shortage",values));
            meta.lore(lore);
        });
    }

    static void risk(ItemStack icon,ConfigurationSection button,Map<String,String> values){
        icon.editMeta(meta->{
            var lore=new ArrayList<>(meta.lore()==null?List.of():meta.lore());
            var risk=button.getStringList("risk-lore").stream().map(line->Texts.parse(line,values)).toList();
            int insert=lore.size();
            if(insert>1&&lore.get(insert-2).equals(Texts.parse("",Map.of())))insert-=2;
            lore.addAll(insert,risk);
            meta.lore(lore);
        });
    }
}
