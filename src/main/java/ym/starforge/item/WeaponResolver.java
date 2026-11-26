package ym.starforge.item;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import ym.itemhub.api.ItemHubApi;
import ym.starforge.config.ForgeSettings;
import ym.starforge.integration.AsterStatsItems;
import java.util.*;

/** Resolve identity, eligibility and latest primary attribute from an undecorated item. */
public final class WeaponResolver {
    private static final NamespacedKey HUB=new NamespacedKey("itemhub","item_id");
    public record Result(ForgeSettings.Weapon rule,double base,EnhancementStat stat,String slot){}
    public Result resolve(ItemStack item,ForgeSettings cfg){
        if(item==null||item.getType().isAir()||item.getAmount()!=1)return null;
        var p=item.getPersistentDataContainer();String id=p.get(HUB,PersistentDataType.STRING),marker=p.get(ForgeData.RULE,PersistentDataType.STRING);
        var matching=cfg.weapons().values().stream().filter(w->matches(w,item,id,marker)).toList();
        if(matching.stream().anyMatch(ForgeSettings.Weapon::blacklisted))return null;
        var rule=matching.stream().min(Comparator.comparingInt(w->w.itemHub()!=null?0:w.marker()!=null?1:2)).orElse(ForgeSettings.defaults());
        boolean armor=NativeArmorAttributes.slot(item.getType())!=null;
        String slot=NativeArmorAttributes.slotName(item.getType());
        EnhancementStat stat=armor?EnhancementStat.DEFENSE:EnhancementStat.ATTACK;
        Map<String,Double> stats=AsterStatsItems.base(item,slot);Double attack=stats.get(stat.key());
        if(attack==null)attack=hubAttribute(item,id,slot,stat.key());
        if(armor){
            double base=attack!=null?attack:NativeArmorAttributes.defense(item);
            validate(base,stat);return new Result(rule,base,stat,slot);
        }
        double nativeAttack=NativeWeaponAttributes.attack(item);
        boolean marked=p.getOrDefault(ForgeData.WEAPON,PersistentDataType.BYTE,(byte)0)==1;
        boolean legacy=!matching.isEmpty()&&rule.legacyRegistered();
        boolean custom=(id!=null&&nativeAttack>0)||(attack!=null&&attack>0);
        if(!NativeWeaponAttributes.standard(item.getType())&&!marked&&!custom&&!legacy)return null;
        double base=attack!=null?attack:nativeAttack>0?nativeAttack:rule.base();
        validate(base,stat);return new Result(rule,base,stat,slot);
    }
    private static void validate(double base,EnhancementStat stat){if(!Double.isFinite(base)||base<0||base>1000000)throw new IllegalArgumentException("Invalid original "+stat.key()+": "+base);}
    private static boolean matches(ForgeSettings.Weapon w,ItemStack i,String id,String marker){
        return w.itemHub()!=null?w.itemHub().equals(id):w.marker()!=null?w.marker().equals(marker):w.vanilla()!=null&&w.vanilla().equals(i.getType().name());
    }
    private static Double hubAttribute(ItemStack item,String id,String slot,String stat){
        if(id==null||!Bukkit.getPluginManager().isPluginEnabled("ItemHub"))return null;
        var hub=Bukkit.getServicesManager().load(ItemHubApi.class);if(hub==null||!hub.isReady())return null;
        var quality=Bukkit.getServicesManager().load(ym.itemhub.api.ItemHubGenerationApi.class);
        var data=quality==null?null:quality.integration(item,"asterstats");
        if(data==null)data=hub.integration(id,"asterstats");if(data==null)return null;
        Object slots=data.value("slots");if(slots instanceof List<?> list&&!list.contains(slot))return null;
        Object value=data.value("stats."+stat);return value instanceof Number n?n.doubleValue():null;
    }
}
