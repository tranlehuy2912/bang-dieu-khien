package vn.huytl.bangdieukhien.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import vn.huytl.bangdieukhien.data.Bai
import vn.huytl.bangdieukhien.data.CauCham
import vn.huytl.bangdieukhien.data.CauClaude
import vn.huytl.bangdieukhien.data.KetQuaCham
import vn.huytl.bangdieukhien.data.KetQuaClaude
import vn.huytl.bangdieukhien.data.KhaiBai
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Chu nut "Chép bản chấm" dua vao bo nho tam: phai ke dung nhu the tren man bai. */
class ChuBanChamTest {

    private val luc = 1_790_133_813_356L

    private fun ngay(ms: Long) =
        SimpleDateFormat("HH:mm 'ngày' d/M/yyyy", Locale.forLanguageTag("vi-VN")).format(Date(ms))

    @Test
    fun ban_may_cham_ke_tung_cau_kem_cho_claude_noi_khac() {
        val bai = Bai(
            id = "b1308359", luc = luc, trangThai = Bai.DUYET, soPhut = 30, anh = emptyList(),
            cham = KetQuaCham(
                mon = "Toán",
                cac = listOf(
                    CauCham(
                        ma = "2.32b", de = "Tính 10^9", ketQua = "1000000000",
                        dung = false, nhanXet = "Thiếu chữ số 0."
                    ),
                    CauCham(ma = "2.33a", de = "Rút gọn", ketQua = "8x^2 + 20xy", dung = true)
                ),
                tomTat = "• Lê Hòa khai: 2.32b, 2.33a",
                phutDeNghi = 30
            ),
            messageId = 0L,
            claude = KetQuaClaude(
                luc = luc + 600_000L,
                cac = listOf(
                    CauClaude(ma = "2.32b", dung = true, chac = true, conViet = "1000000000", goiY = ""),
                    CauClaude(ma = "2.33a", dung = true, chac = true, conViet = "8x^2 + 20xy", goiY = "")
                )
            )
        )
        assertEquals(
            """
            Bài nộp lúc ${ngay(luc)}, mã bài b1308359
            Toán
            • Lê Hòa khai: 2.32b, 2.33a
            Claude chấm lại lúc ${Dinh.lucNgan(luc + 600_000L)}: đúng 2/2 câu

            ✕ Câu 2.32b
            Đề: Tính 10^9
            Lê Hòa viết: 1000000000
            Nhận xét: Thiếu chữ số 0.
            Claude: đúng, máy chấm nhầm

            ✓ Câu 2.33a
            Đề: Rút gọn
            Lê Hòa viết: 8x^2 + 20xy

            AI đề nghị 30 phút
            """.trimIndent(),
            ChuBanCham.cua(bai, "Lê Hòa")
        )
    }

    @Test
    fun chi_co_ban_claude_thi_lay_de_tu_phan_khai() {
        val bai = Bai(
            id = "b77", luc = luc, trangThai = Bai.DUYET, soPhut = 0, anh = emptyList(),
            cham = null, messageId = 0L,
            claude = KetQuaClaude(
                luc = luc, chinh = true,
                cac = listOf(
                    CauClaude(ma = "2.28", dung = false, chac = true, conViet = "x = 2", goiY = "Thay x = 2 vào đề thử."),
                    CauClaude(ma = "2.29", dung = true, chac = false, conViet = "", goiY = "")
                )
            ),
            khai = KhaiBai(
                tenNguon = "SBT Toán 8", bai = "Bài 7", mon = "Toán", onTap = false,
                cac = listOf(KhaiBai.Cau(ma = "2.28", cauId = "t8.2.28", de = "Giải phương trình", dang = ""))
            )
        )
        assertEquals(
            """
            Bài nộp lúc ${ngay(luc)}, mã bài b77
            Toán
            Claude chấm lúc ${Dinh.lucNgan(luc)}: đúng 0/2 câu

            ✕ Câu 2.28
            Đề: Giải phương trình
            Lê Hòa viết: x = 2
            Gợi ý: Thay x = 2 vào đề thử.

            ? Câu 2.29
            Claude đọc chưa chắc câu này
            """.trimIndent(),
            ChuBanCham.cua(bai, "Lê Hòa")
        )
    }

    @Test
    fun chua_ai_cham_thi_khong_co_gi_de_chep() {
        val bai = Bai(
            id = "b1", luc = luc, trangThai = Bai.CHO, soPhut = 0, anh = emptyList(),
            cham = null, messageId = 0L
        )
        assertEquals("", ChuBanCham.cua(bai, "Lê Hòa"))
    }
}
