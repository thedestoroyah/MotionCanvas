# Spec: App minh họa Animation & Canvas (Giữa kỳ – Phát triển ứng dụng di động)

- **Ngày:** 13/10/2026 · **Hạn nộp:** hết ngày 30/10/2026
- **Đề tài:** Topic 02 – #1 "Learn about Animation & Canvas and build an illustrated application", nhóm mục **1.1**
- **Nhóm:** 2 người (gọi là **A** và **B** trong tài liệu này)

## 1. Mục tiêu và tiêu chí thành công

Xây 1 app Android native minh họa **đủ 5 mục** của nhóm 1.1 (đã xác nhận với GV: phải làm hết cả nhóm):

1. Gesture-based animation
2. Physics effects with animation (lò xo, trọng lực, va chạm)
3. Dynamic charts using Canvas
4. Water / fire motion effects bằng Particle System
5. Drawing app với custom brush và màu

**Thành công khi:**
- Cả 5 màn đạt mức *Tối thiểu* (mục 4), chạy mượt trên máy thật, không crash → nhắm mức 75–100% tiêu chí "Chất lượng sản phẩm demo" (4đ).
- Mỗi màn có 1 video ≤ 2 phút quay rõ ràng → tiêu chí "Video" (2đ).
- Báo cáo đi sâu, đúng trọng tâm, tự viết; hình có ghi nguồn; ≤ 3 lỗi chính tả → tiêu chí 1 & 2 (4đ).
- Toàn bộ code tự viết và hiểu được; mỗi người giải thích được phần mình làm.

**Lý do chọn 1.1 thay vì 1.2:** 5 mục chung một nền (Custom View + Canvas + Animator) so với 6 nền khác nhau ở 1.2 (Compose, OpenGL ES, Lottie, AVD…); rủi ro thấp hơn, code dùng chung được, báo cáo đi sâu được một chủ đề.

## 2. Ràng buộc kỹ thuật

| Mục | Giá trị |
|---|---|
| Ngôn ngữ / UI | Java + XML (View system) |
| Template Android Studio | Empty Views Activity |
| minSdk | API 24 (Android 7.0) |
| Thư viện ngoài | chỉ `appcompat`, `material`, `androidx.dynamicanimation` |
| Cấm dùng | thư viện chart, particle, color picker, drawing của bên thứ ba — đây là phần được chấm |

## 3. Kiến trúc

App 1 module, mỗi demo là **1 Activity chứa 1 Custom View** vẽ trong `onDraw(Canvas)`. Màn Home là lưới thẻ, chạm thẻ mở demo tương ứng.

```
<package gốc>
├── MainActivity                 Home: lưới thẻ → startActivity
├── gesture/    GestureActivity, GestureView
├── physics/    PhysicsActivity, PhysicsView, World, Body
├── chart/      ChartActivity, LineChartView, BarChartView, DonutChartView, ChartScale
├── particle/   ParticleActivity, ParticleView, Emitter, Particle
├── drawing/    DrawingActivity, DrawingView, ColorPickerView, Stroke, BrushType
└── playground/ PlaygroundActivity, PlaygroundView   (stretch – xem mục 4.6)
```

**Nguyên tắc:**
- **Logic tách khỏi View:** `World`, `Body`, `Emitter`, `Particle`, `ChartScale`, `Stroke` là Java thuần, không import `android.*` (trừ khi bắt buộc, ví dụ `Path` trong `Stroke`) → test được bằng JUnit và trình bày thuật toán riêng trong báo cáo.
- **Hai cách làm animation, cả hai đều phải có:**
  - *Animator của framework*: `ValueAnimator`, `ObjectAnimator`, `SpringAnimation`, `FlingAnimation` — dùng ở Gesture, Chart.
  - *Mô phỏng tự viết*: mỗi frame tính `dt` thật (giới hạn tối đa ~1/30 s để tránh nhảy khi lag), cập nhật trạng thái, `postInvalidateOnAnimation()` — dùng ở Physics, Particle.
- **Không cấp phát object trong `onDraw`**: `Paint`, `Path`, `RectF` tạo sẵn ở constructor.
- Dừng vòng lặp mô phỏng khi Activity `onPause`, chạy lại khi `onResume`.

## 4. Tính năng từng màn

Mỗi màn: **Tối thiểu** = bắt buộc trước mốc chốt 26/10. **Cộng thêm** = chỉ làm sau mốc chốt.

