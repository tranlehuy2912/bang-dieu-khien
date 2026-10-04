package vn.huytl.bangdieukhien.ui

import vn.huytl.bangdieukhien.data.Buoi
import vn.huytl.bangdieukhien.data.BuoiHoc
import vn.huytl.bangdieukhien.data.NgayNghi
import vn.huytl.bangdieukhien.data.ThoiKhoaBieu
import java.util.Calendar

/**
 * Phan tinh cua tab Lich hoc: buoi nao dang hoc hay sap toi, tuan nao dang xem, luoi co
 * nhung hang nao, ten mon viet gon cho vua o. Tach khoi [LichFragment] de test tren may
 * tinh.
 *
 * Lich lay tu [ThoiKhoaBieu] va [NgayNghi], hai file chep y het tu tablet. Tu 4/10/2026 so
 * lieu cua hai file do la ban lich nhan tu Firestore (xem BanLich), cung document tablet
 * dang nghe, nen hai may hien cung mot lich ma khong phai cai lai app.
 * tools/kiem-duong.sh ben nop-bai so cac ban chep.
 */
object LichHoc {

    /** Mot hang cua luoi: dai ten buoi, hay mot tiet. */
    sealed interface Hang {
        data class Bang(val buoi: Buoi) : Hang
        data class Tiet(val buoi: Buoi, val tiet: Int) : Hang
    }

    /** Buoi dang hoc hay sap toi, kem ngay cua no. */
    data class BuoiToi(
        val ngay: Calendar,
        val buoi: BuoiHoc,
        /** Da vao tiet dau, chua tan. */
        val dangHoc: Boolean,
        /** Tiet dang hoc, chi co khi [dangHoc]. */
        val tiet: Int? = null
    )

    /** Tim toi ba tuan, du vuot ky nghi Tet muoi ngay. */
    private const val TIM_TOI_DA_NGAY = 21

