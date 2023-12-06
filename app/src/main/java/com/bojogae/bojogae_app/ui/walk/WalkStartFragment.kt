package com.bojogae.bojogae_app.ui.walk

import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bojogae.bojogae_app.analyzer.DistanceAnalyzer
import com.bojogae.bojogae_app.databinding.FragmentWalkStartBinding
import com.bojogae.bojogae_app.utils.AppUtil
import com.github.jiangdongguo.R.*
import com.serenegiant.usb.DeviceFilter
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.UVCCamera
import com.serenegiant.usb.common.BaseFragment
import com.serenegiant.usb.common.UVCCameraHandler
import com.serenegiant.usb.widget.CameraViewInterface
import com.serenegiant.utils.HandlerThreadHandler
import com.serenegiant.utils.ThreadPool.queueEvent

/**
 * 사용자가 "보행 시작" 버튼을 눌렀을 때 카메라로 객체 탐지를 시작하는 화면
 * 필요 기능
 * - 음성 안내
 * - 종료 버튼
 */
class WalkStartFragment : BaseFragment() {

    private var _viewBinding: FragmentWalkStartBinding? = null
    private val viewBinding get() = _viewBinding!!

    private var viewModel: WalkStartViewModel? = null


    private lateinit var usbMonitor: USBMonitor

    private lateinit var cameraViewLeft: CameraViewInterface
    private lateinit var cameraViewRight: CameraViewInterface

    private lateinit var handlerL: UVCCameraHandler
    private lateinit var handlerR: UVCCameraHandler

    private lateinit var distanceAnalyzer: DistanceAnalyzer

    private var flag = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _viewBinding = FragmentWalkStartBinding.inflate(layoutInflater)

        viewModel = ViewModelProvider(requireActivity())[WalkStartViewModel::class.java]
        viewModel?.initDistanceAnalyzer(requireContext())   // 거리 측정 분석기 초기화

        // 거리 측정기
        distanceAnalyzer = DistanceAnalyzer(requireContext())



        // 카메라 뷰 설정
        cameraViewLeft = viewBinding.cameraViewLeft
        cameraViewLeft.aspectRatio = (AppUtil.DEFAULT_WIDTH / AppUtil.DEFAULT_HEIGHT.toFloat()).toDouble()
        cameraViewRight = viewBinding.cameraViewRight
        cameraViewRight.aspectRatio = (AppUtil.DEFAULT_WIDTH / AppUtil.DEFAULT_HEIGHT.toFloat()).toDouble()



        // 카메라 핸들러 설정
        handlerL = UVCCameraHandler.createHandler(requireActivity(), cameraViewLeft, 2,
            AppUtil.DEFAULT_WIDTH, AppUtil.DEFAULT_HEIGHT, UVCCamera.PIXEL_FORMAT_RGB565)
        handlerR = UVCCameraHandler.createHandler(requireActivity(), cameraViewRight, 2,
            AppUtil.DEFAULT_WIDTH, AppUtil.DEFAULT_HEIGHT, UVCCamera.PIXEL_FORMAT_RGB565)

        // usb device 관리 모니터
        usbMonitor = USBMonitor(requireContext(), mOnDeviceConnectListener)


        // 디바이스 필터
        val filter = DeviceFilter.getDeviceFilters(requireContext(), xml.device_filter)

        // 카메라 디바이스만 걸러진 디바이스 리스트
        val deviceList = usbMonitor.getDeviceList(filter[0])


        viewBinding.fabCameraDebug1.setOnClickListener {
            if (!handlerL.isOpened) {
                val device = deviceList[0]
                usbMonitor.requestPermission(device)
                AppUtil.ld("카메라 필터로 걸려진 디바이스 1")
                AppUtil.ld(device.deviceId.toString())
            }
        }

        viewBinding.fabCameraDebug2.setOnClickListener {
            if (!handlerR.isOpened) {
                val device = deviceList[1]
                usbMonitor.requestPermission(device)
                AppUtil.ld("카메라 필터로 걸려진 디바이스 2")
                AppUtil.ld(device.deviceId.toString())
            }
        }

        viewBinding.fabChangeDebug.setOnClickListener {
            if (!flag) {
                viewBinding.clDebugView.visibility = View.INVISIBLE
            } else {
                viewBinding.clDebugView.visibility = View.VISIBLE
            }

            flag = !flag

        }

        distanceAnalyzer.disparityView = viewBinding.disparityMap
        distanceAnalyzer.disparityView.setWillNotDraw(false)


        distanceAnalyzer.resultView = viewBinding.objectDetect
        distanceAnalyzer.resultView.setWillNotDraw(false)


        AppUtil.ld("on create view")

        return viewBinding.root
    }


    override fun onStart() {
        super.onStart()
        usbMonitor.register()
        cameraViewLeft.onResume()
        cameraViewRight.onResume()
    }

    override fun onStop() {
        super.onStop()
        handlerL.close()
        handlerR.close()
        cameraViewLeft.onPause()
        cameraViewRight.onResume()
        usbMonitor.unregister()
    }

    override fun onDestroy() {
        super.onDestroy()
        usbMonitor.destroy()
        _viewBinding = null
    }

    private val mOnDeviceConnectListener: USBMonitor.OnDeviceConnectListener = object :
        USBMonitor.OnDeviceConnectListener {
        override fun onAttach(device: UsbDevice) {
            AppUtil.ld("디바이스 접속됨")
        }

        override fun onConnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock, createNew: Boolean) {
            AppUtil.ld("디바이스 연결됨")
            if (!handlerL.isOpened) {
                handlerL.open(ctrlBlock)
                val st = cameraViewLeft.surfaceTexture
                handlerL.setPreviewCallback(distanceAnalyzer.iFrameLeftCallback)
                handlerL.startPreview(Surface(st))

            } else if (!handlerR.isOpened) {
                handlerR.open(ctrlBlock)
                val st = cameraViewRight.surfaceTexture
                handlerR.setPreviewCallback(distanceAnalyzer.iFrameRightCallback)
                handlerR.startPreview(Surface(st))
            }
        }

        override fun onDisconnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock) {
            AppUtil.ld("디바이스 연결 해제됨")
            if (!handlerL.isEqual(device)) {
                queueEvent({
                    handlerL.close()
                }, 0)
            } else if (!handlerR.isEqual(device)) {
                queueEvent({
                    handlerR.close()
                }, 0)
            }
        }

        override fun onDettach(device: UsbDevice) {
            AppUtil.ld("디바이스 접속 해제됨")

        }

        override fun onCancel(device: UsbDevice) {

        }
    }


}