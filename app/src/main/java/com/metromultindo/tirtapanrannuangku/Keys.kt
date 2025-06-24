package com.metromultindo.tirtapanrannuangku

object Keys {
    init {
        System.loadLibrary("native-lib")
    }

    private external fun getNativeApiKey(): String
    external fun getNativeBaseUrl(): String

    val API_KEY: String = getNativeApiKey()
    val BASE_URL: String = getNativeBaseUrl()

}