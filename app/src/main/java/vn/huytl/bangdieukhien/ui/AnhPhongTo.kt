package vn.huytl.bangdieukhien.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.animation.DecelerateInterpolator
import android.widget.OverScroller
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * O anh chum hai ngon de phong to, keo mot ngon de di.
 *
 * Tu viet chu khong keo thu vien: ca app chi dung o hai cho (man xem anh bai tap
 * [XemAnhActivity] va man xem anh laptop [XemAnhLaptop]), va viec can lam gon trong
 * mot ma tran chi co ti le va doi cho.
 *
 * Bai tap chup bang camera tablet thuong co chu but chi nhat o mot goc trang. Khong
 * phong to duoc thi Ba Huy phai mo Telegram len xem, tuc la man hinh nay vo dung.
 *
 * Hieu ung (anh Huy chot 29/10/2026 qua ban mau bam thu, luc them phong to anh man
 * hinh laptop, va cho man bai tap dung chung): bam dup thi anh phong muot len gap ba
 * ngay cho bam, bam dup lan nua thu muot ve vua khung; chum nho hon khung hay to qua
 * [tranPhong] roi tha tay thi anh bat ve; keo qua mep thi anh nang dan, tha ra bat ve
 * mep; keo roi buong nhanh thi anh truot theo da. Truoc ngay do moi thu nhay thang toi
 * dich, va hai chan tren duoi chan cung ngay trong luc chum.
 */
