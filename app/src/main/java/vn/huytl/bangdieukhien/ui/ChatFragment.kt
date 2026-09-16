package vn.huytl.bangdieukhien.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ListenerRegistration
import vn.huytl.bangdieukhien.R
import vn.huytl.bangdieukhien.data.Kho
import vn.huytl.bangdieukhien.data.TinChat
import vn.huytl.bangdieukhien.databinding.FragmentChatBinding
import vn.huytl.bangdieukhien.databinding.ItemTinBinding

/**
 * Nhan tin voi con.
 *
 * Truoc day cho nay la go chu thuong trong Telegram. Doi sang day duoc mot cai:
 * tin cua con va tin cua ba nam cung mot cho, khong lan giua dam thong bao nop
 * bai va cac lenh.
 */
class ChatFragment : Fragment() {

    private var _b: FragmentChatBinding? = null
    private val b get() = _b!!
    private var nghe: ListenerRegistration? = null
    private val bo = Bo()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentChatBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        b.danhSach.layoutManager = LinearLayoutManager(requireContext()).apply {
            // Dinh day: tin moi nhat luon nam duoi cung, ban phim mo ra cung khong
            // day mat cai vua go.
            stackFromEnd = true
        }
        b.danhSach.adapter = bo
        b.nutGui.setOnClickListener { gui() }
    }

    override fun onStart() {
        super.onStart()
        nghe = Kho.ngheChat(requireContext(), 60L) { ds ->
            if (_b == null) return@ngheChat
            bo.dat(ds)
            b.trong.visibility = if (ds.isEmpty()) View.VISIBLE else View.GONE
            if (ds.isNotEmpty()) b.danhSach.scrollToPosition(ds.size - 1)
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

    private fun gui() {
        val chu = b.oGo.text?.toString()?.trim().orEmpty()
        if (chu.isEmpty()) return
        b.oGo.setText("")
        Kho.guiTin(requireContext(), chu) { kq ->
            if (kq is Kho.KetQua.Hong) {
                Dinh.noi(requireContext(), kq.viSao)
                // Tra lai cau vua go de khong phai go lai tu dau.
                _b?.oGo?.setText(chu)
            }
        }
    }

    private inner class Bo : RecyclerView.Adapter<O>() {
        private var cac: List<TinChat> = emptyList()

        fun dat(moi: List<TinChat>) {
            cac = moi
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(cha: ViewGroup, kieu: Int) =
            O(ItemTinBinding.inflate(layoutInflater, cha, false))

        override fun getItemCount() = cac.size

        override fun onBindViewHolder(o: O, i: Int) = o.gan(cac[i])
    }

    private inner class O(private val v: ItemTinBinding) : RecyclerView.ViewHolder(v.root) {
        fun gan(tin: TinChat) {
            val ct = requireContext()
            v.bong.text = tin.chu
            v.gio.text = Dinh.lucNgan(tin.luc)

            val le = if (tin.cuaCon) Gravity.START else Gravity.END
            (v.bong.layoutParams as LinearLayout.LayoutParams).gravity = le
            (v.gio.layoutParams as LinearLayout.LayoutParams).gravity = le

            if (tin.cuaCon) {
                v.bong.setBackgroundResource(R.drawable.bong_con)
                v.bong.setTextColor(ContextCompat.getColor(ct, R.color.ink))
            } else {
                v.bong.setBackgroundResource(R.drawable.bong_ba)
                v.bong.setTextColor(ContextCompat.getColor(ct, android.R.color.white))
            }
        }
    }
}
