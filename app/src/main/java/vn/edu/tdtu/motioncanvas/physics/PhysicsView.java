package vn.edu.tdtu.motioncanvas.physics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Random;

import vn.edu.tdtu.motioncanvas.R;
import vn.edu.tdtu.motioncanvas.core.FrameClock;

/** Vẽ World và nhận chạm, chỉ bóng gắn lò xo được kéo bằng tay */
public class PhysicsView extends View {
    // Tính cả bóng con lắc để tổng số Body luôn nằm trong giới hạn
    public static final int MAX_BODIES = 50;
    private static final int NO_POINTER = -1;
    private static final int BACKGROUND_COLOR = 0xFF263238;
    private static final int SPRING_COLOR = 0xFFB0BEC5;
    private static final int PENDULUM_COLOR = 0xFF7986CB;
    private static final int[] BALL_COLORS = {
            0xFFEF5350, 0xFFAB47BC, 0xFF42A5F5,
            0xFF26A69A, 0xFFFFCA28, 0xFFFF7043
    };
    private static final float MIN_RADIUS_DP = 12f;
    private static final float MAX_RADIUS_DP = 32f;
    private static final float PENDULUM_RADIUS_DP = 22f;
    private static final float REST_LENGTH_DP = 120f;
    private static final float ANCHOR_Y_DP = 40f;
    private static final float STIFFNESS_PER_MASS = 40f;
    private static final float DAMPING_PER_MASS = 1.2f;

    private final World world = new World(0f, 0f, MAX_BODIES);
    private final FrameClock clock = new FrameClock();
    private final Random random = new Random();
    private final Paint ballPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint springPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private Body pendulum;
    private Body draggedBody;
    private int activePointerId = NO_POINTER;
    private float dragOffsetX;
    private float dragOffsetY;
    private boolean running;
    private Toast limitToast;

    public PhysicsView(Context context) {
        this(context, null);
    }

