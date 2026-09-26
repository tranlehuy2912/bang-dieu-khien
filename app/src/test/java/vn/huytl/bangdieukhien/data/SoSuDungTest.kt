package vn.huytl.bangdieukhien.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.huytl.bangdieukhien.ui.Dinh
import java.util.Calendar

/**
 * Doc so dung app tablet gui sang, va cat, gop, cong y nhu NhatKySuDung ben tablet.
 *
 * Hai man cung mot ngay ma ra hai con so khac nhau thi Ba Huy khong biet tin ben nao,
 * nen may phep tinh o day phai giong het ben kia.
 */
class SoSuDungTest {

    private fun luc(ngay: Int, gio: Int, phut: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.SEPTEMBER, ngay, gio, phut)
        }.timeInMillis

    private fun doan(goi: String, tu: Long, den: Long) =
        mapOf(Duong.F_GOI to goi, Duong.F_TU to tu, Duong.F_DEN to den)

    private fun so(vararg cac: SoSuDung.Doan, ten: Map<String, String> = emptyMap()) =
        SoSuDung(cac.toList(), ten, giuNgay = 7, dangGhi = true, capNhatLuc = luc(26, 21, 0))

    private val phut = 60_000L

    @Test
    fun doc_duoc_ban_tablet_ghi() {
        val s = SoSuDung.doc(
            mapOf(
                Duong.F_DOAN to listOf(
                    doan("com.google.android.youtube", luc(26, 14, 3), luc(26, 14, 25)),
                    doan("com.zing.zalo", luc(26, 9, 0), luc(26, 9, 10))
                ),
                Duong.F_APP to listOf(
                    mapOf(Duong.F_GOI to "com.google.android.youtube", Duong.F_TEN to "YouTube"),
                    mapOf(Duong.F_GOI to "com.zing.zalo", Duong.F_TEN to "Zalo")
                ),
                Duong.F_GIU_NGAY to 7L,
                Duong.F_DANG_GHI to false,
                Duong.F_CAP_NHAT_LUC to luc(26, 21, 0)
            )
        )!!
        // Xep lai theo luc mo, du tablet gui theo thu tu nao.
        assertEquals(listOf("com.zing.zalo", "com.google.android.youtube"), s.cac.map { it.goi })
        assertEquals("YouTube", s.tenApp("com.google.android.youtube"))
        assertEquals(7, s.giuNgay)
        assertFalse(s.dangGhi)
        assertEquals(luc(26, 21, 0), s.capNhatLuc)
    }

    @Test
    fun khoang_hong_bi_bo_con_lai_van_doc() {
        val s = SoSuDung.doc(
            mapOf(
                Duong.F_DOAN to listOf(
                    doan("a", luc(26, 8, 0), luc(26, 8, 30)),
                    // Sua tay trong console la co the thanh so thuc.
                    mapOf(Duong.F_GOI to "b", Duong.F_TU to luc(26, 9, 0).toDouble(),
                        Duong.F_DEN to luc(26, 9, 5).toDouble()),
                    mapOf(Duong.F_GOI to "c", Duong.F_TU to luc(26, 10, 0)),
                    doan("", luc(26, 11, 0), luc(26, 11, 5)),
                    doan("d", luc(26, 12, 5), luc(26, 12, 0)),
                    "khong phai map"
                )
            )
        )!!
        assertEquals(listOf("a", "b"), s.cac.map { it.goi })
        assertEquals(5 * phut, s.cac[1].daiMs)
    }

    @Test
    fun thieu_truong_thi_ra_mac_dinh() {
        val s = SoSuDung.doc(emptyMap())!!
        assertTrue(s.cac.isEmpty())
        assertEquals(SoSuDung.GIU_NGAY_MAC_DINH, s.giuNgay)
        assertTrue(s.dangGhi)
        assertEquals(0L, s.capNhatLuc)
        assertNull(SoSuDung.doc(null as Map<String, Any?>?))
    }

    @Test
    fun app_khong_co_ten_thi_hien_ten_goi() {
        val s = so(SoSuDung.Doan("com.la.app", luc(26, 8, 0), luc(26, 8, 30)))
        assertEquals("com.la.app", s.tenApp("com.la.app"))
        assertEquals("com.la.app", s.theoApp(luc(26, 0, 0), luc(27, 0, 0)).single().ten)
    }

    @Test
    fun khoang_vat_nua_dem_bi_cat_o_moc_ngay() {
        val s = so(SoSuDung.Doan("a", luc(25, 23, 30), luc(26, 0, 45)))
        val homQua = s.cuaNgay(luc(25, 0, 0), luc(26, 0, 0))
        val homNay = s.cuaNgay(luc(26, 0, 0), luc(27, 0, 0))
        assertEquals(30 * phut, homQua.single().daiMs)
        assertEquals(45 * phut, homNay.single().daiMs)
        assertEquals(luc(26, 0, 0), homNay.single().tu)
        assertTrue(s.cuaNgay(luc(24, 0, 0), luc(25, 0, 0)).isEmpty())
    }

    @Test
    fun tong_ngay_gop_phan_chia_doi_man_hinh() {
        // YouTube 14:00-15:00, Zalo chia doi man hinh 14:30-15:10, Zalo lai 16:00-16:05.
        val s = so(
            SoSuDung.Doan("yt", luc(26, 14, 0), luc(26, 15, 0)),
            SoSuDung.Doan("zalo", luc(26, 14, 30), luc(26, 15, 10)),
            SoSuDung.Doan("zalo", luc(26, 16, 0), luc(26, 16, 5)),
            ten = mapOf("yt" to "YouTube", "zalo" to "Zalo")
        )
        val dau = luc(26, 0, 0)
        val cuoi = luc(27, 0, 0)
        // Cong thang ra 60 + 40 + 5 = 105 phut; may chi thuc su duoc cam 70 + 5.
        assertEquals(75 * phut, s.tongMs(dau, cuoi))

        val cac = s.theoApp(dau, cuoi)
        assertEquals(listOf("YouTube", "Zalo"), cac.map { it.ten })
        assertEquals(60 * phut, cac[0].tongMs)
        assertEquals(45 * phut, cac[1].tongMs)
        assertEquals(2, cac[1].cacDoan.size)
        assertEquals(luc(26, 14, 0) to luc(26, 16, 5), s.tuDen(dau, cuoi))
    }

    @Test
    fun ngay_khong_co_gi_thi_rong() {
        val s = so(SoSuDung.Doan("a", luc(26, 8, 0), luc(26, 8, 30)))
        val dau = luc(25, 0, 0)
        val cuoi = luc(26, 0, 0)
        assertTrue(s.theoApp(dau, cuoi).isEmpty())
        assertEquals(0L, s.tongMs(dau, cuoi))
        assertNull(s.tuDen(dau, cuoi))
    }

    @Test
    fun moc_ngay_la_nua_dem_theo_gio_may() {
        val bayGio = luc(26, 17, 33)
        assertEquals(luc(26, 0, 0), SoSuDung.dauNgay(0, bayGio))
        assertEquals(luc(25, 0, 0), SoSuDung.dauNgay(1, bayGio))
        assertEquals(luc(27, 0, 0), SoSuDung.dauNgay(-1, bayGio))
        assertEquals(luc(20, 0, 0), SoSuDung.dauNgay(6, bayGio))
    }

    @Test
    fun do_dai_viet_nhu_ben_tablet() {
        assertEquals("dưới 1 phút", Dinh.doDai(59_000L))
        assertEquals("45 phút", Dinh.doDai(45 * phut + 30_000L))
        assertEquals("1 tiếng", Dinh.doDai(60 * phut))
        assertEquals("1 tiếng 5 phút", Dinh.doDai(65 * phut))
    }
}
