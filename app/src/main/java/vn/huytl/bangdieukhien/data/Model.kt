package vn.huytl.bangdieukhien.data

import com.google.firebase.firestore.DocumentSnapshot
import java.util.Calendar

/**
 * Cac the du lieu doc ve tu Firestore.
 *
 * Doc tay bang optString/optLong chu khong dung toObject() cua Firestore: bo doc
 * tu dong an xa bang phan chieu, ma phan chieu thi ban rut gon (R8) hay cat mat
 * ten truong, va luc do app that bai lang le tren ban release trong khi ban go
 * loi chay tot. Doc tay thi thieu truong la ra gia tri mac dinh, nhin thay ngay.
 */

/** Trang thai tablet, doc tu hop/trangthai. */
data class TrangThai(
    val cong: String = Cong.KHOA,
    /**
     * Moc ket thuc phien theo gio that, 0 la khong co phien nao chay.
     *
     * Day la moc chu khong phai so phut con lai - co chu y. Dien thoai tu tru dan
     * tren may minh, nen tablet chi ghi lai khi trang thai *doi*, khong phai moi
     * giay mot luot ghi. Ca ngay het chung vai chuc luot.
     */
    val ketThucLuc: Long = 0L,
    val conLaiMs: Long = 0L,
    /** Ca phien dai bao nhieu ms, de ve thanh chay. Dung yen suot phien. */
    val tongPhienMs: Long = 0L,
    /** Viec nha Le Hoa chua lam xong, theo tablet. Con viec thi tablet dang khoa. */
    val viecNha: List<String> = emptyList(),
    /** Phut con doi bang bai tap hom nay. Gio nguoi lon cho va gio cap tu quy khong tinh. */
    val phutDaDuyet: Int = 0,
    /**
     * Con kiem them duoc bao nhieu phut hom nay, CHI DE HIEN.
     *
     * Tablet tu 29/9/2026 tinh so nay bang tong tran rieng cua cac phan (215 phut) tru
     * [phutDaDuyet]. Khong con tran chung nao chan nut Duyet hay nut cho gio: moi phan tu
     * chan bang tran cua no ben tablet. Tablet cu hon thi day la tran chung tru di.
     */
    val phutConLai: Int = 0,
    val soBaiCho: Int = 0,
    /**
     * So phut trong "Quỹ giờ chơi" cua tablet, xem [Duong.F_QUY_GIO].
     *
     * null la tablet ban cu chua gui truong nay. Luc do man Gio choi an ca hang quy: tablet
     * do cung khong hieu lenh [Lenh.CAP_QUY], bam vao chi duoc cau "Không hiểu lệnh".
     */
    val quyGio: Int? = null,
    val cheDoBaBat: Boolean = false,
    val cheDoBaHetLuc: Long = 0L,
    val quyenTroGiup: Boolean = true,
    val quyenQuanTri: Boolean = true,
    val quyenNoi: Boolean = true,
    val coPin: Boolean = true,
    val pinMay: Int = -1,
    val dangSac: Boolean = false,
    /** Ten app dang tren man hinh tablet. Rong la khong mo app nao. */
    val appTruocMat: String = "",
    /** Luc mo app do, theo gio tablet. 0 la khong co. */
    val appTruocMatTu: Long = 0L,
    /**
     * Man hinh tablet dang sang. null la khong biet: tablet ban cu chua gui truong
     * nay, hoac dich vu canh app ben do dang khong chay.
     */
    val manHinhSang: Boolean? = null,
    val banApp: String = "",
    val capNhatLuc: Long = 0L,
    /** Cau tablet noi lai sau khi lam lenh gan nhat, rong la chua co gi. */
    val traLoi: String = "",
    val traLoiLuc: Long = 0L,
    /** Cau tra loi do do lenh cua ai: [Nguoi.BA_HUY] hay [Nguoi.BA_NOI]. */
    val traLoiCho: String = Nguoi.BA_HUY
) {

    /**
     * So ms con lai tinh theo dong ho ngay bay gio.
     *
     * Dang choi thi lay moc ket thuc tru di hien tai - so nay tu chay. Tam dung
     * hay da duyet ma chua bam thi so phut dung yen, lay thang [conLaiMs].
     */
    fun conLaiBayGio(bayGio: Long = System.currentTimeMillis()): Long = when (cong) {
        Cong.DANG_CHOI -> (ketThucLuc - bayGio).coerceAtLeast(0L)
        else -> conLaiMs
    }

    fun coCanhBao(): Boolean = !quyenTroGiup || !quyenQuanTri || !quyenNoi || !coPin

    companion object {
        /*
         * KHONG CON NGUONG "SO LIEU CU" O DAY NUA.
         *
         * Ban truoc co CU_SAU_MS = 90 phut: tablet im lang lau hon the thi man hinh
         * bao "chua bao ve lau roi". Con so do phai di theo nhip tim ben tablet, ma
         * nhip tim do chay bang Handler - dong ho dung lai khi CPU ngu - nen mot
         * tablet nam im tren ban ca buoi toi van bi goi ten oan. Mot canh bao keu
         * sai nhieu lan thi lan keu dung cung khong ai tin.
         *
         * Bay gio man Bang hoi thang: mo app la go mot lenh PING, tablet dap bang
         * mot ban trang thai moi trong duoi mot giay. Tra loi thi so lieu dung cua
         * giay nay; khong tra loi sau [BangFragment.CHO_PING_MS] thi noi thang la
         * tablet khong tra loi, kem gio bao ve lan cuoi. Khong con con so nao phai
         * khop giua hai app.
         */

        fun doc(d: DocumentSnapshot?): TrangThai? {
            if (d == null || !d.exists()) return null
            val cheDoBa = d.get(Duong.F_CHE_DO_BA) as? Map<*, *>
            val quyen = d.get(Duong.F_QUYEN) as? Map<*, *>
            return TrangThai(
                cong = d.getString(Duong.F_CONG) ?: Cong.KHOA,
                ketThucLuc = d.getLong(Duong.F_KET_THUC_LUC) ?: 0L,
                conLaiMs = d.getLong(Duong.F_CON_LAI_MS) ?: 0L,
                tongPhienMs = d.getLong(Duong.F_TONG_PHIEN_MS) ?: 0L,
                viecNha = (d.get(Duong.F_VIEC_NHA) as? List<*>).orEmpty()
                    .mapNotNull { it as? String },
                phutDaDuyet = (d.getLong(Duong.F_PHUT_DA_DUYET) ?: 0L).toInt(),
                phutConLai = (d.getLong(Duong.F_PHUT_CON_LAI) ?: 0L).toInt(),
                soBaiCho = (d.getLong(Duong.F_SO_BAI_CHO) ?: 0L).toInt(),
                quyGio = d.getLong(Duong.F_QUY_GIO)?.toInt()?.coerceAtLeast(0),
                cheDoBaBat = cheDoBa?.get("bat") as? Boolean ?: false,
                cheDoBaHetLuc = (cheDoBa?.get("hetLuc") as? Number)?.toLong() ?: 0L,
                quyenTroGiup = quyen?.get("trogiup") as? Boolean ?: true,
                quyenQuanTri = quyen?.get("quantri") as? Boolean ?: true,
                quyenNoi = quyen?.get("noi") as? Boolean ?: true,
                coPin = quyen?.get("pin") as? Boolean ?: true,
                pinMay = (d.getLong(Duong.F_PIN_MAY) ?: -1L).toInt(),
                dangSac = d.getBoolean(Duong.F_DANG_SAC) ?: false,
                appTruocMat = d.getString(Duong.F_APP_TRUOC_MAT).orEmpty(),
                appTruocMatTu = d.getLong(Duong.F_APP_TRUOC_MAT_TU) ?: 0L,
                manHinhSang = d.getBoolean(Duong.F_MAN_HINH_SANG),
                banApp = d.getString(Duong.F_BAN_APP).orEmpty(),
                capNhatLuc = d.getLong(Duong.F_CAP_NHAT_LUC) ?: 0L,
                traLoi = (d.get(Duong.F_TRA_LOI) as? Map<*, *>)?.get("chu") as? String ?: "",
                traLoiLuc = ((d.get(Duong.F_TRA_LOI) as? Map<*, *>)?.get("luc") as? Number)
                    ?.toLong() ?: 0L,
                traLoiCho = (d.get(Duong.F_TRA_LOI) as? Map<*, *>)
                    ?.get(Duong.F_AI) as? String ?: Nguoi.BA_HUY
            )
        }
    }
}

