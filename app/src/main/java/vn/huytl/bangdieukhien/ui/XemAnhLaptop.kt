package vn.huytl.bangdieukhien.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import androidx.activity.ComponentDialog
import androidx.activity.addCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updatePadding
import vn.huytl.bangdieukhien.R
import vn.huytl.bangdieukhien.databinding.HopXemAnhLaptopBinding

/**
 * Xem anh man hinh laptop to ca man hinh (anh Huy chot 29/10/2026, cach B trong ban mau bam thu):
 * bam anh nho trong hop "Màn hình laptop" thi anh to dan ra ca man hinh nen den; o do chum, bam
 * dup, keo nhu man xem anh bai tap ([AnhPhongTo]), toi da 4 lan; bam X hay nut Quay lai thi anh
 * thu dan ve dung cho cu trong hop. Cach A (phong ngay trong hop) bi bo vi khung anh trong hop chi
 * cao chung 4 cm, phong len chi thay mot goc nho.
 *
 * La mot Dialog phu kin man hinh chu khong phai Activity: anh da giai nen san trong hop, hop van
 * nghe anh moi tu Firestore va doi duoc anh dang xem to ([doiAnh]). Sang Activity thi phai chuyen
 * anh qua mot cho giu chung, va hieu ung chuyen canh chung phan tu cua Android chi chay giua hai
 * Activity, khong chay tu mot hop thoai. Nen hieu ung bay ra, thu ve do [AnhPhongTo.moTu],
 * [AnhPhongTo.thuVe] tu ve bang ma tran anh, lay cho anh nho bang toa do tren man hinh.
 */
class XemAnhLaptop(private val ct: Context, private val anhNho: ImageView) {

    private var hop: ComponentDialog? = null
    private var bv: HopXemAnhLaptopBinding? = null
    private var dangDong = false

    fun mo(bm: Bitmap, chu: String) {
        if (hop != null) return
        val b = HopXemAnhLaptopBinding.inflate(LayoutInflater.from(ct))
        val h = ComponentDialog(ct, R.style.Theme_BangDieuKhien_XemAnhLaptop)
        h.setContentView(b.root)
        h.window?.let { w ->
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            WindowCompat.setDecorFitsSystemWindows(w, false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                w.attributes = w.attributes.apply {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
            WindowCompat.getInsetsController(w, w.decorView).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
        // Anh ve tran ca duoi thanh trang thai; rieng thanh tren (gio chup, dau X) tranh no ra.
        ViewCompat.setOnApplyWindowInsetsListener(b.root) { _, insets ->
            val thanh = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            b.thanhTren.updatePadding(top = thanh.top + le8)
            insets
        }
        h.onBackPressedDispatcher.addCallback(h) { dong() }
        h.setOnDismissListener {
            anhNho.visibility = View.VISIBLE
            hop = null
            bv = null
        }
        b.nutDong.setOnClickListener { dong() }
        b.anh.tranPhong = TRAN_PHONG
        b.anh.setImageBitmap(bm)
        b.nhan.text = chu
        hop = h
        bv = b
        dangDong = false
        h.show()
        // Do xong kich thuoc moi biet anh nho nam dau so voi khung to. Lan ve dau tien da la anh
        // nam dung cho anh nho, nen nhin nhu chinh anh nho bay ra.
        b.root.doOnLayout {
            b.anh.moTu(oAnhNho(b.anh), MS_MO)
            anhNho.visibility = View.INVISIBLE
            b.nen.animate().alpha(1f).setDuration(MS_MO).start()
            b.thanhTren.animate().alpha(1f).setDuration(MS_MO).start()
        }
    }

    /**
     * Hop co anh moi (Ba Huy bam Chup, laptop gui ve) hay chu gio chup doi trong luc dang xem to:
     * thay luon cho anh dang xem. [bm] null la anh khong doi, chi doi chu.
     */
    fun doiAnh(bm: Bitmap?, chu: String) {
        val b = bv ?: return
        if (dangDong) return
        b.nhan.text = chu
        if (bm != null) {
            b.anh.setImageBitmap(bm)
            b.anh.datLai()
        }
    }

    /** Nut X, nut Quay lai: nen den mo dan, anh thu ve dung cho anh nho trong hop roi moi dong. */
    fun dong() {
        val h = hop ?: return
        val b = bv ?: return
        if (dangDong) return
        dangDong = true
        b.nen.animate().alpha(0f).setDuration(MS_DONG).start()
        b.thanhTren.animate().alpha(0f).setDuration(MS_DONG / 2).start()
        val xong = {
            anhNho.visibility = View.VISIBLE
            h.dismiss()
        }
        if (anhNho.isAttachedToWindow && anhNho.width > 0) {
            b.anh.thuVe(oAnhNho(b.anh), MS_DONG, xong)
        } else {
            b.anh.animate().alpha(0f).setDuration(MS_DONG).withEndAction(xong).start()
        }
    }

    /** Hop "Màn hình laptop" dong theo duong khac: dong luon, khong hieu ung. */
    fun dongNgay() {
        hop?.dismiss()
    }

    /** Cho anh nho trong hop, tinh trong toa do cua [to] (hai view nam o hai cua so khac nhau). */
    private fun oAnhNho(to: View): RectF {
        val a = IntArray(2)
        val c = IntArray(2)
        anhNho.getLocationOnScreen(a)
        to.getLocationOnScreen(c)
        val x = (a[0] - c[0]).toFloat()
        val y = (a[1] - c[1]).toFloat()
        return RectF(x, y, x + anhNho.width, y + anhNho.height)
    }

    private val le8 get() = (8 * ct.resources.displayMetrics.density).toInt()

    private companion object {
        /** Anh laptop rong 1280 diem anh, qua 4 lan vua khung la nhoe (anh Huy chot 29/10/2026). */
        const val TRAN_PHONG = 4f
        const val MS_MO = 300L
        const val MS_DONG = 280L
    }
}
