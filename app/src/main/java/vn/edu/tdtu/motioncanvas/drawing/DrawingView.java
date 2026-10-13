package vn.edu.tdtu.motioncanvas.drawing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;

/**
 * Các mục đã xong được vẽ sẵn lên Bitmap offscreen (`committed`); nét đang vẽ được vẽ đè mỗi frame.
 * Undo/Redo = dựng lại Bitmap từ danh sách mục trong UndoStack.
 */
public class DrawingView extends View {
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
    private long lastTime;
    private float lastWidthFactor;
    private BrushType brush = BrushType.PEN;
    private int color = Color.BLACK;
    private float strokeWidth;
    private Symmetry symmetry = Symmetry.OFF;

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

    public void setSymmetry(Symmetry symmetry) {
        this.symmetry = symmetry;
    }

    public Symmetry getSymmetry() {
        return symmetry;
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

    /** Ảnh để lưu: nền trắng + các mục đã xong. */
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
        for (Stroke s : history.items()) renderer.commit(committed, committedCanvas, s);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.WHITE);
        if (committed == null) return;
        // Lớp riêng: tẩy (CLEAR) chỉ đục lớp vẽ, không đục nền trắng
        int layer = canvas.saveLayer(0f, 0f, getWidth(), getHeight(), null);
        canvas.drawBitmap(committed, 0f, 0f, null);
        if (current != null) renderer.draw(canvas, current, current.pointCount());
        canvas.restoreToCount(layer);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = x;
                lastY = y;
                lastTime = e.getEventTime();
                if (brush == BrushType.FILL) return true; // đổ màu khi nhấc tay
                current = new Stroke(brush, color, strokeWidth, symmetry,
                        getWidth() / 2f, getHeight() / 2f, random.nextLong());
                lastWidthFactor = 1f;
                current.addPoint(x, y, lastWidthFactor);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (current == null) return true;
                current.addPoint(x, y, widthFactorAt(x, y, e.getEventTime()));
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                if (brush == BrushType.FILL) {
                    commitFill(x, y);
                } else if (current != null) {
                    current.addPoint(x, y, widthFactorAt(x, y, e.getEventTime()));
                    commitCurrent();
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

    /** Thư pháp: hệ số độ dày theo tốc độ tay (dp/ms), làm mượt; brush khác luôn là 1. */
    private float widthFactorAt(float x, float y, long time) {
        if (current.type != BrushType.CALLIGRAPHY) return 1f;
        long dt = time - lastTime;
        if (dt > 0) {
            float speed = (float) Math.hypot(x - lastX, y - lastY) / density / dt;
            lastWidthFactor = CalligraphyWidth.smooth(lastWidthFactor, CalligraphyWidth.factor(speed));
            lastX = x;
            lastY = y;
            lastTime = time;
        }
        return lastWidthFactor;
    }

    private void commitCurrent() {
        history.push(current);
        renderer.commit(committed, committedCanvas, current);
        current = null;
        invalidate();
    }

    /** Đổ màu không theo đối xứng; vùng đã đúng màu thì không thêm gì vào lịch sử. */
    private void commitFill(float x, float y) {
        Stroke fill = new Stroke(BrushType.FILL, color, 0f, Symmetry.OFF, 0f, 0f, 0L);
        fill.addPoint(x, y, 1f);
        if (renderer.applyFill(committed, fill)) {
            history.push(fill);
            invalidate();
        }
    }
}
