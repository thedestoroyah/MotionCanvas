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
    public void nearestIndexRoundsAndClamps() {
        assertEquals(2, ChartMath.nearestIndex(24f, 0f, 10f, 5));
        assertEquals(3, ChartMath.nearestIndex(26f, 0f, 10f, 5));
        assertEquals(0, ChartMath.nearestIndex(-100f, 0f, 10f, 5));
        assertEquals(4, ChartMath.nearestIndex(999f, 0f, 10f, 5));
        assertEquals(-1, ChartMath.nearestIndex(5f, 0f, 10f, 0));
    }

    @Test
    public void catmullRomControlsOfTwoPointsLieOnTheSegment() {
        float[] out = new float[4];
        ChartMath.catmullRomControls(new float[]{0f, 6f}, new float[]{0f, 6f}, 2, out);
        assertArrayEquals(new float[]{1f, 1f, 5f, 5f}, out, 1e-4f);
    }

    @Test
    public void catmullRomCurveThroughCollinearPointsStaysOnTheLine() {
        float[] xs = {0f, 1f, 2f, 3f};
        float[] ys = {0f, 2f, 4f, 6f};
        float[] out = new float[12];

        ChartMath.catmullRomControls(xs, ys, 4, out);

        // Đoạn giữa (1,2) → (2,4): tại t = 0.5 phải đúng trung điểm (1.5, 3)
        assertEquals(1.5f, ChartMath.cubicAt(xs[1], out[4], out[6], xs[2], 0.5f), 1e-4f);
        assertEquals(3f, ChartMath.cubicAt(ys[1], out[5], out[7], ys[2], 0.5f), 1e-4f);
    }

    @Test
    public void cubicPassesThroughItsEndPoints() {
        assertEquals(2f, ChartMath.cubicAt(2f, 9f, -4f, 7f, 0f), 1e-6f);
        assertEquals(7f, ChartMath.cubicAt(2f, 9f, -4f, 7f, 1f), 1e-6f);
    }

    @Test
    public void catmullRomWithFewerThanTwoPointsWritesNothing() {
        float[] out = {9f, 9f, 9f, 9f};
        ChartMath.catmullRomControls(new float[]{1f}, new float[]{1f}, 1, out);
        ChartMath.catmullRomControls(new float[0], new float[0], 0, out);
        assertArrayEquals(new float[]{9f, 9f, 9f, 9f}, out, 0f);
    }

    @Test
    public void leadingYInterpolatesAlongTheLastSegment() {
        assertEquals(10f, ChartMath.leadingY(10f, 30f, 1f), 1e-4f); // điểm mới vừa xuất hiện: còn ở điểm trước
        assertEquals(20f, ChartMath.leadingY(10f, 30f, 0.5f), 1e-4f);
        assertEquals(30f, ChartMath.leadingY(10f, 30f, 0f), 1e-4f); // đã trượt xong: ở điểm mới
    }

    @Test
    public void leadingYClampsScrollOffsetOutsideZeroToOne() {
        assertEquals(10f, ChartMath.leadingY(10f, 30f, 5f), 1e-4f);
        assertEquals(30f, ChartMath.leadingY(10f, 30f, -5f), 1e-4f);
    }
}
