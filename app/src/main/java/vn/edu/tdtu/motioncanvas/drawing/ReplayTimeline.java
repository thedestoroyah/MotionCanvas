package vn.edu.tdtu.motioncanvas.drawing;

import java.util.List;

/** Ánh xạ tiến độ replay (0..1) sang "đang vẽ nét nào, tới điểm thứ mấy". */
final class ReplayTimeline {
    static final long MIN_DURATION_MS = 2000L;
    static final long MAX_DURATION_MS = 8000L;
    private static final float MS_PER_POINT = 12f;

    private final int[] counts;
    private final int total;

    ReplayTimeline(int[] counts) {
        this.counts = counts.clone();
        int sum = 0;
        for (int c : counts) sum += c;
        total = sum;
    }

    /** Replay chỉ phát bức tranh đang thấy: bỏ mọi mục trước lần "Xóa hết" cuối cùng. */
    static int firstAfterLastClear(List<Stroke> items) {
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i) == Stroke.CLEAR) return i + 1;
        }
        return 0;
    }

    int total() {
        return total;
    }

    /** Tranh càng nhiều điểm phát càng lâu, nhưng luôn trong [2 s, 8 s]. */
    long durationMs() {
        long ms = Math.round(total * MS_PER_POINT);
        return Math.max(MIN_DURATION_MS, Math.min(MAX_DURATION_MS, ms));
    }

    /** out[0] = nét đang vẽ dở (các nét trước nó đã xong), out[1] = số điểm đã hiện của nét đó. */
    void locate(float progress, int[] out) {
        int shown = Math.round(Math.max(0f, Math.min(1f, progress)) * total);
        for (int i = 0; i < counts.length; i++) {
            if (shown <= counts[i]) {
                out[0] = i;
                out[1] = shown;
                return;
            }
            shown -= counts[i];
        }
        out[0] = counts.length - 1;
        out[1] = counts.length == 0 ? 0 : counts[counts.length - 1];
    }
}
