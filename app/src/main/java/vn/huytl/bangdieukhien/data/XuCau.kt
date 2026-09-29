package vn.huytl.bangdieukhien.data

/**
 * Dung "giaTri" cho lenh [Lenh.XU_CAU]: Ba Huy tu cham nhung cau tablet chua tu cap gio
 * (tu 29/9/2026). Xem [CauCanXem].
 *
 * CACH LAM. Tablet cham lai CA bai tu giaTri nay theo dung duong [Lenh.CHAM_BAI], nen o
 * day khong tinh phut, khong doan luat: chi lay nguyen ban may nay da gui lan truoc
 * ([KetQuaClaude.goi]) va doi dung nhung cau Ba Huy vua xu. Cau khac giu y nguyen tung
 * truong, ke ca truong ban app nay khong biet ten.
 *
 *  - Dung: chac true, dung true, soDong la so Ba Huy chon (cau khong tinh theo dong thi
 *    giu so cu).
 *  - Sai: chac true, dung false.
 *  - Chup lai: chac true, dung false, them chupLai true. Tablet khong cham cau do, khong
 *    ghi so, va nhan con chup lai.
 *
 * Tach khoi BaiActivity de kiem thu goi thang duoc: ghep nham mot cau la cong gio cho mot
 * cau Ba Huy khong he bam, ma loi do khong hien ra o dau ca.
 */
object XuCau {

    /** Ba Huy chon gi cho mot cau. */
    sealed interface Chon {
        /** [soDong] chi dung khi cau tinh theo dong, xem [CauCanXem.canSoDong]. */
        data class Dung(val soDong: Int = 0) : Chon

        data object Sai : Chon

        data object ChupLai : Chon
    }

    /** Ket qua dung giaTri: dung duoc, hay cau noi cho Ba Huy vi sao khong gui. */
    sealed interface Ket {
        data class Duoc(val giaTri: Map<String, Any?>) : Ket
        data class Hong(val viSao: String) : Ket
    }

    /**
     * Hop chon so dong o man bai (BaiActivity) hoi tu 1 toi so nay. Claude da ghi nhieu hon
     * thi hop hoi toi dung so Claude ghi, de bam Dung khong lam cau do mat phut.
     */
    const val SO_DONG_TOI_DA = 20

    const val THIEU_GOI = "Bài này chấm bằng bản app cũ nên máy chưa giữ bản Claude để gửi lại " +
        "cho tablet. Bấm Dán kết quả của Claude để dán lại, rồi chấm tay ở đây."

    /**
     * Vi tri trong goi.cac cua tung cau can xem, cung thu tu voi [canXem]. null la khong
     * thay cau do: goi khong co muc nao mang dung ma Claude ay.
     *
     * Ghep theo ma nguyen van, da bo khoang trang hai dau, vi tablet chep maClaude tu chinh
     * ban may nay gui (ChamTheoClaude ben nop-bai, truong maGoc). KHONG so kieu chuanMa: hai
     * muc "2.33a" va "2.33A" trong cung mot ban la hai cau khac nhau voi tablet.
     *
     * Hai muc cung ma (Claude ghi lap mot cau) thi moi muc chi ghep mot lan, va uu tien muc
     * dung voi ly do tablet ghi: cau chua chac ghep voi muc chac = false, cau thieu dong ghep
     * voi muc dung ma khong co so dong. Khong co muc nao nhu vay (cau da xu o lan gui truoc)
     * thi lay muc cung ma dau tien.
     */
    fun ghep(goi: Map<*, *>?, canXem: List<CauCanXem>): List<Int?> {
        val cac = goi?.get("cac") as? List<*> ?: return canXem.map { null }
        val daDung = mutableSetOf<Int>()
        return canXem.map { x ->
            val cungMa = cac.indices.filter { i ->
                i !in daDung && (cac[i] as? Map<*, *>)?.let { maCua(it) } == x.maClaude.trim()
            }
            val i = cungMa.firstOrNull { hopLyDo(cac[it] as Map<*, *>, x) } ?: cungMa.firstOrNull()
            i?.also { daDung += it }
        }
    }

