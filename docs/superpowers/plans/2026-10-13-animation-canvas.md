# Canvas Demo – Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** App Android (Java + XML) minh họa đủ 5 mục nhóm 1.1 của đề Animation & Canvas: Gesture, Physics, Chart, Particle, Drawing (+ Playground nếu dư thời gian).

**Architecture:** 1 module `app`. Mỗi demo = 1 Activity + 1 Custom View vẽ trong `onDraw(Canvas)`. Logic (vật lý, hạt, quy đổi tọa độ chart, undo, toán chọn màu) nằm trong class Java thuần để test bằng JUnit; View chỉ vẽ và nhận chạm.

**Tech Stack:** Java 11, Android View system + XML, `androidx.appcompat`, `com.google.android.material`, `androidx.dynamicanimation:dynamicanimation:1.0.0`, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-10-13-animation-canvas-design.md`

## Cách đọc plan này

| Phần | Ai làm | Khi nào |
|---|---|---|
| Task 0.1, 0.2 | **B** | 13–14/10 — A chỉ bắt đầu code sau khi B push xong Task 0.2 |
| Track A: A1 → A5 | **A** | A1–A2: 15–19/10 · A3–A5: 20–23/10 |
| Track B: B1 → B5 | **B** | B1–B3: 15–19/10 · B4–B5: 20–23/10 |
| 24–26/10 | cả hai | sửa lỗi; **26/10 = mốc chốt** |
| B6 (Playground) | **B** | chỉ khi mốc chốt đã đạt; 27–28/10 |
| Task F1 | cả hai | 27–30/10 |

Mỗi người chỉ cần đọc phần "Chung" + track của mình. Task B6 dùng `World` (A1) và `Emitter` (A4) — chữ ký của chúng nằm trong khối **Interfaces** của A1/A4.

**Quy ước lệnh:** mọi lệnh `./gradlew` và `git` chạy trong **tab Terminal của Android Studio**, tại thư mục gốc project `CanvasDemo/` (tab này đã có sẵn JDK nên không cần cài `JAVA_HOME`).

**Package:** plan dùng `vn.edu.tdtu.canvasdemo`. Nếu chọn package khác lúc tạo project thì thay toàn bộ chuỗi này (cả đường dẫn thư mục `vn/edu/tdtu/canvasdemo`) trước khi làm.

**Làm việc chung trên Git:** cả hai commit lên `main`, luôn `git pull --rebase` trước khi `git push`. Xung đột chỉ có thể xảy ra ở `strings.xml` (hai người cùng thêm chuỗi) → giữ cả hai phía.

## Global Constraints

- Ngôn ngữ / UI: Java + XML (View system). Không dùng Kotlin, không dùng Jetpack Compose.
- minSdk: API 24 (Android 7.0).
- Thư viện ngoài: chỉ `appcompat`, `material`, `androidx.dynamicanimation`. Cấm thư viện chart, particle, color picker, drawing của bên thứ ba.
- Mọi Activity khóa dọc (`android:screenOrientation="portrait"`).
- App chạy edge-to-edge: mọi Activity gọi `EdgeToEdge.enable(this)` và `SystemBars.applyPadding(findViewById(R.id.main))`; view gốc của mọi layout có `android:id="@+id/main"`.
- Không tạo `Paint`, `Path`, `RectF` trong `onDraw` — tạo sẵn ở constructor/field.
- Không so sánh `R.id.*` bằng `switch` (AGP 8 sinh id không phải hằng) — dùng `if`.
- Chuỗi hiển thị nằm trong `res/values/strings.xml`.
- Mô phỏng (Physics, Particle, Playground): `dt` bị chặn tối đa `1/30 s`; dừng vòng lặp ở `onPause`, chạy lại ở `onResume`.
- Toàn bộ code tự viết và mỗi người giải thích được phần mình phụ trách.

## Review Focus

1. **Quay lại app sau khi ở background / máy lag** → `dt` rất lớn; người dùng mong bóng/hạt không "bay" xuyên tường. Test: `WorldTest.hugeDtIsClampedToMaxDt` (A1), `EmitterTest.hugeDtIsClampedToMaxDt` (A4).
2. **Chạm hai lần cùng một điểm ở màn Physics** → hai bóng trùng tâm; mong đợi bóng tách ra, không NaN, không biến mất. Test: `WorldTest.coincidentBodiesSeparateWithoutNaN` (A1).
3. **Vuốt thẻ khi thẻ đang nằm ngoài màn hoặc đang phóng to lớn hơn màn** → `FlingAnimation` ném `IllegalArgumentException` nếu giá trị đầu nằm ngoài [min, max]; mong đợi thẻ trượt về trong màn, không crash. Test: `GestureMathTest.*` (A3).
4. **Dữ liệu chart phẳng hoặc bằng 0** (mọi giá trị bằng nhau, donut tổng 0) → mong đợi chart vẫn vẽ, không chia cho 0. Test: `ChartScaleTest.flatDataDoesNotDivideByZero`, `ChartMathTest.zeroTotalGivesZeroSweeps` (B1).
5. **Bấm Undo/Redo khi chưa vẽ gì; kéo ngón ra ngoài bảng màu** → mong đợi không làm gì, không crash; màu bị kẹp trong [0, 1]. Test: `UndoStackTest.undoAndRedoOnEmptyReturnFalse` (B4), `PickerMathTest.fractionIsClampedOutsideRange` (B5).

---

# PHẦN 0 — CHUNG (B làm)

### Task 0.1: Tạo project, thêm thư viện, khởi tạo Git

**Files:**
- Create: toàn bộ project `CanvasDemo/` (Android Studio sinh ra)
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts`

**Interfaces:**
- Produces: project build được; dependency `libs.androidx.dynamicanimation`.

- [ ] **Step 1: Tạo project trong Android Studio**

File → New → New Project… → tab *Phone and Tablet* → **Empty Views Activity** → Next:

| Ô | Giá trị |
|---|---|
| Name | `CanvasDemo` |
| Package name | `vn.edu.tdtu.canvasdemo` |
| Save location | `.../Mid_Exam/CanvasDemo` |
| Language | Java |
| Minimum SDK | API 24 ("Nougat"; Android 7.0) |
| Build configuration language | Kotlin DSL (build.gradle.kts) — mặc định |

Finish, đợi Gradle sync xong.

- [ ] **Step 2: Chạy thử app trắng**

Chạy (nút ▶) trên máy thật hoặc emulator. Expected: app mở, hiện "Hello World!".

- [ ] **Step 3: Thêm `dynamicanimation` vào version catalog**

Trong `gradle/libs.versions.toml`, thêm dòng vào cuối khối `[versions]`:

```toml
dynamicanimation = "1.0.0"
```

và thêm dòng vào cuối khối `[libraries]`:

```toml
androidx-dynamicanimation = { group = "androidx.dynamicanimation", name = "dynamicanimation", version.ref = "dynamicanimation" }
```

Trong `app/build.gradle.kts`, khối `dependencies { … }`, thêm ngay sau dòng `implementation(libs.material)`:

```kotlin
    implementation(libs.androidx.dynamicanimation)
```

Bấm **Sync Now**. (Nếu Android Studio gợi ý phiên bản mới hơn của `dynamicanimation`, nhận gợi ý cũng được.)

- [ ] **Step 4: Build kiểm tra**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Đưa tài liệu vào project và commit đầu tiên**

```bash
mv ../docs ./docs
git init -b main
git add .
git commit -m "chore: create CanvasDemo project with dynamicanimation"
```

- [ ] **Step 6: Tạo repo GitHub và mời A**

Trên github.com: New repository → tên `CanvasDemo` → **Private** → không tick README/.gitignore → Create. Sau đó:

```bash
git remote add origin <URL repo vừa tạo>
git push -u origin main
```

Settings → Collaborators → mời tài khoản GitHub của A.

---

### Task 0.2: Màn Home, Activity khung, tiện ích dùng chung

**Files:**
- Create: `app/src/main/java/vn/edu/tdtu/canvasdemo/core/FrameClock.java`
- Create: `app/src/main/java/vn/edu/tdtu/canvasdemo/core/SystemBars.java`
- Create: `app/src/main/java/vn/edu/tdtu/canvasdemo/core/SeekBars.java`
- Create: `app/src/main/java/vn/edu/tdtu/canvasdemo/{gesture/GestureActivity, physics/PhysicsActivity, chart/ChartActivity, particle/ParticleActivity, drawing/DrawingActivity}.java` (khung)
- Create: `app/src/main/res/layout/activity_demo_stub.xml`, `app/src/main/res/layout/item_demo.xml`
- Modify: `app/src/main/java/vn/edu/tdtu/canvasdemo/MainActivity.java`, `app/src/main/res/layout/activity_main.xml`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/vn/edu/tdtu/canvasdemo/core/FrameClockTest.java`

**Interfaces:**
- Produces (dùng ở các task sau):
  - `FrameClock()`, `FrameClock(LongSupplier nanoTime)`, `void reset()`, `float tick()` — giây từ lần `tick()` trước; lần đầu sau `reset()` trả về `0`.
  - `SystemBars.applyPadding(View root)`
  - `SeekBars.onProgress(SeekBar bar, IntConsumer action)` — gọi `action` ngay với giá trị hiện tại và mỗi khi kéo.
  - Các Activity khung: `gesture.GestureActivity`, `physics.PhysicsActivity`, `chart.ChartActivity`, `particle.ParticleActivity`, `drawing.DrawingActivity` — mỗi track sẽ ghi đè file Activity của mình.
  - Chuỗi dùng chung: `@string/clear`.

- [ ] **Step 1: Viết test cho FrameClock**

`app/src/test/java/vn/edu/tdtu/canvasdemo/core/FrameClockTest.java`:

```java
package vn.edu.tdtu.canvasdemo.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FrameClockTest {
    private long now;
    private final FrameClock clock = new FrameClock(() -> now);

    @Test
    public void firstTickAfterResetIsZero() {
        now = 5_000_000_000L;
        assertEquals(0f, clock.tick(), 0f);
    }

    @Test
    public void tickReturnsSecondsSincePreviousTick() {
        clock.tick();
        now += 16_000_000L;
        assertEquals(0.016f, clock.tick(), 1e-6f);
    }

    @Test
    public void resetDropsTimeSpentPaused() {
        clock.tick();
        now += 10_000_000_000L;
        clock.reset();
        assertEquals(0f, clock.tick(), 0f);
    }
}
```

- [ ] **Step 2: Chạy test, xác nhận FAIL**

Run: `./gradlew :app:testDebugUnitTest --tests "vn.edu.tdtu.canvasdemo.core.FrameClockTest"`
Expected: FAIL — `cannot find symbol: class FrameClock`

- [ ] **Step 3: Viết FrameClock**

`app/src/main/java/vn/edu/tdtu/canvasdemo/core/FrameClock.java`:

```java
package vn.edu.tdtu.canvasdemo.core;

import java.util.function.LongSupplier;

/** Đo thời gian (giây) giữa hai frame cho các màn mô phỏng. */
public final class FrameClock {
    private final LongSupplier nanoTime;
    private long last = -1L;

    public FrameClock() {
        this(System::nanoTime);
    }

    public FrameClock(LongSupplier nanoTime) {
        this.nanoTime = nanoTime;
    }

    /** Gọi khi bắt đầu/tiếp tục chạy để frame đầu không tính thời gian đã tạm dừng. */
    public void reset() {
        last = -1L;
    }

    /** Số giây kể từ lần tick trước; lần đầu sau reset() trả về 0. */
    public float tick() {
        long now = nanoTime.getAsLong();
        float dt = last < 0 ? 0f : (now - last) / 1_000_000_000f;
        last = now;
        return dt;
    }
}
```

- [ ] **Step 4: Chạy test, xác nhận PASS**

Run: `./gradlew :app:testDebugUnitTest --tests "vn.edu.tdtu.canvasdemo.core.FrameClockTest"`
Expected: PASS (3 tests)

- [ ] **Step 5: Viết SystemBars và SeekBars**

`app/src/main/java/vn/edu/tdtu/canvasdemo/core/SystemBars.java`:

```java
package vn.edu.tdtu.canvasdemo.core;

import android.view.View;

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
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }
}
```

`app/src/main/java/vn/edu/tdtu/canvasdemo/core/SeekBars.java`:

```java
package vn.edu.tdtu.canvasdemo.core;

import android.widget.SeekBar;

import java.util.function.IntConsumer;

public final class SeekBars {
    private SeekBars() {
    }

    /** Gọi action với giá trị hiện tại ngay lập tức, rồi mỗi khi người dùng kéo thanh trượt. */
    public static void onProgress(SeekBar bar, IntConsumer action) {
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                action.accept(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        action.accept(bar.getProgress());
    }
}
```

- [ ] **Step 6: Thêm chuỗi**

Trong `app/src/main/res/values/strings.xml`, thêm vào trong `<resources>` (giữ nguyên dòng `app_name` có sẵn):

```xml
    <string name="demo_gesture">Gesture Animation</string>
    <string name="demo_gesture_desc">Kéo, vuốt, phóng to và bật lò xo</string>
    <string name="demo_physics">Physics</string>
    <string name="demo_physics_desc">Trọng lực, va chạm và lò xo</string>
    <string name="demo_chart">Dynamic Charts</string>
    <string name="demo_chart_desc">Line realtime, bar và donut động</string>
    <string name="demo_particle">Particle: Lửa &amp; Nước</string>
    <string name="demo_particle_desc">Hệ thống hạt với object pool</string>
    <string name="demo_drawing">Drawing</string>
    <string name="demo_drawing_desc">Brush tùy chỉnh và bảng chọn màu</string>
    <string name="coming_soon">Đang phát triển…</string>
    <string name="clear">Xóa hết</string>
```

- [ ] **Step 7: Layout khung và layout thẻ**

`app/src/main/res/layout/activity_demo_stub.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/main"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:text="@string/coming_soon" />
</FrameLayout>
```

`app/src/main/res/layout/item_demo.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="12dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <TextView
            android:id="@+id/demo_title"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textAppearance="?attr/textAppearanceTitleMedium" />

