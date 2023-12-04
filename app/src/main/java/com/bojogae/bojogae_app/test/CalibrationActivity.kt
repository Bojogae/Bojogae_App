package com.bojogae.bojogae_app.test

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.translation.ViewTranslationCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.bojogae.bojogae_app.R
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.InstallCallbackInterface
import org.opencv.android.JavaCamera2View
import org.opencv.android.JavaCameraView
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.calib3d.Calib3d
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Size
import org.opencv.core.TermCriteria
import org.opencv.imgproc.Imgproc

class CalibrationActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private lateinit var cameraView: JavaCamera2View
    private lateinit var grayR: Mat
    private lateinit var cornersR: MatOfPoint2f
    private var idImage = 0
    private val CRITERIA = TermCriteria(TermCriteria.EPS + TermCriteria.COUNT, 30, 0.1)

    private val baseLoaderCallback: LoaderCallbackInterface = object : LoaderCallbackInterface {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    cameraView.enableView()
                }
                else -> {
                }
            }
        }

        override fun onPackageInstall(operation: Int, callback: InstallCallbackInterface?) {
            // Handle package installation
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calibration)

        // Camera initialization
        cameraView = findViewById(R.id.camera_view)
        cameraView.setViewTranslationCallback(object : ViewTranslationCallback{
            override fun onShowTranslation(view: View): Boolean {
                TODO("Not yet implemented")
            }

            override fun onHideTranslation(view: View): Boolean {
                TODO("Not yet implemented")
            }

            override fun onClearTranslation(view: View): Boolean {
                TODO("Not yet implemented")
            }

        })

        // OpenCV initialization
        OpenCVLoader.initDebug()
        baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)

        grayR = Mat()
        cornersR = MatOfPoint2f()




    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        TODO("Not yet implemented")
    }

    override fun onCameraViewStopped() {
        TODO("Not yet implemented")
    }

    // onCameraFrame 함수 수정
    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        val rgba = inputFrame?.rgba() ?: Mat()
        grayR = Mat(rgba.size(), CvType.CV_8UC1)
        Imgproc.cvtColor(rgba, grayR, Imgproc.COLOR_RGBA2GRAY)

        // cornersR 초기화
        cornersR = MatOfPoint2f()

        // Find chessboard corners
        val retR: Boolean = getCorners(grayR, Size(9.0, 6.0), cornersR)

        // Process the result and save images
        if (retR) {
            // Process the corners and save images
            // ...

            // Draw the corners on the screen (optional)
            Calib3d.drawChessboardCorners(rgba, Size(9.0, 6.0), cornersR, retR)
        }

        return grayR
    }

    private fun getCorners(
        gray: Mat?, patternSize: Size?, corners: MatOfPoint2f?
    ): Boolean {
        var found = false
        if (Calib3d.findChessboardCorners(gray, patternSize, corners)) {
            // Define winSize and zeroZone for cornerSubPix
            val winSize = Size(11.0, 11.0)
            val zeroZone = Size(-1.0, -1.0)

            // Refine corner locations
            Imgproc.cornerSubPix(gray, corners, winSize, zeroZone, CRITERIA)
            found = true
        }
        return found
    }


    private fun getCorners2(
        gray: Mat?, patternSize: Size?, corners: MatOfPoint2f?
    ): Boolean {
        var found = false
        if (Calib3d.findChessboardCorners(gray, patternSize, corners)) {
            // Define winSize and zeroZone for cornerSubPix
            val winSize = Size(11.0, 11.0)
            val zeroZone = Size(-1.0, -1.0)

            // Refine corner locations
            Imgproc.cornerSubPix(gray, corners, winSize, zeroZone, CRITERIA)
            found = true
        }
        return found
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraView.disableView()
    }

    override fun onResume() {
        super.onResume()
        OpenCVLoader.initDebug()
        baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
    }

    override fun onPause() {
        super.onPause()
        cameraView.disableView()
    }

}
