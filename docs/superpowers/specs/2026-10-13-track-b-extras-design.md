# Spec bổ sung: Phần cộng thêm của Track B

- **Ngày:** 13/10/2026 · **Hạn hoàn thành:** mốc chốt 26/10/2026
- **Người làm:** B
- **Bổ sung cho:** `docs/superpowers/specs/2026-10-13-animation-canvas-design.md` (gọi là *spec gốc*). Mọi ràng buộc của spec gốc (mục 2, 5) vẫn áp dụng.

## 1. Mục tiêu

Track B đã đạt mức *Tối thiểu* của spec gốc cho Home, Chart, Drawing. Spec này thêm 4 gói để tăng điểm:

- **Video (2đ):** mỗi tính năng thêm phải thấy rõ trong video ≤ 2 phút.
- **Nội dung báo cáo (2đ):** mỗi tính năng mang thêm một kiến thức Animation/Canvas để trình bày.
- **Chất lượng demo (4đ):** không làm hỏng phần đang chạy; mỗi bước một commit, có thể quay lại.

**Thứ tự làm:** Drawing (gói ① + ④) → Chart (gói ②) → Home và giao diện chung (gói ③).

## 2. Drawing (gói ① Replay + Đối xứng, gói ④ Brush nâng cao + Đổ màu)

### 2.1 Đổi mô hình nét vẽ

- `Stroke` lưu **danh sách điểm** `(x, y, w)` (`w` = hệ số độ dày tại điểm đó, 1 = bình thường) thay cho `Path` cố định. `Path` được dựng từ điểm khi vẽ.
- Mọi brush vẽ được **k điểm đầu** của nét (dùng cho Replay).
- Brush Phun lưu một `seed` ngẫu nhiên trong nét → vẽ lại (undo, replay) cho ra đúng các chấm như lần đầu.
- `Stroke` lưu thêm **chế độ đối xứng** và **tâm đối xứng** tại thời điểm vẽ.
- "Xóa hết" vẫn là một mục trong lịch sử (undo được). `UndoStack` giữ nguyên.

### 2.2 Brush mới (tổng cộng 8 brush)

| Brush | Hành vi | Kỹ thuật |
|---|---|---|
| Thư pháp | Tay nhanh → nét mảnh, tay chậm → nét đậm; hệ số độ dày kẹp trong [0.3, 1.2] | Tốc độ từ `MotionEvent.getEventTime()`, làm mượt (low-pass) để nét không giật; vẽ từng đoạn, đầu tròn |
| Cầu vồng | Màu đổi dần theo chiều dài nét | Hue = (độ dài cộng dồn / bước) mod 360; vẽ từng đoạn, đầu tròn |
| Đổ màu | Chạm vào vùng → tô kín vùng đó, giới hạn bởi các nét đã vẽ | Scanline flood fill trên `int[]` pixel của Bitmap lớp vẽ, có sai số màu để tô kín viền khử răng cưa |

- Đổ màu là một mục trong lịch sử (undo/redo được), lưu điểm chạm và màu; khi dựng lại Bitmap thì tô lại theo đúng thứ tự.
- Đổ màu bỏ qua chế độ đối xứng (chỉ tô một lần).
- Chạm vào vùng đã đúng màu cần tô, hoặc chạm ra ngoài ảnh → không làm gì, không crash.

### 2.3 Đối xứng (kaleidoscope)

- Chế độ: **Tắt / Gương / 4 / 6 / 8**.
  - *Gương:* lật nét qua trục dọc giữa vùng vẽ.
  - *4 / 6 / 8:* xoay nét đều quanh tâm vùng vẽ, mỗi nhánh có thêm bản lật gương.
- Mỗi nét nhớ chế độ lúc nó được vẽ; đổi chế độ không làm đổi nét cũ.
- Cách làm: vẽ cùng một nét nhiều lần với `canvas.rotate` / `canvas.scale`, không nhân bản dữ liệu.

### 2.4 Replay

- Nút **"Phát lại"**: vùng vẽ về trắng rồi vẽ lại từng nét theo thứ tự, `ValueAnimator` chạy theo tổng số điểm.
- Thời lượng tỉ lệ với tổng số điểm, kẹp trong **[2 s, 8 s]**.
- Chỉ phát lại từ sau lần "Xóa hết" gần nhất (đúng bức tranh đang thấy). Tranh trống → nút không làm gì.
- Trong lúc phát: các nút vẽ bị khóa; chạm vào vùng vẽ → dừng phát và hiện ngay toàn bộ tranh.
- Đổ màu trong lúc phát lại hiện ra tức thì khi tới lượt.

### 2.5 Test (JUnit, Java thuần)

- `FloodFill`: tô đúng vùng kín; không lấn qua viền; vùng đã đúng màu → không đổi; điểm ngoài ảnh → không đổi, không crash; sai số màu tô được viền gần giống màu.
- Dựng nét từ điểm: lấy k điểm đầu trả về đúng số điểm; k vượt quá → lấy toàn bộ.
- Độ dày thư pháp theo tốc độ: luôn nằm trong [0.3, 1.2]; tốc độ 0 → 1.2; tốc độ rất lớn → 0.3.
- Replay: cộng dồn số điểm qua các nét, ánh xạ tiến độ 0..1 → (nét thứ mấy, bao nhiêu điểm); bỏ qua các nét trước lần "Xóa hết" gần nhất; thời lượng kẹp trong [2 s, 8 s].

