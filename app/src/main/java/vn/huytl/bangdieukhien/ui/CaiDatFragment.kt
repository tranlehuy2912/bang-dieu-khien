package vn.huytl.bangdieukhien.ui

import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.ListenerRegistration
import vn.huytl.bangdieukhien.R
import vn.huytl.bangdieukhien.data.AppTrenMay
import vn.huytl.bangdieukhien.data.CaiDat
import vn.huytl.bangdieukhien.data.Kho
import vn.huytl.bangdieukhien.data.Lenh
import vn.huytl.bangdieukhien.data.Nha
import vn.huytl.bangdieukhien.databinding.FragmentCaiDatBinding
import vn.huytl.bangdieukhien.databinding.ItemMucBinding

/**
 * Chinh cau hinh tablet tu xa.
 *
 * Man hinh nay khong ghi thang xuong Firestore. No gui lenh CAIDAT, tablet nhan,
 * ap vao Prefs cua no, roi ghi lai ban sao cho cho nay doc. Mot nguoi ghi mot cho
 * thi khong bao gio co canh hai may dap len nhau, va so hien o day luon la so that
 * dang chay chu khong phai so minh vua mong muon.
 */
class CaiDatFragment : Fragment() {

    private var _b: FragmentCaiDatBinding? = null
    private val b get() = _b!!
    private var ngheCaiDat: ListenerRegistration? = null
    private var ngheApp: ListenerRegistration? = null

    /** Dang cho tablet vua cai lai app xin vao nha. Xem [noiLaiTablet]. */
    private var ngheXin: ListenerRegistration? = null

    private var caiDat: CaiDat? = null
    private var dsApp: List<AppTrenMay> = emptyList()

