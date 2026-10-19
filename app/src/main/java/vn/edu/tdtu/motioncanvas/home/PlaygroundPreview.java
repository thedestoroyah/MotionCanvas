package vn.edu.tdtu.motioncanvas.home;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.Arrays;

/** Nền xanh đêm: hai bóng nảy trên sàn, mỗi lần chạm sàn bung một chùm tia lửa nhỏ rồi mờ dần. */
public final class PlaygroundPreview implements Preview {
    private static final int BACKGROUND = 0xFF1A237E;
    private static final int[] COLORS = {0xFFFFCA28, 0xFF26C6DA};
    private static final int SPARKS_PER_HIT = 6;
    private static final float SPARK_LIFE = 0.5f;
    // Đơn vị tính theo chiều cao thẻ (h) để mọi kích thước thẻ trông như nhau
    private static final float GRAVITY_H = 3.2f;
    private static final float RELAUNCH_H = 2.1f;
    private static final float RADIUS_H = 0.09f;
    private static final float SPARK_SPEED_H = 0.6f;
    private static final float SPARK_SIZE_H = 0.025f;

    private final float[] ballY = new float[COLORS.length];
    private final float[] ballVy = new float[COLORS.length];
    private final float[] sparkX = new float[COLORS.length * SPARKS_PER_HIT];
    private final float[] sparkY = new float[sparkX.length];
    private final float[] sparkAge = new float[sparkX.length];
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean started;

    @Override
    public void update(float dt, int width, int height) {
        if (height == 0) return;
        if (!started) {
            for (int i = 0; i < ballY.length; i++) ballY[i] = height * (0.2f + 0.3f * i);
            Arrays.fill(sparkAge, SPARK_LIFE);
            started = true;
        }
        float floor = height - height * RADIUS_H;
        for (int i = 0; i < ballY.length; i++) {
            ballVy[i] += GRAVITY_H * height * dt;
            ballY[i] += ballVy[i] * dt;
            if (ballY[i] > floor) {
                ballY[i] = floor;
                ballVy[i] = -RELAUNCH_H * height;
                burst(i, width * (i + 1f) / (COLORS.length + 1f), floor);
            }
        }
        for (int s = 0; s < sparkAge.length; s++) sparkAge[s] += dt;
    }

    /** Ghi đè chùm tia của bóng `ball`: mỗi bóng có chỗ riêng nên không cần pool chung. */
    private void burst(int ball, float x, float y) {
        for (int k = 0; k < SPARKS_PER_HIT; k++) {
            int s = ball * SPARKS_PER_HIT + k;
            sparkX[s] = x;
            sparkY[s] = y;
            sparkAge[s] = 0f;
        }
    }

    @Override
    public void draw(Canvas canvas, int width, int height) {
        canvas.drawColor(BACKGROUND);
        float radius = height * RADIUS_H;
        for (int i = 0; i < ballY.length; i++) {
            paint.setColor(COLORS[i]);
            canvas.drawCircle(width * (i + 1f) / (COLORS.length + 1f), ballY[i], radius, paint);
        }
        paint.setColor(0xFFFFB74D);
        for (int s = 0; s < sparkAge.length; s++) {
            if (sparkAge[s] >= SPARK_LIFE) continue;
            // Tia thứ k bay theo nửa vòng phía trên: góc chia đều từ 180° đến 360°
            double angle = Math.PI * (1d + (s % SPARKS_PER_HIT) / (SPARKS_PER_HIT - 1d));
            float distance = SPARK_SPEED_H * height * sparkAge[s];
            float fade = 1f - sparkAge[s] / SPARK_LIFE;
            paint.setAlpha((int) (255 * fade));
            canvas.drawCircle(sparkX[s] + (float) Math.cos(angle) * distance,
                    sparkY[s] + (float) Math.sin(angle) * distance, height * SPARK_SIZE_H, paint);
        }
        paint.setAlpha(255);
    }
}
