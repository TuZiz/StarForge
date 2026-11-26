package ym.starforge.workstation;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.joml.Vector3f;

/** Invoked on the display owner. All particles use the same rotated center as the result. */
final class StationEffects {
    private static final Particle.DustOptions CYAN = new Particle.DustOptions(Color.fromRGB(113, 225, 237), .65f);
    private static final Particle.DustOptions EMBER = new Particle.DustOptions(Color.fromRGB(255, 145, 58), .8f);
    private StationEffects() {}

    static void render(StationAnchor.Kind kind, Location anchor, Vector3f translation,
                       int age, int duration) {
        Vector3f offset = StationMotion.worldOffset(translation, anchor.getYaw());
        Location center = anchor.clone().add(offset.x, offset.y, offset.z);
        var world = center.getWorld();
        double progress = (double) age / duration;
        if (kind == StationAnchor.Kind.CRAFTING) {
            if (age == (int) Math.ceil(duration * StationMotion.DRAW_END)) {
                world.playSound(center, "starforge_workstations:claim", SoundCategory.BLOCKS, .7f, 1f);
            }
            if (age % 2 == 0 && progress >= .2 && progress < StationMotion.REVEAL_END) {
                world.spawnParticle(Particle.DUST, center, progress < StationMotion.DRAW_END ? 3 : 1,
                        .10, .04, .08, 0, EMBER);
            }
            return;
        }
        if (age == 1) {
            world.playSound(center, Sound.BLOCK_ENCHANTMENT_TABLE_USE, .65f, 1.3f);
        }
        if (age % 2 == 0) {
            double radius = .12 + .28 * Math.min(1, (1 - progress) / .15);
            for (int i = 0; i < 3; i++) {
                double angle = progress * Math.PI * 4 + i * Math.TAU / 3;
                world.spawnParticle(Particle.DUST, center.clone().add(radius * Math.cos(angle), 0,
                        radius * Math.sin(angle)), 1, 0, 0, 0, 0, CYAN);
            }
        }
        if (age == duration) {
            world.spawnParticle(Particle.END_ROD, center, 6, .06, .08, .06, .012);
            world.playSound(center, Sound.ENTITY_PLAYER_LEVELUP, .5f, 1.65f);
        }
    }
}
