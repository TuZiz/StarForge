package ym.starforge.crafting;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import java.util.HashMap;
import java.util.Map;

/** Mutable only on this viewer's ownership thread; close removes by session identity. */
final class CraftingSession implements InventoryHolder {
    final CraftingConfig revision;
    final CraftingScreen screen;
    final CraftingScreen back;
    final String recipeId;
    final String jobKey;
    final String category;
    final Player viewer;
    final Map<Integer, String> actions = new HashMap<>();
    final Map<Integer, String> recipes = new HashMap<>();
    final Map<Integer, CraftingJob> jobs = new HashMap<>();
    Inventory inventory;
    ScheduledTask task;
    CraftingJob displayedJob;
    int page;
    ym.starforge.workstation.StationAnchor station;

    CraftingSession(CraftingConfig revision, CraftingScreen screen, CraftingScreen back, String recipeId, String jobKey) {
        this(revision, screen, back, recipeId, jobKey, CraftingCategories.DEFAULT, null);
    }
    CraftingSession(CraftingConfig revision, CraftingScreen screen, CraftingScreen back,
                    String recipeId, String jobKey, String category, Player viewer) {
        this.revision = revision; this.screen = screen; this.back = back; this.recipeId = recipeId; this.jobKey = jobKey;
        this.category = category;
        this.viewer = viewer;
    }
    public Inventory getInventory() { return inventory; }
    void cancel() { if (task != null) { task.cancel(); task = null; } }
}
