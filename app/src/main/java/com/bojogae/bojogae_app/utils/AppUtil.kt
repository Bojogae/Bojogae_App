package com.bojogae.bojogae_app.utils

import android.util.Log

object AppUtil {
    const val DEBUG_TAG = "test_device"
    const val DEFAULT_WIDTH = 640
    const val DEFAULT_HEIGHT = 480

    fun log(tag: String, message: String) {
        Log.d(tag, message)
    }
}