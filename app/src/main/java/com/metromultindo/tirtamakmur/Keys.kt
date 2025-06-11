package com.metromultindo.tirtamakmur

object Keys {
    init {
        System.loadLibrary("native-lib")
    }

    private external fun getNativeApiKey(): String

    val API_KEY: String = getNativeApiKey()
}