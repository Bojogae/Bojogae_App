package com.bojogae.bojogae_app.ui.walk

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bojogae.bojogae_app.dto.DriveMode
import com.bojogae.bojogae_app.dto.DriveState


class WalkStartCarFragmentViewModel: ViewModel() {

    private val _driveState = MutableLiveData<DriveState>()

    val driveState : MutableLiveData<DriveState>
        get() = _driveState

    fun setDriveState(state: DriveState){
        driveState.value = state
    }
}