package vn.huytl.bangdieukhien.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.ListenerRegistration
import vn.huytl.bangdieukhien.R
import vn.huytl.bangdieukhien.data.Kho
import vn.huytl.bangdieukhien.data.SoSuDung
import vn.huytl.bangdieukhien.databinding.FragmentNhatKyBinding
import vn.huytl.bangdieukhien.databinding.ItemAppHomNayBinding
import vn.huytl.bangdieukhien.databinding.ItemNhatKyBinding
import java.util.Calendar

/**
 * Tab Nhat ky: chuyen gi da xay ra tren tablet. Ba the chi de doc, khong nut lenh nao: nhat
 * ky tablet ghi, dung app gi, con hoi AI gi.
 *
 * Truoc 27/9/2026 ba the nay nam cuoi tab Bang, duoi ca phan dieu khien gio lan the Viec
 * nha. Ba Huy muon tab do chi con phan dieu khien.
 *
 * Tu 8/10/2026 xem duoc [NGAY_XEM] ngay gan nhat, hang nut ngay o dau tab nhu trang "Thời gian
 * dùng app" (anh Huy chot); truoc do chi co hom nay, du nhat ky cac ngay truoc van nam tren
 * Firestore. Tablet giu nhatky/{ngay}, hoiai/{ngay} dung bay nhieu ngay roi tu xoa.
 */
class NhatKyFragment : Fragment() {

    private var _b: FragmentNhatKyBinding? = null
    private val b get() = _b!!

    private var ngheNhatKy: ListenerRegistration? = null
    private var ngheHoiAi: ListenerRegistration? = null
    private var ngheSuDung: ListenerRegistration? = null

    /** Dang xem ngay nao: 0 la hom nay, 1 la hom qua. */
    private var lui = 0

    /**
     * Qua nua dem thi nghe lai document cua ngay moi. Truoc 8/10/2026 tab chi doc ngay luc mo
     * (onStart), nen de tab mo qua nua dem thi van la nhat ky hom qua cho toi khi tab bi dung
     * roi mo lai (thay tren may ao ngay 25/9/2026).
     */
    private val tay = Handler(Looper.getMainLooper())
    private val quaNuaDem = Runnable {
        if (_b == null) return@Runnable
        ganNgay()
        henNuaDem()
    }

    /**
     * Nhat ky va so hoi AI cua ngay dang xem, giu lai de mo rong hay thu gon the ma khong
     * phai doi Firestore goi lai. Xem [veNhatKy], [veTheHoiAi].
     */
    private var nhatKy: List<String> = emptyList()
    private var moNhatKy = false
    private var hoiAiDong: List<String> = emptyList()
    private var moHoiAi = false

    /** So dung app tablet gui sang, null la chua co. Xem [veSuDung]. */
    private var soSuDung: SoSuDung? = null
    private var daNhanSuDung = false

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentNhatKyBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        lui = s?.getInt(K_LUI) ?: 0
        b.nutXemNhatKy.setOnClickListener {
            moNhatKy = !moNhatKy
            veNhatKy()
        }
        b.nutXemHoiAi.setOnClickListener {
            moHoiAi = !moHoiAi
            veTheHoiAi()
        }
        b.theSuDung.setOnClickListener {
            startActivity(
                Intent(requireContext(), SuDungActivity::class.java).putExtra(SuDungActivity.MO_NGAY, lui)
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(K_LUI, lui)
    }

    override fun onStart() {
        super.onStart()
        // Doc ngay moi lan mo tab chu khong mot lan luc tao: tab giu nguyen qua nua dem thi
        // lan mo sau phai sang document cua ngay moi. Dang mo qua nua dem thi [quaNuaDem].
        ganNgay()
        ngheSuDung = Kho.ngheSuDung(requireContext()) { so ->
            if (_b == null) return@ngheSuDung
            soSuDung = so
            daNhanSuDung = true
            veSuDung()
        }
        veSuDung()
        henNuaDem()
        hoiTablet()
    }

    override fun onStop() {
        tay.removeCallbacks(quaNuaDem)
        ngheNhatKy?.remove()
        ngheHoiAi?.remove()
        ngheSuDung?.remove()
        super.onStop()
    }

    /**
     * Nghe nhat ky va so hoi AI cua ngay [lui], ve lai hang ngay va dong dau. Goi luc mo tab,
     * luc doi ngay va luc qua nua dem: [lui] giu nguyen nen "hôm nay" thanh ngay moi.
     */
    private fun ganNgay() {
        val ct = requireContext()
        ngheNhatKy?.remove()
        ngheHoiAi?.remove()
        val ngay = Dinh.ngay(lui)
        b.tieuDeNgay.text = Dinh.tenNgay(lui).replaceFirstChar { it.uppercase() }
        b.ngayXem.text = Dinh.thuVaNgay(lui)
        veHangNgay()
        ngheNhatKy = Kho.ngheNhatKy(ct, ngay) { dong ->
            if (_b == null) return@ngheNhatKy
            nhatKy = dong
            veNhatKy()
        }
        ngheHoiAi = Kho.ngheHoiAi(ct, ngay) { dong ->
            if (_b == null) return@ngheHoiAi
            hoiAiDong = dong
            veTheHoiAi()
        }
        veNhatKy()
        veTheHoiAi()
        veSuDung()
    }

    /** Bay o ngay nhu trang "Thời gian dùng app" ([SuDungActivity.veHangNgay]). */
    private fun veHangNgay() {
        b.hangNgay.removeAllViews()
        (0 until NGAY_XEM).forEach { n ->
            val nut = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_ngay, b.hangNgay, false) as MaterialButton
            nut.text = Dinh.tenNgay(n).replaceFirstChar { it.uppercase() }
            nut.setOnClickListener { doiNgay(n) }
            toMau(nut, dangXem = n == lui)
            b.hangNgay.addView(nut)
        }
    }

