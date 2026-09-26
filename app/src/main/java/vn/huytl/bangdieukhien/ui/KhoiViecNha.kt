package vn.huytl.bangdieukhien.ui

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.ListenerRegistration
import vn.huytl.bangdieukhien.R
import vn.huytl.bangdieukhien.data.Duong
import vn.huytl.bangdieukhien.data.Kho
import vn.huytl.bangdieukhien.data.Nguoi
import vn.huytl.bangdieukhien.data.ViecNha
import vn.huytl.bangdieukhien.databinding.DongSuaViecBinding
import vn.huytl.bangdieukhien.databinding.FragmentBangBinding
import vn.huytl.bangdieukhien.databinding.ItemViecBinding

/**
 * The Viec nha o man Bang: giao viec, bam xong, sua danh sach viec.
 *
 * Y het khoi viec nha tren may ba noi, va chung mot document: ba giao ben do thi the
 * nay hien ngay, Ba Huy bam xong o day thi may ba cung thay ngay. Moi lan bam la mot
 * transaction, xem [Kho.giaoViec].
 *
 * Tach khoi BangFragment vi khoi nay tu nghe hai document rieng va tu giu trang thai
 * rieng, khong dung chung gi voi phan lenh. BangFragment tao no trong onViewCreated,
 * bat nghe trong onStart, va goi [bo] trong onDestroyView.
 */
class KhoiViecNha(private val ct: Context, private val b: FragmentBangBinding) {

    /** Dot dang giao, doc tu Firestore. null la khong co dot nao. */
    private var dot: ViecNha.Dot? = null

    /** Danh sach chung tren Firestore. null la chua co, dung [ViecNha.MAC_DINH]. */
    private var danhSachChung: List<ViecNha.Viec>? = null

    private var ngheDot: ListenerRegistration? = null
    private var ngheDanhSach: ListenerRegistration? = null

    /**
     * Firestore da tra ban dau tien cua hai document chua.
     *
     * Chua thi chua ve danh sach nao: ve truoc la hien danh sach de chon, roi mot nhip
     * sau doi sang dot dang chay va ca khoi nhay len tren - dung luc Ba Huy dang dua
     * tay toi mot nut.
     */
    private var daCoDot = false
    private var daCoDanhSach = false

    /** Cac viec dang tich de giao, khi chua co dot nao chay. */
    private val daChon = linkedSetOf<String>()

    private var dangGui = false
    private var loi = ""

    /**
     * View cua man hinh con khong. Cau tra loi cua transaction co the ve sau khi
     * BangFragment da bo view, luc do dung vao [b] la dung vao view da chet.
     */
    private var conSong = true

    init {
        b.nutSuaViec.setOnClickListener { hoiSuaDanhSach() }
        ve()
    }

    fun batNghe() {
        goNghe()
        ngheDot = Kho.ngheViecNha(ct) { moi ->
            if (!conSong) return@ngheViecNha
            // Sang dot khac thi cau bao hong cua lan bam truoc khong con noi ve cai gi
            // tren man hinh nua.
            if (moi?.maPhien != dot?.maPhien && !dangGui) loi = ""
            dot = moi
            daCoDot = true
            ve()
        }
        ngheDanhSach = Kho.ngheDanhSachViec(ct) { ds ->
            if (!conSong) return@ngheDanhSachViec
            danhSachChung = ds
            daCoDanhSach = true
            ve()
        }
        // Chua ghep nha thi khong co gi de nghe, va cung khong co gi de cho.
        if (ngheDot == null) daCoDot = true
        if (ngheDanhSach == null) daCoDanhSach = true
        ve()
    }

    fun goNghe() {
        ngheDot?.remove()
        ngheDot = null
        ngheDanhSach?.remove()
        ngheDanhSach = null
    }

    /** Man hinh bo view. Tu day khong ve gi nua. */
    fun bo() {
        conSong = false
        goNghe()
    }

    // ------------------------------------------------------------------- ve

    private fun ve() {
        if (!conSong) return
        if (!daCoDot || !daCoDanhSach) {
            b.hopViecNha.removeAllViews()
            veChu("", canhBao = false)
            b.nutGiaoViec.visibility = View.GONE
            b.nutBoHetViec.visibility = View.GONE
            b.nutSuaViec.visibility = View.GONE
            return
        }
        val d = dot?.takeIf { it.cac.isNotEmpty() }
        xepLai(dangCoDot = d != null)
        b.hopViecNha.removeAllViews()
        if (d == null) veChon() else veDot(d)
    }

