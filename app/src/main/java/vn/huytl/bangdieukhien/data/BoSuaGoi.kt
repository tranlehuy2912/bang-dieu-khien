package vn.huytl.bangdieukhien.data

/**
 * Dung "giaTri" cho lenh [Lenh.BO_SUA]: Ba Huy bo cau sai khoi danh sach "câu cần sửa" tren
 * tablet, khong cong phut (30/9/2026).
 *
 * VI SAO CO. Danh sach do tren tablet chi ngan di khi con sua dung. Cau ngoai sach tu trang
 * vo con khong con giu, hay cau Ba Huy thay khong dang bat chup lai, thi truoc day nam do
 * toi mot tuan (han tablet tu bo tu 30/9/2026). Go "đúng" qua SUA_CHAM thi con duoc cong
 * phut cho cau chua lam dung.
 *
 * Tach khoi BaiActivity de kiem thu goi thang duoc, nhu [SuaChamGoi].
 */
object BoSuaGoi {

    /**
     * Cau trong ban cham ma tablet co the dang ghi la can sua, de Ba Huy chon bo: moi cau chua
     * dung, ke ca cau doc chua ro va cau Ba Huy nho chup lai.
     *
     * Ban dau (30/9/2026 chieu) danh sach bo hai loai sau, coi do la cau cho Ba Huy xem chu
     * khong phai cau sai. Nhung tablet ghi so moi cau cham ra chua dung, ke ca cau Claude doc
     * chua chac, va dong "Có N câu cần sửa" dem ca chung. Bai KHTN 21:15 ngay 28/9/2026 co 7 cau
     * sai va 5 cau chua doc ro: Ba Huy bam bo, 5 cau kia van nam tren man chinh cua con. Cau
     * nao tablet khong con cho sua thi no noi lai o o tra loi, khong bo nham gi.
     */
    fun cauSai(cham: KetQuaCham?): List<CauCham> = cham?.cac.orEmpty().filter { !it.dung }

    /**
     * Chu cua mot cau trong hop chon: ma (ma phieu thi kem nghia, xem [MaCau]), kem ghi chu voi
     * cau chua doc ro hay nho chup lai.
     */
    fun nhan(c: CauCham, chupLai: Set<String> = emptySet()): String {
        val ma = MaCau.hien(c.ma).ifBlank { "câu" }
        return when {
            c.ma.trim() in chupLai -> "$ma (nhờ chụp lại)"
            !c.docRo -> "$ma (đọc chưa rõ)"
            else -> ma
        }
    }

    /** giaTri cua lenh: [{ ma, de }], ma va de lay tu ban cham nhu tablet ghi. */
    fun giaTri(cac: List<CauCham>): List<Map<String, Any>> =
        cac.map { mapOf("ma" to it.ma, "de" to it.de) }
}