### 4.1 Gesture — người A
- **Tối thiểu:** một vật thể (thẻ/bóng) theo ngón tay khi kéo; vuốt mạnh → trượt và chậm dần (`FlingAnimation` có ma sát, dừng ở mép màn hình); hai ngón → phóng to/thu nhỏ (`ScaleGestureDetector`, giới hạn 0.5×–3×); chạm đúp → bật về giữa bằng `SpringAnimation`.
- **Cộng thêm:** xoay bằng hai ngón; chồng thẻ vuốt bỏ kiểu Tinder.
- **Báo cáo:** `onTouchEvent`, `MotionEvent`, `GestureDetector`, physics-based animation (stiffness, damping ratio, friction).

### 4.2 Physics — người A
- **Tối thiểu:**
  - Chạm vùng trống → tạo bóng (bán kính ngẫu nhiên, khối lượng ∝ bán kính²).
  - Trọng lực; va tường nảy lại với hệ số đàn hồi.
  - Va chạm bóng–bóng đàn hồi có xét khối lượng; tách bóng chồng nhau.
  - 1 bóng treo vào điểm cố định bằng lò xo (Hooke + giảm chấn); kéo bóng rồi thả → dao động tắt dần.
  - Thanh trượt: độ lớn trọng lực, hệ số đàn hồi. Nút xóa hết.
  - Giới hạn số bóng (ví dụ 50) để giữ FPS.
- **Cộng thêm:** kéo-ném bóng; chuỗi bóng nối lò xo thành sợi dây.
- **Thuật toán:** Euler bán ẩn (cập nhật vận tốc trước, vị trí sau); chia mỗi frame thành nhiều bước con để tránh xuyên tường; va chạm hình tròn bằng so khoảng cách tâm với tổng bán kính, xung lực theo pháp tuyến.
- **Báo cáo:** vòng lặp mô phỏng, tích phân số, phát hiện và phản hồi va chạm.

### 4.3 Chart — người B
- **Tối thiểu:**
  - *Line chart realtime*: dữ liệu giả lập (random walk), điểm mới thêm liên tục, chart cuộn, trục Y tự co giãn.
  - *Bar chart*: nút đổi bộ dữ liệu → các cột co giãn mượt (`ValueAnimator` nội suy từ giá trị cũ sang mới).
  - *Donut chart*: các phần quét từ 0° → 360° khi mở màn / đổi dữ liệu.
  - Trục, nhãn, đường lưới; chạm vào điểm/cột/phần → tooltip hiện giá trị.
- **Cộng thêm:** kéo xem lại lịch sử và phóng to trên line chart.
- **Thuật toán:** `ChartScale` ánh xạ giá trị ↔ pixel (có test).
- **Báo cáo:** `Path`, `drawArc`, `drawText`/căn chữ, `TypeEvaluator`, interpolator.

### 4.4 Particle — người A
- **Tối thiểu:**
  - Nút chuyển **Lửa / Nước**.
  - *Lửa:* hạt sinh ở nguồn, bay lên với vận tốc ngẫu nhiên nhẹ; màu theo tuổi vàng → cam → đỏ → trong suốt; nhỏ dần; hòa màu cộng (`PorterDuff.Mode.ADD`).
  - *Nước:* vòi phun hướng lên, hạt rơi theo trọng lực; chạm đáy → sinh vài giọt nhỏ bắn tóe rồi biến mất.
  - Kéo ngón tay → di chuyển nguồn phát.
  - Hiển thị FPS và số hạt đang sống.
  - **Object pool** kích thước cố định (ví dụ 2000 hạt): hạt chết được tái sử dụng, không `new` trong vòng lặp.
- **Cộng thêm:** thanh trượt gió; khói phía trên lửa.
- **Báo cáo:** vòng đời hạt (sinh → cập nhật → chết), object pool và GC, đo FPS.

### 4.5 Drawing — người B
- **Tối thiểu:**
  - 5 brush: *bút mượt* (`quadTo` giữa các điểm), *marker* (bán trong suốt), *phun sơn* (chấm ngẫu nhiên quanh ngón tay), *neon* (`BlurMaskFilter`), *tẩy* (`PorterDuff.Mode.CLEAR`).
  - `ColorPickerView` tự vẽ: vòng Hue + ô vuông Saturation/Value; vài ô màu nhanh.
  - Thanh trượt độ dày nét.
  - Undo / Redo (danh sách `Stroke`), Xóa hết.
  - Lưu ảnh vào Gallery qua `MediaStore` (thông báo thành công/thất bại).
- **Cộng thêm:** brush cầu vồng (đổi hue dọc nét); phóng to/kéo vùng vẽ.
- **Báo cáo:** `Path`, `Paint` (stroke cap/join, mask filter, xfermode), vẽ ra Bitmap offscreen, cấu trúc undo/redo.