/**
 * Mot tam anh trong lan nop bai.
 *
 * [khau] la ten hang cua enum CaptureStage ben tablet: DAN_DO, DE_BAI, BAI_GIAI.
 * Ban dau cho nay so voi "DANDO" va "DEBAI", thieu dau gach duoi, nen anh de bai va
 * anh vo dan do deu bi ghi nhan "Bài giải". Van nhan kieu viet cu cho chac.
 */
data class Anh(val fileId: String, val khau: String) {
    val laDanDo: Boolean get() = khau == "DAN_DO" || khau == "DANDO"
    val laDeBai: Boolean get() = khau == "DE_BAI" || khau == "DEBAI"

    fun tenKhau(): String = when {
        laDanDo -> "Vở dặn dò"
        laDeBai -> "Đề bài"
        else -> "Bài giải"
    }
}

/** Mot cau AI da cham. */
data class CauCham(
    val ma: String = "",
    val de: String = "",
    val ketQua: String = "",
    val dung: Boolean = false,
    val docRo: Boolean = true,
    val soDong: Int = 0,
    val nhanXet: String = ""
)

/** Ket qua AI cham mot lan nop. */
data class KetQuaCham(
    val mon: String = "",
    val cac: List<CauCham> = emptyList(),
    val tomTat: String = "",
    val phutDeNghi: Int = 0,
    val lamHetDanDo: Boolean = false,
    /**
     * Cau tablet chua tu cap gio, cho Ba Huy tu cham (tu 29/9/2026). Xem [CauCanXem].
     *
     * Rong la khong co cau nao phai xem, ke ca voi ban cham tablet ghi truoc ngay do
     * (chua co truong nay).
     */
    val canXem: List<CauCanXem> = emptyList()
) {
    fun soDung(): Int = cac.count { it.dung }
    fun coCauKhongRo(): Boolean = cac.any { !it.docRo }
}

