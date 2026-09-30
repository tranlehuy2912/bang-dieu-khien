package vn.huytl.bangdieukhien.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONObject
import vn.huytl.bangdieukhien.data.Anh
import vn.huytl.bangdieukhien.data.Bai
import vn.huytl.bangdieukhien.data.CauClaude
import vn.huytl.bangdieukhien.data.KetQuaCham
import vn.huytl.bangdieukhien.data.KhaiBai
import vn.huytl.bangdieukhien.data.Nha
import vn.huytl.bangdieukhien.telegram.TaiAnh
import java.io.File
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gui mot lan nop bai sang app Claude tren dien thoai de cham lai.
 *
 * VI SAO CO CAI NAY. May cham tren tablet dung mot model nhe, va no doc nham lan
 * giai nham duoc. Bai nop luc 10:23 ngay 23/9/2026 la vi du: cau 2.32b bi doc chu
 * "b" thanh so 1 nen con dung ma thanh sai, con cau 2.33a thi may tu nhan sai roi
 * cham con sai. Ba Huy muon nho Claude cham lai ma khong phai ngoi truoc may Mac.
 * Tu 28/9/2026 Ba Huy bo han may cham tren tablet: bai nao cung do Claude cham, qua
 * dung duong nay.
 *
 * VI SAO LAM O APP NAY. Claude tren dien thoai khong tu lay duoc anh: bot Telegram
 * khong co lenh doc lai tin no da gui, va ma anh chi nam tren Firestore. App nay
 * thi co ca hai, cong them de tung cau trong ban cham. Nen app gom het vao mot lan
 * chia se: anh vo, de cac cau, chu may doc va cach cham. Ba Huy chi con bam gui.
 *
 * KHONG dua token bot sang Claude. Token dan vao chat la nam lai trong lich su chat,
 * ma ai co token la dieu khien duoc bot, ke ca giat mat lenh cua tablet.
 *
 * Loi nho con duoc chep vao bo nho tam. App nhan chia se co the bo qua phan chu khi
 * co anh di kem, luc do Ba Huy giu vao o chat roi dan.
 *
 * DUONG VE. Claude app khong ghi duoc len Firebase. Nen loi nho dan Claude in them
 * mot khoi JSON o cuoi cau tra loi, kem ma bai. Ba Huy chep cau tra loi, quay lai man
 * chi tiet bai bam "Dán kết quả của Claude", va [docKetQua] doc khoi do ra. Ma bai
 * trong khoi chan viec dan nham ket qua sang bai khac.
 *
 * HAI KIEU NHO. Bai chua co ban cham nao, hay da cham roi ma van cho duyet, thi Claude
 * CHAM LUON, va tablet tinh phut theo ban cua Claude. Bai da cham va da xu xong thi
 * Claude CHAM LAI: ket qua dan ve chi sua nhung cau lan cham truoc nham. Lan cham truoc
 * do la cua Claude, hay cua may cham tren tablet voi bai nop truoc 28/9/2026. Xem
 * [chamMoi].
 *
 * Truoc 30/9/2026 con mot loi nho doc vo dan do, cho tam vo may tren tablet doc khong duoc.
 * Tu do may doc khong duoc thi Le Hoa tu go tren tablet, nen loi nho do bo.
 */
object NhoClaude {

    /** Ten goi cua app Claude tren Google Play. */
    private const val GOI_CLAUDE = "com.anthropic.claude"

    private val VN = Locale.forLanguageTag("vi-VN")

    /**
     * Cac tam can gui cho Claude: moi tam tru trang vo dan do.
     *
     * Tu 30/9/2026 khong con tron goi 45 phut, nen Claude cham tung cau ma khong can biet
     * co giao bai gi. Bai nop truoc ngay do con mang anh trang vo; bo di cho Claude khoi
     * doc thua.
     */
    fun anhCanGui(bai: Bai): List<Anh> = bai.anh.filter { !it.laDanDo }

    /**
     * Lay file anh cua cac tam can gui, tai ve neu may chua co.
     *
     * Rong nghia la khong lay duoc tam nao: chua dat token bot, hoac mat mang.
     */
    suspend fun layAnh(context: Context, bai: Bai): List<File> {
        val token = Nha.token(context)
        if (token.isBlank()) return emptyList()
        return anhCanGui(bai).mapNotNull { TaiAnh.lay(context, token, it.fileId) }
    }

    /**
     * Claude cham ca bai va tablet tinh phut theo ban cua Claude, chu khong chi sua cau may
     * nham.
     *
     * Hai canh: bai chua co ban cham nao, hay da cham ma bai van cho duyet (doc chua chac,
     * sai het, tu duyet khong duoc). Canh sau truoc ngay 27/9/2026 di duong cham lai:
     * tablet cong gio cho cau cham nham, ma bai van nam "Dang cho duyet" voi nut Duyet,
     * nhin nhu chua duyet va de bam them lan nua. Tu 28/9/2026 tablet khong tu cham nua,
     * nen moi bai moi nop deu vao canh dau.
     *
     * Ket qua dan ve di bang lenh [vn.huytl.bangdieukhien.data.Lenh.CHAM_BAI]: tablet chay
     * ban cua Claude qua dung cac buoc nhu mot ban AI cham - gia moi cau, tran ngay, moi cau
     * chi tra gio mot lan, tin Telegram - roi tu duyet va dong bai.
     */
    fun chamMoi(bai: Bai): Boolean = bai.cham?.cac.isNullOrEmpty() || bai.dangCho

