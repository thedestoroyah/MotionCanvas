package vn.edu.tdtu.motioncanvas.drawing;

import java.util.Arrays;

/**
 * Một mục trong lịch sử vẽ: danh sách điểm (x, y, w) — w là hệ số độ dày tại điểm đó (1 = bình thường).
 * Chỉ thêm điểm khi đang vẽ; sau khi nhấc tay thì không đổi nữa.
 * Lưu điểm thay vì Path để vẽ được "k điểm đầu" (Replay) với mọi loại brush.
 */
public final class Stroke {
    /** Lệnh "Xóa hết" được lưu như một mục để Undo được. */
    public static final Stroke CLEAR = new Stroke(BrushType.ERASER, 0, 0f, Symmetry.OFF, 0f, 0f, 0L);

    private static final int FIELDS = 3;
    private static final int INITIAL_POINTS = 32;

    public final BrushType type;
    public final int color;
    public final float width;
    /** Chế độ đối xứng và tâm lúc vẽ — đổi chế độ sau đó không ảnh hưởng nét cũ. */
    public final Symmetry symmetry;
    public final float centerX;
    public final float centerY;
    /** Seed cho brush Phun: vẽ lại (undo, replay) ra đúng các chấm như lần đầu. */
    public final long seed;

    private float[] data = new float[FIELDS * INITIAL_POINTS];
    private int count;

    public Stroke(BrushType type, int color, float width, Symmetry symmetry,
                  float centerX, float centerY, long seed) {
        this.type = type;
        this.color = color;
        this.width = width;
        this.symmetry = symmetry;
        this.centerX = centerX;
        this.centerY = centerY;
        this.seed = seed;
    }

    public void addPoint(float x, float y, float w) {
        if ((count + 1) * FIELDS > data.length) data = Arrays.copyOf(data, data.length * 2);
        int base = count * FIELDS;
        data[base] = x;
        data[base + 1] = y;
        data[base + 2] = w;
        count++;
    }

    public int pointCount() {
        return count;
    }

    public float x(int i) {
        return data[i * FIELDS];
    }

    public float y(int i) {
        return data[i * FIELDS + 1];
    }

    public float w(int i) {
        return data[i * FIELDS + 2];
    }

    /** Số điểm thực sự vẽ khi chỉ muốn vẽ `requested` điểm đầu. */
    public int visibleCount(int requested) {
        return Math.max(0, Math.min(requested, count));
    }
}
