package com.bojogae.bojogae_app.ui.walk

import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.bojogae.bojogae_app.analyzer.DistanceAnalyzer
import com.bojogae.bojogae_app.databinding.FragmentWalkStartBinding
import com.bojogae.bojogae_app.listener.OnCameraDistanceListener
import com.bojogae.bojogae_app.utils.AppUtil
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.common.BaseFragment
import com.serenegiant.usb.common.UVCCameraHandler
import com.serenegiant.usb.widget.CameraViewInterface

/**
 * 사용자가 "보행 시작" 버튼을 눌렀을 때 카메라로 객체 탐지를 시작하는 화면
 * 필요 기능
 * - 음성 안내
 * - 종료 버튼
 */
class WalkStartFragment : BaseFragment() {

    private var _viewBinding: FragmentWalkStartBinding? = null
    private val viewBinding get() = _viewBinding!!

    private lateinit var viewModel: WalkStartFragmentViewModel

    private lateinit var usbMonitor: USBMonitor    // USB 디바이스 연결을 위한 클래스
    private lateinit var deviceList: List<UsbDevice>    // 현재 연결된 USB 디바이스 목록

    private lateinit var cameraViewLeft: CameraViewInterface    // 왼쪽 카메라 뷰
    private lateinit var cameraViewRight: CameraViewInterface    // 오른쪽 카메라 뷰

    private lateinit var handlerL: UVCCameraHandler    // 왼쪽 카메라와 연결하기 위한 핸들러
    private lateinit var handlerR: UVCCameraHandler    // 오른쪽 카메라와 연결하기 위한 핸들러

    private lateinit var analyzer: DistanceAnalyzer    // 객체 인식 및 거리 측정을 위한 분석기

    private var cameraViewVisibilityFlag = false

    /**
     * 카메라에서 객체를 인식하고 거리를 계산한 결과를 반환
     */
    private val onCameraDistanceListener = object : OnCameraDistanceListener {
        override fun onDistanceCallback(distance: Double, type: String) {
            AppUtil.ld("$type distance is $distance")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _viewBinding = FragmentWalkStartBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(requireActivity())[WalkStartFragmentViewModel::class.java]

        // 모니터 설정
        usbMonitor = viewModel.initUSBMonitor(requireActivity(), onDeviceConnectListener)

        // 카메라 필터 설정
        deviceList = viewModel.initDeviceList(requireContext())

        // 거리 측정 분석기 초기화
        analyzer = viewModel.initDistanceAnalyzer(requireActivity(), onCameraDistanceListener)
        analyzer.disparityView = viewBinding.disparityMap
        analyzer.disparityView.setWillNotDraw(false)
        analyzer.resultView = viewBinding.objectDetect
        analyzer.resultView.setWillNotDraw(false)


        // 카메라 핸들러 설정
        viewModel.initHandler(requireActivity(), viewBinding.cameraViewLeft, viewBinding.cameraViewRight)
        handlerL = viewModel.leftCameraHandler
        handlerR = viewModel.rightCameraHandler


        // 카메라 뷰 설정
        cameraViewLeft = viewBinding.cameraViewLeft
        cameraViewLeft.aspectRatio = (AppUtil.DEFAULT_WIDTH / AppUtil.DEFAULT_HEIGHT.toFloat()).toDouble()
        cameraViewRight = viewBinding.cameraViewRight
        cameraViewRight.aspectRatio = (AppUtil.DEFAULT_WIDTH / AppUtil.DEFAULT_HEIGHT.toFloat()).toDouble()

        initOnClickListenerMethod() // 클릭 리스너 정의 메소드

        AppUtil.ld("onCreateView method!!")
        return viewBinding.root
    }

    private fun initOnClickListenerMethod() {
        // 왼쪽 카메라 켜기
        viewBinding.fabCameraDebug1.setOnClickListener {
            if (deviceList.size >= 2) {
                if (!viewModel.leftCameraHandler.isOpened) {
                    val device = deviceList[0]
                    usbMonitor.requestPermission(device)
                    AppUtil.ld("카메라 필터로 걸려진 디바이스 1")
                    AppUtil.ld(device.deviceId.toString())
                }
            }
        }

        // 오른쪽 카메라 켜기
        viewBinding.fabCameraDebug2.setOnClickListener {
            if (deviceList.size >= 2) {
                if (!viewModel.rightCameraHandler.isOpened) {
                    val device = deviceList[1]
                    usbMonitor.requestPermission(device)
                    AppUtil.ld("카메라 필터로 걸려진 디바이스 2")
                    AppUtil.ld(device.deviceId.toString())
                }
            }
        }

        // 카메라 숨기기 & 보이기
        viewBinding.fabChangeDebug.setOnClickListener {
            if (!cameraViewVisibilityFlag) {
                viewBinding.clDebugView.visibility = View.INVISIBLE
            } else {
                viewBinding.clDebugView.visibility = View.VISIBLE
            }

            cameraViewVisibilityFlag = !cameraViewVisibilityFlag
        }
    }


    override fun onResume() {
        super.onResume()
        AppUtil.ld("onResume method!!")
        usbMonitor.register()
        cameraViewLeft.onResume()
        cameraViewRight.onResume()
    }

    override fun onStop() {
        super.onStop()
        AppUtil.ld("onStop method!!")
        viewModel.leftCameraHandler.close()
        viewModel.rightCameraHandler.close()
        cameraViewLeft.onPause()
        cameraViewRight.onPause()
        usbMonitor.unregister()
    }

    override fun onDestroy() {
        super.onDestroy()
        AppUtil.ld("onDestroy method!!")
        usbMonitor.destroy()
        _viewBinding = null
    }


    /**
     * 카메라 디바이스와 연결되고 상태에 따른 메서드 리스너 등록
     */
    private val onDeviceConnectListener: USBMonitor.OnDeviceConnectListener = object :
        USBMonitor.OnDeviceConnectListener {
        override fun onAttach(device: UsbDevice) {
            AppUtil.ld("디바이스 접속됨")
        }

        override fun onConnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock, createNew: Boolean) {
            AppUtil.ld("디바이스 연결됨")
            if (!handlerL.isOpened) {
                handlerL.open(ctrlBlock)
                val st = cameraViewLeft.surfaceTexture
                handlerL.setPreviewCallback(analyzer.iFrameLeftCallback)
                handlerL.startPreview(Surface(st))

            } else if (!handlerR.isOpened) {
                handlerR.open(ctrlBlock)
                val st = cameraViewRight.surfaceTexture
                handlerR.setPreviewCallback(analyzer.iFrameRightCallback)
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