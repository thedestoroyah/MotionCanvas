package vn.edu.tdtu.motioncanvas.chart;

/** Ánh xạ tuyến tính giá trị dữ liệu [min, max] → pixel [pixelStart, pixelEnd]. */
public final class ChartScale {
    private final float min;
    private final float max;
    private final float pixelStart;
    private final float pixelEnd;

    public ChartScale(float min, float max, float pixelStart, float pixelEnd) {
        this.min = min;
        // Dữ liệu phẳng (mọi giá trị bằng nhau): nới khoảng để tránh chia cho 0
        this.max = max > min ? max : min + 1f;
        this.pixelStart = pixelStart;
        this.pixelEnd = pixelEnd;
    }

    public float toPixel(float value) {
        return pixelStart + (value - min) / (max - min) * (pixelEnd - pixelStart);
    }

    /** Giá trị của vạch thứ `index` trong `tickCount` vạch chia đều từ min đến max. */
    public float tickValue(int index, int tickCount) {
        if (tickCount < 2) return min;
        return min + (max - min) * index / (tickCount - 1);
    }
}
