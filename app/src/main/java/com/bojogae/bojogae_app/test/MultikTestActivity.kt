package com.bojogae.bojogae_app.test

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.bojogae.bojogae_app.R
import com.bojogae.bojogae_app.utils.AppUtil
import org.opencv.android.OpenCVLoader
import org.opencv.core.CvType
import org.opencv.core.Mat


class MultikTestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multik_test)
        OpenCVLoader.initDebug()

        val objp = Mat(9 * 6, 1, CvType.CV_32FC3)
        var idx = 0
        for (i in 0 until 6) {
            for (j in 0 until 9) {
                objp.put(idx++, 0, j.toDouble(), i.toDouble(), 0.0)
            }
        }

        // Mat 객체 내용을 로그로 출력
        val buffer = FloatArray(3) // CV_32FC3 타입의 Mat은 3개의 float 값을 가짐
        val stringBuilder = StringBuilder()
        for (i in 0 until objp.rows()) {
            objp.get(i, 0, buffer)
            stringBuilder.append("[${buffer[0]}, ${buffer[1]}, ${buffer[2]}]\n")
        }

        Log.d(AppUtil.DEBUG_TAG, "objp contents:\n$stringBuilder")

    }
}