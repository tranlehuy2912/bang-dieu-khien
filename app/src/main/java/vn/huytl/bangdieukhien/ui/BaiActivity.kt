package vn.huytl.bangdieukhien.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import vn.huytl.bangdieukhien.data.CauCham
import vn.huytl.bangdieukhien.data.Kho
import vn.huytl.bangdieukhien.data.Lenh
import vn.huytl.bangdieukhien.data.Nha
import vn.huytl.bangdieukhien.databinding.ActivityBaiBinding
import vn.huytl.bangdieukhien.telegram.TaiAnh

/**
 * Mot lan nop bai, xem cho ky roi quyet dinh.
 *
 * Thu tu tren man hinh la thu tu Ba Huy can: ban cham cua AI truoc, anh sau. Nhin
 * ban cham la biet co phai mo anh ra khong - phan lon cac lan thi khong.
 */
class BaiActivity : AppCompatActivity() {

    private lateinit var b: ActivityBaiBinding
    private var nghe: ListenerRegistration? = null
    private var bai: Bai? = null

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

        veBanCham(bai)
        veNutClaude(bai)
        veAnh(bai)
        veNut(bai)
    }

    // ----------------------------------------------------------- nho Claude

    /**
     * Nut gui bai nay sang app Claude de cham lai. Xem [NhoClaude].
     *
     * Hien ca voi bai da duyet, khong chi bai dang cho: may cham nham thuong chi lo
     * ra sau khi da cap gio, nhu bai 10:23 ngay 23/9/2026.
     */
    private fun veNutClaude(bai: Bai) {
        if (NhoClaude.anhCanGui(bai).isEmpty()) return

        val chuNut = "Nhờ Claude chấm lại"
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
        val ket = NhoClaude.docKetQua(chu)
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

        val theoMa = bai.cham?.cac.orEmpty().associateBy { it.ma.trim() }
        val thanhDung = ket.cac.filter { it.chac && it.dung }
            .mapNotNull { cl -> theoMa[cl.ma]?.takeIf { !(it.docRo && it.dung) } }
        val thanhSai = ket.cac.filter { it.chac && !it.dung }
            .mapNotNull { cl -> theoMa[cl.ma]?.takeIf { it.docRo && it.dung } }
        val khongChac = ket.cac.filter { !it.chac }.map { it.ma }
        val dung = ket.cac.count { it.chac && it.dung }

        val noi = buildString {
            append("Claude chấm đúng $dung/${ket.cac.size} câu.")
            if (thanhDung.isNotEmpty()) {
                append("\n\nMáy bảo sai, Claude bảo đúng: ")
                append(thanhDung.joinToString(", ") { it.ma }).append(". ")
                append("Tablet sẽ bỏ các câu này khỏi danh sách cần sửa của con và cộng giờ theo luật.")
            }
            if (thanhSai.isNotEmpty()) {
                append("\n\nMáy bảo đúng, Claude bảo sai: ")
                append(thanhSai.joinToString(", ") { it.ma }).append(". ")
                append("Giờ của các câu này đã cộng rồi, tablet không rút lại.")
            }
            if (khongChac.isNotEmpty()) {
                append("\n\nClaude đọc chưa chắc: ").append(khongChac.joinToString(", "))
                append(". Các câu này vẫn giữ theo máy.")
            }
            if (thanhDung.isEmpty() && thanhSai.isEmpty()) append("\n\nClaude chấm giống máy.")
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
    private fun ghiKetQua(bai: Bai, ket: NhoClaude.KetQuaDan, thanhDung: List<CauCham>) {
        Kho.ghiChamClaude(this, bai.id, ket.cac) { kq ->
            if (kq is Kho.KetQua.Hong) Dinh.noi(this, kq.viSao)
        }
        if (thanhDung.isEmpty()) {
            Dinh.noi(this, "Đã ghi kết quả của Claude.")
            return
        }
        Kho.guiLenh(
            this, Lenh.SUA_CHAM, baiId = bai.id,
            giaTri = thanhDung.map { mapOf("ma" to it.ma, "de" to it.de) }
        ) { kq ->
            Dinh.noi(
                this,
                if (kq is Kho.KetQua.Hong) kq.viSao else "Đã báo tablet. Tablet trả lời ở màn Bảng."
            )
        }
    }

    // ------------------------------------------------------------- ban cham

    private fun veBanCham(bai: Bai) {
        val cham = bai.cham
        if (cham == null) {
            b.than.addView(theChu(getString(R.string.bai_ai_chua_cham)))
            return
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

        trong.addView(chu(cham.mon.ifBlank { "Bài đã nộp" }, 18f, bold = true))
        if (cham.tomTat.isNotBlank()) {
            trong.addView(chu(cham.tomTat, 15f, mau = R.color.ink_soft).apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = 6.dp().toInt()
            })
        }
        // Da dan ket qua Claude: mot dong tong, con tung cau khac may thi ghi ngay
        // duoi cau do. Xem [danKetQua].
        bai.claude?.let { cl ->
            val dungCl = cl.cac.count { it.chac && it.dung }
            trong.addView(
                chu(
                    "Claude chấm lại lúc ${Dinh.lucNgan(cl.luc)}: đúng $dungCl/${cl.cac.size} câu",
                    14f, bold = true, mau = R.color.brand
                ).apply { (layoutParams as LinearLayout.LayoutParams).topMargin = 8.dp().toInt() }
            )
        }

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
            val dau = when {
                !c.docRo -> "?" to R.color.wait
                c.dung -> "✓" to R.color.ok
                else -> "✕" to R.color.alert
            }
            hang.addView(chu(dau.first, 17f, bold = true, mau = dau.second).apply {
                layoutParams = LinearLayout.LayoutParams(28.dp().toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            val cot = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            cot.addView(chu(c.ma.ifBlank { "câu" }, 15f, bold = true))
            if (c.de.isNotBlank()) cot.addView(chu(c.de, 14f, mau = R.color.ink_soft))
            if (c.ketQua.isNotBlank()) {
                cot.addView(chu("Lê Hòa viết: ${c.ketQua}", 14f, mau = R.color.ink_soft))
            }
            if (!c.docRo) cot.addView(chu("AI đọc không rõ câu này", 13f, mau = R.color.wait))
            if (c.nhanXet.isNotBlank()) cot.addView(chu(c.nhanXet, 13f, mau = R.color.ink_mo))
            val cl = bai.claude?.cua(c.ma)
            if (cl != null && cl.chac && cl.dung != (c.docRo && c.dung)) {
                cot.addView(
                    chu(
                        if (cl.dung) "Claude: đúng, máy chấm nhầm"
                        else "Claude: sai" + if (cl.goiY.isNotBlank()) ". ${cl.goiY}" else "",
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
        the.addView(trong)
        b.than.addView(the)
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
            // duoc cho bai da duyet la co ngay bam hai lan thanh hai phien.
            b.khungNut.visibility = View.GONE
            return
        }
        b.khungNut.visibility = View.VISIBLE

        val phut = bai.cham?.phutDeNghi?.takeIf { it > 0 } ?: 30
        b.nutDuyet.text = "${getString(R.string.bai_duyet)} ${Dinh.phut(phut)}"
        b.nutDuyet.setOnClickListener { duyet(bai, phut) }
        // Giu lau la doi so phut khac, khoi phai mo them mot man nua cho viec
        // chin lan muoi la bam thang.
        b.nutDuyet.setOnLongClickListener {
            hoiSoPhut(bai)
            true
        }
        b.nutTuChoi.setOnClickListener { hoiTuChoi(bai) }
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

        fun mo(ct: Context, id: String) =
            ct.startActivity(Intent(ct, BaiActivity::class.java).putExtra(EXTRA_ID, id))
    }
}
