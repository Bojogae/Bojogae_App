package com.bojogae.bojogae_app.analyzer

import org.opencv.calib3d.StereoSGBM
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.ximgproc.Ximgproc

object DisparityMapProcessor {

    private val leftStereoSGBM = StereoSGBM.create(
        2,
        130-2,
        3,
        8*3*9,
        32*3*9,
        5,
        4,
        10,
        100,
        32,
        StereoSGBM.MODE_SGBM)

    fun calDisparityMap(img1: Mat, img2: Mat): Mat {
        val disparityMapLeft = Mat()
        leftStereoSGBM.compute(img1, img2, disparityMapLeft)

        return disparityMapLeft
    }

    fun calFilteredMap(img1: Mat, img2: Mat): Mat {
        val disparityMapLeft = Mat()
        leftStereoSGBM.compute(img1, img2, disparityMapLeft)

        val rightStereoSGBM = Ximgproc.createRightMatcher(leftStereoSGBM)
        val disparityMapRight = Mat()
        rightStereoSGBM.compute(img2, img1, disparityMapRight)

        val disparityMatFiltered = Mat(disparityMapLeft.rows(), disparityMapLeft.cols(), CvType.CV_8UC1)
        val disparityWLSFilter = Ximgproc.createDisparityWLSFilter(leftStereoSGBM)
        disparityWLSFilter.lambda = 44000.0 //PrefHelper.getLambda(activity)
        disparityWLSFilter.sigmaColor = 2.5//PrefHelper.getSigma(activity)
        disparityWLSFilter.filter(disparityMapLeft, img1, disparityMatFiltered, disparityMapRight, Rect(0, 0, disparityMapLeft.cols(), disparityMapLeft.rows()), img2)
        Core.normalize(disparityMatFiltered, disparityMatFiltered, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8U)

        return disparityMatFiltered
    }




}