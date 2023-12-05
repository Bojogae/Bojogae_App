package com.bojogae.bojogae_app.listener

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

private const val SHAKE_SLOP_TIME_MS = 500
private const val SHAKE_COUNT_RESET_TIME_MS = 3000

class SensorListener: SensorEventListener {

    private var acceleration = 0f // 가속도 크기
    private var currentAcceleration = 0f // 현재 가속도
    private var lastAcceleration = 0f // 최대 가속도
    private var mShakeTimestamp: Long = 0 // 시간 기록
    private var mShakeCount = 0 // 흔든 횟수
    private var mListener: OnShakeListener? = null // 리스너

    init {
        acceleration = 20f
        currentAcceleration = SensorManager.GRAVITY_EARTH
        lastAcceleration = SensorManager.GRAVITY_EARTH
    }

    fun setOnShakeListener(listener:OnShakeListener) {
        this.mListener = listener
    }

    interface OnShakeListener {
        fun onShake(count:Int)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (mListener != null) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            lastAcceleration = currentAcceleration

            currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val delta: Float = currentAcceleration - lastAcceleration
            acceleration = acceleration * 0.9f + delta

            if (acceleration > 15) {

                val now:Long = System.currentTimeMillis()

                if (mShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                    return
                }
                if (mShakeTimestamp + SHAKE_COUNT_RESET_TIME_MS < now) {
                    mShakeCount = 0
                }
                mShakeTimestamp = now
                mShakeCount++

                mListener?.onShake(mShakeCount)
            }
        }

    }
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}