        <TextView
            android:id="@+id/demo_desc"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:textAppearance="?attr/textAppearanceBodyMedium" />
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

Ghi đè `app/src/main/res/layout/activity_main.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/main"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <LinearLayout
        android:id="@+id/demo_list"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/app_name"
            android:textAppearance="?attr/textAppearanceHeadlineSmall" />
    </LinearLayout>
</ScrollView>
```

(Spec cho phép RecyclerView hoặc GridLayout; với 5–6 thẻ, danh sách card trong ScrollView đơn giản hơn và không cần Adapter.)

- [ ] **Step 8: Năm Activity khung**

Tạo 5 file sau, nội dung giống nhau, chỉ khác package và tên class.

`app/src/main/java/vn/edu/tdtu/canvasdemo/gesture/GestureActivity.java`:

```java
package vn.edu.tdtu.canvasdemo.gesture;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import vn.edu.tdtu.canvasdemo.R;
import vn.edu.tdtu.canvasdemo.core.SystemBars;

public class GestureActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_demo_stub);
        SystemBars.applyPadding(findViewById(R.id.main));
    }
}
```

`app/src/main/java/vn/edu/tdtu/canvasdemo/physics/PhysicsActivity.java`:

```java
package vn.edu.tdtu.canvasdemo.physics;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import vn.edu.tdtu.canvasdemo.R;
import vn.edu.tdtu.canvasdemo.core.SystemBars;

public class PhysicsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_demo_stub);
        SystemBars.applyPadding(findViewById(R.id.main));
    }
}
```

`app/src/main/java/vn/edu/tdtu/canvasdemo/chart/ChartActivity.java`:

```java
package vn.edu.tdtu.canvasdemo.chart;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import vn.edu.tdtu.canvasdemo.R;
import vn.edu.tdtu.canvasdemo.core.SystemBars;

public class ChartActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_demo_stub);
        SystemBars.applyPadding(findViewById(R.id.main));
    }
}
```

`app/src/main/java/vn/edu/tdtu/canvasdemo/particle/ParticleActivity.java`:

```java
package vn.edu.tdtu.canvasdemo.particle;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import vn.edu.tdtu.canvasdemo.R;
import vn.edu.tdtu.canvasdemo.core.SystemBars;

public class ParticleActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_demo_stub);
        SystemBars.applyPadding(findViewById(R.id.main));
    }
}
```

`app/src/main/java/vn/edu/tdtu/canvasdemo/drawing/DrawingActivity.java`:

```java
package vn.edu.tdtu.canvasdemo.drawing;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import vn.edu.tdtu.canvasdemo.R;
import vn.edu.tdtu.canvasdemo.core.SystemBars;

public class DrawingActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_demo_stub);
        SystemBars.applyPadding(findViewById(R.id.main));
    }
}
```

- [ ] **Step 9: MainActivity**

Ghi đè `app/src/main/java/vn/edu/tdtu/canvasdemo/MainActivity.java`:

```java
package vn.edu.tdtu.canvasdemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import vn.edu.tdtu.canvasdemo.chart.ChartActivity;
import vn.edu.tdtu.canvasdemo.core.SystemBars;
import vn.edu.tdtu.canvasdemo.drawing.DrawingActivity;
import vn.edu.tdtu.canvasdemo.gesture.GestureActivity;
import vn.edu.tdtu.canvasdemo.particle.ParticleActivity;
import vn.edu.tdtu.canvasdemo.physics.PhysicsActivity;

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
```

- [ ] **Step 10: Khai báo Activity trong Manifest**

Trong `app/src/main/AndroidManifest.xml`: thêm `android:screenOrientation="portrait"` vào thẻ `<activity android:name=".MainActivity" …>` có sẵn, rồi thêm các dòng sau vào trong `<application>`, ngay sau thẻ `</activity>` của MainActivity:

```xml
        <activity
            android:name=".gesture.GestureActivity"
            android:exported="false"
            android:label="@string/demo_gesture"
            android:screenOrientation="portrait" />
        <activity
            android:name=".physics.PhysicsActivity"
            android:exported="false"
            android:label="@string/demo_physics"
            android:screenOrientation="portrait" />
        <activity
            android:name=".chart.ChartActivity"
            android:exported="false"
            android:label="@string/demo_chart"
            android:screenOrientation="portrait" />
        <activity
            android:name=".particle.ParticleActivity"
            android:exported="false"
            android:label="@string/demo_particle"
            android:screenOrientation="portrait" />
        <activity
            android:name=".drawing.DrawingActivity"
            android:exported="false"
            android:label="@string/demo_drawing"
            android:screenOrientation="portrait" />
```

- [ ] **Step 11: Chạy app kiểm tra**

Run: `./gradlew :app:assembleDebug` → Expected: `BUILD SUCCESSFUL`. Chạy app trên máy: Home hiện 5 thẻ, không bị status bar che; chạm từng thẻ mở màn "Đang phát triển…", Back quay về Home.

- [ ] **Step 12: Commit và push**

```bash
git add .
git commit -m "feat: add home screen, demo stubs and shared core helpers"
git push
```

Báo A: "đã push Phần 0, pull về và bắt đầu Track A".

---

# TRACK A — "Chuyển động" (A làm)

Trước khi bắt đầu: `git pull` để có Phần 0.

### Task A1: Engine vật lý (`Body`, `Spring`, `World`)

**Files:**
- Create: `app/src/main/java/vn/edu/tdtu/canvasdemo/physics/Body.java`
- Create: `app/src/main/java/vn/edu/tdtu/canvasdemo/physics/Spring.java`
- Create: `app/src/main/java/vn/edu/tdtu/canvasdemo/physics/World.java`
- Test: `app/src/test/java/vn/edu/tdtu/canvasdemo/physics/WorldTest.java`

**Interfaces:**
- Consumes: không có.
- Produces (A2 và **B6 Playground** dùng — giữ ổn định sau mốc 26/10):
  - `Body(float x, float y, float radius, int color)`; field public `x, y, vx, vy` (px, px/s), `final radius`, `final mass` (= radius²), `final color`, `boolean held`.
  - `Spring(Body body, float anchorX, float anchorY, float restLength, float stiffness, float damping)`; field public final cùng tên.
  - `World(float width, float height, int maxBodies)`
  - `World.MAX_DT = 1f/30f`, `World.MIN_IMPACT_SPEED = 150f`
  - `void setBounds(float w, float h)`, `void setGravity(float pxPerSec2)`, `void setRestitution(float e)` (kẹp 0..1)
  - `boolean addBody(Body b)` (false khi đã đủ `maxBodies`), `void addSpring(Spring s)`, `void clear()`
  - `List<Body> getBodies()`, `List<Spring> getSprings()` (chỉ đọc)
  - `Body findBodyAt(float x, float y)` (null nếu không trúng)
  - `void step(float dt)`
  - `interface World.CollisionListener { void onCollision(float x, float y, float impactSpeed); }`, `void setCollisionListener(CollisionListener l)` — chỉ báo va chạm có tốc độ ≥ `MIN_IMPACT_SPEED`.

- [ ] **Step 1: Viết test**

`app/src/test/java/vn/edu/tdtu/canvasdemo/physics/WorldTest.java`:

```java
package vn.edu.tdtu.canvasdemo.physics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WorldTest {
    private static final float FRAME = 1f / 30f;

    private static World emptyWorld(float w, float h) {
        World world = new World(w, h, 50);
        world.setGravity(0f);
        return world;
    }

    @Test
    public void freeFallMatchesHalfGTSquared() {
        World world = new World(1000f, 100_000f, 50);
        world.setGravity(1000f);
        Body ball = new Body(500f, 100f, 10f, 0);
        world.addBody(ball);

        for (int i = 0; i < 30; i++) world.step(FRAME); // 1 giây

        assertEquals(100f + 0.5f * 1000f, ball.y, 10f);
        assertEquals(1000f, ball.vy, 0.5f);
    }

    @Test
    public void floorBounceReversesVelocityTimesRestitution() {
        World world = emptyWorld(1000f, 1000f);
        world.setRestitution(0.5f);
        Body ball = new Body(500f, 985f, 10f, 0);
        ball.vy = 1000f;
        world.addBody(ball);

        world.step(FRAME);

        assertEquals(-500f, ball.vy, 1e-3f);
        assertTrue(ball.y <= 990f);
    }

    @Test
    public void fastWallHitNotifiesListenerOnce() {
        World world = emptyWorld(1000f, 1000f);
        Body ball = new Body(500f, 985f, 10f, 0);
        ball.vy = 1000f;
        world.addBody(ball);
        int[] count = {0};
        float[] speed = {0f};
        world.setCollisionListener((x, y, impactSpeed) -> {
            count[0]++;
            speed[0] = impactSpeed;
        });

        world.step(FRAME);

        assertEquals(1, count[0]);
        assertEquals(1000f, speed[0], 1e-3f);
    }

    @Test
    public void restingBallDoesNotSpamCollisions() {
        World world = new World(1000f, 1000f, 50);
        world.setGravity(2000f);
        world.addBody(new Body(500f, 990f, 10f, 0));
        int[] count = {0};
        world.setCollisionListener((x, y, s) -> count[0]++);

        for (int i = 0; i < 60; i++) world.step(FRAME);

        assertEquals(0, count[0]);
    }

    @Test
    public void elasticCollisionConservesMomentum() {
        World world = emptyWorld(10_000f, 10_000f);
        world.setRestitution(1f);
        Body a = new Body(100f, 500f, 10f, 0);   // mass 100
        Body b = new Body(125f, 500f, 20f, 0);   // mass 400, đang chồng lên a
        a.vx = 300f;
        b.vx = -100f;
        world.addBody(a);
        world.addBody(b);

        world.step(FRAME);

        assertEquals(-10_000f, a.mass * a.vx + b.mass * b.vx, 0.5f);
        assertEquals(-340f, a.vx, 0.01f);
        assertEquals(60f, b.vx, 0.01f);
    }

    @Test
    public void coincidentBodiesSeparateWithoutNaN() {
        World world = emptyWorld(1000f, 1000f);
        Body a = new Body(500f, 500f, 10f, 0);
        Body b = new Body(500f, 500f, 10f, 0);
        world.addBody(a);
        world.addBody(b);

        world.step(FRAME);

        assertFalse(Float.isNaN(a.x) || Float.isNaN(b.x) || Float.isNaN(a.vx) || Float.isNaN(b.vx));
        float dx = b.x - a.x, dy = b.y - a.y;
        assertTrue(Math.sqrt(dx * dx + dy * dy) >= 20f - 1e-3f);
    }

    @Test
    public void stretchedSpringPullsBodyTowardAnchor() {
        World world = emptyWorld(1000f, 1000f);
        Body ball = new Body(500f, 500f, 10f, 0);
        world.addBody(ball);
        world.addSpring(new Spring(ball, 500f, 100f, 200f, 40f * ball.mass, 0f));

        world.step(FRAME);

        assertTrue(ball.vy < 0f);
    }

    @Test
    public void dampedSpringSettlesAtRestLength() {
        World world = emptyWorld(1000f, 1000f);
        Body ball = new Body(500f, 500f, 10f, 0);
        world.addBody(ball);
        world.addSpring(new Spring(ball, 500f, 100f, 200f, 40f * ball.mass, 1.2f * ball.mass));

        for (int i = 0; i < 300; i++) world.step(FRAME); // 10 giây

        assertEquals(300f, ball.y, 2f);
    }

    @Test
    public void hugeDtIsClampedToMaxDt() {
        World slow = emptyWorld(1000f, 1000f);
        World fast = emptyWorld(1000f, 1000f);
        Body a = new Body(100f, 500f, 10f, 0);
        Body b = new Body(100f, 500f, 10f, 0);
        a.vx = 300f;
        b.vx = 300f;
        slow.addBody(a);
        fast.addBody(b);

        slow.step(5f);
        fast.step(World.MAX_DT);

        assertEquals(b.x, a.x, 1e-4f);
    }

    @Test
    public void addBodyRefusesBeyondMax() {
        World world = new World(1000f, 1000f, 2);
        assertTrue(world.addBody(new Body(100f, 100f, 10f, 0)));
        assertTrue(world.addBody(new Body(200f, 100f, 10f, 0)));
        assertFalse(world.addBody(new Body(300f, 100f, 10f, 0)));
        assertEquals(2, world.getBodies().size());
    }

    @Test
    public void findBodyAtReturnsTopmostHit() {
        World world = emptyWorld(1000f, 1000f);
        Body bottom = new Body(100f, 100f, 20f, 0);
        Body top = new Body(110f, 100f, 20f, 0);
        world.addBody(bottom);
        world.addBody(top);

        assertSame(top, world.findBodyAt(105f, 100f));
        assertNull(world.findBodyAt(500f, 500f));
    }

    @Test
    public void heldBodyDoesNotMove() {
        World world = new World(1000f, 1000f, 50);
        world.setGravity(2000f);
        Body ball = new Body(500f, 500f, 10f, 0);
        ball.held = true;
        world.addBody(ball);

        world.step(FRAME);

        assertEquals(500f, ball.y, 0f);
    }
}
```

- [ ] **Step 2: Chạy test, xác nhận FAIL**

Run: `./gradlew :app:testDebugUnitTest --tests "vn.edu.tdtu.canvasdemo.physics.WorldTest"`
Expected: FAIL — `cannot find symbol: class World`

- [ ] **Step 3: Viết Body và Spring**

`app/src/main/java/vn/edu/tdtu/canvasdemo/physics/Body.java`:

```java
package vn.edu.tdtu.canvasdemo.physics;

/** Quả bóng tròn trong mô phỏng. Đơn vị: pixel và pixel/giây. */
public class Body {
    public float x;
    public float y;
    public float vx;
    public float vy;
    public final float radius;
    /** Khối lượng tỉ lệ diện tích (radius²). */
    public final float mass;
    public final int color;
    /** true khi đang bị ngón tay giữ: World không di chuyển nó và coi như khối lượng vô hạn. */
    public boolean held;

    public Body(float x, float y, float radius, int color) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
        this.mass = radius * radius;
    }

    float inverseMass() {
        return held ? 0f : 1f / mass;
    }
}
```

`app/src/main/java/vn/edu/tdtu/canvasdemo/physics/Spring.java`:

```java
package vn.edu.tdtu.canvasdemo.physics;

/** Lò xo nối một Body với điểm neo cố định (định luật Hooke + giảm chấn). */
public class Spring {
    public final Body body;
    public final float anchorX;
    public final float anchorY;
    public final float restLength;
    /** Hệ số k: lực = -k · độ giãn. */
    public final float stiffness;
    /** Hệ số c: lực cản = -c · vận tốc. */
    public final float damping;

    public Spring(Body body, float anchorX, float anchorY, float restLength, float stiffness, float damping) {
        this.body = body;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.restLength = restLength;
        this.stiffness = stiffness;
        this.damping = damping;
    }
}
```

- [ ] **Step 4: Viết World**

`app/src/main/java/vn/edu/tdtu/canvasdemo/physics/World.java`:

```java
package vn.edu.tdtu.canvasdemo.physics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Mô phỏng bóng tròn: trọng lực, nảy tường, va chạm đàn hồi, lò xo. */
public class World {
    /** dt tối đa mỗi lần step — tránh "bay" xuyên tường sau khi lag hoặc quay lại từ background. */
    public static final float MAX_DT = 1f / 30f;
    /** Va chạm chậm hơn ngưỡng này (px/s) không báo sự kiện — tránh spam khi bóng nằm yên. */
    public static final float MIN_IMPACT_SPEED = 150f;
    /** Chia mỗi frame thành nhiều bước nhỏ để bóng nhanh không xuyên qua nhau. */
    private static final int SUBSTEPS = 4;

    public interface CollisionListener {
        void onCollision(float x, float y, float impactSpeed);
    }

    private final List<Body> bodies = new ArrayList<>();
    private final List<Spring> springs = new ArrayList<>();
    private final int maxBodies;
    private float width;
    private float height;
    private float gravity = 2000f;
    private float restitution = 0.8f;
    private CollisionListener collisionListener;

    public World(float width, float height, int maxBodies) {
        this.width = width;
        this.height = height;
        this.maxBodies = maxBodies;
    }

    public void setBounds(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public void setGravity(float pxPerSec2) {
        gravity = pxPerSec2;
    }

    public void setRestitution(float e) {
        restitution = Math.max(0f, Math.min(1f, e));
    }

    public void setCollisionListener(CollisionListener listener) {
        collisionListener = listener;
    }

    /** @return false nếu đã đủ maxBodies. */
    public boolean addBody(Body body) {
        if (bodies.size() >= maxBodies) return false;
        bodies.add(body);
        return true;
    }

    /** Body của lò xo phải được addBody riêng. */
    public void addSpring(Spring spring) {
        springs.add(spring);
    }

    public void clear() {
        bodies.clear();
        springs.clear();
    }

    public List<Body> getBodies() {
        return Collections.unmodifiableList(bodies);
    }

    public List<Spring> getSprings() {
        return Collections.unmodifiableList(springs);
    }

    /** Bóng chứa điểm (x, y); duyệt ngược để lấy bóng vẽ sau cùng (nằm trên). */
    public Body findBodyAt(float x, float y) {
        for (int i = bodies.size() - 1; i >= 0; i--) {
            Body b = bodies.get(i);
            float dx = x - b.x;
            float dy = y - b.y;
            if (dx * dx + dy * dy <= b.radius * b.radius) return b;
        }
        return null;
    }

    public void step(float dt) {
        if (dt <= 0f) return;
        float h = Math.min(dt, MAX_DT) / SUBSTEPS;
        for (int i = 0; i < SUBSTEPS; i++) {
            applySprings(h);
            integrate(h);
            collideWithWalls();
            collideBodies();
        }
    }

    private void applySprings(float h) {
        for (Spring s : springs) {
            Body b = s.body;
            if (b.held) continue;
            float dx = b.x - s.anchorX;
            float dy = b.y - s.anchorY;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len == 0f) continue;
            float stretch = len - s.restLength;
            float fx = -s.stiffness * stretch * dx / len - s.damping * b.vx;
            float fy = -s.stiffness * stretch * dy / len - s.damping * b.vy;
            b.vx += fx / b.mass * h;
            b.vy += fy / b.mass * h;
        }
    }

    /** Euler bán ẩn: cập nhật vận tốc trước, rồi vị trí theo vận tốc mới (ổn định hơn Euler thường). */
    private void integrate(float h) {
        for (Body b : bodies) {
            if (b.held) continue;
            b.vy += gravity * h;
            b.x += b.vx * h;
            b.y += b.vy * h;
        }
    }

    private void collideWithWalls() {
        for (Body b : bodies) {
            if (b.held) continue;
            if (b.x < b.radius) {
                b.x = b.radius;
                if (b.vx < 0f) b.vx = bounceOffWall(b.vx, 0f, b.y);
            } else if (b.x > width - b.radius) {
                b.x = width - b.radius;
                if (b.vx > 0f) b.vx = bounceOffWall(b.vx, width, b.y);
            }
            if (b.y < b.radius) {
                b.y = b.radius;
                if (b.vy < 0f) b.vy = bounceOffWall(b.vy, b.x, 0f);
            } else if (b.y > height - b.radius) {
                b.y = height - b.radius;
                if (b.vy > 0f) b.vy = bounceOffWall(b.vy, b.x, height);
            }
        }
    }

    /** Đảo chiều vận tốc khi chạm tường; trả về vận tốc mới. */
    private float bounceOffWall(float velocity, float contactX, float contactY) {
        notifyCollision(contactX, contactY, Math.abs(velocity));
        return -velocity * restitution;
    }

    private void collideBodies() {
        for (int i = 0; i < bodies.size(); i++) {
            for (int j = i + 1; j < bodies.size(); j++) {
                resolve(bodies.get(i), bodies.get(j));
            }
        }
    }

    private void resolve(Body a, Body b) {
        float dx = b.x - a.x;
        float dy = b.y - a.y;
        float minDist = a.radius + b.radius;
        float distSq = dx * dx + dy * dy;
        if (distSq >= minDist * minDist) return;
        float invA = a.inverseMass();
        float invB = b.inverseMass();
        float invSum = invA + invB;
        if (invSum == 0f) return; // cả hai đang bị giữ

        float dist = (float) Math.sqrt(distSq);
        // Hai tâm trùng nhau: chọn pháp tuyến tùy ý để tránh chia cho 0
        float nx = 1f;
        float ny = 0f;
        if (dist > 0f) {
            nx = dx / dist;
            ny = dy / dist;
        }

        // Tách hai bóng theo tỉ lệ nghịch khối lượng
        float overlap = minDist - dist;
        a.x -= nx * overlap * invA / invSum;
        a.y -= ny * overlap * invA / invSum;
        b.x += nx * overlap * invB / invSum;
        b.y += ny * overlap * invB / invSum;

        // Chỉ tính xung lực khi hai bóng đang lao vào nhau
        float approach = (b.vx - a.vx) * nx + (b.vy - a.vy) * ny;
        if (approach >= 0f) return;
        float impulse = -(1f + restitution) * approach / invSum;
        a.vx -= impulse * invA * nx;
        a.vy -= impulse * invA * ny;
        b.vx += impulse * invB * nx;
        b.vy += impulse * invB * ny;
        notifyCollision(a.x + nx * a.radius, a.y + ny * a.radius, -approach);
    }

    private void notifyCollision(float x, float y, float speed) {
        if (collisionListener != null && speed >= MIN_IMPACT_SPEED) {
            collisionListener.onCollision(x, y, speed);
        }
    }
}
```

- [ ] **Step 5: Chạy test, xác nhận PASS**

Run: `./gradlew :app:testDebugUnitTest --tests "vn.edu.tdtu.canvasdemo.physics.WorldTest"`
Expected: PASS (12 tests)

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/vn/edu/tdtu/canvasdemo/physics app/src/test/java/vn/edu/tdtu/canvasdemo/physics
git commit -m "feat: add physics world with gravity, collisions and springs"
git pull --rebase && git push
```

---

### Task A2: Màn Physics

**Files:**
- Create: `app/src/main/java/vn/edu/tdtu/canvasdemo/physics/PhysicsView.java`
- Create: `app/src/main/res/layout/activity_physics.xml`
- Modify (ghi đè): `app/src/main/java/vn/edu/tdtu/canvasdemo/physics/PhysicsActivity.java`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `World`, `Body`, `Spring` (A1); `FrameClock`, `SystemBars`, `SeekBars`, `@string/clear` (Task 0.2).
- Produces: `PhysicsView.getWorld()`, `resume()`, `pause()`, `reset()`.

- [ ] **Step 1: Thêm chuỗi**

Trong `strings.xml`, thêm:

```xml
    <string name="physics_gravity">Trọng lực</string>
    <string name="physics_bounce">Độ nảy</string>
    <string name="physics_limit">Tối đa %1$d bóng</string>
```

- [ ] **Step 2: Viết PhysicsView**

`app/src/main/java/vn/edu/tdtu/canvasdemo/physics/PhysicsView.java`:

```java
package vn.edu.tdtu.canvasdemo.physics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.Random;

import vn.edu.tdtu.canvasdemo.R;
import vn.edu.tdtu.canvasdemo.core.FrameClock;

/** Chạm chỗ trống → tạo bóng; chạm vào bóng → giữ và kéo; thả → bóng rơi / con lắc dao động. */
public class PhysicsView extends View {
    public static final int MAX_BODIES = 50;
    private static final int[] PALETTE = {0xFFEF5350, 0xFFAB47BC, 0xFF42A5F5, 0xFF26A69A, 0xFFFFCA28, 0xFFFF7043};
    private static final int BACKGROUND = 0xFF263238;
    private static final int SPRING_COLOR = 0xFF90A4AE;
    private static final int SPRING_BALL_COLOR = 0xFF5C6BC0;
    private static final float MIN_RADIUS_DP = 12f;
    private static final float MAX_RADIUS_DP = 32f;
    private static final float SPRING_BALL_RADIUS_DP = 22f;
    private static final float SPRING_REST_DP = 120f;
    private static final float ANCHOR_Y_DP = 40f;
    private static final float ANCHOR_RADIUS_DP = 6f;
    private static final float SPRING_LINE_DP = 3f;
    // k = 40·m → chu kỳ dao động ≈ 1 s; c = 1.2·m → tắt dần sau vài giây
    private static final float SPRING_STIFFNESS_PER_MASS = 40f;
    private static final float SPRING_DAMPING_PER_MASS = 1.2f;

