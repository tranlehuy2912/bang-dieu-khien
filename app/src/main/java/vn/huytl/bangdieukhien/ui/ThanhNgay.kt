package vn.huytl.bangdieukhien.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ReplacementSpan
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt
import vn.huytl.bangdieukhien.R
import vn.huytl.bangdieukhien.data.SoNgay

/**
 * Thanh han muc gio choi trong ngay o the chinh, ve y het thanh ngay o man chinh tablet
 * (ThanhNgay ben do). Ba mau co dinh, Ba Huy chot ngay 1/10/2026: trang (nen) la tong so phut
 * hom nay co the co, xam nhat la phut da kiem ma chua choi, xanh duong la phut da choi. Hai
 * khuc sat nhau, mau khong doi theo trang thai cong. So lieu tinh o [vn.huytl.bangdieukhien
 * .data.TrangThai.soNgay].
 *
 * Ban sao chu khong dung chung code voi tablet: ba app khong co module chung (giong Duong.kt).
 * Sua mau hay cach ve mot ben thi sua ca ben kia, de Ba Huy nhin hai may thay mot thanh.
 *
 * Truoc 1/10/2026 cho nay la mot LinearProgressIndicator vang chi ve phut da duyet tren tong
 * tran. Tablet ban cu chua gui so da choi thi ve mot khuc xam nhat cho phut da duyet, bang
 * chinh [ve]: khong biet da choi bao nhieu thi khong ve khuc xanh duong.
 */
object ThanhNgay {

    /**
     * Ve hai khuc [daChoi] (xanh duong) va [con] (xam nhat) tren truc dai [tong] phut. Phan con
     * lai cua truc la nen trang cua [khung], nen tong trong so phai bang ca thanh: thieu
     * weightSum thi hai khuc chia nhau lap kin, loi cua thanh tablet truoc 1/10/2026.
     */
    fun ve(
        khung: LinearLayout, phanDaChoi: View, phanCon: View,
        daChoi: Int, con: Int, tong: Int
    ) {
        val ct = khung.context
        // Thuoc tinh clipToOutline trong XML chi co tu Android 12; dat trong code thi may
        // Android cu hon cung bo tron hai dau khuc mau theo nen.
        khung.clipToOutline = true
        phanDaChoi.setBackgroundColor(ContextCompat.getColor(ct, R.color.da_choi))
        phanCon.setBackgroundColor(ContextCompat.getColor(ct, R.color.con_giu))
        khung.weightSum = tong.coerceAtLeast(1).toFloat()
        (phanDaChoi.layoutParams as LinearLayout.LayoutParams).weight = daChoi.coerceAtLeast(0).toFloat()
        (phanCon.layoutParams as LinearLayout.LayoutParams).weight = con.coerceAtLeast(0).toFloat()
        khung.requestLayout()
    }

    /**
     * Dong chu thich duoi thanh, "● đã chơi 30     ● còn 45", moi cham cung mau khuc cua no.
     * Khuc nao bang 0 thi khong noi; ca hai bang 0 thi tra chuoi rong.
     */
    fun chuThich(ct: Context, so: SoNgay): CharSequence {
        val sb = SpannableStringBuilder()
        fun muc(@ColorRes mau: Int, chu: String) {
            if (sb.isNotEmpty()) sb.append("     ")
            val dau = sb.length
            sb.append(' ')
            sb.setSpan(
                ChamTron(ContextCompat.getColor(ct, mau)),
                dau, dau + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            sb.append(chu)
        }
        if (so.daChoi > 0) muc(R.color.da_choi, "đã chơi ${so.daChoi}")
        if (so.con > 0) muc(R.color.con_giu, "còn ${so.con}")
        return sb
    }

    /**
     * Cham tron ve thang len dong chu, thay cho ky tu "●" ve theo bo font cua may. Co cham
     * theo co chu cua dong.
     */
    private class ChamTron(private val mau: Int) : ReplacementSpan() {

        override fun getSize(
            paint: Paint, text: CharSequence?, start: Int, end: Int, fm: Paint.FontMetricsInt?
        ): Int {
            if (fm != null) paint.getFontMetricsInt(fm)
            return (paint.textSize * (DUONG_KINH + CACH_SAU)).roundToInt()
        }

        override fun draw(
            canvas: Canvas, text: CharSequence?, start: Int, end: Int,
            x: Float, top: Int, y: Int, bottom: Int, paint: Paint
        ) {
            val cu = paint.color
            paint.color = mau
            val r = paint.textSize * DUONG_KINH / 2f
            val fm = paint.fontMetrics
            canvas.drawCircle(x + r, y + (fm.ascent + fm.descent) / 2f, r, paint)
            paint.color = cu
        }
    }

    private const val DUONG_KINH = 0.62f
    private const val CACH_SAU = 0.35f
}
