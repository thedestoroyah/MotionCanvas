package vn.edu.tdtu.motioncanvas.chart;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.Locale;

import vn.edu.tdtu.motioncanvas.R;

/** Donut chart: các phần quét từ 0° đến 360°; chạm vào vòng để xem phần trăm. Chú thích bên phải. */
public class DonutChartView extends View {
    private static final String[] LABELS = {"Android", "iOS", "Web", "Desktop", "Khác"};
    private static final int[] COLORS = {0xFF66BB6A, 0xFF42A5F5, 0xFFFFA726, 0xFFAB47BC, 0xFF78909C};
    private static final long SWEEP_MS = 900;
    private static final float START_ANGLE = -90f; // 12 giờ
    private static final float RING_DP = 28f;
    private static final float SELECTED_RING_SCALE = 1.3f;
    private static final float MARGIN_DP = 8f;
    private static final float LEGEND_GAP_DP = 24f;
    private static final float LEGEND_DOT_DP = 6f;
    private static final float LEGEND_LINE_DP = 22f;
    private static final float CENTER_TEXT_SP = 16f;
    private static final int TEXT_COLOR = 0xFF424242;

    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint legendPaint;
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();
    private final ValueAnimator sweepAnimator = ValueAnimator.ofFloat(0f, 1f);
    private final float density;
    private final float ringWidth;
    private float[] sweeps = new float[LABELS.length];
    private float progress;
    private int selectedIndex = -1;
    private float centerX;
    private float centerY;
    private float radius;

    public DonutChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        ringWidth = RING_DP * density;
        arcPaint.setStyle(Paint.Style.STROKE);
        centerPaint.setColor(TEXT_COLOR);
        centerPaint.setTextAlign(Paint.Align.CENTER);
        centerPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, CENTER_TEXT_SP,
                getResources().getDisplayMetrics()));
        legendPaint = ChartAxes.labelPaint(context, Paint.Align.LEFT);
        legendPaint.setColor(TEXT_COLOR);
        sweepAnimator.setDuration(SWEEP_MS);
        sweepAnimator.setInterpolator(new DecelerateInterpolator());
        sweepAnimator.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidate();
        });
    }

    public static int sliceCount() {
        return LABELS.length;
    }

    public void setData(float[] values) {
        if (values.length != LABELS.length) {
            throw new IllegalArgumentException("Cần đúng " + LABELS.length + " giá trị");
        }
        sweeps = ChartMath.sweepAngles(values);
        selectedIndex = -1;
        sweepAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        sweepAnimator.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // Donut chiếm hình vuông bên trái, chú thích ở phần còn lại
        float size = Math.min(w / 2f, h);
        radius = (size - ringWidth * SELECTED_RING_SCALE) / 2f - MARGIN_DP * density;
        centerX = size / 2f;
        centerY = h / 2f;
        arcBounds.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
    }

    private boolean hasData() {
        for (float s : sweeps) {
            if (s > 0f) return true;
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (radius <= 0f) return;
        float start = START_ANGLE;
        for (int i = 0; i < sweeps.length; i++) {
            float sweep = sweeps[i] * progress;
            arcPaint.setColor(COLORS[i]);
            arcPaint.setStrokeWidth(i == selectedIndex ? ringWidth * SELECTED_RING_SCALE : ringWidth);
            canvas.drawArc(arcBounds, start, sweep, false, arcPaint);
            start += sweep;
        }

        String center;
        if (!hasData()) {
            center = getContext().getString(R.string.chart_no_data);
        } else if (selectedIndex >= 0) {
            center = String.format(Locale.US, "%.0f%%", sweeps[selectedIndex] / 3.6f);
        } else {
            center = getContext().getString(R.string.chart_tap_hint);
        }
        canvas.drawText(center, centerX, centerY + centerPaint.getTextSize() / 3f, centerPaint);
        drawLegend(canvas);
    }

    private void drawLegend(Canvas canvas) {
        float line = LEGEND_LINE_DP * density;
        float dot = LEGEND_DOT_DP * density;
        float x = centerX * 2f + LEGEND_GAP_DP * density;
        float y = centerY - (LABELS.length - 1) * line / 2f;
        for (int i = 0; i < LABELS.length; i++) {
            dotPaint.setColor(COLORS[i]);
            canvas.drawCircle(x, y, dot, dotPaint);
            canvas.drawText(LABELS[i], x + 2.5f * dot, y + legendPaint.getTextSize() / 3f, legendPaint);
            y += line;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            float dx = e.getX() - centerX;
            float dy = e.getY() - centerY;
            float dist = (float) Math.hypot(dx, dy);
            boolean onRing = Math.abs(dist - radius) <= ringWidth;
            selectedIndex = onRing ? ChartMath.sliceAt(ChartMath.angleFromTop(dx, dy), sweeps) : -1;
            invalidate();
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
