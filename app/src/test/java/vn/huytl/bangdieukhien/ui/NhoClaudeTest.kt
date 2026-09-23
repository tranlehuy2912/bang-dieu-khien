package vn.huytl.bangdieukhien.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.huytl.bangdieukhien.data.Bai
import vn.huytl.bangdieukhien.data.CauCham
import vn.huytl.bangdieukhien.data.KetQuaCham

/**
 * Doc ket qua Claude tu cau tra loi Ba Huy chep ve, va loi nho gui sang Claude.
 *
 * Doan doc la cho de hong nhat cua duong nay: no nhan chu do Claude viet, ma Claude
 * co the boc khoi JSON trong khung code, viet them chu truoc sau, hay chep lai mot
 * bieu thuc co ngoac vao goi y.
 */
class NhoClaudeTest {

    @Test
    fun doc_duoc_khoi_json_tron() {
        val ket = NhoClaude.docKetQua(
            """{"bai":"b1308359","ket_qua":[{"ma":"2.32b","dung":true,"chac":true,"con_viet":"1000000000","goi_y":""}]}"""
        )
        assertNotNull(ket)
        assertEquals("b1308359", ket!!.bai)
        assertEquals("2.32b", ket.cac.single().ma)
        assertTrue(ket.cac.single().dung)
        assertEquals("1000000000", ket.cac.single().conViet)
    }

    @Test
    fun doc_duoc_khoi_json_trong_cau_tra_loi_dai_co_khung_code() {
        val chu = """
            | Câu | Con viết | Đúng |
            |---|---|---|
            | 2.33a | 8x^2 + 20xy | đúng |

            Máy chấm nhầm câu 2.33a: hai hạng tử 25y^2 triệt tiêu nhau.

            ```json
            {"bai":"b1","ket_qua":[
              {"ma":"2.33a","dung":true,"chac":true,"con_viet":"8x^2 + 20xy","goi_y":""},
              {"ma":"2.34b","dung":false,"chac":true,"con_viet":"(8x - 3y)(...)","goi_y":"64x^3 là lập phương của số nào?"}
            ]}
            ```
            Chúc con học tốt.
        """.trimIndent()
        val ket = NhoClaude.docKetQua(chu)
        assertNotNull(ket)
        assertEquals(listOf("2.33a", "2.34b"), ket!!.cac.map { it.ma })
        assertFalse(ket.cac[1].dung)
        assertEquals("64x^3 là lập phương của số nào?", ket.cac[1].goiY)
    }

    @Test
    fun ngoac_va_nhay_trong_chuoi_khong_lam_lech_khoi() {
        val chu = """{"bai":"b2","ket_qua":[{"ma":"1.1","dung":false,"chac":true,""" +
            """"con_viet":"{a} }","goi_y":"Xem lại chỗ \"(x - y)^3\" và {ngoặc} }"}]} phần chữ sau"""
        val ket = NhoClaude.docKetQua(chu)
        assertNotNull(ket)
        assertEquals("{a} }", ket!!.cac.single().conViet)
        assertEquals("Xem lại chỗ \"(x - y)^3\" và {ngoặc} }", ket.cac.single().goiY)
    }

    @Test
    fun co_hai_khoi_thi_lay_khoi_cuoi() {
        val chu = """Mẫu: {"bai":"cu","ket_qua":[{"ma":"9.9","dung":true}]}
            |Kết quả: {"bai":"moi","ket_qua":[{"ma":"2.28","dung":true}]}""".trimMargin()
        assertEquals("moi", NhoClaude.docKetQua(chu)!!.bai)
    }

    @Test
    fun thieu_hay_sai_kieu_truong_thi_nghieng_ve_an_toan() {
        // Khong co ket luan dung hay sai thi cau do khong mang thong tin: bo di.
        assertNull(NhoClaude.docKetQua("""{"ket_qua":[{"ma":"2.28"}]}"""))
        assertNull(NhoClaude.docKetQua("""{"ket_qua":[{"ma":"2.28","dung":"true"}]}"""))

        val ket = NhoClaude.docKetQua(
            """{"ket_qua":[{"ma":"2.28","dung":true},{"ma":"2.29","dung":true,"chac":"có"}]}"""
        )!!
        assertEquals("", ket.bai)
        // Thieu "chac" thi coi la chac. "chac" viet sai kieu thi coi la khong chac.
        assertTrue(ket.cac[0].chac)
        assertFalse(ket.cac[1].chac)
    }

    @Test
    fun khong_co_khoi_json_thi_tra_null() {
        assertNull(NhoClaude.docKetQua(null))
        assertNull(NhoClaude.docKetQua(""))
        assertNull(NhoClaude.docKetQua("Chỉ là một câu trả lời thường, không có khối nào."))
        assertNull(NhoClaude.docKetQua("""{"ket_qua": [ {"ma": "2.28", "dung": tr"""))
        assertNull(NhoClaude.docKetQua("""{"bai":"b","ket_qua":[]}"""))
    }

    @Test
    fun loi_nho_co_ma_bai_va_dat_cach_cham_truoc_ket_luan_cua_may() {
        val bai = Bai(
            id = "b1308359",
            luc = 1_790_133_813_356L,
            trangThai = Bai.DUYET,
            soPhut = 3,
            anh = emptyList(),
            cham = KetQuaCham(
                mon = "Toán",
                cac = listOf(
                    CauCham(ma = "2.32b", de = "Tính nhanh...", ketQua = "11000000000", dung = false)
                ),
                tomTat = "🤖 AI chấm: Toán\n• Con khai: SGK Toán 8 — tập một — trang 47\n• ..."
            ),
            messageId = 1675L
        )
        val chu = NhoClaude.loiNho(bai, "Lê Hòa")

        assertTrue(chu.contains("bài tập về nhà của Lê Hòa"))
        assertTrue(chu.contains("Con khai đang làm: SGK Toán 8 — tập một — trang 47."))
        assertTrue(chu.contains("\"bai\":\"b1308359\""))
        assertTrue(chu.indexOf("Cách chấm từng câu") < chu.indexOf("Máy đọc con viết: 11000000000"))
    }

    @Test
    fun dan_nham_loi_nho_thi_khong_thanh_ket_qua() {
        val bai = Bai(
            id = "b9", luc = 0L, trangThai = Bai.DUYET, soPhut = 0, anh = emptyList(),
            cham = KetQuaCham(cac = listOf(CauCham(ma = "2.28", de = "Đề", ketQua = "A"))),
            messageId = 0L
        )
        val chu = NhoClaude.loiNho(bai, "Lê Hòa")
        // Bam nho Claude la loi nho nam san trong bo nho tam. Mau JSON trong do tuyet
        // doi khong duoc doc ra thanh ket qua, khong thi cau mau thanh dung.
        assertTrue(NhoClaude.laLoiNho(chu))
        assertNull(NhoClaude.docKetQua(chu))
        assertFalse(NhoClaude.laLoiNho("""{"bai":"b9","ket_qua":[{"ma":"2.28","dung":true}]}"""))
    }
}
