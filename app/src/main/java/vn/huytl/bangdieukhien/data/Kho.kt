package vn.huytl.bangdieukhien.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

/**
 * Cua duy nhat di ra Firestore.
 *
 * Moi man hinh deu goi qua day chu khong tu cam FirebaseFirestore: gom mot cho thi
 * duong dan chi viet mot lan, va sau nay doi cach luu du lieu thi sua mot file.
 *
 * Khong dung Task.await() vi nhu the phai keo them thu vien
 * kotlinx-coroutines-play-services chi de cho vai lan goi. Callback la du.
 */
object Kho {

    private const val TAG = "BangDieuKhien"

    /** Ket qua mot viec co the hong, de man hinh biet noi gi voi Ba Huy. */
    sealed interface KetQua {
        data object Xong : KetQua
        data class Hong(val viSao: String) : KetQua
    }

    /**
     * Da khai bao Firebase chua.
     *
     * Thieu google-services.json thi initializeApp tra ve null. Luc do app van mo
     * duoc, chi la man nao cung bao "chua noi Firebase" thay vi tat ngang.
     */
    fun san(context: Context): Boolean = app(context) != null

    private fun app(context: Context): FirebaseApp? =
        runCatching { FirebaseApp.initializeApp(context.applicationContext) }.getOrNull()
            ?: runCatching { FirebaseApp.getInstance() }.getOrNull()

    private fun db(context: Context): FirebaseFirestore? =
        app(context)?.let { FirebaseFirestore.getInstance(it) }

    private fun auth(context: Context): FirebaseAuth? =
        app(context)?.let { FirebaseAuth.getInstance(it) }

    /** Uid cua may nay. Rong la chua dang nhap xong. */
    fun uid(context: Context): String = auth(context)?.currentUser?.uid.orEmpty()

    /**
     * Dang nhap an danh roi goi [xong].
     *
     * An danh: khong ai phai nho mat khau nao, nhung may van co mot uid rieng de
     * luat truy cap ben Firestore bam vao. Uid do nam lai tren may; go app roi cai
     * lai la ra uid moi va phai ghep doi lai - doi lai khong the mo trom bang cach
     * cai app len may khac.
     */
    fun dangNhap(context: Context, xong: (KetQua) -> Unit) {
        val a = auth(context) ?: return xong(KetQua.Hong(THIEU_FIREBASE))
        if (a.currentUser != null) return xong(KetQua.Xong)
        a.signInAnonymously()
            .addOnSuccessListener { xong(KetQua.Xong) }
            .addOnFailureListener {
                Log.w(TAG, "dang nhap hong", it)
                xong(
                    KetQua.Hong(
                        "Chưa đăng nhập được Firebase. Kiểm tra mạng, và xem đã bật " +
                            "Anonymous trong phần Authentication chưa."
                    )
                )
            }
    }

    // ---------------------------------------------------------------- ghep doi

    /**
     * Xin vao nha.
     *
     * May nay chua phai nguoi nha nen chua ghi duoc gi ngoai mot cho: nha/{ma
     * nha}/ghep/{uid cua chinh minh}. Dat ma sau so vao do roi cho tablet ket nap.
     *
     * Tra ve Xong nghia la da dat duoc loi xin, chua phai da vao duoc nha. Ben goi
     * phai nghe tiep bang [ngheKetNap].
     */
    fun xinVaoNha(context: Context, maNha: String, maGhep: String, xong: (KetQua) -> Unit) {
        dangNhap(context) { kq ->
            if (kq is KetQua.Hong) return@dangNhap xong(kq)
            val d = db(context) ?: return@dangNhap xong(KetQua.Hong(THIEU_FIREBASE))
            val uid = uid(context)
            if (uid.isEmpty()) return@dangNhap xong(KetQua.Hong("Chưa có danh tính máy."))

            d.collection(Duong.NHA).document(maNha.trim())
                .collection(Duong.GHEP).document(uid)
                .set(
                    mapOf(
                        "ma" to maGhep.trim(),
                        "luc" to System.currentTimeMillis(),
                        // May nay xin vao lam nguoi nha day du. May ba noi gui
                        // Nguoi.BA_NOI, va ben kia xep no vao danh sach phu.
                        Duong.F_AI to Nguoi.BA_HUY
                    )
                )
                .addOnSuccessListener { xong(KetQua.Xong) }
                .addOnFailureListener {
                    Log.w(TAG, "xin vao nha hong", it)
                    xong(KetQua.Hong("Không gửi được lời xin. Mã nhà gõ sai, hoặc mất mạng."))
                }
        }
    }

