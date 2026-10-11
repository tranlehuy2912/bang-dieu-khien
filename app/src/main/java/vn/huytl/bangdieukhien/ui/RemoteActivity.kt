package vn.huytl.bangdieukhien.ui

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.SystemClock
import android.text.InputType
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import vn.huytl.bangdieukhien.R
import vn.huytl.bangdieukhien.data.KetNoiRemote
import vn.huytl.bangdieukhien.data.RemoteLaptop
import vn.huytl.bangdieukhien.databinding.ActivityRemoteBinding

/**
 * Man Remote tivi (11/10/2026, anh Huy chon mau B): dieu khien laptop dang chieu len tivi. Lenh di
 * thang qua Wi-Fi nha toi dich vu netflix-remote tren laptop ([KetNoiRemote]), khong qua Firestore;
 * dien thoai khong bat Wi-Fi nha thi khong noi duoc (anh Huy chot khong dung Tailscale).
 *
 * Luc dung remote mat nhin tivi, nen: bam nut nao cung rung nhe, nut am luong o canh dien thoai
 * chinh tieng tivi khi da noi, va man khong tu tat trong luc mo.
 */
class RemoteActivity : AppCompatActivity() {

    private lateinit var b: ActivityRemoteBinding
    private lateinit var remote: RemoteLaptop
    private var ketNoi: KetNoiRemote? = null
    private var lanAmCuoi = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRemoteBinding.inflate(layoutInflater)
        setContentView(b.root)
        ViewCompat.setOnApplyWindowInsetsListener(b.root) { view, insets ->
            val thanh = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = thanh.top, bottom = thanh.bottom)
            insets
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        remote = RemoteLaptop(
            intent.getStringExtra(EXTRA_IP).orEmpty(),
            intent.getIntExtra(EXTRA_CONG, 0),
            intent.getStringExtra(EXTRA_KHOA).orEmpty()
        )

        b.nutVe.setOnClickListener { finish() }
        b.nhanNoi.setOnClickListener {
            if (ketNoi?.trangThai != KetNoiRemote.TrangThai.DA_NOI) noiLai()
        }
        b.banDiChuot.khiDi = { x, y -> ketNoi?.diChuot(x, y) }
        b.banDiChuot.khiBam = { neuDaNoi { it.bamChuot() } }
        b.banDiChuot.khiCuon = { n -> ketNoi?.cuon(n) }

