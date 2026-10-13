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

    /** Chỉ số phần donut chứa góc `angleDeg` (đo từ 12 giờ), -1 nếu không có. */
    public static int sliceAt(float angleDeg, float[] sweeps) {
        float start = 0f;
        for (int i = 0; i < sweeps.length; i++) {
            if (sweeps[i] > 0f && angleDeg >= start && angleDeg < start + sweeps[i]) return i;
            start += sweeps[i];
        }
        return -1;
    }

    /** Chỉ số phần tử gần x nhất, với phần tử i nằm ở start + i·step. */
    public static int nearestIndex(float x, float start, float step, int count) {
        if (count <= 0) return -1;
        if (step <= 0f) return 0;
        int index = Math.round((x - start) / step);
        return Math.max(0, Math.min(count - 1, index));
    }
}
