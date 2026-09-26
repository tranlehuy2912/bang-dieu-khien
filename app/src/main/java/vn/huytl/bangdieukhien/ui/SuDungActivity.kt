package vn.huytl.bangdieukhien.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.firestore.ListenerRegistration
import vn.huytl.bangdieukhien.R
import vn.huytl.bangdieukhien.data.Kho
import vn.huytl.bangdieukhien.data.SoSuDung
import vn.huytl.bangdieukhien.databinding.ActivitySuDungBinding

/**
 * Le Hoa da dung app gi, tu may gio den may gio, bay ngay gan nhat.
 *
 * Chep trang "Dùng app gì, lúc nào" ben tablet. Truoc day muon xem phai cam tablet,
 * qua PIN, hoac go /thongke ben Telegram va chi nhan duoc ban cat bot. So nay tablet
 * day sang khi may nay go [vn.huytl.bangdieukhien.data.Lenh.PING].
 *
 * KHONG CO NUT NAO SUA DUOC GI: nut duy nhat la doi ngay dang xem. Muon dat han cho
 * mot app thi sang tab Cai dat.
 */
class SuDungActivity : AppCompatActivity() {

    private lateinit var b: ActivitySuDungBinding
    private var nghe: ListenerRegistration? = null

    private var so: SoSuDung? = null

    /** Firestore da tra loi lan nao chua. Chua thi "chua co so" la noi som qua. */
    private var daNhan = false

    /** Dang xem ngay nao: 0 la hom nay, 1 la hom qua. */
    private var lui = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySuDungBinding.inflate(layoutInflater)
        setContentView(b.root)
        chuaThanhHeThong()
        b.thanhTren.setNavigationOnClickListener { finish() }

        lui = savedInstanceState?.getInt(K_LUI) ?: 0
        // Xoay man hinh thi khong hoi lai: listener van nghe, va lan hoi truoc con moi.
        if (savedInstanceState == null) hoiTablet()

