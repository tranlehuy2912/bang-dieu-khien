package vn.huytl.bangdieukhien.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.huytl.bangdieukhien.data.XuCau.Chon

/**
 * Lenh XU_CAU: Ba Huy tu cham nhung cau tablet chua tu cap gio (29/9/2026).
 *
 * Cho de hong nhat la ghep: tablet cham lai CA bai tu giaTri nay, nen doi nham mot muc la
 * cong gio cho mot cau Ba Huy khong he bam, hay xoa mat so dong cua mot cau khac. Ban goi
 * o day dung kieu Firestore tra ve: so la Long, map va list long nhau.
 */
class XuCauTest {

    /** Ban CHAM_BAI may nay da gui, y nhu NhoClaude.goiChamBai dung ra. */
    private val goi: Map<String, Any?> = mapOf(
        "cac" to listOf(
            mapOf(
                "ma" to "2.28", "dung" to true, "chac" to true, "conViet" to "B", "goiY" to "",
                "soDong" to 0L, "de" to "", "dang" to "TRAC_NGHIEM", "trongDanDo" to true
            ),
            mapOf(
                "ma" to "2.33a", "dung" to true, "chac" to true, "conViet" to "40xy", "goiY" to "",
                "soDong" to 0L, "de" to "Rút gọn (2x + 5y)^2 − (2x − 5y)^2", "dang" to "CAU_NHO"
            ),
            mapOf(
                "ma" to "Câu 2.34", "dung" to false, "chac" to false, "conViet" to "(8x - 3y)",
                "goiY" to "Dòng 2 thiếu một hạng tử.", "soDong" to 3L, "de" to "", "dang" to "",
                "loaiLoi" to "THIEU"
            ),
            mapOf(
                "ma" to "2.35", "dung" to true, "chac" to true, "conViet" to "7", "goiY" to "",
                "soDong" to 5L, "de" to "", "dang" to "CAU_NHO"
            )
        ),
        "coAnhDanDo" to true,
        "ngayDanDo" to "2026-09-29",
        "baiDuocGiao" to listOf("Toán: bài 2.33"),
        "lamHetDanDo" to false
    )

    /** Tablet doi "Câu 2.34" sang ma sach "2.34", va giu ma Claude o maClaude. */
    private val canXem = listOf(
        CauCanXem("2.34", "Câu 2.34", CauCanXem.CHUA_CHAC, canSoDong = true, soDong = 3),
        CauCanXem("2.33a", "2.33a", CauCanXem.THIEU_DONG, canSoDong = true, soDong = 0)
    )

    private fun cacCua(ket: XuCau.Ket): List<Map<*, *>> {
        assertTrue("ket la $ket", ket is XuCau.Ket.Duoc)
        @Suppress("UNCHECKED_CAST")
        return (ket as XuCau.Ket.Duoc).giaTri["cac"] as List<Map<*, *>>
    }

    private fun cacGoc(): List<*> = goi["cac"] as List<*>

    // ------------------------------------------------------------ doc canXem

    @Test
    fun doc_can_xem_tu_firestore() {
        val ds = CauCanXem.docDanhSach(
            listOf(
                mapOf(
                    "ma" to " 2.34 ", "maClaude" to "Câu 2.34", "lyDo" to "CHUA_CHAC",
                    "canSoDong" to true, "soDong" to 3L
                ),
                // Thieu maClaude thi lay ma, y nhu tablet luc ghi. Thieu canSoDong thi coi la can.
                mapOf("ma" to "2.33a", "lyDo" to "THIEU_DONG", "soDong" to -2L),
                mapOf("ma" to "1", "lyDo" to "CHUA_CHAC", "canSoDong" to false),
                // Muc hong thi bo, khong lam hong ca danh sach.
                mapOf("lyDo" to "CHUA_CHAC"),
                "khong phai map",
                null
            )
        )
        assertEquals(
            listOf(
                CauCanXem("2.34", "Câu 2.34", CauCanXem.CHUA_CHAC, canSoDong = true, soDong = 3),
                CauCanXem("2.33a", "2.33a", CauCanXem.THIEU_DONG, canSoDong = true, soDong = 0),
                CauCanXem("1", "1", CauCanXem.CHUA_CHAC, canSoDong = false, soDong = 0)
            ),
            ds
        )
    }

