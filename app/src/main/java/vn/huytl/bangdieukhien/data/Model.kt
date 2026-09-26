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

/**
 * Mot dot viec nha ba noi giao, doc tu hop/viecnha.
 *
 * Document nay chi may ba ghi. May nay doc de lam mot viec duy nhat: chia ra thay
 * khi tablet chac chan se tu choi - dot da xong het ma go tu lau hon [Duong.QUA_CU_MS],
 * nghia la luc ba bam xong thi tablet dang tat. Xem the trong BangFragment.
 */
data class ViecNhaCho(
    val maPhien: String,
    /** Luc ba bam lan gan nhat. Ghi lai moi lan ba cham vao, khong phai luc giao. */
    val luc: Long,
    val cac: List<Viec>
) {
    data class Viec(val ten: String, val phut: Int, val xong: Boolean)

    val xongHet: Boolean get() = cac.isNotEmpty() && cac.all { it.xong }
    val tongPhut: Int get() = cac.sumOf { it.phut }
    val ke: String get() = cac.joinToString(", ") { it.ten }

    /** Tablet chac chan da bo qua dot nay, va ba thi khong con biet de bam lai. */
    val tabletDaBoQua: Boolean
        get() = xongHet && luc > 0L && System.currentTimeMillis() - luc > Duong.QUA_CU_MS

    companion object {
        fun doc(d: DocumentSnapshot?): ViecNhaCho? {
            if (d == null || !d.exists()) return null
            val ma = d.getString(Duong.F_MA_PHIEN).orEmpty()
            if (ma.isBlank()) return null
            val cac = (d.get(Duong.F_VIEC) as? List<*>).orEmpty()
                .filterIsInstance<Map<*, *>>()
                .mapNotNull { o ->
                    val ten = (o[Duong.F_TEN] as? String)?.trim()?.takeIf { it.isNotEmpty() }
                        ?: return@mapNotNull null
                    Viec(
                        ten = ten,
                        phut = (o[Duong.F_PHUT] as? Number)?.toInt() ?: 0,
                        xong = o[Duong.F_XONG] == true
                    )
                }
            return ViecNhaCho(ma, d.getLong(Duong.F_LUC) ?: 0L, cac)
        }
    }
}

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
    /** Viec nha ba noi giao ma Le Hoa chua lam xong. Con viec thi tablet dang khoa. */
    val viecNha: List<String> = emptyList(),
    val phutDaDuyet: Int = 0,
    val phutConLai: Int = 0,
    val soBaiCho: Int = 0,
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
    val lamHetDanDo: Boolean = false
) {
    fun soDung(): Int = cac.count { it.dung }
    fun coCauKhongRo(): Boolean = cac.any { !it.docRo }
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
    /** 1 la viet bang muc do, 0 la khong, -1 la khong noi. Chi can o lan on tap. */
    val mucDo: Int = -1,
    /** De Claude chep tu anh, chi can khi con khong khai theo sach. */
    val de: String = "",
    /** Dang bai Claude xep, ten hang cua DangBai ben tablet. Chi can o cau ngoai sach. */
    val dang: String = "",
    /**
     * Cau nay thuoc bai co giao trong vo dan do. null la Claude khong noi, luc do tablet
     * tu quyet theo luat cua may cham.
     */
    val trongDanDo: Boolean? = null
)

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
    val cac: List<Cau>
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
    val chinh: Boolean = false
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
    val an: Boolean = false
) {
    /** Con nam trong hang cho cua tablet, tuc la bam Duyet hay Khong duyet con co tac dung. */
    val dangCho: Boolean get() = trangThai == CHO && !quaNgay()

    /** Da xong viec, khong con gi de bam. Chi bai xong moi co nut Xoa. */
    val xong: Boolean get() = !dangCho

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
                an = d.getBoolean(F_AN) ?: false
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
                cac = cac
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
                chinh = m["chinh"] as? Boolean ?: false
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
                lamHetDanDo = m["lamHetDanDo"] as? Boolean ?: false
            )
        }
    }
}

/** Mot tin nhan giua hai cha con. */
data class TinChat(
    val id: String,
    val tu: String,
    val chu: String,
    val luc: Long,
    val daDoc: Boolean
) {
    val cuaCon: Boolean get() = tu == CON

    companion object {
        const val BA = "BA"
        const val CON = "CON"

        fun doc(d: DocumentSnapshot) = TinChat(
            id = d.id,
            tu = d.getString(Duong.F_TU) ?: CON,
            chu = d.getString(Duong.F_CHU).orEmpty(),
            luc = d.getLong(Duong.F_LUC) ?: 0L,
            daDoc = d.getBoolean(Duong.F_DA_DOC) ?: false
        )
    }
}

/** Cau hinh dang chay tren tablet, doc tu hop/caidat. Dien thoai khong ghi vao day. */
data class CaiDat(
    val phutMacDinh: Int = 60,
    val gioNgu: Int = 22 * 60,
    val gioDay: Int = 6 * 60,
    val tranPhutMoiNgay: Int = 120,
    val khoaCaiDat: Boolean = true,
    /** Tablet co tu cham bai bang AI khong. Tat thi Ba Huy cham bang Claude. */
    val chamBangAi: Boolean = true,
    val appChoPhep: List<String> = emptyList(),
    val appChan: List<String> = emptyList(),
    val appAi: List<String> = emptyList(),
    val gioiHanApp: Map<String, Int> = emptyMap()
) {
    companion object {
        fun doc(d: DocumentSnapshot?): CaiDat? {
            if (d == null || !d.exists()) return null
            @Suppress("UNCHECKED_CAST")
            return CaiDat(
                phutMacDinh = (d.getLong("phutMacDinh") ?: 60L).toInt(),
                gioNgu = (d.getLong("gioNgu") ?: (22 * 60L)).toInt(),
                gioDay = (d.getLong("gioDay") ?: (6 * 60L)).toInt(),
                tranPhutMoiNgay = (d.getLong("tranPhutMoiNgay") ?: 120L).toInt(),
                khoaCaiDat = d.getBoolean("khoaCaiDat") ?: true,
                chamBangAi = d.getBoolean("chamBangAi") ?: true,
                appChoPhep = (d.get("appChoPhep") as? List<String>).orEmpty(),
                appChan = (d.get("appChan") as? List<String>).orEmpty(),
                appAi = (d.get("appAi") as? List<String>).orEmpty(),
                gioiHanApp = (d.get("gioiHanApp") as? Map<String, Number>)
                    .orEmpty().mapValues { it.value.toInt() }
            )
        }
    }
}

/** Mot app dang cai tren tablet, de chon tu xa ma khong phai go ten goi. */
data class AppTrenMay(val goi: String, val ten: String)
