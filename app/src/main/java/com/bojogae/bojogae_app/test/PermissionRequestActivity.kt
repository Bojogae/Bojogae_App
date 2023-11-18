package com.bojogae.bojogae_app.test

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bojogae.bojogae_app.R
import com.bojogae.bojogae_app.utils.AppControlUtil
import com.bojogae.bojogae_app.utils.PermissionHelper

class PermissionRequestActivity : AppCompatActivity() {

    private lateinit var necessaryPermissions: MutableList<String>
    private val permissionHelper = PermissionHelper()
    private var requiredPermissionDeviceCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_request)

        permissionHelper.onRequestPermission = object: PermissionHelper.OnRequestPermissionResult {
            override fun onSuccessRequest() {
                Toast.makeText(this@PermissionRequestActivity, "권한을 전부 요청 받았음", Toast.LENGTH_SHORT).show()
            }

            override fun onFailureRequest(permissions: List<String>) {
                Log.d(AppControlUtil.DEBUG_TAG, "허용 받지 못한 권한들")
                Log.d(AppControlUtil.DEBUG_TAG, permissions.toString())
                Toast.makeText(this@PermissionRequestActivity, "권한을 허용 받지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        val permissionOptionValue = intent.getIntExtra(PermissionHelper.OPTION, PermissionHelper.INTERNAL)   // 권한의 종류 확인

        if (permissionOptionValue == PermissionHelper.INTERNAL) {    // 내부 권한 요청

            necessaryPermissions = permissionHelper.checkPermission(this)

            if (necessaryPermissions.isNotEmpty()) {    // 권한 요청이 필요한 경우 권한을 전부 요청
                ActivityCompat.requestPermissions(
                    this, necessaryPermissions.toTypedArray(), PermissionHelper.REQUEST_CODE
                )
            }

        } else if (permissionOptionValue == PermissionHelper.EXTERNAL) { // 외부 권한 요청
            val manager = getSystemService(Context.USB_SERVICE) as UsbManager   // 현재 연결된 USB 디바이스 리스트를 불러옴
            necessaryPermissions = permissionHelper.checkDevicePermission(manager)
            requiredPermissionDeviceCount = necessaryPermissions.size   // 받아야 할 권한 개수 설정

            necessaryPermissions.forEach{ deviceName ->
                val pendingIntent =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_UPDATE_CURRENT

                val permissionIntent = PendingIntent.getBroadcast(this, 0,
                    Intent(PermissionHelper.ACTION_USB_PERMISSION),
                    pendingIntent)

                manager.requestPermission(manager.deviceList[deviceName], permissionIntent) // 디바이스 권한 요청

                // Log.d(AppControlUtil.DEBUG_TAG, "디바이스 요청 " + deviceName)
            }

            val filter = IntentFilter(PermissionHelper.ACTION_USB_PERMISSION)
            ContextCompat.registerReceiver(this, usbReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        }
    }

    private val usbReceiver = object : BroadcastReceiver() {    // USB 권한에 대한 결과를 받을 클래스
        override fun onReceive(context: Context, intent: Intent) {
            if (PermissionHelper.ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.apply {
                            necessaryPermissions.remove(deviceName)
                        }
                    }
                }

                initPermissionHelperRequestResult() // 권한 요청 결과 설정
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PermissionHelper.REQUEST_CODE) {
            for (i in grantResults.indices.reversed()) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    necessaryPermissions.remove(permissions[i])
                }
            }
        }

        initPermissionHelperRequestResult()
    }

    private fun initPermissionHelperRequestResult() {
        if (necessaryPermissions.isEmpty()) {
            permissionHelper.onRequestPermission.onSuccessRequest()
        } else {
            permissionHelper.onRequestPermission.onFailureRequest(necessaryPermissions)
        }
    }
}