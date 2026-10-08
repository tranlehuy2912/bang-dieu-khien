package vn.huytl.bangdieukhien.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lenh dang cho tablet: luc nao hien tren man Bang.
 *
 * Tu 8/10/2026 tablet lam moi lenh du tre bao lau (anh Huy chot), nen khong con canh "qua nua
 * tieng thi tablet bo qua" de thu nhu truoc.
 */
class LenhChoTest {

    private val go = 1_000_000L

    private fun lenh(tao: Long = go, chuaLenMang: Boolean = false) =
        LenhCho(id = "a", kieu = Lenh.CHO, phut = 15, tao = tao, chuaLenMang = chuaLenMang)

    @Test
    fun lenh_vua_go_chua_hien_de_khong_chop_len_moi_lan_bam() {
        // Tablet co mang thi lenh chi nam trong hang chung mot giay.
        assertFalse(lenh().dangHien(go + 1_000L))
        assertFalse(lenh(chuaLenMang = true).dangHien(go + 1_000L))
        assertTrue(lenh().dangHien(go + LenhCho.CHO_HIEN_MS))
        assertTrue(lenh(chuaLenMang = true).dangHien(go + 10_000L))
    }

    @Test
    fun khong_biet_luc_go_thi_hien_luon() {
        // Lenh khong co truong tao thi khong biet no nam do bao lau: hien ra, dung giau.
        assertTrue(lenh(tao = 0L).dangHien(go))
    }

    @Test
    fun nam_cho_lau_van_hien_de_rut_lai() {
        // Truoc 8/10/2026 lenh qua nua tieng thanh "Tablet sẽ bỏ qua". Gio tablet van lam, nen
        // lenh nam cho ca ngay van hien voi nut Rút lại.
        assertTrue(lenh().dangHien(go + 24 * 60 * 60_000L))
    }
}
