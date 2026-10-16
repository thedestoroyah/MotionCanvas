package vn.edu.tdtu.motioncanvas;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

/** Application của app: chạy một lần lúc khởi động, trước mọi Activity. */
public class MotionCanvasApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // App chỉ có giao diện sáng: nếu để theo hệ thống, EdgeToEdge sẽ đổi icon status/navigation bar
        // thành trắng (vì máy đang ở chế độ tối) trên nền sáng → không nhìn thấy giờ và pin
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
