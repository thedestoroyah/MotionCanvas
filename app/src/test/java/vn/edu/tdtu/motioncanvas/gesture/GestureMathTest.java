package vn.edu.tdtu.motioncanvas.gesture;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GestureMathTest {
    @Test
    public void clampKeepsValuesAndBothEndpoints() {
        for (float value : new float[]{24f, 80f, 216f}) {
            assertEquals(value, GestureMath.clamp(value, 24f, 216f), 0f);
        }
    }

    @Test
    public void clampRecoversCentersOutsideEitherEdge() {
        assertEquals(24f, GestureMath.clamp(-60f, 24f, 216f), 0f);
        assertEquals(216f, GestureMath.clamp(400f, 24f, 216f), 0f);
    }

    @Test
    public void clampHandlesLockedAxis() {
        assertEquals(120f, GestureMath.clamp(-10f, 120f, 120f), 0f);
        assertEquals(120f, GestureMath.clamp(350f, 120f, 120f), 0f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void clampRejectsReversedRange() {
        GestureMath.clamp(50f, 100f, 0f);
    }

    @Test
    public void rangeLeavesRoomForBothHalvesOfCard() {
        assertArrayEquals(new float[]{40f, 280f}, GestureMath.centerRange(320f, 40f), 0f);
    }

    @Test
    public void exactFitLocksToCenter() {
        assertArrayEquals(new float[]{160f, 160f}, GestureMath.centerRange(320f, 160f), 0f);
    }

    @Test
    public void oversizedCardLocksToCenter() {
        assertArrayEquals(new float[]{160f, 160f}, GestureMath.centerRange(320f, 300f), 0f);
    }

    @Test
    public void emptyViewHasOneValidCenter() {
        assertArrayEquals(new float[]{0f, 0f}, GestureMath.centerRange(0f, 100f), 0f);
    }

    @Test
    public void zeroExtentCanMoveAcrossWholeView() {
        assertArrayEquals(new float[]{0f, 320f}, GestureMath.centerRange(320f, 0f), 0f);
    }

    @Test
    public void supportedScalesAlwaysProduceValidFlingStart() {
        for (float size : new float[]{0f, 160f, 360f, 900f}) {
            for (float scale : new float[]{0.5f, 1f, 2f, 3f}) {
                float half = 100f * scale;
                float[] range = GestureMath.centerRange(size, half);
                assertTrue(range[0] <= range[1]);
                for (float center : new float[]{-100f, 0f, size / 2f, size, size + 100f}) {
                    float start = GestureMath.clamp(center, range[0], range[1]);
                    assertTrue(start >= range[0] && start <= range[1]);
                    if (half <= size / 2f) {
                        assertTrue(start - half >= 0f);
                        assertTrue(start + half <= size);
                    } else {
                        assertEquals(size / 2f, start, 0f);
                    }
                }
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rangeRejectsNegativeSize() {
        GestureMath.centerRange(-1f, 10f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rangeRejectsNegativeExtent() {
        GestureMath.centerRange(100f, -1f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rangeRejectsNanSize() {
        GestureMath.centerRange(Float.NaN, 10f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rangeRejectsInfiniteExtent() {
        GestureMath.centerRange(100f, Float.POSITIVE_INFINITY);
    }
}
