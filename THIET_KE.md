# Bảng điều khiển — app quản lý của Ba Huy

App thứ ba trong nhà:

| App | Máy | Việc |
|---|---|---|
| **Nộp bài** (`nop-bai`) | tablet Lê Hòa | chụp bài, khoá máy, canh giờ chơi |
| **Việc nhà của Lê Hòa** (`cho-gio-choi`) | điện thoại bà nội | giao việc nhà. Trước 26/09/2026 app tên Cho giờ chơi, có sáu nút cho giờ |
| **Bảng điều khiển** (`bang-dieu-khien`) | điện thoại Ba Huy | duyệt bài, nhờ Claude chấm, xem, chỉnh, cấp quỹ giờ chơi, giao việc nhà; thay phần lệnh Telegram |

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
nền nào. `ApprovalService` (service Telegram) cũng gọi `DongBo.batDau`, phòng khi dịch
vụ trợ năng đang tắt.

## Sơ đồ dữ liệu

Một quy tắc giữ cho mọi thứ không rối: **mỗi document chỉ một bên được ghi.**

Ngoại lệ có chủ ý: hai trường `chamClaude` và `anKhoiDanhSach` trong `bai/` do Bảng
điều khiển ghi; `ghep/{uid}` do máy xin vào tạo, bên kết nạp ghi thêm `trangThai`; khi
cài lại tablet thì điện thoại Ba Huy giữ cửa, ghi `maGhep`, `maGhepHetHan` lên `nha/` rồi
tự thêm uid của tablet vào `uids` (`Kho` bên này, `DongBo.xinVaoNha` bên tablet); và
tablet xoá `hop/viecnha` khi một đợt việc đã khép. Tên
trường đầy đủ nằm ở `Duong.kt` (ba bản giống nhau); sơ đồ dưới đây mà lệch với
file đó thì `Duong.kt` đúng.

Từ ngày 26/09/2026 `hop/viecnha` có hai bên ghi: máy bà nội và Bảng điều khiển. Mỗi
lần bấm là một transaction, xem mục "Việc nhà là trạng thái, cho giờ là sự kiện".

