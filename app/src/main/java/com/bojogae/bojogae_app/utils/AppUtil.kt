package com.bojogae.bojogae_app.utils

import android.util.Log

object AppUtil {
    const val DEBUG_TAG = "test_device"

    fun log(tag: String, message: String) {
        Log.d(tag, message)
    }
}