    /**
     * Muc nay trong goi con bi tablet xep vao canXem khong, theo dung luat ben tablet
     * (ApprovalService.xuLyBanCham va LuatCongGio.thieuSoDong ben nop-bai): chac la false,
     * hay cau tinh theo dong ma dung khong co so dong. Cau chup lai thi tablet khong cham,
     * nen khong bao gio con can xem.
     */
    fun conCanXem(muc: Map<*, *>, canSoDong: Boolean): Boolean {
        if (muc["chupLai"] == true) return false
        // Thieu "dung" thi tablet bo ca muc, khong co gi de xem.
        val dung = muc["dung"] as? Boolean ?: return false
        if (muc["chac"] == false) return true
        return canSoDong && dung && soDongCua(muc) <= 0
    }

    /**
     * Lua chon doc lai tu goi cho tung cau can xem: cau nao da xu o lan gui truoc thi ra
     * dung lua chon do, cau chua xu (hay khong ghep duoc) thi null.
     *
     * Chi goi khi [KetQuaClaude.xuLuc] lon hon 0, tuc la da co lan gui XU_CAU. Truoc lan
     * do goi la nguyen ban Claude, va mot muc tinh co trong nhu da xu thi cung khong phai
     * Ba Huy chon.
     */
    fun chonTuGoi(goi: Map<*, *>?, canXem: List<CauCanXem>): List<Chon?> {
        val cac = goi?.get("cac") as? List<*> ?: return canXem.map { null }
        return ghep(goi, canXem).mapIndexed { k, i ->
            val muc = i?.let { cac[it] as? Map<*, *> } ?: return@mapIndexed null
            when {
                conCanXem(muc, canXem[k].canSoDong) -> null
                muc["chupLai"] == true -> Chon.ChupLai
                muc["dung"] == true -> Chon.Dung(soDongCua(muc))
                else -> Chon.Sai
            }
        }
    }

    /**
     * Da gui XU_CAU cho dung danh sach nay, tablet chua cham lai: moi cau can xem deu da
     * duoc xu trong goi. Tablet cham lai xong thi canXem doi (thuong la rong), va ham nay
     * khong con dung cho danh sach moi.
     */
    fun daGui(claude: KetQuaClaude?, canXem: List<CauCanXem>): Boolean {
        if (claude == null || claude.xuLuc <= 0L || canXem.isEmpty()) return false
        return chonTuGoi(claude.goi, canXem).none { it == null }
    }

    /**
     * giaTri cho lenh XU_CAU: [goi] voi cac cau trong [canXem] doi theo [chon] (cung thu
     * tu voi [canXem]).
     *
     * Khong gui gi khi thieu goi (bai cham bang ban app cu), khi con cau chua chon, khi co
     * cau khong ghep duoc, hay khi bam Dung ma cau tinh theo dong chua co so dong: moi canh
     * do tablet se lai de bai cho, hay cham sai cau.
     */
    fun giaTri(goi: Map<*, *>?, canXem: List<CauCanXem>, chon: List<Chon?>): Ket {
        if (goi == null) return Ket.Hong(THIEU_GOI)
        val cac = goi["cac"] as? List<*>
            ?: return Ket.Hong("Bản Claude đã lưu không có danh sách câu. Bấm Dán kết quả của Claude để dán lại.")
        if (canXem.isEmpty()) return Ket.Hong("Không có câu nào cần chấm tay.")
        if (chon.size != canXem.size || chon.any { it == null }) {
            return Ket.Hong("Còn câu chưa chọn Đúng, Sai hay Chụp lại.")
        }
        val viTri = ghep(goi, canXem)
        val khongThay = canXem.filterIndexed { k, _ -> viTri[k] == null }
        if (khongThay.isNotEmpty()) {
            return Ket.Hong(
                "Không thấy câu ${khongThay.joinToString(", ") { it.ma }} trong bản Claude đã lưu. " +
                    "Bấm Dán kết quả của Claude để dán lại."
            )
        }
        val thieuDong = canXem.filterIndexed { k, x ->
            x.canSoDong && (chon[k] as? Chon.Dung)?.let { it.soDong <= 0 } == true
        }
        if (thieuDong.isNotEmpty()) {
            return Ket.Hong("Câu ${thieuDong.joinToString(", ") { it.ma }} chưa chọn số dòng.")
        }

        val doi = canXem.indices.associate { k -> viTri[k]!! to (canXem[k] to chon[k]!!) }
        val cacMoi = cac.mapIndexed { i, muc ->
            val (x, c) = doi[i] ?: return@mapIndexed muc
            val moi = LinkedHashMap<String, Any?>()
            (muc as Map<*, *>).forEach { (k, v) -> moi[k.toString()] = v }
            when (c) {
                is Chon.Dung -> {
                    moi["chac"] = true
                    moi["dung"] = true
                    if (x.canSoDong) moi["soDong"] = c.soDong
                    moi.remove("chupLai")
                }
                Chon.Sai -> {
                    moi["chac"] = true
                    moi["dung"] = false
                    moi.remove("chupLai")
                }
                Chon.ChupLai -> {
                    moi["chac"] = true
                    moi["dung"] = false
                    moi["chupLai"] = true
                }
            }
            moi
        }
        val ra = LinkedHashMap<String, Any?>()
        goi.forEach { (k, v) -> ra[k.toString()] = v }
        ra["cac"] = cacMoi
        return Ket.Duoc(ra)
    }