### 2.6 Không làm

Phóng to/kéo vùng vẽ; xuất video replay; áp đối xứng cho đổ màu.

## 3. Chart (gói ②)

### 3.1 Line chart

- **Công tắc "Đường cong"** cạnh tiêu đề: bật → đường cong mượt đi **qua đúng mọi điểm dữ liệu** (Catmull-Rom → các đoạn Bézier bậc ba, vẽ bằng `Path.cubicTo`). Đường cong có thể vọt nhẹ quá đỉnh/đáy; vùng vẽ đã được cắt nên không tràn.
- **Vùng tô gradient:** màu xanh ngọc mờ ở trên → trong suốt ở đáy (`LinearGradient`), tạo lại chỉ khi đổi kích thước view.
- **Điểm mới nhất nhấp nháy:** một vòng tròn lan rộng và mờ dần quanh điểm mới nhất, lặp vô hạn (`ValueAnimator`), dừng khi view rời màn hình.

### 3.2 Donut chart

- Chọn một phần → phần đó **trượt ra** theo hướng giữa của nó và nảy nhẹ bằng `SpringAnimation`; phần được chọn trước đó trượt về, cũng bằng lò xo. Mỗi phần có một lò xo riêng. Thay cho kiểu "phần được chọn dày lên".
- Vùng chạm tính theo vị trí phần **đã trượt ra**.
- "Đổi dữ liệu" → mọi phần trượt về chỗ cũ, rồi quét lại.

### 3.3 Bar chart

Giữ nguyên.

### 3.4 Test (JUnit)

Tính điểm điều khiển Catmull-Rom: đường đi qua mọi điểm gốc; 2 điểm → điểm điều khiển nằm trên đoạn thẳng nối chúng; 0 hoặc 1 điểm → không crash, không có đoạn cong.

### 3.5 Không làm

Kéo/phóng to line chart; chú thích động; xuất ảnh chart.

## 4. Home và giao diện chung (gói ③)

### 4.1 Theme

- Chỉ có giao diện sáng: đổi theme gốc sang Light, xóa `values-night/themes.xml`.
- Màu chính **indigo `#3949AB`**, màu phụ **xanh ngọc `#26A69A`**, nền `#F5F6FA`. Kiểm tra độ tương phản và chọn kiểu chữ bằng skill ui-ux-pro-max trước khi chốt.

### 4.2 Thanh tiêu đề và nút quay lại

- Mọi màn demo có ActionBar với tiêu đề lấy từ `android:label` và nút quay lại (`parentActivityName` trong Manifest). Chỉ sửa theme và Manifest — **không sửa code hay layout của A**.
- Home ẩn ActionBar (có header riêng).
- Nếu edge-to-edge làm phần trên bị đệm hai lần, sửa một lần trong `core/SystemBars`.

### 4.3 Màn Home

- Header: "MotionCanvas" + dòng phụ "Animation & Canvas · Android".
- Lưới **2 cột** thẻ. Mỗi thẻ: hình động mini vẽ bằng Canvas ở trên, tên + mô tả một dòng ở dưới.
- Chạm thẻ → màn demo phóng to ra từ vị trí thẻ (`ActivityOptionsCompat.makeScaleUpAnimation`).

### 4.4 Hình động mini (package `home/`)

Một `PreviewView` hiển thị một `Preview` (`update(dt)`, `draw(canvas, w, h)`):

| Thẻ | Hình động |
|---|---|
| Gesture | Thẻ nhỏ đung đưa tắt dần như lò xo, lặp lại |
| Physics | 3 quả bóng rơi và nảy |
| Chart | 5 cột lên xuống theo hình sin |
| Particle | Ngọn lửa nhỏ ~60 hạt, dùng pool |
| Drawing | Nét lượn sóng tự vẽ dần rồi mờ đi, lặp lại (`PathMeasure`) |

- Ba hình động Gesture/Physics/Particle là **bản rút gọn B tự viết**, không dùng code của A.
- Mỗi `PreviewView` dùng `FrameClock` (`dt` kẹp tối đa 1/30 s); dừng khi Home `onPause`, chạy lại khi `onResume`.
- Test: hàm dao động tắt dần của hình Gesture có biên độ giảm theo thời gian.

### 4.5 Icon app

Icon adaptive vector tự vẽ: nền indigo, đường cong + chấm tròn gợi chuyển động; có biến thể monochrome (Android 13+).

### 4.6 Không làm

Dark mode; chuyển cảnh shared-element; màn giới thiệu.

## 5. Phối hợp với A

- Màn của A sẽ có thanh tiêu đề (do đổi theme) — B báo trước cho A.
- Home có hình minh họa chủ đề của A — B báo cho A.
- Khi có Playground (B6), thêm thẻ thứ 6 vào lưới Home với hình động mini riêng.
