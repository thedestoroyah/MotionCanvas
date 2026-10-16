package vn.edu.tdtu.motioncanvas.core;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Tiện ích xử lý status bar / navigation bar khi app chạy edge-to-edge. */
public final class SystemBars {
    private SystemBars() {
    }

    /** Đệm view gốc bằng kích thước status bar / navigation bar (app chạy edge-to-edge). */
    public static void applyPadding(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }
}
