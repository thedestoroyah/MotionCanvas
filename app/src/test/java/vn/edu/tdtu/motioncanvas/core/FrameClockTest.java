package vn.edu.tdtu.motioncanvas.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FrameClockTest {
    private long now;
    private final FrameClock clock = new FrameClock(() -> now);

    @Test
    public void firstTickAfterResetIsZero() {
        now = 5_000_000_000L;
        assertEquals(0f, clock.tick(), 0f);
    }

    @Test
    public void tickReturnsSecondsSincePreviousTick() {
        clock.tick();
        now += 16_000_000L;
        assertEquals(0.016f, clock.tick(), 1e-6f);
    }

    @Test
    public void resetDropsTimeSpentPaused() {
        clock.tick();
        now += 10_000_000_000L;
        clock.reset();
        assertEquals(0f, clock.tick(), 0f);
    }
}
