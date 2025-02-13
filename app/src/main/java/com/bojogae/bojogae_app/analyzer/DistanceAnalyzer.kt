package com.bojogae.bojogae_app.analyzer

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.TextureView
import android.widget.ImageView
import androidx.core.view.drawToBitmap
import com.bojogae.bojogae_app.R
import com.bojogae.bojogae_app.listener.OnCameraDistanceListener
import com.bojogae.bojogae_app.test.tensorflow_lite.midas.DrawingOverlay
import com.bojogae.bojogae_app.utils.AppUtil
import com.serenegiant.usb.IFrameCallback
import com.serenegiant.usb.common.BaseActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.MatOfRect
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import java.math.RoundingMode
import java.nio.ByteBuffer
import kotlin.math.pow


class DistanceAnalyzer(val context: Context, private val onCameraDistanceListener: OnCameraDistanceListener) : CoroutineScope {
    override val coroutineContext = Dispatchers.Default + Job()

    private var initFinished = false

    private var lbpCascadeClassifier: CascadeClassifier? = null

    private var leftBuffer: ByteBuffer? = null
    private var rightBuffer: ByteBuffer? = null

    lateinit var disparityView: ImageView
    lateinit var resultView: ImageView


    var flag = true

    private val syncListener = object : OnSyncListener {
        override fun onSyncSuccess() {
            if (leftBuffer != null && rightBuffer != null && initFinished) {
                analyze(leftBuffer!!, rightBuffer!!)
            }
        }
    }


    val iFrameLeftCallback = IFrameCallback {
        leftBuffer = it
        syncListener.onSyncSuccess()
    }

    val iFrameRightCallback = IFrameCallback {
        rightBuffer = it
        syncListener.onSyncSuccess()
    }


    interface OnSyncListener {
        fun onSyncSuccess()
    }




    interface OnCalibrateFinished {
        fun onSuccess()
    }

    private val onCalibrateFinished = object : OnCalibrateFinished {
        override fun onSuccess() {
            initFinished = true
        }
    }



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


        val disparityMat = DisparityMapProcessor.calDisparityMap(greyLeftMat, greyRightMat)

        disparityMat.convertTo(disparityMat, CvType.CV_32F)
        Core.divide(disparityMat, Scalar(16.0), disparityMat)
        Core.subtract(disparityMat, Scalar(2.0), disparityMat)
        Core.divide(disparityMat, Scalar(128.0), disparityMat)

        val facesLeftRects = MatOfRect()
        lbpCascadeClassifier?.detectMultiScale(greyLeftMat, facesLeftRects, 1.1, 3)
        val facesLeftRectList = facesLeftRects.toList()

        for (rect in facesLeftRectList) {
            val faceCenterX = rect.x + rect.width / 2
            val faceCenterY = rect.y + rect.height / 2

            Imgproc.rectangle(rgbLeftMat, rect, Scalar(0.0, 255.0, 0.0), 3)
            val distance = faceDistance(faceCenterX, faceCenterY, disparityMat)
            Imgproc.putText(rgbLeftMat, "$distance m", Point(rect.x.toDouble(), (rect.y - 10).toDouble()),
                Imgproc.FONT_HERSHEY_SIMPLEX, 0.9, Scalar(255.0, 0.0, 0.0, 255.0), 2)

            onCameraDistanceListener.onDistanceCallback(distance, "Face") // 반환
        }

        val filteredMap = DisparityMapProcessor.calFilteredMap(greyLeftMat, greyRightMat)
        val disparityBitmap = Bitmap.createBitmap(AppUtil.DEFAULT_WIDTH, AppUtil.DEFAULT_HEIGHT, Bitmap.Config.RGB_565)
        Utils.matToBitmap(filteredMap, disparityBitmap)

        val detectResultBitmap = Bitmap.createBitmap(AppUtil.DEFAULT_WIDTH, AppUtil.DEFAULT_HEIGHT, Bitmap.Config.RGB_565)
        Utils.matToBitmap(rgbLeftMat, detectResultBitmap)

        disparityView.setImageBitmap(disparityBitmap)
        resultView.setImageBitmap(detectResultBitmap)
    }

    private fun faceDistance(x: Int, y: Int, disp: Mat): Double {
        var average = 0.0
        for (u in -1 until 2) {
            for (v in -1 until 2) {
                val value = disp.get(y + u, x + v)
                average += value[0]
            }
        }
        average /= 9.0

        val distance = -593.97 * average.pow(3) + 1506.8 * average.pow(2) - 1373.1 * average + 522.06

        return (distance * 0.01).toBigDecimal().setScale(2, RoundingMode.HALF_EVEN).toDouble()
    }

}