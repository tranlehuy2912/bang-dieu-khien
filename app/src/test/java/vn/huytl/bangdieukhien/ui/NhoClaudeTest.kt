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
import vn.huytl.bangdieukhien.data.VoDaSoat

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

    // ------------------------------------------------------------ cham luon

    private val khai = KhaiBai(
        tenNguon = "SGK Toán 8 — tập một",
        bai = "trang 47",
        mon = "Toán",
        onTap = false,
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
        assertTrue(chu.contains("người chấm duy nhất"))
        assertTrue(chu.contains("Con khai đang làm: SGK Toán 8 — tập một — trang 47."))
        assertTrue(chu.contains("ảnh đề bài và ảnh vở bài làm"))
        assertTrue(chu.contains("2. Câu 2.33a. Đề: Rút gọn (2x + 5y)^2 − (2x − 5y)^2"))
        assertTrue(chu.contains("\"so_dong\""))
        assertTrue(chu.contains("\"bai\":\"b77\""))
        // Khong co ket luan cua may nao de ke ra, va khong hoi mau muc khi khong on tap.
        assertFalse(chu.contains("Máy đọc con viết"))
        assertFalse(chu.contains("muc_do"))
        // Cau trong sach da co de va dang, mau khong doi Claude chep lai.
        assertFalse(chu.contains("\"de\":\"chép đề"))

        // Loi nho nay nam san trong bo nho tam, ma dan nham thi khong duoc thanh ket qua.
        assertTrue(NhoClaude.laLoiNho(chu))
        assertNull(NhoClaude.docKetQua(chu))
    }

    @Test
    fun loi_nho_cham_luon_khong_khai_thi_doi_claude_chep_de_va_xep_dang() {
        val chu = NhoClaude.loiNho(baiChuaCham(null), "Lê Hòa")
        assertTrue(chu.contains("Con không khai trước"))
        assertTrue(chu.contains("\"de\":\"chép đề câu đó\",\"dang\":\"CAU_NHO\""))
        assertTrue(chu.contains("KHONG_TINH"))
        assertNull(NhoClaude.docKetQua(chu))
    }

    @Test
    fun loi_nho_on_tap_hoi_mau_muc() {
        val chu = NhoClaude.loiNho(baiChuaCham(khai.copy(onTap = true)), "Lê Hòa")
        assertTrue(chu.contains("bài ôn phải viết bằng mực đỏ"))
        assertTrue(chu.contains("\"muc_do\":true hoặc false"))
        assertNull(NhoClaude.docKetQua(chu))
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
    fun doc_them_so_dong_mau_muc_de_va_dang() {
        val ket = NhoClaude.docKetQua(
            """{"bai":"b77","ket_qua":[""" +
                """{"ma":"2.33a","dung":true,"chac":true,"con_viet":"40xy","so_dong":4,"muc_do":true},""" +
                """{"ma":"3","dung":false,"con_viet":"","so_dong":"2","muc_do":false,"de":"Tính 2 + 3.","dang":"cau_nho"},""" +
                """{"ma":"4","dung":true,"so_dong":-3,"muc_do":"đỏ"}]}"""
        )!!
        val (a, b, c) = ket.cac
        assertEquals(4, a.soDong)
        assertEquals(1, a.mucDo)
        assertEquals("", a.de)
        assertEquals(2, b.soDong)
        assertEquals(0, b.mucDo)
        assertEquals("Tính 2 + 3.", b.de)
        assertEquals("CAU_NHO", b.dang)
        // So am khong thanh so dong, va mau muc viet sai kieu la khong noi gi ve mau.
        assertEquals(0, c.soDong)
        assertEquals(-1, c.mucDo)
    }

    // ------------------------------------------------------------- vo dan do

    private fun baiCoVo() = baiChuaCham(khai).copy(
        anh = listOf(Anh("f2", "BAI_GIAI"), Anh("f0", "DAN_DO"), Anh("f1", "DE_BAI"))
    )

    @Test
    fun cham_luon_gui_ca_vo_dan_do_dat_len_dau_con_cham_lai_thi_bo() {
        assertEquals(listOf("f0", "f2", "f1"), NhoClaude.anhCanGui(baiCoVo()).map { it.fileId })
        val daCham = baiCoVo().copy(cham = KetQuaCham(cac = listOf(CauCham(ma = "2.28"))))
        assertEquals(listOf("f2", "f1"), NhoClaude.anhCanGui(daCham).map { it.fileId })
    }

    @Test
    fun loi_nho_co_vo_dan_do_thi_hoi_ngay_bai_co_giao_va_lam_het_chua() {
        val chu = NhoClaude.loiNho(baiCoVo(), "Lê Hòa")
        assertTrue(chu.contains("gồm trang vở dặn dò, ảnh đề bài và ảnh vở bài làm của con."))
        assertTrue(chu.contains("\"ngay_dan_do\": ngày ghi trong vở"))
        assertTrue(chu.contains("\"lam_het_dan_do\":true hoặc false,\"ket_qua\""))
        assertTrue(chu.contains("\"trong_dan_do\":true hoặc false"))
        assertTrue(chu.contains("5. Cuối cùng"))
        assertTrue(NhoClaude.laLoiNho(chu))
        assertNull(NhoClaude.docKetQua(chu))

        // Khong co trang vo thi khong hoi gi ve vo dan do.
        val khongVo = NhoClaude.loiNho(baiChuaCham(khai), "Lê Hòa")
        assertFalse(khongVo.contains("ngay_dan_do"))
        assertFalse(khongVo.contains("trong_dan_do"))
        assertTrue(khongVo.contains("4. Cuối cùng"))
    }

    @Test
    fun doc_vo_dan_do_ke_ca_khi_ten_bai_co_ngoac() {
        val chu = """
            Vở dặn dò ngày 23/9: cô giao bài 2.28 và 2.33.
            ```json
            {"bai":"b77","ngay_dan_do":"2026-09-23","bai_duoc_giao":["Bài {2.28}", "", "Bài 2.33"],
             "lam_het_dan_do":true,
             "ket_qua":[{"ma":"2.28","dung":true,"trong_dan_do":true},
                        {"ma":"2.33a","dung":true,"trong_dan_do":"có"}]}
            ```
        """.trimIndent()
        val ket = NhoClaude.docKetQua(chu)!!
        assertEquals("b77", ket.bai)
        val d = ket.danDo!!
        assertEquals("2026-09-23", d.ngay)
        assertEquals(listOf("Bài {2.28}", "Bài 2.33"), d.baiDuocGiao)
        assertTrue(d.lamHet)
        assertEquals(true, ket.cac[0].trongDanDo)
        // Viet sai kieu thi coi nhu Claude khong noi, tablet tu quyet.
        assertNull(ket.cac[1].trongDanDo)
    }

    @Test
    fun trang_vo_chup_kem_thi_dan_chi_doc_buoi_gan_ngay_nop() {
        // Vo cua Le Hoa chep lien tay, mot trang hai ba buoi. Khong dan thi Claude de gop
        // bai cua ca trang vao mot ngay.
        val chu = NhoClaude.loiNho(baiCoVo(), "Lê Hòa")
        assertTrue(chu.contains("chép liền nhiều buổi"))
        assertTrue(chu.contains("buổi có ngày gần ngày nộp bài nhất mà không sau ngày nộp"))
    }

    // ------------------------------------------------------- vo dan do con soat

    private val soat = VoDaSoat(
        ngay = "2026-09-23",
        cacBai = listOf("Toán: bài 2.28 trang 47", "Toán: bài 2.33 trang 48"),
        dongKhac = listOf("KHTN: mang sách vở đầy đủ"),
        fileId = "fv"
    )

    /** Tablet gan anh trang vo cua ban soat vao cuoi danh sach anh cua bai. */
    private fun baiCoVoSoat() = baiChuaCham(khai).copy(
        anh = listOf(Anh("f1", "DE_BAI"), Anh("f2", "BAI_GIAI"), Anh("fv", "DAN_DO")),
        voDaSoat = soat
    )

    @Test
    fun vo_con_soat_thi_chep_san_danh_sach_va_chi_hoi_lam_het_chua() {
        val chu = NhoClaude.loiNho(baiCoVoSoat(), "Lê Hòa")
        assertTrue(chu.contains("- Ngày ghi trên vở: 2026-09-23."))
        assertTrue(chu.contains("- Bài cô giao: Toán: bài 2.28 trang 47; Toán: bài 2.33 trang 48."))
        assertTrue(chu.contains("- Dặn dò khác: KHTN: mang sách vở đầy đủ."))
        assertTrue(chu.contains("không tự đọc lại danh sách từ ảnh"))
        assertTrue(chu.contains("gồm trang vở dặn dò, ảnh đề bài và ảnh vở bài làm của con."))
        // Ngay va danh sach da co san, khong hoi lai Claude.
        assertFalse(chu.contains("ngay_dan_do"))
        assertFalse(chu.contains("bai_duoc_giao"))
        assertTrue(chu.contains("\"lam_het_dan_do\":true hoặc false,\"vo_lech\":\"...\",\"ket_qua\""))
        assertTrue(chu.contains("\"trong_dan_do\":true hoặc false"))
        assertTrue(chu.contains("4. Vở dặn dò: con đã làm hết các bài cô giao ở trên chưa, và"))
        assertTrue(chu.contains("5. Cuối cùng"))
        assertTrue(NhoClaude.laLoiNho(chu))
        assertNull(NhoClaude.docKetQua(chu))
        // Anh trang vo di dau de Claude doi chieu, du tablet gan no o cuoi.
        assertEquals(listOf("fv", "f1", "f2"), NhoClaude.anhCanGui(baiCoVoSoat()).map { it.fileId })
    }

    @Test
    fun vo_con_soat_khong_co_bai_tap_thi_noi_ro_moi_cau_la_lam_them() {
        val chu = NhoClaude.loiNho(baiCoVoSoat().copy(voDaSoat = soat.copy(cacBai = emptyList())), "Lê Hòa")
        assertTrue(chu.contains("- Bài cô giao: hôm đó cô KHÔNG giao bài tập nào."))
        assertTrue(chu.contains("Danh sách rỗng thì mọi câu đều false."))
    }

    @Test
    fun vo_con_soat_chua_co_anh_thi_khong_hoi_cho_lech() {
        // Tablet chua gui duoc tin vo dan do: co danh sach ma khong co anh trang vo.
        val bai = baiCoVoSoat().copy(
            anh = listOf(Anh("f2", "BAI_GIAI")),
            voDaSoat = soat.copy(fileId = "")
        )
        val chu = NhoClaude.loiNho(bai, "Lê Hòa")
        assertTrue(chu.contains("- Bài cô giao: Toán: bài 2.28 trang 47"))
        assertTrue(chu.contains("Ảnh đính kèm là ảnh vở bài làm của con."))
        assertFalse(chu.contains("vo_lech"))
        assertTrue(chu.contains("\"lam_het_dan_do\":true hoặc false,\"ket_qua\""))
        assertTrue(chu.contains("4. Vở dặn dò: con đã làm hết các bài cô giao ở trên chưa."))
    }

    @Test
    fun doc_cho_lech_cua_vo_con_soat() {
        val ket = NhoClaude.docKetQua(
            """{"bai":"b77","lam_het_dan_do":true,"vo_lech":"Vở còn bài 2.34, danh sách thiếu.",""" +
                """"ket_qua":[{"ma":"2.28","dung":true,"trong_dan_do":true}]}"""
        )!!
        assertTrue(ket.danDo!!.lamHet)
        assertNull(ket.danDo!!.ngay)
        assertEquals("Vở còn bài 2.34, danh sách thiếu.", ket.voLech)
        assertEquals(true, ket.cac.single().trongDanDo)
        // Khong co truong nay, hay Claude ghi null, thi la khop.
        assertEquals("", NhoClaude.docKetQua("""{"ket_qua":[{"ma":"1","dung":true}]}""")!!.voLech)
        assertEquals(
            "",
            NhoClaude.docKetQua("""{"vo_lech":null,"ket_qua":[{"ma":"1","dung":true}]}""")!!.voLech
        )
    }

    @Test
    fun doc_ban_vo_con_soat_tu_firestore() {
        val v = VoDaSoat.doc(
            mapOf(
                "ngay" to " 2026-09-23 ",
                "cacBai" to listOf("Toán: bài 2.28", " ", 5),
                "dongKhac" to listOf("Mang sách vở"),
                "fileId" to "fv"
            )
        )!!
        assertEquals("2026-09-23", v.ngay)
        assertEquals(listOf("Toán: bài 2.28"), v.cacBai)
        assertEquals(listOf("Mang sách vở"), v.dongKhac)
        assertEquals("fv", v.fileId)
        // Bai khong dung ban soat, hay ban thieu ngay.
        assertNull(VoDaSoat.doc(null))
        assertNull(VoDaSoat.doc(mapOf("cacBai" to listOf("bài 2"))))
        assertEquals("", VoDaSoat.doc(mapOf("ngay" to "2026-09-23"))!!.fileId)
    }

    @Test
    fun vo_dan_do_viet_sai_kieu_thi_khong_co_goi() {
        val ket = NhoClaude.docKetQua(
            """{"ngay_dan_do":null,"lam_het_dan_do":"true","ket_qua":[{"ma":"1","dung":true}]}"""
        )!!
        assertNull(ket.danDo!!.ngay)
        assertFalse(ket.danDo!!.lamHet)
        assertTrue(ket.danDo!!.baiDuocGiao.isEmpty())
        // Khong co truong nao ve vo dan do thi khong co ban doc vo dan do.
        assertNull(NhoClaude.docKetQua("""{"ket_qua":[{"ma":"1","dung":true}]}""")!!.danDo)
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