### 4.6 Playground — người B — stretch, chỉ làm nếu cả 5 màn đã đạt Tối thiểu trước 26/10
- Chạm → tạo bóng (dùng `World` của A), va chạm → bung hạt (dùng `Emitter` của A), mini line chart số va chạm/giây (dùng lại line chart của B).
- Không viết engine mới; chỉ ghép. Cần `World` báo được sự kiện va chạm (vị trí + thời điểm) để B gắn `Emitter` — A giữ API này ổn định sau mốc 26/10.

### 4.7 Home — người B
- Lưới thẻ (`RecyclerView` hoặc `GridLayout`), mỗi thẻ: tên + mô tả 1 dòng → mở Activity tương ứng.

### Cố ý KHÔNG làm
Va chạm đa giác; layer trong app vẽ; dữ liệu chart từ API thật; điều khiển bằng cảm biến (thuộc đề tài Sensor); hỗ trợ xoay ngang màn hình (khóa dọc).

## 5. Xử lý lỗi và hiệu năng

- `dt` bị chặn trên để app không "bay" sau khi lag hoặc quay lại từ background.
- Giới hạn số bóng / hạt; không cấp phát trong `onDraw`.
- Lưu ảnh: xử lý lỗi `MediaStore` (bộ nhớ đầy, từ chối quyền trên API < 29) → Toast thông báo, không crash.
- Mục tiêu: ~60 FPS trên máy thật tầm trung ở mức số hạt mặc định.

## 6. Kiểm thử

- **JUnit (logic thuần):**
  - `World`: rơi tự do sau t giây khớp công thức; bóng chạm sàn nảy với vận tốc × hệ số đàn hồi; 2 bóng va chạm bảo toàn động lượng; lò xo kéo về vị trí cân bằng.
  - `Emitter`/`Particle`: hạt hết tuổi được trả về pool; số hạt không vượt quá kích thước pool.
  - `ChartScale`: min → đáy, max → đỉnh, giá trị giữa → giữa.
  - Undo/redo: undo rồi redo trả lại đúng danh sách nét.
- **Thủ công:** checklist từng màn (mỗi gạch đầu dòng "Tối thiểu" là 1 mục) chạy trên máy thật trước khi quay video.

## 7. Phân công

| | A — "chuyển động" | B — "vẽ & UI" |
|---|---|---|
| Màn | Physics, Gesture, Particle | Home, Chart, Drawing |
| Stretch | Giữ API `World` (có sự kiện va chạm) và `Emitter` ổn định cho Playground | Playground |
| Báo cáo | Lý thuyết + hiện thực 3 màn của A | Lý thuyết + hiện thực 3 màn của B |
| Chung | Mở đầu, kết luận, slide, mind map, soát chính tả chéo | ← |

Mỗi người tự hiểu và giải thích được toàn bộ code mình phụ trách.

## 8. Timeline (13/10 → 30/10/2026)

| Thời gian | A | B |
|---|---|---|
| 13–14/10 | Đọc tài liệu Canvas / animation | Tạo project, màn Home, khung 5 Activity trống |
| 15–19/10 | Physics (Tối thiểu) | Chart (Tối thiểu) |
| 20–23/10 | Gesture + Particle (Tối thiểu) | Drawing (Tối thiểu) |
| 24–26/10 | JUnit, sửa lỗi, ghi chú lý thuyết | JUnit, sửa lỗi, ghi chú lý thuyết |
| **26/10** | **Mốc chốt: 5 màn đạt Tối thiểu, không lỗi** | |
| 27–28/10 | Cộng thêm / Playground (nếu kịp); quay video; hoàn thiện báo cáo, slide, mind map | |
| 29–30/10 | **Dự phòng** — chỉ sửa lỗi, soát chính tả, nộp. Không thêm tính năng | |

Nếu trễ mốc 26/10: bỏ toàn bộ Cộng thêm và Playground, dồn sức cho video + báo cáo.

## 9. Sản phẩm nộp

1. **Báo cáo:** Giới thiệu → Cơ sở lý thuyết (quy trình vẽ của View; Canvas/Paint/Path; Property Animation; Physics-based Animation; vòng lặp mô phỏng; xử lý chạm & cử chỉ) → Thiết kế & hiện thực từng demo (mục tiêu, thuật toán, đoạn code ngắn, ảnh chụp) → Đánh giá (FPS, hạn chế) → Kết luận → Tài liệu tham khảo. Mọi hình đều ghi nguồn. Tự viết.
2. **Slide** trình bày kết quả tìm hiểu.
3. **Mind map** theo cấu trúc phần Lý thuyết + 5 demo.
4. **Source code** (repo GitHub private, commit của cả 2 người).
5. **Video:** 1 video/màn, mỗi video ≤ 2 phút, kịch bản *mục tiêu → demo → điểm kỹ thuật*, quay máy thật (ví dụ `scrcpy`).
