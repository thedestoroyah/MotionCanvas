package vn.edu.tdtu.motioncanvas.chart;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ChartMathTest {
    @Test
    public void sweepsAreProportionalAndSumTo360() {
        assertArrayEquals(new float[]{90f, 270f}, ChartMath.sweepAngles(new float[]{1f, 3f}), 1e-3f);
    }

    @Test
    public void zeroTotalGivesZeroSweeps() {
        assertArrayEquals(new float[]{0f, 0f, 0f}, ChartMath.sweepAngles(new float[]{0f, 0f, 0f}), 0f);
    }

    @Test
    public void negativeValuesCountAsZero() {
        assertArrayEquals(new float[]{0f, 360f}, ChartMath.sweepAngles(new float[]{-5f, 2f}), 1e-3f);
    }

    @Test
    public void angleFromTopGoesClockwise() {
        assertEquals(0f, ChartMath.angleFromTop(0f, -1f), 1e-3f);   // 12 giờ
        assertEquals(90f, ChartMath.angleFromTop(1f, 0f), 1e-3f);   // 3 giờ
        assertEquals(180f, ChartMath.angleFromTop(0f, 1f), 1e-3f);  // 6 giờ
        assertEquals(270f, ChartMath.angleFromTop(-1f, 0f), 1e-3f); // 9 giờ
    }

    @Test
    public void sliceAtFindsContainingSlice() {
        float[] sweeps = {90f, 180f, 90f};
        assertEquals(0, ChartMath.sliceAt(45f, sweeps));
        assertEquals(1, ChartMath.sliceAt(90f, sweeps));
        assertEquals(2, ChartMath.sliceAt(300f, sweeps));
    }

    @Test
    public void sliceAtWithEmptyDataReturnsMinusOne() {
        assertEquals(-1, ChartMath.sliceAt(10f, new float[]{0f, 0f}));
    }

    @Test
    public void nearestIndexRoundsAndClamps() {
        assertEquals(2, ChartMath.nearestIndex(24f, 0f, 10f, 5));
        assertEquals(3, ChartMath.nearestIndex(26f, 0f, 10f, 5));
        assertEquals(0, ChartMath.nearestIndex(-100f, 0f, 10f, 5));
        assertEquals(4, ChartMath.nearestIndex(999f, 0f, 10f, 5));
        assertEquals(-1, ChartMath.nearestIndex(5f, 0f, 10f, 0));
    }
}
