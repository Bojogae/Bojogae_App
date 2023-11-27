package com.bojogae.bojogae_app.utils

import android.util.Log

object AppUtil {
    const val DEBUG_TAG = "test_device"
    const val DEFAULT_WIDTH = 1920
    const val DEFAULT_HEIGHT = 1080

    fun log(tag: String, message: String) {
        Log.d(tag, message)
    }
}