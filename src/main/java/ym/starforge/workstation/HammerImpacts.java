package ym.starforge.workstation;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.joml.Vector3f;

import java.util.concurrent.ThreadLocalRandom;

final class HammerImpacts {
    private static final Particle.DustTransition HOT_METAL =
            new Particle.DustTransition(Color.fromRGB(255, 235, 161), Color.fromRGB(255, 107, 22), .8f);
    private static final Color GOLD = Color.fromRGB(255, 193, 62);
    private static final Color AMBER = Color.fromRGB(255, 130, 38);

    private HammerImpacts() {}

    /** The location is the actual contact point in world space. Offsets follow the table yaw. */
    static void strike(Location at, float yaw, int number) {
        var world = at.getWorld();
        var random = ThreadLocalRandom.current();
        float strength = switch (number) {
            case 2 -> .85f;
            case 3 -> 1.02f;
            case 4 -> 1.25f;
            default -> .96f;
        };
        world.playSound(at, "starforge_workstations:hammer_" + number, SoundCategory.BLOCKS, .85f, 1);
        world.spawnParticle(Particle.DUST_COLOR_TRANSITION, at, number == 4 ? 9 : 5, .05, .025, .04, 0, HOT_METAL);
        int count = number == 4 ? 13 : 8;
        for (int i = 0; i < count; i++) {
            float side = i % 2 == 0 ? 1 : -1;
            var local = new Vector3f(side * random.nextFloat(.35f, .85f),
                    random.nextFloat(.08f, .4f), random.nextFloat(-.36f, .36f)).mul(strength);
            var delta = StationMotion.worldOffset(local, yaw);
            Location tip = at.clone().add(delta.x, delta.y, delta.z);
            world.spawnParticle(Particle.TRAIL, at, 1, 0, 0, 0, 0,
                    new Particle.Trail(tip, i % 3 == 0 ? AMBER : GOLD, random.nextInt(3, 7)));
        }
    }
}
