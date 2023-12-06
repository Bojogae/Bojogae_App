package com.bojogae.bojogae_app.utils

import android.util.Log
import org.opencv.android.OpenCVLoader

object AppUtil {
    const val DEBUG_TAG = "test_device"
    const val DEFAULT_WIDTH = 640
    const val DEFAULT_HEIGHT = 480

    fun log(tag: String, message: String) {
        Log.d(tag, message)
    }

    fun ld(message: String) {
        Log.d(DEBUG_TAG, message)
    }

}


