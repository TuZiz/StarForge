package ym.starforge.crafting;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.inventory.ItemStack;
import ym.starforge.transaction.ForgeTransaction;
import ym.starforge.text.Texts;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;

/** Delivery runs only on the recipient's ownership thread. Durable claiming prevents double clicks. */
public final class CraftingDelivery {
    private final Plugin plugin;
    private final CraftingOrders orders;
    private final Texts texts;
    private final Set<UUID> busy = ConcurrentHashMap.newKeySet();
    public CraftingDelivery(Plugin plugin, CraftingOrders orders, Texts texts) {
        this.plugin = plugin; this.orders = orders; this.texts = texts;
    }
    public void claim(Player player, CraftingJob job, BooleanSupplier valid, Runnable repaint) {
        UUID id = player.getUniqueId();
        if (!job.key().startsWith(id + "/") || !player.hasPermission("starforge.use") || !busy.add(id)) return;
        if (!job.state().equals("running") || job.remaining() > 0) { busy.remove(id); return; }
        orders.beginClaim(job).whenComplete((reserved, error) -> {
            if (error != null) { report(id, error); busy.remove(id); return; }
            if (!plugin.isEnabled()) { busy.remove(id); return; }
            boolean scheduled = player.getScheduler().execute(plugin, () -> deliver(player, job, valid, repaint), () -> restore(id, job), 1);
            if (!scheduled) restore(id, job);
        });
    }
    private void deliver(Player player, CraftingJob job, BooleanSupplier valid, Runnable repaint) {
        UUID id = player.getUniqueId();
        if (!plugin.isEnabled() || !player.isOnline() || !player.hasPermission("starforge.use") || !valid.getAsBoolean()) { restore(id, job); return; }
        try {
            var after = ForgeTransaction.cloneContents(player.getInventory().getStorageContents());
            var item = ItemStack.deserializeBytes(Base64.getDecoder().decode(job.itemBytes()));
            if (!CraftingTransaction.insert(after, item, job.amount())) { texts.send(player, "crafting-space"); restore(id, job); return; }
            // Once applying begins, uncertain failures retain claiming for operator inspection.
            player.getInventory().setStorageContents(after);
            orders.complete(job).whenComplete((v, error) -> { if (error != null) report(id, error); busy.remove(id); });
            texts.send(player, "crafting-claimed"); repaint.run();
        } catch (Exception ex) { report(id, ex); busy.remove(id); texts.send(player, "error"); }
    }
    private void restore(UUID id, CraftingJob job) {
        orders.restore(job).whenComplete((v, error) -> { if (error != null) report(id, error); busy.remove(id); });
    }
    private void report(UUID id, Throwable error) { plugin.getLogger().log(Level.SEVERE, "Crafting delivery failed player=" + id, error); }
}
