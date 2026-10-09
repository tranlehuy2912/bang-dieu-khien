package vn.huytl.bangdieukhien.ui

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
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
import vn.huytl.bangdieukhien.data.TinhTrangLaptop
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

    /** Nghe laptop/{maNha} cho muc "Khoá web", xem [veNhomLaptop]. */
    private var ngheLaptop: ListenerRegistration? = null

    /** Dang cho tablet vua cai lai app xin vao nha. Xem [noiLaiTablet]. */
    private var ngheXin: ListenerRegistration? = null

    private var caiDat: CaiDat? = null
    private var dsApp: List<AppTrenMay> = emptyList()

    /** null la laptop chua noi vao nha (chua co document laptop/{maNha}). */
    private var laptop: TinhTrangLaptop? = null

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
        ngheLaptop = Kho.ngheLaptop(requireContext()) {
            laptop = it
            ve()
        }
        ve()
    }

    override fun onStop() {
        ngheCaiDat?.remove()
        ngheApp?.remove()
        ngheLaptop?.remove()
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
            veNhomLaptop(null)
            veMucMay()
            return
        }

        b.than.addView(tieu("Giờ chơi"))
        b.than.addView(nhom(
            // Khong con muc "Tối đa mỗi ngày" (bo ngay 29/9/2026). Tablet bo tran chung 135
            // phut: moi phan co tran rieng (vo dan do 45, lam tren may 90, on lai 30, Kiem tra
            // bai 20, Do tu vung 30, tong 215), phan lam tren may vuot tran thi vao Quy gio
            // choi. Tablet nhan lenh CAIDAT tranPhutMoiNgay chi tra loi la khong con dung, nen
            // de muc nay lai la mot nut bam khong doi duoc gi.
            muc("Giờ ngủ", Dinh.gio(c.gioNgu), "Quá giờ này là không cấp thêm phút nào") {
                hoiGio(c.gioNgu) { guiCaiDat("gioNgu", it) }
            },
            muc("Giờ dậy", Dinh.gio(c.gioDay), "Trước giờ này máy vẫn khoá") {
                hoiGio(c.gioDay) { guiCaiDat("gioDay", it) }
            }
            // Muc ti le doi Netflix (luc do ten "Đổi sang Netflix") o day tu sang toi chieu
            // 7/10/2026, roi chuyen xuong nhom Laptop ngay duoi, xem [veNhomLaptop].
        ))

        veNhomLaptop(c)

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
            mucBatTat("Khoá màn Cài đặt", c.khoaCaiDat, "Chặn Lê Hòa vào Cài đặt của máy") {
                guiCaiDat("khoaCaiDat", !c.khoaCaiDat)
            }
        ))

        // Khong con muc "Chấm bài bằng AI trên tablet" (bo ngay 28/9/2026): tablet khong con
        // may cham nao de bat tat, bai nao cung cham bang Claude o tab Bai.

        veMucMay()
    }

    /**
     * Nhom "Laptop" (anh Huy chon chieu 7/10/2026): ti le doi phut choi sang phut xem phim, va muc
     * bat tat "Khoá web" (truoc do la hang chu kem nut "Mở web" / "Khoá web" o tab Gio choi, chu
     * luc khoa la "Web khoá, Firefox chỉ vào Netflix.").
     *
     * Ten hai muc anh Huy chot 9/10/2026, khi laptop xem duoc ca Netflix lan YouTube: "Tỷ lệ đổi
     * phút phim" (truoc la "Tỷ lệ đổi Netflix") va "Khoá web" (truoc la "Firefox chỉ được mở
     * Netflix", da sai tu luc luat chan web cho them YouTube).
     *
     * Ti le la cai dat cua tablet, gui lenh CAIDAT nhu moi muc khac; tablet ban cu khong gui
     * truong nay thi an muc. Muc Firefox thi ghi thang vao laptop/{maNha} qua [Kho.datMoWeb],
     * khong qua tablet, nen van hien khi tablet chua gui cau hinh ([c] null); laptop chua noi
     * vao nha thi an muc do. Khong con muc nao thi khong co nhom.
     */
    private fun veNhomLaptop(c: CaiDat?) {
        val cac = listOfNotNull(
            c?.tiLeNetflix?.let { tiLe ->
                // Anh Huy doi chu ngay 7/10/2026: ten cu "Đổi sang Netflix", so cu "1 phút chơi =
                // 2 phút" dai qua, dong nho cu "Lê Hòa đổi phút chơi tablet lấy phút xem trên
                // laptop". Hop chon khi cham van hoi "1 phút chơi đổi được" N phút xem phim.
                muc("Tỷ lệ đổi phút phim", "1:$tiLe", "Đổi phút chơi sang phút xem phim") {
                    hoiTiLe(tiLe) { guiCaiDat("tiLeNetflix", it) }
                }
            },
            laptop?.let { l ->
                // Bat la khoa web, tuc moWeb false.
                mucBatTat("Khoá web", !l.moWeb, chuWeb(l)) {
                    doiWebLaptop(l)
                }
            }
        )
        if (cac.isEmpty()) return
        b.than.addView(tieu("Laptop"))
        b.than.addView(nhom(*cac.toTypedArray()))
    }

    /**
     * Dong nho duoi muc "Khoá web". Chu "Bật" / "Tắt" ben phai la dieu Ba Huy
     * chon (truong moWeb), con dong nay la dieu laptop bao da lam (truong webDangMo). Hai dieu do
     * lech nhau mot luc: laptop hoi Firestore moi phut, va khong mo web luc Le Hoa dang dung.
     */
    private fun chuWeb(l: TinhTrangLaptop): String = when {
        l.moWeb && l.webDangMo -> "Web đang mở. Mở lại Firefox thì mới theo."
        l.moWeb && l.dangDung -> "Lê Hòa đang dùng laptop nên web vẫn khoá."
        l.moWeb -> "Đang chờ laptop mở web."
        l.webDangMo -> "Đang chờ laptop khoá web."
        // Luat chan web cua Firefox ap cho ca may, ke ca tai khoan Admin cua Ba Huy; tu 9/10/2026
        // cho ca YouTube (truoc chi Netflix, chu luc do la "Cả máy, kể cả tài khoản của Ba").
        else -> "Chỉ mở được Netflix, YouTube. Cả tài khoản Admin."
    }

    /**
     * Bat tat web cua laptop. Khong hoi lai, giong muc "Khoá màn Cài đặt": bam lan nua la doi
     * nguoc.
     */
    private fun doiWebLaptop(l: TinhTrangLaptop) {
        Kho.datMoWeb(requireContext(), !l.moWeb) { kq ->
            val ct = context ?: return@datMoWeb
            if (kq is Kho.KetQua.Hong) Dinh.noi(ct, kq.viSao)
        }
    }

    private fun chepMaNha() {
        val ma = Nha.maNha(requireContext())
        if (ma.isBlank()) return
        Dinh.chep(requireContext(), "Mã nhà", ma, "Đã chép mã nhà.")
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

    /** Chon ti le doi phut xem phim, 1 den 5 phut cho moi phut choi (tablet nhan toi 10). */
    private fun hoiTiLe(dangLa: Int, xong: (Int) -> Unit) {
        val cac = (1..5).toList()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("1 phút chơi đổi được")
            .setSingleChoiceItems(
                cac.map { "$it phút xem phim" }.toTypedArray(), cac.indexOf(dangLa)
            ) { d, i ->
                d.dismiss()
                xong(cac[i])
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

    /**
     * Muc bat tat: cong tac gat ben phai thay cho chu "Bật" / "Tắt" (anh Huy doi 9/10/2026, cho hai
     * muc "Khoá màn Cài đặt" va "Khoá web").
     *
     * Cong tac ve dieu dang chay that, giong chu Bat/Tat cu: "Khoá màn Cài đặt" theo ban tablet
     * ghi lai sau khi nhan lenh, "Khoá web" theo dieu Ba Huy da chon tren laptop/{maNha}. Bam
     * thi gui lenh, cong tac chi gat khi ban moi ve, thuong trong vai giay. Cho gat ngay luc bam
     * thi tablet dang tat mang van hien la da khoa, trai voi cach ca man nay chi hien so that.
     */
    private fun mucBatTat(ten: String, bat: Boolean, phu: String?, bam: () -> Unit): View {
        val v = ItemMucBinding.bind(muc(ten, "", phu, bam))
        v.giaTri.visibility = View.GONE
        v.congTac.visibility = View.VISIBLE
        v.congTac.isChecked = bat
        v.root.contentDescription = "$ten, ${if (bat) "đang bật" else "đang tắt"}"
        return v.root
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
}
