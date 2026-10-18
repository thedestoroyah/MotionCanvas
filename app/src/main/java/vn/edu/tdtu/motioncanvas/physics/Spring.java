package vn.edu.tdtu.motioncanvas.physics;

import java.util.Objects;

/** Lò xo có điểm neo cố định, lực đàn hồi và lực cản vận tốc (giảm chấn) */
public class Spring {
    public final Body body;
    public final float anchorX;
    public final float anchorY;
    public final float restLength;
    public final float stiffness;
    public final float damping;

    public Spring(Body body, float anchorX, float anchorY, float restLength, float stiffness, float damping) {
        this.body = Objects.requireNonNull(body, "body");
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.restLength = restLength;
        this.stiffness = stiffness;
        this.damping = damping;
    }
}
