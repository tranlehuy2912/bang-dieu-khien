package vn.huytl.bangdieukhien.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import vn.huytl.bangdieukhien.data.KetQuaLenhLaptop
import vn.huytl.bangdieukhien.data.LenhLaptop
import vn.huytl.bangdieukhien.data.LenhLaptopCho
import vn.huytl.bangdieukhien.data.PhieuLaptopCho
import vn.huytl.bangdieukhien.data.SuKienLaptop
import vn.huytl.bangdieukhien.data.SuKienTrenLaptop
import vn.huytl.bangdieukhien.data.TinhTrangLaptop
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Chu cua the Laptop (8/10/2026): trang thai, dong "Hôm nay", dong lenh. */
class ChuLaptopTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun gioVietNam() {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"))
        }
    }

    private fun luc(s: String): Long =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
        }.parse(s)!!.time

    private fun laptop(
        phien: String = "", phienTu: Long = 0L, batLuc: Long = luc("2026-10-08 15:36"),
        tatLuc: Long = 0L, capNhatLuc: Long = luc("2026-10-08 16:41"),
        suKien: List<SuKienTrenLaptop> = emptyList(), ketQua: List<KetQuaLenhLaptop> = emptyList()
    ) = TinhTrangLaptop(
        ketThucLuc = 0L, conLaiMs = 45 * 60_000L, dangDung = false, capNhatLuc = capNhatLuc,
        moWeb = false, webDangMo = false, phien = phien, phienTu = phienTu, batLuc = batLuc,
        tatLuc = tatLuc, suKien = suKien, ketQua = ketQua
    )

    private val hoiLuc = luc("2026-10-08 16:40")

    @Test
    fun da_tra_loi_thi_noi_ai_dang_dung_va_tu_luc_nao() {
        val tt = ChuLaptop.trangThai(
            laptop(phien = "lehoa", phienTu = luc("2026-10-08 16:19")), "h1", hoiLuc, hoiLuc + 30_000L
        )
        assertEquals("Đang bật · tài khoản Netflix từ 16:19", tt.chu)
        assertEquals(ChuLaptop.Mau.XANH, tt.mau)
        assertTrue(tt.choBam)
    }

    @Test
    fun admin_dang_dung_thi_mau_vang_va_bao_khong_tinh_phut() {
        val tt = ChuLaptop.trangThai(
            laptop(phien = "huy", phienTu = luc("2026-10-08 10:50")), "h1", hoiLuc, hoiLuc + 30_000L
        )
        assertEquals("Đang bật · tài khoản Admin từ 10:50, không tính phút", tt.chu)
        assertEquals(ChuLaptop.Mau.VANG, tt.mau)
    }

    @Test
    fun tra_loi_bang_ket_qua_lenh_hoi_du_cap_nhat_luc_cu() {
        // Dong ho laptop cham hon dien thoai vai giay: capNhatLuc nho hon hoiLuc ma van la tra loi.
        val l = laptop(
            capNhatLuc = hoiLuc - 3_000L,
            ketQua = listOf(KetQuaLenhLaptop("h1", LenhLaptop.HOI, true, "", hoiLuc - 3_000L))
        )
        assertEquals("Đang bật · chưa ai đăng nhập", ChuLaptop.trangThai(l, "h1", hoiLuc, hoiLuc + 60_000L).chu)
    }

    @Test
    fun chua_tra_loi_thi_dang_hoi_roi_qua_150_giay_la_khong_tra_loi_va_khoa_nut() {
        val l = laptop(capNhatLuc = luc("2026-10-08 11:53"))
        val dangHoi = ChuLaptop.trangThai(l, "h1", hoiLuc, hoiLuc + 60_000L)
        assertEquals("Đang hỏi laptop…", dangHoi.chu)
        assertTrue(dangHoi.choBam)
        val im = ChuLaptop.trangThai(l, "h1", hoiLuc, hoiLuc + ChuLaptop.CHO_TRA_LOI_MS + 1_000L)
        assertEquals("Không thấy laptop trả lời từ 11:53", im.chu)
        assertFalse(im.choBam)
    }

    @Test
    fun tat_dung_cach_thi_bao_luc_tat_ke_ca_ngay_khac() {
        val homNay = laptop(batLuc = luc("2026-10-08 10:48"), tatLuc = luc("2026-10-08 11:53"))
        assertEquals("Đã tắt lúc 11:53", ChuLaptop.trangThai(homNay, null, hoiLuc, hoiLuc).chu)
        assertFalse(ChuLaptop.trangThai(homNay, null, hoiLuc, hoiLuc).choBam)
        val homQua = laptop(batLuc = luc("2026-10-07 20:56"), tatLuc = luc("2026-10-07 22:20"))
        assertEquals("Đã tắt lúc 22:20 ngày 7/10", ChuLaptop.trangThai(homQua, null, hoiLuc, hoiLuc).chu)
    }

    @Test
    fun bat_lai_sau_lan_tat_thi_khong_con_la_da_tat() {
        val l = laptop(batLuc = luc("2026-10-08 15:36"), tatLuc = luc("2026-10-08 11:53"), capNhatLuc = hoiLuc)
        assertFalse(l.daTat())
        assertEquals("Đang bật · chưa ai đăng nhập", ChuLaptop.trangThai(l, "h1", hoiLuc, hoiLuc + 5_000L).chu)
    }

    @Test
    fun dong_hom_nay_ke_bat_tat_va_lan_vao_bo_lan_ra() {
        val sk = listOf(
            SuKienTrenLaptop(SuKienLaptop.BAT, luc("2026-10-08 09:36"), ""),
            SuKienTrenLaptop(SuKienLaptop.VAO, luc("2026-10-08 09:36"), "huy"),
            SuKienTrenLaptop(SuKienLaptop.RA, luc("2026-10-08 10:06"), "huy"),
            SuKienTrenLaptop(SuKienLaptop.TAT, luc("2026-10-08 10:06"), ""),
            SuKienTrenLaptop(SuKienLaptop.MAT, luc("2026-10-08 11:53"), ""),
            SuKienTrenLaptop(SuKienLaptop.BAT, luc("2026-10-08 15:36"), ""),
            SuKienTrenLaptop(SuKienLaptop.VAO, luc("2026-10-08 16:19"), "lehoa"),
            // Dong cua hom qua con sot thi khong ke.
            SuKienTrenLaptop(SuKienLaptop.TAT, luc("2026-10-07 22:20"), "")
        )
        assertEquals(
            "Hôm nay: bật 09:36, Admin vào 09:36, tắt 10:06, tắt đột ngột khoảng 11:53, bật 15:36, " +
                "Netflix vào 16:19",
            ChuLaptop.homNay(laptop(suKien = sk), luc("2026-10-08 16:45"))
        )
    }

    @Test
    fun bat_tu_hom_qua_ma_chua_co_gi_hom_nay() {
        val l = laptop(batLuc = luc("2026-10-07 21:00"))
        assertEquals("Hôm nay: đang bật từ hôm qua", ChuLaptop.homNay(l, luc("2026-10-08 07:00")))
        assertNull(ChuLaptop.homNay(laptop(batLuc = 0L), luc("2026-10-08 07:00")))
    }

    @Test
    fun so_netflix_dem_lui_luc_dang_xem() {
        val bayGio = luc("2026-10-08 16:41")
        val dangXem = laptop().copy(dangDung = true, ketThucLuc = bayGio + 38 * 60_000L + 12_000L)
        assertEquals("còn 38:12 Netflix", ChuLaptop.soNetflix(dangXem, bayGio))
        assertEquals("còn 45 phút Netflix", ChuLaptop.soNetflix(laptop(), bayGio))
        assertEquals("Hết phút Netflix", ChuLaptop.soNetflix(laptop().copy(conLaiMs = 0L), bayGio))
    }

    @Test
    fun dong_lenh_ke_ket_qua_lenh_bi_bo_roi_lenh_dang_cho_bo_hoi() {
        val bayGio = luc("2026-10-08 16:50")
        val ketQua = listOf(
            KetQuaLenhLaptop("a", LenhLaptop.NHAN, true, "Đã đọc 2 lần trên tivi lúc 16:41.", luc("2026-10-08 16:41")),
            KetQuaLenhLaptop("h", LenhLaptop.HOI, true, "", luc("2026-10-08 16:49"))
        )
        val bo = listOf(LenhLaptopCho("t", LenhLaptop.TAT_MAY, luc("2026-10-08 16:30")) to luc("2026-10-08 16:35"))
        val cho = listOf(
            LenhLaptopCho("h2", LenhLaptop.HOI, luc("2026-10-08 16:49")),
            LenhLaptopCho("c", LenhLaptop.CHUP, luc("2026-10-08 16:49"))
        )
        val dong = ChuLaptop.dongLenh(cho, ketQua, bo, bayGio)
        assertEquals(
            listOf(
                ChuLaptop.DongLenh("Laptop chưa nhận lệnh Tắt máy trong 5 phút, đã bỏ.", true),
                ChuLaptop.DongLenh("Đã đọc 2 lần trên tivi lúc 16:41.", false),
                ChuLaptop.DongLenh("Đang chờ laptop nhận: Chụp màn hình (gửi 16:49).", false, cho = true)
            ),
            dong
        )
    }

    @Test
    fun phieu_cap_bot_dang_cho_hien_chung_lenh_cho_theo_thu_tu_gui_bo_phieu_hom_truoc() {
        val bayGio = luc("2026-10-09 14:10")
        val cho = listOf(LenhLaptopCho("c", LenhLaptop.CHUP, luc("2026-10-09 14:06")))
        val phieu = listOf(
            PhieuLaptopCho("p1", 20, luc("2026-10-09 14:05")),
            PhieuLaptopCho("p2", -15, luc("2026-10-09 14:07")),
            // Phieu tu toi qua: laptop bat lai se bo, nen khong hien.
            PhieuLaptopCho("cu", 30, luc("2026-10-08 21:00"))
        )
        assertEquals(
            listOf(
                ChuLaptop.DongLenh("Chờ nhận thêm 20 phút (gửi 14:05)", false, cho = true),
                ChuLaptop.DongLenh("Đang chờ laptop nhận: Chụp màn hình (gửi 14:06).", false, cho = true),
                ChuLaptop.DongLenh("Chờ nhận bớt 15 phút (gửi 14:07)", false, cho = true)
            ),
            ChuLaptop.dongLenh(cho, emptyList(), emptyList(), bayGio, phieu)
        )
    }

    @Test
    fun phieu_tu_mot_tieng_ghi_theo_tieng() {
        assertEquals(
            "Chờ nhận thêm 1 tiếng 30 phút (gửi 14:05)",
            ChuLaptop.chuPhieu(PhieuLaptopCho("p", 90, luc("2026-10-09 14:05")))
        )
        assertEquals(
            "Chờ nhận bớt 1 tiếng (gửi 14:05)",
            ChuLaptop.chuPhieu(PhieuLaptopCho("p", -60, luc("2026-10-09 14:05")))
        )
    }

    @Test
    fun ket_qua_cu_hon_nua_tieng_thi_thoi_hien() {
        val ketQua = listOf(KetQuaLenhLaptop("a", LenhLaptop.CHUP, true, "Đã chụp.", luc("2026-10-08 15:00")))
        assertTrue(ChuLaptop.dongLenh(emptyList(), ketQua, emptyList(), luc("2026-10-08 16:00")).isEmpty())
    }

    @Test
    fun lenh_qua_5_phut_la_qua_han() {
        val l = LenhLaptopCho("x", LenhLaptop.NHAN, luc("2026-10-08 16:00"))
        assertFalse(ChuLaptop.quaHan(l, luc("2026-10-08 16:05")))
        assertTrue(ChuLaptop.quaHan(l, luc("2026-10-08 16:05") + 1L))
    }
}
