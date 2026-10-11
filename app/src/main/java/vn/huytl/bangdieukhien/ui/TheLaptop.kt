package vn.huytl.bangdieukhien.ui

import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.ListenerRegistration
import vn.huytl.bangdieukhien.R
import vn.huytl.bangdieukhien.data.AnhLaptop
import vn.huytl.bangdieukhien.data.Duong
import vn.huytl.bangdieukhien.data.Kho
import vn.huytl.bangdieukhien.data.LenhLaptop
import vn.huytl.bangdieukhien.data.LenhLaptopCho
import vn.huytl.bangdieukhien.data.PhieuLaptopCho
import vn.huytl.bangdieukhien.data.TinhTrangLaptop
import vn.huytl.bangdieukhien.databinding.HopAnhLaptopBinding
import vn.huytl.bangdieukhien.databinding.HopNhanTiviBinding
import vn.huytl.bangdieukhien.databinding.TheLaptopBinding

/**
 * The Laptop o tab Gio choi (8/10/2026, anh Huy chon cach B: gop het vao mot the). Chu tinh o
 * [ChuLaptop]; o day lo view, hop thoai va lenh.
 *
 * Laptop chi ghi len Firestore khi co gi doi, nen moi lan tab hien len la gui mot lenh
 * [LenhLaptop.HOI]: laptop tra loi (tu 9/10/2026 laptop nghe Firestore, vai giay; luc khong nghe
 * duoc thi toi mot phut) thi biet no con song. Lenh laptop chua lay sau 5 phut thi may nay xoa va
 * bao "đã bỏ" (anh Huy chot), laptop gap lenh cu cung bo. Laptop tat hay khong tra loi thi cac nut
 * lenh mo di, bam chi bao ly do; rieng nut Them, Bot van bam duoc vi phieu cap nam cho toi khi
 * laptop bat lai (phieu chua nhan hien dong "Thêm 20 phút lúc 14:05 (Chờ)", 9/10/2026), va
 * nut Chup man hinh van mo duoc hop xem anh lan truoc. Moi dong lenh, phieu dang cho co nut "Rút
 * lại" (9/10/2026), xem [veCho].
 *
 * [BangFragment] giu lang nghe document laptop (chung voi hang cu) va goi [capNhat], [nhip].
 */
