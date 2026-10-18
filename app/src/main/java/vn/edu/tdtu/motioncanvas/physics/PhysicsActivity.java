package vn.edu.tdtu.motioncanvas.physics;

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import vn.edu.tdtu.motioncanvas.R;
import vn.edu.tdtu.motioncanvas.core.SeekBars;
import vn.edu.tdtu.motioncanvas.core.SystemBars;

/** Nối bộ điều khiển và vòng đời màn Physics */
public class PhysicsActivity extends AppCompatActivity {
    private PhysicsView physicsView;
    private AlertDialog valueDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_physics);
        SystemBars.applyPadding(findViewById(R.id.main));

        physicsView = findViewById(R.id.physics_view);
        World world = physicsView.getWorld();
        float density = getResources().getDisplayMetrics().density;
        SeekBar gravityBar = findViewById(R.id.seek_gravity);
        SeekBar bounceBar = findViewById(R.id.seek_bounce);
        TextView gravityLabel = findViewById(R.id.physics_gravity_label);
        TextView bounceLabel = findViewById(R.id.physics_bounce_label);
        // Đổi dp/s² trên thanh trượt sang px/s² cho engine
        SeekBars.onProgress(gravityBar, progress -> {
            world.setGravity(progress * density);
            gravityLabel.setText(getString(R.string.physics_gravity_value, progress));
        });
        SeekBars.onProgress(bounceBar, progress -> {
            world.setRestitution(progress / 100f);
            bounceLabel.setText(getString(R.string.physics_bounce_value, progress));
        });
        gravityLabel.setOnClickListener(view -> showValueInput(gravityBar, R.string.physics_gravity_input_title));
        bounceLabel.setOnClickListener(view -> showValueInput(bounceBar, R.string.physics_bounce_input_title));
        findViewById(R.id.btn_clear).setOnClickListener(view -> physicsView.reset());
    }

    private void showValueInput(SeekBar bar, int titleRes) {
        if (valueDialog != null) return;
        View content = getLayoutInflater().inflate(R.layout.dialog_physics_value, null);
        TextInputLayout field = content.findViewById(R.id.physics_value_field);
        TextInputEditText input = content.findViewById(R.id.physics_value_input);
        String rangeHint = getString(R.string.physics_value_range, bar.getMax());
        field.setHelperText(rangeHint);
        input.setText(String.valueOf(bar.getProgress()));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(titleRes)
                .setView(content)
                .setPositiveButton(R.string.physics_apply, null)
                .setNegativeButton(R.string.physics_cancel, null)
                .create();
        valueDialog = dialog;
        dialog.setOnDismissListener(ignored -> valueDialog = null);
        // Giữ hộp nhập mở khi giá trị chưa hợp lệ
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String text = input.getText() == null ? "" : input.getText().toString().trim();
                if (text.isEmpty()) {
                    field.setError(getString(R.string.physics_value_required));
                    return;
                }
                int value;
                try {
                    value = Integer.parseInt(text);
                } catch (NumberFormatException exception) {
                    field.setError(rangeHint);
                    return;
                }
                if (value < 0 || value > bar.getMax()) {
                    field.setError(rangeHint);
                    return;
                }
                // Dùng lại listener thanh trượt để cập nhật nhãn và engine
                bar.setProgress(value);
                dialog.dismiss();
            });
            input.requestFocus();
            input.selectAll();
            Window window = dialog.getWindow();
            if (window != null) {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
        });
        input.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) return false;
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            return true;
        });
        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        physicsView.resume();
    }

    @Override
    protected void onPause() {
        physicsView.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (valueDialog != null) valueDialog.dismiss();
        super.onDestroy();
    }
}
