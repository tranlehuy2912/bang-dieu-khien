package vn.huytl.bangdieukhien.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.huytl.bangdieukhien.data.Anh
import vn.huytl.bangdieukhien.data.Bai
import vn.huytl.bangdieukhien.data.CauCham
import vn.huytl.bangdieukhien.data.CauClaude
import vn.huytl.bangdieukhien.data.KetQuaCham
import vn.huytl.bangdieukhien.data.KhaiBai
import vn.huytl.bangdieukhien.data.XuCau

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
    fun loi_nho_co_ma_bai_va_dat_cach_cham_truoc_ket_luan_lan_truoc() {
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
        assertTrue(chu.contains("Lê Hòa khai đang làm: SGK Toán 8 — tập một — trang 47."))
        assertTrue(chu.contains("\"bai\":\"b1308359\""))
        assertTrue(chu.indexOf("Cách chấm từng câu") < chu.indexOf("Lần trước đọc Lê Hòa viết: 11000000000"))
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

    // ------------------------------------------------------------ cham luon

    private val khai = KhaiBai(
        tenNguon = "SGK Toán 8 — tập một",
        bai = "trang 47",
        mon = "Toán",
        cac = listOf(
            KhaiBai.Cau("2.28", "toan8t1:2.28", "Đa thức x^2 − 9x + 8 được phân tích thành", "TRAC_NGHIEM"),
            KhaiBai.Cau("2.33a", "toan8t1:2.33a", "Rút gọn (2x + 5y)^2 − (2x − 5y)^2", "CAU_NHO")
        )
    )

    private fun baiChuaCham(khai: KhaiBai?) = Bai(
        id = "b77", luc = 1_790_133_813_356L, trangThai = Bai.CHO, soPhut = 0,
        anh = listOf(Anh("f1", "DE_BAI"), Anh("f2", "BAI_GIAI")),
        cham = null, messageId = 0L, khai = khai
    )

    @Test
    fun bai_chua_co_ban_cham_cua_may_thi_claude_cham_luon() {
        assertTrue(NhoClaude.chamMoi(baiChuaCham(null)))
        assertTrue(NhoClaude.chamMoi(baiChuaCham(null).copy(cham = KetQuaCham(mon = "Toán"))))
        assertFalse(
            NhoClaude.chamMoi(
                baiChuaCham(null).copy(cham = KetQuaCham(cac = listOf(CauCham(ma = "2.28"))))
            )
        )
    }

    @Test
    fun loi_nho_cham_luon_co_de_tung_cau_va_hoi_so_dong() {
        val chu = NhoClaude.loiNho(baiChuaCham(khai), "Lê Hòa")

        assertTrue(chu.contains("Nhờ bạn chấm bài tập về nhà của Lê Hòa"))
        assertTrue(chu.contains("Bạn là người chấm bài này"))
        assertTrue(chu.contains("Lê Hòa khai đang làm: SGK Toán 8 — tập một — trang 47."))
        assertTrue(chu.contains("ảnh đề bài và ảnh vở bài làm"))
        assertTrue(chu.contains("2. Câu 2.33a. Đề: Rút gọn (2x + 5y)^2 − (2x − 5y)^2"))
        assertTrue(chu.contains("\"so_dong\""))
        assertTrue(chu.contains("\"bai\":\"b77\""))
        // Khong co ket luan cua lan cham nao de ke ra, va khong hoi mau muc khi khong on tap.
        assertFalse(chu.contains("Lần trước đọc Lê Hòa viết"))
        assertFalse(chu.contains("muc_do"))
        // Cau trong sach da co de va dang, mau khong doi Claude chep lai.
        assertFalse(chu.contains("\"de\":\"chép đề"))

        // Loi nho nay nam san trong bo nho tam, ma dan nham thi khong duoc thanh ket qua.
        assertTrue(NhoClaude.laLoiNho(chu))
        assertNull(NhoClaude.docKetQua(chu))
    }

    // May cham roi ma bai van cho duyet: Claude cham luon, tablet tinh phut theo Claude.
    private val cuaMay = KetQuaCham(
        mon = "Toán",
        cac = listOf(
            CauCham(ma = "1", de = "Tính 2 + 3.", ketQua = "6", dung = false),
            CauCham(ma = "2", de = "Tính 4 + 5.", ketQua = "9", dung = true, docRo = false)
        )
    )

    @Test
    fun may_cham_roi_ma_bai_con_cho_duyet_thi_claude_cham_luon() {
        val homNay = baiChuaCham(null).copy(luc = System.currentTimeMillis(), cham = cuaMay)
        assertTrue(NhoClaude.chamMoi(homNay))
        // Da duyet, hay nop hom truoc (tablet da bo khoi hang cho): van cham lai nhu cu.
        assertFalse(NhoClaude.chamMoi(homNay.copy(trangThai = Bai.DUYET)))
        assertFalse(NhoClaude.chamMoi(homNay.copy(luc = 1_790_133_813_356L)))
    }

    @Test
    fun loi_nho_cham_luon_bai_da_cham_noi_ro_va_chep_cau_lan_truoc() {
        val bai = baiChuaCham(null).copy(luc = System.currentTimeMillis(), cham = cuaMay)
        val chu = NhoClaude.loiNho(bai, "Lê Hòa")
        assertTrue(chu.contains("đã được chấm một lần nhưng chưa duyệt"))
        assertTrue(chu.contains("1. Câu 1. Đề: Tính 2 + 3."))
        assertTrue(chu.contains("Giữ nguyên mã câu như trên"))
        // Hoi so dong nhu moi lan cham luon, va khong dua ket luan cua lan truoc cho Claude.
        assertTrue(chu.contains("\"so_dong\""))
        assertFalse(chu.contains("Lần trước chấm: sai"))
        assertTrue(NhoClaude.laLoiNho(chu))
        assertNull(NhoClaude.docKetQua(chu))
    }

    @Test
    fun cau_ngoai_khai_lay_ma_va_de_cua_may() {
        val bai = baiChuaCham(khai).copy(
            luc = System.currentTimeMillis(),
            cham = KetQuaCham(cac = cuaMay.cac + CauCham(ma = "2.28", de = "Đề của máy cho 2.28"))
        )
        val ket = NhoClaude.KetQuaDan(
            "b77",
            listOf(
                CauClaude(ma = "Câu 1", dung = true, chac = true, conViet = "5", goiY = "", de = "Tinh 2+3"),
                CauClaude(ma = "2.28", dung = true, chac = true, conViet = "A", goiY = ""),
                CauClaude(ma = "7", dung = true, chac = true, conViet = "", goiY = "", de = "Câu máy bỏ sót")
            )
        )
        val khop = NhoClaude.theoMay(ket, bai)
        assertEquals(listOf("1", "2.28", "7"), khop.cac.map { it.ma })
        assertEquals("Tính 2 + 3.", khop.cac[0].de)
        // Cau trong khai da khop qua theoKhai, khong lay de cua may.
        assertEquals("", khop.cac[1].de)
        assertEquals("Câu máy bỏ sót", khop.cac[2].de)
    }

    @Test
    fun loi_nho_cham_luon_khong_khai_thi_doi_claude_chep_de_va_xep_dang() {
        val chu = NhoClaude.loiNho(baiChuaCham(null), "Lê Hòa")
        assertTrue(chu.contains("Lê Hòa không khai trước"))
        assertTrue(chu.contains("\"de\":\"chép đề câu đó\",\"dang\":\"CAU_NHO\""))
        assertTrue(chu.contains("KHONG_TINH"))
        assertNull(NhoClaude.docKetQua(chu))
    }

    // ------------------------------------------------------ nop lai cau sai

    /** Lan nop lai cac cau sai cua mot bai, xem [KhaiBai.suaBai]. */
    private val khaiNopLai = KhaiBai(
        tenNguon = "SBT Toán 8 tập một",
        bai = "sửa bài lúc 11:18",
        mon = "Toán",
        cac = listOf(
            KhaiBai.Cau("2.19b", "sbttoan8t1:2.19b", "Tính nhanh x^3 − 9x^2 + 27x − 27 tại x = 103.", "CAU_NHO"),
            KhaiBai.Cau("câu 3", "", "She ___ (go) to school every day.", "")
        ),
        suaBai = "9210662f"
    )

    @Test
    fun loi_nho_nop_lai_chi_cham_cau_trong_danh_sach() {
        val chu = NhoClaude.loiNho(baiChuaCham(khaiNopLai), "Lê Hòa")
        assertTrue(chu.contains("Chỉ chấm đúng các câu trong danh sách trên"))
        assertTrue(chu.contains("Các câu Lê Hòa nộp lại, kèm đề:"))
        // Lan nop lai khong duoc dan Claude them muc cho cau ngoai danh sach: do la cau da
        // cham o lan truoc, con sua de len cung trang.
        assertFalse(chu.contains("thêm một mục cho câu đó"))
        // Cau ngoai sach khong co dang trong ngan hang: hoi Claude xep dang.
        assertTrue(chu.contains("2. Câu câu 3. Đề: She ___ (go) to school every day. (câu ngoài sách"))
        assertTrue(chu.contains("\"dang\":\"CAU_NHO\""))
        assertTrue(NhoClaude.laLoiNho(chu))
        assertNull(NhoClaude.docKetQua(chu))
    }

    @Test
    fun loi_nho_bai_moi_van_cho_them_cau_ngoai_danh_sach() {
        val chu = NhoClaude.loiNho(baiChuaCham(khai), "Lê Hòa")
        assertTrue(chu.contains("thêm một mục cho câu đó"))
        assertFalse(chu.contains("Chỉ chấm đúng các câu trong danh sách trên"))
    }

    @Test
    fun theo_khai_lan_nop_lai_bo_cau_ngoai_danh_sach() {
        val ket = NhoClaude.KetQuaDan(
            "b77",
            listOf(
                CauClaude(ma = "2.19a", dung = true, chac = true, conViet = "1000000", goiY = ""),
                CauClaude(ma = "2.19 b", dung = true, chac = true, conViet = "1000000", goiY = ""),
                CauClaude(ma = "Câu 3", dung = false, chac = true, conViet = "go", goiY = "Dòng 1 sai.")
            )
        )
        val khop = NhoClaude.theoKhai(ket, khaiNopLai)
        assertEquals(listOf("2.19b", "câu 3"), khop.cac.map { it.ma })
        // Bai moi thi van giu cau ngoai danh sach: do la bai con lam them.
        assertEquals(3, NhoClaude.theoKhai(ket, khaiNopLai.copy(suaBai = "")).cac.size)
    }

    @Test
    fun goi_y_viet_cu_the_khong_con_gioi_han_15_chu() {
        // Ba Huy doc goi y 15 chu nhieu luc khong hieu (28/9/2026): viet cu the, van khong
        // dua dap so, va khong dung dau ngoac kep de khoi JSON dan ve khong vo.
        listOf(baiChuaCham(khai), baiChuaCham(khaiNopLai)).forEach { bai ->
            val chu = NhoClaude.loiNho(bai, "Lê Hòa")
            assertFalse(chu.contains("15 chữ"))
            assertTrue(chu.contains("từ 2 đến 4 câu"))
            assertTrue(chu.contains("Không viết đáp số cuối cùng"))
            assertTrue(chu.contains("không dùng dấu ngoặc kép"))
        }
    }

    /**
     * Claude tra them kieu sai cho cau sai (28/9/2026): man Tien bo va "Luyện chỗ hay vấp"
     * tren tablet cong don theo nhan, ma tu khi bo may cham chi con Claude dat nhan.
     */
    @Test
    fun loi_nho_hoi_kieu_sai_va_doc_duoc_nhan_ve() {
        val chu = NhoClaude.loiNho(baiChuaCham(khai), "Lê Hòa")
        listOf("SAI_DAU", "SAI_BUOC", "NHAM_CONG_THUC", "TINH_NHAM", "THIEU", "LAC_DE", "KHAC")
            .forEach { assertTrue("thieu nhan $it", chu.contains("$it: ")) }
        assertTrue(chu.contains("\"loai_loi\""))
        assertNull(NhoClaude.docKetQua(chu))

        val ket = NhoClaude.docKetQua(
            """{"bai":"b77","ket_qua":[
               {"ma":"2.28","dung":false,"chac":true,"con_viet":"A","goi_y":"Dòng 1 sai dấu.","loai_loi":"sai_dau"},
               {"ma":"2.33a","dung":true,"chac":true,"con_viet":"40xy","goi_y":""}]}"""
        )!!
        assertEquals("SAI_DAU", ket.cac[0].loaiLoi)
        assertEquals("", ket.cac[1].loaiLoi)
    }

    @Test
    fun goi_y_khong_mo_dau_bang_chu_con() {
        // Goi y hien thang tren man cua con: Ba Huy muon "Sửa ...", khong phai "Con sửa ...".
        val chamLai = Bai(
            id = "b9", luc = 0L, trangThai = Bai.DUYET, soPhut = 0, anh = emptyList(),
            cham = KetQuaCham(cac = listOf(CauCham(ma = "2.28", de = "Đề", ketQua = "A"))),
            messageId = 0L
        )
        listOf(chamLai, baiChuaCham(khai)).forEach { bai ->
            val chu = NhoClaude.loiNho(bai, "Lê Hòa")
            assertFalse(chu.contains("gọi con là \"con\""))
            assertTrue(chu.contains("không mở đầu bằng \"Con\""))
        }
    }

    @Test
    fun doc_dong_khai_ca_kieu_cu_lan_kieu_ghi_ten() {
        // Tablet tu ngay 26/9/2026 ghi "• Lê Hòa khai:", ban cham cu van la "• Con khai:".
        fun baiKhai(dong: String) = Bai(
            id = "b9", luc = 0L, trangThai = Bai.DUYET, soPhut = 0, anh = emptyList(),
            cham = KetQuaCham(
                cac = listOf(CauCham(ma = "2.28", de = "Đề", ketQua = "A")),
                tomTat = "🤖 AI chấm: Toán\n$dong\n• ..."
            ),
            messageId = 0L
        )
        listOf("• Con khai: SGK Toán 8 — trang 47", "• Lê Hòa khai: SGK Toán 8 — trang 47").forEach {
            val chu = NhoClaude.loiNho(baiKhai(it), "Lê Hòa")
            assertTrue(it, chu.contains("Lê Hòa khai đang làm: SGK Toán 8 — trang 47."))
        }
    }

    @Test
    fun loi_nho_goi_ten_khong_goi_con() {
        // Chi con "con số" (chu so), ten truong "con_viet" va hai cho dan Claude dung goi
        // "con" - hai cho do nam trong ngoac kep.
        val chamLai = Bai(
            id = "b9", luc = 0L, trangThai = Bai.DUYET, soPhut = 0, anh = emptyList(),
            cham = KetQuaCham(cac = listOf(CauCham(ma = "2.28", de = "Đề", ketQua = "A"))),
            messageId = 0L
        )
        val chuCon = Regex("""(?<![\p{L}_"])[Cc]on(?![\p{L}_"])""")
        listOf(
            chamLai, baiChuaCham(khai), baiChuaCham(null), baiCoVo()
        ).forEach { bai ->
            val chu = NhoClaude.loiNho(bai, "Lê Hòa").replace("con số", "")
            val sot = chuCon.findAll(chu)
                .map { chu.substring(maxOf(0, it.range.first - 20), minOf(chu.length, it.range.last + 20)) }
                .toList()
            assertTrue(sot.joinToString(" | "), sot.isEmpty())
        }
    }

    /**
     * Tu 29/9/2026 bai dan do hom chua co goi tinh mot phut mot dong, khong con san phut moi
     * cau: cau dung ma thieu so dong thi tablet de ca bai cho Ba Huy (cham.canXem). Loi nho
     * cham luon bat Claude luon ghi so dong, ke ca cau la hinh ve hay bang.
     */
    @Test
    fun loi_nho_bat_ghi_so_dong_cho_moi_cau_dung() {
        listOf(baiChuaCham(khai), baiChuaCham(null), baiCoVo()).forEach { bai ->
            val chu = NhoClaude.loiNho(bai, "Lê Hòa")
            assertTrue(chu.contains("Hình vẽ, bảng, sơ đồ Lê Hòa vẽ thì tính theo số dòng vở mà nó chiếm."))
            assertTrue(chu.contains("không phải trắc nghiệm hay học thuộc thì luôn có ít nhất 1 dòng."))
            assertTrue(chu.contains("luôn ghi một số lớn hơn 0, không để 0 hay null"))
            assertNull(NhoClaude.docKetQua(chu))
        }
        // Cham lai bai da xu xong thi khong hoi so dong: tablet khong tinh lai phut.
        val chamLai = Bai(
            id = "b9", luc = 0L, trangThai = Bai.DUYET, soPhut = 0, anh = emptyList(),
            cham = KetQuaCham(cac = listOf(CauCham(ma = "2.28", de = "Đề", ketQua = "A"))),
            messageId = 0L
        )
        assertFalse(NhoClaude.loiNho(chamLai, "Lê Hòa").contains("so_dong"))
    }

    /**
     * Ban CHAM_BAI gui tablet cung nam lai trong chamClaude.goi, de lenh XU_CAU gui lai dung
     * no (29/9/2026). Thieu mot truong nao (so dong, dang) thi lan cham lai tren tablet ra
     * khac lan dau. Tu 30/9/2026 goi khong con phan vo dan do.
     */
    @Test
    fun goi_cham_bai_giu_du_truong_de_gui_lai() {
        val ket = NhoClaude.KetQuaDan(
            "b77",
            listOf(
                CauClaude(
                    ma = "2.28", dung = false, chac = true, conViet = "A", goiY = "Dòng 1 sai dấu.",
                    soDong = 1, dang = "TRAC_NGHIEM", loaiLoi = "SAI_DAU"
                ),
                CauClaude(ma = "2.33a", dung = true, chac = false, conViet = "40xy", goiY = "", soDong = 4)
            )
        )
        val goi = NhoClaude.goiChamBai(ket, baiCoVo())
        val (a, b) = (goi["cac"] as List<*>).map { it as Map<*, *> }
        assertEquals(1, a["soDong"])
        assertEquals("TRAC_NGHIEM", a["dang"])
        assertEquals("SAI_DAU", a["loaiLoi"])
        assertEquals(false, b["chac"])
        assertEquals(4, b["soDong"])
        // Khong co kieu sai thi khong ghi truong, de tablet tu xu nhu truoc.
        assertFalse(b.containsKey("loaiLoi"))
        // chamClaude.cac viet lai tu goi ra dung ban ghiChamClaude van ghi: hai cho khong lech.
        assertEquals(ket.cac.map { it.banGhi() }, XuCau.cacClaude(goi))
        // Bai cu con anh trang vo cung khong gui gi ve vo dan do.
        listOf("coAnhDanDo", "ngayDanDo", "baiDuocGiao", "lamHetDanDo").forEach {
            assertFalse(it, goi.containsKey(it))
        }
        assertEquals(setOf("cac"), goi.keys)
    }

    @Test
    fun doc_them_so_dong_de_va_dang() {
        // Claude ban cu co the con ghi "muc_do": bo qua, khong lam hong cau.
        val ket = NhoClaude.docKetQua(
            """{"bai":"b77","ket_qua":[""" +
                """{"ma":"2.33a","dung":true,"chac":true,"con_viet":"40xy","so_dong":4,"muc_do":true},""" +
                """{"ma":"3","dung":false,"con_viet":"","so_dong":"2","de":"Tính 2 + 3.","dang":"cau_nho"},""" +
                """{"ma":"4","dung":true,"so_dong":-3}]}"""
        )!!
        val (a, b, c) = ket.cac
        assertEquals(4, a.soDong)
        assertEquals("", a.de)
        assertEquals(2, b.soDong)
        assertEquals("Tính 2 + 3.", b.de)
        assertEquals("CAU_NHO", b.dang)
        // So am khong thanh so dong.
        assertEquals(0, c.soDong)
    }

    // ------------------------------------------------------------- vo dan do

    private fun baiCoVo() = baiChuaCham(khai).copy(
        anh = listOf(Anh("f2", "BAI_GIAI"), Anh("f0", "DAN_DO"), Anh("f1", "DE_BAI"))
    )

    /**
     * Tu 30/9/2026 khong con tron goi, Claude cham tung cau ma khong can vo dan do. Bai nop
     * truoc ngay do con mang anh trang vo: khong gui, va loi nho khong hoi gi ve vo.
     */
    @Test
    fun bai_cu_con_anh_vo_thi_khong_gui_va_khong_hoi_ve_vo() {
        assertEquals(listOf("f2", "f1"), NhoClaude.anhCanGui(baiCoVo()).map { it.fileId })
        val daCham = baiCoVo().copy(cham = KetQuaCham(cac = listOf(CauCham(ma = "2.28"))))
        assertEquals(listOf("f2", "f1"), NhoClaude.anhCanGui(daCham).map { it.fileId })

        val chu = NhoClaude.loiNho(baiCoVo(), "Lê Hòa")
        assertTrue(chu.contains("gồm ảnh đề bài và ảnh vở bài làm của Lê Hòa."))
        listOf("vở dặn dò", "ngay_dan_do", "lam_het_dan_do", "trong_dan_do", "bai_duoc_giao").forEach {
            assertFalse(it, chu.contains(it))
        }
        assertTrue(chu.contains("4. Cuối cùng"))
        assertTrue(NhoClaude.laLoiNho(chu))
        assertNull(NhoClaude.docKetQua(chu))
    }

    /** Claude ban cu con ghi phan vo dan do trong khoi JSON: bo qua, van doc duoc tung cau. */
    @Test
    fun ket_qua_con_phan_vo_dan_do_thi_bo_qua() {
        val chu = """
            ```json
            {"bai":"b77","ngay_dan_do":"2026-09-23","bai_duoc_giao":["Bài {2.28}", "Bài 2.33"],
             "lam_het_dan_do":true,"vo_lech":"",
             "ket_qua":[{"ma":"2.28","dung":true,"trong_dan_do":true},
                        {"ma":"2.33a","dung":true,"trong_dan_do":"có"}]}
            ```
        """.trimIndent()
        val ket = NhoClaude.docKetQua(chu)!!
        assertEquals("b77", ket.bai)
        assertEquals(listOf("2.28", "2.33a"), ket.cac.map { it.ma })
    }

    // ---------------------------------------------------- null va ma cau lech

    @Test
    fun null_doc_thanh_chuoi_rong_so_doc_thanh_chu() {
        // Kiem thu JVM dung org.json ban khac voi Android: ban Android doi null thanh chu
        // "null" (da thu tren may ao). Test nay ghim ket qua ma docKetQua phai ra.
        val ket = NhoClaude.docKetQua(
            """{"bai":null,"ket_qua":[""" +
                """{"ma":"1","dung":false,"con_viet":null,"goi_y":null,"de":null,"dang":null},""" +
                """{"ma":2,"dung":true,"con_viet":5}]}"""
        )!!
        assertEquals("", ket.bai)
        val (a, b) = ket.cac
        assertEquals("", a.conViet)
        assertEquals("", a.goiY)
        assertEquals("", a.de)
        assertEquals("", a.dang)
        assertEquals("2", b.ma)
        assertEquals("5", b.conViet)
    }

    @Test
    fun ma_lech_cach_viet_doi_ve_ma_khai_kem_de_va_dang() {
        val ket = NhoClaude.KetQuaDan(
            bai = "b77",
            cac = listOf(
                CauClaude(ma = "Câu 2.33A", dung = true, chac = true, conViet = "40xy", goiY = ""),
                CauClaude(ma = "2.28)", dung = true, chac = true, conViet = "B", goiY = ""),
                CauClaude(ma = "5", dung = true, chac = true, conViet = "5", goiY = "", de = "Tính 2 + 3.")
            )
        )
        val (a, b, c) = NhoClaude.theoKhai(ket, khai).cac
        assertEquals("2.33a", a.ma)
        assertEquals("Rút gọn (2x + 5y)^2 − (2x − 5y)^2", a.de)
        assertEquals("CAU_NHO", a.dang)
        assertEquals("2.28", b.ma)
        assertEquals("TRAC_NGHIEM", b.dang)
        // Cau ngoai sach giu nguyen, ke ca de Claude chep.
        assertEquals("5", c.ma)
        assertEquals("Tính 2 + 3.", c.de)
        // Khong co khai thi khong doi gi.
        assertEquals(ket, NhoClaude.theoKhai(ket, null))
    }

    @Test
    fun chuan_ma_chi_bo_cach_viet_khong_bo_noi_dung() {
        assertEquals("2.33a", NhoClaude.chuanMa("2.33 a:"))
        assertEquals("b3.c7", NhoClaude.chuanMa("Câu B3.C7."))
        assertEquals("1", NhoClaude.chuanMa("Bài 1)"))
        assertNotEquals(NhoClaude.chuanMa("2.33a"), NhoClaude.chuanMa("2.33b"))
    }
}
