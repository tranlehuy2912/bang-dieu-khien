package vn.huytl.bangdieukhien.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Tach dong so hoi AI, dung chuoi NhatKyAi.ghi ben nop-bai ghi ra. */
class DongHoiAiTest {

    @Test
    fun cau_mot_dong_tach_ra_gio_app_cau() {
        assertEquals(
            DongHoiAi.Cau("17:36", "ChatGPT", "giải giúp em bài 2.26 trang 44 sgk toán 8"),
            DongHoiAi.tach("23/09 17:36  [ChatGPT]  giải giúp em bài 2.26 trang 44 sgk toán 8")
        )
    }

    /** Tablet tu 28/9/2026 ghi moi lan con xuong dong thanh " ↵ ". */
    @Test
    fun cau_nhieu_dong_tra_lai_cho_xuong_dong() {
        assertEquals(
            DongHoiAi.Cau(
                "11:25", "Gemini",
                "Cho tam giác ABC vuông tại A, đường cao AH.\n" +
                    "a) Chứng minh AH² = HB.HC.\n" +
                    "b) Tính AH."
            ),
            DongHoiAi.tach(
                "28/09 11:25  [Gemini]  Cho tam giác ABC vuông tại A, đường cao AH. ↵ " +
                    "a) Chứng minh AH² = HB.HC. ↵ b) Tính AH."
            )
        )
    }

    /** Cau ghi truoc 28/9/2026: tablet da thay cho xuong dong bang dau cach, hien lien. */
    @Test
    fun cau_cu_khong_co_dau_thi_giu_nguyen() {
        assertEquals(
            "past simple của go là gì cho em 3 câu ví dụ",
            DongHoiAi.tach("28/09 11:25  [Dola]  past simple của go là gì cho em 3 câu ví dụ")?.cau
        )
    }

    @Test
    fun dong_lac_khuon_van_tra_lai_cho_xuong_dong() {
        assertNull(DongHoiAi.tach("hỏi AI ↵ không có ngày giờ"))
        assertEquals("hỏi AI\nkhông có ngày giờ", DongHoiAi.traXuongDong("hỏi AI ↵ không có ngày giờ"))
    }
}
