package vn.huytl.bangdieukhien.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Dot viec nha va danh sach viec: doc, ghi, sua, va kiem hop sua danh sach.
 *
 * TEN TRUONG VIET THANG CHU KHONG QUA Duong o nhung cho dong vai may khac ghi xuong:
 * bo test nay phai ghim dung cai ten dang chay tren mang. Dung hang so thi doi ten
 * trong Duong.kt van xanh, ma may ba voi tablet thi da het hieu nhau.
 */
class ViecNhaTest {

    private fun viec(ten: String, phut: Int, xong: Boolean) =
        mapOf("ten" to ten, "phut" to phut, "xong" to xong)

    private fun dot(vararg cac: Map<String, Any>, ai: String? = null, luc: Long = 1_000L): ViecNha.Dot {
        val du = mutableMapOf<String, Any?>("maPhien" to "ab12cd34", "luc" to luc, "viec" to cac.toList())
        if (ai != null) du["ai"] = ai
        return ViecNha.docDot(du)!!
    }

    @Test
    fun doc_dung_mot_dot_may_ba_ghi() {
        val d = dot(viec("Quét nhà", 10, false), viec("Rửa chén", 15, true), ai = "banoi")
        assertEquals("ab12cd34", d.maPhien)
        assertEquals(1_000L, d.luc)
        assertEquals(Nguoi.BA_NOI, d.ai)
        assertEquals(listOf("Quét nhà", "Rửa chén"), d.cac.map { it.ten })
        assertEquals(25, d.tongPhut)
        assertFalse(d.xongHet)
        assertEquals("Quét nhà, Rửa chén", d.ke)
    }

    @Test
    fun ban_may_ba_cu_khong_co_truong_ai_thi_coi_la_ba_noi() {
        // Truoc khi Bang dieu khien giao duoc viec, chi may ba ghi o day va no chua
        // gui truong ai. Coi la Ba Huy thi tablet goi sai nguoi giao.
        assertEquals(Nguoi.BA_NOI, dot(viec("Quét nhà", 10, false)).ai)
    }

    @Test
    fun ban_khong_ra_hinh_thu_gi_thi_bo() {
        assertNull(ViecNha.docDot(null))
        assertNull(ViecNha.docDot(mapOf("viec" to listOf(viec("Quét nhà", 10, false)))))
        val d = ViecNha.docDot(
            mapOf(
                "maPhien" to "ab12cd34",
                "viec" to listOf(mapOf("phut" to 10), mapOf("ten" to "  "), mapOf("ten" to "Tưới cây"))
            )
        )!!
        // Muc thieu ten thi bo rieng muc do. Thieu so phut thi la 0 phut.
        assertEquals(listOf("Tưới cây"), d.cac.map { it.ten })
        assertEquals(0, d.cac.single().phut)
        assertEquals(0L, d.luc)
    }

    @Test
    fun so_phut_keo_ve_khoang_ma_tablet_nhan() {
        val d = dot(viec("Quét nhà", 9999, false), viec("Rửa chén", -5, false))
        assertEquals(listOf(ViecNha.PHUT_TOI_DA, 0), d.cac.map { it.phut })
    }

    @Test
    fun ghi_roi_doc_lai_ra_dung_cai_da_ghi() {
        val d = ViecNha.dotMoi(listOf(ViecNha.Viec("Quét nhà", 10)), Nguoi.BA_HUY, 5_000L)
            .xong("Quét nhà", 6_000L)
        val ban = ViecNha.banGhi(d)
        // Dung ten truong may ba va tablet dang doc.
        assertEquals(setOf("maPhien", "luc", "ai", "viec"), ban.keys)
        assertEquals(d, ViecNha.docDot(ban))
    }

    @Test
    fun dot_moi_co_ma_moi_va_chua_viec_nao_xong() {
        val cac = listOf(ViecNha.Viec("Quét nhà", 10), ViecNha.Viec("Rửa chén", 15))
        val a = ViecNha.dotMoi(cac, Nguoi.BA_HUY, 5_000L)
        val b = ViecNha.dotMoi(cac, Nguoi.BA_HUY, 5_000L)
        assertEquals(8, a.maPhien.length)
        assertNotEquals(a.maPhien, b.maPhien)
        assertEquals(Nguoi.BA_HUY, a.ai)
        assertEquals(5_000L, a.luc)
        assertTrue(a.cac.none { it.xong })
        assertEquals(25, a.tongPhut)
    }

