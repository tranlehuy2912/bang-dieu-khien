package vn.huytl.bangdieukhien.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Ba khuc cua thanh ngay o the chinh, tinh tu ban trang thai tablet gui (1/10/2026). Phai ra
 * dung so voi thanh ngay ben tablet, ma ben do tinh bang dong ho cua no: ben nay chi co cac moc
 * dung yen va tu cong doan dang chay.
 */
class SoNgayTest {

    private val phut = 60_000L

    /** Mot moc bat ky, chi dung lam goc cho cac phep cong. */
    private val luc = 1_790_000_000_000L

    @Test
    fun tablet_ban_cu_chua_gui_so_da_choi_thi_khong_co_thanh_ba_khuc() {
        val tt = TrangThai(cong = Cong.DANG_CHOI, phutDaDuyet = 45, phutConLai = 170)
        assertNull(tt.soNgay(luc))
    }

    @Test
    fun vi_du_cua_ba_huy_kiem_45_ba_cho_30_choi_30_roi_tam_dung() {
        val tt = TrangThai(
            cong = Cong.TAM_DUNG, conLaiMs = 45 * phut,
            phutDaDuyet = 45, phutConLai = 170, daChoiMs = 30 * phut
        )
        val so = tt.soNgay(luc)!!
        assertEquals(30, so.daChoi)
        assertEquals(45, so.con)
        assertEquals(75, so.duoc)
        assertEquals(245, so.tong)
    }

    @Test
    fun dang_choi_thi_doan_dang_chay_tu_cong_theo_dong_ho() {
        // Da gom 20 phut tu doan truoc. Doan nay bat dau tu [luc], phien het sau 45 phut.
        val tt = TrangThai(
            cong = Cong.DANG_CHOI, ketThucLuc = luc + 45 * phut, doanChoiTu = luc,
            phutConLai = 170, daChoiMs = 20 * phut
        )
        val so = tt.soNgay(luc + 10 * phut)!!
        assertEquals(30, so.daChoi)
        assertEquals(35, so.con)
    }

    @Test
    fun qua_moc_ket_thuc_ma_tablet_chua_bao_thi_khong_dem_them() {
        // Tablet cat phien o vong dem 30 giay sau. Trong luc do app da khoa, khong phai choi.
        val tt = TrangThai(
            cong = Cong.DANG_CHOI, ketThucLuc = luc + 45 * phut, doanChoiTu = luc, daChoiMs = 0L
        )
        val so = tt.soNgay(luc + 46 * phut)!!
        assertEquals(45, so.daChoi)
        assertEquals(0, so.con)
    }

    @Test
    fun choi_le_giay_thi_hai_so_van_cong_dung_bang_phieu() {
        // Choi 29 phut 30 giay cua phieu 45 phut: 29 + 16 = 45, nhu ben tablet.
        val tt = TrangThai(
            cong = Cong.DANG_CHOI, ketThucLuc = luc + 45 * phut, doanChoiTu = luc, daChoiMs = 0L
        )
        val so = tt.soNgay(luc + 29 * phut + 30_000L)!!
        assertEquals(29, so.daChoi)
        assertEquals(16, so.con)
        assertEquals(45, so.duoc)
    }

    @Test
    fun khong_dang_choi_thi_khong_cong_doan_nao_du_moc_con_sot() {
        // Phieu da duyet chua bam choi: doanChoiTu sot lai cung khong duoc tinh.
        val tt = TrangThai(
            cong = Cong.DA_DUYET, conLaiMs = 30 * phut, doanChoiTu = luc, daChoiMs = 15 * phut
        )
        val so = tt.soNgay(luc + 60 * phut)!!
        assertEquals(15, so.daChoi)
        assertEquals(30, so.con)
    }
}