    private final World world = new World(1f, 1f, MAX_BODIES);
    private final FrameClock clock = new FrameClock();
    private final Random random = new Random();
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint springPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private Body heldBody;
    private boolean running;

    public PhysicsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        springPaint.setColor(SPRING_COLOR);
        springPaint.setStyle(Paint.Style.STROKE);
        springPaint.setStrokeWidth(SPRING_LINE_DP * density);
    }

    public World getWorld() {
        return world;
    }

    public void resume() {
        running = true;
        clock.reset();
        postInvalidateOnAnimation();
    }

    public void pause() {
        running = false;
    }

    /** Xóa hết bóng, giữ lại con lắc lò xo. */
    public void reset() {
        heldBody = null;
        world.clear();
        addSpringBall(getWidth());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        world.setBounds(w, h);
        if (world.getSprings().isEmpty()) addSpringBall(w);
    }

    private void addSpringBall(float viewWidth) {
        float anchorX = viewWidth * 0.75f;
        float anchorY = ANCHOR_Y_DP * density;
        float rest = SPRING_REST_DP * density;
        Body ball = new Body(anchorX, anchorY + rest, SPRING_BALL_RADIUS_DP * density, SPRING_BALL_COLOR);
        world.addBody(ball);
        world.addSpring(new Spring(ball, anchorX, anchorY, rest,
                SPRING_STIFFNESS_PER_MASS * ball.mass, SPRING_DAMPING_PER_MASS * ball.mass));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        world.step(clock.tick());
        canvas.drawColor(BACKGROUND);
        fillPaint.setColor(SPRING_COLOR);
        for (Spring s : world.getSprings()) {
            canvas.drawLine(s.anchorX, s.anchorY, s.body.x, s.body.y, springPaint);
            canvas.drawCircle(s.anchorX, s.anchorY, ANCHOR_RADIUS_DP * density, fillPaint);
        }
        for (Body b : world.getBodies()) {
            fillPaint.setColor(b.color);
            canvas.drawCircle(b.x, b.y, b.radius, fillPaint);
        }
        if (running) postInvalidateOnAnimation();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                heldBody = world.findBodyAt(x, y);
                if (heldBody != null) {
                    heldBody.held = true;
                    heldBody.vx = 0f;
                    heldBody.vy = 0f;
                } else {
                    spawnBall(x, y);
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (heldBody != null) {
                    heldBody.x = x;
                    heldBody.y = y;
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (heldBody != null) {
                    heldBody.held = false;
                    heldBody = null;
                }
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

    private void spawnBall(float x, float y) {
        float radius = (MIN_RADIUS_DP + random.nextFloat() * (MAX_RADIUS_DP - MIN_RADIUS_DP)) * density;
        int color = PALETTE[random.nextInt(PALETTE.length)];
        if (!world.addBody(new Body(x, y, radius, color))) {
            Toast.makeText(getContext(), getContext().getString(R.string.physics_limit, MAX_BODIES),
                    Toast.LENGTH_SHORT).show();
        }
    }
}
```

- [ ] **Step 3: Layout**

`app/src/main/res/layout/activity_physics.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/main"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <vn.edu.tdtu.canvasdemo.physics.PhysicsView
        android:id="@+id/physics_view"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="16dp"
        android:layout_marginTop="8dp"
        android:text="@string/physics_gravity" />

    <SeekBar
        android:id="@+id/seek_gravity"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:max="2000"
        android:progress="800" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="16dp"
        android:text="@string/physics_bounce" />

    <SeekBar
        android:id="@+id/seek_bounce"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:max="100"
        android:progress="80" />

    <Button
        android:id="@+id/btn_clear"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="end"
        android:layout_margin="8dp"
        android:text="@string/clear" />
</LinearLayout>
```

- [ ] **Step 4: Ghi đè PhysicsActivity**

`app/src/main/java/vn/edu/tdtu/canvasdemo/physics/PhysicsActivity.java`:

```java
package vn.edu.tdtu.canvasdemo.physics;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import vn.edu.tdtu.canvasdemo.R;
import vn.edu.tdtu.canvasdemo.core.SeekBars;
import vn.edu.tdtu.canvasdemo.core.SystemBars;

public class PhysicsActivity extends AppCompatActivity {
    private PhysicsView physicsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_physics);
        SystemBars.applyPadding(findViewById(R.id.main));

        physicsView = findViewById(R.id.physics_view);
        World world = physicsView.getWorld();
        float density = getResources().getDisplayMetrics().density;
        // Thanh trượt theo dp/s² để tốc độ rơi trông giống nhau trên mọi màn hình
        SeekBars.onProgress(findViewById(R.id.seek_gravity), p -> world.setGravity(p * density));
        SeekBars.onProgress(findViewById(R.id.seek_bounce), p -> world.setRestitution(p / 100f));
        findViewById(R.id.btn_clear).setOnClickListener(v -> physicsView.reset());
    }

    @Override
    protected void onResume() {
        super.onResume();
        physicsView.resume();
    }

    @Override
    protected void onPause() {
        physicsView.pause();
        super.onPause();
    }
}
```

- [ ] **Step 5: Kiểm tra trên máy thật**

Run: `./gradlew :app:assembleDebug` → `BUILD SUCCESSFUL`, rồi chạy app → Home → Physics. Đối chiếu:
- Con lắc lò xo treo ở góc phải trên, đứng yên hơi giãn.
- Chạm chỗ trống → bóng mới rơi, nảy ở sàn, nảy thấp dần.
- Tạo ~10 bóng: chúng va vào nhau, bóng to đẩy bóng nhỏ, không chồng lên nhau.
- Kéo bóng con lắc xuống rồi thả → dao động tắt dần.
- Kéo "Trọng lực" về 0 → bóng lơ lửng; "Độ nảy" về 0 → bóng không nảy.
- Chạm 2 lần đúng một chỗ → hai bóng tách nhau, không biến mất.
- Tạo đến 50 bóng → hiện Toast "Tối đa 50 bóng".
- Bấm Home của máy, chờ 5 giây, quay lại → bóng không bay xuyên tường.
- "Xóa hết" → chỉ còn con lắc.

- [ ] **Step 6: Commit**

```bash
git add app/src/main
git commit -m "feat: add physics screen with spawnable balls and spring pendulum"
git pull --rebase && git push
```

---

### Task A3: Màn Gesture

**Files:**
- Create: `app/src/main/java/vn/edu/tdtu/canvasdemo/gesture/GestureMath.java`
- Create: `app/src/main/java/vn/edu/tdtu/canvasdemo/gesture/GestureView.java`
- Create: `app/src/main/res/layout/activity_gesture.xml`
- Modify (ghi đè): `app/src/main/java/vn/edu/tdtu/canvasdemo/gesture/GestureActivity.java`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/vn/edu/tdtu/canvasdemo/gesture/GestureMathTest.java`

**Interfaces:**
- Consumes: `SystemBars` (Task 0.2); `androidx.dynamicanimation` (Task 0.1).
- Produces: `GestureMath.clamp(float v, float min, float max)`, `GestureMath.centerRange(float viewSize, float halfExtent)` → `float[]{min, max}` (package-private, chỉ GestureView dùng).

- [ ] **Step 1: Viết test**

`app/src/test/java/vn/edu/tdtu/canvasdemo/gesture/GestureMathTest.java`:

```java
package vn.edu.tdtu.canvasdemo.gesture;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GestureMathTest {
    @Test
    public void centerRangeKeepsCardInsideView() {
        assertArrayEquals(new float[]{100f, 900f}, GestureMath.centerRange(1000f, 100f), 0f);
    }

    @Test
    public void centerRangeLocksToCenterWhenCardBiggerThanView() {
        assertArrayEquals(new float[]{500f, 500f}, GestureMath.centerRange(1000f, 600f), 0f);
    }

    @Test
    public void clampPullsValueIntoRange() {
        assertEquals(100f, GestureMath.clamp(-50f, 100f, 900f), 0f);
        assertEquals(900f, GestureMath.clamp(5000f, 100f, 900f), 0f);
        assertEquals(400f, GestureMath.clamp(400f, 100f, 900f), 0f);
    }
}
```

- [ ] **Step 2: Chạy test, xác nhận FAIL**

Run: `./gradlew :app:testDebugUnitTest --tests "vn.edu.tdtu.canvasdemo.gesture.GestureMathTest"`
Expected: FAIL — `cannot find symbol: variable GestureMath`

- [ ] **Step 3: Viết GestureMath**

`app/src/main/java/vn/edu/tdtu/canvasdemo/gesture/GestureMath.java`:

```java
package vn.edu.tdtu.canvasdemo.gesture;

final class GestureMath {
    private GestureMath() {
    }

    static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Khoảng [min, max] cho tâm thẻ để thẻ nằm trọn trong view.
     * Thẻ to hơn view thì khóa ở giữa — FlingAnimation cần min ≤ giá trị đầu ≤ max.
     */
    static float[] centerRange(float viewSize, float halfExtent) {
        if (2f * halfExtent >= viewSize) {
            return new float[]{viewSize / 2f, viewSize / 2f};
        }
        return new float[]{halfExtent, viewSize - halfExtent};
    }
}
```

- [ ] **Step 4: Chạy test, xác nhận PASS**

Run: `./gradlew :app:testDebugUnitTest --tests "vn.edu.tdtu.canvasdemo.gesture.GestureMathTest"`
Expected: PASS (3 tests)

- [ ] **Step 5: Thêm chuỗi**

```xml
    <string name="gesture_hint_top">Kéo · Vuốt · Pinch</string>
    <string name="gesture_hint_bottom">Chạm đúp để về giữa</string>
```

- [ ] **Step 6: Viết GestureView**

`app/src/main/java/vn/edu/tdtu/canvasdemo/gesture/GestureView.java`:

```java
package vn.edu.tdtu.canvasdemo.gesture;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FlingAnimation;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import vn.edu.tdtu.canvasdemo.R;

/** Thẻ: kéo để di chuyển, vuốt để trượt có ma sát, pinch để phóng to, chạm đúp để bật lò xo về giữa. */
public class GestureView extends View {
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 3f;
    private static final float FLING_FRICTION = 1.1f;
    private static final float CARD_WIDTH_DP = 200f;
    private static final float CARD_HEIGHT_DP = 280f;
    private static final float CORNER_DP = 24f;
    private static final float TEXT_SP = 18f;
    private static final int BACKGROUND = 0xFFECEFF1;
    private static final int CARD_START = 0xFF7E57C2;
    private static final int CARD_END = 0xFF42A5F5;

    private static final FloatPropertyCompat<GestureView> CARD_X = new FloatPropertyCompat<GestureView>("cardX") {
        @Override
        public float getValue(GestureView view) {
            return view.cardX;
        }

        @Override
        public void setValue(GestureView view, float value) {
            view.cardX = value;
            view.invalidate();
        }
    };

    private static final FloatPropertyCompat<GestureView> CARD_Y = new FloatPropertyCompat<GestureView>("cardY") {
        @Override
        public float getValue(GestureView view) {
            return view.cardY;
        }

        @Override
        public void setValue(GestureView view, float value) {
            view.cardY = value;
            view.invalidate();
        }
    };

    private static final FloatPropertyCompat<GestureView> CARD_SCALE = new FloatPropertyCompat<GestureView>("cardScale") {
        @Override
        public float getValue(GestureView view) {
            return view.cardScale;
        }

        @Override
        public void setValue(GestureView view, float value) {
            view.cardScale = value;
            view.invalidate();
        }
    };

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF cardRect = new RectF();
    private final float halfWidth;
    private final float halfHeight;
    private final float corner;
    private final float lineHeight;
    private final String hintTop;
    private final String hintBottom;
    private final FlingAnimation flingX;
    private final FlingAnimation flingY;
    private final SpringAnimation springX;
    private final SpringAnimation springY;
    private final SpringAnimation springScale;
    private final GestureDetector gestureDetector;
    private final ScaleGestureDetector scaleDetector;
    private float cardX;
    private float cardY;
    private float cardScale = 1f;

    public GestureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        halfWidth = CARD_WIDTH_DP * density / 2f;
        halfHeight = CARD_HEIGHT_DP * density / 2f;
        corner = CORNER_DP * density;
        cardRect.set(-halfWidth, -halfHeight, halfWidth, halfHeight);
        // Gradient đặt theo tọa độ của thẻ nên di chuyển/phóng to cùng thẻ
        cardPaint.setShader(new LinearGradient(-halfWidth, -halfHeight, halfWidth, halfHeight,
                CARD_START, CARD_END, Shader.TileMode.CLAMP));
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, TEXT_SP,
                getResources().getDisplayMetrics()));
        lineHeight = textPaint.getTextSize() * 1.4f;
        hintTop = context.getString(R.string.gesture_hint_top);
        hintBottom = context.getString(R.string.gesture_hint_bottom);

        flingX = new FlingAnimation(this, CARD_X).setFriction(FLING_FRICTION);
        flingY = new FlingAnimation(this, CARD_Y).setFriction(FLING_FRICTION);
        springX = new SpringAnimation(this, CARD_X).setSpring(bouncySpring());
        springY = new SpringAnimation(this, CARD_Y).setSpring(bouncySpring());
        springScale = new SpringAnimation(this, CARD_SCALE).setSpring(bouncySpring());
        springScale.setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_SCALE);

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                cancelAnimations();
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                cardX -= distanceX;
                cardY -= distanceY;
                invalidate();
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                startFling(velocityX, velocityY);
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                springBack();
                return true;
            }
        });

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                cardScale = GestureMath.clamp(cardScale * detector.getScaleFactor(), MIN_SCALE, MAX_SCALE);
                invalidate();
                return true;
            }
        });
    }

    private static SpringForce bouncySpring() {
        return new SpringForce()
                .setStiffness(SpringForce.STIFFNESS_LOW)
                .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        cardX = w / 2f;
        cardY = h / 2f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(BACKGROUND);
        canvas.save();
        canvas.translate(cardX, cardY);
        canvas.scale(cardScale, cardScale);
        canvas.drawRoundRect(cardRect, corner, corner, cardPaint);
        canvas.drawText(hintTop, 0f, -lineHeight / 2f, textPaint);
        canvas.drawText(hintBottom, 0f, lineHeight / 2f, textPaint);
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        scaleDetector.onTouchEvent(e);
        gestureDetector.onTouchEvent(e);
        if (e.getActionMasked() == MotionEvent.ACTION_UP) performClick();
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelAnimations();
        super.onDetachedFromWindow();
    }

    private void startFling(float velocityX, float velocityY) {
        float[] xRange = GestureMath.centerRange(getWidth(), halfWidth * cardScale);
        float[] yRange = GestureMath.centerRange(getHeight(), halfHeight * cardScale);
        // FlingAnimation ném IllegalArgumentException nếu giá trị đầu nằm ngoài [min, max]
        cardX = GestureMath.clamp(cardX, xRange[0], xRange[1]);
        cardY = GestureMath.clamp(cardY, yRange[0], yRange[1]);
        flingX.setMinValue(xRange[0]).setMaxValue(xRange[1]).setStartVelocity(velocityX).start();
        flingY.setMinValue(yRange[0]).setMaxValue(yRange[1]).setStartVelocity(velocityY).start();
    }

    private void springBack() {
        cancelAnimations();
        springX.animateToFinalPosition(getWidth() / 2f);
        springY.animateToFinalPosition(getHeight() / 2f);
        springScale.animateToFinalPosition(1f);
    }

    private void cancelAnimations() {
        flingX.cancel();
        flingY.cancel();
        springX.cancel();
        springY.cancel();
        springScale.cancel();
    }
}
```

- [ ] **Step 7: Layout và Activity**

`app/src/main/res/layout/activity_gesture.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/main"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <vn.edu.tdtu.canvasdemo.gesture.GestureView
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
</FrameLayout>
```

Ghi đè `app/src/main/java/vn/edu/tdtu/canvasdemo/gesture/GestureActivity.java`:

```java
package vn.edu.tdtu.canvasdemo.gesture;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import vn.edu.tdtu.canvasdemo.R;
import vn.edu.tdtu.canvasdemo.core.SystemBars;

public class GestureActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gesture);
        SystemBars.applyPadding(findViewById(R.id.main));
    }
}
```

- [ ] **Step 8: Kiểm tra trên máy thật**

Run: `./gradlew :app:assembleDebug` → `BUILD SUCCESSFUL`. Home → Gesture, đối chiếu:
- Thẻ gradient tím–xanh nằm giữa màn.
- Kéo → thẻ theo ngón tay.
- Vuốt mạnh → thẻ trượt, chậm dần, dừng ở mép màn.
- Hai ngón → phóng to/thu nhỏ, không nhỏ hơn 0.5× và không to hơn 3×.
- Chạm đúp → thẻ bật về giữa, nảy nhẹ, kích thước về 1×.
- Kéo thẻ ra nửa ngoài màn rồi vuốt → thẻ trượt vào trong, **không crash**.
- Phóng to 3× rồi vuốt → không crash.

- [ ] **Step 9: Commit**

```bash
git add app/src/main app/src/test/java/vn/edu/tdtu/canvasdemo/gesture
git commit -m "feat: add gesture screen with drag, fling, pinch and spring back"
git pull --rebase && git push
```

---

### Task A4: Hệ thống hạt (`Particle`, `Emitter`, `ParticlePalette`)

**Files:**
- Create: `app/src/main/java/vn/edu/tdtu/canvasdemo/particle/Particle.java`
- Create: `app/src/main/java/vn/edu/tdtu/canvasdemo/particle/Emitter.java`
- Create: `app/src/main/java/vn/edu/tdtu/canvasdemo/particle/ParticlePalette.java`
- Test: `app/src/test/java/vn/edu/tdtu/canvasdemo/particle/EmitterTest.java`

**Interfaces:**
- Consumes: không có.
- Produces (A5 và **B6 Playground** dùng — giữ ổn định sau mốc 26/10):
  - `Particle`: field public `x, y, vx, vy, age, life, size`, `boolean alive`, `boolean splash`, `Emitter.Mode mode`; `float progress()` (0..1).
  - `enum Emitter.Mode { FIRE, WATER }`, `Emitter.MAX_DT = 1f/30f`
  - `Emitter(int capacity, float unit, Random random)` — `unit` = số pixel cho 1 dp (truyền `density`; test truyền `1f`).
  - `void setPosition(float x, float y)`, `void setMode(Mode m)`, `Mode getMode()`, `void setFloor(float y)`, `void setEmitting(boolean on)`
  - `void update(float dt)`, `void burst(float x, float y, int count)` (bung hạt lửa tỏa tròn)
  - `int capacity()`, `Particle get(int index)`, `int aliveCount()`
  - `ParticlePalette.fire(float t)`, `ParticlePalette.water(float t)` → màu ARGB theo tuổi.

- [ ] **Step 1: Viết test**

`app/src/test/java/vn/edu/tdtu/canvasdemo/particle/EmitterTest.java`:

```java
package vn.edu.tdtu.canvasdemo.particle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

public class EmitterTest {
    private static final float FRAME = 1f / 60f;

    private static Emitter fireEmitter(int capacity) {
        return new Emitter(capacity, 1f, new Random(42));
    }

    private static int countAlive(Emitter emitter) {
        int n = 0;
        for (int i = 0; i < emitter.capacity(); i++) {
            if (emitter.get(i).alive) n++;
        }
        return n;
    }

    @Test
    public void fireSpawnsAtConfiguredRate() {
        Emitter emitter = fireEmitter(2000);

        for (int i = 0; i < 6; i++) emitter.update(FRAME); // 0.1 s × 300 hạt/s

        assertEquals(30, emitter.aliveCount());
    }

    @Test
    public void neverExceedsPoolCapacity() {
        Emitter emitter = fireEmitter(50);

        for (int i = 0; i < 120; i++) emitter.update(FRAME);

        assertTrue(emitter.aliveCount() <= 50);
        assertTrue(emitter.aliveCount() > 0);
    }

    @Test
    public void particlesDieAfterLifetime() {
        Emitter emitter = fireEmitter(2000);
        for (int i = 0; i < 6; i++) emitter.update(FRAME);
        emitter.setEmitting(false);

        for (int i = 0; i < 90; i++) emitter.update(FRAME); // 1.5 s > tuổi thọ tối đa của lửa

        assertEquals(0, emitter.aliveCount());
    }

    @Test
    public void waterSplashesWhenHittingFloor() {
        Emitter emitter = fireEmitter(2000);
        emitter.setMode(Emitter.Mode.WATER);
        emitter.setPosition(0f, 0f);
        emitter.setFloor(10f);
        boolean sawSplash = false;

        for (int i = 0; i < 120 && !sawSplash; i++) {
            emitter.update(FRAME);
            for (int j = 0; j < emitter.capacity(); j++) {
                Particle p = emitter.get(j);
                if (p.alive && p.splash) sawSplash = true;
            }
        }

        assertTrue(sawSplash);
    }

    @Test
    public void aliveCountMatchesPool() {
        Emitter emitter = fireEmitter(2000);
        emitter.setMode(Emitter.Mode.WATER);
        emitter.setFloor(10f);

        for (int i = 0; i < 150; i++) emitter.update(FRAME);

        assertEquals(countAlive(emitter), emitter.aliveCount());
    }

    @Test
    public void burstSpawnsExactCount() {
        Emitter emitter = fireEmitter(2000);
        emitter.setEmitting(false);

        emitter.burst(100f, 100f, 20);

        assertEquals(20, emitter.aliveCount());
    }

    @Test
    public void hugeDtIsClampedToMaxDt() {
        Emitter slow = new Emitter(2000, 1f, new Random(7));
        Emitter fast = new Emitter(2000, 1f, new Random(7));

        slow.update(5f);
        fast.update(Emitter.MAX_DT);

        assertEquals(fast.aliveCount(), slow.aliveCount());
        assertEquals(fast.get(0).y, slow.get(0).y, 1e-4f);
    }
}
```

- [ ] **Step 2: Chạy test, xác nhận FAIL**

Run: `./gradlew :app:testDebugUnitTest --tests "vn.edu.tdtu.canvasdemo.particle.EmitterTest"`
Expected: FAIL — `cannot find symbol: class Emitter`

- [ ] **Step 3: Viết Particle**

`app/src/main/java/vn/edu/tdtu/canvasdemo/particle/Particle.java`:

```java
package vn.edu.tdtu.canvasdemo.particle;

/** Một hạt trong pool. Field để public vì View đọc chúng mỗi frame; chỉ Emitter được ghi. */
public final class Particle {
    public float x;
    public float y;
    public float vx;
    public float vy;
    public float age;
    public float life;
    public float size;
    public boolean alive;
    /** Giọt nước bắn tóe: không sinh tiếp giọt khi chạm đáy. */
    public boolean splash;
    public Emitter.Mode mode;

    /** Tuổi tương đối 0..1 — dùng để đổi màu, độ mờ, kích thước. */
    public float progress() {
        return Math.min(1f, age / life);
    }
}
```

- [ ] **Step 4: Viết Emitter**

`app/src/main/java/vn/edu/tdtu/canvasdemo/particle/Emitter.java`:

```java
package vn.edu.tdtu.canvasdemo.particle;

import java.util.Random;

/**
 * Nguồn phát hạt dùng object pool kích thước cố định:
 * hạt chết được tái sử dụng, không tạo object mới mỗi frame → không bị GC làm giật.
 * Hằng số tính theo dp, nhân với `unit` (px/dp).
 */
public class Emitter {
    public enum Mode { FIRE, WATER }

    public static final float MAX_DT = 1f / 30f;

    private static final float FIRE_RATE = 300f;      // hạt/giây
    private static final float WATER_RATE = 250f;
    private static final float FIRE_LIFT = 120f;      // dp/s² hướng lên
    private static final float WATER_GRAVITY = 1400f; // dp/s² hướng xuống
    private static final int SPLASH_DROPS = 3;

    private final Particle[] pool;
    private final float unit;
    private final Random random;
    private Mode mode = Mode.FIRE;
    private float x;
    private float y;
    private float floorY = Float.MAX_VALUE;
    private boolean emitting = true;
    private float spawnBudget;
    private int nextFree;
    private int aliveCount;

    public Emitter(int capacity, float unit, Random random) {
        pool = new Particle[capacity];
        for (int i = 0; i < capacity; i++) pool[i] = new Particle();
        this.unit = unit;
        this.random = random;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Mode getMode() {
        return mode;
    }

    public void setFloor(float floorY) {
        this.floorY = floorY;
    }

    public void setEmitting(boolean emitting) {
        this.emitting = emitting;
    }

    public int capacity() {
        return pool.length;
    }

    public Particle get(int index) {
        return pool[index];
    }

    public int aliveCount() {
        return aliveCount;
    }

    public void update(float dt) {
        if (dt <= 0f) return;
        dt = Math.min(dt, MAX_DT);
        if (emitting) {
            spawnBudget += (mode == Mode.FIRE ? FIRE_RATE : WATER_RATE) * dt;
            while (spawnBudget >= 1f) {
                spawnBudget -= 1f;
                if (!spawn(mode, x, y, false)) {
                    spawnBudget = 0f; // pool đầy: bỏ phần còn lại của frame này
                    break;
                }
            }
        }
        for (Particle p : pool) {
            if (p.alive) step(p, dt);
        }
    }

    /** Bung `count` hạt lửa tỏa tròn tại (x, y) — Playground dùng khi có va chạm. */
    public void burst(float x, float y, int count) {
        for (int i = 0; i < count; i++) {
            Particle p = obtain();
            if (p == null) return;
            double angle = random.nextDouble() * 2 * Math.PI;
            float speed = range(80f, 260f) * unit;
            init(p, Mode.FIRE, x, y, (float) Math.cos(angle) * speed, (float) Math.sin(angle) * speed,
                    range(0.3f, 0.6f), range(4f, 8f) * unit, false);
        }
    }

    private void step(Particle p, float dt) {
        p.age += dt;
        if (p.age >= p.life) {
            kill(p);
            return;
        }
        if (p.mode == Mode.FIRE) {
            p.vy -= FIRE_LIFT * unit * dt;
        } else {
            p.vy += WATER_GRAVITY * unit * dt;
        }
        p.x += p.vx * dt;
        p.y += p.vy * dt;
        if (p.mode == Mode.WATER && !p.splash && p.y >= floorY) {
            kill(p);
            for (int i = 0; i < SPLASH_DROPS; i++) spawn(Mode.WATER, p.x, floorY, true);
        }
    }

    /** @return false nếu pool đã đầy. */
    private boolean spawn(Mode m, float px, float py, boolean splash) {
        Particle p = obtain();
        if (p == null) return false;
        if (splash) {
            init(p, Mode.WATER, px, py, range(-150f, 150f) * unit, -range(150f, 300f) * unit,
                    range(0.3f, 0.5f), range(2f, 4f) * unit, true);
        } else if (m == Mode.FIRE) {
            init(p, Mode.FIRE, px + range(-12f, 12f) * unit, py, range(-40f, 40f) * unit,
                    -range(150f, 300f) * unit, range(0.6f, 1.2f), range(14f, 24f) * unit, false);
        } else {
            init(p, Mode.WATER, px, py, range(-120f, 120f) * unit, -range(700f, 900f) * unit,
                    3f, range(4f, 7f) * unit, false);
        }
        return true;
    }

    /** Tìm hạt chết theo vòng tròn bắt đầu từ vị trí lần trước; null nếu pool đầy. */
    private Particle obtain() {
        for (int i = 0; i < pool.length; i++) {
            int index = (nextFree + i) % pool.length;
            if (!pool[index].alive) {
                nextFree = (index + 1) % pool.length;
                return pool[index];
            }
        }
        return null;
    }

    private void init(Particle p, Mode m, float px, float py, float vx, float vy,
                      float life, float size, boolean splash) {
        p.mode = m;
        p.x = px;
        p.y = py;
        p.vx = vx;
        p.vy = vy;
        p.age = 0f;
        p.life = life;
        p.size = size;
        p.splash = splash;
        p.alive = true;
        aliveCount++;
    }

    private void kill(Particle p) {
        p.alive = false;
        aliveCount--;
    }

    private float range(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }
}
```

- [ ] **Step 5: Chạy test, xác nhận PASS**

Run: `./gradlew :app:testDebugUnitTest --tests "vn.edu.tdtu.canvasdemo.particle.EmitterTest"`
Expected: PASS (7 tests)

- [ ] **Step 6: Viết ParticlePalette**

`app/src/main/java/vn/edu/tdtu/canvasdemo/particle/ParticlePalette.java`:

```java
package vn.edu.tdtu.canvasdemo.particle;

import android.graphics.Color;

public final class ParticlePalette {
    private ParticlePalette() {
    }

    /** Lửa theo tuổi t (0..1): vàng → cam → đỏ, mờ dần. */
    public static int fire(float t) {
        float k = 1f - t;
        return Color.argb((int) (255 * k), 255, (int) (220 * k), (int) (60 * k * k));
    }

    /** Nước: xanh, mờ dần nhẹ theo tuổi. */
    public static int water(float t) {
        return Color.argb((int) (220 * (1f - 0.7f * t)), 90, 170, 255);
    }
}
```

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/vn/edu/tdtu/canvasdemo/particle app/src/test/java/vn/edu/tdtu/canvasdemo/particle
git commit -m "feat: add pooled particle emitter for fire and water"
git pull --rebase && git push
```

---

### Task A5: Màn Particle

**Files:**
- Create: `app/src/main/java/vn/edu/tdtu/canvasdemo/particle/ParticleView.java`
- Create: `app/src/main/res/layout/activity_particle.xml`
- Modify (ghi đè): `app/src/main/java/vn/edu/tdtu/canvasdemo/particle/ParticleActivity.java`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `Emitter`, `Particle`, `ParticlePalette` (A4); `FrameClock`, `SystemBars` (Task 0.2).
- Produces: `ParticleView.setMode(Emitter.Mode)`, `resume()`, `pause()`.

- [ ] **Step 1: Thêm chuỗi**

```xml
    <string name="particle_fire">Lửa</string>
    <string name="particle_water">Nước</string>
    <string name="particle_hud">FPS %1$d · %2$d hạt</string>
```

- [ ] **Step 2: Viết ParticleView**

`app/src/main/java/vn/edu/tdtu/canvasdemo/particle/ParticleView.java`:

```java
package vn.edu.tdtu.canvasdemo.particle;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;

import vn.edu.tdtu.canvasdemo.R;
import vn.edu.tdtu.canvasdemo.core.FrameClock;

/** Vẽ hạt lửa/nước; kéo ngón tay để di chuyển nguồn phát. */
public class ParticleView extends View {
    private static final int CAPACITY = 2000;
    private static final float HUD_TEXT_SP = 14f;
    private static final float HUD_MARGIN_DP = 16f;
    private static final float FIRE_SHRINK = 0.6f;
    private static final float FPS_SMOOTHING = 0.1f;
    private static final int FLOOR_COLOR = 0xFF37474F;

