package vn.huytl.bangdieukhien.ui

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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import vn.huytl.bangdieukhien.R
import vn.huytl.bangdieukhien.data.Bai
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

    private fun ve(bai: Bai) {
        b.thanhTren.title = Dinh.lucNgan(bai.luc)
        b.than.removeAllViews()

        veBanCham(bai)
        veAnh(bai)
        veNut(bai)
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
