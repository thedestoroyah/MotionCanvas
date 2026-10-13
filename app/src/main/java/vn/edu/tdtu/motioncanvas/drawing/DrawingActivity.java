package vn.edu.tdtu.motioncanvas.drawing;

import android.os.Bundle;
import android.widget.RadioGroup;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import vn.edu.tdtu.motioncanvas.R;
import vn.edu.tdtu.motioncanvas.core.SeekBars;
import vn.edu.tdtu.motioncanvas.core.SystemBars;

public class DrawingActivity extends AppCompatActivity {
    private DrawingView drawingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_drawing);
        SystemBars.applyPadding(findViewById(R.id.main));

        drawingView = findViewById(R.id.drawing_view);
        float density = getResources().getDisplayMetrics().density;

        RadioGroup brushes = findViewById(R.id.brush_group);
        brushes.setOnCheckedChangeListener((group, checkedId) -> drawingView.setBrush(brushFor(checkedId)));
        SeekBars.onProgress(findViewById(R.id.seek_width), p -> drawingView.setStrokeWidth(Math.max(1, p) * density));
        findViewById(R.id.btn_undo).setOnClickListener(v -> drawingView.undo());
        findViewById(R.id.btn_redo).setOnClickListener(v -> drawingView.redo());
        findViewById(R.id.btn_clear).setOnClickListener(v -> drawingView.clear());
    }

    private static BrushType brushFor(int checkedId) {
        if (checkedId == R.id.brush_marker) return BrushType.MARKER;
        if (checkedId == R.id.brush_spray) return BrushType.SPRAY;
        if (checkedId == R.id.brush_neon) return BrushType.NEON;
        if (checkedId == R.id.brush_eraser) return BrushType.ERASER;
        return BrushType.PEN;
    }
}
