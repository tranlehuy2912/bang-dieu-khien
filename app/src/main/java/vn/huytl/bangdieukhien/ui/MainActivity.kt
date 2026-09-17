package vn.huytl.bangdieukhien.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
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
        chuaThanhHeThong()

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

    /**
     * Chua cho hai thanh cua he dieu hanh.
     *
     * Tu Android 15 app ve tran ca man hinh va cac thuoc tinh statusBarColor,
     * navigationBarColor trong theme khong con tac dung. Khong chua cho thi ten
     * "Le Hoa" o dau man nam duoi dong ho va pin, con thanh bon the o day thi bi
     * thanh dieu huong de len.
     *
     * Chua o hai cho khac nhau chu khong chua ca man: le duoi dat vao chinh thanh
     * the, nen nen trang cua no phu kin xuong tan day may thay vi ho ra mot dai
     * mau nen o duoi cung.
     */
    private fun chuaThanhHeThong() {
        ViewCompat.setOnApplyWindowInsetsListener(b.root) { _, insets ->
            val thanh = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            b.khung.updatePadding(top = thanh.top)
            b.thanhDuoi.updatePadding(bottom = thanh.bottom)
            insets
        }
    }

    private fun moThe(f: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.khung, f).commit()
    }

    /** Cho [BangFragment] day sang the bai tap khi Ba Huy bam vao o "đang chờ". */
    fun sangTheBai() {
        b.thanhDuoi.selectedItemId = R.id.tab_bai
    }
}