    /**
     * Nghe xem tablet da ket nap chua.
     *
     * Tablet doc ma, so voi ma dang hien tren man hinh no, dung thi ghi
     * trangThai = "OK" vao chinh cai o xin nay. Khong dung thi ghi "SAI" - va do
     * la loi duy nhat ben nay phan biet duoc voi mat mang.
     */
    fun ngheKetNap(
        context: Context,
        maNha: String,
        khi: (trangThai: String) -> Unit
    ): ListenerRegistration? {
        val d = db(context) ?: return null
        val uid = uid(context)
        if (uid.isEmpty()) return null
        return d.collection(Duong.NHA).document(maNha.trim())
            .collection(Duong.GHEP).document(uid)
            .addSnapshotListener { snap, loi ->
                if (loi != null) return@addSnapshotListener
                khi(snap?.getString("trangThai").orEmpty())
            }
    }

    // ------------------------------------------- ket nap lai may tinh bang

    /**
     * Tao mot ma ghep de tablet xin vao lai nha nay.
     *
     * NGUOC CHIEU voi [xinVaoNha]. Binh thuong tablet lap nha va ket nap dien thoai.
     * Nhung cai lai app tren tablet la no mat sach: mat ma nha, va mat ca tu cach
     * nguoi nha tren Firestore. Luc do dien thoai la may duy nhat con trong nha, nen
     * no phai lam nguoi giu cua.
     *
     * Ma song muoi phut, y nhu ma tablet tung phat. Het han thi tao lai.
     */
    fun taoMaGhepChoTablet(context: Context, xong: (ma: String, loi: String) -> Unit) {
        val d = nha(context) ?: return xong("", "Máy này chưa ghép với tablet.")
        val ma = "%06d".format(java.security.SecureRandom().nextInt(1_000_000))
        d.update(
            mapOf(
                Duong.F_MA_GHEP to ma,
                Duong.F_MA_GHEP_HET_HAN to System.currentTimeMillis() + MA_GHEP_SONG_MS
            )
        )
            .addOnSuccessListener { xong(ma, "") }
            .addOnFailureListener {
                Log.w(TAG, "tao ma ghep hong", it)
                xong("", loiNguoiDoc(it))
            }
    }

    /**
     * Nghe xem co may nao dang xin vao nha, va ket nap neu ma dung.
     *
     * Chep dung luat ma tablet van dung: so ma, con han thi them uid vao danh sach
     * nguoi nha roi ghi "OK"; sai thi ghi "SAI". Thu hoi ma ngay sau khi dung - mot
     * ma mot lan, de anh chup man hinh tu tuan truoc khong con gia tri.
     *
     * [khi] duoc goi voi true khi vua ket nap mot may.
     */
    fun ngheXinVao(context: Context, khi: (ketNapDuoc: Boolean) -> Unit): ListenerRegistration? {
        val d = nha(context) ?: return null
        return d.collection(Duong.GHEP).addSnapshotListener { snap, loi ->
            if (loi != null) return@addSnapshotListener
            snap?.documents.orEmpty().forEach { xin ->
                if (xin.getString("trangThai") != null) return@forEach
                // Bo qua chinh loi xin cua may nay, neu no con sot lai tu lan ghep cu.
                if (xin.id == uid(context)) return@forEach
                d.get().addOnSuccessListener { nhaDoc ->
                    val ma = nhaDoc.getString(Duong.F_MA_GHEP).orEmpty()
                    val han = nhaDoc.getLong(Duong.F_MA_GHEP_HET_HAN) ?: 0L
                    val dung = ma.isNotEmpty() &&
                        xin.getString("ma") == ma &&
                        System.currentTimeMillis() < han
                    if (dung) {
                        // May ba noi vao danh sach phu: luat ben Firestore chi cho
                        // danh sach do go lenh cho gio. Xem uidsPhu trong
                        // firestore.rules.
                        val phu = xin.getString(Duong.F_AI) == Nguoi.BA_NOI
                        d.update(
                            if (phu) Duong.F_UIDS_PHU else Duong.F_UIDS,
                            FieldValue.arrayUnion(xin.id)
                        )
                        xin.reference.update("trangThai", "OK")
                        d.update(Duong.F_MA_GHEP, "", Duong.F_MA_GHEP_HET_HAN, 0L)
                        khi(true)
                    } else {
                        xin.reference.update("trangThai", "SAI")
                    }
                }
            }
        }
    }

