package vn.edu.tdtu.motioncanvas.physics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Engine bóng tròn Java thuần. Mỗi frame chia 4 bước nhỏ, tích phân Euler bán ẩn.
 * Dùng trên một thread, callback chỉ nhận sự kiện, không thêm/xóa bóng trong step().
 * Mô phỏng rời rạc có giới hạn: bước con không bảo đảm bắt mọi va chạm ở tốc độ rất lớn.
 */
public class World {
    public static final float MAX_DT = 1f / 30f;
    public static final float MIN_IMPACT_SPEED = 150f;
    private static final int SUBSTEPS = 4;

    public interface CollisionListener {
        /** Điểm tiếp xúc (px), tốc độ tiến lại theo pháp tuyến trước va chạm (px/s) */
        void onCollision(float x, float y, float impactSpeed);
    }

    private final List<Body> bodies = new ArrayList<>();
    private final List<Spring> springs = new ArrayList<>();
    // Tạo wrapper một lần, không cấp phát lại mỗi khi View đọc danh sách để vẽ.
    private final List<Body> bodyView = Collections.unmodifiableList(bodies);
    private final List<Spring> springView = Collections.unmodifiableList(springs);
    private final int maxBodies;
    private float width;
    private float height;
    private float gravity = 2000f;
    private float restitution = 0.8f;
    private CollisionListener collisionListener;

    public World(float width, float height, int maxBodies) {
        if (maxBodies < 0) throw new IllegalArgumentException("maxBodies không được âm");
        this.maxBodies = maxBodies;
        setBounds(width, height);
    }

    public void setBounds(float w, float h) {
        if (!Float.isFinite(w) || !Float.isFinite(h) || w < 0f || h < 0f) {
            throw new IllegalArgumentException("Kích thước vùng mô phỏng không hợp lệ");
        }
        width = w;
        height = h;
    }

    public void setGravity(float pxPerSec2) {
        if (!Float.isFinite(pxPerSec2)) throw new IllegalArgumentException("Trọng lực phải hữu hạn");
        gravity = pxPerSec2;
    }

    public void setRestitution(float e) {
        if (Float.isNaN(e)) throw new IllegalArgumentException("Độ nảy không được là NaN");
        restitution = Math.max(0f, Math.min(e, 1f));
    }

    public boolean addBody(Body b) {
        Objects.requireNonNull(b, "body");
        if (bodies.size() == maxBodies) return false;
        bodies.add(b);
        return true;
    }

    /** Body cần được addBody trước, mỗi đối tượng Body chỉ thêm một lần vào World */
    public void addSpring(Spring s) {
        springs.add(Objects.requireNonNull(s, "spring"));
    }

    public void clear() {
        springs.clear();
        bodies.clear();
    }

    /** List chỉ đọc, nhưng các Body bên trong vẫn có trạng thái thay đổi mỗi frame */
    public List<Body> getBodies() {
        return bodyView;
    }

    public List<Spring> getSprings() {
        return springView;
    }

    public Body findBodyAt(float x, float y) {
        for (int index = bodies.size() - 1; index >= 0; index--) {
            Body body = bodies.get(index);
            float dx = x - body.x;
            float dy = y - body.y;
            if (dx * dx + dy * dy <= body.radius * body.radius) return body;
        }
        return null;
    }

    public void setCollisionListener(CollisionListener listener) {
        collisionListener = listener;
    }

    /** dt tính bằng giây: bỏ qua 0, số âm, NaN và chặn thời gian quá lớn */
    public void step(float dt) {
        if (!(dt > 0f)) return;
        float h = Math.min(dt, MAX_DT) / SUBSTEPS;
        for (int substep = 0; substep < SUBSTEPS; substep++) {
            accelerateFromSprings(h);
            for (int i = 0; i < bodies.size(); i++) {
                Body body = bodies.get(i);
                if (body.held) continue;
                body.vy += gravity * h;
                body.x += body.vx * h;
                body.y += body.vy * h;
                constrainToBounds(body);
            }
            for (int i = 0; i < bodies.size(); i++) {
                for (int j = i + 1; j < bodies.size(); j++) {
                    resolvePair(bodies.get(i), bodies.get(j));
                }
            }
            // Tách bóng chồng nhau có thể đẩy một bóng qua tường ở cuối bước
            for (int i = 0; i < bodies.size(); i++) {
                Body body = bodies.get(i);
                if (!body.held) constrainToBounds(body);
            }
        }
    }

