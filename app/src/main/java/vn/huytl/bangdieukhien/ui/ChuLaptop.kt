package vn.huytl.bangdieukhien.ui

import vn.huytl.bangdieukhien.data.Duong
import vn.huytl.bangdieukhien.data.KetQuaLenhLaptop
import vn.huytl.bangdieukhien.data.LenhLaptop
import vn.huytl.bangdieukhien.data.LenhLaptopCho
import vn.huytl.bangdieukhien.data.PhieuLaptopCho
import vn.huytl.bangdieukhien.data.SuKienLaptop
import vn.huytl.bangdieukhien.data.SuKienTrenLaptop
import vn.huytl.bangdieukhien.data.TinhTrangLaptop
import java.util.Calendar
import kotlin.math.abs

/**
 * Chu cua the Laptop (8/10/2026), tach khoi [TheLaptop] de thu bang test JVM (ChuLaptopTest):
 * khong dung view hay Context, moi moc gio truyen vao.
 */
object ChuLaptop {

    /**
     * Gui [LenhLaptop.HOI] xong cho bao lau thi coi la laptop khong tra loi. Tu 9/10/2026 laptop
     * nghe lenh qua mot ket noi Firestore, tra loi trong vai giay; chi khi khong mo duoc ket noi do
     * no moi hoi lenh mot phut mot lan nhu truoc, cong thoi gian ghi len may chu. 150 giay la hon
     * hai vong hoi do mot chut.
     */
    const val CHO_TRA_LOI_MS = 150_000L

    /** Ket qua lenh, lenh bi bo, hien bao lau sau khi xay ra. Cu hon thi khong con lien quan. */
    const val HIEN_KET_QUA_MS = 30 * 60_000L

    /** Mau nhan trang thai: mau cua Le Hoa khi xem Netflix, mau cua Ba Huy khi Admin dang dung. */
    enum class Mau { NETFLIX, ADMIN, XAM }

    /**
     * Trang thai cua the (9/10/2026 bay giong the tablet, anh Huy chon "mau Moi"): [nhan] la nhan
     * nho goc tren, [chu] la dong ngay duoi. [choBam] false thi cac nut lenh mo di va bam chi bao
     * ly do.
     */
    data class TrangThaiHien(val nhan: String, val chu: String, val mau: Mau, val choBam: Boolean)

    /**
     * Mot dong o cuoi the: [cho] lenh, phieu dang cho (mau nhat, co nut "Rút lại": [lenhId] hay
     * [phieuId] la cai se xoa), ket qua lenh (xanh, [loi] thi do).
     */
    data class DongLenh(
        val chu: String, val loi: Boolean, val cho: Boolean = false,
        val lenhId: String? = null, val phieuId: String? = null
    )

    /** Ten hien o man dang nhap laptop (anh Huy doi 8/10/2026). */
    fun tenHien(ten: String): String = when (ten) {
        "lehoa" -> "Netflix"
        "huy" -> "Admin"
        else -> ten
    }

    fun tenLenh(kieu: String): String = when (kieu) {
        LenhLaptop.NHAN -> "Thông báo"
        LenhLaptop.CHUP -> "Chụp màn hình"
        LenhLaptop.DANG_XUAT -> "Đăng xuất"
        LenhLaptop.TAT_MAY -> "Tắt máy"
        else -> kieu
    }

    /**
     * Laptop dang the nao. [hoiId], [hoiLuc] la lenh HOI may nay gui luc mo tab: laptop chi ghi khi
     * co gi doi, nen phai hoi thi moi biet no con song.
     */
    fun trangThai(l: TinhTrangLaptop, hoiId: String?, hoiLuc: Long, bayGio: Long): TrangThaiHien {
        if (l.daTat()) return TrangThaiHien("Laptop đã tắt", "Tắt lúc ${luc(l.tatLuc, bayGio)}", Mau.XAM, false)
        val daTraLoi = hoiLuc > 0L && (l.capNhatLuc >= hoiLuc || l.ketQua.any { it.id == hoiId })
        if (!daTraLoi) {
            if (hoiLuc > 0L && bayGio - hoiLuc > CHO_TRA_LOI_MS) {
                val tu = if (l.capNhatLuc > 0L) " từ ${luc(l.capNhatLuc, bayGio)}" else ""
                return TrangThaiHien("Không trả lời", "Không thấy laptop trả lời$tu", Mau.XAM, false)
            }
            return TrangThaiHien("Đang hỏi", "Đang hỏi laptop…", Mau.XAM, true)
        }
        val tu = if (l.phienTu > 0L) " từ ${luc(l.phienTu, bayGio)}" else ""
        return when (l.phien) {
            "" -> TrangThaiHien("Ở màn đăng nhập", "Chưa ai đăng nhập", Mau.XAM, true)
            // Xem bang tai khoan Admin thi khong bi tru phut (sang 8/10/2026 xem mot tieng nhu vay).
            "huy" -> TrangThaiHien("Admin đang dùng", "Tài khoản Admin$tu, không tính phút", Mau.ADMIN, true)
            "lehoa" -> TrangThaiHien("Đang xem Netflix", "Tài khoản Netflix$tu", Mau.NETFLIX, true)
            else -> TrangThaiHien("Đang dùng", "Tài khoản ${tenHien(l.phien)}$tu", Mau.XAM, true)
        }
    }

