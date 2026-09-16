package vn.huytl.bangdieukhien.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.ListenerRegistration
import vn.huytl.bangdieukhien.R
import vn.huytl.bangdieukhien.data.Cong
import vn.huytl.bangdieukhien.data.Kho
import vn.huytl.bangdieukhien.data.Lenh
import vn.huytl.bangdieukhien.data.TrangThai
import vn.huytl.bangdieukhien.databinding.FragmentBangBinding

/**
 * Man chinh: liec mot cai la biet tablet dang the nao, va bam duoc ngay.
 *
 * Dong ho o day tu chay tren may nay, tinh tu moc ket thuc tablet gui sang. Tablet
 * khong phai ghi xuong Firestore moi giay - no chi ghi khi trang thai doi. Ca ngay
 * het vai chuc luot ghi thay vi vai chuc nghin.
 */
class BangFragment : Fragment() {

    private var _b: FragmentBangBinding? = null
    private val b get() = _b!!

    private var ngheTrangThai: ListenerRegistration? = null
    private var ngheNhatKy: ListenerRegistration? = null
    private var moiNhat: TrangThai? = null

    /**
     * Cau tra loi cuoi cung da hien, de khong hien lai mot cau hai lan.
     *
     * Can cho nay vi Firestore goi lai lang nghe moi lan document doi bat cu truong
     * nao - pin tablet tut mot phan tram cung la mot lan goi lai. Khong nho thi cau
     * "Da duyet 30 phut" nhay len lai giua luc dang lam viec khac.
     */
    private var traLoiDaHien = 0L

    private val tay = Handler(Looper.getMainLooper())
    private val nhip = object : Runnable {
        override fun run() {
            veDongHo()
            tay.postDelayed(this, 1000L)
        }
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentBangBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        b.tenCon.text = getString(R.string.child_name)

        b.cho15.setOnClickListener { cho(15) }
        b.cho30.setOnClickListener { cho(30) }
        b.cho45.setOnClickListener { cho(45) }
        b.choKhac.setOnClickListener { hoiSoPhut() }

        b.nutDung.setOnClickListener {
            // Mot nut cho ca hai chieu: dang choi thi dung, dang dung thi tiep.
            // Hai nut rieng thi luc nao cung co mot cai vo nghia nam do.
            val dangDung = moiNhat?.cong == Cong.TAM_DUNG
            gui(if (dangDung) Lenh.TIEP else Lenh.DUNG)
        }
        b.nutBot.setOnClickListener { gui(Lenh.BOT, phut = 15) }
        b.nutKhoa.setOnClickListener { hoiRoiKhoa() }
        b.nutMoMay.setOnClickListener { hoiMoMay() }
        b.theBaiCho.setOnClickListener { (activity as? MainActivity)?.sangTheBai() }
    }

    override fun onStart() {
        super.onStart()
        val ct = requireContext()
        ngheTrangThai = Kho.ngheTrangThai(ct) { tt, loi ->
            if (_b == null) return@ngheTrangThai
            if (loi != null) {
                b.chuCanhBao.text = loi
                b.theCanhBao.visibility = View.VISIBLE
                return@ngheTrangThai
            }
            moiNhat = tt
            ve()
            noiLaiNeuCo(tt)
        }
        ngheNhatKy = Kho.ngheNhatKy(ct, Dinh.homNay()) { dong ->
            if (_b == null) return@ngheNhatKy
            b.nhatKy.text =
                if (dong.isEmpty()) getString(R.string.bang_chua_co_gi)
                else dong.joinToString("\n")
        }
        tay.post(nhip)
    }

    override fun onStop() {
        tay.removeCallbacks(nhip)
        ngheTrangThai?.remove()
        ngheNhatKy?.remove()
        super.onStop()
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }

    /**
     * Hien cau tablet noi lai sau khi lam lenh.
     *
     * Chi hien cau con moi: mo lai app sau nua tieng ma thay "Da khoa tablet" nhay
     * len thi khong hieu may vua lam gi.
     */
    private fun noiLaiNeuCo(tt: TrangThai?) {
        val tra = tt ?: return
        if (tra.traLoi.isBlank() || tra.traLoiLuc <= traLoiDaHien) return
        traLoiDaHien = tra.traLoiLuc
        if (System.currentTimeMillis() - tra.traLoiLuc > 60_000L) return
        Dinh.noi(requireContext(), tra.traLoi)
    }

