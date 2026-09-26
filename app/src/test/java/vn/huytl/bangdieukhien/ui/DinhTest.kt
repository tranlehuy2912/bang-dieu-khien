package vn.huytl.bangdieukhien.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import vn.huytl.bangdieukhien.data.Lenh
import vn.huytl.bangdieukhien.data.LenhCho

/** Cau mo ta lenh dang cho tablet tren man Bang. */
class DinhTest {

    private fun lenh(kieu: String, phut: Int? = null) =
        LenhCho(id = "a", kieu = kieu, phut = phut, tao = 1L, chuaLenMang = false)

    @Test
    fun lenh_cho_gio_ghi_ro_so_phut() {
        assertEquals("cho chơi 15 phút", Dinh.lenh(lenh(Lenh.CHO, 15)))
        assertEquals("cho chơi 1 tiếng 30 phút", Dinh.lenh(lenh(Lenh.CHO, 90)))
        assertEquals("duyệt bài, 45 phút", Dinh.lenh(lenh(Lenh.DUYET, 45)))
        assertEquals("mở toàn bộ máy, không đặt hạn", Dinh.lenh(lenh(Lenh.MO_MAY)))
    }

    @Test
    fun lenh_la_thi_hien_ten_kieu() {
        // Ban Bang dieu khien moi hon go ra kieu ban nay chua biet: van hien, khong de trong.
        assertEquals("kieumoi", Dinh.lenh(lenh("KIEUMOI")))
    }
}
