package com.bojogae.bojogae_app.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import org.opencv.core.Mat



fun Context.toast( msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}