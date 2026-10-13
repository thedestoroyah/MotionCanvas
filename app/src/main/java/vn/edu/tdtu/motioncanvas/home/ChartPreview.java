package vn.edu.tdtu.motioncanvas.home;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/** Năm cột lên xuống theo hình sin lệch pha nhau. */
public final class ChartPreview implements Preview {
    private static final int BARS = 5;
    private static final int COLOR = 0xFF26A69A;
    private static final float SPEED = 2.5f;
    private static final float PHASE = 0.9f;
    private static final float BASE = 0.35f;
    private static final float SWING = 0.3f;
    private static final float BAR_FILL = 0.6f;
    private static final float TIME_WRAP_S = 1000f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bar = new RectF();
    private float time;

    public ChartPreview() {
        paint.setColor(COLOR);
    }

    @Override
    public void update(float dt, int width, int height) {
        time = (time + dt) % TIME_WRAP_S;
    }

    @Override
    public void draw(Canvas canvas, int width, int height) {
        float slot = width * 0.8f / BARS;
        float bottom = height * 0.88f;
        float corner = slot * 0.15f;
        for (int i = 0; i < BARS; i++) {
            float fraction = BASE + SWING * (float) Math.sin(time * SPEED + i * PHASE);
            float left = width * 0.1f + slot * i + slot * (1f - BAR_FILL) / 2f;
            bar.set(left, bottom - fraction * height, left + slot * BAR_FILL, bottom);
            canvas.drawRoundRect(bar, corner, corner, paint);
        }
    }
}
