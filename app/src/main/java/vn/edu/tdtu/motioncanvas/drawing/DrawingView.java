package vn.edu.tdtu.motioncanvas.drawing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;

/**
 * Các nét đã xong được vẽ sẵn lên Bitmap offscreen (`committed`); nét đang vẽ được vẽ đè mỗi frame.
 * Undo/Redo = vẽ lại Bitmap từ danh sách nét trong UndoStack.
 */
public class DrawingView extends View {
    private static final int SPRAY_DOTS = 12;
    private static final float SPRAY_RADIUS_FACTOR = 1.5f;
    private static final float SPRAY_DOT_FACTOR = 0.1f;
    private static final float DEFAULT_WIDTH_DP = 8f;

    private final UndoStack<Stroke> history = new UndoStack<>();
    private final StrokeRenderer renderer = new StrokeRenderer();
    private final Random random = new Random();
    private final float density;
    private Bitmap committed;
    private Canvas committedCanvas;
    private Stroke current;
    private float lastX;
    private float lastY;
    private BrushType brush = BrushType.PEN;
    private int color = Color.BLACK;
    private float strokeWidth;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        strokeWidth = DEFAULT_WIDTH_DP * density;
        // BlurMaskFilter (brush Neon) chỉ được tăng tốc phần cứng từ API 28
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void setBrush(BrushType brush) {
        this.brush = brush;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    public void setStrokeWidth(float px) {
        strokeWidth = px;
    }

    public void undo() {
        if (history.undo()) rebuild();
    }

    public void redo() {
        if (history.redo()) rebuild();
    }

    public void clear() {
        if (history.items().isEmpty()) return;
        history.push(Stroke.CLEAR);
        rebuild();
    }

    /** Ảnh để lưu: nền trắng + các nét đã xong. */
    public Bitmap exportBitmap() {
        Bitmap out = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(committed, 0f, 0f, null);
        return out;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (w == 0 || h == 0) return;
        committed = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        committedCanvas = new Canvas(committed);
        rebuild();
    }

    private void rebuild() {
        if (committedCanvas == null) return;
        committed.eraseColor(Color.TRANSPARENT);
        for (Stroke s : history.items()) renderer.draw(committedCanvas, s);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.WHITE);
        if (committed == null) return;
        // Lớp riêng: tẩy (CLEAR) chỉ đục lớp vẽ, không đục nền trắng
        int layer = canvas.saveLayer(0f, 0f, getWidth(), getHeight(), null);
        canvas.drawBitmap(committed, 0f, 0f, null);
        if (current != null) renderer.draw(canvas, current);
        canvas.restoreToCount(layer);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                current = new Stroke(brush, color, strokeWidth, new Path());
                if (brush == BrushType.SPRAY) {
                    sprayAt(x, y);
                } else {
                    current.path.moveTo(x, y);
                }
                lastX = x;
                lastY = y;
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (current == null) return true;
                if (brush == BrushType.SPRAY) {
                    sprayAt(x, y);
                } else {
                    // Đường cong qua trung điểm → nét mượt, không gãy khúc
                    current.path.quadTo(lastX, lastY, (x + lastX) / 2f, (y + lastY) / 2f);
                }
                lastX = x;
                lastY = y;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                if (current != null) {
                    if (brush != BrushType.SPRAY) current.path.lineTo(x, y);
                    history.push(current);
                    renderer.draw(committedCanvas, current);
                    current = null;
                    invalidate();
                }
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                current = null;
                invalidate();
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    /** Rải chấm ngẫu nhiên trong hình tròn quanh ngón tay; sqrt để chấm phân bố đều theo diện tích. */
    private void sprayAt(float x, float y) {
        float radius = strokeWidth * SPRAY_RADIUS_FACTOR;
        float dot = Math.max(density, strokeWidth * SPRAY_DOT_FACTOR);
        for (int i = 0; i < SPRAY_DOTS; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            float r = (float) Math.sqrt(random.nextDouble()) * radius;
            current.path.addCircle(x + (float) Math.cos(angle) * r, y + (float) Math.sin(angle) * r,
                    dot, Path.Direction.CW);
        }
    }
}