    /**
     * Loi nho Claude cham, viet nhu Ba Huy tu go.
     *
     * Cach cham dat TRUOC ket luan cua may. Dat sau thi Claude doc ket luan truoc
     * roi moi nhin anh, va de nghieng theo chu may da doc - dung cai loi can bat.
     */
    fun loiNho(bai: Bai, tenCon: String): String =
        if (chamMoi(bai)) loiNhoChamMoi(bai, tenGoi(tenCon)) else loiNhoChamLai(bai, tenGoi(tenCon))

    private fun tenGoi(tenCon: String): String =
        tenCon.trim().takeIf { it.isNotEmpty() && it != "con" } ?: "con tôi"

    /**
     * Ten dat o dau cau. Ten that thi giu nguyen, con "con tôi" thanh "Con tôi".
     *
     * Tu ngay 26/9/2026 loi nho goi con bang ten o moi cho, khong goi "con" nua, y nhu
     * Bang dieu khien va tablet.
     */
    private fun dauCau(ten: String): String = ten.replaceFirstChar { it.titlecase(VN) }

    /**
     * Loi nho khi Claude la nguoi cham duy nhat.
     *
     * De tung cau lay tu [KhaiBai] tablet ghi luc con nop, vi khong co ban cham nao de
     * chep de ra. Claude phai tra them hai thu may van tu dem: so dong lam bai, vi
     * tablet tinh phut theo so dong, va o lan on tap, bai co viet muc do khong. Cau
     * ngoai danh sach thi them de va dang bai, vi dang bai quyet gia cau do.
     *
     * Truoc 30/9/2026 lan nop co vo dan do thi hoi them ngay trong vo, cac bai co giao, con
     * da lam het chua, de tablet tinh tron goi 45 phut. Bo tron goi thi bo ca doan do.
     */
    private fun loiNhoChamMoi(bai: Bai, ten: String): String = buildString {
        val khai = bai.khai

        val mayDaCham = !bai.cham?.cac.isNullOrEmpty()
        // Lan nop lai cac cau sai cua mot bai truoc. Xem [KhaiBai.suaBai].
        val nopLai = khai?.suaBai?.isNotBlank() == true

        appendLine("$DAU_CHAM_MOI của $ten. Tôi là bố của $ten.")
        appendLine(
            if (mayDaCham) {
                "Bài này đã được chấm một lần nhưng chưa duyệt. Nhờ bạn chấm lại từ đầu, " +
                    "không dựa vào lần chấm trước: số phút chơi của $ten tính theo kết quả bạn chấm."
            } else {
                "Bạn là người chấm bài này. Số phút chơi của $ten tính theo kết quả bạn chấm."
            }
        )
        if (nopLai) {
            appendLine(
                "Đây là lần $ten nộp lại để sửa các câu làm sai ở một bài trước. $ten thường sửa " +
                    "ngay trên trang vở cũ, nên ảnh có thể còn những câu khác đã chấm ở lần trước."
            )
        }
        appendLine()

        dongNop(bai, khai?.mon ?: bai.cham?.mon)
        khai?.let { k ->
            listOf(k.tenNguon, k.bai).filter { it.isNotBlank() }.joinToString(" — ")
                .takeIf { it.isNotEmpty() }
                ?.let { appendLine("${dauCau(ten)} khai đang làm: $it.") }
        }
        val loaiAnh = buildList {
            if (bai.anh.any { it.laDeBai }) add("ảnh đề bài")
            add("ảnh vở bài làm của $ten")
        }
        appendLine(
            if (loaiAnh.size == 1) "Ảnh đính kèm là ${loaiAnh[0]}."
            else "Ảnh đính kèm gồm ${loaiAnh.dropLast(1).joinToString(", ")} và ${loaiAnh.last()}."
        )
        appendLine()

        cachCham(chamMoi = true, ten = ten)
        appendLine()

        if (khai != null) {
            appendLine(if (nopLai) "Các câu $ten nộp lại, kèm đề:" else "Các câu $ten khai, kèm đề:")
            khai.cac.forEachIndexed { i, c ->
                append(i + 1).append(". Câu ").append(c.ma).append(".")
                if (c.de.isNotBlank()) append(" Đề: ").append(c.de.trim())
                // Cau ngoai sach cua lan nop lai khong co dang trong ngan hang, ma dang quyet
                // gia cau do. Xem [coCauNgoaiSach].
                if (c.cauId.isBlank()) append(" (câu ngoài sách: ghi thêm \"dang\")")
                appendLine()
            }
            appendLine(
                "Mỗi câu trên có đúng một mục trong kết quả, giữ nguyên mã câu. Câu $ten " +
                    "chưa làm thì \"dung\" là false, \"con_viet\" để trống, \"so_dong\" là 0."
            )
            if (nopLai) {
                // Tablet cung bo cau ngoai danh sach cua lan nop lai (ChamTheoClaude ben do):
                // cau da dung tu lan truoc ma chep vao day thi so cai khong nhan ra, va con
                // duoc tra gio lan hai. Dan Claude truoc cho khoi ton cong cham.
                appendLine(
                    "Chỉ chấm đúng các câu trong danh sách trên. Câu khác thấy trong ảnh đã chấm " +
                        "ở lần trước: bỏ qua, không thêm mục nào cho chúng."
                )
            } else {
                appendLine(
                    "Ảnh có câu $ten làm mà không có trong danh sách thì thêm một mục cho câu " +
                        "đó: mã theo cách sách đánh số, chép đề vào \"de\", và ghi \"dang\"."
                )
            }
        } else if (mayDaCham) {
            // Chep ma va de may nhan ra, khong chep ket luan cua may. Giu ma thi ket qua khop
            // voi cau may da ghi, xem [theoMay].
            appendLine("${dauCau(ten)} không khai trước là làm câu nào. Lần chấm trước đã nhận ra các câu dưới đây trong ảnh:")
            bai.cham?.cac.orEmpty().forEachIndexed { i, c ->
                append(i + 1).append(". Câu ").append(c.ma.ifBlank { "chưa rõ mã" }).append(".")
                if (c.de.isNotBlank()) append(" Đề: ").append(c.de.trim())
                appendLine()
            }
            appendLine(
                "Giữ nguyên mã câu như trên. Ảnh có câu $ten làm mà máy bỏ sót thì thêm một mục. " +
                    "Mỗi câu chép đề vào \"de\" và ghi \"dang\". Câu nào ảnh không có đề thì để " +
                    "\"de\" trống và \"dung\" là false, vì không có đề thì không biết đúng sai."
            )
        } else {
            appendLine("${dauCau(ten)} không khai trước là làm câu nào. Nhờ bạn tự nhận ra các câu trong ảnh.")
            appendLine(
                "Mỗi câu chép đề vào \"de\" và ghi \"dang\". Câu nào ảnh không có đề thì " +
                    "để \"de\" trống và \"dung\" là false, vì không có đề thì không biết đúng sai."
            )
        }
        appendLine(DANG_BAI)
        appendLine()

        appendLine("Trả lời bằng tiếng Việt, gồm:")
        appendLine("1. Một bảng: mã câu, $ten viết, đáp án đúng, $ten đúng hay sai.")
        appendLine(
            "2. Với mỗi câu $ten làm sai: một đoạn gợi ý để $ten tự sửa. " + cachVietGoiY(ten)
        )
        appendLine("3. Số câu $ten làm đúng.")
        append(
            "4. Cuối cùng, in đúng một khối JSON theo mẫu dưới đây để tôi dán vào app. " +
                "Mỗi câu một mục trong \"ket_qua\". \"dung\" là kết luận của bạn. \"chac\" " +
                "là false nếu bạn không đọc chắc chữ $ten viết. \"con_viet\" là kết quả cuối " +
                "$ten viết, theo bạn đọc. \"so_dong\" là số dòng đếm ở bước 6: câu $ten làm " +
                "đúng mà không phải trắc nghiệm hay học thuộc thì luôn ghi một số lớn hơn 0, " +
                "không để 0 hay null, vì thiếu số dòng thì tôi phải tự đếm lại. \"goi_y\" chỉ " +
                "viết cho câu $ten làm sai: chép nguyên đoạn gợi ý ở mục 2. \"loai_loi\" chỉ " +
                "ghi cho câu $ten làm sai: đúng một trong bảy nhãn dưới đây, chép đúng chữ in hoa."
        )
        appendLine()
        LOAI_LOI.forEach { appendLine(it) }
        val maMau = khai?.cac?.firstOrNull()?.ma ?: "1"
        // Mau CO Y khong phai JSON hop le, cung ly do voi mau o [loiNhoChamLai].
        append("{\"bai\":\"${bai.id}\",")
        append("\"ket_qua\":[{\"ma\":\"$maMau\",\"dung\":true hoặc false,")
        append("\"chac\":true hoặc false,\"con_viet\":\"...\",\"so_dong\":số dòng,\"goi_y\":\"...\",")
        append("\"loai_loi\":\"một nhãn ở trên hoặc chuỗi rỗng\"")
        if (khai == null) append(",\"de\":\"chép đề câu đó\",\"dang\":\"CAU_NHO\"")
        else if (coCauNgoaiSach(khai)) append(",\"dang\":\"CAU_NHO\"")
        append("}]}")
    }