```
nha/{nhaId}                     { tao, tenCon, uids[], uidsPhu[], maGhep, maGhepHetHan }
│                               uids    = người nhà đầy đủ (tablet, điện thoại Ba Huy)
│                               uidsPhu = máy bà nội, quyền hẹp — xem firestore.rules
│
├── ghep/{uid}                  máy xin vào nhà, tablet kết nạp
│     { ma, luc, ai }           ai = bahuy | banoi, quyết vào uids hay uidsPhu
│     trangThai                 OK | SAI, bên kết nạp ghi sau khi so mã (tablet, hay điện
│                               thoại Ba Huy lúc ghép ngược khi cài lại tablet)
│
├── hop/trangthai               ◄── chỉ TABLET ghi
│     cong          LOCKED|PENDING|GRANTED|ACTIVE|PAUSED
│     ketThucLuc    epoch ms phiên kết thúc (0 = không chạy)
│     conLaiMs      số ms đang giữ khi PAUSED, GRANTED, PENDING; 0 khi đang chơi
│                   (điện thoại tự trừ từ ketThucLuc)
│     tongPhienMs   cả phiên dài bao nhiêu ms, đứng yên suốt phiên, để vẽ thanh chạy
│     phutDaDuyet   hôm nay đã duyệt bao nhiêu phút
│     phutConLai    TRAN_NGAY (215, tổng các trần riêng) trừ phutDaDuyet. Từ 29/09/2026
│                   chỉ để hiện, không chặn duyệt hay cấp giờ
│     quyGio        số phút trong quỹ giờ chơi (29/09/2026), cho nút cấp quỹ ở tab Giờ chơi
│     soBaiCho      mấy bài đang chờ duyệt
│     viecNha       tên các việc nhà chưa xong, để hiểu vì sao tablet đang khoá
│     deThi[]       { ma, ten, den, tt, sao, toiDa }: các đề thi in sẵn trong tablet (từ
│                   30/09/2026), cho hàng "Đề thi thử Tiếng Anh" của tab Giờ chơi. den là
│                   Unit cuối đề chạm tới, tablet tự mở đề khi lớp học tới đó; tt là
│                   KHOA|SAN|MO|DANG|XONG; sao, toiDa là điểm lần nộp gần nhất, -1 khi
│                   chưa nộp. Vắng là tablet bản cũ
│     cheDoBa       { bat, hetLuc }
│     quyen         { trogiup, quantri, noi, pin }
│     appTruocMat   tên app đang trên màn hình, LUÔN là chuỗi; đi kèm appTruocMatTu
│                   (lúc mở app đó) và manHinhSang. Vắng cả ba là không biết
│     traLoi        { chu, luc, ai }: câu tablet nói lại sau khi làm một lệnh
│     pinMay, dangSac, banApp, capNhatLuc
│
├── hop/caidat                  ◄── chỉ TABLET ghi (bản sao cấu hình đang chạy)
│     appChoPhep    app "Dùng khi hết giờ chơi": mở được lúc hết giờ chơi, trừ giờ
│                   ngủ và giờ đi học. Trước 27/09/2026 tên là "App luôn được dùng"
│     appMoiLuc     app "Dùng mọi lúc": không khoá theo giờ nào, màn chặn giờ học và
│                   việc nhà nhường cho nó. Danh sách cấm và giờ riêng vẫn áp dụng
│     appCatMang    app mất mạng khi tablet khoá (hết giờ chơi, giờ ngủ, giờ học), kể
│                   cả lúc chạy nền. Tablet cắt bằng VPN của chính nó, Ba Huy phải bấm
│                   OK ở hộp thoại VPN trên tablet một lần. Thêm ngày 27/09/2026
│     appNhac       app "được nghe nền": phát tiếng được khi hết giờ chơi, trừ giờ ngủ
│                   và giờ học. Tablet ghi từ 27/09/2026, bản cũ hơn không có
│     gioiHanApp    { goi: số phút mỗi ngày }, mục "Giờ riêng từng app". Lệnh CAIDAT
│                   gioiHanApp chỉ đổi các app có trong map gửi đi, 0 là bỏ. Bảng
│                   điều khiển sửa được từ 27/09/2026, mỗi lần một app
│     appChan, appAi, khoaCaiDat, gioNgu, gioDay, và tranPhutMoiNgay (vẫn ghi dù trần
│                   chung đã bỏ ngày 29/09/2026, để Bảng điều khiển bản cũ không hỏng)
├── hop/danhsachapp             ◄── chỉ TABLET ghi (app đang cài, để chọn từ xa)
├── hop/dando                   ◄── bỏ từ 30/09/2026, tablet chỉ còn xoá document này.
│                                   Từng giữ vở dặn dò của buổi vừa học; máy không đọc được
│                                   thì tab Giờ chơi hiện thẻ "Nhờ Claude đọc vở". Giờ máy
│                                   không đọc được thì Lê Hòa tự gõ trên tablet
├── hop/nhacbai                 ◄── chỉ TABLET ghi, từ 30/09/2026: các dòng vở dặn dò chưa
│     cacBuoi[] { ma, ngay, ten,    tới hạn, gom theo buổi. Mỗi dòng hạn tới tiết sau của
│       vaoHoc, cac[] { chu, bai,   đúng môn đó (dòng không đọc ra môn thì tới buổi học kế
│       mon, ngayVo } }             tiếp). Tablet nhắc Lê Hòa từ đầu ngày trước buổi hạn;
│                                   tab Giờ chơi hiện thẻ "Bài dặn dò sắp tới". Hết dòng
│                                   thì tablet xoá
├── hop/sudung                  ◄── chỉ TABLET ghi, và chỉ khi nhận PING: sổ dùng app
│     doan[]    { goi, tu, den }: các khoảng Lê Hòa cầm máy, epoch ms theo giờ tablet
│     app[]     { goi, ten }: tên đọc được của từng app có trong doan
│     giuNgay   tablet giữ mấy ngày (7). Mỗi lần ghi đè cả bản, chỉ còn chừng ấy ngày
│     dangGhi   dịch vụ canh app có chạy không; tắt thì sổ trống mà không phải vì
│               Lê Hòa không dùng máy
│     capNhatLuc
│
├── lenh/{id}                   ◄── ĐIỆN THOẠI ghi, tablet đọc rồi xoá
│                                   Bảng điều khiển nghe cả hàng này: lệnh còn nằm đó là
│                                   tablet chưa lấy, tab Giờ chơi hiện ra kèm nút Rút lại
│                                   (xoá document trước khi tablet lấy). Quá 30 phút thì
│                                   ghi là tablet sẽ bỏ qua
│     kieu   DUYET TUCHOI CHO BOT DUNG TIEP KHOA MOMAY DONGMAY XOAPIN CAIDAT
│            NHAN        bỏ từ 27/09/2026 cùng khung chat trong app; tablet mới trả
│                        lời "Không hiểu lệnh NHAN"
│            CONGVIECNHA cộng bù một đợt việc nhà tablet đã bỏ lỡ. Bảng điều
│                        khiển bản mới không gửi nữa: nó bấm Gửi lại như máy bà
│            CHOGOAPP    tắt quản trị thiết bị để gỡ app
│            PING        hỏi tablet ngay, tablet đẩy một bản trạng thái đầy đủ
│                        và sổ dùng app
│            SUACHAM     sửa bản chấm đã có theo kết quả Claude chấm lại. giaTri là
│                        [{ ma, de, soDong }]; câu đúng mà vẫn thiếu số dòng thì tablet
│                        chưa ghi, câu còn chờ sửa (29/09/2026)
│            CHAMBAI     bản chấm đầu tiên do Claude chấm. Từ 28/09/2026 là đường chấm
│                        duy nhất: tablet không tự chấm nữa. giaTri là { cac }, mỗi câu
│                        { ma, dung, chac, conViet, goiY, soDong, de, dang, loaiLoi }
│            TINCO       tin của cô giáo, không bị bỏ vì quá cũ
│            DOCVO       bỏ từ 30/09/2026 (từng là kết quả Claude đọc tấm vở máy không
│                        đọc được); tablet trả lời là không nhận nữa
│            CAPQUY      cấp giờ từ quỹ giờ chơi (29/09/2026); thiếu phut hay 0 là cấp
│                        hết quỹ
│            XUCAU       Ba Huy tự xử câu Claude đọc chưa chắc hay chấm đúng mà không ghi
│                        số dòng (29/09/2026): Đúng kèm số dòng, Sai, hay Chụp lại.
│                        giaTri giống CHAMBAI: gửi lại nguyên bản Claude, chỉ đổi các câu
│                        vừa xử
│            BOSUA       bỏ câu sai khỏi danh sách cần sửa của con, không cộng
│                        phút (30/09/2026). giaTri là [{ ma, de }]
│            MODETHI     mở một đề thi in sẵn cho con, mã đề ở chu (30/09/2026).
│                        Tablet mở cả đề chưa tới phạm vi Unit
│     phut, baiId, chu, giaTri
│     tao    epoch ms theo đồng hồ máy gửi. Tablet dùng trường này để xếp lệnh
│            và bỏ lệnh quá nửa tiếng. taoLuc (server timestamp) chỉ Bảng điều
│            khiển ghi, tablet không đọc
│     ai     bahuy | banoi. Từ 26/09/2026 luật không cho máy bà tạo lệnh nào
│
├── hop/danhsachviec            ◄── BẢNG ĐIỀU KHIỂN ghi, máy bà đọc
│     viec[]    { ten, phut }, tối đa TOI_DA_VIEC (5) việc
│     luc, ai   lúc lưu, ai lưu
│               Chưa có document này thì hai máy dùng chung danh sách mặc định viết
│               sẵn trong app
│
├── hop/viecnha                 ◄── MÁY BÀ NỘI và BẢNG ĐIỀU KHIỂN ghi, bằng transaction;
│                                   tablet xoá khi đợt đã khép
│     maPhien   8 chữ số hex. Đổi mã là giao đợt mới, không phải sửa đợt đang chạy
│     luc       lúc bấm lần cuối, để tablet bỏ đợt nó chưa thấy mà đã quá nửa tiếng
│     viec[]    { ten, phut, xong }
│     ai        người giao đợt này, bahuy | banoi. Thiếu là banoi (máy bà bản cũ).
│               Chỉ để tablet gọi đúng người, không mở quyền gì
│               Đợt khép lại (xong hết, hoặc bỏ hết) thì tablet xoá document,
│               nếu maPhien vẫn là đợt đó. Hai điện thoại coi document biến mất là
│               tablet đã nhận.
│
├── bai/{baiId}                 ◄── TABLET ghi, riêng chamClaude và anKhoiDanhSach do
│                                   Bảng điều khiển ghi
│     luc, trangThai CHO|DUYET|TUCHOI|HUY, soPhut, messageId
│             HUY là con tự huỷ để chụp lại, hoặc tablet tự huỷ bài cũ sai hết khi con
│             nộp lại câu sai của chính bài đó (28/09/2026). Sang ngày mới tablet bỏ bài
│             chưa duyệt khỏi hàng chờ mà không đổi trangThai, nên bài nộp hôm trước còn
│             ghi CHO là bài đã hết chờ; Bảng điều khiển hiện nó là "Quá ngày"
│     lyDo    lý do Ba Huy không duyệt, đi cùng TUCHOI (30/09/2026). Tablet chép từ chu
│             của lệnh TUCHOI hay từ /tuchoi bên Telegram; màn Bài đã chấm trên tablet
│             hiện cho Lê Hòa. Không có lý do thì không có trường này
│     congLuc bài chấm xong trong giờ ngủ: lúc tablet sẽ cộng soPhut, tức lúc hết giờ
│             ngủ. Đi cùng DUYET; cộng xong tablet xoá trường này
│     anh[]   { fileId, khau: DAN_DO|DE_BAI|BAI_GIAI }. DAN_DO chỉ còn ở bài nộp trước
│             30/09/2026, bài mới chỉ có DE_BAI và BAI_GIAI
│             (Bảng điều khiển đọc được cả DANDO|DEBAI|BAIGIAI)
│     cham    bản chấm tablet ghi sau khi tính phút theo kết quả Claude (bài nộp
│             trước 28/09/2026 thì có thể là bản của máy chấm trên tablet):
│             { mon, tomTat, phutDeNghi, cac[], canXem[] }. canXem là các câu tablet
│             chưa tự cấp giờ được (29/09/2026): { ma, maClaude, lyDo CHUA_CHAC|
│             THIEU_DONG, canSoDong, soDong }; Bảng điều khiển hiện chúng ở thẻ "Câu cần
│             Ba Huy xem" và gửi lệnh XUCAU
│     khai    các câu con khai trước khi chụp, kèm đề:
│             { tenNguon, bai, mon, onTap, suaBai, cac[] }. suaBai chỉ có ở lần con
│             nộp lại các câu sai của một bài (nút "Nộp lại N câu sai" trên thẻ bài ở
│             màn kết quả tablet, từ 28/09/2026): là mã bài đó. Lúc ấy cac[] chỉ gồm
│             các câu sai, câu ngoài sách có cauId rỗng và đề chép từ bản chấm cũ. Lời
│             nhờ Claude chỉ cho chấm câu trong cac[], và tablet bỏ câu ngoài danh sách
│     danDo   (chỉ bài nộp trước 30/09/2026, lúc còn trọn gói 45 phút)
│             vở dặn dò của ngày, khi lần nộp không chụp trang vở: { ngay,
│             cacBai[], dongKhac[], fileId, chuaDoc, nguon, chupLuc }. Ảnh trang vở
│             cũng nằm cuối anh[] với khau DAN_DO. Lúc đó Claude chấm theo đúng ngày và
│             danh sách này; chuaDoc là chỉ có ảnh, Claude đọc ảnh lúc chấm và tablet
│             giữ lại lần đọc đầu tiên
│     chamClaude  kết quả Claude chấm mà Ba Huy dán vào: { luc, chinh, cac[], goi }.
│             Từ 28/09/2026 thường là bản chấm đầu tiên (chinh = true): Bảng điều khiển
│             ghi nó rồi gửi lệnh CHAMBAI. goi là nguyên giaTri đã gửi kèm lệnh, để thẻ
│             "Câu cần Ba Huy xem" gửi lại qua XUCAU; lệnh XUCAU ghi đè goi, cac và thêm
│             xuLuc. Dán lại kết quả Claude là ghi đè cả trường. Nằm cạnh cham, không
│             ghi đè lên nó
│     anKhoiDanhSach  true khi Ba Huy bấm Xoá ở tab Bài, false khi bấm Khôi phục
│             trong tấm "Bài đã xoá". Chỉ ẩn khỏi danh sách bên điện thoại; document
│             vẫn còn vì trang Bài đã chấm trên tablet đọc nó
│
├── socai/{cauId}_{luc}         ◄── chỉ TABLET ghi, mỗi lần chấm một câu là một document
│                               sổ cái, để cài lại app thì kéo về được (keoSoVe)
│
├── nhatky/{yyyy-MM-dd}         ◄── chỉ TABLET ghi, gộp cả ngày vào một document
└── hoiai/{yyyy-MM-dd}          ◄── chỉ TABLET ghi. Mảng dong, mỗi câu một phần tử
                                "dd/MM HH:mm  [app]  câu". Chỗ con xuống dòng ghi thành
                                " ↵ " (từ 28/09/2026), điện thoại đổi lại khi vẽ thẻ
```

