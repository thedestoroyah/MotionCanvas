package vn.edu.tdtu.motioncanvas.home;

import android.graphics.Canvas;
import android.graphics.Paint;

/** Ba quả bóng rơi và nảy; khi nảy quá thấp thì được tung lên lại để hình động không dừng. */
public final class PhysicsPreview implements Preview {
    private static final int[] COLORS = {0xFFEF5350, 0xFF42A5F5, 0xFFFFCA28};
    // Đơn vị tính theo chiều cao thẻ (h) để mọi kích thước thẻ trông như nhau
    private static final float GRAVITY_H = 3f;
    private static final float RESTITUTION = 0.8f;
    private static final float MIN_BOUNCE_H = 0.9f;
    private static final float RELAUNCH_H = 2.2f;
    private static final float RADIUS_H = 0.08f;

    private final float[] y = new float[COLORS.length];
    private final float[] vy = new float[COLORS.length];
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean started;

    @Override
    public void update(float dt, int width, int height) {
        if (height == 0) return;
        if (!started) {
            for (int i = 0; i < y.length; i++) y[i] = height * (0.2f + 0.2f * i);
            started = true;
        }
        float floor = height - height * RADIUS_H;
        for (int i = 0; i < y.length; i++) {
            vy[i] += GRAVITY_H * height * dt;
            y[i] += vy[i] * dt;
            if (y[i] > floor) {
                y[i] = floor;
                vy[i] = -vy[i] * RESTITUTION;
                if (-vy[i] < MIN_BOUNCE_H * height) vy[i] = -RELAUNCH_H * height;
            }
        }
    }

    @Override
    public void draw(Canvas canvas, int width, int height) {
        float radius = height * RADIUS_H;
        for (int i = 0; i < y.length; i++) {
            paint.setColor(COLORS[i]);
            canvas.drawCircle(width * (i + 1f) / (y.length + 1f), y[i], radius, paint);
        }
    }
}
