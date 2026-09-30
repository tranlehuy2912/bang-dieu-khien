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
     * Cau sai trong ban cham, de Ba Huy chon bo. Cau doc chua ro khong tinh (do la cau cho
     * Ba Huy xem, khong phai cau sai), cau Ba Huy nho chup lai ([chupLai]) cung khong.
     */
    fun cauSai(cham: KetQuaCham?, chupLai: Set<String> = emptySet()): List<CauCham> =
        cham?.cac.orEmpty().filter { it.docRo && !it.dung && it.ma.trim() !in chupLai }

    /** giaTri cua lenh: [{ ma, de }], ma va de lay tu ban cham nhu tablet ghi. */
    fun giaTri(cac: List<CauCham>): List<Map<String, Any>> =
        cac.map { mapOf("ma" to it.ma, "de" to it.de) }
}
