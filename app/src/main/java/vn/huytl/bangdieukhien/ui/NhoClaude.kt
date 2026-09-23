package vn.huytl.bangdieukhien.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import vn.huytl.bangdieukhien.data.Anh
import vn.huytl.bangdieukhien.data.Bai
import vn.huytl.bangdieukhien.data.KetQuaCham
import vn.huytl.bangdieukhien.data.Nha
import vn.huytl.bangdieukhien.telegram.TaiAnh
import java.io.File
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
 */
object NhoClaude {

    /** Ten goi cua app Claude tren Google Play. */
    private const val GOI_CLAUDE = "com.anthropic.claude"

    private val VN = Locale.forLanguageTag("vi-VN")

    /**
     * Cac tam can gui: moi tam tru vo dan do.
     *
     * Vo dan do chi noi hom nay co bai gi, khong co gi de cham.
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
     * Loi nho Claude cham, viet nhu Ba Huy tu go.
     *
     * Cach cham dat TRUOC ket luan cua may. Dat sau thi Claude doc ket luan truoc
     * roi moi nhin anh, va de nghieng theo chu may da doc - dung cai loi can bat.
     */
    fun loiNho(bai: Bai, tenCon: String): String = buildString {
        val ten = tenCon.trim().takeIf { it.isNotEmpty() && it != "con" } ?: "con tôi"
        val cham = bai.cham

        appendLine("Nhờ bạn chấm lại bài tập về nhà của $ten. Tôi là bố của con.")
        appendLine(
            "Máy chấm tự động trên tablet đã chấm bài này, nhưng máy hay đọc nhầm chữ " +
                "và giải nhầm, nên tôi cần bạn chấm lại độc lập."
        )
        appendLine()

        append("Bài nộp lúc ")
        append(SimpleDateFormat("HH:mm 'ngày' dd/MM/yyyy", VN).format(Date(bai.luc)))
        cham?.mon?.takeIf { it.isNotBlank() }?.let { append(", môn ").append(it) }
        appendLine(".")
        conKhai(cham)?.let { appendLine("Con khai đang làm: $it.") }
        appendLine("Ảnh đính kèm là ảnh vở bài làm của con.")
        appendLine()

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
            "3. Tự giải lại câu đó từng bước. Chỉ so với kết luận của máy sau khi đã tự " +
                "chấm xong. Nếu chạy được code thì kiểm các phép biến đổi bằng sympy."
        )
        appendLine(
            "4. So kết quả cuối của con với lời giải. Câu phân tích thành nhân tử phải " +
                "phân tích hết mới tính là đúng."
        )
        appendLine("5. Chỗ nào mờ, không đọc chắc được thì ghi rõ là không chắc, không đoán.")
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
        append("4. Cuối cùng: số câu con làm đúng thật.")
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
