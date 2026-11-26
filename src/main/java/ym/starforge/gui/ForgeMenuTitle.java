package ym.starforge.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ym.starforge.integration.MenuArt;
import ym.starforge.text.Texts;
import java.util.Map;

/** Viewer scheduler only. Public Bukkit API requires rebuilding the inventory for an Adventure title. */
final class ForgeMenuTitle {
    private ForgeMenuTitle(){}
    static Component render(String template,String background,ItemStack weapon){
        Component name=weapon==null||weapon.getType().isAir()?Component.empty():Texts.plainStyle(weapon.effectiveName());
        return MenuArt.title(replace(template,name),background);
    }
    static Component replace(String template,Component name){
        return Texts.plainStyle(Texts.parse(template,Map.of("level","0"))
                .replaceText(TextReplacementConfig.builder().matchLiteral("{weapon}").replacement(Texts.plainStyle(name)).build()));
    }
    static boolean refresh(Player player,ForgeMenus.Holder holder,Component title){
        if(title.equals(holder.title))return true;
        Inventory old=holder.inventory;
        Inventory replacement=Bukkit.createInventory(holder,old.getSize(),title);
        var contents=old.getContents();
        holder.transitioning=true;
        try{
            old.clear();
            replacement.setContents(contents);
            holder.inventory=replacement;
            player.openInventory(replacement);
            holder.title=title;
            return player.getOpenInventory().getTopInventory()==replacement;
        }catch(RuntimeException ex){
            replacement.clear();old.setContents(contents);holder.inventory=old;
            throw ex;
        }finally{holder.transitioning=false;}
    }
}
