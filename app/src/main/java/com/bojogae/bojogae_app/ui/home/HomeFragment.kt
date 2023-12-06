package com.bojogae.bojogae_app.ui.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.bojogae.bojogae_app.R
import com.bojogae.bojogae_app.databinding.FragmentHomeBinding

/**
 * 앱의 메인 화면
 */
class HomeFragment : Fragment() {

    private var _viewBinding: FragmentHomeBinding? = null
    private val viewBinding get() = _viewBinding!!


    override fun onCreateView(

        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _viewBinding = FragmentHomeBinding.inflate(layoutInflater)


        viewBinding.btnStartWalk.setOnClickListener{
            val action = HomeFragmentDirections.actionHomeFragmentToWalkStartFragment()
            findNavController().navigate(action)
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

    override fun onDestroy() {
        super.onDestroy()
        _viewBinding = null
    }
}