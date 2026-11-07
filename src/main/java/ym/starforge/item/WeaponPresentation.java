package ym.starforge.item;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import ym.starforge.text.Texts;
import java.util.*;

/** Removes only recorded output, preserving later foreign edits instead of replaying an old snapshot. */
public final class WeaponPresentation {
    private static final NamespacedKey BASE_NAME=key("base_name"),BASE_LORE=key("base_lore"),LAST_NAME=key("last_name"),SUFFIX=key("name_suffix"),LAST_LORE=key("last_lore");
    private static final GsonComponentSerializer JSON=GsonComponentSerializer.gson();
    private static NamespacedKey key(String name){return new NamespacedKey("starforge",name);}
    public ItemStack strip(ItemStack input){
        return strip(input,false);
    }
    private ItemStack strip(ItemStack input,boolean freshName){
        var item=input.clone();var meta=item.getItemMeta();if(meta==null)return item;var p=meta.getPersistentDataContainer();
        Component current=meta.customName();String name=p.get(LAST_NAME,PersistentDataType.STRING),base=p.get(BASE_NAME,PersistentDataType.STRING),suffix=p.get(SUFFIX,PersistentDataType.STRING);
        if(freshName){/* ItemHub has just supplied authoritative configuration-managed name. */}
        else if(current!=null&&name!=null&&current.equals(JSON.deserialize(name)))meta.customName(base==null?null:JSON.deserialize(base));
        else if(current!=null&&suffix!=null){
            Component own=JSON.deserialize(suffix);var children=new ArrayList<>(current.children());
            if(children.remove(own))meta.customName(current.children(children));
        }else if(current!=null&&base!=null&&name==null){
            var original=JSON.deserialize(base);var children=current.children();
            if(children.size()>original.children().size()&&current.children(children.subList(0,original.children().size())).equals(original))meta.customName(original);
        }
        String old=p.get(LAST_LORE,PersistentDataType.STRING);
        if(old!=null&&meta.lore()!=null){Component own=JSON.deserialize(old);var lore=new ArrayList<>(meta.lore());lore.removeIf(line->Texts.plainStyle(line).equals(Texts.plainStyle(own)));meta.lore(lore);}
        if(meta.lore()!=null)meta.lore(ForgeLorePresentation.strip(meta.lore()));
        NativeWeaponAttributes.strip(meta);item.setItemMeta(meta);return item;
    }
    public ItemStack apply(ItemStack original,Component suffix,List<Component> decoratedLore,double bonus,boolean vanilla,boolean freshName){
        var item=strip(original,freshName);var meta=item.getItemMeta();var p=meta.getPersistentDataContainer();
        Component base=meta.customName();if(base==null)p.remove(BASE_NAME);else p.set(BASE_NAME,PersistentDataType.STRING,JSON.serialize(base));
        if(suffix!=null){
            Component root=base==null?Component.translatable(item.getType().translationKey()):base;
            Component name=Texts.plainStyle(root.append(suffix));meta.customName(name);
            p.set(SUFFIX,PersistentDataType.STRING,JSON.serialize(Texts.plainStyle(suffix)));p.set(LAST_NAME,PersistentDataType.STRING,JSON.serialize(name));
        }else{p.remove(LAST_NAME);p.remove(SUFFIX);}
        p.remove(LAST_LORE);p.remove(BASE_LORE);
        if(!decoratedLore.isEmpty()||meta.hasLore())meta.lore(decoratedLore.stream().map(Texts::plainStyle).toList());
        if(vanilla)NativeWeaponAttributes.apply(item,meta,bonus);
        item.setItemMeta(meta);return item;
    }
}
