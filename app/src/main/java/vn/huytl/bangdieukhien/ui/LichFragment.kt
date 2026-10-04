package vn.huytl.bangdieukhien.ui

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextPaint
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.ListenerRegistration
import vn.huytl.bangdieukhien.R
import vn.huytl.bangdieukhien.data.Buoi
import vn.huytl.bangdieukhien.data.Kho
import vn.huytl.bangdieukhien.data.LichDangDung
import vn.huytl.bangdieukhien.data.NgayNghi
import vn.huytl.bangdieukhien.data.ThoiKhoaBieu
import vn.huytl.bangdieukhien.databinding.FragmentLichBinding
import vn.huytl.bangdieukhien.ui.LichHoc.Hang
import java.util.Calendar
import kotlin.math.abs

/**
 * Tab Lich hoc: thoi khoa bieu ca tuan cua Le Hoa, chi de xem. Them ngay 29/9/2026.
 *
 * Luoi ve y nhu man Thoi khoa bieu ben tablet (LichActivity): cot la thu, hang la tiet,
 * ke net dut, chu mau cho nam mon chinh, to nen cac o cua buoi toi. Khac ben do o cho o
 * hep hon nhieu: ten mon dai viet gon ([LichHoc.tenNgan]), nhan giu o do thi hien ten du.
 *
 * Lich nghe tu Firestore tu 4/10/2026 (xem [LichDangDung]): ban moi toi thi ve lai ngay, mat
 * mang thi dung ban luu trong may. Tab dang mo thi ve lai moi phut, vi the buoi toi phai doi
 * dung luc vao hoc, tan hoc.
 */
class LichFragment : Fragment() {

    private var _b: FragmentLichBinding? = null
    private val b get() = _b!!

    /** Cac o co ten mon hay ten ky nghi cua lan ve vua roi, xem [datCoChu]. */
    private val oChu = mutableListOf<TextView>()

    private var ngheLich: ListenerRegistration? = null

    private val nhip = Runnable {
        if (_b == null) return@Runnable
        ve()
        henPhutSau()
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentLichBinding.inflate(i, c, false)
        return b.root
    }

    override fun onStart() {
        super.onStart()
        val ct = requireContext()
        LichDangDung.napTuMay(ct)
        ve()
        henPhutSau()
        ngheLich = Kho.ngheLich(ct) { json ->
            if (_b != null && LichDangDung.nhan(ct, json) is LichDangDung.KetQua.Doi) ve()
        }
    }

    override fun onStop() {
        ngheLich?.remove()
        ngheLich = null
        b.root.removeCallbacks(nhip)
        super.onStop()
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }

    /** Ve lai ngay sau dau phut ke tiep: moi moc trong thoi khoa bieu deu tron phut. */
    private fun henPhutSau() {
        b.root.removeCallbacks(nhip)
        b.root.postDelayed(nhip, 60_000L - System.currentTimeMillis() % 60_000L + 200L)
    }

    private fun ve() {
        val now = Calendar.getInstance()
        val toi = LichHoc.buoiToi(now)
        val tuan = LichHoc.tuanXem(now, toi)
        b.tuanXem.text = "${LichHoc.ngayThang(tuan.first())} – ${LichHoc.ngayThang(tuan.last())}"

        b.theBuoiToi.visibility = if (toi == null) View.GONE else View.VISIBLE
        if (toi != null) {
            b.nhanBuoiToi.setText(if (toi.dangHoc) R.string.lich_dang_hoc else R.string.lich_buoi_toi)
            b.tenBuoiToi.text = LichHoc.tenBuoi(toi, now)
            b.chiTietBuoiToi.text = LichHoc.chiTiet(toi)
        }

        val hang = LichHoc.dungHang(tuan)
        veCotLe(hang)
        oChu.clear()
        b.luoi.removeAllViews()
        tuan.forEach { b.luoi.addView(veCot(it, hang, now, toi)) }
        datCoChuTruocKhiVe()
    }

