package vn.huytl.bangdieukhien.ui

/**
 * Doi so mu viet bang dau "^" ra chu so mu that, chi de hien cho Ba Huy doc: "x^2y^3" thanh
 * "x²y³", "10^23" thanh "10²³", ion "Ca^2+" thanh "Ca²⁺".
 *
 * VI SAO CO (29/9/2026). De bai trong ngan hang ben tablet ghi so mu bang "^", va ban cham
 * mang nguyen de do sang day. Man bai hien "(2x + 5y)^2" trong khi tablet hien "(2x + 5y)²".
 *
 * CHEP Y HET vn.huytl.homeworkgate.ui.SoMu ben nop-bai, ke ca luat: chi doi chu so ngay sau
 * "^", va dau ion dung sat sau chu so do hay sat sau "^"; kieu khac ("^(n+1)", "^x") de
 * nguyen. Sua mot ben thi sua ca ben kia.
 *
 * CHI DOI LUC HIEN. De di trong lenh sang tablet (SUA_CHAM, XU_CAU) va loi nho Claude giu
 * nguyen dau "^": tablet so de bang chu luu, doi o day la lenh sua cham khong khop cau nao.
 */
object SoMu {

    private const val SO = "0123456789"
    private const val SO_MU = "⁰¹²³⁴⁵⁶⁷⁸⁹"
    private const val SO_DUOI = "₀₁₂₃₄₅₆₇₈₉"

    /** Ten mon KHTN, cung chuoi voi LichKiemTra.KHTN ben nop-bai. */
    private const val KHTN = "Khoa học tự nhiên"

    /** Chu so ngay sau "^", co the kem mot dau ion; hoac chi mot dau ion ("H^+"). */
    private val MU = Regex("""\^(\d*)([+−-](?![\p{L}\d(]))?""")

    fun hien(chu: String): String {
        if ('^' !in chu) return chu
        return MU.replace(chu) { m ->
            val so = m.groupValues[1]
            val dau = m.groupValues[2]
            if (so.isEmpty() && dau.isEmpty()) {
                m.value
            } else {
                so.map { SO_MU[SO.indexOf(it)] }.joinToString("") +
                    when (dau) {
                        "" -> ""
                        "+" -> "⁺"
                        else -> "⁻"
                    }
            }
        }
    }

    /**
     * Chu so ngay sau ky hieu nguyen to (mot chu hoa, co the them mot chu thuong) hay sau dau
     * ")": chi so trong cong thuc hoa hoc, "C2H6", "Ca(OH)2", "Fe2(SO4)3", "CuSO4.5H2O".
     *
     * Chi xet sau chu hoa. Trong de KHTN (quet 29/9/2026) ngoai cong thuc chi con ky hieu dai
     * luong M1, T1, Et1, ma viet chi so duoi cung la dung. Chu thuong thi de nguyen: "n1" hien
     * lech voi "M₁" mot chut, nhung doi ca chu thuong thi don vi "m3", "cm3" viet thieu dau "^"
     * se thanh chi so duoi.
     */
    private val CHI_SO = Regex("""(?<=[A-Z][a-z]?|\))\d+""")

    /** Doi chi so cong thuc hoa hoc viet bang so thuong ra chi so duoi: "C2H6" thanh "C₂H₆". */
    fun chiSoHoa(chu: String): String =
        CHI_SO.replace(chu) { m -> m.value.map { SO_DUOI[SO.indexOf(it)] }.joinToString("") }

    /**
     * Hien de cua mot cau mon [mon]: moi mon doi so mu, rieng KHTN doi them chi so cong thuc
     * hoa hoc. Sach KHTN luu cong thuc bang so thuong ("CO2").
     */
    fun hienDe(chu: String, mon: String): String =
        if (mon == KHTN) chiSoHoa(hien(chu)) else hien(chu)
}
