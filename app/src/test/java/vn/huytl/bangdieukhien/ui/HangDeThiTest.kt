package vn.huytl.bangdieukhien.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import vn.huytl.bangdieukhien.data.DeThiTT
import java.util.Calendar

/**
 * Cac hang "Đề thi thử <môn>" o tab Gio choi (1/10/2026): thu tu hang theo mon, tieu de, dong
 * tom tat, chu tinh trang tung de trong danh sach "Xem đề", cau hoi lai truoc khi mo.
 */
class HangDeThiTest {

    private val con = "Lê Hòa"

    private fun de(
        ma: String,
        tt: String,
        mon: String = "Toán",
        phamVi: String = "",
        thieu: String = "",
        sao: Int = -1,
        toiDa: Int = -1,
        nopLuc: Long = 0L,
        doRong: Int = -1
    ) = DeThiTT(
        ma = ma, ten = "Đề $ma", tt = tt, mon = mon, phamVi = phamVi, thieu = thieu,
        sao = sao, toiDa = toiDa, nopLuc = nopLuc, doRong = doRong
    )

    @Test
    fun hang_xep_toan_khtn_tieng_anh_roi_toi_mon_la() {
        val ds = listOf(
            de("GK1-1", DeThiTT.SAN, mon = "Tiếng Anh"),
            de("TH-1", DeThiTT.SAN, mon = "Tin học"),
            de("KGK1-1", DeThiTT.KHOA, mon = "Khoa học tự nhiên"),
            de("TGK1-1", DeThiTT.XONG, mon = "Toán"),
            de("GK1-2", DeThiTT.KHOA, mon = "Tiếng Anh"),
            de("LS-1", DeThiTT.SAN, mon = "Lịch sử")
        )

        val hang = HangDeThi.theoMon(ds)

        // Mon la xep sau ba mon chinh, theo thu tu gap trong danh sach tablet gui.
        assertEquals(
            listOf("Toán", "Khoa học tự nhiên", "Tiếng Anh", "Tin học", "Lịch sử"),
            hang.map { it.mon }
        )
        // Trong mot hang, de giu thu tu tablet gui.
        assertEquals(listOf("GK1-1", "GK1-2"), hang[2].cac.map { it.ma })
    }

    @Test
    fun mon_khong_co_de_thi_khong_co_hang() {
        val hang = HangDeThi.theoMon(listOf(de("GK1-1", DeThiTT.SAN, mon = "Tiếng Anh")))
        assertEquals(listOf("Tiếng Anh"), hang.map { it.mon })
        assertEquals(emptyList<HangDeThi.Mon>(), HangDeThi.theoMon(emptyList()))
    }

    @Test
    fun tieu_de_hang_viet_tat_khtn() {
        assertEquals("Đề thi thử Toán", HangDeThi.tieuDe("Toán"))
        assertEquals("Đề thi thử KHTN", HangDeThi.tieuDe("Khoa học tự nhiên"))
        assertEquals("Đề thi thử Tiếng Anh", HangDeThi.tieuDe("Tiếng Anh"))
        assertEquals("Đề thi thử Tin học", HangDeThi.tieuDe("Tin học"))
        assertEquals("Đề thi thử", HangDeThi.tieuDe(""))
    }

    @Test
    fun tom_tat_de_dang_mo_va_so_de_da_lam() {
        val khoa = de("TCK1-1", DeThiTT.KHOA, phamVi = "Đại số tới Bài 18", thieu = "Đại số mới tới Bài 9, đề cần Bài 18")

        assertEquals(
            "Đề TGK1-2 đang làm · Đã làm 1/3 đề",
            HangDeThi.tomTat(listOf(de("TGK1-1", DeThiTT.XONG, sao = 30, toiDa = 40), de("TGK1-2", DeThiTT.DANG), khoa))
        )
        assertEquals("Đề TGK1-2 đang mở · Đã làm 0/2 đề", HangDeThi.tomTat(listOf(de("TGK1-2", DeThiTT.MO), khoa)))
        // Con de mo duoc, hay da lam mot de: van dem so de nhu truoc, khong noi "Mở khi".
        assertEquals("Đã làm 0/2 đề", HangDeThi.tomTat(listOf(de("TGK1-2", DeThiTT.SAN), khoa)))
        assertEquals(
            "Đã làm 1/2 đề",
            HangDeThi.tomTat(listOf(de("TGK1-1", DeThiTT.XONG, sao = 30, toiDa = 40), khoa))
        )
    }

