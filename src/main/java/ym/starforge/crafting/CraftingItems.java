package ym.starforge.crafting;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.util.Key;
import ym.itemhub.api.ItemHubApi;
import java.util.Objects;
import java.util.Base64;
import org.bukkit.NamespacedKey;

/** All methods require the viewing player's ownership thread. */
public final class CraftingItems {
    /** GUI view only. All unclaimed quality orders conceal the saved roll, including ready orders. */
    public ItemStack orderPreview(CraftingJob job) {
        var saved=ItemStack.deserializeBytes(Base64.getDecoder().decode(job.itemBytes()));
        if(!saved.getPersistentDataContainer().has(new NamespacedKey("asterstats","quality_roll"))
                && !saved.getPersistentDataContainer().has(new NamespacedKey("itemhub","quality_roll"))) {
            saved.setAmount(Math.min(job.amount(),saved.getMaxStackSize()));
            return saved;
        }
        var id=Objects.requireNonNull(hub().identify(saved),"ItemHub definition removed for quality order "+job.key());
        var quality=Objects.requireNonNull(Bukkit.getServicesManager().load(ym.itemhub.api.ItemHubGenerationApi.class),
                "ItemHub quality API unavailable for order "+job.key());
        if(!quality.hasVariants(id))throw new IllegalStateException("Quality profile removed for order "+job.key());
        var preview=Objects.requireNonNull(quality.create(id,ym.itemhub.api.ItemGenerationMode.PREVIEW),"ItemHub preview unavailable for order "+job.key());
        // Order menus append their own progress/claim Lore; do not expose numbers or a tier list here.
        preview.editMeta(meta -> meta.lore(null));
        preview.setAmount(Math.min(job.amount(),preview.getMaxStackSize()));
        return preview;
    }
    /** Display is deterministic and never draws a random roll. */
    public ItemStack preview(CraftingRecipe.Ingredient spec) {
        if(!spec.source().equals("itemhub"))return create(spec);
        var quality=Bukkit.getServicesManager().load(ym.itemhub.api.ItemHubGenerationApi.class);
        if(quality==null)return create(spec);
        return Objects.requireNonNull(quality.create(spec.id(),ym.itemhub.api.ItemGenerationMode.PREVIEW),"ItemHub preview "+spec.id());
    }
    /** Called once during transaction preparation; the returned stack is serialized into the order. */
    public ItemStack createResult(CraftingRecipe.Ingredient spec) {
        if(!spec.source().equals("itemhub"))return create(spec);
        hub();
        var quality=Objects.requireNonNull(Bukkit.getServicesManager().load(ym.itemhub.api.ItemHubGenerationApi.class),
                "ItemHub generation API missing; install matching updates");
        if(quality.hasVariants(spec.id())&&spec.amount()!=1)
            throw new IllegalArgumentException("Random quality recipe result must have amount=1: "+spec.id());
        return Objects.requireNonNull(quality.create(spec.id(),ym.itemhub.api.ItemGenerationMode.RANDOM),"ItemHub result "+spec.id());
    }
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