    /** Chua co dot nao: danh sach de tich, va nut giao. */
    private fun veChon() {
        val ds = danhSach()
        daChon.retainAll(ds.map { it.ten }.toSet())
        veChu(
            when {
                loi.isNotEmpty() -> loi
                danhSachChung == null -> ct.getString(R.string.viec_chon_mac_dinh)
                else -> ct.getString(R.string.viec_chon)
            },
            canhBao = loi.isNotEmpty()
        )
        ds.forEach { v ->
            themDong(
                ten = v.ten,
                phu = Dinh.phut(v.phut),
                nhanChinh = ct.getString(
                    if (v.ten in daChon) R.string.viec_bo_chon else R.string.viec_chon_nut
                ),
                chinhMo = !dangGui,
                khiChinh = {
                    if (!daChon.add(v.ten)) daChon.remove(v.ten)
                    ve()
                }
            )
        }
        val phut = ds.filter { it.ten in daChon }.sumOf { it.phut }
        b.nutGiaoViec.visibility = if (daChon.isEmpty()) View.GONE else View.VISIBLE
        b.nutGiaoViec.isEnabled = !dangGui
        b.nutGiaoViec.text =
            if (dangGui) ct.getString(R.string.viec_dang_gui)
            else ct.getString(R.string.viec_giao_nut, daChon.size, phut)
        // Gan lai moi lan ve: canh dot xong het cung dung nut nay cho viec Gui lai.
        b.nutGiaoViec.setOnClickListener { giao() }
        b.nutBoHetViec.visibility = View.GONE
        b.nutSuaViec.visibility = View.VISIBLE
        b.nutSuaViec.isEnabled = !dangGui
    }

    /**
     * Dang co dot viec: tung viec voi nut Xong va Bo.
     *
     * Xong het ma document con do nghia la tablet chua nhan. Qua nua tieng thi tablet
     * se khong tu nhan nua, va dong chu doi mau de Ba Huy bam Gui lai.
     */
    private fun veDot(d: ViecNha.Dot) {
        val choNhan = d.xongHet
        val boQua = d.tabletDaBoQua()
        val nguoi = if (d.ai == Nguoi.BA_HUY) ct.getString(R.string.parent_name) else "Bà nội"
        veChu(
            when {
                loi.isNotEmpty() -> loi
                boQua -> ct.getString(R.string.viec_bi_bo_qua, Dinh.lucNgan(d.luc), d.tongPhut)
                choNhan -> ct.getString(R.string.viec_cho_nhan, Dinh.lucNgan(d.luc), d.tongPhut)
                else -> ct.getString(R.string.viec_dang_lam, nguoi)
            },
            canhBao = loi.isNotEmpty() || boQua
        )
        d.cac.forEach { v ->
            themDong(
                ten = v.ten,
                phu = if (v.xong) ct.getString(R.string.viec_da_xong) else Dinh.phut(v.phut),
                nhanChinh = ct.getString(if (v.xong) R.string.viec_da_xong else R.string.viec_xong),
                chinhMo = !v.xong && !dangGui,
                khiChinh = { xong(d.maPhien, v.ten) },
                nhanPhu = if (v.xong) null else ct.getString(R.string.viec_bo),
                khiPhu = { boViec(d.maPhien, v.ten) }
            )
        }
        if (choNhan) {
            b.nutGiaoViec.visibility = View.VISIBLE
            b.nutGiaoViec.isEnabled = !dangGui
            b.nutGiaoViec.text = ct.getString(
                if (dangGui) R.string.viec_dang_gui else R.string.viec_gui_lai
            )
            b.nutGiaoViec.setOnClickListener { guiLai(d.maPhien) }
            b.nutBoHetViec.visibility = View.GONE
        } else {
            b.nutGiaoViec.visibility = View.GONE
            b.nutBoHetViec.visibility = View.VISIBLE
            b.nutBoHetViec.isEnabled = !dangGui
            b.nutBoHetViec.setOnClickListener { hoiBoHet(d.maPhien) }
        }
        // Sua danh sach khong dong gi toi dot dang chay, de o day chi them roi mat.
        b.nutSuaViec.visibility = View.GONE
    }

    private fun veChu(chu: String, canhBao: Boolean) {
        b.chuViecNha.text = chu
        b.chuViecNha.setTextColor(
            ContextCompat.getColor(ct, if (canhBao) R.color.alert else R.color.ink_soft)
        )
    }

