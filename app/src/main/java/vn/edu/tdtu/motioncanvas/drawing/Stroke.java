package vn.edu.tdtu.motioncanvas.drawing;

import android.graphics.Path;

/** Một nét vẽ. Path chỉ được thêm điểm khi đang vẽ; sau khi nhấc tay thì không đổi nữa. */
public final class Stroke {
    /** Lệnh "Xóa hết" được lưu như một nét để Undo được. */
    public static final Stroke CLEAR = new Stroke(BrushType.ERASER, 0, 0f, new Path());

    public final BrushType type;
    public final int color;
    public final float width;
    public final Path path;

    public Stroke(BrushType type, int color, float width, Path path) {
        this.type = type;
        this.color = color;
        this.width = width;
        this.path = path;
    }
}
