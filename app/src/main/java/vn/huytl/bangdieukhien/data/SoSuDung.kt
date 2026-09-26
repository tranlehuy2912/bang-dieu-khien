package vn.huytl.bangdieukhien.data

import com.google.firebase.firestore.DocumentSnapshot
import java.util.Calendar

/**
 * So ghi Le Hoa dung app gi, tu may gio den may gio, doc tu hop/sudung. Xem
 * [Duong.D_SU_DUNG].
 *
 * Tablet giu so nay cho trang "Dùng app gì, lúc nào" ben do, va chi day sang day khi
 * may nay go [Lenh.PING]. Cach cat ngay, gop va cong o duoi chep dung NhatKySuDung
 * ben tablet, de cung mot ngay thi hai man ra cung mot con so.
 */
data class SoSuDung(
    /** Cac khoang, xep theo luc mo. Chia doi man hinh thi co khoang chong nhau. */
    val cac: List<Doan>,
    /** Ten doc duoc theo ten goi. */
    val ten: Map<String, String>,
    /** Tablet giu bao nhieu ngay, tinh ca hom nay. */
    val giuNgay: Int,
    /** Dich vu canh app tren tablet co chay luc day khong. Xem [Duong.F_DANG_GHI]. */
    val dangGhi: Boolean,
    /** Luc tablet day ban nay, theo gio tablet. */
    val capNhatLuc: Long
) {

    /** Mot khoang lien tuc mot app nam truoc mat. */
    data class Doan(val goi: String, val tu: Long, val den: Long) {
        val daiMs: Long get() = (den - tu).coerceAtLeast(0L)
    }

    /** Tong cua mot app trong mot ngay, kem tung khoang de biet luc nao. */
    data class MotApp(val goi: String, val ten: String, val tongMs: Long, val cacDoan: List<Doan>)

    /** Ten app, hoac chinh ten goi khi tablet khong gui ten, y nhu tablet hien. */
    fun tenApp(goi: String): String = ten[goi] ?: goi

    /**
     * Cac khoang nam trong ngay tu moc [dau] den moc [cuoi], xem [dauNgay].
     *
     * Khoang vat qua nua dem bi cat dung o moc, de tong cua moi ngay cong lai khong
     * vuot qua chinh no.
     */
    fun cuaNgay(dau: Long, cuoi: Long): List<Doan> =
        cac.filter { it.tu < cuoi && it.den > dau }
            .map { Doan(it.goi, maxOf(it.tu, dau), minOf(it.den, cuoi)) }
            .filter { it.den > it.tu }
            .sortedBy { it.tu }

    /** Tung app trong ngay, app dung lau nhat dung truoc. */
    fun theoApp(dau: Long, cuoi: Long): List<MotApp> =
        cuaNgay(dau, cuoi)
            .groupBy { it.goi }
            .map { (goi, cac) -> MotApp(goi, tenApp(goi), cac.sumOf { it.daiMs }, cac) }
            .sortedByDescending { it.tongMs }

    /**
     * Tong thoi gian may duoc dung trong ngay.
     *
     * Gop cac khoang chong nhau truoc khi cong: chia doi man hinh thi hai app cung nam
     * truoc mat, cong thang hai dong thi mot tieng dung may thanh hai tieng.
     */
    fun tongMs(dau: Long, cuoi: Long): Long {
        val gop = mutableListOf<Doan>()
        cuaNgay(dau, cuoi).forEach { d ->
            val truoc = gop.lastOrNull()
            if (truoc != null && d.tu <= truoc.den) {
                gop[gop.lastIndex] = truoc.copy(den = maxOf(truoc.den, d.den))
            } else {
                gop.add(d)
            }
        }
        return gop.sumOf { it.daiMs }
    }

    /** Luc cam may lan dau va luc buong ra lan cuoi trong ngay, hoac null. */
    fun tuDen(dau: Long, cuoi: Long): Pair<Long, Long>? {
        val ngay = cuaNgay(dau, cuoi)
        if (ngay.isEmpty()) return null
        return ngay.first().tu to ngay.maxOf { it.den }
    }

    companion object {

        /** Tablet ban nay giu bay nhieu ngay. Dung khi document thieu truong. */
        const val GIU_NGAY_MAC_DINH = 7

        fun doc(d: DocumentSnapshot?): SoSuDung? =
            if (d == null || !d.exists()) null else doc(d.data)

        /**
         * Doc tu map chu khong tu DocumentSnapshot, de test chay duoc ngoai may.
         *
         * So doc ve la Number chu khong ep Long: Firestore tra Long cho so nguyen, nhung
         * ai sua tay trong console la co the thanh Double. Khoang nao thieu truong thi
         * bo, khong bo ca so.
         */
        fun doc(m: Map<String, Any?>?): SoSuDung? {
            if (m == null) return null
            val cac = (m[Duong.F_DOAN] as? List<*>).orEmpty()
                .filterIsInstance<Map<*, *>>()
                .mapNotNull { o ->
                    val goi = (o[Duong.F_GOI] as? String)?.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null
                    val tu = (o[Duong.F_TU] as? Number)?.toLong() ?: return@mapNotNull null
                    val den = (o[Duong.F_DEN] as? Number)?.toLong() ?: return@mapNotNull null
                    if (den <= tu) null else Doan(goi, tu, den)
                }
                .sortedBy { it.tu }
            val ten = (m[Duong.F_APP] as? List<*>).orEmpty()
                .filterIsInstance<Map<*, *>>()
                .mapNotNull { o ->
                    val goi = o[Duong.F_GOI] as? String ?: return@mapNotNull null
                    val ten = (o[Duong.F_TEN] as? String)?.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null
                    goi to ten
                }
                .toMap()
            return SoSuDung(
                cac = cac,
                ten = ten,
                giuNgay = (m[Duong.F_GIU_NGAY] as? Number)?.toInt()?.takeIf { it > 0 }
                    ?: GIU_NGAY_MAC_DINH,
                dangGhi = m[Duong.F_DANG_GHI] as? Boolean ?: true,
                capNhatLuc = (m[Duong.F_CAP_NHAT_LUC] as? Number)?.toLong() ?: 0L
            )
        }

        /**
         * Nua dem dau ngay, [lui] ngay truoc hom nay, theo gio may nay. Lui = -1 la nua
         * dem dem nay, tuc la moc cuoi cua hom nay.
         *
         * Gio may nay chu khong phai gio tablet: hai may cung o Viet Nam, cung mui gio,
         * va moi dong gio khac tren man hinh nay cung doc theo may nay.
         */
        fun dauNgay(lui: Int, bayGio: Long = System.currentTimeMillis()): Long =
            Calendar.getInstance().apply {
                timeInMillis = bayGio
                add(Calendar.DAY_OF_MONTH, -lui)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
    }
}