    private fun themDong(
        ten: String,
        phu: String,
        nhanChinh: String,
        chinhMo: Boolean,
        khiChinh: () -> Unit,
        nhanPhu: String? = null,
        khiPhu: (() -> Unit)? = null
    ) {
        val d = ItemViecBinding.inflate(LayoutInflater.from(ct), b.hopViecNha, false)
        d.tenViec.text = ten
        d.phuViec.text = phu
        d.nutChinh.text = nhanChinh
        d.nutChinh.isEnabled = chinhMo
        d.nutChinh.setOnClickListener { khiChinh() }
        if (nhanPhu != null && khiPhu != null) {
            d.nutPhu.visibility = View.VISIBLE
            d.nutPhu.text = nhanPhu
            d.nutPhu.isEnabled = !dangGui
            d.nutPhu.setOnClickListener { khiPhu() }
        }
        b.hopViecNha.addView(d.root)
    }

    /**
     * Dang co dot viec thi day ca nhan lan the len ngay duoi cac the bao o dau man
     * (bai cho duyet, vo dan do).
     *
     * Luc do tablet dang khoa vi viec nha, va cai Ba Huy can bam la nut Xong - khong
     * the de no nam duoi ca hang nut cho gio va tin cua co. Het dot thi tra ve cho cu,
     * sau cac nut lenh. May ba cung day khoi viec nha len y nhu vay.
     */
    private fun xepLai(dangCoDot: Boolean) {
        val cot = b.theViecNha.parent as? LinearLayout ?: return
        val moc = if (dangCoDot) b.theVoDanDo else b.chuGui
        // Dung cho roi thi thoi: doi cho view moi lan ve lai la moi lan man hinh nhay.
        if (cot.indexOfChild(b.nhanViecNha) == cot.indexOfChild(moc) + 1) return
        cot.removeView(b.nhanViecNha)
        cot.removeView(b.theViecNha)
        val dat = cot.indexOfChild(moc) + 1
        cot.addView(b.nhanViecNha, dat)
        cot.addView(b.theViecNha, dat + 1)
    }

    /**
     * Danh sach de chon. Cat o [Duong.TOI_DA_VIEC] cho giong may ba: may ba ban cu
     * co the da gui len mot danh sach dai hon, tu luc chua co tran nay.
     */
    private fun danhSach(): List<ViecNha.Viec> =
        (danhSachChung ?: ViecNha.MAC_DINH).take(Duong.TOI_DA_VIEC)

    // ------------------------------------------------------------------ bam

    private fun giao() {
        if (dangGui || daChon.isEmpty()) return
        val cac = danhSach().filter { it.ten in daChon }
        // Giao hong thi giu nguyen cac viec da chon, de chi viec bam lai.
        gui({ xong -> Kho.giaoViec(ct, cac, xong) }) { daChon.clear() }
    }

    private fun xong(maPhien: String, ten: String) =
        gui({ xong -> Kho.xongViec(ct, maPhien, ten, xong) })

    private fun boViec(maPhien: String, ten: String) =
        gui({ xong -> Kho.boViec(ct, maPhien, ten, xong) })

    private fun guiLai(maPhien: String) =
        gui({ xong -> Kho.guiLaiViec(ct, maPhien, xong) })

    /**
     * Hoi truoc khi bo het.
     *
     * Bo het la tablet mo khoa ngay ma khong cong phut nao, ke ca viec da xong. Bam
     * nham mot lan la Le Hoa khoi lam, va khong co nut nao lay lai dot vua bo.
     */
    private fun hoiBoHet(maPhien: String) {
        MaterialAlertDialogBuilder(ct)
            .setTitle(R.string.viec_bo_het_hoi)
            .setMessage(R.string.viec_bo_het_hoi_them)
            .setPositiveButton(R.string.viec_bo_het) { _, _ ->
                gui({ xong -> Kho.boHetViec(ct, maPhien, xong) })
            }
            .setNegativeButton(R.string.huy, null)
            .show()
    }

    /**
     * Gui mot lan bam, va giu the o canh "dang gui" cho toi luc xong.
     *
     * KHONG SUA [dot] O DAY, ke ca khi gui duoc: listener se mang ban moi ve. Tu sua
     * theo cai vua gui thi co luc sai - bam xong viec cuoi la tablet khep dot va xoa
     * document ngay, va tin "da xoa" do co the ve truoc cau tra loi cua transaction.
     * Luc ay the giu mai mot dot khong con nua.
     */
    private fun gui(viec: ((Kho.KetQua) -> Unit) -> Unit, khiXong: () -> Unit = {}) {
        if (dangGui) return
        dangGui = true
        loi = ""
        ve()
        viec { kq ->
            dangGui = false
            if (!conSong) return@viec
            when (kq) {
                is Kho.KetQua.Xong -> khiXong()
                is Kho.KetQua.Hong -> loi = kq.viSao
            }
            ve()
        }
    }