/**
 * Mot cau tablet khong tu cap gio duoc, cho Ba Huy tu cham. Nam trong cham.canXem.
 *
 * VI SAO CO (29/9/2026). Tablet bo san phut moi cau: bai dan do hom chua co goi tinh mot
 * phut mot dong, nen cau Claude cham dung ma khong ghi so dong ra 0 phut, va tablet de ca
 * bai cho Ba Huy thay vi doan. Cau Claude doc chua chac chu con viet cung vay, tu truoc.
 * Tablet tu tinh danh sach nay (ApprovalService.cauCanXem ben nop-bai), de ben nay khong
 * phai chep lai luat tinh phut.
 *
 * Ba Huy bam Dung, Sai hay Chup lai cho tung cau, roi gui lenh [Lenh.XU_CAU], xem
 * [XuCau].
 */
data class CauCanXem(
    /** Ma cau trong ban cham cua tablet, khop voi [CauCham.ma]: ma sach khi khop duoc. */
    val ma: String,
    /**
     * Ma cau nguyen van trong ban Claude dien thoai da gui (goi.cac[].ma, xem
     * [KetQuaClaude.goi]). Khac [ma] khi tablet da doi sang ma sach, vi du Claude ghi
     * "Câu 2.33A" ma sach ghi "2.33a".
     */
    val maClaude: String,
    /** [CHUA_CHAC] hay [THIEU_DONG]. Tablet ban moi hon co the them ly do khac. */
    val lyDo: String,
    /**
     * Cau nay tinh phut theo so dong: khong phai trac nghiem, khong phai hoc thuoc. Bam Dung
     * o cau nay thi phai hoi so dong, khong thi tablet lai de ca bai cho.
     */
    val canSoDong: Boolean,
    /** So dong Claude ghi. 0 la khong ghi. */
    val soDong: Int
) {
    companion object {
        /** Claude doc chua chac chu con viet (chac = false). */
        const val CHUA_CHAC = "CHUA_CHAC"

        /** Claude cham dung ma khong ghi so dong. */
        const val THIEU_DONG = "THIEU_DONG"

        /**
         * Doc truong canXem cua ban cham. Tach khoi [Bai.doc] de kiem thu goi thang duoc.
         *
         * Muc hong thi bo, khong lam hong ca danh sach. Thieu maClaude thi lay [ma], y nhu
         * tablet luc ghi. Thieu canSoDong thi coi la can: hoi thua mot lan so dong thi Ba
         * Huy chi ton mot cham, con thieu so dong thi tablet tra 0 phut va bai lai nam cho.
         */
        fun docDanhSach(v: Any?): List<CauCanXem> = (v as? List<*>).orEmpty().mapNotNull { c ->
            val o = c as? Map<*, *> ?: return@mapNotNull null
            val maClaudeGhi = (o["maClaude"] as? String)?.trim().orEmpty()
            val ma = (o["ma"] as? String)?.trim().orEmpty().ifEmpty { maClaudeGhi }
            if (ma.isEmpty()) return@mapNotNull null
            CauCanXem(
                ma = ma,
                maClaude = maClaudeGhi.ifEmpty { ma },
                lyDo = (o["lyDo"] as? String)?.trim().orEmpty(),
                canSoDong = o["canSoDong"] as? Boolean ?: true,
                soDong = (o["soDong"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0
            )
        }
    }
}

/** Ket luan cua Claude cho mot cau, Ba Huy dan tu app Claude vao. */
data class CauClaude(
    val ma: String,
    val dung: Boolean,
    /** false la Claude khong doc chac chu con viet. Luc do khong lat ket luan cua may. */
    val chac: Boolean,
    val conViet: String,
    /** Goi y cho con tu sua, chi co o cau sai. Khong chua dap an. */
    val goiY: String,
    /** So dong con viet cho cau nay. Tablet tinh phut theo so dong, xem LuatCongGio. */
    val soDong: Int = 0,
    /** De Claude chep tu anh, chi can khi con khong khai theo sach. */
    val de: String = "",
    /** Dang bai Claude xep, ten hang cua DangBai ben tablet. Chi can o cau ngoai sach. */
    val dang: String = "",
    /**
     * Kieu sai, mot trong bay nhan cua LoaiLoi ben tablet. Chi co o cau sai. Tablet cong
     * don nhan nay cho man Tien bo va "Luyện chỗ hay vấp" (tu 28/9/2026 Claude la noi
     * duy nhat dat nhan).
     */
    val loaiLoi: String = "",
    /**
     * Cau nay thuoc bai co giao trong vo dan do. null la Claude khong noi, luc do tablet
     * tu quyet theo luat cua may cham.
     */
    val trongDanDo: Boolean? = null
) {
    /**
     * Mot dong trong chamClaude.cac, dung kieu tablet doc o BaiDaCham ben nop-bai.
     *
     * Chi sau truong nay, tu truoc 29/9/2026. So dong, dang bai, kieu sai va trongDanDo
     * khong nam o day ma nam nguyen ban trong chamClaude.goi, xem [KetQuaClaude.goi].
     */
    fun banGhi(): Map<String, Any> = buildMap<String, Any> {
        put("ma", ma)
        put("dung", dung)
        put("chac", chac)
        put("conViet", conViet)
        put("goiY", goiY)
        if (de.isNotBlank()) put("de", de)
    }
}

/**
 * Cac cau con khai truoc khi chup, kem de tung cau. Tablet ghi luc con nop.
 *
 * Co cai nay thi loi nho gui Claude co de bai ngay ca khi tablet khong tu cham.
 * Xem [Duong.F_KHAI].
 */
data class KhaiBai(
    val tenNguon: String,
    val bai: String,
    val mon: String,
    val onTap: Boolean,
    val cac: List<Cau>,
    /**
     * Lan nop nay la nop lai cac cau sai cua bai nao (ma bai). Rong la bai moi.
     *
     * Con bam "Nộp lại N câu sai" tren the mot bai o man ket qua cua tablet (tu 28/9/2026).
     * Loi nho Claude luc do chi cho cham cau trong danh sach, va cau ngoai sach trong
     * danh sach co cauId rong - xem [NhoClaude.theoKhai].
     */
    val suaBai: String = ""
) {
    data class Cau(val ma: String, val cauId: String, val de: String, val dang: String)
}

/**
 * Vo dan do cua ngay, tablet chep vao bai luc nop va vao hop/dando. Xem [Duong.F_DAN_DO].
 *
 * Co cai nay thi Claude khong phai tu doc trang vo: ngay va cac bai co giao da co san,
 * may doc va con da soat lai. [fileId] la anh trang vo de Claude doi chieu, rong la
 * tablet chua gui duoc anh.
 *
 * [chuaDoc] la ngoai le: chi co anh, may doc khong duoc. Luc do [cacBai] rong nhung chua
 * biet co giao gi, nen khong duoc dung nhu mot danh sach - xem [daDoc].
 */
data class VoDaSoat(
    /** Ngay ghi tren vo, dang yyyy-MM-dd. Ban chi co anh thi la ngay chup. */
    val ngay: String,
    /** Cac dong con tich la bai tap. Rong la hom do co khong giao bai tap nao. */
    val cacBai: List<String>,
    val dongKhac: List<String> = emptyList(),
    val fileId: String = "",
    val chuaDoc: Boolean = false,
    /** Ai doc ra danh sach: CON, CLAUDE hay LUCCHAM, y het VoDanDo ben tablet. */
    val nguon: String = NGUON_CON,
    /** Luc chup tam anh trang vo, de lenh DOCVO ghi vao dung tam do. */
    val chupLuc: Long = 0L,
    /** Luc tablet luu ban nay. Chi co o hop/dando. */
    val luc: Long = 0L
) {
    /** Ban nay dung duoc nhu mot danh sach bai: da co chu, khong phai chi co anh. */
    val daDoc: VoDaSoat? get() = takeUnless { chuaDoc }

    companion object {
        /** null la bai khong dung ban soat nao, hay ban ghi thieu ngay. */
        fun doc(m: Map<*, *>?): VoDaSoat? {
            val ngay = (m?.get("ngay") as? String)?.trim().orEmpty()
            if (ngay.isEmpty()) return null
            fun ds(ten: String) = (m?.get(ten) as? List<*>).orEmpty()
                .mapNotNull { (it as? String)?.trim()?.takeIf { t -> t.isNotEmpty() } }
            return VoDaSoat(
                ngay = ngay,
                cacBai = ds("cacBai"),
                dongKhac = ds("dongKhac"),
                fileId = (m?.get(Duong.F_FILE_ID) as? String)?.trim().orEmpty(),
                chuaDoc = m?.get("chuaDoc") as? Boolean ?: false,
                nguon = (m?.get("nguon") as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: NGUON_CON,
                chupLuc = (m?.get("chupLuc") as? Number)?.toLong() ?: 0L,
                luc = (m?.get(Duong.F_LUC) as? Number)?.toLong() ?: 0L
            )
        }

        const val NGUON_CON = "CON"
        const val NGUON_CLAUDE = "CLAUDE"
        const val NGUON_LUC_CHAM = "LUCCHAM"
    }
}

/** Ban Claude cham, nam o [Duong.F_CHAM_CLAUDE] canh ban cham cua may. */
data class KetQuaClaude(
    val luc: Long,
    val cac: List<CauClaude>,
    /** true la Claude cham luon vi may chua cham, false la Claude cham lai ban cua may. */
    val chinh: Boolean = false,
    /**
     * Nguyen "giaTri" may nay gui tablet lan gan nhat, lenh [Lenh.CHAM_BAI] hay
     * [Lenh.XU_CAU] (tu 29/9/2026). null la bai cham bang ban app cu, hay ban Claude
     * nay khong di kem lenh nao (bai da roi hang cho, duong cham lai).
     *
     * VI SAO GIU NGUYEN BAN. [cac] chi co sau truong, thieu so dong, dang bai, kieu sai,
     * trongDanDo va phan vo dan do. Lenh XU_CAU phai gui lai dung ban da gui, chi doi cau
     * Ba Huy vua xu: tablet cham lai CA bai tu ban do. Dung lai ban tu [cac] thi moi cau
     * mat so dong va ra 0 phut. Doc bang map tho de khong mat truong nao ma ban app nay
     * chua biet ten.
     */
    val goi: Map<*, *>? = null,
    /** Luc Ba Huy gui lenh XU_CAU gan nhat, 0 la chua gui lan nao. */
    val xuLuc: Long = 0L
) {
    fun cua(ma: String): CauClaude? = cac.firstOrNull { it.ma == ma.trim() }
}

/** Mot lan con nop bai. */
data class Bai(
    val id: String,
    val luc: Long,
    val trangThai: String,
    val soPhut: Int,
    val anh: List<Anh>,
    val cham: KetQuaCham?,
    val messageId: Long,
    /** Vo dan do con soat ma lan nop nay dung thay cho trang vo. Xem [VoDaSoat]. */
    val voDaSoat: VoDaSoat? = null,
    val claude: KetQuaClaude? = null,
    val khai: KhaiBai? = null,
    /** Ba Huy da bam Xoa o tab Bai. Xem [F_AN]. */
    val an: Boolean = false,
    /** Luc tablet se cong [soPhut] phut, voi bai cham xong trong gio ngu. 0 la khong giu. */
    val congLuc: Long = 0L
) {
    /** Con nam trong hang cho cua tablet, tuc la bam Duyet hay Khong duyet con co tac dung. */
    val dangCho: Boolean get() = trangThai == CHO && !quaNgay()

    /** Da xong viec, khong con gi de bam. Chi bai xong moi co nut Xoa. */
    val xong: Boolean get() = !dangCho

    /**
     * Tablet cham xong trong gio ngu va dang giu [soPhut] phut, toi [congLuc] moi cong.
     *
     * Trang thai van la DUYET, vi bai da roi hang cho. Tablet cong xong thi xoa [congLuc].
     */
    val choCong: Boolean get() = trangThai == DUYET && congLuc > 0L

    /**
     * Van ghi CHO nhung nop tu hom truoc.
     *
     * Sang ngay moi tablet bo moi bai chua duyet khoi hang cho, de con khong choi bang
     * bai tap hom qua (GateStore.donDepBaiCho ben nop-bai, chia ngay theo lich y nhu
     * [cungNgay]). Tablet khong bao viec do len Firestore, nen document van ghi CHO. Truoc
     * day bai 20:29 ngay 24/9/2026 hien hai nut duyet ca may ngay sau: bam Khong duyet thi
     * tablet dap "Khong co bai nao dang cho" va bai van nam nguyen do.
     *
     * luc bang 0 la document thieu gio nop, khong biet la ngay nao nen coi nhu con cho.
     */
    fun quaNgay(bayGio: Long = System.currentTimeMillis()): Boolean =
        trangThai == CHO && luc > 0L && !cungNgay(luc, bayGio)

    companion object {
        const val CHO = "CHO"
        const val DUYET = "DUYET"
        const val TU_CHOI = "TUCHOI"

        /** Con tu huy de chup lai. Tablet ghi tu luc HomeActivity.huyYeuCau bao sang day. */
        const val HUY = "HUY"

        /**
         * Co an bai khoi danh sach, Ba Huy bam Xoa thi dat. Xem [Kho.anBai].
         *
         * Khong nam trong [Duong] vi chi app nay doc va ghi, tablet khong dung toi.
         */
        const val F_AN = "anKhoiDanhSach"

        /** Hai moc co cung mot ngay theo lich cua may nay khong. */
        fun cungNgay(a: Long, b: Long): Boolean {
            val ca = Calendar.getInstance().apply { timeInMillis = a }
            val cb = Calendar.getInstance().apply { timeInMillis = b }
            return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
                ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
        }

        fun doc(d: DocumentSnapshot): Bai {
            val anh = (d.get(Duong.F_ANH) as? List<*>).orEmpty().mapNotNull { m ->
                val o = m as? Map<*, *> ?: return@mapNotNull null
                val id = o[Duong.F_FILE_ID] as? String ?: return@mapNotNull null
                Anh(id, o[Duong.F_KHAU] as? String ?: "BAIGIAI")
            }
            return Bai(
                id = d.id,
                luc = d.getLong(Duong.F_LUC) ?: 0L,
                trangThai = d.getString(Duong.F_TRANG_THAI) ?: CHO,
                soPhut = (d.getLong(Duong.F_SO_PHUT) ?: 0L).toInt(),
                anh = anh,
                cham = docCham(d.get(Duong.F_CHAM) as? Map<*, *>),
                messageId = d.getLong(Duong.F_MESSAGE_ID) ?: 0L,
                voDaSoat = VoDaSoat.doc(d.get(Duong.F_DAN_DO) as? Map<*, *>),
                claude = docClaude(d.get(Duong.F_CHAM_CLAUDE) as? Map<*, *>),
                khai = docKhai(d.get(Duong.F_KHAI) as? Map<*, *>),
                an = d.getBoolean(F_AN) ?: false,
                congLuc = d.getLong(Duong.F_CONG_LUC) ?: 0L
            )
        }

        private fun docKhai(m: Map<*, *>?): KhaiBai? {
            if (m == null) return null
            val cac = (m["cac"] as? List<*>).orEmpty().mapNotNull { c ->
                val o = c as? Map<*, *> ?: return@mapNotNull null
                val ma = (o["ma"] as? String)?.trim().orEmpty()
                if (ma.isEmpty()) return@mapNotNull null
                KhaiBai.Cau(
                    ma = ma,
                    cauId = o["cauId"] as? String ?: "",
                    de = o["de"] as? String ?: "",
                    dang = o["dang"] as? String ?: ""
                )
            }
            if (cac.isEmpty()) return null
            return KhaiBai(
                tenNguon = m["tenNguon"] as? String ?: "",
                bai = m["bai"] as? String ?: "",
                mon = m["mon"] as? String ?: "",
                onTap = m["onTap"] as? Boolean ?: false,
                cac = cac,
                suaBai = m["suaBai"] as? String ?: ""
            )
        }

        private fun docClaude(m: Map<*, *>?): KetQuaClaude? {
            if (m == null) return null
            val cac = (m["cac"] as? List<*>).orEmpty().mapNotNull { c ->
                val o = c as? Map<*, *> ?: return@mapNotNull null
                val ma = (o["ma"] as? String)?.trim().orEmpty()
                if (ma.isEmpty()) return@mapNotNull null
                CauClaude(
                    ma = ma,
                    dung = o["dung"] as? Boolean ?: false,
                    chac = o["chac"] as? Boolean ?: true,
                    conViet = o["conViet"] as? String ?: "",
                    goiY = o["goiY"] as? String ?: "",
                    de = o["de"] as? String ?: ""
                )
            }
            if (cac.isEmpty()) return null
            return KetQuaClaude(
                luc = (m["luc"] as? Number)?.toLong() ?: 0L,
                cac = cac,
                chinh = m["chinh"] as? Boolean ?: false,
                goi = m["goi"] as? Map<*, *>,
                xuLuc = (m["xuLuc"] as? Number)?.toLong() ?: 0L
            )
        }

        private fun docCham(m: Map<*, *>?): KetQuaCham? {
            if (m == null) return null
            val cac = (m["cac"] as? List<*>).orEmpty().mapNotNull { c ->
                val o = c as? Map<*, *> ?: return@mapNotNull null
                CauCham(
                    ma = o["ma"] as? String ?: "",
                    de = o["de"] as? String ?: "",
                    ketQua = o["ketQua"] as? String ?: "",
                    dung = o["dung"] as? Boolean ?: false,
                    docRo = o["docRo"] as? Boolean ?: true,
                    soDong = (o["soDong"] as? Number)?.toInt() ?: 0,
                    nhanXet = o["nhanXet"] as? String ?: ""
                )
            }
            return KetQuaCham(
                mon = m["mon"] as? String ?: "",
                cac = cac,
                tomTat = m["tomTat"] as? String ?: "",
                phutDeNghi = (m["phutDeNghi"] as? Number)?.toInt() ?: 0,
                lamHetDanDo = m["lamHetDanDo"] as? Boolean ?: false,
                canXem = CauCanXem.docDanhSach(m["canXem"])
            )
        }
    }
}

/** Cau hinh dang chay tren tablet, doc tu hop/caidat. Dien thoai khong ghi vao day. */
data class CaiDat(
    val gioNgu: Int = 22 * 60,
    val gioDay: Int = 6 * 60,
    /**
     * Tran chung moi ngay, KHONG CON HIEN tu 29/9/2026.
     *
     * Ngay do tablet bo tran chung (truoc la 135 phut): moi phan co tran rieng, tong 215
     * phut, va lenh CAIDAT tranPhutMoiNgay chi duoc tablet tra loi la khong con dung. Tablet
     * van ghi truong nay vao hop/caidat, nen van doc de ban sao khong lech, nhung man Cai
     * dat bo muc "Tối đa mỗi ngày".
     */
    val tranPhutMoiNgay: Int = 120,
    val khoaCaiDat: Boolean = true,
    val appChoPhep: List<String> = emptyList(),
    /** App dung moi luc, ke ca gio ngu va gio di hoc. Tablet ban cu khong ghi truong nay. */
    val appMoiLuc: List<String> = emptyList(),
    val appChan: List<String> = emptyList(),
    /**
     * App duoc phat tieng khi het gio choi, tru gio ngu va gio di hoc. Tablet ghi truong
     * nay tu 27/9/2026, ban cu hon thi khong co.
     */
    val appNhac: List<String> = emptyList(),
    /** App mat mang khi tablet khoa. Tablet ban cu khong ghi truong nay. */
    val appCatMang: List<String> = emptyList(),
    val appAi: List<String> = emptyList(),
    /** Gio rieng tung app: goi -> so phut moi ngay. App khong co trong nay la khong dat. */
    val gioiHanApp: Map<String, Int> = emptyMap()
) {
    companion object {
        fun doc(d: DocumentSnapshot?): CaiDat? {
            if (d == null || !d.exists()) return null
            @Suppress("UNCHECKED_CAST")
            return CaiDat(
                gioNgu = (d.getLong("gioNgu") ?: (22 * 60L)).toInt(),
                gioDay = (d.getLong("gioDay") ?: (6 * 60L)).toInt(),
                tranPhutMoiNgay = (d.getLong("tranPhutMoiNgay") ?: 120L).toInt(),
                khoaCaiDat = d.getBoolean("khoaCaiDat") ?: true,
                appChoPhep = (d.get("appChoPhep") as? List<String>).orEmpty(),
                appMoiLuc = (d.get("appMoiLuc") as? List<String>).orEmpty(),
                appChan = (d.get("appChan") as? List<String>).orEmpty(),
                appNhac = (d.get("appNhac") as? List<String>).orEmpty(),
                appCatMang = (d.get("appCatMang") as? List<String>).orEmpty(),
                appAi = (d.get("appAi") as? List<String>).orEmpty(),
                gioiHanApp = (d.get("gioiHanApp") as? Map<String, Number>)
                    .orEmpty().mapValues { it.value.toInt() }
            )
        }
    }
}

/** Mot app dang cai tren tablet, de chon tu xa ma khong phai go ten goi. */
data class AppTrenMay(val goi: String, val ten: String)

/**
 * Mot lenh may nay da go ma tablet chua lay. Xem [Kho.ngheLenhCho].
 *
 * Tablet lam xong lenh nao la xoa document do, ke ca lenh qua cu no bo qua. Nen lenh
 * con nam trong hang tuc la tablet chua nhan: dang mat mang, dang tat may, hay chi la
 * mot hai giay chua kip.
 *
 * VI SAO PHAI HIEN RA. Firestore nhan lenh xong la nut tren man Bang sang lai, ma luc
 * do tablet chua lam gi. Tablet mat mang thi lenh nam cho toi nua tieng, roi tablet
 * co mang lai la van lam. Ba Huy bam "15'", khong thay gi doi, bam them lan nua: hai
 * lenh cho gio cong don, Le Hoa duoc 30 phut.
 */
data class LenhCho(
    val id: String,
    val kieu: String,
    val phut: Int?,
    /** Luc go, theo dong ho may nay. Tablet cung lay truong nay de bo lenh qua cu. */
    val tao: Long,
    /** Chua len duoc may chu: chinh dien thoai dang mat mang. */
    val chuaLenMang: Boolean
) {
    /** Qua [Duong.QUA_CU_MS] thi tablet co lay duoc cung bo qua. */
    fun quaHan(bayGio: Long = System.currentTimeMillis()): Boolean =
        tao > 0L && bayGio - tao > Duong.QUA_CU_MS

    /**
     * Co dang hien tren man Bang khong.
     *
     * Tablet co mang thi lenh chi nam trong hang chung mot giay. Hien ngay tu luc go
     * thi dong nay chop len roi tat sau moi lan bam, ma luc do dong "Đang gửi…" da noi
     * dung viec do. Nen chi hien lenh da nam do qua [CHO_HIEN_MS], ke ca lenh chua len
     * duoc may chu: co mang thi co do cung chi ton tai mot thoang.
     */
    fun dangHien(bayGio: Long = System.currentTimeMillis()): Boolean =
        tao <= 0L || bayGio - tao >= CHO_HIEN_MS

    companion object {
        const val CHO_HIEN_MS = 3_000L

        /**
         * Doc mot document trong lenh/. Tra null voi lenh khong can hien: PING la may
         * nay tu hoi, khong phai viec Ba Huy bam; lenh cua nguoi khac thi khong phai
         * lenh cua may nay de rut lai.
         */
        fun doc(d: DocumentSnapshot): LenhCho? {
            val kieu = d.getString(Duong.F_KIEU) ?: return null
            if (kieu == Lenh.PING) return null
            if ((d.getString(Duong.F_AI) ?: Nguoi.BA_HUY) != Nguoi.BA_HUY) return null
            return LenhCho(
                id = d.id,
                kieu = kieu,
                phut = d.getLong(Duong.F_PHUT)?.toInt(),
                // Kho.guiLenh ghi ten truong nay thang, khong qua Duong.
                tao = d.getLong("tao") ?: 0L,
                chuaLenMang = d.metadata.hasPendingWrites()
            )
        }
    }
}