    /**
     * Loi nho khi bai da cham va da xu xong, Claude cham lai de bat cho lan truoc nham.
     *
     * Lan truoc la Claude, hay may cham tren tablet voi bai nop truoc 28/9/2026: loi nho
     * goi chung la "lần chấm trước", dung cho ca hai.
     */
    private fun loiNhoChamLai(bai: Bai, ten: String): String = buildString {
        val cham = bai.cham

        appendLine("$DAU_LOI_NHO của $ten. Tôi là bố của $ten.")
        appendLine(
            "Bài này đã được chấm một lần, nhưng lần chấm đó có thể đọc nhầm chữ hay chấm " +
                "nhầm, nên tôi cần bạn chấm lại độc lập."
        )
        appendLine()

        dongNop(bai, cham?.mon)
        conKhai(cham, ten)?.let { appendLine("${dauCau(ten)} khai đang làm: $it.") }
        appendLine("Ảnh đính kèm là ảnh vở bài làm của $ten.")
        appendLine()

        cachCham(chamMoi = false, ten = ten)
        appendLine()

        if (cham == null || cham.cac.isEmpty()) {
            appendLine("Bài này chưa có bản chấm nào. Nhờ bạn tự nhận ra các câu trong ảnh rồi chấm.")
        } else {
            appendLine("Các câu cần chấm, kèm đề và kết luận của lần chấm trước:")
            cham.cac.forEachIndexed { i, c ->
                append(i + 1).append(". Câu ").append(c.ma.ifBlank { "chưa rõ mã" }).append(".")
                if (c.de.isNotBlank()) append(" Đề: ").append(c.de.trim())
                append(" Lần trước đọc $ten viết: ").append(c.ketQua.ifBlank { "không đọc ra" }).append(".")
                append(" Lần trước chấm: ")
                append(
                    when {
                        !c.docRo -> "đọc không rõ"
                        c.dung -> "đúng"
                        else -> "sai"
                    }
                )
                append(".")
                if (c.nhanXet.isNotBlank()) append(" Gợi ý lần trước: ").append(c.nhanXet.trim())
                appendLine()
            }
        }
        appendLine()

        appendLine("Trả lời bằng tiếng Việt, gồm:")
        appendLine("1. Một bảng: mã câu, $ten viết, đáp án đúng, $ten đúng hay sai, lần chấm trước đúng hay nhầm.")
        appendLine(
            "2. Các câu lần trước đọc nhầm chữ, các câu lần trước chấm nhầm đúng sai, và các " +
                "câu chấm đúng nhưng gợi ý chỉ sai chỗ."
        )
        appendLine(
            "3. Với mỗi câu $ten làm sai: một đoạn gợi ý để $ten tự sửa. " + cachVietGoiY(ten)
        )
        appendLine("4. Số câu $ten làm đúng thật.")
        appendLine(
            "5. Cuối cùng, in đúng một khối JSON theo mẫu dưới đây để tôi dán vào app. " +
                "Mỗi câu một mục trong \"ket_qua\", giữ nguyên mã câu như trên. \"dung\" là " +
                "kết luận của bạn. \"chac\" là false nếu bạn không đọc chắc chữ $ten viết. " +
                "\"con_viet\" là kết quả cuối $ten viết, theo bạn đọc. \"goi_y\" chỉ viết " +
                "cho câu $ten làm sai: chép nguyên đoạn gợi ý ở mục 3."
        )
        val maMau = cham?.cac?.firstOrNull()?.ma?.takeIf { it.isNotBlank() } ?: "2.28"
        // Mau CO Y khong phai JSON hop le: "true hoặc false" khong doc duoc. Bam nut
        // nho Claude la loi nho nay nam san trong bo nho tam, va neu Ba Huy quen chep
        // cau tra loi cua Claude ma bam dan luon, mot mau doc duoc se thanh ket qua
        // that - cau mau thanh dung, tablet cong gio oan.
        append(
            "{\"bai\":\"${bai.id}\",\"ket_qua\":[{\"ma\":\"$maMau\",\"dung\":true hoặc false," +
                "\"chac\":true hoặc false,\"con_viet\":\"...\",\"goi_y\":\"...\"}]}"
        )
    }

