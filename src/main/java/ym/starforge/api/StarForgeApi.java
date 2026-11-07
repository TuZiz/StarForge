package ym.starforge.api;

import org.bukkit.inventory.ItemStack;
import java.util.Map;

/** ServicesManager service. Live stacks must be read on their entity owner thread.
 * Pure calculation: contains additional values only, never applies combat damage itself. */
public interface StarForgeApi {
    int level(ItemStack item);
    int failures(ItemStack item);
    Map<String, Double> contribution(ItemStack item, Map<String, Double> latestBase);
    /** Detached, idempotent rebuild. No inventory writes or economy operations. */
    ItemStack refresh(ItemStack item);
    /** Pure original-item view, excludes only precisely recorded StarForge presentation and modifiers. */
    default ItemStack baseItem(ItemStack item){return item.clone();}
    default boolean canEnhance(ItemStack item){return false;}
    default int maxLevel(ItemStack item){return 0;}
    /** Returns a detached copy. Caller applies it on the item's ownership thread. Blacklists still win. */
    default ItemStack markWeapon(ItemStack item,boolean enabled){throw new UnsupportedOperationException("Weapon marking is unavailable");}
}
