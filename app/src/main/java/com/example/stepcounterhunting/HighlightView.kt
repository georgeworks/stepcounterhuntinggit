package com.example.stepcounterhunting

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class HighlightView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val overlayColor = Color.parseColor("#CC000000")
    private var highlightRect: RectF? = null
    private var cornerRadius = 8f

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun setHighlight(left: Float, top: Float, right: Float, bottom: Float) {
        highlightRect = RectF(left, top, right, bottom)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Create a full screen overlay
        paint.color = overlayColor
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Cut out the highlight area
        highlightRect?.let { rect ->
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
            paint.xfermode = null
        }
    }
}