    private fun StringBuilder.dongNop(bai: Bai, mon: String?) {
        append("Bài nộp lúc ")
        append(SimpleDateFormat("HH:mm 'ngày' dd/MM/yyyy", VN).format(Date(bai.luc)))
        mon?.takeIf { it.isNotBlank() }?.let { append(", môn ").append(it) }
        appendLine(".")
    }

    /**
     * Cach cham tung cau. Hai kieu nho dung chung, chi khac buoc 3 va buoc 6.
     *
     * Buoc 4 lay dung luat cua may cham: dung la moi buoc deu dung. Claude chi xet ket
     * qua cuoi thi mot bai sai o giua ma ra dung dap an se thanh dung, va con duoc gio
     * cho mot loi giai sai.
     */
    private fun StringBuilder.cachCham(chamMoi: Boolean, ten: String) {
        appendLine("Cách chấm từng câu:")
        appendLine(
            "1. Nhìn kỹ riêng câu đó trong ảnh. Nếu chạy được code thì cắt vùng ảnh của " +
                "câu rồi phóng to. Chép đúng chữ $ten viết ở kết quả cuối, kể cả khi sai."
        )
        appendLine(
            "2. Cẩn thận với nhãn câu viết sát con số. Chữ b viết liền trước một con số " +
                "rất dễ bị đọc thành số 1."
        )
        appendLine(
            if (chamMoi) {
                "3. Tự giải câu đó từng bước trước, rồi mới so với bài của $ten. Nếu chạy " +
                    "được code thì kiểm các phép biến đổi bằng sympy."
            } else {
                "3. Tự giải lại câu đó từng bước. Chỉ so với kết luận của máy sau khi đã tự " +
                    "chấm xong. Nếu chạy được code thì kiểm các phép biến đổi bằng sympy."
            }
        )
        appendLine(
            "4. ${dauCau(ten)} đúng khi mọi bước đều đúng và kết quả cuối đúng hoàn toàn. Sai một dấu " +
                "hay một bước là sai. Câu phân tích thành nhân tử phải phân tích hết mới " +
                "tính là đúng."
        )
        appendLine("5. Chỗ nào mờ, không đọc chắc được thì ghi rõ là không chắc, không đoán.")
        if (chamMoi) {
            // Tu 29/9/2026 bai dan do hom chua co goi tinh mot phut mot dong, khong con san
            // phut moi cau. Cau dung ma thieu so dong ra 0 phut, va tablet de ca bai cho Ba Huy
            // tu dem (cham.canXem). Hinh ve, bang, so do thi truoc day Claude hay de 0 vi khong
            // co "dong chu" nao, nen noi ro cach dem.
            appendLine(
                "6. Đếm số dòng $ten tự viết để làm câu đó. Không tính dòng chép lại đề, " +
                    "dòng trống, dòng đã gạch xoá, dòng lặp lại vô nghĩa. Hình vẽ, bảng, sơ đồ " +
                    "$ten vẽ thì tính theo số dòng vở mà nó chiếm. Câu $ten làm đúng mà không " +
                    "phải trắc nghiệm hay học thuộc thì luôn có ít nhất 1 dòng."
            )
        }
    }

