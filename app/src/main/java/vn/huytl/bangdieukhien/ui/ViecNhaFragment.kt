package vn.huytl.bangdieukhien.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import vn.huytl.bangdieukhien.databinding.FragmentViecNhaBinding

/**
 * Tab Viec nha. Moi viec giao, bam xong va sua danh sach nam o [KhoiViecNha]; tab nay chi
 * giu khoi do song theo vong doi cua minh.
 *
 * Truoc 27/9/2026 khoi nay la mot the giua tab Bang. Tab Gio choi bay gio chi con mot dong
 * "Viec nha chua xong" dan sang day khi tablet dang khoa vi viec nha.
 */
class ViecNhaFragment : Fragment() {

    private var _b: FragmentViecNhaBinding? = null
    private val b get() = _b!!

    private var khoi: KhoiViecNha? = null

    /**
     * Canh "tablet bo qua" tinh theo gio, khong theo Firestore, nen phai xet lai deu dan.
     * Xem [KhoiViecNha.moiGiay].
     */
    private val tay = Handler(Looper.getMainLooper())
    private val nhip = object : Runnable {
        override fun run() {
            khoi?.moiGiay()
            tay.postDelayed(this, 1000L)
        }
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentViecNhaBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        khoi = KhoiViecNha(requireContext(), b)
    }

    override fun onStart() {
        super.onStart()
        khoi?.batNghe()
        tay.post(nhip)
    }

    override fun onStop() {
        tay.removeCallbacks(nhip)
        khoi?.goNghe()
        super.onStop()
    }

    override fun onDestroyView() {
        khoi?.bo()
        khoi = null
        _b = null
        super.onDestroyView()
    }
}
