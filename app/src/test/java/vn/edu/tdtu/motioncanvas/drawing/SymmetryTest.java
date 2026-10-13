package vn.edu.tdtu.motioncanvas.drawing;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SymmetryTest {
    @Test
    public void copiesCountRotationsAndMirror() {
        assertEquals(1, Symmetry.OFF.copies());
        assertEquals(2, Symmetry.MIRROR.copies());
        assertEquals(8, Symmetry.FOUR.copies());
        assertEquals(16, Symmetry.EIGHT.copies());
    }

    @Test
    public void nextCyclesBackToOff() {
        assertEquals(Symmetry.MIRROR, Symmetry.OFF.next());
        assertEquals(Symmetry.OFF, Symmetry.EIGHT.next());
    }
}