    /**
     * Cach xep dang bai, chep tu cau lenh cua may cham ben tablet.
     *
     * Dang bai quyet gia moi cau: trac nghiem tinh theo cum, hoc thuoc khong tinh. Cau
     * trong sach da co dang trong ngan hang, nen Claude chi can xep cau ngoai sach.
     */
    /**
     * Bay kieu sai, chep y nguyen tap nhan LoaiLoi ben tablet (Ba Huy muon Claude tra
     * nhan ngay 28/9/2026, sau khi bo may cham - truoc do chi may cham tren tablet dat
     * nhan). Man Tien bo va "Luyện chỗ hay vấp" tren tablet cong don theo nhan, nen tap
     * nhan phai dong: nhan la bi ep ve KHAC. Them hay doi nhan thi sua ca hai cho.
     */
    private val LOAI_LOI = listOf(
        "SAI_DAU: sai dấu, mất dấu, nhầm dấu khi chuyển vế hay khi phá ngoặc.",
        "SAI_BUOC: một bước biến đổi hay một bước lập luận sai, các bước khác đúng.",
        "NHAM_CONG_THUC: dùng nhầm công thức, quy tắc, định nghĩa, hằng đẳng thức.",
        "TINH_NHAM: cộng trừ nhân chia ra số sai, cách làm vẫn đúng.",
        "THIEU: thiếu trường hợp, thiếu điều kiện, thiếu kết luận, hoặc bỏ dở giữa chừng.",
        "LAC_DE: làm lệch cái đề hỏi, trả lời sang chuyện khác.",
        "KHAC: sai mà không thuộc sáu nhãn trên."
    )

    private const val DANG_BAI = "\"dang\" là một trong: TRAC_NGHIEM (chỉ khoanh, ghi Đúng/Sai, " +
        "nối cột, điền một từ), CAU_NHO (câu nhỏ có trình bày lời giải), BAI_RIENG (bài " +
        "đứng riêng), VIET_DAI (đoạn văn, bài văn), KHONG_TINH (học thuộc, luyện chữ, chép bài)."

