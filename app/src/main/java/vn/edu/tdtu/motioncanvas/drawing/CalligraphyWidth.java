package vn.edu.tdtu.motioncanvas.drawing;

/** Brush thư pháp: tay càng nhanh nét càng mảnh. */
final class CalligraphyWidth {
    static final float MIN = 0.3f;
    static final float MAX = 1.2f;
    /** Tốc độ (dp/ms) từ đó trở lên thì nét mảnh nhất. */
    private static final float SPEED_FOR_MIN = 3f;
    /** Mỗi điểm chỉ tiến 30% về độ dày mới → nét đổi độ dày êm, không giật. */
    private static final float SMOOTHING = 0.3f;

    private CalligraphyWidth() {
    }

    static float factor(float speedDpPerMs) {
        if (!(speedDpPerMs > 0f)) return MAX; // 0, âm hoặc NaN: coi như đứng yên
        float t = Math.min(1f, speedDpPerMs / SPEED_FOR_MIN);
        return MAX - (MAX - MIN) * t;
    }

    static float smooth(float previous, float target) {
        return previous + SMOOTHING * (target - previous);
    }
}
