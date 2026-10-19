package vn.edu.tdtu.motioncanvas.particle;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButtonToggleGroup;

import vn.edu.tdtu.motioncanvas.R;
import vn.edu.tdtu.motioncanvas.core.SystemBars;

/** Màn Particle: chọn lửa/nước bằng nhóm nút và nối vòng đời với ParticleView. */
public class ParticleActivity extends AppCompatActivity {
    private ParticleView particleView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_particle);
        SystemBars.applyPadding(findViewById(R.id.main));
        particleView = findViewById(R.id.particle_view);
        MaterialButtonToggleGroup modes = findViewById(R.id.particle_modes);
        modes.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) applyMode(checkedId);
        });
        applyMode(modes.getCheckedButtonId());
    }

    private void applyMode(int checkedId) {
        particleView.setMode(checkedId == R.id.particle_water ? Emitter.Mode.WATER : Emitter.Mode.FIRE);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        MaterialButtonToggleGroup modes = findViewById(R.id.particle_modes);
        applyMode(modes.getCheckedButtonId());
    }

    @Override
    protected void onResume() {
        super.onResume();
        particleView.resume();
    }

    @Override
    protected void onPause() {
        particleView.pause();
        super.onPause();
    }
}