    /**
     * Chay [datCoChu] ngay truoc lan ve dau tien cua luoi moi: luc do moi biet o rong bao
     * nhieu. Co chu doi thi bo lan ve do, luoi ve lai ngay voi co moi, khong thay chu nhay.
     */
    private fun datCoChuTruocKhiVe() {
        val luoi = b.luoi
        luoi.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                luoi.viewTreeObserver.removeOnPreDrawListener(this)
                return _b == null || !datCoChu()
            }
        })
    }

    /**
     * Mot co chu chung cho moi o mon: lon nhat 12sp ma chu dai nhat van vua mot dong, va hai
     * dong van vua chieu cao o. Tra ve true khi co chu doi.
     *
     * Khong de tung o tu co chu (autoSize): o chu dai nho han o chu ngan, ma o hai chu van
     * gay giua chu, "Trải n" voi "ghiệm", vi nhu vay van vua hai dong. Gap may 360dp dat
     * chu to 1,3 lan la thay.
     */
    private fun datCoChu(): Boolean {
        val mot = oChu.firstOrNull() ?: return false
        val rong = mot.width - mot.paddingLeft - mot.paddingRight
        if (rong <= 0) return false
        val tran = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, resources.displayMetrics)
        // Do bang chu dam: o cua buoi toi viet dam, rong hon chu thuong.
        val but = TextPaint(mot.paint).apply {
            textSize = tran
            typeface = Typeface.create(mot.typeface, Typeface.BOLD)
        }
        val dai = oChu.flatMap { it.text.split(' ', '\n') }.maxOf { but.measureText(it) }
        val dong = but.fontMetrics.let { it.descent - it.ascent } / tran
        val cao = resources.getDimension(R.dimen.cao_o_lich) - mot.lineSpacingExtra
        val co = minOf(tran, tran * rong / dai, cao / (2 * dong))
        if (abs(co - mot.textSize) < 0.5f) return false
        oChu.forEach { it.setTextSize(TypedValue.COMPLEX_UNIT_PX, co) }
        return true
    }

    /** Cot le trai: ten buoi, va so tiet kem gio vao tiet. */
    private fun veCotLe(hang: List<Hang>) {
        val cot = b.cotLe
        cot.removeAllViews()
        // Chua cho dau cot cua cac ngay, de hang dau cot le thang hang dau cac cot ben phai.
        cot.addView(View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.cao_dau_cot)
            )
            background = ke(veTrai = false)
        })

        hang.forEachIndexed { i, h ->
            val o = taoO(cot, caoCua(h))
            o.background = ke(veTrai = false, veDuoi = i != hang.lastIndex)
            // Cot trai chu nho hon o mon, va tu nho lai khi khong vua: chu to 1,3 lan tren may
            // 360dp thi "CHIỀU" bi cat.
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(o, 7, 11, 1, TypedValue.COMPLEX_UNIT_SP)
            when (h) {
                is Hang.Bang -> {
                    o.maxLines = 1
                    o.setText(if (h.buoi == Buoi.SANG) R.string.lich_sang else R.string.lich_chieu)
                    o.setTextColor(mau(R.color.brand_dark))
                    o.letterSpacing = 0.06f
                    o.setTypeface(null, Typeface.BOLD)
                }
                is Hang.Tiet -> {
                    o.text = "tiết ${h.tiet}\n${Dinh.gio(ThoiKhoaBieu.gioTiet(h.buoi, h.tiet))}"
                    o.setTextColor(mau(R.color.ink_soft))
                }
            }
            cot.addView(o)
        }
    }

    private fun veCot(ngay: Calendar, hang: List<Hang>, now: Calendar, toi: LichHoc.BuoiToi?): View {
        val cot = LayoutInflater.from(requireContext()).inflate(R.layout.cot_lich, b.luoi, false)
        cot.findViewById<View>(R.id.dauCot).background = ke(veTrai = true)

        val homNay = LichHoc.cungNgay(ngay, now)
        val mauDau = mau(if (homNay) R.color.ok else R.color.brand_dark)
        cot.findViewById<TextView>(R.id.nhanThu).apply {
            text = LichHoc.dauCot(ngay)
            setTextColor(mauDau)
        }
        cot.findViewById<TextView>(R.id.nhanNgay).apply {
            text = LichHoc.ngayThang(ngay)
            if (homNay) setTextColor(mauDau)
        }

        val khung = cot.findViewById<LinearLayout>(R.id.danhSachO)
        val nghi = NgayNghi.tenKyNghi(ngay)
        if (nghi == null) veNgayHoc(khung, ngay, hang, toi) else veNgayNghi(khung, hang, nghi)
        return cot
    }

    private fun veNgayHoc(khung: LinearLayout, ngay: Calendar, hang: List<Hang>, toi: LichHoc.BuoiToi?) {
        val thu = ngay.get(Calendar.DAY_OF_WEEK)
        val cacBuoi = ThoiKhoaBieu.buoiHocCua(thu)
        val maToi = toi?.let { LichHoc.maBuoi(it.ngay, it.buoi.buoi) }

        hang.forEachIndexed { i, h ->
            val cuoi = i == hang.lastIndex
            val o = taoO(khung, caoCua(h))
            when (h) {
                // Dai SANG/CHIEU: ke net ngang duoi nhung KHONG ke net doc. Net doc dut mot
                // doan o day chinh la cho tach hai buoi, y nhu luoi ben tablet.
                is Hang.Bang -> o.background = ke(veTrai = false, veDuoi = !cuoi)
                is Hang.Tiet -> {
                    val mon = cacBuoi.firstOrNull { it.buoi == h.buoi }?.monTheoTiet?.get(h.tiet)
                    val laToi = mon != null && LichHoc.maBuoi(ngay, h.buoi) == maToi
                    // Dang hoc thi o cua tiet dang hoc dam hon ca buoi.
                    val tietNay = laToi && toi?.dangHoc == true && toi.tiet == h.tiet
                    val nen = when {
                        tietNay -> ColorUtils.setAlphaComponent(mau(R.color.brand), 72)
                        laToi -> ColorUtils.setAlphaComponent(mau(R.color.brand), 34)
                        else -> Color.TRANSPARENT
                    }
                    o.background = ke(veTrai = true, veDuoi = !cuoi, nen = nen)
                    if (mon != null) {
                        val gon = LichHoc.tenNgan(mon)
                        o.text = gon
                        oChu += o
                        // Mau o chu va chi cho nam mon chinh, nhu ben tablet: to mau ca bay
                        // mon thi doc mot ten phai luot qua bay mau.
                        o.setTextColor(mau(MAU_MON[mon] ?: if (laToi) R.color.brand_dark else R.color.ink_soft))
                        if (laToi) o.setTypeface(null, Typeface.BOLD)
                        if (gon != mon) o.tooltipText = mon
                        o.contentDescription = "${ThoiKhoaBieu.tenThu(thu)}, tiết ${h.tiet}: $mon"
                    }
                }
            }
            khung.addView(o)
        }
    }

    /**
     * Ngay nghi: cac tiet lien nhau cua moi buoi gop thanh mot o, ten ky nghi ghi vao o dau.
     * Mot o tiet chi du hai dong chu, ma ten ky nghi dai ("Nghỉ Tết Nguyên đán").
     */
    private fun veNgayNghi(khung: LinearLayout, hang: List<Hang>, nghi: String) {
        var daGhi = false
        var i = 0
        while (i < hang.size) {
            val h = hang[i]
            if (h is Hang.Bang) {
                val o = taoO(khung, caoCua(h))
                o.background = ke(veTrai = false, veDuoi = i != hang.lastIndex)
                khung.addView(o)
                i++
                continue
            }
            var het = i
            while (het < hang.size && hang[het] is Hang.Tiet) het++
            val o = taoO(khung, (het - i) * resources.getDimensionPixelSize(R.dimen.cao_o_lich))
            o.background = ke(veTrai = true, veDuoi = het != hang.size)
            if (!daGhi) {
                // Bo chu "nghỉ" co san trong ten ky nghi, khong thi ra "Nghỉ nghỉ Tết".
                o.text = "Nghỉ ${nghi.removePrefix("nghỉ ")}"
                o.maxLines = 2 * (het - i) + 1
                o.setTextColor(mau(R.color.ink_soft))
                oChu += o
                daGhi = true
            }
            khung.addView(o)
            i = het
        }
    }

    private fun caoCua(h: Hang): Int = resources.getDimensionPixelSize(
        if (h is Hang.Bang) R.dimen.cao_dai_buoi else R.dimen.cao_o_lich
    )

    private fun taoO(cha: ViewGroup, cao: Int): TextView =
        (LayoutInflater.from(requireContext()).inflate(R.layout.o_lich, cha, false) as TextView).apply {
            layoutParams = layoutParams.apply { height = cao }
        }

    private fun mau(id: Int) = ContextCompat.getColor(requireContext(), id)

    /** Mot o co ke net dut, xem [KeDut]. [nen] khac trong suot la o cua buoi toi. */
    private fun ke(veTrai: Boolean, veDuoi: Boolean = true, nen: Int = Color.TRANSPARENT) = KeDut(
        mau = mau(R.color.ke_luoi),
        doDay = resources.displayMetrics.density,
        veDuoi = veDuoi,
        veTrai = veTrai,
        nen = nen
    )

    private companion object {
        /** Nam mon chinh, cung mau voi ben tablet (MatMon.DAT_TAY). */
        val MAU_MON = mapOf(
            "Toán" to R.color.mon_toan,
            "Khoa học tự nhiên" to R.color.mon_khtn,
            "Ngữ văn" to R.color.mon_van,
            "Lịch sử - Địa lý" to R.color.mon_su_dia,
            "Tiếng Anh" to R.color.mon_anh
        )
    }
}
