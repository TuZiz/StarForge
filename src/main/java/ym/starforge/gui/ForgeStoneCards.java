package ym.starforge.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import ym.starforge.config.ForgeSettings;
import ym.starforge.integration.MaterialItems;
import ym.starforge.item.WeaponService;
import ym.starforge.text.Texts;
import ym.starforge.transaction.ForgeStoneSelection;
import java.util.*;

/** Virtual material cards never own real inventory inputs. Templates stay in gui/main.yml. */
final class ForgeStoneCards {
    static ConfigurationSection style(ForgeSettings cfg){return cfg.gui().getConfigurationSection("virtual-materials");}
    static List<String> ids(ForgeSettings cfg){return style(cfg).getStringList("materials");}
    static void initialize(ForgeMenus.Holder h){
        if(!h.selected.isEmpty())return;
        for(String id:ids(h.revision))h.selected.put(id,0);
    }
    static List<Float> digits(int number){
        String text=Integer.toString(number);var list=new ArrayList<Float>();int pad=(9-text.length())/2;
        for(int i=0;i<9;i++){int at=i-pad;list.add(at>=0&&at<text.length()?(float)(text.charAt(at)-'0'+1):0f);}
        return List.copyOf(list);
    }
    static String countModel(int number){
        return Integer.toString(number).length()%2==0?"enhancement/count_even":"crafting/count";
    }
    static int delta(org.bukkit.event.inventory.ClickType click){
        return switch(click){case LEFT->1;case RIGHT->-1;case SHIFT_LEFT->5;case SHIFT_RIGHT->-5;default->0;};
    }
    static void present(ItemStack icon,Component name,List<Component> lore){
        icon.editMeta(meta->{meta.customName(Texts.plainStyle(name));meta.lore(lore.stream().map(Texts::plainStyle).toList());});
    }
    static void paint(ForgeMenus.Holder h,MaterialItems materials,ForgeStoneSelection selection,ForgeSettings.Level level,
                      String preferred,int current,int target,boolean active){
        var ids=ids(h.revision);var style=style(h.revision);h.stoneActions.clear();
        for(int type=0;type<ids.size();type++){
            String id=ids.get(type);var slots=new ArrayList<Integer>();
            for(int i=type;i<h.stoneSlots.size();i+=ids.size())slots.add(h.stoneSlots.get(i));
            if(slots.isEmpty())continue;
            int chosen=h.selected.get(id),owned=selection.owned().getOrDefault(id,0),used=selection.consumed().getOrDefault(id,0);
            var rule=h.revision.materials().get(id).stone();
            boolean compatible=rule==null?id.equals(preferred):rule.allows(target)&&rule.steps()==target-current;
            double per=compatible?level.stoneChance(id,rule):0;
            var vars=new HashMap<String,String>();vars.put("selected",""+chosen);vars.put("owned",""+owned);
            vars.put("consumed",""+used);vars.put("untouched",""+(chosen-used));
            vars.put("contribution",WeaponService.fmt(chosen*per*100));
            vars.put("per-stone",ym.starforge.item.ForgeLorePresentation.compact(per*100));
            vars.put("target",Integer.toString(target));
            var icon=materials.create(id);var extra=new ArrayList<Component>();
            if(icon.getItemMeta().lore()!=null)extra.addAll(icon.getItemMeta().lore());
            if(active){
                extra.add(Texts.parse(style.getString("target-chance"),vars));
                style.getStringList(chosen>owned?"shortage-lore":"selection-lore").forEach(line->extra.add(Texts.parse(line,vars)));
            }
            var name=Texts.plainStyle(icon.effectiveName());
            present(icon,name,extra);
            icon.editMeta(meta->meta.setEnchantmentGlintOverride(active&&chosen>0&&chosen<=owned));
            icon.setAmount(1);
            for(int slot:slots){h.inventory.setItem(slot,null);if(active)h.stoneActions.put(slot,id);}
            h.inventory.setItem(slots.get(slots.size()/2),icon);
            if(active&&slots.size()>1){
                var number=MaterialItems.blankButton();
                present(number,name,extra);
                number.editMeta(meta->{
                    meta.setItemModel(new NamespacedKey("starforge",countModel(chosen)));
                    var data=meta.getCustomModelDataComponent();data.setFloats(digits(chosen));data.setFlags(List.of(chosen<=owned));
                    meta.setCustomModelDataComponent(data);
                });
                h.inventory.setItem(slots.getLast(),number);
            }
        }
    }
    static List<Component> breakdown(MaterialItems materials,ForgeStoneSelection selection,ConfigurationSection style){
        var lines=new ArrayList<Component>();
        for(var e:selection.selected().entrySet())if(e.getValue()>0){
            int used=selection.consumed().getOrDefault(e.getKey(),0);
            var line=Texts.parse(style.getString("material-line"),Map.of("selected",""+e.getValue(),"consumed",""+used));
            line=line.replaceText(b->b.matchLiteral("{material}").replacement(materials.displayName(e.getKey())));
            lines.add(Texts.plainStyle(line));
        }
        return lines;
    }
}
