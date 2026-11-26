package ym.starforge.workstation;

import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Poses relative to the CE furniture origin, before the display's own placement yaw. */
final class StationMotion {
    static final double DRAW_END = .40;
    static final double REVEAL_END = .62;
    private static final double HOLD_END = .95;
    private StationMotion() {}

    static Transformation frame(StationAnchor.Kind kind, double progress, double rise) {
        float t = (float) Math.clamp(progress, 0, 1);
        if (kind == StationAnchor.Kind.CRAFTING) return drawFromHearth(t, (float) rise);

        float lift = smooth(t / .70f);
        float close = smooth((t - .85f) / .15f);
        Vector3f center = fromModel(8, 17, -1).add(0, (float) rise * lift, 0);
        Quaternionf turn = new Quaternionf().rotateY(radians(360 * lift)).rotateZ(radians(-12));
        return pose(center, turn, .72f - .68f * close);
    }

    private static Transformation drawFromHearth(float t, float rise) {
        float flip = smooth((float) ((t - DRAW_END) / (REVEAL_END - DRAW_END)));
        float close = smooth((float) ((t - HOLD_END) / (1 - HOLD_END)));
        // Coal bed ends at model Z=-4.7. Clear its hood before turning the product over.
        Vector3f center = fromModel(-7.2f, 14, hearthModelZ(t));
        center.y += .48f * rise * flip;
        // For a standard generated tool in FIXED the handle points down-left. Align that
        // diagonal with the outward draw, then roll the broad face towards the player.
        Quaternionf turn = new Quaternionf().rotateY(radians(90 - 76 * flip))
                .rotateX(radians(75 + 105 * flip)).rotateZ(radians(-45 - 105 * flip));
        return pose(center, turn, (.44f + .24f * flip) * (1 - .92f * close));
    }

    private static float hearthModelZ(float progress) {
        return 2 - 19 * smooth((float) (progress / DRAW_END));
    }

    static boolean clearsHearthLip(float previousProgress, float scale) {
        // The furnace's foremost ledge is Z=-6.7. Take the previous interpolation
        // endpoint and a sphere enclosing a rotated unit item, with a small margin.
        double reachInModelUnits = 16 * (Math.sqrt(3) * .5 * scale + .03);
        return hearthModelZ(previousProgress) + reachInModelUnits < -6.7;
    }

    // The CE body uses display context NONE and position Y=0.5. The client centers the
    // 16-unit model and rotates an ItemDisplay by 180 degrees around Y before drawing it.
    static Vector3f fromModel(float x, float y, float z) {
        return new Vector3f((8 - x) / 16, y / 16, (8 - z) / 16);
    }

    static Vector3f worldOffset(Vector3f local, float placementYaw) {
        return new Vector3f(local).rotateY(radians(-placementYaw));
    }

    private static Transformation pose(Vector3f center, Quaternionf rotation, float scale) {
        return new Transformation(center, rotation, new Vector3f(scale), new Quaternionf());
    }

    private static float smooth(float t) {
        float bounded = Math.clamp(t, 0f, 1f);
        return bounded * bounded * (3 - 2 * bounded);
    }

    private static float radians(float degrees) {
        return (float) Math.toRadians(degrees);
    }
}
