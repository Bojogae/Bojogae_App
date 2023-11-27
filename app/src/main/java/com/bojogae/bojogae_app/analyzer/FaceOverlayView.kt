package com.bojogae.bojogae_app.analyzer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import org.opencv.core.Rect

class FaceOverlayView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var detectedFaces: MutableList<Rect>? = null
    private val location = IntArray(2)
    private val paint: Paint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }

    fun updateFaces(detectedFaces: MutableList<Rect>) {
        this.detectedFaces = detectedFaces
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        getLocationOnScreen(location)

        detectedFaces?.forEach { rect ->
            // 멤버 변수의 값을 사용
            val adjustedX = rect.x + location[0]
            val adjustedY = rect.y + location[1]

            canvas.drawRect(
                adjustedX.toFloat(),
                adjustedY.toFloat(),
                (adjustedX + rect.width).toFloat(),
                (adjustedY + rect.height).toFloat(),
                paint
            )
        }
    }

}
