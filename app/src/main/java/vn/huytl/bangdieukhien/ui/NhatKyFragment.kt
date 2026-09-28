package vn.huytl.bangdieukhien.ui

import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.ListenerRegistration
import vn.huytl.bangdieukhien.R
import vn.huytl.bangdieukhien.data.Kho
import vn.huytl.bangdieukhien.data.SoSuDung
import vn.huytl.bangdieukhien.databinding.FragmentNhatKyBinding
import vn.huytl.bangdieukhien.databinding.ItemAppHomNayBinding
import vn.huytl.bangdieukhien.databinding.ItemNhatKyBinding

/**
 * Tab Nhat ky: chuyen gi da xay ra tren tablet hom nay. Ba the chi de doc, khong nut
 * lenh nao: nhat ky tablet ghi, dung app gi, con hoi AI gi.
 *
 * Truoc 27/9/2026 ba the nay nam cuoi tab Bang, duoi ca phan dieu khien gio lan the Viec
 * nha. Ba Huy muon tab do chi con phan dieu khien.
 */
class NhatKyFragment : Fragment() {

    private var _b: FragmentNhatKyBinding? = null
    private val b get() = _b!!

    private var ngheNhatKy: ListenerRegistration? = null
    private var ngheHoiAi: ListenerRegistration? = null
    private var ngheSuDung: ListenerRegistration? = null

    /**
     * Nhat ky va so hoi AI cua hom nay, giu lai de mo rong hay thu gon the ma khong
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
        b.nutXemNhatKy.setOnClickListener {
            moNhatKy = !moNhatKy
            veNhatKy()
        }
        b.nutXemHoiAi.setOnClickListener {
            moHoiAi = !moHoiAi
            veTheHoiAi()
        }
        b.theSuDung.setOnClickListener {
            startActivity(Intent(requireContext(), SuDungActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        val ct = requireContext()
        // Doc ngay moi lan mo tab chu khong mot lan luc tao: tab giu nguyen qua nua dem thi
        // lan mo sau phai sang document cua ngay moi.
        val ngay = Dinh.homNay()
        b.ngayXem.text = Dinh.thuVaNgay(0)
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
        ngheSuDung = Kho.ngheSuDung(ct) { so ->
            if (_b == null) return@ngheSuDung
            soSuDung = so
            daNhanSuDung = true
            veSuDung()
        }
        veNhatKy()
        veTheHoiAi()
        veSuDung()
        hoiTablet()
    }

    override fun onStop() {
        ngheNhatKy?.remove()
        ngheHoiAi?.remove()
        ngheSuDung?.remove()
        super.onStop()
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
     * Tablet giu toi bon muoi dong mot ngay. Hien het thi hai the Dung app, Hoi AI bi day
     * xuong tan duoi.
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
     * The "Hoi AI hom nay": moi cau hai phan, gio va ten app nhat o tren, cau con go
     * o duoi, xuong dong dung cho con xuong dong. Hai cau ngan nhau bang mot dong trong.
     *
     * Tablet ghi moi cau thanh mot dong, [DongHoiAi] tach ra. Dong nao khong dung khuon
     * thi hien nguyen dong chu khong bo, de khong mat chu nao cua con.
     *
     * Ngay tren dong da bo: the nay chi co hom nay, ma document tren Firestore cung
     * dat ten theo ngay.
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
     * dai gio cua hom nay.
     *
     * Tablet chi gui so nay khi nhan PING, xem [hoiTablet]. Nen ban hien dau tien la ban
     * cua lan hoi truoc, vai giay sau moi den ban moi.
     */
    private fun veSuDung() {
        val s = soSuDung
        val dau = SoSuDung.dauNgay(0)
        val cuoi = SoSuDung.dauNgay(-1)
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
                else -> Dinh.viSaoTrong(s, dau, laHomNay = true, ten = "hôm nay")
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
    }
}
