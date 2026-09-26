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
import vn.huytl.bangdieukhien.data.VoDaSoat
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
 * HAI KIEU NHO. Bai da co ban cham cua may thi Claude CHAM LAI: ket qua dan ve chi sua
 * nhung cau may nham. Bai chua co ban cham nao - tablet tat cham AI, hay AI hong luc
 * con nop - thi Claude CHAM LUON, va ket qua dan ve la ban cham dau tien: tablet tinh
 * phut theo no. Xem [chamMoi].
 */
object NhoClaude {

    /** Ten goi cua app Claude tren Google Play. */
    private const val GOI_CLAUDE = "com.anthropic.claude"

    private val VN = Locale.forLanguageTag("vi-VN")

    /**
     * Cac tam can gui cho Claude.
     *
     * Cham lai thi bo vo dan do: may da doc no va da tinh xong tron goi, Claude chi can
     * soat bai lam. Cham luon thi gui ca vo dan do, dat len dau: Claude la nguoi duy
     * nhat doc duoc co giao bai gi, va tron goi 45 phut dua vao do.
     */
    fun anhCanGui(bai: Bai): List<Anh> =
        if (chamMoi(bai)) bai.anh.sortedBy { if (it.laDanDo) 0 else 1 }
        else bai.anh.filter { !it.laDanDo }

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
     * Bai nay chua co ban cham nao cua may, nen Claude cham luon chu khong cham lai.
     *
     * Luc do ket qua dan ve di bang lenh [vn.huytl.bangdieukhien.data.Lenh.CHAM_BAI]:
     * tablet chay ban cua Claude qua dung cac buoc nhu mot ban AI cham - gia moi cau,
     * tran ngay, moi cau chi tra gio mot lan, tin Telegram.
     */
    fun chamMoi(bai: Bai): Boolean = bai.cham?.cac.isNullOrEmpty()

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
     * Loi nho khi Claude la nguoi cham duy nhat.
     *
     * De tung cau lay tu [KhaiBai] tablet ghi luc con nop, vi khong co ban cham nao de
     * chep de ra. Claude phai tra them hai thu may van tu dem: so dong lam bai, vi
     * tablet tinh phut theo so dong, va o lan on tap, bai co viet muc do khong. Cau
     * ngoai danh sach thi them de va dang bai, vi dang bai quyet gia cau do.
     *
     * Lan nop co trang vo dan do thi hoi them dung ba thu may cham van doc ra - ngay
     * trong vo, cac bai co giao, con da lam het chua - va cau nao thuoc bai co giao.
     * Quy tac viet theo cau lenh cua may cham ben tablet, de tron goi tinh nhu cu.
     *
     * Lan nop dung ban vo con soat tu dau buoi ([Bai.voDaSoat]) thi chep san ngay va
     * danh sach bai, va chi hoi hai thu: con lam het chua, cau nao thuoc bai co giao. Xem
     * [voDaSoat].
     */
    private fun loiNhoChamMoi(bai: Bai, ten: String): String = buildString {
        val khai = bai.khai
        val onTap = khai?.onTap == true
        val soat = bai.voDaSoat
        val coAnhVo = bai.anh.any { it.laDanDo }
        val coVo = soat != null || coAnhVo

        appendLine("$DAU_CHAM_MOI của $ten. Tôi là bố của con.")
        appendLine(
            "Hôm nay máy trên tablet không tự chấm, bạn là người chấm duy nhất bài này. " +
                "Số phút chơi của con tính theo kết quả bạn chấm."
        )
        appendLine()

        dongNop(bai, khai?.mon ?: bai.cham?.mon)
        khai?.let { k ->
            listOf(k.tenNguon, k.bai).filter { it.isNotBlank() }.joinToString(" — ")
                .takeIf { it.isNotEmpty() }
                ?.let { appendLine("Con khai đang làm: $it.") }
        }
        val loaiAnh = buildList {
            if (coAnhVo) add("trang vở dặn dò")
            if (bai.anh.any { it.laDeBai }) add("ảnh đề bài")
            add("ảnh vở bài làm của con")
        }
        appendLine(
            if (loaiAnh.size == 1) "Ảnh đính kèm là ${loaiAnh[0]}."
            else "Ảnh đính kèm gồm ${loaiAnh.dropLast(1).joinToString(", ")} và ${loaiAnh.last()}."
        )
        if (onTap) appendLine("Lần này con ôn lại bài cũ. Nhà quy định bài ôn phải viết bằng mực đỏ.")
        appendLine()

        cachCham(chamMoi = true)
        appendLine()

        if (khai != null) {
            appendLine("Các câu con khai, kèm đề:")
            khai.cac.forEachIndexed { i, c ->
                append(i + 1).append(". Câu ").append(c.ma).append(".")
                if (c.de.isNotBlank()) append(" Đề: ").append(c.de.trim())
                appendLine()
            }
            appendLine(
                "Mỗi câu trên có đúng một mục trong kết quả, giữ nguyên mã câu. Câu con " +
                    "chưa làm thì \"dung\" là false, \"con_viet\" để trống, \"so_dong\" là 0."
            )
            appendLine(
                "Ảnh có câu con làm mà không có trong danh sách thì thêm một mục cho câu " +
                    "đó: mã theo cách sách đánh số, chép đề vào \"de\", và ghi \"dang\"."
            )
        } else {
            appendLine("Con không khai trước là làm câu nào. Nhờ bạn tự nhận ra các câu trong ảnh.")
            appendLine(
                "Mỗi câu chép đề vào \"de\" và ghi \"dang\". Câu nào ảnh không có đề thì " +
                    "để \"de\" trống và \"dung\" là false, vì không có đề thì không biết đúng sai."
            )
        }
        appendLine(DANG_BAI)
        appendLine()

        if (soat != null) {
            voDaSoat(soat, coAnhVo)
        } else if (coAnhVo) {
            // Trang vo cua Le Hoa chep lien tay, mot trang hai ba buoi. Khong dan thi
            // Claude de gop bai cua ca trang vao mot ngay.
            appendLine(
                "Vở dặn dò là trang cô giáo ghi ngày và dặn bài về nhà. Trang thường chép " +
                    "liền nhiều buổi, mỗi buổi mở đầu bằng một dòng ghi ngày. Nhờ bạn chỉ đọc " +
                    "buổi có ngày gần ngày nộp bài nhất mà không sau ngày nộp, rồi ghi vào khối JSON:"
            )
            appendLine(
                "- \"ngay_dan_do\": ngày ghi trong vở, dạng yyyy-MM-dd. Vở chỉ ghi ngày và " +
                    "tháng thì lấy năm sao cho gần ngày nộp bài nhất. Không thấy ngày thì để null."
            )
            appendLine(
                "- \"bai_duoc_giao\": tên từng bài tập phải làm mà vở giao, ví dụ [\"bài 2\", " +
                    "\"bài 3\", \"SBT 2.26\"]. Dặn việc như mang sách vở, tiết sau kiểm tra, " +
                    "học thuộc thì không phải bài tập. Không có bài tập nào thì để []."
            )
            appendLine(
                "- \"lam_het_dan_do\": true chỉ khi \"bai_duoc_giao\" có ít nhất một bài và " +
                    "ảnh cho thấy con đã làm hết các bài đó."
            )
            appendLine(
                "- Mỗi câu trong \"ket_qua\" ghi thêm \"trong_dan_do\": true nếu câu đó " +
                    "thuộc một bài trong \"bai_duoc_giao\", false nếu là bài con làm thêm."
            )
            appendLine()
        }

        appendLine("Trả lời bằng tiếng Việt, gồm:")
        appendLine("1. Một bảng: mã câu, con viết, đáp án đúng, con đúng hay sai.")
        appendLine(
            "2. Với mỗi câu con làm sai: một câu gợi ý để con tự sửa, gọi con là \"con\", " +
                "không đưa đáp án."
        )
        appendLine("3. Số câu con làm đúng.")
        if (soat != null) {
            appendLine(
                "4. Vở dặn dò: con đã làm hết các bài cô giao ở trên chưa" +
                    if (coAnhVo) ", và danh sách con soát có khớp với trang vở không." else "."
            )
        } else if (coAnhVo) {
            appendLine("4. Vở dặn dò: ngày ghi trong vở, các bài cô giao, và con đã làm hết chưa.")
        }
        append(
            "${if (coVo) 5 else 4}. Cuối cùng, in đúng một khối JSON theo mẫu dưới đây để tôi dán vào app. " +
                "Mỗi câu một mục trong \"ket_qua\". \"dung\" là kết luận của bạn. \"chac\" " +
                "là false nếu bạn không đọc chắc chữ con viết. \"con_viet\" là kết quả cuối " +
                "con viết, theo bạn đọc. \"so_dong\" là số dòng đếm ở bước 6. \"goi_y\" chỉ " +
                "viết cho câu con làm sai: đúng câu gợi ý ở mục 2, tối đa 15 chữ."
        )
        if (onTap) {
            append(
                " \"muc_do\" là true nếu bài làm của câu đó viết bằng mực đỏ, false nếu viết " +
                    "mực khác hoặc không rõ màu."
            )
        }
        appendLine()
        val maMau = khai?.cac?.firstOrNull()?.ma ?: "1"
        // Mau CO Y khong phai JSON hop le, cung ly do voi mau o [loiNhoChamLai].
        append("{\"bai\":\"${bai.id}\",")
        if (soat != null) {
            append("\"lam_het_dan_do\":true hoặc false,")
            if (coAnhVo) append("\"vo_lech\":\"...\",")
        } else if (coAnhVo) {
            append("\"ngay_dan_do\":\"yyyy-MM-dd hoặc null\",\"bai_duoc_giao\":[\"...\"],")
            append("\"lam_het_dan_do\":true hoặc false,")
        }
        append("\"ket_qua\":[{\"ma\":\"$maMau\",\"dung\":true hoặc false,")
        append("\"chac\":true hoặc false,\"con_viet\":\"...\",\"so_dong\":số dòng,\"goi_y\":\"...\"")
        if (onTap) append(",\"muc_do\":true hoặc false")
        if (coVo) append(",\"trong_dan_do\":true hoặc false")
        if (khai == null) append(",\"de\":\"chép đề câu đó\",\"dang\":\"CAU_NHO\"")
        append("}]}")
    }

