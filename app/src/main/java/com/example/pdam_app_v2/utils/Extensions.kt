package com.example.pdam_app_v2.utils

import android.content.Context
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.pdam_app_v2.R
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale

fun getErrorMessage(errorCode: Int, defaultMessage: String): String {
    return when (errorCode) {
        -1 -> "Terjadi kesalahan koneksi. Silakan coba lagi nanti."
        100 -> "Request data sukses"
        101 -> "Pelanggan tidak memiliki tagihan"
        102 -> "Pelanggan tidak ditemukan"
        103 -> "Tagihan lebih dari 3 bulan. Silahkan hubungi kantor PDAM terdekat."
        104 -> "Data tidak ditemukan. Silahkan periksa parameter dan kriteria-nya"
        105 -> "Kolektor tidak memiliki tagihan"
        600 -> "Access Denied. Invalid Api key"
        601 -> "Api key is missing or user on suspended"
        900 -> "Pelanggan tidak aktif. Silahkan hubungi Unit/Cabang PDAM Terdekat."
        else -> defaultMessage
    }
}

fun formatCurrency(amount: Int): String {
    val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    format.maximumFractionDigits = 0
    return format.format(amount.toLong()).replace("Rp", "Rp. ")
}

fun formatPeriod(period: Int): String {
    val year = period / 100
    val month = period % 100

    val monthName = when (month) {
        1 -> "Januari"
        2 -> "Februari"
        3 -> "Maret"
        4 -> "April"
        5 -> "Mei"
        6 -> "Juni"
        7 -> "Juli"
        8 -> "Agustus"
        9 -> "September"
        10 -> "Oktober"
        11 -> "November"
        12 -> "Desember"
        else -> "Unknown"
    }

    return "$monthName $year"
}

fun String.encodeForNavigation(): String {
    return URLEncoder.encode(this, "UTF-8")
}

fun String?.encodeForNavigationOrEmpty(): String {
    return this?.encodeForNavigation() ?: ""
}