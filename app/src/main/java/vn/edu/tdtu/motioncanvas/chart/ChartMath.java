package vn.edu.tdtu.motioncanvas.chart;

public final class ChartMath {
    private ChartMath() {
    }

    /** Góc quét (độ) của từng phần donut; tổng = 360. Giá trị âm coi như 0; tổng ≤ 0 → toàn 0. */
    public static float[] sweepAngles(float[] values) {
        float total = 0f;
        for (float v : values) total += Math.max(0f, v);
        float[] sweeps = new float[values.length];
        if (total <= 0f) return sweeps;
        for (int i = 0; i < values.length; i++) {
            sweeps[i] = Math.max(0f, values[i]) / total * 360f;
        }
        return sweeps;
    }

    /** Góc của (dx, dy) so với tâm: 0° = 12 giờ, tăng theo chiều kim đồng hồ (trục y hướng xuống). */
    public static float angleFromTop(float dx, float dy) {
        float deg = (float) Math.toDegrees(Math.atan2(dy, dx)) + 90f;
        return deg < 0f ? deg + 360f : deg;
    }

    /** Chỉ số phần tử gần x nhất, với phần tử i nằm ở start + i·step. */
    public static int nearestIndex(float x, float start, float step, int count) {
        if (count <= 0) return -1;
        if (step <= 0f) return 0;
        int index = Math.round((x - start) / step);
        return Math.max(0, Math.min(count - 1, index));
    }

    /**
     * Điểm điều khiển Bézier bậc ba để vẽ đường Catmull-Rom đi qua đúng n điểm (xs[i], ys[i]).
     * Đoạn i → i+1 ghi vào out[4i..4i+3] = c1x, c1y, c2x, c2y. Hai đầu dùng chính điểm biên làm điểm kề.
     */
    public static void catmullRomControls(float[] xs, float[] ys, int n, float[] out) {
        for (int i = 0; i < n - 1; i++) {
            int prev = Math.max(0, i - 1);
            int next = Math.min(n - 1, i + 2);
            out[4 * i] = xs[i] + (xs[i + 1] - xs[prev]) / 6f;
            out[4 * i + 1] = ys[i] + (ys[i + 1] - ys[prev]) / 6f;
            out[4 * i + 2] = xs[i + 1] - (xs[next] - xs[i]) / 6f;
            out[4 * i + 3] = ys[i + 1] - (ys[next] - ys[i]) / 6f;
        }
    }

    /** Giá trị đường Bézier bậc ba tại t ∈ [0, 1] trên một trục. */
    public static float cubicAt(float p0, float c1, float c2, float p1, float t) {
        float u = 1f - t;
        return u * u * u * p0 + 3f * u * u * t * c1 + 3f * u * t * t * c2 + t * t * t * p1;
    }

    /**
     * Độ cao (y) của đầu đường tại mép phải vùng vẽ, khi điểm mới nhất còn đang trượt vào.
     * scrollOffset = 1 → điểm mới vừa xuất hiện, đầu đường còn ở điểm trước; 0 → đã tới điểm mới.
     */
    public static float leadingY(float prevY, float lastY, float scrollOffset) {
        float t = 1f - Math.max(0f, Math.min(1f, scrollOffset));
        return prevY + t * (lastY - prevY);
    }
}
