package vn.edu.tdtu.motioncanvas.core;

import java.util.function.LongSupplier;

/** Đo thời gian (giây) giữa hai frame cho các màn mô phỏng. */
public final class FrameClock {
    private final LongSupplier nanoTime;
    private long last = -1L;

    public FrameClock() {
        this(System::nanoTime);
    }

    public FrameClock(LongSupplier nanoTime) {
        this.nanoTime = nanoTime;
    }

    /** Gọi khi bắt đầu/tiếp tục chạy để frame đầu không tính thời gian đã tạm dừng. */
    public void reset() {
        last = -1L;
    }

    /** Số giây kể từ lần tick trước; lần đầu sau reset() trả về 0. */
    public float tick() {
        long now = nanoTime.getAsLong();
        float dt = last < 0 ? 0f : (now - last) / 1_000_000_000f;
        last = now;
        return dt;
    }
}
