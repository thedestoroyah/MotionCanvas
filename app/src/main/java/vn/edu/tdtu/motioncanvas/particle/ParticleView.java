package vn.edu.tdtu.motioncanvas.particle;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.Random;

import vn.edu.tdtu.motioncanvas.R;
import vn.edu.tdtu.motioncanvas.core.FrameClock;

/** Vẽ pool của Emitter và cho phép kéo nguồn phát trên Canvas */
public class ParticleView extends View {
    private static final int CAPACITY = 2000;
    private static final int NO_POINTER = -1;
    private static final long HUD_INTERVAL_MS = 500L;
    private static final float FIRE_SHRINK = 0.7f;

    private final FrameClock clock = new FrameClock();
    private final Paint firePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint waterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint floorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hudBackground = new Paint();
    private final Emitter emitter;
    private final float density;
    private final float hudTextSize;
    private final float hudMargin;
    private float sourceX;
    private float sourceY;
    private float floorY;
    private float hudWidth;
    private String fpsText;
    private String countText;
    private boolean running;
    private int activePointerId = NO_POINTER;
    private int drawnFrames;
    private long sampleStartNanos;

    // Định dạng chuỗi ngoài onDraw, chỉ hai lần mỗi giây
    private final Runnable updateHud = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            long now = System.nanoTime();
            double seconds = (now - sampleStartNanos) / 1_000_000_000d;
            int fps = seconds > 0d ? (int) Math.round(drawnFrames / seconds) : 0;
            fpsText = getResources().getString(R.string.particle_fps, fps);
            updateCountText();
            fitHudText();
            drawnFrames = 0;
            sampleStartNanos = now;
            postDelayed(this, HUD_INTERVAL_MS);
        }
    };

    public ParticleView(Context context) {
        this(context, null);
    }

    public ParticleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ParticleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;
        emitter = new Emitter(CAPACITY, density, new Random());
        firePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
        floorPaint.setColor(0xFF78909C);
        floorPaint.setStrokeWidth(2f * density);
        hudPaint.setColor(Color.WHITE);
        hudBackground.setColor(Color.BLACK);
        hudTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, getResources().getDisplayMetrics());
        hudMargin = 12f * density;
        fpsText = getResources().getString(R.string.particle_fps_pending);
        updateCountText();
        hudPaint.setTextSize(hudTextSize);
        setClickable(true);
    }

    /** Chọn lửa hoặc nước cho hạt sinh từ giờ. */
    public void setMode(Emitter.Mode mode) {
        // Chỉ đổi mode cho hạt mới, hạt cũ giữ mode lúc sinh
        emitter.setMode(mode);
        invalidate();
    }

    /** Chạy vòng lặp vẽ và bộ đếm FPS; gọi ở onResume. */
    public void resume() {
        if (running || isInEditMode()) return;
        clock.reset();
        drawnFrames = 0;
        sampleStartNanos = System.nanoTime();
        fpsText = getResources().getString(R.string.particle_fps_pending);
        updateCountText();
        fitHudText();
        running = true;
        postDelayed(updateHud, HUD_INTERVAL_MS);
        postInvalidateOnAnimation();
    }

    /** Dừng vòng lặp và bộ đếm FPS; gọi ở onPause. */
    public void pause() {
        running = false;
        activePointerId = NO_POINTER;
        removeCallbacks(updateHud);
    }

    @Override
    protected void onDetachedFromWindow() {
        pause();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        floorY = h * 0.92f;
        emitter.setFloor(floorY);
        if (oldw <= 0 || oldh <= 0) {
            moveSource(w * 0.5f, h * 0.75f);
        } else {
            moveSource(sourceX * w / oldw, sourceY * h / oldh);
        }
        // Preview vẽ cảnh tĩnh, không chạy vòng lặp
        if (isInEditMode() && w > 0 && h > 0) {
            for (int i = 0; i < 45; i++) {
                emitter.update(1f / 60f);
            }
            updateCountText();
        }
        fitHudText();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        // Frame còn chờ sau pause chỉ vẽ lại
        if (running) {
            emitter.update(clock.tick());
            drawnFrames++;
        }
        canvas.drawColor(Color.BLACK);
        boolean showFloor = emitter.getMode() == Emitter.Mode.WATER;
        for (int i = 0; i < emitter.capacity(); i++) {
            Particle particle = emitter.get(i);
            if (!particle.alive) continue;
            float progress = particle.progress();
            if (particle.mode == Emitter.Mode.FIRE) {
                firePaint.setColor(ParticlePalette.fire(progress));
                float radius = particle.size * (1f - FIRE_SHRINK * progress);
                canvas.drawCircle(particle.x, particle.y, radius, firePaint);
            } else {
                showFloor = true;
                waterPaint.setColor(ParticlePalette.water(progress));
                canvas.drawCircle(particle.x, particle.y, particle.size, waterPaint);
            }
        }
        // Giữ sàn khi vẫn còn giọt nước cũ
        if (showFloor) {
            canvas.drawLine(0f, floorY, getWidth(), floorY, floorPaint);
        }
        drawHud(canvas);
        if (running) postInvalidateOnAnimation();
    }

    private void updateCountText() {
        countText = getResources().getString(R.string.particle_count, emitter.aliveCount(), emitter.capacity());
    }

    private void fitHudText() {
        hudPaint.setTextSize(hudTextSize);
        hudWidth = Math.max(hudPaint.measureText(fpsText), hudPaint.measureText(countText));
        float available = Math.max(1f, getWidth() - 2f * hudMargin);
        if (hudWidth > available) {
            hudPaint.setTextSize(hudTextSize * available / hudWidth);
            hudWidth = available;
        }
    }

    private void drawHud(Canvas canvas) {
        float lineHeight = hudPaint.descent() - hudPaint.ascent() + 4f * density;
        float baseline = hudMargin - hudPaint.ascent();
        canvas.drawRect(0f, 0f, hudWidth + 2f * hudMargin, 2f * hudMargin + 2f * lineHeight, hudBackground);
        canvas.drawText(fpsText, hudMargin, baseline, hudPaint);
        canvas.drawText(countText, hudMargin, baseline + lineHeight, hudPaint);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (!running || getWidth() <= 0 || getHeight() <= 0) return false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                activePointerId = event.getPointerId(0);
                moveSource(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_MOVE:
                int index = event.findPointerIndex(activePointerId);
                if (index >= 0) moveSource(event.getX(index), event.getY(index));
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                if (event.getPointerId(event.getActionIndex()) == activePointerId) {
                    // Chuyển sang ngón còn lại
                    int next = event.getActionIndex() == 0 ? 1 : 0;
                    activePointerId = event.getPointerId(next);
                    moveSource(event.getX(next), event.getY(next));
                }
                return true;
            case MotionEvent.ACTION_UP:
                moveSource(event.getX(), event.getY());
                activePointerId = NO_POINTER;
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                activePointerId = NO_POINTER;
                return true;
            default:
                return true;
        }
    }

    private void moveSource(float x, float y) {
        if (!Float.isFinite(x) || !Float.isFinite(y)) return;
        float side = Math.min(12f * density, getWidth() * 0.5f);
        float top = Math.min(12f * density, floorY * 0.5f);
        sourceX = Math.max(side, Math.min(x, getWidth() - side));
        sourceY = Math.max(top, Math.min(y, floorY - top));
        emitter.setPosition(sourceX, sourceY);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
