package vn.edu.tdtu.motioncanvas.particle;

/** Bảng màu theo tuổi hạt: t = 0 (mới sinh) đến 1 (sắp chết), ngoài khoảng thì bị cắt. */
public final class ParticlePalette {
    private ParticlePalette() {
    }

    /** Màu ARGB của hạt lửa: vàng đỏ, mờ dần theo tuổi. */
    public static int fire(float t) {
        float remaining = 1f - clamp(t);
        int alpha = (int) (255f * remaining);
        int green = (int) (220f * remaining);
        int blue = (int) (60f * remaining * remaining);
        return argb(alpha, 255, green, blue);
    }

    /** Màu ARGB của giọt nước: xanh, mờ nhẹ theo tuổi. */
    public static int water(float t) {
        int alpha = (int) (220f * (1f - 0.7f * clamp(t)));
        return argb(alpha, 90, 170, 255);
    }

    private static float clamp(float t) {
        if (!(t > 0f)) return 0f;
        return Math.min(t, 1f);
    }

    // Ghép bốn kênh màu, không cần phụ thuộc android.graphics.Color
    private static int argb(int alpha, int red, int green, int blue) {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
}
