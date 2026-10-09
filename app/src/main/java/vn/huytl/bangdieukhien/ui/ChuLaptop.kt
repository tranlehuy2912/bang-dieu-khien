package vn.huytl.bangdieukhien.ui

import vn.huytl.bangdieukhien.data.Duong
import vn.huytl.bangdieukhien.data.KetQuaLenhLaptop
import vn.huytl.bangdieukhien.data.LenhLaptop
import vn.huytl.bangdieukhien.data.LenhLaptopCho
import vn.huytl.bangdieukhien.data.SuKienLaptop
import vn.huytl.bangdieukhien.data.TinhTrangLaptop
import java.util.Calendar

/**
 * Chu cua the Laptop (8/10/2026), tach khoi [TheLaptop] de thu bang test JVM (ChuLaptopTest):
 * khong dung view hay Context, moi moc gio truyen vao.
 */
object ChuLaptop {

    /**
     * Gui [LenhLaptop.HOI] xong cho bao lau thi coi la laptop khong tra loi. Laptop hoi lenh mot
     * phut mot lan (anh Huy chot giu mot phut), cong thoi gian ghi len may chu; 150 giay la hon
     * hai vong hoi mot chut.
     */
    const val CHO_TRA_LOI_MS = 150_000L

    /** Ket qua lenh, lenh bi bo, hien bao lau sau khi xay ra. Cu hon thi khong con lien quan. */
    const val HIEN_KET_QUA_MS = 30 * 60_000L

    enum class Mau { XANH, VANG, XAM }

    /** Dong trang thai cua the. [choBam] false thi cac nut lenh mo di va bam chi bao ly do. */
    data class TrangThaiHien(val chu: String, val mau: Mau, val choBam: Boolean)

    /** Mot dong o cuoi the: [cho] lenh dang cho (mau nhat), ket qua lenh (xanh, [loi] thi do). */
    data class DongLenh(val chu: String, val loi: Boolean, val cho: Boolean = false)

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
        if (l.daTat()) return TrangThaiHien("Đã tắt lúc ${luc(l.tatLuc, bayGio)}", Mau.XAM, false)
        val daTraLoi = hoiLuc > 0L && (l.capNhatLuc >= hoiLuc || l.ketQua.any { it.id == hoiId })
        if (!daTraLoi) {
            if (hoiLuc > 0L && bayGio - hoiLuc > CHO_TRA_LOI_MS) {
                val tu = if (l.capNhatLuc > 0L) " từ ${luc(l.capNhatLuc, bayGio)}" else ""
                return TrangThaiHien("Không thấy laptop trả lời$tu", Mau.XAM, false)
            }
            return TrangThaiHien("Đang hỏi laptop…", Mau.XAM, true)
        }
        val tu = if (l.phienTu > 0L) " từ ${luc(l.phienTu, bayGio)}" else ""
        return when (l.phien) {
            "" -> TrangThaiHien("Đang bật · chưa ai đăng nhập", Mau.XANH, true)
            // Xem bang tai khoan Admin thi khong bi tru phut (sang 8/10/2026 xem mot tieng nhu vay).
            "huy" -> TrangThaiHien("Đang bật · tài khoản Admin$tu, không tính phút", Mau.VANG, true)
            else -> TrangThaiHien("Đang bật · tài khoản ${tenHien(l.phien)}$tu", Mau.XANH, true)
        }
    }

    /** "còn 38:12 Netflix" luc dang xem (dem lui tung giay), "còn 45 phút Netflix", "Hết phút Netflix". */
    fun soNetflix(l: TinhTrangLaptop, bayGio: Long): String {
        val con = l.conLai(bayGio)
        return when {
            con <= 0L -> "Hết phút Netflix"
            l.dangDung && l.ketThucLuc > 0L -> "còn ${Dinh.dongHo(con)} Netflix"
            else -> "còn ${Dinh.doDai(con)} Netflix"
        }
    }

    /**
     * "Hôm nay: bật 09:36, Admin vào 09:36, tắt 10:06, ..." tu [Duong.F_SU_KIEN]. Bo dong dang xuat
     * (ra) cho ngan: sau no la tat may hay mot lan vao khac. null khi khong co gi.
     */
    fun homNay(l: TinhTrangLaptop, bayGio: Long): String? {
        val dauNgay = dauNgay(bayGio)
        val cac = mutableListOf<String>()
        if (l.batLuc in 1 until dauNgay && !l.daTat()) cac += "đang bật từ hôm qua"
        for (s in l.suKien.filter { it.luc >= dauNgay }.sortedBy { it.luc }) {
            val gio = Dinh.gioPhut(s.luc)
            when (s.kieu) {
                SuKienLaptop.BAT -> cac += "bật $gio"
                SuKienLaptop.TAT -> cac += "tắt $gio"
                SuKienLaptop.MAT -> cac += "tắt đột ngột khoảng $gio"
                SuKienLaptop.VAO -> cac += "${tenHien(s.ai)} vào $gio"
            }
        }
        return if (cac.isEmpty()) null else "Hôm nay: " + cac.joinToString(", ")
    }

    /**
     * Cac dong cuoi the: lenh dang cho laptop nhan (tru HOI), ket qua lenh moi nhat trong
     * [HIEN_KET_QUA_MS], va cac lenh may nay da bo vi qua 5 phut ([bo]: lenh kem luc bo).
     */
    fun dongLenh(
        cho: List<LenhLaptopCho>, ketQua: List<KetQuaLenhLaptop>, bo: List<Pair<LenhLaptopCho, Long>>,
        bayGio: Long
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
        for (l in cho.filter { it.kieu != LenhLaptop.HOI }) {
            ra += DongLenh("Đang chờ laptop nhận: ${tenLenh(l.kieu)} (gửi ${Dinh.gioPhut(l.tao)}).", false, cho = true)
        }
        return ra
    }

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
