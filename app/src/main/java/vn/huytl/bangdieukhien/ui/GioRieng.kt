package vn.huytl.bangdieukhien.ui

import vn.huytl.bangdieukhien.data.AppTrenMay
import vn.huytl.bangdieukhien.data.CaiDat

/**
 * Hop "Giờ riêng từng app" o tab Cai dat: moi app mot dong, kem so phut moi ngay.
 *
 * Tablet dem va khoa. May nay chi doc so phut o hop/caidat, va gui lenh CAIDAT
 * gioiHanApp moi lan mot app: {goi: so phut}, 0 la bo. Tablet chi doi dung app duoc
 * gui, app khac giu nguyen.
 *
 * Tach khoi CaiDatFragment de test duoc thu tu va cau chu ma khong can man hinh.
 */
object GioRieng {

    /** So phut chon san, giong het man Giờ riêng từng app tren tablet. */
    val MUC_PHUT = listOf(15, 30, 45, 60, 90, 120, 180)

    /**
     * Chinh app Nop bai. Man chon ben tablet khong liet ke no, va dich vu canh app
     * khong bao gio chan no, nen dat gio cho no cung khong khoa duoc gi.
     */
    private const val GOI_NOP_BAI = "vn.huytl.homeworkgate"

    /** Mot dong trong hop. [phut] bang 0 la app chua dat gio rieng. */
    data class Dong(val goi: String, val ten: String, val phut: Int)

    /**
     * Thu tu cac app luc mo hop: app da dat gio len dau, trong moi nhom giu thu tu
     * theo ten ma tablet gui sang.
     *
     * Chot mot lan luc mo. Chon xong mot app thi mot hai giay sau tablet moi ghi lai
     * so phut; xep lai luc do thi app vua cham nhay len dau, lot khoi cho Ba Huy dang
     * nhin, va trong nhu chua doi gi.
     *
     * App co gio rieng ma danh sach app cua tablet khong co (da go, hay danh sach cu)
     * van hien bang ten goi, de con bo gio duoc.
     */
    fun thuTu(ds: List<AppTrenMay>, han: Map<String, Int>): List<String> {
        val (co, khong) = (ds.map { it.goi } + han.keys)
            .distinct()
            .filter { it != GOI_NOP_BAI }
            .partition { (han[it] ?: 0) > 0 }
        return co + khong
    }

    /**
     * Cac dong theo [thuTu] da chot, voi so phut moi nhat. App vua co gio rieng trong
     * luc hop dang mo (Ba Huy dat ngay tren tablet) noi vao cuoi. App da go va khong
     * con gio rieng thi bo.
     */
    fun cacDong(thuTu: List<String>, ds: List<AppTrenMay>, han: Map<String, Int>): List<Dong> {
        val ten = ds.associate { it.goi to it.ten }
        return (thuTu + han.keys.filter { it !in thuTu && it != GOI_NOP_BAI })
            .filter { it in ten || (han[it] ?: 0) > 0 }
            .map { Dong(it, ten[it] ?: it, han[it] ?: 0) }
    }

    /**
     * Dong phu cho app co gio rieng: het gio choi thi app co mo duoc khong.
     *
     * Gio rieng chi la cai tran, khong mo app ra. Thieu dong nay thi nhin "YouTube
     * 30 phút" de tuong Le Hoa co rieng 30 phut YouTube moi ngay, khong can gio choi.
     * Cung ba cau voi man tren tablet.
     */
    fun khiHetGio(goi: String, c: CaiDat): String = when (goi) {
        in c.appMoiLuc -> "Mở được mọi lúc"
        in c.appChoPhep -> "Mở được khi hết giờ chơi, trừ giờ ngủ, giờ học"
        else -> "Chỉ mở trong giờ chơi"
    }

    /**
     * Cac so phut cho chon, 0 la "Không giới hạn" dung dau. So dang dat ma khong nam
     * trong [MUC_PHUT] (lenh CAIDAT nhan moi so tu 1 toi 1440) van co mot dong, de hop
     * danh dau duoc dong dang chon.
     */
    fun cacMuc(dangCo: Int): List<Int> =
        listOf(0) + (MUC_PHUT + dangCo).filter { it > 0 }.distinct().sorted()

    /** "Không giới hạn", "45 phút mỗi ngày", "1 tiếng 30 phút mỗi ngày". */
    fun tenMuc(phut: Int): String =
        if (phut <= 0) "Không giới hạn" else "${Dinh.phut(phut)} mỗi ngày"
}
