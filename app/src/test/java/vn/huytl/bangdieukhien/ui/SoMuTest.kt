package vn.huytl.bangdieukhien.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Doi so mu luc hien de tren man bai. Xem [SoMu]. Cung bo test voi SoMuTest ben nop-bai, vi
 * hai ban SoMu phai giong nhau.
 */
class SoMuTest {

    @Test
    fun so_mu_la_chu_so() {
        assertEquals("x²y³ − 6x² + 9", SoMu.hien("x^2y^3 − 6x^2 + 9"))
        assertEquals("1,2044·10²² phân tử", SoMu.hien("1,2044·10^22 phân tử"))
        assertEquals("(x + 1)³/(x² − 1)", SoMu.hien("(x + 1)^3/(x^2 − 1)"))
        assertEquals("Rút gọn (2x + 5y)² − (2x − 5y)²", SoMu.hien("Rút gọn (2x + 5y)^2 − (2x − 5y)^2"))
    }

    @Test
    fun dau_cua_ion() {
        assertEquals("ion Ca²⁺ và Mg²⁺.", SoMu.hien("ion Ca^2+ và Mg^2+."))
        assertEquals("H⁺, OH⁻, SO4²⁻", SoMu.hien("H^+, OH^−, SO4^2−"))
    }

    @Test
    fun dau_tru_lien_sau_so_mu_van_la_phep_tru() {
        assertEquals("x²−1", SoMu.hien("x^2−1"))
        assertEquals("x²+y", SoMu.hien("x^2+y"))
    }

    @Test
    fun kieu_khac_thi_de_nguyen() {
        assertEquals("x^(n+1)", SoMu.hien("x^(n+1)"))
        assertEquals("Không có số mũ", SoMu.hien("Không có số mũ"))
    }
}
