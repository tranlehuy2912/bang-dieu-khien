package vn.huytl.bangdieukhien.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import vn.huytl.bangdieukhien.data.Buoi
import vn.huytl.bangdieukhien.data.LichMau
import vn.huytl.bangdieukhien.data.NgayNghi
import vn.huytl.bangdieukhien.ui.LichHoc.Hang
import java.util.Calendar

/** Tab Lich hoc: buoi toi, tuan dang xem, hang cua luoi, chu tren the. */
class LichHocTest {

    /** Tinh theo lich: chay tren ban lich dung yen, xem [LichMau]. */
    @get:Rule
    val lich = LichMau.Rule()

    private fun luc(nam: Int, thang: Int, ngay: Int, gio: Int, phut: Int = 0): Calendar =
        NgayNghi.calendarCua(nam, thang, ngay, gio, phut)

    private fun ngay(c: Calendar) = LichHoc.ngayThang(c)

    /** Thu ba 29/9/2026 buoi sang: chua toi buoi chieu, the ghi du ba moc. */
    @Test
    fun sang_thu_ba_buoi_toi_la_chieu_nay() {
        val now = luc(2026, 9, 29, 9)
        val toi = LichHoc.buoiToi(now)!!

        assertEquals("29/09", ngay(toi.ngay))
        assertEquals(Buoi.CHIEU, toi.buoi.buoi)
        assertFalse(toi.dangHoc)
        assertEquals("Chiều nay", LichHoc.tenBuoi(toi, now))
        assertEquals("Buông máy 11:30 · vào học 12:45 · tan 17:00", LichHoc.chiTiet(toi))
    }

    /** Da qua moc buong may, chua vao tiet dau: van la buoi sap toi. */
    @Test
    fun da_buong_may_chua_vao_hoc_van_la_sap_toi() {
        val toi = LichHoc.buoiToi(luc(2026, 9, 29, 12, 30))!!

        assertEquals("29/09", ngay(toi.ngay))
        assertFalse(toi.dangHoc)
    }

    /** Dang o truong: ghi tiet dang hoc, ke ca giua gio ra choi thi van la tiet vua hoc. */
    @Test
    fun dang_hoc_ghi_tiet_dang_hoc() {
        val now = luc(2026, 9, 29, 14, 20)
        val toi = LichHoc.buoiToi(now)!!

        assertTrue(toi.dangHoc)
        assertEquals(3, toi.tiet)
        assertEquals("Chiều nay", LichHoc.tenBuoi(toi, now))
        assertEquals("Tiết 3: Âm nhạc · tan 17:00", LichHoc.chiTiet(toi))

        // Tiet 3 bat dau 14:15, tiet 4 luc 15:30: 15:10 la ra choi sau tiet 3.
        assertEquals(3, LichHoc.buoiToi(luc(2026, 9, 29, 15, 10))!!.tiet)
        assertEquals(1, LichHoc.buoiToi(luc(2026, 9, 29, 12, 45))!!.tiet)
    }

    /** Tan hoc roi thi sang buoi cua ngay mai. */
    @Test
    fun tan_hoc_roi_thi_la_buoi_ngay_mai() {
        val now = luc(2026, 9, 29, 17, 0)
        val toi = LichHoc.buoiToi(now)!!

        assertEquals("30/09", ngay(toi.ngay))
        assertEquals("Chiều mai", LichHoc.tenBuoi(toi, now))
    }

    /** Thu hai co ca buoi sang: sang tan roi thi toi buoi chieu cung ngay. */
    @Test
    fun thu_hai_hai_buoi() {
        val sang = LichHoc.buoiToi(luc(2026, 9, 28, 7))!!
        assertEquals(Buoi.SANG, sang.buoi.buoi)
        assertEquals("Buông máy 08:45 · vào học 09:15 · tan 10:45", LichHoc.chiTiet(sang))

        val chieu = LichHoc.buoiToi(luc(2026, 9, 28, 10, 45))!!
        assertEquals(Buoi.CHIEU, chieu.buoi.buoi)
        assertEquals("28/09", ngay(chieu.ngay))
    }

