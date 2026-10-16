package vn.edu.tdtu.motioncanvas.drawing;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Bảng chọn màu HSV tự vẽ: vòng ngoài chọn Hue (SweepGradient),
 * ô vuông bên trong chọn Saturation (ngang) và Value (dọc).
 */
public class ColorPickerView extends View {
    private static final int[] HUE_COLORS = {
            Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED};
    private static final float RING_RATIO = 0.1f;      // độ dày vòng / cạnh view
    private static final float SQUARE_RATIO = 0.65f;   // nửa cạnh ô vuông / bán kính trong của vòng (< 1/√2)
    private static final float MARKER_DP = 3f;
    private static final int TARGET_NONE = 0;
    private static final int TARGET_RING = 1;
    private static final int TARGET_SQUARE = 2;

    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint saturationPaint = new Paint();
    private final Paint valuePaint = new Paint();
    private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint swatchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF square = new RectF();
    private final float[] hsv = {0f, 1f, 1f};
    private final float[] pureHue = {0f, 1f, 1f};
    private final float density;
    private float centerX;
    private float centerY;
    private float ringRadius;
    private float ringWidth;
    private int dragTarget = TARGET_NONE;

    public ColorPickerView(Context context) {
        this(context, null);
    }

    public ColorPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        ringPaint.setStyle(Paint.Style.STROKE);
        markerPaint.setStyle(Paint.Style.STROKE);
        markerPaint.setColor(Color.WHITE);
        markerPaint.setStrokeWidth(MARKER_DP * density);
    }

    /** Đặt màu đang chọn (bỏ qua alpha); vòng hue và ô vuông S/V cập nhật theo. */
    public void setColor(int color) {
        Color.colorToHSV(color, hsv);
        updateSaturationShader();
        invalidate();
    }

    public int getColor() {
        return Color.HSVToColor(hsv);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float size = Math.min(w, h);
        centerX = w / 2f;
        centerY = h / 2f;
        ringWidth = size * RING_RATIO;
        ringRadius = size / 2f - ringWidth;
        ringPaint.setStrokeWidth(ringWidth);
        ringPaint.setShader(new SweepGradient(centerX, centerY, HUE_COLORS, null));
        float half = (ringRadius - ringWidth / 2f) * SQUARE_RATIO;
        square.set(centerX - half, centerY - half, centerX + half, centerY + half);
        valuePaint.setShader(new LinearGradient(0f, square.top, 0f, square.bottom,
                Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP));
        updateSaturationShader();
    }

    /** Lớp ngang: trắng → màu hue thuần. Chỉ tạo lại khi hue hoặc kích thước đổi. */
    private void updateSaturationShader() {
        if (square.isEmpty()) return;
        pureHue[0] = hsv[0];
        saturationPaint.setShader(new LinearGradient(square.left, 0f, square.right, 0f,
                Color.WHITE, Color.HSVToColor(pureHue), Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawCircle(centerX, centerY, ringRadius, ringPaint);
        canvas.drawRect(square, saturationPaint);
        canvas.drawRect(square, valuePaint); // lớp dọc: trong suốt → đen, phủ lên lớp ngang

        double hueRad = Math.toRadians(hsv[0]);
        float hueX = centerX + (float) Math.cos(hueRad) * ringRadius;
        float hueY = centerY + (float) Math.sin(hueRad) * ringRadius;
        canvas.drawCircle(hueX, hueY, ringWidth / 2f, markerPaint);

        float svX = square.left + hsv[1] * square.width();
        float svY = square.top + (1f - hsv[2]) * square.height();
        float markerRadius = ringWidth / 3f;
        swatchPaint.setColor(getColor());
        canvas.drawCircle(svX, svY, markerRadius, swatchPaint);
        canvas.drawCircle(svX, svY, markerRadius, markerPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                float dist = (float) Math.hypot(x - centerX, y - centerY);
                if (Math.abs(dist - ringRadius) <= ringWidth) {
                    dragTarget = TARGET_RING;
                } else if (square.contains(x, y)) {
                    dragTarget = TARGET_SQUARE;
                } else {
                    dragTarget = TARGET_NONE;
                }
                updateFromTouch(x, y);
                return true;
            case MotionEvent.ACTION_MOVE:
                updateFromTouch(x, y);
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

    /** Đang kéo vòng thì chỉ đổi hue, đang kéo ô vuông thì chỉ đổi S/V — kể cả khi ngón tay trượt ra ngoài. */
    private void updateFromTouch(float x, float y) {
        if (dragTarget == TARGET_RING) {
            hsv[0] = PickerMath.hueAt(x - centerX, y - centerY);
            updateSaturationShader();
        } else if (dragTarget == TARGET_SQUARE) {
            hsv[1] = PickerMath.fraction(x, square.left, square.width());
            hsv[2] = 1f - PickerMath.fraction(y, square.top, square.height());
        } else {
            return;
        }
        invalidate();
    }
}
