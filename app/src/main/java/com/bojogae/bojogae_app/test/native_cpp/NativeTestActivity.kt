package com.bojogae.bojogae_app.test.native_cpp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bojogae.bojogae_app.databinding.ActivityNativeTestBinding

class NativeTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNativeTestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNativeTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Example of a call to a native method
        binding.sampleText.text = stringFromJNI()
    }

    /**
     * A native method that is implemented by the 'nativetest' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'nativetest' library on application startup.
        init {
            System.loadLibrary("nativetest")
        }
    }
}