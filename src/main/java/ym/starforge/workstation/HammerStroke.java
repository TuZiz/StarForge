package ym.starforge.workstation;

import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** One 3.05-second loop. Angles are measured from the flat striking face on the billet. */
final class HammerStroke {
    static final int PERIOD = 61;
    private record Key(int tick, float angle, float lift) {}
    private static final Key[] KEYS = {
            new Key(0, 94, .17f), new Key(4, 101, .22f), new Key(6, 94, .20f),
            new Key(8, 57, .11f), new Key(9, 30, .055f), new Key(10, 0, 0),
            new Key(11, 4, .01f), new Key(13, 53, .10f), new Key(16, 86, .17f),
            new Key(18, 90, .18f), new Key(20, 56, .10f), new Key(22, 0, 0),
            new Key(23, 5, .014f), new Key(26, 73, .15f), new Key(29, 96, .21f),
            new Key(30, 95, .20f), new Key(32, 57, .11f), new Key(34, 0, 0),
            new Key(35, 3, .008f), new Key(37, 51, .10f), new Key(40, 94, .21f),
            new Key(43, 111, .275f), new Key(44, 111, .275f), new Key(46, 74, .16f),
            new Key(47, 37, .075f), new Key(48, 0, 0), new Key(49, 0, 0),
            new Key(50, 10, .02f), new Key(52, 51, .094f), new Key(55, 86, .145f),
            new Key(58, 91, .16f), new Key(PERIOD, 94, .17f)
    };

    private HammerStroke() {}

    static int impact(int tick) {
        return switch (Math.floorMod(tick, PERIOD)) {
            case 10 -> 1;
            case 22 -> 2;
            case 34 -> 3;
            case 48 -> 4;
            default -> 0;
        };
    }

    static Vector3f billet() {
        // Top of the fixed hot steel blank, clear of the crossing tongs on its left end.
        return StationMotion.fromModel(5.7f, 19.5f, 8);
    }

    static Transformation frame(int tick) {
        int phase = Math.floorMod(tick, PERIOD);
        int i = 1;
        while (KEYS[i].tick < phase) i++;
        Key a = KEYS[i - 1], b = KEYS[i];
        float t = (float) (phase - a.tick) / (b.tick - a.tick);
        float angle = Math.fma(t, b.angle - a.angle, a.angle);
        float lift = Math.fma(t, b.lift - a.lift, a.lift);
        // The NONE item context turns the head at (-7,3,8) about the grip at (8,8,8).
        // At impact its local offset after the client's Y half-turn is (+15,-5,0)/16.
        Vector3f grip = billet().add(-15 / 16f - .075f * lift, 5 / 16f + lift, 0);
        return new Transformation(grip, new Quaternionf().rotateZ((float) Math.toRadians(angle)),
                new Vector3f(1), new Quaternionf());
    }
}
