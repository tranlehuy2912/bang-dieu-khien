# Bảng điều khiển — app quản lý của Ba Huy

App thứ ba trong nhà:

| App | Máy | Việc |
|---|---|---|
| **Nộp bài** (`homework-gate`) | tablet Lê Hòa | chụp bài, khoá máy, canh giờ chơi |
| **Cho giờ chơi** (`homework-gate-2`) | điện thoại bà nội | cho cháu chơi N phút, và giao việc nhà |
| **Bảng điều khiển** (`homework-gate-3`) | điện thoại Ba Huy | duyệt bài, xem, chỉnh — thay phần lệnh Telegram |

## Vì sao không đi bằng Telegram

App này không thể giả làm Ba Huy gõ lệnh. Hai lý do, cả hai đều là luật của
Telegram chứ không phải chuyện lập trình:

1. Một con bot không bao giờ thấy tin nhắn của chính nó (hay của bot khác) trong
   `getUpdates`. Nên app cầm token cũng không đặt được lệnh vào hàng đợi mà
   tablet đang nghe.
2. Mỗi token chỉ một tiến trình `getUpdates` được. App quản lý mà nghe cùng token
   với tablet thì hai bên giật update của nhau (lỗi 409).

App của bà nội từng đâm vào đúng bức tường này và lách bằng cách đặt lệnh vào mô
tả nhóm, tablet ghé đọc mỗi một đến ba phút. Cách đó chạy được, nhưng có ba chỗ
đau: bà bấm "đã xong" mà cháu ngồi chờ cả phút trước màn hình khoá; mô tả nhóm chỉ
có một ô nên lệnh cho giờ và lệnh việc nhà đè nhau; và máy bà phải cầm token bot —
ai rút được file APK ra khỏi máy bà là nắm cả con bot.

Nên cả ba app đi chung một đường: **Firestore**. Hai chiều, nghe thay đổi trong
dưới một giây, và miễn phí ở mức dùng của một nhà. Máy bà không cần token nữa.

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
nha/{nhaId}                     { tao, tenCon, uids[], uidsPhu[], maGhep, maGhepHetHan }
│                               uids    = người nhà đầy đủ (tablet, điện thoại Ba Huy)
│                               uidsPhu = máy bà nội, quyền hẹp — xem firestore.rules
│
├── ghep/{uid}                  máy xin vào nhà, tablet kết nạp
│     { ma, luc, ai }           ai = bahuy | banoi, quyết vào uids hay uidsPhu
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
├── lenh/{id}                   ◄── ĐIỆN THOẠI ghi, tablet đọc rồi xoá
│     kieu   DUYET TUCHOI CHO BOT DUNG TIEP KHOA MOMAY DONGMAY XOAPIN CAIDAT NHAN
│            CONGVIECNHA — cộng bù một đợt việc nhà tablet đã bỏ lỡ
│     phut, baiId, chu, taoLuc
│     ai     bahuy | banoi — máy bà chỉ tạo được lệnh CHO, luật chặn tận gốc
│
├── hop/viecnha                 ◄── chỉ MÁY BÀ NỘI ghi
│     maPhien   đổi mã nghĩa là bà giao đợt mới, không phải sửa đợt đang chạy
│     luc       lúc bà bấm, để tablet bỏ lệnh cũ quá nửa tiếng
│     viec[]    { ten, phut, xong }
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
không phải mỗi giây một lượt ghi.

"Khi trạng thái đổi" là điều phải giữ bằng tay, và bản đầu đã không giữ được.
Tablet nghe cả file prefs để biết có gì mới, mà cả app dùng chung một file đó:
nhật ký dùng app ghi vài phút một lần, đồng hồ phiên chơi ghi mốc mỗi 20 giây,
màn hình chính gọi `tick()` mỗi 10 giây. Mỗi lần ghi kéo theo một lượt đẩy, nên
con số thật là vài trăm lượt một ngày chứ không phải vài chục.

Ba chỗ vá, tất cả nằm ở phía tablet:

