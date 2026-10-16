package vn.edu.tdtu.motioncanvas.home;

/** Dao động tắt dần: x(t) = A·e^(−ζωt)·cos(ω_d·t), với ω_d = ω·√(1 − ζ²). */
final class DampedOscillation {
    private final float amplitude;
    private final float omega;
    private final float zeta;
    private final float dampedOmega;

    /** @param omega tần số góc riêng (rad/s); @param zeta hệ số tắt dần, 0 < ζ < 1. */
    DampedOscillation(float amplitude, float omega, float zeta) {
        this.amplitude = amplitude;
        this.omega = omega;
        this.zeta = zeta;
        this.dampedOmega = omega * (float) Math.sqrt(1f - zeta * zeta);
    }

    /** Biên độ còn lại tại thời điểm t. */
    float envelope(float t) {
        return amplitude * (float) Math.exp(-zeta * omega * t);
    }

    /** Độ lệch tại thời điểm t (giây), dao động quanh 0 trong khoảng ±envelope(t). */
    float valueAt(float t) {
        return envelope(t) * (float) Math.cos(dampedOmega * t);
    }
}
