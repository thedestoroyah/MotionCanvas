# Track B Extras – Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Thêm 4 gói cộng thêm cho track B: Drawing (Replay, Đối xứng, brush Thư pháp/Cầu vồng, Đổ màu), Chart (đường cong Bézier, gradient, điểm nhấp nháy, donut trượt bằng lò xo), Home (lưới thẻ có hình động mini, chuyển màn phóng to), theme sáng + thanh tiêu đề + icon.

**Architecture:** Mọi phần toán (flood fill, độ dày thư pháp, dòng thời gian replay, điểm điều khiển Catmull-Rom, dao động tắt dần) là Java thuần có JUnit. `Stroke` đổi từ `Path` cố định sang danh sách điểm `(x, y, w)` để mọi brush vẽ được "k điểm đầu" (phục vụ Replay). Home dùng một `PreviewView` chung hiển thị các `Preview` nhỏ.

**Tech Stack:** Java 11, Android View + XML, `androidx.dynamicanimation`, Material 3, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-10-13-track-b-extras-design.md` (bổ sung cho `docs/superpowers/specs/2026-10-13-animation-canvas-design.md`).

## Cách đọc plan này

- Chỉ **B** làm. Thứ tự: X1 → X3 (Drawing) → X4, X5 (Chart) → X6, X7 (giao diện, Home) → X8 (kiểm tra tổng).
- Mọi lệnh chạy ở thư mục gốc project `MotionCanvas/`. Đường dẫn Java gốc: `app/src/main/java/vn/edu/tdtu/motioncanvas/` (viết tắt **`$J`**), test: `app/src/test/java/vn/edu/tdtu/motioncanvas/` (viết tắt **`$T`**).
- Sau mỗi task: `git pull --rebase && git push` (B tự push).

## Global Constraints

- Ngôn ngữ / UI: Java + XML (View system). Không Kotlin, không Compose.
- minSdk: API 24.
- Thư viện ngoài: chỉ `appcompat`, `material`, `androidx.dynamicanimation`.
- Không tạo `Paint`, `Path`, `RectF` trong `onDraw` — tạo sẵn ở constructor/field.
- Không so sánh `R.id.*` bằng `switch` — dùng `if`.
- Chuỗi hiển thị nằm trong `res/values/strings.xml`.
- Mô phỏng/animation theo frame: `dt` kẹp tối đa `1/30 s`; dừng khi rời màn.
- Không sửa code hay layout của A (Physics, Gesture, Particle).
- Chỉ giao diện sáng (không dark mode).

## Review Focus

1. **Hai `MotionEvent` cùng thời điểm hoặc tốc độ không xác định với brush Thư pháp** → mong đợi độ dày vẫn hợp lệ, không NaN. Test: `CalligraphyWidthTest.nanSpeedGivesThickest`, `infiniteSpeedGivesThinnest` (X1).
2. **Chạm Đổ màu sát mép phải/dưới (tọa độ làm tròn ra ngoài ảnh)** → không crash, không đổi gì. Test: `FloodFillTest.outOfBoundsDoesNothing` (X1).
3. **Bấm "Phát lại" ngay sau "Xóa hết"** → không có gì để phát, nút không làm gì. Test: `ReplayTimelineTest.firstAfterLastClearWhenClearIsLast` (X3).
4. **Tranh kết thúc bằng một lần Đổ màu** → vùng tô phải hiện ra khi replay chạy tới cuối. Test: `ReplayTimelineTest.locateAtEndReturnsLastStrokeComplete` (X1).
5. **Line chart chế độ Đường cong khi mới có 0–1 điểm** → không crash. Test: `ChartMathTest.catmullRomWithFewerThanTwoPointsWritesNothing` (X4).

---

# DRAWING (gói ① + ④)

### Task X1: Toán thuần cho Drawing (`Symmetry`, `CalligraphyWidth`, `FloodFill`, `ReplayTimeline`)

Task này chỉ **thêm** file mới; app hiện tại vẫn build và chạy như cũ.

**Files:**
- Create: `$J/drawing/Symmetry.java`, `$J/drawing/CalligraphyWidth.java`, `$J/drawing/FloodFill.java`, `$J/drawing/ReplayTimeline.java`
- Test: `$T/drawing/SymmetryTest.java`, `$T/drawing/CalligraphyWidthTest.java`, `$T/drawing/FloodFillTest.java`, `$T/drawing/ReplayTimelineTest.java`

**Interfaces:**
- Consumes: không có.
- Produces:
  - `enum Symmetry { OFF, MIRROR, FOUR, SIX, EIGHT }` — field `int rotations`, `boolean mirror`; `int copies()`; `Symmetry next()` (EIGHT → OFF).
  - `CalligraphyWidth.MIN = 0.3f`, `MAX = 1.2f`; `static float factor(float speedDpPerMs)`; `static float smooth(float previous, float target)`.
  - `FloodFill.fill(int[] pixels, int width, int height, int x, int y, int newColor, int tolerance)` → `boolean` (true nếu có pixel đổi).
  - `ReplayTimeline(int[] counts)`; `int total()`; `long durationMs()` (kẹp `MIN_DURATION_MS = 2000`, `MAX_DURATION_MS = 8000`); `void locate(float progress, int[] out)` → `out[0]` = chỉ số nét đang vẽ dở, `out[1]` = số điểm đã hiện của nét đó.

- [ ] **Step 1: Viết test**

`$T/drawing/SymmetryTest.java`:

```java
package vn.edu.tdtu.motioncanvas.drawing;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SymmetryTest {
    @Test
    public void copiesCountRotationsAndMirror() {
        assertEquals(1, Symmetry.OFF.copies());
        assertEquals(2, Symmetry.MIRROR.copies());
        assertEquals(8, Symmetry.FOUR.copies());
        assertEquals(16, Symmetry.EIGHT.copies());
    }

    @Test
    public void nextCyclesBackToOff() {
        assertEquals(Symmetry.MIRROR, Symmetry.OFF.next());
        assertEquals(Symmetry.OFF, Symmetry.EIGHT.next());
    }
}
```

`$T/drawing/CalligraphyWidthTest.java`:

```java
package vn.edu.tdtu.motioncanvas.drawing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CalligraphyWidthTest {
    @Test
    public void standingStillGivesThickest() {
        assertEquals(CalligraphyWidth.MAX, CalligraphyWidth.factor(0f), 0f);
    }

    @Test
    public void veryFastGivesThinnest() {
        assertEquals(CalligraphyWidth.MIN, CalligraphyWidth.factor(1000f), 0f);
    }

    @Test
    public void factorStaysInRange() {
        for (float speed = -5f; speed <= 10f; speed += 0.25f) {
            float f = CalligraphyWidth.factor(speed);
            assertTrue(f >= CalligraphyWidth.MIN && f <= CalligraphyWidth.MAX);
        }
    }

    @Test
    public void nanSpeedGivesThickest() {
        assertEquals(CalligraphyWidth.MAX, CalligraphyWidth.factor(Float.NaN), 0f);
    }

    @Test
    public void infiniteSpeedGivesThinnest() {
        assertEquals(CalligraphyWidth.MIN, CalligraphyWidth.factor(Float.POSITIVE_INFINITY), 0f);
    }

    @Test
    public void smoothMovesPartWayTowardTarget() {
        float next = CalligraphyWidth.smooth(1f, 0f);
        assertTrue(next < 1f && next > 0f);
    }
}
```

`$T/drawing/FloodFillTest.java`:

```java
package vn.edu.tdtu.motioncanvas.drawing;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FloodFillTest {
    private static final int W = 0xFFFFFFFF;
    private static final int B = 0xFF000000;
    private static final int R = 0xFFFF0000;
    private static final int SIZE = 5;

    /** 5×5 nền trắng, viền đen hình vuông 3×3 ở giữa, ô (2,2) trắng bên trong. */
    private static int[] ring() {
        return new int[]{
                W, W, W, W, W,
                W, B, B, B, W,
                W, B, W, B, W,
                W, B, B, B, W,
                W, W, W, W, W,
        };
    }

    @Test
    public void fillsOnlyTheEnclosedRegion() {
        int[] px = ring();

        assertTrue(FloodFill.fill(px, SIZE, SIZE, 2, 2, R, 0));

        int[] expected = ring();
        expected[2 * SIZE + 2] = R;
        assertArrayEquals(expected, px);
    }

    @Test
    public void outsideFillDoesNotCrossTheBorder() {
        int[] px = ring();

        FloodFill.fill(px, SIZE, SIZE, 0, 0, R, 0);

        assertEquals(W, px[2 * SIZE + 2]); // bên trong viền vẫn trắng
        assertEquals(B, px[SIZE + 1]);     // viền giữ nguyên
        assertEquals(R, px[4 * SIZE + 4]); // góc ngoài đã tô
    }

    @Test
    public void sameColorDoesNothing() {
        int[] px = ring();
        assertFalse(FloodFill.fill(px, SIZE, SIZE, 1, 1, B, 0));
        assertArrayEquals(ring(), px);
    }

    @Test
    public void outOfBoundsDoesNothing() {
        int[] px = ring();
        assertFalse(FloodFill.fill(px, SIZE, SIZE, SIZE, 0, R, 0));
        assertFalse(FloodFill.fill(px, SIZE, SIZE, -1, 2, R, 0));
        assertArrayEquals(ring(), px);
    }

    @Test
    public void toleranceAbsorbsNearlySameColor() {
        int nearWhite = 0xFFF0F0F0;
        int[] strict = {W, nearWhite, W};
        int[] loose = {W, nearWhite, W};

        FloodFill.fill(strict, 3, 1, 0, 0, R, 0);
        FloodFill.fill(loose, 3, 1, 0, 0, R, 32);

        assertArrayEquals(new int[]{R, nearWhite, W}, strict);
        assertArrayEquals(new int[]{R, R, R}, loose);
    }
}
```

`$T/drawing/ReplayTimelineTest.java`:

```java
package vn.edu.tdtu.motioncanvas.drawing;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ReplayTimelineTest {
    private final ReplayTimeline timeline = new ReplayTimeline(new int[]{10, 5});
    private final int[] out = new int[2];

    @Test
    public void totalSumsAllPoints() {
        assertEquals(15, timeline.total());
    }

    @Test
    public void durationIsClampedBetweenTwoAndEightSeconds() {
        assertEquals(ReplayTimeline.MIN_DURATION_MS, new ReplayTimeline(new int[]{3}).durationMs());
        assertEquals(ReplayTimeline.MAX_DURATION_MS, new ReplayTimeline(new int[]{100_000}).durationMs());
        assertEquals(3600L, new ReplayTimeline(new int[]{300}).durationMs());
    }

    @Test
    public void locateAtStartIsFirstStrokeWithNoPoints() {
        timeline.locate(0f, out);
        assertArrayEquals(new int[]{0, 0}, out);
    }

    @Test
    public void locateInsideFirstStroke() {
        timeline.locate(0.5f, out); // 7.5 → 8 điểm
        assertArrayEquals(new int[]{0, 8}, out);
    }

    @Test
    public void locateMovesIntoSecondStroke() {
        timeline.locate(0.8f, out); // 12 điểm = 10 của nét 0 + 2 của nét 1
        assertArrayEquals(new int[]{1, 2}, out);
    }

    @Test
    public void locateAtEndReturnsLastStrokeComplete() {
        timeline.locate(1f, out);
        assertArrayEquals(new int[]{1, 5}, out);
    }

    @Test
    public void locateClampsProgressOutsideZeroToOne() {
        timeline.locate(-1f, out);
        assertArrayEquals(new int[]{0, 0}, out);
        timeline.locate(9f, out);
        assertArrayEquals(new int[]{1, 5}, out);
    }
}
```

- [ ] **Step 2: Chạy test, xác nhận FAIL**

Run: `./gradlew :app:testDebugUnitTest --tests "vn.edu.tdtu.motioncanvas.drawing.*"`
Expected: FAIL — `cannot find symbol: class Symmetry` (và các class còn lại)

- [ ] **Step 3: Viết Symmetry**

`$J/drawing/Symmetry.java`:

```java
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

    public Symmetry next() {
        Symmetry[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
```

- [ ] **Step 4: Viết CalligraphyWidth**

`$J/drawing/CalligraphyWidth.java`:

```java
package vn.edu.tdtu.motioncanvas.drawing;

/** Brush thư pháp: tay càng nhanh nét càng mảnh. */
final class CalligraphyWidth {
    static final float MIN = 0.3f;
    static final float MAX = 1.2f;
    /** Tốc độ (dp/ms) từ đó trở lên thì nét mảnh nhất. */
    private static final float SPEED_FOR_MIN = 3f;
    /** Mỗi điểm chỉ tiến 30% về độ dày mới → nét đổi độ dày êm, không giật. */
    private static final float SMOOTHING = 0.3f;

    private CalligraphyWidth() {
    }

    static float factor(float speedDpPerMs) {
        if (!(speedDpPerMs > 0f)) return MAX; // 0, âm hoặc NaN: coi như đứng yên
        float t = Math.min(1f, speedDpPerMs / SPEED_FOR_MIN);
        return MAX - (MAX - MIN) * t;
    }

    static float smooth(float previous, float target) {
        return previous + SMOOTHING * (target - previous);
    }
}
```

- [ ] **Step 5: Viết FloodFill**

`$J/drawing/FloodFill.java`:

```java
package vn.edu.tdtu.motioncanvas.drawing;

import java.util.Arrays;

/**
 * Tô loang theo hàng (scanline): mỗi lần lấy một điểm, tô cả đoạn ngang liền nhau,
 * rồi đẩy điểm đầu các đoạn tô được ở hàng trên và hàng dưới vào ngăn xếp.
 */
final class FloodFill {
    private FloodFill() {
    }

    /** @return true nếu có pixel bị đổi màu. */
    static boolean fill(int[] pixels, int width, int height, int x, int y, int newColor, int tolerance) {
        if (x < 0 || y < 0 || x >= width || y >= height) return false;
        int target = pixels[y * width + x];
        if (target == newColor) return false;

        boolean[] done = new boolean[pixels.length]; // tránh lặp vô hạn khi newColor gần giống target
        IntStack stack = new IntStack();
        stack.push(y * width + x);
        boolean changed = false;
        while (!stack.isEmpty()) {
            int seed = stack.pop();
            if (!fillable(pixels, done, seed, target, tolerance)) continue;
            int row = seed / width;
            int rowStart = row * width;
            int rowEnd = rowStart + width - 1;
            int left = seed;
            while (left > rowStart && fillable(pixels, done, left - 1, target, tolerance)) left--;
            int right = seed;
            while (right < rowEnd && fillable(pixels, done, right + 1, target, tolerance)) right++;
            for (int i = left; i <= right; i++) {
                pixels[i] = newColor;
                done[i] = true;
            }
            changed = true;
            if (row > 0) pushRuns(pixels, done, stack, left - width, right - width, target, tolerance);
            if (row < height - 1) pushRuns(pixels, done, stack, left + width, right + width, target, tolerance);
        }
        return changed;
    }

    /** Đẩy điểm đầu của mỗi đoạn liên tiếp tô được trong [from, to]. */
    private static void pushRuns(int[] pixels, boolean[] done, IntStack stack, int from, int to,
                                 int target, int tolerance) {
        boolean inRun = false;
        for (int i = from; i <= to; i++) {
            boolean ok = fillable(pixels, done, i, target, tolerance);
            if (ok && !inRun) stack.push(i);
            inRun = ok;
        }
    }

    private static boolean fillable(int[] pixels, boolean[] done, int i, int target, int tolerance) {
        return !done[i] && close(pixels[i], target, tolerance);
    }

    /** Hai màu ARGB lệch nhau không quá `tolerance` ở mọi kênh. */
    private static boolean close(int a, int b, int tolerance) {
        return Math.abs((a >>> 24) - (b >>> 24)) <= tolerance
                && Math.abs(((a >> 16) & 0xFF) - ((b >> 16) & 0xFF)) <= tolerance
                && Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF)) <= tolerance
                && Math.abs((a & 0xFF) - (b & 0xFF)) <= tolerance;
    }

    /** Ngăn xếp int tự giãn — tránh boxing Integer cho hàng triệu pixel. */
    private static final class IntStack {
        private int[] data = new int[256];
        private int size;

        void push(int value) {
            if (size == data.length) data = Arrays.copyOf(data, size * 2);
            data[size++] = value;
        }

        int pop() {
            return data[--size];
        }

        boolean isEmpty() {
            return size == 0;
        }
    }
}
```

- [ ] **Step 6: Viết ReplayTimeline**

`$J/drawing/ReplayTimeline.java`:

```java
package vn.edu.tdtu.motioncanvas.drawing;

