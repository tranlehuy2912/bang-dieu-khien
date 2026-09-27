package vn.huytl.bangdieukhien.ui

import android.content.Context
import android.widget.Toast
import vn.huytl.bangdieukhien.data.Lenh
import vn.huytl.bangdieukhien.data.LenhCho
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * May ham nho dung o nhieu man hinh. Gom mot cho de moi man hinh khong tu viet
 * mot kieu hien gio khac nhau.
 */
object Dinh {

    private val VN = Locale.forLanguageTag("vi-VN")

    /** "45:07" hoac "1:02:11" khi con hon mot tieng. */
    fun dongHo(ms: Long): String {
        val giay = (ms / 1000).coerceAtLeast(0L)
        val h = giay / 3600
        val p = (giay % 3600) / 60
        val g = giay % 60
        return if (h > 0) "%d:%02d:%02d".format(h, p, g) else "%02d:%02d".format(p, g)
    }

    /** So phut thanh cau nguoi doc: "45 phút", "1 tiếng 15 phút". */
    fun phut(p: Int): String = when {
        p < 60 -> "$p phút"
        p % 60 == 0 -> "${p / 60} tiếng"
        else -> "${p / 60} tiếng ${p % 60} phút"
    }

    /** Do dai tinh bang ms: "dưới 1 phút", "45 phút", "1 tiếng 15 phút". */
    fun doDai(ms: Long): String =
        if (ms < 60_000L) "dưới 1 phút" else phut((ms / 60_000L).toInt())

    /** "hôm nay", "hôm qua", "thứ tư 23/09": ngay [lui] ngay truoc hom nay. */
    fun tenNgay(lui: Int): String = when (lui) {
        0 -> "hôm nay"
        1 -> "hôm qua"
        else -> thuVaNgay(lui)
    }

    /** "thứ tư 23/09", ke ca hom nay va hom qua: ngay [lui] ngay truoc hom nay. */
    fun thuVaNgay(lui: Int): String {
        val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -lui) }
        val thu = when (c.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "thứ hai"
            Calendar.TUESDAY -> "thứ ba"
            Calendar.WEDNESDAY -> "thứ tư"
            Calendar.THURSDAY -> "thứ năm"
            Calendar.FRIDAY -> "thứ sáu"
            Calendar.SATURDAY -> "thứ bảy"
            else -> "chủ nhật"
        }
        return "$thu ${SimpleDateFormat("dd/MM", VN).format(c.time)}"
    }

    /** Phut tu dau ngay thanh "21:30". */
    fun gio(phutTrongNgay: Int): String =
        "%02d:%02d".format(phutTrongNgay / 60, phutTrongNgay % 60)

    fun gioPhut(luc: Long): String = SimpleDateFormat("HH:mm", VN).format(Date(luc))

    /**
     * Nhan thoi gian cho danh sach: hom nay thi chi gio, hom khac thi kem ngay.
     * Danh sach bai tap gan nhu luc nao cung la hom nay, ghi ca ngay vao moi dong
     * chi lam dai them ma khong noi gi.
     */
    fun lucNgan(luc: Long): String {
        val c1 = Calendar.getInstance().apply { timeInMillis = luc }
        val c2 = Calendar.getInstance()
        val cungNgay = c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
        return SimpleDateFormat(if (cungNgay) "HH:mm" else "HH:mm dd/MM", VN).format(Date(luc))
    }

    /**
     * Mot lenh dang cho tablet, viet thanh cum nguoi doc: "cho chơi 15 phút".
     *
     * Viet thuong o dau vi no dung sau "Đang chờ tablet nhận:". Lenh kieu la (Bang
     * dieu khien ban moi hon go ra, hay lenh cu) thi hien nguyen ten kieu, con hon la
     * mot dong trong.
     */
    fun lenh(l: LenhCho): String = when (l.kieu) {
        Lenh.CHO -> l.phut?.let { "cho chơi ${phut(it)}" } ?: "cho chơi"
        Lenh.DUYET -> "duyệt bài" + (l.phut?.let { ", ${phut(it)}" } ?: "")
        Lenh.TU_CHOI -> "không duyệt bài"
        Lenh.BOT -> "bớt ${phut(l.phut ?: 15)}"
        Lenh.DUNG -> "tạm dừng phiên chơi"
        Lenh.TIEP -> "cho chơi tiếp"
        Lenh.KHOA -> "khoá tablet"
        Lenh.MO_MAY -> l.phut?.let { "mở toàn bộ máy ${phut(it)}" } ?: "mở toàn bộ máy, không đặt hạn"
        Lenh.DONG_MAY -> "đóng chế độ Ba Huy"
        Lenh.CHO_GO_APP -> "cho gỡ app"
        Lenh.XOA_PIN -> "xoá mã PIN"
        Lenh.CAI_DAT -> "đổi cài đặt"
        Lenh.CONG_VIEC_NHA -> "cộng giờ việc nhà"
        Lenh.SUA_CHAM -> "sửa bản chấm theo Claude"
        Lenh.CHAM_BAI -> "chấm bài theo Claude"
        Lenh.TIN_CO -> "tin của cô giáo"
        Lenh.DOC_VO -> "vở dặn dò Claude đọc"
        else -> l.kieu.lowercase(VN)
    }

    /** Ngay hom nay dang "yyyy-MM-dd", dung lam ten document nhat ky. */
    fun homNay(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun noi(context: Context, chu: String) =
        Toast.makeText(context, chu, Toast.LENGTH_SHORT).show()

    /**
     * Vi sao mot ngay so dung app trong. Chung cho the o tab Nhat ky va man xem tung ngay, de
     * hai cho noi cung mot cau cho cung mot canh.
     *
     * Ban cuoi tablet gui tu truoc ngay do: "chua ghi duoc app nao" luc nay la noi sai, vi
     * so ngay do chua ai gui sang. Dich vu tat thi chi giai thich duoc hom nay, khong giai
     * thich duoc vi sao thu Hai tuan truoc khong co dong nao.
     */
    fun viSaoTrong(s: vn.huytl.bangdieukhien.data.SoSuDung, dau: Long, laHomNay: Boolean, ten: String): String = when {
        s.capNhatLuc > 0L && s.capNhatLuc < dau ->
            "Chưa có số liệu $ten: lần cuối tablet gửi sổ lúc ${lucNgan(s.capNhatLuc)}."
        laHomNay && !s.dangGhi ->
            "Dịch vụ canh app trên tablet đang tắt nên máy không ghi được app nào, cũng không chặn app nào."
        else -> "Chưa ghi được app nào $ten."
    }
}
