package vn.edu.tdtu.motioncanvas.chart;

import android.animation.FloatArrayEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import java.util.Locale;

/** Bar chart: đổi dữ liệu thì các cột co giãn mượt bằng ValueAnimator + FloatArrayEvaluator. */
public class BarChartView extends View {
    public static final float MAX_VALUE = 100f;
    private static final String[] LABELS = {"T2", "T3", "T4", "T5", "T6", "T7"};
    private static final int GRID_LINES = 5;
    private static final long ANIMATION_MS = 700;
    private static final float OVERSHOOT_TENSION = 1.2f;
    private static final float BAR_FILL = 0.6f; // bề rộng cột / bề rộng ô
    private static final int BAR_COLOR = 0xFF42A5F5;
    private static final int SELECTED_COLOR = 0xFF1565C0;
    private static final float CORNER_DP = 4f;
    private static final float LABEL_GAP_DP = 6f;

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint;
    private final Paint axisLabelPaint;
    private final Paint labelPaint;
    private final RectF plot = new RectF();
    private final RectF bar = new RectF();
    private final float density;
    private float[] values = new float[LABELS.length];
    private ValueAnimator animator;
    private int selectedIndex = -1;

    public BarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        gridPaint = ChartAxes.gridPaint(density);
        axisLabelPaint = ChartAxes.labelPaint(context, Paint.Align.RIGHT);
        labelPaint = ChartAxes.labelPaint(context, Paint.Align.CENTER);
    }

    public static int barCount() {
        return LABELS.length;
    }

    /**
     * Đổi dữ liệu: các cột co giãn từ giá trị đang hiển thị tới `target` (có overshoot).
     *
     * @param target đúng barCount() giá trị, thang 0..MAX_VALUE (ngoài thang sẽ bị kẹp khi vẽ)
     * @throws IllegalArgumentException nếu số giá trị khác barCount()
     */
    public void setData(float[] target) {
        if (target.length != LABELS.length) {
            throw new IllegalArgumentException("Cần đúng " + LABELS.length + " giá trị");
        }
        if (animator != null) animator.cancel();
        // Bắt đầu từ giá trị đang hiển thị → đổi dữ liệu giữa chừng vẫn mượt
        animator = ValueAnimator.ofObject(new FloatArrayEvaluator(), values.clone(), target.clone());
        animator.setDuration(ANIMATION_MS);
        animator.setInterpolator(new OvershootInterpolator(OVERSHOOT_TENSION));
        animator.addUpdateListener(a -> {
            values = (float[]) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        ChartAxes.plotArea(plot, w, h, density);
    }

    private float slotWidth() {
        return plot.width() / LABELS.length;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (plot.isEmpty()) return;
        ChartScale scale = new ChartScale(0f, MAX_VALUE, plot.bottom, plot.top);
        ChartAxes.drawHorizontalGrid(canvas, plot, scale, GRID_LINES, LABEL_GAP_DP * density,
                gridPaint, axisLabelPaint);

        float slot = slotWidth();
        float corner = CORNER_DP * density;
        for (int i = 0; i < LABELS.length; i++) {
            // Overshoot có thể đẩy giá trị vượt [0, MAX] trong chốc lát
            float v = Math.max(0f, Math.min(MAX_VALUE, values[i]));
            float left = plot.left + slot * i + slot * (1f - BAR_FILL) / 2f;
            bar.set(left, scale.toPixel(v), left + slot * BAR_FILL, plot.bottom);
            barPaint.setColor(i == selectedIndex ? SELECTED_COLOR : BAR_COLOR);
            canvas.drawRoundRect(bar, corner, corner, barPaint);
            canvas.drawText(LABELS[i], bar.centerX(), plot.bottom + labelPaint.getTextSize() + 4f * density, labelPaint);
            if (i == selectedIndex) {
                canvas.drawText(String.format(Locale.US, "%.0f", v), bar.centerX(), bar.top - LABEL_GAP_DP * density, labelPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            float x = e.getX();
            float slot = slotWidth();
            boolean inside = x >= plot.left && x <= plot.right;
            selectedIndex = inside ? ChartMath.nearestIndex(x, plot.left + slot / 2f, slot, LABELS.length) : -1;
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
