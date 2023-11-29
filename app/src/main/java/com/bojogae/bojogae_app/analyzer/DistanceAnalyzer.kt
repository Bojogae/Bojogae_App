package com.bojogae.bojogae_app.analyzer

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.bojogae.bojogae_app.utils.AppUtil
import com.bojogae.bojogae_app.utils.contents
import com.serenegiant.usb.IFrameCallback
import com.serenegiant.usb.common.BaseActivity
import org.opencv.android.Utils
import org.opencv.calib3d.StereoSGBM
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import java.lang.Double.sum
import java.nio.ByteBuffer
import kotlin.math.pow


class DistanceAnalyzer(val context: Context) {

    private var initFinished = false

    private var lbpCascadeClassifier: CascadeClassifier? = null

    private var leftByteBuffer: ByteBuffer? = null
    private var rightByteBuffer: ByteBuffer? = null
    private var stereo: StereoSGBM? = null
    private val windowSize = 3
    private val minDisp = 2
    private val numDisp = 130 - minDisp

    var flag = true


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


    private fun analyze(leftBuffer: ByteBuffer, rightBuffer: ByteBuffer) {

        leftBuffer.clear()
        rightBuffer.clear()

        val srcLeftBitmap = Bitmap.createBitmap(AppUtil.DEFAULT_WIDTH, AppUtil.DEFAULT_HEIGHT, Bitmap.Config.RGB_565)
        srcLeftBitmap.copyPixelsFromBuffer(leftBuffer)

        val srcRightBitmap = Bitmap.createBitmap(AppUtil.DEFAULT_WIDTH, AppUtil.DEFAULT_HEIGHT, Bitmap.Config.RGB_565)
        srcRightBitmap.copyPixelsFromBuffer(rightBuffer)

        val rgbLeftMat = Mat()
        val rgbRightMat = Mat()

        Utils.bitmapToMat(srcLeftBitmap, rgbLeftMat)
        Utils.bitmapToMat(srcRightBitmap, rgbRightMat)

        Log.d(AppUtil.DEBUG_TAG, "imgproc remap success!!")

        val greyLeftMat = Mat()
        val greyRightMat = Mat()

        Imgproc.cvtColor(rgbLeftMat, greyLeftMat, Imgproc.COLOR_RGB2GRAY)
        Imgproc.cvtColor(rgbRightMat, greyRightMat, Imgproc.COLOR_RGB2GRAY)

        // 불일치 맵 계산
        val disp = Mat()
        stereo?.compute(greyLeftMat, greyRightMat, disp)

        Log.d(AppUtil.DEBUG_TAG, "disp sucess!!")
        Log.d(AppUtil.DEBUG_TAG, disp.get(1, 1).toString())
        Log.d(AppUtil.DEBUG_TAG, disp.toString())




//        // 결과 정규화
//        val dispNorm = Mat()
//        Core.convertScaleAbs(disp, dispNorm, 1.0 / 16)
//        Core.subtract(dispNorm, Scalar(minDisp.toDouble()), dispNorm)
//        Core.divide(dispNorm, Scalar(numDisp.toDouble()), dispNorm)


//        Log.d(AppUtil.DEBUG_TAG, "dispNorm sucess!!")
//        Log.d(AppUtil.DEBUG_TAG, dispNorm.toString())

        val facesLeftRects = MatOfRect()
        lbpCascadeClassifier?.detectMultiScale(greyLeftMat, facesLeftRects, 1.1, 3)
        val facesLeftRectList = facesLeftRects.toList()

        val normalizedDisp = Mat()
        Core.normalize(disp, normalizedDisp, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8UC1)

        // 얼굴 인식 및 거리 측정
        for (rect in facesLeftRectList) {
            // 얼굴 중심점의 좌표 계산
            val faceCenterX = rect.x + rect.width / 2
            val faceCenterY = rect.y + rect.height / 2

            Imgproc.rectangle(rgbLeftMat, rect, Scalar(0.0, 255.0, 0.0), 3)
            val distance = faceDistance(faceCenterX, faceCenterY, normalizedDisp)
            val distanceToString = "%.2f".format(distance * 0.01)

            Imgproc.putText(rgbLeftMat, "$distanceToString m", Point(rect.x.toDouble(), (rect.y - 10).toDouble()),
                Imgproc.FONT_HERSHEY_SIMPLEX, 0.9, Scalar(255.0, 0.0, 0.0, 255.0), 2)
        }




        val dispBitmap = Bitmap.createBitmap(normalizedDisp.cols(), normalizedDisp.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(normalizedDisp, dispBitmap)

        // val disparityBitmap = Bitmap.createBitmap(AppUtil.DEFAULT_WIDTH, AppUtil.DEFAULT_HEIGHT, Bitmap.Config.ARGB_8888)
        val detectResultBitmap = Bitmap.createBitmap(AppUtil.DEFAULT_WIDTH, AppUtil.DEFAULT_HEIGHT, Bitmap.Config.ARGB_8888)

        Log.d(AppUtil.DEBUG_TAG, "image processing success!!")
        Log.d(AppUtil.DEBUG_TAG, rgbLeftMat.toString())

        // Utils.matToBitmap(disp, disparityBitmap)
        Utils.matToBitmap(rgbLeftMat, detectResultBitmap)

        resultListener?.onResult(dispBitmap, detectResultBitmap)

    }

    private fun faceDistance(x: Int, y: Int, dispNorm: Mat): Double {
        var average = 0.0
        for (u in -1..1) {
            for (v in -1..1) {
                val value = dispNorm.get(y + u, x + v)
                average += value.sum()
                Log.d(AppUtil.DEBUG_TAG, value.toString())
                Log.d(AppUtil.DEBUG_TAG, "$average average")
            }
        }
        average /= 9.0

        return -593.97 * average.pow(3) + 1506.8 * average.pow(2) - 1373.1 * average + 522.06
    }


}