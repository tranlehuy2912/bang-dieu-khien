package vn.huytl.bangdieukhien.telegram

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Tai anh bai tap tu Telegram ve.
 *
 * VI SAO ANH NAM O TELEGRAM: Firebase Storage bay gio bat bat thanh toan moi dung
 * duoc, ma viec nay khong dang phai nhap the. Tablet van gui anh len Telegram y
 * nhu truoc, roi chi ghi file_id xuong Firestore. Cam file_id la tai ve duoc.
 *
 * App nay chi goi getFile va tai file. KHONG BAO GIO goi getUpdates: Telegram chi
 * cho mot may nghe mot bot, goi o day la giat mat ket noi cua tablet (loi 409) va
 * ca he thong dung lenh.
 *
 * Anh tai ve nam trong cacheDir. He dieu hanh don cho do khi may het bo nho, nen
 * khong phai tu xoa; mat thi lan sau tai lai.
 */
object TaiAnh {

    private const val TAG = "BangDieuKhien"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    /**
     * Tra ve file anh da nam tren may, tai ve neu chua co.
     *
     * Chay tren Dispatchers.IO. Tra null khi khong tai duoc - hong o day chi lam
     * mot o anh trong, khong dang de man hinh do vi mot tam.
     */
    suspend fun lay(context: Context, token: String, fileId: String): File? =
        withContext(Dispatchers.IO) {
            if (token.isBlank() || fileId.isBlank()) return@withContext null

            val dich = File(thuMuc(context), fileId.hashCode().toString() + ".jpg")
            if (dich.exists() && dich.length() > 0) return@withContext dich

            runCatching {
                val duong = hoiDuongDan(token, fileId) ?: return@runCatching null
                val req = Request.Builder()
                    .url("https://api.telegram.org/file/bot$token/$duong")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@runCatching null
                    // Ghi ra file tam roi doi ten: tai giua chung mat mang thi con
                    // lai mot file cut, ma lan sau ham nay thay file do la tuong da
                    // co anh va tra ve mot tam hong mai mai.
                    val tam = File(dich.absolutePath + ".tam")
                    resp.body.byteStream().use { vao ->
                        tam.outputStream().use { ra -> vao.copyTo(ra) }
                    }
                    if (tam.renameTo(dich)) dich else tam
                }
            }.onFailure { Log.w(TAG, "tai anh hong: ${it.message}") }.getOrNull()
        }

    /** Doc anh ra bitmap, thu nho cho vua o hien. */
    suspend fun doc(file: File, rongToiDa: Int): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val do1 = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, do1)
            val do2 = BitmapFactory.Options().apply {
                inSampleSize = tiLe(do1.outWidth, rongToiDa)
            }
            BitmapFactory.decodeFile(file.absolutePath, do2)
        }.getOrNull()
    }

    /**
     * getFile tra ve mot duong dan song duoc mot tieng.
     *
     * Khong luu lai duong nay: het han thi tai ve ra mot trang HTML bao loi chu
     * khong phai anh, ma trong lai giong mot file binh thuong. Goi lai moi lan re
     * hon nhieu so voi mot tam anh hong khong biet vi sao.
     */
    private fun hoiDuongDan(token: String, fileId: String): String? {
        val req = Request.Builder()
            .url("https://api.telegram.org/bot$token/getFile?file_id=$fileId")
            .build()
        client.newCall(req).execute().use { resp ->
            val json = JSONObject(resp.body.string())
            if (!json.optBoolean("ok")) {
                Log.w(TAG, "getFile: ${json.optString("description")}")
                return null
            }
            return json.optJSONObject("result")?.optString("file_path")?.takeIf { it.isNotBlank() }
        }
    }

    private fun tiLe(rongThat: Int, rongMuon: Int): Int {
        var ti = 1
        while (rongThat / ti > rongMuon * 2) ti *= 2
        return ti
    }

    private fun thuMuc(context: Context): File =
        File(context.cacheDir, "anh").apply { mkdirs() }
}