    /** Loi nho khi may da cham, Claude cham lai de bat cho may nham. */
    private fun loiNhoChamLai(bai: Bai, ten: String): String = buildString {
        val cham = bai.cham

        appendLine("$DAU_LOI_NHO của $ten. Tôi là bố của con.")
        appendLine(
            "Máy chấm tự động trên tablet đã chấm bài này, nhưng máy hay đọc nhầm chữ " +
                "và giải nhầm, nên tôi cần bạn chấm lại độc lập."
        )
        appendLine()

        dongNop(bai, cham?.mon)
        conKhai(cham)?.let { appendLine("Con khai đang làm: $it.") }
        appendLine("Ảnh đính kèm là ảnh vở bài làm của con.")
        appendLine()

        cachCham(chamMoi = false)
        appendLine()

        if (cham == null || cham.cac.isEmpty()) {
            appendLine("Máy chấm chưa chấm bài này. Nhờ bạn tự nhận ra các câu trong ảnh rồi chấm.")
        } else {
            appendLine("Các câu cần chấm, kèm đề và kết luận của máy chấm:")
            cham.cac.forEachIndexed { i, c ->
                append(i + 1).append(". Câu ").append(c.ma.ifBlank { "chưa rõ mã" }).append(".")
                if (c.de.isNotBlank()) append(" Đề: ").append(c.de.trim())
                append(" Máy đọc con viết: ").append(c.ketQua.ifBlank { "không đọc ra" }).append(".")
                append(" Máy chấm: ")
                append(
                    when {
                        !c.docRo -> "đọc không rõ"
                        c.dung -> "đúng"
                        else -> "sai"
                    }
                )
                append(".")
                if (c.nhanXet.isNotBlank()) append(" Nhận xét của máy: ").append(c.nhanXet.trim())
                appendLine()
            }
        }
        appendLine()

        appendLine("Trả lời bằng tiếng Việt, gồm:")
        appendLine("1. Một bảng: mã câu, con viết, đáp án đúng, con đúng hay sai, máy chấm đúng hay nhầm.")
        appendLine(
            "2. Các câu máy đọc nhầm chữ, các câu máy chấm nhầm đúng sai, và các câu máy " +
                "chấm đúng nhưng nhận xét chỉ sai chỗ."
        )
        appendLine(
            "3. Với mỗi câu con làm sai: một câu gợi ý để con tự sửa, gọi con là \"con\", " +
                "không đưa đáp án."
        )
        appendLine("4. Số câu con làm đúng thật.")
        appendLine(
            "5. Cuối cùng, in đúng một khối JSON theo mẫu dưới đây để tôi dán vào app. " +
                "Mỗi câu một mục trong \"ket_qua\", giữ nguyên mã câu như trên. \"dung\" là " +
                "kết luận của bạn. \"chac\" là false nếu bạn không đọc chắc chữ con viết. " +
                "\"con_viet\" là kết quả cuối con viết, theo bạn đọc. \"goi_y\" chỉ viết " +
                "cho câu con làm sai: đúng câu gợi ý ở mục 3, không đưa đáp án."
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
     * Doan vo dan do khi con da soat tu dau buoi.
     *
     * Chep y cau lenh cua may cham luc co ban soat (PromptCham.doanDanDo ben tablet): dung
     * dung ngay va danh sach nay, khong suy tu anh. Nho vay ca hai duong cham tinh tron
     * goi theo cung mot danh sach, danh sach con da soat bang mat va Ba Huy da thay tren
     * Telegram tu dau buoi.
     *
     * Anh trang vo gui kem chi de doi chieu. Cho nao lech thi Claude ghi ra cho Ba Huy
     * doc trong hop thoai dan ket qua, khong doi phep tinh. Vang anh (tablet chua gui duoc
     * tin vo dan do) thi khong hoi cho lech.
     */
    private fun StringBuilder.voDaSoat(v: VoDaSoat, coAnh: Boolean) {
        appendLine("Vở dặn dò hôm đó con đã chụp từ đầu buổi, máy đọc ra chữ và con đã soát lại:")
        appendLine("- Ngày ghi trên vở: ${v.ngay}.")
        appendLine(
            "- Bài cô giao: " +
                if (v.cacBai.isEmpty()) "hôm đó cô KHÔNG giao bài tập nào." else v.cacBai.joinToString("; ") + "."
        )
        if (v.dongKhac.isNotEmpty()) appendLine("- Dặn dò khác: ${v.dongKhac.joinToString("; ")}.")
        appendLine("Dùng đúng danh sách này, không tự đọc lại danh sách từ ảnh. Ghi vào khối JSON:")
        appendLine(
            "- \"lam_het_dan_do\": true chỉ khi danh sách bài cô giao ở trên có ít nhất một bài " +
                "và ảnh bài làm cho thấy con đã làm hết các bài đó."
        )
        appendLine(
            "- Mỗi câu trong \"ket_qua\" ghi thêm \"trong_dan_do\": true nếu câu đó thuộc một bài " +
                "trong danh sách trên, false nếu là bài con làm thêm. Danh sách rỗng thì mọi câu đều false."
        )
        if (coAnh) {
            appendLine(
                "- \"vo_lech\": ảnh đầu tiên là trang vở đó, gửi kèm để bạn đối chiếu. Trang có thể " +
                    "chép nhiều buổi, chỉ xem buổi ghi ngày trên. Nếu danh sách trên thiếu một bài " +
                    "tập, có dòng không phải bài tập, hay ngày không khớp với trang vở, ghi ngắn chỗ " +
                    "lệch vào đây. Khớp thì để chuỗi rỗng. Mục này để tôi đọc, không đổi cách tính giờ."
            )
        }
        appendLine()
    }

    /**
     * Cach cham tung cau. Hai kieu nho dung chung, chi khac buoc 3 va buoc 6.
     *
     * Buoc 4 lay dung luat cua may cham: dung la moi buoc deu dung. Claude chi xet ket
     * qua cuoi thi mot bai sai o giua ma ra dung dap an se thanh dung, va con duoc gio
     * cho mot loi giai sai.
     */
    private fun StringBuilder.cachCham(chamMoi: Boolean) {
        appendLine("Cách chấm từng câu:")
        appendLine(
            "1. Nhìn kỹ riêng câu đó trong ảnh. Nếu chạy được code thì cắt vùng ảnh của " +
                "câu rồi phóng to. Chép đúng chữ con viết ở kết quả cuối, kể cả khi sai."
        )
        appendLine(
            "2. Cẩn thận với nhãn câu viết sát con số. Chữ b viết liền trước một con số " +
                "rất dễ bị đọc thành số 1."
        )
        appendLine(
            if (chamMoi) {
                "3. Tự giải câu đó từng bước trước, rồi mới so với bài của con. Nếu chạy " +
                    "được code thì kiểm các phép biến đổi bằng sympy."
            } else {
                "3. Tự giải lại câu đó từng bước. Chỉ so với kết luận của máy sau khi đã tự " +
                    "chấm xong. Nếu chạy được code thì kiểm các phép biến đổi bằng sympy."
            }
        )
        appendLine(
            "4. Con đúng khi mọi bước đều đúng và kết quả cuối đúng hoàn toàn. Sai một dấu " +
                "hay một bước là sai. Câu phân tích thành nhân tử phải phân tích hết mới " +
                "tính là đúng."
        )
        appendLine("5. Chỗ nào mờ, không đọc chắc được thì ghi rõ là không chắc, không đoán.")
        if (chamMoi) {
            appendLine(
                "6. Đếm số dòng con tự viết để làm câu đó. Không tính dòng chép lại đề, " +
                    "dòng trống, dòng đã gạch xoá, dòng lặp lại vô nghĩa."
            )
        }
    }

    /**
     * Cach xep dang bai, chep tu cau lenh cua may cham ben tablet.
     *
     * Dang bai quyet gia moi cau: trac nghiem tinh theo cum, hoc thuoc khong tinh. Cau
     * trong sach da co dang trong ngan hang, nen Claude chi can xep cau ngoai sach.
     */
    private const val DANG_BAI = "\"dang\" là một trong: TRAC_NGHIEM (chỉ khoanh, ghi Đúng/Sai, " +
        "nối cột, điền một từ), CAU_NHO (câu nhỏ có trình bày lời giải), BAI_RIENG (bài " +
        "đứng riêng), VIET_DAI (đoạn văn, bài văn), KHONG_TINH (học thuộc, luyện chữ, chép bài)."

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
        val cac: List<CauClaude>,
        /** Claude doc vo dan do ra gi. null la khoi JSON khong co truong nao ve vo dan do. */
        val danDo: DanDoDan? = null,
        /**
         * Cho Claude thay danh sach con soat lech voi anh trang vo. Rong la khop, hay bai
         * khong dung ban soat. Chi de Ba Huy doc, khong di sang tablet.
         */
        val voLech: String = ""
    )

    /**
     * Vo dan do Claude doc ra: dung ba truong ma may cham van tra ve.
     *
     * [ngay] giu nguyen chu Claude viet. Tablet tu doc ngay, nhan ca kieu "14/9/2026"
     * hay "Thứ hai, ngày 14 tháng 9", xem LuatCongGio.docNgay ben tablet.
     */
    data class DanDoDan(val ngay: String?, val baiDuocGiao: List<String>, val lamHet: Boolean)

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
    fun docKetQua(chu: String?): KetQuaDan? {
        if (chu.isNullOrBlank()) return null
        val moc = chu.lastIndexOf("\"ket_qua\"")
        if (moc < 0) return null
        var dau = chu.lastIndexOf('{', moc)
        while (dau >= 0) {
            val o = khoiTu(chu, dau)
            if (o?.optJSONArray("ket_qua") != null) return docKhoi(o)
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
                // Khong noi mau muc thi la -1, khong phai "khong do": tablet noi rieng hai
                // canh do voi Ba Huy. Xem CauCham.mucDo ben tablet.
                mucDo = when (val m = c.opt("muc_do")) {
                    is Boolean -> if (m) 1 else 0
                    is Number -> if (m.toInt() == 1) 1 else 0
                    else -> -1
                },
                de = c.chuoi("de"),
                dang = c.chuoi("dang").uppercase(Locale.ROOT),
                // Chi nhan true hay false that. Khong noi thi tablet tu quyet theo luat
                // cua may cham: co trang vo thi la lam them, khong co thi la bai co giao.
                trongDanDo = c.opt("trong_dan_do") as? Boolean
            )
        }
        if (cac.isEmpty()) return null
        val coDanDo = o.has("ngay_dan_do") || o.has("bai_duoc_giao") || o.has("lam_het_dan_do")
        val danDo = if (!coDanDo) null else DanDoDan(
            ngay = (o.opt("ngay_dan_do") as? String)?.trim()?.takeIf { it.isNotEmpty() && it != "null" },
            baiDuocGiao = o.optJSONArray("bai_duoc_giao")?.let { a ->
                (0 until a.length()).mapNotNull { (a.opt(it) as? String)?.trim()?.takeIf { t -> t.isNotEmpty() } }
            }.orEmpty(),
            // Viet sai kieu la khong co goi, y nhu "dung": mot chu "true" khong duoc thanh
            // 45 phut.
            lamHet = o.opt("lam_het_dan_do") as? Boolean ?: false
        )
        return KetQuaDan(o.chuoi("bai"), cac, danDo, voLech = o.chuoi("vo_lech"))
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
        return ket.copy(cac = ket.cac.map { c ->
            val k = khai.cac.firstOrNull { chuanMa(it.ma) == chuanMa(c.ma) && it.ma !in daDung }
                ?: return@map c
            daDung += k.ma
            c.copy(ma = k.ma, de = c.de.ifBlank { k.de }, dang = c.dang.ifBlank { k.dang })
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

    /** Dong "Con khai: ..." trong tom tat cua tablet, bo dau cham dau dong. */
    private fun conKhai(cham: KetQuaCham?): String? =
        cham?.tomTat?.lineSequence()
            ?.map { it.trim().removePrefix("•").trim() }
            ?.firstOrNull { it.startsWith("Con khai:") }
            ?.removePrefix("Con khai:")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
}
