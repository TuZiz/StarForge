package ym.starforge.workstation;

import org.bukkit.Color;
import org.bukkit.entity.ItemDisplay;

/** Cosmetic state of the furnace result's display entity. The item stack is never edited. */
final class CraftingHeat {
    private static final double COOL_AT = StationMotion.REVEAL_END;
    private CraftingHeat() {}

    static int outline(int age, int duration, float scale) {
        double progress = (double) age / duration;
        if (progress >= COOL_AT || !StationMotion.clearsHearthLip((age - 1f) / duration, scale)) return -1;
        double fade = Math.clamp((progress - .46) / (COOL_AT - .46), 0, 1);
        int red = (int) Math.round(255 - 151 * fade);
        int green = (int) Math.round(186 - 140 * fade);
        int blue = (int) Math.round(79 - 54 * fade);
        return red << 16 | green << 8 | blue;
    }

    /** Display owner thread, after applying each new pose. */
    static void apply(ItemDisplay display, int age, int duration, float scale) {
        int rgb = outline(age, duration, scale);
        if (rgb >= 0) {
            display.setGlowColorOverride(Color.fromRGB(rgb));
            if (!display.isGlowing()) display.setGlowing(true);
        } else if (display.isGlowing()) {
            display.setGlowing(false);
            display.setGlowColorOverride(null);
        }
        if ((double) age / duration >= COOL_AT && display.getBrightness() != null) {
            display.setBrightness(null);
        }
    }
}
