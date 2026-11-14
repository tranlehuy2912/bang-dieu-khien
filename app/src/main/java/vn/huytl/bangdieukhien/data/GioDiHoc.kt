package vn.huytl.bangdieukhien.data

import java.util.Calendar

/** Khoang khoa mot thu: [tu] toi [toi], phut tinh tu 0 gio, [tu] nho hon [toi]. */
data class NgayKhoa(val bat: Boolean, val tu: Int, val toi: Int)

/**
 * Gio di hoc theo tung thu (anh Huy chot 14/11/2026), xem [Duong.F_KHOA_HOC]: moi thu mot khoang
 * khoa, bat tat tung thu. Tablet va laptop moi may mot bo rieng, anh Huy tu chinh cho giong nhau:
 * bo cua tablet doc o hop/caidat va doi bang lenh CAIDAT (tablet la nguon), bo cua laptop may nay
 * ghi thang vao laptop/{maNha}.
 *
 * Gia tri luc moi cai phai giong ben tablet (data/KhoaHoc.kt cua nop-bai) va ben laptop
 * (tools/laptop/netflix_gio.py): may nao chua co cai dat thi khoa theo do.
 *
 * Thu la so cua Calendar.DAY_OF_WEEK: 1 chu nhat, 2 thu hai ... 7 thu bay.
 */
object GioDiHoc {

    /** Thu hai, thu sau 8:45-17:00; thu ba, tu, nam, bay 11:00-17:00; chu nhat khong khoa. */
    val MAC_DINH: Map<Int, NgayKhoa> = mapOf(
        Calendar.SUNDAY to NgayKhoa(false, 11 * 60, 17 * 60),
        Calendar.MONDAY to NgayKhoa(true, 8 * 60 + 45, 17 * 60),
        Calendar.TUESDAY to NgayKhoa(true, 11 * 60, 17 * 60),
        Calendar.WEDNESDAY to NgayKhoa(true, 11 * 60, 17 * 60),
        Calendar.THURSDAY to NgayKhoa(true, 11 * 60, 17 * 60),
        Calendar.FRIDAY to NgayKhoa(true, 8 * 60 + 45, 17 * 60),
        Calendar.SATURDAY to NgayKhoa(true, 11 * 60, 17 * 60),
    )

    /** Thu hai truoc, chu nhat sau cung, nhu tren lich. */
    val THU_TU = listOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
        Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY,
    )

    /**
     * Doc truong khoaHoc (hop/caidat hay laptop/{maNha}). null khi khong co truong do: tablet,
     * laptop ban cu, hay Ba Huy chua dat lan nao. Thu nao thieu hay sai thi lay [MAC_DINH].
     */
    fun doc(o: Any?): Map<Int, NgayKhoa>? {
        val m = o as? Map<*, *> ?: return null
        return MAC_DINH.mapValues { (thu, md) ->
            val n = m[thu.toString()] as? Map<*, *> ?: return@mapValues md
            val bat = n[Duong.F_BAT] as? Boolean ?: md.bat
            val tu = (n[Duong.F_TU] as? Number)?.toInt() ?: md.tu
            val toi = (n[Duong.F_TOI] as? Number)?.toInt() ?: md.toi
            if (hopLe(tu, toi)) NgayKhoa(bat, tu, toi) else md
        }
    }

    /** Dang ghi Firestore, cung dang tablet doc trong lenh CAIDAT khoaHoc. */
    fun luuFirestore(ds: Map<Int, NgayKhoa>): Map<String, Map<String, Any>> =
        ds.toSortedMap().entries.associate { (thu, k) ->
            thu.toString() to mapOf(Duong.F_BAT to k.bat, Duong.F_TU to k.tu, Duong.F_TOI to k.toi)
        }

    fun hopLe(tu: Int, toi: Int): Boolean = tu in 0 until 24 * 60 && toi in 1..24 * 60 && tu < toi

    /** Thu dau tien co gio mo lai khong sau gio khoa, null neu deu dung. */
    fun thuSai(ds: Map<Int, NgayKhoa>): Int? =
        THU_TU.firstOrNull { thu -> ds[thu]?.let { it.bat && !hopLe(it.tu, it.toi) } == true }

    /** Chu ben phai dong "Giờ đi học": "6 ngày", "Không khoá". */
    fun tomTat(ds: Map<Int, NgayKhoa>): String {
        val n = ds.values.count { it.bat }
        return if (n == 0) "Không khoá" else "$n ngày"
    }

    fun tenThu(thu: Int): String = when (thu) {
        Calendar.MONDAY -> "Thứ hai"
        Calendar.TUESDAY -> "Thứ ba"
        Calendar.WEDNESDAY -> "Thứ tư"
        Calendar.THURSDAY -> "Thứ năm"
        Calendar.FRIDAY -> "Thứ sáu"
        Calendar.SATURDAY -> "Thứ bảy"
        else -> "Chủ nhật"
    }
}
