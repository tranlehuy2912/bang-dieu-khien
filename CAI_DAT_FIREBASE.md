# Việc Ba Huy phải làm một lần (khoảng 5 phút)

App quản lý và tablet nói chuyện với nhau qua Firestore. Firestore cần một project
Firebase, mà project đó phải mở bằng tài khoản Google của anh — chỗ này không ai
làm hộ được.

Toàn bộ những gì dùng ở đây nằm trong gói **Spark (miễn phí)**: không phải nhập
thẻ, không phải bật thanh toán. Ba máy một nhà thì mỗi ngày dùng chừng vài trăm
lượt đọc/ghi, trong khi mức miễn phí là 50.000 đọc + 20.000 ghi mỗi ngày.

## 1. Tạo project

1. Mở https://console.firebase.google.com
2. **Create a project** → đặt tên `homework-gate` → **Continue** (dự án thật đang dùng
   hiện tên `nop-bai`, ID `nop-bai-4934d`)
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

Ẩn danh nghĩa là các máy không phải nhớ mật khẩu nào, nhưng vẫn có danh tính
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
- App nickname: `Cho giờ chơi` (app đổi tên thành "Việc nhà của Lê Hòa" ngày
  26/09/2026; nickname chỉ là nhãn trên console, để nguyên cũng được)
- SHA-1: để trống, bấm **Register app**
- Tải `google-services.json` rồi chép vào:
  `~/Documents/Working/nop-bai/cho-gio-choi/app/google-services.json`

Mỗi lần tải là tải file của cả project, chứa mọi app đã khai tới lúc đó. Tải sau khi đã
khai đủ ba app thì một file dùng chung được cho cả ba repo (dự án thử đang làm vậy). File
tải giữa chừng thì thiếu các app khai sau, và repo nào có package không nằm trong file sẽ
không build được. Kiểm ngày 01/10/2026 trên máy của anh: file dự án thật ở `nop-bai` và
`bang-dieu-khien` chỉ có hai app `homeworkgate`, `bangdieukhien`, còn `cho-gio-choi` chưa
có file thật; muốn build bản release app bà thì tải lại file. Cả ba chỗ đặt file đã nằm
trong `.gitignore`.

Phải là cùng một project cho cả ba: chúng nhìn chung một cái nhà trên Firestore,
khác project là không thấy nhau.

## 5. Dán luật truy cập

1. **Build → Firestore Database** → thẻ **Rules**
2. Xoá hết, dán toàn bộ nội dung file `firestore.rules` trong thư mục này
3. **Publish**

Làm lại bước này mỗi lần `firestore.rules` đổi. Luật không đi theo bản app: nó nằm
trong console, và dán bản cũ đè lên là mở lại đúng cái vừa chặn. Luật có `uidsPhu`,
danh sách quyền hẹp cho máy bà nội: từ 26/09/2026 máy bà chỉ giao việc nhà, trước đó
còn cho giờ. Không dán thì máy bà bấm gì cũng bị từ chối.

Ngày 26/09/2026 luật đổi hai chỗ. Thêm cửa `hop/danhsachviec`: máy bà đọc được
danh sách việc chung. Bỏ cửa `lenh/` của máy bà: app bà
không còn nút cho giờ, và máy bà không tạo được lệnh nào nữa. Chưa dán bản này thì máy
bà vẫn giao việc được, chỉ là dùng danh sách trong máy bà thay cho danh sách Ba Huy
sửa trên Bảng điều khiển. Dán cho cả hai dự án, thật và thử.

Ngày 27/09/2026 luật đổi thêm một chỗ: bỏ quyền máy bà đọc `hop/trangthai`, vì app bà
không còn đọc chỗ đó từ khi bỏ nút cho giờ. Đây là bản mới nhất tính tới 01/10/2026; dán
cho cả hai dự án. Muốn biết console đang giữ bản nào thì đọc luật trên console rồi so với
file trong repo (cách đọc ghi ở `CLAUDE.md`, mục hai dự án Firebase).

## 6. Một dự án riêng để thử

> **Đã làm xong ngày 16/09/2026.** Dự án thử tên `homework-gate-thu` đã tạo, đã bật
> Firestore (asia-southeast1) và đăng nhập ẩn danh, đã khai app `vn.huytl.homeworkgate`,
> đã dán luật, và file cấu hình đã nằm ở `nop-bai/app/src/debug/google-services.json`.
> Máy ảo đã chạy thử và nối đúng vào dự án đó. Phần dưới giữ lại để sau này cần dựng
> lại thì có đường đi, **đừng tạo thêm một dự án thử thứ hai**.
>
> Dự án thử đã khai đủ ba app: "Nộp bài (máy ảo)", "Cho giờ chơi (máy ảo)", và "Bảng
> điều khiển (máy ảo)" khai sau cùng, ngày 24/09/2026. File `google-services.json` tải
> từ dự án thử chứa cả ba app, nên một bản dùng chung được cho `app/src/debug/` của cả
> ba repo; trên máy của anh cả ba chỗ đã có file đó.

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
     (Đã khai ngày 24/09/2026 với tên "Bảng điều khiển (máy ảo)".) App của bà nội thì
     khai `vn.huytl.chogiochoi`, file đặt ở
     `~/Documents/Working/nop-bai/cho-gio-choi/app/src/debug/google-services.json`
     (đã khai với tên "Cho giờ chơi (máy ảo)").
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
  Database → Data → nha**, xoá các document lạ. Đừng nhận nhà của tablet theo trường
  `tenCon`: mọi nhà do app tablet lập đều có trường đó, kể cả nhà rác của máy ảo. Nhận
  theo mã nhà tablet đang dùng, hiện ở màn cài đặt của tablet (dòng "Mã nhà") và trong
  hộp mã khi bấm "Nối điện thoại ba Huy" ở màn phụ huynh. Ngày 01/10/2026 nhà thật là
  `czfy4pdxn6gz5u8t3hap`; nhà cũ `4nsswi3wh96y3uehxsq3` còn giữ máy bà nội, đừng xoá
  trước khi ghép lại máy bà vào nhà mới.
- Muốn chắc hơn nữa thì chuyển `app/google-services.json` sang
  `app/src/release/google-services.json`. Khi đó bản gỡ lỗi không còn đường tụt xuống
  dùng file thật. Gradle chỉ bật plugin google-services khi có `app/google-services.json`
  hoặc file thử, nên sau khi chuyển mà thiếu cả file thử thì plugin không bật: build vẫn
  qua nhưng cả hai bản đều không nối Firebase, và Gradle chỉ in một dòng cảnh báo. Còn
  file thử thì mọi thứ chạy bình thường, chỉ có dòng cảnh báo "ban release se khong build
  duoc" in nhầm. Chỉ nên làm sau khi dự án thử đã chạy ngon.

## Xong thì báo tôi

Tôi chạy `nop-bai/tools/emu.sh status` để xem bản gỡ lỗi và bản thật đang nối dự án
Firebase nào trước khi cài lên máy thật.

---

### Nếu sau này muốn bỏ Firebase

Không còn bỏ được dễ như lúc mới làm. Ba việc giờ chỉ đi qua Firestore: chấm bài (từ
28/09/2026 tablet không tự chấm, bài chỉ được chấm qua lệnh `CHAMBAI` của Bảng điều
khiển), việc nhà của máy bà, và bản sao sổ cái ở `socai/` để cài lại tablet không mất.
Bỏ Firebase thì mất cả ba. Đường Telegram vẫn còn để duyệt tay, cho giờ, khoá máy, xem
nhật ký.
