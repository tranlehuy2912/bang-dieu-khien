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
    val appTruocMat: String = "",
    val banApp: String = "",
    val capNhatLuc: Long = 0L,
    /** Cau tablet noi lai sau khi lam lenh gan nhat, rong la chua co gi. */
    val traLoi: String = "",
    val traLoiLuc: Long = 0L
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

    /** Tablet im lang qua lau thi so lieu tren man hinh khong con dang tin. */
    fun cu(bayGio: Long = System.currentTimeMillis()): Boolean =
        capNhatLuc > 0L && bayGio - capNhatLuc > CU_SAU_MS

    fun coCanhBao(): Boolean = !quyenTroGiup || !quyenQuanTri || !quyenNoi || !coPin

    companion object {
        /** Tablet ghi lai moi luc doi trang thai, va it nhat mot lan moi 15 phut. */
        const val CU_SAU_MS = 40 * 60_000L

        fun doc(d: DocumentSnapshot?): TrangThai? {
            if (d == null || !d.exists()) return null
            val cheDoBa = d.get(Duong.F_CHE_DO_BA) as? Map<*, *>
            val quyen = d.get(Duong.F_QUYEN) as? Map<*, *>
            return TrangThai(
                cong = d.getString(Duong.F_CONG) ?: Cong.KHOA,
                ketThucLuc = d.getLong(Duong.F_KET_THUC_LUC) ?: 0L,
                conLaiMs = d.getLong(Duong.F_CON_LAI_MS) ?: 0L,
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
                banApp = d.getString(Duong.F_BAN_APP).orEmpty(),
                capNhatLuc = d.getLong(Duong.F_CAP_NHAT_LUC) ?: 0L,
                traLoi = (d.get(Duong.F_TRA_LOI) as? Map<*, *>)?.get("chu") as? String ?: "",
                traLoiLuc = ((d.get(Duong.F_TRA_LOI) as? Map<*, *>)?.get("luc") as? Number)
                    ?.toLong() ?: 0L
            )
        }
    }
}

/** Mot tam anh trong lan nop bai. */
data class Anh(val fileId: String, val khau: String) {
    fun tenKhau(): String = when (khau) {
        "DANDO" -> "Vở dặn dò"
        "DEBAI" -> "Đề bài"
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

/** Mot lan con nop bai. */
data class Bai(
    val id: String,
    val luc: Long,
    val trangThai: String,
    val soPhut: Int,
    val anh: List<Anh>,
    val cham: KetQuaCham?,
    val messageId: Long
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
                messageId = d.getLong(Duong.F_MESSAGE_ID) ?: 0L
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