    private final Emitter emitter;
    private final FrameClock clock = new FrameClock();
    private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint floorPaint = new Paint();
    // Cộng sáng: hạt chồng lên nhau sáng hơn — tạo lõi lửa rực
    private final PorterDuffXfermode additive = new PorterDuffXfermode(PorterDuff.Mode.ADD);
    private final float hudMargin;
    private float floorY;
    private float fps;
    private boolean running;

    public ParticleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        emitter = new Emitter(CAPACITY, density, new Random());
        hudMargin = HUD_MARGIN_DP * density;
        hudPaint.setColor(Color.WHITE);
        hudPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, HUD_TEXT_SP,
                getResources().getDisplayMetrics()));
        floorPaint.setColor(FLOOR_COLOR);
        floorPaint.setStrokeWidth(2f * density);
    }

    public void setMode(Emitter.Mode mode) {
        emitter.setMode(mode);
    }

    public void resume() {
        running = true;
        clock.reset();
        postInvalidateOnAnimation();
    }

    public void pause() {
        running = false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        floorY = h * 0.92f;
        emitter.setFloor(floorY);
        emitter.setPosition(w / 2f, h * 0.75f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float dt = clock.tick();
        emitter.update(dt);
        if (dt > 0f) fps = fps == 0f ? 1f / dt : fps + FPS_SMOOTHING * (1f / dt - fps);

        canvas.drawColor(Color.BLACK);
        if (emitter.getMode() == Emitter.Mode.WATER) {
            canvas.drawLine(0f, floorY, getWidth(), floorY, floorPaint);
        }
        for (int i = 0; i < emitter.capacity(); i++) {
            Particle p = emitter.get(i);
            if (!p.alive) continue;
            float t = p.progress();
            if (p.mode == Emitter.Mode.FIRE) {
                particlePaint.setXfermode(additive);
                particlePaint.setColor(ParticlePalette.fire(t));
                canvas.drawCircle(p.x, p.y, p.size * (1f - FIRE_SHRINK * t), particlePaint);
            } else {
                particlePaint.setXfermode(null);
                particlePaint.setColor(ParticlePalette.water(t));
                canvas.drawCircle(p.x, p.y, p.size, particlePaint);
            }
        }
        canvas.drawText(getContext().getString(R.string.particle_hud, Math.round(fps), emitter.aliveCount()),
                hudMargin, hudMargin + hudPaint.getTextSize(), hudPaint);
        if (running) postInvalidateOnAnimation();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                emitter.setPosition(e.getX(), e.getY());
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
}
```

- [ ] **Step 3: Layout**

`app/src/main/res/layout/activity_particle.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/main"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <vn.edu.tdtu.canvasdemo.particle.ParticleView
        android:id="@+id/particle_view"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />

    <com.google.android.material.button.MaterialButtonToggleGroup
        android:id="@+id/mode_group"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center_horizontal"
        android:layout_margin="8dp"
        app:checkedButton="@+id/btn_fire"
        app:selectionRequired="true"
        app:singleSelection="true">

        <Button
            android:id="@+id/btn_fire"
            style="?attr/materialButtonOutlinedStyle"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/particle_fire" />

        <Button
            android:id="@+id/btn_water"
            style="?attr/materialButtonOutlinedStyle"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/particle_water" />
    </com.google.android.material.button.MaterialButtonToggleGroup>
</LinearLayout>
```

- [ ] **Step 4: Ghi đè ParticleActivity**

`app/src/main/java/vn/edu/tdtu/canvasdemo/particle/ParticleActivity.java`:

```java
package vn.edu.tdtu.canvasdemo.particle;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButtonToggleGroup;

import vn.edu.tdtu.canvasdemo.R;
import vn.edu.tdtu.canvasdemo.core.SystemBars;

public class ParticleActivity extends AppCompatActivity {
    private ParticleView particleView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_particle);
        SystemBars.applyPadding(findViewById(R.id.main));

        particleView = findViewById(R.id.particle_view);
        MaterialButtonToggleGroup modeGroup = findViewById(R.id.mode_group);
        modeGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            particleView.setMode(checkedId == R.id.btn_water ? Emitter.Mode.WATER : Emitter.Mode.FIRE);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        particleView.resume();
    }

    @Override
    protected void onPause() {
        particleView.pause();
        super.onPause();
    }
}
```

- [ ] **Step 5: Kiểm tra trên máy thật**

Run: `./gradlew :app:assembleDebug` → `BUILD SUCCESSFUL`. Home → Particle, đối chiếu:
- Mặc định là Lửa: ngọn lửa vàng ở giữa-dưới, đỉnh chuyển đỏ và mờ dần, lõi sáng hơn.
- Kéo ngón tay → nguồn lửa đi theo.
- Chọn Nước → vòi phun lên, rơi xuống, chạm vạch sàn bắn tóe giọt nhỏ.
- Góc trên trái hiện FPS (~60 trên máy tầm trung) và số hạt (không vượt 2000).
- Chuyển Lửa ↔ Nước liên tục 10 lần → không crash, hạt cũ tàn đúng màu của nó.
- Ra Home của máy 5 giây rồi quay lại → không có "vụ nổ" hạt.

- [ ] **Step 6: Commit**

```bash
git add app/src/main
git commit -m "feat: add particle screen with fire and water modes"
git pull --rebase && git push
```

---

# TRACK B — "Vẽ & UI" (B làm)

### Task B1: Toán của chart (`ChartScale`, `ChartMath`)

**Files:**
- Create: `app/src/main/java/vn/edu/tdtu/canvasdemo/chart/ChartScale.java`
- Create: `app/src/main/java/vn/edu/tdtu/canvasdemo/chart/ChartMath.java`
- Test: `app/src/test/java/vn/edu/tdtu/canvasdemo/chart/ChartScaleTest.java`
- Test: `app/src/test/java/vn/edu/tdtu/canvasdemo/chart/ChartMathTest.java`

**Interfaces:**
- Consumes: không có.
- Produces:
  - `ChartScale(float min, float max, float pixelStart, float pixelEnd)` (max ≤ min → tự nới thành min + 1); `float toPixel(float value)`; `float tickValue(int index, int tickCount)`.
  - `ChartMath.sweepAngles(float[] values)` → `float[]` độ, tổng 360 (giá trị âm coi như 0; tổng ≤ 0 → toàn 0).
  - `ChartMath.angleFromTop(float dx, float dy)` → độ 0..360, 0 = 12 giờ, theo chiều kim đồng hồ (trục y hướng xuống).
  - `ChartMath.sliceAt(float angleDeg, float[] sweeps)` → chỉ số phần, −1 nếu không có.
  - `ChartMath.nearestIndex(float x, float start, float step, int count)` → chỉ số gần nhất (kẹp trong 0..count−1), −1 nếu count ≤ 0.

- [ ] **Step 1: Viết test**

`app/src/test/java/vn/edu/tdtu/canvasdemo/chart/ChartScaleTest.java`:

```java
package vn.edu.tdtu.canvasdemo.chart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ChartScaleTest {
    // Trục Y: giá trị 0 ở đáy (pixel 200), 100 ở đỉnh (pixel 0)
    private final ChartScale scale = new ChartScale(0f, 100f, 200f, 0f);

    @Test
    public void minMapsToStartAndMaxMapsToEnd() {
        assertEquals(200f, scale.toPixel(0f), 1e-4f);
        assertEquals(0f, scale.toPixel(100f), 1e-4f);
    }

    @Test
    public void middleValueMapsToMiddlePixel() {
        assertEquals(100f, scale.toPixel(50f), 1e-4f);
    }

    @Test
    public void flatDataDoesNotDivideByZero() {
        ChartScale flat = new ChartScale(42f, 42f, 200f, 0f);
        float px = flat.toPixel(42f);
        assertFalse(Float.isNaN(px) || Float.isInfinite(px));
    }

    @Test
    public void tickValuesAreEvenlySpaced() {
        assertEquals(0f, scale.tickValue(0, 5), 1e-4f);
        assertEquals(50f, scale.tickValue(2, 5), 1e-4f);
        assertEquals(100f, scale.tickValue(4, 5), 1e-4f);
        assertEquals(0f, scale.tickValue(0, 1), 1e-4f);
    }
}
```

`app/src/test/java/vn/edu/tdtu/canvasdemo/chart/ChartMathTest.java`:

```java
package vn.edu.tdtu.canvasdemo.chart;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ChartMathTest {
    @Test
    public void sweepsAreProportionalAndSumTo360() {
        assertArrayEquals(new float[]{90f, 270f}, ChartMath.sweepAngles(new float[]{1f, 3f}), 1e-3f);
    }

    @Test
    public void zeroTotalGivesZeroSweeps() {
        assertArrayEquals(new float[]{0f, 0f, 0f}, ChartMath.sweepAngles(new float[]{0f, 0f, 0f}), 0f);
    }

    @Test
    public void negativeValuesCountAsZero() {
        assertArrayEquals(new float[]{0f, 360f}, ChartMath.sweepAngles(new float[]{-5f, 2f}), 1e-3f);
    }

    @Test
    public void angleFromTopGoesClockwise() {
        assertEquals(0f, ChartMath.angleFromTop(0f, -1f), 1e-3f);   // 12 giờ
        assertEquals(90f, ChartMath.angleFromTop(1f, 0f), 1e-3f);   // 3 giờ
        assertEquals(180f, ChartMath.angleFromTop(0f, 1f), 1e-3f);  // 6 giờ
        assertEquals(270f, ChartMath.angleFromTop(-1f, 0f), 1e-3f); // 9 giờ
    }

    @Test
    public void sliceAtFindsContainingSlice() {
        float[] sweeps = {90f, 180f, 90f};
        assertEquals(0, ChartMath.sliceAt(45f, sweeps));
        assertEquals(1, ChartMath.sliceAt(90f, sweeps));
        assertEquals(2, ChartMath.sliceAt(300f, sweeps));
    }

    @Test
    public void sliceAtWithEmptyDataReturnsMinusOne() {
        assertEquals(-1, ChartMath.sliceAt(10f, new float[]{0f, 0f}));
    }

    @Test
    public void nearestIndexRoundsAndClamps() {
        assertEquals(2, ChartMath.nearestIndex(24f, 0f, 10f, 5));
        assertEquals(3, ChartMath.nearestIndex(26f, 0f, 10f, 5));
        assertEquals(0, ChartMath.nearestIndex(-100f, 0f, 10f, 5));
        assertEquals(4, ChartMath.nearestIndex(999f, 0f, 10f, 5));
        assertEquals(-1, ChartMath.nearestIndex(5f, 0f, 10f, 0));
    }
}
```

- [ ] **Step 2: Chạy test, xác nhận FAIL**

Run: `./gradlew :app:testDebugUnitTest --tests "vn.edu.tdtu.canvasdemo.chart.*"`
Expected: FAIL — `cannot find symbol: class ChartScale`

- [ ] **Step 3: Viết ChartScale**

`app/src/main/java/vn/edu/tdtu/canvasdemo/chart/ChartScale.java`:

```java
package vn.edu.tdtu.canvasdemo.chart;

