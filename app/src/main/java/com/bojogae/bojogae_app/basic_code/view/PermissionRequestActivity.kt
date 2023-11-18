package com.bojogae.bojogae_app.basic_code.view

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bojogae.bojogae_app.R

/**
 * permission checking
 * Created by jiangdongguo on 2019/6/27.
 */
class PermissionRequestActivity : AppCompatActivity() {
    private val mMissPermissions: MutableList<String> = ArrayList()
    private var unPermissionDevice = mutableMapOf<String, UsbDevice>()
    private lateinit var manager: UsbManager

    private interface PermissionAllCheck {
        fun check()
    }

    private lateinit var permissionAllCheck: PermissionAllCheck

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_request)

        manager = getSystemService(Context.USB_SERVICE) as UsbManager


        permissionAllCheck = object: PermissionAllCheck {
            override fun check() {
                if (mMissPermissions.isEmpty() && unPermissionDevice.isEmpty()) {
                    startMainActivity()
                } else {
                    checkAndRequestPermissions()
                }
            }
        }
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        mMissPermissions.clear()
        for (permission in REQUIRED_PERMISSION_LIST) {
            val result = ContextCompat.checkSelfPermission(this, permission)
            if (result != PackageManager.PERMISSION_GRANTED) {
                mMissPermissions.add(permission)
            }
        }

        if (mMissPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this@PermissionRequestActivity,
                mMissPermissions.toTypedArray(),
                REQUEST_CODE
            )
        }


        val deviceList = manager.deviceList // 디바이스 리스트 설정
        deviceList.values.forEach { device ->
            Log.d(TAG, device.deviceName)
            if (!manager.hasPermission(device)) {
                unPermissionDevice[device.deviceName] = device
            }
        }

        unPermissionDevice.values.forEach{ device ->
            val permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_UPDATE_CURRENT)

            manager.requestPermission(device, permissionIntent)

            val filter = IntentFilter(ACTION_USB_PERMISSION)
            ContextCompat.registerReceiver(this, usbReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        }

        permissionAllCheck.check()

    }

    private val usbReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.apply {
                            unPermissionDevice.remove(deviceName)


                        }
                    } else {
                        Log.d(TAG, "permission denied for device $device")
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            for (i in grantResults.indices.reversed()) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    mMissPermissions.remove(permissions[i])
                }
            }
        }
        // Get permissions success or not
        if (mMissPermissions.isEmpty()) {
            startMainActivity()
        } else {
            Toast.makeText(
                this@PermissionRequestActivity,
                "get permissions failed,exiting...",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    private fun startMainActivity() {
        Handler().postDelayed({
            startActivity(Intent(this@PermissionRequestActivity, USBCameraActivity::class.java))
            finish()
        }, 3000)
    }

    companion object {
        private const val TAG = "test"
        private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"

        private val REQUIRED_PERMISSION_LIST = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        private const val REQUEST_CODE = 1
    }
}