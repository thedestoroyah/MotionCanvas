package vn.edu.tdtu.motioncanvas.particle;

/** Trạng thái một hạt trong pool. View chỉ đọc, Emitter quản lý vòng đời */
public final class Particle {
    public float x;
    public float y;
    public float vx;
    public float vy;
    public float age;
    public float life;
    public float size;
    public boolean alive;
    public boolean splash;
    public Emitter.Mode mode;

    /** Tuổi tương đối để View tính màu và kích thước khi vẽ */
    public float progress() {
        if (!(life > 0f)) return 1f;
        if (!(age > 0f)) return 0f;
        return Math.min(age / life, 1f);
    }
}
