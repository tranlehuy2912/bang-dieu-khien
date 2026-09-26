package vn.huytl.bangdieukhien.data

import android.content.Context
import vn.huytl.bangdieukhien.R

/**
 * Hai thu app nay phai nho giua cac lan mo: nha nao va token bot nao.
 *
 * Ma nha lay duoc luc ghep doi, khong bao gio doi nua. Token bot chi dung de tai
 * anh bai tap tu Telegram ve xem - app nay khong bao gio goi getUpdates, nen
 * khong bao gio giat ket noi cua tablet (loi 409).
 */
object Nha {

    fun maNha(context: Context): String = sp(context).getString(K_NHA, "").orEmpty()

    fun datMaNha(context: Context, ma: String) {
        sp(context).edit().putString(K_NHA, ma.trim()).commit()
    }

    fun daGhep(context: Context): Boolean = maNha(context).isNotEmpty()

    /** Token bot Telegram. De trong thi man bai tap khong tai duoc anh ve. */
    fun token(context: Context): String =
        sp(context).getString(K_TOKEN, null)?.takeIf { it.isNotBlank() } ?: Defaults.BOT_TOKEN

    fun datToken(context: Context, token: String) {
        sp(context).edit().putString(K_TOKEN, token.trim()).commit()
    }

    /**
     * Ten con, de hien tren man hinh va dua vao loi nho Claude.
     *
     * Tablet co ghi ten len Firestore (truong tenCon) nhung app nay chua doc ve, chua cho
     * nao goi [datTenCon]. Truoc ngay 26/9/2026 cho nay tra ve chu "con", nen loi nho gui
     * Claude ghi "bai tap ve nha cua con toi" chu khong co ten. Nay lay [R.string.child_name].
     */
    fun tenCon(context: Context): String =
        sp(context).getString(K_TEN_CON, null)?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.child_name)

    fun datTenCon(context: Context, ten: String) {
        sp(context).edit().putString(K_TEN_CON, ten).commit()
    }

    /** Tin nhan cuoi cung da doc, de biet co tin moi hay khong. */
    fun tinDaDocLuc(context: Context): Long = sp(context).getLong(K_TIN_DA_DOC, 0L)

    fun datTinDaDocLuc(context: Context, luc: Long) {
        sp(context).edit().putLong(K_TIN_DA_DOC, luc).apply()
    }

    fun xoaHet(context: Context) {
        sp(context).edit().clear().commit()
    }

    private fun sp(context: Context) =
        context.getSharedPreferences("bang_dieu_khien", Context.MODE_PRIVATE)

    private const val K_NHA = "ma_nha"
    private const val K_TOKEN = "bot_token"
    private const val K_TEN_CON = "ten_con"
    private const val K_TIN_DA_DOC = "tin_da_doc"
}
