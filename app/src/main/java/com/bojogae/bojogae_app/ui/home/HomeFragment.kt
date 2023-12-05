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




        val walkStartBtn = viewBinding.btnStartWalk



        val guideBtn = viewBinding.btnGuide
        val settingsBtn = viewBinding.btnSetting



        walkStartBtn.setOnClickListener{
            findNavController().navigate(R.id.home_to_walk_start)
        }

        guideBtn.setOnClickListener{
            findNavController().navigate(R.id.home_to_guide)
        }

        settingsBtn.setOnClickListener {
            findNavController().navigate(R.id.home_to_setting)
        }

        return viewBinding.root
    }
}