package ym.starforge.crafting;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import ym.starforge.integration.MenuArt;
import ym.starforge.text.Texts;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/** Six workstation presentation; unclaimed result icons conceal the immutable order's roll. */
final class CraftingOrderMenuRenderer {
    private final Plugin plugin;
    private final CraftingOrders orders;
    private final Texts texts;
    private final CraftingItems items = new CraftingItems();
    private final Set<String> reported = ConcurrentHashMap.newKeySet();
    CraftingOrderMenuRenderer(Plugin plugin, CraftingOrders orders, Texts texts) {
        this.plugin = plugin; this.orders = orders; this.texts = texts;
    }
    Component title(Player player, CraftingSession h) {
        var y = h.revision.menu(h.screen);
        return MenuArt.title(Texts.parse(y.getString("title"), Map.of("used", "" + orders.count(player.getUniqueId()),
                "limit", "" + h.revision.maxActiveOrders())), y.getString("background-image"));
    }
    void paint(Player player, CraftingSession h) {
        var y = h.revision.menu(h.screen); var jobs = orders.jobs(player.getUniqueId());
        var slots = CraftingOrderLayout.slots(y, "order");
        var cells = CraftingOrderLayout.slots(y, "progress").isEmpty()
                ? slots.stream().map(s -> new CraftingOrderLayout.Cell(s, -1)).toList() : CraftingOrderLayout.cells(y);
        int pages = Math.max(1, (jobs.size() + cells.size() - 1) / cells.size());
        h.page = Math.max(0, Math.min(h.page, pages - 1));
        var common = Map.of("page", "" + (h.page + 1), "pages", "" + pages,
                "used", "" + jobs.size(), "limit", "" + h.revision.maxActiveOrders());
        h.inventory.clear(); h.jobs.clear(); h.recipes.clear(); h.actions.clear();
        for (int i = 0; i < cells.size(); i++) {
            var cell = cells.get(i); int index = h.page * cells.size() + i;
            CraftingJob job = index < jobs.size() ? jobs.get(index) : null;
            boolean locked = index >= h.revision.maxActiveOrders();
            var e = y.getConfigurationSection("elements." + CraftingOrderLayout.symbol(y, cell.itemSlot()));
            render(h, cell.itemSlot(), () -> {
                if (job == null) {
                    if (!e.contains("empty-craft-engine")) return null;
                    var item = CraftingGuiIcons.ce(e.getString(locked ? "locked-craft-engine" : "empty-craft-engine"));
                    item.editMeta(m -> m.displayName(Texts.parse(e.getString(locked ? "locked-name" : "empty-name", ""), common)));
                    return item;
                }
                h.jobs.put(cell.itemSlot(), job);
                var item = items.orderPreview(job);
                CraftingMenuRenderer.decorate(item, e.getStringList("lore"), values(job)); return item;
            });
            if (cell.progressSlot() >= 0) {
                var bar = y.getConfigurationSection("elements." + CraftingOrderLayout.symbol(y, cell.progressSlot()));
                render(h, cell.progressSlot(), () -> {
                    String id = job == null ? bar.getString(locked ? "locked-craft-engine" : "empty-craft-engine")
                            : !job.state().equals("running") ? bar.getString("pending-craft-engine")
                            : bar.getString("craft-engine-prefix") + String.format(Locale.ROOT, "%03d", job.progress());
                    var item = CraftingGuiIcons.ce(id);
                    String nameKey = job == null ? locked ? "locked-name" : "empty-name"
                            : !job.state().equals("running") ? "pending-name" : "name";
                    item.editMeta(m -> m.displayName(Texts.parse(bar.getString(nameKey, ""), job == null ? common : values(job))));
                    if (job != null) {
                        h.jobs.put(cell.progressSlot(), job);
                        CraftingMenuRenderer.decorate(item, bar.getStringList("lore"), values(job));
                    }
                    return item;
                });
            }
            if (job != null && h.jobs.containsKey(cell.itemSlot()) && cell.progressSlot() >= 0)
                for (int center : List.of(cell.itemSlot(), cell.progressSlot())) for (int delta : List.of(-1, 1)) {
                    int neighbor = center + delta;
                    if (neighbor / 9 == center / 9 && "none".equals(y.getString("elements."
                            + CraftingOrderLayout.symbol(y, neighbor) + ".action", "none"))) h.jobs.putIfAbsent(neighbor, job);
                }
        }
        int slot = 0;
        for (String row : y.getStringList("shape")) for (char symbol : row.toCharArray()) {
            int target = slot++; var e = y.getConfigurationSection("elements." + symbol);
            if (e == null) continue;
            String action = e.getString("action", "none");
            if (Set.of("none", "order", "progress").contains(action)) continue;
            if (!navigationVisible(action, h.page, pages)) continue;
            h.actions.put(target, action);
            render(h, target, () -> {
                String pageIcon = action.equals("info") && e.contains("page-craft-engine-prefix")
                        ? pageIcon(e.getString("page-craft-engine-prefix"), e.getString("overflow-craft-engine"), h.page, pages) : null;
                var item = pageIcon == null ? CraftingGuiIcons.create(e) : CraftingGuiIcons.ce(pageIcon);
                if (!e.contains("craft-engine") || e.contains("name")) item.editMeta(m -> m.displayName(Texts.parse(e.getString("name", ""), common)));
                CraftingMenuRenderer.decorate(item, e.getStringList("lore"), common); return item;
            });
        }
    }
    static boolean navigationVisible(String action, int page, int pages) {
        return switch (action) {
            case "previous" -> page > 0;
            case "next" -> page + 1 < pages;
            default -> true;
        };
    }
    static String pageIcon(String prefix, String overflow, int page, int pages) {
        return pages <= 9 ? prefix + (page + 1) + "_" + pages : overflow;
    }
    private Map<String, String> values(CraftingJob job) {
        int progress = job.progress();
        String key = !job.state().equals("running") ? "crafting-pending" : job.remaining() == 0 ? "crafting-ready" : "crafting-running";
        return Map.of("state", net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(texts.get(key, Map.of())),
                "progress", "" + progress, "seconds", "" + job.remaining(), "amount", "" + job.amount(),
                "bar", "|".repeat(progress / 10) + "-".repeat(10 - progress / 10));
    }
    private void render(CraftingSession h, int slot, java.util.function.Supplier<ItemStack> factory) {
        try { h.inventory.setItem(slot, factory.get()); }
        catch (Exception ex) {
            h.jobs.remove(slot); h.actions.remove(slot);
            String context = "gui/crafting-orders.yml slot=" + slot + " reason=" + ex.getMessage();
            if (reported.add(context)) plugin.getLogger().log(Level.WARNING, "Crafting order display unavailable " + context, ex);
            var barrier = new ItemStack(Material.BARRIER);
            barrier.editMeta(m -> m.displayName(texts.get("crafting-unavailable", Map.of()))); h.inventory.setItem(slot, barrier);
        }
    }
}