Trước 27/09/2026 còn `chat/{id}`, hai bên cùng ghi, mỗi tin một document. Khung chat
trong app đã bỏ, và tablet bản mới xoá hết document cũ một lần lúc nối, xem "Lê Hòa
nhắn tin bằng Telegram thật".

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

### Sổ dùng app chỉ đi khi có người hỏi

Sổ "dùng app gì, lúc nào" trên tablet đổi vài phút một lần suốt lúc Lê Hòa cầm
máy. Đẩy theo từng lần đổi thì quay lại đúng cảnh vài trăm lượt ghi một ngày ở
trên. Nên tablet không nghe sổ này: nó chỉ ghi `hop/sudung` khi nhận `PING`, tức
là lúc Ba Huy mở app. Khoảng đang mở dở được tính đến lúc ghi, nên số không trễ.

Mỗi lần ghi là ghi đè cả bản, và bản đó chỉ có 7 ngày gần nhất. Ngày thứ tám rơi
khỏi Firestore ở lần PING kế tiếp, giống như nó rơi khỏi tablet ở lần ghi sổ kế
tiếp; không cần ai đi xoá. Tablet hỏng hẳn thì bản cuối cùng nằm lại, như mọi
document khác của nhà.

### Cấu hình đi một chiều

Nguồn sự thật của cấu hình vẫn là `Prefs` trên tablet. Điện thoại không ghi đè
lên `hop/caidat`; nó gửi lệnh `CAIDAT`, tablet nhận, áp vào `Prefs`, rồi ghi lại
`hop/caidat` cho đúng cái đang chạy. Một người ghi một chỗ thì không bao giờ có
cảnh hai bên đè nhau.

