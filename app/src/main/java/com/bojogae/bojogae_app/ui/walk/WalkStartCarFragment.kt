package com.bojogae.bojogae_app.ui.walk

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Matrix
import android.graphics.Path
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.lifecycle.ViewModelProvider
import com.bojogae.bojogae_app.R
import com.bojogae.bojogae_app.databinding.FragmentWalkStartCarBinding
import com.bojogae.bojogae_app.dto.DriveState
import com.bojogae.bojogae_app.utils.AppUtil
import com.serenegiant.usb.common.BaseFragment


/**
 * 사용자가 "보행 시작" 버튼을 눌렀을 때 카메라로 객체 탐지를 시작하는 화면
 * 필요 기능
 * - 음성 안내
 * - 종료 버튼
 */
class WalkStartCarFragment : BaseFragment() {

    private var _viewBinding: FragmentWalkStartCarBinding? = null
    private val viewBinding get() = _viewBinding!!

    private lateinit var viewModel: WalkStartCarFragmentViewModel
    private val animators = ArrayList<ObjectAnimator>()
    private val radius = 300 // 중심에서 작은 원까지의 거리

    private val numCircles = 10 // 원의 개수

    private val angleStep = 360.0f / numCircles

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _viewBinding = FragmentWalkStartCarBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(requireActivity())[WalkStartCarFragmentViewModel::class.java]

        initializeCircles(viewBinding.mainLayout)
        viewBinding.btnDriveState.setOnClickListener {
            if(viewModel.driveState.value == DriveState.RUNNING){
                stopAnimations()
            }
            else if(viewModel.driveState.value == DriveState.PAUSE){
                startAnimations()
            }

        }

        return viewBinding.root
    }

    private fun getPath(radius: Float, angle: Float): Path {
        val path = Path()
        path.addCircle(0F, 0F, radius, Path.Direction.CW)
        val matrix = Matrix()
        matrix.postRotate(angle)
        path.transform(matrix)
        return path
    }

    private fun stopAnimations() {
        for (animator in animators) {
            viewModel.setDriveState(DriveState.PAUSE)
            viewBinding.btnDriveState.text = "주행 정지"
            animator.cancel()
        }
    }

    private fun startAnimations() {
        for (animator in animators) {
            if (animator.isPaused) {
                viewModel.setDriveState(DriveState.RUNNING)
                viewBinding.btnDriveState.text = "주행 중"
                animator.resume()
            } else {
                animator.start()
                viewModel.setDriveState(DriveState.RUNNING)
                viewBinding.btnDriveState.text = "주행 중"
            }
        }
    }

    private fun initializeCircles(layout: FrameLayout) {
        for (i in 0 until numCircles) {
            val circle = ImageView(context)
            circle.setImageResource(R.drawable.circle_shape)
            layout.addView(circle, FrameLayout.LayoutParams(50, 50, Gravity.CENTER))
            val angle: Float = angleStep * i
            val animation = ObjectAnimator.ofFloat(
                circle, "translationX", "translationY",
                getPath(radius.toFloat(), angle)
            )
            animation.setDuration(2000)
            animation.repeatCount = ValueAnimator.INFINITE
            animation.interpolator = LinearInterpolator()
            animators.add(animation)
        }
        startAnimations()
    }




    override fun onResume() {
        super.onResume()
        AppUtil.ld("onResume method!!")
    }

    override fun onStop() {
        super.onStop()
        AppUtil.ld("onStop method!!")
    }

    override fun onDestroy() {
        super.onDestroy()
        AppUtil.ld("onDestroy method!!")
        _viewBinding = null
    }


}