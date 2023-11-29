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



    }
}