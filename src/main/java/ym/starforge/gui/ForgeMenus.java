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
    private final ym.starforge.workstation.StationOrigins stationOrigins;
    private final ym.starforge.workstation.StationAnimations stationAnimations;
    private final Plugin plugin;private final Supplier<ForgeSettings> settings;private final WeaponService weapons;private final ForgeTransaction transaction;private final Texts texts;
    private final Map<UUID,Player> viewers=new ConcurrentHashMap<>();
    private final ForgeResultReopen resultReopens;
    private final ForgeMenuRenderer renderer;
    private final ForgePausedInputs<Holder> pausedInputs=new ForgePausedInputs<>();
    public ForgeMenus(Plugin p,Supplier<ForgeSettings> s,WeaponService w,ForgeTransaction t,Texts x){this(p,s,w,t,x,null,null);}
    public ForgeMenus(Plugin p,Supplier<ForgeSettings> s,WeaponService w,ForgeTransaction t,Texts x,
                      ym.starforge.workstation.StationOrigins origins,ym.starforge.workstation.StationAnimations animations){
        plugin=p;settings=s;weapons=w;transaction=t;texts=x;resultReopens=new ForgeResultReopen(p);
        stationOrigins=origins;stationAnimations=animations;
        renderer=new ForgeMenuRenderer(p,w,x,(player,holder)->{returnInputs(player,holder);viewers.remove(player.getUniqueId());});
    }
    public static final class Holder implements InventoryHolder {
        final boolean admin; boolean lucky,protect; int weaponSlot=-1; final List<Integer> stoneSlots=new ArrayList<>(); final ForgeSettings revision; final Map<Integer,String> actions=new HashMap<>(); Inventory inventory;
        boolean transitioning; net.kyori.adventure.text.Component title;
        ym.starforge.workstation.StationAnchor station;
        final LinkedHashMap<String,Integer> selected=new LinkedHashMap<>();
        final Map<Integer,String> stoneActions=new HashMap<>();
        io.papermc.paper.threadedregions.scheduler.ScheduledTask refresh;
        ItemStack[] previewStorage;ItemStack previewWeapon;
        Holder(boolean a,boolean l,boolean p,ForgeSettings r){admin=a;lucky=l;protect=p;revision=r;}
        public Inventory getInventory(){return inventory;}
    }
    public void open(Player p,boolean admin,boolean lucky,boolean protect){
        resultReopens.cancel(p.getUniqueId());
        releasePaused(p);
        if(!p.hasPermission(admin?"starforge.admin":"starforge.use")){texts.send(p,"no-permission");return;}
        var cfg=settings.get();
        if(!plugin.isEnabled()||!p.isOnline()||cfg==null)return;
        var holder=new Holder(admin,lucky,protect,cfg);
        if(stationOrigins!=null)holder.station=stationOrigins.take(p,ym.starforge.workstation.StationAnchor.Kind.ENHANCEMENT);
        var y=admin?cfg.admin():cfg.gui();
        var rows=y.getStringList("shape");
        var title=ForgeMenuTitle.render(y.getString("title"),y.getString("background-image"),null);
        holder.title=title;
        holder.inventory=Bukkit.createInventory(holder,rows.size()*9,title);
        paint(p,holder);
        try{
            p.openInventory(holder.inventory);
            if(p.getOpenInventory().getTopInventory()==holder.inventory){viewers.put(p.getUniqueId(),p);watch(p,holder);}
        }finally{
            if(p.getOpenInventory().getTopInventory()!=holder.inventory)returnInputs(p,holder);
        }
    }
    private void paint(Player player,Holder holder){renderer.paint(player,holder);}
    @EventHandler(ignoreCancelled=true) public void click(InventoryClickEvent event){
        if(!(event.getView().getTopInventory().getHolder() instanceof Holder h))return;event.setCancelled(true);
        if(!(event.getWhoClicked() instanceof Player p))return;
        String material=h.stoneActions.get(event.getRawSlot());
        if(material!=null&&!h.admin){
            var click=event.getClick();
            int delta=ForgeStoneCards.delta(click);
            if(delta!=0){
                p.getScheduler().execute(plugin,()->{
                    if(!valid(p,h))return;
                    var owned=new MaterialItems(()->h.revision).countSelected(p.getInventory().getStorageContents(),h.selected.keySet());
                    h.selected.put(material,ym.starforge.transaction.ForgeStoneSelection.adjusted(h.selected.get(material),delta,owned.get(material)));
                    paint(p,h);
                },null,1);
            }
            return;
        }
        if(!h.admin&&h.revision==settings.get()&&p.hasPermission("starforge.use")){
            int slot=event.getRawSlot();
            if((event.getClick()==ClickType.LEFT||event.getClick()==ClickType.RIGHT)&&slot>=h.inventory.getSize()&&slot<event.getView().countSlots()){
                event.setCancelled(false);
                repaint(p,h);
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
        var expectedSelection=new LinkedHashMap<>(h.selected);
        p.getScheduler().execute(plugin,()->{
            if(!plugin.isEnabled()||!p.isOnline()||p.getOpenInventory().getTopInventory()!=h.inventory)return;
            if(settings.get()!=h.revision){p.closeInventory();return;}
            if(!p.hasPermission(h.admin?"starforge.admin":"starforge.use")){p.closeInventory();texts.send(p,"no-permission");return;}
            switch(action){
                case "close" -> p.closeInventory();
                case "forge" -> {
                    if(h.admin)return;
                    if(!Objects.equals(expectedWeapon,h.inventory.getItem(h.weaponSlot))
                            ||!Objects.equals(h.previewWeapon,expectedWeapon)||!h.selected.equals(expectedSelection)
                            ||!Arrays.equals(h.previewStorage,p.getInventory().getStorageContents())){
                        texts.send(p,"selection-changed");paint(p,h);return;
                    }
                    transaction.executeSelection(p,h.inventory,h.weaponSlot,expectedSelection,h.lucky,h.protect,
                            ()->reopenAfterResult(p,h),item->{if(stationAnimations!=null)stationAnimations.play(p,h.station,item);});
                    if(p.getOpenInventory().getTopInventory()==h.inventory)paint(p,h);
                }
                case "lucky", "protect" -> {
                    if(h.weaponSlot>=0&&ym.starforge.item.ForgeData.cracked(h.inventory.getItem(h.weaponSlot)))return;
                    boolean enabled=action.equals("lucky")?h.lucky:h.protect;
                    if(action.equals("lucky"))h.lucky=!enabled;
                    else h.protect=!enabled;
                    paint(p,h);
                }
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
    private void reopenAfterResult(Player player,Holder holder){
        if(player.getOpenInventory().getTopInventory()!=holder.inventory)return;
        pausedInputs.pause(player.getUniqueId(),holder);
        player.closeInventory();
        var closedView=player.getOpenInventory().getTopInventory();
        if(closedView.getType()!=InventoryType.CRAFTING){releasePaused(player);return;}
        try{if(!resultReopens.schedule(player,()->{
            resumePaused(player,holder,closedView);
        }))releasePaused(player);}catch(RuntimeException ex){releasePaused(player);throw ex;}
    }
    /** Player ownership. Take once before open events; failure must never strand the weapon. */
    private void resumePaused(Player player,Holder holder,Inventory closedView){
        if(!pausedInputs.take(player.getUniqueId(),holder))return;
        boolean restored=false;
        try{
            if(plugin.isEnabled()&&player.isOnline()&&settings.get()==holder.revision
                    &&player.hasPermission("starforge.use")&&player.getOpenInventory().getTopInventory()==closedView){
                player.openInventory(holder.inventory);
                if(player.getOpenInventory().getTopInventory()==holder.inventory){paint(player,holder);watch(player,holder);restored=true;}
            }
        }finally{
            if(!restored){
                if(holder.refresh!=null){holder.refresh.cancel();holder.refresh=null;}
                try{if(player.getOpenInventory().getTopInventory()==holder.inventory)player.closeInventory();}
                finally{returnInputs(player,holder);viewers.remove(player.getUniqueId());}
            }
        }
    }
    private void releasePaused(Player player){
        var holder=pausedInputs.take(player.getUniqueId());
        if(holder!=null){returnInputs(player,holder);viewers.remove(player.getUniqueId());}
    }
    private static ItemStack copy(ItemStack item){return item==null?null:item.clone();}
    private boolean isInput(Holder h,int slot){return slot>=0&&slot==h.weaponSlot;}
    private boolean accepts(Holder h,int slot,ItemStack item){
        if(slot==h.weaponSlot)return item.getAmount()==1&&weapons.canEnhance(item);
        return false;
    }
    private void repaint(Player player,Holder h){
        player.getScheduler().execute(plugin,()->{
            if(player.getOpenInventory().getTopInventory()==h.inventory&&settings.get()==h.revision)paint(player,h);
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
        resultReopens.cancel(p.getUniqueId());
        releasePaused(p);
        if(p.getOpenInventory().getTopInventory().getHolder() instanceof Holder h)returnInputs(p,h);
        viewers.remove(p.getUniqueId());transaction.quit(p.getUniqueId());
    }
    @EventHandler public void closed(InventoryCloseEvent e){
        if(e.getInventory().getHolder() instanceof Holder h&&e.getPlayer() instanceof Player p){
            if(!h.transitioning&&h.refresh!=null){h.refresh.cancel();h.refresh=null;}
            if(h.transitioning||pausedInputs.contains(p.getUniqueId(),h))return;
            returnInputs(p,h);
            viewers.remove(p.getUniqueId());
        }else if(e.getPlayer() instanceof Player p){
            resultReopens.cancel(p.getUniqueId());releasePaused(p);
        }
    }
    @EventHandler public void opened(InventoryOpenEvent e){
        resultReopens.cancel(e.getPlayer().getUniqueId());
        if(e.getPlayer() instanceof Player player)releasePaused(player);
    }
    private void returnInputs(Player player,Holder h){
        var inputs=new ArrayList<Integer>();
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
        resultReopens.close();
        for(Player p:viewers.values()){
            Runnable close=()->{
                releasePaused(p);
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
        var valid=Set.of("none","weapon","stone","chance","close","forge","lucky","protect","player-menu","reload","give-stone","give-intermediate","give-advanced","give-lucky","give-protect","bind");
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
        var ids=y.getStringList("virtual-materials.materials");
        long slots=String.join("",y.getStringList("shape")).chars().filter(c->"stone".equals(y.getString("elements."+(char)c+".action"))).count();
        if(ids.isEmpty()||ids.size()!=new HashSet<>(ids).size()||ids.size()>slots)
            throw new IllegalArgumentException("gui/main.yml:virtual-materials.materials requires distinct materials fitting the stone slots");
    }
    private boolean valid(Player p,Holder h){return plugin.isEnabled()&&p.isOnline()&&p.hasPermission("starforge.use")&&h.revision==settings.get()&&p.getOpenInventory().getTopInventory()==h.inventory;}
    private void watch(Player p,Holder h){
        if(h.refresh!=null)h.refresh.cancel();
        if(h.admin)return;
        h.refresh=p.getScheduler().runAtFixedRate(plugin,t->{
            if(!valid(p,h)){t.cancel();return;}
            try{paint(p,h);}catch(RuntimeException ex){t.cancel();p.closeInventory();plugin.getLogger().log(java.util.logging.Level.SEVERE,"Forge refresh failed player="+p.getUniqueId(),ex);}
        },null,20,20);
    }
}
