package ym.starforge.workstation;

import org.bukkit.entity.Player;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** A CE event supplies a one-shot origin, consumed by its existing synchronous command. */
public final class StationOrigins {
    private record Pending(StationAnchor anchor, int tick) {}
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();
    public void offer(Player player, StationAnchor anchor) {
        pending.put(player.getUniqueId(), new Pending(anchor, player.getTicksLived()));
    }
    public StationAnchor take(Player player, StationAnchor.Kind kind) {
        var value = pending.remove(player.getUniqueId());
        return value != null && value.tick == player.getTicksLived() && value.anchor.kind() == kind
                && value.anchor.nearby(player.getLocation(), 8) ? value.anchor : null;
    }
    public void clear(UUID player) { pending.remove(player); }
    public void clear() { pending.clear(); }
}
