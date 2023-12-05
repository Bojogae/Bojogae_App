package com.bojogae.bojogae_app

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.fragment.findNavController

/**
 * 앱의 메인 화면
 */
class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val walkStartBtn = view.findViewById<Button>(R.id.walkStartBtn)
        val guideBtn = view. findViewById<Button>(R.id.guideBtn)
        val settingsBtn = view.findViewById<Button>(R.id.settingBtn)

        walkStartBtn.setOnClickListener{
            findNavController().navigate(R.id.home_to_walk_start)
        }

        guideBtn.setOnClickListener{
            findNavController().navigate(R.id.home_to_guide)
        }

        settingsBtn.setOnClickListener {
            findNavController().navigate(R.id.home_to_setting)
        }

        return view
    }
}