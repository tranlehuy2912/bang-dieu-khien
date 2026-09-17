package vn.huytl.bangdieukhien.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import vn.huytl.bangdieukhien.data.Nha
import vn.huytl.bangdieukhien.databinding.ActivityXemAnhBinding
import vn.huytl.bangdieukhien.telegram.TaiAnh

/** Mot tam anh bai tap, to het man hinh, chum ngon phong to duoc. */
class XemAnhActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val b = ActivityXemAnhBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Anh thi cu tran het man hinh, day la man xem anh nen den. Chi rieng
        // nhan khau o goc tren phai tranh thanh trang thai, khong thi tu Android 15
        // no nam ngay duoi dong ho.
        ViewCompat.setOnApplyWindowInsetsListener(b.root) { _, insets ->
            val thanh = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            b.nhanKhau.updateLayoutParams<android.view.ViewGroup.MarginLayoutParams> {
                topMargin = thanh.top + le16
            }
            insets
        }

        b.nhanKhau.text = intent.getStringExtra(EXTRA_KHAU).orEmpty()
        val fileId = intent.getStringExtra(EXTRA_FILE_ID).orEmpty()

        lifecycleScope.launch {
            val f = TaiAnh.lay(this@XemAnhActivity, Nha.token(this@XemAnhActivity), fileId)
            b.quay.visibility = View.GONE
            // Doc o do phan giai cao hon man hinh mot chut: phong to len moi con net,
            // ma doc nguyen ban anh 4000px thi may yeu de het bo nho.
            val bm = f?.let { TaiAnh.doc(it, resources.displayMetrics.widthPixels * 2) }
            if (bm == null) {
                Dinh.noi(this@XemAnhActivity, "Không tải được ảnh này.")
                finish()
                return@launch
            }
            b.anh.setImageBitmap(bm)
            b.anh.datLai()
        }

        b.anh.setOnLongClickListener {
            finish()
            true
        }
    }

    /** Le 16dp von ghi trong layout, giu lai khi cong them phan thanh he thong. */
    private val le16 get() = (16 * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_FILE_ID = "file_id"
        const val EXTRA_KHAU = "khau"
    }
}
