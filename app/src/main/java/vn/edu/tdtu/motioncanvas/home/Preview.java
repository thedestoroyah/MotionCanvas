package vn.edu.tdtu.motioncanvas.home;

import android.graphics.Canvas;

/** Một hình động mini trên thẻ Home. Kích thước tính theo width/height để vừa mọi thẻ. */
public interface Preview {
    void update(float dt, int width, int height);

    void draw(Canvas canvas, int width, int height);
}
