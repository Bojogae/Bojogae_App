package com.bojogae.bojogae_app.test

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.bojogae.bojogae_app.R
import com.bojogae.bojogae_app.utils.AppUtil
import org.opencv.android.OpenCVLoader
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.videoio.VideoCapture


class MultikTestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multik_test)
        OpenCVLoader.initDebug()

        val videoCapture = VideoCapture()



    }
}