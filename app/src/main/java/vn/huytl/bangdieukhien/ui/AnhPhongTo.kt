package vn.huytl.bangdieukhien.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

/**
 * O anh chum hai ngon de phong to, keo mot ngon de di.
 *
 * Tu viet chu khong keo thu vien: ca app chi can dung mot cho nay, va viec can lam
 * gon trong mot ma tran bon phep.
 *
 * Bai tap chup bang camera tablet thuong co chu but chi nhat o mot goc trang. Khong
 * phong to duoc thi Ba Huy phai mo Telegram len xem, tuc la man hinh nay vo dung.
 */
@SuppressLint("ClickableViewAccessibility")
class AnhPhongTo @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    private val m = Matrix()
    private val khung = RectF()

    private val chum = ScaleGestureDetector(context, object :
        ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(d: ScaleGestureDetector): Boolean {
            phongTo(d.scaleFactor, d.focusX, d.focusY)
            return true
        }
    })

    private val cham = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
            m.postTranslate(-dx, -dy)
            ghim()
            return true
        }

        /** Bam hai cai: dang thu thi phong len gap ba, dang to thi ve nguyen. */
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (tiLe() > 1.2f) datLai() else phongTo(3f, e.x, e.y)
            return true
        }
    })

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        chum.onTouchEvent(e)
        cham.onTouchEvent(e)
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, cu1: Int, cu2: Int) {
        super.onSizeChanged(w, h, cu1, cu2)
        datLai()
    }

    /** Dat anh vua khung, can giua. Goi lai moi lan doi anh hoac doi kich thuoc. */
    fun datLai() {
        val d = drawable ?: return
        if (width == 0 || height == 0) return
        val ti = minOf(width.toFloat() / d.intrinsicWidth, height.toFloat() / d.intrinsicHeight)
        m.setScale(ti, ti)
        m.postTranslate(
            (width - d.intrinsicWidth * ti) / 2f,
            (height - d.intrinsicHeight * ti) / 2f
        )
        imageMatrix = m
    }

    private fun phongTo(ti: Float, x: Float, y: Float) {
        // Chan tren va duoi: tha ra thi anh thu nho thanh mot cham hoac to den muc
        // chi con thay mot o pixel, ca hai deu khong keo lai duoc bang tay.
        val hienTai = tiLe()
        val moi = (hienTai * ti).coerceIn(vuaKhung() * 0.9f, vuaKhung() * 8f)
        m.postScale(moi / hienTai, moi / hienTai, x, y)
        ghim()
    }

    /** Khong cho keo anh ra ngoai khung den muc mat hut. */
    private fun ghim() {
        val d = drawable ?: return
        khung.set(0f, 0f, d.intrinsicWidth.toFloat(), d.intrinsicHeight.toFloat())
        m.mapRect(khung)
        var dx = 0f
        var dy = 0f
        if (khung.width() <= width) dx = (width - khung.width()) / 2f - khung.left
        else if (khung.left > 0) dx = -khung.left
        else if (khung.right < width) dx = width - khung.right

        if (khung.height() <= height) dy = (height - khung.height()) / 2f - khung.top
        else if (khung.top > 0) dy = -khung.top
        else if (khung.bottom < height) dy = height - khung.bottom

        m.postTranslate(dx, dy)
        imageMatrix = m
    }

    private fun tiLe(): Float {
        val so = FloatArray(9)
        m.getValues(so)
        return so[Matrix.MSCALE_X]
    }

    /** Ti le luc anh vua khit khung, dung lam moc cho hai chan tren duoi. */
    private fun vuaKhung(): Float {
        val d = drawable ?: return 1f
        if (d.intrinsicWidth == 0) return 1f
        return minOf(
            width.toFloat() / d.intrinsicWidth,
            height.toFloat() / d.intrinsicHeight
        ).coerceAtLeast(0.01f)
    }
}