        nghe = Kho.ngheSuDung(this) { moi ->
            so = moi
            daNhan = true
            ve()
        }
        // Chua ghep may hay chua noi Firebase thi khong co listener nao ca, va se
        // khong bao gio co cau tra loi de doi.
        if (nghe == null) daNhan = true
        ve()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(K_LUI, lui)
    }

    override fun onDestroy() {
        nghe?.remove()
        super.onDestroy()
    }

    /**
     * Hoi tablet de so tren man nay la so moi.
     *
     * Tab Bang vua hoi xong thi thoi: Ba Huy mo app roi cham ngay vao the dung app,
     * hai lenh PING cach nhau vai giay la hai lan tablet ghi ca ban ma khong them gi.
     */
    private fun hoiTablet() {
        if (System.currentTimeMillis() - Kho.pingLuc < BangFragment.GIAN_HOI_MS) return
        Kho.guiPing(this)
    }

    /**
     * Le tren dat vao thanh tieu de, le duoi dat vao cuoi phan cuon, nhu man Bai.
     *
     * Thanh tieu de phai cao them dung bang le tren. Giu nguyen chieu cao ma chi them
     * le thi nut quay lai va tieu de bi ep vao phan con lai, thap hon ca ngon tay.
     */
    private fun chuaThanhHeThong() {
        val caoThanh = b.thanhTren.layoutParams.height
        val leDuoi = b.than.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(b.root) { _, insets ->
            val thanh = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            b.thanhTren.updatePadding(top = thanh.top)
            if (caoThanh > 0) b.thanhTren.updateLayoutParams { height = caoThanh + thanh.top }
            b.than.updatePadding(bottom = leDuoi + thanh.bottom)
            insets
        }
    }

    // ------------------------------------------------------------------- ve

    private fun ve() {
        val s = so
        veHangNgay(s?.giuNgay ?: SoSuDung.GIU_NGAY_MAC_DINH)

        if (s == null) {
            b.theTong.visibility = View.GONE
            b.theDanhSach.visibility = View.GONE
            b.chuGhiChu.visibility = View.GONE
            b.chuTrong.visibility = View.VISIBLE
            b.chuTrong.text = getString(if (daNhan) R.string.su_dung_chua_gui else R.string.su_dung_dang_lay)
            return
        }

        val dau = SoSuDung.dauNgay(lui)
        val cuoi = SoSuDung.dauNgay(lui - 1)
        val ten = Dinh.tenNgay(lui)
        val cac = s.theoApp(dau, cuoi)

        b.chuGhiChu.visibility = View.VISIBLE
        b.chuGhiChu.text = buildString {
            if (s.capNhatLuc > 0L) append("Tablet gửi lúc ${Dinh.lucNgan(s.capNhatLuc)}. ")
            append("Chỉ ghi app mở được từ màn hình chính. Màn hình chính, bàn phím và app ")
            append("Nộp bài không tính. Quá ${s.giuNgay} ngày thì tự xoá, trên tablet lẫn ")
            append("trên Firestore.")
        }

        if (cac.isEmpty()) {
            b.theTong.visibility = View.GONE
            b.theDanhSach.visibility = View.GONE
            b.chuTrong.visibility = View.VISIBLE
            b.chuTrong.text = viSaoTrong(s, dau, ten)
            return
        }
        b.chuTrong.visibility = View.GONE
        b.theTong.visibility = View.VISIBLE
        b.theDanhSach.visibility = View.VISIBLE

        b.chuTong.text = "${getString(R.string.child_name)} dùng máy ${Dinh.doDai(s.tongMs(dau, cuoi))}"
        val khung = s.tuDen(dau, cuoi)
        b.chuKhungGio.text = buildString {
            append(ten.replaceFirstChar { it.uppercase() })
            if (khung != null) {
                append(" · từ ${Dinh.gioPhut(khung.first)} đến ${Dinh.gioPhut(khung.second)}")
            }
            append(" · ${cac.size} app")
        }
        b.daiNgay.dat(
            dauNgay = dau,
            cac = s.cuaNgay(dau, cuoi).map { it.tu to it.den },
            mauVach = mau(R.color.brand),
            mauNen = mau(R.color.line),
            mauGio = mau(R.color.surface)
        )

        b.danhSach.removeAllViews()
        val daiNhat = cac.first().tongMs
        cac.forEachIndexed { i, app ->
            if (i > 0) b.danhSach.addView(duongKe())
            b.danhSach.addView(veDong(app, daiNhat))
        }
    }

    /**
     * Noi ro vi sao ngay nay trong.
     *
     * Mot trang trong tron nhin nhu "hom nay khong dung may", trong khi hai ly do
     * hay gap hon: tablet chua gui so tu dau ngay nay (Ba Huy chua mo app, hay tablet
     * khong tra loi), va dich vu canh app tren tablet dang tat.
     */
    private fun viSaoTrong(s: SoSuDung, dau: Long, ten: String): String = when {
        s.capNhatLuc > 0L && s.capNhatLuc < dau ->
            "Chưa có số liệu $ten: lần cuối tablet gửi sổ là lúc ${Dinh.lucNgan(s.capNhatLuc)}."
        // Dich vu tat luc gui chi giai thich duoc ngay hom do, khong giai thich duoc
        // vi sao thu hai tuan truoc khong co dong nao.
        lui == 0 && !s.dangGhi ->
            "Dịch vụ canh app trên tablet đang TẮT nên máy không ghi được gì, mà cũng không chặn gì."
        else -> "Không ghi được app nào $ten."
    }

    /** Hang o chon ngay, hom nay dung dau. */
    private fun veHangNgay(soNgay: Int) {
        b.hangNgay.removeAllViews()
        (0 until soNgay).forEach { n ->
            val nut = LayoutInflater.from(this)
                .inflate(R.layout.item_ngay, b.hangNgay, false) as MaterialButton
            nut.text = Dinh.tenNgay(n).replaceFirstChar { it.uppercase() }
            nut.setOnClickListener {
                if (lui == n) return@setOnClickListener
                lui = n
                ve()
            }
            toMau(nut, dangXem = n == lui)
            b.hangNgay.addView(nut)
        }
    }

    private fun toMau(nut: MaterialButton, dangXem: Boolean) {
        if (dangXem) {
            nut.setBackgroundColor(mau(R.color.brand))
            nut.setTextColor(Color.WHITE)
            nut.strokeColor = ColorStateList.valueOf(mau(R.color.brand))
        } else {
            nut.setBackgroundColor(Color.TRANSPARENT)
            nut.setTextColor(mau(R.color.ink_soft))
            nut.strokeColor = ColorStateList.valueOf(mau(R.color.line))
        }
    }

    private fun veDong(app: SoSuDung.MotApp, daiNhat: Long): View {
        val dong = LayoutInflater.from(this).inflate(R.layout.item_su_dung, b.danhSach, false)
        dong.findViewById<TextView>(R.id.appTen).text = app.ten
        dong.findViewById<TextView>(R.id.appTong).text = Dinh.doDai(app.tongMs)

        // Thanh dai theo ty le voi app dung lau nhat trong ngay, khong theo tong ca
        // ngay: theo tong thi ngay dung muoi app la ca muoi thanh deu ngan tit.
        val thanh = dong.findViewById<LinearProgressIndicator>(R.id.appThanh)
        thanh.progress = if (daiNhat <= 0L) 0 else (app.tongMs * thanh.max / daiNhat).toInt()

        dong.findViewById<TextView>(R.id.appKhoang).text = buildString {
            append(app.cacDoan.take(MAX_KHOANG).joinToString(" · ") {
                "${Dinh.gioPhut(it.tu)}–${Dinh.gioPhut(it.den)}"
            })
            if (app.cacDoan.size > MAX_KHOANG) append(" · +${app.cacDoan.size - MAX_KHOANG} lần nữa")
        }
        return dong
    }

    private fun duongKe(): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            resources.displayMetrics.density.toInt().coerceAtLeast(1)
        ).apply { marginStart = (16 * resources.displayMetrics.density).toInt() }
        setBackgroundColor(mau(R.color.line))
    }

    private fun mau(id: Int) = ContextCompat.getColor(this, id)

    companion object {
        private const val K_LUI = "lui"

        /** Bao nhieu khoang thi ghi het, hon nua thi gom lai mot cau. Nhu ben tablet. */
        private const val MAX_KHOANG = 20
    }
}