    public PhysicsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PhysicsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;
        springPaint.setColor(SPRING_COLOR);
        springPaint.setStrokeWidth(3f * density);
        springPaint.setStrokeCap(Paint.Cap.ROUND);
        setClickable(true);
    }

    public World getWorld() {
        return world;
    }

    public void resume() {
        if (running) return;
        clock.reset();
        running = true;
        postInvalidateOnAnimation();
    }

    public void pause() {
        running = false;
        finishTouch();
        cancelLimitToast();
    }

    /** Xóa bóng thường, tạo lại con lắc, giữ các giá trị thanh trượt */
    public void reset() {
        finishTouch();
        cancelLimitToast();
        world.clear();
        pendulum = null;
        if (getWidth() > 0 && getHeight() > 0) {
            createPendulum(getWidth(), getHeight());
        }
        clock.reset();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        world.setBounds(w, h);
        // Đổi vùng vẽ thì dựng lại cảnh để điểm neo luôn ở góc phải trên
        reset();
    }

    private void createPendulum(float width, float height) {
        float radius = Math.min(PENDULUM_RADIUS_DP * density, Math.min(width, height) / 2f);
        float anchorX = clampCenter(width * 0.75f, radius, width);
        float anchorY = Math.min(ANCHOR_Y_DP * density, height * 0.15f);
        float restLength = Math.min(REST_LENGTH_DP * density, Math.max(0f, height - radius - anchorY) * 0.6f);
        float centerY = clampCenter(anchorY + restLength, radius, height);
        pendulum = new Body(anchorX, centerY, radius, PENDULUM_COLOR);
        world.addBody(pendulum);
        // k và c tăng theo khối lượng để giữ nhịp dao động
        world.addSpring(new Spring(pendulum, anchorX, anchorY, restLength, STIFFNESS_PER_MASS * pendulum.mass, DAMPING_PER_MASS * pendulum.mass));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        // Frame còn chờ sau pause chỉ vẽ lại
        if (running) {
            world.step(clock.tick());
        }
        canvas.drawColor(BACKGROUND_COLOR);
        List<Spring> springs = world.getSprings();
        ballPaint.setColor(SPRING_COLOR);
        for (int i = 0; i < springs.size(); i++) {
            Spring spring = springs.get(i);
            canvas.drawLine(spring.anchorX, spring.anchorY, spring.body.x, spring.body.y, springPaint);
            canvas.drawCircle(spring.anchorX, spring.anchorY, 6f * density, ballPaint);
        }
        List<Body> bodies = world.getBodies();
        for (int i = 0; i < bodies.size(); i++) {
            Body body = bodies.get(i);
            ballPaint.setColor(body.color);
            canvas.drawCircle(body.x, body.y, body.radius, ballPaint);
        }
        if (running) {
            postInvalidateOnAnimation();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!running || getWidth() <= 0 || getHeight() <= 0) return false;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                finishTouch();
                activePointerId = event.getPointerId(0);
                float x = event.getX();
                float y = event.getY();
                if (touchesPendulum(x, y)) {
                    draggedBody = pendulum;
                    draggedBody.held = true;
                    draggedBody.vx = 0f;
                    draggedBody.vy = 0f;
                    dragOffsetX = draggedBody.x - x;
                    dragOffsetY = draggedBody.y - y;
                } else {
                    spawnBall(x, y);
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                int index = event.findPointerIndex(activePointerId);
                if (index < 0) {
                    finishTouch();
                } else {
                    movePendulum(event.getX(index), event.getY(index));
                }
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                if (event.getPointerId(event.getActionIndex()) == activePointerId) {
                    finishTouch();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (event.getPointerId(event.getActionIndex()) == activePointerId) {
                    movePendulum(event.getX(event.getActionIndex()), event.getY(event.getActionIndex()));
                    finishTouch();
                    performClick();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                finishTouch();
                return true;
            default:
                // Bỏ qua ngón thứ hai
                return true;
        }
    }

    private boolean touchesPendulum(float x, float y) {
        if (pendulum == null) return false;
        float dx = x - pendulum.x;
        float dy = y - pendulum.y;
        return dx * dx + dy * dy <= pendulum.radius * pendulum.radius;
    }

    private void movePendulum(float x, float y) {
        if (draggedBody == null) return;
        // Engine không chặn tường khi held, View chặn vị trí kéo
        draggedBody.x = clampCenter(x + dragOffsetX, draggedBody.radius, getWidth());
        draggedBody.y = clampCenter(y + dragOffsetY, draggedBody.radius, getHeight());
    }

    private void finishTouch() {
        if (draggedBody != null) {
            draggedBody.held = false;
            draggedBody = null;
        }
        activePointerId = NO_POINTER;
    }

    private void spawnBall(float x, float y) {
        if (world.getBodies().size() >= MAX_BODIES) {
            cancelLimitToast();
            limitToast = Toast.makeText(getContext(), getResources().getString(R.string.physics_limit, MAX_BODIES), Toast.LENGTH_SHORT);
            limitToast.show();
            return;
        }
        float radius = (MIN_RADIUS_DP + random.nextFloat() * (MAX_RADIUS_DP - MIN_RADIUS_DP)) * density;
        radius = Math.min(radius, Math.min(getWidth(), getHeight()) / 2f);
        int color = BALL_COLORS[random.nextInt(BALL_COLORS.length)];
        world.addBody(new Body(clampCenter(x, radius, getWidth()), clampCenter(y, radius, getHeight()), radius, color));
    }

    private static float clampCenter(float position, float radius, float size) {
        if (size <= 2f * radius) return size / 2f;
        return Math.max(radius, Math.min(position, size - radius));
    }

    private void cancelLimitToast() {
        if (limitToast != null) {
            limitToast.cancel();
            limitToast = null;
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    protected void onDetachedFromWindow() {
        pause();
        super.onDetachedFromWindow();
    }
}
