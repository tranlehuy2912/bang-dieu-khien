package vn.huytl.bangdieukhien.ui

import vn.huytl.bangdieukhien.data.Bai
import vn.huytl.bangdieukhien.data.KetQuaClaude
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ban cham cua mot lan nop, viet thanh chu de chep ra ngoai.
 *
 * Ba Huy chep de dan cho Claude Code khi may cham nham, hay gui qua Zalo. Nen chu o day
 * di dung thu tu va dung cau cua the ban cham tren [BaiActivity], them hai thu man hinh
 * khong can ma nguoi doc o cho khac thi can: ngay nop day du, va ma bai de tim lai dung
 * document tren Firestore.
 *
 * Doi cach ve the ban cham ben [BaiActivity] thi doi ca o day.
 */
object ChuBanCham {

    private val VN = Locale.forLanguageTag("vi-VN")

    /** Rong khi bai chua co ban cham nao: luc do man hinh cung khong co the nao de chep. */
    fun cua(bai: Bai, tenCon: String): String {
        val cham = bai.cham
        if (cham == null || cham.cac.isEmpty()) {
            bai.claude?.let { return theoClaude(bai, it, tenCon) }
            if (cham == null) return ""
        }
        return buildString {
            append(dau(bai)).append('\n')
            append(cham.mon.ifBlank { "Bài đã nộp" })
            if (cham.tomTat.isNotBlank()) append('\n').append(cham.tomTat.trim())
            bai.claude?.let { cl ->
                append('\n')
                append(if (cl.chinh) "Claude chấm lúc " else "Claude chấm lại lúc ")
                append("${Dinh.lucNgan(cl.luc)}: đúng ${cl.cac.count { it.chac && it.dung }}/${cl.cac.size} câu")
            }
            cham.cac.forEach { c ->
                append("\n\n")
                append(
                    when {
                        !c.docRo -> "?"
                        c.dung -> "✓"
                        else -> "✕"
                    }
                ).append(' ').append(tenCau(c.ma))
                if (c.de.isNotBlank()) append("\nĐề: ").append(c.de.trim())
                if (c.ketQua.isNotBlank()) append("\n$tenCon viết: ").append(c.ketQua.trim())
                if (!c.docRo) append("\nAI đọc không rõ câu này")
                if (c.nhanXet.isNotBlank()) append("\nNhận xét: ").append(c.nhanXet.trim())
                // Cung dieu kien voi dong Claude tren the: chi ghi khi Claude chac va noi khac may.
                val cl = bai.claude?.cua(c.ma)
                if (cl != null && cl.chac && cl.dung != (c.docRo && c.dung)) {
                    append('\n')
                    append(
                        if (cl.dung) "Claude: đúng, máy chấm nhầm"
                        else "Claude: sai" + if (cl.goiY.isNotBlank()) ". ${cl.goiY.trim()}" else ""
                    )
                }
            }
            if (cham.phutDeNghi > 0) append("\n\nAI đề nghị ${Dinh.phut(cham.phutDeNghi)}")
        }
    }

    /** The chi co ban cua Claude: may chua cham, xem [BaiActivity] cho the nay. */
    private fun theoClaude(bai: Bai, cl: KetQuaClaude, tenCon: String): String = buildString {
        append(dau(bai)).append('\n')
        append(bai.khai?.mon?.takeIf { it.isNotBlank() } ?: "Bài đã nộp")
        append("\nClaude chấm lúc ${Dinh.lucNgan(cl.luc)}: đúng ${cl.cac.count { it.chac && it.dung }}/${cl.cac.size} câu")
        if (bai.dangCho) append("\nĐang chờ tablet tính phút.")
        val deTheoMa = bai.khai?.cac.orEmpty().associate { it.ma to it.de }
        cl.cac.forEach { c ->
            append("\n\n")
            append(
                when {
                    !c.chac -> "?"
                    c.dung -> "✓"
                    else -> "✕"
                }
            ).append(' ').append(tenCau(c.ma))
            val de = c.de.ifBlank { deTheoMa[c.ma].orEmpty() }
            if (de.isNotBlank()) append("\nĐề: ").append(de.trim())
            if (c.conViet.isNotBlank()) append("\n$tenCon viết: ").append(c.conViet.trim())
            if (!c.chac) append("\nClaude đọc chưa chắc câu này")
            if (!c.dung && c.goiY.isNotBlank()) append("\nGợi ý: ").append(c.goiY.trim())
        }
    }

    /** "Bài nộp lúc 10:23 ngày 23/9/2026, mã bài b1308359". */
    private fun dau(bai: Bai): String {
        val luc = if (bai.luc > 0L) {
            "lúc " + SimpleDateFormat("HH:mm 'ngày' d/M/yyyy", VN).format(Date(bai.luc))
        } else {
            "không rõ giờ"
        }
        return "Bài nộp $luc, mã bài ${bai.id}"
    }

    private fun tenCau(ma: String): String = "Câu " + ma.trim().ifBlank { "chưa rõ số" }
}
