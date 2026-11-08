package ym.starforge.crafting;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.Plugin;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import ym.starforge.integration.MenuArt;
import ym.starforge.integration.MaterialItems;
import ym.starforge.text.Texts;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Inventory is a read-only view. All interactions and refreshes use the viewer's ownership thread. */
public final class CraftingMenu implements Listener, AutoCloseable {
    private final Plugin plugin;
    private final Supplier<CraftingConfig> config;
    private final CraftingOrders orders;
    private final CraftingTransaction transaction;
    private final CraftingDelivery delivery;
    private final Texts texts;
    private final CraftingItems items = new CraftingItems();
    private final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();
    private final Map<UUID, Player> viewers = new ConcurrentHashMap<>();
    private final Set<String> reported = ConcurrentHashMap.newKeySet();
    public CraftingMenu(Plugin plugin, Supplier<CraftingConfig> config, CraftingOrders orders, Texts texts) {
        this.plugin = plugin; this.config = config; this.orders = orders; this.texts = texts;
        transaction = new CraftingTransaction(plugin, orders, texts); delivery = new CraftingDelivery(plugin, orders, texts);
    }
    private static final class Holder implements InventoryHolder {
        final CraftingConfig revision;
        final Map<Integer, String> actions = new HashMap<>();
        final Map<Integer, Integer> recipes = new HashMap<>();
        final Map<Integer, CraftingJob> jobs = new HashMap<>();
        Inventory inventory; int selected, page; boolean progress;
        Holder(CraftingConfig revision) { this.revision = revision; }
        public Inventory getInventory() { return inventory; }
    }
    public void open(Player player) {
        if (!player.hasPermission("starforge.use")) { texts.send(player, "no-permission"); return; }
        var cfg = config.get();
        if (cfg == null) { texts.send(player, "crafting-unavailable"); return; }
        var h = new Holder(cfg); var y = cfg.menu();
        h.inventory = Bukkit.createInventory(h, y.getStringList("shape").size() * 9,
                MenuArt.title(Texts.parse(y.getString("title"), Map.of()), y.getString("background-image")));
        paint(player, h); player.openInventory(h.inventory); viewers.put(player.getUniqueId(), player);
        var task = player.getScheduler().runAtFixedRate(plugin, t -> {
            if (!valid(player, h)) { t.cancel(); tasks.remove(player.getUniqueId(), t); return; }
            paint(player, h);
        }, null, 20, 20);
        if (task != null) tasks.put(player.getUniqueId(), task);
    }
    private boolean valid(Player player, Holder h) {
        return plugin.isEnabled() && player.isOnline() && player.hasPermission("starforge.use")
                && config.get() == h.revision && player.getOpenInventory().getTopInventory() == h.inventory;
    }
    private void paint(Player player, Holder h) {
        h.inventory.clear(); h.actions.clear(); h.recipes.clear(); h.jobs.clear();
        var y = h.revision.menu(); var recipes = h.revision.recipes(); var jobs = orders.jobs(player.getUniqueId());
        int slots = (int)y.getStringList("shape").stream().flatMapToInt(String::chars)
                .filter(c -> "recipe".equals(y.getString("elements." + (char)c + ".action"))).count();
        int count = h.progress ? jobs.size() : recipes.size();
        h.page = Math.min(h.page, Math.max(0, (count - 1) / slots));
        int index = h.page * slots, slot = 0;
        CraftingRecipe selected = recipes.isEmpty() ? null : recipes.get(Math.min(h.selected, recipes.size() - 1));
        for (String row : y.getStringList("shape")) for (char symbol : row.toCharArray()) {
            var e = y.getConfigurationSection("elements." + symbol);
            int target = slot++;
            if (e == null) continue;
            String action = e.getString("action", "none"); h.actions.put(target, action);
            try {
                if (action.equals("recipe")) {
                    if (index >= count) continue;
                    if (h.progress) {
                        var job = jobs.get(index++); h.jobs.put(target, job);
                        var item = ItemStack.deserializeBytes(Base64.getDecoder().decode(job.itemBytes()));
                        decorate(item, e.getStringList("lore"), values(player, null, job)); h.inventory.setItem(target, item);
                    } else {
                        int recipeIndex = index++; var recipe = recipes.get(recipeIndex); h.recipes.put(target, recipeIndex);
                        var item = items.create(recipe.result());
                        decorate(item, e.getStringList("lore"), values(player, recipe, orders.job(player.getUniqueId(), recipe)));
                        h.inventory.setItem(target, item);
                    }
                } else if (action.equals("result") && selected != null) {
                    var item = items.create(selected.result());
                    var lines = new ArrayList<>(e.getStringList("lore"));
                    for (var input : selected.ingredients()) {
                        int owned = Arrays.stream(player.getInventory().getStorageContents()).filter(i -> items.matches(i, input)).mapToInt(ItemStack::getAmount).sum();
                        var vars = new HashMap<>(values(player, selected, orders.job(player.getUniqueId(), selected)));
                        var material = items.create(input); var meta = material.getItemMeta();
                        var name = meta.hasDisplayName() ? meta.displayName() : net.kyori.adventure.text.Component.translatable(material.getType().translationKey());
                        vars.put("material", net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(name));
                        vars.put("owned", "" + owned); vars.put("required", "" + input.amount());
                        lines.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().serialize(Texts.parse(y.getString("ingredient-line"), vars)));
                    }
                    decorate(item, lines, values(player, selected, orders.job(player.getUniqueId(), selected))); h.inventory.setItem(target, item);
                } else if (!action.equals("none") && !action.equals("result")) {
                    var item = MaterialItems.blankButton();
                    if (item.getType().isAir()) item = new ItemStack(Material.PAPER);
                    var vars = values(player, selected, selected == null ? null : orders.job(player.getUniqueId(), selected));
                    item.editMeta(m -> m.displayName(Texts.parse(e.getString("name", ""), vars)));
                    decorate(item, e.getStringList("lore"), vars); h.inventory.setItem(target, item);
                }
            } catch (Exception ex) {
                if (reported.add(symbol + ":" + ex.getMessage())) plugin.getLogger().log(java.util.logging.Level.WARNING,
                        "Crafting menu item unavailable player=" + player.getUniqueId() + " element=" + symbol, ex);
                var item = new ItemStack(Material.BARRIER); item.editMeta(m -> m.displayName(texts.get("crafting-unavailable", Map.of())));
                h.inventory.setItem(target, item);
                // Item source may be temporarily unavailable; no crafting is performed by painting.
            }
        }
    }
    private Map<String, String> values(Player player, CraftingRecipe recipe, CraftingJob job) {
        return Map.of("money", recipe == null ? "0" : "" + recipe.money(), "seconds", "" + (job == null ? 0 : job.remaining()),
                "progress", "" + (job == null ? 0 : job.progress()), "state", net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(
                        texts.get(job == null ? "crafting-idle" : !job.state().equals("running") ? "crafting-pending" : job.remaining() == 0 ? "crafting-ready" : "crafting-running", Map.of())));
    }
    private void decorate(ItemStack item, List<String> lines, Map<String, String> values) {
        item.editMeta(meta -> {
            var lore = meta.lore() == null ? new ArrayList<net.kyori.adventure.text.Component>() : new ArrayList<>(meta.lore());
            lines.forEach(line -> lore.add(Texts.parse(line, values)));
            meta.lore(lore.stream().map(Texts::plainStyle).toList());
            if (meta.hasDisplayName()) meta.displayName(Texts.plainStyle(meta.displayName()));
            if (meta.hasItemName()) meta.itemName(Texts.plainStyle(meta.itemName()));
        });
    }
    @EventHandler public void click(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder h)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player p) || !valid(p, h) || event.getClick() != ClickType.LEFT) return;
        int slot = event.getRawSlot();
        if (h.jobs.containsKey(slot)) { delivery.claim(p, h.jobs.get(slot), () -> valid(p, h), () -> paint(p, h)); return; }
        if (h.recipes.containsKey(slot)) { h.selected = h.recipes.get(slot); paint(p, h); return; }
        switch (h.actions.getOrDefault(slot, "none")) {
            case "close" -> p.closeInventory();
            case "previous" -> { h.page = Math.max(0, h.page - 1); paint(p, h); }
            case "next" -> { h.page++; paint(p, h); }
            case "progress" -> { h.progress = !h.progress; h.page = 0; paint(p, h); }
            case "confirm" -> {
                if (h.revision.recipes().isEmpty()) { texts.send(p, "crafting-empty"); return; }
                var recipe = h.revision.recipes().get(h.selected); var job = orders.job(p.getUniqueId(), recipe);
                if (job != null) delivery.claim(p, job, () -> valid(p, h), () -> paint(p, h));
                else transaction.execute(p, recipe, () -> valid(p, h), () -> paint(p, h));
            }
        }
    }
    @EventHandler public void drag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) event.setCancelled(true);
    }
    @EventHandler public void closed(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder)) return;
        var task = tasks.remove(event.getPlayer().getUniqueId()); if (task != null) task.cancel();
        viewers.remove(event.getPlayer().getUniqueId());
    }
    public void close() {
        tasks.values().forEach(ScheduledTask::cancel); tasks.clear();
        viewers.values().forEach(p -> p.getScheduler().execute(plugin, () -> {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof Holder) p.closeInventory();
        }, null, 1)); viewers.clear();
    }
}
