package vn.edu.tdtu.motioncanvas.drawing;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StrokeTest {
    private static Stroke pen() {
        return new Stroke(BrushType.PEN, 0xFF000000, 8f, Symmetry.OFF, 50f, 50f, 1L);
    }

    @Test
    public void keepsPointsBeyondInitialCapacity() {
        Stroke s = pen();
        for (int i = 0; i < 100; i++) s.addPoint(i, 2 * i, 0.5f);

        assertEquals(100, s.pointCount());
        assertEquals(99f, s.x(99), 0f);
        assertEquals(198f, s.y(99), 0f);
        assertEquals(0.5f, s.w(99), 0f);
        assertEquals(0f, s.x(0), 0f);
    }

    @Test
    public void visibleCountIsClampedToPointCount() {
        Stroke s = pen();
        s.addPoint(1f, 1f, 1f);
        s.addPoint(2f, 2f, 1f);
        s.addPoint(3f, 3f, 1f);

        assertEquals(2, s.visibleCount(2));
        assertEquals(3, s.visibleCount(99));
        assertEquals(0, s.visibleCount(-4));
    }

    @Test
    public void clearMarkerHasNoPoints() {
        assertEquals(0, Stroke.CLEAR.pointCount());
    }
}
