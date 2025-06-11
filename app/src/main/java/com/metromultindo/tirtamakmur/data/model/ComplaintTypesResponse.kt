package com.metromultindo.tirtamakmur.data.model

data class ComplaintTypesResponse(
    val error: Boolean,
    val message_code: Int,
    val message_text: String,
    val count: Int,
    val complaintTypes: List<ComplaintType>
)

data class ComplaintType(
    val cmp_type: Int,
    val cmp_code: String,
    val cmp_desc: String,
    val parentid: Int?,
    val urut: Int,
    val cmp_level: Int
)