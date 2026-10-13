package vn.edu.tdtu.motioncanvas.chart;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

import vn.edu.tdtu.motioncanvas.R;
import vn.edu.tdtu.motioncanvas.core.SystemBars;

public class ChartActivity extends AppCompatActivity {
    private static final float MIN_RANDOM_VALUE = 10f;

    private final Random random = new Random();
    private BarChartView barChart;
    private DonutChartView donutChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chart);
        SystemBars.applyPadding(findViewById(R.id.main));

        barChart = findViewById(R.id.bar_chart);
        donutChart = findViewById(R.id.donut_chart);
        findViewById(R.id.btn_shuffle).setOnClickListener(v -> shuffle());
        shuffle();
    }

    private void shuffle() {
        barChart.setData(randomValues(BarChartView.barCount(), BarChartView.MAX_VALUE));
        donutChart.setData(randomValues(DonutChartView.sliceCount(), 100f));
    }

    private float[] randomValues(int count, float max) {
        float[] values = new float[count];
        for (int i = 0; i < count; i++) {
            values[i] = MIN_RANDOM_VALUE + random.nextFloat() * (max - MIN_RANDOM_VALUE);
        }
        return values;
    }
}
