package vn.edu.tdtu.motioncanvas.drawing;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

import java.util.Random;

/** Vẽ một Stroke lên Canvas theo loại brush, lặp lại theo chế độ đối xứng. */
final class StrokeRenderer {
    private static final int MARKER_ALPHA = 90;
    private static final float MARKER_WIDTH_FACTOR = 2f;
    private static final float NEON_CORE_FACTOR = 0.35f;
    private static final int SPRAY_DOTS = 12;
    private static final float SPRAY_RADIUS_FACTOR = 1.5f;
    private static final float SPRAY_DOT_FACTOR = 0.1f;
    private static final float RAINBOW_DEGREES_PER_PX = 0.6f;
    /** Sai số màu khi đổ màu: tô kín cả viền khử răng cưa nhạt quanh nét. */
    private static final int FILL_TOLERANCE = 64;

    private final Paint paint = new Paint();
    private final Paint neonCore = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Random sprayRandom = new Random();
    private final float[] hsv = new float[3];
    private final PorterDuffXfermode clearMode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
    private BlurMaskFilter neonBlur;
    private float neonBlurRadius = -1f;
    private int[] pixels = new int[0];

    StrokeRenderer() {
        neonCore.setColor(Color.WHITE);
        neonCore.setStyle(Paint.Style.STROKE);
        neonCore.setStrokeCap(Paint.Cap.ROUND);
        neonCore.setStrokeJoin(Paint.Join.ROUND);
    }

    /** Ghi hẳn một mục lịch sử lên Bitmap lớp vẽ (khi nhấc tay, dựng lại sau undo, replay). */
    void commit(Bitmap bitmap, Canvas canvas, Stroke s) {
        if (s == Stroke.CLEAR) {
            bitmap.eraseColor(Color.TRANSPARENT);
        } else if (s.type == BrushType.FILL) {
            applyFill(bitmap, s);
        } else {
            draw(canvas, s, s.pointCount());
        }
    }

    /** Tô loang tại điểm của nét FILL; @return true nếu có pixel đổi màu. */
    boolean applyFill(Bitmap bitmap, Stroke s) {
        if (s.pointCount() == 0) return false;
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (pixels.length != w * h) pixels = new int[w * h];
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
        boolean changed = FloodFill.fill(pixels, w, h, (int) s.x(0), (int) s.y(0), s.color, FILL_TOLERANCE);
        if (changed) bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        return changed;
    }

    /** Vẽ `points` điểm đầu của nét; mỗi bản sao đối xứng là một lần xoay/lật Canvas quanh tâm. */
    void draw(Canvas canvas, Stroke s, int points) {
        int n = s.visibleCount(points);
        if (n == 0 || s.type == BrushType.FILL) return;
        Symmetry symmetry = s.symmetry;
        int mirrors = symmetry.mirror ? 2 : 1;
        for (int r = 0; r < symmetry.rotations; r++) {
            for (int m = 0; m < mirrors; m++) {
                canvas.save();
                canvas.rotate(360f * r / symmetry.rotations, s.centerX, s.centerY);
                if (m == 1) canvas.scale(-1f, 1f, s.centerX, s.centerY);
                drawOnce(canvas, s, n);
                canvas.restore();
            }
        }
    }

    private void drawOnce(Canvas canvas, Stroke s, int n) {
        switch (s.type) {
            case SPRAY:
                drawSpray(canvas, s, n);
                break;
            case CALLIGRAPHY:
            case RAINBOW:
                drawSegments(canvas, s, n);
                break;
            default:
                drawSmoothPath(canvas, s, n);
                break;
        }
    }

    private void drawSmoothPath(Canvas canvas, Stroke s, int n) {
        configure(s);
        path.reset();
        path.moveTo(s.x(0), s.y(0));
        for (int i = 1; i < n; i++) {
            float px = s.x(i - 1);
            float py = s.y(i - 1);
            // Đường cong qua trung điểm → nét mượt, không gãy khúc
            path.quadTo(px, py, (px + s.x(i)) / 2f, (py + s.y(i)) / 2f);
        }
        path.lineTo(s.x(n - 1), s.y(n - 1)); // 1 điểm → đoạn dài 0, đầu tròn → một chấm
        canvas.drawPath(path, paint);
        if (s.type == BrushType.NEON) {
            // Neon = quầng sáng mờ màu + lõi trắng mảnh ở giữa
            neonCore.setStrokeWidth(s.width * NEON_CORE_FACTOR);
            canvas.drawPath(path, neonCore);
        }
    }

