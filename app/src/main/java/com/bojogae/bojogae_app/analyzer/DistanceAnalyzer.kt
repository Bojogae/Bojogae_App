package com.bojogae.bojogae_app.analyzer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.bojogae.bojogae_app.utils.AppUtil
import com.bojogae.bojogae_app.utils.contents
import com.serenegiant.usb.IFrameCallback
import com.serenegiant.usb.common.BaseActivity
import org.opencv.android.Utils
import org.opencv.calib3d.Calib3d
import org.opencv.calib3d.StereoSGBM
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.MatOfRect
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.core.TermCriteria
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.math.pow


class DistanceAnalyzer(val context: Context) {

    private var initFinished = false

    private var lbpCascadeClassifier: CascadeClassifier? = null

    private var leftByteBuffer: ByteBuffer? = null
    private var rightByteBuffer: ByteBuffer? = null
    private val leftStereoMapX = Mat()
    private val leftStereoMapY = Mat()
    private val rightStereoMapX = Mat()
    private val rightStereoMapY = Mat()
    private var stereo: StereoSGBM? = null
    private var leftMatcher: StereoSGBM? = null
    private var rightMatcher: StereoSGBM? = null
    private val windowSize = 3
    private val minDisp = 2
    private val numDisp = 130 - minDisp
    private val kernel = Mat(3, 3, CvType.CV_8U)
    private var disp: Mat? = null
    private var dispC: Mat? = null

    var flag = true

    // 캘리브레이션 파라미터
    private val fx = 513.443409
    private val fy = 513.443409
    private val cx = 320.000000
    private val cy = 240.000000
    private val baseline = 8

    val iFrameLeftCallback = IFrameCallback {
        leftByteBuffer = it

//        if (initFinished) {
//            Log.d(AppUtil.DEBUG_TAG, "success")
//            Toast.makeText(context, "success", Toast.LENGTH_SHORT).show()
//        }

        if (leftByteBuffer != null && rightByteBuffer != null && initFinished) {

            analyze(leftByteBuffer!!, rightByteBuffer!!)
        }
    }

    val iFrameRightCallback = IFrameCallback {
        rightByteBuffer = it
    }


    interface OnResultListener {
        fun onResult(leftBitmap: Bitmap, rightBitmap: Bitmap)
    }

    interface OnCalibrateFinished {
        fun onSuccess()
    }

    private val onCalibrateFinished = object : OnCalibrateFinished {
        override fun onSuccess() {
            initFinished = true
        }
    }

    var resultListener: OnResultListener? = null

    init {
        val inputStream = context.resources.openRawResource(org.opencv.R.raw.lbpcascade_frontalface)
        val file = File(context.getDir(
            "cascade", BaseActivity.MODE_PRIVATE
        ),
            "lbpcascade_frontalface.xml")
        val fileOutputStream = FileOutputStream(file)
        // asd
        val data = ByteArray(4096)
        var readBytes: Int

        while (inputStream.read(data).also { readBytes = it } != -1) {
            fileOutputStream.write(data, 0, readBytes)
        }

        lbpCascadeClassifier = CascadeClassifier(file.absolutePath)

        inputStream.close()
        fileOutputStream.close()
        file.delete()

        initCalibrate()

    }


