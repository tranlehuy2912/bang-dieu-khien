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
import vn.huytl.bangdieukhien.data.Nguoi
import vn.huytl.bangdieukhien.data.TrangThai
import vn.huytl.bangdieukhien.data.ViecNhaCho
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
    private var ngheViecNha: ListenerRegistration? = null
    private var moiNhat: TrangThai? = null

    /** Dot viec nha ba noi dang giao, null la khong co dot nao tren Firestore. */
    private var viecCho: ViecNhaCho? = null

    /** Dang gui lenh cong gio cho dot viec ket, de khong bam hai lan. */
    private var dangGoDotKet = false

    /**
     * Lenh dang tren duong di, va cau bao hong cua lan gui gan nhat.
     *
     * Firestore nhan lenh xong khong co nghia la tablet da lam; nhung it nhat tu luc
     * bam den luc ghi duoc phai co gi do tren man hinh. Truoc day cho nay trong tron:
     * bam "30 phut" xong khong thay gi doi, nen ai cung bam them lan nua, va tablet
     * nhan hai lenh.
     */
    private var dangGuiLenh = false
    private var loiGui = ""

    /**
     * Luc may nay go lenh hoi tablet, va tablet da dap chua.
     *
     * KHONG SO HAI MOC THOI GIAN de biet tablet da dap: [TrangThai.capNhatLuc] doc
     * tu dong ho tablet, con moc hoi thi doc tu dong ho may nay, va hai cai do khong
     * bao gio khop tuyet doi. Thay vao do nho lai con so tablet bao ve TRUOC khi
     * hoi, roi doi den khi no khac di - bat ky lan ghi nao cung chung minh dau kia
     * con song, khong can biet may gio.
     */
    private var hoiLuc = 0L
    private var capNhatTruocKhiHoi = 0L
    private var daDap = false
    private var loiHoi = ""

    /** Da ve man hinh o trang thai "khong dap" chua, de khong ve lai moi giay. */
    private var daBaoKhongDap = false

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
            // Qua han cho ma tablet van im: ve lai dung mot lan de cham doi mau va
            // dong canh bao moc len. Khong ve moi giay - khong co gi doi nua.
            if (khongDap() != daBaoKhongDap) {
                daBaoKhongDap = khongDap()
                ve()
            }
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
        b.theViecKet.setOnClickListener { hoiRoiGoDotKet() }
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
            // Ban moi khac ban truoc luc hoi: tablet con song. Xem [capNhatTruocKhiHoi].
            if (!daDap && tt != null && tt.capNhatLuc != capNhatTruocKhiHoi) daDap = true
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
        ngheViecNha = Kho.ngheViecNha(ct) { dot ->
            if (_b == null) return@ngheViecNha
            viecCho = dot
            veViecKet()
        }
        hoiTablet()
        tay.post(nhip)
    }

    override fun onStop() {
        tay.removeCallbacks(nhip)
        ngheTrangThai?.remove()
        ngheNhatKy?.remove()
        ngheViecNha?.remove()
        super.onStop()
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }

    /**
     * Go mot lenh hoi tablet, de so lieu tren man nay la cua giay nay.
     *
     * VI SAO KHONG DE TABLET TU BAO DINH KY. Tablet day trang thai moi lan co gi
     * doi, cong mot nhip tim thua cho nhung luc khong co gi doi. Nhip tim do chay
     * bang Handler, ma Handler dem theo dong ho dung lai khi CPU ngu - tablet nam
     * im tren ban ca buoi toi thi khong day gi hang tieng lien. Hoi thang thi khong
     * ai phai doan: khong ai mo app thi khong ton mot luot ghi nao, ma mo ra la co
     * so lieu that.
     *
     * KHONG HOI DOA: doi tab qua lai cung goi onStart, ma moi lan hoi la mot
     * document trong hang lenh cua tablet.
     */
    private fun hoiTablet() {
        val bayGio = System.currentTimeMillis()
        if (hoiLuc > 0L && bayGio - hoiLuc < GIAN_HOI_MS) return
        hoiLuc = bayGio
        capNhatTruocKhiHoi = moiNhat?.capNhatLuc ?: 0L
        daDap = false
        loiHoi = ""
        daBaoKhongDap = false
        Kho.guiPing(requireContext()) { kq ->
            if (_b == null) return@guiPing
            // Hong ngay tu luc gui - may nay mat mang, hay chua ghep nha. Noi ra
            // chu khong de man hinh cho mot cau tra loi khong bao gio den.
            if (kq is Kho.KetQua.Hong) {
                loiHoi = kq.viSao
                ve()
            }
        }
    }

    /**
     * Da hoi ma tablet chua noi gi, qua han cho.
     *
     * Chua hoi lan nao ([hoiLuc] = 0) thi tra false: luc do khong biet gi ca, va
     * mot man hinh vua mo ra da bao dong la mot man hinh khong ai tin.
     */
    private fun khongDap(bayGio: Long = System.currentTimeMillis()): Boolean =
        hoiLuc > 0L && !daDap && (loiHoi.isNotEmpty() || bayGio - hoiLuc > CHO_PING_MS)

    /**
     * Hien cau tablet noi lai sau khi lam lenh.
     *
     * Chi hien cau con moi: mo lai app sau nua tieng ma thay "Da khoa tablet" nhay
     * len thi khong hieu may vua lam gi.
     *
     * Va chi hien cau tra loi cho lenh cua MAY NAY. O traLoi tren Firestore co mot
     * cho duy nhat, ma tu khi may ba noi cung go lenh thi hai nguoi cung ghi vao do
     * - khong loc thi Ba Huy thay "Hom nay ba cho mot lan roi" nhay len giua man
     * hinh minh, khong hieu may vua noi voi ai.
     */
    private fun noiLaiNeuCo(tt: TrangThai?) {
        val tra = tt ?: return
        if (tra.traLoiCho != Nguoi.BA_HUY) return
        if (tra.traLoi.isBlank() || tra.traLoiLuc <= traLoiDaHien) return
        traLoiDaHien = tra.traLoiLuc
        if (System.currentTimeMillis() - tra.traLoiLuc > 60_000L) return
        Dinh.noi(requireContext(), tra.traLoi)
    }

    // ------------------------------------------------------- viec nha ket

    /**
     * The "ba bao xong ma tablet bo qua".
     *
     * Chi hien dung mot canh, va la canh khong con ai go duoc: ba bam xong het trong
     * luc tablet dang tat, roi app ben ba cung xoa dot di sau khi tablet nhan - nhung
     * tablet khong bao gio nhan, vi den luc no song lai thi ban da qua nua tieng. Le
     * Hoa lam xong viec ma khong duoc phut nao, va khong mot dong nao bao ai ca.
     *
     * Khong tu cong. Tablet co the dang tat ca buoi vi mot ly do gi do, va cong gio
     * thi phai co nguoi lon quyet.
     */
    private fun veViecKet() {
        val dot = viecCho
        if (dot == null || !dot.tabletDaBoQua) {
            b.theViecKet.visibility = View.GONE
            return
        }
        val con = getString(R.string.child_name)
        b.chuViecKet.text = getString(
            R.string.bang_viec_ket,
            con, Dinh.lucNgan(dot.luc), dot.ke, dot.tongPhut
        )
        b.theViecKet.visibility = View.VISIBLE
    }

    private fun hoiRoiGoDotKet() {
        val dot = viecCho ?: return
        if (dangGoDotKet) return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.bang_viec_ket_hoi, dot.tongPhut))
            .setMessage(getString(R.string.bang_viec_ket_hoi_them, dot.ke))
            .setPositiveButton(getString(R.string.bang_viec_ket_cong, dot.tongPhut)) { _, _ ->
                goDotKet(dot)
            }
            .setNegativeButton(R.string.huy, null)
            .show()
    }

    /**
     * Cong gio roi don dot viec di.
     *
     * Thu tu nay quan trong: cong truoc, xoa sau. Xoa truoc ma lenh cong khong di thi
     * ca hai dau deu sach - ba mat dot viec, tablet khong cong gi, va khong con dau
     * vet nao de biet chuyen gi da xay ra.
     *
     * Gui kem ten cac viec o truong chu: tablet lay danh sach do ghi vao nhat ky, ra
     * dung cau ma duong binh thuong van ghi - "Xong viec nha (quet nha, rua chen):
     * +20 phut". Le Hoa doc nhat ky tren man hinh chinh, va do phai la cong chau lam
     * ra chu khong phai mot lan nguoi lon cho.
     */
    private fun goDotKet(dot: ViecNhaCho) {
        dangGoDotKet = true
        val ct = requireContext()
        Kho.guiLenh(ct, Lenh.CONG_VIEC_NHA, phut = dot.tongPhut, chu = dot.ke) { kq ->
            if (kq is Kho.KetQua.Hong) {
                dangGoDotKet = false
                Dinh.noi(ct, kq.viSao)
                return@guiLenh
            }
            Kho.xoaViecNha(ct, dot.maPhien) { kq2 ->
                dangGoDotKet = false
                if (kq2 is Kho.KetQua.Hong) Dinh.noi(ct, kq2.viSao)
            }
        }
    }

    // ------------------------------------------------------------------- ve

    private fun ve() {
        val tt = moiNhat
        if (tt == null) {
            // Chua co ban trang thai nao tu tablet. Khong ve gi ca ngoai viec khoa
            // nut lai: bam mot lenh luc nay la bam vao khoang khong.
            khoaNut()
            veCanhBao(null)
            veDuongLenh()
            return
        }
        val ct = requireContext()

        val (chu, mau, mauNhat) = when {
            // Viec nha xet truoc ca che do Ba: dang khoa vi viec nha thi moi thu
            // khac tren man hinh nay deu khong giai thich duoc cai tablet dang the.
            tt.viecNha.isNotEmpty() ->
                Bo("Đang làm việc nhà bà giao", R.color.wait, R.color.wait_soft)
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
        // Ba mau, va mau giua la mau that su hay gap: vua mo app ra, lenh hoi vua
        // di, chua co gi de noi. Ban truoc chi co xanh va do nen giay dau tien nao
        // cung phai chon mot ben, va no chon sai.
        b.chamSong.backgroundTintList = ContextCompat.getColorStateList(
            ct,
            when {
                khongDap() -> R.color.alert
                daDap -> R.color.ok
                else -> R.color.wait
            }
        )

        veCanhBao(tt)
        veDongHo()
        // Sau cung: no bat lai hay tat het nut tuy theo co lenh dang gui khong, nen
        // phai chay sau moi dong isEnabled o tren.
        veDuongLenh()
    }

    /**
     * Bang canh bao. Gop het vao mot the do, va chi hien khi co viec that.
     *
     * Cai bang nay ma luc nao cung nam do thi mat luot qua no, den hom quyen
     * Accessibility that su bi tat cung khong ai nhin thay.
     */
    private fun veCanhBao(tt: TrangThai?) {
        val cac = buildList {
            if (khongDap()) {
                val luc = tt?.capNhatLuc ?: 0L
                add(
                    if (loiHoi.isNotEmpty()) getString(R.string.bang_hoi_hong, loiHoi)
                    else if (luc <= 0L) getString(R.string.bang_khong_dap_lan_nao)
                    else getString(R.string.bang_khong_dap, Dinh.gioPhut(luc))
                )
            }
            // Cac muc quyen doc tu ban trang thai: chua co ban nao thi khong biet
            // gi ve chung, va doan bua ra thi bang canh bao noi sai.
            if (tt == null) return@buildList
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

    /** Chay moi giay. Chi doi vai dong chu, khong dung toi Firestore. */
    private fun veDongHo() {
        val tt = moiNhat ?: return
        if (_b == null) return
        veDangMo(tt)

        // Dang co viec nha chua xong: tablet bi che kin man hinh, khong phai dang
        // dem gio. Ke ten viec ra chu khong de dong ho dem nguoc gi ca - khong co
        // moc nao de dem, viec het khi ba noi bam xong.
        if (tt.viecNha.isNotEmpty()) {
            b.dongHo.text = if (tt.viecNha.size == 1) "1 việc" else "${tt.viecNha.size} việc"
            b.duoiDongHo.text = tt.viecNha.joinToString(", ") + " — bà nội bấm xong thì máy mở"
            b.thanhPhien.visibility = View.GONE
            return
        }

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
                // Tong lay tu tablet. Truoc day lay conLaiMs, ma truong do luc
                // dang choi chinh la so dang chay, nen thanh luon day gan het.
                val tong = maxOf(tt.tongPhienMs, tt.conLaiMs, conLai)
                b.thanhPhien.max = (tong / 1000).toInt().coerceAtLeast(1)
                b.thanhPhien.progress = (conLai / 1000).toInt()
            }
        }
    }

    /**
     * Dong "dang mo gi" duoi nhan trang thai.
     *
     * Tablet ghi lai moi lan doi app, nen chu o day doi theo gan nhu ngay lap tuc.
     * So phut thi may nay tu dem tu moc bat dau, giong dong ho phien choi.
     *
     * An di trong ba canh, deu la luc khong biet chac:
     *  - tablet ban cu, hoac dich vu canh app ben do khong chay: khong co truong;
     *  - tablet khong tra loi: dong nay la cua lan cuoi no bao, co khi tu may tieng
     *    truoc, ma "dang mo YouTube, 3 tieng" thi la noi sai;
     *  - dang mo toan bo may: luc do la Ba Huy dung may chu khong phai con.
     */
    private fun veDangMo(tt: TrangThai) {
        val sang = tt.manHinhSang
        if (sang == null || khongDap() || tt.cheDoBaBat) {
            b.dangMo.visibility = View.GONE
            return
        }
        b.dangMo.visibility = View.VISIBLE
        b.dangMo.text = when {
            !sang -> "Màn hình tablet đang tắt"
            tt.appTruocMat.isBlank() -> "Không mở app nào"
            tt.appTruocMatTu <= 0L -> "Đang mở ${tt.appTruocMat}"
            else -> {
                // Gio hai may lech nhau vai giay la chuyen thuong; am thi thoi khong
                // ghi so phut, khong hien "-1 phut".
                val phut = ((System.currentTimeMillis() - tt.appTruocMatTu) / 60_000L).toInt()
                "Đang mở ${tt.appTruocMat} từ ${Dinh.gioPhut(tt.appTruocMatTu)}" +
                    if (phut >= 1) " · ${Dinh.phut(phut)}" else ""
            }
        }
    }

    // ----------------------------------------------------------------- lenh

    private fun cho(phut: Int) = gui(Lenh.CHO, phut = phut)

    /**
     * Gui mot lenh, va cho ca man hinh biet la dang gui.
     *
     * Khoa het nut trong luc cho: hai lenh "cho choi" lien nhau la hai phien, ma
     * nguoi bam thi tuong minh vua bam hut mot cai.
     */
    private fun gui(kieu: String, phut: Int? = null, chu: String? = null) {
        if (dangGuiLenh) return
        dangGuiLenh = true
        loiGui = ""
        veDuongLenh()
        Kho.guiLenh(requireContext(), kieu, phut = phut, chu = chu) { kq ->
            dangGuiLenh = false
            // Man hinh co the da bi go trong luc cho mang.
            if (_b == null) return@guiLenh
            loiGui = if (kq is Kho.KetQua.Hong) kq.viSao else ""
            ve()
        }
    }

    /**
     * Dong bao tinh hinh duoi hai hang nut, va khoa nut khi dang gui.
     *
     * Goi ca tu [ve] lan tu chinh [gui]: luc vua bam thi chua co ban trang thai moi
     * nao tu Firestore ve de [ve] chay theo.
     */
    private fun veDuongLenh() {
        if (_b == null) return
        val ct = requireContext()
        when {
            dangGuiLenh -> {
                b.chuGui.visibility = View.VISIBLE
                b.chuGui.text = getString(R.string.bang_dang_gui)
                b.chuGui.setTextColor(ContextCompat.getColor(ct, R.color.ink_soft))
            }
            loiGui.isNotEmpty() -> {
                b.chuGui.visibility = View.VISIBLE
                b.chuGui.text = getString(R.string.bang_gui_hong, loiGui)
                b.chuGui.setTextColor(ContextCompat.getColor(ct, R.color.alert))
            }
            else -> b.chuGui.visibility = View.GONE
        }
        if (dangGuiLenh) khoaNut()
    }

    /** Tat het nut bam duoc trong luc mot lenh dang tren duong di. */
    private fun khoaNut() {
        listOf(
            b.cho15, b.cho30, b.cho45, b.choKhac,
            b.nutDung, b.nutBot, b.nutKhoa, b.nutMoMay
        ).forEach { it.isEnabled = false }
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
     *
     * KHONG DUOC GOI setMessage O DAY. AlertDialog chi dung duoc MOT trong hai: mot
     * doan van, hay mot danh sach. Dat ca hai thi doan van thang, va danh sach khong
     * bao gio duoc gan vao layout - hop thoai hien ra chi con tieu de, doan van va
     * nut Huy. Khong mot dong log nao bao gi ca, ma cai nut thi thanh vo dung: bam
     * vao chi co duong bam Huy. Lan truoc cho nay dung ca hai, va tinh nang mo may
     * tu Bang dieu khien chua bao gio chay.
     *
     * Nen dieu can noi nam trong chinh cac dong de chon. Do cung la cho nguoi ta
     * dang nhin luc phai quyet.
     */
    private fun hoiMoMay() {
        if (moiNhat?.cheDoBaBat == true) {
            gui(Lenh.DONG_MAY)
            return
        }
        val cac = arrayOf(
            "15 phút rồi tự khoá lại",
            "30 phút rồi tự khoá lại",
            "1 tiếng rồi tự khoá lại",
            "Không đặt hạn — nhớ tự đóng"
        )
        val so = arrayOf(15, 30, 60, null)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Mở toàn bộ máy, bỏ hết chặn")
            .setItems(cac) { _, i -> gui(Lenh.MO_MAY, phut = so[i]) }
            .setNegativeButton(R.string.huy, null)
            .show()
    }

    private data class Bo(val chu: String, val mau: Int, val mauNhat: Int)

    companion object {
        /**
         * Doi tablet dap bay lau roi moi bao la no khong dap.
         *
         * Tam giay: lenh di qua Firestore, tablet doc, day ban trang thai, ban do
         * quay ve - duong nay binh thuong het duoi mot giay. Nhung tablet vua tu
         * ngu day thi con phai cho song vo tuyen noi lai, nen phai rong tay hon
         * mot cai bam nut. Ngan hon nua thi man hinh bao dong moi lan mo app o cho
         * song yeu, ma bao dong sai vai lan la lan bao dong that cung bi bo qua.
         */
        private const val CHO_PING_MS = 8_000L

        /**
         * Hai lan hoi cach nhau it nhat bay nhieu.
         *
         * Doi qua tab Bai tap roi quay lai cung goi onStart, ma moi lan hoi la mot
         * document trong hang lenh cua tablet. Nua phut la du ngan de so lieu khong
         * bao gio cu, du dai de nghich thanh tab khong sinh ra mot tram lenh.
         */
        private const val GIAN_HOI_MS = 30_000L
    }
}
