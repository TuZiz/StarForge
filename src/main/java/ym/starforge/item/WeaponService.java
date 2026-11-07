package ym.starforge.item;

import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;
import ym.starforge.api.StarForgeApi;
import ym.starforge.config.ForgeSettings;
import ym.starforge.text.Texts;
import java.util.*;
import java.util.function.Supplier;

/** Pure detached-item operations. Callers own any live inventory reads and writes. */
public final class WeaponService implements StarForgeApi {
    private final Supplier<ForgeSettings> settings;
    private final WeaponResolver resolver=new WeaponResolver();
    private final WeaponPresentation presentation=new WeaponPresentation();
    public WeaponService(Supplier<ForgeSettings> settings){this.settings=settings;}
    private WeaponResolver.Result resolve(ItemStack item){return item==null?null:resolver.resolve(baseItem(item),settings.get());}
    public ForgeSettings.Weapon match(ItemStack item){var result=resolve(item);return result==null?null:result.rule();}
    public boolean canEnhance(ItemStack item){return match(item)!=null;}
    public int maxLevel(ItemStack item){var w=match(item);return w==null?0:w.max(settings.get().max());}
    public double base(ItemStack item){var result=resolve(item);return result==null?0:result.base();}
    public double bonus(ItemStack item,double base,int level){var w=match(item);return w==null?0:settings.get().bonus(base,level,w);}
    public int level(ItemStack item){return ForgeData.level(item);}
    public int failures(ItemStack item){return ForgeData.failures(item);}
    public ItemStack markWeapon(ItemStack item,boolean enabled){
        if(item==null||item.getType().isAir()||item.getAmount()!=1)throw new IllegalArgumentException("Single non-air item required");
        var copy=item.clone();copy.editMeta(meta->{var p=meta.getPersistentDataContainer();if(enabled)p.set(ForgeData.WEAPON,PersistentDataType.BYTE,(byte)1);else{p.remove(ForgeData.WEAPON);p.remove(ForgeData.RULE);}});return copy;
    }
    public ItemStack baseItem(ItemStack item){return presentation.strip(item);}
    public Map<String,Double> contribution(ItemStack item,Map<String,Double> latestBase){
        var result=resolve(item);if(result==null||settings.get().backend().equals("vanilla"))return Map.of();
        double base=latestBase.getOrDefault("attack",result.base());
        return Map.of("attack",settings.get().bonus(base,level(item),result.rule()));
    }
    public ItemStack refresh(ItemStack input){return render(input,false);}
    public List<Component> decorateLore(ItemStack item,List<Component> lore){
        var result=resolve(item);if(result==null)return ForgeLorePresentation.strip(lore);
        return ForgeLorePresentation.decorate(lore,level(item),settings.get().bonus(result.base(),level(item),result.rule()),settings.get());
    }
    public Component loreLine(ItemStack item){
        double base=base(item),bonus=bonus(item,base,level(item));
        return Texts.parse(settings.get().messages().get("weapon-line"),Map.of("level",""+level(item),"base",fmt(base),"bonus",fmt(bonus),"total",fmt(base+bonus)));
    }
    /** freshBase means ItemHub just supplied new configuration-managed metadata. */
    public ItemStack render(ItemStack input,boolean freshBase){
        var result=resolve(input);if(result==null)return input.getPersistentDataContainer().has(ForgeData.LEVEL)?baseItem(input):input.clone();
        if(!input.getPersistentDataContainer().has(ForgeData.LEVEL))return input.clone();
        Component suffix=level(input)>0?Texts.parse(settings.get().messages().get("level-suffix"),Map.of("level",""+level(input))):null;
        var base=baseItem(input);var lore=base.getItemMeta().lore();
        var decorated=ForgeLorePresentation.decorate(lore==null?List.of():lore,level(input),settings.get().bonus(result.base(),level(input),result.rule()),settings.get());
        return presentation.apply(input,suffix,decorated,settings.get().bonus(result.base(),level(input),result.rule()),settings.get().backend().equals("vanilla"),freshBase);
    }
    public static String fmt(double n){return String.format(Locale.ROOT,"%.2f",n);}
}
