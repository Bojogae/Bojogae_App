package com.bojogae.bojogae_app

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.findNavController
import java.util.Objects
import kotlin.math.sqrt


private const val PERMISSIONS_REQUEST_CODE = 10
private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)

private const val SHAKE_THRESHOLD_GRAVITY = 2.7f
private const val SHAKE_SLOP_TIME_MS = 500
private const val SHAKE_COUNT_RESET_TIME_MS = 3000

class MainActivity : AppCompatActivity() {

    private var sensorManager: SensorManager? = null // 가속도 센서 매니저
    private var acceleration = 0f // 가속도 크기
    private var currentAcceleration = 0f // 현재 가속도
    private var lastAcceleration = 0f // 최대 가속도
    private var mShakeTimestamp: Long = 0 // 시간 기록
    private var mShakeCount = 0 // 흔든 횟수

    @RequiresApi(34)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val manager: UsbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_UPDATE_CURRENT)
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(usbReceiver, filter)

        // 연결된 USB 디바이스 목록을 검색하고 권한이 없는 디바이스에 대해 권한 요청을 진행합니다.
        val deviceList = manager.deviceList
        deviceList.values.forEach { device ->

            Log.d(TAG, device.deviceName)

            if (!manager.hasPermission(device)) {
                manager.requestPermission(device, permissionIntent)
            }
        }

        // 핸드쉐이킹 센서(가속도 센서)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        Objects.requireNonNull(sensorManager)!!
            .registerListener(sensorListener, sensorManager!!
            .getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_NORMAL)

        acceleration = 20f
        currentAcceleration = SensorManager.GRAVITY_EARTH
        lastAcceleration = SensorManager.GRAVITY_EARTH
    }

    // 핸드쉐이킹 기능 구현
    private val sensorListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            lastAcceleration = currentAcceleration

            currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val delta: Float = currentAcceleration - lastAcceleration
            acceleration = acceleration * 0.9f + delta


            if (acceleration > 15) {

                val now:Long = System.currentTimeMillis()

                if (mShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                    return
                }
                if (mShakeTimestamp + SHAKE_COUNT_RESET_TIME_MS < now) {
                    mShakeCount = 0
                }
                mShakeTimestamp = now;
                mShakeCount++;

                navigateToNewFragment()
            }
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    private fun navigateToNewFragment() {
        val navController: NavController = findNavController(R.id.fragmentContainerView)

        // 프래그먼트마다의 핸드쉐이크 기능 구분
        when (navController.currentDestination?.id) {
            R.id.homeFragment -> navController.navigate(R.id.home_to_walk_start)
            R.id.walkStartFragment -> navController.navigate(R.id.walk_start_to_home)
        }

    }


    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
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
                        Log.d(TAG, "permission denied for device $device")
                        // 여기서 사용자에게 권한이 필요하다는 것을 알리고 다시 요청할 수 있습니다.
                    }
                }
            }
        }
    }




    companion object {

        private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
        private const val TAG = "test"

        /** Convenience method used to check if all permissions required by this app are granted */
        fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

}

