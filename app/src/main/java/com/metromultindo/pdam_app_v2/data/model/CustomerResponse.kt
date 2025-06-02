package com.metromultindo.pdam_app_v2.data.model

data class CustomerResponse(
    val error: Boolean,
    val message_code: Int,
    val message_text: String,
    val entity_title: String,
    val entity_address: String,
    val cust_code: String,
    val cust_name: String,
    val cust_address: String,
    val total_sheets: Int,
    val unpaid_sheets: String,
    val total_tagihan: Int,
    val cust_data: List<Bill>
)