    /**
     * Cach viet goi y cho cau sai, dung chung cho hai kieu nho.
     *
     * Goi y hien thang tren man cua con. Truoc ngay 26/9/2026 loi nho dan Claude goi con
     * la "con", nen man do hien "Con sửa ...". Ba Huy muon no chi ghi "Sửa ...", cho nao
     * can goi thi goi ten, y nhu cac cau khac tren tablet.
     *
     * Truoc ngay 28/9/2026 goi y toi da 15 chu, va Ba Huy doc nhieu luc khong hieu no noi
     * gi. Gio viet cu the: dong nao, sai gi, vi sao, lam lai buoc nao. Van khong dua dap
     * so: con nop lai dung la duoc cong gio, co san dap so thi chep vao la xong.
     *
     * Cam dau ngoac kep trong goi y: goi y dai co khi trich chu con viet, va mot dau
     * ngoac kep quen thoat la ca khoi JSON dan ve doc khong duoc.
     */
    private fun cachVietGoiY(ten: String): String = "Gợi ý hiện thẳng trên tablet cho $ten " +
        "đọc, nên viết cụ thể, từ 2 đến 4 câu: chép lại đúng đoạn $ten viết sai và nói nó ở " +
        "dòng mấy, nói sai gì và vì sao sai (sai dấu nào, dùng nhầm quy tắc hay công thức " +
        "nào), rồi nói bước nào phải làm lại. Không viết đáp số cuối cùng và không giải hộ: " +
        "$ten sửa đúng mới được cộng giờ. Mở đầu bằng số dòng hoặc việc cần làm, không mở " +
        "đầu bằng \"Con\"; cần nhắc tới $ten thì gọi tên, không gọi \"con\". Trong gợi ý " +
        "không dùng dấu ngoặc kép, cần trích chữ thì để trong ngoặc đơn. Ví dụ: Dòng 2 viết " +
        "(x - 3)^2 + y^2, sai dấu trước y^2: biểu thức ban đầu là hiệu hai bình phương nên " +
        "phải là dấu trừ. Viết lại dòng 2 rồi phân tích tiếp."

    /** Dong mo dau cua loi nho cham lai, de nhan ra bo nho tam dang giu loi nho chu khong phai tra loi. */
    private const val DAU_LOI_NHO = "Nhờ bạn chấm lại bài tập về nhà"

    /** Dong mo dau cua loi nho cham luon. Khong chua [DAU_LOI_NHO] nen phai kiem rieng. */
    private const val DAU_CHAM_MOI = "Nhờ bạn chấm bài tập về nhà"

    /**
     * Bo nho tam dang giu chinh loi nho gui Claude, chua phai cau tra loi.
     *
     * Canh de xay ra nhat: bam "Nhờ Claude chấm lại", gui trong Claude, doc xong quay
     * lai bam dan ma quen chep cau tra loi.
     */
    fun laLoiNho(chu: String?): Boolean =
        chu != null && (chu.contains(DAU_LOI_NHO) || chu.contains(DAU_CHAM_MOI))

    /** Ket qua Claude cham, doc tu cau tra loi Ba Huy chep tu app Claude. */
    data class KetQuaDan(
        /** Ma bai Claude chep lai tu loi nho. Rong la Claude khong ghi. */
        val bai: String,
        val cac: List<CauClaude>
    )

    /**
     * Tim khoi JSON o cuoi cau tra loi cua Claude, doc ra ket luan tung cau.
     *
     * Nut chep trong app Claude chep ca cau tra loi: bang, loi giai thich, va khoi
     * JSON nam trong khung code. Nen o day khong doi chu phai la JSON tron: tim chu
     * "ket_qua" cuoi cung, lui dan tung dau ngoac nhon mo, dem ngoac de lay dung mot
     * khoi, va lay khoi dau tien doc duoc ma co mang "ket_qua". Phai lui dan chu khong
     * lay luon ngoac gan nhat: dung truoc "ket_qua" con cac truong cua vo dan do, va
     * ten mot bai co giao co the chua dau ngoac. Dem ngoac thi bo qua ngoac nam trong
     * chuoi, vi goi y cua Claude co the chep lai mot bieu thuc co ngoac.
     *
     * Tra null khi khong thay khoi nao doc duoc: bo nho tam dang giu thu khac, hay
     * Claude quen in khoi JSON.
     */
    fun docKetQua(chu: String?): KetQuaDan? = timKhoi(chu, "ket_qua")?.let { docKhoi(it) }

    /** Khoi JSON cuoi cung co mang [khoa] trong cau tra loi chep tu app Claude. Xem [docKetQua]. */
    private fun timKhoi(chu: String?, khoa: String): JSONObject? {
        if (chu.isNullOrBlank()) return null
        val moc = chu.lastIndexOf("\"$khoa\"")
        if (moc < 0) return null
        var dau = chu.lastIndexOf('{', moc)
        while (dau >= 0) {
            val o = khoiTu(chu, dau)
            if (o?.optJSONArray(khoa) != null) return o
            dau = chu.lastIndexOf('{', dau - 1)
        }
        return null
    }

    /** Khoi JSON mo ngoac dung o [dau], dem ngoac toi ngoac dong cua chinh no. */
    private fun khoiTu(chu: String, dau: Int): JSONObject? {
        var sau = 0
        var trongChuoi = false
        var thoat = false
        for (i in dau until chu.length) {
            val ch = chu[i]
            if (trongChuoi) {
                when {
                    thoat -> thoat = false
                    ch == '\\' -> thoat = true
                    ch == '"' -> trongChuoi = false
                }
                continue
            }
            when (ch) {
                '"' -> trongChuoi = true
                '{' -> sau++
                '}' -> {
                    sau--
                    if (sau == 0) {
                        return runCatching { JSONObject(chu.substring(dau, i + 1)) }.getOrNull()
                    }
                }
            }
        }
        return null
    }