/** Ánh xạ tuyến tính giá trị dữ liệu [min, max] → pixel [pixelStart, pixelEnd]. */
public final class ChartScale {
    private final float min;
    private final float max;
    private final float pixelStart;
    private final float pixelEnd;

    public ChartScale(float min, float max, float pixelStart, float pixelEnd) {
        this.min = min;
        // Dữ liệu phẳng (mọi giá trị bằng nhau): nới khoảng để tránh chia cho 0
        this.max = max > min ? max : min + 1f;
        this.pixelStart = pixelStart;
        this.pixelEnd = pixelEnd;
    }

    public float toPixel(float value) {
        return pixelStart + (value - min) / (max - min) * (pixelEnd - pixelStart);
    }

    /** Giá trị của vạch thứ `index` trong `tickCount` vạch chia đều từ min đến max. */
    public float tickValue(int index, int tickCount) {
        if (tickCount < 2) return min;
        return min + (max - min) * index / (tickCount - 1);
    }
}
```

- [ ] **Step 4: Viết ChartMath**

`app/src/main/java/vn/edu/tdtu/canvasdemo/chart/ChartMath.java`:

```java
package vn.edu.tdtu.canvasdemo.chart;

public final class ChartMath {
    private ChartMath() {
    }

    /** Góc quét (độ) của từng phần donut; tổng = 360. Giá trị âm coi như 0; tổng ≤ 0 → toàn 0. */
    public static float[] sweepAngles(float[] values) {
        float total = 0f;
        for (float v : values) total += Math.max(0f, v);
        float[] sweeps = new float[values.length];
        if (total <= 0f) return sweeps;
        for (int i = 0; i < values.length; i++) {
            sweeps[i] = Math.max(0f, values[i]) / total * 360f;
        }
        return sweeps;
    }

    /** Góc của (dx, dy) so với tâm: 0° = 12 giờ, tăng theo chiều kim đồng hồ (trục y hướng xuống). */
    public static float angleFromTop(float dx, float dy) {
        float deg = (float) Math.toDegrees(Math.atan2(dy, dx)) + 90f;
        return deg < 0f ? deg + 360f : deg;
    }

    /** Chỉ số phần donut chứa góc `angleDeg` (đo từ 12 giờ), -1 nếu không có. */
    public static int sliceAt(float angleDeg, float[] sweeps) {
        float start = 0f;
        for (int i = 0; i < sweeps.length; i++) {
            if (sweeps[i] > 0f && angleDeg >= start && angleDeg < start + sweeps[i]) return i;
            start += sweeps[i];
        }
        return -1;
    }

    /** Chỉ số phần tử gần x nhất, với phần tử i nằm ở start + i·step. */
    public static int nearestIndex(float x, float start, float step, int count) {
        if (count <= 0) return -1;
        if (step <= 0f) return 0;
        int index = Math.round((x - start) / step);
        return Math.max(0, Math.min(count - 1, index));
    }
}
```

- [ ] **Step 5: Chạy test, xác nhận PASS**

Run: `./gradlew :app:testDebugUnitTest --tests "vn.edu.tdtu.canvasdemo.chart.*"`
Expected: PASS (11 tests)

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/vn/edu/tdtu/canvasdemo/chart app/src/test/java/vn/edu/tdtu/canvasdemo/chart
git commit -m "feat: add chart scale and donut math helpers"
git pull --rebase && git push
```

---

### Task B2: Line chart realtime

**Files:**
- Create: `app/src/main/java/vn/edu/tdtu/canvasdemo/chart/ChartAxes.java`
- Create: `app/src/main/java/vn/edu/tdtu/canvasdemo/chart/LineChartView.java`
- Create: `app/src/main/res/layout/activity_chart.xml`
- Modify (ghi đè): `app/src/main/java/vn/edu/tdtu/canvasdemo/chart/ChartActivity.java`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `ChartScale`, `ChartMath` (B1); `SystemBars` (Task 0.2).
- Produces:
  - `ChartAxes.plotArea(RectF out, int w, int h, float density)`, `ChartAxes.gridPaint(float density)`, `ChartAxes.labelPaint(Context, Paint.Align)`, `ChartAxes.drawHorizontalGrid(Canvas, RectF plot, ChartScale, int lines, float labelGap, Paint grid, Paint label)` (package-private, B3 dùng).
  - `LineChartView.SAMPLE_INTERVAL_MS` (public, = 400), `LineChartView.setDataSource(Supplier<Float> source)` — **B6 dùng**. Mặc định: random walk 0..100.

- [ ] **Step 1: Thêm chuỗi**

```xml
    <string name="chart_line">Line chart realtime</string>
```

- [ ] **Step 2: Viết ChartAxes**

`app/src/main/java/vn/edu/tdtu/canvasdemo/chart/ChartAxes.java`:

```java
package vn.edu.tdtu.canvasdemo.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.TypedValue;

import java.util.Locale;

/** Phần vẽ dùng chung cho các chart có trục Y. */
final class ChartAxes {
    private static final int GRID_COLOR = 0xFFE0E0E0;
    private static final int LABEL_COLOR = 0xFF757575;
    private static final float LABEL_SP = 11f;
    private static final float INSET_LEFT_DP = 40f;
    private static final float INSET_TOP_DP = 12f;
    private static final float INSET_RIGHT_DP = 12f;
    private static final float INSET_BOTTOM_DP = 24f;

    private ChartAxes() {
    }

    /** Vùng vẽ dữ liệu: chừa trái cho nhãn trục Y, dưới cho nhãn trục X. */
    static void plotArea(RectF out, int w, int h, float density) {
        out.set(INSET_LEFT_DP * density, INSET_TOP_DP * density,
                w - INSET_RIGHT_DP * density, h - INSET_BOTTOM_DP * density);
    }

    static Paint gridPaint(float density) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(GRID_COLOR);
        paint.setStrokeWidth(density);
        return paint;
    }

    static Paint labelPaint(Context context, Paint.Align align) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(LABEL_COLOR);
        paint.setTextAlign(align);
        paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, LABEL_SP,
                context.getResources().getDisplayMetrics()));
        return paint;
    }

    /** `lines` đường lưới ngang chia đều, nhãn giá trị nằm bên trái vùng vẽ. */
    static void drawHorizontalGrid(Canvas canvas, RectF plot, ChartScale scale, int lines,
                                   float labelGap, Paint grid, Paint label) {
        for (int i = 0; i < lines; i++) {
            float value = scale.tickValue(i, lines);
            float y = scale.toPixel(value);
            canvas.drawLine(plot.left, y, plot.right, y, grid);
            canvas.drawText(String.format(Locale.US, "%.0f", value), plot.left - labelGap,
                    y + label.getTextSize() / 3f, label);
        }
    }
}
```

- [ ] **Step 3: Viết LineChartView**

`app/src/main/java/vn/edu/tdtu/canvasdemo/chart/LineChartView.java`:

