package com.bojogae.bojogae_app.analyzer

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.bojogae.bojogae_app.R
import com.bojogae.bojogae_app.utils.AppUtil
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
import java.nio.ByteBuffer
import kotlin.math.pow


class DistanceAnalyzer(val context: Context) {

    private val lock = Any()
    private var initFinished = false

    private var lbpCascadeClassifier: CascadeClassifier? = null

    private var leftByteBuffer: ByteBuffer? = null
    private var rightByteBuffer: ByteBuffer? = null
    var flag = true


    val iFrameLeftCallback = IFrameCallback {
        synchronized(lock) {
            leftByteBuffer = it.duplicate()
            if (leftByteBuffer != null && rightByteBuffer != null && initFinished) {
                analyze(leftByteBuffer!!.duplicate(), rightByteBuffer!!.duplicate()) // 복사본을 전달합니다.
            }
        }
    }

    val iFrameRightCallback = IFrameCallback {
        synchronized(lock) {
            rightByteBuffer = it.duplicate()
        }
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
        val inputStream = context.resources.openRawResource(R.raw.haarcascade_frontalface_default)
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


        val greyLeftMat = Mat()
        val greyRightMat = Mat()

        Imgproc.cvtColor(rgbLeftMat, greyLeftMat, Imgproc.COLOR_RGB2GRAY)
        Imgproc.cvtColor(rgbRightMat, greyRightMat, Imgproc.COLOR_RGB2GRAY)



        val disparityMat = DisparityMapProcessor.calculateDisparityMap(greyLeftMat, greyRightMat)
        disparityMat.convertTo(disparityMat, CvType.CV_32F)
        Core.divide(disparityMat, Scalar(16.0), disparityMat)
        Core.subtract(disparityMat, Scalar(2.0), disparityMat)
        Core.divide(disparityMat, Scalar(128.0), disparityMat)


        val facesLeftRects = MatOfRect()
        lbpCascadeClassifier?.detectMultiScale(greyLeftMat, facesLeftRects, 1.1, 3)
        val facesLeftRectList = facesLeftRects.toList()


        // 얼굴 인식 및 거리 측정
        for (rect in facesLeftRectList) {
            // 얼굴 중심점의 좌표 계산
            val faceCenterX = rect.x + rect.width / 2
            val faceCenterY = rect.y + rect.height / 2

            Imgproc.rectangle(rgbLeftMat, rect, Scalar(0.0, 255.0, 0.0), 3)
            val distance = faceDistance(faceCenterX, faceCenterY, disparityMat)
            val distanceToString = "%.2f".format(distance * 0.01)

            Imgproc.putText(rgbLeftMat, "$distanceToString m", Point(rect.x.toDouble(), (rect.y - 10).toDouble()),
                Imgproc.FONT_HERSHEY_SIMPLEX, 0.9, Scalar(255.0, 0.0, 0.0, 255.0), 2)
        }


        Core.normalize(disparityMat, disparityMat, 0.0, 255.0, Core.NORM_MINMAX)
        disparityMat.convertTo(disparityMat, CvType.CV_8U)

        val disparityBitmap = Bitmap.createBitmap(AppUtil.DEFAULT_WIDTH, AppUtil.DEFAULT_HEIGHT, Bitmap.Config.RGB_565)
        Utils.matToBitmap(disparityMat, disparityBitmap)


        val detectResultBitmap = Bitmap.createBitmap(AppUtil.DEFAULT_WIDTH, AppUtil.DEFAULT_HEIGHT, Bitmap.Config.RGB_565)
        Utils.matToBitmap(rgbLeftMat, detectResultBitmap)

        resultListener?.onResult(disparityBitmap, detectResultBitmap)

    }

    private fun faceDistance(x: Int, y: Int, disp: Mat): Double {
        var average = 0.0
        for (u in -1..1) {
            for (v in -1..1) {
                val value = disp.get(y + u, x + v)
                average += value.sum()
            }
        }
        average /= 9.0

        return (-593.97 * average.pow(3) + 1506.8 * average.pow(2) - 1373.1 * average + 522.06)/2
    }



}