package vn.edu.tdtu.motioncanvas.gesture;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import vn.edu.tdtu.motioncanvas.R;
import vn.edu.tdtu.motioncanvas.core.SystemBars;

public class GestureActivity extends AppCompatActivity {
    private GestureView gestureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gesture);
        SystemBars.applyPadding(findViewById(R.id.main));
        gestureView = findViewById(R.id.gesture_view);
    }

    @Override
    protected void onPause() {
        gestureView.pause();
        super.onPause();
    }
}
