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

import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import java.util.Locale;

import vn.edu.tdtu.motioncanvas.R;

/**
 * Donut chart: các phần quét từ 0° đến 360°; chạm vào một phần → phần đó trượt ra ngoài bằng lò xo
 * và giữa vòng hiện phần trăm. Chú thích bên phải.
 */
public class DonutChartView extends View {
    private static final String[] LABELS = {"Android", "iOS", "Web", "Desktop", "Khác"};
    private static final int[] COLORS = {0xFF66BB6A, 0xFF42A5F5, 0xFFFFA726, 0xFFAB47BC, 0xFF78909C};
    private static final long SWEEP_MS = 900;
    private static final float START_ANGLE = -90f; // 12 giờ, theo hệ góc của Canvas
    private static final float RING_DP = 28f;
    private static final float EXPLODE_DP = 12f;
    private static final float MARGIN_DP = 8f;
    private static final float LEGEND_GAP_DP = 24f;
    private static final float LEGEND_DOT_DP = 6f;
    private static final float LEGEND_LINE_DP = 22f;
    private static final float CENTER_TEXT_SP = 16f;
    private static final float MIN_VISIBLE_OFFSET = 0.002f; // đơn vị: phần của EXPLODE_DP
    private static final int TEXT_COLOR = 0xFF424242;

    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint legendPaint;
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();
    private final ValueAnimator sweepAnimator = ValueAnimator.ofFloat(0f, 1f);
    /** Mức trượt ra của từng phần: 0 = đúng chỗ, 1 = ra hết EXPLODE_DP; mỗi phần một lò xo. */
    private final float[] offsets = new float[LABELS.length];
    private final SpringAnimation[] springs = new SpringAnimation[LABELS.length];
    private final float density;
    private final float ringWidth;
    private final float explodeDistance;
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
        explodeDistance = EXPLODE_DP * density;
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(ringWidth);
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
        for (int i = 0; i < LABELS.length; i++) springs[i] = createSpring(i);
    }

    private SpringAnimation createSpring(int index) {
        FloatPropertyCompat<DonutChartView> property = new FloatPropertyCompat<DonutChartView>("slice" + index) {
            @Override
            public float getValue(DonutChartView view) {
                return view.offsets[index];
            }

            @Override
            public void setValue(DonutChartView view, float value) {
                view.offsets[index] = value;
                view.invalidate();
            }
        };
        SpringAnimation spring = new SpringAnimation(this, property, 0f);
        spring.getSpring()
                .setStiffness(SpringForce.STIFFNESS_MEDIUM)
                .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
        spring.setMinimumVisibleChange(MIN_VISIBLE_OFFSET);
        return spring;
    }

    public static int sliceCount() {
        return LABELS.length;
    }

    /**
     * Đặt dữ liệu mới và chạy lại hiệu ứng quét vòng; bỏ chọn phần đang trượt ra.
     *
     * @param values đúng sliceCount() giá trị thô (không cần tổng = 100; giá trị âm coi như 0)
     * @throws IllegalArgumentException nếu số giá trị khác sliceCount()
     */
    public void setData(float[] values) {
        if (values.length != LABELS.length) {
            throw new IllegalArgumentException("Cần đúng " + LABELS.length + " giá trị");
        }
        sweeps = ChartMath.sweepAngles(values);
        select(-1); // mọi phần trượt về chỗ cũ trong lúc quét lại
        sweepAnimator.start();
    }

    /** Phần `index` trượt ra (lò xo), mọi phần khác trượt về; -1 = bỏ chọn. */
    private void select(int index) {
        selectedIndex = index;
        for (int i = 0; i < springs.length; i++) springs[i].animateToFinalPosition(i == index ? 1f : 0f);
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        sweepAnimator.cancel();
        for (SpringAnimation spring : springs) spring.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // Donut chiếm hình vuông bên trái (chừa chỗ cho phần trượt ra), chú thích ở phần còn lại
        float size = Math.min(w / 2f, h);
        radius = size / 2f - ringWidth / 2f - explodeDistance - MARGIN_DP * density;
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
        float start = 0f; // độ, tính từ 12 giờ theo chiều kim đồng hồ
        for (int i = 0; i < sweeps.length; i++) {
            float sweep = sweeps[i] * progress;
            // Trượt theo hướng giữa của phần: dịch cả cung đi một đoạn dọc theo bán kính
            double mid = Math.toRadians(START_ANGLE + start + sweep / 2f);
            float shift = offsets[i] * explodeDistance;
            canvas.save();
            canvas.translate((float) Math.cos(mid) * shift, (float) Math.sin(mid) * shift);
            arcPaint.setColor(COLORS[i]);
            canvas.drawArc(arcBounds, START_ANGLE + start, sweep, false, arcPaint);
            canvas.restore();
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
        fitCenterText(center);
        canvas.drawText(center, centerX, centerY + centerPaint.getTextSize() / 3f, centerPaint);
        drawLegend(canvas);
    }

    /** Thu nhỏ chữ giữa vòng cho vừa lòng vòng, không lấn sang cung màu. */
    private void fitCenterText(String text) {
        float defaultSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, CENTER_TEXT_SP,
                getResources().getDisplayMetrics());
        centerPaint.setTextSize(defaultSize);
        float available = 2f * (radius - ringWidth / 2f - MARGIN_DP * density);
        float measured = centerPaint.measureText(text);
        if (available > 0f && measured > available) centerPaint.setTextSize(defaultSize * available / measured);
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
            select(sliceAtTouch(e.getX(), e.getY()));
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

    /** Phần chứa điểm chạm, tính theo vị trí ĐÃ trượt của từng phần; -1 nếu chạm ngoài vòng. */
    private int sliceAtTouch(float x, float y) {
        float start = 0f;
        for (int i = 0; i < sweeps.length; i++) {
            float sweep = sweeps[i];
            if (sweep > 0f) {
                double mid = Math.toRadians(START_ANGLE + start + sweep / 2f);
                float shift = offsets[i] * explodeDistance;
                float dx = x - centerX - (float) Math.cos(mid) * shift;
                float dy = y - centerY - (float) Math.sin(mid) * shift;
                boolean onRing = Math.abs((float) Math.hypot(dx, dy) - radius) <= ringWidth;
                float angle = ChartMath.angleFromTop(dx, dy);
                if (onRing && angle >= start && angle < start + sweep) return i;
            }
            start += sweep;
        }
        return -1;
    }
}