    @Test
    fun ban_cham_cu_khong_co_can_xem_thi_rong() {
        // Tablet truoc 29/9/2026 khong ghi truong nay.
        assertTrue(CauCanXem.docDanhSach(null).isEmpty())
        assertTrue(CauCanXem.docDanhSach("sai kieu").isEmpty())
        assertTrue(KetQuaCham().canXem.isEmpty())
        // Chi co maClaude thi van doc duoc, ma lay theo maClaude.
        assertEquals("2.1", CauCanXem.docDanhSach(listOf(mapOf("maClaude" to "2.1"))).single().ma)
    }

    // ------------------------------------------------------------ dung giaTri

    @Test
    fun cau_dung_co_so_dong_ba_huy_chon() {
        val cac = cacCua(XuCau.giaTri(goi, canXem, listOf(Chon.Sai, Chon.Dung(4))))
        val c = cac[1]
        assertEquals(true, c["chac"])
        assertEquals(true, c["dung"])
        assertEquals(4, c["soDong"])
        // Cac truong khac cua muc do giu nguyen, ke ca de va dang.
        assertEquals("Rút gọn (2x + 5y)^2 − (2x − 5y)^2", c["de"])
        assertEquals("CAU_NHO", c["dang"])
        assertEquals("40xy", c["conViet"])
        assertFalse(c.containsKey("chupLai"))
    }

    @Test
    fun cau_sai_thanh_chac_va_sai() {
        val cac = cacCua(XuCau.giaTri(goi, canXem, listOf(Chon.Sai, Chon.Dung(4))))
        val c = cac[2]
        assertEquals("Câu 2.34", c["ma"])
        assertEquals(true, c["chac"])
        assertEquals(false, c["dung"])
        // Goi y va kieu sai cua Claude giu nguyen cho con doc.
        assertEquals("Dòng 2 thiếu một hạng tử.", c["goiY"])
        assertEquals("THIEU", c["loaiLoi"])
        assertEquals(3L, c["soDong"])
    }

    @Test
    fun cau_chup_lai_them_chup_lai() {
        val cac = cacCua(XuCau.giaTri(goi, canXem, listOf(Chon.ChupLai, Chon.Sai)))
        assertEquals(true, cac[2]["chac"])
        assertEquals(false, cac[2]["dung"])
        assertEquals(true, cac[2]["chupLai"])
        assertEquals(false, cac[1]["dung"])
        assertFalse(cac[1].containsKey("chupLai"))
    }

    @Test
    fun cau_khong_bi_xu_va_phan_vo_dan_do_giu_nguyen() {
        val ket = XuCau.giaTri(goi, canXem, listOf(Chon.Dung(2), Chon.Dung(4))) as XuCau.Ket.Duoc
        val cac = cacCua(ket)
        assertEquals(cacGoc().size, cac.size)
        assertEquals(cacGoc()[0], cac[0])
        assertEquals(cacGoc()[3], cac[3])
        // Phan vo dan do la cua tablet tinh tron goi: khong doi mot chu.
        listOf("coAnhDanDo", "ngayDanDo", "baiDuocGiao", "lamHetDanDo").forEach {
            assertEquals(it, goi[it], ket.giaTri[it])
        }
        // Khong sua vao chinh ban goi dang giu.
        assertEquals(false, (cacGoc()[2] as Map<*, *>)["chac"])
    }

    @Test
    fun ghep_theo_ma_claude_khong_theo_ma_sach() {
        // Them mot muc mang dung ma sach "2.34": khong phai cau tablet dang hoi.
        val goiHaiMa = goi + ("cac" to cacGoc() + mapOf("ma" to "2.34", "dung" to true, "chac" to true))
        val cac = cacCua(XuCau.giaTri(goiHaiMa, canXem.take(1), listOf(Chon.Dung(3))))
        assertEquals(true, cac[2]["dung"])
        assertEquals(3, cac[2]["soDong"])
        assertEquals(mapOf("ma" to "2.34", "dung" to true, "chac" to true), cac[4])
        // Ma Claude chi khac hoa thuong la mot cau khac voi tablet: khong ghep.
        val lech = listOf(canXem[0].copy(maClaude = "câu 2.34"))
        assertTrue(XuCau.giaTri(goi, lech, listOf(Chon.Sai)) is XuCau.Ket.Hong)
        assertEquals(listOf(null), XuCau.ghep(goi, lech))
    }

