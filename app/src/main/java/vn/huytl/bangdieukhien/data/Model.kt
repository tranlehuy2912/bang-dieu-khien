package vn.huytl.bangdieukhien.data

import com.google.firebase.firestore.DocumentSnapshot

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
    val goiY: String
)

/** Ban Claude cham lai, nam o [Duong.F_CHAM_CLAUDE] canh ban cham cua may. */
data class KetQuaClaude(val luc: Long, val cac: List<CauClaude>) {
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
    val claude: KetQuaClaude? = null
) {
    val dangCho: Boolean get() = trangThai == CHO

    companion object {
        const val CHO = "CHO"
        const val DUYET = "DUYET"
        const val TU_CHOI = "TUCHOI"

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
                claude = docClaude(d.get(Duong.F_CHAM_CLAUDE) as? Map<*, *>)
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
                    goiY = o["goiY"] as? String ?: ""
                )
            }
            if (cac.isEmpty()) return null
            return KetQuaClaude((m["luc"] as? Number)?.toLong() ?: 0L, cac)
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
