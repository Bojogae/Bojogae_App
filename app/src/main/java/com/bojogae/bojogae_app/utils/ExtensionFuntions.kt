package com.bojogae.bojogae_app.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import org.opencv.core.Mat

fun Context.toast( msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}


fun Mat.contents(): String {
    val channels = this.channels()
    val buffer = FloatArray(this.cols() * channels) // 채널 수에 따라 배열 크기 동적 조정
    val stringBuilder = StringBuilder()

    for (i in 0 until this.rows()) {
        this.get(i, 0, buffer)
        stringBuilder.append("[")

        for (j in 0 until this.cols() * channels step channels) {
            stringBuilder.append("(")
            for (c in 0 until channels) {
                stringBuilder.append("${buffer[j + c]}")
                if (c < channels - 1) stringBuilder.append(", ")
            }
            stringBuilder.append(") ")
        }
        stringBuilder.append("]\n")
    }
    return stringBuilder.toString()
}