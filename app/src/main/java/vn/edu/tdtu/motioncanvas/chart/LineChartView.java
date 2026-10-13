package vn.edu.tdtu.motioncanvas.chart;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Supplier;

/** Line chart nhận điểm mới định kỳ và trượt mượt sang trái; chạm để xem giá trị. */
public class LineChartView extends View {
    public static final long SAMPLE_INTERVAL_MS = 400;
    private static final int VISIBLE_POINTS = 30;
    private static final int GRID_LINES = 5;
    private static final float WALK_STEP = 20f;
    private static final float WALK_MIN = 0f;
    private static final float WALK_MAX = 100f;
    private static final float Y_PADDING_RATIO = 0.1f;
    private static final int LINE_COLOR = 0xFF26A69A;
    private static final int FILL_COLOR = 0x3326A69A;
    private static final float LINE_DP = 2.5f;
    private static final float MARKER_DP = 5f;
    private static final float LABEL_GAP_DP = 6f;

    private final List<Float> points = new ArrayList<>();
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint;
    private final Paint axisLabelPaint;
    private final Paint valuePaint;
    private final Path linePath = new Path();
    private final Path fillPath = new Path();
    private final RectF plot = new RectF();
    private final ValueAnimator scrollAnimator = ValueAnimator.ofFloat(1f, 0f);
    private final Random random = new Random();
    private final float density;
    private Supplier<Float> dataSource = this::randomWalk;
    private float lastValue = (WALK_MIN + WALK_MAX) / 2f;
    /** 1 → 0 trong mỗi chu kỳ lấy mẫu: phần bước mà điểm mới nhất còn phải trượt vào. */
    private float scrollOffset;
    private int selectedIndex = -1;

    private final Runnable sampler = new Runnable() {
        @Override
        public void run() {
            addPoint(dataSource.get());
            postDelayed(this, SAMPLE_INTERVAL_MS);
        }
    };

    public LineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        linePaint.setColor(LINE_COLOR);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(LINE_DP * density);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        fillPaint.setColor(FILL_COLOR);
        markerPaint.setColor(LINE_COLOR);
        gridPaint = ChartAxes.gridPaint(density);
        axisLabelPaint = ChartAxes.labelPaint(context, Paint.Align.RIGHT);
        valuePaint = ChartAxes.labelPaint(context, Paint.Align.CENTER);
        scrollAnimator.setDuration(SAMPLE_INTERVAL_MS);
        scrollAnimator.setInterpolator(new LinearInterpolator());
        scrollAnimator.addUpdateListener(a -> {
            scrollOffset = (float) a.getAnimatedValue();
            invalidate();
        });
    }

    /** Thay nguồn dữ liệu (mặc định: random walk 0..100). Gọi trên main thread. */
    public void setDataSource(Supplier<Float> source) {
        dataSource = source;
    }

    private float randomWalk() {
        float next = lastValue + (random.nextFloat() - 0.5f) * WALK_STEP;
        lastValue = Math.max(WALK_MIN, Math.min(WALK_MAX, next));
        return lastValue;
    }

    private void addPoint(float value) {
        points.add(value);
        if (points.size() > VISIBLE_POINTS + 1) {
            points.remove(0);
            selectedIndex--; // giữ tooltip bám đúng điểm khi dữ liệu trượt sang trái
        }
        scrollAnimator.start();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        removeCallbacks(sampler);
        post(sampler);
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(sampler);
        scrollAnimator.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        ChartAxes.plotArea(plot, w, h, density);
    }

    private float stepWidth() {
        return plot.width() / (VISIBLE_POINTS - 1);
    }

    /** Điểm mới nhất nằm ở mép phải (+ phần đang trượt vào), các điểm cũ lùi dần sang trái. */
    private float xAt(int index, float step) {
        return plot.right - (points.size() - 1 - index - scrollOffset) * step;
    }

    private ChartScale yScale() {
        float lo = Float.MAX_VALUE;
        float hi = -Float.MAX_VALUE;
        for (float v : points) {
            lo = Math.min(lo, v);
            hi = Math.max(hi, v);
        }
        float pad = Math.max((hi - lo) * Y_PADDING_RATIO, 1f);
        return new ChartScale(lo - pad, hi + pad, plot.bottom, plot.top);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (points.isEmpty() || plot.isEmpty()) return;
        ChartScale scale = yScale();
        ChartAxes.drawHorizontalGrid(canvas, plot, scale, GRID_LINES, LABEL_GAP_DP * density,
                gridPaint, axisLabelPaint);

        float step = stepWidth();
        linePath.reset();
        fillPath.reset();
        for (int i = 0; i < points.size(); i++) {
            float px = xAt(i, step);
            float py = scale.toPixel(points.get(i));
            if (i == 0) {
                linePath.moveTo(px, py);
                fillPath.moveTo(px, plot.bottom);
            } else {
                linePath.lineTo(px, py);
            }
            fillPath.lineTo(px, py);
        }
        fillPath.lineTo(xAt(points.size() - 1, step), plot.bottom);
        fillPath.close();

        canvas.save();
        canvas.clipRect(plot);
        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);
        canvas.restore();

        if (selectedIndex >= 0 && selectedIndex < points.size()) {
            float value = points.get(selectedIndex);
            float px = xAt(selectedIndex, step);
            float py = scale.toPixel(value);
            canvas.drawCircle(px, py, MARKER_DP * density, markerPaint);
            canvas.drawText(String.format(Locale.US, "%.1f", value), px, py - 2 * MARKER_DP * density, valuePaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // Không để ScrollView cha cướp cử chỉ kéo ngang trên chart
                getParent().requestDisallowInterceptTouchEvent(true);
                selectAt(e.getX());
                return true;
            case MotionEvent.ACTION_MOVE:
                selectAt(e.getX());
                return true;
            case MotionEvent.ACTION_UP:
                performClick();
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void selectAt(float x) {
        float step = stepWidth();
        selectedIndex = ChartMath.nearestIndex(x, xAt(0, step), step, points.size());
        invalidate();
    }
}
