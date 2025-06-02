package com.metromultindo.pdam_app_v2.data.model

import com.google.gson.annotations.SerializedName

data class FCMResponse(
    @SerializedName("error")
    val error: Boolean = false,

    @SerializedName("message_code")
    val messageCode: Int = 0,

    @SerializedName("message_text")
    val messageText: String = "",

    @SerializedName("data")
    val data: FCMData? = null
)

data class FCMData(
    @SerializedName("token_id")
    val tokenId: String? = null,

    @SerializedName("device_id")
    val deviceId: String? = null,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("registered_at")
    val registeredAt: String? = null
)