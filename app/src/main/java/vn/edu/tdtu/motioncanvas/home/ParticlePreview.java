package vn.edu.tdtu.motioncanvas.home;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

import java.util.Random;

/** Ngọn lửa nhỏ: 60 hạt trong mảng cố định, hạt hết tuổi được sinh lại ngay (pool). */
public final class ParticlePreview implements Preview {
    private static final int COUNT = 60;
    private static final int BACKGROUND = 0xFF1A1A2E;
    private static final float LIFT_H = 0.6f;
    private static final float SIZE_H = 0.09f;

    private final float[] x = new float[COUNT];
    private final float[] y = new float[COUNT];
    private final float[] vx = new float[COUNT];
    private final float[] vy = new float[COUNT];
    private final float[] age = new float[COUNT];
    private final float[] life = new float[COUNT];
    private final Random random = new Random();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean started;

    public ParticlePreview() {
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD)); // hạt chồng nhau sáng hơn
    }

    @Override
    public void update(float dt, int width, int height) {
        if (height == 0) return;
        if (!started) {
            for (int i = 0; i < COUNT; i++) {
                spawn(i, width, height);
                age[i] = random.nextFloat() * life[i]; // lệch tuổi để không cháy bùng cùng lúc
            }
            started = true;
        }
        for (int i = 0; i < COUNT; i++) {
            age[i] += dt;
            if (age[i] >= life[i]) {
                spawn(i, width, height);
                continue;
            }
            vy[i] -= LIFT_H * height * dt;
            x[i] += vx[i] * dt;
            y[i] += vy[i] * dt;
        }
    }

    private void spawn(int i, int width, int height) {
        x[i] = width / 2f + (random.nextFloat() - 0.5f) * height * 0.25f;
        y[i] = height * 0.85f;
        vx[i] = (random.nextFloat() - 0.5f) * height * 0.3f;
        vy[i] = -height * (0.3f + 0.4f * random.nextFloat());
        age[i] = 0f;
        life[i] = 0.5f + 0.5f * random.nextFloat();
    }

    @Override
    public void draw(Canvas canvas, int width, int height) {
        canvas.drawColor(BACKGROUND);
        for (int i = 0; i < COUNT; i++) {
            float t = Math.min(1f, age[i] / life[i]);
            float k = 1f - t;
            paint.setColor(Color.argb((int) (255 * k), 255, (int) (220 * k), (int) (60 * k * k)));
            canvas.drawCircle(x[i], y[i], height * SIZE_H * (1f - 0.6f * t), paint);
        }
    }
}
