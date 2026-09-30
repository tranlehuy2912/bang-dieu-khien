package vn.huytl.bangdieukhien.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import vn.huytl.bangdieukhien.R
import vn.huytl.bangdieukhien.data.Bai
import vn.huytl.bangdieukhien.data.CauCanXem
import vn.huytl.bangdieukhien.data.CauCham
import vn.huytl.bangdieukhien.data.Kho
import vn.huytl.bangdieukhien.data.Lenh
import vn.huytl.bangdieukhien.data.Nha
import vn.huytl.bangdieukhien.data.SuaChamGoi
import vn.huytl.bangdieukhien.data.BoSuaGoi
import vn.huytl.bangdieukhien.data.XuCau
import vn.huytl.bangdieukhien.databinding.ActivityBaiBinding
import vn.huytl.bangdieukhien.telegram.TaiAnh

/**
 * Mot lan nop bai, xem cho ky roi quyet dinh.
 *
 * Thu tu tren man hinh la thu tu Ba Huy can: ban cham cua AI truoc, anh sau. Nhin
 * ban cham la biet co phai mo anh ra khong - phan lon cac lan thi khong.
 *
 * Tu 29/9/2026 tren ca ban cham con mot the "Câu cần Ba Huy xem" khi tablet de bai cho vi
 * mot vai cau Claude chua cham chac, xem [veXuCau]. Lan do thi phai mo anh.
 */
class BaiActivity : AppCompatActivity() {

    private lateinit var b: ActivityBaiBinding
    private var nghe: ListenerRegistration? = null
    private var bai: Bai? = null

    /** Lua chon o the "Câu cần Ba Huy xem", cung thu tu voi cham.canXem. Xem [veXuCau]. */
    private var chonXu: MutableList<XuCau.Chon?> = mutableListOf()

    /** Bai va danh sach canXem ma [chonXu] dang ung voi. Doi la chon lai tu dau. */
    private var kyXu = ""

    /** Lenh XU_CAU dang tren duong di: nut gui tat, de khong gui hai lan mot luc. */
    private var dangGuiXu = false

    /** Phan than cua the "Câu cần Ba Huy xem" dang hien. Bam mot nut chi ve lai phan nay. */
    private var trongXu: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityBaiBinding.inflate(layoutInflater)
        setContentView(b.root)
        chuaThanhHeThong()
        b.thanhTren.setNavigationOnClickListener { finish() }

