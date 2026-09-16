package vn.huytl.bangdieukhien.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import vn.huytl.bangdieukhien.R
import vn.huytl.bangdieukhien.data.Kho
import vn.huytl.bangdieukhien.data.Nha
import vn.huytl.bangdieukhien.databinding.ActivityMainBinding

/**
 * Khung cua app: bon the o thanh duoi, moi the mot man.
 *
 * Chua ghep doi thi day thang sang [GhepDoiActivity] - khong co ma nha thi moi
 * man hinh deu rong, hien ra chi lam Ba Huy tuong app hong.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.thanhDuoi.setOnItemSelectedListener { muc ->
            moThe(
                when (muc.itemId) {
                    R.id.tab_bai -> BaiFragment()
                    R.id.tab_chat -> ChatFragment()
                    R.id.tab_caidat -> CaiDatFragment()
                    else -> BangFragment()
                }
            )
            true
        }

        if (savedInstanceState == null) moThe(BangFragment())
    }

    override fun onStart() {
        super.onStart()
        if (!Nha.daGhep(this)) {
            startActivity(Intent(this, GhepDoiActivity::class.java))
            return
        }
        // Dang nhap lai moi lan mo app. Firebase giu phien tren may nen thuong la
        // khong goi mang; nhung app bi he dieu hanh don dep thi lan sau phai co.
        Kho.dangNhap(this) {}
    }

    private fun moThe(f: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.khung, f).commit()
    }

    /** Cho [BangFragment] day sang the bai tap khi Ba Huy bam vao o "đang chờ". */
    fun sangTheBai() {
        b.thanhDuoi.selectedItemId = R.id.tab_bai
    }
}
