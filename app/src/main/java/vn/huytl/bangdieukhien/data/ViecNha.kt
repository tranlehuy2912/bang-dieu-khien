package vn.huytl.bangdieukhien.data

import kotlin.random.Random

/**
 * Viec nha: dot dang giao o hop/viecnha, va danh sach de chon o hop/danhsachviec.
 *
 * TRUOC DAY chi may ba noi giao viec, va may nay chi doc hop/viecnha de nhin mot
 * canh: ba bam xong het luc tablet dang tat. Bay gio Ba Huy giao va bam xong ngay tren
 * may nay, nen ca hai dien thoai cung ghi mot document. Moi lan bam la mot transaction
 * doc ban tren may chu roi moi sua, xem [Kho.giaoViec]; may ba lam y het, chung mot
 * khuon du lieu nhu duoi day.
 *
 * Doc tu Map chu khong tu DocumentSnapshot, de bai test chay tren may tinh khong can
 * Firebase.
 */
object ViecNha {

    /** Mot dau viec trong danh sach de chon. */
    data class Viec(val ten: String, val phut: Int)

    /** Mot dau viec dang giao, kem trang thai. */
    data class DangLam(val ten: String, val phut: Int, val xong: Boolean)

    /**
     * Mot dot viec dang giao.
     *
     * @param ai nguoi giao dot nay, [Nguoi.BA_NOI] hay [Nguoi.BA_HUY]
     * @param luc lan cuoi co nguoi bam, theo gio cua may bam. Khong phai luc giao.
     */
    data class Dot(val maPhien: String, val luc: Long, val ai: String, val cac: List<DangLam>) {

        /** Xong het ma document con do nghia la tablet chua nhan. */
        val xongHet: Boolean get() = cac.isNotEmpty() && cac.all { it.xong }
        val tongPhut: Int get() = cac.sumOf { it.phut }
        val ke: String get() = cac.joinToString(", ") { it.ten }

        /**
         * Tablet chac chan da bo qua dot nay.
         *
         * Tablet bo mot dot no chua tung thay ma luc da cu hon [Duong.QUA_CU_MS]. Xong
         * het ma van nam do lau hon the nghia la luc bam xong tablet dang tat, va no se
         * khong tu cong nua. Phai bam Gui lai de ghi lai moc luc.
         */
        fun tabletDaBoQua(bayGio: Long = System.currentTimeMillis()): Boolean =
            xongHet && luc > 0L && bayGio - luc > Duong.QUA_CU_MS

        fun xong(ten: String, bayGio: Long) =
            copy(luc = bayGio, cac = cac.map { if (it.ten == ten) it.copy(xong = true) else it })

        fun bo(ten: String, bayGio: Long) = copy(luc = bayGio, cac = cac.filterNot { it.ten == ten })

        /** Danh sach rong la cach noi "bo het" voi tablet. Tablet thay thi xoa document. */
        fun boHet(bayGio: Long) = copy(luc = bayGio, cac = emptyList())

        /** Chi doi moc luc, de tablet dang bo qua vi qua cu thi nhan lai. */
        fun guiLai(bayGio: Long) = copy(luc = bayGio)
    }

    /**
     * Danh sach khi hop/danhsachviec chua co. Giong het MAC_DINH ben may ba, nen luc
     * chua co danh sach chung thi hai may van hien cung mot danh sach.
     *
     * May nay khong tu ghi danh sach nay len, chi ghi khi Ba Huy bam Luu o hop sua
     * danh sach: khong co gi phai ghi khi hai may da giong nhau.
     */
    val MAC_DINH = listOf(
        Viec("Quét nhà lau nhà", 10),
        Viec("Rửa chén", 10),
        Viec("Tập thể dục", 10),
        Viec("Tắm rửa", 10)
    )

    /** So phut cua mot viec, ca hai dien thoai va tablet cung keo ve khoang nay. */
    const val PHUT_TOI_DA = 240

    // ------------------------------------------------------------- danh sach

    /** Doc truong [Duong.F_VIEC] cua hop/danhsachviec. Muc thieu ten thi bo. */
    fun docDanhSach(cac: List<*>?): List<Viec> =
        cac.orEmpty().filterIsInstance<Map<*, *>>().mapNotNull { o ->
            val ten = (o[Duong.F_TEN] as? String)?.trim()?.takeIf { it.isNotEmpty() }
                ?: return@mapNotNull null
            Viec(ten, docPhut(o[Duong.F_PHUT]))
        }

    fun banDanhSach(cac: List<Viec>): List<Map<String, Any>> =
        cac.map { mapOf(Duong.F_TEN to it.ten, Duong.F_PHUT to it.phut) }

