package vn.huytl.bangdieukhien.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import vn.huytl.bangdieukhien.data.AppTrenMay
import vn.huytl.bangdieukhien.data.CaiDat

/** Hop "Giờ riêng từng app" o tab Cai dat: thu tu, dong nao hien, cau chu. */
class GioRiengTest {

    private val chrome = AppTrenMay("com.android.chrome", "Chrome")
    private val netflix = AppTrenMay("com.netflix.mediaclient", "Netflix")
    private val nopBai = AppTrenMay("vn.huytl.homeworkgate", "Nộp bài")
    private val youtube = AppTrenMay("com.google.android.youtube", "YouTube")

    /** Tablet gui danh sach da xep theo ten. */
    private val ds = listOf(chrome, netflix, nopBai, youtube)

    @Test
    fun app_da_dat_gio_len_dau_con_lai_giu_thu_tu_tablet_gui() {
        val han = mapOf(youtube.goi to 30, netflix.goi to 90)
        assertEquals(
            listOf(netflix.goi, youtube.goi, chrome.goi),
            GioRieng.thuTu(ds, han)
        )
    }

    /** Dich vu canh app khong bao gio chan Nop bai, dat gio cho no khong khoa duoc gi. */
    @Test
    fun khong_hien_chinh_app_nop_bai() {
        val han = mapOf(nopBai.goi to 30)
        val thuTu = GioRieng.thuTu(ds, han)
        assertEquals(listOf(chrome.goi, netflix.goi, youtube.goi), thuTu)
        assertEquals(3, GioRieng.cacDong(thuTu, ds, han).size)
    }

    /** Hop dang mo ma so phut doi: cac dong dung yen, chi so phut doi. */
    @Test
    fun thu_tu_chot_luc_mo_hop() {
        val thuTu = GioRieng.thuTu(ds, emptyMap())

        val dong = GioRieng.cacDong(thuTu, ds, mapOf(youtube.goi to 45))

        assertEquals(listOf("Chrome", "Netflix", "YouTube"), dong.map { it.ten })
        assertEquals(listOf(0, 0, 45), dong.map { it.phut })
    }

    /** App da go khoi tablet ma con gio rieng: van hien bang ten goi de bo gio duoc. */
    @Test
    fun app_da_go_con_gio_rieng_van_hien_bang_ten_goi() {
        val han = mapOf("com.app.dago" to 60)
        val thuTu = GioRieng.thuTu(listOf(chrome), han)

        val dong = GioRieng.cacDong(thuTu, listOf(chrome), han)

        assertEquals(listOf(GioRieng.Dong("com.app.dago", "com.app.dago", 60),
            GioRieng.Dong(chrome.goi, "Chrome", 0)), dong)
        // Bo gio xong, tablet ghi lai: app do khong con gi de hien.
        assertEquals(listOf("Chrome"), GioRieng.cacDong(thuTu, listOf(chrome), emptyMap()).map { it.ten })
    }

    /** Ba Huy dat gio ngay tren tablet trong luc hop tren dien thoai dang mo. */
    @Test
    fun app_moi_co_gio_trong_luc_hop_mo_noi_vao_cuoi() {
        val thuTu = GioRieng.thuTu(listOf(chrome), emptyMap())

        val dong = GioRieng.cacDong(thuTu, listOf(chrome), mapOf("com.app.moi" to 15))

        assertEquals(listOf(chrome.goi, "com.app.moi"), dong.map { it.goi })
    }

    @Test
    fun dong_phu_noi_ro_het_gio_choi_co_mo_duoc_khong() {
        val c = CaiDat(appChoPhep = listOf(netflix.goi), appMoiLuc = listOf(youtube.goi))
        assertEquals("Mở được mọi lúc", GioRieng.khiHetGio(youtube.goi, c))
        assertEquals("Mở được khi hết giờ chơi, trừ giờ ngủ, giờ học", GioRieng.khiHetGio(netflix.goi, c))
        assertEquals("Chỉ mở trong giờ chơi", GioRieng.khiHetGio(chrome.goi, c))
    }

    @Test
    fun cac_muc_giong_tablet_va_giu_so_le_dang_dat() {
        assertEquals(listOf(0, 15, 30, 45, 60, 90, 120, 180), GioRieng.cacMuc(0))
        assertEquals(listOf(0, 15, 30, 45, 60, 90, 120, 180), GioRieng.cacMuc(45))
        assertEquals(listOf(0, 15, 20, 30, 45, 60, 90, 120, 180), GioRieng.cacMuc(20))
    }

    @Test
    fun ten_muc() {
        assertEquals("Không giới hạn", GioRieng.tenMuc(0))
        assertEquals("45 phút mỗi ngày", GioRieng.tenMuc(45))
        assertEquals("1 tiếng 30 phút mỗi ngày", GioRieng.tenMuc(90))
        assertEquals("3 tiếng mỗi ngày", GioRieng.tenMuc(180))
    }
}