    /** Chu nhat va chieu thu bay da tan: buoi toi la sang thu hai, luoi sang tuan toi. */
    @Test
    fun cuoi_tuan_xem_tuan_toi() {
        listOf(luc(2026, 10, 3, 17, 30), luc(2026, 10, 4, 10)).forEach { now ->
            val toi = LichHoc.buoiToi(now)!!
            assertEquals("05/10", ngay(toi.ngay))
            assertEquals(Buoi.SANG, toi.buoi.buoi)

            val tuan = LichHoc.tuanXem(now, toi)
            assertEquals(listOf("05/10", "06/10", "07/10", "08/10", "09/10", "10/10"), tuan.map(::ngay))
        }
        // Chu nhat thi sang thu hai la "mai".
        val cn = luc(2026, 10, 4, 10)
        assertEquals("Sáng mai", LichHoc.tenBuoi(LichHoc.buoiToi(cn)!!, cn))
        // Chieu thu bay thi con cach mot ngay: ghi thu va ngay.
        val t7 = luc(2026, 10, 3, 17, 30)
        assertEquals("Sáng thứ hai 05/10", LichHoc.tenBuoi(LichHoc.buoiToi(t7)!!, t7))
    }

    /** Giua tuan: luoi la thu hai toi thu bay cua tuan nay. */
    @Test
    fun giua_tuan_xem_tuan_nay() {
        val now = luc(2026, 9, 30, 20)
        val tuan = LichHoc.tuanXem(now, LichHoc.buoiToi(now))

        assertEquals(listOf("28/09", "29/09", "30/09", "01/10", "02/10", "03/10"), tuan.map(::ngay))
        assertEquals(listOf("Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7"), tuan.map(LichHoc::dauCot))
    }

    /** Nghi Tet 1/2 toi 10/2/2027: buoi toi la chieu thu nam 11/2, luoi la tuan co ngay do. */
    @Test
    fun nghi_tet_nhay_qua_ky_nghi() {
        val now = luc(2027, 1, 30, 18)
        val toi = LichHoc.buoiToi(now)!!

        assertEquals("11/02", ngay(toi.ngay))
        assertEquals(Buoi.CHIEU, toi.buoi.buoi)
        assertEquals("08/02", ngay(LichHoc.tuanXem(now, toi).first()))
    }

    /** Het nam hoc: khong con buoi nao, the an, luoi van la tuan nay. */
    @Test
    fun nghi_he_khong_con_buoi_toi() {
        val now = luc(2027, 6, 2, 9)

        assertNull(LichHoc.buoiToi(now))
        assertEquals("31/05", ngay(LichHoc.tuanXem(now, null).first()))
    }

    /**
     * Luoi bo tiet ca tuan khong ai hoc: buoi sang chi con tiet 3, 4 (the duc thu hai, tin
     * hoc thu sau), buoi chieu du nam tiet.
     */
    @Test
    fun luoi_bo_tiet_ca_tuan_trong() {
        val now = luc(2026, 9, 30, 9)
        val hang = LichHoc.dungHang(LichHoc.tuanXem(now, LichHoc.buoiToi(now)))

        assertEquals(
            listOf(
                Hang.Bang(Buoi.SANG),
                Hang.Tiet(Buoi.SANG, 3), Hang.Tiet(Buoi.SANG, 4),
                Hang.Bang(Buoi.CHIEU),
                Hang.Tiet(Buoi.CHIEU, 1), Hang.Tiet(Buoi.CHIEU, 2), Hang.Tiet(Buoi.CHIEU, 3),
                Hang.Tiet(Buoi.CHIEU, 4), Hang.Tiet(Buoi.CHIEU, 5)
            ),
            hang
        )
    }

    /** Ten dai viet gon cho vua o, ten ngan va mon la giu nguyen. */
    @Test
    fun ten_ngan_trong_o() {
        assertEquals("KHTN", LichHoc.tenNgan("Khoa học tự nhiên"))
        assertEquals("Trải nghiệm", LichHoc.tenNgan("Trải nghiệm hướng nghiệp"))
        assertEquals("Toán", LichHoc.tenNgan("Toán"))
        assertEquals("Môn mới", LichHoc.tenNgan("Môn mới"))
    }

    /** Ma buoi phan biet sang, chieu cua cung mot ngay. */
    @Test
    fun ma_buoi() {
        val c = luc(2026, 9, 28, 0)
        assertEquals("20260928-SANG", LichHoc.maBuoi(c, Buoi.SANG))
        assertEquals("20260928-CHIEU", LichHoc.maBuoi(c, Buoi.CHIEU))
    }
}
