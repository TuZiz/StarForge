package ym.starforge.crafting;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.util.Key;
import ym.itemhub.api.ItemHubApi;
import java.util.Objects;

/** All methods require the viewing player's ownership thread. */
public final class CraftingItems {
    public boolean matches(ItemStack item, CraftingRecipe.Ingredient spec) {
        if (item == null || item.getType().isAir()) return false;
        return switch (spec.source()) {
            case "vanilla" -> item.getType() == Material.matchMaterial(spec.id()) && !item.hasItemMeta();
            case "craft-engine" -> Bukkit.getPluginManager().isPluginEnabled("CraftEngine")
                    && Key.of(spec.id()).equals(CraftEngineItems.getCustomItemId(item));
            case "itemhub" -> hub().isItem(item, spec.id());
            default -> throw new IllegalArgumentException(spec.source());
        };
    }
    public ItemStack create(CraftingRecipe.Ingredient spec) {
        return switch (spec.source()) {
            case "vanilla" -> new ItemStack(Objects.requireNonNull(Material.matchMaterial(spec.id())));
            case "craft-engine" -> {
                if (!Bukkit.getPluginManager().isPluginEnabled("CraftEngine")) throw new IllegalStateException("CraftEngine unavailable");
                yield Objects.requireNonNull(CraftEngineItems.byId(Key.of(spec.id())), "CE item " + spec.id()).buildBukkitItem();
            }
            case "itemhub" -> Objects.requireNonNull(hub().createItem(spec.id(), 1), "ItemHub item " + spec.id());
            default -> throw new IllegalArgumentException(spec.source());
        };
    }
    private ItemHubApi hub() {
        if (!Bukkit.getPluginManager().isPluginEnabled("ItemHub")) throw new IllegalStateException("ItemHub unavailable");
        var api = Objects.requireNonNull(Bukkit.getServicesManager().load(ItemHubApi.class), "ItemHub API unavailable");
        if (!api.isReady()) throw new IllegalStateException("ItemHub not ready");
        return api;
    }
}
