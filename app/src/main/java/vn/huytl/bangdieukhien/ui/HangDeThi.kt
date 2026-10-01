package vn.huytl.bangdieukhien.ui

import vn.huytl.bangdieukhien.data.DeThiTT
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Cac hang "Đề thi thử <môn>" o tab Gio choi (1/10/2026): xep hang theo mon, tieu de, dong tom
 * tat, chu tinh trang tung de trong danh sach "Xem đề", va cau hoi lai truoc khi mo mot de.
 *
 * VI SAO MOI MON MOT HANG. Truoc ngay do tablet chi co de Tieng Anh, va man Bang co dung mot
 * hang "Đề thi thử Tiếng Anh" ve san trong layout. Tu 1/10/2026 tablet them de Toan, KHTN, va Ba
 * Huy chot moi mon toi da mot de dang mo, de sau cua mon nao mo tu hom sau ngay nop de truoc cua
 * chinh mon do. Gop ca ba mon vao mot hang thi "Đã làm 3/20 đề" khong noi duoc mon nao dang mo,
 * mon nao con cho lop hoc toi dau.
 *
 * May nay khong tu tinh luat mo de: de Toan, KHTN mo theo moc "Lớp đã học tới" cua tung phan, va
 * moc do chi tablet biet. Tablet viet san chu pham vi va phan con thieu ([DeThiTT.phamVi],
 * [DeThiTT.thieu]), o day chi ghep chu.
 *
 * Tach khoi BangFragment de test duoc thu tu va cau chu ma khong can man hinh, y nhu [GioRieng].
 */
object HangDeThi {

    private const val TOAN = "Toán"
    private const val KHTN = "Khoa học tự nhiên"

    /** Thu tu hang tren man Bang. Mon la (tablet ban moi hon them mon) xep sau, theo thu tu gap. */
    private val THU_TU = listOf(TOAN, KHTN, DeThiTT.MON_ANH)

    private val VN = Locale.forLanguageTag("vi-VN")

    /** Mot hang: ten mon nhu tablet gui, va cac de cua mon do theo thu tu tablet gui. */
    data class Mon(val mon: String, val cac: List<DeThiTT>)

    /**
     * Gom de theo mon, moi mon mot hang: Toan, KHTN, Tieng Anh, roi toi mon la. Mon khong co de
     * nao thi khong co hang. Trong mot hang, de giu thu tu tablet gui (thu tu trong bo de).
     */
    fun theoMon(ds: List<DeThiTT>): List<Mon> =
        ds.groupBy { it.mon }
            .map { (mon, cac) -> Mon(mon, cac) }
            .sortedBy { THU_TU.indexOf(it.mon).let { i -> if (i < 0) THU_TU.size else i } }

    /**
     * "Đề thi thử Toán", "Đề thi thử KHTN", "Đề thi thử Tiếng Anh". KHTN viet tat vi ten day du
     * dai qua cho dong chu nho canh nut. Tablet khong ghi mon thi chi con "Đề thi thử".
     */
    fun tieuDe(mon: String): String = when (mon) {
        "" -> "Đề thi thử"
        KHTN -> "Đề thi thử KHTN"
        else -> "Đề thi thử $mon"
    }

    /**
     * Dong chu duoi tieu de hang: "Đề giữa kì 1 số 2 đang làm · Đã làm 1/4 đề", giong hang Tieng
     * Anh truoc 1/10/2026.
     *
     * Chua de nao mo duoc va chua lam de nao (moi de deu KHOA) thi "Đã làm 0/4 đề" khong noi
     * duoc gi, nen dong nay noi de hep nhat se mo khi nao, bang dung chu pham vi tablet gui:
     * "Mở khi lớp học Đại số tới Bài 9, Hình học tới Bài 14", giong dong de khoa o trang Luyen
     * tap tren tablet. Hep nhat theo [DeThiTT.doRong] tablet tinh; khong biet (-1) thi xep sau,
     * bang nhau thi lay de dung truoc trong bo de. Phan con thieu cu the nam o danh sach "Xem
     * đề": may nay khong tach chu [DeThiTT.thieu] ra, de tablet doi cach viet cung khong hong.
     */
    fun tomTat(cac: List<DeThiTT>): String {
        val mo = cac.firstOrNull { it.tt == DeThiTT.MO || it.tt == DeThiTT.DANG }
        val daLam = cac.count { it.daNop }
        if (mo == null && daLam == 0 && cac.none { it.tt == DeThiTT.SAN || it.tt == DeThiTT.XONG }) {
            cac.filter { it.tt == DeThiTT.KHOA }
                .minByOrNull { if (it.doRong >= 0) it.doRong else Int.MAX_VALUE }
                ?.phamVi?.takeIf { it.isNotBlank() }
                ?.let { return "Mở khi lớp học $it" }
        }
        return listOfNotNull(
            mo?.let { "${it.ten} " + if (it.tt == DeThiTT.DANG) "đang làm" else "đang mở" },
            "Đã làm $daLam/${cac.size} đề"
        ).joinToString(" · ")
    }

