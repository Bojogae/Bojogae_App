package com.bojogae.bojogae_app

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bojogae.bojogae_app.dto.DriveMode

class MainActivityViewModel: ViewModel() {

    var backPressedTime = 0L

    private val _driveMode = MutableLiveData<DriveMode>()

    val driveMode : MutableLiveData<DriveMode>
        get() = _driveMode


    fun setDriveMode(mode: DriveMode){
        driveMode.value = mode
    }


}