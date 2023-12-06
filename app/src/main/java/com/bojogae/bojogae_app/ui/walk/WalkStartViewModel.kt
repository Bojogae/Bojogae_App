package com.bojogae.bojogae_app.ui.walk

import android.app.Activity
import android.content.Context
import android.hardware.usb.UsbDevice
import androidx.lifecycle.ViewModel
import com.bojogae.bojogae_app.analyzer.DistanceAnalyzer
import com.bojogae.bojogae_app.listener.OnCameraDistanceListener
import com.bojogae.bojogae_app.utils.AppUtil
import com.serenegiant.usb.DeviceFilter
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener
import com.serenegiant.usb.UVCCamera
import com.serenegiant.usb.common.UVCCameraHandler
import com.serenegiant.usb.widget.CameraViewInterface

class WalkStartViewModel: ViewModel() {



    private var analyzerInitialized = false
    private lateinit var distanceAnalyzer: DistanceAnalyzer    // 거리 측정 분석기

    private var usbMonitorInitialized = false
    private lateinit var usbMonitor: USBMonitor
    lateinit var leftCameraHandler: UVCCameraHandler
    lateinit var rightCameraHandler: UVCCameraHandler



    /**
     * 거리 측정기를 초기화
     * 뷰모델 한번 생성 시 한 번만 초기화
     * 두 번 이상 초기화 필요x
     */
    fun initDistanceAnalyzer(activity: Activity, onCameraDistanceListener: OnCameraDistanceListener) : DistanceAnalyzer {
        if (!analyzerInitialized) {
            distanceAnalyzer = DistanceAnalyzer(activity, onCameraDistanceListener)
            AppUtil.ld("init distance analyzer")
            analyzerInitialized = true
        }
        return distanceAnalyzer
    }

    fun initUSBMonitor(activity: Activity, connectListener: OnDeviceConnectListener) : USBMonitor {
        usbMonitor = USBMonitor(activity, connectListener)
        return usbMonitor
    }

    /**
     * 연결된 웹캠 카메라와 상호작용 하기 위한 핸들러 초기화
     */
    fun initHandler(activity: Activity, leftView: CameraViewInterface, rightView: CameraViewInterface) {
        leftCameraHandler = UVCCameraHandler.createHandler(activity, leftView, 2,
            AppUtil.DEFAULT_WIDTH, AppUtil.DEFAULT_HEIGHT, UVCCamera.PIXEL_FORMAT_RGB565)

        rightCameraHandler = UVCCameraHandler.createHandler(activity, rightView, 2,
            AppUtil.DEFAULT_WIDTH, AppUtil.DEFAULT_HEIGHT, UVCCamera.PIXEL_FORMAT_RGB565)

    }

    fun initDeviceList(context: Context): List<UsbDevice> {
        val filteredDeviceList =
            DeviceFilter.getDeviceFilters(context, com.github.jiangdongguo.R.xml.device_filter)
        return usbMonitor.getDeviceList(filteredDeviceList[0])

    }






}