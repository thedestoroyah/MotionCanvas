package vn.edu.tdtu.motioncanvas.core;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Tiện ích xử lý status bar / navigation bar khi app chạy edge-to-edge. */
public final class SystemBars {
    private SystemBars() {
    }

    /** Đệm view gốc bằng kích thước status bar / navigation bar (app chạy edge-to-edge), cộng thêm padding đã khai báo trong XML. */
    public static void applyPadding(View root) {
        // Chụp padding gốc một lần: listener chạy lại mỗi khi insets đổi, không được cộng dồn
        int left = root.getPaddingLeft();
        int top = root.getPaddingTop();
        int right = root.getPaddingRight();
        int bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom);
            return insets;
        });
    }
}
