package vn.huytl.bangdieukhien.ui

import android.app.TimePickerDialog
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import vn.huytl.bangdieukhien.R
import vn.huytl.bangdieukhien.data.GioDiHoc
import vn.huytl.bangdieukhien.data.NgayKhoa

/**
 * Hop sua gio di hoc ca tuan (anh Huy chon mau B ngay 14/11/2026, trong hai mau ve o
 * mau-gio-di-hoc.html): bay thu trong mot hop, moi thu mot cong tac va hai o gio "khoa luc",
 * "mo lai luc". Sua xong bam Luu moi gui mot lan, vi thoi khoa bieu doi thi thuong doi nhieu thu
 * cung luc. Dung chung cho tablet va laptop, moi may mot bo.
 *
 * Bam o gio thi hien dong ho chon gio nhu "Giờ ngủ". Thu dang tat thi hai o mo di, khong bam duoc.
 * Luu ma con thu nao gio mo lai khong sau gio khoa thi bao va giu hop lai.
 */
object HopGioDiHoc {

    fun mo(
        context: Context,
        tieuDe: String,
        dangCo: Map<Int, NgayKhoa>,
        luu: (Map<Int, NgayKhoa>) -> Unit
    ) {
        val ds = GioDiHoc.MAC_DINH.toMutableMap().apply { putAll(dangCo) }
        val dp = context.resources.displayMetrics.density
        fun Int.dp() = (this * dp).toInt()

        val than = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dp(), 0, 16.dp(), 4.dp())
        }
        than.addView(TextView(context).apply {
            text = "Ngày nghỉ trong lịch và ngày Ba Huy bấm \"Cho nghỉ học\" thì không khoá."
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(0, 0, 0, 8.dp())
        })

        val mauChu = MaterialColors.getColor(than, androidx.appcompat.R.attr.colorPrimary)
        val mauVien = MaterialColors.getColor(than, com.google.android.material.R.attr.colorOutlineVariant)

        for (thu in GioDiHoc.THU_TU) {
            val hang = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 4.dp(), 0, 4.dp())
            }
            val ten = TextView(context).apply {
                text = GioDiHoc.tenThu(thu)
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
            }
            val cong = MaterialSwitch(context)
            fun o(): TextView = TextView(context).apply {
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
                setTextColor(mauChu)
                setPadding(10.dp(), 6.dp(), 10.dp(), 6.dp())
                background = GradientDrawable().apply {
                    cornerRadius = 10f * dp
                    setStroke(1.dp().coerceAtLeast(1), mauVien)
                }
            }
            val tu = o()
            val toi = o()
            val gach = TextView(context).apply {
                text = "–"
                setPadding(6.dp(), 0, 6.dp(), 0)
            }

            fun ve() {
                val k = ds.getValue(thu)
                cong.isChecked = k.bat
                tu.text = Dinh.gio(k.tu)
                toi.text = Dinh.gio(k.toi)
                listOf(tu, toi, gach).forEach { it.alpha = if (k.bat) 1f else 0.35f }
                tu.isEnabled = k.bat
                toi.isEnabled = k.bat
                hang.contentDescription = "${GioDiHoc.tenThu(thu)}, " +
                    if (k.bat) "khoá từ ${tu.text} tới ${toi.text}" else "không khoá"
            }
            cong.setOnCheckedChangeListener { _, bat ->
                if (ds.getValue(thu).bat != bat) {
                    ds[thu] = ds.getValue(thu).copy(bat = bat)
                    ve()
                }
            }
            tu.setOnClickListener {
                hoiGio(context, ds.getValue(thu).tu) { ds[thu] = ds.getValue(thu).copy(tu = it); ve() }
            }
            toi.setOnClickListener {
                hoiGio(context, ds.getValue(thu).toi) { ds[thu] = ds.getValue(thu).copy(toi = it); ve() }
            }
            ve()

            hang.addView(ten, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            hang.addView(cong)
            hang.addView(View(context), LinearLayout.LayoutParams(8.dp(), 1))
            hang.addView(tu)
            hang.addView(gach)
            hang.addView(toi)
            than.addView(hang)
        }

        val hop = MaterialAlertDialogBuilder(context)
            .setTitle(tieuDe)
            .setView(ScrollView(context).apply { addView(than) })
            .setNegativeButton(R.string.huy, null)
            .setPositiveButton("Lưu", null)
            .show()
        // Nut Luu tu xet roi moi dong hop: sai gio thi giu hop de sua, khong mat cac thu da sua.
        hop.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val sai = GioDiHoc.thuSai(ds)
            if (sai != null) {
                Dinh.noi(context, "${GioDiHoc.tenThu(sai)}: giờ mở lại phải sau giờ khoá.")
                return@setOnClickListener
            }
            hop.dismiss()
            if (ds != dangCo) luu(ds.toMap())
        }
    }

    private fun hoiGio(context: Context, dangLa: Int, xong: (Int) -> Unit) {
        TimePickerDialog(
            context,
            { _, gio, phut -> xong(gio * 60 + phut) },
            dangLa / 60, dangLa % 60, true
        ).show()
    }
}