/** Ánh xạ tiến độ replay (0..1) sang "đang vẽ nét nào, tới điểm thứ mấy". */
final class ReplayTimeline {
    static final long MIN_DURATION_MS = 2000L;
    static final long MAX_DURATION_MS = 8000L;
    private static final float MS_PER_POINT = 12f;

    private final int[] counts;
    private final int total;

    ReplayTimeline(int[] counts) {
        this.counts = counts.clone();
        int sum = 0;
        for (int c : counts) sum += c;
        total = sum;
    }

    int total() {
        return total;
    }

    /** Tranh càng nhiều điểm phát càng lâu, nhưng luôn trong [2 s, 8 s]. */
    long durationMs() {
        long ms = Math.round(total * MS_PER_POINT);
        return Math.max(MIN_DURATION_MS, Math.min(MAX_DURATION_MS, ms));
    }

    /** out[0] = nét đang vẽ dở (các nét trước nó đã xong), out[1] = số điểm đã hiện của nét đó. */
    void locate(float progress, int[] out) {
        int shown = Math.round(Math.max(0f, Math.min(1f, progress)) * total);
        for (int i = 0; i < counts.length; i++) {
            if (shown <= counts[i]) {
                out[0] = i;
                out[1] = shown;
                return;
            }
            shown -= counts[i];
        }
        out[0] = counts.length - 1;
        out[1] = counts.length == 0 ? 0 : counts[counts.length - 1];
    }
}
```

- [ ] **Step 7: Chạy test, xác nhận PASS**

Run: `./gradlew :app:testDebugUnitTest --tests "vn.edu.tdtu.motioncanvas.drawing.*"`
Expected: PASS — Symmetry 2, CalligraphyWidth 6, FloodFill 5, ReplayTimeline 7, cùng UndoStack 4 và PickerMath 4 cũ.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/vn/edu/tdtu/motioncanvas/drawing app/src/test/java/vn/edu/tdtu/motioncanvas/drawing
git commit -m "feat: add symmetry, calligraphy width, flood fill and replay timeline helpers"
```

---

### Task X2: Nét vẽ dạng điểm, brush Thư pháp / Cầu vồng / Đổ màu, Đối xứng

**Files:**
- Modify (ghi đè): `$J/drawing/Stroke.java`, `$J/drawing/BrushType.java`, `$J/drawing/StrokeRenderer.java`, `$J/drawing/DrawingView.java`, `$J/drawing/DrawingActivity.java`, `app/src/main/res/layout/activity_drawing.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `$T/drawing/StrokeTest.java`

**Interfaces:**
- Consumes: `Symmetry`, `CalligraphyWidth`, `FloodFill` (X1); `UndoStack` (B4, không đổi); `ColorPickerView`, `ImageSaver` (B5, không đổi); `SeekBars`, `SystemBars` (Task 0.2).
- Produces (X3 dùng):
  - `enum BrushType { PEN, MARKER, SPRAY, NEON, ERASER, CALLIGRAPHY, RAINBOW, FILL }`
  - `Stroke(BrushType type, int color, float width, Symmetry symmetry, float centerX, float centerY, long seed)`; `Stroke.CLEAR`; `void addPoint(float x, float y, float w)`; `int pointCount()`; `float x(int i)`, `y(int i)`, `w(int i)`; `int visibleCount(int requested)`.
  - `StrokeRenderer.commit(Bitmap bitmap, Canvas canvas, Stroke s)` — ghi hẳn một mục lịch sử lên Bitmap; `StrokeRenderer.draw(Canvas canvas, Stroke s, int points)` — vẽ `points` điểm đầu; `boolean applyFill(Bitmap bitmap, Stroke s)`.
  - `DrawingView.setSymmetry(Symmetry)`, `Symmetry getSymmetry()`.
  - Layout: `LinearLayout` `@id/controls` bọc mọi điều khiển; hàng `@id/tool_row` chứa `@id/btn_symmetry`.

- [ ] **Step 1: Viết test cho Stroke dạng điểm**

`$T/drawing/StrokeTest.java`:

```java
package vn.edu.tdtu.motioncanvas.drawing;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StrokeTest {
    private static Stroke pen() {
        return new Stroke(BrushType.PEN, 0xFF000000, 8f, Symmetry.OFF, 50f, 50f, 1L);
    }

    @Test
    public void keepsPointsBeyondInitialCapacity() {
        Stroke s = pen();
        for (int i = 0; i < 100; i++) s.addPoint(i, 2 * i, 0.5f);

        assertEquals(100, s.pointCount());
        assertEquals(99f, s.x(99), 0f);
        assertEquals(198f, s.y(99), 0f);
        assertEquals(0.5f, s.w(99), 0f);
        assertEquals(0f, s.x(0), 0f);
    }

    @Test
    public void visibleCountIsClampedToPointCount() {
        Stroke s = pen();
        s.addPoint(1f, 1f, 1f);
        s.addPoint(2f, 2f, 1f);
        s.addPoint(3f, 3f, 1f);

        assertEquals(2, s.visibleCount(2));
        assertEquals(3, s.visibleCount(99));
        assertEquals(0, s.visibleCount(-4));
    }

    @Test
    public void clearMarkerHasNoPoints() {
        assertEquals(0, Stroke.CLEAR.pointCount());
    }
}
```

- [ ] **Step 2: Chạy test, xác nhận FAIL**

Run: `./gradlew :app:testDebugUnitTest --tests "vn.edu.tdtu.motioncanvas.drawing.StrokeTest"`
Expected: FAIL — `constructor Stroke in class Stroke cannot be applied to given types` (Stroke cũ nhận `Path`).

- [ ] **Step 3: Ghi đè BrushType và Stroke**

`$J/drawing/BrushType.java`:

```java
package vn.edu.tdtu.motioncanvas.drawing;

public enum BrushType { PEN, MARKER, SPRAY, NEON, ERASER, CALLIGRAPHY, RAINBOW, FILL }
```

`$J/drawing/Stroke.java`:

```java
package vn.edu.tdtu.motioncanvas.drawing;

import java.util.Arrays;

/**
 * Một mục trong lịch sử vẽ: danh sách điểm (x, y, w) — w là hệ số độ dày tại điểm đó (1 = bình thường).
 * Chỉ thêm điểm khi đang vẽ; sau khi nhấc tay thì không đổi nữa.
 * Lưu điểm thay vì Path để vẽ được "k điểm đầu" (Replay) với mọi loại brush.
 */
public final class Stroke {
    /** Lệnh "Xóa hết" được lưu như một mục để Undo được. */
    public static final Stroke CLEAR = new Stroke(BrushType.ERASER, 0, 0f, Symmetry.OFF, 0f, 0f, 0L);

    private static final int FIELDS = 3;
    private static final int INITIAL_POINTS = 32;

    public final BrushType type;
    public final int color;
    public final float width;
    /** Chế độ đối xứng và tâm lúc vẽ — đổi chế độ sau đó không ảnh hưởng nét cũ. */
    public final Symmetry symmetry;
    public final float centerX;
    public final float centerY;
    /** Seed cho brush Phun: vẽ lại (undo, replay) ra đúng các chấm như lần đầu. */
    public final long seed;

    private float[] data = new float[FIELDS * INITIAL_POINTS];
    private int count;

    public Stroke(BrushType type, int color, float width, Symmetry symmetry,
                  float centerX, float centerY, long seed) {
        this.type = type;
        this.color = color;
        this.width = width;
        this.symmetry = symmetry;
        this.centerX = centerX;
        this.centerY = centerY;
        this.seed = seed;
    }

    public void addPoint(float x, float y, float w) {
        if ((count + 1) * FIELDS > data.length) data = Arrays.copyOf(data, data.length * 2);
        int base = count * FIELDS;
        data[base] = x;
        data[base + 1] = y;
        data[base + 2] = w;
        count++;
    }

    public int pointCount() {
        return count;
    }

    public float x(int i) {
        return data[i * FIELDS];
    }

    public float y(int i) {
        return data[i * FIELDS + 1];
    }

    public float w(int i) {
        return data[i * FIELDS + 2];
    }

    /** Số điểm thực sự vẽ khi chỉ muốn vẽ `requested` điểm đầu. */
    public int visibleCount(int requested) {
        return Math.max(0, Math.min(requested, count));
    }
}
```

- [ ] **Step 4: Ghi đè StrokeRenderer**

`$J/drawing/StrokeRenderer.java`:

```java
package vn.edu.tdtu.motioncanvas.drawing;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

import java.util.Random;

/** Vẽ một Stroke lên Canvas theo loại brush, lặp lại theo chế độ đối xứng. */
final class StrokeRenderer {
    private static final int MARKER_ALPHA = 90;
    private static final float MARKER_WIDTH_FACTOR = 2f;
    private static final float NEON_CORE_FACTOR = 0.35f;
    private static final int SPRAY_DOTS = 12;
    private static final float SPRAY_RADIUS_FACTOR = 1.5f;
    private static final float SPRAY_DOT_FACTOR = 0.1f;
    private static final float RAINBOW_DEGREES_PER_PX = 0.6f;
    /** Sai số màu khi đổ màu: tô kín cả viền khử răng cưa nhạt quanh nét. */
    private static final int FILL_TOLERANCE = 64;

    private final Paint paint = new Paint();
    private final Paint neonCore = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Random sprayRandom = new Random();
    private final float[] hsv = new float[3];
    private final PorterDuffXfermode clearMode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
    private BlurMaskFilter neonBlur;
    private float neonBlurRadius = -1f;
    private int[] pixels = new int[0];

    StrokeRenderer() {
        neonCore.setColor(Color.WHITE);
        neonCore.setStyle(Paint.Style.STROKE);
        neonCore.setStrokeCap(Paint.Cap.ROUND);
        neonCore.setStrokeJoin(Paint.Join.ROUND);
    }

    /** Ghi hẳn một mục lịch sử lên Bitmap lớp vẽ (khi nhấc tay, dựng lại sau undo, replay). */
    void commit(Bitmap bitmap, Canvas canvas, Stroke s) {
        if (s == Stroke.CLEAR) {
            bitmap.eraseColor(Color.TRANSPARENT);
        } else if (s.type == BrushType.FILL) {
            applyFill(bitmap, s);
        } else {
            draw(canvas, s, s.pointCount());
        }
    }

    /** Tô loang tại điểm của nét FILL; @return true nếu có pixel đổi màu. */
    boolean applyFill(Bitmap bitmap, Stroke s) {
        if (s.pointCount() == 0) return false;
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (pixels.length != w * h) pixels = new int[w * h];
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
        boolean changed = FloodFill.fill(pixels, w, h, (int) s.x(0), (int) s.y(0), s.color, FILL_TOLERANCE);
        if (changed) bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        return changed;
    }

    /** Vẽ `points` điểm đầu của nét; mỗi bản sao đối xứng là một lần xoay/lật Canvas quanh tâm. */
    void draw(Canvas canvas, Stroke s, int points) {
        int n = s.visibleCount(points);
        if (n == 0 || s.type == BrushType.FILL) return;
        Symmetry symmetry = s.symmetry;
        int mirrors = symmetry.mirror ? 2 : 1;
        for (int r = 0; r < symmetry.rotations; r++) {
            for (int m = 0; m < mirrors; m++) {
                canvas.save();
                canvas.rotate(360f * r / symmetry.rotations, s.centerX, s.centerY);
                if (m == 1) canvas.scale(-1f, 1f, s.centerX, s.centerY);
                drawOnce(canvas, s, n);
                canvas.restore();
            }
        }
    }

