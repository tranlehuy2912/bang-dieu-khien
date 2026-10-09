package vn.huytl.bangdieukhien.ui

import android.content.ClipboardManager
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import vn.huytl.bangdieukhien.R
import vn.huytl.bangdieukhien.data.Cong
import vn.huytl.bangdieukhien.data.Duong
import vn.huytl.bangdieukhien.data.Kho
import vn.huytl.bangdieukhien.data.Lenh
import vn.huytl.bangdieukhien.data.LenhCho
import vn.huytl.bangdieukhien.data.Nguoi
import vn.huytl.bangdieukhien.data.Nha
import vn.huytl.bangdieukhien.data.DeThiTT
import vn.huytl.bangdieukhien.data.TrangThai
import vn.huytl.bangdieukhien.data.TinhTrangLaptop
import vn.huytl.bangdieukhien.data.NhacBaiBuoi
import vn.huytl.bangdieukhien.databinding.FragmentBangBinding
import vn.huytl.bangdieukhien.databinding.ItemDeThiBinding

/**
 * Tab Gio choi, man chinh: liec mot cai la biet tablet dang the nao, va bam duoc ngay.
 *
 * Truoc 27/9/2026 tab nay ten la Bang, va chua ca the Viec nha lan ba the nhat ky, dung
 * app, hoi AI. Nay moi thu mot tab rieng: [ViecNhaFragment], [NhatKyFragment]. O day chi
 * con mot dong "Viec nha chua xong" dan sang tab Viec nha.
 *
 * Dong ho o day tu chay tren may nay, tinh tu moc ket thuc tablet gui sang. Tablet
 * khong phai ghi xuong Firestore moi giay - no chi ghi khi trang thai doi. Ca ngay
 * het vai chuc luot ghi thay vi vai chuc nghin.
 */
class BangFragment : Fragment() {

    private var _b: FragmentBangBinding? = null
    private val b get() = _b!!

    private var ngheTrangThai: ListenerRegistration? = null
    private var ngheNhacBai: ListenerRegistration? = null
    private var ngheLenh: ListenerRegistration? = null
    private var ngheLaptop: ListenerRegistration? = null

    /** Laptop xem Netflix, null la chua noi vao nha. Xem [veLaptop]. */
    private var laptop: TinhTrangLaptop? = null

    /** The Laptop (8/10/2026), dung lai cung view. Xem [TheLaptop]. */
    private var theLaptop: TheLaptop? = null

    /** Lenh may nay da go ma tablet chua lay, cu nhat truoc. Xem [veLenhCho]. */
    private var lenhCho: List<LenhCho> = emptyList()

    /** Khoi lenh dang cho da ve voi nhung dong nao, de khong dung lai view moi giay. */
    private var daVeLenhCho = ""

    /**
     * Cac hang "Đề thi thử <môn>" dang tren man hinh, theo ten mon, dung thu tu hien. Xem
     * [veDeThi]. Giu lai de chi dung lai view khi danh sach mon doi, va de [khoaNut] tat duoc
     * nut cua tung hang.
     */
    private var cacHangDeThi: Map<String, ItemDeThiBinding> = emptyMap()

    private var moiNhat: TrangThai? = null

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

    /**
     * Lenh hoi da len may chu chua. Mat mang thi add() khong hong ma cung khong xong, lenh
     * nam trong may nay. Luc do dung noi "tablet khong tra loi": ben im la dien thoai.
     */
    private var pingDaLen = false

    /** Da ve man hinh o trang thai "khong dap" chua, de khong ve lai moi giay. */
    private var daBaoKhongDap = false

    /**
     * Doi ban trang thai dau tien TU MAY CHU ve roi moi hoi tablet.
     *
     * Truoc day onStart hoi ngay. Mo nguoi thi luc do chua co ban nao, [capNhatTruocKhiHoi]
     * la 0, va ban dau tien doc ve khac 0 nen bi coi la tablet vua dap: tablet tat mang ma
     * man hinh van cham xanh, khong co dai canh bao, the khong mo, bam cho gio cung khong
     * hoi lai. Doi tab qua lai cung vay, chi la hiem hon: ban may nho tu lan truoc cu hon
     * ban tren may chu. Lay moc tu ban may chu roi moi hoi thi con so truoc khi hoi la so
     * that. Qua [CHO_BAN_DAU_MS] ma chua co ban tu may chu (may nay mat mang) thi van hoi,
     * nhu truoc day.
     */
    private var hoiKhiCoBanDau = false
    private val hoiDuPhong = Runnable {
        if (hoiKhiCoBanDau && _b != null) {
            hoiKhiCoBanDau = false
            hoiTablet()
        }
    }

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
            // Lenh qua ba giay moi hien, qua nua tieng thi doi chu: ca hai moc deu la
            // gio troi qua chu khong phai Firestore doi, nen phai xet lai moi giay.
            veLenhCho()
            veLaptop()
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
        b.choBot.setOnClickListener { hoiPhutBot() }
        b.nutCapQuy.setOnClickListener { hoiCapQuy() }
        // Nut cap them Netflix nam trong the Laptop tu 8/10/2026.
        theLaptop = TheLaptop(this, b.theLaptop, khiCap = { hoiCapLaptop() }, khiBot = { hoiCapLaptop(bot = true) })
        // Nut "Mở web" / "Khoá web" cua laptop chuyen sang tab Cai dat (7/10/2026), xem
        // CaiDatFragment.doiWebLaptop.
        // Nut "Xem đề" cua tung mon gan luc dung hang, xem [veDeThi].

