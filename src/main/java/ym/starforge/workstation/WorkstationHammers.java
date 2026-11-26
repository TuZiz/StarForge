package ym.starforge.workstation;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;

/** Paper/Purpur main-thread owner of the shared, nonpersistent hammers for physical orders. */
public final class WorkstationHammers implements AutoCloseable {
    private static final int SEARCH_INTERVAL = 10;
    private static final NamespacedKey MODEL = new NamespacedKey("starforge", "workstations/forge_hammer");
    private final Plugin plugin;
    private final Supplier<Map<UUID, Long>> activeUntil;
    private final Supplier<StationAnimationSettings> settings;
    private final Map<UUID, Swing> swings = new HashMap<>();
    private BukkitTask task;
    private int searchIn;

    public WorkstationHammers(Plugin plugin, Supplier<Map<UUID, Long>> activeUntil,
                              Supplier<StationAnimationSettings> settings) {
        this.plugin = plugin;
        this.activeUntil = activeUntil;
        this.settings = settings;
    }

    public void start() {
        if (task != null || !settings.get().enabled()) return;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return;
        } catch (ClassNotFoundException paper) {
            task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1, 1);
        }
    }

    private void tick() {
        var orders = activeUntil.get();
        var cfg = settings.get();
        long now = System.currentTimeMillis();
        boolean search = searchIn-- == 0;
        if (search) searchIn = SEARCH_INTERVAL - 1;

        for (var it = swings.values().iterator(); it.hasNext();) {
            Swing swing = it.next();
            try {
                if (cfg.enabled() && orders.getOrDefault(swing.furniture.getUniqueId(), 0L) > now && swing.valid()
                        && (!search || hasViewer(swing.origin, cfg.distance()))) {
                    swing.advance();
                    continue;
                }
            } catch (RuntimeException ex) {
                report(swing.furniture.getUniqueId(), ex);
            }
            swing.remove();
            it.remove();
        }

        if (!search || !cfg.enabled()) return;
        for (var entry : orders.entrySet()) {
            UUID furnitureId = entry.getKey();
            if (entry.getValue() <= now || swings.containsKey(furnitureId)) continue;
            try {
                findTable(furnitureId, cfg.distance());
            } catch (RuntimeException ex) {
                report(furnitureId, ex);
            }
        }
    }

    private void findTable(UUID furnitureId, double distance) {
        Entity furniture = plugin.getServer().getEntity(furnitureId);
        if (furniture == null || !furniture.isValid()) return;
        Location origin = furniture.getLocation();
        origin.setPitch(0);
        World world = origin.getWorld();
        if (!world.isChunkLoaded(origin.getBlockX() >> 4, origin.getBlockZ() >> 4)
                || !hasViewer(origin, distance)) return;
        var item = new ItemStack(Material.IRON_INGOT);
        item.editMeta(meta -> meta.setItemModel(MODEL));
        var hammer = world.spawn(origin, ItemDisplay.class, entity -> {
            entity.setPersistent(false);
            entity.setInvulnerable(true);
            entity.setGravity(false);
            entity.setRotation(origin.getYaw(), 0);
            entity.setItemStack(item);
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            entity.setBrightness(new Display.Brightness(12, 12));
            entity.setInterpolationDuration(1);
            entity.setTransformation(HammerStroke.frame(0));
        });
        swings.put(furnitureId, new Swing(origin, furniture, hammer));
    }

    private static boolean hasViewer(Location origin, double distance) {
        return !origin.getWorld().getNearbyPlayers(origin, distance,
                p -> p.getLocation().distanceSquared(origin) <= distance * distance).isEmpty();
    }

    private void report(UUID furniture, RuntimeException ex) {
        plugin.getLogger().log(Level.WARNING, "Forge hammer failed at furniture=" + furniture, ex);
    }

    @Override public void close() {
        if (task != null) task.cancel();
        task = null;
        searchIn = 0;
        swings.values().forEach(Swing::remove);
        swings.clear();
    }

    private static final class Swing {
        final Location origin;
        final Location strikeAt;
        final Entity furniture;
        final ItemDisplay display;
        int tick;

        Swing(Location origin, Entity furniture, ItemDisplay display) {
            this.origin = origin;
            this.furniture = furniture;
            this.display = display;
            var contact = StationMotion.worldOffset(HammerStroke.billet(), origin.getYaw());
            strikeAt = origin.clone().add(contact.x, contact.y, contact.z);
        }

        boolean valid() {
            return display.isValid() && furniture.isValid()
                    && furniture.getWorld() == origin.getWorld()
                    && furniture.getX() == origin.getX() && furniture.getY() == origin.getY()
                    && furniture.getZ() == origin.getZ() && furniture.getYaw() == origin.getYaw()
                    && origin.getWorld().isChunkLoaded(origin.getBlockX() >> 4, origin.getBlockZ() >> 4);
        }

        void advance() {
            int hit = HammerStroke.impact(tick);
            if (hit != 0) HammerImpacts.strike(strikeAt, origin.getYaw(), hit);
            // The client spends the next tick interpolating to this target. Emit the
            // strike on arrival, once the preceding tick's target has reached the steel.
            tick = (tick + 1) % HammerStroke.PERIOD;
            display.setTransformation(HammerStroke.frame(tick));
            display.setInterpolationDelay(0);
        }

        void remove() {
            if (display.isValid()) display.remove();
        }
    }
}