    /**
     * Phut Netflix con lai cho dong ho to cua the, cung kieu dong ho the tablet: "38:12" (dang xem thi
     * dem lui tung giay), "45:00", het phut thi "00:00".
     */
    fun soNetflix(l: TinhTrangLaptop, bayGio: Long): String = Dinh.dongHo(l.conLai(bayGio))

    /**
     * Mot lan dung laptop trong ngay: tai khoan [ai] tu [tu] toi [den] (ms). [dangDung] la phien con
     * dang chay, [den] khi do la luc ve.
     */
    data class LanDung(val ai: String, val tu: Long, val den: Long, val dangDung: Boolean = false) {
        val dai: Long get() = (den - tu).coerceAtLeast(0L)
    }

    /**
     * Cac lan dung laptop trong ngay [dauNgay, cuoiNgay) tu so ngay cua laptop (laptop/{maNha}/ngay,
     * 9/10/2026), cho the "Thời gian dùng laptop" o tab Nhat ky. Tinh tu luc dang nhap toi luc thoat
     * (anh Huy chot): vao mo mot lan; ra, tat, tat dot ngot dong lan dang mo; vao tai khoan khac
     * cung dong lan truoc. Dong ra ma chua thay vao (phien chay tu hom truoc) thi tinh tu dau ngay.
     * Con lan dang mo luc het so: hom nay thi toi [bayGio] neu laptop con bao [phienHienTai] la
     * nguoi do, ngay cu thi toi het ngay.
     */
    fun cacLanDung(
        suKien: List<SuKienTrenLaptop>, dauNgay: Long, cuoiNgay: Long, bayGio: Long, phienHienTai: String
    ): List<LanDung> {
        val ra = mutableListOf<LanDung>()
        var dang: Pair<String, Long>? = null
        for (s in suKien.filter { it.luc in dauNgay until cuoiNgay }.sortedBy { it.luc }) {
            when (s.kieu) {
                SuKienLaptop.VAO -> {
                    val d = dang
                    if (d != null && d.first == s.ai) continue
                    if (d != null) ra += LanDung(d.first, d.second, s.luc)
                    dang = s.ai to s.luc
                }
                SuKienLaptop.RA, SuKienLaptop.TAT, SuKienLaptop.MAT -> {
                    val d = dang
                    if (d != null) {
                        ra += LanDung(d.first, d.second, s.luc)
                        dang = null
                    } else if (s.kieu == SuKienLaptop.RA && ra.isEmpty() && s.ai.isNotEmpty()) {
                        ra += LanDung(s.ai, dauNgay, s.luc)
                    }
                }
            }
        }
        dang?.let { (ai, tu) ->
            ra += if (bayGio < cuoiNgay) LanDung(ai, tu, maxOf(bayGio, tu), dangDung = phienHienTai == ai)
            else LanDung(ai, tu, cuoiNgay)
        }
        return ra
    }

    /** "Netflix 26 phút · Admin 1 tiếng 35 phút": Netflix truoc, roi Admin, roi tai khoan khac. */
    fun tongLanDung(cac: List<LanDung>): String {
        val theoAi = cac.groupBy { it.ai }.mapValues { (_, l) -> l.sumOf { it.dai } }
        val thuTu = listOf("lehoa", "huy") + theoAi.keys.filter { it != "lehoa" && it != "huy" }.sorted()
        return thuTu.filter { it in theoAi }.joinToString(" · ") { "${tenHien(it)} ${Dinh.doDai(theoAi.getValue(it))}" }
    }

