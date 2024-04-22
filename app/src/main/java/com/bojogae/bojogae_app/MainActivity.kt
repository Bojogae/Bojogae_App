package com.bojogae.bojogae_app

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.bojogae.bojogae_app.databinding.ActivityMainBinding
import com.bojogae.bojogae_app.listener.SensorListener
import com.bojogae.bojogae_app.utils.AppUtil
import com.bojogae.bojogae_app.utils.toast


class MainActivity : AppCompatActivity() {

    private lateinit var sensorManager: SensorManager // 가속도 센서 매니저
    private var acceleration = 0f // 가속도 크기
    private var currentAcceleration = 0f // 현재 가속도
    private var lastAcceleration = 0f // 최대 가속도

    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var viewModel: MainActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[MainActivityViewModel::class.java]
        setContentView(viewBinding.root)

        // 뒤로가기 버튼 동작시 메서드
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.backPressedTime + 2000 > System.currentTimeMillis()) {
                    finish()
                } else {
                    toast("한번 더 누르면 종료됩니다.")
                }
                viewModel.backPressedTime = System.currentTimeMillis()
            }
        })

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
            R.id.homeFragment -> navController.navigate(R.id.action_homeFragment_to_walkStartFragment)
            R.id.walkStartFragment -> navController.navigate(R.id.action_walkStartFragment_to_homeFragment)
        }
    }



}