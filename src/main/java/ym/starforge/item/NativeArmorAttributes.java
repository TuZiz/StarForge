package ym.starforge.item;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;

/** Armor classification and native armor modifiers, always bound to the actual wearing slot. */
public final class NativeArmorAttributes {
    public static final NamespacedKey BONUS=new NamespacedKey("starforge","defense");
    public static EquipmentSlot slot(Material type){
        String name=type.name();
        if(name.endsWith("_HELMET"))return EquipmentSlot.HEAD;
        if(name.endsWith("_CHESTPLATE"))return EquipmentSlot.CHEST;
        if(name.endsWith("_LEGGINGS"))return EquipmentSlot.LEGS;
        if(name.endsWith("_BOOTS"))return EquipmentSlot.FEET;
        return null;
    }
    public static String slotName(Material type){
        var slot=slot(type);
        return slot==null?"mainhand":switch(slot){case HEAD->"head";case CHEST->"chest";case LEGS->"legs";case FEET->"feet";default->throw new IllegalStateException("Unexpected armor slot "+slot);};
    }
    public static double defense(ItemStack item){
        var slot=slot(item.getType());if(slot==null)return 0;
        var meta=item.getItemMeta();
        var modifiers=meta.hasAttributeModifiers()?meta.getAttributeModifiers(slot):item.getType().getDefaultAttributeModifiers(slot);
        if(modifiers==null)return 0;
        double flat=0,scalar=0,multiplier=1;
        for(var m:modifiers.get(Attribute.ARMOR)){
            if(m.getKey().equals(BONUS))continue;
            switch(m.getOperation()){
                case ADD_NUMBER->flat+=m.getAmount();
                case ADD_SCALAR->scalar+=m.getAmount();
                case MULTIPLY_SCALAR_1->multiplier*=1+m.getAmount();
            }
        }
        double value=flat*(1+scalar)*multiplier;
        if(!Double.isFinite(value))throw new IllegalArgumentException("Non-finite original ARMOR: "+item.getType());
        return Math.max(0,value);
    }
    public static void strip(ItemMeta meta){
        var modifiers=meta.getAttributeModifiers(Attribute.ARMOR);
        if(modifiers!=null)for(var m:List.copyOf(modifiers))if(m.getKey().equals(BONUS))meta.removeAttributeModifier(Attribute.ARMOR,m);
    }
    public static void apply(ItemStack item,ItemMeta meta,double bonus){
        strip(meta);var slot=slot(item.getType());if(bonus==0||slot==null)return;
        if(!meta.hasAttributeModifiers())meta.setAttributeModifiers(item.getType().getDefaultAttributeModifiers());
        var group=switch(slot){case HEAD->EquipmentSlotGroup.HEAD;case CHEST->EquipmentSlotGroup.CHEST;case LEGS->EquipmentSlotGroup.LEGS;case FEET->EquipmentSlotGroup.FEET;default->throw new IllegalStateException("Unexpected armor slot "+slot);};
        meta.addAttributeModifier(Attribute.ARMOR,new AttributeModifier(BONUS,bonus,AttributeModifier.Operation.ADD_NUMBER,group));
    }
}