    /**
     * Buoi hoc chua tan gan nhat: buoi dang hoc, hay buoi ke tiep. Bo ngay nghi. null la
     * ba tuan toi khong co buoi nao (nghi he).
     *
     * Khac buoiKeTiep ben tablet: ben do bo buoi da vao hoc, vi Le Hoa can biet buoi phai
     * soan tap. Ba Huy mo tab luc con dang o truong thi can biet con dang hoc gi.
     */
    fun buoiToi(now: Calendar): BuoiToi? {
        val phut = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        for (lui in 0 until TIM_TOI_DA_NGAY) {
            val ngay = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, lui) }
            if (NgayNghi.laNgayNghi(ngay)) continue
            val cacBuoi = ThoiKhoaBieu.buoiHocCua(ngay.get(Calendar.DAY_OF_WEEK))
            val buoi = (if (lui == 0) cacBuoi.firstOrNull { phut < it.phutTanHoc } else cacBuoi.firstOrNull())
                ?: continue
            return if (lui == 0 && phut >= buoi.phutVaoHoc) {
                BuoiToi(ngay, buoi, dangHoc = true, tiet = tietDangHoc(buoi, phut))
            } else {
                BuoiToi(ngay, buoi, dangHoc = false)
            }
        }
        return null
    }

    /** Tiet cuoi cung da vao hoc. Giua hai tiet thi van la tiet vua hoc. */
    private fun tietDangHoc(buoi: BuoiHoc, phut: Int): Int =
        buoi.monTheoTiet.keys.sorted().lastOrNull { phut >= ThoiKhoaBieu.gioTiet(buoi.buoi, it) }
            ?: buoi.tietDau

    /**
     * Thu hai toi thu bay cua tuan co [toi]. Khong con buoi nao thi la tuan nay, chu nhat
     * thi tuan toi, y nhu man lich ben tablet.
     *
     * Theo buoi toi chu khong theo hom nay: chieu thu bay tan hoc roi thi buoi toi la sang
     * thu hai, va luoi phai co o dang to dam cho buoi do.
     */
    fun tuanXem(now: Calendar, toi: BuoiToi?): List<Calendar> {
        val moc = toi?.ngay ?: now
        val lui = when (val thu = moc.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> -1
            else -> thu - Calendar.MONDAY
        }
        val thuHai = (moc.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -lui) }
        return (0..5).map { (thuHai.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, it) } }
    }

    /**
     * Cac hang cua luoi: moi buoi mot dai ten, roi cac tiet co mon o it nhat mot ngay.
     *
     * Bo tiet ca tuan khong ai hoc. Luoi ben tablet giu du nam tiet moi buoi, de Le Hoa
     * khong tuong tiet 3, 4 la hai tiet cuoi buoi sang. O day cot trai ghi gio tung tiet,
     * con man dien thoai thi ngan: nam tiet sang chi co hai tiet hoc (thu hai, thu sau) thi
     * ba hang trong chi day luoi xuong duoi man.
     *
     * Lay theo thu trong tuan, khong xet ngay nghi: tuan nghi Tet van ve du hang, ngay nghi
     * ghi ten ky nghi vao o dau.
     */
    fun dungHang(tuan: List<Calendar>): List<Hang> {
        val cacBuoi = tuan.flatMap { ThoiKhoaBieu.buoiHocCua(it.get(Calendar.DAY_OF_WEEK)) }
        return buildList {
            Buoi.entries.forEach { buoi ->
                val coMon = cacBuoi.filter { it.buoi == buoi }.flatMap { it.monTheoTiet.keys }.toSet()
                if (coMon.isEmpty()) return@forEach
                add(Hang.Bang(buoi))
                ThoiKhoaBieu.cacTiet(buoi).filter { it in coMon }.forEach { add(Hang.Tiet(buoi, it)) }
            }
        }
    }

    /**
     * Ten mon trong o luoi. O tren dien thoai chi rong chung nam, sau chu, nen ten dai viet
     * theo cach goi quen o truong. Mon khong co trong bang thi giu nguyen ten.
     */
    fun tenNgan(mon: String): String = TEN_NGAN[mon] ?: mon

    private val TEN_NGAN = mapOf(
        "Khoa học tự nhiên" to "KHTN",
        "Lịch sử - Địa lý" to "Sử - Địa",
        "Giáo dục thể chất" to "Thể dục",
        "Giáo dục công dân" to "GDCD",
        "Giáo dục địa phương" to "GDĐP",
        "Trải nghiệm hướng nghiệp" to "Trải nghiệm",
        "Trí tuệ nhân tạo" to "AI"
    )

    /** Ma cua mot buoi trong mot ngay, de so o nao thuoc buoi toi. */
    fun maBuoi(ngay: Calendar, buoi: Buoi): String = "%04d%02d%02d-%s".format(
        ngay.get(Calendar.YEAR),
        ngay.get(Calendar.MONTH) + 1,
        ngay.get(Calendar.DAY_OF_MONTH),
        buoi.name
    )

    /** "Chiều nay", "Sáng mai", "Sáng thứ hai 05/10". */
    fun tenBuoi(toi: BuoiToi, now: Calendar): String {
        val buoi = if (toi.buoi.buoi == Buoi.SANG) "Sáng" else "Chiều"
        return when (cachNgay(now, toi.ngay)) {
            0 -> "$buoi nay"
            1 -> "$buoi mai"
            else -> "$buoi ${ThoiKhoaBieu.tenThu(toi.buoi.thu)} ${ngayThang(toi.ngay)}"
        }
    }

    /** "Buông máy 11:30 · vào học 12:45 · tan 17:00", hay luc dang hoc "Tiết 3 · ...". */
    fun chiTiet(toi: BuoiToi): String {
        val tan = "tan ${Dinh.gio(toi.buoi.phutTanHoc)}"
        if (toi.dangHoc && toi.tiet != null) {
            val mon = toi.buoi.monTheoTiet[toi.tiet].orEmpty()
            return "Tiết ${toi.tiet}: $mon · $tan"
        }
        return "Buông máy ${Dinh.gio(ThoiKhoaBieu.phutBuongMay(toi.buoi))} · " +
            "vào học ${Dinh.gio(toi.buoi.phutVaoHoc)} · $tan"
    }

    fun ngayThang(ngay: Calendar): String =
        "%02d/%02d".format(ngay.get(Calendar.DAY_OF_MONTH), ngay.get(Calendar.MONTH) + 1)

    /** "Thứ 2" toi "Thứ 7" cho dau cot: "thứ hai" viet du thi khong vua o. */
    fun dauCot(ngay: Calendar): String = when (val thu = ngay.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SUNDAY -> "CN"
        else -> "Thứ $thu"
    }

    fun cungNgay(a: Calendar, b: Calendar): Boolean = cachNgay(a, b) == 0

    /** So ngay tu [tu] toi [den], dem theo lich chu khong theo 24 tieng. */
    private fun cachNgay(tu: Calendar, den: Calendar): Int {
        val a = (tu.clone() as Calendar).apply { dauNgay() }
        val b = (den.clone() as Calendar).apply { dauNgay() }
        return Math.round((b.timeInMillis - a.timeInMillis) / 86_400_000.0).toInt()
    }

    private fun Calendar.dauNgay() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