    /** Mot dong trong danh sach "Xem đề": "Đề giữa kì 1 số 2 · đang làm". */
    fun dong(d: DeThiTT, con: String): String = "${d.ten} · ${tinhTrang(d, con)}"

    /**
     * Tinh trang mot de, viet thuong vi dung sau ten de. [con] la ten con (R.string.child_name).
     *
     * De chua toi pham vi ghi nguyen chu phan thieu tablet gui, de Ba Huy thay ngay phai cho lop
     * hoc toi dau. Tablet ban cu khong gui phan thieu, luc do ghi pham vi trong ngoac. De da lam
     * ghi ngay nop va so sao lan gan nhat; thieu ngay (tablet ban cu) thi bo ngay.
     *
     * Tinh trang la (tablet ban moi hon them loai) thi hien nguyen ten loai, y nhu [Dinh.lenh],
     * con hon mot dong trong.
     */
    fun tinhTrang(d: DeThiTT, con: String): String = when (d.tt) {
        DeThiTT.KHOA -> when {
            d.thieu.isNotBlank() -> "chưa tới phạm vi: ${d.thieu}"
            d.phamVi.isNotBlank() -> "chưa tới phạm vi (${d.phamVi})"
            else -> "chưa tới phạm vi"
        }
        DeThiTT.SAN -> "mở được, chưa mở"
        DeThiTT.MO -> "đang mở, $con chưa bắt đầu"
        DeThiTT.DANG -> "đang làm"
        DeThiTT.XONG -> listOfNotNull(
            d.nopLuc.takeIf { it > 0L }?.let { ngay(it) },
            if (d.daNop) "${d.sao}/${d.toiDa} ★" else null
        ).joinToString(", ").let { if (it.isEmpty()) "đã làm" else "đã làm $it" }
        else -> d.tt.lowercase(VN).ifEmpty { "chưa rõ tình trạng" }
    }

    /**
     * Cau hoi lai truoc khi gui lenh mo [d]. null khi de dang mo: khong co gi de gui,
     * BangFragment chi noi la de dang mo.
     *
     * Tablet mo ca de lop chua hoc toi, vi Ba Huy chu dong bam. Nen voi de do cau hoi noi lop
     * con thieu phan nao, bang chu tablet viet: "Đề này chưa tới phạm vi (Hình học mới tới Bài
     * 12, đề cần Bài 14). Vẫn mở cho Lê Hòa?". Chu do da co moc de can, nen khong ghep them
     * [DeThiTT.phamVi]: ghep ca hai thi de Anh doc ra "cần học tới Unit 3 (..., đề cần Unit 3)"
     * (thu tren may ao ngay 1/10/2026). Tablet ban cu khong gui phan thieu thi noi theo pham vi.
     * De da lam thi nhac diem lan truoc. Hai cau sau giu nguyen nhu truoc 1/10/2026.
     */
    fun hoiLai(d: DeThiTT, con: String): String? = when (d.tt) {
        DeThiTT.MO, DeThiTT.DANG -> null
        DeThiTT.KHOA -> {
            val can = d.phamVi.takeIf { it.isNotBlank() }?.let { "Đề này cần ${canHoc(it)}" }
            val thieu = d.thieu.takeIf { it.isNotBlank() }
            when {
                thieu != null -> "Đề này chưa tới phạm vi ($thieu). Vẫn mở cho $con?"
                can != null -> "$can, lớp chưa học tới đó. Vẫn mở cho $con?"
                else -> "Lớp chưa học tới phạm vi của đề này. Vẫn mở cho $con?"
            }
        }
        DeThiTT.XONG -> "$con đã làm đề này" +
            (if (d.daNop) " (${d.sao}/${d.toiDa} ★)" else "") +
            ". Làm lại chỉ cộng phần sao hơn lần trước. Vẫn mở?"
        else -> "Mở cho $con làm ngay. Đề nằm ở trang Luyện tập, $con bấm Bắt đầu thì đồng hồ mới chạy."
    }

    /** "tới Unit 3" (de Tieng Anh, pham vi khong co ten phan) thanh "học tới Unit 3" cho xuoi cau. */
    private fun canHoc(phamVi: String): String =
        if (phamVi.startsWith("tới ")) "học $phamVi" else phamVi

    /** "28/09", theo gio may nay. */
    private fun ngay(luc: Long): String = SimpleDateFormat("dd/MM", VN).format(Date(luc))
}