    /**
     * chamClaude.cac viet lai tu mot giaTri, cung kieu [CauClaude.banGhi].
     *
     * Ghi cung luc voi lenh XU_CAU. Man ket qua ben tablet (BaiDaCham.dungCuoi ben nop-bai)
     * lay ket luan trong chamClaude khi cau do "chac", nen cau Claude cham dung ma Ba Huy bam
     * Sai se hien la dung tren man cua con, trong khi so cai ghi la sai, neu cho nay giu
     * nguyen ban Claude. Viet lai tu giaTri thi ba cho (chamClaude, cham tablet ghi lai, so
     * cai) noi cung mot dieu.
     */
    fun cacClaude(giaTri: Map<*, *>): List<Map<String, Any>> =
        (giaTri["cac"] as? List<*>).orEmpty().mapNotNull { muc ->
            val o = muc as? Map<*, *> ?: return@mapNotNull null
            val ma = maCua(o)
            val dung = o["dung"] as? Boolean
            if (ma.isEmpty() || dung == null) return@mapNotNull null
            CauClaude(
                ma = ma,
                dung = dung,
                chac = o["chac"] as? Boolean ?: true,
                conViet = o["conViet"] as? String ?: "",
                goiY = o["goiY"] as? String ?: "",
                de = o["de"] as? String ?: ""
            ).banGhi()
        }

    /**
     * Ma cac cau Ba Huy da bam Chup lai, theo goi. Rong khi chua co.
     *
     * Tablet ghi cau chup lai vao ban cham la sai (dung false), vi lenh gui chac true va dung
     * false cho cau do. Man bai dung tap nay de ghi "nhờ chụp lại" thay cho dau sai.
     */
    fun maChupLai(goi: Map<*, *>?): Set<String> =
        (goi?.get("cac") as? List<*>).orEmpty().mapNotNull { muc ->
            val o = muc as? Map<*, *> ?: return@mapNotNull null
            maCua(o).takeIf { o["chupLai"] == true && it.isNotEmpty() }
        }.toSet()

    private fun maCua(muc: Map<*, *>): String = (muc["ma"] as? String)?.trim().orEmpty()

    private fun soDongCua(muc: Map<*, *>): Int = (muc["soDong"] as? Number)?.toInt() ?: 0

    private fun hopLyDo(muc: Map<*, *>, x: CauCanXem): Boolean = when (x.lyDo) {
        CauCanXem.CHUA_CHAC -> muc["chac"] == false
        CauCanXem.THIEU_DONG -> muc["chac"] != false && muc["dung"] == true && soDongCua(muc) <= 0
        else -> conCanXem(muc, x.canSoDong)
    }
}
