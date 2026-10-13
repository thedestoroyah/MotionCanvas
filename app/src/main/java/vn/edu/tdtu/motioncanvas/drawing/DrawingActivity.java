package vn.edu.tdtu.motioncanvas.drawing;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vn.edu.tdtu.motioncanvas.R;
import vn.edu.tdtu.motioncanvas.core.SeekBars;
import vn.edu.tdtu.motioncanvas.core.SystemBars;

public class DrawingActivity extends AppCompatActivity {
    private static final int[] SWATCHES = {
            Color.BLACK, 0xFFE53935, 0xFFFB8C00, 0xFFFDD835, 0xFF43A047, 0xFF1E88E5, 0xFF8E24AA};
    private static final float SWATCH_DP = 32f;
    private static final float SWATCH_MARGIN_DP = 4f;
    private static final float PICKER_DP = 280f;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final ActivityResultLauncher<String> storagePermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    saveDrawing();
                } else {
                    toast(getString(R.string.save_permission_denied));
                }
            });
    private DrawingView drawingView;
    private float density;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_drawing);
        SystemBars.applyPadding(findViewById(R.id.main));

        drawingView = findViewById(R.id.drawing_view);
        density = getResources().getDisplayMetrics().density;

        RadioGroup brushes = findViewById(R.id.brush_group);
        brushes.setOnCheckedChangeListener((group, checkedId) -> drawingView.setBrush(brushFor(checkedId)));
        SeekBars.onProgress(findViewById(R.id.seek_width), p -> drawingView.setStrokeWidth(Math.max(1, p) * density));
        buildSwatches(findViewById(R.id.swatch_row));
        findViewById(R.id.btn_palette).setOnClickListener(v -> showColorPicker());
        findViewById(R.id.btn_undo).setOnClickListener(v -> drawingView.undo());
        findViewById(R.id.btn_redo).setOnClickListener(v -> drawingView.redo());
        findViewById(R.id.btn_clear).setOnClickListener(v -> drawingView.clear());
        findViewById(R.id.btn_save).setOnClickListener(v -> onSaveClicked());
    }

    @Override
    protected void onDestroy() {
        io.shutdown();
        super.onDestroy();
    }

    private static BrushType brushFor(int checkedId) {
        if (checkedId == R.id.brush_marker) return BrushType.MARKER;
        if (checkedId == R.id.brush_spray) return BrushType.SPRAY;
        if (checkedId == R.id.brush_neon) return BrushType.NEON;
        if (checkedId == R.id.brush_eraser) return BrushType.ERASER;
        return BrushType.PEN;
    }

    private void buildSwatches(LinearLayout row) {
        int size = (int) (SWATCH_DP * density);
        int margin = (int) (SWATCH_MARGIN_DP * density);
        for (int color : SWATCHES) {
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(color);
            dot.setStroke((int) density, Color.LTGRAY);

            View swatch = new View(this);
            swatch.setBackground(dot);
            swatch.setContentDescription(getString(R.string.swatch_desc,
                    String.format(Locale.US, "#%06X", color & 0xFFFFFF)));
            swatch.setOnClickListener(v -> drawingView.setColor(color));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, margin, margin, margin);
            row.addView(swatch, params);
        }
    }

    private void showColorPicker() {
        ColorPickerView picker = new ColorPickerView(this);
        picker.setColor(drawingView.getColor());
        int size = (int) (PICKER_DP * density);
        FrameLayout frame = new FrameLayout(this);
        frame.addView(picker, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.pick_color)
                .setView(frame)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> drawingView.setColor(picker.getColor()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void onSaveClicked() {
        // Android 10+ ghi vào MediaStore không cần quyền; Android 7–9 cần WRITE_EXTERNAL_STORAGE
        boolean needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED;
        if (needsPermission) {
            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        } else {
            saveDrawing();
        }
    }

    private void saveDrawing() {
        Bitmap bitmap = drawingView.exportBitmap();
        io.execute(() -> {
            try {
                ImageSaver.save(getApplicationContext(), bitmap);
                runOnUiThread(() -> toast(getString(R.string.save_ok)));
            } catch (IOException | RuntimeException e) {
                runOnUiThread(() -> toast(getString(R.string.save_failed, e.getMessage())));
            }
        });
    }

    private void toast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}
