package vn.huytl.bangdieukhien.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Mot ngay thanh mot dai ngang, moi khoang Le Hoa cam may la mot vach tren do.
 *
 * Chep tu DaiNgay ben tablet, de hai man nhin ra cung mot hinh. Con so tong noi app
 * nao bao lau, nhung khong noi luc nao: hai tieng YouTube rai deu ca buoi chieu va
 * hai tieng lien sau chin gio toi thi tong giong het nhau.
 *
 * Ve bang drawRect chu khong ghep View: mot ngay co the co vai chuc khoang, moi
 * khoang mot View la vai chuc View chi de ve may cai vach.
 */
class DaiNgay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val butNen = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val butVach = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val butGio = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private var dauNgay = 0L
    private var cac: List<Pair<Long, Long>> = emptyList()

    /** [cac] la cac khoang tinh bang milli giay tuyet doi, cat san trong mot ngay. */
    fun dat(dauNgay: Long, cac: List<Pair<Long, Long>>, mauVach: Int, mauNen: Int, mauGio: Int) {
        this.dauNgay = dauNgay
        this.cac = cac
        butVach.color = mauVach
        butNen.color = mauNen
        butGio.color = mauGio
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val cao = height.toFloat()
        val rong = width.toFloat()
        if (rong <= 0f) return
        val bo = cao / 2f

        canvas.drawRoundRect(RectF(0f, 0f, rong, cao), bo, bo, butNen)

        // Vach chia sau tieng mot: khong co no thi khong doc duoc mot vach nam o
        // khoang nao trong ngay.
        val dayGio = rong / 24f
        for (g in 6..18 step 6) {
            val x = dayGio * g
            canvas.drawRect(x - dp(0.5f), 0f, x + dp(0.5f), cao, butGio)
        }

        val mot = 24L * 60 * 60_000L
        cac.forEach { (tu, den) ->
            val t1 = ((tu - dauNgay).coerceIn(0L, mot)).toFloat() / mot * rong
            val t2 = ((den - dauNgay).coerceIn(0L, mot)).toFloat() / mot * rong
            // Khoang vai phut ra mot vach mong hon soi toc: keo len toi thieu 2dp de
            // no con nhin thay duoc.
            val phai = maxOf(t2, t1 + dp(2f))
            canvas.drawRoundRect(RectF(t1, 0f, minOf(phai, rong), cao), bo, bo, butVach)
        }
    }

    private fun dp(v: Float) = v * resources.displayMetrics.density
}
