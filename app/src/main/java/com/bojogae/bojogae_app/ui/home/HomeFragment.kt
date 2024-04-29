package com.bojogae.bojogae_app.ui.home

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bojogae.bojogae_app.MainActivityViewModel
import com.bojogae.bojogae_app.R
import com.bojogae.bojogae_app.databinding.FragmentHomeBinding
import com.bojogae.bojogae_app.dto.DriveMode
import com.bojogae.bojogae_app.utils.UDPClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 앱의 메인 화면
 */
class HomeFragment : Fragment() {

    private var _viewBinding: FragmentHomeBinding? = null
    private val viewBinding get() = _viewBinding!!
    private lateinit var _viewModel: MainActivityViewModel

    override fun onCreateView(

        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _viewBinding = FragmentHomeBinding.inflate(layoutInflater)
        _viewModel = ViewModelProvider(requireActivity())[MainActivityViewModel::class.java]
        Log.d("test","HOME 화면")

        viewBinding.btnStartWalk.setOnClickListener{
            when(_viewModel.driveMode.value){
                DriveMode.PHONE -> {
                    Log.d("test","Phone")
                    val action = HomeFragmentDirections.actionHomeFragmentToWalkStartPhoneFragment()
                    findNavController().navigate(action)
                    CoroutineScope(Dispatchers.IO).launch {
                        test()
                    }
                }
                DriveMode.ROBOT_CAR -> {
                    Log.d("test","Car")
                    val action = HomeFragmentDirections.actionHomeFragmentToWalkStartCarFragment()
                    findNavController().navigate(action)
                    CoroutineScope(Dispatchers.IO).launch {
                        test()
                    }
                }
                else -> {
                    _viewModel.setDriveMode(DriveMode.ROBOT_CAR)
                    Toast.makeText(context,"기본 주행 모드를 CAR로 설정합니다",Toast.LENGTH_SHORT).show()
                }
            }

        }

        viewBinding.btnGuide.setOnClickListener{
            val action = HomeFragmentDirections.actionHomeFragmentToGuideFragment()
            findNavController().navigate(action)
        }

        viewBinding.btnSetting.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToSettingsFragment()
            findNavController().navigate(action)
        }

        return viewBinding.root
    }
        val ipAddress = "192.168.50.184"
        val port = 6900 // 사용할 포트 번호

        fun test() {
        val client = UDPClient(ipAddress, port)

        // 메시지 전송
        val message = "1"
        client.sendMessage(message)
    }

    override fun onDestroy() {
        super.onDestroy()
        _viewBinding = null
    }
}