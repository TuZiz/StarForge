package ym.starforge.workstation;

import org.bukkit.Location;
import java.util.UUID;

/** Immutable furniture identity/position; no live entity or mutable Location crosses schedulers. */
public record StationAnchor(UUID furniture, UUID world, double x, double y, double z, float yaw, Kind kind) {
    public enum Kind { ENHANCEMENT, CRAFTING }
    public static StationAnchor of(UUID furniture, Location location, Kind kind) {
        return new StationAnchor(furniture, location.getWorld().getUID(), location.x(), location.y(), location.z(), location.getYaw(), kind);
    }
    public boolean nearby(Location player, double range) {
        return player.getWorld().getUID().equals(world)
                && Math.pow(player.x()-x,2)+Math.pow(player.y()-y,2)+Math.pow(player.z()-z,2)<=range*range;
    }
}
