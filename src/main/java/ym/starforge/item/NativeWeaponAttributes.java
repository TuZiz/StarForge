package ym.starforge.item;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

/** Original main-hand weapon modifiers only; excludes player base attack and StarForge's own modifier. */
public final class NativeWeaponAttributes {
    public static final NamespacedKey BONUS=new NamespacedKey("starforge","attack");
    public static boolean standard(org.bukkit.Material type){String name=type.name();return name.endsWith("_SWORD")||name.endsWith("_AXE");}
    public static double attack(ItemStack item){
        var meta=item.getItemMeta();if(meta==null)return 0;
        var modifiers=meta.hasAttributeModifiers()?meta.getAttributeModifiers(EquipmentSlot.HAND):item.getType().getDefaultAttributeModifiers(EquipmentSlot.HAND);
        if(modifiers==null)return 0;
        double flat=0,scalar=0,multiplier=1;
        for(var m:modifiers.get(Attribute.ATTACK_DAMAGE)){
            if(m.getKey().equals(BONUS))continue;
            switch(m.getOperation()){
                case ADD_NUMBER -> flat+=m.getAmount();
                case ADD_SCALAR -> scalar+=m.getAmount();
                case MULTIPLY_SCALAR_1 -> multiplier*=1+m.getAmount();
            }
        }
        double result=flat*(1+scalar)*multiplier;
        if(!Double.isFinite(result))throw new IllegalArgumentException("Non-finite original ATTACK_DAMAGE: "+item.getType());
        return Math.max(0,result);
    }
    public static void strip(ItemMeta meta){
        var all=meta.getAttributeModifiers(Attribute.ATTACK_DAMAGE);
        if(all!=null)for(var m:List.copyOf(all))if(m.getKey().equals(BONUS))meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE,m);
    }
    public static void apply(ItemStack item,ItemMeta meta,double bonus){
        strip(meta);if(bonus==0)return;
        // Writing a single explicit modifier would hide the material's implicit attack and speed.
        if(!meta.hasAttributeModifiers())meta.setAttributeModifiers(item.getType().getDefaultAttributeModifiers());
        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE,new AttributeModifier(BONUS,bonus,AttributeModifier.Operation.ADD_NUMBER,EquipmentSlotGroup.MAINHAND));
    }
}