    /** Ma ghep song bao lau. Du de cam hai may len go, khong du de quen. */
    private const val MA_GHEP_SONG_MS = 10 * 60_000L

    // ------------------------------------------------------------------- nghe

    fun ngheTrangThai(context: Context, khi: (TrangThai?, String?) -> Unit): ListenerRegistration? =
        hop(context, Duong.D_TRANG_THAI)?.addSnapshotListener { snap, loi ->
            if (loi != null) khi(null, loiNguoiDoc(loi)) else khi(TrangThai.doc(snap), null)
        }

    fun ngheCaiDat(context: Context, khi: (CaiDat?) -> Unit): ListenerRegistration? =
        hop(context, Duong.D_CAI_DAT)?.addSnapshotListener { snap, loi ->
            if (loi == null) khi(CaiDat.doc(snap))
        }

    fun ngheDanhSachApp(context: Context, khi: (List<AppTrenMay>) -> Unit): ListenerRegistration? =
        hop(context, Duong.D_DANH_SACH_APP)?.addSnapshotListener { snap, loi ->
            if (loi != null) return@addSnapshotListener
            val ds = (snap?.get("app") as? List<*>).orEmpty().mapNotNull { m ->
                val o = m as? Map<*, *> ?: return@mapNotNull null
                val goi = o["goi"] as? String ?: return@mapNotNull null
                AppTrenMay(goi, o["ten"] as? String ?: goi)
            }
            khi(ds)
        }

    /** Cac lan nop bai, moi nhat truoc. */
    fun ngheBai(context: Context, soLuong: Long, khi: (List<Bai>) -> Unit): ListenerRegistration? =
        nha(context)?.collection(Duong.BAI)
            ?.orderBy(Duong.F_LUC, Query.Direction.DESCENDING)
            ?.limit(soLuong)
            ?.addSnapshotListener { snap, loi ->
                if (loi != null) return@addSnapshotListener
                khi(snap?.documents.orEmpty().map { Bai.doc(it) })
            }

    /** Mot lan nop bai cu the, de man chi tiet thay ngay khi Ba Huy vua duyet xong. */
    fun ngheMotBai(context: Context, id: String, khi: (Bai?) -> Unit): ListenerRegistration? =
        nha(context)?.collection(Duong.BAI)?.document(id)
            ?.addSnapshotListener { snap, loi ->
                if (loi != null) return@addSnapshotListener
                khi(if (snap != null && snap.exists()) Bai.doc(snap) else null)
            }

    fun ngheChat(context: Context, soLuong: Long, khi: (List<TinChat>) -> Unit): ListenerRegistration? =
        nha(context)?.collection(Duong.CHAT)
            ?.orderBy(Duong.F_LUC, Query.Direction.DESCENDING)
            ?.limit(soLuong)
            ?.addSnapshotListener { snap, loi ->
                if (loi != null) return@addSnapshotListener
                // Doc ve moi nhat truoc de gioi han lay dung phan cuoi, roi lat lai
                // cho dung thu tu doc tren man hinh.
                khi(snap?.documents.orEmpty().map { TinChat.doc(it) }.reversed())
            }

    /**
     * Nghe dot viec nha ba noi giao.
     *
     * Document nay chi may ba ghi, va tablet xoa di khi da khep dot lai. Con nam do
     * nghia la chua ai nhan.
     */
    fun ngheViecNha(context: Context, khi: (ViecNhaCho?) -> Unit): ListenerRegistration? =
        hop(context, Duong.D_VIEC_NHA)?.addSnapshotListener { snap, loi ->
            if (loi != null) return@addSnapshotListener
            khi(ViecNhaCho.doc(snap))
        }

