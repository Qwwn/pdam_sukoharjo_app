package com.metromultindo.tirtamakmur.data.model

import com.google.gson.annotations.SerializedName

data class CustomerResponse(
    val error: Boolean,
    val message_code: Int,
    val message_text: String,
    val entity_title: String,
    val entity_address: String,
    val cust_code: String,
    val cust_name: String,
    val cust_address: String,
    val cust_phone: String,
    val tarif_class: String,
    val total_sheets: Int,
    val unpaid_sheets: String,
    val total_tagihan: Int,
    val cust_data: List<Bill>
)

data class UpdatePhoneResponse(
    @SerializedName("error") val error: Boolean,
    @SerializedName("message_code") val messageCode: Int,
    @SerializedName("message_text") val messageText: String,
    @SerializedName("cust_code") val custCode: String?,
    @SerializedName("cust_phone") val custPhone: String?
)

data class UpdatePhoneRequest(
    val nosambung: String,
    val phone: String
)