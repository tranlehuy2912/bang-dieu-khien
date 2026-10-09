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
    /**
     * So ms con da choi that hom nay, tinh toi dau doan dang chay, xem [Duong.F_DA_CHOI_MS].
     *
     * null la tablet ban cu chua gui truong nay: luc do khoi so lieu trong ngay hien kieu
     * "Đã duyệt hôm nay" nhu truoc 1/10/2026, vi khong co so da choi thi khong ve duoc thanh
     * ba khuc.
     */
    val daChoiMs: Long? = null,
    /** Luc doan phien dang chay bat dau, theo gio tablet. 0 la khong co doan nao chay. */
    val doanChoiTu: Long = 0L,
    val soBaiCho: Int = 0,
    /**
     * So phut trong "Quỹ giờ chơi" cua tablet, xem [Duong.F_QUY_GIO].
     *
     * null la tablet ban cu chua gui truong nay. Luc do man Gio choi an ca hang quy: tablet
     * do cung khong hieu lenh [Lenh.CAP_QUY], bam vao chi duoc cau "Không hiểu lệnh".
     */
    val quyGio: Int? = null,
    /**
     * Cac de thi in san trong tablet va tinh trang tung de, xem [DeThiTT.doc].
     *
     * Tu 1/10/2026 la de cua ca ba mon, doc tu [Duong.F_CAC_DE_THI]. Tablet ban truoc ngay do
     * chi gui [Duong.F_DE_THI], toan de Tieng Anh. null la tablet ban cu hon nua, chua co de
     * thi: man Bang an het cac hang "Đề thi thử", vi tablet do khong hieu lenh [Lenh.MO_DE_THI].
     */
    val deThi: List<DeThiTT>? = null,
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

    /**
     * Ba khuc cua thanh ngay, tinh dung nhu ThanhNgay ben tablet (1/10/2026). null la tablet
     * ban cu, xem [daChoiMs].
     *
     * Doan dang chay tu cong tu [doanChoiTu], va khong dai qua moc ket thuc: phien het luc
     * [ketThucLuc] ma tablet chua kip day ban moi thi phan sau do khong phai la choi. Da choi
     * lam tron xuong, dang giu lam tron len, nhu ben tablet, de hai so cong lai dung bang
     * phieu.
     */
    fun soNgay(bayGio: Long = System.currentTimeMillis()): SoNgay? {
        val daGom = daChoiMs ?: return null
        val doan = if (cong == Cong.DANG_CHOI && doanChoiTu > 0L) {
            (bayGio - doanChoiTu).coerceIn(0L, (ketThucLuc - doanChoiTu).coerceAtLeast(0L))
        } else 0L
        return SoNgay(
            daChoi = ((daGom + doan) / 60_000L).toInt(),
            con = ((conLaiBayGio(bayGio) + 59_999L) / 60_000L).toInt(),
            conKiem = phutConLai
        )
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
                daChoiMs = d.getLong(Duong.F_DA_CHOI_MS)?.coerceAtLeast(0L),
                doanChoiTu = d.getLong(Duong.F_DOAN_CHOI_TU) ?: 0L,
                soBaiCho = (d.getLong(Duong.F_SO_BAI_CHO) ?: 0L).toInt(),
                quyGio = d.getLong(Duong.F_QUY_GIO)?.toInt()?.coerceAtLeast(0),
                deThi = DeThiTT.doc(d.get(Duong.F_CAC_DE_THI), d.get(Duong.F_DE_THI)),
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
 * So phut cua ba khuc thanh ngay: da choi, dang giu ([con]), con kiem them duoc bang bai.
 *
 * [duoc] la "được chơi", gom ca gio nguoi lon cho; [tong] la ca thanh. Kiem 45 phut, ba cho
 * 30, choi 30 thi daChoi 30, con 45, duoc 75, tong 75 + 170 = 245 (vi du Ba Huy dua 1/10/2026).
 */
data class SoNgay(val daChoi: Int, val con: Int, val conKiem: Int) {
    val duoc: Int get() = daChoi + con
    val tong: Int get() = duoc + conKiem
}

/**
 * Mot de thi in san ben tablet, doc tu [Duong.F_CAC_DE_THI] cua hop/trangthai (tablet ban cu:
 * [Duong.F_DE_THI]). Cac hang "Đề thi thử <môn>" o tab Gio choi ve tu day, xem
 * [vn.huytl.bangdieukhien.ui.HangDeThi].
 *
 * VI SAO PHAM VI LA CHU (1/10/2026). Truoc ngay do tablet chi co de Tieng Anh, moi de kem so
 * Unit cuoi de cham toi (truong den), va man Bang tu ghep "tới Unit N". De Toan, KHTN mo theo
 * moc "Lớp đã học tới" cua tung phan (Dai so, Hinh hoc; Hoa, Li, Sinh), moc do chi tablet biet,
 * nen tablet viet san [phamVi] va [thieu]; may nay chi hien, khong tu tinh lai luat mo de. Ban
 * doc deThi cu doi so Unit thanh [phamVi] ngay luc doc, nen lop nay khong con truong den: hai
 * kieu du lieu cua tablet ra cung mot kieu tren man Bang.
 *
 * @param tt tinh trang: [KHOA] chua toi pham vi, [SAN] mo duoc, [MO] dang mo ma con chua bat
 *   dau, [DANG] dang lam, [XONG] da nop va khong dang mo.
 * @param mon ten mon day du nhu tablet gui: "Toán", "Khoa học tự nhiên", "Tiếng Anh". Rong la
 *   tablet khong ghi.
 * @param phamVi pham vi cua de: "Đại số tới Bài 9, Hình học tới Bài 14", "tới Unit 3". Rong la
 *   khong biet.
 * @param thieu phan lop chua hoc toi khi [KHOA]: "Hình học mới tới Bài 12, đề cần Bài 14",
 *   nhieu phan noi bang "; ". Rong khi khong khoa, va luon rong voi tablet ban cu.
 * @param sao diem lan nop gan nhat, -1 khi chua nop lan nao.
 * @param nopLuc luc nop lan gan nhat (ms), 0 khi chua nop, hay tablet ban cu khong gui.
 * @param phut gio lam bai cua de, 0 khi tablet ban cu khong gui.
 * @param doRong do rong pham vi tablet tinh (so Unit cua de Anh, tong so bai cac phan cua de
 *   Toan, KHTN), de chon de hep nhat cho dong "Mở khi ...". -1 khi khong biet.
 */
data class DeThiTT(
    val ma: String,
    val ten: String,
    val tt: String,
    val mon: String = "",
    val phamVi: String = "",
    val thieu: String = "",
    val sao: Int = -1,
    val toiDa: Int = -1,
    val nopLuc: Long = 0L,
    val phut: Int = 0,
    val doRong: Int = -1
) {
    val daNop: Boolean get() = sao >= 0 && toiDa > 0

    companion object {
        const val KHOA = "KHOA"
        const val SAN = "SAN"
        const val MO = "MO"
        const val DANG = "DANG"
        const val XONG = "XONG"

        /** Mon cua moi de trong [Duong.F_DE_THI]: truong do chi co de Tieng Anh. */
        const val MON_ANH = "Tiếng Anh"

        /**
         * Doc hai truong de thi cua hop/trangthai: [Duong.F_CAC_DE_THI] truoc, vang (hay khong
         * phai mang) thi [Duong.F_DE_THI] cua tablet ban cu. null khi vang ca hai: tablet chua
         * co de thi nao. Phan tu thieu ma thi bo.
         *
         * cacDeThi la mang rong van la tablet moi, khong lui ve deThi: tablet moi lay deThi tu
         * chinh danh sach do (chi giu de Tieng Anh), nen ben nay rong thi ben kia cung rong.
         *
         * Tach khoi [TrangThai.doc] de test doc duoc ma khong can Firestore.
         */
        fun doc(cacDeThi: Any?, deThi: Any?): List<DeThiTT>? =
            docCacDeThi(cacDeThi) ?: docDeThiCu(deThi)

        /** Mang { ma, mon, ten, phamVi, tt, thieu, sao, toiDa, nopLuc, phut, doRong }, tu 1/10/2026. */
        private fun docCacDeThi(tho: Any?): List<DeThiTT>? =
            (tho as? List<*>)?.mapNotNull { x ->
                val m = x as? Map<*, *> ?: return@mapNotNull null
                val ma = chu(m["ma"])
                if (ma.isEmpty()) return@mapNotNull null
                DeThiTT(
                    ma = ma,
                    ten = chu(m["ten"]).ifEmpty { ma },
                    tt = chu(m["tt"]),
                    mon = chu(m["mon"]),
                    phamVi = chu(m["phamVi"]),
                    thieu = chu(m["thieu"]),
                    sao = (m["sao"] as? Number)?.toInt() ?: -1,
                    toiDa = (m["toiDa"] as? Number)?.toInt() ?: -1,
                    nopLuc = (m["nopLuc"] as? Number)?.toLong()?.coerceAtLeast(0L) ?: 0L,
                    phut = (m["phut"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                    doRong = (m["doRong"] as? Number)?.toInt() ?: -1
                )
            }

        /**
         * Mang { ma, ten, den, tt, sao, toiDa } cua tablet ban cu (30/9/2026): toan de Tieng
         * Anh, den la Unit cuoi de cham toi. Doi den thanh [phamVi] "tới Unit N", dung chu tablet
         * moi viet cho de Tieng Anh, de man Bang khong phai biet hai kieu. Ban cu khong gui thieu,
         * nopLuc, phut.
         */
        private fun docDeThiCu(tho: Any?): List<DeThiTT>? =
            (tho as? List<*>)?.mapNotNull { x ->
                val m = x as? Map<*, *> ?: return@mapNotNull null
                val ma = chu(m["ma"])
                if (ma.isEmpty()) return@mapNotNull null
                val den = (m["den"] as? Number)?.toInt() ?: 0
                DeThiTT(
                    ma = ma,
                    ten = chu(m["ten"]).ifEmpty { ma },
                    tt = chu(m["tt"]),
                    mon = MON_ANH,
                    phamVi = if (den > 0) "tới Unit $den" else "",
                    sao = (m["sao"] as? Number)?.toInt() ?: -1,
                    toiDa = (m["toiDa"] as? Number)?.toInt() ?: -1,
                    // De Anh: do rong la so Unit, nhu tablet tinh.
                    doRong = if (den > 0) den else -1
                )
            }

        private fun chu(v: Any?): String = (v as? String)?.trim().orEmpty()
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
    val loaiLoi: String = ""
) {
    /**
     * Mot dong trong chamClaude.cac, dung kieu tablet doc o BaiDaCham ben nop-bai.
     *
     * Chi sau truong nay, tu truoc 29/9/2026. So dong, dang bai va kieu sai khong nam o
     * day ma nam nguyen ban trong chamClaude.goi, xem [KetQuaClaude.goi].
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
 *
 * Truong onTap (lan nop la on lai bai chup anh) bo ngay 2/10/2026 cung duong on chup anh
 * tren tablet. Truoc do lop nay doc no ma khong cho nao dung.
 */
data class KhaiBai(
    val tenNguon: String,
    val bai: String,
    val mon: String,
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
 * Cac dong vo dan do chua toi han cua mot buoi hoc, trong hop/nhacbai. Xem [Duong.D_NHAC_BAI].
 *
 * Tablet tinh han: moi dong toi tiet sau cua dung mon do, dong khong doc ra mon thi toi
 * buoi hoc ke tiep. Dien thoai chi hien.
 */
data class NhacBaiBuoi(
    /** Khoa buoi, "20261003-CHIEU". */
    val ma: String,
    /** Ngay cua buoi, dang yyyy-MM-dd. */
    val ngay: String,
    /** "chiều thứ bảy 3/10". */
    val ten: String,
    /** Luc vao hoc, epoch ms. */
    val vaoHoc: Long,
    val cac: List<Dong>
) {
    /** [bai] la dong con tich la bai tap; [ngayVo] la ngay ghi tren trang vo co dong do. */
    data class Dong(val chu: String, val bai: Boolean, val mon: String, val ngayVo: String)

    val cacBai: List<Dong> get() = cac.filter { it.bai }
    val dongKhac: List<Dong> get() = cac.filterNot { it.bai }

    companion object {
        /** Doc ca document hop/nhacbai. Thieu hay hong thi ra danh sach rong. */
        fun docHet(m: Map<*, *>?): List<NhacBaiBuoi> =
            (m?.get(Duong.F_CAC_BUOI) as? List<*>).orEmpty().mapNotNull { doc(it as? Map<*, *>) }

        fun doc(m: Map<*, *>?): NhacBaiBuoi? {
            if (m == null) return null
            val ma = (m["ma"] as? String)?.trim().orEmpty()
            val ten = (m["ten"] as? String)?.trim().orEmpty()
            if (ma.isEmpty() || ten.isEmpty()) return null
            val cac = (m["cac"] as? List<*>).orEmpty().mapNotNull { x ->
                val d = x as? Map<*, *> ?: return@mapNotNull null
                val chu = (d["chu"] as? String)?.trim().orEmpty()
                if (chu.isEmpty()) return@mapNotNull null
                Dong(
                    chu = chu,
                    bai = d["bai"] as? Boolean ?: false,
                    mon = (d["mon"] as? String)?.trim().orEmpty(),
                    ngayVo = (d["ngayVo"] as? String)?.trim().orEmpty()
                )
            }
            if (cac.isEmpty()) return null
            return NhacBaiBuoi(
                ma = ma,
                ngay = (m["ngay"] as? String)?.trim().orEmpty(),
                ten = ten,
                vaoHoc = (m["vaoHoc"] as? Number)?.toLong() ?: 0L,
                cac = cac
            )
        }
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
    val gioiHanApp: Map<String, Int> = emptyMap(),
    /**
     * Mot phut choi doi duoc may phut Netflix tren laptop (7/10/2026). null la tablet ban
     * cu, chua co muc nay: man Cai dat an dong do.
     */
    val tiLeNetflix: Int? = null
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
                    .orEmpty().mapValues { it.value.toInt() },
                tiLeNetflix = d.getLong("tiLeNetflix")?.toInt()
            )
        }
    }
}

/**
 * Tinh trang laptop xem Netflix, doc tu laptop/{maNha} (7/10/2026, xem [Duong.LAPTOP]).
 *
 * Laptop chi ghi khi co gi doi. Luc con dang xem thi [ketThucLuc] la luc het gio va may nay
 * tu dem lui, nhu the Gio choi; luc khong ai dung thi [conLaiMs] dung yen.
 */
data class TinhTrangLaptop(
    val ketThucLuc: Long,
    val conLaiMs: Long,
    val dangDung: Boolean,
    val capNhatLuc: Long,
    /** Ba Huy da bam "Mở web" ([Duong.F_MO_WEB]). */
    val moWeb: Boolean,
    /** Laptop bao luat chan web dang go that ([Duong.F_WEB_DANG_MO]). */
    val webDangMo: Boolean,
    /**
     * Tu 8/10/2026 (laptop ban cu khong ghi, de mac dinh): tai khoan dang ngoi man hinh
     * ([Duong.F_PHIEN], rong la o man dang nhap) va tu luc nao, luc may bat, luc may tat dung
     * cach (0 khi dang chay), cac lan bat tat dang nhap trong ngay, ket qua cac lenh gan nhat.
     */
    val phien: String = "",
    val phienTu: Long = 0L,
    val batLuc: Long = 0L,
    val tatLuc: Long = 0L,
    val suKien: List<SuKienTrenLaptop> = emptyList(),
    val ketQua: List<KetQuaLenhLaptop> = emptyList()
) {
    fun conLai(bayGio: Long = System.currentTimeMillis()): Long =
        if (ketThucLuc > 0L) (ketThucLuc - bayGio).coerceAtLeast(0L) else conLaiMs

    /** Laptop da ghi luc tat cho lan bat gan nhat: tat dung cach, chua bat lai. */
    fun daTat(): Boolean = tatLuc > 0L && tatLuc >= batLuc

    companion object {
        /** null la laptop chua noi vao nha: chua co document, hay chua co uid laptop. */
        fun doc(d: DocumentSnapshot?): TinhTrangLaptop? {
            if (d == null || !d.exists() || d.getString(Duong.F_UID_LAPTOP).isNullOrEmpty()) return null
            return TinhTrangLaptop(
                ketThucLuc = d.getLong(Duong.F_KET_THUC_LUC) ?: 0L,
                conLaiMs = d.getLong(Duong.F_CON_LAI_MS) ?: 0L,
                dangDung = d.getBoolean(Duong.F_DANG_DUNG) ?: false,
                capNhatLuc = d.getLong(Duong.F_CAP_NHAT_LUC) ?: 0L,
                moWeb = d.getBoolean(Duong.F_MO_WEB) ?: false,
                webDangMo = d.getBoolean(Duong.F_WEB_DANG_MO) ?: false,
                phien = d.getString(Duong.F_PHIEN).orEmpty(),
                phienTu = d.getLong(Duong.F_PHIEN_TU) ?: 0L,
                batLuc = d.getLong(Duong.F_BAT_LUC) ?: 0L,
                tatLuc = d.getLong(Duong.F_TAT_LUC) ?: 0L,
                suKien = (d.get(Duong.F_SU_KIEN) as? List<*>).orEmpty().mapNotNull { SuKienTrenLaptop.doc(it) },
                ketQua = (d.get(Duong.F_KET_QUA) as? List<*>).orEmpty().mapNotNull { KetQuaLenhLaptop.doc(it) }
            )
        }
    }
}

/** Mot dong trong [Duong.F_SU_KIEN]: [kieu] la mot trong [SuKienLaptop], [ai] ten tai khoan. */
data class SuKienTrenLaptop(val kieu: String, val luc: Long, val ai: String) {
    companion object {
        fun doc(o: Any?): SuKienTrenLaptop? {
            val m = o as? Map<*, *> ?: return null
            val kieu = m[Duong.F_KIEU] as? String ?: return null
            val luc = (m[Duong.F_LUC] as? Number)?.toLong() ?: return null
            return SuKienTrenLaptop(kieu, luc, m[Duong.F_AI] as? String ?: "")
        }
    }
}

/** Ket qua mot lenh laptop da lam, xem [Duong.F_KET_QUA]. [id] la ma document lenh. */
data class KetQuaLenhLaptop(val id: String, val kieu: String, val ok: Boolean, val chu: String, val luc: Long) {
    companion object {
        fun doc(o: Any?): KetQuaLenhLaptop? {
            val m = o as? Map<*, *> ?: return null
            val id = m[Duong.F_ID] as? String ?: return null
            return KetQuaLenhLaptop(
                id = id,
                kieu = m[Duong.F_KIEU] as? String ?: "",
                ok = m[Duong.F_OK] as? Boolean ?: false,
                chu = m[Duong.F_CHU] as? String ?: "",
                luc = (m[Duong.F_LUC] as? Number)?.toLong() ?: 0L
            )
        }
    }
}

/** Mot lenh may nay gui laptop ma laptop chua lay (con nam trong laptop/{maNha}/lenh). */
data class LenhLaptopCho(val id: String, val kieu: String, val tao: Long)

/**
 * Mot phieu cap hay bot phut Netflix laptop chua nhan (con nam trong laptop/{maNha}/cap), cho dong
 * "Chờ nhận thêm 20 phút (gửi 14:05)" cua the Laptop (anh Huy chot 9/10/2026). [phut] am la phieu
 * bot. Laptop nhan xong la xoa phieu, nen phieu con nam do tuc la laptop dang tat, mat mang, hay
 * chi vai giay chua kip. Phieu Le Hoa doi phut tren tablet cung nam o day.
 */
data class PhieuLaptopCho(val id: String, val phut: Int, val tao: Long)

/** Anh chup man hinh laptop moi nhat, xem [Duong.ANH]. */
class AnhLaptop(val jpg: ByteArray, val luc: Long, val phien: String)

/** Mot app dang cai tren tablet, de chon tu xa ma khong phai go ten goi. */
data class AppTrenMay(val goi: String, val ten: String)

/**
 * Mot lenh may nay da go ma tablet chua lay. Xem [Kho.ngheLenhCho].
 *
 * Tablet lam xong lenh nao la xoa document do. Nen lenh con nam trong hang tuc la tablet
 * chua nhan: dang mat mang, dang tat may, hay chi la mot hai giay chua kip.
 *
 * VI SAO PHAI HIEN RA. Firestore nhan lenh xong la nut tren man Bang sang lai, ma luc
 * do tablet chua lam gi. Tablet mat mang thi lenh nam cho, tablet co mang lai la van lam,
 * tre bao lau cung lam (tu 8/10/2026; truoc do lenh cu hon nua tieng bi bo). Ba Huy bam
 * "15'", khong thay gi doi, bam them lan nua: hai lenh cho gio cong don, Le Hoa duoc 30
 * phut. Thua thi bam "Bớt", lenh bot di sau va tablet lam dung thu tu bam.
 */
data class LenhCho(
    val id: String,
    val kieu: String,
    val phut: Int?,
    /** Luc go, theo dong ho may nay. Tablet lay truong nay de lam cac lenh dung thu tu bam. */
    val tao: Long,
    /** Chua len duoc may chu: chinh dien thoai dang mat mang. */
    val chuaLenMang: Boolean
) {
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
