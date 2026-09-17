package vn.huytl.bangdieukhien.ui

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
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
        _b = null
        super.onDestroyView()
    }

    private fun ve() {
        if (_b == null) return
        b.than.removeAllViews()
        val c = caiDat

        if (c == null) {
            b.than.addView(tieu("Tablet chưa gửi cấu hình sang. Chờ máy đó lên mạng một lần."))
            veMucMay()
            return
        }

        b.than.addView(tieu("Giờ chơi"))
        b.than.addView(nhom(
            muc("Mỗi lần duyệt", Dinh.phut(c.phutMacDinh), "Số phút bấm một cái là xong") {
                hoiSo("Mỗi lần duyệt bao nhiêu phút", c.phutMacDinh, 5, 180) {
                    guiCaiDat("phutMacDinh", it)
                }
            },
            muc("Trần mỗi ngày", Dinh.phut(c.tranPhutMoiNgay), "Duyệt bài không vượt quá số này") {
                hoiSo("Trần phút mỗi ngày", c.tranPhutMoiNgay, 15, 480) {
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
            muc("App được chơi", "${c.appChoPhep.size} app", "Mở được trong giờ chơi") {
                chonApp("App được chơi", c.appChoPhep) { guiCaiDat("appChoPhep", it) }
            },
            muc("App chặn hẳn", "${c.appChan.size} app", "Không mở được kể cả trong giờ chơi") {
                chonApp("App chặn hẳn", c.appChan) { guiCaiDat("appChan", it) }
            },
            muc("App AI ghi câu hỏi", "${c.appAi.size} app", "Ghi lại câu Lê Hòa hỏi AI") {
                chonApp("App AI ghi câu hỏi", c.appAi) { guiCaiDat("appAi", it) }
            },
            muc("Khoá màn Cài đặt", if (c.khoaCaiDat) "Bật" else "Tắt",
                "Chặn Lê Hòa vào Cài đặt của máy") {
                guiCaiDat("khoaCaiDat", !c.khoaCaiDat)
            }
        ))

        veMucMay()
    }

    /** May muc thuoc ve chinh dien thoai nay, khong gui di dau ca. */
    private fun veMucMay() {
        b.than.addView(tieu("Máy này"))
        b.than.addView(nhom(
            muc("Token bot Telegram", if (Nha.token(requireContext()).isBlank()) "chưa đặt" else "đã đặt",
                "Chỉ dùng để tải ảnh bài tập về xem") { hoiToken() },
            muc("Mã nhà", Nha.maNha(requireContext()).take(8) + "…", null) {},
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
                xong(dsApp.filterIndexed { i, _ -> da[i] }.map { it.goi })
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