    // --------------------------------------------------------- sua danh sach

    /**
     * Sua danh sach viec chung: ten va so phut tung viec.
     *
     * Truoc day hop nay nam tren may ba, va Ba Huy phai cam may ba len moi sua duoc.
     * Luu o day thi may ba doi theo ngay, vi no nghe hop/danhsachviec.
     *
     * Nut Luu khong dong hop khi con dong sai: dong lai la mat het chu vua go.
     */
    private fun hoiSuaDanhSach() {
        val dem = ct.resources.displayMetrics.density
        val cot = LinearLayout(ct).apply {
            orientation = LinearLayout.VERTICAL
            val p = (20 * dem).toInt()
            setPadding(p, (8 * dem).toInt(), p, 0)
        }
        cot.addView(TextView(ct).apply {
            text = ct.getString(
                R.string.viec_sua_huong_dan, Duong.TOI_DA_VIEC, ViecNha.PHUT_TOI_DA
            )
            setTextColor(ContextCompat.getColor(ct, R.color.ink_soft))
            textSize = 14f
            setPadding(0, 0, 0, (12 * dem).toInt())
        })
        // Loi chung cho ca danh sach (trong het, dai qua) hien o day, tren cac dong.
        val chuLoi = TextView(ct).apply {
            setTextColor(ContextCompat.getColor(ct, R.color.alert))
            textSize = 14f
            visibility = View.GONE
            setPadding(0, 0, 0, (8 * dem).toInt())
        }
        cot.addView(chuLoi)

        val cacDong = mutableListOf<DongSuaViecBinding>()
        val nutThem = MaterialButton(
            ct, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = ct.getString(R.string.viec_them)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        fun themHang(ten: String, phut: String): DongSuaViecBinding {
            val d = DongSuaViecBinding.inflate(LayoutInflater.from(ct), cot, false)
            d.oTen.setText(ten)
            d.oPhut.setText(phut)
            d.nutXoa.setOnClickListener {
                cot.removeView(d.root)
                cacDong.remove(d)
                nutThem.isEnabled = cacDong.size < Duong.TOI_DA_VIEC
            }
            cacDong += d
            // Hang moi chen ngay truoc nut Them, nut do luon nam duoi cung.
            val viTriNut = cot.indexOfChild(nutThem)
            if (viTriNut < 0) cot.addView(d.root) else cot.addView(d.root, viTriNut)
            nutThem.isEnabled = cacDong.size < Duong.TOI_DA_VIEC
            return d
        }

        (danhSachChung ?: ViecNha.MAC_DINH).take(Duong.TOI_DA_VIEC)
            .forEach { themHang(it.ten, it.phut.toString()) }
        nutThem.setOnClickListener { themHang("", "10").oTen.requestFocus() }
        cot.addView(nutThem)

        val hop = MaterialAlertDialogBuilder(ct)
            .setTitle(R.string.viec_sua_tieu_de)
            .setView(ScrollView(ct).apply { addView(cot) })
            .setPositiveButton(R.string.viec_luu, null)
            .setNegativeButton(R.string.huy, null)
            .show()
        hop.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val kq = ViecNha.kiem(cacDong.map { it.oTen.text.toString() to it.oPhut.text.toString() })
            cacDong.forEachIndexed { i, d -> d.khungTen.error = kq.loi[i] }
            chuLoi.text = kq.loiChung.orEmpty()
            chuLoi.visibility = if (kq.loiChung == null) View.GONE else View.VISIBLE
            if (!kq.dung) return@setOnClickListener
            hop.dismiss()
            // Dong hop ngay chu khong doi may chu tra loi: Firestore ghi vao bo nho may
            // nay truoc, nen the Viec nha doi ngay, va mat mang thi no tu gui khi co
            // mang. Chi bao khi that su hong, vi du may nay bi go khoi nha.
            Kho.datDanhSachViec(ct, kq.cac) { r ->
                if (r is Kho.KetQua.Hong && conSong) Dinh.noi(ct, r.viSao)
            }
        }
    }
}
