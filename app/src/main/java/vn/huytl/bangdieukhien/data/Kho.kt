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
                .set(mapOf("ma" to maGhep.trim(), "luc" to System.currentTimeMillis()))
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
