package com.bojogae.bojogae_app.analyzer

import android.content.Context
import android.graphics.Bitmap
import com.bojogae.bojogae_app.utils.AppUtil
import com.serenegiant.usb.IFrameCallback
import com.serenegiant.usb.common.BaseActivity
import org.opencv.android.Utils
import org.opencv.calib3d.StereoBM
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer



class DistanceAnalyzer(val context: Context) {



    private var lbpCascadeClassifier: CascadeClassifier? = null

    private var leftByteBuffer: ByteBuffer? = null
    private var rightByteBuffer: ByteBuffer? = null


    var flag = true

    // 캘리브레이션 파라미터
    private val fx = 513.443409
    private val fy = 513.443409
    private val cx = 320.000000
    private val cy = 240.000000
    private val baseline = 8

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

        val srcLeftBitmap = Bitmap.createBitmap(AppUtil.DEFAULT_WIDTH, AppUtil.DEFAULT_HEIGHT, Bitmap.Config.RGB_565)
        srcLeftBitmap.copyPixelsFromBuffer(leftBuffer)


        val srcRightBitmap = Bitmap.createBitmap(AppUtil.DEFAULT_WIDTH, AppUtil.DEFAULT_HEIGHT, Bitmap.Config.RGB_565)
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
        val facesRightRectList = facesRightRects.toList()


        // 스테레오 이미지에서 깊이 맵 생성 (이 부분은 캘리브레이션 데이터와 세부 설정에 따라 달라짐)
        val depthMap = Mat()
        val stereoMatcher = StereoBM.create()
        stereoMatcher.compute(greyLeftMat, greyRightMat, depthMap)

        // 얼굴 인식 및 거리 측정
        for (rect in facesLeftRectList) {
            // 거리 측정 함수 호출 (아래에 정의)
            val distance = calculateDistance(depthMap, rect)
            // 거리 정보를 사각형 위에 표시
            Imgproc.putText(rgbLeftMat, "Distance: $distance", rect.tl(), Imgproc.FONT_HERSHEY_PLAIN, 4.0, Scalar(0.0, 0.0, 0.0), 3)
            Imgproc.rectangle(rgbLeftMat, rect, Scalar(0.0, 255.0, 0.0), 3)

        }

        for (rect in facesRightRectList) {
            // 거리 측정 함수 호출 (아래에 정의)
            val distance = calculateDistance(depthMap, rect)
            // 거리 정보를 사각형 위에 표시
            Imgproc.putText(rgbRightMat, "Distance: $distance", rect.tl(), Imgproc.FONT_HERSHEY_PLAIN, 4.0, Scalar(0.0, 0.0, 0.0), 3)
            Imgproc.rectangle(rgbRightMat, rect, Scalar(0.0, 255.0, 0.0), 3)
        }


        val leftBitmap = Bitmap.createBitmap(AppUtil.DEFAULT_WIDTH, AppUtil.DEFAULT_HEIGHT, Bitmap.Config.RGB_565)
        val rightBitmap = Bitmap.createBitmap(AppUtil.DEFAULT_WIDTH, AppUtil.DEFAULT_HEIGHT, Bitmap.Config.RGB_565)

        Utils.matToBitmap(rgbLeftMat, leftBitmap) //convert mat to bitmap
        Utils.matToBitmap(rgbRightMat, rightBitmap)

        resultListener?.onResult(leftBitmap, rightBitmap)


    }

    private fun calculateDistance(depthMap: Mat, rect: Rect): Double {
        // rect 중심에서 깊이 정보 추출
        val depthValue = depthMap.get(rect.y + rect.height / 2, rect.x + rect.width / 2)[0]

        // 실제 거리 계산
        val realDistance = (baseline * fx) / depthValue

        return realDistance
    }


}