    // ------------------------------------------------------------------- ve

    private fun ve() {
        val tt = moiNhat ?: return
        val ct = requireContext()

        val (chu, mau, mauNhat) = when {
            tt.cheDoBaBat -> Bo(getString(R.string.bang_che_do_ba), R.color.parent_tint, R.color.parent_soft)
            tt.cong == Cong.DANG_CHOI -> Bo(getString(R.string.bang_dang_choi), R.color.ok, R.color.ok_soft)
            tt.cong == Cong.TAM_DUNG -> Bo(getString(R.string.bang_tam_dung), R.color.wait, R.color.wait_soft)
            tt.cong == Cong.DA_DUYET -> Bo(getString(R.string.bang_da_duyet), R.color.brand, R.color.brand_soft)
            tt.cong == Cong.CHO_DUYET -> Bo(getString(R.string.bang_cho_duyet), R.color.wait, R.color.wait_soft)
            else -> Bo(getString(R.string.bang_dang_khoa), R.color.locked, R.color.locked_soft)
        }
        b.nhanTrangThai.text = chu
        b.nhanTrangThai.setTextColor(ContextCompat.getColor(ct, mau))
        b.nhanTrangThai.backgroundTintList =
            ContextCompat.getColorStateList(ct, mauNhat)
        b.thanhPhien.setIndicatorColor(ContextCompat.getColor(ct, mau))

        b.nutDung.text = if (tt.cong == Cong.TAM_DUNG) "Chơi tiếp" else "Tạm dừng"
        b.nutDung.isEnabled = tt.cong == Cong.DANG_CHOI || tt.cong == Cong.TAM_DUNG
        b.nutBot.isEnabled = tt.cong == Cong.DANG_CHOI
        b.nutKhoa.isEnabled = tt.cong != Cong.KHOA || tt.cheDoBaBat
        b.nutMoMay.text =
            if (tt.cheDoBaBat) "Đóng chế độ Ba Huy, khoá máy lại"
            else "Mở toàn bộ máy cho Ba Huy dùng"

        b.daDuyet.text = Dinh.phut(tt.phutDaDuyet)
        b.conLaiNgay.text = Dinh.phut(tt.phutConLai)
        val tran = tt.phutDaDuyet + tt.phutConLai
        b.thanhNgay.max = if (tran > 0) tran else 1
        b.thanhNgay.setProgressCompat(tt.phutDaDuyet, true)

        if (tt.soBaiCho > 0) {
            b.theBaiCho.visibility = View.VISIBLE
            b.chuBaiCho.text = "${tt.soBaiCho} bài đang chờ duyệt — bấm để xem"
        } else {
            b.theBaiCho.visibility = View.GONE
        }

        b.pinMay.text = when {
            tt.pinMay < 0 -> ""
            tt.dangSac -> "⚡ ${tt.pinMay}%"
            else -> "${tt.pinMay}%"
        }
        b.chamSong.backgroundTintList = ContextCompat.getColorStateList(
            ct, if (tt.cu()) R.color.alert else R.color.ok
        )

        veCanhBao(tt)
        veDongHo()
    }

    /**
     * Bang canh bao. Gop het vao mot the do, va chi hien khi co viec that.
     *
     * Cai bang nay ma luc nao cung nam do thi mat luot qua no, den hom quyen
     * Accessibility that su bi tat cung khong ai nhin thay.
     */
    private fun veCanhBao(tt: TrangThai) {
        val cac = buildList {
            if (tt.cu()) add(getString(R.string.bang_mat_lien_lac))
            if (!tt.quyenTroGiup) add("Quyền Trợ giúp (Accessibility) đang tắt — máy không chặn được app nào.")
            if (!tt.quyenQuanTri) add("Quản trị thiết bị đang tắt — app gỡ được.")
            if (!tt.quyenNoi) add("Quyền hiện trên app khác đang tắt — màn chặn không hiện lên được.")
            if (!tt.coPin) add("Tablet chưa đặt mã PIN.")
        }
        if (cac.isEmpty()) {
            b.theCanhBao.visibility = View.GONE
        } else {
            b.theCanhBao.visibility = View.VISIBLE
            b.chuCanhBao.text = cac.joinToString("\n")
        }
    }

