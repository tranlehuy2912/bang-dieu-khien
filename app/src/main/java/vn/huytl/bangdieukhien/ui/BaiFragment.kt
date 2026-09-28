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
import com.google.android.material.bottomsheet.BottomSheetDialog
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
import vn.huytl.bangdieukhien.databinding.TamBaiDaXoaBinding
import vn.huytl.bangdieukhien.telegram.TaiAnh

/**
 * Danh sach cac lan con nop bai, moi nhat truoc.
 *
 * Chi hien mot dong tom tat va vai tam anh nho. Bam vao mot dong moi mo man chi
 * tiet - cho do moi tai anh to va goi ban cham cua AI ra.
 *
 * Bai da xong co nut Xoa, dau danh sach co nut xoa het bai da xong. Xoa chi an bai
 * khoi danh sach nay, xem [Kho.anBai]. Cuoi danh sach co nut nho "Bài đã xoá (3)" mo
 * mot tam keo tu duoi len, moi bai da xoa mot nut Khoi phuc.
 *
 * Truoc 27/9/2026 cho nut nho do la mot dong chu "Hiện lại 3 bài đã xoá" rong het man,
 * bam la dua ca ba bai ve mot luot, khong xem truoc duoc do la nhung bai nao.
 */
class BaiFragment : Fragment() {

    private var _b: FragmentBaiBinding? = null
    private val b get() = _b!!
    private var nghe: ListenerRegistration? = null
    private val bo = Bo(daXoa = false)
    private val dau = DongNut(R.layout.item_nut_danh_sach) { xoaHetBaiXong() }
    private val cuoi = DongNut(R.layout.item_nut_bai_da_xoa) { moBaiDaXoa() }

    /** Ca danh sach vua doc ve, ke ca bai da xoa. */
    private var tatCa: List<Bai> = emptyList()

    /**
     * Cac bai da xoa, cho tam keo len. Song theo fragment nhu [bo] va cung doi moi lan
     * Firestore goi lai, nen tam dang mo thay ngay bai vua khoi phuc bien di.
     */
    private val boDaXoa = Bo(daXoa = true)

