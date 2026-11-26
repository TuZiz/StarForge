package ym.starforge.gui;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import ym.starforge.config.ForgeSettings;
import ym.starforge.integration.MaterialItems;
import ym.starforge.item.ForgeData;
import ym.starforge.item.ForgeLorePresentation;
import ym.starforge.item.WeaponService;
import ym.starforge.transaction.*;
import ym.starforge.text.Texts;
import java.util.*;
import java.util.function.BiConsumer;

/** All presentation runs on viewer ownership; this renderer never transfers items. */
final class ForgeMenuRenderer {
    private final Plugin plugin;private final WeaponService weapons;private final Texts texts;
    private final BiConsumer<Player,ForgeMenus.Holder> invalid;
    ForgeMenuRenderer(Plugin plugin,WeaponService weapons,Texts texts,BiConsumer<Player,ForgeMenus.Holder> invalid){
        this.plugin=plugin;this.weapons=weapons;this.texts=texts;this.invalid=invalid;
    }
    void paint(Player player,ForgeMenus.Holder holder){
        var cfg=holder.revision;
        boolean lucky=holder.lucky,protect=holder.protect;
        ItemStack item=holder.weaponSlot<0?null:holder.inventory.getItem(holder.weaponSlot);
        if(item==null)item=new ItemStack(Material.AIR);
        var rule=weapons.match(item);
        boolean hasWeapon=rule!=null;
        boolean cracked=hasWeapon&&ForgeData.cracked(item);
        if(!hasWeapon)rule=ForgeSettings.defaults();
        if(!holder.admin)ForgeStoneCards.initialize(holder);
        int stoneCount=holder.selected.values().stream().mapToInt(Integer::intValue).sum();
        var materials=new MaterialItems(()->cfg);
        var storage=player.getInventory().getStorageContents();
        holder.previewStorage=ForgeTransaction.cloneContents(storage);holder.previewWeapon=item.getType().isAir()?null:item.clone();
        var scrolls=materials.countSelected(storage,List.of("lucky","protect"));
        boolean useLucky=lucky&&scrolls.get("lucky")>0;
        boolean useProtect=protect&&scrolls.get("protect")>0;
        int current=weapons.level(item),target=Math.max(1,Math.min(rule.max(cfg.max()),current+1));
        var plan=ForgePlan.prepare(cfg,rule,current,weapons.failures(item),useLucky);
        var computed=ForgeStoneSelection.prepare(cfg,rule,current,weapons.failures(item),useLucky,
                holder.selected,materials.countSelected(storage,holder.selected.keySet()));
        var selection=hasWeapon?computed:new ForgeStoneSelection(null,computed.selected(),Map.of(),computed.owned(),computed.enough());
        boolean validStones=hasWeapon&&!cracked&&selection.ready();
        if(selection.plan()!=null)plan=selection.plan();
        if(plan!=null)target=plan.target();
        var level=rule.level(target,cfg.levels());double base=weapons.base(item);
        String materialId=plan==null?level.stone():plan.material();
        String stoneName;
        try{stoneName=net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(new ym.starforge.integration.MaterialItems(()->cfg).displayName(materialId));}
        catch(RuntimeException unavailable){stoneName=net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(texts.get("material-unavailable",Map.of()));}
        Map<String,String> values=new HashMap<>();
        values.put("level",""+current);values.put("next",""+target);
        double chance=validStones?plan.chance():0;
        values.put("chance",WeaponService.fmt(chance==1?100:Math.min(99.99,Math.floor(chance*10000)/100)));
        if(validStones){
            var odds=ForgeRoll.odds(current,plan,useProtect);
            values.put("keep-chance",ForgeLorePresentation.compact(odds.unchanged()*100));
            values.put("drop-chance",ForgeLorePresentation.compact(odds.downgrade()*100));
            values.put("crack-chance",ForgeLorePresentation.compact(odds.crack()*100));
        }
        int repairOwned=cracked?materials.countSelected(storage,List.of(ForgeRepair.MATERIAL)).get(ForgeRepair.MATERIAL):0;
        if(cracked){
            values.put("repair-count",Integer.toString(ForgeRepair.AMOUNT));
            values.put("repair-owned",Integer.toString(repairOwned));
        }
        values.put("money",WeaponService.fmt(plan==null?0:plan.money()));
        int consumed=validStones?plan.amount():0;
        values.put("materials",Integer.toString(consumed));
        values.put("stones-loaded",Integer.toString(stoneCount));
        values.put("stones-returned",Integer.toString(stoneCount-consumed));
        values.put("current",WeaponService.fmt(base+weapons.bonus(item,base,current)));
        values.put("next-attack",WeaponService.fmt(base+weapons.bonus(item,base,target)));
        values.put("next-value",values.get("next-attack"));
        values.put("stat-name",net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(texts.get("stat-"+weapons.stat(item).key(),Map.of())));
        values.put("max-level",""+rule.max(cfg.max()));
        values.put("lucky",net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(texts.get(!lucky?"option-disabled":useLucky?"option-enabled":"option-waiting",Map.of())));
        values.put("protect",net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(texts.get(!protect?"option-disabled":useProtect?"option-enabled":"option-waiting",Map.of())));
        values.put("lucky-owned",Integer.toString(scrolls.get("lucky")));values.put("protect-owned",Integer.toString(scrolls.get("protect")));
        values.put("lucky-used",Integer.toString(ForgeScrollCards.used(scrolls.get("lucky"),lucky,validStones)));
        values.put("protect-used",Integer.toString(ForgeScrollCards.used(scrolls.get("protect"),protect,validStones)));
        String required=plan==null?"":plan.requiredStone();
        boolean needsRequired=!required.isEmpty()&&holder.selected.getOrDefault(required,0)==0;
        if(needsRequired)values.put("material",net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(materials.displayName(required)));
        String selectionStatus=!hasWeapon?"insert-weapon":cracked?"crack-repair-required":current>=rule.max(cfg.max())?"max"
            :!selection.enough()?"selection-shortage":needsRequired?"required-stone-missing":selection.plan()==null?"materials-missing":"selection-ready";
        values.put("stone-name",stoneName);
        var meta=item.getItemMeta();
        values.put("weapon",meta!=null&&meta.customName()!=null?net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(meta.customName()):item.getType().name());
        var y=holder.admin?cfg.admin():cfg.gui();
        String shape=String.join("",y.getStringList("shape"));
        boolean repairIcon=false;
        for(int slot=0;slot<shape.length();slot++){
            char c=shape.charAt(slot);if(c==' ')continue;var button=y.getConfigurationSection("elements."+c);String action=button.getString("action","none");
            if(!holder.admin&&action.equals("weapon")){holder.weaponSlot=slot;continue;}
            if(!holder.admin&&action.equals("stone")){if(!holder.stoneSlots.contains(slot))holder.stoneSlots.add(slot);continue;}
            if(!holder.admin&&cracked&&Set.of("chance","lucky","protect").contains(action)){
                holder.inventory.setItem(slot,null);
                holder.actions.remove(slot);
                continue;
            }
            ItemStack icon;
            {
                icon=button.getBoolean("pixel-button",false)?ym.starforge.integration.MaterialItems.blankButton():new ItemStack(Material.valueOf(button.getString("material")));
                boolean materialIcon=action.equals("stone")||button.contains("craft-engine");
                if(materialIcon) {
                    try {icon=new ym.starforge.integration.MaterialItems(()->cfg).create(action.equals("stone")?materialId:button.getString("craft-engine"));}
                    catch(RuntimeException unavailable) {plugin.getLogger().warning("GUI material unavailable: "+button.getCurrentPath()+": "+unavailable.getMessage());}
                }
                if(!materialIcon)icon.editMeta(m->{m.customName(Texts.parse(button.getString("name",""),values));m.lore(button.getStringList(!holder.admin&&action.equals("forge")?"selection-lore":"lore").stream()
                        .flatMap(t->t.equals("{materials-breakdown}")?ForgeStoneCards.breakdown(materials,selection,ForgeStoneCards.style(cfg)).stream():java.util.stream.Stream.of(t.equals("{selection-status}")?texts.get(selectionStatus,values):Texts.parse(t,values))).toList());});
                if(!holder.admin&&action.equals("forge")){
                    if(cracked){
                        if(!repairIcon){icon=new ItemStack(Material.ANVIL);repairIcon=true;}
                        ForgeButtonLore.repair(icon,button,texts,values,repairOwned>=ForgeRepair.AMOUNT);
                    }else if(validStones){
                        ForgeButtonLore.risk(icon,button,values);
                    }
                }
                if(!holder.admin&&(action.equals("lucky")||action.equals("protect"))&&!icon.getType().isAir()){
                    boolean enabled=action.equals("lucky")?lucky:protect;
                    icon.editMeta(scrollMeta->{
                        var lore=new ArrayList<>(ForgeScrollCards.lore(scrollMeta.lore(),button,texts,values,scrolls.get(action),enabled,validStones));
                        if(action.equals("protect"))lore.add(texts.get("protect-crack",Map.of()));
                        scrollMeta.lore(lore);
                        scrollMeta.setEnchantmentGlintOverride(enabled);
                    });
                }
                if(!holder.admin&&action.equals("chance"))ForgeChanceDisplay.apply(icon,chance);
            }
            holder.inventory.setItem(slot,icon);holder.actions.put(slot,action);
        }
        if(!holder.admin)ForgeStoneCards.paint(holder,materials,selection,level,materialId,current,target,!cracked);
        if(!holder.admin&&player.getOpenInventory().getTopInventory()==holder.inventory){
            var title=ForgeMenuTitle.render(y.getString("title"),y.getString("background-image"),holder.inventory.getItem(holder.weaponSlot));
            try{
                if(!ForgeMenuTitle.refresh(player,holder,title)){invalid.accept(player,holder);}
            }catch(RuntimeException ex){invalid.accept(player,holder);throw ex;}
        }
    }
}
