package com.bojogae.bojogae_app.analyzer

import org.opencv.calib3d.StereoSGBM
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.ximgproc.Ximgproc

object DisparityMapProcessor {

    private val minDisparity = DisparityParams.minDisparity
    private val numDisparities = DisparityParams.numDisparities
    private val blockSize = DisparityParams.blockSize
    private val P1 = DisparityParams.P1
    private val P2 = DisparityParams.P2
    private val disp12MaxDiff = DisparityParams.disp12MaxDiff
    private val preFilterCap = DisparityParams.preFilterCap
    private val uniquenessRatio = DisparityParams.uniquenessRatio
    private val speckleWindowSize = DisparityParams.speckleWindowSize
    private val speckleRange = DisparityParams.speckleRange
    private val mode = StereoSGBM.MODE_SGBM

    private val leftStereoSGBM = StereoSGBM.create(
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

    val leftStereoSGBM2 = StereoSGBM.create(
        2,
        128,
        3,
        8*3*9,
        32*3*9,
        5,
        4,
        10,
        100,
        2
    )

    fun calculateDisparityMap(img1: Mat, img2: Mat): Mat {

        val disparityMapLeft = Mat()
        leftStereoSGBM2.compute(img1, img2, disparityMapLeft)

//        val rightStereoSGBM = Ximgproc.createRightMatcher(leftStereoSGBM)
//        val disparityMapRight = Mat()
//        rightStereoSGBM.compute(img2, img1, disparityMapRight)
//
//        val disparityMatFiltered = Mat(disparityMapLeft.rows(), disparityMapLeft.cols(), CvType.CV_8UC1)
//        val disparityWLSFilter = Ximgproc.createDisparityWLSFilter(leftStereoSGBM)
//        disparityWLSFilter.lambda = 44000.0 //PrefHelper.getLambda(activity)
//        disparityWLSFilter.sigmaColor = 2.5//PrefHelper.getSigma(activity)
//        disparityWLSFilter.filter(disparityMapLeft, img1, disparityMatFiltered, disparityMapRight, Rect(0, 0, disparityMapLeft.cols(), disparityMapLeft.rows()), img2)

//        val disp8 = Mat()
//
//        Core.normalize(disparityMapLeft, disp8, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8U)

        return disparityMapLeft
    }

}