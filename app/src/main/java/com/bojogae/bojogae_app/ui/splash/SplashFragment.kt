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
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.findNavController
import com.bojogae.bojogae_app.databinding.FragmentSplashBinding
import com.bojogae.bojogae_app.utils.AppUtil
import com.bojogae.bojogae_app.utils.PermissionHelper

class SplashFragment : Fragment() {

    private var _viewBinding: FragmentSplashBinding? = null
    private val viewBinding get() = _viewBinding!!

    private lateinit var necessaryPermissions: MutableList<String>
    private val permissionHelper = PermissionHelper()

    interface OnLoadingFinishListener {
        fun onSuccess()
        fun onFailure()
    }

    private val onLoadingFinishListener = object : OnLoadingFinishListener {
        override fun onSuccess() {
            TODO("Not yet implemented")
        }

        override fun onFailure() {
            TODO("Not yet implemented")
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _viewBinding = FragmentSplashBinding.inflate(layoutInflater)

        if (AppUtil.initApp()) {
            AppUtil.ld("Application Bojogae init success!!")

        } else {
            AppUtil.ld("Application Bojogae cant't init!")
        }

        permissionHelper.onRequestPermission = object: PermissionHelper.OnRequestPermissionResult {
            override fun onSuccessRequest() {
                Toast.makeText(requireContext(), "권한을 전부 요청 받았음", Toast.LENGTH_SHORT).show()
                Log.d(AppUtil.DEBUG_TAG, "전부 허용 받았음")
            }

            override fun onFailureRequest(permissions: List<String>) {
                Log.d(AppUtil.DEBUG_TAG, "허용 받지 못한 권한들")
                Log.d(AppUtil.DEBUG_TAG, permissions.toString())
                Toast.makeText(requireContext(), "권한을 허용 받지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }


        necessaryPermissions = permissionHelper.checkPermission(requireContext())

        if (necessaryPermissions.isNotEmpty()) {    // 권한 요청이 필요한 경우 권한을 전부 요청
            ActivityCompat.requestPermissions(
                requireActivity(), necessaryPermissions.toTypedArray(), PermissionHelper.REQUEST_CODE
            )
        }


        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            val action = SplashFragmentDirections.actionSplashFragmentToHomeFragment()
            findNavController().navigate(action)
        }, 3000)


        return viewBinding.root
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

    override fun onDestroy() {
        super.onDestroy()
        _viewBinding = null
    }

}