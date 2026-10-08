package vn.huytl.bangdieukhien.ui

import android.os.Bundle
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

    // Nhip moi giay de doi chu sang canh "tablet bo qua" bo ngay 8/10/2026, cung luc tablet
    // khong bo dot viec nha cu nua.

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
    }

    override fun onStop() {
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