```java
package vn.edu.tdtu.canvasdemo.chart;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Supplier;

/** Line chart nhận điểm mới định kỳ và trượt mượt sang trái; chạm để xem giá trị. */
public class LineChartView extends View {
    public static final long SAMPLE_INTERVAL_MS = 400;
    private static final int VISIBLE_POINTS = 30;
    private static final int GRID_LINES = 5;
    private static final float WALK_STEP = 20f;
    private static final float WALK_MIN = 0f;
    private static final float WALK_MAX = 100f;
    private static final float Y_PADDING_RATIO = 0.1f;
    private static final int LINE_COLOR = 0xFF26A69A;
    private static final int FILL_COLOR = 0x3326A69A;
    private static final float LINE_DP = 2.5f;
    private static final float MARKER_DP = 5f;
    private static final float LABEL_GAP_DP = 6f;

    private final List<Float> points = new ArrayList<>();
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint;
    private final Paint axisLabelPaint;
    private final Paint valuePaint;
    private final Path linePath = new Path();
    private final Path fillPath = new Path();
    private final RectF plot = new RectF();
    private final ValueAnimator scrollAnimator = ValueAnimator.ofFloat(1f, 0f);
    private final Random random = new Random();
    private final float density;
    private Supplier<Float> dataSource = this::randomWalk;
    private float lastValue = (WALK_MIN + WALK_MAX) / 2f;
    /** 1 → 0 trong mỗi chu kỳ lấy mẫu: phần bước mà điểm mới nhất còn phải trượt vào. */
    private float scrollOffset;
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
        fillPaint.setColor(FILL_COLOR);
        markerPaint.setColor(LINE_COLOR);
        gridPaint = ChartAxes.gridPaint(density);
        axisLabelPaint = ChartAxes.labelPaint(context, Paint.Align.RIGHT);
        valuePaint = ChartAxes.labelPaint(context, Paint.Align.CENTER);
        scrollAnimator.setDuration(SAMPLE_INTERVAL_MS);
        scrollAnimator.setInterpolator(new LinearInterpolator());
        scrollAnimator.addUpdateListener(a -> {
            scrollOffset = (float) a.getAnimatedValue();
            invalidate();
        });
    }

    /** Thay nguồn dữ liệu (mặc định: random walk 0..100). Gọi trên main thread. */
    public void setDataSource(Supplier<Float> source) {
        dataSource = source;
    }

    private float randomWalk() {
        float next = lastValue + (random.nextFloat() - 0.5f) * WALK_STEP;
        lastValue = Math.max(WALK_MIN, Math.min(WALK_MAX, next));
        return lastValue;
    }

    private void addPoint(float value) {
        points.add(value);
        if (points.size() > VISIBLE_POINTS + 1) {
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
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(sampler);
        scrollAnimator.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        ChartAxes.plotArea(plot, w, h, density);
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
        if (points.isEmpty() || plot.isEmpty()) return;
        ChartScale scale = yScale();
        ChartAxes.drawHorizontalGrid(canvas, plot, scale, GRID_LINES, LABEL_GAP_DP * density,
                gridPaint, axisLabelPaint);

        float step = stepWidth();
        linePath.reset();
        fillPath.reset();
        for (int i = 0; i < points.size(); i++) {
            float px = xAt(i, step);
            float py = scale.toPixel(points.get(i));
            if (i == 0) {
                linePath.moveTo(px, py);
                fillPath.moveTo(px, plot.bottom);
            } else {
                linePath.lineTo(px, py);
            }
            fillPath.lineTo(px, py);
        }
        fillPath.lineTo(xAt(points.size() - 1, step), plot.bottom);
        fillPath.close();

        canvas.save();
        canvas.clipRect(plot);
        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);
        canvas.restore();

        if (selectedIndex >= 0 && selectedIndex < points.size()) {
            float value = points.get(selectedIndex);
            float px = xAt(selectedIndex, step);
            float py = scale.toPixel(value);
            canvas.drawCircle(px, py, MARKER_DP * density, markerPaint);
            canvas.drawText(String.format(Locale.US, "%.1f", value), px, py - 2 * MARKER_DP * density, valuePaint);
        }
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

- [ ] **Step 4: Layout và Activity (bản chỉ có line chart — B3 sẽ bổ sung)**

`app/src/main/res/layout/activity_chart.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/main"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/chart_line"
            android:textAppearance="?attr/textAppearanceTitleMedium" />

        <vn.edu.tdtu.canvasdemo.chart.LineChartView
            android:id="@+id/line_chart"
            android:layout_width="match_parent"
            android:layout_height="200dp" />
    </LinearLayout>
</ScrollView>
```

Ghi đè `app/src/main/java/vn/edu/tdtu/canvasdemo/chart/ChartActivity.java`:

```java
package vn.edu.tdtu.canvasdemo.chart;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import vn.edu.tdtu.canvasdemo.R;
import vn.edu.tdtu.canvasdemo.core.SystemBars;

public class ChartActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chart);
        SystemBars.applyPadding(findViewById(R.id.main));
    }
}
```

- [ ] **Step 5: Kiểm tra trên máy thật**

Run: `./gradlew :app:assembleDebug` → `BUILD SUCCESSFUL`. Home → Dynamic Charts, đối chiếu:
- Đường xanh ngọc có vùng tô mờ bên dưới, điểm mới xuất hiện ~0,4 s/lần và cả đường trượt mượt sang trái (không giật từng nấc).
- Trục Y có 5 nhãn và tự co giãn theo dữ liệu.
- Chạm/kéo ngang → chấm tròn + giá trị bám theo điểm gần nhất; khi dữ liệu trượt, chấm trôi theo điểm của nó rồi biến mất ở mép trái.
- Thoát màn rồi vào lại vài lần → không crash, không chạy chồng nhiều luồng dữ liệu.

- [ ] **Step 6: Commit**

```bash
git add app/src/main
git commit -m "feat: add realtime line chart drawn on canvas"
git pull --rebase && git push
```

---

### Task B3: Bar chart, Donut chart, hoàn thiện màn Chart

**Files:**
- Create: `app/src/main/java/vn/edu/tdtu/canvasdemo/chart/BarChartView.java`
- Create: `app/src/main/java/vn/edu/tdtu/canvasdemo/chart/DonutChartView.java`
- Modify (ghi đè): `app/src/main/res/layout/activity_chart.xml`, `app/src/main/java/vn/edu/tdtu/canvasdemo/chart/ChartActivity.java`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `ChartScale`, `ChartMath` (B1); `ChartAxes` (B2).
- Produces:
  - `BarChartView.MAX_VALUE` (= 100), `BarChartView.barCount()`, `setData(float[] values)` (độ dài phải = `barCount()`, nếu sai ném `IllegalArgumentException`).
  - `DonutChartView.sliceCount()`, `setData(float[] values)` (độ dài phải = `sliceCount()`).

- [ ] **Step 1: Thêm chuỗi**

```xml
    <string name="chart_bar">Bar chart</string>
    <string name="chart_donut">Donut chart</string>
    <string name="chart_shuffle">Đổi dữ liệu</string>
    <string name="chart_tap_hint">Chạm vào vòng</string>
    <string name="chart_no_data">Không có dữ liệu</string>
```

- [ ] **Step 2: Viết BarChartView**

`app/src/main/java/vn/edu/tdtu/canvasdemo/chart/BarChartView.java`:

```java
package vn.edu.tdtu.canvasdemo.chart;

import android.animation.FloatArrayEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import java.util.Locale;

/** Bar chart: đổi dữ liệu thì các cột co giãn mượt bằng ValueAnimator + FloatArrayEvaluator. */
public class BarChartView extends View {
    public static final float MAX_VALUE = 100f;
    private static final String[] LABELS = {"T2", "T3", "T4", "T5", "T6", "T7"};
    private static final int GRID_LINES = 5;
    private static final long ANIMATION_MS = 700;
    private static final float OVERSHOOT_TENSION = 1.2f;
    private static final float BAR_FILL = 0.6f; // bề rộng cột / bề rộng ô
    private static final int BAR_COLOR = 0xFF42A5F5;
    private static final int SELECTED_COLOR = 0xFF1565C0;
    private static final float CORNER_DP = 4f;
    private static final float LABEL_GAP_DP = 6f;

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint;
    private final Paint axisLabelPaint;
    private final Paint labelPaint;
    private final RectF plot = new RectF();
    private final RectF bar = new RectF();
    private final float density;
    private float[] values = new float[LABELS.length];
    private ValueAnimator animator;
    private int selectedIndex = -1;