    /** Nhat ky mot ngay, dang "yyyy-MM-dd". */
    fun ngheNhatKy(context: Context, ngay: String, khi: (List<String>) -> Unit): ListenerRegistration? =
        nha(context)?.collection(Duong.NHAT_KY)?.document(ngay)
            ?.addSnapshotListener { snap, loi ->
                if (loi != null) return@addSnapshotListener
                khi((snap?.get(Duong.F_DONG) as? List<*>).orEmpty().filterIsInstance<String>())
            }

    fun ngheHoiAi(context: Context, ngay: String, khi: (List<String>) -> Unit): ListenerRegistration? =
        nha(context)?.collection(Duong.HOI_AI)?.document(ngay)
            ?.addSnapshotListener { snap, loi ->
                if (loi != null) return@addSnapshotListener
                khi((snap?.get(Duong.F_DONG) as? List<*>).orEmpty().filterIsInstance<String>())
            }

    // ------------------------------------------------------------------- ghi

    /**
     * Ghi ban Claude cham lai vao dung bai do, canh ban cham cua may.
     *
     * Khong ghi de [Duong.F_CHAM]: ban cua may giu nguyen de doi chieu, va man ket
     * qua ben tablet tu chon ket luan cua Claude khi co. Viec cong gio cho cau may
     * cham nham thi di duong lenh [Lenh.SUA_CHAM], vi chi tablet biet con han muc
     * hay dang gio ngu.
     */
    fun ghiChamClaude(
        context: Context,
        baiId: String,
        cac: List<CauClaude>,
        xong: (KetQua) -> Unit = {}
    ) {
        val n = nha(context) ?: return xong(KetQua.Hong(THIEU_FIREBASE))
        val ban = mapOf(
            "luc" to System.currentTimeMillis(),
            "cac" to cac.map {
                mapOf(
                    "ma" to it.ma,
                    "dung" to it.dung,
                    "chac" to it.chac,
                    "conViet" to it.conViet,
                    "goiY" to it.goiY
                )
            }
        )
        n.collection(Duong.BAI).document(baiId).update(Duong.F_CHAM_CLAUDE, ban)
            .addOnSuccessListener { xong(KetQua.Xong) }
            .addOnFailureListener {
                Log.w(TAG, "ghi ban Claude hong", it)
                xong(KetQua.Hong(loiNguoiDoc(it)))
            }
    }

    /**
     * Dat mot lenh vao hang doi cua tablet.
     *
     * Tablet nghe hang nay, lam xong thi xoa document di. Khong sua trang thai o
     * day: chi tablet moi biet that su co cap duoc gio khong (con han muc ngay
     * khong, co dang gio ngu khong), nen man hinh cho tablet noi lai.
     */
    fun guiLenh(
        context: Context,
        kieu: String,
        phut: Int? = null,
        baiId: String? = null,
        chu: String? = null,
        giaTri: Any? = null,
        xong: (KetQua) -> Unit = {}
    ) {
        val n = nha(context) ?: return xong(KetQua.Hong(THIEU_FIREBASE))
        val noi = mutableMapOf<String, Any>(
            Duong.F_KIEU to kieu,
            // May nay la cua Ba Huy, khong gioi han lenh nao. May ba noi go lenh
            // thi gui Nguoi.BA_NOI, va tablet chi nhan moi lenh cho gio.
            Duong.F_AI to Nguoi.BA_HUY,
            // Gio may chu gui kem gio may chu: tablet lay cai nay de bo lenh go tu
            // hom qua, con dong ho hai may thi khong bao gio khop nhau tuyet doi.
            Duong.F_TAO_LUC to FieldValue.serverTimestamp(),
            "tao" to System.currentTimeMillis()
        )
        phut?.let { noi[Duong.F_PHUT] = it }
        baiId?.let { noi[Duong.F_BAI_ID] = it }
        chu?.let { noi[Duong.F_CHU] = it }
        giaTri?.let { noi["giaTri"] = it }

        n.collection(Duong.LENH).add(noi)
            .addOnSuccessListener { xong(KetQua.Xong) }
            .addOnFailureListener {
                Log.w(TAG, "gui lenh hong", it)
                xong(KetQua.Hong(loiNguoiDoc(it)))
            }
    }

