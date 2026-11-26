package ym.starforge.crafting;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.plugin.Plugin;
import ym.starforge.text.Texts;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Read-only chest inventories. Navigation, inventory APIs and refresh run on the viewer scheduler. */
public final class CraftingMenu implements Listener, AutoCloseable {
    private final ym.starforge.workstation.StationOrigins stationOrigins;
    private final ym.starforge.workstation.StationAnimations stationAnimations;
    private final Plugin plugin;
    private final Supplier<CraftingConfig> config;
    private final CraftingTransaction transaction;
    private final CraftingDelivery delivery;
    private final Texts texts;
    private final RecipeUnlocks unlocks;
    private final CraftingMenuRenderer renderer;
    private final Map<UUID, CraftingSession> sessions = new ConcurrentHashMap<>();

    public CraftingMenu(Plugin plugin, Supplier<CraftingConfig> config, CraftingOrders orders, Texts texts, RecipeUnlocks unlocks) {
        this(plugin,config,orders,texts,unlocks,null,null);
    }
    public CraftingMenu(Plugin plugin, Supplier<CraftingConfig> config, CraftingOrders orders, Texts texts, RecipeUnlocks unlocks,
                        ym.starforge.workstation.StationOrigins origins,ym.starforge.workstation.StationAnimations animations) {
        stationOrigins=origins;stationAnimations=animations;
        this.plugin = plugin; this.config = config; this.texts = texts; this.unlocks = unlocks;
        transaction = new CraftingTransaction(plugin, orders, texts, unlocks);
        delivery = new CraftingDelivery(plugin, orders, texts);
        renderer = new CraftingMenuRenderer(plugin, orders, texts, unlocks);
    }
    public void open(Player player) {
        var origin=stationOrigins==null?null:stationOrigins.take(player,ym.starforge.workstation.StationAnchor.Kind.CRAFTING);
        show(player, CraftingScreen.CATEGORIES, CraftingScreen.CATEGORIES, null, null, origin, CraftingCategories.DEFAULT);
    }

