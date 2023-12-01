package com.bojogae.bojogae_app.analyzer

import com.valpa.disparitymap.imageProcessing.DisparityParams
import org.opencv.calib3d.StereoSGBM
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.ximgproc.Ximgproc

object DisparityMapProcessor {

    fun calculateDisparityMap(img1: Mat, img2: Mat): Mat {

        val img1Gray = img1.clone()
        val img2Gray = img2.clone()

        val minDisparity = DisparityParams.minDisparity
        val numDisparities = DisparityParams.numDisparities
        val blockSize = DisparityParams.blockSize
        val P1 = DisparityParams.P1
        val P2 = DisparityParams.P2
        val disp12MaxDiff = DisparityParams.disp12MaxDiff
        val preFilterCap = DisparityParams.preFilterCap
        val uniquenessRatio = DisparityParams.uniquenessRatio
        val speckleWindowSize = DisparityParams.speckleWindowSize
        val speckleRange = DisparityParams.speckleRange
        val mode = StereoSGBM.MODE_SGBM

        val leftStereoSGBM = StereoSGBM.create(
            minDisparity,
            numDisparities,
            blockSize,
            P1,
            P2,
            disp12MaxDiff,
            preFilterCap,
            uniquenessRatio,
            speckleWindowSize,
            speckleRange,
            mode)

        val disparityMapLeft = Mat()
        leftStereoSGBM.compute(img1Gray, img2Gray, disparityMapLeft)

        val rightStereoSGBM = Ximgproc.createRightMatcher(leftStereoSGBM)
        val disparityMapRight = Mat()
        rightStereoSGBM.compute(img2Gray, img1Gray, disparityMapRight)

        val disparityMatFiltered = Mat(disparityMapLeft.rows(), disparityMapLeft.cols(), CvType.CV_8UC1)
        val disparityWLSFilter = Ximgproc.createDisparityWLSFilter(leftStereoSGBM)
        disparityWLSFilter.lambda = 44000.0 //PrefHelper.getLambda(activity)
        disparityWLSFilter.sigmaColor = 2.5//PrefHelper.getSigma(activity)
        disparityWLSFilter.filter(disparityMapLeft, img1Gray, disparityMatFiltered, disparityMapRight, Rect(0, 0, disparityMapLeft.cols(), disparityMapLeft.rows()), img2Gray)

        val disp8 = Mat()
        Core.normalize(disparityMatFiltered, disp8, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8U)

        return disp8

    }

}