package vn.huytl.bangdieukhien.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import vn.huytl.bangdieukhien.R
import vn.huytl.bangdieukhien.data.Bai
import vn.huytl.bangdieukhien.data.Kho
import vn.huytl.bangdieukhien.data.Nha
import vn.huytl.bangdieukhien.databinding.FragmentBaiBinding
import vn.huytl.bangdieukhien.databinding.ItemBaiBinding
import vn.huytl.bangdieukhien.telegram.TaiAnh

/**
 * Danh sach cac lan con nop bai, moi nhat truoc.
 *
 * Chi hien mot dong tom tat va vai tam anh nho. Bam vao mot dong moi mo man chi
 * tiet - cho do moi tai anh to va goi ban cham cua AI ra.
 */
class BaiFragment : Fragment() {

    private var _b: FragmentBaiBinding? = null
    private val b get() = _b!!
    private var nghe: ListenerRegistration? = null
    private val bo = Bo()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentBaiBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        b.danhSach.layoutManager = LinearLayoutManager(requireContext())
        b.danhSach.adapter = bo
    }

    override fun onStart() {
        super.onStart()
        // Ba muoi lan nop gan nhat la du xa: hon the thi khong ai cuon toi, ma moi
        // document doc ve deu tinh mot luot trong han muc ngay cua Firestore.
        nghe = Kho.ngheBai(requireContext(), 30L) { ds ->
            if (_b == null) return@ngheBai
            bo.dat(ds)
            b.trong.visibility = if (ds.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onStop() {
        nghe?.remove()
        super.onStop()
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }

    private inner class Bo : RecyclerView.Adapter<O>() {
        private var cac: List<Bai> = emptyList()

        fun dat(moi: List<Bai>) {
            cac = moi
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(cha: ViewGroup, kieu: Int) =
            O(ItemBaiBinding.inflate(layoutInflater, cha, false))

        override fun getItemCount() = cac.size

        override fun onBindViewHolder(o: O, i: Int) = o.gan(cac[i])
    }

    private inner class O(private val v: ItemBaiBinding) : RecyclerView.ViewHolder(v.root) {

        fun gan(bai: Bai) {
            val ct = requireContext()
            v.gio.text = Dinh.lucNgan(bai.luc)

            val (chu, mau, nen) = when (bai.trangThai) {
                Bai.DUYET -> Triple(
                    getString(R.string.bai_da_duyet) + " " + Dinh.phut(bai.soPhut),
                    R.color.ok, R.color.ok_soft
                )
                Bai.TU_CHOI -> Triple(getString(R.string.bai_tu_choi), R.color.alert, R.color.alert_soft)
                else -> Triple(getString(R.string.bai_cho), R.color.wait, R.color.wait_soft)
            }
            v.nhan.text = chu
            v.nhan.setTextColor(ContextCompat.getColor(ct, mau))
            v.nhan.backgroundTintList = ContextCompat.getColorStateList(ct, nen)

            val cham = bai.cham
            v.tomTat.text = when {
                cham == null -> getString(R.string.bai_ai_chua_cham)
                cham.tomTat.isNotBlank() -> cham.tomTat
                else -> "${cham.mon}: đúng ${cham.soDung()}/${cham.cac.size} câu"
            }

            veAnh(bai)
            v.root.setOnClickListener {
                startActivity(
                    Intent(requireContext(), BaiActivity::class.java)
                        .putExtra(BaiActivity.EXTRA_ID, bai.id)
                )
            }
        }

        /**
         * Vai tam anh nho trong dong.
         *
         * Dung ba tam thoi. Mot lan nop co the co den muoi tam, tai het ca muoi cho
         * mot dong danh sach la ton mang ma khong ai nhin - muon xem thi bam vao.
         */
        private fun veAnh(bai: Bai) {
            v.hangAnh.removeAllViews()
            val token = Nha.token(requireContext())
            if (token.isBlank() || bai.anh.isEmpty()) {
                v.hangAnh.visibility = View.GONE
                return
            }
            v.hangAnh.visibility = View.VISIBLE

            bai.anh.take(3).forEach { anh ->
                val o = ImageView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(150, 150).also {
                        it.marginEnd = 16
                    }
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.canvas))
                }
                v.hangAnh.addView(o)
                viewLifecycleOwner.lifecycleScope.launch {
                    val f = TaiAnh.lay(requireContext(), token, anh.fileId) ?: return@launch
                    TaiAnh.doc(f, 200)?.let { o.setImageBitmap(it) }
                }
            }
        }
    }
}
