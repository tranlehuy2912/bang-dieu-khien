package vn.huytl.bangdieukhien.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Hai truong de thi cua hop/trangthai: cacDeThi (1/10/2026, de cua ca ba mon, xem
 * [Duong.F_CAC_DE_THI]) doc truoc, vang thi deThi cua tablet ban cu (30/9/2026, chi de Tieng
 * Anh, xem [Duong.F_DE_THI]).
 */
class DeThiTTTest {

    /** Mot phan tu cacDeThi dung nhu tablet ghi: so nguyen Firestore tra ve la Long. */
    private val deToan = mapOf(
        "ma" to "TGK1-2", "mon" to "Toán", "ten" to "Đề giữa kì 1 số 2",
        "phamVi" to "Đại số tới Bài 9, Hình học tới Bài 14", "tt" to "KHOA",
        "thieu" to "Hình học mới tới Bài 12, đề cần Bài 14",
        "sao" to -1L, "toiDa" to -1L, "nopLuc" to 0L, "phut" to 90L, "doRong" to 14L
    )

    private val deAnhCu = mapOf(
        "ma" to "GK1-1", "ten" to "Đề giữa kì 1 số 1", "den" to 3L, "tt" to "XONG", "sao" to 41L, "toiDa" to 55L
    )

    @Test
    fun vang_ca_hai_truong_la_tablet_chua_co_de_thi() {
        assertNull(DeThiTT.doc(null, null))
        assertNull(DeThiTT.doc("khong phai mang", "cung khong phai"))
    }

    @Test
    fun doc_cac_de_thi_du_truong() {
        val anh = mapOf(
            "ma" to "GK1-1", "mon" to "Tiếng Anh", "ten" to "Đề giữa kì 1 số 1", "phamVi" to "tới Unit 3",
            "tt" to "XONG", "thieu" to "", "sao" to 41L, "toiDa" to 55L,
            "nopLuc" to 1_790_000_000_000L, "phut" to 45L
        )
        val ds = DeThiTT.doc(listOf(deToan, anh), null)!!

        assertEquals(
            DeThiTT(
                ma = "TGK1-2", ten = "Đề giữa kì 1 số 2", tt = DeThiTT.KHOA, mon = "Toán",
                phamVi = "Đại số tới Bài 9, Hình học tới Bài 14",
                thieu = "Hình học mới tới Bài 12, đề cần Bài 14",
                sao = -1, toiDa = -1, nopLuc = 0L, phut = 90, doRong = 14
            ),
            ds[0]
        )
        assertEquals(
            DeThiTT(
                ma = "GK1-1", ten = "Đề giữa kì 1 số 1", tt = DeThiTT.XONG, mon = "Tiếng Anh",
                phamVi = "tới Unit 3", thieu = "", sao = 41, toiDa = 55,
                nopLuc = 1_790_000_000_000L, phut = 45
            ),
            ds[1]
        )
        assertFalse(ds[0].daNop)
        assertTrue(ds[1].daNop)
    }

    /** Thieu truong thi ra gia tri mac dinh nhin thay ngay, khong lam hong ca danh sach. */
    @Test
    fun cac_de_thi_thieu_truong_ra_gia_tri_mac_dinh() {
        val ds = DeThiTT.doc(listOf(mapOf("ma" to " KGK1-1 ", "tt" to "SAN", "nopLuc" to -5L)), null)!!

        assertEquals(
            DeThiTT(
                ma = "KGK1-1", ten = "KGK1-1", tt = DeThiTT.SAN, mon = "", phamVi = "", thieu = "",
                sao = -1, toiDa = -1, nopLuc = 0L, phut = 0
            ),
            ds.single()
        )
    }

    @Test
    fun co_cac_de_thi_thi_khong_doc_de_thi_cu() {
        assertEquals(listOf("TGK1-2"), DeThiTT.doc(listOf(deToan), listOf(deAnhCu))!!.map { it.ma })
        // Mang rong van la tablet moi, chi la chua co de nao: khong lui ve deThi.
        assertEquals(emptyList<DeThiTT>(), DeThiTT.doc(emptyList<Any>(), listOf(deAnhCu)))
        // cacDeThi hong (khong phai mang) thi coi nhu vang.
        assertEquals(listOf("GK1-1"), DeThiTT.doc("rác", listOf(deAnhCu))!!.map { it.ma })
    }

    @Test
    fun tablet_ban_cu_doc_de_thi_va_coi_moi_de_la_tieng_anh() {
        val ds = DeThiTT.doc(
            null,
            listOf(
                deAnhCu,
                mapOf("ma" to "GK1-4", "ten" to "Đề giữa kì 1 số 4", "den" to 9L, "tt" to "KHOA", "sao" to -1L, "toiDa" to -1L)
            )
        )!!

        assertEquals(
            DeThiTT(
                ma = "GK1-1", ten = "Đề giữa kì 1 số 1", tt = DeThiTT.XONG, mon = DeThiTT.MON_ANH,
                phamVi = "tới Unit 3", sao = 41, toiDa = 55, doRong = 3
            ),
            ds[0]
        )
        // Ban cu khong gui phan thieu, luc nop, gio lam bai.
        assertEquals(listOf("tới Unit 9", "", 0L, 0), ds[1].let { listOf(it.phamVi, it.thieu, it.nopLuc, it.phut) })
        assertEquals("Tiếng Anh", ds[1].mon)
    }

    @Test
    fun bo_phan_tu_thieu_ma_va_lay_ma_lam_ten_khi_thieu_ten() {
        val rac = listOf(mapOf("ten" to "không mã"), "rác")

        val moi = DeThiTT.doc(rac + mapOf("ma" to "TCK1-1", "tt" to "KHOA"), null)!!
        assertEquals(1, moi.size)
        assertEquals("TCK1-1", moi[0].ten)

        val cu = DeThiTT.doc(null, rac + mapOf("ma" to "HK1-1", "tt" to "KHOA"))!!
        assertEquals(1, cu.size)
        assertEquals("HK1-1", cu[0].ten)
        // Thieu den thi khong biet pham vi, khong ghi "tới Unit 0".
        assertEquals("", cu[0].phamVi)
        assertEquals(-1, cu[0].doRong)
        assertEquals(-1, cu[0].sao)
    }
}
