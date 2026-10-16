package vn.edu.tdtu.motioncanvas.drawing;

/** Chế độ đối xứng: số lần xoay quanh tâm, và có lật gương mỗi nhánh hay không. */
public enum Symmetry {
    OFF(1, false),
    MIRROR(1, true),
    FOUR(4, true),
    SIX(6, true),
    EIGHT(8, true);

    public final int rotations;
    public final boolean mirror;

    Symmetry(int rotations, boolean mirror) {
        this.rotations = rotations;
        this.mirror = mirror;
    }

    /** Số bản sao của một nét được vẽ ra. */
    public int copies() {
        return rotations * (mirror ? 2 : 1);
    }

    /** Chế độ kế tiếp theo vòng (EIGHT → OFF), dùng cho nút bấm xoay vòng. */
    public Symmetry next() {
        Symmetry[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