### Thời khoá biểu chép sang, không đi qua Firestore

Tab Lịch học (từ 29/09/2026) vẽ thời khoá biểu từ `ThoiKhoaBieu.kt` và `NgayNghi.kt`, hai
file chép y hệt từ tablet, chỉ khác dòng `package`. Bên tablet lịch nằm thẳng trong code, đổi
lịch là cài lại app bên đó, nên cài lại app này cùng lúc là hai máy hiện cùng một lịch. Đổi
lại, tab chạy với mọi bản tablet và không tốn lượt đọc nào. `tools/kiem-duong.sh` bên nop-bai
so hai bản này, cùng lúc so ba bản `Duong.kt`.

### Việc nhà là trạng thái, cho giờ là sự kiện

Cho giờ và việc nhà đi hai đường khác nhau, và khác vì bản chất khác:

- **cho giờ** là một sự kiện — một document trong `lenh/`, tablet làm xong thì xoá
  đi. Đọc hai lần là cộng giờ hai lần, nên phải xoá.
- **việc nhà** là trạng thái đầy đủ — cả danh sách việc lẫn việc nào đã xong nằm
  gọn trong `hop/viecnha`, mỗi lần bấm là ghi lại cả bản. Đọc lại cùng một bản mười
  lần cũng không sao, vì `ViecNha.apDung` bên tablet so với bản đang giữ rồi mới
  quyết. Nhờ thế listener bắn lại vì đổi mạng hay vì khởi động lại app cũng không
  sinh ra hai lần cộng giờ.

