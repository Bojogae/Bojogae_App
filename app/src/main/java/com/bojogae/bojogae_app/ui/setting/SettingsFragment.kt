package com.bojogae.bojogae_app.ui.setting

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.SpinnerAdapter
import androidx.lifecycle.ViewModelProvider
import com.bojogae.bojogae_app.MainActivityViewModel
import com.bojogae.bojogae_app.R
import com.bojogae.bojogae_app.databinding.FragmentSettingsBinding
import com.bojogae.bojogae_app.dto.DriveMode

class SettingsFragment : Fragment() {
    private var _viewBinding: FragmentSettingsBinding? = null
    private val viewBinding get() = _viewBinding!!
    private lateinit var _viewModel: MainActivityViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _viewBinding = FragmentSettingsBinding.inflate(layoutInflater)
        _viewModel = ViewModelProvider(requireActivity())[MainActivityViewModel::class.java]
        viewBinding.scDriveMode.isChecked = _viewModel.driveMode.value == DriveMode.PHONE
        viewBinding.scDriveMode.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked){
                _viewModel.setDriveMode(DriveMode.PHONE)
            }else{
                _viewModel.setDriveMode(DriveMode.ROBOT_CAR)
            }
        }


        val devicesArray = resources.getStringArray(R.array.string_array_devices)
        val spinnerAdapter = ArrayAdapter(requireContext(), R.layout.cv_spinner_black_bg_white_text, devicesArray)
        viewBinding.spinnerDiscoverDevice.adapter = spinnerAdapter
        spinnerAdapter.setDropDownViewResource(R.layout.cv_spinner_black_bg_white_text)




        return viewBinding.root
    }
}