    @Test
    fun hai_muc_cung_ma_thi_ghep_theo_ly_do() {
        // Claude ghi lap cau 3: muc dau thieu dong, muc sau doc chua chac.
        val lap = mapOf(
            "cac" to listOf(
                mapOf("ma" to "3", "dung" to true, "chac" to true, "soDong" to 0L),
                mapOf("ma" to "3", "dung" to false, "chac" to false, "soDong" to 2L)
            )
        )
        val hoi = listOf(
            CauCanXem("3", "3", CauCanXem.CHUA_CHAC, canSoDong = true, soDong = 2),
            CauCanXem("3", "3", CauCanXem.THIEU_DONG, canSoDong = true, soDong = 0)
        )
        assertEquals(listOf(1, 0), XuCau.ghep(lap, hoi))
        val cac = cacCua(XuCau.giaTri(lap, hoi, listOf(Chon.Sai, Chon.Dung(6))))
        assertEquals(6, cac[0]["soDong"])
        assertEquals(true, cac[0]["dung"])
        assertEquals(false, cac[1]["dung"])
        assertEquals(true, cac[1]["chac"])
    }

    @Test
    fun thieu_goi_thi_khong_gui() {
        // Bai cham bang ban app cu: chamClaude khong co goi.
        assertEquals(XuCau.Ket.Hong(XuCau.THIEU_GOI), XuCau.giaTri(null, canXem, listOf(Chon.Sai, Chon.Sai)))
        assertTrue(XuCau.giaTri(mapOf("coAnhDanDo" to false), canXem, listOf(Chon.Sai, Chon.Sai)) is XuCau.Ket.Hong)
        assertEquals(listOf(null, null), XuCau.chonTuGoi(null, canXem))
        assertFalse(XuCau.daGui(KetQuaClaude(1L, emptyList(), goi = null, xuLuc = 5L), canXem))
    }

    @Test
    fun chua_chon_het_hay_dung_thieu_so_dong_thi_khong_gui() {
        assertTrue(XuCau.giaTri(goi, canXem, listOf(Chon.Sai, null)) is XuCau.Ket.Hong)
        assertTrue(XuCau.giaTri(goi, canXem, listOf(Chon.Sai)) is XuCau.Ket.Hong)
        assertTrue(XuCau.giaTri(goi, canXem, listOf(Chon.Sai, Chon.Dung(0))) is XuCau.Ket.Hong)
        assertTrue(XuCau.giaTri(goi, emptyList(), emptyList()) is XuCau.Ket.Hong)
    }

    @Test
    fun cau_khong_tinh_theo_dong_bam_dung_thi_giu_so_dong_cu() {
        // Trac nghiem: tablet khong hoi so dong, nut Dung khong hoi, so cu giu nguyen.
        val hoi = listOf(CauCanXem("2.28", "2.28", CauCanXem.CHUA_CHAC, canSoDong = false, soDong = 0))
        val goiChuaChac = goi + ("cac" to cacGoc().mapIndexed { i, m ->
            if (i == 0) (m as Map<*, *>) + ("chac" to false) else m
        })
        val cac = cacCua(XuCau.giaTri(goiChuaChac, hoi, listOf(Chon.Dung())))
        assertEquals(true, cac[0]["chac"])
        assertEquals(true, cac[0]["dung"])
        assertEquals(0L, cac[0]["soDong"])
    }

    // ------------------------------------------------------- sau lan gui dau