    private fun toMau(nut: MaterialButton, dangXem: Boolean) {
        val ct = requireContext()
        if (dangXem) {
            nut.setBackgroundColor(ContextCompat.getColor(ct, R.color.brand))
            nut.setTextColor(Color.WHITE)
            nut.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(ct, R.color.brand))
        } else {
            nut.setBackgroundColor(Color.TRANSPARENT)
            nut.setTextColor(ContextCompat.getColor(ct, R.color.ink_soft))
            nut.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(ct, R.color.line))
        }
    }

    private fun doiNgay(n: Int) {
        if (n == lui) return
        lui = n
        // So cua ngay cu khong con dung: xoa truoc de khong ve nham trong luc cho Firestore.
        nhatKy = emptyList()
        hoiAiDong = emptyList()
        moNhatKy = false
        moHoiAi = false
        ganNgay()
    }

    /** Hen [quaNuaDem] dung luc sang ngay moi (cong mot giay cho chac da qua 0:00). */
    private fun henNuaDem() {
        tay.removeCallbacks(quaNuaDem)
        val mai = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 1)
            set(Calendar.MILLISECOND, 0)
        }
        tay.postDelayed(quaNuaDem, (mai.timeInMillis - System.currentTimeMillis()).coerceAtLeast(1_000L))
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }

    /**
     * Hoi tablet de nhat ky va so dung app la cua luc nay.
     *
     * Tablet chi day so dung app khi nhan PING, va lan day ban trang thai do cung day luon
     * nhat ky. Tab Gio choi vua hoi xong thi thoi, y nhu man Dung app gi, luc nao.
     */
    private fun hoiTablet() {
        if (System.currentTimeMillis() - Kho.pingLuc < BangFragment.GIAN_HOI_MS) return
        Kho.guiPing(requireContext())
    }

    /**
     * The nhat ky: [NHAT_KY_THU_GON] dong moi nhat, bam Xem ca ngay moi mo het.
     *
     * Tu 8/10/2026 tablet khong gioi han so dong mot ngay (truoc do 40). Hien het thi hai the
     * Dung app, Hoi AI bi day xuong tan duoi.
     *
     * Gio mot cot, chu mot cot. Tablet ghi moi dong "17:30  chu", xem DayLog ben do; dong
     * nao khong dung khuon thi hien nguyen dong o cot chu.
     */
    private fun veNhatKy() {
        val hop = b.hopNhatKy
        hop.removeAllViews()
        b.nhatKyTrong.visibility = if (nhatKy.isEmpty()) View.VISIBLE else View.GONE
        val lop = LayoutInflater.from(requireContext())
        (if (moNhatKy) nhatKy else nhatKy.takeLast(NHAT_KY_THU_GON)).forEach { d ->
            val m = KHUON_NHAT_KY.matchEntire(d)
            val dong = ItemNhatKyBinding.inflate(lop, hop, false)
            dong.gio.text = m?.groupValues?.get(1).orEmpty()
            dong.chu.text = m?.groupValues?.get(2) ?: d
            hop.addView(dong.root)
        }
        b.nutXemNhatKy.visibility = if (nhatKy.size > NHAT_KY_THU_GON) View.VISIBLE else View.GONE
        b.nutXemNhatKy.text =
            if (moNhatKy) getString(R.string.bang_thu_gon)
            else getString(R.string.bang_xem_ca_ngay, nhatKy.size)
    }

    /** The Hoi AI: [HOI_AI_THU_GON] cau moi nhat, bam moi mo het. */
    private fun veTheHoiAi() {
        val ct = requireContext()
        b.hoiAi.text = veHoiAi(if (moHoiAi) hoiAiDong else hoiAiDong.takeLast(HOI_AI_THU_GON))
        b.hoiAi.setTextColor(
            ContextCompat.getColor(ct, if (hoiAiDong.isEmpty()) R.color.ink_soft else R.color.ink)
        )
        b.soHoiAi.text =
            if (hoiAiDong.isEmpty()) "" else getString(R.string.bang_so_cau, hoiAiDong.size)
        b.nutXemHoiAi.visibility = if (hoiAiDong.size > HOI_AI_THU_GON) View.VISIBLE else View.GONE
        b.nutXemHoiAi.text =
            if (moHoiAi) getString(R.string.bang_thu_gon)
            else getString(R.string.bang_xem_het_hoi_ai, hoiAiDong.size)
    }

    /**
     * The "Hoi AI" cua ngay dang xem: moi cau hai phan, gio va ten app nhat o tren, cau con
     * go o duoi, xuong dong dung cho con xuong dong. Hai cau ngan nhau bang mot dong trong.
     *
     * Tablet ghi moi cau thanh mot dong, [DongHoiAi] tach ra. Dong nao khong dung khuon
     * thi hien nguyen dong chu khong bo, de khong mat chu nao cua con.
     *
     * Ngay tren dong da bo: the nay chi co mot ngay (ngay dang xem o hang ngay), ma document
     * tren Firestore cung dat ten theo ngay.
     */
    private fun veHoiAi(dong: List<String>): CharSequence {
        if (dong.isEmpty()) return getString(R.string.bang_chua_hoi_ai)
        val nhat = ContextCompat.getColor(requireContext(), R.color.ink_soft)
        val sb = SpannableStringBuilder()
        dong.forEach { d ->
            if (sb.isNotEmpty()) sb.append("\n\n")
            val c = DongHoiAi.tach(d)
            if (c == null) {
                sb.append(DongHoiAi.traXuongDong(d))
                return@forEach
            }
            val dau = sb.length
            sb.append("${c.gio} · ${c.app}")
            sb.setSpan(ForegroundColorSpan(nhat), dau, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.setSpan(RelativeSizeSpan(0.9f), dau, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.append("\n").append(c.cau)
        }
        return sb
    }

    /**
     * The "Dung app": tong thoi gian o dong ten the, ba app lau nhat moi app mot dong, va
     * dai gio cua ngay dang xem (so nay tablet giu bay ngay tu truoc 8/10/2026).
     *
     * Tablet chi gui so nay khi nhan PING, xem [hoiTablet]. Nen ban hien dau tien la ban
     * cua lan hoi truoc, vai giay sau moi den ban moi.
     */
    private fun veSuDung() {
        val s = soSuDung
        val dau = SoSuDung.dauNgay(lui)
        val cuoi = SoSuDung.dauNgay(lui - 1)
        val cac = s?.theoApp(dau, cuoi).orEmpty()
        val coSo = s != null && cac.isNotEmpty()
        b.suDungTong.text = if (s != null && coSo) Dinh.doDai(s.tongMs(dau, cuoi)) else ""
        b.suDungChu.visibility = if (coSo) View.GONE else View.VISIBLE
        b.suDungApp.visibility = if (coSo) View.VISIBLE else View.GONE
        b.khoiDai.visibility = if (coSo) View.VISIBLE else View.GONE
        if (s == null || cac.isEmpty()) {
            b.suDungChu.text = when {
                s == null ->
                    getString(if (daNhanSuDung) R.string.su_dung_chua_gui else R.string.su_dung_dang_lay)
                else -> Dinh.viSaoTrong(s, dau, laHomNay = lui == 0, ten = Dinh.tenNgay(lui))
            }
            return
        }

        val ct = requireContext()
        val lop = LayoutInflater.from(ct)
        b.suDungApp.removeAllViews()
        cac.take(3).forEach { app ->
            val d = ItemAppHomNayBinding.inflate(lop, b.suDungApp, false)
            d.tenApp.text = app.ten
            d.baoLau.text = Dinh.doDai(app.tongMs)
            b.suDungApp.addView(d.root)
        }
        if (cac.size > 3) {
            val d = ItemAppHomNayBinding.inflate(lop, b.suDungApp, false)
            d.tenApp.text = "${cac.size - 3} app khác"
            d.tenApp.setTextColor(ContextCompat.getColor(ct, R.color.ink_soft))
            d.baoLau.text = Dinh.doDai(cac.drop(3).sumOf { it.tongMs })
            b.suDungApp.addView(d.root)
        }
        b.suDungDai.dat(
            dauNgay = dau,
            cac = s.cuaNgay(dau, cuoi).map { it.tu to it.den },
            mauVach = ContextCompat.getColor(ct, R.color.brand),
            mauNen = ContextCompat.getColor(ct, R.color.line),
            mauGio = ContextCompat.getColor(ct, R.color.surface)
        )
    }

    companion object {
        /** Mot dong nhat ky tablet ghi: gio, hai dau cach, chu. Xem DayLog ben homework-gate. */
        private val KHUON_NHAT_KY = Regex("""^(\d{1,2}:\d{2}) {2}(.*)$""")

        /** The nhat ky, the Hoi AI luc thu gon hien bay nhieu dong moi nhat. */
        private const val NHAT_KY_THU_GON = 8
        private const val HOI_AI_THU_GON = 3

        /**
         * So ngay xem duoc, tinh ca hom nay. Bang so ngay tablet giu nhatky/, hoiai/ tren
         * Firestore (DayLog.NGAY_GIU_TREN_MANG ben homework-gate) va bang trang "Thời gian dùng
         * app" (SoSuDung.GIU_NGAY_MAC_DINH).
         */
        private const val NGAY_XEM = 7

        private const val K_LUI = "lui"
    }
}
