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
    fun lenh_cap_quy_va_xu_cau_co_nhan_rieng() {
        // Tu 29/9/2026. Thieu so phut hay 0 la cap het quy, y nhu tablet hieu.
        assertEquals("cấp 30 phút từ quỹ giờ chơi", Dinh.lenh(lenh(Lenh.CAP_QUY, 30)))
        assertEquals("cấp hết quỹ giờ chơi", Dinh.lenh(lenh(Lenh.CAP_QUY)))
        assertEquals("cấp hết quỹ giờ chơi", Dinh.lenh(lenh(Lenh.CAP_QUY, 0)))
        assertEquals("xử câu chưa chắc", Dinh.lenh(lenh(Lenh.XU_CAU)))
    }

    @Test
    fun dong_cho_tablet_cho_choi_bot_gio_viet_nhu_the_laptop() {
        // Anh Huy doi 9/10/2026, luc dau la "Đang chờ tablet nhận: cho chơi 15 phút, gửi lúc 14:06.".
        assertEquals("Thêm 15 phút lúc 14:06 (Chờ)", Dinh.dongChoTablet(lenh(Lenh.CHO, 15), "14:06"))
        assertEquals("Thêm 1 tiếng 30 phút lúc 14:06 (Chờ)", Dinh.dongChoTablet(lenh(Lenh.CHO, 90), "14:06"))
        assertEquals("Bớt 10 phút lúc 14:07 (Chờ)", Dinh.dongChoTablet(lenh(Lenh.BOT, 10), "14:07"))
        assertEquals("Bớt 15 phút lúc 14:07 (Chờ)", Dinh.dongChoTablet(lenh(Lenh.BOT), "14:07"))
        // Lenh khac giu cau cu.
        assertEquals(
            "Đang chờ tablet nhận: duyệt bài, 45 phút, gửi lúc 14:06.",
            Dinh.dongChoTablet(lenh(Lenh.DUYET, 45), "14:06")
        )
        assertEquals("Đang chờ tablet nhận: cho chơi, gửi lúc 14:06.", Dinh.dongChoTablet(lenh(Lenh.CHO), "14:06"))
    }

    @Test
    fun lenh_la_thi_hien_ten_kieu() {
        // Ban Bang dieu khien moi hon go ra kieu ban nay chua biet: van hien, khong de trong.
        assertEquals("kieumoi", Dinh.lenh(lenh("KIEUMOI")))
    }
}