    /** Tam bai da xoa dang mo, null khi dong. */
    private var tamDaXoa: BottomSheetDialog? = null

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentBaiBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        b.danhSach.layoutManager = LinearLayoutManager(requireContext())
        b.danhSach.adapter = ConcatAdapter(dau, bo, cuoi)
        b.nutDaXoaTrong.setOnClickListener { moBaiDaXoa() }
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
        // Ba adapter song theo fragment, con danh sach thi chet theo view. Khong go ra
        // thi moi lan doi tab lai de lai mot danh sach cu (ca anh nho) treo tren adapter.
        _b?.danhSach?.adapter = null
        _b = null
        // Tam la mot cua so rieng: khong dong thi no con treo lai sau khi xoay man.
        tamDaXoa?.dismiss()
        super.onDestroyView()
    }

    private fun ve(ds: List<Bai>) {
        tatCa = ds
        /*
         * Dang o dau danh sach thi ve xong van o dau.
         *
         * Bai vua nop chen vao vi tri 0, ma LinearLayoutManager giu dong dang nam tren cung
         * dung yen. Danh sach da tran man thi bai moi nam khuat tren mep man, Ba Huy thay
         * dong dau van la bai cu va tuong dien thoai chua nhan. Co dong "Xoá hết" o dau thi
         * dong do dung yen va bai moi hien ngay duoi no, nen truoc 28/9/2026 luc thay luc
         * khong. Dang cuon xuong xem bai cu thi thoi, khong keo len.
         *
         * Chua ve lan nao (vua xoay man) thi thoi: luc do canScrollVertically luon tra false,
         * va cuon ve dau se bo mat cho dang cuon vua khoi phuc.
         */
        val oDau = b.danhSach.isLaidOut && !b.danhSach.canScrollVertically(-1)
        val hien = ds.filter { !it.an }
        val daXoa = ds.filter { it.an }
        val soXong = hien.count { it.xong }
        // Mot bai xong thi nut Xoa ngay tren dong do la du.
        dau.dat(if (soXong >= 2) "Xoá hết $soXong bài đã xong" else null)
        bo.dat(hien)

        val chuDaXoa = if (daXoa.isEmpty()) null else getString(R.string.bai_da_xoa_nut, daXoa.size)
        // Danh sach trong thi nut nam duoi dong chu giua man, thay cho dong cuoi danh sach.
        cuoi.dat(if (hien.isEmpty()) null else chuDaXoa)
        b.nutDaXoaTrong.text = chuDaXoa
        b.nutDaXoaTrong.visibility = if (chuDaXoa != null) View.VISIBLE else View.GONE
        b.trong.setText(if (ds.isEmpty()) R.string.bai_trong else R.string.bai_da_xoa_het)
        b.khungTrong.visibility = if (hien.isEmpty()) View.VISIBLE else View.GONE

        boDaXoa.dat(daXoa)
        // Khoi phuc het roi thi tam khong con gi de xem.
        if (daXoa.isEmpty()) tamDaXoa?.dismiss()

        if (oDau) b.danhSach.scrollToPosition(0)
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

    /**
     * Mo tam bai da xoa.
     *
     * Tam chu khong phai mot man rieng: van la danh sach vua doc ve, khong phai doc lai
     * Firestore lan nua, va keo xuong la ve dung cho cu trong danh sach.
     */
    private fun moBaiDaXoa() {
        if (tamDaXoa != null) return
        val tam = BottomSheetDialog(requireContext(), R.style.ThemeOverlay_BangDieuKhien_TamDuoi)
        // Dung theme cua tam, xem ThemeOverlay.BangDieuKhien.TamDuoi.
        val v = TamBaiDaXoaBinding.inflate(LayoutInflater.from(tam.context))
        v.danhSach.layoutManager = LinearLayoutManager(tam.context)
        v.danhSach.adapter = boDaXoa
        tamDaXoa = tam.apply {
            setContentView(v.root)
            setOnDismissListener {
                v.danhSach.adapter = null
                tamDaXoa = null
            }
            show()
        }
    }

    /**
     * Dua mot bai ve lai danh sach. Khong co thanh bao: bai do bien khoi tam ngay truoc
     * mat, va muon xoa lai thi nut Xoa van nam tren dong cua no.
     */
    private fun khoiPhuc(bai: Bai) {
        val ct = requireContext().applicationContext
        Kho.anBai(ct, listOf(bai.id), false) { kq -> if (kq is Kho.KetQua.Hong) Dinh.noi(ct, kq.viSao) }
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    /** [daXoa] la danh sach trong tam bai da xoa: nut Khoi phuc thay cho nut Xoa. */
    private inner class Bo(private val daXoa: Boolean) : RecyclerView.Adapter<O>() {
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
            O(ItemBaiBinding.inflate(LayoutInflater.from(cha.context), cha, false), daXoa)

        override fun getItemCount() = cac.size

        override fun onBindViewHolder(o: O, i: Int) = o.gan(cac[i])
    }

    /**
     * Mot nut nam rieng mot dong o dau hay cuoi danh sach. Chu null la khong co dong nao.
     *
     * Nam trong danh sach chu khong ghim tren dau man hinh, nen cuon xuong la khuat va
     * khong chiem cho cua cac bai.
     */
    private inner class DongNut(private val khuon: Int, private val bam: () -> Unit) :
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
            val dong = layoutInflater.inflate(khuon, cha, false)
            dong.findViewById<MaterialButton>(R.id.nut).setOnClickListener { bam() }
            return object : RecyclerView.ViewHolder(dong) {}
        }

        override fun onBindViewHolder(o: RecyclerView.ViewHolder, i: Int) {
            o.itemView.findViewById<MaterialButton>(R.id.nut).text = chu
        }
    }

    private inner class O(private val v: ItemBaiBinding, private val daXoa: Boolean) :
        RecyclerView.ViewHolder(v.root) {

        fun gan(bai: Bai) {
            val ct = requireContext()
            v.gio.text = Dinh.lucNgan(bai.luc)

            val (chu, mau, nen) = when {
                bai.dangCho -> Triple(getString(R.string.bai_cho), R.color.wait, R.color.wait_soft)
                // Cham xong trong gio ngu: gio chua vao tay con, het gio ngu tablet moi cong.
                bai.choCong -> Triple(
                    "${Dinh.gioPhut(bai.congLuc)} cộng ${Dinh.phut(bai.soPhut)}",
                    R.color.wait, R.color.wait_soft
                )
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

            v.nutXoa.visibility = if (bai.xong && !daXoa) View.VISIBLE else View.GONE
            v.nutXoa.setOnClickListener { xoa(bai) }
            v.nutKhoiPhuc.visibility = if (daXoa) View.VISIBLE else View.GONE
            v.nutKhoiPhuc.setOnClickListener { khoiPhuc(bai) }

            val cham = bai.cham
            val cl = bai.claude
            v.tomTat.text = when {
                cham != null && cham.tomTat.isNotBlank() -> cham.tomTat
                cham != null && cham.cac.isNotEmpty() ->
                    "${cham.mon}: đúng ${cham.soDung()}/${cham.cac.size} câu"
                cl != null -> "Claude chấm: đúng ${cl.cac.count { it.chac && it.dung }}/${cl.cac.size} câu"
                // Tablet khong tu cham (tu 28/9/2026): day la danh sach bai cho Ba Huy cham
                // bang Claude.
                bai.dangCho && bai.anh.isNotEmpty() -> "Chưa chấm. Bấm vào để nhờ Claude chấm."
                else -> "Chưa chấm."
            }

            // Trong tam bai da xoa chi can nhan ra bai nao, dong gon thi thay duoc nhieu
            // bai mot luc. Muon xem anh thi bam vao dong.
            if (daXoa) v.hangAnh.visibility = View.GONE else veAnh(bai)
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
