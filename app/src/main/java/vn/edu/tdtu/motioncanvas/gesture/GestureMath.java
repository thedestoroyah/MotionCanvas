package vn.edu.tdtu.motioncanvas.gesture;

/** Phép tính biên dùng chung cho kéo và animation */
final class GestureMath {
    private GestureMath() {
    }

    static float clamp(float value, float min, float max) {
        if (min > max) throw new IllegalArgumentException("Khoảng giới hạn bị đảo");
        if (value < min) return min;
        return Math.min(value, max);
    }

    /** Thẻ không vừa một chiều thì tâm được cố định ở giữa chiều đó */
    static float[] centerRange(float viewSize, float halfExtent) {
        if (!Float.isFinite(viewSize) || !Float.isFinite(halfExtent)
                || viewSize < 0f || halfExtent < 0f) {
            throw new IllegalArgumentException("Kích thước phải hữu hạn và không âm");
        }
        float min = Math.min(halfExtent, viewSize / 2f);
        return new float[]{min, viewSize - min};
    }
}
