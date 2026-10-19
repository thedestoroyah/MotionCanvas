package vn.edu.tdtu.motioncanvas;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import vn.edu.tdtu.motioncanvas.chart.ChartActivity;
import vn.edu.tdtu.motioncanvas.core.SystemBars;
import vn.edu.tdtu.motioncanvas.drawing.DrawingActivity;
import vn.edu.tdtu.motioncanvas.gesture.GestureActivity;
import vn.edu.tdtu.motioncanvas.home.ChartPreview;
import vn.edu.tdtu.motioncanvas.home.DrawingPreview;
import vn.edu.tdtu.motioncanvas.home.GesturePreview;
import vn.edu.tdtu.motioncanvas.home.ParticlePreview;
import vn.edu.tdtu.motioncanvas.home.PhysicsPreview;
import vn.edu.tdtu.motioncanvas.home.Preview;
import vn.edu.tdtu.motioncanvas.home.PreviewView;
import vn.edu.tdtu.motioncanvas.particle.ParticleActivity;
import vn.edu.tdtu.motioncanvas.physics.PhysicsActivity;
import vn.edu.tdtu.motioncanvas.playground.PlaygroundActivity;

/** Màn Home: lưới 2 cột, mỗi thẻ chạy một Preview động và mở màn demo tương ứng khi chạm. */
public class MainActivity extends AppCompatActivity {
    private static final float CARD_MARGIN_DP = 6f;

    /** Một thẻ trên màn Home. */
    private static final class Demo {
        final int title;
        final int description;
        final Class<? extends Activity> screen;
        final Supplier<Preview> preview;

        Demo(int title, int description, Class<? extends Activity> screen, Supplier<Preview> preview) {
            this.title = title;
            this.description = description;
            this.screen = screen;
            this.preview = preview;
        }
    }

    private static final Demo[] DEMOS = {
            new Demo(R.string.demo_gesture, R.string.demo_gesture_desc, GestureActivity.class, GesturePreview::new),
            new Demo(R.string.demo_physics, R.string.demo_physics_desc, PhysicsActivity.class, PhysicsPreview::new),
            new Demo(R.string.demo_chart, R.string.demo_chart_desc, ChartActivity.class, ChartPreview::new),
            new Demo(R.string.demo_particle, R.string.demo_particle_desc, ParticleActivity.class, ParticlePreview::new),
            new Demo(R.string.demo_drawing, R.string.demo_drawing_desc, DrawingActivity.class, DrawingPreview::new),
            new Demo(R.string.demo_playground, R.string.demo_playground_desc, PlaygroundActivity.class, PhysicsPreview::new),
    };

    private final List<PreviewView> previews = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        SystemBars.applyPadding(findViewById(R.id.main));

        GridLayout grid = findViewById(R.id.demo_grid);
        LayoutInflater inflater = getLayoutInflater();
        int margin = (int) (CARD_MARGIN_DP * getResources().getDisplayMetrics().density);
        for (Demo demo : DEMOS) {
            View card = inflater.inflate(R.layout.item_demo, grid, false);
            PreviewView preview = new PreviewView(this, demo.preview.get());
            ((FrameLayout) card.findViewById(R.id.demo_preview)).addView(preview,
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            previews.add(preview);
            ((TextView) card.findViewById(R.id.demo_title)).setText(demo.title);
            ((TextView) card.findViewById(R.id.demo_desc)).setText(demo.description);
            card.setOnClickListener(v -> open(v, demo.screen));

            // Hai cột chia đều bề ngang: width = 0 + trọng số cột 1
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f));
            params.width = 0;
            params.setMargins(margin, margin, margin, margin);
            grid.addView(card, params);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        for (PreviewView preview : previews) preview.resume();
    }

    @Override
    protected void onPause() {
        for (PreviewView preview : previews) preview.pause();
        super.onPause();
    }

    /** Màn demo phóng to ra từ chính vị trí thẻ vừa chạm. */
    private void open(View card, Class<? extends Activity> screen) {
        ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(
                card, 0, 0, card.getWidth(), card.getHeight());
        startActivity(new Intent(this, screen), options.toBundle());
    }
}