    @Test
    fun da_gui_thi_doc_lai_lua_chon_tu_goi() {
        val chon = listOf(Chon.ChupLai, Chon.Dung(4))
        val moi = (XuCau.giaTri(goi, canXem, chon) as XuCau.Ket.Duoc).giaTri
        // Ban Claude nguyen goc: chua cau nao xu.
        assertEquals(listOf(null, null), XuCau.chonTuGoi(goi, canXem))
        assertEquals(chon, XuCau.chonTuGoi(moi, canXem))

        val claude = KetQuaClaude(luc = 1L, cac = emptyList(), chinh = true, goi = moi, xuLuc = 9L)
        assertTrue(XuCau.daGui(claude, canXem))
        // Chua co lan gui XU_CAU nao thi khong phai Ba Huy chon.
        assertFalse(XuCau.daGui(claude.copy(xuLuc = 0L), canXem))
        // Tablet hoi mot cau ma muc trong goi van dung dieu kien bi hoi: cau do chua xu, the
        // hien lai ba nut chu khong bao da gui.
        val hoiMoi = listOf(CauCanXem("2.28", "2.28", CauCanXem.THIEU_DONG, canSoDong = true, soDong = 0))
        assertEquals(listOf(null), XuCau.chonTuGoi(moi, hoiMoi))
        assertFalse(XuCau.daGui(claude, hoiMoi))
        assertFalse(XuCau.daGui(claude, canXem.map { it.copy(maClaude = "khong co") }))
    }

    @Test
    fun gui_lai_ra_dung_ban_da_gui() {
        val moi = (XuCau.giaTri(goi, canXem, listOf(Chon.Sai, Chon.Dung(4))) as XuCau.Ket.Duoc).giaTri
        // Bam "Gửi lại cho tablet": lua chon doc tu goi, ban gui di y het lan truoc.
        val lai = XuCau.giaTri(moi, canXem, XuCau.chonTuGoi(moi, canXem)) as XuCau.Ket.Duoc
        assertEquals(moi, lai.giaTri)
        // Doi y mot cau o lan xu sau: chi cau do doi, lua chon cau kia giu nguyen.
        val doi = cacCua(XuCau.giaTri(moi, canXem, listOf(Chon.Sai, Chon.ChupLai)))
        assertEquals(true, doi[1]["chupLai"])
        assertEquals(false, doi[2]["dung"])
        assertEquals(true, doi[2]["chac"])
    }

    @Test
    fun cham_claude_viet_lai_theo_lua_chon() {
        val moi = (XuCau.giaTri(goi, canXem, listOf(Chon.ChupLai, Chon.Sai)) as XuCau.Ket.Duoc).giaTri
        val cac = XuCau.cacClaude(moi)
        assertEquals(listOf("2.28", "2.33a", "Câu 2.34", "2.35"), cac.map { it["ma"] })
        // Cau Claude cham dung ma Ba Huy bam Sai: man ket qua tablet phai thay sai.
        assertEquals(false, cac[1]["dung"])
        assertEquals(true, cac[1]["chac"])
        // Dung kieu chamClaude.cac tu truoc: sau truong, de chi co khi khong rong.
        assertEquals(setOf("ma", "dung", "chac", "conViet", "goiY", "de"), cac[1].keys)
        assertFalse(cac[0].containsKey("de"))
        assertFalse(cac[0].containsKey("soDong"))
        assertEquals(setOf("Câu 2.34"), XuCau.maChupLai(moi))
        assertTrue(XuCau.maChupLai(goi).isEmpty())
        assertTrue(XuCau.maChupLai(null).isEmpty())
    }

    @Test
    fun muc_con_can_xem_theo_luat_tablet() {
        assertTrue(XuCau.conCanXem(mapOf("dung" to false, "chac" to false), canSoDong = false))
        assertTrue(XuCau.conCanXem(mapOf("dung" to true, "chac" to true, "soDong" to 0L), canSoDong = true))
        assertFalse(XuCau.conCanXem(mapOf("dung" to true, "chac" to true, "soDong" to 0L), canSoDong = false))
        assertFalse(XuCau.conCanXem(mapOf("dung" to true, "soDong" to 2L), canSoDong = true))
        assertFalse(XuCau.conCanXem(mapOf("dung" to false, "chac" to true), canSoDong = true))
        assertFalse(XuCau.conCanXem(mapOf("dung" to false, "chac" to true, "chupLai" to true), canSoDong = true))
        // Thieu "dung" thi tablet bo ca muc.
        assertFalse(XuCau.conCanXem(mapOf("chac" to false), canSoDong = true))
        assertNull(XuCau.ghep(goi, listOf(CauCanXem("9", "9", CauCanXem.CHUA_CHAC, true, 0))).single())
    }
}
