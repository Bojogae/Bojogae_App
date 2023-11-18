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
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bojogae.bojogae_app.R

/**
 * permission checking
 * Created by jiangdongguo on 2019/6/27.
 */
class PermissionRequestActivity : AppCompatActivity() {

    private val missPermissions: MutableList<String> = mutableListOf()
    private val unPermissionDevice: MutableMap<String, UsbDevice> = mutableMapOf()
    private lateinit var manager: UsbManager

    private interface PermissionCheck {
        fun check()
    }

    private lateinit var permissionCheck: PermissionCheck

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_request)

        manager = getSystemService(Context.USB_SERVICE) as UsbManager

        permissionCheck = object : PermissionCheck {
            override fun check() {
                if (missPermissions.isEmpty() && unPermissionDevice.isEmpty()) {
                    startMainActivity()
                } else {
                    checkAndRequestPermissions()
                }
            }
        }

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        missPermissions.clear()

        for (permission in REQUIRED_PERMISSION_LIST) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                missPermissions.add(permission)
            }
        }

        if (missPermissions.isNotEmpty()) {
            requestPermissions(
                missPermissions.toTypedArray(),
                REQUEST_CODE
            )
        } else {
            checkUsbPermissions()
        }
    }

    private fun checkUsbPermissions() {
        val deviceList = manager.deviceList
        deviceList.values.forEach { device ->
            if (!manager.hasPermission(device)) {
                unPermissionDevice[device.deviceName ?: ""] = device
            }
        }

        if (unPermissionDevice.isNotEmpty()) {
            val permissionIntent = PendingIntent.getBroadcast(
                this,
                0,
                Intent(ACTION_USB_PERMISSION),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
            )

            for (device in unPermissionDevice.values) {
                manager.requestPermission(device, permissionIntent)
            }

            val filter = IntentFilter(ACTION_USB_PERMISSION)
            ContextCompat.registerReceiver(this,usbReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        } else {
            permissionCheck.check()
        }
    }

    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_USB_PERMISSION) {
                synchronized(this) {
                    val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.let {
                            unPermissionDevice.remove(it.deviceName ?: "")
                        }
                    } else {
                        Log.d(TAG, "permission denied for device $device")
                    }

                    if (unPermissionDevice.isEmpty()) {
                        permissionCheck.check()
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
                    missPermissions.remove(permissions[i])
                }
            }
        }

        if (missPermissions.isEmpty()) {
            permissionCheck.check()
        } else {

            Toast.makeText(this, "Get permissions failed, exiting...", Toast.LENGTH_SHORT).show()
            finish()
        }
    }



    private fun startMainActivity() {
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, USBCameraActivity::class.java))
            finish()
        }, 3000)
    }


    companion object {
        private const val TAG = "test"
        private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"

        private val REQUEST_CODE_READ_EXTERNAL_STORAGE = 1
        private val REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 1
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private val REQUIRED_PERMISSION_LIST = arrayOf(
            //Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VIDEO,
        )
        private const val REQUEST_CODE = 1
    }
}