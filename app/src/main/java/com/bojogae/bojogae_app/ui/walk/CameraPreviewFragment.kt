package com.bojogae.bojogae_app.ui.walk

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bojogae.bojogae_app.R
import com.bojogae.bojogae_app.databinding.FragmentCameraPreviewBinding

class CameraPreviewFragment : Fragment() {

    private var _viewBinding: FragmentCameraPreviewBinding? = null
    private val viewBinding get() = _viewBinding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _viewBinding = FragmentCameraPreviewBinding.inflate(layoutInflater)




        return viewBinding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _viewBinding = null
    }
}