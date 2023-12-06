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

    fun initApp() : Boolean {
        if (!OpenCVLoader.initDebug()) {
            Log.d(DEBUG_TAG, "OpenCV Error")    // opencv를 초기화하고 초기화하지 못하면 앱을 종료
            return false
        } else {
            Log.d(DEBUG_TAG, "OpenCV Success")



        }
        return true
    }
}


