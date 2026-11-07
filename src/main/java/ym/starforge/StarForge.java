package ym.starforge;

import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.entity.Player;
import ym.starforge.api.StarForgeApi;
import ym.starforge.config.*;
import ym.starforge.item.*;
import ym.starforge.gui.ForgeMenus;
import ym.starforge.command.ForgeCommand;
import ym.starforge.integration.*;
import ym.starforge.text.Texts;
import ym.starforge.transaction.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public final class StarForge extends JavaPlugin {
    private volatile ForgeSettings settings;private final AtomicBoolean loading=new AtomicBoolean();
    private EquipmentSync sync;private WeaponService weapons;private ForgeMenus menus;private ForgeTransaction transaction;private OperationLog log;private AutoCloseable hub;private PlaceholderCache placeholders;
    public void onEnable(){getLogger().info("StarForge 1.0.0 on "+getServer().getName()+" "+getServer().getMinecraftVersion()+" Java "+System.getProperty("java.version"));reload();}
    public void reload(){
        if(!loading.compareAndSet(false,true))return;
        Bukkit.getAsyncScheduler().runNow(this,task->{
            try{var loaded=new SettingsLoader(this).load();ForgeMenus.validate(loaded.gui());ForgeMenus.validate(loaded.admin());
                if(!isEnabled())return;
                Bukkit.getGlobalRegionScheduler().execute(this,()->{try{if(!isEnabled())return;
                    if(loaded.backend().equals("asterstats")) {
                        var stats=getServer().getPluginManager().getPlugin("AsterStats");
                        if(stats==null||!stats.isEnabled())throw new IllegalStateException("config.yml:attribute-backend=asterstats requires AsterStats with StarForge bridge");
                        stats.getClass().getClassLoader().loadClass("dev.aster.asterstats.equipment.StarForgeAttributes");
                        stats.getClass().getClassLoader().loadClass("dev.aster.asterstats.api.AsterStatsItemApi");
                    }
                    if(menus!=null)menus.close();settings=loaded;if(weapons==null)start();
                    getLogger().info("Loaded configuration schema=1 weapons-schema=2 max="+loaded.max()+" backend="+loaded.backend()+" weapon-rules="+loaded.weapons().size());
                    sync.close();for(Player p:Bukkit.getOnlinePlayers())sync.request(p);
                }catch(Exception e){getLogger().log(Level.SEVERE,"StarForge apply failed",e);if(menus==null)getServer().getPluginManager().disablePlugin(this);}finally{loading.set(false);}});
            }catch(Exception e){getLogger().log(Level.SEVERE,"StarForge config load failed; previous settings retained",e);loading.set(false);}
        });
    }
    private void start(){
        weapons=new WeaponService(()->settings);var texts=new Texts(()->settings);var materials=new MaterialItems(()->settings);
        log=new OperationLog(getDataFolder().toPath(),getLogger());transaction=new ForgeTransaction(this,()->settings,weapons,materials,texts,log);
        menus=new ForgeMenus(this,()->settings,weapons,transaction,texts);getServer().getPluginManager().registerEvents(menus,this);sync=new EquipmentSync(this,weapons,()->placeholders);getServer().getPluginManager().registerEvents(sync,this);
        getServer().getServicesManager().register(StarForgeApi.class,weapons,this,ServicePriority.Normal);
        if(getServer().getPluginManager().isPluginEnabled("ItemHub"))hub=HubBridge.register(this,weapons);
        if(getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")){placeholders=PapiBridge.create();}
        var cmd=new ForgeCommand(this,menus,materials,weapons,texts,this::reload,()->settings.max());getCommand("starforge").setExecutor(cmd);getCommand("starforge").setTabCompleter(cmd);
    }
    public void onDisable(){if(sync!=null)sync.close();if(menus!=null)menus.close();if(transaction!=null)transaction.close();if(hub!=null)try{hub.close();}catch(Exception e){getLogger().log(Level.SEVERE,"Close ItemHub registration",e);}if(placeholders!=null){placeholders.close();}getServer().getServicesManager().unregisterAll(this);Bukkit.getAsyncScheduler().cancelTasks(this);Bukkit.getGlobalRegionScheduler().cancelTasks(this);if(log!=null)log.close();}
}
