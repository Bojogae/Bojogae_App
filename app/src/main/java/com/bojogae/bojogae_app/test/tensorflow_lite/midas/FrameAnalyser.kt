/*
 * Copyright 2021 Shubham Panchal
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bojogae.bojogae_app.test.tensorflow_lite.midas

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.bojogae.bojogae_app.R
import com.serenegiant.usb.common.BaseActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import java.math.RoundingMode

// Image Analyser for performing depth estimation on camera frames.
class FrameAnalyser(
    private var depthEstimationModel : MiDASModel,
    private var drawingOverlay: DrawingOverlay,
    private var resultOverlay: DrawingOverlay,
    private val context: Context
) : ImageAnalysis.Analyzer {

    private var frameBitmap : Bitmap? = null
    private var isFrameProcessing = false
    var isComputingDepthMap = false
    private var lbpCascadeClassifier: CascadeClassifier? = null

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
    }

    override fun analyze(image: ImageProxy) {
        // Return if depth map computation is turned off. See MainActivity.kt
        if ( !isComputingDepthMap ) {
            image.close()
            return
        }
        // If a frame is being processed, drop the current frame.
        if ( isFrameProcessing ) {
            image.close()
            return
        }
        isFrameProcessing = true
        if ( image.image != null ) {
            // Get the `Bitmap` of the current frame ( with corrected rotation ).
            frameBitmap = BitmapUtils.imageToBitmap(image.image!!, image.imageInfo.rotationDegrees)
            image.close()
            CoroutineScope( Dispatchers.Main ).launch {
                runModel( frameBitmap!! )
            }
        }
    }


    private suspend fun runModel( inputImage : Bitmap ) = withContext( Dispatchers.Main ) {
        // Compute the depth given the frame Bitmap.
        val output = depthEstimationModel.getDepthMap( inputImage )
        withContext( Dispatchers.Main ) {
            // Notify that the current frame is processed and the pipeline is
            // ready for the next frame.
            isFrameProcessing = false
            if ( drawingOverlay.visibility == View.GONE ) {
                drawingOverlay.visibility = View.VISIBLE
            }

            val rgbMat = Mat()
            Utils.bitmapToMat(inputImage, rgbMat)

            val grayMat = Mat()

            Imgproc.cvtColor(rgbMat, grayMat, Imgproc.COLOR_RGB2GRAY)

            val facesLeftRects = MatOfRect()
            lbpCascadeClassifier?.detectMultiScale(grayMat, facesLeftRects, 1.1, 3)
            val facesLeftRectList = facesLeftRects.toList()

            val bitmap = BitmapUtils.resizeBitmap(
                output, rgbMat.width(), rgbMat.height()
            )

            val dispMat = Mat()
            Utils.bitmapToMat(bitmap, dispMat)


            for (rect in facesLeftRectList) {
                Imgproc.rectangle(rgbMat, rect, Scalar(0.0, 255.0, 0.0), 3)
                val faceCenterX = rect.x + rect.width / 2
                val faceCenterY = rect.y + rect.height / 2
                val distance = faceDistance(faceCenterX, faceCenterY, dispMat).toString()

                Imgproc.putText(rgbMat, "$distance m", Point(rect.x.toDouble(), (rect.y - 10).toDouble()),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.9, Scalar(255.0, 0.0, 0.0, 255.0), 2)
            }

            val resultBitmap = Bitmap.createBitmap(rgbMat.width(), rgbMat.height(), Bitmap.Config.RGB_565)
            Utils.matToBitmap(rgbMat, resultBitmap)

            resultOverlay.depthMaskBitmap = resultBitmap
            resultOverlay.invalidate()





            // Submit the depth Bitmap to the DrawingOverlay and update it.
            // Note, calling `drawingOverlay.invalidate()` here will call `onDraw()` in DrawingOverlay.kt.
            drawingOverlay.depthMaskBitmap = bitmap
            drawingOverlay.invalidate()
        }
    }


    val alpha = 0.2
    var previousDepth = 0.0
    var depthScale = 1.0

    private fun faceDistance(x: Int, y: Int, dispMat: Mat): Double {
        // Assuming dispMat is the depth map similar to the output in Python
        // Get the depth value at the specified x, y coordinates
        val depthValue = dispMat.get(y, x)[0]

        // Convert the depth value to distance (the formula might need adjustments)
        val distance = 1.0 / (depthValue * depthScale)

        // Apply exponential moving average filter if needed
        val filteredDistance = applyEmaFilter(distance)

        return filteredDistance
    }



    private fun applyEmaFilter(currentDepth: Double): Double {
        return alpha * currentDepth + (1 - alpha) * previousDepth
    }

}