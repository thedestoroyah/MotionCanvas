package vn.edu.tdtu.motioncanvas.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.TypedValue;

import java.util.Locale;

/** Phần vẽ dùng chung cho các chart có trục Y. */
final class ChartAxes {
    private static final int GRID_COLOR = 0xFFE0E0E0;
    private static final int LABEL_COLOR = 0xFF757575;
    private static final float LABEL_SP = 11f;
    private static final float INSET_LEFT_DP = 40f;
    private static final float INSET_TOP_DP = 12f;
    private static final float INSET_RIGHT_DP = 12f;
    private static final float INSET_BOTTOM_DP = 24f;

    private ChartAxes() {
    }

    /** Vùng vẽ dữ liệu: chừa trái cho nhãn trục Y, dưới cho nhãn trục X. */
    static void plotArea(RectF out, int w, int h, float density) {
        out.set(INSET_LEFT_DP * density, INSET_TOP_DP * density,
                w - INSET_RIGHT_DP * density, h - INSET_BOTTOM_DP * density);
    }

    static Paint gridPaint(float density) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(GRID_COLOR);
        paint.setStrokeWidth(density);
        return paint;
    }

    static Paint labelPaint(Context context, Paint.Align align) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(LABEL_COLOR);
        paint.setTextAlign(align);
        paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, LABEL_SP,
                context.getResources().getDisplayMetrics()));
        return paint;
    }

    /** `lines` đường lưới ngang chia đều, nhãn giá trị nằm bên trái vùng vẽ. */
    static void drawHorizontalGrid(Canvas canvas, RectF plot, ChartScale scale, int lines,
                                   float labelGap, Paint grid, Paint label) {
        for (int i = 0; i < lines; i++) {
            float value = scale.tickValue(i, lines);
            float y = scale.toPixel(value);
            canvas.drawLine(plot.left, y, plot.right, y, grid);
            canvas.drawText(String.format(Locale.US, "%.0f", value), plot.left - labelGap,
                    y + label.getTextSize() / 3f, label);
        }
    }
}
