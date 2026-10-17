package vn.edu.tdtu.motioncanvas.physics;

/** Bóng tròn: tọa độ pixel, vận tốc pixel/giây. View được đổi vị trí khi kéo */
public class Body {
    public float x;
    public float y;
    public float vx;
    public float vy;
    public final float radius;
    public final float mass;
    public final int color;
    /** Đang được tay giữ: không tích phân chuyển động, không bị bóng khác đẩy */
    public boolean held;

    public Body(float x, float y, float radius, int color) {
        if (!Float.isFinite(radius) || radius <= 0f
                || !Float.isFinite(radius * radius) || radius * radius == 0f) {
            throw new IllegalArgumentException("Bán kính phải dương và có khối lượng hữu hạn");
        }
        this.x = x;
        this.y = y;
        this.radius = radius;
        // Bỏ hệ số pi chung: chỉ cần tỉ lệ khối lượng giữa các bóng cùng mật độ
        this.mass = radius * radius;
        this.color = color;
    }

    float inverseMass() {
        return held ? 0f : 1f / mass;
    }
}
