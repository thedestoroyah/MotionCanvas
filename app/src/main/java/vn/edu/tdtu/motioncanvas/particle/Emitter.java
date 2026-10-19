package vn.edu.tdtu.motioncanvas.particle;

import java.util.Objects;
import java.util.Random;

/**
 * Pool hạt cố định, dùng lại hạt đã chết. Gọi từ cùng thread với View.
 * Vị trí tính bằng px, thời gian bằng giây, unit là số px cho một dp.
 */
public class Emitter {
    public enum Mode {
        FIRE,
        WATER
    }

    public static final float MAX_DT = 1f / 30f;
    private static final float FIRE_RATE = 300f;
    private static final float WATER_RATE = 250f;
    private static final float FIRE_LIFT = 120f;
    private static final float WATER_GRAVITY = 1400f;
    private static final int SPLASH_DROPS = 3;

    private final Particle[] pool;
    private final float[] splashPositions;
    private final float unit;
    private final Random random;
    private Mode mode = Mode.FIRE;
    private float x;
    private float y;
    private float floorY = Float.MAX_VALUE;
    private boolean emitting = true;
    private double emissionRemainder;
    private int nextFree;
    private int aliveCount;

    /**
     * @param capacity số hạt tối đa trong pool
     * @param unit     số px cho một dp, dùng để quy đổi vận tốc và kích thước
     * @param random   nguồn ngẫu nhiên (truyền Random có seed khi cần kết quả lặp lại)
     * @throws IllegalArgumentException nếu capacity âm hoặc unit không dương
     */
    public Emitter(int capacity, float unit, Random random) {
        if (capacity < 0) throw new IllegalArgumentException("capacity không được âm");
        if (!Float.isFinite(unit) || unit <= 0f) {
            throw new IllegalArgumentException("unit phải dương và hữu hạn");
        }
        this.random = Objects.requireNonNull(random, "random");
        this.unit = unit;
        pool = new Particle[capacity];
        splashPositions = new float[capacity];
        for (int i = 0; i < capacity; i++) {
            pool[i] = new Particle();
        }
    }

    /** Đặt vị trí nguồn phát liên tục (px). */
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    /** Đổi chế độ cho hạt sinh sau đó; hạt đang sống giữ chế độ lúc sinh. */
    public void setMode(Mode mode) {
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    public Mode getMode() {
        return mode;
    }

    /** Đặt tung độ sàn (px): giọt nước chạm sàn thì tan và bắn ra vài giọt nhỏ. */
    public void setFloor(float y) {
        floorY = y;
    }

    /** Bật/tắt phát liên tục; burst() vẫn hoạt động khi tắt. */
    public void setEmitting(boolean on) {
        emitting = on;
        if (!on) emissionRemainder = 0d;
    }

    public int capacity() {
        return pool.length;
    }

    public Particle get(int index) {
        return pool[index];
    }

    /** Số hạt đang sống trong pool. */
    public int aliveCount() {
        return aliveCount;
    }

    /** Cập nhật mọi hạt và phát hạt mới; dt tính bằng giây, bị chặn tối đa MAX_DT, dt không dương thì bỏ qua. */
    public void update(float dt) {
        if (!(dt > 0f)) return;
        float elapsed = Math.min(dt, MAX_DT);
        int splashCount = 0;
        for (Particle particle : pool) {
            if (!particle.alive) continue;
            particle.age += elapsed;
            if (particle.age >= particle.life) {
                retire(particle);
                continue;
            }

            float acceleration = particle.mode == Mode.FIRE ? -FIRE_LIFT : WATER_GRAVITY;
            particle.vy += acceleration * unit * elapsed;
            particle.x += particle.vx * elapsed;
            particle.y += particle.vy * elapsed;
            if (particle.mode == Mode.WATER && particle.vy > 0f && particle.y >= floorY) {
                if (!particle.splash) {
                    splashPositions[splashCount++] = particle.x;
                }
                retire(particle);
            }
        }

        // Sinh sau khi duyệt xong để mọi hạt mới đều bắt đầu ở tuổi 0.
        for (int i = 0; i < splashCount; i++) {
            for (int drop = 0; drop < SPLASH_DROPS; drop++) {
                if (!spawnSplash(splashPositions[i])) break;
            }
        }
        if (emitting) emitContinuous(elapsed);
    }

    /** Bung tia lửa tại điểm va chạm, kể cả khi đã tắt phát liên tục */
    public void burst(float x, float y, int count) {
        for (int i = 0; i < count; i++) {
            Particle particle = acquire(Mode.FIRE, x, y, false);
            if (particle == null) return;
            double angle = random.nextDouble() * Math.PI * 2d;
            float speed = sample(80f, 260f) * unit;
            particle.vx = (float) Math.cos(angle) * speed;
            particle.vy = (float) Math.sin(angle) * speed;
            particle.life = sample(0.3f, 0.6f);
            particle.size = sample(4f, 8f) * unit;
        }
    }

    private void emitContinuous(float dt) {
        float rate = mode == Mode.FIRE ? FIRE_RATE : WATER_RATE;
        emissionRemainder += (double) rate * dt;
        // Bù sai số float ở sát số nguyên, ví dụ 0.01 giây nhân 300 hạt/giây.
        int requested = (int) (emissionRemainder + 0.000001d);
        emissionRemainder = Math.max(0d, emissionRemainder - requested);
        for (int i = 0; i < requested; i++) {
            Particle particle = acquire(mode, x, y, false);
            if (particle == null) {
                emissionRemainder = 0d;
                return;
            }
            if (mode == Mode.FIRE) {
                particle.x += sample(-12f, 12f) * unit;
                particle.vx = sample(-40f, 40f) * unit;
                particle.vy = -sample(150f, 300f) * unit;
                particle.life = sample(0.6f, 1.2f);
                particle.size = sample(14f, 24f) * unit;
            } else {
                particle.vx = sample(-120f, 120f) * unit;
                particle.vy = -sample(700f, 900f) * unit;
                particle.life = 3f;
                particle.size = sample(4f, 7f) * unit;
            }
        }
    }

    private boolean spawnSplash(float x) {
        Particle particle = acquire(Mode.WATER, x, floorY, true);
        if (particle == null) return false;
        particle.vx = sample(-150f, 150f) * unit;
        particle.vy = -sample(150f, 300f) * unit;
        particle.life = sample(0.3f, 0.5f);
        particle.size = sample(2f, 4f) * unit;
        return true;
    }

    private Particle acquire(Mode mode, float x, float y, boolean splash) {
        if (aliveCount == pool.length) return null;
        for (int scanned = 0; scanned < pool.length; scanned++) {
            int index = nextFree;
            nextFree = (nextFree + 1) % pool.length;
            Particle particle = pool[index];
            if (particle.alive) continue;
            particle.x = x;
            particle.y = y;
            particle.age = 0f;
            particle.mode = mode;
            particle.splash = splash;
            particle.alive = true;
            aliveCount++;
            return particle;
        }
        return null;
    }

    private void retire(Particle particle) {
        particle.alive = false;
        aliveCount--;
    }

    private float sample(float min, float max) {
        return min + (max - min) * random.nextFloat();
    }
}
