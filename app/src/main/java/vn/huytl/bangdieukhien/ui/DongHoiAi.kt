package vn.huytl.bangdieukhien.ui

/**
 * Mot dong trong so hoi AI tablet day sang (hoiai/{ngay}, truong dong), tach ra de ve
 * the "Hoi AI" o tab Nhat ky.
 *
 * Tablet ghi moi cau thanh mot dong "23/09 17:36  [ChatGPT]  cau hoi", xem
 * NhatKyAi.ghi ben nop-bai. Khuon o day khop tay voi ben do. Doi khuon ben do ma quen
 * doi o day thi khong hong gi: moi dong chi hien nguyen ban, xau hon mot chut.
 *
 * Tach khoi NhatKyFragment de test duoc bang dung chuoi tablet ghi, khong can man hinh.
 */
object DongHoiAi {

    /** Mot cau da tach. [cau] da tra lai cac cho con xuong dong. */
    data class Cau(val gio: String, val app: String, val cau: String)

    /**
     * Ngay, gio, [ten app], cau. Hai dau cach giua cac phan la dung nhu tablet ghi.
     *
     * Dau cham o nhom cuoi khong khop U+2028, U+2029, U+0085. Tablet tu 28/9/2026 doi ca
     * ba thanh [XUONG_DONG] truoc khi ghi, xem NhatKyAi.motDong ben do.
     */
    private val KHUON = Regex("""^\d{2}/\d{2} (\d{2}:\d{2}) {2}\[(.*?)\] {2}(.*)$""")

    /**
     * Dau tablet ghi thay cho moi lan con xuong dong trong cau (NhatKyAi.XUONG_DONG ben
     * nop-bai, " ↵ "). Nhan ca khi thieu dau cach hai ben.
     *
     * Truoc 28/9/2026 tablet thay cho xuong dong bang dau cach, nen cau cu khong co dau
     * nay va van hien thanh mot doan lien: chu da mat tu phia tablet, o day khong lam gi
     * duoc.
     */
    private val XUONG_DONG = Regex(""" ?↵ ?""")

    /** null la dong lac khuon: the Hoi AI hien nguyen dong, qua [traXuongDong]. */
    fun tach(dong: String): Cau? {
        val m = KHUON.matchEntire(dong) ?: return null
        val (gio, app, cau) = m.destructured
        return Cau(gio, app, traXuongDong(cau))
    }

    /** Doi [XUONG_DONG] ve lai xuong dong that. */
    fun traXuongDong(chu: String): String = chu.replace(XUONG_DONG, "\n")
}
