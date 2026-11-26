package ym.starforge.gui;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import java.util.List;

/** Viewer ownership. Native item model updates without rebuilding the open inventory. */
final class ForgeChanceDisplay {
    private ForgeChanceDisplay(){}
    static int percent(double chance){
        if(!Double.isFinite(chance)||chance<=0)return 0;
        return chance>=1?100:Math.min(99,(int)Math.floor(chance*100));
    }
    static void apply(ItemStack icon,double chance){
        icon.editMeta(meta->{
            meta.setItemModel(new NamespacedKey("starforge","enhancement/chance"));
            var data=meta.getCustomModelDataComponent();data.setFloats(List.of((float)percent(chance)));meta.setCustomModelDataComponent(data);
        });
    }
}