    private void drawOnce(Canvas canvas, Stroke s, int n) {
        switch (s.type) {
            case SPRAY:
                drawSpray(canvas, s, n);
                break;
            case CALLIGRAPHY:
            case RAINBOW:
                drawSegments(canvas, s, n);
                break;
            default:
                drawSmoothPath(canvas, s, n);
                break;
        }
    }

    private void drawSmoothPath(Canvas canvas, Stroke s, int n) {
        configure(s);
        path.reset();
        path.moveTo(s.x(0), s.y(0));
        for (int i = 1; i < n; i++) {
            float px = s.x(i - 1);
            float py = s.y(i - 1);
            // Đường cong qua trung điểm → nét mượt, không gãy khúc
            path.quadTo(px, py, (px + s.x(i)) / 2f, (py + s.y(i)) / 2f);
        }
        path.lineTo(s.x(n - 1), s.y(n - 1)); // 1 điểm → đoạn dài 0, đầu tròn → một chấm
        canvas.drawPath(path, paint);
        if (s.type == BrushType.NEON) {
            // Neon = quầng sáng mờ màu + lõi trắng mảnh ở giữa
            neonCore.setStrokeWidth(s.width * NEON_CORE_FACTOR);
            canvas.drawPath(path, neonCore);
        }
    }

    /** Rải chấm quanh từng điểm; Random đặt lại theo seed nên lần vẽ nào cũng ra đúng các chấm đó. */
    private void drawSpray(Canvas canvas, Stroke s, int n) {
        configure(s);
        sprayRandom.setSeed(s.seed);
        float radius = s.width * SPRAY_RADIUS_FACTOR;
        float dot = Math.max(1f, s.width * SPRAY_DOT_FACTOR);
        path.reset();
        for (int i = 0; i < n; i++) {
            for (int d = 0; d < SPRAY_DOTS; d++) {
                double angle = sprayRandom.nextDouble() * 2 * Math.PI;
                // sqrt để chấm phân bố đều theo diện tích
                float r = (float) Math.sqrt(sprayRandom.nextDouble()) * radius;
                path.addCircle(s.x(i) + (float) Math.cos(angle) * r, s.y(i) + (float) Math.sin(angle) * r,
                        dot, Path.Direction.CW);
            }
        }
        canvas.drawPath(path, paint);
    }

    /** Thư pháp / Cầu vồng: vẽ từng đoạn ngắn, mỗi đoạn có độ dày (và màu) riêng, đầu tròn để nối liền. */
    private void drawSegments(Canvas canvas, Stroke s, int n) {
        configure(s);
        Color.colorToHSV(s.color, hsv);
        float startHue = hsv[0];
        hsv[1] = 1f;
        hsv[2] = 1f;
        if (n == 1) {
            paint.setStyle(Paint.Style.FILL);
            if (s.type == BrushType.RAINBOW) paint.setColor(hueColor(s, startHue, 0f));
            canvas.drawCircle(s.x(0), s.y(0), s.width * s.w(0) / 2f, paint);
            return;
        }
        float length = 0f;
        for (int i = 1; i < n; i++) {
            float x0 = s.x(i - 1);
            float y0 = s.y(i - 1);
            float x1 = s.x(i);
            float y1 = s.y(i);
            paint.setStrokeWidth(s.width * (s.w(i - 1) + s.w(i)) / 2f);
            if (s.type == BrushType.RAINBOW) paint.setColor(hueColor(s, startHue, length));
            canvas.drawLine(x0, y0, x1, y1, paint);
            length += (float) Math.hypot(x1 - x0, y1 - y0);
        }
    }

    /** Màu cầu vồng: hue bắt đầu từ màu đang chọn, xoay dần theo độ dài nét. */
    private int hueColor(Stroke s, float startHue, float length) {
        hsv[0] = (startHue + length * RAINBOW_DEGREES_PER_PX) % 360f;
        return Color.HSVToColor(Color.alpha(s.color), hsv);
    }

    private void configure(Stroke s) {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(s.color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(s.width);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        switch (s.type) {
            case MARKER:
                paint.setAlpha(MARKER_ALPHA);
                paint.setStrokeWidth(s.width * MARKER_WIDTH_FACTOR);
                paint.setStrokeCap(Paint.Cap.SQUARE);
                break;
            case SPRAY:
                paint.setStyle(Paint.Style.FILL); // nét phun là tập hợp chấm tròn
                break;
            case NEON:
                paint.setMaskFilter(blurFor(s.width));
                break;
            case ERASER:
                paint.setXfermode(clearMode); // đục trong suốt trên lớp vẽ, lộ nền trắng bên dưới
                break;
            default:
                break;
        }
    }

    /** Chỉ tạo BlurMaskFilter mới khi độ dày đổi — không cấp phát mỗi frame. */
    private BlurMaskFilter blurFor(float radius) {
        if (radius != neonBlurRadius) {
            neonBlur = new BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL);
            neonBlurRadius = radius;
        }
        return neonBlur;
    }
}
```

- [ ] **Step 5: Ghi đè DrawingView**

`$J/drawing/DrawingView.java`:

```java
package vn.edu.tdtu.motioncanvas.drawing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;

/**
 * Các mục đã xong được vẽ sẵn lên Bitmap offscreen (`committed`); nét đang vẽ được vẽ đè mỗi frame.
 * Undo/Redo = dựng lại Bitmap từ danh sách mục trong UndoStack.
 */
public class DrawingView extends View {
    private static final float DEFAULT_WIDTH_DP = 8f;

    private final UndoStack<Stroke> history = new UndoStack<>();
    private final StrokeRenderer renderer = new StrokeRenderer();
    private final Random random = new Random();
    private final float density;
    private Bitmap committed;
    private Canvas committedCanvas;
    private Stroke current;
    private float lastX;
    private float lastY;
    private long lastTime;
    private float lastWidthFactor;
    private BrushType brush = BrushType.PEN;
    private int color = Color.BLACK;
    private float strokeWidth;
    private Symmetry symmetry = Symmetry.OFF;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        strokeWidth = DEFAULT_WIDTH_DP * density;
        // BlurMaskFilter (brush Neon) chỉ được tăng tốc phần cứng từ API 28
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void setBrush(BrushType brush) {
        this.brush = brush;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    public void setStrokeWidth(float px) {
        strokeWidth = px;
    }

    public void setSymmetry(Symmetry symmetry) {
        this.symmetry = symmetry;
    }

    public Symmetry getSymmetry() {
        return symmetry;
    }

    public void undo() {
        if (history.undo()) rebuild();
    }

    public void redo() {
        if (history.redo()) rebuild();
    }

    public void clear() {
        if (history.items().isEmpty()) return;
        history.push(Stroke.CLEAR);
        rebuild();
    }

    /** Ảnh để lưu: nền trắng + các mục đã xong. */
    public Bitmap exportBitmap() {
        Bitmap out = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(committed, 0f, 0f, null);
        return out;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (w == 0 || h == 0) return;
        committed = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        committedCanvas = new Canvas(committed);
        rebuild();
    }

    private void rebuild() {
        if (committedCanvas == null) return;
        committed.eraseColor(Color.TRANSPARENT);
        for (Stroke s : history.items()) renderer.commit(committed, committedCanvas, s);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.WHITE);
        if (committed == null) return;
        // Lớp riêng: tẩy (CLEAR) chỉ đục lớp vẽ, không đục nền trắng
        int layer = canvas.saveLayer(0f, 0f, getWidth(), getHeight(), null);
        canvas.drawBitmap(committed, 0f, 0f, null);
        if (current != null) renderer.draw(canvas, current, current.pointCount());
        canvas.restoreToCount(layer);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = x;
                lastY = y;
                lastTime = e.getEventTime();
                if (brush == BrushType.FILL) return true; // đổ màu khi nhấc tay
                current = new Stroke(brush, color, strokeWidth, symmetry,
                        getWidth() / 2f, getHeight() / 2f, random.nextLong());
                lastWidthFactor = 1f;
                current.addPoint(x, y, lastWidthFactor);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (current == null) return true;
                current.addPoint(x, y, widthFactorAt(x, y, e.getEventTime()));
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                if (brush == BrushType.FILL) {
                    commitFill(x, y);
                } else if (current != null) {
                    current.addPoint(x, y, widthFactorAt(x, y, e.getEventTime()));
                    commitCurrent();
                }
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                current = null;
                invalidate();
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    /** Thư pháp: hệ số độ dày theo tốc độ tay (dp/ms), làm mượt; brush khác luôn là 1. */
    private float widthFactorAt(float x, float y, long time) {
        if (current.type != BrushType.CALLIGRAPHY) return 1f;
        long dt = time - lastTime;
        if (dt > 0) {
            float speed = (float) Math.hypot(x - lastX, y - lastY) / density / dt;
            lastWidthFactor = CalligraphyWidth.smooth(lastWidthFactor, CalligraphyWidth.factor(speed));
            lastX = x;
            lastY = y;
            lastTime = time;
        }
        return lastWidthFactor;
    }

    private void commitCurrent() {
        history.push(current);
        renderer.commit(committed, committedCanvas, current);
        current = null;
        invalidate();
    }

    /** Đổ màu không theo đối xứng; vùng đã đúng màu thì không thêm gì vào lịch sử. */
    private void commitFill(float x, float y) {
        Stroke fill = new Stroke(BrushType.FILL, color, 0f, Symmetry.OFF, 0f, 0f, 0L);
        fill.addPoint(x, y, 1f);
        if (renderer.applyFill(committed, fill)) {
            history.push(fill);
            invalidate();
        }
    }
}
```

- [ ] **Step 6: Thêm chuỗi**

Trong `strings.xml`, thêm vào trước `</resources>`:

```xml
    <string name="brush_calligraphy">Thư pháp</string>
    <string name="brush_rainbow">Cầu vồng</string>
    <string name="brush_fill">Đổ màu</string>
    <string name="symmetry_label">Đối xứng: %1$s</string>
    <string name="symmetry_off">Tắt</string>
    <string name="symmetry_mirror">Gương</string>
```

- [ ] **Step 7: Ghi đè layout**

`app/src/main/res/layout/activity_drawing.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/main"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <vn.edu.tdtu.motioncanvas.drawing.DrawingView
        android:id="@+id/drawing_view"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />

    <LinearLayout
        android:id="@+id/controls"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">

        <HorizontalScrollView
            android:layout_width="match_parent"
            android:layout_height="wrap_content">

            <RadioGroup
                android:id="@+id/brush_group"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:checkedButton="@+id/brush_pen"
                android:orientation="horizontal"
                android:paddingHorizontal="8dp">

                <RadioButton
                    android:id="@+id/brush_pen"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/brush_pen" />

                <RadioButton
                    android:id="@+id/brush_marker"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/brush_marker" />

                <RadioButton
                    android:id="@+id/brush_spray"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/brush_spray" />

                <RadioButton
                    android:id="@+id/brush_neon"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/brush_neon" />

                <RadioButton
                    android:id="@+id/brush_calligraphy"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/brush_calligraphy" />

                <RadioButton
                    android:id="@+id/brush_rainbow"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/brush_rainbow" />

                <RadioButton
                    android:id="@+id/brush_fill"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/brush_fill" />

                <RadioButton
                    android:id="@+id/brush_eraser"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/brush_eraser" />
            </RadioGroup>
        </HorizontalScrollView>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:gravity="center_vertical"
            android:orientation="horizontal"
            android:paddingHorizontal="8dp">

            <HorizontalScrollView
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1">

                <LinearLayout
                    android:id="@+id/swatch_row"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal" />
            </HorizontalScrollView>

            <Button
                android:id="@+id/btn_palette"
                style="?attr/materialButtonOutlinedStyle"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/palette" />
        </LinearLayout>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:gravity="center_vertical"
            android:orientation="horizontal"
            android:paddingHorizontal="16dp">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/stroke_width" />

            <SeekBar
                android:id="@+id/seek_width"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:max="60"
                android:progress="8" />
        </LinearLayout>

        <LinearLayout
            android:id="@+id/tool_row"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:paddingHorizontal="8dp">

            <Button
                android:id="@+id/btn_symmetry"
                style="?attr/materialButtonOutlinedStyle"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginEnd="4dp"
                android:layout_weight="1" />
        </LinearLayout>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal">

            <Button
                android:id="@+id/btn_undo"
                style="?attr/borderlessButtonStyle"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="@string/undo" />

            <Button
                android:id="@+id/btn_redo"
                style="?attr/borderlessButtonStyle"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="@string/redo" />

            <Button
                android:id="@+id/btn_clear"
                style="?attr/borderlessButtonStyle"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="@string/clear" />

