package com.bojogae.bojogae_app.test

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bojogae.bojogae_app.R
import com.bojogae.bojogae_app.databinding.ActivityTestBinding
import com.bojogae.bojogae_app.utils.PermissionHelper

class TestActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityTestBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)


        // 내부 권한 요청
        viewBinding.btnRequestPermission.setOnClickListener {
            val intent = Intent(this, PermissionRequestActivity::class.java)
            intent.putExtra(PermissionHelper.OPTION, PermissionHelper.INTERNAL)
            startActivity(intent)
        }

        // 외부 권한 요청
        viewBinding.btnRequestDevice.setOnClickListener {
            val intent = Intent(this, PermissionRequestActivity::class.java)
            intent.putExtra(PermissionHelper.OPTION, PermissionHelper.EXTERNAL)
            startActivity(intent)
        }

    }
}