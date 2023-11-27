package com.bojogae.bojogae_app.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class PermissionHelper {
    interface OnRequestPermissionResult {   // 권한 요청 리스너
        fun onSuccessRequest()
        fun onFailureRequest(permissions: List<String>)
    }

    lateinit var onRequestPermission: OnRequestPermissionResult

    private val REQUIRED_PERMISSION_LIST: MutableList<String> = arrayListOf(    // 필요한 내부 권한 리스트
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {    // 안드로이드 13 이상 권한
            addPermission(Manifest.permission.READ_MEDIA_IMAGES)
            addPermission(Manifest.permission.READ_MEDIA_AUDIO)
            addPermission(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            addPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            addPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }



    private fun addPermission(permission: String) {
        REQUIRED_PERMISSION_LIST.add(permission)
    }

    fun checkPermission(context: Context): MutableList<String>{ // 내부 권한 요청
        val necessaryPermissions = mutableListOf<String>()  // 필요한 권한
        for (permission in REQUIRED_PERMISSION_LIST) {  // 권한을 확인하고 허용되지 않은 권한을 필요한 권한 리스트에 추가
            val result = ContextCompat.checkSelfPermission(context, permission) // 권한 확인
            if (result != PackageManager.PERMISSION_GRANTED) {
                necessaryPermissions.add(permission)
            }
        }
        return necessaryPermissions
    }



    fun checkDevicePermission(manager: UsbManager): MutableList<String> {   // USB 권한 요청
        val deviceList = manager.deviceList
        val necessaryPermissions = mutableListOf<String>()  // 필요한 권한
        for (device in deviceList.values) {
            if (!manager.hasPermission(device)) {
                necessaryPermissions.add(device.deviceName)
            }
        }
        return necessaryPermissions
    }

    companion object {
        const val ACTION_USB_PERMISSION = "com.bojogae.USB_PERMISSION"
        const val OPTION = "PERMISSION_OPTION"
        const val INTERNAL = 0
        const val EXTERNAL = 1
        const val REQUEST_CODE = 1
    }


}

