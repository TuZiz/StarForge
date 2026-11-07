package ym.starforge.integration;

import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import ym.starforge.config.ForgeSettings;
import ym.starforge.item.ForgeData;
import ym.starforge.text.Texts;
import java.util.*;
import java.util.function.Supplier;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.util.Key;

public final class MaterialItems {
    private final Supplier<ForgeSettings> settings;
    public MaterialItems(Supplier<ForgeSettings> settings){this.settings=settings;}
    public boolean matches(ItemStack item,String id){
        if(item==null||item.getType().isAir())return false;
        var spec=settings.get().materials().get(id);if(spec==null)return false;
        if(spec.binding()!=null)return spec.binding().equals(item.getPersistentDataContainer().get(ForgeData.MATERIAL,PersistentDataType.STRING));
        if(spec.ce()!=null)return Bukkit.getPluginManager().isPluginEnabled("CraftEngine")&&Key.of(spec.ce()).equals(CraftEngineItems.getCustomItemId(item));
        return item.getType().name().equals(spec.vanilla())&&!item.hasItemMeta();
    }
    public ItemStack create(String id){
        var s=Objects.requireNonNull(settings.get().materials().get(id),"Unknown material "+id);
        if(s.ce()!=null){if(!Bukkit.getPluginManager().isPluginEnabled("CraftEngine"))throw new IllegalStateException("CraftEngine unavailable");return Objects.requireNonNull(CraftEngineItems.byId(Key.of(s.ce())),"CE item not loaded: "+s.ce()).buildBukkitItem();}
        ItemStack item=new ItemStack(s.vanilla()==null?Material.PAPER:Material.valueOf(s.vanilla()));
        if(s.binding()!=null)item.editMeta(m->m.getPersistentDataContainer().set(ForgeData.MATERIAL,PersistentDataType.STRING,s.binding()));
        return item;
    }
    public Map<String,Integer> countStones(Collection<ItemStack> stacks,String preferred){
        var ids=settings.get().materials().keySet().stream()
                .filter(id->!id.equals("lucky")&&!id.equals("protect")).sorted().toList();
        Map<String,Integer> counts=new LinkedHashMap<>();
        for(ItemStack stack:stacks){
            String id=matches(stack,preferred)?preferred:ids.stream().filter(key->matches(stack,key)).findFirst().orElse("");
            counts.merge(id,stack.getAmount(),Integer::sum);
        }
        return counts;
    }
    public net.kyori.adventure.text.Component displayName(String id){
        var item=create(id);var meta=item.getItemMeta();
        var name=meta.customName();
        if(name==null&&meta.hasItemName())name=meta.itemName();
        return Texts.plainStyle(name==null?net.kyori.adventure.text.Component.translatable(item.getType().translationKey()):name);
    }
    public static ItemStack blankButton(){
        if(!Bukkit.getPluginManager().isPluginEnabled("CraftEngine"))return new ItemStack(Material.AIR);
        var definition=CraftEngineItems.byId(Key.of("starforge:gui_blank"));
        return definition==null?new ItemStack(Material.AIR):definition.buildBukkitItem();
    }
}