    private fun docKhoi(o: JSONObject): KetQuaDan? {
        val mang = o.optJSONArray("ket_qua") ?: return null
        val cac = (0 until mang.length()).mapNotNull { i ->
            val c = mang.optJSONObject(i) ?: return@mapNotNull null
            val ma = c.chuoi("ma")
            if (ma.isEmpty()) return@mapNotNull null
            /*
             * "dung" phai la true hay false that, khong phai chu.
             *
             * org.json doc de dai: gap "dung":true hoặc false no doc ra mot chuoi chu
             * khong bao loi, va optBoolean lai doan tu chuoi. Mot cau khong co ket luan
             * ro rang thi khong mang thong tin gi, bo di. "chac" viet sai thi coi la
             * khong chac, de cau do giu theo may.
             */
            val dung = c.opt("dung") as? Boolean ?: return@mapNotNull null
            val chac = if (c.has("chac")) c.opt("chac") as? Boolean ?: false else true
            CauClaude(
                ma = ma,
                dung = dung,
                chac = chac,
                conViet = c.chuoi("con_viet"),
                goiY = c.chuoi("goi_y"),
                // So dong chi doi so phut, khong doi dung sai, nen doc de dai: "4" cung la 4.
                soDong = c.optInt("so_dong", 0).coerceAtLeast(0),
                de = c.chuoi("de"),
                dang = c.chuoi("dang").uppercase(Locale.ROOT),
                // Tablet ep nhan la ve KHAC va bo nhan cua cau dung (LoaiLoi.doc ben do).
                loaiLoi = c.chuoi("loai_loi").uppercase(Locale.ROOT)
            )
        }
        if (cac.isEmpty()) return null
        return KetQuaDan(o.chuoi("bai"), cac)
    }

    /**
     * Doc mot truong chu. Khong co truong, hay truong la null, thi ra chuoi rong.
     *
     * KHONG dung optString: org.json ban Android doi null thanh chu "null", da thu tren
     * may ao ngay 24/9/2026. Claude hay ghi null cho cho khong co gi, va luc do goi y
     * "null" hien cho con doc, con de "null" lam tablet tuong cau do co de. Kiem thu
     * JVM dung ban org.json khac, tra chuoi rong, nen khong bat duoc loi nay. So thi
     * van doc ra chu: "con_viet": 5 la "5".
     */
    private fun JSONObject.chuoi(ten: String): String =
        if (isNull(ten)) "" else opt(ten).toString().trim()

    /**
     * Ma cau da bo het nhung cach viet khac nhau cua cung mot cau.
     *
     * Claude co khi chep "2.33A", "2.33 a", "Câu 2.33a" hay "2.33a)" thay cho "2.33a".
     * Chi bo nhung thu khong bao gio phan biet hai cau: hoa thuong, khoang trang, chu
     * "câu" hay "bài" o dau, dau cham, ngoac dong va hai cham o cuoi.
     *
     * Y het ChamTheoClaude.chuanMa ben tablet. Sua ben nay thi sua ca ben do.
     */
    internal fun chuanMa(ma: String): String {
        val t = Normalizer.normalize(ma, Normalizer.Form.NFC).lowercase().filterNot { it.isWhitespace() }
        val dau = listOf("câu", "cau", "bài", "bai").firstOrNull { t.startsWith(it) }
        return (if (dau == null) t else t.removePrefix(dau)).trimEnd('.', ')', ']', ':')
    }

    /**
     * Dua ket qua Claude ve dung ma, de va dang bai cua cac cau con khai.
     *
     * Ma lech cach viet thi doi ve ma trong khai: so y het thi cau do khong khop cau nao,
     * tablet mat de va tra 0 phut du con lam dung. Cau con khai thi Claude khong chep de,
     * nen dien de va dang tu khai: tablet van tinh dung cau do ke ca khi no mat ban khai
     * cua minh. Cau khong khop cau khai nao thi giu nguyen, do la cau ngoai sach.
     */
    fun theoKhai(ket: KetQuaDan, khai: KhaiBai?): KetQuaDan {
        if (khai == null) return ket
        val daDung = mutableSetOf<String>()
        val nopLai = khai.suaBai.isNotBlank()
        return ket.copy(cac = ket.cac.mapNotNull { c ->
            val k = khai.cac.firstOrNull { chuanMa(it.ma) == chuanMa(c.ma) && it.ma !in daDung }
            // Lan nop lai cac cau sai: cau ngoai danh sach la cau da cham o lan truoc, con
            // sua de len cung trang. Bo di, tablet cung bo (ChamTheoClaude ben do).
                ?: return@mapNotNull if (nopLai) null else c
            daDung += k.ma
            c.copy(ma = k.ma, de = c.de.ifBlank { k.de }, dang = c.dang.ifBlank { k.dang })
        })
    }

    /** Phan khai co cau ngoai sach (cauId rong), chi co o lan nop lai. Xem [KhaiBai.suaBai]. */
    private fun coCauNgoaiSach(khai: KhaiBai): Boolean = khai.cac.any { it.cauId.isBlank() }