    /** Moi de deu khoa, chua lam de nao: noi de hep nhat (theo doRong tablet gui) se mo khi nao. */
    @Test
    fun chua_mo_duoc_chua_lam_de_nao_thi_noi_de_hep_nhat_mo_khi_nao() {
        val ds = listOf(
            de(
                "TCK1-1", DeThiTT.KHOA, phamVi = "Đại số tới Bài 20, Hình học tới Bài 17",
                thieu = "Đại số mới tới Bài 9, đề cần Bài 20; Hình học mới tới Bài 12, đề cần Bài 17", doRong = 20
            ),
            de(
                "TGK1-2", DeThiTT.KHOA, phamVi = "Đại số tới Bài 9, Hình học tới Bài 14",
                thieu = "Hình học mới tới Bài 12, đề cần Bài 14", doRong = 14
            ),
            // Cung do rong voi de tren: lay de dung truoc trong bo de.
            de(
                "TGK1-3", DeThiTT.KHOA, phamVi = "Đại số tới Bài 9, Hình học tới Bài 13",
                thieu = "Đại số mới tới Bài 5, đề cần Bài 9", doRong = 14
            )
        )

        assertEquals("Mở khi lớp học Đại số tới Bài 9, Hình học tới Bài 14", HangDeThi.tomTat(ds))
    }

    /** Dong tom tat ghi nguyen chu pham vi, khong tach chu phan thieu (tablet doi cach viet cung khong hong). */
    @Test
    fun mo_khi_ghi_nguyen_chu_pham_vi_khong_biet_do_rong_thi_theo_thu_tu_gui() {
        val khtn = de(
            "KGK1-1", DeThiTT.KHOA, mon = "Khoa học tự nhiên", phamVi = "Hoá tới Bài 6, Sinh tới Bài 31",
            thieu = "Sinh chưa chọn bài, đề cần Bài 31"
        )
        assertEquals("Mở khi lớp học Hoá tới Bài 6, Sinh tới Bài 31", HangDeThi.tomTat(listOf(khtn)))

        val khongBiet = listOf(
            de("TGK1-1", DeThiTT.KHOA, phamVi = "Đại số tới Bài 9"),
            de("TGK1-2", DeThiTT.KHOA, phamVi = "Đại số tới Bài 5", doRong = 5)
        )
        assertEquals("de biet do rong dung truoc de khong biet", "Mở khi lớp học Đại số tới Bài 5", HangDeThi.tomTat(khongBiet))
        assertEquals(
            "Mở khi lớp học Đại số tới Bài 9",
            HangDeThi.tomTat(listOf(de("TGK1-1", DeThiTT.KHOA, phamVi = "Đại số tới Bài 9"), de("TGK1-3", DeThiTT.KHOA, phamVi = "Đại số tới Bài 2")))
        )
    }

    @Test
    fun tablet_ban_cu_khong_gui_phan_thieu_thi_noi_theo_pham_vi() {
        val ds = DeThiTT.doc(
            null,
            listOf(
                mapOf("ma" to "GK1-4", "ten" to "Đề giữa kì 1 số 4", "den" to 9L, "tt" to "KHOA"),
                mapOf("ma" to "GK1-1", "ten" to "Đề giữa kì 1 số 1", "den" to 3L, "tt" to "KHOA")
            )
        )!!
        assertEquals("Mở khi lớp học tới Unit 3", HangDeThi.tomTat(ds))

        // Khong co ca pham vi lan phan thieu: khong co gi de noi, dem so de nhu cu.
        assertEquals("Đã làm 0/1 đề", HangDeThi.tomTat(listOf(de("X-1", DeThiTT.KHOA))))
    }

