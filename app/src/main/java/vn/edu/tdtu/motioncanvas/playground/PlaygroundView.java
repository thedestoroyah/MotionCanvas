package vn.edu.tdtu.motioncanvas.playground;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;

import vn.edu.tdtu.motioncanvas.core.FrameClock;
import vn.edu.tdtu.motioncanvas.particle.Emitter;
import vn.edu.tdtu.motioncanvas.particle.Particle;
import vn.edu.tdtu.motioncanvas.particle.ParticlePalette;
import vn.edu.tdtu.motioncanvas.physics.Body;
import vn.edu.tdtu.motioncanvas.physics.World;

/** Ghép World (A) + Emitter (A): chạm để thả bóng, mỗi va chạm mạnh bung ra tia lửa. */
public class PlaygroundView extends View {
    private static final int MAX_BODIES = 40;
    private static final int SPARK_CAPACITY = 1500;
    private static final int BURST_SIZE = 14;
    private static final float GRAVITY_DP = 800f;
    private static final float RESTITUTION = 0.85f;
    private static final float MIN_RADIUS_DP = 10f;
    private static final float MAX_RADIUS_DP = 26f;
    private static final float MAX_SIDE_SPEED_DP = 300f;
    private static final float SPARK_SHRINK = 0.5f;
    private static final int BACKGROUND = 0xFF1A237E;
    private static final int[] PALETTE = {0xFFFFCA28, 0xFF26C6DA, 0xFFEC407A, 0xFF9CCC65};

    private final World world = new World(1f, 1f, MAX_BODIES);
    private final Emitter sparks;
    private final FrameClock clock = new FrameClock();
    private final Random random = new Random();
    private final Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sparkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private int collisionsSinceSample;
    private boolean running;

    public PlaygroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        world.setGravity(GRAVITY_DP * density);
        world.setRestitution(RESTITUTION);
        sparks = new Emitter(SPARK_CAPACITY, density, random);
        sparks.setEmitting(false); // chỉ bung hạt khi có va chạm
        sparkPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
        world.setCollisionListener((x, y, impactSpeed) -> {
            sparks.burst(x, y, BURST_SIZE);
            collisionsSinceSample++;
        });
    }

    /** Số va chạm kể từ lần gọi trước, rồi đếm lại từ 0. */
    public float takeCollisionCount() {
        int count = collisionsSinceSample;
        collisionsSinceSample = 0;
        return count;
    }

    public void resume() {
        running = true;
        clock.reset();
        postInvalidateOnAnimation();
    }

    public void pause() {
        running = false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        world.setBounds(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float dt = clock.tick();
        world.step(dt);
        sparks.update(dt);

        canvas.drawColor(BACKGROUND);
        for (Body b : world.getBodies()) {
            bodyPaint.setColor(b.color);
            canvas.drawCircle(b.x, b.y, b.radius, bodyPaint);
        }
        for (int i = 0; i < sparks.capacity(); i++) {
            Particle p = sparks.get(i);
            if (!p.alive) continue;
            float t = p.progress();
            sparkPaint.setColor(ParticlePalette.fire(t));
            canvas.drawCircle(p.x, p.y, p.size * (1f - SPARK_SHRINK * t), sparkPaint);
        }
        if (running) postInvalidateOnAnimation();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            float radius = (MIN_RADIUS_DP + random.nextFloat() * (MAX_RADIUS_DP - MIN_RADIUS_DP)) * density;
            Body ball = new Body(e.getX(), e.getY(), radius, PALETTE[random.nextInt(PALETTE.length)]);
            ball.vx = (random.nextFloat() * 2f - 1f) * MAX_SIDE_SPEED_DP * density;
            world.addBody(ball); // đủ MAX_BODIES thì bỏ qua — chỉ là sân chơi
            return true;
        }
        if (e.getActionMasked() == MotionEvent.ACTION_UP) {
            performClick();
            return true;
        }
        return super.onTouchEvent(e);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
