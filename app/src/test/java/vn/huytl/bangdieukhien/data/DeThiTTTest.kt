package vn.huytl.bangdieukhien.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Truong deThi cua hop/trangthai (30/9/2026): doc mang de thi tablet gui, xem [Duong.F_DE_THI]. */
class DeThiTTTest {

    @Test
    fun vang_truong_la_tablet_ban_cu() {
        assertNull(DeThiTT.docDanhSach(null))
        assertNull(DeThiTT.docDanhSach("khong phai mang"))
    }

    @Test
    fun doc_du_truong_so_la_long_nhu_firestore_tra_ve() {
        val ds = DeThiTT.docDanhSach(
            listOf(
                mapOf("ma" to "GK1-1", "ten" to "Đề giữa kì 1 số 1", "den" to 3L, "tt" to "XONG", "sao" to 41L, "toiDa" to 55L),
                mapOf("ma" to "GK1-2", "ten" to "Đề giữa kì 1 số 2", "den" to 3L, "tt" to "SAN", "sao" to -1L, "toiDa" to -1L)
            )
        )!!
        assertEquals(listOf("GK1-1", "GK1-2"), ds.map { it.ma })
        assertEquals(DeThiTT(ma = "GK1-1", ten = "Đề giữa kì 1 số 1", den = 3, tt = DeThiTT.XONG, sao = 41, toiDa = 55), ds[0])
        assertTrue(ds[0].daNop)
        assertFalse(ds[1].daNop)
    }

    @Test
    fun bo_phan_tu_thieu_ma_va_lay_ma_lam_ten_khi_thieu_ten() {
        val ds = DeThiTT.docDanhSach(listOf(mapOf("ten" to "không mã"), "rác", mapOf("ma" to "HK1-1", "tt" to "KHOA")))!!
        assertEquals(1, ds.size)
        assertEquals("HK1-1", ds[0].ten)
        assertEquals(0, ds[0].den)
        assertEquals(-1, ds[0].sao)
    }
}
