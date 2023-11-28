package com.bojogae.bojogae_app.test

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.View.OnClickListener
import android.widget.Toast
import com.bojogae.bojogae_app.R
import com.bojogae.bojogae_app.analyzer.DistanceAnalyzer
import com.bojogae.bojogae_app.databinding.ActivityObjectDistanceBinding
import com.bojogae.bojogae_app.utils.AppUtil
import com.serenegiant.usb.CameraDialog
import com.serenegiant.usb.CameraDialog.CameraDialogParent
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener
import com.serenegiant.usb.UVCCamera
import com.serenegiant.usb.common.BaseActivity
import com.serenegiant.usb.common.UVCCameraHandler
import com.serenegiant.usb.widget.CameraViewInterface
import com.serenegiant.usb.widget.UVCCameraTextureView
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.calib3d.Calib3d
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.core.TermCriteria
import org.opencv.imgproc.Imgproc

class ObjectDistanceActivity : BaseActivity(), CameraDialogParent {


    private lateinit var viewBinding: ActivityObjectDistanceBinding


    // for accessing USB and USB camera
    private lateinit var usbMonitor: USBMonitor

    private lateinit var handlerL: UVCCameraHandler
    private lateinit var handlerR: UVCCameraHandler

    private lateinit var cameraViewLeft: CameraViewInterface
    private lateinit var cameraViewRight: CameraViewInterface

    private lateinit var previewLeft: Surface
    private lateinit var previewRight: Surface



    private lateinit var distanceAnalyzer: DistanceAnalyzer

