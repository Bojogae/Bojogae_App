package com.bojogae.bojogae_app

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
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
import com.bojogae.bojogae_app.listener.SensorListener
import java.util.Objects


class MainActivity : AppCompatActivity() {

    private lateinit var sensorManager: SensorManager // 가속도 센서 매니저
    private var acceleration = 0f // 가속도 크기
    private var currentAcceleration = 0f // 현재 가속도
    private var lastAcceleration = 0f // 최대 가속도


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 핸드쉐이킹 센서(가속도 센서)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val listener = SensorListener()
        listener.setOnShakeListener(object: SensorListener.OnShakeListener {
            override fun onShake(count: Int) {
                navigateToNewFragment()
            }
        })

        // 센서 매니저 등록
        sensorManager.let{
            it.registerListener(listener,
                it.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),SensorManager.SENSOR_DELAY_NORMAL)
        }



    }

    private fun navigateToNewFragment() {
        val navController: NavController = findNavController(R.id.fragmentContainerView)

        // 프래그먼트마다의 핸드쉐이크 기능 구분
        when (navController.currentDestination?.id) {
            R.id.homeFragment -> navController.navigate(R.id.home_to_walk_start)
            R.id.walkStartFragment -> navController.navigate(R.id.walk_start_to_home)
        }

    }


}