# Bảng điều khiển — app quản lý của Ba Huy

App thứ ba trong nhà:

| App | Máy | Việc |
|---|---|---|
| **Nộp bài** (`homework-gate`) | tablet Lê Hòa | chụp bài, khoá máy, canh giờ chơi |
| **Cho giờ chơi** (`homework-gate-2`) | điện thoại bà nội | một nút: cho cháu chơi N phút |
| **Bảng điều khiển** (`homework-gate-3`) | điện thoại Ba Huy | duyệt bài, xem, chỉnh — thay phần lệnh Telegram |

## Vì sao không đi bằng Telegram

App này không thể giả làm Ba Huy gõ lệnh. Hai lý do, cả hai đều là luật của
Telegram chứ không phải chuyện lập trình:

1. Một con bot không bao giờ thấy tin nhắn của chính nó (hay của bot khác) trong
   `getUpdates`. Nên app cầm token cũng không đặt được lệnh vào hàng đợi mà
   tablet đang nghe.
2. Mỗi token chỉ một tiến trình `getUpdates` được. App quản lý mà nghe cùng token
   với tablet thì hai bên giật update của nhau (lỗi 409).

App của bà nội đã đâm vào đúng bức tường này và lách bằng cách đặt lệnh vào mô tả
nhóm. Cách đó đủ cho **một nút bấm mỗi ngày**, nhưng không đủ cho một bảng điều
khiển: mô tả nhóm chỉ chứa được một dòng, và chiều tablet → điện thoại thì gần
như không có đường.

Nên đường truyền là **Firestore**: hai bên đều ghi và đọc được, có nghe thay đổi
tức thì, và miễn phí ở mức dùng của một nhà.

## Ảnh bài tập vẫn nằm ở Telegram

Firebase Storage giờ bắt bật thanh toán mới dùng được, mà việc này không đáng
phải nhập thẻ. Tablet vẫn gửi ảnh lên Telegram y như hôm nay, rồi chỉ ghi
`file_id` xuống Firestore. App quản lý cầm token bot, gọi `getFile` là tải ảnh về
xem được. `getFile` không đụng gì tới `getUpdates` nên không gây lỗi 409.

Nói cách khác: Telegram làm kho ảnh và làm chuông báo, Firestore làm dây điều
khiển.

## Tablet nghe lệnh ở đâu

Không mở service mới, không dùng FCM (FCM đẩy từ app sang app thì phải nhúng khoá
máy chủ vào APK — mất điện thoại là mất khoá).

Chỗ nghe là **`GuardAccessibilityService`**: nó vốn đã chạy 24/7 vì Android giữ
nó sống, kể cả khi cổng đang khoá và mọi service khác đã tắt. Gắn một listener
Firestore vào đó là lệnh tới trong dưới một giây, mà không thêm một tiến trình
nền nào.

## Sơ đồ dữ liệu

Một quy tắc giữ cho mọi thứ không rối: **mỗi document chỉ một bên được ghi.**

```
nha/{nhaId}                     { tao, tenCon, uids[], maGhep, maGhepHetHan }
│
├── ghep/{uid}                  điện thoại xin vào nhà, tablet kết nạp
│
├── hop/trangthai               ◄── chỉ TABLET ghi
│     cong          LOCKED|PENDING|GRANTED|ACTIVE|PAUSED
│     ketThucLuc    epoch ms phiên kết thúc (0 = không chạy)
│     conLaiMs      số ms còn lại lúc ghi
│     phutDaDuyet   hôm nay đã duyệt bao nhiêu phút
│     phutConLai    hạn mức ngày còn lại
│     soBaiCho      mấy bài đang chờ duyệt
│     cheDoBa       { bat, hetLuc }
│     quyen         { trogiup, quantri, noi, pin }
│     pinMay, mang, banApp, capNhatLuc
│
├── hop/caidat                  ◄── chỉ TABLET ghi (bản sao cấu hình đang chạy)
├── hop/danhsachapp             ◄── chỉ TABLET ghi (app đang cài, để chọn từ xa)
│
├── lenh/{id}                   ◄── chỉ ĐIỆN THOẠI ghi, tablet đọc rồi xoá
│     kieu   DUYET TUCHOI CHO BOT DUNG TIEP KHOA MOMAY DONGMAY XOAPIN CAIDAT NHAN
│     phut, baiId, chu, taoLuc
│
├── bai/{baiId}                 ◄── chỉ TABLET ghi
│     luc, trangThai CHO|DUYET|TUCHOI, soPhut, messageId
│     anh[]   { fileId, khau: DANDO|DEBAI|BAIGIAI }
│     cham    kết quả AI chấm, nếu có
│
├── nhatky/{yyyy-MM-dd}         ◄── chỉ TABLET ghi, gộp cả ngày vào một document
├── hoiai/{yyyy-MM-dd}          ◄── chỉ TABLET ghi
└── chat/{id}                   hai bên cùng ghi, mỗi tin một document
```

### Đồng hồ đếm ngược không tốn lượt ghi

`ketThucLuc` là mốc kết thúc theo giờ thật, không phải số phút còn lại. Điện
thoại tự trừ dần trên máy mình, nên tablet chỉ ghi lại khi trạng thái *đổi* —
không phải mỗi giây một lượt ghi. Cả ngày chừng vài chục lượt.

### Cấu hình đi một chiều

Nguồn sự thật của cấu hình vẫn là `Prefs` trên tablet. Điện thoại không ghi đè
lên `hop/caidat`; nó gửi lệnh `CAIDAT`, tablet nhận, áp vào `Prefs`, rồi ghi lại
`hop/caidat` cho đúng cái đang chạy. Một người ghi một chỗ thì không bao giờ có
cảnh hai bên đè nhau.

## Telegram còn lại gì

Còn nguyên. Đường Telegram vẫn nhận lệnh như cũ, vẫn gửi ảnh, vẫn báo. Nếu điện
thoại Ba Huy hết pin, mất máy, hay Firestore trục trặc, mở Telegram lên là điều
khiển được như hôm nay. Firestore là đường đi hàng ngày, Telegram là đường lui.
