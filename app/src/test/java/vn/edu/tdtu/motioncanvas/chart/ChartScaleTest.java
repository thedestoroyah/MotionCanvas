package vn.edu.tdtu.motioncanvas.chart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ChartScaleTest {
    // Trục Y: giá trị 0 ở đáy (pixel 200), 100 ở đỉnh (pixel 0)
    private final ChartScale scale = new ChartScale(0f, 100f, 200f, 0f);

    @Test
    public void minMapsToStartAndMaxMapsToEnd() {
        assertEquals(200f, scale.toPixel(0f), 1e-4f);
        assertEquals(0f, scale.toPixel(100f), 1e-4f);
    }

    @Test
    public void middleValueMapsToMiddlePixel() {
        assertEquals(100f, scale.toPixel(50f), 1e-4f);
    }

    @Test
    public void flatDataDoesNotDivideByZero() {
        ChartScale flat = new ChartScale(42f, 42f, 200f, 0f);
        float px = flat.toPixel(42f);
        assertFalse(Float.isNaN(px) || Float.isInfinite(px));
    }

    @Test
    public void tickValuesAreEvenlySpaced() {
        assertEquals(0f, scale.tickValue(0, 5), 1e-4f);
        assertEquals(50f, scale.tickValue(2, 5), 1e-4f);
        assertEquals(100f, scale.tickValue(4, 5), 1e-4f);
        assertEquals(0f, scale.tickValue(0, 1), 1e-4f);
    }
}
