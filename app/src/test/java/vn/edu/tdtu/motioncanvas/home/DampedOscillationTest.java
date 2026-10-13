package vn.edu.tdtu.motioncanvas.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DampedOscillationTest {
    private final DampedOscillation swing = new DampedOscillation(30f, 6f, 0.12f);

    @Test
    public void startsAtFullAmplitude() {
        assertEquals(30f, swing.valueAt(0f), 1e-4f);
    }

    @Test
    public void envelopeShrinksOverTime() {
        assertTrue(swing.envelope(2f) < swing.envelope(1f));
        assertTrue(swing.envelope(1f) < swing.envelope(0f));
    }

    @Test
    public void valueNeverExceedsEnvelope() {
        for (float t = 0f; t <= 5f; t += 0.05f) {
            assertTrue(Math.abs(swing.valueAt(t)) <= swing.envelope(t) + 1e-4f);
        }
    }
}