    /** Hop "Giờ riêng từng app" dang mo, va cach ve lai no khi tablet ghi so moi. */
    private var hopGioRieng: AlertDialog? = null
    private var veGioRieng: (() -> Unit)? = null

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentCaiDatBinding.inflate(i, c, false)
        return b.root
    }

    override fun onStart() {
        super.onStart()
        ngheCaiDat = Kho.ngheCaiDat(requireContext()) {
            caiDat = it
            ve()
        }
        ngheApp = Kho.ngheDanhSachApp(requireContext()) {
            dsApp = it
            ve()
        }
        ve()
    }

    override fun onStop() {
        ngheCaiDat?.remove()
        ngheApp?.remove()
        super.onStop()
    }

    override fun onDestroyView() {
        ngheXin?.remove()
        ngheXin = null
        hopGioRieng?.dismiss()
        _b = null
        super.onDestroyView()
    }

    private fun ve() {
        if (_b == null) return
        veGioRieng?.invoke()
        b.than.removeAllViews()
        val c = caiDat

        if (c == null) {
            b.than.addView(tieu("Tablet chưa gửi cấu hình sang. Chờ máy đó lên mạng một lần."))
            veMucMay()
            return
        }

        b.than.addView(tieu("Giờ chơi"))
        b.than.addView(nhom(
            muc("Tối đa mỗi ngày", Dinh.phut(c.tranPhutMoiNgay), "Duyệt bài không vượt quá số này") {
                hoiSo("Tối đa phút mỗi ngày", c.tranPhutMoiNgay, 15, 480) {
                    guiCaiDat("tranPhutMoiNgay", it)
                }
            },
            muc("Giờ ngủ", Dinh.gio(c.gioNgu), "Quá giờ này là không cấp thêm phút nào") {
                hoiGio(c.gioNgu) { guiCaiDat("gioNgu", it) }
            },
            muc("Giờ dậy", Dinh.gio(c.gioDay), "Trước giờ này máy vẫn khoá") {
                hoiGio(c.gioDay) { guiCaiDat("gioDay", it) }
            }
        ))

        b.than.addView(tieu("Ứng dụng"))
        b.than.addView(nhom(
            // Ten cu "App duoc choi · Mo duoc trong gio choi" noi nguoc nghia: trong gio
            // choi thi app nao cung mo duoc. Day la danh sach mo duoc ca khi HET gio
            // choi, tru gio ngu va gio di hoc. Goi dung ten ben tablet de hai may noi
            // cung mot cau.
            //
            // Truoc 27/9/2026 muc nay ten "App luon duoc dung". Doi ten khi co them muc
            // "Dung moi luc" ngay duoi: hai chu "luon" va "moi luc" dat canh nhau la nham.
            muc("Dùng khi hết giờ chơi", "${c.appChoPhep.size} app", "Trừ giờ ngủ và giờ đi học") {
                chonApp("Dùng khi hết giờ chơi", c.appChoPhep) { guiCaiDat("appChoPhep", it) }
            },
            // Khong bao gio khoa theo gio, ke ca gio ngu, gio hoc va luc lam viec nha. Them
            // cho Telegram: Le Hoa nhan tin voi ba bang Telegram that. Tablet ban cu khong
            // biet muc nay thi tra loi "Khong co muc cai dat", va hop/caidat khong co
            // truong appMoiLuc nen dem ra 0 app.
            muc("Dùng mọi lúc", "${c.appMoiLuc.size} app", "Kể cả giờ ngủ, giờ học, lúc làm việc nhà") {
                chonApp("Dùng mọi lúc", c.appMoiLuc) { guiCaiDat("appMoiLuc", it) }
            },
            // Hai muc duoi co tu 27/9/2026, cung thu tu voi Cai dat tren tablet. Truoc do
            // may nay doc gioiHanApp ma khong hien o dau, con appNhac thi tablet chua gui.
            muc("Giờ riêng từng app", "${c.gioiHanApp.size} app",
                "Hết số phút là app khoá, kể cả trong giờ chơi") { moGioRieng() },
            // Tablet ban cu khong biet muc nay thi tra loi "Khong co muc cai dat", va
            // hop/caidat khong co truong appNhac nen dem ra 0 app.
            muc("App được nghe nền", "${c.appNhac.size} app",
                "Phát tiếng khi hết giờ chơi, trừ giờ ngủ, giờ học") {
                chonApp("App được nghe nền", c.appNhac) { guiCaiDat("appNhac", it) }
            },
            muc("App chặn hẳn", "${c.appChan.size} app", "Không mở được kể cả trong giờ chơi") {
                chonApp("App chặn hẳn", c.appChan) { guiCaiDat("appChan", it) }
            },
            // Tablet cat mang cac app nay bang VPN luc bi khoa, ke ca khi app chay nen nhu
            // cua so noi cua YouTube. Tablet ban cu tra loi "Khong co muc cai dat".
            muc("Cắt mạng khi bị khoá", "${c.appCatMang.size} app",
                "Mất mạng lúc hết giờ chơi, giờ ngủ, giờ học") {
                chonApp("Cắt mạng khi bị khoá", c.appCatMang) { guiCaiDat("appCatMang", it) }
            },
            muc("App AI ghi câu hỏi", "${c.appAi.size} app", "Ghi lại câu Lê Hòa hỏi AI") {
                chonApp("App AI ghi câu hỏi", c.appAi) { guiCaiDat("appAi", it) }
            },
            muc("Khoá màn Cài đặt", if (c.khoaCaiDat) "Bật" else "Tắt",
                "Chặn Lê Hòa vào Cài đặt của máy") {
                guiCaiDat("khoaCaiDat", !c.khoaCaiDat)
            }
        ))

        b.than.addView(tieu("Chấm bài"))
        b.than.addView(nhom(
            muc("Chấm bài bằng AI trên tablet", if (c.chamBangAi) "Bật" else "Tắt",
                if (c.chamBangAi) "Máy tự chấm và tự cộng giờ" else "Ba chấm bằng Claude ở tab Bài") {
                hoiChamBangAi(c.chamBangAi)
            }
        ))

        veMucMay()
    }

    /**
     * Hoi lai truoc khi doi cach cham, vi hai cach khac nhau o nhung cho de quen.
     *
     * Tat AI thi bai nop nam cho, khong ai cham cho den khi Ba Huy dan ket qua Claude.
     * Vo dan do van nhu luc bat: con chup mot lan o man vo dan do, may doc cho con soat,
     * va Claude cham theo dung danh sach do, nen tron goi 45 phut van tinh. May doc vo
     * hong thi tablet gan tam anh vo vao bai, va Claude tu doc anh.
     */
    private fun hoiChamBangAi(dangBat: Boolean) {
        val con = getString(R.string.child_name)
        val (tieuDe, noi, nut) = if (dangBat) {
            Triple(
                "Tắt chấm bằng AI?",
                "Bài $con nộp sẽ nằm chờ, máy không tự chấm và không tự cộng giờ. Mỗi bài, " +
                    "mở tab Bài, bấm Nhờ Claude chấm, rồi dán kết quả của Claude về.\n\n" +
                    "Vở dặn dò vẫn như cũ: $con chụp một lần đầu buổi, máy đọc cho $con soát, " +
                    "và Claude chấm theo đúng danh sách $con đã soát. Gói 45 phút làm hết bài " +
                    "cô giao vẫn tính như cũ.",
                "Tắt"
            )
        } else {
            Triple(
                "Bật lại chấm bằng AI?",
                "Máy sẽ tự chấm và tự cộng giờ mỗi lần $con nộp. Nút Nhờ Claude chấm lại " +
                    "vẫn dùng được khi thấy máy chấm nhầm.",
                "Bật"
            )
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(tieuDe)
            .setMessage(noi)
            .setNegativeButton(R.string.huy, null)
            .setPositiveButton(nut) { _, _ -> guiCaiDat("chamBangAi", !dangBat) }
            .show()
    }

    private fun chepMaNha() {
        val ma = Nha.maNha(requireContext())
        if (ma.isBlank()) return
        val bang = requireContext()
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        bang.setPrimaryClip(ClipData.newPlainText("Mã nhà", ma))
        Dinh.noi(requireContext(), "Đã chép mã nhà.")
    }

    /** May muc thuoc ve chinh dien thoai nay, khong gui di dau ca. */
    private fun veMucMay() {
        b.than.addView(tieu("Máy này"))
        b.than.addView(nhom(
            muc("Token bot Telegram", if (Nha.token(requireContext()).isBlank()) "chưa đặt" else "đã đặt",
                "Chỉ dùng để tải ảnh bài tập về xem") { hoiToken() },
            // Bam vao la chep. Truoc day dong nay bam khong ra gi ca, ma van loe len
            // mot cai vi no nam trong cung mot the co nen bam duoc - va ma nha thi
            // dung la thu thinh thoang phai go sang may khac.
            muc(
                "Mã nhà", Nha.maNha(requireContext()).take(8) + "…",
                "Chạm để chép mã đầy đủ"
            ) { chepMaNha() },
            muc("Ghép đôi lại", "", "Nối máy này với tablet một lần nữa") {
                startActivity(Intent(requireContext(), GhepDoiActivity::class.java))
            },
            muc(
                "Nối lại máy tính bảng", "",
                "Khi tablet vừa cài lại app và mất hết dữ liệu"
            ) { noiLaiTablet() }
        ))

        b.than.addView(tieu("Nguy hiểm"))
        b.than.addView(nhom(
            muc("Xoá mã PIN trên tablet", "", "Xoá xong đặt lại PIN mới trên chính tablet") {
                xacNhan("Xoá mã PIN trên tablet?", "Máy sẽ mở khoá và chờ đặt PIN mới.") {
                    Kho.guiLenh(requireContext(), Lenh.XOA_PIN)
                }
            },
            muc("Tắt quản trị thiết bị", "", "Để gỡ được app Nộp bài khỏi tablet") {
                xacNhan(
                    "Tắt quản trị thiết bị?",
                    "Tablet mở 15 phút và gỡ được app. Chỉ làm khi thật sự muốn gỡ."
                ) { Kho.guiLenh(requireContext(), Lenh.CHO_GO_APP) }
            }
        ))
    }

    /**
     * Phat ma ghep cho tablet vua cai lai app, roi ngoi cho no xin vao.
     *
     * NGUOC CHIEU voi man ghep doi cu. Binh thuong tablet lap nha va ket nap may nay;
     * nhung khi tablet vua bi cai lai thi no mat sach - mat ma nha, mat ca tu cach
     * nguoi nha tren Firestore. Luc do may nay la may duy nhat con trong nha, nen no
     * phai lam nguoi giu cua.
     *
     * Hien ca MA NHA day du chu khong cat bot: day la luc Ba Huy phai go lai ma do
     * sang tablet, ma tam chu "abc123…" thi go kieu gi.
     */
    private fun noiLaiTablet() {
        val maNha = Nha.maNha(requireContext())
        if (maNha.isEmpty()) {
            Dinh.noi(requireContext(), "Máy này chưa ghép với tablet.")
            return
        }
        Kho.taoMaGhepChoTablet(requireContext()) { ma, loi ->
            if (ma.isEmpty()) {
                Dinh.noi(requireContext(), loi.ifBlank { "Không tạo được mã ghép." })
                return@taoMaGhepChoTablet
            }
            val hop = MaterialAlertDialogBuilder(requireContext())
                .setTitle("Nối lại máy tính bảng")
                .setMessage(
                    "Trên tablet vào Cài đặt → Khôi phục sau khi cài lại app, gõ:\n\n" +
                        "Mã nhà:  $maNha\n" +
                        "Mã ghép: $ma\n\n" +
                        "Mã sống 10 phút. Để màn này mở đến khi tablet nối được."
                )
                .setPositiveButton("Đóng", null)
                .setCancelable(false)
                .show()

            ngheXin?.remove()
            ngheXin = Kho.ngheXinVao(requireContext()) { duoc ->
                if (!duoc) return@ngheXinVao
                ngheXin?.remove()
                ngheXin = null
                hop.setMessage("Đã nối lại máy tính bảng. Sổ cũ sẽ được kéo về ngay trên tablet.")
                Dinh.noi(requireContext(), "Đã nối lại máy tính bảng")
            }
        }
    }

    // ------------------------------------------------------------- hoi va gui

    private fun guiCaiDat(ten: String, giaTri: Any) {
        Kho.guiLenh(requireContext(), Lenh.CAI_DAT, chu = ten, giaTri = giaTri) { kq ->
            if (kq is Kho.KetQua.Hong) Dinh.noi(requireContext(), kq.viSao)
        }
    }

    private fun hoiSo(tieuDe: String, dangLa: Int, thapNhat: Int, caoNhat: Int, xong: (Int) -> Unit) {
        val o = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(dangLa.toString())
            setPadding(48, 32, 48, 32)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(tieuDe)
            .setView(o)
            .setPositiveButton(R.string.xong) { _, _ ->
                val so = o.text.toString().toIntOrNull()
                if (so == null || so !in thapNhat..caoNhat) {
                    Dinh.noi(requireContext(), "Gõ một số từ $thapNhat đến $caoNhat.")
                } else {
                    xong(so)
                }
            }
            .setNegativeButton(R.string.huy, null)
            .show()
    }

    private fun hoiGio(dangLa: Int, xong: (Int) -> Unit) {
        TimePickerDialog(
            requireContext(),
            { _, gio, phut -> xong(gio * 60 + phut) },
            dangLa / 60, dangLa % 60, true
        ).show()
    }

    /**
     * Chon app tu danh sach tablet gui sang.
     *
     * Truoc day muon them mot app phai go dung ten goi kieu com.mojang.minecraftpe
     * vao Telegram. Go sai mot chu thi khong bao loi gi ca, chi la app do khong bao
     * gio duoc chan - ma mai sau moi phat hien.
     */
    private fun chonApp(tieuDe: String, dangChon: List<String>, xong: (List<String>) -> Unit) {
        if (dsApp.isEmpty()) {
            Dinh.noi(requireContext(), "Tablet chưa gửi danh sách app sang.")
            return
        }
        val ten = dsApp.map { it.ten }.toTypedArray()
        val da = dsApp.map { it.goi in dangChon }.toBooleanArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(tieuDe)
            .setMultiChoiceItems(ten, da) { _, i, chon -> da[i] = chon }
            .setPositiveButton(R.string.xong) { _, _ ->
                // Giu ca nhung goi dang chon ma danh sach app o day khong co. Tablet
                // chi gui danh sach do luc dich vu vua bat, nen app cai sau luc do
                // khong nam trong nay; bo chung di thi bam Xong mot cai la app do
                // roi khoi danh sach tren tablet ma khong ai hay.
                val khongCo = dangChon.filter { goi -> dsApp.none { it.goi == goi } }
                xong(dsApp.filterIndexed { i, _ -> da[i] }.map { it.goi } + khongCo)
            }
            .setNegativeButton(R.string.huy, null)
            .show()
    }

    /**
     * Hop dat so phut moi ngay cho tung app. Xem [GioRieng].
     *
     * Cham mot app la hoi so phut, chon xong la gui ngay, khong doi bam Xong: moi app
     * mot con so rieng, khong co chuyen chon ca cum roi luu mot lan nhu cac danh sach
     * app. Hop giu nguyen sau khi chon, va dong cua app do doi so khi tablet ghi lai
     * hop/caidat. Tablet dang mat mang thi so cu nam nguyen, tab Bang bao lenh dang cho.
     */
    private fun moGioRieng() {
        val c = caiDat ?: return
        // Cham hai lan lien thi hop thu hai de len hop dau, va luc hop dau dong no xoa
        // mat cach ve lai cua hop thu hai.
        if (hopGioRieng?.isShowing == true) return
        if (dsApp.isEmpty()) {
            Dinh.noi(requireContext(), "Tablet chưa gửi danh sách app sang.")
            return
        }
        val thuTu = GioRieng.thuTu(dsApp, c.gioiHanApp)
        var cacDong = GioRieng.cacDong(thuTu, dsApp, c.gioiHanApp)

        val bang = object : BaseAdapter() {
            override fun getCount() = cacDong.size
            override fun getItem(i: Int) = cacDong[i]
            override fun getItemId(i: Int) = i.toLong()

            override fun getView(i: Int, cu: View?, cha: ViewGroup): View {
                val v = if (cu == null) {
                    ItemMucBinding.inflate(layoutInflater, cha, false)
                } else {
                    ItemMucBinding.bind(cu)
                }
                val d = cacDong[i]
                // Le ngang bang tieu de hop, khong phai bang le cua the o man Cai dat.
                v.root.setPaddingRelative(24.dp(), v.root.paddingTop, 24.dp(), v.root.paddingBottom)
                v.ten.text = d.ten
                v.giaTri.text = if (d.phut > 0) Dinh.phut(d.phut) else ""
                val cd = caiDat
                if (d.phut > 0 && cd != null) {
                    v.phu.visibility = View.VISIBLE
                    v.phu.text = GioRieng.khiHetGio(d.goi, cd)
                } else {
                    v.phu.visibility = View.GONE
                }
                v.root.setOnClickListener { hoiGioRieng(d) }
                return v.root
            }
        }

        veGioRieng = {
            caiDat?.let { cacDong = GioRieng.cacDong(thuTu, dsApp, it.gioiHanApp) }
            bang.notifyDataSetChanged()
        }
        hopGioRieng = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Giờ riêng từng app")
            .setAdapter(bang, null)
            .setPositiveButton("Đóng", null)
            .setOnDismissListener {
                hopGioRieng = null
                veGioRieng = null
            }
            .show()
    }

    /** Hoi so phut moi ngay cho mot app, cung cac muc voi man tren tablet. */
    private fun hoiGioRieng(d: GioRieng.Dong) {
        val muc = GioRieng.cacMuc(d.phut)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(d.ten)
            .setSingleChoiceItems(
                muc.map { GioRieng.tenMuc(it) }.toTypedArray(),
                muc.indexOf(d.phut)
            ) { hop, i ->
                hop.dismiss()
                if (muc[i] != d.phut) guiCaiDat("gioiHanApp", mapOf(d.goi to muc[i]))
            }
            .setNegativeButton(R.string.huy, null)
            .show()
    }

    private fun hoiToken() {
        val o = EditText(requireContext()).apply {
            setText(Nha.token(requireContext()))
            setPadding(48, 32, 48, 32)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Token bot Telegram")
            .setMessage("Lấy trong @BotFather. App này chỉ dùng token để tải ảnh bài tập về.")
            .setView(o)
            .setPositiveButton(R.string.xong) { _, _ ->
                Nha.datToken(requireContext(), o.text.toString())
                ve()
            }
            .setNegativeButton(R.string.huy, null)
            .show()
    }

    private fun xacNhan(tieuDe: String, chu: String, lam: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(tieuDe)
            .setMessage(chu)
            .setPositiveButton("Làm") { _, _ -> lam() }
            .setNegativeButton(R.string.huy, null)
            .show()
    }

    // -------------------------------------------------------------- ve vat

    private fun tieu(chu: String): View = TextView(requireContext()).apply {
        text = chu
        setTextAppearance(R.style.Chu_Tieu)
        setPadding(4.dp(), 20.dp(), 4.dp(), 8.dp())
    }

    private fun nhom(vararg cac: View): View {
        val the = MaterialCardView(requireContext()).apply {
            radius = 20.dp().toFloat()
            strokeWidth = 1
            strokeColor = ContextCompat.getColor(context, R.color.line)
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface))
            cardElevation = 0f
        }
        val cot = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 6.dp(), 0, 6.dp())
        }
        cac.forEach { cot.addView(it) }
        the.addView(cot)
        return the
    }

    private fun muc(ten: String, giaTri: String, phu: String?, bam: () -> Unit): View {
        val v = ItemMucBinding.inflate(layoutInflater)
        v.ten.text = ten
        v.giaTri.text = giaTri
        if (phu.isNullOrBlank()) {
            v.phu.visibility = View.GONE
        } else {
            v.phu.visibility = View.VISIBLE
            v.phu.text = phu
        }
        v.root.setOnClickListener { bam() }
        return v.root
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
}
