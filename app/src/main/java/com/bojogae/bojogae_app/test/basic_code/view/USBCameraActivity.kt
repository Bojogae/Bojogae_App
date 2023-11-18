package com.bojogae.bojogae_app.test.basic_code.view

import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.Surface
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bojogae.bojogae_app.R
import com.bojogae.bojogae_app.test.basic_code.application.MyApplication
import com.bojogae.bojogae_app.databinding.ActivityUsbcameraBinding
import com.jiangdg.usbcamera.UVCCameraHelper
import com.jiangdg.usbcamera.UVCCameraHelper.OnMyDevConnectListener
import com.jiangdg.usbcamera.utils.FileUtils
import com.serenegiant.usb.CameraDialog.CameraDialogParent
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.common.AbstractUVCCameraHandler.OnCaptureListener
import com.serenegiant.usb.common.AbstractUVCCameraHandler.OnEncodeResultListener
import com.serenegiant.usb.common.AbstractUVCCameraHandler.OnPreViewResultListener
import com.serenegiant.usb.encoder.RecordParams
import com.serenegiant.usb.widget.CameraViewInterface

/**
 * UVCCamera use demo
 *
 *
 * Created by jiangdongguo on 2017/9/30.
 */
class USBCameraActivity : AppCompatActivity(), CameraDialogParent, CameraViewInterface.Callback {
    var mToolbar: Toolbar? = null
    var mSeekBrightness: SeekBar? = null
    var mSeekContrast: SeekBar? = null
    var mSwitchVoice: Switch? = null
    private var mCameraHelper: UVCCameraHelper? = null
    private var mUVCCameraView: CameraViewInterface? = null
    private var mDialog: AlertDialog? = null
    private var isRequest = false
    private var isPreview = false
    private val listener: OnMyDevConnectListener = object : OnMyDevConnectListener {
        override fun onAttachDev(device: UsbDevice) {
            // request open permission
            if (!isRequest) {
                isRequest = true
                if (mCameraHelper != null) {
                    mCameraHelper!!.requestPermission(0)
                }
            }
        }

        override fun onDettachDev(device: UsbDevice) {
            // close camera
            if (isRequest) {
                isRequest = false
                mCameraHelper!!.closeCamera()
                showShortMsg(device.deviceName + " is out")
            }
        }

        override fun onConnectDev(device: UsbDevice, isConnected: Boolean) {
            if (!isConnected) {
                showShortMsg("fail to connect,please check resolution params")
                isPreview = false
            } else {
                isPreview = true
                showShortMsg("connecting")
                // initialize seekbar
                // need to wait UVCCamera initialize over
                Thread {
                    try {
                        Thread.sleep(2500)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    Looper.prepare()
                    if (mCameraHelper != null && mCameraHelper!!.isCameraOpened) {
                        mSeekBrightness!!.progress =
                            mCameraHelper!!.getModelValue(UVCCameraHelper.MODE_BRIGHTNESS)
                        mSeekContrast!!.progress =
                            mCameraHelper!!.getModelValue(UVCCameraHelper.MODE_CONTRAST)
                    }
                    Looper.loop()
                }.start()
            }
        }

        override fun onDisConnectDev(device: UsbDevice) {
            showShortMsg("disconnecting")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityUsbcameraBinding.inflate(
            layoutInflater
        )
        setContentView(binding.root)
        mToolbar = binding.toolbar
        mSeekBrightness = binding.seekbarBrightness
        mSeekContrast = binding.seekbarContrast
        mSwitchVoice = binding.switchRecVoice
        initView()

        // step.1 initialize UVCCameraHelper
        mUVCCameraView = binding.cameraView
        mUVCCameraView?.setCallback(this)
        mCameraHelper = UVCCameraHelper.getInstance()
        mCameraHelper?.setDefaultFrameFormat(UVCCameraHelper.FRAME_FORMAT_MJPEG)
        mCameraHelper?.initUSBMonitor(this, mUVCCameraView, listener)
        mCameraHelper?.setOnPreviewFrameListener(OnPreViewResultListener { nv21Yuv ->
            Log.d(
                TAG,
                "onPreviewResult: " + nv21Yuv.size
            )
        })

        // 아무거나 추가
    }

    private fun initView() {
        setSupportActionBar(mToolbar)
        mSeekBrightness!!.max = 100
        mSeekBrightness!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (mCameraHelper != null && mCameraHelper!!.isCameraOpened) {
                    mCameraHelper!!.setModelValue(UVCCameraHelper.MODE_BRIGHTNESS, progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        mSeekContrast!!.max = 100
        mSeekContrast!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (mCameraHelper != null && mCameraHelper!!.isCameraOpened) {
                    mCameraHelper!!.setModelValue(UVCCameraHelper.MODE_CONTRAST, progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    override fun onStart() {
        super.onStart()
        // step.2 register USB event broadcast
        if (mCameraHelper != null) {
            mCameraHelper!!.registerUSB()
        }
    }

    override fun onStop() {
        super.onStop()
        // step.3 unregister USB event broadcast
        if (mCameraHelper != null) {
            mCameraHelper!!.unregisterUSB()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_toobar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_takepic -> {
                if (mCameraHelper == null || !mCameraHelper!!.isCameraOpened) {
                    showShortMsg("sorry,camera open failed")
                    return super.onOptionsItemSelected(item)
                }
                val picPath = (FileUtils.ROOT_PATH + "/" + MyApplication.DIRECTORY_NAME + "/images/"
                        + System.currentTimeMillis() + UVCCameraHelper.SUFFIX_JPEG)
                mCameraHelper!!.capturePicture(picPath, OnCaptureListener { path ->
                    if (TextUtils.isEmpty(path)) {
                        return@OnCaptureListener
                    }
                    Handler(mainLooper).post {
                        Toast.makeText(
                            this@USBCameraActivity,
                            "save path:$path",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }

            R.id.menu_recording -> {
                if (mCameraHelper == null || !mCameraHelper!!.isCameraOpened) {
                    showShortMsg("sorry,camera open failed")
                    return super.onOptionsItemSelected(item)
                }
                if (!mCameraHelper!!.isPushing) {
                    val videoPath =
                        (FileUtils.ROOT_PATH + "/" + MyApplication.DIRECTORY_NAME + "/videos/" + System.currentTimeMillis()
                                + UVCCameraHelper.SUFFIX_MP4)

//                    FileUtils.createfile(FileUtils.ROOT_PATH + "test666.h264");
                    // if you want to record,please create RecordParams like this
                    val params = RecordParams()
                    params.recordPath = videoPath
                    params.recordDuration = 0 // auto divide saved,default 0 means not divided
                    params.isVoiceClose = mSwitchVoice!!.isChecked // is close voice
                    params.isSupportOverlay = true // overlay only support armeabi-v7a & arm64-v8a
                    mCameraHelper!!.startPusher(params, object : OnEncodeResultListener {
                        override fun onEncodeResult(
                            data: ByteArray,
                            offset: Int,
                            length: Int,
                            timestamp: Long,
                            type: Int
                        ) {
                            // type = 1,h264 video stream
                            if (type == 1) {
                                FileUtils.putFileStream(data, offset, length)
                            }
                            // type = 0,aac audio stream
                            if (type == 0) {
                            }
                        }

                        override fun onRecordResult(videoPath: String) {
                            if (TextUtils.isEmpty(videoPath)) {
                                return
                            }
                            Handler(mainLooper).post {
                                Toast.makeText(
                                    this@USBCameraActivity,
                                    "save videoPath:$videoPath",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    })
                    // if you only want to push stream,please call like this
                    // mCameraHelper.startPusher(listener);
                    showShortMsg("start record...")
                    mSwitchVoice!!.isEnabled = false
                } else {
                    FileUtils.releaseFile()
                    mCameraHelper!!.stopPusher()
                    showShortMsg("stop record...")
                    mSwitchVoice!!.isEnabled = true
                }
            }

            R.id.menu_resolution -> {
                if (mCameraHelper == null || !mCameraHelper!!.isCameraOpened) {
                    showShortMsg("sorry,camera open failed")
                    return super.onOptionsItemSelected(item)
                }
                showResolutionListDialog()
            }

            R.id.menu_focus -> {
                if (mCameraHelper == null || !mCameraHelper!!.isCameraOpened) {
                    showShortMsg("sorry,camera open failed")
                    return super.onOptionsItemSelected(item)
                }
                mCameraHelper!!.startCameraFoucs()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showResolutionListDialog() {
        val builder = AlertDialog.Builder(this@USBCameraActivity)
        val rootView =
            LayoutInflater.from(this@USBCameraActivity).inflate(R.layout.layout_dialog_list, null)
        val listView = rootView.findViewById<View>(R.id.listview_dialog) as ListView
        val adapter = ArrayAdapter(
            this@USBCameraActivity,
            android.R.layout.simple_list_item_1,
            resolutionList!!
        )
        if (adapter != null) {
            listView.adapter = adapter
        }
        listView.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, position, id ->
                if (mCameraHelper == null || !mCameraHelper!!.isCameraOpened) return@OnItemClickListener
                val resolution = adapterView.getItemAtPosition(position) as String
                val tmp = resolution.split("x".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                if (tmp != null && tmp.size >= 2) {
                    val widht = Integer.valueOf(tmp[0])
                    val height = Integer.valueOf(tmp[1])
                    mCameraHelper!!.updateResolution(widht, height)
                }
                mDialog!!.dismiss()
            }
        builder.setView(rootView)
        mDialog = builder.create()
        mDialog!!.show()
    }

    private val resolutionList: List<String>?
        // example: {640x480,320x240,etc}
        private get() {
            val list = mCameraHelper!!.supportedPreviewSizes
            var resolutions: MutableList<String>? = null
            if (list != null && list.size != 0) {
                resolutions = ArrayList()
                for (size in list) {
                    if (size != null) {
                        resolutions.add(size.width.toString() + "x" + size.height)
                    }
                }
            }
            return resolutions
        }

    override fun onDestroy() {
        super.onDestroy()
        FileUtils.releaseFile()
        // step.4 release uvc camera resources
        if (mCameraHelper != null) {
            mCameraHelper!!.release()
        }
    }

    private fun showShortMsg(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun getUSBMonitor(): USBMonitor {
        return mCameraHelper!!.usbMonitor
    }

    override fun onDialogResult(canceled: Boolean) {
        if (canceled) {
            showShortMsg("取消操作")
        }
    }

    val isCameraOpened: Boolean
        get() = mCameraHelper!!.isCameraOpened

    override fun onSurfaceCreated(view: CameraViewInterface, surface: Surface) {
        if (!isPreview && mCameraHelper!!.isCameraOpened) {
            mCameraHelper!!.startPreview(mUVCCameraView)
            isPreview = true
        }
    }

    override fun onSurfaceChanged(
        view: CameraViewInterface,
        surface: Surface,
        width: Int,
        height: Int
    ) {
    }

    override fun onSurfaceDestroy(view: CameraViewInterface, surface: Surface) {
        if (isPreview && mCameraHelper!!.isCameraOpened) {
            mCameraHelper!!.stopPreview()
            isPreview = false
        }
    }

    companion object {
        private const val TAG = "Debug"
    }
}