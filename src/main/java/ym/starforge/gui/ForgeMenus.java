package ym.starforge.gui;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import ym.starforge.config.ForgeSettings;
import ym.starforge.item.WeaponService;
import ym.starforge.transaction.ForgeTransaction;
import ym.starforge.transaction.ForgePlan;
import ym.starforge.integration.MaterialItems;
import ym.starforge.text.Texts;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class ForgeMenus implements Listener,AutoCloseable {
    private final Plugin plugin;private final Supplier<ForgeSettings> settings;private final WeaponService weapons;private final ForgeTransaction transaction;private final Texts texts;
    private final Map<UUID,Player> viewers=new ConcurrentHashMap<>();
    public ForgeMenus(Plugin p,Supplier<ForgeSettings> s,WeaponService w,ForgeTransaction t,Texts x){plugin=p;settings=s;weapons=w;transaction=t;texts=x;}
    public static final class Holder implements InventoryHolder {
        final boolean admin; boolean lucky,protect; int weaponSlot=-1; final List<Integer> stoneSlots=new ArrayList<>(); final ForgeSettings revision; final Map<Integer,String> actions=new HashMap<>(); Inventory inventory;
        Holder(boolean a,boolean l,boolean p,ForgeSettings r){admin=a;lucky=l;protect=p;revision=r;}
        public Inventory getInventory(){return inventory;}
    }
    public void open(Player p,boolean admin,boolean lucky,boolean protect){
        if(!p.hasPermission(admin?"starforge.admin":"starforge.use")){texts.send(p,"no-permission");return;}
        var cfg=settings.get();
        var holder=new Holder(admin,lucky,protect,cfg);
        var y=admin?cfg.admin():cfg.gui();
        var rows=y.getStringList("shape");
        var title=ym.starforge.integration.MenuArt.title(Texts.parse(y.getString("title"),Map.of("level","0")),y.getString("background-image"));
        holder.inventory=Bukkit.createInventory(holder,rows.size()*9,title);
        paint(holder);
        p.openInventory(holder.inventory);
        viewers.put(p.getUniqueId(),p);
    }
    private void paint(Holder holder){
        var cfg=holder.revision;
        boolean lucky=holder.lucky,protect=holder.protect;
        ItemStack item=holder.weaponSlot<0?null:holder.inventory.getItem(holder.weaponSlot);
        if(item==null)item=new ItemStack(Material.AIR);
        var rule=weapons.match(item);
        boolean hasWeapon=rule!=null;
        if(!hasWeapon)rule=ForgeSettings.defaults();
        var stones=ForgeTransaction.snapshotStones(holder.inventory,holder.stoneSlots);
        int stoneCount=stones.values().stream().mapToInt(ItemStack::getAmount).sum();
        var materials=new MaterialItems(()->cfg);
        int current=weapons.level(item),target=Math.max(1,Math.min(rule.max(cfg.max()),current+1));
        var plan=ForgePlan.prepare(cfg,rule,current,weapons.failures(item),lucky);
        var mixedPlan=hasWeapon&&plan!=null?plan.withStones(cfg,rule,current,weapons.failures(item),lucky,
                materials.countStones(stones.values(),plan.material())):null;
        boolean validStones=mixedPlan!=null;
        if(validStones)plan=mixedPlan;
        var level=rule.level(target,cfg.levels());double base=weapons.base(item);
        if(plan!=null)target=plan.target();
        String materialId=plan==null?level.stone():plan.material();
        String stoneName;
        try{stoneName=net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(new ym.starforge.integration.MaterialItems(()->cfg).displayName(materialId));}
        catch(RuntimeException unavailable){stoneName=net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(texts.get("material-unavailable",Map.of()));}
        Map<String,String> values=new HashMap<>();
        values.put("level",""+current);values.put("next",""+target);
        double chance=validStones?plan.chance():0;
        values.put("chance",WeaponService.fmt(chance==1?100:Math.min(99.99,Math.floor(chance*10000)/100)));
        values.put("money",WeaponService.fmt(plan==null?0:plan.money()));
        values.put("materials",(validStones?stoneCount:plan==null?0:plan.amount())+"");
        values.put("current",WeaponService.fmt(base+weapons.bonus(item,base,current)));
        values.put("next-attack",WeaponService.fmt(base+weapons.bonus(item,base,target)));
        values.put("max-level",""+rule.max(cfg.max()));
        values.put("lucky",net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(texts.get(lucky?"option-enabled":"option-disabled",Map.of())));
        values.put("protect",net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(texts.get(protect?"option-enabled":"option-disabled",Map.of())));
        values.put("stone-name",stoneName);
        var meta=item.getItemMeta();
        values.put("weapon",meta!=null&&meta.customName()!=null?net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(meta.customName()):item.getType().name());
        var y=holder.admin?cfg.admin():cfg.gui();
        String shape=String.join("",y.getStringList("shape"));
        for(int slot=0;slot<shape.length();slot++){
            char c=shape.charAt(slot);if(c==' ')continue;var button=y.getConfigurationSection("elements."+c);String action=button.getString("action","none");
            if(!holder.admin&&action.equals("weapon")){holder.weaponSlot=slot;continue;}
            if(!holder.admin&&action.equals("stone")){if(!holder.stoneSlots.contains(slot))holder.stoneSlots.add(slot);continue;}
            ItemStack icon;
            {
                icon=button.getBoolean("pixel-button",false)?ym.starforge.integration.MaterialItems.blankButton():new ItemStack(Material.valueOf(button.getString("material")));
                boolean materialIcon=action.equals("stone")||button.contains("craft-engine");
                if(materialIcon) {
                    try {icon=new ym.starforge.integration.MaterialItems(()->cfg).create(action.equals("stone")?materialId:button.getString("craft-engine"));}
                    catch(RuntimeException unavailable) {plugin.getLogger().warning("GUI material unavailable: "+button.getCurrentPath()+": "+unavailable.getMessage());}
                }
                if(!materialIcon)icon.editMeta(m->{m.customName(Texts.parse(button.getString("name",""),values));m.lore(button.getStringList("lore").stream().map(t->Texts.parse(t,values)).toList());});
                if(!holder.admin&&(action.equals("lucky")||action.equals("protect"))&&!icon.getType().isAir()){
                    boolean enabled=action.equals("lucky")?lucky:protect;
                    icon.editMeta(scrollMeta->{
                        var lore=new ArrayList<net.kyori.adventure.text.Component>();
                        if(scrollMeta.lore()!=null)lore.addAll(scrollMeta.lore());
                        lore.add(net.kyori.adventure.text.Component.empty());
                        lore.add(texts.get(enabled?"option-enabled":"option-disabled",Map.of()));
                        button.getStringList("toggle-lore").forEach(line->lore.add(Texts.parse(line,values)));
                        scrollMeta.lore(lore.stream().map(Texts::plainStyle).toList());
                        scrollMeta.setEnchantmentGlintOverride(enabled);
                    });
                }
            }
            holder.inventory.setItem(slot,icon);holder.actions.put(slot,action);
        }
    }
    @EventHandler(ignoreCancelled=true) public void click(InventoryClickEvent event){
        if(!(event.getView().getTopInventory().getHolder() instanceof Holder h))return;event.setCancelled(true);
        if(!(event.getWhoClicked() instanceof Player p))return;
        if(!h.admin&&h.revision==settings.get()&&p.hasPermission("starforge.use")){
            int slot=event.getRawSlot();
            if((event.getClick()==ClickType.LEFT||event.getClick()==ClickType.RIGHT)&&slot>=h.inventory.getSize()&&slot<event.getView().countSlots()){
                event.setCancelled(false);
                return;
            }
            if(isInput(h,slot)&&(event.getClick()==ClickType.LEFT||event.getClick()==ClickType.RIGHT)){
                ItemStack cursor=event.getCursor();
                boolean mergesWeapon=slot==h.weaponSlot&&!cursor.getType().isAir()
                        &&event.getCurrentItem()!=null&&!event.getCurrentItem().getType().isAir()
                        &&event.getAction()!=InventoryAction.SWAP_WITH_CURSOR;
                if(!mergesWeapon&&(cursor.getType().isAir()||accepts(h,slot,cursor))&&event.getAction()!=InventoryAction.COLLECT_TO_CURSOR){
                    event.setCancelled(false);
                    repaint(p,h);
                }
                return;
            }
        }
        if(event.getClick()!=ClickType.LEFT)return;
        String action=h.actions.get(event.getRawSlot());if(action==null)return;
        ItemStack expectedWeapon=action.equals("forge")&&!h.admin?copy(h.inventory.getItem(h.weaponSlot)):null;
        var expectedStones=action.equals("forge")&&!h.admin?ForgeTransaction.snapshotStones(h.inventory,h.stoneSlots):Map.<Integer,ItemStack>of();
        p.getScheduler().execute(plugin,()->{
            if(!plugin.isEnabled()||!p.isOnline()||p.getOpenInventory().getTopInventory()!=h.inventory)return;
            if(settings.get()!=h.revision){p.closeInventory();return;}
            if(!p.hasPermission(h.admin?"starforge.admin":"starforge.use")){p.closeInventory();texts.send(p,"no-permission");return;}
            switch(action){
                case "close" -> p.closeInventory();
                case "forge" -> {
                    if(h.admin)return;
                    if(!Objects.equals(expectedWeapon,h.inventory.getItem(h.weaponSlot))
                            ||!Objects.equals(expectedStones,ForgeTransaction.snapshotStones(h.inventory,h.stoneSlots))){
                        texts.send(p,"changed");return;
                    }
                    transaction.execute(p,h.inventory,h.weaponSlot,h.stoneSlots,h.lucky,h.protect);
                    if(p.getOpenInventory().getTopInventory()==h.inventory)paint(h);
                }
                case "lucky" -> {h.lucky=!h.lucky;paint(h);}
                case "protect" -> {h.protect=!h.protect;paint(h);}
                case "player-menu" -> open(p,false,false,false);
                case "reload" -> p.performCommand("starforge reload");
                case "give-stone" -> p.performCommand("starforge give "+p.getName()+" stone 16");
                case "give-intermediate" -> p.performCommand("starforge give "+p.getName()+" intermediate_stone 16");
                case "give-advanced" -> p.performCommand("starforge give "+p.getName()+" advanced_stone 16");
                case "give-lucky" -> p.performCommand("starforge give "+p.getName()+" lucky 1");
                case "give-protect" -> p.performCommand("starforge give "+p.getName()+" protect 1");
                case "bind" -> p.performCommand("starforge bind stone");
            }
        },null,1);
    }
    private static ItemStack copy(ItemStack item){return item==null?null:item.clone();}
    private boolean isInput(Holder h,int slot){return slot>=0&&(slot==h.weaponSlot||h.stoneSlots.contains(slot));}
    private boolean accepts(Holder h,int slot,ItemStack item){
        if(slot==h.weaponSlot)return item.getAmount()==1&&weapons.canEnhance(item);
        var materials=new MaterialItems(()->h.revision);
        return h.revision.materials().keySet().stream()
                .filter(id->!id.equals("lucky")&&!id.equals("protect"))
                .anyMatch(id->materials.matches(item,id));
    }
    private void repaint(Player player,Holder h){
        player.getScheduler().execute(plugin,()->{
            if(player.getOpenInventory().getTopInventory()==h.inventory&&settings.get()==h.revision)paint(h);
        },null,1);
    }
    @EventHandler(ignoreCancelled=true) public void drag(InventoryDragEvent event){
        if(!(event.getView().getTopInventory().getHolder() instanceof Holder h))return;
        if(h.admin||h.revision!=settings.get()||!(event.getWhoClicked() instanceof Player p)||!p.hasPermission("starforge.use")){
            event.setCancelled(true);return;
        }
        for(var entry:event.getNewItems().entrySet()){
            if(entry.getKey()<h.inventory.getSize()&&(!isInput(h,entry.getKey())||!accepts(h,entry.getKey(),entry.getValue()))){
                event.setCancelled(true);return;
            }
        }
        repaint(p,h);
    }
    @EventHandler public void quit(PlayerQuitEvent e){
        var p=e.getPlayer();
        if(p.getOpenInventory().getTopInventory().getHolder() instanceof Holder h)returnInputs(p,h);
        viewers.remove(p.getUniqueId());transaction.quit(p.getUniqueId());
    }
    @EventHandler public void closed(InventoryCloseEvent e){
        if(e.getInventory().getHolder() instanceof Holder h&&e.getPlayer() instanceof Player p){
            returnInputs(p,h);
            viewers.remove(p.getUniqueId());
        }
    }
    private void returnInputs(Player player,Holder h){
        var inputs=new ArrayList<>(h.stoneSlots);
        inputs.add(h.weaponSlot);
        for(int slot:inputs){
            if(slot<0)continue;
            ItemStack item=h.inventory.getItem(slot);
            if(item==null||item.getType().isAir())continue;
            h.inventory.setItem(slot,null);
            player.getInventory().addItem(item).values().forEach(left->player.getWorld().dropItemNaturally(player.getLocation(),left));
        }
    }
    public void close(){
        for(Player p:viewers.values()){
            Runnable close=()->{
                if(p.getOpenInventory().getTopInventory().getHolder() instanceof Holder h){
                    returnInputs(p,h);
                    p.closeInventory();
                }
            };
            if(Bukkit.isOwnedByCurrentRegion(p))close.run();
            else if(plugin.isEnabled())p.getScheduler().execute(plugin,close,null,1);
        }
        viewers.clear();
    }
    public static void validate(YamlConfiguration y){
        var rows=y.getStringList("shape");
        if(rows.isEmpty()||rows.size()>6||rows.stream().anyMatch(s->s.length()!=9))throw new IllegalArgumentException("gui: shape requires 1..6 rows of 9 characters");
        var valid=Set.of("none","weapon","stone","close","forge","lucky","protect","player-menu","reload","give-stone","give-intermediate","give-advanced","give-lucky","give-protect","bind");
        for(char c:String.join("",rows).toCharArray())if(c!=' '){
            var element=y.getConfigurationSection("elements."+c);if(element==null)throw new IllegalArgumentException("gui: unmapped symbol "+c);
            try{Material.valueOf(element.getString("material"));}catch(Exception e){throw new IllegalArgumentException("gui: elements."+c+".material invalid '"+element.get("material")+"'",e);}
            if(!valid.contains(element.getString("action")))throw new IllegalArgumentException("gui: elements."+c+".action invalid '"+element.get("action")+"'");
        }
    }
    public static void validateInputs(YamlConfiguration y){
        for(String action:List.of("weapon","stone")){
            long count=String.join("",y.getStringList("shape")).chars()
                    .filter(symbol->action.equals(y.getString("elements."+(char)symbol+".action"))).count();
            if(action.equals("weapon")?count!=1:count<1)throw new IllegalArgumentException("gui/main.yml: shape requires one weapon slot and at least one stone slot");
        }
    }
}
