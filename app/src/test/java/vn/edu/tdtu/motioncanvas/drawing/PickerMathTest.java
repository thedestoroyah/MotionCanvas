package vn.edu.tdtu.motioncanvas.drawing;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PickerMathTest {
    @Test
    public void hueFollowsSweepGradientDirection() {
        assertEquals(0f, PickerMath.hueAt(1f, 0f), 1e-3f);    // 3 giờ: đỏ
        assertEquals(90f, PickerMath.hueAt(0f, 1f), 1e-3f);   // 6 giờ (y hướng xuống)
        assertEquals(180f, PickerMath.hueAt(-1f, 0f), 1e-3f); // 9 giờ: cyan
        assertEquals(270f, PickerMath.hueAt(0f, -1f), 1e-3f); // 12 giờ
    }

    @Test
    public void fractionMapsPositionInsideRange() {
        assertEquals(0.25f, PickerMath.fraction(125f, 100f, 100f), 1e-4f);
    }

    @Test
    public void fractionIsClampedOutsideRange() {
        assertEquals(0f, PickerMath.fraction(-500f, 100f, 100f), 0f);
        assertEquals(1f, PickerMath.fraction(9999f, 100f, 100f), 0f);
    }

    @Test
    public void fractionOfZeroLengthIsZero() {
        assertEquals(0f, PickerMath.fraction(150f, 100f, 0f), 0f);
    }
}