    private void accelerateFromSprings(float h) {
        for (int i = 0; i < springs.size(); i++) {
            Spring spring = springs.get(i);
            Body body = spring.body;
            if (body.held) continue;
            float dx = spring.anchorX - body.x;
            float dy = spring.anchorY - body.y;
            float length = (float) Math.hypot(dx, dy);
            // Tại đúng điểm neo, phương đàn hồi chưa xác định, vẫn áp dụng lực cản
            float pull = length > 0f
                    ? spring.stiffness * (length - spring.restLength) / length : 0f;
            float velocityFactor = h / body.mass;
            body.vx += (pull * dx - spring.damping * body.vx) * velocityFactor;
            body.vy += (pull * dy - spring.damping * body.vy) * velocityFactor;
        }
    }

    private void constrainToBounds(Body body) {
        float radius = body.radius;
        if (width <= 2f * radius) {
            body.x = width * 0.5f;
            body.vx = 0f;
        } else if (body.x < radius) {
            body.x = radius;
            if (body.vx < 0f) {
                report(0f, body.y, -body.vx);
                body.vx *= -restitution;
            }
        } else if (body.x > width - radius) {
            body.x = width - radius;
            if (body.vx > 0f) {
                report(width, body.y, body.vx);
                body.vx *= -restitution;
            }
        }
        if (height <= 2f * radius) {
            body.y = height * 0.5f;
            body.vy = 0f;
        } else if (body.y < radius) {
            body.y = radius;
            if (body.vy < 0f) {
                report(body.x, 0f, -body.vy);
                body.vy *= -restitution;
            }
        } else if (body.y > height - radius) {
            body.y = height - radius;
            if (body.vy > 0f) {
                report(body.x, height, body.vy);
                body.vy *= -restitution;
            }
        }
    }

    private void resolvePair(Body a, Body b) {
        float dx = b.x - a.x;
        float dy = b.y - a.y;
        float distance = (float) Math.hypot(dx, dy);
        float overlap = a.radius + b.radius - distance;
        if (overlap <= 0f) return;

        float inverseA = a.inverseMass();
        float inverseB = b.inverseMass();
        float inverseTotal = inverseA + inverseB;
        if (inverseTotal == 0f) return;

        // Hai tâm trùng nhau: chọn trục X để vẫn tách được, không chia cho 0
        float nx = distance > 0f ? dx / distance : 1f;
        float ny = distance > 0f ? dy / distance : 0f;
        float correction = overlap / inverseTotal;
        a.x -= correction * inverseA * nx;
        a.y -= correction * inverseA * ny;
        b.x += correction * inverseB * nx;
        b.y += correction * inverseB * ny;

        // Body đang held được xem đứng yên, kể cả View chưa xóa vận tốc cũ
        float relativeX = (b.held ? 0f : b.vx) - (a.held ? 0f : a.vx);
        float relativeY = (b.held ? 0f : b.vy) - (a.held ? 0f : a.vy);
        float normalVelocity = relativeX * nx + relativeY * ny;
        if (normalVelocity >= 0f) return;

        // Xung lực làm vận tốc tương đối sau va chạm bằng -e lần trước va chạm
        float impulse = -(1f + restitution) * normalVelocity / inverseTotal;
        a.vx -= impulse * inverseA * nx;
        a.vy -= impulse * inverseA * ny;
        b.vx += impulse * inverseB * nx;
        b.vy += impulse * inverseB * ny;
        report(a.x + nx * a.radius, a.y + ny * a.radius, -normalVelocity);
    }

    private void report(float x, float y, float speed) {
        if (collisionListener != null && speed >= MIN_IMPACT_SPEED) {
            collisionListener.onCollision(x, y, speed);
        }
    }
}
