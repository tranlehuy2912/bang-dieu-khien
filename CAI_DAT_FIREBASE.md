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

## 4. Khai báo ba app

Bánh răng góc trên trái → **Project settings** → kéo xuống **Your apps** →
biểu tượng Android.

**Lần 1 — tablet của Lê Hòa:**
- Android package name: `vn.huytl.homeworkgate`
- App nickname: `Nộp bài`
- SHA-1: để trống, bấm **Register app**
- Bấm **Download google-services.json**, chép file đó vào:
  `~/Documents/Working/nop-bai/nop-bai/app/google-services.json`
- Mấy bước "Add Firebase SDK" tiếp theo: **bỏ qua**, tôi đã viết sẵn trong Gradle.

**Lần 2 — điện thoại của Ba Huy** (bấm **Add app** → Android lần nữa):
- Android package name: `vn.huytl.bangdieukhien`
- App nickname: `Bảng điều khiển`
- SHA-1: để trống, bấm **Register app**
- Tải `google-services.json` rồi chép vào:
  `~/Documents/Working/nop-bai/bang-dieu-khien/app/google-services.json`

**Lần 3 — điện thoại của bà nội** (bấm **Add app** → Android lần nữa):
- Android package name: `vn.huytl.chogiochoi`
- App nickname: `Cho giờ chơi`
- SHA-1: để trống, bấm **Register app**
- Tải `google-services.json` rồi chép vào:
  `~/Documents/Working/nop-bai/cho-gio-choi/app/google-services.json`

Ba file trùng tên nhưng khác nội dung, đừng chép nhầm chỗ. Cả ba đã nằm trong
`.gitignore`.

Phải là cùng một project cho cả ba: chúng nhìn chung một cái nhà trên Firestore,
khác project là không thấy nhau.

## 5. Dán luật truy cập

1. **Build → Firestore Database** → thẻ **Rules**
2. Xoá hết, dán toàn bộ nội dung file `firestore.rules` trong thư mục này
3. **Publish**

Làm lại bước này mỗi lần `firestore.rules` đổi. Luật không đi theo bản app: nó nằm
trong console, và dán bản cũ đè lên là mở lại đúng cái vừa chặn. Lần này luật có
thêm `uidsPhu` — danh sách quyền hẹp cho máy bà nội, chỉ cho giờ và giao việc nhà.
Không dán thì máy bà bấm gì cũng bị từ chối.

## 6. Một dự án riêng để thử

> **Đã làm xong ngày 16/09/2026.** Dự án thử tên `homework-gate-thu` đã tạo, đã bật
> Firestore (asia-southeast1) và đăng nhập ẩn danh, đã khai app `vn.huytl.homeworkgate`,
> đã dán luật, và file cấu hình đã nằm ở `nop-bai/app/src/debug/google-services.json`.
> Máy ảo đã chạy thử và nối đúng vào dự án đó. Phần dưới giữ lại để sau này cần dựng
> lại thì có đường đi, **đừng tạo thêm một dự án thử thứ hai**.
>
> Ngày 24/09/2026 khai thêm app `vn.huytl.bangdieukhien` vào dự án thử. File
> `google-services.json` tải từ dự án thử chứa cả ba app, nên một bản dùng chung được
> cho `app/src/debug/` của cả ba repo.

Máy ảo cũng ghi vào Firestore thật: mỗi lần chạy bộ test là một "nhà" mới mở ra
trong dự án. Chung một dự án thì rác của máy ảo nằm cạnh dữ liệu thật của Lê Hòa,
dùng chung hạn mức miễn phí, và một lần vào console dọn nhầm tay là mất dữ liệu
thật. Nên tách hẳn hai dự án: một cho máy ảo, một cho tablet.

Không phải sửa dòng code nào — Gradle tự chọn file theo bản build:

```
app/src/debug/google-services.json   dự án THỬ  (máy ảo dùng)
app/google-services.json             dự án THẬT (tablet của Lê Hòa dùng)
```

**Các bước** (giống hệt mục 1–5 ở trên, chỉ khác tên và chỗ đặt file):

1. https://console.firebase.google.com → **Create a project** → tên
   `homework-gate-thu` → tắt Analytics → **Create project**
2. **Build → Firestore Database** → **Create database** → **asia-southeast1
   (Singapore)** → **Start in production mode**
3. **Build → Authentication** → **Get started** → **Anonymous** → **Enable**
4. Bánh răng → **Project settings** → **Your apps** → Android:
   - Android package name: `vn.huytl.homeworkgate`
   - SHA-1 để trống → **Register app** → **Download google-services.json**
   - Chép file đó vào — **chú ý đường dẫn khác mục 4 ở trên**:
     `~/Documents/Working/nop-bai/nop-bai/app/src/debug/google-services.json`
   - Muốn thử cả app điện thoại trên máy ảo thì khai thêm app
     `vn.huytl.bangdieukhien`, file tải về đặt ở
     `~/Documents/Working/nop-bai/bang-dieu-khien/app/src/debug/google-services.json`.
     (Đã khai ngày 24/09/2026 với tên "Bảng điều khiển (máy ảo)".)
5. **Firestore Database → Rules** → dán `firestore.rules` → **Publish**

**Kiểm lại:**

```
nop-bai/tools/emu.sh status
```

Dòng đầu phải ra như thế này:

```
--- firebase ---
ban go loi: homework-gate-thu
ban that:   nop-bai-4934d
```

Chưa đặt file thì mỗi lần build sẽ có một dòng cảnh báo `CHU Y: ... BAN GO LOI
DANG NOI VAO DU AN FIREBASE THAT`, và `emu.sh status` ghi rõ `<-- DU AN THAT!`.

**Vài điều đi kèm:**

- Tablet không phải làm gì cả. Bản release vẫn lấy `app/google-services.json`
  như cũ, cài đè lên máy Lê Hòa không mất gì.
- Đổi xong, máy ảo lập một nhà mới bên dự án thử, nên điện thoại nào đã ghép với
  nhà cũ thì phải ghép lại **nếu muốn thử trên máy ảo**. Ghép với tablet thật thì
  không đụng gì.
- Mấy cái nhà rác máy ảo đã tạo trong dự án thật: vào console, **Firestore
  Database → Data → nha**, xoá các document lạ. Nhà của tablet là nhà có
  `tenCon` và đang được cập nhật liên tục.
- Muốn chắc hơn nữa thì chuyển `app/google-services.json` sang
  `app/src/release/google-services.json`. Khi đó bản gỡ lỗi không còn đường tụt
  xuống dùng file thật — nhưng đổi lại, thiếu file thử là bản gỡ lỗi không build
  được luôn. Chỉ nên làm sau khi dự án thử đã chạy ngon.

## Xong thì báo tôi

Tôi chạy `nop-bai/tools/emu.sh status` để xem bản gỡ lỗi và bản thật đang nối dự án
Firebase nào trước khi cài lên máy thật.

---

### Nếu sau này muốn bỏ Firebase

Đường Telegram vẫn giữ nguyên, không đụng tới. Gỡ app quản lý là mọi thứ chạy
đúng như hôm nay.