Đó cũng là lý do không nhét việc nhà vào hàng `lenh/`: cái vòng "làm xong rồi xoá"
sẽ ăn mất một bản trạng thái đang còn hiệu lực.

Từ 26/09/2026 Ba Huy cũng giao và bấm xong được trên Bảng điều khiển, nên hai điện
thoại cùng ghi `hop/viecnha`. Bản trước, máy bà giữ đợt việc trong máy rồi ghi đè cả
bản mỗi lần bấm, và chỉ nhìn Firestore để biết document còn hay mất. Hai máy cùng ghi
kiểu đó thì máy bà không thấy việc Ba Huy giao, và cái bấm tiếp theo của bà xoá mất
việc Ba Huy vừa báo xong. Nên giờ:

- cả hai máy vẽ màn hình theo document trên Firestore, không giữ bản riêng;
- mỗi lần bấm là một transaction: đọc bản trên máy chủ, kiểm `maPhien` còn đúng đợt
  đang nhìn, sửa đúng việc vừa bấm rồi mới ghi. Hai máy bấm cùng lúc thì Firestore
  bắt một bên làm lại trên bản mới. Đổi lại, lúc bấm phải có mạng;
- đang có đợt thì không giao đợt mới được, transaction từ chối. Không thì một máy
  giao đè lên đợt máy kia vừa giao mà không ai hay;