    private void show(Player player, CraftingScreen screen, CraftingScreen back, String recipe, String job) {
        var previous=sessions.get(player.getUniqueId());
        show(player,screen,back,recipe,job,previous==null?null:previous.station,
            previous==null?CraftingCategories.DEFAULT:previous.category);
    }
    private void show(Player player, CraftingScreen screen, CraftingScreen back, String recipe, String job,
                      ym.starforge.workstation.StationAnchor station, String category) {
        if (!player.hasPermission("starforge.use")) { texts.send(player, "no-permission"); return; }
        var cfg = config.get();
        if (cfg == null || !plugin.isEnabled() || !player.isOnline()) { texts.send(player, "crafting-unavailable"); return; }
        var h = new CraftingSession(cfg, screen, back, recipe, job, category, player); var y = cfg.menu(screen);
        h.station=station;
        h.inventory = Bukkit.createInventory(h, y.getStringList("shape").size() * 9,
                renderer.title(player, h));
        renderer.paint(player, h);
        // Opening fires the previous inventory's close event synchronously. Register afterwards.
        player.openInventory(h.inventory);
        if (player.getOpenInventory().getTopInventory() != h.inventory) return;
        if (config.get() != cfg) { player.closeInventory(); return; }
        sessions.put(player.getUniqueId(), h);
        h.task = player.getScheduler().runAtFixedRate(plugin, task -> {
            if (!valid(player, h)) {
                task.cancel();
                if (config.get() != h.revision && player.getOpenInventory().getTopInventory() == h.inventory)
                    player.closeInventory();
                sessions.remove(player.getUniqueId(), h);
                return;
            }
            renderer.paint(player, h);
        }, () -> sessions.remove(player.getUniqueId(), h), 20, 20);
    }
    private boolean valid(Player player, CraftingSession h) {
        return plugin.isEnabled() && player.isOnline() && player.hasPermission("starforge.use")
                && sessions.get(player.getUniqueId()) == h && config.get() == h.revision
                && player.getOpenInventory().getTopInventory() == h.inventory;
    }
    private void navigate(Player player, CraftingSession h, CraftingScreen screen, CraftingScreen back, String recipe, String job) {
        player.getScheduler().execute(plugin, () -> { if (valid(player, h)) show(player, screen, back, recipe, job); }, null, 1);
    }
    @EventHandler public void click(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof CraftingSession h)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player p) || !valid(p, h) || event.getClick() != ClickType.LEFT) return;
        int slot = event.getRawSlot();
        if (h.jobs.containsKey(slot)) {
            var job = h.jobs.get(slot);
            if (h.screen == CraftingScreen.ORDERS) delivery.claim(p, job, () -> valid(p, h),item->claimed(p,h,item), () -> {
                if (valid(p, h)) show(p, CraftingScreen.ORDERS, h.back, null, null);
            });
            else navigate(p, h, CraftingScreen.DETAIL, h.screen, job.recipe(), job.key());
            return;
        }
        if (h.recipes.containsKey(slot)) {
            navigate(p, h, CraftingScreen.DETAIL, CraftingScreen.RECIPES, h.recipes.get(slot), null); return;
        }
        String category = h.screen == CraftingScreen.CATEGORIES ? h.revision.categoryAt(slot) : null;
        if (category != null) {
            p.getScheduler().execute(plugin, () -> {
                if (valid(p, h)) show(p, CraftingScreen.RECIPES, CraftingScreen.CATEGORIES, null, null, h.station, category);
            }, null, 1);
            return;
        }
        switch (h.actions.getOrDefault(slot, "none")) {
            case "close" -> p.getScheduler().execute(plugin, () -> { if (valid(p, h)) p.closeInventory(); }, null, 1);
            case "previous" -> { h.page = Math.max(0, h.page - 1); renderer.paint(p, h); }
            case "next" -> { h.page++; renderer.paint(p, h); }
            case "orders" -> navigate(p, h, CraftingScreen.ORDERS, h.screen, null, null);
            case "back" -> navigate(p, h, h.back,
                    h.back == CraftingScreen.RECIPES ? CraftingScreen.CATEGORIES : CraftingScreen.RECIPES, null, null);
            case "confirm" -> {
                var job = renderer.job(p, h);
                // A completed claim must not turn an old 'claim' click into a new paid craft.
                if (!java.util.Objects.equals(job, h.displayedJob)) { renderer.paint(p, h); return; }
                if (job != null) delivery.claim(p, job, () -> valid(p, h),item->claimed(p,h,item), () -> renderer.paint(p, h));
                else if (h.jobKey == null) {
                    var recipe = renderer.recipe(h);
                    if (recipe == null) { texts.send(p, "crafting-unavailable"); return; }
                    if (!unlocks.unlocked(p.getUniqueId(), recipe)) { texts.send(p, RecipeUnlocks.requirementKey(recipe)); return; }
                    transaction.execute(p, recipe, h.revision.maxActiveOrders(), h.station, () -> valid(p, h), () -> renderer.paint(p, h),
                            () -> show(p, CraftingScreen.ORDERS, CraftingScreen.RECIPES, null, null));
                }
            }
        }
    }
    @EventHandler public void drag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof CraftingSession) event.setCancelled(true);
    }
    private void claimed(Player player,CraftingSession h,org.bukkit.inventory.ItemStack item) {
        if(valid(player,h)&&stationAnimations!=null&&stationAnimations.play(player,h.station,item))player.closeInventory();
    }
    @EventHandler public void closed(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof CraftingSession h)) return;
        h.cancel();
        sessions.remove(event.getPlayer().getUniqueId(), h);
    }
    public void close() {
        retire(null);
    }
    /** Close only retired snapshots after publishing configuration on the global scheduler. */
    public void retire(CraftingConfig current) {
        sessions.forEach((id, h) -> {
            if (h.revision == current || !sessions.remove(id, h)) return;
            var p = h.viewer;
            p.getScheduler().execute(plugin, () -> {
                h.cancel();
                if (p.getOpenInventory().getTopInventory() == h.inventory) p.closeInventory();
            }, null, 1);
        });
    }
}
