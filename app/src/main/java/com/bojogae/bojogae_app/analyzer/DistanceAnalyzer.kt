package com.bojogae.bojogae_app.analyzer

import android.content.Context
import android.graphics.Bitmap
import com.serenegiant.usb.IFrameCallback
import com.serenegiant.usb.common.BaseActivity
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer



class DistanceAnalyzer(val context: Context) {

    private val DEFAULT_WIDTH = 640
    private val DEFAULT_HEIGHT = 480

    private var lbpCascadeClassifier: CascadeClassifier? = null

    private var leftByteBuffer: ByteBuffer? = null
    private var rightByteBuffer: ByteBuffer? = null


    var flag = true


    val iFrameLeftCallback = IFrameCallback {
        leftByteBuffer = it
    }

    val iFrameRightCallback = IFrameCallback {
        rightByteBuffer = it
    }


    interface OnResultListener {
        fun onResult(leftBitmap: Bitmap, rightBitmap: Bitmap)
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
    }

    fun runAnalyze() {
        val thread = Thread {
            kotlin.run {
                while (flag) {
                    if (leftByteBuffer != null && rightByteBuffer != null) {
                        analyze(leftByteBuffer!!, rightByteBuffer!!)
                    }
                }
            }
        }

        thread.start()
    }


    private fun analyze(leftBuffer: ByteBuffer, rightBuffer: ByteBuffer) {

        leftBuffer.clear()
        rightBuffer.clear()

        val srcLeftBitmap = Bitmap.createBitmap(DEFAULT_WIDTH, DEFAULT_HEIGHT, Bitmap.Config.RGB_565)
        srcLeftBitmap.copyPixelsFromBuffer(leftBuffer)


        val srcRightBitmap = Bitmap.createBitmap(DEFAULT_WIDTH, DEFAULT_HEIGHT, Bitmap.Config.RGB_565)
        srcRightBitmap.copyPixelsFromBuffer(rightBuffer)

        val rgbLeftMat = Mat()
        val rgbRightMat = Mat()

        val greyLeftMat = Mat()
        val greyRightMat = Mat()

        Utils.bitmapToMat(srcLeftBitmap, rgbLeftMat) //convert original bitmap to Mat, R G B.
        Utils.bitmapToMat(srcRightBitmap, rgbRightMat)


        Imgproc.cvtColor(rgbLeftMat, greyLeftMat, Imgproc.COLOR_RGB2GRAY) //rgbMat to gray grayMat
        Imgproc.cvtColor(rgbRightMat, greyRightMat, Imgproc.COLOR_RGB2GRAY)


        val facesLeftRects = MatOfRect()
        val facesRightRects = MatOfRect()

        lbpCascadeClassifier?.detectMultiScale(greyLeftMat, facesLeftRects, 1.1, 3)
        lbpCascadeClassifier?.detectMultiScale(greyRightMat, facesRightRects, 1.1, 3)

        val facesLeftRectList = facesLeftRects.toList()
        for (rect in facesLeftRectList) {
            val subMat = rgbLeftMat.submat(rect)
            Imgproc.rectangle(rgbLeftMat, rect, Scalar(0.0, 255.0, 0.0), 3)
        }

        val facesRightRectList = facesRightRects.toList()
        for (rect in facesRightRectList) {
            val subMat = rgbRightMat.submat(rect)
            Imgproc.rectangle(rgbRightMat, rect, Scalar(0.0, 255.0, 0.0), 3)
        }





        val leftBitmap = Bitmap.createBitmap(DEFAULT_WIDTH, DEFAULT_HEIGHT, Bitmap.Config.RGB_565)
        val rightBitmap = Bitmap.createBitmap(DEFAULT_WIDTH, DEFAULT_HEIGHT, Bitmap.Config.RGB_565)

        Utils.matToBitmap(rgbLeftMat, leftBitmap) //convert mat to bitmap
        Utils.matToBitmap(rgbRightMat, rightBitmap)

        resultListener?.onResult(leftBitmap, rightBitmap)


    }


}