- đợt xong hết mà tablet chưa nhận thì hai máy đều có nút Gửi lại, chỉ ghi lại
  `luc`. Tablet nhận theo đường thường. Gửi lại hai lần cũng không cộng hai lần, vì
  tablet nhớ mã đợt vừa khép. Thẻ "tablet bỏ qua" và lệnh `CONGVIECNHA` của Bảng điều
  khiển bản trước thôi dùng.

Danh sách việc để chọn nằm ở `hop/danhsachviec`, Ba Huy sửa trên Bảng điều khiển, máy
bà chỉ đọc. Chưa lưu lần nào thì hai máy dùng chung một danh sách mặc định viết sẵn
trong app, nên vẫn hiện giống nhau.

### Máy bà nội bị chặn ở đâu

Hai lớp, và lớp thật nằm ở luật:

1. `firestore.rules` chỉ cho `uidsPhu` đọc và ghi `hop/viecnha`, đọc
   `hop/danhsachviec`. Mọi thứ khác từ chối, kể cả tạo document trong `lenh/` và đọc
   `hop/trangthai` (bỏ ngày 27/09/2026, app bà không còn đọc chỗ đó).
2. `ThiHanhLenh` bên tablet bỏ qua mọi lệnh khi `ai == banoi`.
   Lớp này chặn nhầm tay là chính: luật thì sửa bằng tay trong console Firebase ở
   một chỗ không ai nhìn thấy, còn dòng kiểm tra kia đi theo bản app.

Trước 26/09/2026 máy bà có sáu nút cho giờ, mỗi ngày một lượt, và luật cho máy bà
tạo lệnh `CHO` từ 1 đến 60 phút. Ngày đó app bà bỏ sáu nút, luật đóng cửa `lenh/`
của máy bà. Máy bà còn chạy bản cũ thì bấm nút giờ sẽ bị Firestore từ chối. Phần đếm
một lượt mỗi ngày bên tablet (`LuotBaNoi`) vẫn còn trong code nhưng không còn lệnh nào
của máy bà tới được đó.

## Các tab của Bảng điều khiển

Sáu tab ở `ui/MainActivity`. Tab đang ẩn bị hạ về `CREATED` để listener Firestore được
gỡ trong `onStop`.

