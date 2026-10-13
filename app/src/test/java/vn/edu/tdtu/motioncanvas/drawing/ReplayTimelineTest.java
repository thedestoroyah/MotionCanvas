package vn.edu.tdtu.motioncanvas.drawing;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ReplayTimelineTest {
    private final ReplayTimeline timeline = new ReplayTimeline(new int[]{10, 5});
    private final int[] out = new int[2];

    @Test
    public void totalSumsAllPoints() {
        assertEquals(15, timeline.total());
    }

    @Test
    public void durationIsClampedBetweenTwoAndEightSeconds() {
        assertEquals(ReplayTimeline.MIN_DURATION_MS, new ReplayTimeline(new int[]{3}).durationMs());
        assertEquals(ReplayTimeline.MAX_DURATION_MS, new ReplayTimeline(new int[]{100_000}).durationMs());
        assertEquals(3600L, new ReplayTimeline(new int[]{300}).durationMs());
    }

    @Test
    public void locateAtStartIsFirstStrokeWithNoPoints() {
        timeline.locate(0f, out);
        assertArrayEquals(new int[]{0, 0}, out);
    }

    @Test
    public void locateInsideFirstStroke() {
        timeline.locate(0.5f, out); // 7.5 → 8 điểm
        assertArrayEquals(new int[]{0, 8}, out);
    }

    @Test
    public void locateMovesIntoSecondStroke() {
        timeline.locate(0.8f, out); // 12 điểm = 10 của nét 0 + 2 của nét 1
        assertArrayEquals(new int[]{1, 2}, out);
    }

    @Test
    public void locateAtEndReturnsLastStrokeComplete() {
        timeline.locate(1f, out);
        assertArrayEquals(new int[]{1, 5}, out);
    }

    @Test
    public void locateClampsProgressOutsideZeroToOne() {
        timeline.locate(-1f, out);
        assertArrayEquals(new int[]{0, 0}, out);
        timeline.locate(9f, out);
        assertArrayEquals(new int[]{1, 5}, out);
    }
}