        nut(b.nutPhat) { it.phim("space") }
        nut(b.nutLui) { it.phim("Left") }
        nut(b.nutTien) { it.phim("Right") }
        nut(b.nutBoQua) { it.phim("s") }
        nut(b.nutNho) { it.amLuong(false) }
        nut(b.nutTo) { it.amLuong(true) }
        b.nutBanPhim.setOnClickListener { neuDaNoi { hopGoChu() } }
        b.nutManChon.setOnClickListener { neuDaNoi { hoiVeManChon() } }
        veTrangThai(KetNoiRemote.TrangThai.DANG_NOI)
    }

    override fun onStart() {
        super.onStart()
        noiLai()
    }

    override fun onStop() {
        super.onStop()
        ketNoi?.dong()
        ketNoi = null
    }

    private fun noiLai() {
        ketNoi?.dong()
        lateinit var k: KetNoiRemote
        k = KetNoiRemote(
            remote,
            khiDoi = { t -> if (ketNoi === k) veTrangThai(t) },
            khiAm = { pt, tat -> if (ketNoi === k) veAm(pt, tat) },
            khiBao = { chu -> if (ketNoi === k) Dinh.noi(this, chu) }
        )
        ketNoi = k
        k.noi()
    }

    /** Nut am luong o canh dien thoai chinh tieng tivi khi da noi; chua noi thi chinh dien thoai. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val tang = when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> true
            KeyEvent.KEYCODE_VOLUME_DOWN -> false
            else -> return super.onKeyDown(keyCode, event)
        }
        val k = ketNoi?.takeIf { it.daNoi } ?: return super.onKeyDown(keyCode, event)
        // Giu nut thi Android lap lai rat nhanh, moi lan laptop phai goi pactl: gioi han lai.
        val bayGio = SystemClock.uptimeMillis()
        if (bayGio - lanAmCuoi >= AM_CACH_MS) {
            lanAmCuoi = bayGio
            k.amLuong(tang)
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) &&
            ketNoi?.daNoi == true
        ) return true
        return super.onKeyUp(keyCode, event)
    }

    private fun nut(v: View, lam: (KetNoiRemote) -> Unit) {
        v.setOnClickListener {
            neuDaNoi {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                lam(it)
            }
        }
    }

    private fun neuDaNoi(lam: (KetNoiRemote) -> Unit) {
        val k = ketNoi
        if (k != null && k.daNoi) lam(k) else Dinh.noi(this, CHUA_NOI)
    }

    private fun veTrangThai(t: KetNoiRemote.TrangThai) {
        val (chu, mauChu, mauNen) = when (t) {
            KetNoiRemote.TrangThai.DANG_NOI -> Triple("Đang nối", R.color.wait_ink, R.color.wait_soft)
            KetNoiRemote.TrangThai.DA_NOI -> Triple("Wi-Fi nhà", R.color.ok, R.color.ok_soft)
            KetNoiRemote.TrangThai.KHONG_NOI_DUOC -> Triple("Chưa nối được", R.color.alert, R.color.alert_soft)
            KetNoiRemote.TrangThai.SAI_MA -> Triple("Sai mã", R.color.alert, R.color.alert_soft)
        }
        b.nhanNoi.text = chu
        b.nhanNoi.setTextColor(ContextCompat.getColor(this, mauChu))
        b.nhanNoi.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, mauNen))
        val mo = if (t == KetNoiRemote.TrangThai.DA_NOI) 1f else 0.4f
        for (v in listOf(
            b.khungDiChuot, b.nutBoQua, b.nutLui, b.nutPhat, b.nutTien, b.nutNho, b.nutTo,
            b.nutBanPhim, b.nutManChon
        )) v.alpha = mo
        when (t) {
            KetNoiRemote.TrangThai.KHONG_NOI_DUOC -> Dinh.noi(this, CHUA_NOI)
            KetNoiRemote.TrangThai.SAI_MA ->
                Dinh.noi(this, "Laptop không nhận mã. Đóng màn này rồi mở lại.")
            KetNoiRemote.TrangThai.DA_NOI -> {}
            KetNoiRemote.TrangThai.DANG_NOI -> veAm(null, false)
        }
    }

    private fun veAm(pt: Int?, tat: Boolean) {
        b.chuAm.text = if (pt == null) "--" else "$pt%"
        b.iconLoa.setImageResource(if (tat) R.drawable.ic_loa_tat else R.drawable.ic_loa)
    }

    /**
     * Hop go chu: go den dau gui den do, nhu ban phim cam vao laptop. Chu xoa thi gui bay nhieu lan
     * xoa lui, nen bo go tieng Viet doi "a" thanh "á" cung ra dung tren tivi. Enter gui phim Enter.
     */
    private fun hopGoChu() {
        val o = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_SEND
            isSingleLine = true
            hint = "Tìm phim, mã PIN..."
        }
        val khung = FrameLayout(this).apply {
            val le = (20 * resources.displayMetrics.density).toInt()
            setPadding(le, le / 2, le, 0)
            addView(o)
        }
        var daGui = ""
        o.doAfterTextChanged { e ->
            val k = ketNoi?.takeIf { it.daNoi } ?: return@doAfterTextChanged
            val moi = e?.toString().orEmpty()
            val chung = daGui.commonPrefixWith(moi).length
            repeat((daGui.length - chung).coerceAtMost(XOA_TOI_DA)) { k.phim("BackSpace") }
            k.go(moi.substring(chung))
            daGui = moi
        }
        val hop = MaterialAlertDialogBuilder(this)
            .setTitle("Gõ lên tivi")
            .setView(khung)
            .setPositiveButton("Enter") { _, _ -> ketNoi?.phim("Return") }
            .setNegativeButton("Xong", null)
            .create()
        o.setOnEditorActionListener { _, _, _ ->
            ketNoi?.phim("Return")
            hop.dismiss()
            true
        }
        hop.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        hop.show()
        o.requestFocus()
    }

    private fun hoiVeManChon() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Về màn chọn Netflix, YouTube?")
            .setMessage("Phim đang chiếu sẽ đóng.")
            .setPositiveButton("Về màn chọn") { _, _ -> ketNoi?.veManChon() }
            .setNegativeButton(R.string.huy, null)
            .show()
    }

    companion object {
        private const val EXTRA_IP = "ip"
        private const val EXTRA_CONG = "cong"
        private const val EXTRA_KHOA = "khoa"
        private const val AM_CACH_MS = 150L
        private const val XOA_TOI_DA = 50
        private const val CHUA_NOI =
            "Chưa nối được laptop. Điện thoại phải bắt Wi-Fi nhà và laptop phải đang bật."

        fun mo(context: Context, r: RemoteLaptop) {
            context.startActivity(
                Intent(context, RemoteActivity::class.java)
                    .putExtra(EXTRA_IP, r.ip)
                    .putExtra(EXTRA_CONG, r.cong)
                    .putExtra(EXTRA_KHOA, r.khoa)
            )
        }
    }
}
