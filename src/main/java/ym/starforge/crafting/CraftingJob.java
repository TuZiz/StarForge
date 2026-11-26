package ym.starforge.crafting;

import ym.starforge.workstation.StationAnchor;

public record CraftingJob(String key, String recipe, long started, long ready, String itemBytes, int amount,
                          String state, StationAnchor station) {
    public CraftingJob(String key, String recipe, long started, long ready, String itemBytes, int amount, String state) {
        this(key, recipe, started, ready, itemBytes, amount, state, null);
    }
    public long remaining() { return Math.max(0, (ready - System.currentTimeMillis() + 999) / 1000); }
    public int progress() {
        if (ready <= started) return 100;
        return (int)Math.max(0, Math.min(100, (System.currentTimeMillis() - started) * 100 / (ready - started)));
    }
    public CraftingJob state(String next) { return new CraftingJob(key, recipe, started, ready, itemBytes, amount, next, station); }
}
