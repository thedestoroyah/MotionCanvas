package vn.edu.tdtu.motioncanvas.chart;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Line chart nhận điểm mới định kỳ và trượt mượt sang trái; chạm để xem giá trị.
 * Tùy chọn đường cong Catmull-Rom; điểm mới nhất có vòng sáng lan ra liên tục.
 */
public class LineChartView extends View {
    public static final long SAMPLE_INTERVAL_MS = 400;
    private static final int VISIBLE_POINTS = 30;
    private static final int MAX_POINTS = VISIBLE_POINTS + 1;
    private static final int GRID_LINES = 5;
    private static final float WALK_STEP = 20f;
    private static final float WALK_MIN = 0f;
    private static final float WALK_MAX = 100f;
    private static final float Y_PADDING_RATIO = 0.1f;
    private static final int LINE_COLOR = 0xFF26A69A;
    private static final int FILL_TOP_COLOR = 0x8026A69A;
    private static final int FILL_BOTTOM_COLOR = 0x0026A69A;
    private static final float LINE_DP = 2.5f;
    private static final float MARKER_DP = 5f;
    private static final float HEAD_DP = 4f;
    private static final float PULSE_DP = 14f;
    private static final long PULSE_MS = 1200;
    private static final float LABEL_GAP_DP = 6f;

    private final List<Float> points = new ArrayList<>();
    private final float[] xs = new float[MAX_POINTS];
    private final float[] ys = new float[MAX_POINTS];
    private final float[] controls = new float[4 * MAX_POINTS];
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint;
    private final Paint axisLabelPaint;
    private final Paint valuePaint;
    private final Path linePath = new Path();
    private final Path fillPath = new Path();
    private final RectF plot = new RectF();
    private final ValueAnimator scrollAnimator = ValueAnimator.ofFloat(1f, 0f);
    private final ValueAnimator pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
    private final Random random = new Random();
    private final float density;
    private Supplier<Float> dataSource = this::randomWalk;
    private float lastValue = (WALK_MIN + WALK_MAX) / 2f;
    /** 1 → 0 trong mỗi chu kỳ lấy mẫu: phần bước mà điểm mới nhất còn phải trượt vào. */
    private float scrollOffset;
    private float pulse;
    private boolean smooth;
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
        markerPaint.setColor(LINE_COLOR);
        pulsePaint.setColor(LINE_COLOR);
        pulsePaint.setStyle(Paint.Style.STROKE);
        pulsePaint.setStrokeWidth(2f * density);
        gridPaint = ChartAxes.gridPaint(density);
        axisLabelPaint = ChartAxes.labelPaint(context, Paint.Align.RIGHT);
        valuePaint = ChartAxes.labelPaint(context, Paint.Align.CENTER);
        scrollAnimator.setDuration(SAMPLE_INTERVAL_MS);
        scrollAnimator.setInterpolator(new LinearInterpolator());
        scrollAnimator.addUpdateListener(a -> {
            scrollOffset = (float) a.getAnimatedValue();
            invalidate();
        });
        pulseAnimator.setDuration(PULSE_MS);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new DecelerateInterpolator());
        pulseAnimator.addUpdateListener(a -> {
            pulse = (float) a.getAnimatedValue();
            invalidate();
        });
    }

    /** Thay nguồn dữ liệu (mặc định: random walk 0..100). Gọi trên main thread. */
    public void setDataSource(Supplier<Float> source) {
        dataSource = source;
    }

    /** true → đường cong Catmull-Rom qua đúng mọi điểm; false → đường gấp khúc. */
    public void setSmooth(boolean smooth) {
        this.smooth = smooth;
        invalidate();
    }

    private float randomWalk() {
        float next = lastValue + (random.nextFloat() - 0.5f) * WALK_STEP;
        lastValue = Math.max(WALK_MIN, Math.min(WALK_MAX, next));
        return lastValue;
    }

    private void addPoint(float value) {
        points.add(value);
        if (points.size() > MAX_POINTS) {
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
        pulseAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(sampler);
        scrollAnimator.cancel();
        pulseAnimator.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        ChartAxes.plotArea(plot, w, h, density);
        // Gradient theo trục dọc của vùng vẽ: đậm ở trên, trong suốt ở đáy
        fillPaint.setShader(new LinearGradient(0f, plot.top, 0f, plot.bottom,
                FILL_TOP_COLOR, FILL_BOTTOM_COLOR, Shader.TileMode.CLAMP));
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
        int n = points.size();
        if (n == 0 || plot.isEmpty()) return;
        ChartScale scale = yScale();
        ChartAxes.drawHorizontalGrid(canvas, plot, scale, GRID_LINES, LABEL_GAP_DP * density,
                gridPaint, axisLabelPaint);

        float step = stepWidth();
        for (int i = 0; i < n; i++) {
            xs[i] = xAt(i, step);
            ys[i] = scale.toPixel(points.get(i));
        }
        buildLinePath(n);
        fillPath.set(linePath);
        fillPath.lineTo(xs[n - 1], plot.bottom);
        fillPath.lineTo(xs[0], plot.bottom);
        fillPath.close();

        canvas.save();
        canvas.clipRect(plot);
        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);
        drawPulse(canvas, xs[n - 1], ys[n - 1]);
        canvas.restore();

        if (selectedIndex >= 0 && selectedIndex < n) {
            float px = xs[selectedIndex];
            float py = ys[selectedIndex];
            canvas.drawCircle(px, py, MARKER_DP * density, markerPaint);
            canvas.drawText(String.format(Locale.US, "%.1f", points.get(selectedIndex)), px,
                    py - 2 * MARKER_DP * density, valuePaint);
        }
    }

    private void buildLinePath(int n) {
        linePath.reset();
        linePath.moveTo(xs[0], ys[0]);
        if (smooth && n >= 2) {
            ChartMath.catmullRomControls(xs, ys, n, controls);
            for (int i = 0; i < n - 1; i++) {
                linePath.cubicTo(controls[4 * i], controls[4 * i + 1],
                        controls[4 * i + 2], controls[4 * i + 3], xs[i + 1], ys[i + 1]);
            }
        } else {
            for (int i = 1; i < n; i++) linePath.lineTo(xs[i], ys[i]);
        }
    }

    /** Chấm đặc ở điểm mới nhất + vòng tròn lan rộng và mờ dần (kiểu "đang trực tiếp"). */
    private void drawPulse(Canvas canvas, float x, float y) {
        canvas.drawCircle(x, y, HEAD_DP * density, markerPaint);
        pulsePaint.setAlpha((int) (255 * (1f - pulse)));
        canvas.drawCircle(x, y, (HEAD_DP + pulse * PULSE_DP) * density, pulsePaint);
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
