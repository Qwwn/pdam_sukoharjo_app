package com.example.pdam_app_v2

object Keys {
    init {
        System.loadLibrary("native-lib")
    }

    private external fun getNativeApiKey(): String

    val API_KEY: String = getNativeApiKey()
}