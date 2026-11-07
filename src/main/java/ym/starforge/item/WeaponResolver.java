package ym.starforge.item;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import ym.itemhub.api.ItemHubApi;
import ym.starforge.config.ForgeSettings;
import ym.starforge.integration.AsterStatsItems;
import java.util.*;

/** Resolve identity, eligibility and latest original attack from a detached undecorated item. */
public final class WeaponResolver {
    private static final NamespacedKey HUB=new NamespacedKey("itemhub","item_id");
    public record Result(ForgeSettings.Weapon rule,double base){}
    public Result resolve(ItemStack item,ForgeSettings cfg){
        if(item==null||item.getType().isAir()||item.getAmount()!=1)return null;
        var p=item.getPersistentDataContainer();String id=p.get(HUB,PersistentDataType.STRING),marker=p.get(ForgeData.RULE,PersistentDataType.STRING);
        var matching=cfg.weapons().values().stream().filter(w->matches(w,item,id,marker)).toList();
        if(matching.stream().anyMatch(ForgeSettings.Weapon::blacklisted))return null;
        var rule=matching.stream().min(Comparator.comparingInt(w->w.itemHub()!=null?0:w.marker()!=null?1:2)).orElse(ForgeSettings.defaults());
        Map<String,Double> stats=AsterStatsItems.base(item);Double attack=stats.get("attack");
        if(attack==null)attack=hubAttack(id);
        double nativeAttack=NativeWeaponAttributes.attack(item);
        boolean marked=p.getOrDefault(ForgeData.WEAPON,PersistentDataType.BYTE,(byte)0)==1;
        boolean legacy=!matching.isEmpty()&&rule.legacyRegistered();
        boolean custom=(id!=null&&nativeAttack>0)||(attack!=null&&attack>0);
        if(!NativeWeaponAttributes.standard(item.getType())&&!marked&&!custom&&!legacy)return null;
        double base=attack!=null?attack:nativeAttack>0?nativeAttack:rule.base();
        if(!Double.isFinite(base)||base<0||base>1000000)throw new IllegalArgumentException("Invalid original weapon attack: "+base);
        return new Result(rule,base);
    }
    private static boolean matches(ForgeSettings.Weapon w,ItemStack i,String id,String marker){
        return w.itemHub()!=null?w.itemHub().equals(id):w.marker()!=null?w.marker().equals(marker):w.vanilla()!=null&&w.vanilla().equals(i.getType().name());
    }
    private static Double hubAttack(String id){
        if(id==null||!Bukkit.getPluginManager().isPluginEnabled("ItemHub"))return null;
        var hub=Bukkit.getServicesManager().load(ItemHubApi.class);if(hub==null||!hub.isReady())return null;
        var data=hub.integration(id,"asterstats");if(data==null)return null;
        Object slots=data.value("slots");if(slots instanceof List<?> list&&!list.contains("mainhand"))return null;
        Object value=data.value("stats.attack");return value instanceof Number n?n.doubleValue():null;
    }
}
