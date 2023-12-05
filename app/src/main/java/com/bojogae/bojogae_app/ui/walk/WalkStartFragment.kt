package com.bojogae.bojogae_app.ui.walk

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bojogae.bojogae_app.R
import com.bojogae.bojogae_app.databinding.FragmentWalkStartBinding

/**
 * 사용자가 "보행 시작" 버튼을 눌렀을 때 카메라로 객체 탐지를 시작하는 화면
 * 필요 기능
 * - 음성 안내
 * - 종료 버튼
 */
class WalkStartFragment : Fragment() {

    private var _viewBinding: FragmentWalkStartBinding? = null
    private val viewBinding get() = _viewBinding!!

    private var viewModel: WalkStartViewModel? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _viewBinding = FragmentWalkStartBinding.inflate(layoutInflater)

        viewModel = ViewModelProvider(requireActivity())[WalkStartViewModel::class.java]

        viewBinding.homeBtn.setOnClickListener{
            findNavController().navigate(WalkStartFragmentDirections.walkStartToHome())
        }






        return viewBinding.root
    }


    override fun onDestroy() {
        super.onDestroy()
        _viewBinding = null
    }


}