    private fun initCalibrate() {
        Thread {
            run {
                Log.d(AppUtil.DEBUG_TAG, "init calibrate thread start")


                val criteria = TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 30, 0.001)
                val criteriaStereo = TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 30, 0.001)



                val objp = Mat.zeros(9 * 6, 1, CvType.CV_32FC3)
                var idx = 0
                for (i in 0 until 6) {
                    for (j in 0 until 9) {
                        objp.put(idx++, 0, j.toDouble(), i.toDouble(), 0.0)
                    }
                }

                Log.d(AppUtil.DEBUG_TAG, "objp init")


                val objpoints = mutableListOf<Mat>()
                val imgpointsR = mutableListOf<Mat>()
                val imgpointsL = mutableListOf<Mat>()

                val assetManager = context.assets

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

                        Imgproc.cvtColor(rightChessMat, rightChessMat, Imgproc.COLOR_RGB2GRAY)
                        Imgproc.cvtColor(leftChessMat, leftChessMat, Imgproc.COLOR_RGB2GRAY)

                        // 체스판 코너를 더 정확하게 정렬
                        Imgproc.cornerSubPix(rightChessMat, cornersR, Size(11.0, 11.0), Size(-1.0, -1.0), criteria)
                        Imgproc.cornerSubPix(leftChessMat, cornersL, Size(11.0, 11.0), Size(-1.0, -1.0), criteria)
                    }

                }

                Log.d(AppUtil.DEBUG_TAG, "calibrate success")
                Log.d(AppUtil.DEBUG_TAG, objp.contents())


                val mtxR = Mat()
                val distR = Mat()
                val rvecsR = mutableListOf<Mat>()
                val tvecsR = mutableListOf<Mat>()


                val mtxL = Mat()
                val distL = Mat()
                val rvecsL = mutableListOf<Mat>()
                val tvecsL = mutableListOf<Mat>()

                val retR: Double = Calib3d.calibrateCamera(objpoints, imgpointsR, rightChessMat?.size(), mtxR, distR, rvecsR, tvecsR)
                val retL: Double = Calib3d.calibrateCamera(objpoints, imgpointsL, rightChessMat?.size(), mtxL, distL, rvecsL, tvecsL)

                Log.d(AppUtil.DEBUG_TAG, "calibrate camera value")
                Log.d(AppUtil.DEBUG_TAG, retR.toString())
                Log.d(AppUtil.DEBUG_TAG, retL.toString())


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

                Log.d(AppUtil.DEBUG_TAG, "Optimal New Camera Matrix")


                val R = Mat()   // 스테레오 보정을 위한 회전 매트릭스
                val T = Mat()   // 스테레오 보정을 위한 이동 벡터
                val E = Mat()
                val F = Mat()
                val imageSize = Size(rightChessMat!!.cols().toDouble(), rightChessMat.rows().toDouble())

                Calib3d.stereoCalibrate(
                    objpoints, imgpointsL, imgpointsR,
                    mtxL, distL, mtxR, distR, imageSize,
                    R, T, E, F, Calib3d.CALIB_FIX_INTRINSIC
                )

                Log.d(AppUtil.DEBUG_TAG, "stereo calibrate success!!")

                val rectifyScale = 0
                val alpha = 0.0

                val RL = Mat() // 왼쪽 카메라의 보정된 회전 매트릭스
                val RR = Mat() // 오른쪽 카메라의 보정된 회전 매트릭스
                val PL = Mat() // 왼쪽 카메라의 보정된 프로젝션 매트릭스
                val PR = Mat() // 오른쪽 카메라의 보정된 프로젝션 매트릭스
                val Q = Mat()  // Q 매트릭스

                Calib3d.stereoRectify(
                    mtxL, distL, mtxR, distR, imageSize,
                    R, T, RL, RR, PL, PR, Q,
                    rectifyScale, alpha, imageSize,
                    roiL, roiR
                )

                Log.d(AppUtil.DEBUG_TAG, "stereo rectify success!!")


                // 왼쪽 카메라에 대한 매핑 계산
                Calib3d.initUndistortRectifyMap(
                    mtxL, distL, RL, PL,
                    rightChessMat.size(), CvType.CV_16SC2,
                    leftStereoMapX, leftStereoMapY
                )

                // 오른쪽 카메라에 대한 매핑 계산
                Calib3d.initUndistortRectifyMap(
                    mtxR, distR, RR, PR,
                    rightChessMat.size(), CvType.CV_16SC2,
                    rightStereoMapX, rightStereoMapY
                )

                stereo = StereoSGBM.create(minDisp,
                    numDisp, windowSize, 10, 100,
                    32, 5,
                    8*3*windowSize.toDouble().pow(2).toInt(),
                    32*3*windowSize.toDouble().pow(2).toInt()
                    )

                Log.d(AppUtil.DEBUG_TAG, "stereo SGBM success!!")
                Log.d(AppUtil.DEBUG_TAG, stereo.toString())




                onCalibrateFinished.onSuccess()

            }
        }.start()
    }




    private fun analyze(leftBuffer: ByteBuffer, rightBuffer: ByteBuffer) {

        leftBuffer.clear()
        rightBuffer.clear()

        val srcLeftBitmap = Bitmap.createBitmap(AppUtil.DEFAULT_WIDTH, AppUtil.DEFAULT_HEIGHT, Bitmap.Config.RGB_565)
        srcLeftBitmap.copyPixelsFromBuffer(leftBuffer)

        val srcRightBitmap = Bitmap.createBitmap(AppUtil.DEFAULT_WIDTH, AppUtil.DEFAULT_HEIGHT, Bitmap.Config.RGB_565)
        srcRightBitmap.copyPixelsFromBuffer(rightBuffer)

        val rgbLeftMat = Mat()
        val rgbRightMat = Mat()

        Utils.bitmapToMat(srcLeftBitmap, rgbLeftMat) //convert original bitmap to Mat, R G B.
        Utils.bitmapToMat(srcRightBitmap, rgbRightMat)



        Log.d(AppUtil.DEBUG_TAG, "imgproc remap success!!")

        val greyLeftMat = Mat()
        val greyRightMat = Mat()

        Imgproc.cvtColor(rgbLeftMat, greyLeftMat, Imgproc.COLOR_RGB2GRAY)
        Imgproc.cvtColor(rgbRightMat, greyRightMat, Imgproc.COLOR_RGB2GRAY)

        // 불일치 맵 계산
        disp = Mat()
        stereo?.compute(greyLeftMat, greyRightMat, disp)

        Log.d(AppUtil.DEBUG_TAG, "stereo compute success and disp create!!")

        dispC = Mat()
        Core.convertScaleAbs(disp, dispC, 1.0 / 16)
        // 색상 맵 적용
        Imgproc.applyColorMap(dispC, dispC, Imgproc.COLORMAP_OCEAN)

        Log.d(AppUtil.DEBUG_TAG, "dispc sucess!!")
        Log.d(AppUtil.DEBUG_TAG, disp.toString())
        Log.d(AppUtil.DEBUG_TAG, dispC.toString())

        val facesLeftRects = MatOfRect()
        lbpCascadeClassifier?.detectMultiScale(greyLeftMat, facesLeftRects, 1.1, 3)
        val facesLeftRectList = facesLeftRects.toList()


        // 얼굴 인식 및 거리 측정
        for (rect in facesLeftRectList) {
            // 얼굴 중심점의 좌표 계산
            val faceCenterX = rect.x + rect.width / 2
            val faceCenterY = rect.y + rect.height / 2

            Imgproc.rectangle(rgbLeftMat, rect, Scalar(0.0, 255.0, 0.0), 3)
            val distance = faceDistance(faceCenterX, faceCenterY)
            val distanceToString = "%.2f".format(distance * 0.01)

            Imgproc.putText(rgbLeftMat, "$distanceToString m", Point(rect.x.toDouble(), (rect.y - 10).toDouble()),
                Imgproc.FONT_HERSHEY_SIMPLEX, 0.9, Scalar(255.0, 0.0, 0.0, 255.0), 2)

        }

        val disparityBitmap = Bitmap.createBitmap(AppUtil.DEFAULT_WIDTH, AppUtil.DEFAULT_HEIGHT, Bitmap.Config.RGB_565)
        val detectResultBitmap = Bitmap.createBitmap(AppUtil.DEFAULT_WIDTH, AppUtil.DEFAULT_HEIGHT, Bitmap.Config.RGB_565)

        Log.d(AppUtil.DEBUG_TAG, "image processing success!!")

        Log.d(AppUtil.DEBUG_TAG, rgbLeftMat.toString())

        Utils.matToBitmap(dispC, disparityBitmap)
        Utils.matToBitmap(rgbLeftMat, detectResultBitmap)

        resultListener?.onResult(disparityBitmap, detectResultBitmap)

    }

    private fun faceDistance(x: Int, y: Int): Double {
        var average = 0.0
        for (u in -1..1) {
            for (v in -1..1) {
                average += dispC!!.get(y + u, x + v)[0]
            }
        }
        average /= 9.0

        val distance =
            -593.97 * average.pow(3) + 1506.8 * average.pow(2) - 1373.1 * average + 522.06

        return distance
    }


}