package vn.edu.tdtu.motioncanvas.core;

import android.widget.SeekBar;

import java.util.function.IntConsumer;

/** Tiện ích rút gọn listener của SeekBar. */
public final class SeekBars {
    private SeekBars() {
    }

    /** Gọi action với giá trị hiện tại ngay lập tức, rồi mỗi khi người dùng kéo thanh trượt. */
    public static void onProgress(SeekBar bar, IntConsumer action) {
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                action.accept(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        action.accept(bar.getProgress());
    }
}