            <Button
                android:id="@+id/btn_save"
                style="?attr/borderlessButtonStyle"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="@string/save" />
        </LinearLayout>
    </LinearLayout>
</LinearLayout>
```

- [ ] **Step 8: Ghi đè DrawingActivity**

`$J/drawing/DrawingActivity.java`:

```java
package vn.edu.tdtu.motioncanvas.drawing;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vn.edu.tdtu.motioncanvas.R;
import vn.edu.tdtu.motioncanvas.core.SeekBars;
import vn.edu.tdtu.motioncanvas.core.SystemBars;

public class DrawingActivity extends AppCompatActivity {
    private static final int[] SWATCHES = {
            Color.BLACK, 0xFFE53935, 0xFFFB8C00, 0xFFFDD835, 0xFF43A047, 0xFF1E88E5, 0xFF8E24AA};
    private static final float SWATCH_DP = 32f;
    private static final float SWATCH_MARGIN_DP = 4f;
    private static final float PICKER_DP = 280f;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final ActivityResultLauncher<String> storagePermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    saveDrawing();
                } else {
                    toast(getString(R.string.save_permission_denied));
                }
            });
    private DrawingView drawingView;
    private Button symmetryButton;
    private float density;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_drawing);
        SystemBars.applyPadding(findViewById(R.id.main));

        drawingView = findViewById(R.id.drawing_view);
        density = getResources().getDisplayMetrics().density;

        RadioGroup brushes = findViewById(R.id.brush_group);
        brushes.setOnCheckedChangeListener((group, checkedId) -> drawingView.setBrush(brushFor(checkedId)));
        SeekBars.onProgress(findViewById(R.id.seek_width), p -> drawingView.setStrokeWidth(Math.max(1, p) * density));
        buildSwatches(findViewById(R.id.swatch_row));
        symmetryButton = findViewById(R.id.btn_symmetry);
        symmetryButton.setOnClickListener(v -> {
            drawingView.setSymmetry(drawingView.getSymmetry().next());
            showSymmetry();
        });
        showSymmetry();
        findViewById(R.id.btn_palette).setOnClickListener(v -> showColorPicker());
        findViewById(R.id.btn_undo).setOnClickListener(v -> drawingView.undo());
        findViewById(R.id.btn_redo).setOnClickListener(v -> drawingView.redo());
        findViewById(R.id.btn_clear).setOnClickListener(v -> drawingView.clear());
        findViewById(R.id.btn_save).setOnClickListener(v -> onSaveClicked());
    }

    @Override
    protected void onDestroy() {
        io.shutdown();
        super.onDestroy();
    }

    private static BrushType brushFor(int checkedId) {
        if (checkedId == R.id.brush_marker) return BrushType.MARKER;
        if (checkedId == R.id.brush_spray) return BrushType.SPRAY;
        if (checkedId == R.id.brush_neon) return BrushType.NEON;
        if (checkedId == R.id.brush_calligraphy) return BrushType.CALLIGRAPHY;
        if (checkedId == R.id.brush_rainbow) return BrushType.RAINBOW;
        if (checkedId == R.id.brush_fill) return BrushType.FILL;
        if (checkedId == R.id.brush_eraser) return BrushType.ERASER;
        return BrushType.PEN;
    }

    private void showSymmetry() {
        Symmetry s = drawingView.getSymmetry();
        String name;
        if (s == Symmetry.OFF) {
            name = getString(R.string.symmetry_off);
        } else if (s == Symmetry.MIRROR) {
            name = getString(R.string.symmetry_mirror);
        } else {
            name = String.valueOf(s.rotations);
        }
        symmetryButton.setText(getString(R.string.symmetry_label, name));
    }

    private void buildSwatches(LinearLayout row) {
        int size = (int) (SWATCH_DP * density);
        int margin = (int) (SWATCH_MARGIN_DP * density);
        for (int color : SWATCHES) {
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(color);
            dot.setStroke((int) density, Color.LTGRAY);

            View swatch = new View(this);
            swatch.setBackground(dot);
            swatch.setContentDescription(getString(R.string.swatch_desc,
                    String.format(Locale.US, "#%06X", color & 0xFFFFFF)));
            swatch.setOnClickListener(v -> drawingView.setColor(color));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, margin, margin, margin);
            row.addView(swatch, params);
        }
    }

    private void showColorPicker() {
        ColorPickerView picker = new ColorPickerView(this);
        picker.setColor(drawingView.getColor());
        int size = (int) (PICKER_DP * density);
        FrameLayout frame = new FrameLayout(this);
        frame.addView(picker, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.pick_color)
                .setView(frame)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> drawingView.setColor(picker.getColor()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void onSaveClicked() {
        // Android 10+ ghi vào MediaStore không cần quyền; Android 7–9 cần WRITE_EXTERNAL_STORAGE
        boolean needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED;
        if (needsPermission) {
            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        } else {
            saveDrawing();
        }
    }

    private void saveDrawing() {
        Bitmap bitmap = drawingView.exportBitmap();
        io.execute(() -> {
            try {
                ImageSaver.save(getApplicationContext(), bitmap);
                runOnUiThread(() -> toast(getString(R.string.save_ok)));
            } catch (IOException | RuntimeException e) {
                runOnUiThread(() -> toast(getString(R.string.save_failed, e.getMessage())));
            }
        });
    }

    private void toast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}
```

- [ ] **Step 9: Chạy test và build, xác nhận PASS**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`; StrokeTest 3/3 PASS, các test drawing khác vẫn PASS.

- [ ] **Step 10: Kiểm tra trên máy thật**

Home → Drawing, đối chiếu:
- Mọi brush cũ (Bút, Marker, Phun, Neon, Tẩy) trông như trước; chạm 1 cái vẫn ra chấm.
- Thư pháp: vẽ chậm → nét đậm, quét nhanh → nét mảnh, chuyển êm không giật.
- Cầu vồng: nét đổi màu dần theo chiều dài, bắt đầu từ màu đang chọn.
- Đổ màu: vẽ một vòng kín bằng Bút → chọn Đổ màu, chạm bên trong → chỉ trong vòng được tô; chạm nền trắng bên ngoài → tô hết phần ngoài, không lấn vào trong vòng. Chạm lại cùng chỗ với cùng màu → không có gì (Hoàn tác không bị "mất lượt").
- Đối xứng: bấm nút → nhãn đổi Tắt → Gương → 4 → 6 → 8 → Tắt. Chế độ 6: một nét tạo hình bông hoa 12 cánh quanh tâm. Đổi chế độ → các nét cũ giữ nguyên.
- Phun rồi Hoàn tác / Làm lại → các chấm hiện lại đúng y vị trí cũ.
- Hoàn tác một lần Đổ màu → vùng tô biến mất; Làm lại → hiện lại.
- Lưu ảnh → ảnh có đủ vùng tô và hình đối xứng.

- [ ] **Step 11: Commit**

```bash
git add app/src
git commit -m "feat: store strokes as points and add calligraphy, rainbow, fill brushes and symmetry"
```

---

### Task X3: Replay — vẽ lại bức tranh từng nét

**Files:**
- Modify: `$J/drawing/ReplayTimeline.java` (thêm `firstAfterLastClear`)
- Modify (ghi đè): `$J/drawing/DrawingView.java`
- Modify: `$J/drawing/DrawingActivity.java`, `app/src/main/res/layout/activity_drawing.xml`, `app/src/main/res/values/strings.xml`
- Test: `$T/drawing/ReplayTimelineTest.java` (thêm test)

**Interfaces:**
- Consumes: `ReplayTimeline(int[])`, `durationMs()`, `locate(float, int[])` (X1); `Stroke`, `Stroke.CLEAR`, `StrokeRenderer.commit/draw` (X2); layout `@id/controls`, `@id/tool_row` (X2).
- Produces:
  - `static int ReplayTimeline.firstAfterLastClear(List<Stroke> items)` — chỉ số mục đầu tiên sau lần "Xóa hết" cuối cùng (0 nếu chưa xóa lần nào, `items.size()` nếu mục cuối là "Xóa hết").
  - `DrawingView.startReplay()` → `boolean` (false nếu không có gì để phát), `stopReplay()`, `boolean isReplaying()`, `interface DrawingView.ReplayListener { void onReplayChanged(boolean replaying); }`, `setReplayListener(ReplayListener)`.

- [ ] **Step 1: Thêm test**

Trong `$T/drawing/ReplayTimelineTest.java`, thêm import:

```java
import java.util.Arrays;
import java.util.Collections;
```

và thêm các test sau vào trong class:

```java
    private static Stroke pen() {
        return new Stroke(BrushType.PEN, 0xFF000000, 8f, Symmetry.OFF, 0f, 0f, 0L);
    }

    @Test
    public void firstAfterLastClearWithoutClearIsZero() {
        assertEquals(0, ReplayTimeline.firstAfterLastClear(Arrays.asList(pen(), pen())));
    }

    @Test
    public void firstAfterLastClearSkipsEverythingBeforeTheLastClear() {
        assertEquals(4, ReplayTimeline.firstAfterLastClear(
                Arrays.asList(pen(), Stroke.CLEAR, pen(), Stroke.CLEAR, pen())));
    }

    @Test
    public void firstAfterLastClearWhenClearIsLast() {
        assertEquals(2, ReplayTimeline.firstAfterLastClear(Arrays.asList(pen(), Stroke.CLEAR)));
    }

    @Test
    public void firstAfterLastClearOfEmptyHistoryIsZero() {
        assertEquals(0, ReplayTimeline.firstAfterLastClear(Collections.emptyList()));
    }
```

- [ ] **Step 2: Chạy test, xác nhận FAIL**

Run: `./gradlew :app:testDebugUnitTest --tests "vn.edu.tdtu.motioncanvas.drawing.ReplayTimelineTest"`
Expected: FAIL — `cannot find symbol: method firstAfterLastClear`

- [ ] **Step 3: Thêm firstAfterLastClear**

Trong `$J/drawing/ReplayTimeline.java`, thêm `import java.util.List;` dưới dòng `package`, và thêm phương thức sau vào trong class (ngay sau constructor):

```java
    /** Replay chỉ phát bức tranh đang thấy: bỏ mọi mục trước lần "Xóa hết" cuối cùng. */
    static int firstAfterLastClear(List<Stroke> items) {
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i) == Stroke.CLEAR) return i + 1;
        }
        return 0;
    }
```

- [ ] **Step 4: Chạy test, xác nhận PASS**

Run: `./gradlew :app:testDebugUnitTest --tests "vn.edu.tdtu.motioncanvas.drawing.ReplayTimelineTest"`
Expected: PASS (11 tests)

- [ ] **Step 5: Ghi đè DrawingView (thêm Replay)**

`$J/drawing/DrawingView.java`:

```java
package vn.edu.tdtu.motioncanvas.drawing;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Các mục đã xong được vẽ sẵn lên Bitmap offscreen (`committed`); nét đang vẽ được vẽ đè mỗi frame.
 * Undo/Redo = dựng lại Bitmap từ danh sách mục trong UndoStack.
 * Replay = dựng lại từ đầu trên một Bitmap riêng, mỗi frame thêm vài điểm theo ReplayTimeline.
 */
public class DrawingView extends View {
    private static final float DEFAULT_WIDTH_DP = 8f;

    public interface ReplayListener {
        void onReplayChanged(boolean replaying);
    }

    private final UndoStack<Stroke> history = new UndoStack<>();
    private final StrokeRenderer renderer = new StrokeRenderer();
    private final Random random = new Random();
    private final ValueAnimator replayAnimator = ValueAnimator.ofFloat(0f, 1f);
    private final int[] replayCursor = new int[2];
    private final float density;
    private Bitmap committed;
    private Canvas committedCanvas;
    private Stroke current;
    private float lastX;
    private float lastY;
    private long lastTime;
    private float lastWidthFactor;
    private BrushType brush = BrushType.PEN;
    private int color = Color.BLACK;
    private float strokeWidth;
    private Symmetry symmetry = Symmetry.OFF;

    private List<Stroke> replayStrokes = Collections.emptyList();
    private ReplayTimeline timeline;
    private Bitmap replayBitmap;
    private Canvas replayCanvas;
    /** Số nét đã ghi hẳn vào replayBitmap. */
    private int replayDone;
    private boolean replaying;
    /** Cử chỉ bắt đầu bằng cú chạm dừng replay: bỏ qua cho tới khi nhấc tay. */
    private boolean ignoreGesture;
    private ReplayListener replayListener;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        strokeWidth = DEFAULT_WIDTH_DP * density;
        // BlurMaskFilter (brush Neon) chỉ được tăng tốc phần cứng từ API 28
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) setLayerType(LAYER_TYPE_SOFTWARE, null);
        replayAnimator.setInterpolator(new LinearInterpolator());
        replayAnimator.addUpdateListener(a -> onReplayFrame(a.getAnimatedFraction()));
        replayAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                finishReplay();
            }
        });
    }

    public void setBrush(BrushType brush) {
        this.brush = brush;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    public void setStrokeWidth(float px) {
        strokeWidth = px;
    }

    public void setSymmetry(Symmetry symmetry) {
        this.symmetry = symmetry;
    }

    public Symmetry getSymmetry() {
        return symmetry;
    }

    public void setReplayListener(ReplayListener listener) {
        replayListener = listener;
    }

    public boolean isReplaying() {
        return replaying;
    }

    public void undo() {
        if (!replaying && history.undo()) rebuild();
    }

    public void redo() {
        if (!replaying && history.redo()) rebuild();
    }

    public void clear() {
        if (replaying || history.items().isEmpty()) return;
        history.push(Stroke.CLEAR);
        rebuild();
    }

    /** Ảnh để lưu: nền trắng + các mục đã xong. */
    public Bitmap exportBitmap() {
        Bitmap out = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(committed, 0f, 0f, null);
        return out;
    }

    /** @return false nếu không có gì để phát (tranh trống hoặc vừa "Xóa hết"). */
    public boolean startReplay() {
        if (replaying || committed == null) return false;
        List<Stroke> items = history.items();
        int from = ReplayTimeline.firstAfterLastClear(items);
        if (from >= items.size()) return false;
        replayStrokes = new ArrayList<>(items.subList(from, items.size()));
        int[] counts = new int[replayStrokes.size()];
        for (int i = 0; i < counts.length; i++) counts[i] = Math.max(1, replayStrokes.get(i).pointCount());
        timeline = new ReplayTimeline(counts);

        if (replayBitmap == null || replayBitmap.getWidth() != committed.getWidth()
                || replayBitmap.getHeight() != committed.getHeight()) {
            replayBitmap = Bitmap.createBitmap(committed.getWidth(), committed.getHeight(), Bitmap.Config.ARGB_8888);
            replayCanvas = new Canvas(replayBitmap);
        }
        replayBitmap.eraseColor(Color.TRANSPARENT);
        replayDone = 0;
        replayCursor[0] = 0;
        replayCursor[1] = 0;
        current = null;
        replaying = true;
        if (replayListener != null) replayListener.onReplayChanged(true);
        replayAnimator.setDuration(timeline.durationMs());
        replayAnimator.start();
        return true;
    }

    /** Dừng giữa chừng: hiện ngay toàn bộ tranh. */
    public void stopReplay() {
        if (replaying) replayAnimator.cancel(); // cancel() cũng gọi onAnimationEnd → finishReplay
    }

    private void onReplayFrame(float progress) {
        if (!replaying) return;
        timeline.locate(progress, replayCursor);
        int index = replayCursor[0];
        // Các nét trước nét đang vẽ dở đã xong; đổ màu thì hiện ngay khi tới lượt
        while (replayDone < index || (replayDone == index && replayCursor[1] > 0
                && replayStrokes.get(index).type == BrushType.FILL)) {
            renderer.commit(replayBitmap, replayCanvas, replayStrokes.get(replayDone));
            replayDone++;
        }
        invalidate();
    }

    private void finishReplay() {
        if (!replaying) return;
        replaying = false;
        replayStrokes = Collections.emptyList();
        timeline = null;
        if (replayListener != null) replayListener.onReplayChanged(false);
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopReplay();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (w == 0 || h == 0) return;
        stopReplay();
        committed = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        committedCanvas = new Canvas(committed);
        rebuild();
    }

    private void rebuild() {
        if (committedCanvas == null) return;
        committed.eraseColor(Color.TRANSPARENT);
        for (Stroke s : history.items()) renderer.commit(committed, committedCanvas, s);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.WHITE);
        if (committed == null) return;
        // Lớp riêng: tẩy (CLEAR) chỉ đục lớp vẽ, không đục nền trắng
        int layer = canvas.saveLayer(0f, 0f, getWidth(), getHeight(), null);
        if (replaying) {
            canvas.drawBitmap(replayBitmap, 0f, 0f, null);
            int index = replayCursor[0];
            if (replayDone == index) renderer.draw(canvas, replayStrokes.get(index), replayCursor[1]);
        } else {
            canvas.drawBitmap(committed, 0f, 0f, null);
            if (current != null) renderer.draw(canvas, current, current.pointCount());
        }
        canvas.restoreToCount(layer);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) ignoreGesture = replaying;
        if (ignoreGesture) {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) stopReplay();
            return true;
        }
        float x = e.getX();
        float y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = x;
                lastY = y;
                lastTime = e.getEventTime();
                if (brush == BrushType.FILL) return true; // đổ màu khi nhấc tay
                current = new Stroke(brush, color, strokeWidth, symmetry,
                        getWidth() / 2f, getHeight() / 2f, random.nextLong());
                lastWidthFactor = 1f;
                current.addPoint(x, y, lastWidthFactor);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (current == null) return true;
                current.addPoint(x, y, widthFactorAt(x, y, e.getEventTime()));
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                if (brush == BrushType.FILL) {
                    commitFill(x, y);
                } else if (current != null) {
                    current.addPoint(x, y, widthFactorAt(x, y, e.getEventTime()));
                    commitCurrent();
                }
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                current = null;
                invalidate();
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    /** Thư pháp: hệ số độ dày theo tốc độ tay (dp/ms), làm mượt; brush khác luôn là 1. */
    private float widthFactorAt(float x, float y, long time) {
        if (current.type != BrushType.CALLIGRAPHY) return 1f;
        long dt = time - lastTime;
        if (dt > 0) {
            float speed = (float) Math.hypot(x - lastX, y - lastY) / density / dt;
            lastWidthFactor = CalligraphyWidth.smooth(lastWidthFactor, CalligraphyWidth.factor(speed));
            lastX = x;
            lastY = y;
            lastTime = time;
        }
        return lastWidthFactor;
    }

    private void commitCurrent() {
        history.push(current);
        renderer.commit(committed, committedCanvas, current);
        current = null;
        invalidate();
    }

    /** Đổ màu không theo đối xứng; vùng đã đúng màu thì không thêm gì vào lịch sử. */
    private void commitFill(float x, float y) {
        Stroke fill = new Stroke(BrushType.FILL, color, 0f, Symmetry.OFF, 0f, 0f, 0L);
        fill.addPoint(x, y, 1f);
        if (renderer.applyFill(committed, fill)) {
            history.push(fill);
            invalidate();
        }
    }
}
```

- [ ] **Step 6: Chuỗi, nút Phát lại, khóa điều khiển**

`strings.xml`, thêm:

```xml
    <string name="replay">Phát lại</string>
```

`activity_drawing.xml`: trong `LinearLayout` `@id/tool_row`, thêm ngay sau thẻ `Button` `@id/btn_symmetry`:

```xml
            <Button
                android:id="@+id/btn_replay"
                style="?attr/materialButtonOutlinedStyle"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginStart="4dp"
                android:layout_weight="1"
                android:text="@string/replay" />
```

`DrawingActivity.java`:
1. Thêm import `import android.view.ViewGroup;`.
2. Trong `onCreate`, thêm ngay sau dòng `findViewById(R.id.btn_save).setOnClickListener(v -> onSaveClicked());`:

```java
        View controls = findViewById(R.id.controls);
        findViewById(R.id.btn_replay).setOnClickListener(v -> drawingView.startReplay());
        // Trong lúc phát lại khóa mọi điều khiển; chạm vào tranh để dừng
        drawingView.setReplayListener(replaying -> setEnabledTree(controls, !replaying));
```

3. Thêm phương thức vào class (ngay sau `onDestroy`):

```java
    private static void setEnabledTree(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) setEnabledTree(group.getChildAt(i), enabled);
        }
    }
```

- [ ] **Step 7: Build**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`, mọi test PASS.

- [ ] **Step 8: Kiểm tra trên máy thật**

Home → Drawing, đối chiếu:
- Tranh trống → bấm "Phát lại" → không có gì xảy ra.
- Vẽ 5–6 nét (trộn Bút, Phun, Cầu vồng, Thư pháp, một nét đối xứng 6, một lần Đổ màu) → "Phát lại": màn về trắng, các nét hiện lại theo đúng thứ tự, từng nét được "vẽ" dần; đổ màu hiện ra đúng lúc tới lượt; hết 2–8 giây thì tranh đầy đủ.
- Trong lúc phát: các nút xám lại, không bấm được; vuốt lên tranh → dừng ngay, tranh đầy đủ hiện ra, cú vuốt đó **không** để lại nét.
- Sau khi phát xong: Hoàn tác / vẽ tiếp bình thường.
- Vẽ vài nét → "Xóa hết" → "Phát lại" → không có gì. Vẽ thêm 2 nét → "Phát lại" → chỉ phát 2 nét mới.
- Đang phát thì bấm nút Back → thoát màn, không crash.

- [ ] **Step 9: Commit**

```bash
git add app/src
git commit -m "feat: add stroke-by-stroke replay to drawing screen"
```

---

# CHART (gói ②)

### Task X4: Line chart — đường cong Bézier, vùng tô gradient, điểm nhấp nháy

**Files:**
- Modify: `$J/chart/ChartMath.java` (thêm `catmullRomControls`, `cubicAt`)
- Modify (ghi đè): `$J/chart/LineChartView.java`, `$J/chart/ChartActivity.java`, `app/src/main/res/layout/activity_chart.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `$T/chart/ChartMathTest.java` (thêm test)

**Interfaces:**
- Consumes: `ChartScale`, `ChartMath.nearestIndex`, `ChartAxes` (B1, B2).
- Produces:
  - `ChartMath.catmullRomControls(float[] xs, float[] ys, int n, float[] out)` — ghi `c1x, c1y, c2x, c2y` cho mỗi đoạn `i → i+1` vào `out[4i..4i+3]`; `n < 2` → không ghi gì.
  - `ChartMath.cubicAt(float p0, float c1, float c2, float p1, float t)`.
  - `LineChartView.setSmooth(boolean)`; giữ nguyên `SAMPLE_INTERVAL_MS`, `setDataSource(Supplier<Float>)` (B6 dùng).

- [ ] **Step 1: Thêm test**

Trong `$T/chart/ChartMathTest.java`, thêm các test sau vào trong class:

```java
    @Test
    public void catmullRomControlsOfTwoPointsLieOnTheSegment() {
        float[] out = new float[4];
        ChartMath.catmullRomControls(new float[]{0f, 6f}, new float[]{0f, 6f}, 2, out);
        assertArrayEquals(new float[]{1f, 1f, 5f, 5f}, out, 1e-4f);
    }

    @Test
    public void catmullRomCurveThroughCollinearPointsStaysOnTheLine() {
        float[] xs = {0f, 1f, 2f, 3f};
        float[] ys = {0f, 2f, 4f, 6f};
        float[] out = new float[12];

        ChartMath.catmullRomControls(xs, ys, 4, out);

        // Đoạn giữa (1,2) → (2,4): tại t = 0.5 phải đúng trung điểm (1.5, 3)
        assertEquals(1.5f, ChartMath.cubicAt(xs[1], out[4], out[6], xs[2], 0.5f), 1e-4f);
        assertEquals(3f, ChartMath.cubicAt(ys[1], out[5], out[7], ys[2], 0.5f), 1e-4f);
    }

    @Test
    public void cubicPassesThroughItsEndPoints() {
        assertEquals(2f, ChartMath.cubicAt(2f, 9f, -4f, 7f, 0f), 1e-6f);
        assertEquals(7f, ChartMath.cubicAt(2f, 9f, -4f, 7f, 1f), 1e-6f);
    }

    @Test
    public void catmullRomWithFewerThanTwoPointsWritesNothing() {
        float[] out = {9f, 9f, 9f, 9f};
        ChartMath.catmullRomControls(new float[]{1f}, new float[]{1f}, 1, out);
        ChartMath.catmullRomControls(new float[0], new float[0], 0, out);
        assertArrayEquals(new float[]{9f, 9f, 9f, 9f}, out, 0f);
    }
```

- [ ] **Step 2: Chạy test, xác nhận FAIL**

Run: `./gradlew :app:testDebugUnitTest --tests "vn.edu.tdtu.motioncanvas.chart.ChartMathTest"`
Expected: FAIL — `cannot find symbol: method catmullRomControls`

- [ ] **Step 3: Thêm hàm vào ChartMath**

Trong `$J/chart/ChartMath.java`, thêm vào cuối class (trước dấu `}` cuối):

```java
    /**
     * Điểm điều khiển Bézier bậc ba để vẽ đường Catmull-Rom đi qua đúng n điểm (xs[i], ys[i]).
     * Đoạn i → i+1 ghi vào out[4i..4i+3] = c1x, c1y, c2x, c2y. Hai đầu dùng chính điểm biên làm điểm kề.
     */
    public static void catmullRomControls(float[] xs, float[] ys, int n, float[] out) {
        for (int i = 0; i < n - 1; i++) {
            int prev = Math.max(0, i - 1);
            int next = Math.min(n - 1, i + 2);
            out[4 * i] = xs[i] + (xs[i + 1] - xs[prev]) / 6f;
            out[4 * i + 1] = ys[i] + (ys[i + 1] - ys[prev]) / 6f;
            out[4 * i + 2] = xs[i + 1] - (xs[next] - xs[i]) / 6f;
            out[4 * i + 3] = ys[i + 1] - (ys[next] - ys[i]) / 6f;
        }
    }

    /** Giá trị đường Bézier bậc ba tại t ∈ [0, 1] trên một trục. */
    public static float cubicAt(float p0, float c1, float c2, float p1, float t) {
        float u = 1f - t;
        return u * u * u * p0 + 3f * u * u * t * c1 + 3f * u * t * t * c2 + t * t * t * p1;
    }
```

- [ ] **Step 4: Chạy test, xác nhận PASS**

Run: `./gradlew :app:testDebugUnitTest --tests "vn.edu.tdtu.motioncanvas.chart.ChartMathTest"`
Expected: PASS (11 tests)

- [ ] **Step 5: Ghi đè LineChartView**

`$J/chart/LineChartView.java`:

```java
package vn.edu.tdtu.motioncanvas.chart;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Line chart nhận điểm mới định kỳ và trượt mượt sang trái; chạm để xem giá trị.
 * Tùy chọn đường cong Catmull-Rom; điểm mới nhất có vòng sáng lan ra liên tục.
 */
public class LineChartView extends View {
    public static final long SAMPLE_INTERVAL_MS = 400;
    private static final int VISIBLE_POINTS = 30;
    private static final int MAX_POINTS = VISIBLE_POINTS + 1;
    private static final int GRID_LINES = 5;
    private static final float WALK_STEP = 20f;
    private static final float WALK_MIN = 0f;
    private static final float WALK_MAX = 100f;
    private static final float Y_PADDING_RATIO = 0.1f;
    private static final int LINE_COLOR = 0xFF26A69A;
    private static final int FILL_TOP_COLOR = 0x8026A69A;
    private static final int FILL_BOTTOM_COLOR = 0x0026A69A;
    private static final float LINE_DP = 2.5f;
    private static final float MARKER_DP = 5f;
    private static final float HEAD_DP = 4f;
    private static final float PULSE_DP = 14f;
    private static final long PULSE_MS = 1200;
    private static final float LABEL_GAP_DP = 6f;

    private final List<Float> points = new ArrayList<>();
    private final float[] xs = new float[MAX_POINTS];
    private final float[] ys = new float[MAX_POINTS];
    private final float[] controls = new float[4 * MAX_POINTS];
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint;
    private final Paint axisLabelPaint;
    private final Paint valuePaint;
    private final Path linePath = new Path();
    private final Path fillPath = new Path();
    private final RectF plot = new RectF();
    private final ValueAnimator scrollAnimator = ValueAnimator.ofFloat(1f, 0f);
    private final ValueAnimator pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
    private final Random random = new Random();
    private final float density;
    private Supplier<Float> dataSource = this::randomWalk;
    private float lastValue = (WALK_MIN + WALK_MAX) / 2f;
    /** 1 → 0 trong mỗi chu kỳ lấy mẫu: phần bước mà điểm mới nhất còn phải trượt vào. */
    private float scrollOffset;
    private float pulse;
    private boolean smooth;
    private int selectedIndex = -1;

    private final Runnable sampler = new Runnable() {
        @Override
        public void run() {
            addPoint(dataSource.get());
            postDelayed(this, SAMPLE_INTERVAL_MS);
        }
    };

    public LineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        linePaint.setColor(LINE_COLOR);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(LINE_DP * density);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        markerPaint.setColor(LINE_COLOR);
        pulsePaint.setColor(LINE_COLOR);
        pulsePaint.setStyle(Paint.Style.STROKE);
        pulsePaint.setStrokeWidth(2f * density);
        gridPaint = ChartAxes.gridPaint(density);
        axisLabelPaint = ChartAxes.labelPaint(context, Paint.Align.RIGHT);
        valuePaint = ChartAxes.labelPaint(context, Paint.Align.CENTER);
        scrollAnimator.setDuration(SAMPLE_INTERVAL_MS);
        scrollAnimator.setInterpolator(new LinearInterpolator());
        scrollAnimator.addUpdateListener(a -> {
            scrollOffset = (float) a.getAnimatedValue();
            invalidate();
        });
        pulseAnimator.setDuration(PULSE_MS);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new DecelerateInterpolator());
        pulseAnimator.addUpdateListener(a -> {
            pulse = (float) a.getAnimatedValue();
            invalidate();
        });
    }

    /** Thay nguồn dữ liệu (mặc định: random walk 0..100). Gọi trên main thread. */
    public void setDataSource(Supplier<Float> source) {
        dataSource = source;
    }

    /** true → đường cong Catmull-Rom qua đúng mọi điểm; false → đường gấp khúc. */
    public void setSmooth(boolean smooth) {
        this.smooth = smooth;
        invalidate();
    }

    private float randomWalk() {
        float next = lastValue + (random.nextFloat() - 0.5f) * WALK_STEP;
        lastValue = Math.max(WALK_MIN, Math.min(WALK_MAX, next));
        return lastValue;
    }

    private void addPoint(float value) {
        points.add(value);
        if (points.size() > MAX_POINTS) {
            points.remove(0);
            selectedIndex--; // giữ tooltip bám đúng điểm khi dữ liệu trượt sang trái
        }
        scrollAnimator.start();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        removeCallbacks(sampler);
        post(sampler);
        pulseAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(sampler);
        scrollAnimator.cancel();
        pulseAnimator.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        ChartAxes.plotArea(plot, w, h, density);
        // Gradient theo trục dọc của vùng vẽ: đậm ở trên, trong suốt ở đáy
        fillPaint.setShader(new LinearGradient(0f, plot.top, 0f, plot.bottom,
                FILL_TOP_COLOR, FILL_BOTTOM_COLOR, Shader.TileMode.CLAMP));
    }

    private float stepWidth() {
        return plot.width() / (VISIBLE_POINTS - 1);
    }

    /** Điểm mới nhất nằm ở mép phải (+ phần đang trượt vào), các điểm cũ lùi dần sang trái. */
    private float xAt(int index, float step) {
        return plot.right - (points.size() - 1 - index - scrollOffset) * step;
    }

    private ChartScale yScale() {
        float lo = Float.MAX_VALUE;
        float hi = -Float.MAX_VALUE;
        for (float v : points) {
            lo = Math.min(lo, v);
            hi = Math.max(hi, v);
        }
        float pad = Math.max((hi - lo) * Y_PADDING_RATIO, 1f);
        return new ChartScale(lo - pad, hi + pad, plot.bottom, plot.top);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int n = points.size();
        if (n == 0 || plot.isEmpty()) return;
        ChartScale scale = yScale();
        ChartAxes.drawHorizontalGrid(canvas, plot, scale, GRID_LINES, LABEL_GAP_DP * density,
                gridPaint, axisLabelPaint);

        float step = stepWidth();
        for (int i = 0; i < n; i++) {
            xs[i] = xAt(i, step);
            ys[i] = scale.toPixel(points.get(i));
        }
        buildLinePath(n);
        fillPath.set(linePath);
        fillPath.lineTo(xs[n - 1], plot.bottom);
        fillPath.lineTo(xs[0], plot.bottom);
        fillPath.close();

        canvas.save();
        canvas.clipRect(plot);
        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);
        drawPulse(canvas, xs[n - 1], ys[n - 1]);
        canvas.restore();

        if (selectedIndex >= 0 && selectedIndex < n) {
            float px = xs[selectedIndex];
            float py = ys[selectedIndex];
            canvas.drawCircle(px, py, MARKER_DP * density, markerPaint);
            canvas.drawText(String.format(Locale.US, "%.1f", points.get(selectedIndex)), px,
                    py - 2 * MARKER_DP * density, valuePaint);
        }
    }

    private void buildLinePath(int n) {
        linePath.reset();
        linePath.moveTo(xs[0], ys[0]);
        if (smooth && n >= 2) {
            ChartMath.catmullRomControls(xs, ys, n, controls);
            for (int i = 0; i < n - 1; i++) {
                linePath.cubicTo(controls[4 * i], controls[4 * i + 1],
                        controls[4 * i + 2], controls[4 * i + 3], xs[i + 1], ys[i + 1]);
            }
        } else {
            for (int i = 1; i < n; i++) linePath.lineTo(xs[i], ys[i]);
        }
    }

    /** Chấm đặc ở điểm mới nhất + vòng tròn lan rộng và mờ dần (kiểu "đang trực tiếp"). */
    private void drawPulse(Canvas canvas, float x, float y) {
        canvas.drawCircle(x, y, HEAD_DP * density, markerPaint);
        pulsePaint.setAlpha((int) (255 * (1f - pulse)));
        canvas.drawCircle(x, y, (HEAD_DP + pulse * PULSE_DP) * density, pulsePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // Không để ScrollView cha cướp cử chỉ kéo ngang trên chart
                getParent().requestDisallowInterceptTouchEvent(true);
                selectAt(e.getX());
                return true;
            case MotionEvent.ACTION_MOVE:
                selectAt(e.getX());
                return true;
            case MotionEvent.ACTION_UP:
                performClick();
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void selectAt(float x) {
        float step = stepWidth();
        selectedIndex = ChartMath.nearestIndex(x, xAt(0, step), step, points.size());
        invalidate();
    }
}
```

- [ ] **Step 6: Công tắc "Đường cong"**

`strings.xml`, thêm:

```xml
    <string name="chart_smooth">Đường cong</string>
```

`activity_chart.xml`: thay thẻ `TextView` tiêu đề line chart (thẻ có `android:text="@string/chart_line"`) bằng:

```xml
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:gravity="center_vertical"
            android:orientation="horizontal">

            <TextView
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="@string/chart_line"
                android:textAppearance="?attr/textAppearanceTitleMedium" />

            <com.google.android.material.materialswitch.MaterialSwitch
                android:id="@+id/switch_smooth"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/chart_smooth" />
        </LinearLayout>
```

`ChartActivity.java`: trong `onCreate`, thêm ngay sau dòng `donutChart = findViewById(R.id.donut_chart);`:

```java
        LineChartView lineChart = findViewById(R.id.line_chart);
        MaterialSwitch smoothSwitch = findViewById(R.id.switch_smooth);
        smoothSwitch.setOnCheckedChangeListener((button, checked) -> lineChart.setSmooth(checked));
```

và thêm import `import com.google.android.material.materialswitch.MaterialSwitch;`.

- [ ] **Step 7: Build**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`, mọi test PASS.

- [ ] **Step 8: Kiểm tra trên máy thật**

Home → Dynamic Charts, đối chiếu:
- Vùng dưới đường là gradient: xanh ngọc ở trên, nhạt dần về trong suốt ở đáy.
- Điểm mới nhất có chấm đặc + vòng tròn lan ra và mờ dần, lặp liên tục, đi theo đường khi trượt.
- Bật "Đường cong" ngay khi vừa mở màn (mới 0–1 điểm) → không crash; sau đó đường thành cong mượt, vẫn đi qua các điểm (chạm để xem chấm tooltip nằm đúng trên đường); tắt → về gấp khúc.
- Tooltip, cuộn màn, bar và donut vẫn như trước.

- [ ] **Step 9: Commit**

```bash
git add app/src
git commit -m "feat: add smooth curve, gradient fill and live pulse to line chart"
```

---

### Task X5: Donut — phần được chọn trượt ra bằng lò xo

**Files:**
- Modify (ghi đè): `$J/chart/DonutChartView.java`
- Modify: `$J/chart/ChartMath.java` (xóa `sliceAt`), `$T/chart/ChartMathTest.java` (xóa 2 test của `sliceAt`)

**Interfaces:**
- Consumes: `ChartMath.sweepAngles`, `ChartMath.angleFromTop` (B1); `ChartAxes.labelPaint` (B2); `androidx.dynamicanimation` `SpringAnimation`, `SpringForce`, `FloatPropertyCompat`.
- Produces: giữ nguyên `DonutChartView.sliceCount()`, `setData(float[])` (ChartActivity dùng).

- [ ] **Step 1: Ghi đè DonutChartView**

`$J/chart/DonutChartView.java`:

```java
package vn.edu.tdtu.motioncanvas.chart;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import java.util.Locale;

import vn.edu.tdtu.motioncanvas.R;

/**
 * Donut chart: các phần quét từ 0° đến 360°; chạm vào một phần → phần đó trượt ra ngoài bằng lò xo
 * và giữa vòng hiện phần trăm. Chú thích bên phải.
 */
public class DonutChartView extends View {
    private static final String[] LABELS = {"Android", "iOS", "Web", "Desktop", "Khác"};
    private static final int[] COLORS = {0xFF66BB6A, 0xFF42A5F5, 0xFFFFA726, 0xFFAB47BC, 0xFF78909C};
    private static final long SWEEP_MS = 900;
    private static final float START_ANGLE = -90f; // 12 giờ, theo hệ góc của Canvas
    private static final float RING_DP = 28f;
    private static final float EXPLODE_DP = 12f;
    private static final float MARGIN_DP = 8f;
    private static final float LEGEND_GAP_DP = 24f;
    private static final float LEGEND_DOT_DP = 6f;
    private static final float LEGEND_LINE_DP = 22f;
    private static final float CENTER_TEXT_SP = 16f;
    private static final float MIN_VISIBLE_OFFSET = 0.002f; // đơn vị: phần của EXPLODE_DP
    private static final int TEXT_COLOR = 0xFF424242;

    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint legendPaint;
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();
    private final ValueAnimator sweepAnimator = ValueAnimator.ofFloat(0f, 1f);
    /** Mức trượt ra của từng phần: 0 = đúng chỗ, 1 = ra hết EXPLODE_DP; mỗi phần một lò xo. */
    private final float[] offsets = new float[LABELS.length];
    private final SpringAnimation[] springs = new SpringAnimation[LABELS.length];
    private final float density;
    private final float ringWidth;
    private final float explodeDistance;
    private float[] sweeps = new float[LABELS.length];
    private float progress;
    private int selectedIndex = -1;
    private float centerX;
    private float centerY;
    private float radius;

    public DonutChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        ringWidth = RING_DP * density;
        explodeDistance = EXPLODE_DP * density;
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(ringWidth);
        centerPaint.setColor(TEXT_COLOR);
        centerPaint.setTextAlign(Paint.Align.CENTER);
        centerPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, CENTER_TEXT_SP,
                getResources().getDisplayMetrics()));
        legendPaint = ChartAxes.labelPaint(context, Paint.Align.LEFT);
        legendPaint.setColor(TEXT_COLOR);
        sweepAnimator.setDuration(SWEEP_MS);
        sweepAnimator.setInterpolator(new DecelerateInterpolator());
        sweepAnimator.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidate();
        });
        for (int i = 0; i < LABELS.length; i++) springs[i] = createSpring(i);
    }

    private SpringAnimation createSpring(int index) {
        FloatPropertyCompat<DonutChartView> property = new FloatPropertyCompat<DonutChartView>("slice" + index) {
            @Override
            public float getValue(DonutChartView view) {
                return view.offsets[index];
            }

            @Override
            public void setValue(DonutChartView view, float value) {
                view.offsets[index] = value;
                view.invalidate();
            }
        };
        SpringAnimation spring = new SpringAnimation(this, property, 0f);
        spring.getSpring()
                .setStiffness(SpringForce.STIFFNESS_MEDIUM)
                .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
        spring.setMinimumVisibleChange(MIN_VISIBLE_OFFSET);
        return spring;
    }

    public static int sliceCount() {
        return LABELS.length;
    }

    public void setData(float[] values) {
        if (values.length != LABELS.length) {
            throw new IllegalArgumentException("Cần đúng " + LABELS.length + " giá trị");
        }
        sweeps = ChartMath.sweepAngles(values);
        select(-1); // mọi phần trượt về chỗ cũ trong lúc quét lại
        sweepAnimator.start();
    }

    /** Phần `index` trượt ra (lò xo), mọi phần khác trượt về; -1 = bỏ chọn. */
    private void select(int index) {
        selectedIndex = index;
        for (int i = 0; i < springs.length; i++) springs[i].animateToFinalPosition(i == index ? 1f : 0f);
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        sweepAnimator.cancel();
        for (SpringAnimation spring : springs) spring.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // Donut chiếm hình vuông bên trái (chừa chỗ cho phần trượt ra), chú thích ở phần còn lại
        float size = Math.min(w / 2f, h);
        radius = size / 2f - ringWidth / 2f - explodeDistance - MARGIN_DP * density;
        centerX = size / 2f;
        centerY = h / 2f;
        arcBounds.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
    }

    private boolean hasData() {
        for (float s : sweeps) {
            if (s > 0f) return true;
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (radius <= 0f) return;
        float start = 0f; // độ, tính từ 12 giờ theo chiều kim đồng hồ
        for (int i = 0; i < sweeps.length; i++) {
            float sweep = sweeps[i] * progress;
            // Trượt theo hướng giữa của phần: dịch cả cung đi một đoạn dọc theo bán kính
            double mid = Math.toRadians(START_ANGLE + start + sweep / 2f);
            float shift = offsets[i] * explodeDistance;
            canvas.save();
            canvas.translate((float) Math.cos(mid) * shift, (float) Math.sin(mid) * shift);
            arcPaint.setColor(COLORS[i]);
            canvas.drawArc(arcBounds, START_ANGLE + start, sweep, false, arcPaint);
            canvas.restore();
            start += sweep;
        }

        String center;
        if (!hasData()) {
            center = getContext().getString(R.string.chart_no_data);
        } else if (selectedIndex >= 0) {
            center = String.format(Locale.US, "%.0f%%", sweeps[selectedIndex] / 3.6f);
        } else {
            center = getContext().getString(R.string.chart_tap_hint);
        }
        canvas.drawText(center, centerX, centerY + centerPaint.getTextSize() / 3f, centerPaint);
        drawLegend(canvas);
    }

    private void drawLegend(Canvas canvas) {
        float line = LEGEND_LINE_DP * density;
        float dot = LEGEND_DOT_DP * density;
        float x = centerX * 2f + LEGEND_GAP_DP * density;
        float y = centerY - (LABELS.length - 1) * line / 2f;
        for (int i = 0; i < LABELS.length; i++) {
            dotPaint.setColor(COLORS[i]);
            canvas.drawCircle(x, y, dot, dotPaint);
            canvas.drawText(LABELS[i], x + 2.5f * dot, y + legendPaint.getTextSize() / 3f, legendPaint);
            y += line;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            select(sliceAtTouch(e.getX(), e.getY()));
            return true;
        }
        if (e.getActionMasked() == MotionEvent.ACTION_UP) {
            performClick();
            return true;
        }
        return super.onTouchEvent(e);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    /** Phần chứa điểm chạm, tính theo vị trí ĐÃ trượt của từng phần; -1 nếu chạm ngoài vòng. */
    private int sliceAtTouch(float x, float y) {
        float start = 0f;
        for (int i = 0; i < sweeps.length; i++) {
            float sweep = sweeps[i];
            if (sweep > 0f) {
                double mid = Math.toRadians(START_ANGLE + start + sweep / 2f);
                float shift = offsets[i] * explodeDistance;
                float dx = x - centerX - (float) Math.cos(mid) * shift;
                float dy = y - centerY - (float) Math.sin(mid) * shift;
                boolean onRing = Math.abs((float) Math.hypot(dx, dy) - radius) <= ringWidth;
                float angle = ChartMath.angleFromTop(dx, dy);
                if (onRing && angle >= start && angle < start + sweep) return i;
            }
            start += sweep;
        }
        return -1;
    }
}
```

- [ ] **Step 2: Xóa `sliceAt` không còn dùng**

Donut giờ tự dò phần theo vị trí đã trượt, nên `ChartMath.sliceAt` không còn chỗ dùng.

Run: `git grep -n "sliceAt(" -- app/src`
Expected: chỉ còn trong `ChartMath.java` (định nghĩa) và `ChartMathTest.java` (2 test `sliceAtFindsContainingSlice`, `sliceAtWithEmptyDataReturnsMinusOne`).

Xóa phương thức `sliceAt` (cả comment Javadoc phía trên) trong `$J/chart/ChartMath.java`, và xóa 2 test đó trong `$T/chart/ChartMathTest.java`.

- [ ] **Step 3: Build**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`; ChartMathTest còn 9 test, tất cả PASS.

- [ ] **Step 4: Kiểm tra trên máy thật**

Home → Dynamic Charts, cuộn xuống donut, đối chiếu:
- Chạm một phần → phần đó trượt ra ngoài và nảy nhẹ; giữa vòng hiện %.
- Chạm phần khác → phần cũ trượt về, phần mới trượt ra, cả hai mượt cùng lúc.
- Chạm liên tục nhanh qua lại giữa 2 phần → không giật, không kẹt ở giữa.
- Chạm đúng vào phần **đang trượt ra** (mép ngoài của nó) → vẫn chọn đúng phần đó.
- Chạm lỗ giữa → mọi phần về chỗ, hiện "Chạm vào vòng".
- "Đổi dữ liệu" khi đang có phần trượt ra → phần đó trượt về, donut quét lại.
- Phần trượt ra không bị cắt mép view; chú thích bên phải không bị đè.

- [ ] **Step 5: Commit**

```bash
git add app/src
git commit -m "feat: spring selected donut slice outward"
```

---

# HOME VÀ GIAO DIỆN CHUNG (gói ③)

### Task X6: Theme sáng, thanh tiêu đề + nút quay lại, icon app

**Files:**
- Modify (ghi đè): `app/src/main/res/values/themes.xml`, `app/src/main/res/values/colors.xml`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/drawable/ic_launcher_background.xml`, `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Delete: `app/src/main/res/values-night/themes.xml`
- Có thể sửa: `$J/core/SystemBars.java` (chỉ khi Step 5 thấy lỗi đệm hai lần)

**Interfaces:**
- Consumes: không có.
- Produces (X7 dùng): màu `@color/brand_primary`, `@color/brand_secondary`, `@color/brand_background`, `@color/preview_background`; style `@style/Theme.MotionCanvas.NoActionBar`.

- [ ] **Step 1: Kiểm tra bảng màu bằng ui-ux-pro-max**

Gọi skill `ui-ux-pro-max` với yêu cầu: "kiểm tra bảng màu cho app Android học tập: primary #3949AB, secondary #26A69A, background #F5F6FA; chữ trắng trên primary". Giữ bảng màu này nếu chữ trắng trên `#3949AB` đạt tương phản ≥ 4.5:1 (tính tay: ≈ 7.7:1). Nếu skill đề xuất đổi màu, ghi `Ruling:` vào ledger kèm lý do rồi dùng màu mới ở Step 2 và Step 4.

- [ ] **Step 2: Màu và theme**

`app/src/main/res/values/colors.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>
    <color name="brand_primary">#FF3949AB</color>
    <color name="brand_secondary">#FF26A69A</color>
    <color name="brand_background">#FFF5F6FA</color>
    <color name="preview_background">#FFE8EAF6</color>
</resources>
```

`app/src/main/res/values/themes.xml`:

```xml
<resources>
    <!-- Chỉ có giao diện sáng; theme có ActionBar để mọi màn demo có tiêu đề + nút quay lại -->
    <style name="Base.Theme.MotionCanvas" parent="Theme.Material3.Light">
        <item name="colorPrimary">@color/brand_primary</item>
        <item name="colorOnPrimary">@color/white</item>
        <item name="colorSecondary">@color/brand_secondary</item>
        <item name="colorOnSecondary">@color/white</item>
        <item name="android:colorBackground">@color/brand_background</item>
        <item name="actionBarStyle">@style/Widget.MotionCanvas.ActionBar</item>
    </style>

    <style name="Theme.MotionCanvas" parent="Base.Theme.MotionCanvas" />

    <!-- Home có header riêng nên ẩn ActionBar -->
    <style name="Theme.MotionCanvas.NoActionBar">
        <item name="windowActionBar">false</item>
        <item name="windowNoTitle">true</item>
    </style>

    <!-- homeAsUp: hiện mũi tên quay lại; AppCompat tự điều hướng theo parentActivityName -->
    <style name="Widget.MotionCanvas.ActionBar" parent="Widget.Material3.ActionBar.Solid">
        <item name="displayOptions">showTitle|homeAsUp</item>
    </style>
</resources>
```

Run: `git rm app/src/main/res/values-night/themes.xml`

- [ ] **Step 3: Manifest — Home không ActionBar, màn demo có nút quay lại**

`app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission
        android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="28" />

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.MotionCanvas">
        <!-- singleTop: nút quay lại trên ActionBar trở về Home cũ thay vì tạo Home mới -->
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTop"
            android:screenOrientation="portrait"
            android:theme="@style/Theme.MotionCanvas.NoActionBar"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        <activity
            android:name=".gesture.GestureActivity"
            android:exported="false"
            android:label="@string/demo_gesture"
            android:parentActivityName=".MainActivity"
            android:screenOrientation="portrait" />
        <activity
            android:name=".physics.PhysicsActivity"
            android:exported="false"
            android:label="@string/demo_physics"
            android:parentActivityName=".MainActivity"
            android:screenOrientation="portrait" />
        <activity
            android:name=".chart.ChartActivity"
            android:exported="false"
            android:label="@string/demo_chart"
            android:parentActivityName=".MainActivity"
            android:screenOrientation="portrait" />
        <activity
            android:name=".particle.ParticleActivity"
            android:exported="false"
            android:label="@string/demo_particle"
            android:parentActivityName=".MainActivity"
            android:screenOrientation="portrait" />
        <activity
            android:name=".drawing.DrawingActivity"
            android:exported="false"
            android:label="@string/demo_drawing"
            android:parentActivityName=".MainActivity"
            android:screenOrientation="portrait" />
    </application>

</manifest>
```

Trước khi ghi đè, chạy `git diff HEAD -- app/src/main/AndroidManifest.xml` và `git log --oneline -3 -- app/src/main/AndroidManifest.xml`: nếu A đã thêm/sửa gì trong Manifest (ví dụ Activity mới), giữ lại phần của A trong bản ghi đè.

- [ ] **Step 4: Icon app**

`app/src/main/res/drawable/ic_launcher_background.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#3949AB"
        android:pathData="M0,0h108v108h-108z" />
</vector>
```

`app/src/main/res/drawable/ic_launcher_foreground.xml` (đường cong + chấm tròn, nằm trong vùng an toàn 66×66 ở giữa):

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#00000000"
        android:pathData="M32,70 C40,42 56,40 62,56 C66,66 70,64 74,52"
        android:strokeColor="#FFFFFF"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"
        android:strokeWidth="6" />
    <path
        android:fillColor="#26A69A"
        android:pathData="M76,40m-7,0a7,7 0,1 0,14 0a7,7 0,1 0,-14 0" />
</vector>
```

(`mipmap-anydpi-v26/ic_launcher.xml` và `ic_launcher_round.xml` đã trỏ `monochrome` về `ic_launcher_foreground` → themed icon Android 13+ tự có, không cần sửa.)

- [ ] **Step 5: Build và kiểm tra thanh tiêu đề trên máy thật**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

Gỡ app cũ khỏi máy rồi cài lại (để launcher cập nhật icon). Đối chiếu:
- Icon trên launcher: nền indigo, đường cong trắng, chấm xanh ngọc.
- Home: không có thanh tiêu đề, nền xám rất nhạt.
- Mở từng màn demo (cả màn của A): có thanh tiêu đề đúng tên màn + mũi tên quay lại; bấm mũi tên → về Home (Home không bị tạo lại: thẻ/cuộn giữ nguyên).
- Nút Back của hệ thống vẫn về Home.
- Không có khoảng trống thừa giữa thanh tiêu đề và nội dung; nội dung đáy không bị thanh điều hướng che.

**Nếu thấy khoảng trống thừa đúng bằng chiều cao status bar ngay dưới thanh tiêu đề** (đệm hai lần), ghi đè `$J/core/SystemBars.java`:

```java
package vn.edu.tdtu.motioncanvas.core;

import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public final class SystemBars {
    private SystemBars() {
    }

    /** Đệm view gốc bằng kích thước status bar / navigation bar (app chạy edge-to-edge). */
    public static void applyPadding(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Màn có ActionBar: ActionBar đã nằm dưới status bar, nội dung không cần đệm trên nữa
            int top = hasActionBar(v) ? 0 : bars.top;
            v.setPadding(bars.left, top, bars.right, bars.bottom);
            return insets;
        });
    }

    private static boolean hasActionBar(View view) {
        return view.getContext() instanceof AppCompatActivity
                && ((AppCompatActivity) view.getContext()).getSupportActionBar() != null;
    }
}
```

rồi build và kiểm tra lại. Ghi `Ruling:` vào ledger. **Nếu thanh tiêu đề bị status bar che** (chữ tiêu đề nằm dưới giờ/pin): dừng lại, chụp màn hình, hỏi lại trước khi sửa.

- [ ] **Step 6: Commit**

```bash
git add -A app/src
git commit -m "feat: add light brand theme, action bar with up navigation and app icon"
```

---

### Task X7: Home — lưới thẻ có hình động mini, chuyển màn phóng to từ thẻ

**Files:**
- Create: `$J/home/Preview.java`, `$J/home/PreviewView.java`, `$J/home/DampedOscillation.java`, `$J/home/GesturePreview.java`, `$J/home/PhysicsPreview.java`, `$J/home/ChartPreview.java`, `$J/home/ParticlePreview.java`, `$J/home/DrawingPreview.java`
- Modify (ghi đè): `$J/MainActivity.java`, `app/src/main/res/layout/activity_main.xml`, `app/src/main/res/layout/item_demo.xml`
- Modify: `app/src/main/res/values/strings.xml`, `docs/superpowers/plans/2026-10-13-animation-canvas.md` (Task B6 Step 5)
- Test: `$T/home/DampedOscillationTest.java`

**Interfaces:**
- Consumes: `FrameClock` (Task 0.2); màu `@color/preview_background`, `@color/brand_primary` (X6).
- Produces:
  - `interface Preview { void update(float dt, int width, int height); void draw(Canvas canvas, int width, int height); }`
  - `PreviewView(Context, Preview)`, `resume()`, `pause()`.
  - `DampedOscillation(float amplitude, float omega, float zeta)`, `float envelope(float t)`, `float valueAt(float t)`.
  - `MainActivity.Demo(int title, int description, Class<? extends Activity> screen, Supplier<Preview> preview)` — **B6 phải truyền thêm tham số thứ 4**.

- [ ] **Step 1: Viết test DampedOscillation**

`$T/home/DampedOscillationTest.java`:

```java
package vn.edu.tdtu.motioncanvas.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DampedOscillationTest {
    private final DampedOscillation swing = new DampedOscillation(30f, 6f, 0.12f);

    @Test
    public void startsAtFullAmplitude() {
        assertEquals(30f, swing.valueAt(0f), 1e-4f);
    }

    @Test
    public void envelopeShrinksOverTime() {
        assertTrue(swing.envelope(2f) < swing.envelope(1f));
        assertTrue(swing.envelope(1f) < swing.envelope(0f));
    }

    @Test
    public void valueNeverExceedsEnvelope() {
        for (float t = 0f; t <= 5f; t += 0.05f) {
            assertTrue(Math.abs(swing.valueAt(t)) <= swing.envelope(t) + 1e-4f);
        }
    }
}
```

- [ ] **Step 2: Chạy test, xác nhận FAIL**

Run: `./gradlew :app:testDebugUnitTest --tests "vn.edu.tdtu.motioncanvas.home.DampedOscillationTest"`
Expected: FAIL — `cannot find symbol: class DampedOscillation`

- [ ] **Step 3: Viết DampedOscillation**

`$J/home/DampedOscillation.java`:

```java
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

    float valueAt(float t) {
        return envelope(t) * (float) Math.cos(dampedOmega * t);
    }
}
```

- [ ] **Step 4: Chạy test, xác nhận PASS**

Run: `./gradlew :app:testDebugUnitTest --tests "vn.edu.tdtu.motioncanvas.home.DampedOscillationTest"`
Expected: PASS (3 tests)

- [ ] **Step 5: Preview và PreviewView**

`$J/home/Preview.java`:

```java
package vn.edu.tdtu.motioncanvas.home;

