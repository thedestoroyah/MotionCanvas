# MotionCanvas

Ứng dụng Android minh họa **Animation & Canvas** (Topic 02, nhóm 1.1), bài giữa kỳ môn Phát triển ứng dụng di động, Trường Đại học Tôn Đức Thắng.

| Thành viên | MSSV |
|---|---|
| Phạm Huỳnh Quốc Đạt | 52300098 |
| Nguyễn Hữu Bích Khoa | 72100008 |

Giảng viên hướng dẫn: ThS. Trần Đại Nhân

## Các màn hình

| Màn hình | Nội dung |
|---|---|
| Home | Lưới 6 thẻ, mỗi thẻ có hình động thu nhỏ; mở màn hình bằng hiệu ứng phóng to từ thẻ |
| Gesture Animation | Kéo, vuốt (fling), chụm hai ngón thu phóng 0,5×–3×, chạm đúp để lò xo đưa thẻ về giữa |
| Physics | Trọng lực, va chạm có xét khối lượng, con lắc lò xo; chỉnh trọng lực và độ nảy (tối đa 50 bóng) |
| Dynamic Charts | Line chart thời gian thực (Catmull–Rom), bar chart co giãn mượt, donut chart chạm để chọn |
| Particle | Hiệu ứng lửa và nước với object pool 2000 hạt, hiển thị FPS |
| Drawing | 8 công cụ vẽ, bảng màu HSV tự vẽ, đối xứng, hoàn tác/làm lại, phát lại, lưu ảnh |
| Playground | Ghép bộ mô phỏng vật lý, hệ thống hạt và biểu đồ: va chạm bung tia lửa, đếm va chạm/giây |

## Công nghệ

- Java 11 + XML (View truyền thống, không dùng Jetpack Compose)
- minSdk 24 (Android 7.0), targetSdk 37
- Thư viện: AndroidX AppCompat, Material Components, DynamicAnimation
- Biểu đồ, bảng màu, bộ mô phỏng vật lý và hệ thống hạt do nhóm tự viết, không dùng thư viện bên thứ ba

## Cấu trúc mã nguồn

```
app/src/main/java/vn/edu/tdtu/motioncanvas/
├── home/        Màn hình chính và các hình động thu nhỏ
├── gesture/     Gesture Animation
├── physics/     World, Body, Spring (Java thuần) + PhysicsView
├── chart/       ChartScale, ChartMath + Line/Bar/Donut ChartView
├── particle/    Emitter, Particle (Java thuần) + ParticleView
├── drawing/     Stroke, StrokeRenderer, UndoStack, FloodFill + DrawingView
├── playground/  Ghép World + Emitter + LineChartView
└── core/        FrameClock và tiện ích dùng chung
```

Phần tính toán (vật lý, hạt, biểu đồ, tô màu, lịch sử nét) được tách khỏi các lớp View nên kiểm thử được bằng JUnit trên máy tính.

## Build và chạy

Yêu cầu: Android Studio (bản hỗ trợ AGP 9.4) và JDK 17 trở lên để chạy Gradle.

```bash
./gradlew assembleDebug          # build APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug           # cài lên thiết bị/emulator đang kết nối
./gradlew testDebugUnitTest      # chạy kiểm thử đơn vị
```

Hoặc mở thư mục dự án bằng Android Studio và chọn **Run 'app'**.

## Kiểm thử

111 ca kiểm thử đơn vị JUnit 4 cho 15 lớp (World, Emitter, GestureMath, ChartMath, ReplayTimeline, FloodFill, UndoStack, …). Kiểm thử chức năng thực hiện trên điện thoại POCO X8 Pro (Android 16).
