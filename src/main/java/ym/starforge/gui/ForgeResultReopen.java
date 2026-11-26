package ym.starforge.gui;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Owns only delayed callbacks, never player items. All inventory work runs on the entity scheduler. */
final class ForgeResultReopen implements AutoCloseable {
    private final Plugin plugin;
    private final Map<UUID, Ticket> pending = new ConcurrentHashMap<>();
    private static final class Ticket { volatile ScheduledTask task; }
    ForgeResultReopen(Plugin plugin) { this.plugin = plugin; }

    boolean schedule(Player player, Runnable reopen) {
        UUID id = player.getUniqueId(); cancel(id);
        var ticket = new Ticket(); pending.put(id, ticket);
        ticket.task = player.getScheduler().runDelayed(plugin, task -> {
            if (pending.remove(id, ticket) && plugin.isEnabled() && player.isOnline()) reopen.run();
        }, () -> pending.remove(id, ticket), 30);
        if (ticket.task == null) pending.remove(id, ticket);
        else if (pending.get(id) != ticket) ticket.task.cancel();
        return ticket.task!=null && pending.get(id)==ticket;
    }
    void cancel(UUID id) {
        var ticket = pending.remove(id);
        if (ticket != null && ticket.task != null) ticket.task.cancel();
    }
    public void close() { for (UUID id : pending.keySet()) cancel(id); }
}
