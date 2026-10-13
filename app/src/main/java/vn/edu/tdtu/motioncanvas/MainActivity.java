package vn.edu.tdtu.motioncanvas;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import vn.edu.tdtu.motioncanvas.chart.ChartActivity;
import vn.edu.tdtu.motioncanvas.core.SystemBars;
import vn.edu.tdtu.motioncanvas.drawing.DrawingActivity;
import vn.edu.tdtu.motioncanvas.gesture.GestureActivity;
import vn.edu.tdtu.motioncanvas.particle.ParticleActivity;
import vn.edu.tdtu.motioncanvas.physics.PhysicsActivity;

public class MainActivity extends AppCompatActivity {

    /** Một thẻ trên màn Home. */
    private static final class Demo {
        final int title;
        final int description;
        final Class<? extends Activity> screen;

        Demo(int title, int description, Class<? extends Activity> screen) {
            this.title = title;
            this.description = description;
            this.screen = screen;
        }
    }

    private static final Demo[] DEMOS = {
            new Demo(R.string.demo_gesture, R.string.demo_gesture_desc, GestureActivity.class),
            new Demo(R.string.demo_physics, R.string.demo_physics_desc, PhysicsActivity.class),
            new Demo(R.string.demo_chart, R.string.demo_chart_desc, ChartActivity.class),
            new Demo(R.string.demo_particle, R.string.demo_particle_desc, ParticleActivity.class),
            new Demo(R.string.demo_drawing, R.string.demo_drawing_desc, DrawingActivity.class),
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        SystemBars.applyPadding(findViewById(R.id.main));

        LinearLayout list = findViewById(R.id.demo_list);
        LayoutInflater inflater = getLayoutInflater();
        for (Demo demo : DEMOS) {
            View card = inflater.inflate(R.layout.item_demo, list, false);
            ((TextView) card.findViewById(R.id.demo_title)).setText(demo.title);
            ((TextView) card.findViewById(R.id.demo_desc)).setText(demo.description);
            card.setOnClickListener(v -> startActivity(new Intent(this, demo.screen)));
            list.addView(card);
        }
    }
}
