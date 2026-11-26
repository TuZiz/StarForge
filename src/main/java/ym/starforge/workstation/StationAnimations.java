package ym.starforge.workstation;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;

/** Cosmetic only. Caller snapshots on player ownership; spawning runs on the station region,
 * transforms/particles/removal on the stationary display's entity scheduler. Never teleports items. */
public final class StationAnimations implements Listener, AutoCloseable {
    private final Plugin plugin;
    private final StationOrigins origins;
    private final Supplier<StationAnimationSettings> settings;
    private final boolean supported;
    private final Map<UUID, Flight> flights=new ConcurrentHashMap<>();
    private final Map<UUID, Flight> players=new ConcurrentHashMap<>();
    private static final class Flight {
        final UUID player;
        final StationAnchor anchor;
        volatile boolean cancelled;
        volatile ItemDisplay display;
        Flight(UUID player,StationAnchor anchor){this.player=player;this.anchor=anchor;}
    }
    public StationAnimations(Plugin plugin,StationOrigins origins,Supplier<StationAnimationSettings> settings) {
        this.plugin=plugin;this.origins=origins;this.settings=settings;
        // Public schedulers reject new tasks after disable. Do not create remote-region displays
        // on Folia until cross-region shutdown can be guaranteed without borrowing another plugin.
        boolean folia;
        try{Class.forName("io.papermc.paper.threadedregions.RegionizedServer");folia=true;}
        catch(ClassNotFoundException ignored){folia=false;}
        supported=!folia;
        if(!supported)plugin.getLogger().info("Station item animations disabled on Folia; enhancement and crafting remain available");
    }
    /** Player ownership thread. False means no effect accepted (command menu, busy table, or away). */
    public boolean play(Player player,StationAnchor anchor,ItemStack item) {
        var cfg=settings.get();
        if(!supported||anchor==null||item==null||item.getType().isAir()||cfg==null||!cfg.enabled()
                ||!plugin.isEnabled()||!player.isOnline()||!player.hasPermission("starforge.use"))return false;
        // CE can rotate or move the same piece of furniture while its menu is open.
        var furniture=player.getWorld().getEntity(anchor.furniture());
        if(furniture==null||!furniture.isValid())return false;
        var at=furniture.getLocation();
        at.setPitch(0);
        var current=StationAnchor.of(anchor.furniture(),at,anchor.kind());
        if(!current.nearby(player.getLocation(),cfg.distance()))return false;
        var copy=item.clone();copy.setAmount(1);
        var flight=new Flight(player.getUniqueId(),current);
        // One visual per table. Concurrent transactions remain entirely independent.
        if(flights.putIfAbsent(anchor.furniture(),flight)!=null)return false;
        var old=players.put(player.getUniqueId(),flight);
        if(old!=null)stop(old);
        try {
            Bukkit.getRegionScheduler().execute(plugin,at,()->start(flight,at,copy,cfg));
            return true;
        } catch(RuntimeException ex) { stop(flight);report(flight,ex);return false; }
    }
    private void start(Flight f,Location at,ItemStack copy,StationAnimationSettings cfg) {
        if(f.cancelled||!plugin.isEnabled()){forget(f);return;}
        try {
            if(!at.getWorld().isChunkLoaded(at.getBlockX()>>4,at.getBlockZ()>>4)){forget(f);return;}
            var furniture=Bukkit.getEntity(f.anchor.furniture());
            if(furniture==null||!furniture.isValid()){forget(f);return;}
            f.display=at.getWorld().spawn(at,ItemDisplay.class,display->{
                display.setPersistent(false);display.setInvulnerable(true);display.setGravity(false);
                display.setRotation(at.getYaw(), 0);
                display.setItemStack(copy);display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                display.setBrightness(new Display.Brightness(15,15));display.setViewRange(0.7f);
                display.setInterpolationDuration(1);
                display.setTransformation(StationMotion.frame(f.anchor.kind(), 0, cfg.rise()));
            });
            if(f.cancelled){f.display.remove();forget(f);return;}
            int[] ticks={0};
            var task=f.display.getScheduler().runAtFixedRate(plugin,t->{
                try {
                    if(f.cancelled||!plugin.isEnabled()||!f.display.isValid()||!furniture.isValid()) {
                        t.cancel();f.display.remove();forget(f);return;
                    }
                    int age=++ticks[0];
                    double progress=Math.min(1,(double)age/cfg.ticks());
                    var pose = StationMotion.frame(f.anchor.kind(), progress, cfg.rise());
                    f.display.setTransformation(pose);
                    f.display.setInterpolationDelay(0);
                    if (f.anchor.kind() == StationAnchor.Kind.CRAFTING) {
                        CraftingHeat.apply(f.display, age, cfg.ticks(), pose.getScale().x);
                    }
                    StationEffects.render(f.anchor.kind(), at, pose.getTranslation(), age, cfg.ticks());
                    if(age>=cfg.ticks()) {
                        t.cancel();f.display.remove();forget(f);
                    }
                }catch(RuntimeException ex){t.cancel();f.display.remove();forget(f);report(f,ex);}
            },()->forget(f),1,1);
            if(task==null){f.display.remove();forget(f);}
        }catch(RuntimeException ex){stop(f);report(f,ex);}
    }
    private void forget(Flight f){flights.remove(f.anchor.furniture(),f);players.remove(f.player,f);}
    private void stop(Flight f) {
        f.cancelled=true;
        var display=f.display;
        if(display==null){forget(f);return;}
        if(Bukkit.isOwnedByCurrentRegion(display)){display.remove();forget(f);}
        else if(plugin.isEnabled())display.getScheduler().execute(plugin,()->{display.remove();forget(f);},()->forget(f),1);
        else {
            // Displays are non-persistent: never save cosmetic items into world/player data.
            plugin.getLogger().warning("Station animation shutdown outside entity ownership; non-persistent display="+display.getUniqueId());
            forget(f);
        }
    }
    private void report(Flight f,Throwable ex){plugin.getLogger().log(Level.WARNING,
            "Station visual failed player="+f.player+" furniture="+f.anchor.furniture()+"; committed items unchanged",ex);}
    @EventHandler public void quit(PlayerQuitEvent e){cancel(e.getPlayer().getUniqueId());}
    @EventHandler public void world(PlayerChangedWorldEvent e){cancel(e.getPlayer().getUniqueId());}
    @EventHandler public void teleport(PlayerTeleportEvent e){cancel(e.getPlayer().getUniqueId());}
    @EventHandler public void command(PlayerCommandPreprocessEvent e){origins.clear(e.getPlayer().getUniqueId());}
    @EventHandler public void unload(WorldUnloadEvent e){
        flights.values().stream().filter(f->f.anchor.world().equals(e.getWorld().getUID())).toList().forEach(this::stop);
    }
    private void cancel(UUID id){origins.clear(id);var f=players.get(id);if(f!=null)stop(f);}
    public void close(){origins.clear();List.copyOf(flights.values()).forEach(this::stop);}
}
