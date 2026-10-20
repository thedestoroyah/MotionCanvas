package vn.edu.tdtu.motioncanvas.drawing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BrushPreviewWidthTest {
    private static final float MAX_VISIBLE = 26f;

    @Test
    public void thinStrokesKeepTrueSize() {
        assertEquals(8f, BrushPreviewView.displayWidthDp(8f, MAX_VISIBLE), 0f);
    }

    @Test
    public void maxStrokeFillsVisibleHeight() {
        assertEquals(MAX_VISIBLE, BrushPreviewView.displayWidthDp(BrushPreviewView.MAX_STROKE_DP, MAX_VISIBLE), 1e-4f);
    }

    @Test
    public void everyWidthStepLooksDifferent() {
        // Lỗi cũ: từ khoảng 50% trở lên nét bị kẹp nên trông y như nhau
        for (int dp = 2; dp <= BrushPreviewView.MAX_STROKE_DP; dp++) {
            assertTrue(BrushPreviewView.displayWidthDp(dp, MAX_VISIBLE)
                    > BrushPreviewView.displayWidthDp(dp - 1, MAX_VISIBLE));
        }
    }
}
