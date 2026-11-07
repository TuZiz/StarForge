package ym.starforge.item;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.Plugin;
import ym.starforge.integration.PlaceholderCache;
import ym.starforge.transaction.ForgeTransaction;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;

/** Event-driven coalesced ownership-thread sync. No polling and no offline archive access. */
public final class EquipmentSync implements Listener,AutoCloseable {
    private final Plugin plugin;private final WeaponService weapons;private final Supplier<PlaceholderCache> placeholders;
    private final Map<UUID,ScheduledTask> tasks=new ConcurrentHashMap<>();
    public EquipmentSync(Plugin p,WeaponService w,Supplier<PlaceholderCache> h){plugin=p;weapons=w;placeholders=h;}
    public void request(Player player){
        if(!plugin.isEnabled())return;
        UUID id=player.getUniqueId();var old=tasks.remove(id);if(old!=null)old.cancel();
        var task=player.getScheduler().runDelayed(plugin,t->{
            tasks.remove(id,t);if(!plugin.isEnabled()||!player.isOnline())return;
            var inv=player.getInventory();var before=ForgeTransaction.cloneContents(inv.getContents());
            var replacements=new LinkedHashMap<Integer,org.bukkit.inventory.ItemStack>();
            try {
                for(int i=0;i<before.length;i++)if(before[i]!=null&&before[i].getPersistentDataContainer().has(ForgeData.LEVEL)){
                    var next=weapons.refresh(before[i]);if(!next.equals(before[i]))replacements.put(i,next);
                }
                try{replacements.forEach(inv::setItem);}catch(Exception error){inv.setContents(before);throw error;}
                var papi=placeholders.get();if(papi!=null)papi.update(id,weapons.level(inv.getItemInMainHand()),weapons.failures(inv.getItemInMainHand()));
            }catch(Exception error){plugin.getLogger().log(Level.SEVERE,"Equipment sync failed player="+id,error);}
        },()->tasks.remove(id),2);
        if(task!=null)tasks.put(id,task);
    }
    @EventHandler public void join(PlayerJoinEvent e){request(e.getPlayer());}
    @EventHandler public void held(PlayerItemHeldEvent e){request(e.getPlayer());}
    @EventHandler public void swap(PlayerSwapHandItemsEvent e){request(e.getPlayer());}
    @EventHandler public void close(InventoryCloseEvent e){if(e.getPlayer() instanceof Player p)request(p);}
    @EventHandler(ignoreCancelled=true) public void click(InventoryClickEvent e){if(e.getWhoClicked() instanceof Player p)request(p);}
    @EventHandler public void pickup(org.bukkit.event.entity.EntityPickupItemEvent e){if(e.getEntity() instanceof Player p)request(p);}
    @EventHandler public void quit(PlayerQuitEvent e){var task=tasks.remove(e.getPlayer().getUniqueId());if(task!=null)task.cancel();var papi=placeholders.get();if(papi!=null)papi.remove(e.getPlayer().getUniqueId());}
    public void close(){tasks.values().forEach(ScheduledTask::cancel);tasks.clear();}
}