    /** Ket qua kiem hop sua danh sach: loi tung dong (null la dong do dung), va ban sach. */
    data class KiemDanhSach(val loi: List<String?>, val cac: List<Viec>, val loiChung: String?) {
        val dung: Boolean get() = loiChung == null && loi.all { it == null }
    }

    /**
     * Kiem cac dong Ba Huy vua go: moi dong la (ten, so phut) dang chu.
     *
     * Dong trong ca hai o thi bo qua - la dong vua bam Them ma khong go gi. Ten trung
     * thi bao loi chu khong gop: tablet va hai may nhan viec theo ten, hai viec cung
     * ten thi bam Xong mot cai la xong ca hai.
     */
    fun kiem(dong: List<Pair<String, String>>): KiemDanhSach {
        val loi = MutableList<String?>(dong.size) { null }
        val cac = mutableListOf<Viec>()
        val daCo = mutableSetOf<String>()
        dong.forEachIndexed { i, (tenGo, phutGo) ->
            val ten = tenGo.trim()
            val phutChu = phutGo.trim()
            if (ten.isEmpty() && phutChu.isEmpty()) return@forEachIndexed
            val phut = phutChu.toIntOrNull()
            loi[i] = when {
                ten.isEmpty() -> "Chưa có tên việc."
                !daCo.add(ten.lowercase()) -> "Trùng tên với một việc ở trên."
                phut == null -> "Số phút phải là số."
                phut < 0 || phut > PHUT_TOI_DA -> "Số phút từ 0 đến $PHUT_TOI_DA."
                else -> null
            }
            if (loi[i] == null && phut != null) cac += Viec(ten, phut)
        }
        val loiChung = when {
            loi.any { it != null } -> null
            cac.isEmpty() -> "Phải còn ít nhất một việc."
            cac.size > Duong.TOI_DA_VIEC -> "Tối đa ${Duong.TOI_DA_VIEC} việc."
            else -> null
        }
        return KiemDanhSach(loi, cac, loiChung)
    }

    // ---------------------------------------------------------------- dot viec

    /**
     * Doc document hop/viecnha. null la khong co dot nao.
     *
     * Document con ma danh sach rong thi van tra ve mot dot: do la luc da bo het va
     * tablet chua kip xoa. Man hinh coi no nhu khong co gi, con giao dot moi thi duoc
     * ghi de len.
     */
    fun docDot(du: Map<String, Any?>?): Dot? {
        if (du == null) return null
        val ma = (du[Duong.F_MA_PHIEN] as? String).orEmpty()
        if (ma.isBlank()) return null
        val cac = (du[Duong.F_VIEC] as? List<*>).orEmpty().filterIsInstance<Map<*, *>>()
            .mapNotNull { o ->
                val ten = (o[Duong.F_TEN] as? String)?.trim()?.takeIf { it.isNotEmpty() }
                    ?: return@mapNotNull null
                DangLam(ten, docPhut(o[Duong.F_PHUT]), o[Duong.F_XONG] == true)
            }
        return Dot(
            maPhien = ma,
            luc = (du[Duong.F_LUC] as? Number)?.toLong() ?: 0L,
            // Thieu la may ba ban cu ghi: truoc do chi may ba giao viec.
            ai = du[Duong.F_AI] as? String ?: Nguoi.BA_NOI,
            cac = cac
        )
    }

    /** Ban ghi xuong hop/viecnha. Luon ghi ca ban, khong merge. */
    fun banGhi(d: Dot): Map<String, Any> = mapOf(
        Duong.F_MA_PHIEN to d.maPhien,
        Duong.F_LUC to d.luc,
        Duong.F_AI to d.ai,
        Duong.F_VIEC to d.cac.map {
            mapOf(Duong.F_TEN to it.ten, Duong.F_PHUT to it.phut, Duong.F_XONG to it.xong)
        }
    )

    /**
     * Mot dot moi, voi ma phien moi de tablet biet day khong phai dot cu.
     *
     * Tam chu so hex: tablet nho ma cua dot vua khep de khong cong gio hai lan, nen
     * mot dot moi trung ma dot cu se bi xoa ngay ma khong khoa may.
     */
    fun dotMoi(cac: List<Viec>, ai: String, bayGio: Long) = Dot(
        maPhien = "%08x".format(Random.nextInt()),
        luc = bayGio,
        ai = ai,
        cac = cac.map { DangLam(it.ten, it.phut, false) }
    )

    private fun docPhut(v: Any?): Int = ((v as? Number)?.toInt() ?: 0).coerceIn(0, PHUT_TOI_DA)
}
