package com.bojogae.bojogae_app.ui.splash

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.bojogae.bojogae_app.databinding.FragmentSplashBinding

class SplashFragment : Fragment() {

    private var _viewBinding: FragmentSplashBinding? = null
    private val viewBinding get() = _viewBinding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _viewBinding = FragmentSplashBinding.inflate(layoutInflater)


        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            val action = SplashFragmentDirections.actionSplashFragmentToHomeFragment()
            findNavController().navigate(action)
        }, 3000)


        return viewBinding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _viewBinding = null
    }

}