    /** "09:36–10:06", lan dang chay thi "16:19 – nay". */
    fun khoangGio(l: LanDung): String =
        if (l.dangDung) "${Dinh.gioPhut(l.tu)} – nay" else "${Dinh.gioPhut(l.tu)}–${Dinh.gioPhut(l.den)}"

    /**
     * Cac dong cuoi the: lenh dang cho laptop nhan (tru HOI), ket qua lenh moi nhat trong
     * [HIEN_KET_QUA_MS], cac lenh may nay da bo vi qua 5 phut ([bo]: lenh kem luc bo), va phieu cap,
     * bot phut Netflix laptop chua nhan ([phieu], 9/10/2026).
     */
    fun dongLenh(
        cho: List<LenhLaptopCho>, ketQua: List<KetQuaLenhLaptop>, bo: List<Pair<LenhLaptopCho, Long>>,
        bayGio: Long, phieu: List<PhieuLaptopCho> = emptyList()
    ): List<DongLenh> {
        val ra = mutableListOf<DongLenh>()
        val gan = ketQua.filter { it.kieu != LenhLaptop.HOI && bayGio - it.luc <= HIEN_KET_QUA_MS }
            .maxByOrNull { it.luc }
        val boGan = bo.filter { bayGio - it.second <= HIEN_KET_QUA_MS }
        // Viec cu truoc, viec moi sau: ket qua, lenh bi bo, roi lenh dang cho.
        val cu = mutableListOf<Pair<Long, DongLenh>>()
        if (gan != null && gan.chu.isNotBlank()) cu += gan.luc to DongLenh(gan.chu, !gan.ok)
        for ((l, luc) in boGan) {
            cu += luc to DongLenh("Laptop chưa nhận lệnh ${tenLenh(l.kieu)} trong 5 phút, đã bỏ.", true)
        }
        ra += cu.sortedBy { it.first }.map { it.second }
        // Lenh va phieu dang cho, theo thu tu gui. Phieu tu hom truoc thi laptop bo luc bat lai (phut
        // Netflix chi dung trong ngay), nen khong hien.
        val dangCho = mutableListOf<Pair<Long, DongLenh>>()
        for (l in cho.filter { it.kieu != LenhLaptop.HOI }) {
            dangCho += l.tao to DongLenh(
                "Đang chờ laptop nhận: ${tenLenh(l.kieu)} (gửi ${Dinh.gioPhut(l.tao)}).", false, cho = true,
                lenhId = l.id
            )
        }
        val dau = dauNgay(bayGio)
        for (p in phieu.filter { it.tao >= dau && it.phut != 0 }) {
            dangCho += p.tao to DongLenh(chuPhieu(p), false, cho = true, phieuId = p.id)
        }
        ra += dangCho.sortedBy { it.first }.map { it.second }
        return ra
    }

    /**
     * "Thêm 20 phút lúc 14:05 (Chờ)", "Bớt 15 phút lúc 14:05 (Chờ)" (chu anh Huy chon 9/10/2026, luc
     * dau la "Chờ nhận thêm 20 phút (gửi 14:05)"). Gio la luc gui phieu.
     */
    fun chuPhieu(p: PhieuLaptopCho): String =
        "${if (p.phut < 0) "Bớt" else "Thêm"} ${Dinh.phut(abs(p.phut))} lúc ${Dinh.gioPhut(p.tao)} (Chờ)"

    /** Lenh cho qua [Duong.LENH_LAPTOP_HET_HAN_MS] thi may nay xoa (anh Huy chot 8/10/2026). */
    fun quaHan(l: LenhLaptopCho, bayGio: Long): Boolean = bayGio - l.tao > Duong.LENH_LAPTOP_HET_HAN_MS

    /** "11:53", them ngay khi khong phai hom nay: "11:53 ngày 7/10". */
    fun luc(ms: Long, bayGio: Long): String {
        if (ms >= dauNgay(bayGio)) return Dinh.gioPhut(ms)
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        return "${Dinh.gioPhut(ms)} ngày ${c.get(Calendar.DAY_OF_MONTH)}/${c.get(Calendar.MONTH) + 1}"
    }

    private fun dauNgay(bayGio: Long): Long = Calendar.getInstance().apply {
        timeInMillis = bayGio
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
