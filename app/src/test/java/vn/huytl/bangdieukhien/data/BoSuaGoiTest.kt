package vn.huytl.bangdieukhien.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** Lenh BO_SUA (30/9/2026): chi cau sai that, gui ma va de nhu tablet ghi. */
class BoSuaGoiTest {

    private val cham = KetQuaCham(
        cac = listOf(
            CauCham(ma = "2.33a", de = "Rút gọn (2x + 5y)^2", dung = false),
            CauCham(ma = "2.34", de = "Tính nhanh 101 · 99", dung = false, docRo = false),
            CauCham(ma = "2.35", de = "Tính 3x · 2x", dung = true),
            CauCham(ma = "2.36 ", de = "Phân tích x^2 − 4", dung = false)
        )
    )

    @Test
    fun chi_lay_cau_sai_doc_ro_va_bo_cau_nho_chup_lai() {
        assertEquals(listOf("2.33a", "2.36 "), BoSuaGoi.cauSai(cham).map { it.ma })
        assertEquals(listOf("2.33a"), BoSuaGoi.cauSai(cham, chupLai = setOf("2.36")).map { it.ma })
        assertEquals(emptyList<CauCham>(), BoSuaGoi.cauSai(null))
    }

    @Test
    fun gia_tri_mang_ma_va_de() {
        assertEquals(
            listOf(mapOf("ma" to "2.33a", "de" to "Rút gọn (2x + 5y)^2")),
            BoSuaGoi.giaTri(BoSuaGoi.cauSai(cham).take(1))
        )
    }
}