    init {
        val kernel = Mat(3, 3, CvType.CV_8U)


        val criteria = TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 30, 0.001)
        val criteriaStereo = TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 30, 0.001)

        val objp = Mat(9 * 6, 1, CvType.CV_32FC3)
        var idx = 0
        for (i in 0 until 6) {
            for (j in 0 until 9) {
                objp.put(idx++, 0, j.toDouble(), i.toDouble(), 0.0)
            }
        }


        val objpoints = mutableListOf<Mat>()
        val imgpointsR = mutableListOf<Mat>()
        val imgpointsL = mutableListOf<Mat>()

        val assetManager = assets

        var rightChessMat: Mat? = null
        var leftChessMat: Mat? = null

        for (i in 0 .. 70) {
            val t = i.toString()

            val chessImaRightIS = assetManager.open("calibration_image/chessboard_r${i}.png")
            val chessImaLeftIS = assetManager.open("calibration_image/chessboard_l${i}.png")

            val rightBitmap = BitmapFactory.decodeStream(chessImaRightIS)
            chessImaRightIS.close()

            rightChessMat = Mat()
            Utils.bitmapToMat(rightBitmap, rightChessMat)

            val leftBitmap = BitmapFactory.decodeStream(chessImaLeftIS)
            chessImaLeftIS.close()

            leftChessMat = Mat()
            Utils.bitmapToMat(leftBitmap, leftChessMat)

            // 체스판 코너 찾기 (오른쪽 이미지)
            val cornersR = MatOfPoint2f()
            val retR = Calib3d.findChessboardCorners(rightChessMat, Size(9.0, 6.0), cornersR)

            // 체스판 코너 찾기 (왼쪽 이미지)
            val cornersL = MatOfPoint2f()
            val retL = Calib3d.findChessboardCorners(leftChessMat, Size(9.0, 6.0), cornersL)

            if (retR && retL) {
                // 코너를 성공적으로 찾은 경우, 결과를 저장
                objpoints.add(objp.clone())
                imgpointsR.add(cornersR)
                imgpointsL.add(cornersL)

                // 체스판 코너를 더 정확하게 정렬
                Imgproc.cornerSubPix(rightChessMat, cornersR, Size(11.0, 11.0), Size(-1.0, -1.0), criteria)
                Imgproc.cornerSubPix(leftChessMat, cornersL, Size(11.0, 11.0), Size(-1.0, -1.0), criteria)
            }

        }

        val mtxR = Mat()
        val distR = Mat()
        val rvecsR = mutableListOf<Mat>()
        val tvecsR = mutableListOf<Mat>()
        val retR = Calib3d.calibrateCamera(objpoints, imgpointsR, rightChessMat?.size(), mtxR, distR, rvecsR, tvecsR)

        val mtxL = Mat()
        val distL = Mat()
        val rvecsL = mutableListOf<Mat>()
        val tvecsL = mutableListOf<Mat>()
        val retL = Calib3d.calibrateCamera(objpoints, imgpointsL, rightChessMat?.size(), mtxL, distL, rvecsL, tvecsL)


        val hR = rightChessMat?.rows() ?: 0
        val wR = rightChessMat?.cols() ?: 0

        val hL = leftChessMat?.rows() ?: 0
        val wL = leftChessMat?.cols() ?: 0

        val omtxR = Size()
        val roiR = Rect()

        Calib3d.getOptimalNewCameraMatrix(mtxR, distR, Size(wR.toDouble(), hR.toDouble()), 1.0, omtxR, roiR)

        val omtxL = Size()
        val roiL = Rect()

        Calib3d.getOptimalNewCameraMatrix(mtxL, distL, Size(wL.toDouble(), hL.toDouble()), 1.0, omtxL, roiL)

        // 스테레오 카메라 보정 수행
        val R = Mat()  // 회전 매트릭스
        val T = Mat()  // 이동 벡터
        val E = Mat()  // 에센셜 매트릭스
        val F = Mat()  // 펀더멘탈 매트릭스
        val RL = Mat() // 왼쪽 카메라의 회전 매트릭스
        val RR = Mat() // 오른쪽 카메라의 회전 매트릭스
        val PL = Mat() // 왼쪽 카메라의 보정된 프로젝션 매트릭스
        val PR = Mat() // 오른쪽 카메라의 보정된 프로젝션 매트릭스
        val Q = Mat()  // Q 매트릭스

        val imageSize = Size(wR.toDouble(), hR.toDouble())

        // 객체 점과 이미지 점을 MatOfPoint2f 리스트로 변환
        val objpointsMatOfPoint2f = objpoints.map { objp ->
            val mop2f = MatOfPoint2f()
            mop2f.create(objp.rows(), 1, CvType.CV_32FC2)
            for (i in 0 until objp.rows()) {
                mop2f.put(i, 0, objp.get(i, 0)[0], objp.get(i, 0)[1])
            }
            mop2f
        }

        val imgpointsRMatOfPoint2f = imgpointsR.map { it as MatOfPoint2f }
        val imgpointsLMatOfPoint2f = imgpointsL.map { it as MatOfPoint2f }


        // 스테레오 카메라 보정
        val retS = Calib3d.stereoCalibrate(
            objpointsMatOfPoint2f, imgpointsLMatOfPoint2f, imgpointsRMatOfPoint2f,
            mtxL, distL,
            mtxR, distR,
            imageSize,
            R, T, E, F,
            Calib3d.CALIB_FIX_INTRINSIC,
            criteriaStereo
        )

        val rectifyScale = 0.0
        Calib3d.stereoRectify(
            mtxL, distL, mtxR, distR, imageSize,
            R, T, RL, RR, PL, PR, Q,
            Calib3d.CALIB_ZERO_DISPARITY, rectifyScale, imageSize
        )

        val chessImageSize = Size(wR.toDouble(), hR.toDouble())

        //


    }

    private fun face_disp(x: Int, y: Int) {
        val average = 0
        for (u in -1 .. 2) {
            for (v in -1 .. 2) {

            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityObjectDistanceBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        if (!OpenCVLoader.initDebug()) {
            Log.d(AppUtil.DEBUG_TAG, "OpenCV Error")
        }

        distanceAnalyzer = DistanceAnalyzer(this)
        distanceAnalyzer.resultListener = object : DistanceAnalyzer.OnResultListener {
            override fun onResult(leftBitmap: Bitmap, rightBitmap: Bitmap) {
                runOnUiThread {
                    viewBinding.disparityMap.setImageBitmap(leftBitmap)
                    viewBinding.objectDetect.setImageBitmap(rightBitmap)
                }
            }
        }

        cameraViewLeft = viewBinding.cameraViewLeft
        cameraViewLeft.aspectRatio = (AppUtil.DEFAULT_WIDTH / AppUtil.DEFAULT_HEIGHT.toFloat()).toDouble()
        (cameraViewLeft as UVCCameraTextureView).setOnClickListener(mOnClickListener)

        handlerL = UVCCameraHandler.createHandler(this, cameraViewLeft, 2,
            AppUtil.DEFAULT_WIDTH, AppUtil.DEFAULT_HEIGHT, UVCCamera.PIXEL_FORMAT_YUV420SP)

        // handlerL = UVCCameraHandler.createHandler(this, cameraViewLeft, UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, BANDWIDTH_FACTORS[0])

        cameraViewRight = viewBinding.cameraViewRight
        cameraViewRight.aspectRatio = (AppUtil.DEFAULT_WIDTH / AppUtil.DEFAULT_HEIGHT.toFloat()).toDouble()
        (cameraViewRight as UVCCameraTextureView).setOnClickListener(mOnClickListener)

        // handlerR = UVCCameraHandler.createHandler(this, cameraViewRight, UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, BANDWIDTH_FACTORS[1])
        handlerR = UVCCameraHandler.createHandler(this, cameraViewRight, 2,
            AppUtil.DEFAULT_WIDTH, AppUtil.DEFAULT_HEIGHT, UVCCamera.PIXEL_FORMAT_YUV420SP)


        usbMonitor = USBMonitor(this, mOnDeviceConnectListener)

        Log.d(AppUtil.DEBUG_TAG, "oncreate")

        distanceAnalyzer.runAnalyze()

    }

    override fun onStart() {
        super.onStart()
        usbMonitor.register()
        cameraViewRight.onResume()
        cameraViewLeft.onResume()

    }

    override fun onStop() {
        handlerR.close()
        cameraViewRight.onPause()

        handlerL.close()
        cameraViewLeft.onPause()

        usbMonitor.unregister()

        super.onStop()
    }

    override fun onDestroy() {
        usbMonitor.destroy()
        distanceAnalyzer.flag = false
        super.onDestroy()
    }

    private val mOnClickListener = OnClickListener { view ->
        when (view.id) {
            R.id.camera_view_left -> if (!handlerL.isOpened) {
                CameraDialog.showDialog(this@ObjectDistanceActivity)
            } else {
                handlerL.close()
                setCameraButton()
            }


            R.id.camera_view_right -> if (!handlerR.isOpened) {
                CameraDialog.showDialog(this@ObjectDistanceActivity)
            } else {
                handlerR.close()
                setCameraButton()
            }

        }
    }
    private val mOnDeviceConnectListener: OnDeviceConnectListener = object : OnDeviceConnectListener {
        override fun onAttach(device: UsbDevice) {
            if (DEBUG) Log.v(TAG, "onAttach:$device")
            Toast.makeText(this@ObjectDistanceActivity, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show()
        }

        override fun onConnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock, createNew: Boolean) {
            if (DEBUG) Log.v(TAG, "onConnect:$device")
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
            if (DEBUG) Log.v(TAG, "onDisconnect:$device")
            if (!handlerL.isEqual(device)) {
                queueEvent({
                    handlerL.close()
                    previewLeft.release()
                    setCameraButton()

                }, 0)
            } else if (!handlerR.isEqual(device)) {
                queueEvent({
                    handlerR.close()
                    previewRight.release()
                    setCameraButton()
                }, 0)
            }
        }

        override fun onDettach(device: UsbDevice) {
            if (DEBUG) Log.v(TAG, "onDettach:$device")
            Toast.makeText(this@ObjectDistanceActivity, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show()
        }

        override fun onCancel(device: UsbDevice) {
            if (DEBUG) Log.v(TAG, "onCancel:")
        }
    }

    /**
     * to access from CameraDialog
     * @return
     */
    override fun getUSBMonitor(): USBMonitor {
        return usbMonitor
    }

    override fun onDialogResult(canceled: Boolean) {
        if (canceled) {
            runOnUiThread(Runnable { setCameraButton() }, 0)
        }
    }

    private fun setCameraButton() {

    }

    companion object {
        private const val DEBUG = false // FIXME set false when production
        private const val TAG = "MainActivity"
        private val BANDWIDTH_FACTORS = floatArrayOf(0.5f, 0.5f)

    }


}