import android.graphics.Canvas;

/** Một hình động mini trên thẻ Home. Kích thước tính theo width/height để vừa mọi thẻ. */
public interface Preview {
    void update(float dt, int width, int height);

    void draw(Canvas canvas, int width, int height);
}
```

`$J/home/PreviewView.java`:

```java
package vn.edu.tdtu.motioncanvas.home;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

import vn.edu.tdtu.motioncanvas.core.FrameClock;

/** Chạy một Preview theo từng frame; Home gọi resume/pause theo vòng đời để không tốn pin khi ẩn. */
public class PreviewView extends View {
    private static final float MAX_DT = 1f / 30f;

    private final Preview preview;
    private final FrameClock clock = new FrameClock();
    private boolean running;

    public PreviewView(Context context, Preview preview) {
        super(context);
        this.preview = preview;
    }

    public void resume() {
        if (running) return;
        running = true;
        clock.reset();
        postInvalidateOnAnimation();
    }

    public void pause() {
        running = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        preview.update(Math.min(clock.tick(), MAX_DT), w, h);
        preview.draw(canvas, w, h);
        if (running) postInvalidateOnAnimation();
    }
}
```

- [ ] **Step 6: Năm hình động mini**

`$J/home/GesturePreview.java`:

```java
package vn.edu.tdtu.motioncanvas.home;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/** Thẻ nhỏ treo trên dây, đung đưa tắt dần như lò xo rồi lặp lại. */
public final class GesturePreview implements Preview {
    private static final float PERIOD_S = 4f;
    private static final int CARD_COLOR = 0xFF5C6BC0;
    private static final int STRING_COLOR = 0xFF9FA8DA;

