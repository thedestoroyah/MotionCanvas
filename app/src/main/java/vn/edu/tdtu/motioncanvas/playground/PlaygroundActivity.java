package vn.edu.tdtu.motioncanvas.playground;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import vn.edu.tdtu.motioncanvas.R;
import vn.edu.tdtu.motioncanvas.chart.LineChartView;
import vn.edu.tdtu.motioncanvas.core.SystemBars;

public class PlaygroundActivity extends AppCompatActivity {
    private static final float MS_PER_SECOND = 1000f;

    private PlaygroundView playgroundView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_playground);
        SystemBars.applyPadding(findViewById(R.id.main));

        playgroundView = findViewById(R.id.playground_view);
        LineChartView rateChart = findViewById(R.id.rate_chart);
        // Mỗi lần chart lấy mẫu: số va chạm trong chu kỳ → quy ra va chạm/giây
        rateChart.setDataSource(() ->
                playgroundView.takeCollisionCount() * MS_PER_SECOND / LineChartView.SAMPLE_INTERVAL_MS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        playgroundView.resume();
    }

    @Override
    protected void onPause() {
        playgroundView.pause();
        super.onPause();
    }
}
