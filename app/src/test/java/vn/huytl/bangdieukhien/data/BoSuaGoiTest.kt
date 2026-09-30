package vn.huytl.bangdieukhien.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** Lenh BO_SUA (30/9/2026): moi cau chua dung, gui ma va de nhu tablet ghi. */
class BoSuaGoiTest {

    private val cham = KetQuaCham(
        cac = listOf(
            CauCham(ma = "2.33a", de = "Rút gọn (2x + 5y)^2", dung = false),
            CauCham(ma = "2.34", de = "Tính nhanh 101 · 99", dung = false, docRo = false),
            CauCham(ma = "2.35", de = "Tính 3x · 2x", dung = true),
            CauCham(ma = "2.36 ", de = "Phân tích x^2 − 4", dung = false),
            CauCham(ma = "2.37", de = "Tính 5x · 2", dung = true, docRo = false)
        )
    )

    /**
     * Tablet ghi so ca cau doc chua ro ma Claude cham chua dung, nen dong "Có N câu cần sửa" dem
     * ca cau do. Bai KHTN 28/9/2026: bo 7 cau sai, 5 cau chua doc ro van o lai man chinh.
     */
    @Test
    fun lay_moi_cau_chua_dung_ke_ca_doc_chua_ro_va_nho_chup_lai() {
        assertEquals(listOf("2.33a", "2.34", "2.36 "), BoSuaGoi.cauSai(cham).map { it.ma })
        assertEquals(emptyList<CauCham>(), BoSuaGoi.cauSai(null))
    }

    @Test
    fun hop_chon_ghi_chu_cau_doc_chua_ro_va_cau_nho_chup_lai() {
        val sai = BoSuaGoi.cauSai(cham)
        assertEquals(
            listOf("2.33a", "2.34 (đọc chưa rõ)", "2.36 (nhờ chụp lại)"),
            sai.map { BoSuaGoi.nhan(it, chupLai = setOf("2.36")) }
        )
        assertEquals("câu", BoSuaGoi.nhan(CauCham(ma = " ", dung = false)))
    }

    @Test
    fun gia_tri_mang_ma_va_de() {
        assertEquals(
            listOf(mapOf("ma" to "2.33a", "de" to "Rút gọn (2x + 5y)^2")),
            BoSuaGoi.giaTri(BoSuaGoi.cauSai(cham).take(1))
        )
    }
}
