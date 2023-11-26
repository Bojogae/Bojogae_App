package com.bojogae.bojogae_app.test

import android.graphics.Bitmap
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.View.OnClickListener
import android.widget.Toast
import com.bojogae.bojogae_app.R
import com.bojogae.bojogae_app.databinding.ActivityObjectDistanceBinding
import com.bojogae.bojogae_app.utils.AppUtil
import com.serenegiant.usb.CameraDialog
import com.serenegiant.usb.CameraDialog.CameraDialogParent
import com.serenegiant.usb.IFrameCallback
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener
import com.serenegiant.usb.UVCCamera
import com.serenegiant.usb.common.BaseActivity
import com.serenegiant.usb.common.UVCCameraHandler
import com.serenegiant.usb.widget.CameraViewInterface
import com.serenegiant.usb.widget.UVCCameraTextureView
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import org.opencv.android.Utils
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class ObjectDistanceActivity : BaseActivity(), CameraDialogParent {


    private lateinit var viewBinding: ActivityObjectDistanceBinding


    // for accessing USB and USB camera
    private lateinit var usbMonitor: USBMonitor

    private lateinit var handlerL: UVCCameraHandler
    private lateinit var handlerR: UVCCameraHandler

    private lateinit var cameraViewLeft: CameraViewInterface
    private lateinit var cameraViewRight: CameraViewInterface

    private lateinit var previewLeft: Surface
    private lateinit var previewRight: Surface

    private var lbpCascadeClassifier: CascadeClassifier? = null

    private var bitmap: Bitmap? = null

    private val iframeCallback = IFrameCallback { frame ->
        frame.clear()
        val srcBitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.RGB_565)
        srcBitmap.copyPixelsFromBuffer(frame)

        val rgbMat = Mat()
        val greyMat = Mat()

        bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.RGB_565)

        Utils.bitmapToMat(srcBitmap, rgbMat) //convert original bitmap to Mat, R G B.

        Imgproc.cvtColor(rgbMat, greyMat, Imgproc.COLOR_RGB2GRAY) //rgbMat to gray grayMat



//        val transposeRGB = rgbMat.t()
//        val transposeGrey = greyMat.t()



        val facesRects = MatOfRect()
        lbpCascadeClassifier?.detectMultiScale(greyMat, facesRects, 1.1, 3)

        for (rect in facesRects.toList()) {
            val subMat = rgbMat.submat(rect)
            //Imgproc.blur(subMat, subMat, Size(10.0, 10.0))
            Imgproc.rectangle(rgbMat, rect, Scalar(0.0, 255.0, 0.0), 3)
        }


        Utils.matToBitmap(rgbMat, bitmap) //convert mat to bitmap

        runOnUiThread {
            viewBinding.cameraViewResultLeft.setImageBitmap(bitmap)
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        viewBinding = ActivityObjectDistanceBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)






        cameraViewLeft = viewBinding.cameraViewLeft
        cameraViewLeft.aspectRatio = (UVCCamera.DEFAULT_PREVIEW_WIDTH / UVCCamera.DEFAULT_PREVIEW_HEIGHT.toFloat()).toDouble()
        (cameraViewLeft as UVCCameraTextureView).setOnClickListener(mOnClickListener)

        handlerL = UVCCameraHandler.createHandler(this, cameraViewLeft, 2,
            UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.PIXEL_FORMAT_YUV420SP)

        // handlerL = UVCCameraHandler.createHandler(this, cameraViewLeft, UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, BANDWIDTH_FACTORS[0])

        cameraViewRight = viewBinding.cameraViewRight
        cameraViewRight.aspectRatio = (UVCCamera.DEFAULT_PREVIEW_WIDTH / UVCCamera.DEFAULT_PREVIEW_HEIGHT.toFloat()).toDouble()
        (cameraViewRight as UVCCameraTextureView).setOnClickListener(mOnClickListener)

        // handlerR = UVCCameraHandler.createHandler(this, cameraViewRight, UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, BANDWIDTH_FACTORS[1])
        handlerR = UVCCameraHandler.createHandler(this, cameraViewRight, 2,
            UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.PIXEL_FORMAT_YUV420SP)


        usbMonitor = USBMonitor(this, mOnDeviceConnectListener)

        if (OpenCVLoader.initDebug()) {

            val inputStream =  resources.openRawResource(org.opencv.R.raw.lbpcascade_frontalface)
            val file = File(getDir(
                "cascade", MODE_PRIVATE
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







        Log.d(AppUtil.DEBUG_TAG, "oncreate")




    }

    override fun onStart() {
        super.onStart()
        usbMonitor.register()
        cameraViewRight.onResume()
        cameraViewLeft.onResume()





    }

    override fun onStop() {
        handlerR.close()
        cameraViewRight.onPause()

        handlerL.close()
        cameraViewLeft.onPause()

        usbMonitor.unregister()

        super.onStop()
    }

    override fun onDestroy() {
        usbMonitor.destroy()
        super.onDestroy()
    }

    private val mOnClickListener = OnClickListener { view ->
        when (view.id) {
            R.id.camera_view_left -> if (!handlerL.isOpened) {
                CameraDialog.showDialog(this@ObjectDistanceActivity)
            } else {
                handlerL.close()
                setCameraButton()
            }


            R.id.camera_view_right -> if (!handlerR.isOpened) {
                CameraDialog.showDialog(this@ObjectDistanceActivity)
            } else {
                handlerR.close()
                setCameraButton()
            }

        }
    }
    private val mOnDeviceConnectListener: OnDeviceConnectListener = object : OnDeviceConnectListener {
        override fun onAttach(device: UsbDevice) {
            if (DEBUG) Log.v(TAG, "onAttach:$device")
            Toast.makeText(this@ObjectDistanceActivity, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show()
        }

        override fun onConnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock, createNew: Boolean) {
            if (DEBUG) Log.v(TAG, "onConnect:$device")
            if (!handlerL.isOpened) {
                handlerL.open(ctrlBlock)
                val st = cameraViewLeft.surfaceTexture
                handlerL.setPreviewCallback(iframeCallback)
                handlerL.startPreview(Surface(st))



            } else if (!handlerR.isOpened) {
                handlerR.open(ctrlBlock)
                val st = cameraViewRight.surfaceTexture
                handlerR.startPreview(Surface(st))

            }
        }

        override fun onDisconnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock) {
            if (DEBUG) Log.v(TAG, "onDisconnect:$device")
            if (!handlerL.isEqual(device)) {
                queueEvent({
                    handlerL.close()
                    previewLeft.release()
                    setCameraButton()

                }, 0)
            } else if (!handlerR.isEqual(device)) {
                queueEvent({
                    handlerR.close()
                    previewRight.release()
                    setCameraButton()
                }, 0)
            }
        }

        override fun onDettach(device: UsbDevice) {
            if (DEBUG) Log.v(TAG, "onDettach:$device")
            Toast.makeText(this@ObjectDistanceActivity, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show()
        }

        override fun onCancel(device: UsbDevice) {
            if (DEBUG) Log.v(TAG, "onCancel:")
        }
    }

    /**
     * to access from CameraDialog
     * @return
     */
    override fun getUSBMonitor(): USBMonitor {
        return usbMonitor
    }

    override fun onDialogResult(canceled: Boolean) {
        if (canceled) {
            runOnUiThread(Runnable { setCameraButton() }, 0)
        }
    }

    private fun setCameraButton() {

    }

    companion object {
        private const val DEBUG = false // FIXME set false when production
        private const val TAG = "MainActivity"
        private val BANDWIDTH_FACTORS = floatArrayOf(0.5f, 0.5f)

    }


}