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
    public EnhancementStat stat(ItemStack item){var result=resolve(item);return result==null?EnhancementStat.ATTACK:result.stat();}
    public double bonus(ItemStack item,double base,int level){var result=resolve(item);return result==null?0:bonus(result,base,level);}
    private double bonus(WeaponResolver.Result result,double base,int level){return settings.get().bonus(base,level,result.rule(),result.stat()==EnhancementStat.DEFENSE);}
    public int level(ItemStack item){return ForgeData.level(item);}
    public int failures(ItemStack item){return ForgeData.failures(item);}
    public ItemStack markWeapon(ItemStack item,boolean enabled){
        if(item==null||item.getType().isAir()||item.getAmount()!=1)throw new IllegalArgumentException("Single non-air item required");
        var copy=item.clone();copy.editMeta(meta->{var p=meta.getPersistentDataContainer();if(enabled)p.set(ForgeData.WEAPON,PersistentDataType.BYTE,(byte)1);else{p.remove(ForgeData.WEAPON);p.remove(ForgeData.RULE);}});return copy;
    }
    public ItemStack baseItem(ItemStack item){return presentation.strip(item);}
    public Map<String,Double> contribution(ItemStack item,Map<String,Double> latestBase){
        var result=resolve(item);if(result==null||settings.get().backend().equals("vanilla"))return Map.of();
        double base=latestBase.getOrDefault(result.stat().key(),result.base());
        return Map.of(result.stat().key(),bonus(result,base,level(item)));
    }
    @Override public Map<String,Double> contribution(ItemStack item,String slot,Map<String,Double> latestBase){
        var result=resolve(item);if(result==null||!result.slot().equals(slot))return Map.of();
        return contribution(item,latestBase);
    }
    public List<Component> decorateLore(ItemStack item,List<Component> lore){
        var result=resolve(item);
        var cfg=settings.get();
        var clean=result==null?ForgeLorePresentation.strip(lore)
            :ForgeLorePresentation.decorate(lore,level(item),bonus(result,result.base(),level(item)),cfg,result.stat());
        return ForgeLorePresentation.withCrack(clean,ForgeData.cracked(item),cfg);
    }
    public Component loreLine(ItemStack item){
        double base=base(item),bonus=bonus(item,base,level(item));
        return Texts.parse(settings.get().messages().get(stat(item).messagePrefix()+"-line"),Map.of("level",""+level(item),"base",fmt(base),"bonus",fmt(bonus),"total",fmt(base+bonus)));
    }
    /** ItemHub may supply either a new base name or a preserved decorated name. */
    public ItemStack refresh(ItemStack input){
        var result=resolve(input);if(result==null)return input.getPersistentDataContainer().has(ForgeData.LEVEL)?baseItem(input):input.clone();
        if(!input.getPersistentDataContainer().has(ForgeData.LEVEL))return input.clone();
        Component suffix=level(input)>0?Texts.parse(settings.get().messages().get("level-suffix"),Map.of("level",""+level(input))):null;
        var base=baseItem(input);var lore=base.getItemMeta().lore();
        double bonus=bonus(result,result.base(),level(input));
        var cfg=settings.get();
        var decorated=ForgeLorePresentation.withCrack(
            ForgeLorePresentation.decorate(lore==null?List.of():lore,level(input),bonus,cfg,result.stat()),ForgeData.cracked(input),cfg);
        return presentation.apply(input,suffix,decorated,bonus,settings.get().backend().equals("vanilla"),result.stat());
    }
    public static String fmt(double n){return String.format(Locale.ROOT,"%.2f",n);}
}
