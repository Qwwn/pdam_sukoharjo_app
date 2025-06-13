// SelfMeterModels.kt
package com.metromultindo.tirtamakmur.data.model

import com.google.gson.annotations.SerializedName

data class SelfMeterResponse(
    @SerializedName("error") val error: Boolean,
    @SerializedName("message_code") val messageCode: Int,
    @SerializedName("message_text") val messageText: String,
    @SerializedName("self_meter") val selfMeter: SelfMeterData?
)

data class SelfMeterData(
    @SerializedName("id") val id: Int,
    @SerializedName("cust_code") val custCode: String,
    @SerializedName("stand_meter") val standMeter: Int,
    @SerializedName("date") val date: String,
    @SerializedName("image") val image: String?,
    @SerializedName("latlong") val latlong: String?,
    @SerializedName("location_status") val locationStatus: String
)

data class SelfMeterRequest(
    val custCode: String,
    val standMeter: Int,
    val latitude: Double?,
    val longitude: Double?,
    val imageUri: android.net.Uri?,
    val cameraFilePath: String?
)

// Data untuk tampilan customer info di self meter screen
data class SelfMeterCustomerInfo(
    val custCode: String,
    val name: String,
    val address: String,
    val tariffClass: String,
    val status: String,
    val startMeter: String,
    val phone: String

)