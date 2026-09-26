package vn.huytl.bangdieukhien.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Bai nao con cho duyet, bai nao da xong.
 *
 * Canh that: bai nop 20:29 ngay 24/9/2026 van ghi CHO tren Firestore sau khi tablet da
 * bo no luc sang ngay 25, va man bai hien hai nut duyet ma bam khong con tac dung gi.
 */
class BaiTest {

    private fun luc(ngay: Int, gio: Int, phut: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.SEPTEMBER, ngay, gio, phut)
        }.timeInMillis

    private fun bai(trangThai: String, luc: Long) = Bai(
        id = "b1", luc = luc, trangThai = trangThai, soPhut = 0,
        anh = emptyList(), cham = null, messageId = 0L
    )

    @Test
    fun bai_cho_nop_hom_truoc_la_qua_ngay() {
        val b = bai(Bai.CHO, luc(24, 20, 29))
        assertTrue(b.quaNgay(bayGio = luc(26, 17, 33)))
        // Qua nua dem mot phut la da sang ngay moi, giong cach tablet chia ngay.
        assertTrue(bai(Bai.CHO, luc(24, 23, 59)).quaNgay(bayGio = luc(25, 0, 1)))
        assertFalse(b.quaNgay(bayGio = luc(24, 23, 59)))
    }

    @Test
    fun chi_bai_cho_moi_qua_ngay() {
        val bayGio = luc(26, 17, 33)
        assertFalse(bai(Bai.DUYET, luc(24, 20, 29)).quaNgay(bayGio))
        assertFalse(bai(Bai.TU_CHOI, luc(24, 20, 29)).quaNgay(bayGio))
        assertFalse(bai(Bai.HUY, luc(24, 20, 29)).quaNgay(bayGio))
        // Thieu gio nop thi khong biet ngay nao, coi nhu con cho.
        assertFalse(bai(Bai.CHO, 0L).quaNgay(bayGio))
    }

    @Test
    fun bai_cho_hom_nay_con_nut_duyet_va_khong_xoa_duoc() {
        val b = bai(Bai.CHO, System.currentTimeMillis())
        assertTrue(b.dangCho)
        assertFalse(b.xong)
    }

    @Test
    fun bai_qua_ngay_va_bai_da_xu_ly_deu_xoa_duoc() {
        assertTrue(bai(Bai.CHO, luc(24, 20, 29)).xong)
        assertFalse(bai(Bai.CHO, luc(24, 20, 29)).dangCho)
        assertTrue(bai(Bai.DUYET, System.currentTimeMillis()).xong)
        assertTrue(bai(Bai.TU_CHOI, System.currentTimeMillis()).xong)
        assertTrue(bai(Bai.HUY, System.currentTimeMillis()).xong)
    }
}
