package vn.huytl.bangdieukhien.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import vn.huytl.bangdieukhien.ui.LichHoc

/**
 * Ngay Ba Huy cho nghi hoc ngoai lich (30/10/2026), xem [NgayNghi.ngayThem].
 *
 * NgayNghi.kt chep y het ban tablet (kiem-duong.sh so hai ban), nen test nay cung la test cho
 * luat ben tablet: ngay cho nghi thi khong chan gio hoc, the buoi toi nhay qua ngay do.
 */
class NgayNghiTest {

    @get:Rule
    val lich = LichMau.Rule()

    @After
    fun tearDown() {
        NgayNghi.ngayThem = { emptySet() }
    }

    @Test
    fun ngay_cho_nghi_thanh_ngay_nghi_ngay_khac_van_hoc() {
        // Thu sau 30/10/2026 hoc ca hai buoi theo lich mau.
        val thuSau = NgayNghi.calendarCua(2026, 10, 30)
        assertNull(NgayNghi.tenKyNghi(thuSau))

        NgayNghi.ngayThem = { setOf(20261030) }
        assertEquals(NgayNghi.TEN_NGHI_THEM, NgayNghi.tenKyNghi(thuSau))
        // tenTheoLich khong ke ngay cho nghi: tablet dung no de bao "đã nghỉ sẵn theo lịch".
        assertNull(NgayNghi.tenTheoLich(thuSau))
        assertNull(NgayNghi.tenKyNghi(NgayNghi.calendarCua(2026, 10, 31)))
    }

    /** Chu nhat da nghi san: ten van la "chủ nhật", khong bi ten ngay cho nghi de len. */
    @Test
    fun ngay_da_nghi_theo_lich_giu_ten_cu() {
        NgayNghi.ngayThem = { setOf(20261101) }
        assertEquals("chủ nhật", NgayNghi.tenKyNghi(NgayNghi.calendarCua(2026, 11, 1)))
    }

    /** Sang thu sau duoc cho nghi: buoi toi la chieu thu bay, khong phai buoi sang thu sau. */
    @Test
    fun cho_nghi_hom_nay_thi_buoi_toi_sang_ngay_hoc_ke_tiep() {
        val now = NgayNghi.calendarCua(2026, 10, 30, 7, 0)
        assertEquals("30/10", LichHoc.ngayThang(LichHoc.buoiToi(now)!!.ngay))

        NgayNghi.ngayThem = { setOf(20261030) }
        val toi = LichHoc.buoiToi(now)!!
        assertEquals("31/10", LichHoc.ngayThang(toi.ngay))
        assertEquals(Buoi.CHIEU, toi.buoi.buoi)
    }

    @Test
    fun so_ngay_dang_yyyymmdd() {
        assertEquals(20270105, NgayNghi.soNgay(NgayNghi.calendarCua(2027, 1, 5)))
    }
}
