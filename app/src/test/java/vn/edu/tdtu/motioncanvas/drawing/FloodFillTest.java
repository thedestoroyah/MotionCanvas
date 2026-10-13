package vn.edu.tdtu.motioncanvas.drawing;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FloodFillTest {
    private static final int W = 0xFFFFFFFF;
    private static final int B = 0xFF000000;
    private static final int R = 0xFFFF0000;
    private static final int SIZE = 5;

    /** 5×5 nền trắng, viền đen hình vuông 3×3 ở giữa, ô (2,2) trắng bên trong. */
    private static int[] ring() {
        return new int[]{
                W, W, W, W, W,
                W, B, B, B, W,
                W, B, W, B, W,
                W, B, B, B, W,
                W, W, W, W, W,
        };
    }

    @Test
    public void fillsOnlyTheEnclosedRegion() {
        int[] px = ring();

        assertTrue(FloodFill.fill(px, SIZE, SIZE, 2, 2, R, 0));

        int[] expected = ring();
        expected[2 * SIZE + 2] = R;
        assertArrayEquals(expected, px);
    }

    @Test
    public void outsideFillDoesNotCrossTheBorder() {
        int[] px = ring();

        FloodFill.fill(px, SIZE, SIZE, 0, 0, R, 0);

        assertEquals(W, px[2 * SIZE + 2]); // bên trong viền vẫn trắng
        assertEquals(B, px[SIZE + 1]);     // viền giữ nguyên
        assertEquals(R, px[4 * SIZE + 4]); // góc ngoài đã tô
    }

    @Test
    public void sameColorDoesNothing() {
        int[] px = ring();
        assertFalse(FloodFill.fill(px, SIZE, SIZE, 1, 1, B, 0));
        assertArrayEquals(ring(), px);
    }

    @Test
    public void outOfBoundsDoesNothing() {
        int[] px = ring();
        assertFalse(FloodFill.fill(px, SIZE, SIZE, SIZE, 0, R, 0));
        assertFalse(FloodFill.fill(px, SIZE, SIZE, -1, 2, R, 0));
        assertArrayEquals(ring(), px);
    }

    @Test
    public void toleranceAbsorbsNearlySameColor() {
        int nearWhite = 0xFFF0F0F0;
        int[] strict = {W, nearWhite, W};
        int[] loose = {W, nearWhite, W};

        FloodFill.fill(strict, 3, 1, 0, 0, R, 0);
        FloodFill.fill(loose, 3, 1, 0, 0, R, 32);

        assertArrayEquals(new int[]{R, nearWhite, W}, strict);
        assertArrayEquals(new int[]{R, R, R}, loose);
    }
}
