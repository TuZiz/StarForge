package ym.starforge.item;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import java.util.UUID;

public final class ForgeData {
    public static final NamespacedKey LEVEL=new NamespacedKey("starforge","level"), FAILS=new NamespacedKey("starforge","failures"), UID=new NamespacedKey("starforge","weapon_uid"), MATERIAL=new NamespacedKey("starforge","material");
    public static final NamespacedKey WEAPON=new NamespacedKey("starforge","weapon"), RULE=new NamespacedKey("starforge","weapon_rule");
    public static int level(ItemStack i){return integer(i,LEVEL);}
    public static int failures(ItemStack i){return integer(i,FAILS);}
    private static int integer(ItemStack i,NamespacedKey key){return i==null||i.getItemMeta()==null?0:Math.max(0,i.getItemMeta().getPersistentDataContainer().getOrDefault(key,PersistentDataType.INTEGER,0));}
    public static void write(ItemStack i,int level,int failures){i.editMeta(m->{var p=m.getPersistentDataContainer();p.set(LEVEL,PersistentDataType.INTEGER,level);p.set(FAILS,PersistentDataType.INTEGER,failures);if(!p.has(UID))p.set(UID,PersistentDataType.STRING,UUID.randomUUID().toString());});}
}
