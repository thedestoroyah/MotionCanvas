package vn.edu.tdtu.motioncanvas.drawing;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Các mục đã xong được vẽ sẵn lên Bitmap offscreen (`committed`); nét đang vẽ được vẽ đè mỗi frame.
 * Undo/Redo = dựng lại Bitmap từ danh sách mục trong UndoStack.
 * Replay = dựng lại từ đầu trên một Bitmap riêng, mỗi frame thêm vài điểm theo ReplayTimeline.
 */
public class DrawingView extends View {
    private static final float DEFAULT_WIDTH_DP = 8f;

    public interface ReplayListener {
        void onReplayChanged(boolean replaying);
    }

    private final UndoStack<Stroke> history = new UndoStack<>();
    private final StrokeRenderer renderer = new StrokeRenderer();
    private final Random random = new Random();
    private final ValueAnimator replayAnimator = ValueAnimator.ofFloat(0f, 1f);
    private final int[] replayCursor = new int[2];
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

    private List<Stroke> replayStrokes = Collections.emptyList();
    private ReplayTimeline timeline;
    private Bitmap replayBitmap;
    private Canvas replayCanvas;
    /** Số nét đã ghi hẳn vào replayBitmap. */
    private int replayDone;
    private boolean replaying;
    /** Cử chỉ bắt đầu bằng cú chạm dừng replay: bỏ qua cho tới khi nhấc tay. */
    private boolean ignoreGesture;
    private ReplayListener replayListener;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        strokeWidth = DEFAULT_WIDTH_DP * density;
        // BlurMaskFilter (brush Neon) chỉ được tăng tốc phần cứng từ API 28
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) setLayerType(LAYER_TYPE_SOFTWARE, null);
        replayAnimator.setInterpolator(new LinearInterpolator());
        replayAnimator.addUpdateListener(a -> onReplayFrame(a.getAnimatedFraction()));
        replayAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                finishReplay();
            }
        });
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

    public void setReplayListener(ReplayListener listener) {
        replayListener = listener;
    }

    public boolean isReplaying() {
        return replaying;
    }

    public void undo() {
        if (!replaying && history.undo()) rebuild();
    }

    public void redo() {
        if (!replaying && history.redo()) rebuild();
    }

    public void clear() {
        if (replaying || history.items().isEmpty()) return;
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

    /** @return false nếu không có gì để phát (tranh trống hoặc vừa "Xóa hết"). */
    public boolean startReplay() {
        if (replaying || committed == null) return false;
        List<Stroke> items = history.items();
        int from = ReplayTimeline.firstAfterLastClear(items);
        if (from >= items.size()) return false;
        replayStrokes = new ArrayList<>(items.subList(from, items.size()));
        int[] counts = new int[replayStrokes.size()];
        for (int i = 0; i < counts.length; i++) counts[i] = Math.max(1, replayStrokes.get(i).pointCount());
        timeline = new ReplayTimeline(counts);

        if (replayBitmap == null || replayBitmap.getWidth() != committed.getWidth()
                || replayBitmap.getHeight() != committed.getHeight()) {
            replayBitmap = Bitmap.createBitmap(committed.getWidth(), committed.getHeight(), Bitmap.Config.ARGB_8888);
            replayCanvas = new Canvas(replayBitmap);
        }
        replayBitmap.eraseColor(Color.TRANSPARENT);
        replayDone = 0;
        replayCursor[0] = 0;
        replayCursor[1] = 0;
        current = null;
        replaying = true;
        if (replayListener != null) replayListener.onReplayChanged(true);
        replayAnimator.setDuration(timeline.durationMs());
        replayAnimator.start();
        return true;
    }

    /** Dừng giữa chừng: hiện ngay toàn bộ tranh. */
    public void stopReplay() {
        if (replaying) replayAnimator.cancel(); // cancel() cũng gọi onAnimationEnd → finishReplay
    }

    private void onReplayFrame(float progress) {
        if (!replaying) return;
        timeline.locate(progress, replayCursor);
        int index = replayCursor[0];
        // Các nét trước nét đang vẽ dở đã xong; đổ màu thì hiện ngay khi tới lượt
        while (replayDone < index || (replayDone == index && replayCursor[1] > 0
                && replayStrokes.get(index).type == BrushType.FILL)) {
            renderer.commit(replayBitmap, replayCanvas, replayStrokes.get(replayDone));
            replayDone++;
        }
        invalidate();
    }

    private void finishReplay() {
        if (!replaying) return;
        replaying = false;
        replayStrokes = Collections.emptyList();
        timeline = null;
        if (replayListener != null) replayListener.onReplayChanged(false);
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopReplay();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (w == 0 || h == 0) return;
        stopReplay();
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
        if (replaying) {
            canvas.drawBitmap(replayBitmap, 0f, 0f, null);
            int index = replayCursor[0];
            if (replayDone == index) renderer.draw(canvas, replayStrokes.get(index), replayCursor[1]);
        } else {
            canvas.drawBitmap(committed, 0f, 0f, null);
            if (current != null) renderer.draw(canvas, current, current.pointCount());
        }
        canvas.restoreToCount(layer);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) ignoreGesture = replaying;
        if (ignoreGesture) {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) stopReplay();
            return true;
        }
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