    private final DampedOscillation swing = new DampedOscillation(28f, 6f, 0.12f);
    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF card = new RectF();
    private float time;

    public GesturePreview() {
        cardPaint.setColor(CARD_COLOR);
        stringPaint.setColor(STRING_COLOR);
    }

    @Override
    public void update(float dt, int width, int height) {
        time = (time + dt) % PERIOD_S;
    }

    @Override
    public void draw(Canvas canvas, int width, int height) {
        float pivotX = width / 2f;
        float pivotY = height * 0.08f;
        float cardW = height * 0.38f;
        float cardH = height * 0.5f;
        float top = height * 0.35f;
        stringPaint.setStrokeWidth(height * 0.02f);
        canvas.save();
        canvas.rotate(swing.valueAt(time), pivotX, pivotY);
        canvas.drawLine(pivotX, pivotY, pivotX, top, stringPaint);
        card.set(pivotX - cardW / 2f, top, pivotX + cardW / 2f, top + cardH);
        canvas.drawRoundRect(card, cardW * 0.15f, cardW * 0.15f, cardPaint);
        canvas.restore();
    }
}
```

`$J/home/PhysicsPreview.java`:

```java
package vn.edu.tdtu.motioncanvas.home;

import android.graphics.Canvas;
import android.graphics.Paint;

