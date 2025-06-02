package com.example.pdam_app_v2.data.model

import com.google.gson.annotations.SerializedName

data class Bill(
    @SerializedName("NOSAMBUNG") val customerNumber: String,
    @SerializedName("NAMA") val name: String,
    @SerializedName("ALAMAT") val address: String,
    @SerializedName("STATUS") val status: String,
    @SerializedName("BULAN") val period: Int,
    @SerializedName("KLS_TARIP") val tariffClass: String,
    @SerializedName("STAND_AWAL") val startMeter: Int,
    @SerializedName("STAND_AKHIR") val endMeter: Int,
    @SerializedName("PHOTO_FILE") val photoUrl: String?,
    @SerializedName("PEMAKAIANM3") val usage: Int,
    @SerializedName("TAGIHAN_RP") val billAmount: Int,
    @SerializedName("TGLBAYAR") val paymentDate: String?,
    @SerializedName("BLOK1_RP") val block1_RP: Int,
    @SerializedName("BLOK2_RP") val block2_RP: Int,
    @SerializedName("BLOK3_RP") val block3_RP: Int,
    @SerializedName("BLOK4_RP") val block4_RP: Int,
    @SerializedName("ADM_RP") val adm_rp: Int,
    @SerializedName("DWM_RP") val dmw_rp: Int,
    @SerializedName("ABONEMEN_RP") val abonemen_rp: Int,
    @SerializedName("PPN_RP") val ppn_rp: Int,
    @SerializedName("DENDA_RP") val denda_rp: Int,
    @SerializedName("DISCOUNT_RP") val discount_rp: Int,
    @SerializedName("ANGSURAN_RP") val angsuran_rp: Int,
    @SerializedName("ANGSURAN_KE") val angsuran_ke: String,
    )