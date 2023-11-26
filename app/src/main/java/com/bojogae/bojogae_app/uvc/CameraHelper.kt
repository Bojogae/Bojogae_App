package com.bojogae.bojogae_app.uvc

import android.app.Activity
import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import com.bojogae.bojogae_app.utils.AppUtil
import com.github.jiangdongguo.R
import com.serenegiant.usb.DeviceFilter
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.UVCCamera
import com.serenegiant.usb.common.AbstractUVCCameraHandler.OnPreViewResultListener
import com.serenegiant.usb.common.UVCCameraHandler
import com.serenegiant.usb.widget.CameraViewInterface
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CameraHelper {
    private var frameFormat = UVCCamera.FRAME_FORMAT_YUYV
    private lateinit var usbMonitor: USBMonitor
    private lateinit var cameraHandler: UVCCameraHandler
    private lateinit var ctrlBlock: USBMonitor.UsbControlBlock
    private lateinit var cameraView: CameraViewInterface
    private lateinit var activity: Activity

    private val previewWidth = 640
    private val previewHeight = 480


    interface OnDevConnectListener {
        fun onAttachDev(device: UsbDevice)

        fun onDettachDev(device: UsbDevice)

        fun onConnectDev(device: UsbDevice, isConnected: Boolean)

        fun onDisConnectDev(device: UsbDevice)
    }


    @OptIn(DelicateCoroutinesApi::class)
    fun initUSBMonitor(activity: Activity, cameraView: CameraViewInterface, listener: OnDevConnectListener) {
        this.activity = activity
        this.cameraView = cameraView

        usbMonitor = USBMonitor(activity.applicationContext, object : USBMonitor.OnDeviceConnectListener {
            override fun onAttach(device: UsbDevice) {  // called by checking usb device
                listener.onAttachDev(device)
            }

            override fun onDettach(device: UsbDevice) {  // called by taking out usb device
                listener.onDettachDev(device)
            }

            override fun onConnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock, createNew: Boolean) { // called by connect to usb camera
                this@CameraHelper.ctrlBlock = ctrlBlock
                openCamera(ctrlBlock)
                GlobalScope.launch(Dispatchers.Main) {
                    try {
                        delay(500) // 대기 시간 500ms
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    // 카메라 생성 후 프리뷰 시작
                    startPreview(this@CameraHelper.cameraView)
                }
            }

            override fun onDisconnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock?) {  // called by disconnect to usb camera
                listener.onDisConnectDev(device)
            }

            override fun onCancel(device: UsbDevice) {

            }

        })

        createUVCCamera()
    }

    private fun createUVCCamera() {
        cameraView.aspectRatio = previewWidth / previewHeight.toDouble()
        cameraHandler = UVCCameraHandler.createHandler(activity, cameraView, 2,
            previewWidth, previewHeight, frameFormat)
        Log.d(AppUtil.DEBUG_TAG, "createUVCCamera")

    }

    private fun openCamera(ctrlBlock: USBMonitor.UsbControlBlock) {
        Log.d(AppUtil.DEBUG_TAG, "openCamera")
        cameraHandler.open(ctrlBlock)
    }

    fun startPreview(cameraView: CameraViewInterface) {
        Log.d(AppUtil.DEBUG_TAG, "startPreview")
        cameraHandler.startPreview(cameraView.surfaceTexture)
    }

    fun stopPreview() {
        cameraHandler.stopPreview()
    }

    fun closeCamera() {
        cameraHandler.close()
    }

    fun setDefaultFrameFormat(format: Int) {
        frameFormat = format
    }

    fun release() {
        Log.d(AppUtil.DEBUG_TAG, "release")
        cameraHandler.release()
        usbMonitor.destroy()
    }

    fun registerUSB() {
        usbMonitor.register()
        Log.d(AppUtil.DEBUG_TAG, "registerUSB")
    }

    fun unRegisterUSB() {
        usbMonitor.unregister()
        Log.d(AppUtil.DEBUG_TAG, "unRegisterUSB")
    }

    fun isCameraOpened() : Boolean {
        return cameraHandler.isOpened
    }

    fun setOnPreviewFrameListener(listener: OnPreViewResultListener) {
        cameraHandler.setOnPreViewResultListener(listener)
    }

    fun requestPermission(index: Int) {
        val usbManager = activity.getSystemService(Context.USB_SERVICE) as UsbManager
        usbMonitor.requestPermission(getUsbDeviceList()?.get(index))
    }

    private fun getUsbDeviceList(): List<UsbDevice?>? {
        val deviceFilters = DeviceFilter.getDeviceFilters(activity.applicationContext, R.xml.device_filter)
        return if (deviceFilters == null) null else usbMonitor.getDeviceList(deviceFilters) // matching all of filter devices
    }

}