@SuppressLint("ClickableViewAccessibility")
class AnhPhongTo @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    /**
     * Phong to toi da bao nhieu lan so voi luc vua khung. Anh bai tap doc o gap doi be
     * ngang man hinh nen 8 lan van con net. Anh laptop chi rong 1280 diem anh, qua 4 lan
     * la nhoe (anh Huy chot 4 ngay 29/10/2026), [XemAnhLaptop] dat lai.
     */
    var tranPhong = 8f

    private val m = Matrix()
    private val so = FloatArray(9)

    /** Ti le va cho dat goc tren trai cua anh. Ma tran chi co hai phep nay, khong xoay. */
    private data class Trang(val ti: Float, val x: Float, val y: Float)

    private var hoat: ValueAnimator? = null
    private val truot = OverScroller(context)
    private var dangTruot = false

    /** Lan cham nay co chum hai ngon: buong tay ra thi khong truot theo da. */
    private var daChum = false

    /** Diem giua hai ngon lan chum cuoi, de bat ve quanh dung cho do chu khong quanh tam anh. */
    private var tieuX = 0f
    private var tieuY = 0f

    /** Dang thu anh ve cho cu ([thuVe]): khong nhan cham nua. */
    private var khoaCham = false

    private val chum = ScaleGestureDetector(context, object :
        ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(d: ScaleGestureDetector): Boolean {
            // Bam dup roi giu keo cung la phong (quick scale cua Android): dung hieu ung bam
            // dup dang chay, khong thi hai ben gianh nhau dat ma tran tung khung hinh.
            dungHet()
            daChum = true
            return true
        }

        override fun onScale(d: ScaleGestureDetector): Boolean {
            tieuX = d.focusX
            tieuY = d.focusY
            phongTo(d.scaleFactor, d.focusX, d.focusY)
            return true
        }
    })

    private val cham = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
            keo(-dx, -dy)
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            if (daChum) return false
            return truotTheoDa(vx, vy)
        }

        /** Bam hai cai: dang thu thi phong len gap ba, dang to thi ve nguyen. */
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (drawable == null) return true
            val t = doc()
            val toi = if (t.ti > vuaKhung() * 1.2f) {
                vuaKhit()
            } else {
                val ti = vuaKhung() * minOf(PHONG_DUP, tranPhong)
                val r = ti / t.ti
                dich(Trang(ti, e.x - (e.x - t.x) * r, e.y - (e.y - t.y) * r), e.x, e.y)
            }
            chayToi(toi, MS_DUP)
            return true
        }
    })

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (khoaCham) return true
        if (e.actionMasked == MotionEvent.ACTION_DOWN) {
            // Ngon tay dat xuong la chup lay anh dang chay giua chung, khong de no giat tiep.
            dungHet()
            daChum = false
            tieuX = width / 2f
            tieuY = height / 2f
        }
        chum.onTouchEvent(e)
        cham.onTouchEvent(e)
        val buong = e.actionMasked == MotionEvent.ACTION_UP ||
            e.actionMasked == MotionEvent.ACTION_CANCEL
        // Buong tay ma khong truot, khong dang chay hieu ung bam dup: bat ve neu dang lo.
        if (buong && !dangTruot && hoat?.isRunning != true) thaRa(tieuX, tieuY)
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, cu1: Int, cu2: Int) {
        super.onSizeChanged(w, h, cu1, cu2)
        datLai()
    }

    override fun onDetachedFromWindow() {
        dungHet()
        super.onDetachedFromWindow()
    }

    /** Dat anh vua khung, can giua. Goi lai moi lan doi anh hoac doi kich thuoc. */
    fun datLai() {
        if (drawable == null || width == 0 || height == 0) return
        dungHet()
        dat(vuaKhit())
    }

    /**
     * Mo tu anh nho: dat anh khit giua khung [o] (toa do trong view nay, la cho anh nho dang
     * nam), roi phong muot ra vua khung. Goi sau khi view da do kich thuoc.
     */
    fun moTu(o: RectF, ms: Long) {
        if (drawable == null || width == 0 || height == 0) return
        khoaCham = false
        dungHet()
        dat(khitTrong(o))
        chayToi(vuaKhit(), ms)
    }

    /** Thu anh muot ve khung [o] (nguoc voi [moTu]) roi goi [xong]. Tu luc nay khong nhan cham. */
    fun thuVe(o: RectF, ms: Long, xong: () -> Unit) {
        khoaCham = true
        if (drawable == null) {
            xong()
            return
        }
        chayToi(khitTrong(o), ms, xong)
    }

    // Trong luc chum cho lo ra ngoai gioi han mot doan (nho toi nua khung, to qua tran 60%),
    // tha tay moi bat ve. Chan cung ngay nhu truoc thi chum toi mep la anh dung khung lai,
    // tuong may do. Nhung van phai co chan: tha ra thi anh thu thanh mot cham hoac to toi
    // muc chi con thay mot o pixel, ca hai deu khong keo lai duoc bang tay.
    private fun phongTo(ti: Float, x: Float, y: Float) {
        val hienTai = tiLe()
        val moi = (hienTai * ti).coerceIn(vuaKhung() * 0.5f, vuaKhung() * tranPhong * 1.6f)
        m.postScale(moi / hienTai, moi / hienTai, x, y)
        imageMatrix = m
    }

    /**
     * Keo anh. Keo ra xa them khi anh da lo mep thi chi di mot phan ba quang duong ([NANG]),
     * keo nguoc vao thi di du: nguoi xem thay anh nang dan, biet la het cho.
     */
    private fun keo(dx: Float, dy: Float) {
        if (drawable == null) return
        val t = doc()
        val b = bien(t.ti)
        val nx = t.x + dx
        val ny = t.y + dy
        val x = if ((dx > 0 && nx > b.right) || (dx < 0 && nx < b.left)) t.x + dx * NANG else nx
        val y = if ((dy > 0 && ny > b.bottom) || (dy < 0 && ny < b.top)) t.y + dy * NANG else ny
        dat(Trang(t.ti, x, y))
    }

    /** Buong tay luc dang keo nhanh: anh truot tiep roi cham dan, toi mep thi nay nhe va dung. */
    private fun truotTheoDa(vx: Float, vy: Float): Boolean {
        if (drawable == null) return false
        val t = doc()
        val b = bien(t.ti)
        // Anh dang nam gon trong khung ca hai chieu thi khong co gi de truot.
        if (b.left == b.right && b.top == b.bottom) return false
        truot.fling(
            t.x.roundToInt(), t.y.roundToInt(), vx.roundToInt(), vy.roundToInt(),
            b.left.roundToInt(), b.right.roundToInt(), b.top.roundToInt(), b.bottom.roundToInt(),
            nay, nay
        )
        dangTruot = true
        postOnAnimation(buocTruot)
        return true
    }

    /** Truot theo da toi mep thi anh nay qua mep chung nay diem anh (16dp) roi moi bat ve. */
    private val nay = (16 * resources.displayMetrics.density).roundToInt()

    private val buocTruot = object : Runnable {
        override fun run() {
            if (!dangTruot) return
            if (truot.computeScrollOffset()) {
                dat(Trang(tiLe(), truot.currX.toFloat(), truot.currY.toFloat()))
                postOnAnimation(this)
            } else {
                dangTruot = false
                thaRa(width / 2f, height / 2f)
            }
        }
    }

    /** Buong tay: anh dang to qua, nho qua hay lo mep thi bat muot ve cho dung yen gan nhat. */
    private fun thaRa(x: Float, y: Float) {
        if (drawable == null || width == 0) return
        val t = doc()
        val d = dich(t, x, y)
        if (abs(d.ti - t.ti) < 1e-4f && abs(d.x - t.x) < 0.5f && abs(d.y - t.y) < 0.5f) return
        chayToi(d, MS_BAT_VE)
    }

    /** Chay muot tu cho dang dung toi [toi]. Ti le va cho dat doi cung nhip. */
    private fun chayToi(toi: Trang, ms: Long, xong: (() -> Unit)? = null) {
        dungHet()
        val tu = doc()
        hoat = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ms
            // Nhanh luc dau, cham dan luc toi (bac ba), giong mau anh Huy da bam thu.
            interpolator = DecelerateInterpolator(1.5f)
            addUpdateListener { a ->
                val f = a.animatedValue as Float
                dat(Trang(tu.ti + (toi.ti - tu.ti) * f, tu.x + (toi.x - tu.x) * f, tu.y + (toi.y - tu.y) * f))
            }
            addListener(object : AnimatorListenerAdapter() {
                private var huy = false
                override fun onAnimationCancel(a: Animator) {
                    huy = true
                }

                override fun onAnimationEnd(a: Animator) {
                    if (hoat === a) hoat = null
                    if (!huy) xong?.invoke()
                }
            })
            start()
        }
    }

    private fun dungHet() {
        hoat?.cancel()
        hoat = null
        if (dangTruot) {
            dangTruot = false
            truot.forceFinished(true)
            removeCallbacks(buocTruot)
        }
    }

    /**
     * Cho dung yen gan nhat voi [tu]: ti le trong khoang vua khung toi [tranPhong], anh khong
     * ho mep (anh nho hon khung chieu nao thi nam giua chieu do). Phai doi ti le thi giu yen
     * diem ([x], [y]).
     */
    private fun dich(tu: Trang, x: Float, y: Float): Trang {
        val ti = tu.ti.coerceIn(vuaKhung(), vuaKhung() * tranPhong)
        val r = ti / tu.ti
        val b = bien(ti)
        return Trang(
            ti,
            (x - (x - tu.x) * r).coerceIn(b.left, b.right),
            (y - (y - tu.y) * r).coerceIn(b.top, b.bottom)
        )
    }

    /**
     * Khoang duoc phep cua goc tren trai anh o ti le [ti]: trai-phai la x nho nhat, lon nhat,
     * tren-duoi la y. Anh hep hon khung chieu nao thi hai dau bang nhau, la cho can giua.
     */
    private fun bien(ti: Float): RectF {
        val d = drawable ?: return RectF()
        val rong = d.intrinsicWidth * ti
        val cao = d.intrinsicHeight * ti
        val x0 = if (rong <= width) (width - rong) / 2f else width - rong
        val x1 = if (rong <= width) x0 else 0f
        val y0 = if (cao <= height) (height - cao) / 2f else height - cao
        val y1 = if (cao <= height) y0 else 0f
        return RectF(x0, y0, x1, y1)
    }

    private fun vuaKhit(): Trang {
        val d = drawable ?: return Trang(1f, 0f, 0f)
        val ti = vuaKhung()
        return Trang(ti, (width - d.intrinsicWidth * ti) / 2f, (height - d.intrinsicHeight * ti) / 2f)
    }

    /** Anh khit giua khung [o], nhu ImageView kieu fitCenter ve anh trong khung do. */
    private fun khitTrong(o: RectF): Trang {
        val d = drawable ?: return Trang(1f, 0f, 0f)
        val ti = minOf(o.width() / d.intrinsicWidth, o.height() / d.intrinsicHeight)
        return Trang(
            ti,
            o.left + (o.width() - d.intrinsicWidth * ti) / 2f,
            o.top + (o.height() - d.intrinsicHeight * ti) / 2f
        )
    }

    private fun doc(): Trang {
        m.getValues(so)
        return Trang(so[Matrix.MSCALE_X], so[Matrix.MTRANS_X], so[Matrix.MTRANS_Y])
    }

    private fun dat(t: Trang) {
        m.setScale(t.ti, t.ti)
        m.postTranslate(t.x, t.y)
        imageMatrix = m
    }

    private fun tiLe(): Float {
        m.getValues(so)
        return so[Matrix.MSCALE_X]
    }

    /** Ti le luc anh vua khit khung, dung lam moc cho hai chan tren duoi. */
    private fun vuaKhung(): Float {
        val d = drawable ?: return 1f
        if (d.intrinsicWidth == 0 || width == 0) return 1f
        return minOf(
            width.toFloat() / d.intrinsicWidth,
            height.toFloat() / d.intrinsicHeight
        ).coerceAtLeast(0.01f)
    }

    private companion object {
        /** Bam dup phong len bao nhieu lan so voi vua khung (giu so cu, tu truoc 29/10/2026). */
        const val PHONG_DUP = 3f

        /** Keo qua mep thi anh chi di phan nay cua quang duong ngon tay. */
        const val NANG = 0.35f

        const val MS_DUP = 300L
        const val MS_BAT_VE = 320L
    }
}
