package com.bojogae.bojogae_app.test

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Surface
import com.bojogae.bojogae_app.databinding.ActivityObjectDistanceBinding
import com.bojogae.bojogae_app.utils.AppControlUtil
import com.bojogae.bojogae_app.utils.toast
import com.bojogae.bojogae_app.uvc.CameraHelper
import com.jiangdg.usbcamera.utils.FileUtils
import com.serenegiant.usb.UVCCamera
import com.serenegiant.usb.widget.CameraViewInterface
import com.serenegiant.usb.widget.UVCCameraTextureView
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ObjectDistanceActivity : AppCompatActivity(){

    private lateinit var viewBinding: ActivityObjectDistanceBinding
    private lateinit var cameraHelperTop: CameraHelper
    private lateinit var cameraHelperBottom: CameraHelper

    private lateinit var uvcCameraViewTop: UVCCameraTextureView
    private lateinit var uvcCameraViewBottom: UVCCameraTextureView

    private var isTopRequest = false
    private var isBottomRequest = false

    private var isTopPreview = false
    private var isBottomPreview = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityObjectDistanceBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        initializeUVCCameraHelper()

        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        Log.d(AppControlUtil.DEBUG_TAG, usbManager.deviceList.keys.toString())
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun initializeUVCCameraHelper() {
        // 카메라 탑 뷰 설정
        uvcCameraViewTop = viewBinding.cameraViewTop
        uvcCameraViewTop.setCallback(object : CameraViewInterface.Callback {
            override fun onSurfaceCreated(view: CameraViewInterface?, surface: Surface?) {
                if (!isTopPreview && cameraHelperTop.isCameraOpened()) {
                    cameraHelperTop.startPreview(uvcCameraViewTop)
                    isTopPreview = true
                }
            }

            override fun onSurfaceChanged(view: CameraViewInterface?, surface: Surface?, width: Int, height: Int) {

            }

            override fun onSurfaceDestroy(view: CameraViewInterface?, surface: Surface?) {
                if (!isTopPreview && cameraHelperTop.isCameraOpened()) {
                    cameraHelperTop.stopPreview()
                    isTopPreview = false
                }
            }
        })

        // 카메라 바텀 뷰 설정
        uvcCameraViewBottom = viewBinding.cameraViewBottom
        uvcCameraViewBottom.setCallback(object : CameraViewInterface.Callback {
            override fun onSurfaceCreated(view: CameraViewInterface?, surface: Surface?) {
                if (!isBottomPreview && cameraHelperBottom.isCameraOpened()) {
                    cameraHelperBottom.startPreview(uvcCameraViewBottom)
                    isBottomPreview = true
                }
            }

            override fun onSurfaceChanged(view: CameraViewInterface?, surface: Surface?, width: Int, height: Int) {

            }

            override fun onSurfaceDestroy(view: CameraViewInterface?, surface: Surface?) {
                if (!isBottomPreview && cameraHelperBottom.isCameraOpened()) {
                    cameraHelperBottom.stopPreview()
                    isBottomPreview = false
                }
            }
        })

        // 탑 카메라 헬퍼 설정
        cameraHelperTop = CameraHelper()
        cameraHelperTop.setDefaultFrameFormat(UVCCamera.FRAME_FORMAT_YUYV)
        cameraHelperTop.initUSBMonitor(this@ObjectDistanceActivity, uvcCameraViewTop, object : CameraHelper.OnDevConnectListener {
            override fun onAttachDev(device: UsbDevice) {
                if (!isTopRequest) {
                    isTopRequest = true
                    cameraHelperTop.requestPermission(0)
                }
            }

            override fun onDettachDev(device: UsbDevice) {
                if (isTopRequest) {
                    isTopRequest = false
                    cameraHelperTop.closeCamera()
                    this@ObjectDistanceActivity.toast("${device.deviceName} 종료")
                }
            }

            override fun onConnectDev(device: UsbDevice, isConnected: Boolean) {
                isTopPreview = if (!isConnected) {
                    this@ObjectDistanceActivity.toast("연결 실패")
                    false
                } else {
                    this@ObjectDistanceActivity.toast("연결 성공")
                    GlobalScope.launch {
                        delay(2500)
                    }
                    true
                }
            }

            override fun onDisConnectDev(device: UsbDevice) {
                this@ObjectDistanceActivity.toast("연결 종료")
            }

        })
        cameraHelperTop.setOnPreviewFrameListener { yuv ->
            Log.d(AppControlUtil.DEBUG_TAG, yuv.toString())
            Log.d(AppControlUtil.DEBUG_TAG, "top")
        }


        // 바텀 카메라 헬퍼 설정
        cameraHelperBottom = CameraHelper()
        cameraHelperBottom.setDefaultFrameFormat(UVCCamera.FRAME_FORMAT_YUYV)
        cameraHelperBottom.initUSBMonitor(this@ObjectDistanceActivity, uvcCameraViewBottom, object : CameraHelper.OnDevConnectListener {
            override fun onAttachDev(device: UsbDevice) {
                if (!isBottomRequest) {
                    isBottomRequest = true
                    cameraHelperTop.requestPermission(0)
                }
            }

            override fun onDettachDev(device: UsbDevice) {
                if (isBottomRequest) {
                    isBottomRequest = false
                    cameraHelperBottom.closeCamera()
                    this@ObjectDistanceActivity.toast("${device.deviceName} 종료")
                }
            }

            override fun onConnectDev(device: UsbDevice, isConnected: Boolean) {
                isBottomPreview = if (!isConnected) {
                    this@ObjectDistanceActivity.toast("연결 실패")
                    false
                } else {
                    this@ObjectDistanceActivity.toast("연결 성공")
                    GlobalScope.launch {
                        delay(2500)
                    }
                    true
                }
            }

            override fun onDisConnectDev(device: UsbDevice) {
                this@ObjectDistanceActivity.toast("연결 종료")
                Log.d(AppControlUtil.DEBUG_TAG, "${device.deviceName} 종료")
            }

        })
        cameraHelperBottom.setOnPreviewFrameListener { yuv ->
            Log.d(AppControlUtil.DEBUG_TAG, yuv.toString())
            Log.d(AppControlUtil.DEBUG_TAG, "bottom")
        }
    }


    override fun onStart() {
        super.onStart()
        cameraHelperTop.registerUSB()
        cameraHelperBottom.registerUSB()
    }

    override fun onStop() {
        super.onStop()
        cameraHelperTop.unRegisterUSB()
        cameraHelperBottom.unRegisterUSB()
    }

    override fun onDestroy() {
        super.onDestroy()
        FileUtils.releaseFile()
        cameraHelperTop.release()
        cameraHelperBottom.release()
    }



}