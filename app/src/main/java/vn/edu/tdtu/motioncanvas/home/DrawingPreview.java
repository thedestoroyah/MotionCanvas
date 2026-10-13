package vn.edu.tdtu.motioncanvas.home;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;

/** Nét lượn sóng tự vẽ dần (PathMeasure.getSegment), giữ một lúc, mờ đi rồi lặp lại. */
public final class DrawingPreview implements Preview {
    private static final float CYCLE_S = 3f;
    private static final float DRAW_S = 1.8f;
    private static final float FADE_START_S = 2.4f;
    private static final int COLOR = 0xFFEC407A;

    private final Path wave = new Path();
    private final Path segment = new Path();
    private final PathMeasure measure = new PathMeasure();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float length;
    private int builtWidth;
    private int builtHeight;
    private float time;

    public DrawingPreview() {
        paint.setColor(COLOR);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override
    public void update(float dt, int width, int height) {
        time = (time + dt) % CYCLE_S;
        if (width != builtWidth || height != builtHeight) build(width, height);
    }

    /** Dựng lại đường sóng chỉ khi kích thước thẻ đổi. */
    private void build(int w, int h) {
        wave.reset();
        wave.moveTo(w * 0.1f, h * 0.55f);
        wave.cubicTo(w * 0.25f, h * 0.1f, w * 0.35f, h * 0.9f, w * 0.5f, h * 0.5f);
        wave.cubicTo(w * 0.65f, h * 0.1f, w * 0.75f, h * 0.9f, w * 0.9f, h * 0.45f);
        measure.setPath(wave, false);
        length = measure.getLength();
        paint.setStrokeWidth(h * 0.05f);
        builtWidth = w;
        builtHeight = h;
    }

    @Override
    public void draw(Canvas canvas, int width, int height) {
        float drawn = Math.min(1f, time / DRAW_S);
        float alpha = time < FADE_START_S ? 1f : 1f - (time - FADE_START_S) / (CYCLE_S - FADE_START_S);
        segment.reset();
        measure.getSegment(0f, length * drawn, segment, true);
        paint.setAlpha((int) (255 * alpha));
        canvas.drawPath(segment, paint);
    }
}