/** Ba quả bóng rơi và nảy; khi nảy quá thấp thì được tung lên lại để hình động không dừng. */
public final class PhysicsPreview implements Preview {
    private static final int[] COLORS = {0xFFEF5350, 0xFF42A5F5, 0xFFFFCA28};
    // Đơn vị tính theo chiều cao thẻ (h) để mọi kích thước thẻ trông như nhau
    private static final float GRAVITY_H = 3f;
    private static final float RESTITUTION = 0.8f;
    private static final float MIN_BOUNCE_H = 0.9f;
    private static final float RELAUNCH_H = 2.2f;
    private static final float RADIUS_H = 0.08f;

    private final float[] y = new float[COLORS.length];
    private final float[] vy = new float[COLORS.length];
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean started;

    @Override
    public void update(float dt, int width, int height) {
        if (height == 0) return;
        if (!started) {
            for (int i = 0; i < y.length; i++) y[i] = height * (0.2f + 0.2f * i);
            started = true;
        }
        float floor = height - height * RADIUS_H;
        for (int i = 0; i < y.length; i++) {
            vy[i] += GRAVITY_H * height * dt;
            y[i] += vy[i] * dt;
            if (y[i] > floor) {
                y[i] = floor;
                vy[i] = -vy[i] * RESTITUTION;
                if (-vy[i] < MIN_BOUNCE_H * height) vy[i] = -RELAUNCH_H * height;
            }
        }
    }

