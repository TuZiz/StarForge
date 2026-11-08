package ym.starforge.crafting;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import net.milkbowl.vault.economy.Economy;
import ym.starforge.text.Texts;
import ym.starforge.transaction.ForgeTransaction;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;

/** Prepare and apply on the player's scheduler, persist reservations on the store's writer. */
public final class CraftingTransaction {
    private final Plugin plugin;
    private final CraftingOrders cooldowns;
    private final Texts texts;
    private final CraftingItems items = new CraftingItems();
    private final Set<UUID> busy = ConcurrentHashMap.newKeySet();
    public CraftingTransaction(Plugin plugin, CraftingOrders cooldowns, Texts texts) {
        this.plugin = plugin; this.cooldowns = cooldowns; this.texts = texts;
    }
    public void execute(Player player, CraftingRecipe recipe, BooleanSupplier valid, Runnable repaint) {
        UUID id = player.getUniqueId();
        if (!player.hasPermission("starforge.use") || !busy.add(id)) return;
        try {
            if (cooldowns.job(id, recipe) != null) { texts.send(player, "crafting-cooldown"); busy.remove(id); return; }
            ItemStack[] before = ForgeTransaction.cloneContents(player.getInventory().getStorageContents());
            ItemStack[] after = ForgeTransaction.cloneContents(before);
            for (var ingredient : recipe.ingredients()) {
                int left = ingredient.amount();
                for (int slot = 0; slot < after.length && left > 0; slot++) {
                    if (!items.matches(after[slot], ingredient)) continue;
                    int take = Math.min(left, after[slot].getAmount());
                    left -= take; after[slot].subtract(take);
                    if (after[slot].getAmount() == 0) after[slot] = null;
                }
                if (left > 0) { texts.send(player, "materials-missing"); busy.remove(id); return; }
            }
            ItemStack result = items.create(recipe.result());

            Economy economy = recipe.money() == 0 ? null : Bukkit.getServicesManager().load(Economy.class);
            if (recipe.money() > 0 && economy == null) { texts.send(player, "economy-missing"); busy.remove(id); return; }
            cooldowns.reserve(id, recipe, Base64.getEncoder().encodeToString(result.serializeAsBytes())).whenComplete((unused, error) -> {
                if (error != null) {
                    plugin.getLogger().log(Level.SEVERE, "Crafting reservation failed player=" + id + " recipe=" + recipe.id(), error);
                    busy.remove(id);
                    if (plugin.isEnabled()) player.getScheduler().execute(plugin, () -> { if (player.isOnline()) texts.send(player, "error"); }, null, 1);
                    return;
                }
                if (!plugin.isEnabled()) { busy.remove(id); return; }
                boolean scheduled = player.getScheduler().execute(plugin, () -> apply(player, recipe, valid, repaint, before, after, economy),
                        () -> release(id, recipe), 1);
                if (!scheduled) release(id, recipe);
            });
        } catch (Exception ex) {
            busy.remove(id); texts.send(player, "error");
            plugin.getLogger().log(Level.SEVERE, "Crafting prepare failed player=" + id + " recipe=" + recipe.id(), ex);
        }
    }
    private void apply(Player player, CraftingRecipe recipe, BooleanSupplier valid, Runnable repaint,
                       ItemStack[] before, ItemStack[] after, Economy economy) {
        UUID id = player.getUniqueId(); boolean paid = false, writing = false;
        try {
            if (!valid.getAsBoolean() || !player.isOnline() || !player.hasPermission("starforge.use")
                    || !Arrays.equals(before, player.getInventory().getStorageContents())) { release(id, recipe); return; }
            if (recipe.money() > 0) {
                var response = economy.withdrawPlayer(player, recipe.money());
                if (!response.transactionSuccess()) { texts.send(player, "money-missing"); release(id, recipe); return; }
                paid = true;
            }
            if (!plugin.isEnabled() || !valid.getAsBoolean() || !player.isOnline() || !player.hasPermission("starforge.use")
                    || !Arrays.equals(before, player.getInventory().getStorageContents())) throw new IllegalStateException("Crafting state changed during economy call");
            writing = true;
            player.getInventory().setStorageContents(after);
        } catch (Exception ex) {
            if (writing) try { player.getInventory().setStorageContents(before); }
            catch (Exception rollback) { plugin.getLogger().log(Level.SEVERE, "Crafting inventory rollback failed player=" + id, rollback); }
            if (paid) try {
                var refund = economy.depositPlayer(player, recipe.money());
                if (!refund.transactionSuccess()) throw new IllegalStateException(refund.errorMessage);
            } catch (Exception refund) { plugin.getLogger().log(Level.SEVERE, "Crafting refund failed player=" + id + " recipe=" + recipe.id(), refund); }
            release(id, recipe);
            plugin.getLogger().log(Level.SEVERE, "Crafting apply failed player=" + id + " recipe=" + recipe.id(), ex);
            texts.send(player, "error"); return;
        }
        cooldowns.activate(id, recipe).whenComplete((v, error) -> {
            busy.remove(id);
            if (error != null) plugin.getLogger().log(Level.SEVERE, "CRAFT_ACTIVATE_FAILED player=" + id + " recipe=" + recipe.id() + "; paid inputs retained in prepared order", error);
            if (plugin.isEnabled()) player.getScheduler().execute(plugin, () -> {
                if (!player.isOnline()) return;
                texts.send(player, error == null ? "crafting-success" : "error");
                if (valid.getAsBoolean()) repaint.run();
            }, null, 1);
        });
    }
    private void release(UUID id, CraftingRecipe recipe) {
        cooldowns.release(id, recipe).whenComplete((v, error) -> {
            if (error != null) plugin.getLogger().log(Level.SEVERE, "Crafting cooldown rollback failed player=" + id + " recipe=" + recipe.id(), error);
            busy.remove(id);
        });
    }
    static boolean insert(ItemStack[] contents, ItemStack template, int amount) {
        int left = amount;
        for (ItemStack stack : contents) {
            if (stack == null || !stack.isSimilar(template)) continue;
            int add = Math.min(left, Math.max(0, stack.getMaxStackSize() - stack.getAmount()));
            stack.add(add); left -= add;
        }
        for (int i = 0; i < contents.length && left > 0; i++) {
            if (contents[i] != null && !contents[i].getType().isAir()) continue;
            var stack = template.clone(); int add = Math.min(left, stack.getMaxStackSize());
            stack.setAmount(add); contents[i] = stack; left -= add;
        }
        return left == 0;
    }
}