        b.nutDung.setOnClickListener {
            // Mot nut cho ca hai chieu: dang choi thi dung, dang dung thi tiep.
            // Hai nut rieng thi luc nao cung co mot cai vo nghia nam do.
            val dangDung = moiNhat?.cong == Cong.TAM_DUNG
            gui(if (dangDung) Lenh.TIEP else Lenh.DUNG)
        }
        b.nutKhoa.setOnClickListener { hoiRoiKhoa() }
        b.nutDongMay.setOnClickListener { gui(Lenh.DONG_MAY) }
        b.nutMoMay.setOnClickListener { hoiMoMay() }
        b.nutTinCo.setOnClickListener { hoiTinCo() }
        b.theViecCho.setOnClickListener { (activity as? MainActivity)?.sangTheViecNha() }
        b.theBaiCho.setOnClickListener { (activity as? MainActivity)?.sangTheBai() }
        // Nhan giu the canh bao hay dong bao hong la chep ca doan, de dan cho Claude Code
        // luc go loi: cau bao hong co khi mang nguyen van loi cua Firestore.
        b.chuCanhBao.setOnLongClickListener { chepChu(b.chuCanhBao, "Đã chép dòng cảnh báo.") }
        // Dong nay co luc la cau "Đang gửi lệnh…" chu khong phai loi, nen bao chung chung.
        b.chuGui.setOnLongClickListener { chepChu(b.chuGui, "Đã chép.") }
    }

    private fun chepChu(o: TextView, bao: String): Boolean {
        val chu = o.text?.toString().orEmpty()
        if (chu.isBlank()) return false
        Dinh.chep(requireContext(), "Báo lỗi", chu, bao)
        return true
    }

    override fun onStart() {
        super.onStart()
        val ct = requireContext()
        ngheTrangThai = Kho.ngheTrangThai(ct) { tt, loi, tuBoNho ->
            if (_b == null) return@ngheTrangThai
            if (loi != null) {
                b.chuCanhBao.text = loi
                b.theCanhBao.visibility = View.VISIBLE
                return@ngheTrangThai
            }
            // Ban moi tu may chu khac ban truoc luc hoi: tablet con song. Ban tu bo nho may
            // thi khong tinh, xem [hoiKhiCoBanDau].
            if (!daDap && !tuBoNho && tt != null && tt.capNhatLuc != capNhatTruocKhiHoi) daDap = true
            moiNhat = tt
            if (hoiKhiCoBanDau && !tuBoNho) {
                hoiKhiCoBanDau = false
                tay.removeCallbacks(hoiDuPhong)
                hoiTablet()
            }
            ve()
            noiLaiNeuCo(tt)
        }
        ngheNhacBai = Kho.ngheNhacBai(ct) { ds ->
            if (_b == null) return@ngheNhacBai
            veNhacBai(ds)
        }
        ngheLenh = Kho.ngheLenhCho(ct) { ds ->
            if (_b == null) return@ngheLenhCho
            lenhCho = ds
            veLenhCho()
        }
        theLaptop?.batDau()
        ngheLaptop = Kho.ngheLaptop(ct) { l ->
            if (_b == null) return@ngheLaptop
            laptop = l
            theLaptop?.capNhat(l)
        }
        // Lang nghe o tren hoi tablet khi ban dau tien tu may chu ve. Xem [hoiKhiCoBanDau].
        hoiKhiCoBanDau = true
        tay.postDelayed(hoiDuPhong, CHO_BAN_DAU_MS)
        tay.post(nhip)
    }

    override fun onStop() {
        tay.removeCallbacks(nhip)
        tay.removeCallbacks(hoiDuPhong)
        hoiKhiCoBanDau = false
        ngheTrangThai?.remove()
        ngheNhacBai?.remove()
        ngheLenh?.remove()
        ngheLaptop?.remove()
        theLaptop?.dung()
        super.onStop()
    }

    override fun onDestroyView() {
        // View moi dung lai thi khoi lenh dang cho con trong, phai ve lai tu dau. Giu
        // chu ky cu thi [veLenhCho] thay danh sach khong doi va bo qua: doi tab qua lai
        // luc co lenh dang cho la mat dong do va nut Rut lai.
        daVeLenhCho = ""
        // Cung ly do: cac hang de thi thuoc view cu. Giu lai thi [veDeThi] thay mon khong doi,
        // khong dung hang vao view moi, va chu cung nut ghi vao nhung hang khong con tren man.
        cacHangDeThi = emptyMap()
        theLaptop = null
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
        pingDaLen = false
        daBaoKhongDap = false
        Kho.guiPing(requireContext()) { kq ->
            if (kq is Kho.KetQua.Xong) pingDaLen = true
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

    /** Khong dap vi chinh dien thoai nay chua gui duoc cau hoi len may chu. */
    private fun dienThoaiMatMang(): Boolean = khongDap() && loiHoi.isEmpty() && !pingDaLen

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

    // ------------------------------------------------------------------- ve

    private fun ve() {
        val tt = moiNhat
        if (tt == null) {
            // Chua co ban trang thai nao tu tablet. Khong ve gi ca ngoai viec khoa
            // nut lai: bam mot lenh luc nay la bam vao khoang khong.
            khoaNut()
            b.hangPhien.visibility = View.GONE
            veCanhBao(null)
            veDuongLenh()
            b.khoiTrangThai.alpha = 1f
            b.khoiHanMuc.alpha = 1f
            return
        }
        val ct = requireContext()
        // Tablet khong tra loi: moi con so trong the la cua lan cuoi no bao ve. Dai do o
        // dau man da noi ra, nhung so van to va dam nhu so that thi mat van doc no truoc.
        // Chi mo phan so: nut van bam duoc, lenh nam cho toi luc tablet co mang.
        val doDam = if (khongDap()) ALPHA_SO_CU else 1f
        b.khoiTrangThai.alpha = doDam
        b.khoiHanMuc.alpha = doDam
        b.khoiQuy.alpha = doDam
        // Phan chu cac hang de thi mo di o [veDeThi]: hang co the vua dung lai o do.

        val (chu, mau, mauNhat) = when {
            // Viec nha xet truoc ca che do Ba: dang khoa vi viec nha thi moi thu
            // khac tren man hinh nay deu khong giai thich duoc cai tablet dang the.
            tt.viecNha.isNotEmpty() ->
                Bo("Đang làm việc nhà", R.color.wait, R.color.wait_soft)
            tt.cheDoBaBat -> Bo(getString(R.string.bang_che_do_ba), R.color.parent_tint, R.color.parent_soft)
            tt.cong == Cong.DANG_CHOI -> Bo(getString(R.string.bang_dang_choi), R.color.ok, R.color.ok_soft)
            tt.cong == Cong.TAM_DUNG -> Bo(getString(R.string.bang_tam_dung), R.color.wait, R.color.wait_soft)
            tt.cong == Cong.DA_DUYET -> Bo(getString(R.string.bang_da_duyet), R.color.brand, R.color.brand_soft)
            tt.cong == Cong.CHO_DUYET -> Bo(getString(R.string.bang_cho_duyet), R.color.wait, R.color.wait_soft)
            else -> Bo(getString(R.string.bang_dang_khoa), R.color.locked, R.color.locked_soft)
        }
        b.nhanTrangThai.text = chu
        // Chu vang tren nen vang nhat thi mo qua, doc khong ra: nhan vang dung chu vang dam.
        b.nhanTrangThai.setTextColor(
            ContextCompat.getColor(ct, if (mau == R.color.wait) R.color.wait_ink else mau)
        )
        b.nhanTrangThai.backgroundTintList =
            ContextCompat.getColorStateList(ct, mauNhat)
        b.thanhPhien.setIndicatorColor(ContextCompat.getColor(ct, mau))

        b.nutDung.text = if (tt.cong == Cong.TAM_DUNG) "Chơi tiếp" else "Tạm dừng"
        veHangPhien(tt)
        // Luc dang mo toan bo may thi nut dong nam trong the chinh, xem [veHangPhien].
        b.nutMoMay.visibility = if (tt.cheDoBaBat) View.GONE else View.VISIBLE
        // Nut nao dang hien la bam duoc. Phai bat lai o day vi [khoaNut] tat chung trong
        // luc gui: truoc day khong cho nao bat lai, nen bam xong mot lenh la hang Cho
        // choi ngay va nut Mo toan bo may cu nam xam.
        listOf(
            b.cho15, b.cho30, b.cho45, b.choKhac, b.choBot, b.nutDung, b.nutDongMay,
            b.nutKhoa, b.nutMoMay, b.nutTinCo
        ).forEach { it.isEnabled = true }

        veNgay(tt)
        // Sau vong bat nut o tren: nut cap tu quy tat khi quy trong, khong bat lai mu quang.
        // Nut "Xem đề" cua tung mon dung bang code nen khong nam trong vong do, [veDeThi] bat.
        veQuy(tt)
        veDeThi(tt, doDam)

        // Ten tung viec theo tablet. The o tab Viec nha doc document chung, co khi di truoc
        // tablet vai giay; con dong nay noi vi sao tablet dang khoa.
        if (tt.viecNha.isNotEmpty()) {
            b.theViecCho.visibility = View.VISIBLE
            b.chuViecCho.text = tt.viecNha.joinToString(", ")
        } else {
            b.theViecCho.visibility = View.GONE
        }

        if (tt.soBaiCho > 0) {
            b.theBaiCho.visibility = View.VISIBLE
            b.chuBaiCho.text = getString(R.string.bang_bai_cho, tt.soBaiCho)
        } else {
            b.theBaiCho.visibility = View.GONE
        }

        b.pinMay.text = when {
            tt.pinMay < 0 -> ""
            tt.dangSac -> getString(R.string.bang_pin_sac, tt.pinMay)
            else -> getString(R.string.bang_pin, tt.pinMay)
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
     * So phut trong ngay: duoc choi, con kiem duoc, thanh ba khuc va mot dong chu cuoi.
     *
     * Tu 1/10/2026 khoi nay ve giong thanh ngay o man chinh tablet ([ThanhNgay]): so ben trai
     * la "được chơi" (da choi cong dang giu, gom ca gio nguoi lon cho); tren thanh xanh duong
     * la da choi, xam nhat la dang giu, trang la con kiem duoc. Truoc do ben trai la "Đã duyệt
     * hôm nay", chi dem phut doi bang bai, nen ba cho 30 phut thi hai may noi hai so khac nhau.
     *
     * Tu 29/9/2026 tablet bo tran chung (truoc la 135 phut): [TrangThai.phutConLai] la tong
     * tran rieng cua cac phan tru so da duyet, chi de xem.
     *
     * Chieu 7/10/2026 anh Huy bo het chu giai thich cua khoi nay: nhan "Được chơi hôm nay" (tablet
     * ban cu: "Đã duyệt hôm nay"), nhan "Còn kiếm được", va dong cuoi "Đã duyệt bằng bài N phút,
     * tối đa M phút một ngày (tổng trần riêng của từng phần)" (tablet ban cu: "Tối đa M phút
     * một ngày..."). Dong cuoi do tung de "Còn kiếm được" khong bi doc thanh mot han muc chan nut
     * Duyet, va de Ba Huy biet bai lam ra bao nhieu gio; gio chi con hai so, thanh va dong cham
     * mau. Tong M lay tu hai so tablet gui, [tranBai], van dung de ve thanh cho tablet ban cu.
     *
     * Tablet ban cu chua gui so da choi thi hien nhu truoc 1/10/2026.
     */
    private fun veNgay(tt: TrangThai) {
        val tranBai = tt.phutDaDuyet + tt.phutConLai
        val so = tt.soNgay()
        if (so == null) {
            b.duocChoi.text = Dinh.phut(tt.phutDaDuyet)
            b.conLaiNgay.text = Dinh.phut(tt.phutConLai)
            ThanhNgay.ve(b.khungNgay, b.phanDaChoi, b.phanCon, 0, tt.phutDaDuyet, tranBai)
            b.chuThichNgay.visibility = View.GONE
            return
        }

        b.duocChoi.text = Dinh.phut(so.duoc)
        b.conLaiNgay.text = Dinh.phut(so.conKiem)
        ThanhNgay.ve(b.khungNgay, b.phanDaChoi, b.phanCon, so.daChoi, so.con, so.tong)
        val chuThich = ThanhNgay.chuThich(requireContext(), so)
        b.chuThichNgay.text = chuThich
        b.chuThichNgay.visibility = if (chuThich.isEmpty()) View.GONE else View.VISIBLE
    }

    /**
     * Hang "Quỹ giờ chơi". An khi tablet chua gui so quy: ban cu, khong hieu lenh
     * [Lenh.CAP_QUY]. Quy trong thi nut tat, vi tablet chi tra loi "Quỹ giờ chơi đang trống".
     *
     * Goi sau vong bat nut cua [ve] va truoc [veDuongLenh], nen lenh dang gui thi nut nay
     * cung khoa nhu moi nut cho gio.
     */
    private fun veQuy(tt: TrangThai) {
        val quy = tt.quyGio
        if (quy == null) {
            b.hangQuy.visibility = View.GONE
            return
        }
        b.hangQuy.visibility = View.VISIBLE
        b.soQuy.text = Dinh.phut(quy)
        b.nutCapQuy.isEnabled = quy > 0
    }

    /**
     * Hang nut cua phien trong the chinh: chi hien nut bam duoc o trang thai nay.
     *
     * Truoc day ca ba nut luc nao cung nam do, tablet khoa thi hai nut xam, con nut Khoa
     * ngay tat ma van do tuoi vi mau chu dat cung. Dang mo toan bo may thi dong ho phien
     * khong hien, nen hai nut cua phien cung an, nhuong cho cho nut Dong che do Ba Huy.
     *
     * Nut "Bớt 15'" tung nam o hang nay, chi hien luc dang choi. Tu 8/10/2026 bot gio co
     * che giong cho them (anh Huy chot) nen nut do thanh nut "Bớt" do o cuoi hang cho gio,
     * xem [hoiPhutBot].
     */
    private fun veHangPhien(tt: TrangThai) {
        val choi = tt.cong == Cong.DANG_CHOI && !tt.cheDoBaBat
        val dung = tt.cong == Cong.TAM_DUNG && !tt.cheDoBaBat
        val hien = mapOf(
            b.nutDung to (choi || dung),
            b.nutDongMay to tt.cheDoBaBat,
            b.nutKhoa to (tt.cong != Cong.KHOA || tt.cheDoBaBat)
        )
        hien.forEach { (nut, co) -> nut.visibility = if (co) View.VISIBLE else View.GONE }
        b.hangPhien.visibility = if (hien.values.any { it }) View.VISIBLE else View.GONE
        // Mot nut thi chiem nua hang chu khong keo het the.
        b.hangPhien.weightSum = maxOf(
            hien.filterValues { it }.keys.sumOf {
                (it.layoutParams as LinearLayout.LayoutParams).weight.toDouble()
            }.toFloat(),
            2f
        )
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
                    else if (dienThoaiMatMang()) "Điện thoại này chưa gửi được câu hỏi tới tablet. Kiểm tra mạng của điện thoại."
                    else if (luc <= 0L) getString(R.string.bang_khong_dap_lan_nao)
                    else getString(R.string.bang_khong_dap, Dinh.gioPhut(luc))
                )
            }
            // Cac muc quyen doc tu ban trang thai: chua co ban nao thi khong biet
            // gi ve chung, va doan bua ra thi bang canh bao noi sai.
            if (tt == null) return@buildList
            if (!tt.quyenTroGiup) add("Quyền Trợ giúp (Accessibility) đang tắt, máy không chặn được app nào.")
            if (!tt.quyenQuanTri) add("Quản trị thiết bị đang tắt, gỡ được app Nộp bài.")
            if (!tt.quyenNoi) add("Quyền hiện trên app khác đang tắt, màn chặn không hiện lên được.")
            if (!tt.coPin) add("Tablet chưa đặt mã PIN.")
        }
        if (cac.isEmpty()) {
            b.theCanhBao.visibility = View.GONE
        } else {
            b.theCanhBao.visibility = View.VISIBLE
            // Hai dong tro len thi moi dong mot gach dau, khong thi cau sau dinh lien cau
            // truoc va doc nhu mot doan.
            b.chuCanhBao.text = cac.singleOrNull() ?: cac.joinToString("\n") { "• $it" }
        }
    }

    /** Chay moi giay. Chi doi vai dong chu, khong dung toi Firestore. */
    private fun veDongHo() {
        val tt = moiNhat ?: return
        if (_b == null) return
        veDangMo(tt)
        // Dang choi thi khuc da choi lon dan, khuc dang giu nho dan tung phut, ma tablet chi
        // day ban moi khi trang thai doi: may nay tu tinh, nhu dong ho dem nguoc o tren.
        if (tt.cong == Cong.DANG_CHOI) veNgay(tt)

        // Dang co viec nha chua xong: tablet bi che kin man hinh, khong phai dang
        // dem gio. Dem so viec chu khong de dong ho dem nguoc gi ca - khong co moc
        // nao de dem, viec het khi co nguoi bam xong, o tab Viec nha hay may ba.
        if (tt.viecNha.isNotEmpty()) {
            b.dongHo.text = "${tt.viecNha.size} việc"
            // Ten tung viec nam o dong "Viec nha chua xong" ngay duoi the.
            b.duoiDongHo.text = "chưa xong"
            b.thanhPhien.visibility = View.GONE
            return
        }

        if (tt.cheDoBaBat) {
            b.dongHo.text = if (tt.cheDoBaHetLuc > 0) {
                Dinh.dongHo(tt.cheDoBaHetLuc - System.currentTimeMillis())
            } else "∞"
            b.duoiDongHo.text =
                if (tt.cheDoBaHetLuc > 0) "còn lại của chế độ Ba Huy"
                else "không đặt hạn, nhớ tự đóng"
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
                // Thanh chay theo phien hien tai chu khong theo so phut ca ngay: moc
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

    /**
     * Cho choi ngay. Hoi lai o hai canh ma bam xong se khong thay gi doi.
     *
     * Tablet dang khong tra loi: lenh nam cho, tablet co mang lai trong nua tieng thi
     * van lam. Hay con lenh cho gio truoc chua toi tablet: tablet cong don phieu gio,
     * nen bam them la cong them chu khong phai bam lai cho chac. Hai canh do la luc
     * nguoi ta de bam lan hai nhat.
     */
    private fun cho(phut: Int) {
        val choCu = lenhCho.filter { it.kieu == Lenh.CHO }
        val im = khongDap()
        if (choCu.isEmpty() && !im) return gui(Lenh.CHO, phut = phut)

        val con = getString(R.string.child_name)
        val noi = buildString {
            if (im && dienThoaiMatMang()) {
                append("Điện thoại này đang không gửi được lệnh (mất mạng?). Lệnh sẽ nằm chờ ")
                append("trên máy này, có mạng lại mới tới tablet.")
            } else if (im) {
                // Tu 8/10/2026 tablet lam moi lenh du tre bao lau, khong con bo lenh qua nua tieng.
                append("Tablet đang không trả lời. Lệnh này sẽ nằm chờ: tablet có mạng lại ")
                append("thì $con vẫn được ${Dinh.phut(phut)}, trễ bao lâu cũng vậy.")
            }
            if (choCu.isNotEmpty()) {
                if (isNotEmpty()) append("\n\n")
                val tong = phut + choCu.sumOf { it.phut ?: 0 }
                append("Còn lệnh ${choCu.joinToString(", ") { Dinh.lenh(it) }} chưa tới tablet. ")
                append("Gửi thêm thì các lệnh cộng dồn, $con được tất cả ${Dinh.phut(tong)}.")
            }
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Vẫn cho chơi ${Dinh.phut(phut)}?")
            .setMessage(noi)
            .setPositiveButton("Vẫn gửi") { _, _ -> gui(Lenh.CHO, phut = phut) }
            .setNegativeButton(R.string.huy, null)
            .show()
    }

    /**
     * Khoi "lenh dang cho tablet": moi lenh mot dong, kem nut rut lai.
     *
     * Chi ve lai khi danh sach dong doi. Ham nay chay moi giay tu [nhip], ma dung lai
     * view moi giay thi nut dang bam do cung bi thay giua chung.
     */
    private fun veLenhCho() {
        if (_b == null) return
        val bayGio = System.currentTimeMillis()
        val hien = lenhCho.filter { it.dangHien(bayGio) }
        val ky = hien.joinToString("|") { "${it.id}:${it.chuaLenMang}" }
        if (ky == daVeLenhCho) return
        daVeLenhCho = ky

        val hop = b.hopLenhCho
        hop.removeAllViews()
        hop.visibility = if (hien.isEmpty()) View.GONE else View.VISIBLE
        val ct = requireContext()
        hien.forEach { l ->
            val luc = Dinh.gioPhut(l.tao)
            val dong = LinearLayout(ct).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            dong.addView(TextView(ct).apply {
                // Khong con dong "quá nửa tiếng, tablet sẽ bỏ qua" (bo 8/10/2026): tablet lam
                // moi lenh du tre bao lau.
                text = when {
                    l.chuaLenMang -> "Chưa gửi lên được vì điện thoại mất mạng: ${Dinh.lenh(l)}. " +
                        "Có mạng lại là tự gửi."
                    else -> "Đang chờ tablet nhận: ${Dinh.lenh(l)}, gửi lúc $luc."
                }
                textSize = 14f
                setLineSpacing(2f * resources.displayMetrics.density, 1f)
                setTextColor(ContextCompat.getColor(ct, R.color.ink_soft))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            dong.addView(
                MaterialButton(ct, null, androidx.appcompat.R.attr.borderlessButtonStyle).apply {
                    text = "Rút lại"
                    setOnClickListener {
                        isEnabled = false
                        Kho.rutLenh(ct, l.id) { kq ->
                            if (kq is Kho.KetQua.Hong) {
                                isEnabled = true
                                Dinh.noi(ct, kq.viSao)
                            }
                        }
                    }
                }
            )
            hop.addView(dong)
        }
    }

    /**
     * Gui mot lenh, va cho ca man hinh biet la dang gui.
     *
     * Khoa het nut trong luc cho: hai lenh "cho choi" lien nhau la hai phien, ma
     * nguoi bam thi tuong minh vua bam hut mot cai.
     */
    private fun gui(kieu: String, phut: Int? = null, chu: String? = null, giaTri: Any? = null) {
        if (dangGuiLenh) {
            // Hop "Gui cho tablet" cua the vo dan do khong nam trong cac nut bi khoa, nen
            // bam duoc luc lenh truoc chua len may chu. Noi ra, dung lang le bo lenh.
            context?.let { Dinh.noi(it, "Lệnh trước chưa gửi xong, đợi một chút rồi bấm lại.") }
            return
        }
        dangGuiLenh = true
        loiGui = ""
        veDuongLenh()
        Kho.guiLenh(requireContext(), kieu, phut = phut, chu = chu, giaTri = giaTri) { kq ->
            dangGuiLenh = false
            // Man hinh co the da bi go trong luc cho mang.
            if (_b == null) return@guiLenh
            loiGui = if (kq is Kho.KetQua.Hong) kq.viSao else ""
            ve()
        }
    }

    /**
     * Dong bao tinh hinh duoi cac hang nut, va khoa nut khi dang gui.
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

    /**
     * Tat het nut bam duoc trong luc mot lenh dang tren duong di, va luc chua co ban trang thai
     * nao. Ke ca nut "Xem đề" cua tung mon (1/10/2026): nut do dung bang code o [veDeThi], khong
     * co ten trong binding, nen phai di qua [cacHangDeThi].
     */
    private fun khoaNut() {
        listOf(
            b.cho15, b.cho30, b.cho45, b.choKhac, b.choBot, b.nutCapQuy,
            b.nutDung, b.nutDongMay, b.nutKhoa, b.nutMoMay, b.nutTinCo
        ).forEach { it.isEnabled = false }
        cacHangDeThi.values.forEach { it.nutXemDe.isEnabled = false }
    }

    /**
     * Cac hang "Đề thi thử <môn>" (1/10/2026): moi mon mot hang, thu tu Toan, KHTN, Tieng Anh,
     * mon khong co de thi khong co hang. Moi hang mot dong tom tat va nut "Xem đề" mo danh sach
     * de cua mon do; chu lay tu [HangDeThi]. An ca the theDeThi khi khong co de nao, ke ca khi
     * tablet chua gui danh sach de thi ([TrangThai.deThi] null): ban cu, khong hieu lenh
     * [Lenh.MO_DE_THI]. The rieng nay co tu 7/10/2026, truoc do cac hang nam trong the chinh.
     *
     * Truoc ngay do chi co mot hang Tieng Anh ve san trong layout. Nay so hang di theo du lieu
     * nen dung bang code tu item_de_thi, nhung chi dung lai khi danh sach mon doi: ham nay chay
     * moi lan Firestore goi lai (pin tablet tut mot phan tram cung la mot lan), ma dung lai view
     * thi nut dang bam do bi thay giua chung, y nhu [veLenhCho].
     *
     * Goi sau vong bat nut cua [ve] va truoc [veDuongLenh], y nhu [veQuy]: nut cua hang nao cung
     * bat o day, ke ca hang vua dung, roi lenh dang gui thi [khoaNut] tat lai. Phan chu mo di khi
     * tablet khong tra loi ([doDam]), nut van bam duoc, nhu hang quy.
     */
    private fun veDeThi(tt: TrangThai, doDam: Float) {
        val cac = HangDeThi.theoMon(tt.deThi.orEmpty())
        if (cac.map { it.mon } != cacHangDeThi.keys.toList()) {
            val lop = LayoutInflater.from(requireContext())
            b.hopDeThi.removeAllViews()
            cacHangDeThi = cac.associate { m ->
                val h = ItemDeThiBinding.inflate(lop, b.hopDeThi, false)
                // Bam thi doc lai danh sach luc bam, theo ten mon, khong giu danh sach luc dung
                // hang: hang chi dung lai khi mon doi, con tinh trang tung de doi luon.
                h.nutXemDe.setOnClickListener { hoiMoDeThi(m.mon) }
                b.hopDeThi.addView(h.root)
                m.mon to h
            }
        }
        b.theDeThi.visibility = if (cac.isEmpty()) View.GONE else View.VISIBLE
        cac.forEach { m ->
            val h = cacHangDeThi[m.mon] ?: return@forEach
            h.tieuDeDeThi.text = HangDeThi.tieuDe(m.mon)
            h.chuDeThi.text = HangDeThi.tomTat(m.cac)
            h.khoiDeThi.alpha = doDam
            h.nutXemDe.isEnabled = true
        }
    }

    /**
     * Danh sach de cua mot mon, moi de mot dong kem tinh trang tablet bao ve; chon mot de thi
     * hoi lai roi gui lenh [Lenh.MO_DE_THI].
     *
     * De dang mo thi khong gui gi. Moi de con lai deu hoi lai mot cau truoc khi gui, xem
     * [HangDeThi.hoiLai]: tablet mo ca de lop chua hoc toi, nen Ba Huy nen biet minh dang mo mot
     * de con chua hoc toi (de can toi dau, lop con thieu phan nao), hay mot de con da lam roi.
     */
    private fun hoiMoDeThi(mon: String) {
        val ds = moiNhat?.deThi.orEmpty().filter { it.mon == mon }
        if (ds.isEmpty()) return
        val con = getString(R.string.child_name)
        val dong = ds.map { HangDeThi.dong(it, con) }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(HangDeThi.tieuDe(mon))
            .setItems(dong) { _, i -> xacNhanMoDeThi(ds[i]) }
            .setNegativeButton(R.string.huy, null)
            .show()
    }

    private fun xacNhanMoDeThi(d: DeThiTT) {
        val noi = HangDeThi.hoiLai(d, getString(R.string.child_name))
            ?: return Dinh.noi(
                requireContext(),
                "${d.ten} " + (if (d.tt == DeThiTT.DANG) "đang làm" else "đang mở") + " trên tablet."
            )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Mở ${d.ten}?")
            .setMessage(noi)
            .setPositiveButton(R.string.bang_mo_de_thi) { _, _ -> gui(Lenh.MO_DE_THI, chu = d.ma) }
            .setNegativeButton(R.string.huy, null)
            .show()
    }

    /**
     * The Laptop, chay moi giay tu [nhip]: luc con dang xem, laptop chi ghi moc het gio mot lan, may
     * nay tu dem lui. Truoc 8/10/2026 day la hang "Xem Netflix" trong the chinh (7/10/2026, toi
     * chieu hom do ten "Netflix trên laptop").
     *
     * Hang mo, khoa web tung nam ngay duoi hang do; chieu 7/10/2026 anh Huy chuyen no sang tab
     * Cai dat thanh muc "Firefox chỉ được mở Netflix", xem CaiDatFragment.
     */
    private fun veLaptop() {
        // Tu 8/10/2026 hang "Xem Netflix" thanh the Laptop: so phut, dong ho, trang thai deu o day.
        theLaptop?.nhip()
    }

    /**
     * Ba Huy cho them phut Netflix, khong tru phut choi cua tablet (anh Huy chon 7/10/2026).
     * Phieu ghi thang vao laptop/{maNha}/cap, khong qua tablet; laptop nhan trong vong mot
     * phut neu dang mo. Phut chi dung trong ngay.
     *
     * Cac muc o [MUC_CAP_LAPTOP], dong cuoi "Khác" mo o go so ([hoiPhutKhacLaptop]); muc 45 phut
     * va dong do anh Huy them chieu 7/10/2026, dong do luc dau ghi "Phút khác" roi anh doi ngay.
     *
     * [bot] la nut "Bớt" cua the Laptop (9/10/2026, luc dau ghi "Bớt Netflix"; anh Huy chot "cap
     * them thi cung phai bot"): cung hop, cung muc, phieu mang so phut am, laptop bot toi da ve 0.
     */
    private fun hoiCapLaptop(bot: Boolean = false) {
        val cac = (MUC_CAP_LAPTOP.map { Dinh.phut(it) } + "Khác").toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (bot) "Bớt phút Netflix" else "Cho thêm phút Netflix")
            .setItems(cac) { _, i ->
                if (i < MUC_CAP_LAPTOP.size) capLaptop(MUC_CAP_LAPTOP[i], bot) else hoiPhutKhacLaptop(bot)
            }
            .setNegativeButton(R.string.huy, null)
            .show()
    }

    /**
     * Dong "Khác" cua [hoiCapLaptop]: go so phut, tu 1 toi [CAP_LAPTOP_TOI_DA] (anh Huy chon
     * 7/10/2026). Tran do la tran luat Firestore dat cho phieu laptop/{maNha}/cap (phut tu -600
     * toi 600, khac 0, xem firestore.rules): vuot thi phieu bi tu choi ma Ba Huy chi thay mot cau
     * loi quyen, nen chan truoc o day. So sai thi bao ngay duoi o va khong dong hop, y nhu
     * [hoiTinCo]: dong lai la mat so vua go.
     */
    private fun hoiPhutKhacLaptop(bot: Boolean = false) {
        val ct = requireContext()
        val o = EditText(ct).apply {
            hint = "Số phút"
            inputType = InputType.TYPE_CLASS_NUMBER
            setPadding(48, 32, 48, 32)
        }
        val hop = MaterialAlertDialogBuilder(ct)
            .setTitle(if (bot) "Bớt bao nhiêu phút?" else "Cho thêm bao nhiêu phút?")
            .setMessage("Từ 1 tới $CAP_LAPTOP_TOI_DA phút.")
            .setView(o)
            .setPositiveButton(if (bot) "Bớt" else "Cho", null)
            .setNegativeButton(R.string.huy, null)
            .show()
        hop.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val phut = o.text.toString().trim().toIntOrNull()
            if (phut == null || phut < 1 || phut > CAP_LAPTOP_TOI_DA) {
                o.error = "Gõ một số từ 1 tới $CAP_LAPTOP_TOI_DA."
            } else {
                hop.dismiss()
                capLaptop(phut, bot)
            }
        }
    }

    private fun capLaptop(phut: Int, bot: Boolean = false) {
        Kho.capNetflix(requireContext(), if (bot) -phut else phut) { kq ->
            val ct = context ?: return@capNetflix
            Dinh.noi(
                ct,
                when {
                    kq is Kho.KetQua.Hong -> kq.viSao
                    bot -> "Đã gửi bớt ${Dinh.phut(phut)} Netflix, laptop nhận trong khoảng một phút."
                    else -> "Đã gửi ${Dinh.phut(phut)} Netflix, laptop nhận trong khoảng một phút."
                }
            )
        }
    }

    /**
     * Cap gio tu "Quỹ giờ chơi": hoi bao nhieu, roi gui lenh [Lenh.CAP_QUY].
     *
     * Chi hien cac muc khong qua so dang co trong quy, cong mot dong cap het. Cap het thi
     * lenh khong kem so phut: tablet cap het so co luc nhan lenh, ke ca khi quy vua tang.
     * Hop chi co danh sach, khong co doan van, vi AlertDialog chi dung duoc mot trong hai,
     * xem [hoiMoMay].
     */
    private fun hoiCapQuy() {
        val quy = moiNhat?.quyGio ?: return
        if (quy <= 0) return
        val muc = MUC_CAP_QUY.filter { it <= quy }
        val cac = (muc.map { Dinh.phut(it) } + "Cấp hết (${Dinh.phut(quy)})").toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cấp từ quỹ giờ chơi")
            .setItems(cac) { _, i -> capQuy(muc.getOrNull(i)) }
            .setNegativeButton(R.string.huy, null)
            .show()
    }

    /**
     * Gui lenh cap tu quy. [phut] null la cap het.
     *
     * Hoi lai o hai canh ma bam xong se khong thay gi doi, y nhu [cho]: tablet dang khong
     * tra loi (so trong quy la so cu), hay con lenh cap tu quy truoc chua toi tablet. Tablet
     * cap ca hai lenh, quy con bao nhieu cap bay nhieu, nen bam them vi tuong lan truoc hut
     * la Le Hoa duoc gap doi.
     */
    private fun capQuy(phut: Int?) {
        val cu = lenhCho.filter { it.kieu == Lenh.CAP_QUY }
        val im = khongDap()
        if (cu.isEmpty() && !im) return gui(Lenh.CAP_QUY, phut = phut)

        val noi = buildString {
            if (im && dienThoaiMatMang()) {
                append("Điện thoại này đang không gửi được lệnh (mất mạng?). Lệnh sẽ nằm chờ ")
                append("trên máy này, có mạng lại mới tới tablet.")
            } else if (im) {
                append("Tablet đang không trả lời, số trong quỹ là lần cuối nó báo về. Lệnh sẽ ")
                append("nằm chờ: tablet có mạng lại thì cấp, trễ bao lâu cũng vậy.")
            }
            if (cu.isNotEmpty()) {
                if (isNotEmpty()) append("\n\n")
                append("Còn lệnh ${cu.joinToString(", ") { Dinh.lenh(it) }} chưa tới tablet. ")
                append("Gửi thêm thì tablet cấp cả hai lần, quỹ còn bao nhiêu cấp bấy nhiêu.")
            }
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Vẫn cấp ${phut?.let { Dinh.phut(it) } ?: "hết quỹ"}?")
            .setMessage(noi)
            .setPositiveButton("Vẫn gửi") { _, _ -> gui(Lenh.CAP_QUY, phut = phut) }
            .setNegativeButton(R.string.huy, null)
            .show()
    }

    /**
     * Nut "Khác" cua hang cho choi: mo thang o go so phut (anh Huy chot 8/10/2026). Truoc do la
     * danh sach 10, 20, 60, 90 phut; sang 8/10 co them dong "Khác" go so, roi cung ngay anh bo
     * han danh sach: ba nut 15', 30', 45' da la cac muc san.
     */
    private fun hoiSoPhut() {
        hoiGoPhut("Cho chơi bao nhiêu phút?", "Cho") { cho(it) }
    }

    /**
     * Nut "Bớt" do o cuoi hang cho choi (anh Huy chot 8/10/2026): go so phut roi gui lenh
     * [Lenh.BOT]. Bot luc nao cung duoc nhu cho them: tablet tru vao phien dang choi, phan
     * dang tam dung hay phieu chua bam Bat dau; Le Hoa khong giu phut nao thi tablet khong
     * lam gi va tra loi la khong co gio de bot (GateStore.bot ben tablet). Lenh nam cho luc
     * tablet tat thi tablet bat lai van lam, dung thu tu bam voi cac lenh cho gio.
     */
    private fun hoiPhutBot() {
        hoiGoPhut("Bớt bao nhiêu phút?", "Bớt") { gui(Lenh.BOT, phut = it) }
    }

    /**
     * Hop go so phut, tu 1 toi [GO_PHUT_TOI_DA]. So sai thi bao ngay duoi o va khong dong hop,
     * y nhu [hoiPhutKhacLaptop]: dong lai la mat so vua go.
     */
    private fun hoiGoPhut(tieuDe: String, chuNut: String, lam: (Int) -> Unit) {
        val ct = requireContext()
        val o = EditText(ct).apply {
            hint = "Số phút"
            inputType = InputType.TYPE_CLASS_NUMBER
            setPadding(48, 32, 48, 32)
        }
        val hop = MaterialAlertDialogBuilder(ct)
            .setTitle(tieuDe)
            .setMessage("Từ 1 tới $GO_PHUT_TOI_DA phút.")
            .setView(o)
            .setPositiveButton(chuNut, null)
            .setNegativeButton(R.string.huy, null)
            .show()
        hop.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val phut = o.text.toString().trim().toIntOrNull()
            if (phut == null || phut < 1 || phut > GO_PHUT_TOI_DA) {
                o.error = "Gõ một số từ 1 tới $GO_PHUT_TOI_DA."
            } else {
                hop.dismiss()
                lam(phut)
            }
        }
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
            "Không đặt hạn, nhớ tự đóng"
        )
        val so = arrayOf(15, 30, 60, null)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Mở toàn bộ máy, bỏ hết chặn")
            .setItems(cac) { _, i -> gui(Lenh.MO_MAY, phut = so[i]) }
            .setNegativeButton(R.string.huy, null)
            .show()
    }

    // ------------------------------------------------------------ vo dan do

    /**
     * The "Bài dặn dò sắp tới": moi buoi mot khoi, buoi som truoc. Tablet tinh han va nhac
     * Le Hoa tu hom truoc buoi do (30/9/2026); o day chi de Ba Huy thay cung danh sach.
     *
     * Moi dong mot dau "•", bai tap hay dan do khac cung vay (Ba Huy chon 30/9/2026, ca ben
     * tablet). Bai tap van dung truoc.
     */
    private fun veNhacBai(ds: List<NhacBaiBuoi>) {
        b.theNhacBai.visibility = if (ds.isEmpty()) View.GONE else View.VISIBLE
        if (ds.isEmpty()) return
        b.chuNhacBai.text = ds.joinToString("\n\n") { buoi ->
            buildString {
                append(buoi.ten.replaceFirstChar { it.uppercase() })
                (buoi.cacBai + buoi.dongKhac).forEach { d ->
                    append("\n• ").append(d.chu)
                    Dinh.ngayNgan(d.ngayVo)?.let { append(" (vở ").append(it).append(')') }
                }
            }
        }
    }

    // ----------------------------------------------------------- tin cua co

    /**
     * Dua mot tin cua co giao len man chinh tablet.
     *
     * Ba Huy chep tin trong nhom lop Zalo roi bam nut nay: chu trong bo nho tam dien
     * san vao o, sua bot duoc truoc khi gui. Bo nho tam khong co chu thi o de trong
     * cho go tay. Chi doc bo nho tam luc Ba Huy bam nut, y nhu nut dan ket qua Claude.
     *
     * Nut Dua len tablet khong dong hop thoai khi o con trong hay tin dai qua: dong
     * lai la mat chu vua dan, ma chu do Ba Huy co the da sua bot.
     */
    private fun hoiTinCo() {
        val ct = requireContext()
        val o = EditText(ct).apply {
            setText(chuBoNhoTam())
            hint = "Dán hoặc gõ tin của cô"
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            gravity = Gravity.TOP or Gravity.START
            minLines = 3
            maxLines = 10
            setPadding(48, 32, 48, 32)
        }
        val hop = MaterialAlertDialogBuilder(ct)
            .setTitle("Tin của cô giáo")
            .setMessage(
                "Tin hiện ở màn chính tablet, máy kêu báo cho " +
                    "${getString(R.string.child_name)}. Tablet đang tắt thì tin chờ đến " +
                    "lúc mở máy."
            )
            .setView(o)
            .setPositiveButton("Đưa lên tablet", null)
            .setNegativeButton(R.string.huy, null)
            .show()
        hop.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val chu = o.text.toString().trim()
            when {
                chu.isEmpty() -> o.error = "Chưa có chữ nào."
                chu.length > TIN_CO_TOI_DA ->
                    o.error = "Tin dài ${chu.length} ký tự, cắt bớt còn dưới $TIN_CO_TOI_DA."
                else -> {
                    hop.dismiss()
                    gui(Lenh.TIN_CO, chu = chu)
                }
            }
        }
    }

    /** Chu dang nam trong bo nho tam, rong neu khong co. */
    private fun chuBoNhoTam(): String {
        val ct = requireContext()
        return ct.getSystemService(ClipboardManager::class.java)?.primaryClip
            ?.takeIf { it.itemCount > 0 }?.getItemAt(0)
            ?.coerceToText(ct)?.toString()?.trim().orEmpty()
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

        /** Do dam cua phan so lieu trong the chinh khi so do la so cu. */
        private const val ALPHA_SO_CU = 0.5f

        /**
         * Cac muc trong hop cap tu quy, xem [hoiCapQuy]. Cung nhip 15 phut voi hang Cho choi
         * ngay; muc nao lon hon so trong quy thi an.
         */
        private val MUC_CAP_QUY = listOf(15, 30, 45, 60)

        /** So phut Netflix Ba Huy cho them, xem [hoiCapLaptop]. Muc 45 them chieu 7/10/2026. */
        private val MUC_CAP_LAPTOP = listOf(15, 30, 45, 60)

        /**
         * So phut lon nhat go duoc o dong "Khác", xem [hoiPhutKhacLaptop]. Phai bang tran
         * request.resource.data.phut <= 600 cua phieu cap trong firestore.rules.
         */
        private const val CAP_LAPTOP_TOI_DA = 600

        /**
         * So phut lon nhat go duoc o o "Khác" cua hop cho choi va o hop "Bớt" (8/10/2026).
         * Bang tran tablet tu cat: GateStore.approve gioi han phieu 1 toi 600 phut.
         */
        private const val GO_PHUT_TOI_DA = 600

        /** Cho ban trang thai tu may chu toi da bay lau roi van hoi tablet. */
        private const val CHO_BAN_DAU_MS = 5_000L

        /**
         * Hai lan hoi cach nhau it nhat bay nhieu.
         *
         * Doi qua tab Bai tap roi quay lai cung goi onStart, ma moi lan hoi la mot
         * document trong hang lenh cua tablet. Nua phut la du ngan de so lieu khong
         * bao gio cu, du dai de nghich thanh tab khong sinh ra mot tram lenh.
         *
         * Man dung app va tab Nhat ky cung giu khoang nay, tinh tu lan hoi cua bat ky man
         * nao, xem [SuDungActivity], [NhatKyFragment].
         */
        internal const val GIAN_HOI_MS = 30_000L

        /**
         * Tin cua co dai nhat bao nhieu ky tu.
         *
         * Tin that trong nhom lop chi vai tram chu. Tran nay chan mot lan dan nham ca
         * mot van ban dai: tablet giu tin trong file cai dat chung, va file do ghi lai
         * ca cuc moi lan app doi bat ky muc nao.
         */
        private const val TIN_CO_TOI_DA = 3000
    }
}
