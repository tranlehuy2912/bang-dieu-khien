package vn.huytl.bangdieukhien.data

/**
 * Dung "giaTri" cho lenh [Lenh.SUA_CHAM]: cau lan cham truoc bao sai (hay doc chua ro) ma
 * Claude cham lai chac la dung.
 *
 * VI SAO KEM SO DONG (29/9/2026). Lan cham dau la ban Claude (lenh CHAM_BAI): Claude chi
 * noi so dong chu khong chep tung dong, nen so cai ben tablet khong co dong bai lam nao cua
 * cau do. Lenh chi co ma va de thi tablet dem ra 0 dong, va tu khi bo san phut moi cau
 * (29/9/2026) cau sua duoc ghi la xong voi 0 phut: con lam dung ma mat han phut cua cau.
 * Nay gui kem so dong Claude ghi o lan cham lai. Claude khong ghi thi gui 0: tablet de cau
 * cho sua va noi lai o o tra loi tren man Bang.
 *
 * Tach khoi BaiActivity de kiem thu goi thang duoc, nhu [XuCau].
 */
object SuaChamGoi {

    /** Mot cau se sua: [cu] la muc cua lan cham truoc, [soDong] la so dong Claude ghi lan nay. */
    data class Cau(val cu: CauCham, val soDong: Int)

    /**
     * Cau Claude lan nay chac la dung ma lan cham truoc ([truoc]) bao sai hay doc chua ro.
     * Ghep theo ma nhu man bai van lam: ma lan truoc bo khoang trang hai dau, ma Claude giu
     * nguyen.
     */
    fun thanhDung(truoc: List<CauCham>, claude: List<CauClaude>): List<Cau> {
        val theoMa = truoc.associateBy { it.ma.trim() }
        return claude.filter { it.chac && it.dung }.mapNotNull { cl ->
            theoMa[cl.ma]?.takeIf { !(it.docRo && it.dung) }?.let { Cau(it, cl.soDong) }
        }
    }

    /** giaTri cua lenh: [{ ma, de, soDong }], ma va de lay tu lan cham truoc nhu tablet ghi. */
    fun giaTri(cac: List<Cau>): List<Map<String, Any>> = cac.map {
        mapOf("ma" to it.cu.ma, "de" to it.cu.de, "soDong" to it.soDong.coerceAtLeast(0))
    }
}
