package vn.edu.tdtu.motioncanvas.drawing;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vn.edu.tdtu.motioncanvas.R;
import vn.edu.tdtu.motioncanvas.core.SeekBars;
import vn.edu.tdtu.motioncanvas.core.SystemBars;

/** Màn Drawing: nối thanh công cụ (brush, màu, độ dày, đối xứng, undo/redo, replay, lưu ảnh) với DrawingView. */
public class DrawingActivity extends AppCompatActivity {
    /** Cùng thứ tự với R.array.brushes hiển thị trong combobox. */
    private static final BrushType[] BRUSHES = {
            BrushType.PEN, BrushType.MARKER, BrushType.SPRAY, BrushType.NEON,
            BrushType.CALLIGRAPHY, BrushType.RAINBOW, BrushType.FILL, BrushType.ERASER};
    private static final int[] SWATCHES = {
            Color.BLACK, Color.WHITE, 0xFFE53935, 0xFFD81B60, 0xFFFB8C00,
            0xFFFDD835, 0xFF43A047, 0xFF00ACC1, 0xFF1E88E5, 0xFF8E24AA};
    private static final float SWATCH_DP = 32f;
    private static final float SWATCH_MARGIN_DP = 4f;
    private static final float PICKER_DP = 280f;
    private static final float PREVIEW_DOT_DP = 20f;
    private static final float SELECTED_STROKE_DP = 3f;
    private static final int SELECTED_STROKE_COLOR = 0xFF212121;
    /** Các độ dày có sẵn (dp) trong popup; độ dày khác chỉnh bằng thanh kéo. */
    private static final int[] WIDTH_PRESETS = {2, 4, 8, 16, 24, 40};
    private static final int DEFAULT_WIDTH_DP = 8;
    private static final float POPUP_WIDTH_DP = 260f;
    private static final float POPUP_CORNER_DP = 12f;
    private static final float POPUP_ELEVATION_DP = 8f;
    private static final float PRESET_LINE_DP = 56f;
    private static final float PRESET_MIN_HEIGHT_DP = 48f;

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
    private BrushPreviewView brushPreview;
    private Button symmetryButton;
    private MaterialButton paletteButton;
    private final List<GradientDrawable> swatchDots = new ArrayList<>();
    private float density;
    private int widthDp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_drawing);
        SystemBars.applyPadding(findViewById(R.id.main));

        drawingView = findViewById(R.id.drawing_view);
        brushPreview = findViewById(R.id.brush_preview);
        density = getResources().getDisplayMetrics().density;

        Spinner brushes = findViewById(R.id.brush_spinner);
        brushes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                drawingView.setBrush(BRUSHES[position]);
                brushPreview.setBrush(BRUSHES[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Spinner luôn có một mục được chọn nên không cần xử lý
            }
        });
        setWidthDp(DEFAULT_WIDTH_DP);
        brushPreview.setOnClickListener(this::showWidthPopup);
        buildSwatches(findViewById(R.id.swatch_row));
        symmetryButton = findViewById(R.id.btn_symmetry);
        symmetryButton.setOnClickListener(v -> {
            drawingView.setSymmetry(drawingView.getSymmetry().next());
            showSymmetry();
        });
        showSymmetry();
        paletteButton = findViewById(R.id.btn_palette);
        paletteButton.setOnClickListener(v -> showColorPicker());
        showColor();
        findViewById(R.id.btn_undo).setOnClickListener(v -> drawingView.undo());
        findViewById(R.id.btn_redo).setOnClickListener(v -> drawingView.redo());
        findViewById(R.id.btn_clear).setOnClickListener(v -> drawingView.clear());
        findViewById(R.id.btn_save).setOnClickListener(v -> onSaveClicked());
        View controls = findViewById(R.id.controls);
        findViewById(R.id.btn_replay).setOnClickListener(v -> drawingView.startReplay());
        // Trong lúc phát lại khóa mọi điều khiển; chạm vào tranh để dừng
        drawingView.setReplayListener(replaying -> setEnabledTree(controls, !replaying));
    }

    @Override
    protected void onDestroy() {
        io.shutdown();
        super.onDestroy();
    }

    private static void setEnabledTree(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) setEnabledTree(group.getChildAt(i), enabled);
        }
    }

    private void setWidthDp(int dp) {
        widthDp = dp;
        drawingView.setStrokeWidth(dp * density);
        brushPreview.setStrokeWidth(dp * density);
    }

    /** Popup kiểu combobox dưới ô xem trước: chọn độ dày có sẵn (đóng popup) hoặc kéo thanh để chỉnh tự do. */
    @SuppressLint("InflateParams") // nội dung PopupWindow không có view cha nên root là null
    private void showWidthPopup(View anchor) {
        View content = getLayoutInflater().inflate(R.layout.popup_stroke_width, null);
        LinearLayout presets = content.findViewById(R.id.width_presets);
        TextView label = content.findViewById(R.id.width_label);
        SeekBar seek = content.findViewById(R.id.seek_width);
        PopupWindow popup = new PopupWindow(content, (int) (POPUP_WIDTH_DP * density),
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        for (int dp : WIDTH_PRESETS) {
            TextView row = presetRow(dp);
            row.setOnClickListener(v -> {
                seek.setProgress(dp);
                popup.dismiss();
            });
            presets.addView(row);
        }
        seek.setMax((int) BrushPreviewView.MAX_STROKE_DP);
        seek.setProgress(widthDp);
        SeekBars.onProgress(seek, p -> {
            setWidthDp(Math.max(1, p));
            label.setText(getString(R.string.stroke_width_value, widthDp));
            for (int i = 0; i < presets.getChildCount(); i++) {
                boolean selected = WIDTH_PRESETS[i] == widthDp;
                ((TextView) presets.getChildAt(i)).setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
            }
        });

        GradientDrawable background = new GradientDrawable();
        background.setColor(MaterialColors.getColor(anchor, com.google.android.material.R.attr.colorSurfaceContainer));
        background.setCornerRadius(POPUP_CORNER_DP * density);
        popup.setBackgroundDrawable(background);
        popup.setElevation(POPUP_ELEVATION_DP * density);
        // PopupWindow chỉ tự lật lên trên khi biết trước chiều cao → đo nội dung rồi đặt chiều cao cụ thể
        content.measure(View.MeasureSpec.makeMeasureSpec(popup.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        popup.setHeight(content.getMeasuredHeight());
        popup.showAsDropDown(anchor);
    }

    /** Một dòng trong danh sách độ dày: vạch vẽ đúng độ dày thật + nhãn "N dp". */
    private TextView presetRow(int dp) {
        GradientDrawable line = new GradientDrawable();
        line.setColor(Color.DKGRAY);
        line.setCornerRadius(dp * density / 2f);
        line.setSize((int) (PRESET_LINE_DP * density), Math.max(1, (int) (dp * density)));

        TextView row = new TextView(this);
        row.setText(getString(R.string.width_dp, dp));
        row.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        row.setCompoundDrawablesRelativeWithIntrinsicBounds(line, null, null, null);
        int pad = (int) (16 * density);
        row.setCompoundDrawablePadding(pad);
        row.setPadding(pad, 0, pad, 0);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinHeight((int) (PRESET_MIN_HEIGHT_DP * density));
        TypedValue ripple = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true);
        row.setBackgroundResource(ripple.resourceId);
        return row;
    }

    private void showSymmetry() {
        Symmetry s = drawingView.getSymmetry();
        String name;
        if (s == Symmetry.OFF) {
            name = getString(R.string.symmetry_off);
        } else if (s == Symmetry.MIRROR) {
            name = getString(R.string.symmetry_mirror);
        } else {
            name = String.valueOf(s.rotations);
        }
        symmetryButton.setText(getString(R.string.symmetry_label, name));
    }

    private void buildSwatches(LinearLayout row) {
        int size = (int) (SWATCH_DP * density);
        int margin = (int) (SWATCH_MARGIN_DP * density);
        for (int color : SWATCHES) {
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(color);
            swatchDots.add(dot);

            View swatch = new View(this);
            swatch.setBackground(dot);
            swatch.setContentDescription(getString(R.string.swatch_desc,
                    String.format(Locale.US, "#%06X", color & 0xFFFFFF)));
            swatch.setOnClickListener(v -> chooseColor(color));

            // Mỗi ô nằm giữa một phần weight bằng nhau → cả hàng dàn đều theo chiều ngang, ô vẫn tròn
            FrameLayout cell = new FrameLayout(this);
            cell.addView(swatch, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, size + 2 * margin, 1f);
            row.addView(cell, params);
        }
    }

    private void chooseColor(int color) {
        drawingView.setColor(color);
        showColor();
    }

    /** Viền đậm quanh ô màu đang chọn (nếu có trong danh sách) và chấm màu hiện tại trên nút "Màu…". */
    private void showColor() {
        int current = drawingView.getColor();
        brushPreview.setColor(current);
        for (int i = 0; i < swatchDots.size(); i++) {
            boolean selected = SWATCHES[i] == current;
            swatchDots.get(i).setStroke((int) ((selected ? SELECTED_STROKE_DP : 1f) * density),
                    selected ? SELECTED_STROKE_COLOR : Color.LTGRAY);
        }
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(current);
        dot.setStroke((int) density, Color.LTGRAY);
        int size = (int) (PREVIEW_DOT_DP * density);
        dot.setSize(size, size);
        paletteButton.setIconTint(null); // giữ nguyên màu, không nhuộm theo màu chữ
        paletteButton.setIcon(dot);
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
                .setPositiveButton(android.R.string.ok, (dialog, which) -> chooseColor(picker.getColor()))
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
