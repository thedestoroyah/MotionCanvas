package vn.edu.tdtu.motioncanvas.drawing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CalligraphyWidthTest {
    @Test
    public void standingStillGivesThickest() {
        assertEquals(CalligraphyWidth.MAX, CalligraphyWidth.factor(0f), 0f);
    }

    @Test
    public void veryFastGivesThinnest() {
        assertEquals(CalligraphyWidth.MIN, CalligraphyWidth.factor(1000f), 0f);
    }

    @Test
    public void factorStaysInRange() {
        for (float speed = -5f; speed <= 10f; speed += 0.25f) {
            float f = CalligraphyWidth.factor(speed);
            assertTrue(f >= CalligraphyWidth.MIN && f <= CalligraphyWidth.MAX);
        }
    }

    @Test
    public void nanSpeedGivesThickest() {
        assertEquals(CalligraphyWidth.MAX, CalligraphyWidth.factor(Float.NaN), 0f);
    }

    @Test
    public void infiniteSpeedGivesThinnest() {
        assertEquals(CalligraphyWidth.MIN, CalligraphyWidth.factor(Float.POSITIVE_INFINITY), 0f);
    }

    @Test
    public void smoothMovesPartWayTowardTarget() {
        float next = CalligraphyWidth.smooth(1f, 0f);
        assertTrue(next < 1f && next > 0f);
    }
}