    /**
     * Dua cau Claude cham ve dung ma va de cua cau may da cham, voi cau khong nam trong khai.
     *
     * So cai ben tablet giu cau ngoai sach theo de bai (SoCaiBai.khoaCua ben do), ma Claude
     * chep de moi lan mot khac. Lay de cua Claude thi cau may cham sai van nam "can sua" tren
     * man con, du Claude cham dung va tablet da tinh phut cho no. Cau trong khai thi da khop
     * qua [theoKhai], cau may khong co thi giu nguyen.
     */
    fun theoMay(ket: KetQuaDan, bai: Bai): KetQuaDan {
        val cuaMay = bai.cham?.cac.orEmpty().filter { it.ma.isNotBlank() && it.de.isNotBlank() }
        if (cuaMay.isEmpty()) return ket
        val khai = bai.khai?.cac.orEmpty().map { chuanMa(it.ma) }.toSet()
        val daDung = mutableSetOf<Int>()
        return ket.copy(cac = ket.cac.map { c ->
            if (chuanMa(c.ma) in khai) return@map c
            val i = cuaMay.indices.firstOrNull { chuanMa(cuaMay[it].ma) == chuanMa(c.ma) && it !in daDung }
                ?: return@map c
            daDung += i
            c.copy(ma = cuaMay[i].ma.trim(), de = cuaMay[i].de)
        })
    }

    /**
     * "giaTri" cua lenh [vn.huytl.bangdieukhien.data.Lenh.CHAM_BAI] cho ket qua Claude vua
     * dan, da qua [theoKhai] va [theoMay]. Kieu goi xem chu thich cua lenh do trong Duong.
     *
     * Tach khoi BaiActivity ngay 29/9/2026: ban nay con duoc giu nguyen trong chamClaude.goi
     * (xem [vn.huytl.bangdieukhien.data.KetQuaClaude.goi]), de lenh XU_CAU gui lai dung no,
     * chi doi cau Ba Huy tu cham.
     *
     * Truoc 30/9/2026 goi nay con mang phan vo dan do (coAnhDanDo, ngayDanDo, baiDuocGiao,
     * lamHetDanDo, trongDanDo tung cau) de tablet tinh tron goi. Bo tron goi thi bo het.
     */
    fun goiChamBai(ket: KetQuaDan, bai: Bai): Map<String, Any> = buildMap<String, Any> {
        put("cac", ket.cac.map {
            buildMap<String, Any> {
                put("ma", it.ma)
                put("dung", it.dung)
                put("chac", it.chac)
                put("conViet", it.conViet)
                put("goiY", it.goiY)
                put("soDong", it.soDong)
                put("de", it.de)
                put("dang", it.dang)
                if (it.loaiLoi.isNotBlank()) put("loaiLoi", it.loaiLoi)
            }
        })
    }

    /**
     * Mo app Claude voi anh va loi nho. May khong co app Claude thi hien bang chon app.
     *
     * [anh] phai co it nhat mot tam: nguoi goi kiem truoc, vi Claude khong co anh vo
     * thi khong cham duoc gi.
     */
    fun mo(context: Context, anh: List<File>, loiNho: String) {
        require(anh.isNotEmpty()) { "phai co it nhat mot tam anh" }

        (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
            ?.setPrimaryClip(ClipData.newPlainText("Lời nhờ Claude chấm", loiNho))

        val uris = ArrayList<Uri>(anh.map { FileProvider.getUriForFile(context, "${context.packageName}.anh", it) })
        val gui = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0])
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }.apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_TEXT, loiNho)
            // Quyen doc anh di theo ClipData chu khong theo EXTRA_STREAM. Thieu dong
            // nay thi app nhan mo ra thay anh hong, tuy may.
            clipData = ClipData.newRawUri("", uris[0]).apply {
                uris.drop(1).forEach { addItem(ClipData.Item(it)) }
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(Intent(gui).setPackage(GOI_CLAUDE))
        } catch (e: ActivityNotFoundException) {
            context.startActivity(Intent.createChooser(gui, "Gửi cho Claude"))
        }
    }

    /**
     * Dong "Lê Hòa khai: ..." trong tom tat cua tablet, bo dau cham dau dong.
     *
     * Tablet truoc ngay 26/9/2026 ghi "Con khai:", tu do ghi ten con. Ban cham cu tren
     * Firestore van giu kieu cu, va tablet chua cai ban moi cung vay, nen doc ca hai.
     */
    private fun conKhai(cham: KetQuaCham?, ten: String): String? {
        val dau = listOf("Con khai:", "$ten khai:").map { nfc(it) }
        return cham?.tomTat?.lineSequence()
            ?.map { nfc(it.trim().removePrefix("•").trim()) }
            ?.firstNotNullOfOrNull { dong -> dau.firstOrNull { dong.startsWith(it) }?.let { dong.removePrefix(it) } }
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    private fun nfc(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFC)
}