    @Test
    fun chu_tinh_trang_tung_loai() {
        assertEquals(
            "Đề TGK1-2 · chưa tới phạm vi: Hình học mới tới Bài 12, đề cần Bài 14",
            HangDeThi.dong(de("TGK1-2", DeThiTT.KHOA, phamVi = "Đại số tới Bài 9, Hình học tới Bài 14", thieu = "Hình học mới tới Bài 12, đề cần Bài 14"), con)
        )
        assertEquals(
            "Đề GK1-1 · chưa tới phạm vi (tới Unit 3)",
            HangDeThi.dong(de("GK1-1", DeThiTT.KHOA, phamVi = "tới Unit 3"), con)
        )
        assertEquals("Đề X · chưa tới phạm vi", HangDeThi.dong(de("X", DeThiTT.KHOA), con))
        assertEquals("Đề GK1-2 · mở được, chưa mở", HangDeThi.dong(de("GK1-2", DeThiTT.SAN), con))
        assertEquals("Đề GK1-3 · đang mở, Lê Hòa chưa bắt đầu", HangDeThi.dong(de("GK1-3", DeThiTT.MO), con))
        assertEquals("Đề GK1-4 · đang làm", HangDeThi.dong(de("GK1-4", DeThiTT.DANG), con))
        // Tinh trang la thi hien nguyen ten loai, khong de dong trong.
        assertEquals("Đề GK1-5 · het", HangDeThi.dong(de("GK1-5", "HET"), con))
    }

    @Test
    fun da_lam_ghi_ngay_nop_va_so_sao_thieu_ngay_thi_bo_ngay() {
        // Lay ms theo gio may chay test, de ngay in ra khong phu thuoc mui gio.
        val luc = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 28, 20, 15, 0) }.timeInMillis

        assertEquals("đã làm 28/09, 41/55 ★", HangDeThi.tinhTrang(de("GK1-1", DeThiTT.XONG, sao = 41, toiDa = 55, nopLuc = luc), con))
        assertEquals("đã làm 41/55 ★", HangDeThi.tinhTrang(de("GK1-1", DeThiTT.XONG, sao = 41, toiDa = 55), con))
        assertEquals("đã làm 28/09", HangDeThi.tinhTrang(de("GK1-1", DeThiTT.XONG, nopLuc = luc), con))
    }

    @Test
    fun hoi_lai_de_chua_toi_pham_vi_noi_ro_pham_vi_va_phan_thieu() {
        assertEquals(
            "Đề này chưa tới phạm vi (Hình học mới tới Bài 12, đề cần Bài 14). Vẫn mở cho Lê Hòa?",
            HangDeThi.hoiLai(
                de("TGK1-2", DeThiTT.KHOA, phamVi = "Đại số tới Bài 9, Hình học tới Bài 14", thieu = "Hình học mới tới Bài 12, đề cần Bài 14"),
                con
            )
        )
        // Tablet ban cu: chi co pham vi.
        assertEquals(
            "Đề này cần học tới Unit 3, lớp chưa học tới đó. Vẫn mở cho Lê Hòa?",
            HangDeThi.hoiLai(de("GK1-1", DeThiTT.KHOA, phamVi = "tới Unit 3"), con)
        )
    }

    @Test
    fun hoi_lai_de_dang_mo_thi_khong_hoi_de_da_lam_nhac_diem() {
        assertNull(HangDeThi.hoiLai(de("GK1-1", DeThiTT.MO), con))
        assertNull(HangDeThi.hoiLai(de("GK1-1", DeThiTT.DANG), con))
        assertEquals(
            "Lê Hòa đã làm đề này (41/55 ★). Làm lại chỉ cộng phần sao hơn lần trước. Vẫn mở?",
            HangDeThi.hoiLai(de("GK1-1", DeThiTT.XONG, sao = 41, toiDa = 55), con)
        )
        assertEquals(
            "Mở cho Lê Hòa làm ngay. Đề nằm ở trang Luyện tập, Lê Hòa bấm Bắt đầu thì đồng hồ mới chạy.",
            HangDeThi.hoiLai(de("GK1-2", DeThiTT.SAN), con)
        )
    }
}
