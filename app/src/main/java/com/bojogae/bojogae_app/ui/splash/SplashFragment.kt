package com.bojogae.bojogae_app.ui.splash

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bojogae.bojogae_app.databinding.FragmentSplashBinding
import com.bojogae.bojogae_app.listener.OnLoadingFinishListener
import com.bojogae.bojogae_app.utils.AppUtil
import com.bojogae.bojogae_app.utils.PermissionHelper
import com.bojogae.bojogae_app.utils.toast
import org.opencv.android.OpenCVLoader

class SplashFragment : Fragment() {

    private var _viewBinding: FragmentSplashBinding? = null
    private val viewBinding get() = _viewBinding!!


    private lateinit var necessaryPermissions: MutableList<String>
    private val permissionHelper = PermissionHelper()

    var openCVInitialized = false


    private val onLoadingFinishListener = object : OnLoadingFinishListener {
        override fun onSuccess() {
            AppUtil.ld("권한을 전부 허용 받았음")
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                val action = SplashFragmentDirections.actionSplashFragmentToHomeFragment()
                findNavController().navigate(action)
            }, 3000)
        }

        override fun onFailure() {
            if (openCVInitialized) {
                Toast.makeText(requireContext(), "권한을 허용받지 못했습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "라이브러리를 초기화하지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
            requireActivity().finish()
        }
    }

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) {

        for (entry in it.entries) {
            necessaryPermissions.remove(entry.key)
        }

        if (necessaryPermissions.isEmpty()) {
            permissionHelper.onRequestPermission.onSuccessRequest()
        } else {
            permissionHelper.onRequestPermission.onFailureRequest(necessaryPermissions)
        }

    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _viewBinding = FragmentSplashBinding.inflate(layoutInflater)

        initOpenCV()    // opencv 초기화 설정
        initPermissionListener()    // 권한 설정 리스너 설정

        // 권한 헬퍼를 이용한 필요한 권한을 체크하여 리스트로 반환 받음

        necessaryPermissions = permissionHelper.checkPermission(requireContext())

        // 권한 요청이 필요한 경우 권한을 전부 요청
        if (necessaryPermissions.isNotEmpty()) {
            requestPermission.launch(necessaryPermissions.toTypedArray())
        } else {
            onLoadingFinishListener.onSuccess()
        }

        return viewBinding.root
    }

    private fun initOpenCV() {
        openCVInitialized = if (!OpenCVLoader.initDebug()) {
            Log.d(AppUtil.DEBUG_TAG, "OpenCV Error")
            true

        } else {
            Log.d(AppUtil.DEBUG_TAG, "OpenCV Success")
            false
        }
    }

    private fun initPermissionListener() {
        permissionHelper.onRequestPermission = object : PermissionHelper.OnRequestPermissionResult {
            override fun onSuccessRequest() {
                requireContext().toast("권한을 전부 받았습니다!")
                onLoadingFinishListener.onSuccess()
            }

            override fun onFailureRequest(permissions: List<String>) {
                Log.d(AppUtil.DEBUG_TAG, "허용 받지 못한 권한들")
                Log.d(AppUtil.DEBUG_TAG, permissions.toString())
                onLoadingFinishListener.onFailure()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _viewBinding = null
    }



}