    /** Rải chấm quanh từng điểm; Random đặt lại theo seed nên lần vẽ nào cũng ra đúng các chấm đó. */
    private void drawSpray(Canvas canvas, Stroke s, int n) {
        configure(s);
        sprayRandom.setSeed(s.seed);
        float radius = s.width * SPRAY_RADIUS_FACTOR;
        float dot = Math.max(1f, s.width * SPRAY_DOT_FACTOR);
        path.reset();
        for (int i = 0; i < n; i++) {
            for (int d = 0; d < SPRAY_DOTS; d++) {
                double angle = sprayRandom.nextDouble() * 2 * Math.PI;
                // sqrt để chấm phân bố đều theo diện tích
                float r = (float) Math.sqrt(sprayRandom.nextDouble()) * radius;
                path.addCircle(s.x(i) + (float) Math.cos(angle) * r, s.y(i) + (float) Math.sin(angle) * r,
                        dot, Path.Direction.CW);
            }
        }
        canvas.drawPath(path, paint);
    }

    /** Thư pháp / Cầu vồng: vẽ từng đoạn ngắn, mỗi đoạn có độ dày (và màu) riêng, đầu tròn để nối liền. */
    private void drawSegments(Canvas canvas, Stroke s, int n) {
        configure(s);
        Color.colorToHSV(s.color, hsv);
        float startHue = hsv[0];
        hsv[1] = 1f;
        hsv[2] = 1f;
        if (n == 1) {
            paint.setStyle(Paint.Style.FILL);
            if (s.type == BrushType.RAINBOW) paint.setColor(hueColor(s, startHue, 0f));
            canvas.drawCircle(s.x(0), s.y(0), s.width * s.w(0) / 2f, paint);
            return;
        }
        float length = 0f;
        for (int i = 1; i < n; i++) {
            float x0 = s.x(i - 1);
            float y0 = s.y(i - 1);
            float x1 = s.x(i);
            float y1 = s.y(i);
            paint.setStrokeWidth(s.width * (s.w(i - 1) + s.w(i)) / 2f);
            if (s.type == BrushType.RAINBOW) paint.setColor(hueColor(s, startHue, length));
            canvas.drawLine(x0, y0, x1, y1, paint);
            length += (float) Math.hypot(x1 - x0, y1 - y0);
        }
    }

    /** Màu cầu vồng: hue bắt đầu từ màu đang chọn, xoay dần theo độ dài nét. */
    private int hueColor(Stroke s, float startHue, float length) {
        hsv[0] = (startHue + length * RAINBOW_DEGREES_PER_PX) % 360f;
        return Color.HSVToColor(Color.alpha(s.color), hsv);
    }

    private void configure(Stroke s) {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(s.color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(s.width);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        switch (s.type) {
            case MARKER:
                paint.setAlpha(MARKER_ALPHA);
                paint.setStrokeWidth(s.width * MARKER_WIDTH_FACTOR);
                paint.setStrokeCap(Paint.Cap.SQUARE);
                break;
            case SPRAY:
                paint.setStyle(Paint.Style.FILL); // nét phun là tập hợp chấm tròn
                break;
            case NEON:
                paint.setMaskFilter(blurFor(s.width));
                break;
            case ERASER:
                paint.setXfermode(clearMode); // đục trong suốt trên lớp vẽ, lộ nền trắng bên dưới
                break;
            default:
                break;
        }
    }

    /** Chỉ tạo BlurMaskFilter mới khi độ dày đổi — không cấp phát mỗi frame. */
    private BlurMaskFilter blurFor(float radius) {
        if (radius != neonBlurRadius) {
            neonBlur = new BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL);
            neonBlurRadius = radius;
        }
        return neonBlur;
    }
}
