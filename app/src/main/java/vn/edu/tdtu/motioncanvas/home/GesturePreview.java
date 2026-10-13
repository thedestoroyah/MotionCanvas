package vn.edu.tdtu.motioncanvas.home;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/** Thẻ nhỏ treo trên dây, đung đưa tắt dần như lò xo rồi lặp lại. */
public final class GesturePreview implements Preview {
    private static final float PERIOD_S = 4f;
    private static final int CARD_COLOR = 0xFF5C6BC0;
    private static final int STRING_COLOR = 0xFF9FA8DA;

    private final DampedOscillation swing = new DampedOscillation(28f, 6f, 0.12f);
    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF card = new RectF();
    private float time;

    public GesturePreview() {
        cardPaint.setColor(CARD_COLOR);
        stringPaint.setColor(STRING_COLOR);
    }

    @Override
    public void update(float dt, int width, int height) {
        time = (time + dt) % PERIOD_S;
    }

    @Override
    public void draw(Canvas canvas, int width, int height) {
        float pivotX = width / 2f;
        float pivotY = height * 0.08f;
        float cardW = height * 0.38f;
        float cardH = height * 0.5f;
        float top = height * 0.35f;
        stringPaint.setStrokeWidth(height * 0.02f);
        canvas.save();
        canvas.rotate(swing.valueAt(time), pivotX, pivotY);
        canvas.drawLine(pivotX, pivotY, pivotX, top, stringPaint);
        card.set(pivotX - cardW / 2f, top, pivotX + cardW / 2f, top + cardH);
        canvas.drawRoundRect(card, cardW * 0.15f, cardW * 0.15f, cardPaint);
        canvas.restore();
    }
}
