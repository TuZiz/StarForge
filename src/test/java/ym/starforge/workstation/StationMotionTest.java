package ym.starforge.workstation;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static ym.starforge.workstation.StationAnchor.Kind.*;

class StationMotionTest {
    @Test void enhancementRisesInFrontOfCrystalThenPausesAndContracts() {
        var crystal = StationMotion.fromModel(8, 21.2f, 9.35f);
        var start = StationMotion.frame(ENHANCEMENT, 0, .9);
        assertEquals(crystal.x, start.getTranslation().x);
        assertTrue(start.getTranslation().z > crystal.z + .4f);
        var peak = StationMotion.frame(ENHANCEMENT, .7, .9);
        assertEquals(.9, peak.getTranslation().y - start.getTranslation().y, .0001);
        assertEquals(peak, StationMotion.frame(ENHANCEMENT, .84, .9));
        assertTrue(StationMotion.frame(ENHANCEMENT, 1, .9).getScale().x < .1f);
    }

    @Test void craftingDrawsHandleFirstThroughHearthBeforeFlippingFaceUpForDisplay() {
        var start = StationMotion.frame(CRAFTING, 0, .9);
        var pulled = StationMotion.frame(CRAFTING, .4, .9);
        var revealed = StationMotion.frame(CRAFTING, .62, .9);
        var mouth = StationMotion.fromModel(-7.2f, 14, -4.8f);
        assertEquals(mouth.x, start.getTranslation().x);
        assertTrue(start.getTranslation().z < mouth.z && pulled.getTranslation().z > mouth.z + .65f);
        assertEquals(mouth.y, pulled.getTranslation().y);
        Vector3f handle = new Vector3f(-1, -1, 0).normalize().rotate(start.getLeftRotation());
        assertTrue(handle.z > .99f);
        assertTrue(revealed.getTranslation().y > pulled.getTranslation().y);
        assertTrue(new Vector3f(0, 0, 1).rotate(revealed.getLeftRotation()).z < -.9f);
    }

    @Test void furnaceAndOutwardDirectionFollowAllFourFurniturePlacements() {
        var hearth = StationMotion.frame(CRAFTING, 0, .9).getTranslation();
        var outward = new Vector3f(0, 0, 1);
        Vector3f[] expectedHearth = {hearth, new Vector3f(-hearth.z, hearth.y, hearth.x),
                new Vector3f(-hearth.x, hearth.y, -hearth.z), new Vector3f(hearth.z, hearth.y, -hearth.x)};
        Vector3f[] expectedOut = {outward, new Vector3f(-1,0,0), new Vector3f(0,0,-1), new Vector3f(1,0,0)};
        for (int i = 0; i < 4; i++) {
            assertTrue(StationMotion.worldOffset(hearth, 90 * i).equals(expectedHearth[i], .0001f));
            assertTrue(StationMotion.worldOffset(outward, 90 * i).equals(expectedOut[i], .0001f));
        }
    }

    @Test void furnaceOutlineClearsTheFrontLipThenCoolsBeforeDisplayHoldAtEveryDuration() {
        float ledge = StationMotion.fromModel(-7.2f, 9.5f, -6.7f).z;
        for (int ticks = 10; ticks <= 100; ticks++) {
            int brightest = -1;
            int lastColor = -1;
            boolean cooling = false;
            boolean hold = false;
            for (int age = 1; age <= ticks; age++) {
                double t = (double) age / ticks;
                var pose = StationMotion.frame(CRAFTING, t, .9);
                var previous = StationMotion.frame(CRAFTING, (age - 1d) / ticks, .9);
                float scale = pose.getScale().x;
                int rgb = CraftingHeat.outline(age, ticks, scale);
                if (rgb >= 0) {
                    assertTrue(previous.getTranslation().z - Math.sqrt(3) * .5 * scale > ledge);
                    assertTrue(t < .62);
                    if (brightest < 0) brightest = rgb;
                    if (lastColor >= 0) {
                        for (int shift : new int[]{0, 8, 16}) {
                            assertTrue(((rgb >> shift) & 255) <= ((lastColor >> shift) & 255));
                        }
                    }
                    cooling |= rgb < brightest;
                    lastColor = rgb;
                }
                if (t >= .62 && t <= .95) {
                    assertEquals(-1, rgb);
                    assertEquals(.68f, scale, .0001f);
                    hold = true;
                }
            }
            assertEquals(0xFFBA4F, brightest, "warm outline at duration " + ticks);
            assertTrue(cooling && hold, "cooling and unmodified display hold at duration " + ticks);
        }
    }

    @Test void defaultClaimPresentsTheCooledProductForTenConsecutiveTicks() {
        var reveal = StationMotion.frame(CRAFTING, 19 / 30d, .9);
        for (int age = 19; age <= 28; age++) {
            assertEquals(reveal, StationMotion.frame(CRAFTING, age / 30d, .9));
            assertEquals(-1, CraftingHeat.outline(age, 30, reveal.getScale().x));
        }
    }
}