    public BarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        gridPaint = ChartAxes.gridPaint(density);
        axisLabelPaint = ChartAxes.labelPaint(context, Paint.Align.RIGHT);
        labelPaint = ChartAxes.labelPaint(context, Paint.Align.CENTER);
    }

    public static int barCount() {
        return LABELS.length;
    }

    public void setData(float[] target) {
        if (target.length != LABELS.length) {
            throw new IllegalArgumentException("Cần đúng " + LABELS.length + " giá trị");
        }
        if (animator != null) animator.cancel();
        // Bắt đầu từ giá trị đang hiển thị → đổi dữ liệu giữa chừng vẫn mượt
        animator = ValueAnimator.ofObject(new FloatArrayEvaluator(), values.clone(), target.clone());
        animator.setDuration(ANIMATION_MS);
        animator.setInterpolator(new OvershootInterpolator(OVERSHOOT_TENSION));
        animator.addUpdateListener(a -> {
            values = (float[]) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        ChartAxes.plotArea(plot, w, h, density);
    }

    private float slotWidth() {
        return plot.width() / LABELS.length;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (plot.isEmpty()) return;
        ChartScale scale = new ChartScale(0f, MAX_VALUE, plot.bottom, plot.top);
        ChartAxes.drawHorizontalGrid(canvas, plot, scale, GRID_LINES, LABEL_GAP_DP * density,
                gridPaint, axisLabelPaint);

        float slot = slotWidth();
        float corner = CORNER_DP * density;
        for (int i = 0; i < LABELS.length; i++) {
            // Overshoot có thể đẩy giá trị vượt [0, MAX] trong chốc lát
            float v = Math.max(0f, Math.min(MAX_VALUE, values[i]));
            float left = plot.left + slot * i + slot * (1f - BAR_FILL) / 2f;
            bar.set(left, scale.toPixel(v), left + slot * BAR_FILL, plot.bottom);
            barPaint.setColor(i == selectedIndex ? SELECTED_COLOR : BAR_COLOR);
            canvas.drawRoundRect(bar, corner, corner, barPaint);
            canvas.drawText(LABELS[i], bar.centerX(), plot.bottom + labelPaint.getTextSize() + 4f * density, labelPaint);
            if (i == selectedIndex) {
                canvas.drawText(String.format(Locale.US, "%.0f", v), bar.centerX(), bar.top - LABEL_GAP_DP * density, labelPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            float x = e.getX();
            float slot = slotWidth();
            boolean inside = x >= plot.left && x <= plot.right;
            selectedIndex = inside ? ChartMath.nearestIndex(x, plot.left + slot / 2f, slot, LABELS.length) : -1;
            invalidate();
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
}
```

- [ ] **Step 3: Viết DonutChartView**

`app/src/main/java/vn/edu/tdtu/canvasdemo/chart/DonutChartView.java`:

```java
package vn.edu.tdtu.canvasdemo.chart;

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

import java.util.Locale;

import vn.edu.tdtu.canvasdemo.R;

/** Donut chart: các phần quét từ 0° đến 360°; chạm vào vòng để xem phần trăm. Chú thích bên phải. */
public class DonutChartView extends View {
    private static final String[] LABELS = {"Android", "iOS", "Web", "Desktop", "Khác"};
    private static final int[] COLORS = {0xFF66BB6A, 0xFF42A5F5, 0xFFFFA726, 0xFFAB47BC, 0xFF78909C};
    private static final long SWEEP_MS = 900;
    private static final float START_ANGLE = -90f; // 12 giờ
    private static final float RING_DP = 28f;
    private static final float SELECTED_RING_SCALE = 1.3f;
    private static final float MARGIN_DP = 8f;
    private static final float LEGEND_GAP_DP = 24f;
    private static final float LEGEND_DOT_DP = 6f;
    private static final float LEGEND_LINE_DP = 22f;
    private static final float CENTER_TEXT_SP = 16f;
    private static final int TEXT_COLOR = 0xFF424242;

    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint legendPaint;
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();
    private final ValueAnimator sweepAnimator = ValueAnimator.ofFloat(0f, 1f);
    private final float density;
    private final float ringWidth;
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
        arcPaint.setStyle(Paint.Style.STROKE);
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
    }

    public static int sliceCount() {
        return LABELS.length;
    }

    public void setData(float[] values) {
        if (values.length != LABELS.length) {
            throw new IllegalArgumentException("Cần đúng " + LABELS.length + " giá trị");
        }
        sweeps = ChartMath.sweepAngles(values);
        selectedIndex = -1;
        sweepAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        sweepAnimator.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // Donut chiếm hình vuông bên trái, chú thích ở phần còn lại
        float size = Math.min(w / 2f, h);
        radius = (size - ringWidth * SELECTED_RING_SCALE) / 2f - MARGIN_DP * density;
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
        float start = START_ANGLE;
        for (int i = 0; i < sweeps.length; i++) {
            float sweep = sweeps[i] * progress;
            arcPaint.setColor(COLORS[i]);
            arcPaint.setStrokeWidth(i == selectedIndex ? ringWidth * SELECTED_RING_SCALE : ringWidth);
            canvas.drawArc(arcBounds, start, sweep, false, arcPaint);
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
            float dx = e.getX() - centerX;
            float dy = e.getY() - centerY;
            float dist = (float) Math.hypot(dx, dy);
            boolean onRing = Math.abs(dist - radius) <= ringWidth;
            selectedIndex = onRing ? ChartMath.sliceAt(ChartMath.angleFromTop(dx, dy), sweeps) : -1;
            invalidate();
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
}
```

- [ ] **Step 4: Ghi đè layout**

`app/src/main/res/layout/activity_chart.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/main"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/chart_line"
            android:textAppearance="?attr/textAppearanceTitleMedium" />

        <vn.edu.tdtu.canvasdemo.chart.LineChartView
            android:id="@+id/line_chart"
            android:layout_width="match_parent"
            android:layout_height="200dp" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:text="@string/chart_bar"
            android:textAppearance="?attr/textAppearanceTitleMedium" />

        <vn.edu.tdtu.canvasdemo.chart.BarChartView
            android:id="@+id/bar_chart"
            android:layout_width="match_parent"
            android:layout_height="220dp" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:text="@string/chart_donut"
            android:textAppearance="?attr/textAppearanceTitleMedium" />

        <vn.edu.tdtu.canvasdemo.chart.DonutChartView
            android:id="@+id/donut_chart"
            android:layout_width="match_parent"
            android:layout_height="200dp" />

        <Button
            android:id="@+id/btn_shuffle"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center_horizontal"
            android:layout_marginTop="16dp"
            android:text="@string/chart_shuffle" />
    </LinearLayout>
</ScrollView>
```

- [ ] **Step 5: Ghi đè ChartActivity**

`app/src/main/java/vn/edu/tdtu/canvasdemo/chart/ChartActivity.java`:

```java
package vn.edu.tdtu.canvasdemo.chart;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

import vn.edu.tdtu.canvasdemo.R;
import vn.edu.tdtu.canvasdemo.core.SystemBars;

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
```

- [ ] **Step 6: Kiểm tra trên máy thật**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug` → `BUILD SUCCESSFUL`, test chart vẫn PASS. Home → Dynamic Charts, đối chiếu:
- Mở màn: các cột mọc lên từ 0 với nảy nhẹ (overshoot); donut quét vòng từ 12 giờ theo chiều kim đồng hồ.
- "Đổi dữ liệu" → cột co giãn từ chiều cao cũ sang mới, donut quét lại.
- Bấm "Đổi dữ liệu" liên tục 5 lần nhanh → cột chuyển mượt, không nhảy về 0.
- Chạm 1 cột → cột đậm màu + hiện số phía trên.
- Chạm 1 phần donut → phần đó dày lên, giữa vòng hiện %; chạm giữa lỗ donut → bỏ chọn, hiện "Chạm vào vòng".
- Chú thích 5 màu bên phải donut khớp với màu các phần.
- Cuộn màn lên xuống bình thường; line chart vẫn chạy.

- [ ] **Step 7: Commit**

```bash
git add app/src/main
git commit -m "feat: add animated bar and donut charts"
git pull --rebase && git push
```

---

### Task B4: App vẽ — brush, undo/redo, độ dày

**Files:**
- Create: `app/src/main/java/vn/edu/tdtu/canvasdemo/drawing/UndoStack.java`
- Create: `app/src/main/java/vn/edu/tdtu/canvasdemo/drawing/BrushType.java`
- Create: `app/src/main/java/vn/edu/tdtu/canvasdemo/drawing/Stroke.java`
- Create: `app/src/main/java/vn/edu/tdtu/canvasdemo/drawing/StrokeRenderer.java`
- Create: `app/src/main/java/vn/edu/tdtu/canvasdemo/drawing/DrawingView.java`
- Create: `app/src/main/res/layout/activity_drawing.xml`
- Modify (ghi đè): `app/src/main/java/vn/edu/tdtu/canvasdemo/drawing/DrawingActivity.java`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/vn/edu/tdtu/canvasdemo/drawing/UndoStackTest.java`

**Interfaces:**
- Consumes: `SystemBars`, `SeekBars`, `@string/clear` (Task 0.2).
- Produces (B5 dùng):
  - `UndoStack<T>`: `push(T)`, `boolean undo()`, `boolean redo()`, `List<T> items()`.
  - `enum BrushType { PEN, MARKER, SPRAY, NEON, ERASER }`
  - `DrawingView`: `setBrush(BrushType)`, `setColor(int)`, `int getColor()`, `setStrokeWidth(float px)`, `undo()`, `redo()`, `clear()`, `Bitmap exportBitmap()` (nền trắng).
  - Layout `activity_drawing.xml` có sẵn `swatch_row`, `btn_palette`, `btn_save` — B5 gắn chức năng.

- [ ] **Step 1: Viết test UndoStack**

`app/src/test/java/vn/edu/tdtu/canvasdemo/drawing/UndoStackTest.java`:

```java
package vn.edu.tdtu.canvasdemo.drawing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class UndoStackTest {
    @Test
    public void undoRemovesLastAndRedoRestoresIt() {
        UndoStack<String> stack = new UndoStack<>();
        stack.push("a");
        stack.push("b");

        assertTrue(stack.undo());
        assertEquals(Collections.singletonList("a"), stack.items());

        assertTrue(stack.redo());
        assertEquals(Arrays.asList("a", "b"), stack.items());
    }

    @Test
    public void pushAfterUndoClearsRedo() {
        UndoStack<String> stack = new UndoStack<>();
        stack.push("a");
        stack.push("b");
        stack.undo();

        stack.push("c");

        assertFalse(stack.redo());
        assertEquals(Arrays.asList("a", "c"), stack.items());
    }

    @Test
    public void undoAndRedoOnEmptyReturnFalse() {
        UndoStack<String> stack = new UndoStack<>();
        assertFalse(stack.undo());
        assertFalse(stack.redo());
        assertTrue(stack.items().isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void itemsIsReadOnly() {
        UndoStack<String> stack = new UndoStack<>();
        stack.items().add("x");
    }
}
```

- [ ] **Step 2: Chạy test, xác nhận FAIL**

Run: `./gradlew :app:testDebugUnitTest --tests "vn.edu.tdtu.canvasdemo.drawing.UndoStackTest"`
Expected: FAIL — `cannot find symbol: class UndoStack`

- [ ] **Step 3: Viết UndoStack**

`app/src/main/java/vn/edu/tdtu/canvasdemo/drawing/UndoStack.java`:

```java
package vn.edu.tdtu.canvasdemo.drawing;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/** Lịch sử thao tác: undo chuyển phần tử cuối sang ngăn redo; thao tác mới xóa ngăn redo. */
public final class UndoStack<T> {
    private final List<T> done = new ArrayList<>();
    private final Deque<T> undone = new ArrayDeque<>();

    public void push(T item) {
        done.add(item);
        undone.clear();
    }

    public boolean undo() {
        if (done.isEmpty()) return false;
        undone.push(done.remove(done.size() - 1));
        return true;
    }

    public boolean redo() {
        if (undone.isEmpty()) return false;
        done.add(undone.pop());
        return true;
    }

    public List<T> items() {
        return Collections.unmodifiableList(done);
    }
}
```

- [ ] **Step 4: Chạy test, xác nhận PASS**

Run: `./gradlew :app:testDebugUnitTest --tests "vn.edu.tdtu.canvasdemo.drawing.UndoStackTest"`
Expected: PASS (4 tests)

- [ ] **Step 5: BrushType, Stroke, StrokeRenderer**

`app/src/main/java/vn/edu/tdtu/canvasdemo/drawing/BrushType.java`:

```java
package vn.edu.tdtu.canvasdemo.drawing;

public enum BrushType { PEN, MARKER, SPRAY, NEON, ERASER }
```

`app/src/main/java/vn/edu/tdtu/canvasdemo/drawing/Stroke.java`:

```java
package vn.edu.tdtu.canvasdemo.drawing;

import android.graphics.Path;

/** Một nét vẽ. Path chỉ được thêm điểm khi đang vẽ; sau khi nhấc tay thì không đổi nữa. */
public final class Stroke {
    /** Lệnh "Xóa hết" được lưu như một nét để Undo được. */
    public static final Stroke CLEAR = new Stroke(BrushType.ERASER, 0, 0f, new Path());

    public final BrushType type;
    public final int color;
    public final float width;
    public final Path path;

    public Stroke(BrushType type, int color, float width, Path path) {
        this.type = type;
        this.color = color;
        this.width = width;
        this.path = path;
    }
}
```

`app/src/main/java/vn/edu/tdtu/canvasdemo/drawing/StrokeRenderer.java`:

```java
package vn.edu.tdtu.canvasdemo.drawing;

import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

/** Cấu hình Paint theo loại brush rồi vẽ nét lên Canvas. */
final class StrokeRenderer {
    private static final int MARKER_ALPHA = 90;
    private static final float MARKER_WIDTH_FACTOR = 2f;
    private static final float NEON_CORE_FACTOR = 0.35f;

    private final Paint paint = new Paint();
    private final Paint neonCore = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final PorterDuffXfermode clearMode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
    private BlurMaskFilter neonBlur;
    private float neonBlurRadius = -1f;

    StrokeRenderer() {
        neonCore.setColor(Color.WHITE);
        neonCore.setStyle(Paint.Style.STROKE);
        neonCore.setStrokeCap(Paint.Cap.ROUND);
        neonCore.setStrokeJoin(Paint.Join.ROUND);
    }

    void draw(Canvas canvas, Stroke stroke) {
        if (stroke == Stroke.CLEAR) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            return;
        }
        configure(stroke);
        canvas.drawPath(stroke.path, paint);
        if (stroke.type == BrushType.NEON) {
            // Neon = quầng sáng mờ màu + lõi trắng mảnh ở giữa
            neonCore.setStrokeWidth(stroke.width * NEON_CORE_FACTOR);
            canvas.drawPath(stroke.path, neonCore);
        }
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

- [ ] **Step 6: Viết DrawingView**

`app/src/main/java/vn/edu/tdtu/canvasdemo/drawing/DrawingView.java`:

```java
package vn.edu.tdtu.canvasdemo.drawing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;

/**
 * Các nét đã xong được vẽ sẵn lên Bitmap offscreen (`committed`); nét đang vẽ được vẽ đè mỗi frame.
 * Undo/Redo = vẽ lại Bitmap từ danh sách nét trong UndoStack.
 */
public class DrawingView extends View {
    private static final int SPRAY_DOTS = 12;
    private static final float SPRAY_RADIUS_FACTOR = 1.5f;
    private static final float SPRAY_DOT_FACTOR = 0.1f;
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
    private BrushType brush = BrushType.PEN;
    private int color = Color.BLACK;
    private float strokeWidth;

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

    /** Ảnh để lưu: nền trắng + các nét đã xong. */
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
        for (Stroke s : history.items()) renderer.draw(committedCanvas, s);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.WHITE);
        if (committed == null) return;
        // Lớp riêng: tẩy (CLEAR) chỉ đục lớp vẽ, không đục nền trắng
        int layer = canvas.saveLayer(0f, 0f, getWidth(), getHeight(), null);
        canvas.drawBitmap(committed, 0f, 0f, null);
        if (current != null) renderer.draw(canvas, current);
        canvas.restoreToCount(layer);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                current = new Stroke(brush, color, strokeWidth, new Path());
                if (brush == BrushType.SPRAY) {
                    sprayAt(x, y);
                } else {
                    current.path.moveTo(x, y);
                }
                lastX = x;
                lastY = y;
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (current == null) return true;
                if (brush == BrushType.SPRAY) {
                    sprayAt(x, y);
                } else {
                    // Đường cong qua trung điểm → nét mượt, không gãy khúc
                    current.path.quadTo(lastX, lastY, (x + lastX) / 2f, (y + lastY) / 2f);
                }
                lastX = x;
                lastY = y;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                if (current != null) {
                    if (brush != BrushType.SPRAY) current.path.lineTo(x, y);
                    history.push(current);
                    renderer.draw(committedCanvas, current);
                    current = null;
                    invalidate();
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

    /** Rải chấm ngẫu nhiên trong hình tròn quanh ngón tay; sqrt để chấm phân bố đều theo diện tích. */
    private void sprayAt(float x, float y) {
        float radius = strokeWidth * SPRAY_RADIUS_FACTOR;
        float dot = Math.max(density, strokeWidth * SPRAY_DOT_FACTOR);
        for (int i = 0; i < SPRAY_DOTS; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            float r = (float) Math.sqrt(random.nextDouble()) * radius;
            current.path.addCircle(x + (float) Math.cos(angle) * r, y + (float) Math.sin(angle) * r,
                    dot, Path.Direction.CW);
        }
    }
}
```

- [ ] **Step 7: Thêm chuỗi**

```xml
    <string name="brush_pen">Bút</string>
    <string name="brush_marker">Marker</string>
    <string name="brush_spray">Phun</string>
    <string name="brush_neon">Neon</string>
    <string name="brush_eraser">Tẩy</string>
    <string name="stroke_width">Độ dày</string>
    <string name="palette">Màu…</string>
    <string name="undo">Hoàn tác</string>
    <string name="redo">Làm lại</string>
    <string name="save">Lưu</string>
```

- [ ] **Step 8: Layout**

`app/src/main/res/layout/activity_drawing.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/main"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <vn.edu.tdtu.canvasdemo.drawing.DrawingView
        android:id="@+id/drawing_view"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />

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
```

- [ ] **Step 9: Ghi đè DrawingActivity (bản B4 — B5 bổ sung màu và lưu)**

`app/src/main/java/vn/edu/tdtu/canvasdemo/drawing/DrawingActivity.java`:

```java
package vn.edu.tdtu.canvasdemo.drawing;

import android.os.Bundle;
import android.widget.RadioGroup;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import vn.edu.tdtu.canvasdemo.R;
import vn.edu.tdtu.canvasdemo.core.SeekBars;
import vn.edu.tdtu.canvasdemo.core.SystemBars;

public class DrawingActivity extends AppCompatActivity {
    private DrawingView drawingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_drawing);
        SystemBars.applyPadding(findViewById(R.id.main));

        drawingView = findViewById(R.id.drawing_view);
        float density = getResources().getDisplayMetrics().density;

        RadioGroup brushes = findViewById(R.id.brush_group);
        brushes.setOnCheckedChangeListener((group, checkedId) -> drawingView.setBrush(brushFor(checkedId)));
        SeekBars.onProgress(findViewById(R.id.seek_width), p -> drawingView.setStrokeWidth(Math.max(1, p) * density));
        findViewById(R.id.btn_undo).setOnClickListener(v -> drawingView.undo());
        findViewById(R.id.btn_redo).setOnClickListener(v -> drawingView.redo());
        findViewById(R.id.btn_clear).setOnClickListener(v -> drawingView.clear());
    }

    private static BrushType brushFor(int checkedId) {
        if (checkedId == R.id.brush_marker) return BrushType.MARKER;
        if (checkedId == R.id.brush_spray) return BrushType.SPRAY;
        if (checkedId == R.id.brush_neon) return BrushType.NEON;
        if (checkedId == R.id.brush_eraser) return BrushType.ERASER;
        return BrushType.PEN;
    }
}
```

- [ ] **Step 10: Kiểm tra trên máy thật**

Run: `./gradlew :app:assembleDebug` → `BUILD SUCCESSFUL`. Home → Drawing, đối chiếu:
- Bút: nét đen mượt, không gãy khúc khi vẽ vòng tròn nhanh; chạm 1 cái tạo chấm.
- Marker: nét to, bán trong suốt, đầu vuông; vẽ đè lên nét bút vẫn thấy nét bên dưới.
- Phun: chấm rải quanh ngón tay, dày hơn khi giữ lâu một chỗ.
- Neon: nét có quầng mờ + lõi trắng.
- Tẩy: xóa nét, lộ nền trắng (không ra màu đen).
- Kéo "Độ dày" về 0 rồi vẽ → vẫn có nét mảnh, không crash.
- Hoàn tác / Làm lại khi chưa vẽ gì → không có gì xảy ra, không crash.
- Vẽ 3 nét → Hoàn tác 2 lần còn 1 nét → Làm lại 1 lần còn 2 nét → vẽ nét mới → Làm lại không còn tác dụng.
- "Xóa hết" → trắng; Hoàn tác → các nét quay lại.
- (Nút "Màu…" và "Lưu" chưa có tác dụng — làm ở B5.)

- [ ] **Step 11: Commit**

```bash
git add app/src/main app/src/test/java/vn/edu/tdtu/canvasdemo/drawing
git commit -m "feat: add drawing screen with custom brushes and undo/redo"
git pull --rebase && git push
```

---

### Task B5: Bảng chọn màu HSV, ô màu nhanh, lưu ảnh vào Gallery

**Files:**
- Create: `app/src/main/java/vn/edu/tdtu/canvasdemo/drawing/PickerMath.java`
- Create: `app/src/main/java/vn/edu/tdtu/canvasdemo/drawing/ColorPickerView.java`
- Create: `app/src/main/java/vn/edu/tdtu/canvasdemo/drawing/ImageSaver.java`
- Modify (ghi đè): `app/src/main/java/vn/edu/tdtu/canvasdemo/drawing/DrawingActivity.java`
- Modify: `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/vn/edu/tdtu/canvasdemo/drawing/PickerMathTest.java`

**Interfaces:**
- Consumes: `DrawingView` (B4): `setColor`, `getColor`, `exportBitmap`; layout ids `swatch_row`, `btn_palette`, `btn_save` (B4).
- Produces:
  - `PickerMath.hueAt(float dx, float dy)` → 0..360 (0 = 3 giờ, chiều kim đồng hồ, trục y hướng xuống — khớp `SweepGradient`); `PickerMath.fraction(float pos, float start, float length)` → kẹp trong [0, 1].
  - `ColorPickerView(Context)`: `setColor(int)`, `int getColor()`.
  - `ImageSaver.save(Context, Bitmap)` → `Uri`, ném `IOException` nếu lỗi.

- [ ] **Step 1: Viết test**

`app/src/test/java/vn/edu/tdtu/canvasdemo/drawing/PickerMathTest.java`:

```java
package vn.edu.tdtu.canvasdemo.drawing;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PickerMathTest {
    @Test
    public void hueFollowsSweepGradientDirection() {
        assertEquals(0f, PickerMath.hueAt(1f, 0f), 1e-3f);    // 3 giờ: đỏ
        assertEquals(90f, PickerMath.hueAt(0f, 1f), 1e-3f);   // 6 giờ (y hướng xuống)
        assertEquals(180f, PickerMath.hueAt(-1f, 0f), 1e-3f); // 9 giờ: cyan
        assertEquals(270f, PickerMath.hueAt(0f, -1f), 1e-3f); // 12 giờ
    }

    @Test
    public void fractionMapsPositionInsideRange() {
        assertEquals(0.25f, PickerMath.fraction(125f, 100f, 100f), 1e-4f);
    }

    @Test
    public void fractionIsClampedOutsideRange() {
        assertEquals(0f, PickerMath.fraction(-500f, 100f, 100f), 0f);
        assertEquals(1f, PickerMath.fraction(9999f, 100f, 100f), 0f);
    }

    @Test
    public void fractionOfZeroLengthIsZero() {
        assertEquals(0f, PickerMath.fraction(150f, 100f, 0f), 0f);
    }
}
```

- [ ] **Step 2: Chạy test, xác nhận FAIL**

Run: `./gradlew :app:testDebugUnitTest --tests "vn.edu.tdtu.canvasdemo.drawing.PickerMathTest"`
Expected: FAIL — `cannot find symbol: variable PickerMath`

- [ ] **Step 3: Viết PickerMath**

`app/src/main/java/vn/edu/tdtu/canvasdemo/drawing/PickerMath.java`:

```java
package vn.edu.tdtu.canvasdemo.drawing;

final class PickerMath {
    private PickerMath() {
    }

    /** Hue (0..360) của điểm (dx, dy) so với tâm; trục y hướng xuống nên tăng theo chiều kim đồng hồ. */
    static float hueAt(float dx, float dy) {
        float deg = (float) Math.toDegrees(Math.atan2(dy, dx));
        return deg < 0f ? deg + 360f : deg;
    }

    /** Vị trí tương đối của pos trong [start, start + length], kẹp trong [0, 1]. */
    static float fraction(float pos, float start, float length) {
        if (length <= 0f) return 0f;
        return Math.max(0f, Math.min(1f, (pos - start) / length));
    }
}
```

- [ ] **Step 4: Chạy test, xác nhận PASS**

Run: `./gradlew :app:testDebugUnitTest --tests "vn.edu.tdtu.canvasdemo.drawing.PickerMathTest"`
Expected: PASS (4 tests)

- [ ] **Step 5: Viết ColorPickerView**

`app/src/main/java/vn/edu/tdtu/canvasdemo/drawing/ColorPickerView.java`:

```java
package vn.edu.tdtu.canvasdemo.drawing;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Bảng chọn màu HSV tự vẽ: vòng ngoài chọn Hue (SweepGradient),
 * ô vuông bên trong chọn Saturation (ngang) và Value (dọc).
 */
public class ColorPickerView extends View {
    private static final int[] HUE_COLORS = {
            Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED};
    private static final float RING_RATIO = 0.1f;      // độ dày vòng / cạnh view
    private static final float SQUARE_RATIO = 0.65f;   // nửa cạnh ô vuông / bán kính trong của vòng (< 1/√2)
    private static final float MARKER_DP = 3f;
    private static final int TARGET_NONE = 0;
    private static final int TARGET_RING = 1;
    private static final int TARGET_SQUARE = 2;

    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint saturationPaint = new Paint();
    private final Paint valuePaint = new Paint();
    private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint swatchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF square = new RectF();
    private final float[] hsv = {0f, 1f, 1f};
    private final float[] pureHue = {0f, 1f, 1f};
    private final float density;
    private float centerX;
    private float centerY;
    private float ringRadius;
    private float ringWidth;
    private int dragTarget = TARGET_NONE;

    public ColorPickerView(Context context) {
        this(context, null);
    }

    public ColorPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        ringPaint.setStyle(Paint.Style.STROKE);
        markerPaint.setStyle(Paint.Style.STROKE);
        markerPaint.setColor(Color.WHITE);
        markerPaint.setStrokeWidth(MARKER_DP * density);
    }

    public void setColor(int color) {
        Color.colorToHSV(color, hsv);
        updateSaturationShader();
        invalidate();
    }

    public int getColor() {
        return Color.HSVToColor(hsv);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float size = Math.min(w, h);
        centerX = w / 2f;
        centerY = h / 2f;
        ringWidth = size * RING_RATIO;
        ringRadius = size / 2f - ringWidth;
        ringPaint.setStrokeWidth(ringWidth);
        ringPaint.setShader(new SweepGradient(centerX, centerY, HUE_COLORS, null));
        float half = (ringRadius - ringWidth / 2f) * SQUARE_RATIO;
        square.set(centerX - half, centerY - half, centerX + half, centerY + half);
        valuePaint.setShader(new LinearGradient(0f, square.top, 0f, square.bottom,
                Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP));
        updateSaturationShader();
    }

    /** Lớp ngang: trắng → màu hue thuần. Chỉ tạo lại khi hue hoặc kích thước đổi. */
    private void updateSaturationShader() {
        if (square.isEmpty()) return;
        pureHue[0] = hsv[0];
        saturationPaint.setShader(new LinearGradient(square.left, 0f, square.right, 0f,
                Color.WHITE, Color.HSVToColor(pureHue), Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawCircle(centerX, centerY, ringRadius, ringPaint);
        canvas.drawRect(square, saturationPaint);
        canvas.drawRect(square, valuePaint); // lớp dọc: trong suốt → đen, phủ lên lớp ngang

        double hueRad = Math.toRadians(hsv[0]);
        float hueX = centerX + (float) Math.cos(hueRad) * ringRadius;
        float hueY = centerY + (float) Math.sin(hueRad) * ringRadius;
        canvas.drawCircle(hueX, hueY, ringWidth / 2f, markerPaint);

        float svX = square.left + hsv[1] * square.width();
        float svY = square.top + (1f - hsv[2]) * square.height();
        float markerRadius = ringWidth / 3f;
        swatchPaint.setColor(getColor());
        canvas.drawCircle(svX, svY, markerRadius, swatchPaint);
        canvas.drawCircle(svX, svY, markerRadius, markerPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                float dist = (float) Math.hypot(x - centerX, y - centerY);
                if (Math.abs(dist - ringRadius) <= ringWidth) {
                    dragTarget = TARGET_RING;
                } else if (square.contains(x, y)) {
                    dragTarget = TARGET_SQUARE;
                } else {
                    dragTarget = TARGET_NONE;
                }
                updateFromTouch(x, y);
                return true;
            case MotionEvent.ACTION_MOVE:
                updateFromTouch(x, y);
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

    /** Đang kéo vòng thì chỉ đổi hue, đang kéo ô vuông thì chỉ đổi S/V — kể cả khi ngón tay trượt ra ngoài. */
    private void updateFromTouch(float x, float y) {
        if (dragTarget == TARGET_RING) {
            hsv[0] = PickerMath.hueAt(x - centerX, y - centerY);
            updateSaturationShader();
        } else if (dragTarget == TARGET_SQUARE) {
            hsv[1] = PickerMath.fraction(x, square.left, square.width());
            hsv[2] = 1f - PickerMath.fraction(y, square.top, square.height());
        } else {
            return;
        }
        invalidate();
    }
}
```

- [ ] **Step 6: Viết ImageSaver**

`app/src/main/java/vn/edu/tdtu/canvasdemo/drawing/ImageSaver.java`:

```java
package vn.edu.tdtu.canvasdemo.drawing;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.IOException;
import java.io.OutputStream;

/** Lưu Bitmap thành PNG vào thư viện ảnh qua MediaStore. Gọi trên luồng nền. */
final class ImageSaver {
    private static final String ALBUM = "CanvasDemo";

    private ImageSaver() {
    }

    static Uri save(Context context, Bitmap bitmap) throws IOException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "drawing_" + System.currentTimeMillis() + ".png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + ALBUM);
            values.put(MediaStore.Images.Media.IS_PENDING, 1); // ẩn file khỏi Gallery khi đang ghi dở
        }

        ContentResolver resolver = context.getContentResolver();
        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new IOException("Không tạo được file ảnh");

        try (OutputStream out = resolver.openOutputStream(uri)) {
            if (out == null || !bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                throw new IOException("Không ghi được ảnh");
            }
        } catch (IOException e) {
            resolver.delete(uri, null, null); // không để lại file rỗng
            throw e;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues done = new ContentValues();
            done.put(MediaStore.Images.Media.IS_PENDING, 0);
            resolver.update(uri, done, null, null);
        }
        return uri;
    }
}
```

- [ ] **Step 7: Quyền ghi bộ nhớ cho Android 7–9**

Trong `app/src/main/AndroidManifest.xml`, thêm ngay trên thẻ `<application`:

```xml
    <uses-permission
        android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="28" />
```

- [ ] **Step 8: Thêm chuỗi**

```xml
    <string name="pick_color">Chọn màu</string>
    <string name="swatch_desc">Màu %1$s</string>
    <string name="save_ok">Đã lưu ảnh vào Thư viện</string>
    <string name="save_failed">Lưu thất bại: %1$s</string>
    <string name="save_permission_denied">Cần quyền bộ nhớ để lưu ảnh</string>
```

- [ ] **Step 9: Ghi đè DrawingActivity (bản đầy đủ)**

`app/src/main/java/vn/edu/tdtu/canvasdemo/drawing/DrawingActivity.java`:

```java
package vn.edu.tdtu.canvasdemo.drawing;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
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

import vn.edu.tdtu.canvasdemo.R;
import vn.edu.tdtu.canvasdemo.core.SeekBars;
import vn.edu.tdtu.canvasdemo.core.SystemBars;

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
        if (checkedId == R.id.brush_eraser) return BrushType.ERASER;
        return BrushType.PEN;
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
            swatch.setContentDescription(getString(R.string.swatch_desc, String.format(Locale.US, "#%06X", color & 0xFFFFFF)));
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

- [ ] **Step 10: Kiểm tra trên máy thật**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug` → `BUILD SUCCESSFUL`. Home → Drawing, đối chiếu:
- Hàng 7 ô màu tròn; chạm ô đỏ rồi vẽ → nét đỏ.
- "Màu…" → hộp thoại có vòng cầu vồng + ô vuông; ô vuông bắt đầu đúng màu đang dùng.
- Kéo trên vòng → ô vuông đổi tông màu; kéo trong ô vuông → chấm chọn đổi theo, góc trái trên trắng, đáy đen.
- Bắt đầu kéo trong ô vuông rồi trượt ra ngoài → chấm dừng ở mép, không nhảy sang chọn hue.
- OK → vẽ ra đúng màu đã chọn; Cancel → giữ màu cũ.
- "Lưu" → Toast "Đã lưu ảnh vào Thư viện"; mở app Ảnh/Files → thấy ảnh trong `Pictures/CanvasDemo` (Android 10+), nền trắng, vùng tẩy cũng trắng.
- Nếu có máy/emulator Android 7–9: lần đầu bấm Lưu hiện xin quyền; từ chối → Toast "Cần quyền bộ nhớ để lưu ảnh", không crash.

- [ ] **Step 11: Commit**

```bash
git add app/src/main app/src/test/java/vn/edu/tdtu/canvasdemo/drawing
git commit -m "feat: add HSV color picker, swatches and save to gallery"
git pull --rebase && git push
```

---

### Task B6 (stretch): Playground — va chạm bung hạt + biểu đồ realtime

> **Chỉ làm khi** đến 26/10 cả 5 màn đã qua checklist của chúng, và A đã push A1 + A4. Nếu không kịp: bỏ qua task này, không ảnh hưởng điểm tối thiểu.

**Files:**
- Create: `app/src/main/java/vn/edu/tdtu/canvasdemo/playground/PlaygroundView.java`
- Create: `app/src/main/java/vn/edu/tdtu/canvasdemo/playground/PlaygroundActivity.java`
- Create: `app/src/main/res/layout/activity_playground.xml`
- Modify: `app/src/main/java/vn/edu/tdtu/canvasdemo/MainActivity.java`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `World`, `Body`, `World.CollisionListener` (A1); `Emitter`, `Particle`, `ParticlePalette` (A4); `LineChartView.setDataSource`, `LineChartView.SAMPLE_INTERVAL_MS` (B2); `FrameClock`, `SystemBars` (Task 0.2).
- Produces: `PlaygroundView.takeCollisionCount()` → số va chạm kể từ lần gọi trước (gọi trên main thread).

- [ ] **Step 1: Thêm chuỗi**

```xml
    <string name="demo_playground">Playground</string>
    <string name="demo_playground_desc">Va chạm bung hạt + biểu đồ realtime</string>
    <string name="playground_rate">Va chạm / giây</string>
```

- [ ] **Step 2: Viết PlaygroundView**

`app/src/main/java/vn/edu/tdtu/canvasdemo/playground/PlaygroundView.java`:

```java
package vn.edu.tdtu.canvasdemo.playground;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;

import vn.edu.tdtu.canvasdemo.core.FrameClock;
import vn.edu.tdtu.canvasdemo.particle.Emitter;
import vn.edu.tdtu.canvasdemo.particle.Particle;
import vn.edu.tdtu.canvasdemo.particle.ParticlePalette;
import vn.edu.tdtu.canvasdemo.physics.Body;
import vn.edu.tdtu.canvasdemo.physics.World;

/** Ghép World (A) + Emitter (A): chạm để thả bóng, mỗi va chạm mạnh bung ra tia lửa. */
public class PlaygroundView extends View {
    private static final int MAX_BODIES = 40;
    private static final int SPARK_CAPACITY = 1500;
    private static final int BURST_SIZE = 14;
    private static final float GRAVITY_DP = 800f;
    private static final float RESTITUTION = 0.85f;
    private static final float MIN_RADIUS_DP = 10f;
    private static final float MAX_RADIUS_DP = 26f;
    private static final float MAX_SIDE_SPEED_DP = 300f;
    private static final float SPARK_SHRINK = 0.5f;
    private static final int BACKGROUND = 0xFF1A237E;
    private static final int[] PALETTE = {0xFFFFCA28, 0xFF26C6DA, 0xFFEC407A, 0xFF9CCC65};

    private final World world = new World(1f, 1f, MAX_BODIES);
    private final Emitter sparks;
    private final FrameClock clock = new FrameClock();
    private final Random random = new Random();
    private final Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sparkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private int collisionsSinceSample;
    private boolean running;

    public PlaygroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        world.setGravity(GRAVITY_DP * density);
        world.setRestitution(RESTITUTION);
        sparks = new Emitter(SPARK_CAPACITY, density, random);
        sparks.setEmitting(false); // chỉ bung hạt khi có va chạm
        sparkPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
        world.setCollisionListener((x, y, impactSpeed) -> {
            sparks.burst(x, y, BURST_SIZE);
            collisionsSinceSample++;
        });
    }

    /** Số va chạm kể từ lần gọi trước, rồi đếm lại từ 0. */
    public float takeCollisionCount() {
        int count = collisionsSinceSample;
        collisionsSinceSample = 0;
        return count;
    }

    public void resume() {
        running = true;
        clock.reset();
        postInvalidateOnAnimation();
    }

    public void pause() {
        running = false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        world.setBounds(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float dt = clock.tick();
        world.step(dt);
        sparks.update(dt);

        canvas.drawColor(BACKGROUND);
        for (Body b : world.getBodies()) {
            bodyPaint.setColor(b.color);
            canvas.drawCircle(b.x, b.y, b.radius, bodyPaint);
        }
        for (int i = 0; i < sparks.capacity(); i++) {
            Particle p = sparks.get(i);
            if (!p.alive) continue;
            float t = p.progress();
            sparkPaint.setColor(ParticlePalette.fire(t));
            canvas.drawCircle(p.x, p.y, p.size * (1f - SPARK_SHRINK * t), sparkPaint);
        }
        if (running) postInvalidateOnAnimation();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            float radius = (MIN_RADIUS_DP + random.nextFloat() * (MAX_RADIUS_DP - MIN_RADIUS_DP)) * density;
            Body ball = new Body(e.getX(), e.getY(), radius, PALETTE[random.nextInt(PALETTE.length)]);
            ball.vx = (random.nextFloat() * 2f - 1f) * MAX_SIDE_SPEED_DP * density;
            world.addBody(ball); // đủ MAX_BODIES thì bỏ qua — chỉ là sân chơi
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
}
```

- [ ] **Step 3: Layout**

`app/src/main/res/layout/activity_playground.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/main"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <vn.edu.tdtu.canvasdemo.playground.PlaygroundView
        android:id="@+id/playground_view"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="16dp"
        android:layout_marginTop="8dp"
        android:text="@string/playground_rate"
        android:textAppearance="?attr/textAppearanceTitleSmall" />

    <vn.edu.tdtu.canvasdemo.chart.LineChartView
        android:id="@+id/rate_chart"
        android:layout_width="match_parent"
        android:layout_height="140dp" />
</LinearLayout>
```

- [ ] **Step 4: PlaygroundActivity**

`app/src/main/java/vn/edu/tdtu/canvasdemo/playground/PlaygroundActivity.java`:

```java
package vn.edu.tdtu.canvasdemo.playground;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import vn.edu.tdtu.canvasdemo.R;
import vn.edu.tdtu.canvasdemo.chart.LineChartView;
import vn.edu.tdtu.canvasdemo.core.SystemBars;

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
```

- [ ] **Step 5: Đăng ký màn trong Home và Manifest**

Trong `MainActivity.java`: thêm import

```java
import vn.edu.tdtu.canvasdemo.playground.PlaygroundActivity;
```

và thêm phần tử cuối vào mảng `DEMOS` (ngay sau dòng `DrawingActivity`):

```java
            new Demo(R.string.demo_playground, R.string.demo_playground_desc, PlaygroundActivity.class),
```

Trong `AndroidManifest.xml`, thêm cạnh các `<activity>` khác:

```xml
        <activity
            android:name=".playground.PlaygroundActivity"
            android:exported="false"
            android:label="@string/demo_playground"
            android:screenOrientation="portrait" />
```

- [ ] **Step 6: Kiểm tra trên máy thật**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug` → `BUILD SUCCESSFUL`. Home → Playground (thẻ thứ 6), đối chiếu:
- Chạm → bóng màu rơi kèm vận tốc ngang ngẫu nhiên.
- Bóng đập sàn/tường/nhau mạnh → bung tia lửa vàng–đỏ; bóng nằm yên thì không bung.
- Biểu đồ dưới cùng tăng khi thả nhiều bóng, giảm về ~0 khi bóng nằm yên.
- Thả 40+ bóng liên tục → không crash, FPS vẫn mượt.

- [ ] **Step 7: Commit**

```bash
git add app/src/main
git commit -m "feat: add playground combining physics, sparks and live chart"
git pull --rebase && git push
```

---

# PHẦN CUỐI — CHUNG (cả hai)

### Task F1: Dọn dẹp, kiểm tra tổng, quay video, nộp bài

**Files:**
- Delete: `app/src/main/res/layout/activity_demo_stub.xml`
- Modify: `app/src/main/res/values/strings.xml` (xóa `coming_soon`)

**Interfaces:**
- Consumes: toàn bộ các task trước.
- Produces: bản nộp cuối cùng.

- [ ] **Step 1: Xóa layout khung không còn dùng**

Run: `git grep -n "activity_demo_stub\|coming_soon" -- app/src/main/java app/src/main/res/layout`
Expected: chỉ còn chính file `activity_demo_stub.xml` (không Activity nào dùng nó nữa). Nếu còn Activity nào dùng → task của Activity đó chưa xong, dừng lại làm cho xong.

```bash
git rm app/src/main/res/layout/activity_demo_stub.xml
```

Xóa dòng `<string name="coming_soon">…</string>` trong `strings.xml`.

- [ ] **Step 2: Chạy toàn bộ test và build**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug`
Expected: `BUILD SUCCESSFUL`; tất cả test PASS (FrameClock 3, World 12, GestureMath 3, Emitter 7, ChartScale 4, ChartMath 7, UndoStack 4, PickerMath 4 = 44 test). Lint có thể còn cảnh báo (warning) — đọc qua, sửa các mục `Error` nếu có.

- [ ] **Step 3: Chạy lại checklist thủ công của từng màn trên máy thật**

Lần lượt làm lại bước "Kiểm tra trên máy thật" của A2, A3, A5, B3, B5 (và B6 nếu có) trên **bản build cuối**. Thêm 2 kiểm tra chung:
- Mở mỗi màn → xoay máy → màn vẫn dọc, không mất dữ liệu.
- Mở/đóng từng màn 5 lần liên tiếp → không crash, không chậm dần.

- [ ] **Step 4: Commit bản cuối**

```bash
git add .
git commit -m "chore: remove demo stub and finalize for submission"
git pull --rebase && git push
```

- [ ] **Step 5: Quay video (mỗi màn 1 video ≤ 2 phút)**

Quay trên máy thật, ví dụ `scrcpy --record gesture.mp4` (bật "Hiện thao tác chạm" trong Tùy chọn nhà phát triển để thấy ngón tay). Mỗi video theo khung:
1. **Mục tiêu** (~15 s): màn này minh họa mục nào của đề.
2. **Demo** (~60–80 s): đi lần lượt các mục trong checklist của màn.
3. **Điểm kỹ thuật** (~20 s): 1–2 ý chính (ví dụ Physics: Euler bán ẩn + chia bước con; Particle: object pool + FPS).

Người phụ trách màn nào quay và thuyết minh màn đó.

- [ ] **Step 6: Báo cáo, slide, mind map**

Theo mục 9 của spec. Mỗi người tự viết phần lý thuyết + hiện thực của các màn mình làm; ảnh chụp từ app của nhóm, ảnh lấy từ nơi khác phải ghi nguồn; soát chính tả chéo cho nhau (rubric: > 3 lỗi là mất điểm).

- [ ] **Step 7: Nộp**

Nén source (bỏ thư mục `build/`, `.gradle/`, `.idea/`), kèm báo cáo, slide, mind map, video → nộp trên Elearning **trước hết ngày 30/10/2026**.
