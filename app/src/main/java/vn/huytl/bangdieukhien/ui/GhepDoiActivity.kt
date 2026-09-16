package vn.huytl.bangdieukhien.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.ListenerRegistration
import vn.huytl.bangdieukhien.R
import vn.huytl.bangdieukhien.data.Kho
import vn.huytl.bangdieukhien.data.Nha
import vn.huytl.bangdieukhien.databinding.ActivityGhepDoiBinding

/**
 * Noi may nay voi tablet, mot lan duy nhat.
 *
 * Duong di: go ma nha + ma sau so -> dat mot loi xin vao
 * nha/{ma nha}/ghep/{uid} -> tablet doc, so ma, ket nap bang cach them uid nay
 * vao danh sach nguoi nha -> ghi lai "OK" vao chinh o do -> man nay thay va di
 * tiep.
 *
 * Vi sao khong phai chi mot ma: ma nha la ten mot cho tren Firestore, no khong
 * bi mat nhung cung khong doi duoc. Ma sau so thi het han sau muoi phut, nen ai
 * nhin trom duoc anh chup man hinh tablet tu tuan truoc cung khong dung duoc.
 */
class GhepDoiActivity : AppCompatActivity() {

    private lateinit var b: ActivityGhepDoiBinding
    private var nghe: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityGhepDoiBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.oMaNha.setText(Nha.maNha(this))
        b.oToken.setText(Nha.token(this))

        if (!Kho.san(this)) {
            hien(getString(R.string.chua_noi_firebase), hong = true)
            b.nutXin.isEnabled = false
            return
        }

        b.nutXin.setOnClickListener { xin() }
    }

    private fun xin() {
        val maNha = b.oMaNha.text?.toString()?.trim().orEmpty()
        val maGhep = b.oMaGhep.text?.toString()?.trim().orEmpty()
        if (maNha.isEmpty() || maGhep.length != 6) {
            hien("Gõ đủ mã nhà và 6 số mã ghép đang hiện trên tablet.", hong = true)
            return
        }

        Nha.datToken(this, b.oToken.text?.toString().orEmpty())
        b.nutXin.isEnabled = false
        hien(getString(R.string.ghep_dang_cho), hong = false)

        Kho.xinVaoNha(this, maNha, maGhep) { kq ->
            when (kq) {
                is Kho.KetQua.Hong -> {
                    b.nutXin.isEnabled = true
                    hien(kq.viSao, hong = true)
                }
                // Dat duoc loi xin roi thi ngoi cho tablet. Khong dat ma nha vao
                // Nha luc nay: chua duoc ket nap ma da coi la ghep xong thi lan sau
                // mo app se bo qua man nay va vao mot bang trong tron.
                Kho.KetQua.Xong -> choKetNap(maNha)
            }
        }
    }

    private fun choKetNap(maNha: String) {
        nghe?.remove()
        nghe = Kho.ngheKetNap(this, maNha) { trangThai ->
            when (trangThai) {
                "OK" -> {
                    Nha.datMaNha(this, maNha)
                    hien(getString(R.string.ghep_xong), hong = false)
                    startActivity(
                        Intent(this, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    )
                    finish()
                }
                "SAI" -> {
                    b.nutXin.isEnabled = true
                    hien(getString(R.string.ghep_sai_ma), hong = true)
                }
            }
        }
    }

    private fun hien(chu: String, hong: Boolean) {
        b.chuTinhHinh.text = chu
        b.chuTinhHinh.setTextColor(
            ContextCompat.getColor(this, if (hong) R.color.alert else R.color.ok)
        )
    }

    override fun onDestroy() {
        nghe?.remove()
        super.onDestroy()
    }
}
