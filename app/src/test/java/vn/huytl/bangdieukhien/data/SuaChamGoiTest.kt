package vn.huytl.bangdieukhien.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lenh SUA_CHAM gui kem so dong Claude ghi o lan cham lai (29/9/2026). Thieu so dong thi
 * tablet dem ra 0 dong, va truoc ngay do cau sua bi ghi la xong voi 0 phut.
 */
class SuaChamGoiTest {

    /** Lan cham truoc: 2.33a sai, 2.34 doc chua ro, 2.35 dung. */
    private val truoc = listOf(
        CauCham(ma = "2.33a", de = "Rút gọn (2x + 5y)^2 − (2x − 5y)^2", dung = false),
        CauCham(ma = "2.34 ", de = "Tính nhanh 101 · 99", dung = true, docRo = false),
        CauCham(ma = "2.35", de = "Tính 3x · 2x", dung = true)
    )

    private fun claude(ma: String, dung: Boolean, chac: Boolean = true, soDong: Int = 0) =
        CauClaude(ma = ma, dung = dung, chac = chac, conViet = "", goiY = "", soDong = soDong)

    @Test
    fun chi_lay_cau_lan_truoc_sai_hay_doc_chua_ro_kem_so_dong_claude_ghi() {
        val cac = SuaChamGoi.thanhDung(
            truoc,
            listOf(
                claude("2.33a", dung = true, soDong = 3),
                claude("2.34", dung = true, soDong = 2),
                // Lan truoc da dung: gio da tra, khong sua.
                claude("2.35", dung = true, soDong = 1),
                // Claude chua chac thi giu theo lan cham truoc.
                claude("2.36", dung = true, chac = false, soDong = 4)
            )
        )

        assertEquals(listOf("2.33a", "2.34 "), cac.map { it.cu.ma })
        assertEquals(listOf(3, 2), cac.map { it.soDong })
    }

    @Test
    fun gia_tri_mang_ma_de_lan_truoc_va_so_dong() {
        val cac = SuaChamGoi.thanhDung(truoc, listOf(claude("2.33a", dung = true, soDong = 3)))

        assertEquals(
            listOf(mapOf("ma" to "2.33a", "de" to "Rút gọn (2x + 5y)^2 − (2x − 5y)^2", "soDong" to 3)),
            SuaChamGoi.giaTri(cac)
        )
    }

    @Test
    fun claude_khong_ghi_so_dong_thi_gui_0_cho_tablet_giu_cau_cho_sua() {
        val cac = SuaChamGoi.thanhDung(truoc, listOf(claude("2.33a", dung = true)))

        assertEquals(0, SuaChamGoi.giaTri(cac).single()["soDong"])
        // So am (khoi JSON hong) cung ve 0, khong gui so am sang tablet.
        assertEquals(0, SuaChamGoi.giaTri(listOf(SuaChamGoi.Cau(truoc[0], -2))).single()["soDong"])
        assertTrue(SuaChamGoi.thanhDung(truoc, listOf(claude("2.33a", dung = false))).isEmpty())
    }
}