    /** Chay moi giay. Chi doi hai dong chu, khong dung toi Firestore. */
    private fun veDongHo() {
        val tt = moiNhat ?: return
        if (_b == null) return

        if (tt.cheDoBaBat) {
            b.dongHo.text = if (tt.cheDoBaHetLuc > 0) {
                Dinh.dongHo(tt.cheDoBaHetLuc - System.currentTimeMillis())
            } else "∞"
            b.duoiDongHo.text =
                if (tt.cheDoBaHetLuc > 0) "còn lại của chế độ Ba Huy"
                else "không đặt hạn — nhớ tự đóng"
            b.thanhPhien.visibility = View.GONE
            return
        }

        val conLai = tt.conLaiBayGio()
        when (tt.cong) {
            Cong.KHOA -> {
                b.dongHo.text = "Khoá"
                b.duoiDongHo.text =
                    if (tt.soBaiCho > 0) "đang chờ Ba Huy duyệt bài"
                    else "chưa có phiên chơi nào"
                b.thanhPhien.visibility = View.GONE
            }
            Cong.CHO_DUYET -> {
                b.dongHo.text = "${tt.soBaiCho} bài"
                b.duoiDongHo.text = "đang chờ Ba Huy duyệt"
                b.thanhPhien.visibility = View.GONE
            }
            else -> {
                b.dongHo.text = Dinh.dongHo(conLai)
                b.duoiDongHo.text = when (tt.cong) {
                    Cong.DANG_CHOI -> "còn lại của phiên đang chơi"
                    Cong.TAM_DUNG -> "đang giữ, chưa chạy"
                    else -> "đã duyệt, Lê Hòa chưa bấm chơi"
                }
                b.thanhPhien.visibility = View.VISIBLE
                // Thanh chay theo phien hien tai chu khong theo han muc ngay: moc
                // day la luc bat dau phien, tuc la so phut duoc cap lan nay.
                val tong = (tt.conLaiMs).coerceAtLeast(conLai)
                b.thanhPhien.max = (tong / 1000).toInt().coerceAtLeast(1)
                b.thanhPhien.progress = (conLai / 1000).toInt()
            }
        }
    }

    // ----------------------------------------------------------------- lenh

    private fun cho(phut: Int) = gui(Lenh.CHO, phut = phut)

    private fun gui(kieu: String, phut: Int? = null, chu: String? = null) {
        Kho.guiLenh(requireContext(), kieu, phut = phut, chu = chu) { kq ->
            if (kq is Kho.KetQua.Hong) Dinh.noi(requireContext(), kq.viSao)
        }
    }

    private fun hoiSoPhut() {
        val cac = arrayOf("10 phút", "20 phút", "60 phút", "90 phút")
        val so = intArrayOf(10, 20, 60, 90)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cho chơi bao lâu")
            .setItems(cac) { _, i -> cho(so[i]) }
            .setNegativeButton(R.string.huy, null)
            .show()
    }

    private fun hoiRoiKhoa() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Khoá tablet ngay?")
            .setMessage("Số phút còn lại của phiên này mất luôn.")
            .setPositiveButton("Khoá") { _, _ -> gui(Lenh.KHOA) }
            .setNegativeButton(R.string.huy, null)
            .show()
    }

    /**
     * Mo toan bo may.
     *
     * Hoi han bao lau chu khong mo vo han: lan nao quen tat cung la tablet mo toang
     * ca dem. Van co lua chon "khong dat han" cho luc that su can lam lau.
     */
    private fun hoiMoMay() {
        if (moiNhat?.cheDoBaBat == true) {
            gui(Lenh.DONG_MAY)
            return
        }
        val cac = arrayOf("15 phút", "30 phút", "1 tiếng", "Không đặt hạn")
        val so = arrayOf(15, 30, 60, null)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Mở toàn bộ máy")
            .setMessage("Tablet bỏ hết chặn để Ba Huy dùng. Hết hạn thì tự khoá lại.")
            .setItems(cac) { _, i -> gui(Lenh.MO_MAY, phut = so[i]) }
            .setNegativeButton(R.string.huy, null)
            .show()
    }

    private data class Bo(val chu: String, val mau: Int, val mauNhat: Int)
}
