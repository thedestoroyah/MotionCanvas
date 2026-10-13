package vn.edu.tdtu.motioncanvas.home;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

import vn.edu.tdtu.motioncanvas.core.FrameClock;

/** Chạy một Preview theo từng frame; Home gọi resume/pause theo vòng đời để không tốn pin khi ẩn. */
public class PreviewView extends View {
    private static final float MAX_DT = 1f / 30f;

    private final Preview preview;
    private final FrameClock clock = new FrameClock();
    private boolean running;

    public PreviewView(Context context, Preview preview) {
        super(context);
        this.preview = preview;
    }

    public void resume() {
        if (running) return;
        running = true;
        clock.reset();
        postInvalidateOnAnimation();
    }

    public void pause() {
        running = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        preview.update(Math.min(clock.tick(), MAX_DT), w, h);
        preview.draw(canvas, w, h);
        if (running) postInvalidateOnAnimation();
    }
}
