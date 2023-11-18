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
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bojogae.bojogae_app.MainActivity
import com.bojogae.bojogae_app.R

/**
 * permission checking
 * Created by jiangdongguo on 2019/6/27.
 */
class SplashActivity : AppCompatActivity() {
    private val mMissPermissions: MutableList<String> = ArrayList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_splash)
        if (isVersionM) {
            checkAndRequestPermissions()
        } else {
            startMainActivity()
        }
    }

    private val isVersionM: Boolean
        private get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    private fun checkAndRequestPermissions() {
        mMissPermissions.clear()
        for (permission in REQUIRED_PERMISSION_LIST) {
            val result = ContextCompat.checkSelfPermission(this, permission)
            if (result != PackageManager.PERMISSION_GRANTED) {
                mMissPermissions.add(permission)
            }
        }
        // check permissions has granted
        if (mMissPermissions.isEmpty()) {
            startMainActivity()
        } else {

            ActivityCompat.requestPermissions(
                this,
                mMissPermissions.toTypedArray(),
                REQUEST_CODE
            )
        }

        val manager: UsbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(SplashActivity.ACTION_USB_PERMISSION),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_UPDATE_CURRENT)
        val filter = IntentFilter(SplashActivity.ACTION_USB_PERMISSION)
        registerReceiver(usbReceiver, filter)
        // 연결된 USB 디바이스 목록을 검색하고 권한이 없는 디바이스에 대해 권한 요청을 진행합니다.
        val deviceList = manager.deviceList
        deviceList.values.forEach { device ->

            Log.d(SplashActivity.TAG, device.deviceName)

            if (!manager.hasPermission(device)) {
                manager.requestPermission(device, permissionIntent)
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
                this@SplashActivity,
                "get permissions failed,exiting...",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    private fun startMainActivity() {
        Handler().postDelayed({
            startActivity(Intent(this@SplashActivity, USBCameraActivity::class.java))
            finish()
        }, 3000)
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (SplashActivity.ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }

                    // 권한이 부여되었을 때만 디바이스를 사용
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.apply {
                            // 디바이스 사용을 위한 코드를 여기에 추가합니다.
                            // 예: 카메라를 열고 스트림을 시작합니다.
                        }
                    } else {
                        // 권한이 거부되었을 때
                        Log.d(SplashActivity.TAG, "permission denied for device $device")
                        // 여기서 사용자에게 권한이 필요하다는 것을 알리고 다시 요청할 수 있습니다.
                    }
                }
            }
        }

    }

    companion object {
        private val REQUIRED_PERMISSION_LIST = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
        )
        private const val TAG = "test"
        private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"

        private const val REQUEST_CODE = 1
    }


}