        val id = intent.getStringExtra(EXTRA_ID).orEmpty()
        if (id.isEmpty()) {
            finish()
            return
        }
        // Truoc 30/9/2026 man nay con nghe ca hop/dando de ghep vo da doc vao bai cho Claude
        // tinh tron goi. Bo tron goi thi cham bai khong dung vo nua.
        nghe = Kho.ngheMotBai(this, id) { moi ->
            bai = moi
            if (moi == null) finish() else ve(moi)
        }
    }

    override fun onDestroy() {
        nghe?.remove()
        super.onDestroy()
    }

    /**
     * Chua cho hai thanh cua he dieu hanh: le tren dat vao thanh tieu de, le duoi
     * dat vao day nut duyet. Ca hai deu nen trang, nen chua o do thi mau trang phu
     * kin den mep may.
     */
    private fun chuaThanhHeThong() {
        ViewCompat.setOnApplyWindowInsetsListener(b.root) { _, insets ->
            val thanh = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            b.thanhTren.updatePadding(top = thanh.top)
            b.khungNut.updatePadding(bottom = thanh.bottom)
            insets
        }
    }

    private fun ve(bai: Bai) {
        b.thanhTren.title = Dinh.lucNgan(bai.luc)
        b.than.removeAllViews()

        veViSaoHetCho(bai)
        veNopLai(bai)
        veXuCau(bai)
        veBanCham(bai)
        veNutClaude(bai)
        veAnh(bai)
        veNut(bai)
    }

    /**
     * Mot dong noi vi sao bai khong con nut duyet, o nhung canh Ba Huy khong tu bam gi.
     *
     * Thieu dong nay thi bai qua ngay trong nhu hong: hom qua con hai nut, hom nay mat
     * ca hai ma khong ai noi gi. Bai cham xong trong gio ngu cung vay: nhan ghi da duyet,
     * ma tablet chua cho con choi phut nao.
     */
    private fun veViSaoHetCho(bai: Bai) {
        val noi = when {
            bai.choCong -> "Tablet chấm xong lúc đang giờ ngủ. Hết giờ ngủ lúc " +
                "${Dinh.gioPhut(bai.congLuc)} tablet mới cộng ${Dinh.phut(bai.soPhut)} cho " +
                "${Nha.tenCon(this)}."
            bai.quaNgay() -> "Bài nộp hôm trước. Sang ngày mới tablet tự bỏ bài chưa duyệt " +
                "khỏi hàng chờ, nên bài này không duyệt được nữa. Muốn cho giờ thì bấm " +
                "Cho chơi ngay ở tab Giờ chơi."
            bai.trangThai == Bai.HUY -> "${Nha.tenCon(this)} đã huỷ lần nộp này để chụp lại."
            else -> return
        }
        b.than.addView(theChu(noi))
    }

    /**
     * Mot dong noi day la lan nop lai cac cau sai cua mot bai truoc (tu 28/9/2026 con nop
     * lai bang nut tren the cua bai do o man ket qua tablet). Loi nho Claude luc nay chi
     * cho cham cau trong danh sach. Xem [vn.huytl.bangdieukhien.data.KhaiBai.suaBai].
     */
    private fun veNopLai(bai: Bai) {
        val khai = bai.khai?.takeIf { it.suaBai.isNotBlank() } ?: return
        b.than.addView(theChu("Lần nộp lại các câu sai: ${khai.bai.ifBlank { "sửa bài cũ" }}."))
    }

    // ------------------------------------------------------ cau can Ba Huy xem

    /**
     * The "Câu cần Ba Huy xem": cac cau tablet khong tu cap gio duoc, cho Ba Huy tu cham (tu
     * 29/9/2026). Claude doc chua chac, hay cham dung ma khong ghi so dong: tablet de ca bai
     * cho, va truoc ngay do Ba Huy chi con cach bam Duyet voi so phut doan. Xem [CauCanXem].
     *
     * Moi cau ba nut Dung, Sai, Chup lai. Chon du thi bam gui, may nay gui lenh XU_CAU voi
     * nguyen ban Claude da gui, chi doi cac cau vua cham ([XuCau.giaTri]), va tablet cham lai
     * ca bai theo dung duong CHAM_BAI.
     *
     * Dat tren cung, truoc ban cham: day la viec dang giu gio cua ca bai. Chi hien khi bai con
     * cho duyet, vi tablet chi cham lai bai con trong hang cho.
     *
     * Lua chon song trong man nay qua cac lan Firestore goi lai. Danh sach canXem doi (tablet
     * vua cham lai) thi chon lai tu dau. Da gui roi thi lua chon doc lai tu chamClaude.goi, nen
     * mo lai man van thay dung cai da gui, kem luc gui.
     */
    private fun veXuCau(bai: Bai) {
        trongXu = null
        val canXem = bai.cham?.canXem.orEmpty()
        if (!bai.dangCho || canXem.isEmpty()) {
            kyXu = ""
            return
        }
        val ky = bai.id + "|" + canXem.joinToString("|") { "${it.maClaude}:${it.lyDo}" }
        if (ky != kyXu) {
            kyXu = ky
            val cl = bai.claude
            chonXu = (
                if (cl != null && cl.xuLuc > 0L) XuCau.chonTuGoi(cl.goi, canXem)
                else canXem.map { null }
                ).toMutableList()
        }
        val the = MaterialCardView(this).apply {
            radius = 20f.dp()
            strokeWidth = 1.dp().toInt()
            strokeColor = ContextCompat.getColor(context, R.color.wait)
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface))
            cardElevation = 0f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12.dp().toInt() }
        }
        val trong = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp().toInt(), 16.dp().toInt(), 16.dp().toInt(), 16.dp().toInt())
        }
        the.addView(trong)
        b.than.addView(the)
        trongXu = trong
        veTrongXu(bai)
    }

    /** Ve lai phan than cua the, sau moi lan bam va moi lan Firestore goi lai. */
    /** Mon cua bai de hien de dung cach (so mu, chi so hoa hoc), xem [SoMu.hienDe]. */
    private fun monCua(bai: Bai): String = bai.cham?.mon?.takeIf { it.isNotBlank() } ?: bai.khai?.mon.orEmpty()

    private fun veTrongXu(bai: Bai) {
        val trong = trongXu ?: return
        trong.removeAllViews()
        val canXem = bai.cham?.canXem.orEmpty()
        val cl = bai.claude
        val goi = cl?.goi
        val viTri = XuCau.ghep(goi, canXem)
        val khongThay = canXem.filterIndexed { k, _ -> viTri[k] == null }
        // Thieu goi (bai cham bang ban app cu) hay khong ghep duoc cau nao thi khong gui duoc
        // gi: van ke cac cau ra de Ba Huy biet, va chi cach dan lai ket qua Claude.
        val guiDuoc = goi != null && khongThay.isEmpty()
        val daGui = guiDuoc && XuCau.daGui(cl, canXem) && chonXu == XuCau.chonTuGoi(goi, canXem)
        val con = Nha.tenCon(this)

        trong.addView(chu("Câu cần Ba Huy xem", 18f, bold = true))
        trong.addView(
            chu(
                "Claude chưa chắc hoặc chưa ghi số dòng ở ${canXem.size} câu dưới đây, nên " +
                    "tablet chưa cộng giờ cho bài này. Xem ảnh bài rồi chấm từng câu.",
                14f, mau = R.color.ink_soft
            ).apply { (layoutParams as LinearLayout.LayoutParams).topMargin = 4.dp().toInt() }
        )
        if (!guiDuoc) {
            trong.addView(
                chu(
                    if (goi == null) XuCau.THIEU_GOI
                    else "Không thấy câu ${khongThay.joinToString(", ") { it.ma }} trong bản " +
                        "Claude đã lưu. Bấm Dán kết quả của Claude để dán lại.",
                    14f, mau = R.color.wait_ink
                ).apply { (layoutParams as LinearLayout.LayoutParams).topMargin = 8.dp().toInt() }
            )
        }

        // De va chu con viet lay tu ban cham tablet ghi, theo ma sach. Cau khong co trong do
        // (hiem) thi lay tu ban Claude may nay da gui.
        val theoMa = bai.cham?.cac.orEmpty().associateBy { it.ma.trim() }
        val cacGoi = goi?.get("cac") as? List<*>
        canXem.forEachIndexed { k, x ->
            val c = theoMa[x.ma.trim()]
            val muc = viTri[k]?.let { cacGoi?.getOrNull(it) as? Map<*, *> }
            val cot = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 16.dp().toInt(), 0, 0)
            }
            cot.addView(chu(x.ma, 15f, bold = true))
            val de = c?.de?.takeIf { it.isNotBlank() } ?: (muc?.get("de") as? String).orEmpty()
            if (de.isNotBlank()) cot.addView(chu(SoMu.hienDe(ngan(de), monCua(bai)), 14f, mau = R.color.ink_soft))
            val viet = c?.ketQua?.takeIf { it.isNotBlank() } ?: (muc?.get("conViet") as? String).orEmpty()
            if (viet.isNotBlank()) cot.addView(chu("$con viết: ${SoMu.hienDe(viet, monCua(bai))}", 14f, mau = R.color.ink_soft))
            cot.addView(chu(lyDoXu(x, c, con), 13f, mau = R.color.wait_ink).apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = 2.dp().toInt()
            })
            if (guiDuoc) cot.addView(hangNutXu(bai, k, x))
            trong.addView(cot)
        }
        if (!guiDuoc) return

        val conThieu = chonXu.count { it == null }
        trong.addView(
            chu(
                when {
                    daGui -> "Đã gửi cho tablet lúc ${Dinh.lucNgan(cl!!.xuLuc)}. Tablet chấm lại " +
                        "xong thì thẻ này tự ẩn. Lâu không thấy ẩn thì bấm gửi lại."
                    conThieu > 0 -> "Còn $conThieu câu chưa chọn."
                    else -> "Tablet chấm lại cả bài theo lựa chọn này, rồi báo số phút trên Telegram."
                },
                13f, mau = R.color.ink_soft
            ).apply { (layoutParams as LinearLayout.LayoutParams).topMargin = 16.dp().toInt() }
        )
        trong.addView(
            MaterialButton(this).apply {
                text = when {
                    dangGuiXu -> "Đang gửi…"
                    daGui -> "Gửi lại cho tablet"
                    else -> "Gửi cho tablet"
                }
                isEnabled = !dangGuiXu && conThieu == 0
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 8.dp().toInt() }
                setOnClickListener { guiXuCau(bai) }
            }
        )
    }

    /**
     * Mot dong noi Claude ket luan gi va vi sao tablet chua tu cong gio cau nay.
     *
     * Ket luan lay tu ban cham tablet ghi (dung la ket luan cua Claude, vi tablet cham theo
     * Claude), khong lay tu goi: goi da mang lua chon cua Ba Huy sau lan gui dau.
     */
    private fun lyDoXu(x: CauCanXem, c: CauCham?, con: String): String {
        val ketLuan = c?.let { if (it.dung) "đúng" else "sai" }
        return when (x.lyDo) {
            CauCanXem.CHUA_CHAC ->
                (ketLuan?.let { "Claude chấm $it nhưng" } ?: "Claude") + " đọc chưa chắc chữ $con viết."
            CauCanXem.THIEU_DONG -> "Claude chấm đúng nhưng chưa ghi số dòng."
            else -> (ketLuan?.let { "Claude chấm $it. " } ?: "") + "Tablet chưa tự cộng giờ câu này."
        }
    }

    /** Ba nut Dung, Sai, Chup lai cua mot cau. Nut dang chon to mau, hai nut kia de trang. */
    private fun hangNutXu(bai: Bai, k: Int, x: CauCanXem): View {
        val hang = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 8.dp().toInt() }
        }
        val chon = chonXu.getOrNull(k)
        // Chu "Đúng, 12 dòng" dai hon hai nut kia nen nut Dung rong hon.
        val dung = (chon as? XuCau.Chon.Dung)?.takeIf { x.canSoDong && it.soDong > 0 }
        hang.addView(
            nutXu(dung?.let { "Đúng, ${it.soDong} dòng" } ?: "Đúng", chon is XuCau.Chon.Dung,
                R.color.ok, R.color.ok, R.color.ok_soft, 1.4f, cuoi = false) { bamDung(bai, k, x) }
        )
        hang.addView(
            nutXu("Sai", chon == XuCau.Chon.Sai, R.color.alert, R.color.alert, R.color.alert_soft,
                1f, cuoi = false) { chonCau(bai, k, XuCau.Chon.Sai) }
        )
        hang.addView(
            nutXu("Chụp lại", chon == XuCau.Chon.ChupLai, R.color.wait_ink, R.color.wait,
                R.color.wait_soft, 1f, cuoi = true) { chonCau(bai, k, XuCau.Chon.ChupLai) }
        )
        return hang
    }

    /**
     * Mot nut trong [hangNutXu]. Mau dat tay chu khong dua vao trang thai "checked" cua
     * Material: nut vien cua Material3 khong doi mau ro rang khi chon, ma o day Ba Huy phai
     * nhin mot cai la biet cau nao chon gi.
     */
    private fun nutXu(
        chuNut: String,
        dangChon: Boolean,
        mauChu: Int,
        mauVien: Int,
        mauNen: Int,
        trongSo: Float,
        cuoi: Boolean,
        bam: () -> Unit
    ): View = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
        text = chuNut
        textSize = 14f
        maxLines = 1
        setPaddingRelative(4.dp().toInt(), paddingTop, 4.dp().toInt(), paddingBottom)
        setTextColor(ContextCompat.getColor(context, if (dangChon) mauChu else R.color.ink))
        strokeColor = ColorStateList.valueOf(
            ContextCompat.getColor(context, if (dangChon) mauVien else R.color.line)
        )
        backgroundTintList = ColorStateList.valueOf(
            if (dangChon) ContextCompat.getColor(context, mauNen) else Color.TRANSPARENT
        )
        isEnabled = !dangGuiXu
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, trongSo)
            .apply { if (!cuoi) marginEnd = 8.dp().toInt() }
        setOnClickListener { bam() }
    }

    /**
     * Bam Dung. Cau tinh theo dong thi hoi so dong truoc: thieu so dong la tablet tra 0 phut
     * va lai de ca bai cho. Mac dinh la so da chon, khong thi so Claude ghi.
     *
     * Hoi tu 1 toi [XuCau.SO_DONG_TOI_DA], rong them neu Claude ghi nhieu hon: cat so Claude
     * dem duoc xuong la bot phut cua con ma khong ai hay.
     */
    private fun bamDung(bai: Bai, k: Int, x: CauCanXem) {
        if (!x.canSoDong) return chonCau(bai, k, XuCau.Chon.Dung())
        val macDinh = (chonXu.getOrNull(k) as? XuCau.Chon.Dung)?.soDong?.takeIf { it > 0 }
            ?: x.soDong.takeIf { it > 0 }
        val so = (1..maxOf(XuCau.SO_DONG_TOI_DA, macDinh ?: 0)).toList()
        MaterialAlertDialogBuilder(this)
            .setTitle("Câu ${x.ma}: ${Nha.tenCon(this)} viết mấy dòng?")
            .setSingleChoiceItems(so.map { "$it dòng" }.toTypedArray(), macDinh?.let { it - 1 } ?: -1) { hop, i ->
                hop.dismiss()
                chonCau(bai, k, XuCau.Chon.Dung(so[i]))
            }
            .setNegativeButton(R.string.huy, null)
            .show()
    }

    private fun chonCau(bai: Bai, k: Int, c: XuCau.Chon) {
        if (k !in chonXu.indices) return
        chonXu[k] = c
        veTrongXu(bai)
    }

    /**
     * Gui lenh XU_CAU, va ghi lai ban Claude cung luc ([Kho.ghiXuCau]).
     *
     * Hai viec chay song song, y nhu [ghiChamMoi]: mat mang thi lenh van nam san trong hang
     * doi cua tablet. Gui lai (the dang bao da gui) la gui dung ban cu: tablet chi cham lai
     * bai con cho duyet, nen lenh thua toi sau khi bai da duyet thi tablet bo qua.
     */
    private fun guiXuCau(bai: Bai) {
        if (dangGuiXu) return
        val ket = XuCau.giaTri(bai.claude?.goi, bai.cham?.canXem.orEmpty(), chonXu)
        if (ket is XuCau.Ket.Hong) {
            Dinh.noi(this, ket.viSao)
            return
        }
        val giaTri = (ket as XuCau.Ket.Duoc).giaTri
        dangGuiXu = true
        veTrongXu(bai)
        Kho.ghiXuCau(this, bai.id, giaTri) { kq ->
            if (kq is Kho.KetQua.Hong) Dinh.noi(this, kq.viSao)
        }
        Kho.guiLenh(this, Lenh.XU_CAU, baiId = bai.id, giaTri = giaTri) { kq ->
            dangGuiXu = false
            Dinh.noi(
                this,
                if (kq is Kho.KetQua.Hong) kq.viSao
                else "Đã gửi cho tablet. Tablet chấm lại cả bài rồi báo số phút trên Telegram."
            )
            this.bai?.let { veTrongXu(it) }
        }
    }

    /** De dai thi cat bot, the nay chi can du de nhan ra cau nao. */
    private fun ngan(de: String): String {
        val gon = de.trim().replace(Regex("\\s+"), " ")
        return if (gon.length <= DE_NGAN) gon else gon.take(DE_NGAN).trimEnd() + "…"
    }

    // ----------------------------------------------------------- nho Claude

    /**
     * Nut gui bai nay sang app Claude de cham lai. Xem [NhoClaude].
     *
     * Hien ca voi bai da duyet, khong chi bai dang cho: may cham nham thuong chi lo
     * ra sau khi da cap gio, nhu bai 10:23 ngay 23/9/2026.
     *
     * Bai may chua cham thi nut la "Nhờ Claude chấm": Claude cham luon, xem
     * [NhoClaude.chamMoi].
     */
    private fun veNutClaude(bai: Bai) {
        if (NhoClaude.anhCanGui(bai).isEmpty()) return

        val chuNut = if (NhoClaude.chamMoi(bai)) "Nhờ Claude chấm" else "Nhờ Claude chấm lại"
        val nut = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = chuNut
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12.dp().toInt() }
        }
        nut.setOnClickListener {
            if (Nha.token(this).isBlank()) {
                Dinh.noi(this, "Chưa đặt token bot nên không lấy được ảnh. Vào tab Cài đặt để đặt.")
                return@setOnClickListener
            }
            nut.isEnabled = false
            nut.text = "Đang lấy ảnh bài…"
            lifecycleScope.launch {
                val anh = NhoClaude.layAnh(this@BaiActivity, bai)
                nut.isEnabled = true
                nut.text = chuNut
                if (anh.isEmpty()) {
                    Dinh.noi(this@BaiActivity, "Không lấy được ảnh bài. Kiểm tra mạng rồi thử lại.")
                    return@launch
                }
                NhoClaude.mo(this@BaiActivity, anh, NhoClaude.loiNho(bai, Nha.tenCon(this@BaiActivity)))
                Dinh.noi(this@BaiActivity, "Đã chép sẵn lời nhờ. Ô chat còn trống thì giữ vào ô đó rồi dán.")
            }
        }
        b.than.addView(nut)

        // Nut thu hai: dua ket qua Claude cham ve lai day. Xem [danKetQua].
        b.than.addView(
            MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = "Dán kết quả của Claude"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12.dp().toInt() }
                setOnClickListener { danKetQua(bai) }
            }
        )
    }

    /**
     * Doc ket qua Claude tu bo nho tam, hoi lai Ba Huy, roi ghi va bao tablet.
     *
     * Claude app khong tu ghi duoc len Firebase, nen ket qua di ve bang tay: Ba Huy
     * chep cau tra loi trong Claude, quay lai day bam nut. Loi nho da dan Claude in
     * mot khoi JSON o cuoi, xem [NhoClaude.docKetQua].
     *
     * LUON HOI LAI TRUOC KHI GHI. Hop thoai ke ra dung nhung cau Claude cham khac
     * may, vi do la cho duy nhat sinh ra viec: cau may bao sai ma Claude bao dung thi
     * tablet cong gio cho con. Cau may bao dung ma Claude bao sai thi chi ghi lai de
     * con biet, khong rut gio.
     */
    private fun danKetQua(bai: Bai) {
        val chu = getSystemService(ClipboardManager::class.java)?.primaryClip
            ?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()
        if (NhoClaude.laLoiNho(chu)) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Đây là lời nhờ, chưa phải kết quả")
                .setMessage(
                    "Bộ nhớ tạm đang giữ lời nhờ gửi Claude. Trong app Claude, bấm chép " +
                        "câu trả lời của Claude rồi quay lại đây bấm nút này."
                )
                .setPositiveButton("Đã hiểu", null)
                .show()
            return
        }
        // Doi ma va de ve dung cau con khai, roi cau may da cham, truoc moi phep so: ca duong
        // cham lai lan cham luon.
        val ket = NhoClaude.docKetQua(chu)?.let { NhoClaude.theoMay(NhoClaude.theoKhai(it, bai.khai), bai) }
        if (ket == null) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Chưa thấy kết quả của Claude")
                .setMessage(
                    "Trong app Claude, bấm chép câu trả lời có khối JSON ở cuối, " +
                        "rồi quay lại đây bấm nút này."
                )
                .setPositiveButton("Đã hiểu", null)
                .show()
            return
        }
        // Chep nham cau tra loi cua bai khac la cong gio nham cho cau cung ma o bai khac.
        if (ket.bai.isNotEmpty() && ket.bai != bai.id) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Kết quả của bài khác")
                .setMessage(
                    "Khối kết quả này ghi mã bài ${ket.bai}, còn bài đang mở là ${bai.id}. " +
                        "Mở đúng bài rồi dán lại."
                )
                .setPositiveButton("Đã hiểu", null)
                .show()
            return
        }
        if (NhoClaude.chamMoi(bai)) {
            hoiChamMoi(bai, ket)
            return
        }

        val theoMa = bai.cham?.cac.orEmpty().associateBy { it.ma.trim() }
        val thanhDung = SuaChamGoi.thanhDung(bai.cham?.cac.orEmpty(), ket.cac)
        val thanhSai = ket.cac.filter { it.chac && !it.dung }
            .mapNotNull { cl -> theoMa[cl.ma]?.takeIf { it.docRo && it.dung } }
        val khongChac = ket.cac.filter { !it.chac }.map { it.ma }
        val dung = ket.cac.count { it.chac && it.dung }

        val noi = buildString {
            append("Claude chấm đúng $dung/${ket.cac.size} câu.")
            if (thanhDung.isNotEmpty()) {
                append("\n\nLần chấm trước bảo sai, Claude lần này bảo đúng: ")
                append(thanhDung.joinToString(", ") { it.cu.ma }).append(". ")
                append("Tablet sẽ bỏ các câu này khỏi danh sách cần sửa của ")
                append(getString(R.string.child_name)).append(" và cộng giờ theo luật.")
            }
            if (thanhSai.isNotEmpty()) {
                append("\n\nLần chấm trước bảo đúng, Claude lần này bảo sai: ")
                append(thanhSai.joinToString(", ") { it.ma }).append(". ")
                append("Giờ của các câu này đã cộng rồi, tablet không rút lại.")
            }
            if (khongChac.isNotEmpty()) {
                append("\n\nClaude đọc chưa chắc: ").append(khongChac.joinToString(", "))
                append(". Các câu này vẫn giữ theo lần chấm trước.")
            }
            if (thanhDung.isEmpty() && thanhSai.isEmpty()) append("\n\nClaude chấm giống lần trước.")
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Kết quả của Claude")
            .setMessage(noi)
            .setNegativeButton(R.string.huy, null)
            .setPositiveButton(if (thanhDung.isEmpty()) "Ghi" else "Ghi và báo tablet") { _, _ ->
                ghiKetQua(bai, ket, thanhDung)
            }
            .show()
    }

    /**
     * Ghi ban Claude len bai va, neu co cau may cham nham, go lenh sua cham.
     *
     * Hai viec chay song song chu khong noi duoi nhau: ghi ban Claude chi xong khi may
     * chu nhan, ma mat mang thi lenh sua cham van nen nam san trong hang doi cua tablet.
     */
    private fun ghiKetQua(bai: Bai, ket: NhoClaude.KetQuaDan, thanhDung: List<SuaChamGoi.Cau>) {
        Kho.ghiChamClaude(this, bai.id, ket.cac) { kq ->
            if (kq is Kho.KetQua.Hong) Dinh.noi(this, kq.viSao)
        }
        if (thanhDung.isEmpty()) {
            Dinh.noi(this, "Đã ghi kết quả của Claude.")
            return
        }
        Kho.guiLenh(
            this, Lenh.SUA_CHAM, baiId = bai.id,
            giaTri = SuaChamGoi.giaTri(thanhDung)
        ) { kq ->
            Dinh.noi(
                this,
                if (kq is Kho.KetQua.Hong) kq.viSao else "Đã báo tablet. Tablet trả lời ở màn Bảng."
            )
        }
    }

    /**
     * Ket qua Claude la ban cham dau tien cua bai nay, vi may chua cham.
     *
     * Khac duong cham lai o cho tablet tinh phut theo CA ban nay, khong chi nhung cau
     * khac may. Nen hop thoai ke ra nhung cho lam tablet chua tu cong gio, de Ba Huy
     * biet truoc la se phai bam Duyet: cau Claude doc chua chac. Cau con khai ma Claude
     * bo sot thi tablet ghi la chua thay bai lam.
     *
     * Lan nop co trang vo dan do thi ke ra Claude doc vo ra gi, vi 45 phut tron goi
     * dua vao dung ba thu do. Ba Huy nhin mot dong la biet Claude doc dung hay nham.
     * Lan nop dung ban vo con soat thi ke lai danh sach do, kem cho Claude thay lech.
     */
    private fun hoiChamMoi(bai: Bai, ket: NhoClaude.KetQuaDan) {
        val dung = ket.cac.count { it.chac && it.dung }
        val khongChac = ket.cac.filter { !it.chac }.map { it.ma }
        val sot = bai.khai?.cac.orEmpty().map { it.ma }.filter { ma -> ket.cac.none { it.ma == ma } }

        val noi = buildString {
            append("Claude chấm đúng $dung/${ket.cac.size} câu.")
            if (sot.isNotEmpty()) {
                append("\n\nClaude không chấm câu ${getString(R.string.child_name)} đã khai: ")
                append(sot.joinToString(", "))
                append(". Tablet sẽ ghi là chưa thấy bài làm.")
            }
            if (khongChac.isNotEmpty()) {
                // Tu 29/9/2026 cac cau nay hien o the "Câu cần Ba Huy xem" dau man, cham tay
                // xong la tablet tinh phut. Nut Duyet van con cho ai muon duyet thang.
                append("\n\nClaude đọc chưa chắc: ").append(khongChac.joinToString(", "))
                append(". Tablet sẽ chưa cộng giờ: các câu này hiện ở thẻ Câu cần Ba Huy xem ")
                append("đầu màn này để chấm tay, hoặc bấm Duyệt.")
            }
            // Bai da cham mot lan ma chua duyet cung di duong nay, xem NhoClaude.chamMoi.
            if (!bai.cham?.cac.isNullOrEmpty() && bai.dangCho) {
                append("\n\nBài này đã chấm một lần nhưng chưa duyệt, nên tablet tính phút theo ")
                append("bản lần này thay cho bản trước.")
            }
            append("\n\n")
            append(
                if (bai.dangCho) "Tablet tính phút theo luật rồi báo trên Telegram."
                else "Bài này không còn chờ duyệt nên tablet không cộng giờ nữa. Kết quả chỉ " +
                    "được ghi lại để ${getString(R.string.child_name)} xem câu nào sai."
            )
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Kết quả của Claude")
            .setMessage(noi)
            .setNegativeButton(R.string.huy, null)
            .setPositiveButton(if (bai.dangCho) "Ghi và báo tablet" else "Ghi") { _, _ ->
                ghiChamMoi(bai, ket)
            }
            .show()
    }

    /**
     * Ghi ban Claude len bai va go lenh cham cho tablet.
     *
     * Bai da roi hang cho thi chi ghi: tablet se tu choi lenh cham, va cong gio cho mot
     * bai da duyet tay la cong hai lan.
     */
    private fun ghiChamMoi(bai: Bai, ket: NhoClaude.KetQuaDan) {
        // Ban gui kem lenh CHAM_BAI nam lai nguyen trong chamClaude.goi, de the "Câu cần Ba
        // Huy xem" gui lai dung no qua lenh XU_CAU (tu 29/9/2026). Bai da roi hang cho thi
        // khong gui lenh nao, nen cung khong co goi.
        val giaTri = if (bai.dangCho) NhoClaude.goiChamBai(ket, bai) else null
        Kho.ghiChamClaude(this, bai.id, ket.cac, chinh = true, goi = giaTri) { kq ->
            if (kq is Kho.KetQua.Hong) Dinh.noi(this, kq.viSao)
        }
        if (giaTri == null) {
            Dinh.noi(this, "Đã ghi kết quả của Claude.")
            return
        }
        Kho.guiLenh(this, Lenh.CHAM_BAI, baiId = bai.id, giaTri = giaTri) { kq ->
            Dinh.noi(
                this,
                if (kq is Kho.KetQua.Hong) kq.viSao
                else "Đã gửi cho tablet chấm. Số phút báo trên Telegram."
            )
        }
    }

    // ------------------------------------------------------------- ban cham

    private fun veBanCham(bai: Bai) {
        val cham = bai.cham
        if (cham == null || cham.cac.isEmpty()) {
            // Da dan ket qua Claude ma tablet chua cham xong, hay bai da roi hang cho.
            val cl = bai.claude
            if (cl != null) {
                veClaudeRieng(bai, cl)
                return
            }
            if (cham == null) {
                b.than.addView(
                    theChu(
                        if (NhoClaude.anhCanGui(bai).isEmpty()) "Bài chưa chấm."
                        else "Bài chưa chấm. Bấm \"Nhờ Claude chấm\" bên dưới để Claude chấm."
                    )
                )
                return
            }
        }

        val the = MaterialCardView(this).apply {
            radius = 20f.dp()
            strokeWidth = 1
            strokeColor = ContextCompat.getColor(context, R.color.line)
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface))
            cardElevation = 0f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12.dp().toInt() }
        }
        val trong = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp().toInt(), 16.dp().toInt(), 16.dp().toInt(), 16.dp().toInt())
        }

        trong.addView(dauThe(cham.mon.ifBlank { "Bài đã nộp" }, bai))
        if (cham.tomTat.isNotBlank()) {
            trong.addView(chu(cham.tomTat, 15f, mau = R.color.ink_soft).apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = 6.dp().toInt()
            })
        }
        // Da dan ket qua Claude: mot dong tong, con tung cau khac may thi ghi ngay
        // duoi cau do. Xem [danKetQua]. Ba Huy da cham tay o the "Câu cần Ba Huy xem" thi
        // noi ca luc do, vi tu luc ay chamClaude mang ca ket luan cua Ba Huy (XuCau.cacClaude).
        bai.claude?.let { cl ->
            val dungCl = cl.cac.count { it.chac && it.dung }
            trong.addView(
                chu(
                    (if (cl.chinh) "Claude chấm lúc " else "Claude chấm lại lúc ") +
                        Dinh.lucNgan(cl.luc) +
                        (if (cl.xuLuc > 0L) ", Ba Huy chấm tay thêm lúc ${Dinh.lucNgan(cl.xuLuc)}" else "") +
                        ": đúng $dungCl/${cl.cac.size} câu",
                    14f, bold = true, mau = R.color.brand
                ).apply { (layoutParams as LinearLayout.LayoutParams).topMargin = 8.dp().toInt() }
            )
        }

        // Cau dang nam o the "Câu cần Ba Huy xem": khong ghi dong so voi Claude ben duoi.
        // Ba Huy vua gui lua chon thi chamClaude da mang ket luan moi, con ban cham nay la
        // ban cu cho toi luc tablet cham lai, va dong "lần chấm trước nhầm" la noi sai.
        val dangXem = if (bai.dangCho) cham.canXem.map { it.ma.trim() }.toSet() else emptySet()
        // Cau Ba Huy bam Chup lai: tablet ghi vao ban cham la sai, xem [XuCau.maChupLai].
        val chupLai = XuCau.maChupLai(bai.claude?.goi)

        // Mot dong moi cau: dung hay sai, va co doc ro khong.
        //
        // "Khong doc ro" quan trong khong kem "sai": AI gap chu mo co xu huong dien
        // vao dap an dung ma no biet san, nen mot cau khong doc ro la mot cau phai
        // tu nhin anh chu khong tin may.
        cham.cac.forEach { c ->
            val hang = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 10.dp().toInt(), 0, 0)
            }
            val nhoChup = c.ma.trim() in chupLai
            val dau = when {
                nhoChup || !c.docRo -> "?" to R.color.wait
                c.dung -> "✓" to R.color.ok
                else -> "✕" to R.color.alert
            }
            hang.addView(chu(dau.first, 17f, bold = true, mau = dau.second).apply {
                layoutParams = LinearLayout.LayoutParams(28.dp().toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            val cot = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            cot.addView(chu(c.ma.ifBlank { "câu" }, 15f, bold = true))
            if (c.de.isNotBlank()) cot.addView(chu(SoMu.hienDe(c.de, monCua(bai)), 14f, mau = R.color.ink_soft))
            if (c.ketQua.isNotBlank()) {
                cot.addView(chu("Lê Hòa viết: ${SoMu.hienDe(c.ketQua, monCua(bai))}", 14f, mau = R.color.ink_soft))
            }
            if (nhoChup) cot.addView(chu("Ba Huy nhờ chụp lại câu này", 13f, mau = R.color.wait))
            else if (!c.docRo) cot.addView(chu("Đọc chưa chắc câu này", 13f, mau = R.color.wait))
            if (c.nhanXet.isNotBlank()) cot.addView(chu(SoMu.hienDe(c.nhanXet, monCua(bai)), 13f, mau = R.color.ink_mo))
            val cl = bai.claude?.cua(c.ma)
            if (cl != null && c.ma.trim() !in dangXem && cl.chac && cl.dung != (c.docRo && c.dung)) {
                cot.addView(
                    chu(
                        if (cl.dung) "Claude: đúng, lần chấm trước nhầm"
                        else "Claude: sai" + if (cl.goiY.isNotBlank()) ". ${SoMu.hienDe(cl.goiY, monCua(bai))}" else "",
                        13f, bold = true, mau = if (cl.dung) R.color.ok else R.color.alert
                    )
                )
            }
            hang.addView(cot)
            trong.addView(hang)
        }

        if (cham.phutDeNghi > 0) {
            trong.addView(
                chu("AI đề nghị ${Dinh.phut(cham.phutDeNghi)}", 14f, bold = true, mau = R.color.brand)
                    .apply { (layoutParams as? LinearLayout.LayoutParams)?.topMargin = 12.dp().toInt() }
            )
        }
        // Nut bo cau sai khoi danh sach can sua cua con (30/9/2026), xem [hoiBoSua].
        val sai = BoSuaGoi.cauSai(cham, chupLai)
        if (sai.isNotEmpty()) {
            trong.addView(
                MaterialButton(this, null, androidx.appcompat.R.attr.borderlessButtonStyle).apply {
                    text = "Không bắt sửa câu sai…"
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = 8.dp().toInt() }
                    setOnClickListener { hoiBoSua(bai, sai) }
                }
            )
        }

        the.addView(trong)
        b.than.addView(the)
    }

    /**
     * Ba Huy chon cau sai de tablet bo khoi dong "Có N câu cần sửa", khong cong phut. Xem
     * [Lenh.BO_SUA].
     *
     * Tich san het: thuong Ba Huy bam nut nay la de bo ca bai (trang vo con khong con giu).
     * Tablet chi bo cau con dang cho sua; cau con da sua dung roi thi no noi lai o o tra loi.
     */
    private fun hoiBoSua(bai: Bai, sai: List<CauCham>) {
        val chon = BooleanArray(sai.size) { true }
        MaterialAlertDialogBuilder(this)
            .setTitle("Bỏ khỏi danh sách cần sửa")
            .setMultiChoiceItems(sai.map { it.ma.trim().ifBlank { "câu" } }.toTypedArray(), chon) { _, i, co ->
                chon[i] = co
            }
            .setNegativeButton(R.string.huy, null)
            .setPositiveButton("Bỏ, không cộng phút") { _, _ ->
                val cac = sai.filterIndexed { i, _ -> chon[i] }
                if (cac.isEmpty()) return@setPositiveButton
                Kho.guiLenh(this, Lenh.BO_SUA, baiId = bai.id, giaTri = BoSuaGoi.giaTri(cac)) { kq ->
                    Dinh.noi(
                        this,
                        if (kq is Kho.KetQua.Hong) kq.viSao else "Đã báo tablet. Tablet trả lời ở màn Bảng."
                    )
                }
            }
            .show()
    }

    /**
     * Ban cham chi co cua Claude: may chua cham, va tablet chua cham theo Claude.
     *
     * Hien ngay sau khi dan, truoc khi tablet tra loi, va o lai neu bai da roi hang
     * cho. De cau lay tu phan con khai khi co, vi Claude chi chep de cau ngoai sach.
     */
    private fun veClaudeRieng(bai: Bai, cl: vn.huytl.bangdieukhien.data.KetQuaClaude) {
        val the = MaterialCardView(this).apply {
            radius = 20f.dp()
            strokeWidth = 1
            strokeColor = ContextCompat.getColor(context, R.color.line)
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface))
            cardElevation = 0f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12.dp().toInt() }
        }
        val trong = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp().toInt(), 16.dp().toInt(), 16.dp().toInt(), 16.dp().toInt())
        }
        trong.addView(dauThe(bai.khai?.mon?.takeIf { it.isNotBlank() } ?: "Bài đã nộp", bai))
        val dungCl = cl.cac.count { it.chac && it.dung }
        trong.addView(
            chu(
                "Claude chấm lúc ${Dinh.lucNgan(cl.luc)}: đúng $dungCl/${cl.cac.size} câu",
                14f, bold = true, mau = R.color.brand
            ).apply { (layoutParams as LinearLayout.LayoutParams).topMargin = 6.dp().toInt() }
        )
        if (bai.dangCho) {
            trong.addView(
                chu("Đang chờ tablet tính phút.", 14f, mau = R.color.ink_soft)
                    .apply { (layoutParams as LinearLayout.LayoutParams).topMargin = 4.dp().toInt() }
            )
        }
        val deTheoMa = bai.khai?.cac.orEmpty().associate { it.ma to it.de }
        cl.cac.forEach { c ->
            val hang = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 10.dp().toInt(), 0, 0)
            }
            val dau = when {
                !c.chac -> "?" to R.color.wait
                c.dung -> "✓" to R.color.ok
                else -> "✕" to R.color.alert
            }
            hang.addView(chu(dau.first, 17f, bold = true, mau = dau.second).apply {
                layoutParams = LinearLayout.LayoutParams(28.dp().toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            val cot = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            cot.addView(chu(c.ma, 15f, bold = true))
            val de = c.de.ifBlank { deTheoMa[c.ma].orEmpty() }
            if (de.isNotBlank()) cot.addView(chu(SoMu.hienDe(de, monCua(bai)), 14f, mau = R.color.ink_soft))
            if (c.conViet.isNotBlank()) {
                cot.addView(chu("Lê Hòa viết: ${SoMu.hienDe(c.conViet, monCua(bai))}", 14f, mau = R.color.ink_soft))
            }
            if (!c.chac) cot.addView(chu("Claude đọc chưa chắc câu này", 13f, mau = R.color.wait))
            if (!c.dung && c.goiY.isNotBlank()) cot.addView(chu(SoMu.hienDe(c.goiY, monCua(bai)), 13f, mau = R.color.ink_mo))
            hang.addView(cot)
            trong.addView(hang)
        }
        the.addView(trong)
        b.than.addView(the)
    }

    /**
     * Dong dau the ban cham: ten mon, va icon chep ca ban cham o goc phai. Chu chep ra xem
     * [ChuBanCham].
     *
     * Icon nam o dau the chu khong o day: mot icon dung rieng o day the thi ton ca mot hang,
     * ma chep la viec phu, hai nut duyet o day man hinh moi la viec chinh.
     */
    private fun dauThe(tieuDe: String, bai: Bai): View {
        // Hang cao du 48dp cho nut, le am dat vao HANG chu khong vao nut: dong ten mon van
        // nam dung cho cu, icon 22dp o giua nut thang mep phai voi chu trong the. Le am dat
        // vao nut thi nut tran ra ngoai hang, ma Android khong chuyen cu cham o phan tran
        // do: vung bam con chung 35 x 24dp.
        val hang = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = -(12.dp().toInt())
                bottomMargin = -(12.dp().toInt())
                marginEnd = -(13.dp().toInt())
            }
        }
        hang.addView(chu(tieuDe, 18f, bold = true).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val nut = layoutInflater.inflate(R.layout.nut_chep, hang, false)
        nut.layoutParams = LinearLayout.LayoutParams(48.dp().toInt(), 48.dp().toInt())
        nut.setOnClickListener {
            Dinh.chep(this, "Bản chấm", ChuBanCham.cua(bai, Nha.tenCon(this)), "Đã chép bản chấm.")
        }
        hang.addView(nut)
        return hang
    }

    // ------------------------------------------------------------------ anh

    private fun veAnh(bai: Bai) {
        val token = Nha.token(this)
        if (bai.anh.isEmpty()) return
        if (token.isBlank()) {
            b.than.addView(theChu("Chưa đặt token bot nên không tải được ảnh. Vào tab Cài đặt để đặt."))
            return
        }

        bai.anh.forEachIndexed { i, anh ->
            b.than.addView(chu(anh.tenKhau(), 13f, mau = R.color.ink_mo).apply {
                (layoutParams as? LinearLayout.LayoutParams)?.topMargin = 12.dp().toInt()
            })
            val o = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 6.dp().toInt() }
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                setBackgroundColor(ContextCompat.getColor(context, R.color.line))
                minimumHeight = 160.dp().toInt()
                setOnClickListener {
                    startActivity(
                        Intent(this@BaiActivity, XemAnhActivity::class.java)
                            .putExtra(XemAnhActivity.EXTRA_FILE_ID, anh.fileId)
                            .putExtra(XemAnhActivity.EXTRA_KHAU, anh.tenKhau())
                    )
                }
            }
            b.than.addView(o)
            lifecycleScope.launch {
                val f = TaiAnh.lay(this@BaiActivity, token, anh.fileId)
                if (f == null) {
                    o.visibility = View.GONE
                    return@launch
                }
                TaiAnh.doc(f, 1200)?.let { o.setImageBitmap(it) }
            }
        }
    }

    // ------------------------------------------------------------------ nut

    private fun veNut(bai: Bai) {
        if (!bai.dangCho) {
            // Bai da xu ly roi thi giau ca hai nut di. De lai mot nut "Duyệt" mo
            // duoc cho bai da duyet la co ngay bam hai lan thanh hai phien. Cho do
            // chi con nut Xoa, de danh sach o tab Bai gon lai. Bai da xoa (mo tu tam
            // Bai da xoa) thi la nut Khoi phuc.
            b.nutDuyet.visibility = View.GONE
            b.nutTuChoi.visibility = View.GONE
            b.nutXoa.visibility = if (bai.an) View.GONE else View.VISIBLE
            b.nutXoa.setOnClickListener { xoa(bai) }
            b.nutKhoiPhuc.visibility = if (bai.an) View.VISIBLE else View.GONE
            b.nutKhoiPhuc.setOnClickListener { khoiPhuc(bai) }
            b.khungNut.visibility = View.VISIBLE
            return
        }
        b.khungNut.visibility = View.VISIBLE
        b.nutDuyet.visibility = View.VISIBLE
        b.nutTuChoi.visibility = View.VISIBLE
        b.nutXoa.visibility = View.GONE
        b.nutKhoiPhuc.visibility = View.GONE

        // Khong con so phut mac dinh (27/9/2026): bai da cham thi nut Duyet mang dung
        // so phut may tinh, chua cham thi nut Duyet mo hop chon so phut.
        val phut = bai.cham?.phutDeNghi?.takeIf { it > 0 }
        if (phut != null) {
            b.nutDuyet.text = "${getString(R.string.bai_duyet)} ${Dinh.phut(phut)}"
            b.nutDuyet.setOnClickListener { duyet(bai, phut) }
        } else {
            b.nutDuyet.text = getString(R.string.bai_duyet)
            b.nutDuyet.setOnClickListener { hoiSoPhut(bai) }
        }
        // Khong con cach doi so phut tren nut co so (27/9/2026, Ba Huy bo nut "Chọn số phút
        // khác" va giu lau nut Duyet): so tren nut la so may hay Claude tinh theo luat.
        b.nutTuChoi.setOnClickListener { hoiTuChoi(bai) }
    }

    /** An bai khoi danh sach o tab Bai roi dong man nay. Xem [Kho.anBai]. */
    private fun xoa(bai: Bai) {
        val ct = applicationContext
        Kho.anBai(ct, listOf(bai.id), true) { kq ->
            if (kq is Kho.KetQua.Hong) Dinh.noi(ct, kq.viSao)
        }
        Dinh.noi(ct, "Đã xoá khỏi danh sách. Xem lại ở nút Bài đã xoá cuối danh sách.")
        finish()
    }

    /** Dua bai da xoa ve lai danh sach o tab Bai roi dong man nay, ve lai tam Bai da xoa. */
    private fun khoiPhuc(bai: Bai) {
        val ct = applicationContext
        Kho.anBai(ct, listOf(bai.id), false) { kq ->
            if (kq is Kho.KetQua.Hong) Dinh.noi(ct, kq.viSao)
        }
        Dinh.noi(ct, "Đã khôi phục vào danh sách.")
        finish()
    }

    private fun duyet(bai: Bai, phut: Int) {
        Kho.guiLenh(this, Lenh.DUYET, phut = phut, baiId = bai.id) { kq ->
            if (kq is Kho.KetQua.Hong) Dinh.noi(this, kq.viSao) else finish()
        }
    }

    private fun hoiSoPhut(bai: Bai) {
        val so = intArrayOf(15, 30, 45, 60, 90)
        MaterialAlertDialogBuilder(this)
            .setTitle("Duyệt bao nhiêu phút")
            .setItems(so.map { Dinh.phut(it) }.toTypedArray()) { _, i -> duyet(bai, so[i]) }
            .setNegativeButton(R.string.huy, null)
            .show()
    }

    private fun hoiTuChoi(bai: Bai) {
        val cac = arrayOf(
            "Làm ẩu, làm lại đi",
            "Thiếu bài, chưa làm hết",
            "Chụp mờ quá, chụp lại",
            "Không ghi lý do"
        )
        MaterialAlertDialogBuilder(this)
            .setTitle("Không duyệt vì")
            .setItems(cac) { _, i ->
                Kho.guiLenh(
                    this, Lenh.TU_CHOI, baiId = bai.id,
                    chu = if (i == cac.lastIndex) null else cac[i]
                ) { kq ->
                    if (kq is Kho.KetQua.Hong) Dinh.noi(this, kq.viSao) else finish()
                }
            }
            .setNegativeButton(R.string.huy, null)
            .show()
    }

    // -------------------------------------------------------------- ve vat

    private fun chu(
        noi: String,
        co: Float,
        bold: Boolean = false,
        mau: Int = R.color.ink
    ): TextView = TextView(this).apply {
        text = noi
        textSize = co
        setTextColor(ContextCompat.getColor(context, mau))
        if (bold) setTypeface(typeface, Typeface.BOLD)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    private fun theChu(noi: String): View = TextView(this).apply {
        text = noi
        textSize = 15f
        gravity = Gravity.CENTER
        setTextColor(ContextCompat.getColor(context, R.color.ink_mo))
        setPadding(16.dp().toInt(), 24.dp().toInt(), 16.dp().toInt(), 24.dp().toInt())
    }

    private fun Float.dp(): Float = this * resources.displayMetrics.density
    private fun Int.dp(): Float = this * resources.displayMetrics.density

    companion object {
        const val EXTRA_ID = "bai_id"

        /**
         * De dai nhat bao nhieu ky tu o the "Câu cần Ba Huy xem". Du de nhan ra cau nao, de
         * day du nam o the ban cham ngay duoi.
         */
        private const val DE_NGAN = 140

        fun mo(ct: Context, id: String) =
            ct.startActivity(Intent(ct, BaiActivity::class.java).putExtra(EXTRA_ID, id))
    }
}