- **Giờ chơi** (`BangFragment`, id `tab_bang`): trạng thái tablet và đồng hồ, cho thêm
  hay bớt giờ, quỹ giờ chơi với nút cấp quỹ (`CAPQUY`), hàng "Đề thi thử Tiếng Anh"
  (`MODETHI`), thẻ "Bài dặn dò sắp tới", các lệnh tablet chưa lấy kèm nút Rút lại. Mở tab
  là gửi `PING`.
- **Bài tập** (`BaiFragment`, màn bài `BaiActivity`): danh sách bài, ảnh tải từ Telegram,
  nút "Nhờ Claude chấm" và ô dán kết quả, thẻ "Câu cần Ba Huy xem" (`XUCAU`), nút bỏ câu
  sai khỏi danh sách cần sửa (`BOSUA`), duyệt hay không duyệt kèm lý do.
- **Việc nhà** (`ViecNhaFragment`): giao, bấm xong, bỏ việc như máy bà, và nút "Sửa danh
  sách" (`hop/danhsachviec`).
- **Nhật ký** (`NhatKyFragment`): nhật ký trong ngày (`nhatky/`), câu Lê Hòa hỏi AI
  (`hoiai/`), và màn Thời gian dùng app (`SuDungActivity`, đọc `hop/sudung`).
- **Lịch học** (`LichFragment`): thời khoá biểu cả tuần, xem mục "Thời khoá biểu chép
  sang".
- **Cài đặt** (`CaiDatFragment`): cấu hình tablet qua lệnh `CAIDAT`, token bot để tải ảnh,
  ghép máy và ghép ngược khi cài lại tablet.

Tab Nhắn bỏ ngày 27/09/2026, xem "Lê Hòa nhắn tin bằng Telegram thật".

## Nhờ Claude chấm

Từ 28/09/2026 tablet không tự chấm. Một bài đi như sau:

1. Lê Hòa chụp rồi gửi trên tablet. Tablet gửi ảnh lên Telegram, ghi `bai/{id}` kèm `khai`
   (các câu con khai, có đề) và nhắn Ba Huy.
2. Ở màn bài, Ba Huy bấm "Nhờ Claude chấm". `NhoClaude` gói ảnh, đề từng câu và lời dặn,
   chia sẻ sang app Claude trên điện thoại. Không gửi token.
3. Claude trả một khối JSON kèm mã bài. Ba Huy dán vào màn bài, `NhoClaude.docKetQua` đọc
   ra. Claude hay bọc khối trong code fence hay viết thêm chữ trước sau, nên đoạn đọc này có
   `NhoClaudeTest`.
4. Bảng điều khiển ghi `chamClaude` rồi gửi lệnh `CHAMBAI`. Tablet tính phút theo luật, bỏ
   câu đã trả giờ theo sổ cái, cấp giờ, ghi sổ, báo Telegram, rồi ghi `cham`.
5. Câu Claude đọc chưa chắc, hay chấm đúng mà không ghi số dòng, thì tablet chưa cấp giờ
   cho câu đó và ghi nó vào `cham.canXem`. Ba Huy xử từng câu ở thẻ "Câu cần Ba Huy xem"
   (Đúng kèm số dòng, Sai, Chụp lại), gửi `XUCAU`, và tablet chấm lại cả bài theo đúng
   đường của `CHAMBAI`.
6. Lần chấm trước nhầm thì dán kết quả Claude chấm lại, đi lệnh `SUACHAM`. Câu sai không
   muốn bắt con sửa nữa thì bấm bỏ, đi lệnh `BOSUA`, không cộng phút.

Lần con nộp lại câu sai của một bài (`khai.suaBai`) thì lời nhờ chỉ cho chấm câu trong
danh sách, và tablet bỏ mọi câu ngoài danh sách. Lần nộp thường thì lời nhờ dặn Claude
chấm thêm câu thấy trong ảnh mà con không khai. Câu thêm như vậy được ghi sổ theo chữ của
đề, nên một câu đã trả giờ mà lọt vào ảnh có thể được cộng lần nữa. Ba Huy chốt ngày
01/10/2026 coi như một lần ôn tập, không sửa.