    /**
     * Hoi tablet con song khong, va trang thai that bay gio la gi.
     *
     * Khong cho cau tra loi o day: tablet dap bang cach day mot ban trang thai moi,
     * va listener dang mo san se nhan duoc. Xem [Lenh.PING].
     */
    fun guiPing(context: Context, xong: (KetQua) -> Unit = {}) =
        guiLenh(context, Lenh.PING, xong = xong)

    /** Nhan mot cau cho con. Tablet hien thanh thong bao co tieng. */
    fun guiTin(context: Context, chu: String, xong: (KetQua) -> Unit = {}) {
        val n = nha(context) ?: return xong(KetQua.Hong(THIEU_FIREBASE))
        n.collection(Duong.CHAT).add(
            mapOf(
                Duong.F_TU to TinChat.BA,
                Duong.F_CHU to chu,
                Duong.F_LUC to System.currentTimeMillis(),
                Duong.F_DA_DOC to false
            )
        ).addOnSuccessListener {
            // Gui lenh NHAN de tablet keu len ngay, khong doi lan doc tiep theo.
            guiLenh(context, Lenh.NHAN, chu = chu)
            xong(KetQua.Xong)
        }.addOnFailureListener { xong(KetQua.Hong(loiNguoiDoc(it))) }
    }

    /**
     * Xoa dot viec nha sau khi may nay da thay tablet cong gio.
     *
     * XOA CO DIEU KIEN, y het ben tablet. hop/viecnha la mot duong dan co dinh: ba
     * giao dot moi dung luc Ba Huy bam cong gio thi lenh xoa roi trung dot moi, va
     * may ba thay document bien mat se tuong tablet da nhan - trong khi chua ai nhan
     * ca, va ba thi khong con nut de gui lai.
     */
    fun xoaViecNha(context: Context, maPhien: String, xong: (KetQua) -> Unit = {}) {
        val h = hop(context, Duong.D_VIEC_NHA) ?: return xong(KetQua.Hong(THIEU_FIREBASE))
        h.firestore.runTransaction { tr ->
            val nay = tr.get(h)
            if (nay.exists() && nay.getString(Duong.F_MA_PHIEN) == maPhien) tr.delete(h)
            null
        }.addOnSuccessListener { xong(KetQua.Xong) }
            .addOnFailureListener {
                Log.w(TAG, "xoa viec nha hong", it)
                xong(KetQua.Hong(loiNguoiDoc(it)))
            }
    }

    // ---------------------------------------------------------------- rieng tu

    private fun nha(context: Context): DocumentReference? {
        val ma = Nha.maNha(context)
        if (ma.isEmpty()) return null
        return db(context)?.collection(Duong.NHA)?.document(ma)
    }

    private fun hop(context: Context, ten: String): DocumentReference? =
        nha(context)?.collection(Duong.HOP)?.document(ten)

    /**
     * Doi loi cua Firestore sang cau noi duoc viec phai lam.
     *
     * PERMISSION_DENIED gan nhu luc nao cung co mot nghia: may nay chua duoc ket
     * nap, hoac da bi go ra khoi nha. Cau tieng Anh goc thi doc xong khong biet
     * phai bam vao dau.
     */
    private fun loiNguoiDoc(loi: Exception): String {
        val chu = loi.message.orEmpty()
        return when {
            chu.contains("PERMISSION_DENIED", true) || chu.contains("permission", true) ->
                "Máy này chưa được tablet kết nạp. Vào Cài đặt để ghép đôi lại."
            chu.contains("UNAVAILABLE", true) || chu.contains("network", true) ->
                "Mất mạng. Lệnh sẽ tự gửi lại khi có mạng."
            else -> "Firestore báo: $chu"
        }
    }

    private const val THIEU_FIREBASE =
        "Bản app này chưa nối Firebase (thiếu google-services.json lúc build)."
}
