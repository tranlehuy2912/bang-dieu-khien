package vn.huytl.bangdieukhien.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.Calendar

/**
 * Doc va kiem mot ban lich (4/10/2026). File BanLich.kt chep y het ben tablet, nen test o day
 * chay tren may tinh la kiem luon cho tablet.
 *
 * Doi lich that thi Claude Code dan JSON moi vao LichMacDinh.kt roi chay lop nay truoc khi ghi
 * len Firestore: [ban_mac_dinh_doc_duoc] hong la ban moi co cho sai.
 */
class BanLichTest {

    @Test
    fun ban_mac_dinh_doc_duoc() {
        val b = BanLich.doc(LichMacDinh.JSON)
        assertTrue(b.phienBan >= 1)
        assertTrue(b.lop.isNotBlank())
        assertTrue(b.monTheoBuoi.values.any { it.isNotEmpty() })
    }

    @Test
    fun doc_dung_tung_phan_cua_lich_mau() {
        val b = LichMau.ban
        assertEquals("8A15", b.lop)
        assertEquals(45, b.phutMoiTiet)
        assertEquals(12 * 60 + 45, b.gioTiet.getValue(Buoi.CHIEU).getValue(1))
        assertEquals(listOf(1, 2, 3, 4, 5), b.gioTiet.getValue(Buoi.SANG).keys.toList())
        assertEquals(11 * 60 + 30, b.buongMayChieu)
        assertEquals(mapOf(Calendar.MONDAY to 8 * 60 + 45, Calendar.FRIDAY to 8 * 60 + 45), b.buongMaySang)
        assertEquals("Toán", b.monTheoBuoi.getValue(Buoi.CHIEU).getValue(Calendar.MONDAY).getValue(2))
        assertEquals(setOf(Calendar.MONDAY, Calendar.FRIDAY), b.monTheoBuoi.getValue(Buoi.SANG).keys)
        assertEquals(BanLich.KyNghi(20270201, 20270210, "nghỉ Tết Nguyên đán"), b.cacKyNghi[1])
        assertEquals(20270531, b.hetNamHoc)
    }

    @Test
    fun ghi_chu_khong_bat_buoc() {
        assertEquals("", BanLich.doc(sua { it.remove("ghiChu") }).ghiChu)
    }

    @Test
    fun ban_hong_thi_noi_dung_cho_hong() {
        fun hong(cho: String, json: String) {
            try {
                BanLich.doc(json)
                fail("phai hong o $cho")
            } catch (e: IllegalArgumentException) {
                assertTrue("can '$cho', ra '${e.message}'", e.message.orEmpty().startsWith("$cho:"))
            }
        }
        hong("JSON", "{ chưa đóng")
        hong("phienBan", sua { it.put("phienBan", "2") })
        hong("phienBan", sua { it.put("phienBan", 0) })
        hong("chieu", sua { it.remove("chieu") })
        // Go sai ten khoa: bao ngay, khong im lang bo qua.
        hong("chieuu", sua { it.put("chieuu", JSONObject()) })
        hong("chieu.8", sua { it.getJSONObject("chieu").put("8", JSONObject("""{"1":"Toán"}""")) })
        hong("chieu.7", sua { it.getJSONObject("chieu").put("7", JSONObject()) })
        hong("chieu.3.6", sua { it.getJSONObject("chieu").getJSONObject("3").put("6", "Toán") })
        hong("chieu.2.1", sua { it.getJSONObject("chieu").getJSONObject("2").put("1", "  ") })
        hong("gioTiet.sang.1", sua { it.getJSONObject("gioTiet").getJSONObject("sang").put("1", "7:15") })
        hong("gioTiet.sang.1", sua { it.getJSONObject("gioTiet").getJSONObject("sang").put("1", "25:00") })
        hong("gioTiet.chieu.2", sua { it.getJSONObject("gioTiet").getJSONObject("chieu").put("2", "12:50") })
        hong("gioTiet.chieu", sua { it.getJSONObject("gioTiet").getJSONObject("chieu").remove("3") })
        hong("buongMay.chieu", sua { it.getJSONObject("buongMay").put("chieu", "13:00") })
        hong("buongMay.sang.3", sua { it.getJSONObject("buongMay").getJSONObject("sang").put("3", "08:00") })
        hong("buongMay.sang.2", sua { it.getJSONObject("buongMay").getJSONObject("sang").put("2", "09:30") })
        hong("ngayNghi.1", sua { it.getJSONArray("ngayNghi").getJSONObject(1).put("den", "2027-01-31") })
        hong("ngayNghi.0.tu", sua { it.getJSONArray("ngayNghi").getJSONObject(0).put("tu", "2027-02-30") })
        hong("hetNamHoc", sua { it.put("hetNamHoc", "31/05/2027") })
        hong("sang, chieu", sua {
            it.put("sang", JSONObject())
            it.put("chieu", JSONObject())
            it.getJSONObject("buongMay").put("sang", JSONObject())
        })
    }

    /** Lich mau sua theo [doi], tra ve chuoi JSON. */
    private fun sua(doi: (JSONObject) -> Unit): String =
        JSONObject(LichMau.JSON).apply(doi).toString()
}
