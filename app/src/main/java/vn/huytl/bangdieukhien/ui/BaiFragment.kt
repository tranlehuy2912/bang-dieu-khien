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
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
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
 *
 * Bai da xong co nut Xoa, dau danh sach co nut xoa het bai da xong, cuoi danh sach co
 * nut hien lai. Xoa chi an bai khoi danh sach nay, xem [Kho.anBai].
 */
class BaiFragment : Fragment() {

    private var _b: FragmentBaiBinding? = null
    private val b get() = _b!!
    private var nghe: ListenerRegistration? = null
    private val bo = Bo()
    private val dau = DongNut { xoaHetBaiXong() }
    private val cuoi = DongNut { hienLai() }

    /** Ca danh sach vua doc ve, ke ca bai da xoa: nut hien lai can biet do la nhung bai nao. */
    private var tatCa: List<Bai> = emptyList()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentBaiBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        b.danhSach.layoutManager = LinearLayoutManager(requireContext())
        b.danhSach.adapter = ConcatAdapter(dau, bo, cuoi)
    }

    override fun onStart() {
        super.onStart()
        // Ba muoi lan nop gan nhat la du xa: hon the thi khong ai cuon toi, ma moi
        // document doc ve deu tinh mot luot trong han muc ngay cua Firestore.
        nghe = Kho.ngheBai(requireContext(), 30L) { ds ->
            if (_b == null) return@ngheBai
            ve(ds)
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

    private fun ve(ds: List<Bai>) {
        tatCa = ds
        val hien = ds.filter { !it.an }
        val soXong = hien.count { it.xong }
        val soAn = ds.size - hien.size
        // Mot bai xong thi nut Xoa ngay tren dong do la du.
        dau.dat(if (soXong >= 2) "Xoá hết $soXong bài đã xong" else null)
        bo.dat(hien)
        cuoi.dat(if (soAn > 0) "Hiện lại $soAn bài đã xoá" else null)
        b.trong.setText(if (ds.isEmpty()) R.string.bai_trong else R.string.bai_da_xoa_het)
        b.trong.visibility = if (hien.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun xoa(bai: Bai) =
        an(listOf(bai.id), "Đã xoá bài ${Dinh.lucNgan(bai.luc)} khỏi danh sách.")

    private fun xoaHetBaiXong() {
        val ids = tatCa.filter { !it.an && it.xong }.map { it.id }
        an(ids, "Đã xoá ${ids.size} bài khỏi danh sách.")
    }

    /**
     * An bai roi hien thanh bao co nut hoan tac.
     *
     * Khong hoi lai truoc: bam nham thi hoan tac ngay tren thanh bao, qua luc do thi con
     * nut hien lai o cuoi danh sach.
     */
    private fun an(ids: List<String>, noi: String) {
        if (ids.isEmpty()) return
        val ct = requireContext().applicationContext
        Kho.anBai(ct, ids, true) { kq -> if (kq is Kho.KetQua.Hong) Dinh.noi(ct, kq.viSao) }
        Snackbar.make(b.root, noi, Snackbar.LENGTH_LONG)
            .setAnchorView(requireActivity().findViewById<View>(R.id.thanhDuoi))
            .setAction(R.string.bai_hoan_tac) { Kho.anBai(ct, ids, false) }
            .show()
    }

    private fun hienLai() {
        val ct = requireContext().applicationContext
        val ids = tatCa.filter { it.an }.map { it.id }
        Kho.anBai(ct, ids, false) { kq -> if (kq is Kho.KetQua.Hong) Dinh.noi(ct, kq.viSao) }
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private inner class Bo : RecyclerView.Adapter<O>() {
        private var cac: List<Bai> = emptyList()

        /**
         * Chi ve lai nhung dong that su doi.
         *
         * Firestore goi lai moi lan bat ky truong nao doi, ke ca mot bai moi them
         * o dau danh sach. Ve lai tat ca thi moi dong deu vut anh di roi doc lai,
         * va ca danh sach chop mot cai. [Bai] la data class nen so sanh duoc thang
         * bang dau bang.
         */
        fun dat(moi: List<Bai>) {
            val cu = cac
            cac = moi
            DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize() = cu.size
                override fun getNewListSize() = moi.size
                override fun areItemsTheSame(a: Int, b: Int) = cu[a].id == moi[b].id
                override fun areContentsTheSame(a: Int, b: Int) = cu[a] == moi[b]
            }).dispatchUpdatesTo(this)
        }

        override fun onCreateViewHolder(cha: ViewGroup, kieu: Int) =
            O(ItemBaiBinding.inflate(layoutInflater, cha, false))

        override fun getItemCount() = cac.size

        override fun onBindViewHolder(o: O, i: Int) = o.gan(cac[i])
    }

    /**
     * Mot nut nam rieng mot dong o dau hay cuoi danh sach. Chu null la khong co dong nao.
     *
     * Nam trong danh sach chu khong ghim tren dau man hinh, nen cuon xuong la khuat va
     * khong chiem cho cua cac bai.
     */
    private inner class DongNut(private val bam: () -> Unit) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var chu: String? = null

        fun dat(moi: String?) {
            val cu = chu
            chu = moi
            when {
                cu == null && moi != null -> notifyItemInserted(0)
                cu != null && moi == null -> notifyItemRemoved(0)
                cu != moi -> notifyItemChanged(0)
            }
        }

        override fun getItemCount() = if (chu == null) 0 else 1

        override fun onCreateViewHolder(cha: ViewGroup, kieu: Int): RecyclerView.ViewHolder {
            val nut = layoutInflater.inflate(R.layout.item_nut_danh_sach, cha, false)
            nut.setOnClickListener { bam() }
            return object : RecyclerView.ViewHolder(nut) {}
        }

        override fun onBindViewHolder(o: RecyclerView.ViewHolder, i: Int) {
            (o.itemView as MaterialButton).text = chu
        }
    }

    private inner class O(private val v: ItemBaiBinding) : RecyclerView.ViewHolder(v.root) {

        fun gan(bai: Bai) {
            val ct = requireContext()
            v.gio.text = Dinh.lucNgan(bai.luc)

            val (chu, mau, nen) = when {
                bai.dangCho -> Triple(getString(R.string.bai_cho), R.color.wait, R.color.wait_soft)
                bai.trangThai == Bai.DUYET -> Triple(
                    getString(R.string.bai_da_duyet) + " " + Dinh.phut(bai.soPhut),
                    R.color.ok, R.color.ok_soft
                )
                bai.trangThai == Bai.TU_CHOI ->
                    Triple(getString(R.string.bai_tu_choi), R.color.alert, R.color.alert_soft)
                bai.trangThai == Bai.HUY ->
                    Triple(getString(R.string.bai_huy, Nha.tenCon(ct)), R.color.ink_soft, R.color.line)
                bai.quaNgay() ->
                    Triple(getString(R.string.bai_qua_ngay), R.color.ink_soft, R.color.line)
                else -> Triple(bai.trangThai, R.color.ink_soft, R.color.line)
            }
            v.nhan.text = chu
            v.nhan.setTextColor(ContextCompat.getColor(ct, mau))
            v.nhan.backgroundTintList = ContextCompat.getColorStateList(ct, nen)

            v.nutXoa.visibility = if (bai.xong) View.VISIBLE else View.GONE
            v.nutXoa.setOnClickListener { xoa(bai) }

            val cham = bai.cham
            val cl = bai.claude
            v.tomTat.text = when {
                cham != null && cham.tomTat.isNotBlank() -> cham.tomTat
                cham != null && cham.cac.isNotEmpty() ->
                    "${cham.mon}: đúng ${cham.soDung()}/${cham.cac.size} câu"
                cl != null -> "Claude chấm: đúng ${cl.cac.count { it.chac && it.dung }}/${cl.cac.size} câu"
                // May tat cham AI thi day la danh sach bai cho Ba Huy cham bang Claude.
                bai.dangCho && bai.anh.isNotEmpty() -> "Máy chưa chấm. Bấm vào để nhờ Claude chấm."
                else -> getString(R.string.bai_ai_chua_cham)
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

            val canh = 76.dp()
            val cach = 10.dp()
            bai.anh.take(3).forEach { anh ->
                val o = ImageView(requireContext()).apply {
                    // Do bang dp chu khong bang pixel: 150 pixel tho tren mot may
                    // 3x ra vua dung 50dp, tuc la tam anh trang vo co lai bang con
                    // tem va khong con doc duoc chu gi.
                    layoutParams = LinearLayout.LayoutParams(canh, canh).also {
                        it.marginEnd = cach
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
