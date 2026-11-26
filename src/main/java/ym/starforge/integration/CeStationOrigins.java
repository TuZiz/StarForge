package ym.starforge.integration;

import net.momirealms.craftengine.bukkit.api.event.FurnitureInteractEvent;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import org.bukkit.event.*;
import org.bukkit.plugin.Plugin;
import ym.starforge.workstation.*;

/** Observe only: CE still checks its interact flag and executes its own configured command. */
public final class CeStationOrigins implements Listener {
    private final Plugin plugin;
    private final StationOrigins origins;
    public CeStationOrigins(Plugin plugin, StationOrigins origins) { this.plugin=plugin; this.origins=origins; }
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void interact(FurnitureInteractEvent event) {
        if(event.hand()!=InteractionHand.MAIN_HAND)return;
        var kind=switch(event.furniture().id().asString()) {
            case "starforge:enhancement_station" -> StationAnchor.Kind.ENHANCEMENT;
            case "starforge:crafting_station" -> StationAnchor.Kind.CRAFTING;
            default -> null;
        };
        if(kind==null)return;
        var player=event.getPlayer();
        origins.offer(player,StationAnchor.of(event.furniture().uuid(),event.furniture().location(),kind));
        player.getScheduler().execute(plugin,()->origins.clear(player.getUniqueId()),
                ()->origins.clear(player.getUniqueId()),1);
    }
}
