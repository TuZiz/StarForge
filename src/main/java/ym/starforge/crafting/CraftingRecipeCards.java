package ym.starforge.crafting;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import ym.starforge.text.Texts;
import java.util.*;
import java.util.function.Supplier;

/** Viewer-thread presentation: one recipe's six cells share text and action, but retain their models. */
final class CraftingRecipeCards {
    private CraftingRecipeCards() {}

    static List<Integer> cells(YamlConfiguration menu, int anchor) {
        int size=menu.getStringList("shape").size()*9;
        if(anchor<0||anchor>=size)return List.of();
        if(anchor%9==0||anchor%9==8)return List.of(anchor);
        var cells=new ArrayList<Integer>();
        for(int row=0;row<2;row++)for(int column=-1;column<=1;column++){
            int slot=anchor+row*9+column;
            if(slot>=size)continue;
            String action=menu.getString("elements."+CraftingOrderLayout.symbol(menu,slot)+".action","none");
            if(slot==anchor||action.equals("none")||slot==anchor+9&&action.equals("recipe-caption"))cells.add(slot);
        }
        return List.copyOf(cells);
    }
    static void paint(CraftingSession session,YamlConfiguration menu,Supplier<ItemStack> blank) {
        for(int anchor:CraftingOrderLayout.slots(menu,"recipe")){
            String recipe=session.recipes.get(anchor);
            ItemStack source=session.inventory.getItem(anchor);
            if(recipe==null||source==null)continue;
            Component name=Texts.plainStyle(source.effectiveName());
            var sourceLore=source.getItemMeta().lore();
            List<Component> lore=sourceLore==null?List.of():sourceLore.stream().map(Texts::plainStyle).toList();
            for(int slot:cells(menu,anchor)){
                ItemStack target=slot==anchor||slot==anchor+9?session.inventory.getItem(slot):null;
                if(target==null)target=blank.get();
                applyText(target,name,lore);
                session.inventory.setItem(slot,target);
                session.recipes.put(slot,recipe);
            }
        }
    }
    static void applyText(ItemStack target,Component name,List<Component> lore) {
        target.editMeta(meta->{meta.displayName(Texts.plainStyle(name));meta.lore(lore.stream().map(Texts::plainStyle).toList());});
    }
}