## Lê Hòa nhắn tin bằng Telegram thật

Từ 27/09/2026 Lê Hòa có tài khoản Telegram riêng trên tablet. Nút "Nhắn cho ba Huy"
mở thẳng khung chat giữa tài khoản đó và tài khoản của Ba Huy, bằng link
`tg://openmessage?user_id=` với chat id cài lúc đặt bot. Tin không đi qua bot hay
Firestore nữa.

Telegram mở được lúc nào là theo các danh sách app, như mọi app khác. Nằm trong "Dùng
mọi lúc" thì Lê Hòa nhắn được cả giờ ngủ, giờ học, lúc làm việc nhà. Lần đầu chạy bản
có danh sách này, tablet tự thêm Telegram (nếu đã cài) vào đó; Ba Huy bỏ ra thì tablet
không thêm lại.

Màn chặn giờ học và việc nhà che kín màn hình, mà lúc đó Android không cho dịch vụ trợ
năng thấy cửa sổ bên dưới. Nên màn chặn chỉ nhường cho Telegram khi Telegram mở ra lúc
màn chặn đang tạm ẩn, tức là mở từ nút "Nhắn cho ba Huy" trong app Nộp bài. Đã nhường
thì giữ qua lúc tắt màn hình, màn khoá, thanh thông báo. Bấm thẳng vào thông báo
Telegram lúc màn chặn đang che thì màn chặn vẫn che: bấm vào màn chặn để vào Nộp bài rồi
bấm nút.

Tablet đọc thông báo của Telegram, chỉ mã khung chat và số tin, để nút ở màn chính
ghi "Ba Huy nhắn 3 tin mới".

Cùng ngày, Ba Huy bảo bỏ hẳn đường nhắn cũ: khung chat trong app Nộp bài, tab Nhắn ở
app này, lệnh `NHAN`, và chuyện bot chuyển chữ thường cho Lê Hòa. Ba gõ chữ thường
hay gửi ảnh cho bot thì bot trả lời là tin không tới Lê Hòa, nhắn thẳng trên Telegram.
Ba Huy bảo xoá luôn tin cũ. Tablet bản mới làm việc đó một lần: lúc nối Firestore thì
xoá từng lô document trong `chat/` đến khi rỗng rồi đánh dấu đã xong (hỏng giữa chừng
thì lần nối sau xoá tiếp); lúc app khởi động thì xoá các câu chat trong prefs, thư mục
ảnh chat và kênh thông báo "Tin nhắn của Ba Huy". Xem `DongBo.xoaChatCu` và `ChatCu`.

## Telegram còn lại gì

Còn cho Ba Huy, nhưng không còn ngang với Bảng điều khiển. Telegram vẫn gửi ảnh, vẫn
báo, và nhận các lệnh trong bảng `/trogiup`: duyệt, không duyệt, cho, bớt, cấp quỹ
(`/quy`), dừng, tiếp, khoá, xem trạng thái và nhật ký, soạn tập, và mấy lệnh phòng hờ.
Không có lệnh nào để chấm bài, sửa chấm, xử câu, bỏ câu sai, mở đề thi, giao việc nhà
hay đổi cài đặt; bảng ghi "sửa trong app". Từ 28/09/2026 tablet không tự chấm, nên điện
thoại hết pin hay mất máy thì bài chỉ còn duyệt tay bằng `/duyet`.

Telegram cũng chậm hơn trước (`ApprovalService.nhipNgheMs`). Lúc cổng khoá mà đường
Firestore còn sống, tablet chỉ hỏi Telegram 5 phút một lần, ban đêm 10 phút; màn hình tắt
mà không đang chơi cũng 5 phút. Lệnh gõ bên đó có thể chờ chừng ấy mới chạy. Đường
Firestore chết thì ban ngày tablet quay về nghe Telegram liên tục. Firestore là đường đi
hàng ngày, Telegram là đường lui.

Máy bà nội thì không còn đường Telegram nào: hộp thư mô tả nhóm đã gỡ hẳn, cùng
với `HopThuBaNoi` bên tablet và toàn bộ phần chống đọc trùng tự dựng của nó.
