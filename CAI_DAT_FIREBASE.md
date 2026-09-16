# Việc Ba Huy phải làm một lần (khoảng 5 phút)

App quản lý và tablet nói chuyện với nhau qua Firestore. Firestore cần một project
Firebase, mà project đó phải mở bằng tài khoản Google của anh — chỗ này không ai
làm hộ được.

Toàn bộ những gì dùng ở đây nằm trong gói **Spark (miễn phí)**: không phải nhập
thẻ, không phải bật thanh toán. Hai máy một nhà thì mỗi ngày dùng chừng vài trăm
lượt đọc/ghi, trong khi mức miễn phí là 50.000 đọc + 20.000 ghi mỗi ngày.

## 1. Tạo project

1. Mở https://console.firebase.google.com
2. **Create a project** → đặt tên `homework-gate` → **Continue**
3. Trang Google Analytics: **tắt** đi cho gọn (app này không cần) → **Create project**

## 2. Bật Firestore

1. Menu trái: **Build → Firestore Database** → **Create database**
2. Vùng đặt máy chủ: chọn **asia-southeast1 (Singapore)** — gần Việt Nam nhất,
   lệnh đi về nhanh nhất. Chọn xong không đổi được, nên đừng bấm nhầm vùng Mỹ.
3. Chọn **Start in production mode** → **Create**

Luật truy cập (rules) tôi viết sẵn trong `firestore.rules`, dán vào sau.

## 3. Bật đăng nhập ẩn danh

1. Menu trái: **Build → Authentication** → **Get started**
2. Thẻ **Sign-in method** → chọn **Anonymous** → gạt **Enable** → **Save**

Ẩn danh nghĩa là hai máy không phải nhớ mật khẩu nào, nhưng vẫn có danh tính
riêng để luật truy cập chặn người lạ.

## 4. Khai báo hai app

Bánh răng góc trên trái → **Project settings** → kéo xuống **Your apps** →
biểu tượng Android.

**Lần 1 — tablet của Lê Hòa:**
- Android package name: `vn.huytl.homeworkgate`
- App nickname: `Nộp bài`
- SHA-1: để trống, bấm **Register app**
- Bấm **Download google-services.json**, chép file đó vào:
  `~/Documents/homework-gate/app/google-services.json`
- Mấy bước "Add Firebase SDK" tiếp theo: **bỏ qua**, tôi đã viết sẵn trong Gradle.

**Lần 2 — điện thoại của Ba Huy** (bấm **Add app** → Android lần nữa):
- Android package name: `vn.huytl.bangdieukhien`
- App nickname: `Bảng điều khiển`
- SHA-1: để trống, bấm **Register app**
- Tải `google-services.json` rồi chép vào:
  `~/Documents/homework-gate-3/app/google-services.json`

Hai file trùng tên nhưng khác nội dung, đừng chép nhầm chỗ. Cả hai đã nằm trong
`.gitignore`.

## 5. Dán luật truy cập

1. **Build → Firestore Database** → thẻ **Rules**
2. Xoá hết, dán toàn bộ nội dung file `firestore.rules` trong thư mục này
3. **Publish**

## Xong thì báo tôi

Tôi chạy `tools/kiemtra.sh` để thử kết nối trước khi cài lên máy thật.

---

### Nếu sau này muốn bỏ Firebase

Đường Telegram vẫn giữ nguyên, không đụng tới. Gỡ app quản lý là mọi thứ chạy
đúng như hôm nay.
