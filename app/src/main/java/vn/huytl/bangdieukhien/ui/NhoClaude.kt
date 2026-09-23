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
 *
 * DUONG VE. Claude app khong ghi duoc len Firebase. Nen loi nho dan Claude in them
 * mot khoi JSON o cuoi cau tra loi, kem ma bai. Ba Huy chep cau tra loi, quay lai man
 * chi tiet bai bam "Dán kết quả của Claude", va [docKetQua] doc khoi do ra. Ma bai
 * trong khoi chan viec dan nham ket qua sang bai khac.
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

        appendLine("$DAU_LOI_NHO của $ten. Tôi là bố của con.")
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

    /** Dong mo dau cua moi loi nho, de nhan ra bo nho tam dang giu loi nho chu khong phai tra loi. */
    private const val DAU_LOI_NHO = "Nhờ bạn chấm lại bài tập về nhà"

    /**
     * Bo nho tam dang giu chinh loi nho gui Claude, chua phai cau tra loi.
     *
     * Canh de xay ra nhat: bam "Nhờ Claude chấm lại", gui trong Claude, doc xong quay
     * lai bam dan ma quen chep cau tra loi.
     */
    fun laLoiNho(chu: String?): Boolean = chu?.contains(DAU_LOI_NHO) == true

    /** Ket qua Claude cham lai, doc tu cau tra loi Ba Huy chep tu app Claude. */
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
     * "ket_qua" cuoi cung, lui ve dau ngoac nhon mo gan nhat, roi dem ngoac de lay
     * dung mot khoi. Dem ngoac thi bo qua ngoac nam trong chuoi, vi goi y cua Claude
     * co the chep lai mot bieu thuc co ngoac.
     *
     * Tra null khi khong thay khoi nao doc duoc: bo nho tam dang giu thu khac, hay
     * Claude quen in khoi JSON.
     */
    fun docKetQua(chu: String?): KetQuaDan? {
        if (chu.isNullOrBlank()) return null
        val moc = chu.lastIndexOf("\"ket_qua\"")
        if (moc < 0) return null
        val dau = chu.lastIndexOf('{', moc)
        if (dau < 0) return null

        var sau = 0
        var trongChuoi = false
        var thoat = false
        var cuoi = -1
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
                        cuoi = i
                        break
                    }
                }
            }
        }
        if (cuoi < 0) return null

        val o = runCatching { JSONObject(chu.substring(dau, cuoi + 1)) }.getOrNull()
            ?: return null
        val mang = o.optJSONArray("ket_qua") ?: return null
        val cac = (0 until mang.length()).mapNotNull { i ->
            val c = mang.optJSONObject(i) ?: return@mapNotNull null
            val ma = c.optString("ma").trim()
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
                conViet = c.optString("con_viet").trim(),
                goiY = c.optString("goi_y").trim()
            )
        }
        if (cac.isEmpty()) return null
        return KetQuaDan(o.optString("bai").trim(), cac)
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
