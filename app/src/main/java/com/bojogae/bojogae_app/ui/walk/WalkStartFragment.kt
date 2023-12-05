package com.bojogae.bojogae_app.ui.walk

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.fragment.findNavController
import com.bojogae.bojogae_app.R

/**
 * 사용자가 "보행 시작" 버튼을 눌렀을 때 카메라로 객체 탐지를 시작하는 화면
 * 필요 기능
 * - 음성 안내
 * - 종료 버튼
 */
class WalkStartFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_walk_start, container, false)

        val homeBtn = view.findViewById<Button>(R.id.homeBtn)

        homeBtn.setOnClickListener{
            findNavController().navigate(R.id.walk_start_to_home)
        }

        return view
    }

}