class TheLaptop(
    private val fragment: Fragment,
    private val v: TheLaptopBinding,
    /** Nut 15', 30', 45': gui ngay phieu cong chung ay phut, khong hoi lai (anh Huy chot 9/10/2026). */
    private val khiThem: (Int) -> Unit,
    /** Nut "Khác": o go so phut cong. */
    private val khiKhac: () -> Unit,
    /** Nut "Bớt": o go so phut bot (9/10/2026; truoc la danh sach chon nhu nut Them cu). */
    private val khiBot: () -> Unit
) {
    private var laptop: TinhTrangLaptop? = null
    private var cho: List<LenhLaptopCho> = emptyList()
    private var phieu: List<PhieuLaptopCho> = emptyList()

    /** Lenh may nay da bo vi qua 5 phut, kem luc bo, de hien "đã bỏ" mot luc. */
    private val bo = mutableListOf<Pair<LenhLaptopCho, Long>>()
    private val daBo = mutableSetOf<String>()

    private var hoiId: String? = null
    private var hoiLuc = 0L
    private var ngheLenh: ListenerRegistration? = null
    private var nghePhieu: ListenerRegistration? = null

    /** Hop anh dang mo, de ve lai khi anh hay ket qua lenh chup ve. */
    private var hopAnh: HopAnhLaptopBinding? = null
    private var anh: AnhLaptop? = null
    private var chupId: String? = null
    private var chupLuc = 0L

    init {
        // Phieu cap, bot nam cho toi khi laptop bat lai trong ngay, nen bam luc nao cung duoc.
        v.them15.setOnClickListener { khiThem(15) }
        v.them30.setOnClickListener { khiThem(30) }
        v.them45.setOnClickListener { khiThem(45) }
        v.themKhac.setOnClickListener { khiKhac() }
        v.nutBot.setOnClickListener { khiBot() }
        v.nutRemote.setOnClickListener { moRemote() }
        v.nutNhan.setOnClickListener { neuChoBam { hoiNhan() } }
        // Mo hop xem anh lan truoc thi duoc ca luc laptop tat; nut Chup trong hop moi can laptop.
        v.nutChup.setOnClickListener { moAnh() }
        v.nutDangXuat.setOnClickListener { neuChoBam { hoiDangXuat() } }
        v.nutTatMay.setOnClickListener { neuChoBam { hoiTatMay() } }
    }

    private val ct get() = fragment.requireContext()

    /** Tab hien len: nghe hang lenh, va hoi laptop lai o lan [capNhat] ke tiep. */
    fun batDau() {
        hoiLuc = 0L
        hoiId = null
        ngheLenh = Kho.ngheLenhLaptop(ct) { ds ->
            cho = ds
            ve()
        }
        nghePhieu = Kho.ngheCapLaptop(ct) { ds ->
            phieu = ds
            ve()
        }
    }

    fun dung() {
        ngheLenh?.remove()
        ngheLenh = null
        nghePhieu?.remove()
        nghePhieu = null
    }

    fun capNhat(l: TinhTrangLaptop?) {
        laptop = l
        if (l != null && hoiLuc == 0L) hoi()
        ve()
        veAnh()
    }

    /** Moi giay: dong ho Netflix, moc 150 giay cho tra loi, moc 5 phut bo lenh. */
    fun nhip() {
        boLenhQuaHan()
        ve()
    }

    private fun hoi() {
        hoiLuc = System.currentTimeMillis()
        Kho.guiLenhLaptop(ct, LenhLaptop.HOI) { kq, id ->
            if (kq is Kho.KetQua.Xong) hoiId = id
        }
    }

    private fun boLenhQuaHan() {
        val bayGio = System.currentTimeMillis()
        for (l in cho) {
            if (l.id in daBo || !ChuLaptop.quaHan(l, bayGio)) continue
            daBo += l.id
            Kho.xoaLenhLaptop(ct, l.id)
            if (l.kieu != LenhLaptop.HOI) bo += l to bayGio
        }
    }

    private fun trangThai(l: TinhTrangLaptop) =
        ChuLaptop.trangThai(l, hoiId, hoiLuc, System.currentTimeMillis())

    fun ve() {
        val l = laptop
        if (l == null) {
            v.root.visibility = View.GONE
            return
        }
        v.root.visibility = View.VISIBLE
        val bayGio = System.currentTimeMillis()
        v.soNetflix.text = ChuLaptop.soNetflix(l, bayGio)
        val tt = trangThai(l)
        v.nhanTrangThai.text = tt.nhan
        v.dongTaiKhoan.text = tt.chu
        // Nhan to mau nhu nhanTrangThai cua the tablet: mau chu dam tren nen nhat cung ho mau.
        val (mauChu, mauNen) = when (tt.mau) {
            ChuLaptop.Mau.NETFLIX -> R.color.child_tint to R.color.child_soft
            ChuLaptop.Mau.ADMIN -> R.color.parent_tint to R.color.parent_soft
            ChuLaptop.Mau.XAM -> R.color.ink_soft to R.color.line
        }
        v.nhanTrangThai.setTextColor(ContextCompat.getColor(ct, mauChu))
        v.nhanTrangThai.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ct, mauNen))
        // Dong "Hôm nay: bật …, tắt …" bo ngay 9/10/2026 (anh Huy chot), cac lan dung laptop chuyen
        // sang the "Thời gian dùng laptop" o tab Nhat ky.
        val mo = if (tt.choBam) 1f else 0.4f
        for (n in listOf(v.nutNhan, v.nutDangXuat, v.nutTatMay)) n.alpha = mo

        val tatCa = ChuLaptop.dongLenh(cho.filter { it.id !in daBo }, l.ketQua, bo, bayGio, phieu)
        veCho(tatCa.filter { it.cho })
        val dong = tatCa.filter { !it.cho }
        if (dong.isEmpty()) {
            v.chuLenh.visibility = View.GONE
        } else {
            val chu = SpannableStringBuilder()
            for (d in dong) {
                if (chu.isNotEmpty()) chu.append("\n")
                val dau = chu.length
                chu.append(d.chu)
                val mau = when {
                    d.loi -> R.color.alert
                    d.cho -> R.color.ink_soft
                    else -> R.color.ok
                }
                chu.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(ct, mau)), dau, chu.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            v.chuLenh.text = chu
            v.chuLenh.visibility = View.VISIBLE
        }
    }

    /** Khoa cua lan ve [veCho] truoc, de khong dung lai view khi danh sach khong doi. */
    private var daVeCho = ""

    /**
     * Khoi lenh, phieu dang cho laptop nhan: moi dong mot nut "Rút lại" (anh Huy dan 9/10/2026),
     * giong khoi lenh cho tablet o the Gio choi (BangFragment.veLenhCho). Chi ve lai khi danh sach
     * doi: [ve] chay moi giay tu [nhip], dung lai view moi giay thi nut dang bam do bi thay giua
     * chung. Rut xong thi listener cua [cho] hay [phieu] bo dong do.
     */
    private fun veCho(ds: List<ChuLaptop.DongLenh>) {
        val ky = ds.joinToString("|") { "${it.lenhId}:${it.phieuId}:${it.chu}" }
        if (ky == daVeCho) return
        daVeCho = ky
        val hop = v.hopCho
        hop.removeAllViews()
        hop.visibility = if (ds.isEmpty()) View.GONE else View.VISIBLE
        val c = ct
        for (d in ds) {
            val dong = LinearLayout(c).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            dong.addView(TextView(c).apply {
                text = d.chu
                textSize = 13f
                setTextColor(ContextCompat.getColor(c, R.color.ink_soft))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            dong.addView(MaterialButton(c, null, androidx.appcompat.R.attr.borderlessButtonStyle).apply {
                text = "Rút lại"
                setOnClickListener {
                    isEnabled = false
                    val xong: (Kho.KetQua) -> Unit = { kq ->
                        if (kq is Kho.KetQua.Hong) {
                            isEnabled = true
                            fragment.context?.let { Dinh.noi(it, kq.viSao) }
                        }
                    }
                    d.phieuId?.let { Kho.rutPhieuLaptop(c, it, xong) }
                        ?: d.lenhId?.let { Kho.rutLenhLaptop(c, it, xong) }
                }
            })
            hop.addView(dong)
        }
    }

    /** Laptop tat hay khong tra loi thi khong gui (anh Huy chot), chi noi ly do. */
    /**
     * Man Remote noi thang laptop qua Wi-Fi nha (11/10/2026), khong cho Firestore: mo duoc ca luc the
     * dang bao laptop khong tra loi, man do tu bao co noi duoc khong.
     */
    private fun moRemote() {
        val r = laptop?.remote
        if (r == null) {
            Dinh.noi(ct, "Laptop chưa cài bản có remote.")
            return
        }
        RemoteActivity.mo(ct, r)
    }

    private fun neuChoBam(lam: () -> Unit) {
        val l = laptop ?: return
        val tt = trangThai(l)
        if (tt.choBam) lam() else Dinh.noi(ct, "${tt.chu}, chưa gửi được lệnh.")
    }

    private fun gui(kieu: String, chu: String? = null, soLan: Int? = null, xong: (String) -> Unit = {}) {
        Kho.guiLenhLaptop(ct, kieu, chu, soLan) { kq, id ->
            val c = fragment.context ?: return@guiLenhLaptop
            if (kq is Kho.KetQua.Hong) {
                Dinh.noi(c, kq.viSao)
            } else {
                // Bo ve "laptop nhận trong khoảng một phút" (anh Huy dan 9/10/2026).
                Dinh.noi(c, "Đã gửi.")
                if (id != null) xong(id)
            }
        }
    }

    /** Hop "Thông báo" (nut "Thông báo", truoc 9/10/2026 ghi "Nhắn lên tivi"): cau toi da 200 ky tu, doc 1 toi 3 lan, mac dinh 1 (anh Huy chot). */
    private fun hoiNhan() {
        val h = HopNhanTiviBinding.inflate(LayoutInflater.from(ct))
        val hop = MaterialAlertDialogBuilder(ct)
            .setTitle("Thông báo")
            .setView(h.root)
            .setPositiveButton("Gửi", null)
            .setNegativeButton(R.string.huy, null)
            .show()
        h.oChu.doAfterTextChanged { h.khungChu.error = null }
        hop.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val chu = h.oChu.text?.toString().orEmpty().trim().replace(Regex("\\s+"), " ")
            if (chu.isEmpty()) {
                h.khungChu.error = "Gõ câu nhắn trước."
                return@setOnClickListener
            }
            val soLan = when (h.nhomSoLan.checkedChipId) {
                h.lan2.id -> 2
                h.lan3.id -> 3
                else -> 1
            }
            hop.dismiss()
            gui(LenhLaptop.NHAN, chu.take(Duong.NHAN_TOI_DA), soLan)
        }
    }

    private fun hoiDangXuat() {
        val ten = laptop?.phien.orEmpty()
        if (ten.isEmpty()) {
            Dinh.noi(ct, "Chưa ai đăng nhập laptop.")
            return
        }
        val sau = if (ten == "lehoa") "Phim đang chiếu sẽ tắt." else "Việc đang làm dở trên đó sẽ mất."
        MaterialAlertDialogBuilder(ct)
            .setTitle("Đăng xuất tài khoản ${ChuLaptop.tenHien(ten)}?")
            .setMessage(sau)
            .setPositiveButton("Đăng xuất") { _, _ -> gui(LenhLaptop.DANG_XUAT) }
            .setNegativeButton(R.string.huy, null)
            .show()
    }

    private fun hoiTatMay() {
        MaterialAlertDialogBuilder(ct)
            .setTitle("Tắt laptop?")
            .setMessage("Muốn bật lại phải có người bấm nút nguồn.")
            .setPositiveButton("Tắt máy") { _, _ -> gui(LenhLaptop.TAT_MAY) }
            .setNegativeButton(R.string.huy, null)
            .show()
    }

    /**
     * Hop "Màn hình laptop": mo ra chi hien anh chup lan truoc, bam nut "Chụp" o duoi moi gui lenh
     * chup (anh Huy doi 9/10/2026; truoc do mo hop la chup ngay). Dau X o tren de dong. Chi co mot
     * anh moi nhat, anh sau de anh truoc (anh Huy hoi lai 9/10/2026 va giu nhu vay).
     */
    private fun moAnh() {
        val h = HopAnhLaptopBinding.inflate(LayoutInflater.from(ct))
        hopAnh = h
        chupLuc = 0L
        chupId = null
        val nghe = Kho.ngheAnhLaptop(ct) { a ->
            anh = a
            veAnh()
        }
        val hop = MaterialAlertDialogBuilder(ct)
            .setView(h.root)
            .setOnDismissListener {
                nghe?.remove()
                hopAnh = null
            }
            .show()
        h.nutDong.setOnClickListener { hop.dismiss() }
        h.nutChupAnh.setOnClickListener { neuChoBam { chup() } }
        veAnh()
    }

    private fun chup() {
        chupLuc = System.currentTimeMillis()
        chupId = null
        veAnh()
        gui(LenhLaptop.CHUP) { id -> chupId = id }
    }

    private fun veAnh() {
        val h = hopAnh ?: return
        val a = anh
        val bayGio = System.currentTimeMillis()
        if (a != null && h.anh.tag != a.luc) {
            h.anh.setImageBitmap(BitmapFactory.decodeByteArray(a.jpg, 0, a.jpg.size))
            h.anh.tag = a.luc
        }
        val hong = laptop?.ketQua?.firstOrNull { it.id == chupId && !it.ok }
        // Vua bam Chup ma anh tren may chu van cu hon luc bam: dang cho laptop chup.
        val dangCho = chupLuc > 0L && (a == null || a.luc < chupLuc - 5_000L)
        val cuaAi = a?.phien?.takeIf { it.isNotEmpty() }?.let { " · tài khoản ${ChuLaptop.tenHien(it)}" }
            ?: " · màn đăng nhập"
        h.chuAnh.text = when {
            hong != null -> hong.chu
            dangCho && a != null -> "Đang chờ laptop chụp ảnh mới. Ảnh dưới chụp lúc " +
                "${ChuLaptop.luc(a.luc, bayGio)}."
            dangCho -> "Đang chờ laptop chụp."
            a != null -> "Chụp lúc ${ChuLaptop.luc(a.luc, bayGio)}$cuaAi"
            else -> "Chưa có ảnh nào. Bấm Chụp để chụp màn hình laptop."
        }
    }
}
