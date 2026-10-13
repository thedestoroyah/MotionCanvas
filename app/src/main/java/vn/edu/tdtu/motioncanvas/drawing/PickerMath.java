package vn.edu.tdtu.motioncanvas.drawing;

final class PickerMath {
    private PickerMath() {
    }

    /** Hue (0..360) của điểm (dx, dy) so với tâm; trục y hướng xuống nên tăng theo chiều kim đồng hồ. */
    static float hueAt(float dx, float dy) {
        float deg = (float) Math.toDegrees(Math.atan2(dy, dx));
        return deg < 0f ? deg + 360f : deg;
    }

    /** Vị trí tương đối của pos trong [start, start + length], kẹp trong [0, 1]. */
    static float fraction(float pos, float start, float length) {
        if (length <= 0f) return 0f;
        return Math.max(0f, Math.min(1f, (pos - start) / length));
    }
}
