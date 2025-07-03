# Phát Hiện Ổ Gà

Ứng dụng Android giúp phát hiện, báo cáo và chia sẻ vị trí các ổ gà trên đường, hỗ trợ cộng đồng lái xe an toàn hơn.

## Tính năng chính

- **Phát hiện ổ gà tự động:** Sử dụng cảm biến gia tốc (accelerometer) để tự động phát hiện rung lắc bất thường khi xe đi qua ổ gà.
- **Báo cáo ổ gà:** Người dùng có thể gửi báo cáo về vị trí ổ gà, kèm thông tin vị trí GPS.
- **Bản đồ ổ gà:** Xem các ổ gà đã được cộng đồng báo cáo trên bản đồ.
- **Leaderboard:** Bảng xếp hạng người dùng đóng góp nhiều báo cáo ổ gà nhất theo tuần.
- **Thời tiết:** Xem thông tin thời tiết tại vị trí hiện tại hoặc vị trí mặc định.
- **Quản lý tài khoản:** Đăng ký, đăng nhập, đổi mật khẩu, chỉnh sửa hồ sơ cá nhân.
- **Hỗ trợ đa ngôn ngữ:** Tiếng Việt và Tiếng Anh.
- **Chính sách & hỗ trợ:** Xem chính sách bảo mật, điều khoản sử dụng và gửi phản hồi/hỗ trợ.

## Công nghệ sử dụng

- Android (Java)
- Firebase Authentication & Realtime Database
- HERE SDK (bản .aar đặt trong `app/libs/`)
- Google Play Services Location
- MPAndroidChart (thống kê, biểu đồ)
- Material Design

## Yêu cầu hệ thống

- Android Studio (Giraffe hoặc mới hơn)
- JDK 11 trở lên
- Thiết bị/emulator Android API 24+
- Kết nối Internet

## Hướng dẫn cài đặt & build

1. **Clone project:**
   ```bash
   git clone <repo-url>
   cd potholes
   ```

2. **Thêm file HERE SDK:**
   - Tải file `.aar` mới nhất của HERE SDK for Android và đặt vào thư mục `app/libs/`.

3. **Cấu hình Firebase:**
   - Đảm bảo file `google-services.json` đã được đặt trong thư mục `app/`.

4. **Mở project bằng Android Studio.**
   - Chọn "Open an existing project" và trỏ tới thư mục `potholes`.

5. **Build & chạy ứng dụng:**
   - Nhấn "Run" hoặc sử dụng thiết bị/emulator Android.

## Hướng dẫn sử dụng cơ bản

- Đăng ký tài khoản hoặc đăng nhập.
- Cho phép quyền truy cập vị trí và cảm biến.
- Di chuyển trên đường, ứng dụng sẽ tự động phát hiện và thông báo khi đi qua ổ gà.
- Có thể chủ động báo cáo ổ gà hoặc sự cố khác qua chức năng "Báo cáo sự cố".
- Xem bản đồ để biết các vị trí ổ gà đã được cộng đồng ghi nhận.
- Theo dõi bảng xếp hạng đóng góp của mình và người khác.

## Thư viện & bản quyền

- HERE SDK: Cần tuân thủ điều khoản của HERE.
- Firebase: Sử dụng theo chính sách của Google.