- các khoá không có mặt trong bản đẩy (offset Telegram, nhịp tim, dấu danh sách
  app) bị lọc ra khỏi phần nghe;
- nhật ký dùng app sang một file prefs riêng, vẫn mã hoá, nhưng không ai nghe;
- trước khi ghi, so bản sắp đẩy với bản vừa đẩy, giống hệt thì thôi. Phép so bỏ
  qua `capNhatLuc` vì trường đó lần nào cũng khác, và `conLaiMs` giờ là con số
  đứng yên chứ không phải số đang chạy.

Cộng thêm việc bỏ hẳn hai mốc đồng hồ ghi mỗi 20 giây (chống chỉnh giờ giờ tính
bằng độ lệch giữa hai đồng hồ kể từ lúc bấm Bắt đầu, không cần ghi gì), con số
mới đúng là vài chục lượt một ngày.

### Cấu hình đi một chiều

Nguồn sự thật của cấu hình vẫn là `Prefs` trên tablet. Điện thoại không ghi đè
lên `hop/caidat`; nó gửi lệnh `CAIDAT`, tablet nhận, áp vào `Prefs`, rồi ghi lại
`hop/caidat` cho đúng cái đang chạy. Một người ghi một chỗ thì không bao giờ có
cảnh hai bên đè nhau.

### Việc nhà là trạng thái, cho giờ là sự kiện

Hai thứ bà bấm đi hai đường khác nhau, và khác vì bản chất khác:

- **cho giờ** là một sự kiện — một document trong `lenh/`, tablet làm xong thì xoá
  đi. Đọc hai lần là cộng giờ hai lần, nên phải xoá.
- **việc nhà** là trạng thái đầy đủ — cả danh sách việc lẫn việc nào đã xong nằm
  gọn trong `hop/viecnha`, bà bấm gì thì ghi đè cả bản. Đọc lại cùng một bản mười
  lần cũng không sao, vì `ViecNha.apDung` bên tablet so với bản đang giữ rồi mới
  quyết. Nhờ thế listener bắn lại vì đổi mạng hay vì khởi động lại app cũng không
  sinh ra hai lần cộng giờ.

Đó cũng là lý do không nhét việc nhà vào hàng `lenh/`: cái vòng "làm xong rồi xoá"
sẽ ăn mất một bản trạng thái đang còn hiệu lực.

### Máy bà nội bị chặn ở đâu

Hai lớp, và lớp thật nằm ở luật:

1. `firestore.rules` chỉ cho `uidsPhu` tạo document trong `lenh/` khi `kieu == CHO`,
   `ai == banoi` và `phut` trong khoảng 1–60; cho ghi `hop/viecnha`; cho đọc
   `hop/trangthai`. Mọi thứ khác từ chối.
2. `ThiHanhLenh` bên tablet bỏ qua mọi lệnh không phải `CHO` khi `ai == banoi`.
   Lớp này chặn nhầm tay là chính — luật thì sửa bằng tay trong console Firebase ở
   một chỗ không ai nhìn thấy, còn dòng kiểm tra kia đi theo bản app.

Một lượt mỗi ngày thì đếm ở cả hai đầu: máy bà đếm để tắt nút đi cho bà khỏi bấm
vào khoảng không, tablet đếm vì đó mới là chỗ thật sự từ chối được — máy bà cài
lại app là số đếm bên đó về không.

## Telegram còn lại gì

Còn nguyên cho Ba Huy. Đường Telegram vẫn nhận lệnh như cũ, vẫn gửi ảnh, vẫn báo.
Nếu điện thoại hết pin, mất máy, hay Firestore trục trặc, mở Telegram lên là điều
khiển được như hôm nay. Firestore là đường đi hàng ngày, Telegram là đường lui.

Máy bà nội thì không còn đường Telegram nào: hộp thư mô tả nhóm đã gỡ hẳn, cùng
với `HopThuBaNoi` bên tablet và toàn bộ phần chống đọc trùng tự dựng của nó.
