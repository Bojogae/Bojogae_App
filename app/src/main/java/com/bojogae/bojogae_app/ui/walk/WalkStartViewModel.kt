package com.bojogae.bojogae_app.ui.walk

import android.content.Context
import android.view.Surface
import androidx.lifecycle.ViewModel
import com.bojogae.bojogae_app.analyzer.DistanceAnalyzer
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.common.UVCCameraHandler
import com.serenegiant.usb.widget.CameraViewInterface

class WalkStartViewModel: ViewModel() {






    private lateinit var distanceAnalyzer: DistanceAnalyzer    // 거리 측정 분석기

    fun initDistanceAnalyzer(context: Context) {
        distanceAnalyzer = DistanceAnalyzer(context)


    }




}