    @Override
    public void draw(Canvas canvas, int width, int height) {
        float radius = height * RADIUS_H;
        for (int i = 0; i < y.length; i++) {
            paint.setColor(COLORS[i]);
            canvas.drawCircle(width * (i + 1f) / (y.length + 1f), y[i], radius, paint);
        }
    }
}
```

`$J/home/ChartPreview.java`:

```java
package vn.edu.tdtu.motioncanvas.home;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/** Năm cột lên xuống theo hình sin lệch pha nhau. */
public final class ChartPreview implements Preview {
    private static final int BARS = 5;
    private static final int COLOR = 0xFF26A69A;
    private static final float SPEED = 2.5f;
    private static final float PHASE = 0.9f;
    private static final float BASE = 0.35f;
    private static final float SWING = 0.3f;
    private static final float BAR_FILL = 0.6f;
    private static final float TIME_WRAP_S = 1000f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bar = new RectF();
    private float time;

    public ChartPreview() {
        paint.setColor(COLOR);
    }

    @Override
    public void update(float dt, int width, int height) {
        time = (time + dt) % TIME_WRAP_S;
    }

    @Override
    public void draw(Canvas canvas, int width, int height) {
        float slot = width * 0.8f / BARS;
        float bottom = height * 0.88f;
        float corner = slot * 0.15f;
        for (int i = 0; i < BARS; i++) {
            float fraction = BASE + SWING * (float) Math.sin(time * SPEED + i * PHASE);
            float left = width * 0.1f + slot * i + slot * (1f - BAR_FILL) / 2f;
            bar.set(left, bottom - fraction * height, left + slot * BAR_FILL, bottom);
            canvas.drawRoundRect(bar, corner, corner, paint);
        }
    }
}
```

`$J/home/ParticlePreview.java`:

```java
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
```

`$J/home/DrawingPreview.java`:

```java
package vn.edu.tdtu.motioncanvas.home;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;

/** Nét lượn sóng tự vẽ dần (PathMeasure.getSegment), giữ một lúc, mờ đi rồi lặp lại. */
public final class DrawingPreview implements Preview {
    private static final float CYCLE_S = 3f;
    private static final float DRAW_S = 1.8f;
    private static final float FADE_START_S = 2.4f;
    private static final int COLOR = 0xFFEC407A;

    private final Path wave = new Path();
    private final Path segment = new Path();
    private final PathMeasure measure = new PathMeasure();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float length;
    private int builtWidth;
    private int builtHeight;
    private float time;

    public DrawingPreview() {
        paint.setColor(COLOR);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override
    public void update(float dt, int width, int height) {
        time = (time + dt) % CYCLE_S;
        if (width != builtWidth || height != builtHeight) build(width, height);
    }

    /** Dựng lại đường sóng chỉ khi kích thước thẻ đổi. */
    private void build(int w, int h) {
        wave.reset();
        wave.moveTo(w * 0.1f, h * 0.55f);
        wave.cubicTo(w * 0.25f, h * 0.1f, w * 0.35f, h * 0.9f, w * 0.5f, h * 0.5f);
        wave.cubicTo(w * 0.65f, h * 0.1f, w * 0.75f, h * 0.9f, w * 0.9f, h * 0.45f);
        measure.setPath(wave, false);
        length = measure.getLength();
        paint.setStrokeWidth(h * 0.05f);
        builtWidth = w;
        builtHeight = h;
    }

    @Override
    public void draw(Canvas canvas, int width, int height) {
        float drawn = Math.min(1f, time / DRAW_S);
        float alpha = time < FADE_START_S ? 1f : 1f - (time - FADE_START_S) / (CYCLE_S - FADE_START_S);
        segment.reset();
        measure.getSegment(0f, length * drawn, segment, true);
        paint.setAlpha((int) (255 * alpha));
        canvas.drawPath(segment, paint);
    }
}
```

- [ ] **Step 7: Chuỗi và layout**

`strings.xml`, thêm:

```xml
    <string name="home_subtitle">Animation &amp; Canvas · Android</string>
```

`app/src/main/res/layout/activity_main.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/main"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="?android:attr/colorBackground">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/app_name"
            android:textAppearance="?attr/textAppearanceHeadlineMedium"
            android:textColor="@color/brand_primary"
            android:textStyle="bold" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginBottom="8dp"
            android:text="@string/home_subtitle"
            android:textAppearance="?attr/textAppearanceBodyMedium" />

        <GridLayout
            android:id="@+id/demo_grid"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:columnCount="2" />
    </LinearLayout>
</ScrollView>
```

`app/src/main/res/layout/item_demo.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardCornerRadius="16dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">

        <FrameLayout
            android:id="@+id/demo_preview"
            android:layout_width="match_parent"
            android:layout_height="110dp"
            android:background="@color/preview_background" />

        <TextView
            android:id="@+id/demo_title"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:paddingHorizontal="12dp"
            android:paddingTop="10dp"
            android:textAppearance="?attr/textAppearanceTitleSmall" />

        <TextView
            android:id="@+id/demo_desc"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:maxLines="2"
            android:paddingHorizontal="12dp"
            android:paddingTop="2dp"
            android:paddingBottom="12dp"
            android:textAppearance="?attr/textAppearanceBodySmall" />
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

- [ ] **Step 8: Ghi đè MainActivity**

`$J/MainActivity.java`:

```java
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
```

- [ ] **Step 9: Cập nhật Task B6 trong plan gốc cho khớp `Demo` mới**

Trong `docs/superpowers/plans/2026-10-13-animation-canvas.md`, Task B6 Step 5:
- thay dòng `new Demo(R.string.demo_playground, R.string.demo_playground_desc, PlaygroundActivity.class),` bằng `new Demo(R.string.demo_playground, R.string.demo_playground_desc, PlaygroundActivity.class, PhysicsPreview::new),`
- trong khối `<activity android:name=".playground.PlaygroundActivity" …>`, thêm dòng `android:parentActivityName=".MainActivity"` ngay trên dòng `android:screenOrientation="portrait" />`.

Run: `git grep -n "PlaygroundActivity.class" -- docs`
Expected: dòng tìm được có `PhysicsPreview::new`.

- [ ] **Step 10: Build**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`, mọi test PASS (có DampedOscillation 3).

- [ ] **Step 11: Kiểm tra trên máy thật**

Đối chiếu:
- Home: tiêu đề "MotionCanvas" màu indigo, dòng phụ bên dưới; lưới 2 cột, 5 thẻ bo góc, thẻ thứ 5 nằm cột trái hàng cuối.
- Mỗi thẻ có hình động chạy mượt: thẻ đung đưa tắt dần (lặp mỗi 4 s), 3 bóng nảy không ngừng, 5 cột nhấp nhô, ngọn lửa nền tối, nét sóng hồng tự vẽ rồi mờ.
- Chạm thẻ → màn demo phóng to ra từ đúng vị trí thẻ.
- Vào màn demo rồi quay lại → hình động chạy tiếp, không có "nhảy cóc" (bóng không bay mất).
- Bấm Home của máy 10 giây rồi mở lại → hình động chạy tiếp bình thường.
- Cuộn Home mượt.

- [ ] **Step 12: Commit**

```bash
git add app/src docs/superpowers/plans/2026-10-13-animation-canvas.md
git commit -m "feat: redesign home grid with live canvas previews and scale-up transition"
```

---

# KIỂM TRA TỔNG

### Task X8: Chạy toàn bộ test, lint, checklist tổng

**Files:** không tạo file mới.

**Interfaces:**
- Consumes: mọi task X1–X7.
- Produces: bản build cuối của các gói cộng thêm.

- [ ] **Step 1: Toàn bộ test, build, lint**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug`
Expected: `BUILD SUCCESSFUL`. Test mới so với trước plan: Symmetry 2, CalligraphyWidth 6, FloodFill 5, ReplayTimeline 11, Stroke 3, DampedOscillation 3, ChartMath +4 −2. Đọc báo cáo lint `app/build/reports/lint-results-debug.html`; sửa mọi mục mức `Error`.

- [ ] **Step 2: Checklist tổng trên máy thật**

Làm lại bước "Kiểm tra trên máy thật" của X2, X3, X4, X5, X6, X7 trên bản build cuối, thêm:
- Mở/đóng từng màn 5 lần liên tiếp → không crash, không chậm dần.
- Drawing: vẽ khoảng 30 nét (có 3–4 lần Đổ màu) → Hoàn tác liên tục 10 lần → không đứng hình quá 1 giây mỗi lần.

- [ ] **Step 3: Báo A**

Gửi A (tin nhắn nhóm): (1) mọi màn demo giờ có thanh tiêu đề + nút quay lại do đổi theme — A không cần sửa gì; (2) Home có hình minh họa chủ đề của A; (3) Task B6 trong plan gốc đã được cập nhật cho `Demo` mới.