    @Test
    fun bam_xong_chi_doi_dung_viec_do_va_moc_luc() {
        val d = dot(viec("Quét nhà", 10, false), viec("Rửa chén", 10, false), ai = "banoi")
        val moi = d.xong("Rửa chén", 9_000L)
        assertEquals(listOf(false, true), moi.cac.map { it.xong })
        assertEquals(9_000L, moi.luc)
        // Nguoi giao van la ba du Ba Huy la nguoi bam xong.
        assertEquals(Nguoi.BA_NOI, moi.ai)
        assertEquals(d.maPhien, moi.maPhien)
        assertTrue(moi.xong("Quét nhà", 9_500L).xongHet)
    }

    @Test
    fun bo_mot_viec_bo_het_va_gui_lai() {
        val d = dot(viec("Quét nhà", 10, false), viec("Rửa chén", 10, true))
        assertEquals(listOf("Rửa chén"), d.bo("Quét nhà", 2_000L).cac.map { it.ten })
        // Bo viec cuoi chua xong thi phan con lai la xong het: tablet se cong gio.
        assertTrue(d.bo("Quét nhà", 2_000L).xongHet)
        assertTrue(d.boHet(2_000L).cac.isEmpty())
        val guiLai = d.guiLai(3_000L)
        assertEquals(3_000L, guiLai.luc)
        assertEquals(d.cac, guiLai.cac)
    }

    @Test
    fun tablet_bo_qua_chi_khi_xong_het_va_qua_nua_tieng() {
        val bayGio = 10_000_000L
        val cu = bayGio - Duong.QUA_CU_MS - 1
        assertTrue(dot(viec("Quét nhà", 10, true), luc = cu).tabletDaBoQua(bayGio))
        // Con viec chua xong: tablet dang khoa hay chua nhan, khong phai chuyen bo qua.
        assertFalse(dot(viec("Quét nhà", 10, false), luc = cu).tabletDaBoQua(bayGio))
        // Moi xong: tablet con nhan duoc.
        assertFalse(dot(viec("Quét nhà", 10, true), luc = bayGio - 60_000L).tabletDaBoQua(bayGio))
        // Khong co moc luc thi khong ket luan gi.
        assertFalse(dot(viec("Quét nhà", 10, true), luc = 0L).tabletDaBoQua(bayGio))
    }

    @Test
    fun doc_danh_sach_chung() {
        val ds = ViecNha.docDanhSach(
            listOf(mapOf("ten" to " Quét nhà ", "phut" to 10), mapOf("phut" to 5), "rac", mapOf("ten" to "Tắm"))
        )
        assertEquals(listOf(ViecNha.Viec("Quét nhà", 10), ViecNha.Viec("Tắm", 0)), ds)
        assertTrue(ViecNha.docDanhSach(null).isEmpty())
        assertEquals(
            listOf(mapOf("ten" to "Quét nhà", "phut" to 10)),
            ViecNha.banDanhSach(listOf(ViecNha.Viec("Quét nhà", 10)))
        )
    }

    @Test
    fun kiem_danh_sach_dung_thi_ra_ban_sach() {
        val kq = ViecNha.kiem(listOf(" Quét nhà " to "10", "" to "", "Rửa chén" to " 15 "))
        assertTrue(kq.dung)
        assertEquals(listOf(ViecNha.Viec("Quét nhà", 10), ViecNha.Viec("Rửa chén", 15)), kq.cac)
        // Dong trong ca hai o la dong vua bam Them ma chua go gi: bo qua, khong bao loi.
        assertEquals(listOf(null, null, null), kq.loi)
    }

    @Test
    fun kiem_danh_sach_bao_loi_dung_dong() {
        val kq = ViecNha.kiem(
            listOf(
                "Quét nhà" to "10",
                "quét nhà" to "10",
                "" to "10",
                "Rửa chén" to "mười",
                "Tắm" to "500"
            )
        )
        assertFalse(kq.dung)
        assertNull(kq.loi[0])
        // Hai viec cung ten thi bam Xong mot cai la xong ca hai.
        assertNotNull(kq.loi[1])
        assertNotNull(kq.loi[2])
        assertNotNull(kq.loi[3])
        assertNotNull(kq.loi[4])
    }

    @Test
    fun kiem_danh_sach_trong_hay_dai_qua() {
        val trong = ViecNha.kiem(listOf("" to ""))
        assertFalse(trong.dung)
        assertNotNull(trong.loiChung)

        val dai = ViecNha.kiem((1..Duong.TOI_DA_VIEC + 1).map { "Việc $it" to "10" })
        assertFalse(dai.dung)
        assertNotNull(dai.loiChung)

        val vuaDu = ViecNha.kiem((1..Duong.TOI_DA_VIEC).map { "Việc $it" to "10" })
        assertTrue(vuaDu.dung)
    }
}
