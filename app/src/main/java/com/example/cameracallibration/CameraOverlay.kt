package com.example.cameracallibration

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class CameraOverlay(context: Context?, attrs: AttributeSet): View(context, attrs) {
    private var paint = Paint()
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        paint.style = Paint.Style.STROKE
        paint.color = Color.GREEN
        paint.strokeWidth = 10f

        //center

        //center
        val x0 = width / 2
        val y0 = height / 2
        val dx = height / 4
        val dy = height / 4
        //draw guide box
        //draw guide box
        canvas?.drawRect(
            (x0 - dx).toFloat(), (y0 - dy).toFloat(), (x0 + dx).toFloat(),
            (y0 + dy).toFloat(), paint
        )

    }

}