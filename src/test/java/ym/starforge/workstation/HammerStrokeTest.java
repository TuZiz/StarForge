package ym.starforge.workstation;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HammerStrokeTest {
    private static Vector3f bottomFace(int tick) {
        var pose = HammerStroke.frame(tick);
        // Center of the exported model's hammer shoe (-7,3,8) relative to grip (8,8,8),
        // converted to the client's centered item-display coordinates.
        return new Vector3f(15 / 16f, -5 / 16f, 0).mul(pose.getScale())
                .rotate(pose.getLeftRotation()).add(pose.getTranslation());
    }

    @Test void eachBeatStrikesTheActualHotBlankAtAllFourPlacementAngles() {
        var hits = new ArrayList<Integer>();
        var order = new ArrayList<Integer>();
        for (int tick = 0; tick < HammerStroke.PERIOD; tick++) {
            if (HammerStroke.impact(tick) == 0) continue;
            hits.add(tick);
            order.add(HammerStroke.impact(tick));
            for (int yaw = 0; yaw < 360; yaw += 90) {
                var expected = StationMotion.worldOffset(StationMotion.fromModel(5.7f, 19.5f, 8), yaw);
                assertTrue(StationMotion.worldOffset(bottomFace(tick), yaw).equals(expected, .00001f));
            }
            assertEquals(0f, HammerStroke.frame(tick).getLeftRotation().angle(), .00001f);
        }
        assertEquals(List.of(10, 22, 34, 48), hits);
        assertEquals(List.of(1, 2, 3, 4), order);
    }

    @Test void theLastStrokeHasTheHighestBackswingAndHoldsContact() {
        float[] peak = new float[4];
        int stroke = 0;
        for (int tick = 0; tick <= 48; tick++) {
            float height = bottomFace(tick).y - HammerStroke.billet().y;
            assertTrue(height >= -.00001f);
            peak[stroke] = Math.max(peak[stroke], height);
            if (HammerStroke.impact(tick) > 0) {
                assertTrue(bottomFace(tick - 1).y > bottomFace(tick).y + .45f);
                stroke++;
            }
        }
        for (int i = 0; i < 3; i++) {
            assertTrue(peak[i] > 1);
            assertTrue(peak[3] > peak[i] + .04f);
        }
        assertTrue(bottomFace(48).equals(bottomFace(49), .00001f));
        assertEquals(0, HammerStroke.impact(49));
    }

    @Test void windupRestartsWithoutASnapOrAnExtraImpact() {
        assertTrue(bottomFace(0).distance(bottomFace(HammerStroke.PERIOD - 1)) < .05);
        for (int tick = 0; tick < HammerStroke.PERIOD; tick++) {
            assertEquals(HammerStroke.frame(tick), HammerStroke.frame(tick + HammerStroke.PERIOD));
            assertEquals(HammerStroke.impact(tick), HammerStroke.impact(tick + HammerStroke.PERIOD));
        }
    }
}
