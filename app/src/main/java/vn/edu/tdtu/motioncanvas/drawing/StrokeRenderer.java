package vn.edu.tdtu.motioncanvas.drawing;

import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

/** Cấu hình Paint theo loại brush rồi vẽ nét lên Canvas. */
final class StrokeRenderer {
    private static final int MARKER_ALPHA = 90;
    private static final float MARKER_WIDTH_FACTOR = 2f;
    private static final float NEON_CORE_FACTOR = 0.35f;

    private final Paint paint = new Paint();
    private final Paint neonCore = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final PorterDuffXfermode clearMode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
    private BlurMaskFilter neonBlur;
    private float neonBlurRadius = -1f;

    StrokeRenderer() {
        neonCore.setColor(Color.WHITE);
        neonCore.setStyle(Paint.Style.STROKE);
        neonCore.setStrokeCap(Paint.Cap.ROUND);
        neonCore.setStrokeJoin(Paint.Join.ROUND);
    }

    void draw(Canvas canvas, Stroke stroke) {
        if (stroke == Stroke.CLEAR) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            return;
        }
        configure(stroke);
        canvas.drawPath(stroke.path, paint);
        if (stroke.type == BrushType.NEON) {
            // Neon = quầng sáng mờ màu + lõi trắng mảnh ở giữa
            neonCore.setStrokeWidth(stroke.width * NEON_CORE_FACTOR);
            canvas.drawPath(stroke.path, neonCore);
        }
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
