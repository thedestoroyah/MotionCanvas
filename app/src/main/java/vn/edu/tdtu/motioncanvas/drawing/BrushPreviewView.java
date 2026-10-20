package vn.edu.tdtu.motioncanvas.drawing;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/** Ô xem trước: một nét lượn sóng vẽ bằng đúng brush, màu và độ dày đang chọn (dùng chung StrokeRenderer); chạm để chọn độ dày. */
public class BrushPreviewView extends View {
    /** Độ dày lớn nhất (dp) mà thanh kéo cho chọn. */
    public static final float MAX_STROKE_DP = 60f;
    /** Dưới mức này nét được vẽ đúng độ dày thật; trên mức này bị nén để vẫn vừa ô. */
    private static final float TRUE_SIZE_DP = 12f;
    private static final int POINTS = 48;
    private static final float CORNER_DP = 8f;
    private static final float PADDING_DP = 12f;
    /** Nét không dày quá 60% chiều cao ô, nếu không sẽ tràn khỏi khung. */
    private static final float MAX_WIDTH_RATIO = 0.6f;
    /** Mũi tên ▾ ở mép phải báo hiệu ô bấm được, giống Spinner. */
    private static final float ARROW_DP = 5f;
    private static final float ARROW_AREA_DP = 20f;
    private static final int ARROW_COLOR = 0xFF616161;
    private static final int ERASER_BACKDROP = 0xFFBDBDBD;
    private static final long SPRAY_SEED = 42L;

    private final StrokeRenderer renderer = new StrokeRenderer();
    private final Paint framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF frame = new RectF();
    private final Path arrow = new Path();
    private final float density;
    private BrushType brush = BrushType.PEN;
    private int color = Color.BLACK;
    private float strokeWidth;
    private Stroke sample;

    public BrushPreviewView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        framePaint.setStyle(Paint.Style.STROKE);
        framePaint.setStrokeWidth(density);
        framePaint.setColor(Color.LTGRAY);
        arrowPaint.setColor(ARROW_COLOR);
        // Giống DrawingView: BlurMaskFilter (Neon) chỉ chạy trên GPU từ Android 9
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void setBrush(BrushType brush) {
        this.brush = brush;
        rebuild();
    }

    public void setColor(int color) {
        this.color = color;
        rebuild();
    }

    /** Độ dày nét theo pixel, giống DrawingView.setStrokeWidth. */
    public void setStrokeWidth(float px) {
        strokeWidth = px;
        rebuild();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float inset = framePaint.getStrokeWidth() / 2f;
        frame.set(inset, inset, w - inset, h - inset);
        float a = ARROW_DP * density;
        float cx = w - ARROW_AREA_DP * density / 2f;
        arrow.rewind();
        arrow.moveTo(cx - a, h / 2f - a / 2f);
        arrow.lineTo(cx + a, h / 2f - a / 2f);
        arrow.lineTo(cx, h / 2f + a / 2f);
        arrow.close();
        rebuild();
    }

    /**
     * Độ dày hiển thị trong ô (dp): đúng kích thước thật tới TRUE_SIZE_DP, phần trên đó nén tuyến tính
     * sao cho MAX_STROKE_DP vừa khít maxVisibleDp → mọi mức độ dày đều nhìn khác nhau, không bão hòa.
     */
    static float displayWidthDp(float widthDp, float maxVisibleDp) {
        float knee = Math.min(TRUE_SIZE_DP, maxVisibleDp);
        if (widthDp <= knee) return widthDp;
        float t = Math.min(1f, (widthDp - knee) / (MAX_STROKE_DP - knee));
        return knee + t * (maxVisibleDp - knee);
    }

    /** Dựng lại nét mẫu: một chu kỳ sin ngang ô; thư pháp đổi độ dày theo độ dốc để thấy rõ nét đậm/mảnh. */
    private void rebuild() {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0f || h <= 0f) return;
        float width = displayWidthDp(strokeWidth / density, h * MAX_WIDTH_RATIO / density) * density;
        float pad = PADDING_DP * density + width / 2f;
        float amplitude = Math.max(0f, h / 2f - pad);
        float right = w - ARROW_AREA_DP * density;
        sample = new Stroke(brush, color, width, Symmetry.OFF, w / 2f, h / 2f, SPRAY_SEED);
        for (int i = 0; i < POINTS; i++) {
            double t = 2 * Math.PI * i / (POINTS - 1);
            float factor = brush == BrushType.CALLIGRAPHY
                    ? CalligraphyWidth.MIN + (CalligraphyWidth.MAX - CalligraphyWidth.MIN) * (float) Math.abs(Math.cos(t))
                    : 1f;
            sample.addPoint(pad + (right - 2 * pad) * i / (POINTS - 1), h / 2f - amplitude * (float) Math.sin(t), factor);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float corner = CORNER_DP * density;
        fillPaint.setColor(Color.WHITE);
        canvas.drawRoundRect(frame, corner, corner, fillPaint);
        if (sample != null) {
            if (brush == BrushType.FILL) {
                // Đổ màu không có nét: xem trước bằng cả ô tô màu
                fillPaint.setColor(color);
                canvas.drawRoundRect(frame, corner, corner, fillPaint);
            } else {
                // Tẩy (CLEAR) chỉ thấy được khi đục qua một lớp có màu, nên vẽ lớp xám rồi tẩy trên đó
                int layer = canvas.saveLayer(frame, null);
                if (brush == BrushType.ERASER) {
                    fillPaint.setColor(ERASER_BACKDROP);
                    canvas.drawRoundRect(frame, corner, corner, fillPaint);
                }
                renderer.draw(canvas, sample, sample.pointCount());
                canvas.restoreToCount(layer);
            }
        }
        canvas.drawRoundRect(frame, corner, corner, framePaint);
        canvas.drawPath(arrow, arrowPaint);
    }
}
