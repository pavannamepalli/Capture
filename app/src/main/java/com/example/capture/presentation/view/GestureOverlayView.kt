package com.example.capture.presentation.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.example.capture.R
import com.example.capture.core.ml.GestureResult
import com.example.capture.core.ml.GestureType

class GestureOverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var gestureResult: GestureResult? = null
    private var actionText: String? = null
    private var cooldownText: String? = null
    private var isRecording = false
    private var gestureMessageHandler: android.os.Handler? = null
    private var showStartupMessage = true
    private var startupMessageHandler: android.os.Handler? = null

    private var gestureBoxRect: RectF? = null


    private val outsideBoxPaint = Paint().apply {
        color = Color.argb(255, 0, 0, 0)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    fun updateGesture(gestureResult: GestureResult) {
        if (gestureResult.gestureType != GestureType.NONE &&
            (this.gestureResult == null || this.gestureResult?.gestureType != gestureResult.gestureType)
        ) {

            if (showStartupMessage) {
                hideStartupMessage()
            }

            gestureMessageHandler?.removeCallbacksAndMessages(null)

            this.gestureResult = gestureResult
            invalidate()

            gestureMessageHandler = android.os.Handler(android.os.Looper.getMainLooper())
            gestureMessageHandler?.postDelayed({
                this.gestureResult = null
                invalidate()
            }, 3000L)
        }
    }

    private fun hideStartupMessage() {
        showStartupMessage = false
        startupMessageHandler?.removeCallbacksAndMessages(null)
        invalidate()
    }

    fun updateAction(actionText: String) {
        this.actionText = actionText
        invalidate()
    }

   fun updateCooldown(cooldown: String?) {
        cooldownText = cooldown
        invalidate()
    }

    fun updateRecordingState(recording: Boolean) {
        isRecording = recording
        invalidate()
    }


    private fun calculateGestureBox() {
        val boxWidth = width * 0.7f
        val boxHeight = height * 0.65f
        val left = (width - boxWidth) / 2f
        val top = (height - boxHeight) / 2f
        val right = left + boxWidth
        val bottom = top + boxHeight

        gestureBoxRect = RectF(left, top, right, bottom)
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        calculateGestureBox()

        drawOutsideBox(canvas)

        drawGestureBox(canvas)

        if (showStartupMessage) {
            drawStartupMessage(canvas)
        }

        gestureResult?.let { gesture ->
            if (gesture.gestureType != GestureType.NONE && gesture.confidence > 0.7f) {
                drawGestureDetectionMessage(canvas, gesture)
            }
        }

        cooldownText?.let { cooldown ->
            drawCooldownStatus(canvas, cooldown)
        }

        if (isRecording) {
            drawRecordingIndicator(canvas)
        }
    }


    private fun getGestureIcon(gestureType: GestureType): String {
        return when (gestureType) {
            GestureType.OPEN_PALM -> "✋"
            GestureType.PEACE_SIGN -> "✌️"
            GestureType.THUMBS_UP -> "👍"
            GestureType.OK_SIGN -> "👌"
            GestureType.PINCH_ZOOM_IN -> "🤏➕"
            GestureType.PINCH_ZOOM_OUT -> "🤏➖"
            GestureType.THREE_FINGERS_UP -> "🤟"
            GestureType.NONE -> ""
        }
    }

    private fun getGestureName(gestureType: GestureType): String {
        return when (gestureType) {
            GestureType.OPEN_PALM -> context.getString(R.string.gesture_display_open_palm)
            GestureType.PEACE_SIGN -> context.getString(R.string.gesture_display_peace_sign)
            GestureType.THUMBS_UP -> context.getString(R.string.gesture_display_thumbs_up)
            GestureType.OK_SIGN -> context.getString(R.string.gesture_display_ok_sign)
            GestureType.PINCH_ZOOM_IN -> context.getString(R.string.gesture_display_pinch_zoom_in)
            GestureType.PINCH_ZOOM_OUT -> context.getString(R.string.gesture_display_pinch_zoom_out)
            GestureType.THREE_FINGERS_UP -> context.getString(R.string.gesture_display_three_fingers_up)
            GestureType.NONE -> ""
        }
    }


    private fun drawGestureDetectionMessage(canvas: Canvas, gesture: GestureResult) {
        val gestureIcon = getGestureIcon(gesture.gestureType)
        val gestureName = getGestureName(gesture.gestureType)
        val message = context.getString(R.string.ui_user_shown_gesture, gestureName, gestureIcon)

        val messagePaint = Paint().apply {
            color = Color.CYAN
            textSize = 48f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
        }

        val textBounds = Rect()
        messagePaint.getTextBounds(message, 0, message.length, textBounds)

        canvas.drawText(
            message,
            width / 2f - textBounds.width() / 2f,
            100f, messagePaint
        )
    }

    private fun drawCooldownStatus(canvas: Canvas, cooldown: String) {
        val isErrorMessage = cooldown.startsWith("Error:")
        val textColor = if (isErrorMessage) Color.RED else Color.WHITE
        
        val cooldownPaint = Paint().apply {
            color = textColor
            textSize = 48f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            setShadowLayer(5f, 2f, 2f, Color.BLACK)
        }

        val textBounds = Rect()
        cooldownPaint.getTextBounds(cooldown, 0, cooldown.length, textBounds)

        canvas.drawText(
            cooldown,
            width / 2f - textBounds.width() / 2f,
            height - 120f, cooldownPaint
        )
    }

    private fun drawStartupMessage(canvas: Canvas) {
        val message = context.getString(R.string.ui_show_gestures_within_box)

        val startupPaint = Paint().apply {
            color = Color.parseColor("#FFD700")
            textSize = 56f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            setShadowLayer(8f, 4f, 4f, Color.parseColor("#80000000"))
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText(
            message,
            width / 2f,
            height / 2f + startupPaint.textSize / 3f, startupPaint
        )

        if (startupMessageHandler == null) {
            startupMessageHandler = android.os.Handler(android.os.Looper.getMainLooper())
            startupMessageHandler?.postDelayed({
                hideStartupMessage()
            }, 3000L)
        }
    }

    private fun drawRecordingIndicator(canvas: Canvas) {
        val dotRadius = 20f
        val margin = 30f

        val ringPaint = Paint().apply {
            color = Color.argb(200, 255, 0, 0)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(margin + dotRadius, margin + dotRadius, dotRadius, ringPaint)

        val dotPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(margin + dotRadius, margin + dotRadius, dotRadius * 0.6f, dotPaint)

        val recTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            setShadowLayer(2f, 1f, 1f, Color.BLACK)
        }
        canvas.drawText(
            context.getString(R.string.ui_recording_indicator),
            margin + dotRadius * 2 + 10f,
            margin + dotRadius + 8f,
            recTextPaint
        )
    }


    private fun drawOutsideBox(canvas: Canvas) {
        gestureBoxRect?.let { box ->
            canvas.drawRect(0f, 0f, width.toFloat(), box.top, outsideBoxPaint)
            canvas.drawRect(0f, box.bottom, width.toFloat(), height.toFloat(), outsideBoxPaint)
            canvas.drawRect(0f, box.top, box.left, box.bottom, outsideBoxPaint)
            canvas.drawRect(box.right, box.top, width.toFloat(), box.bottom, outsideBoxPaint)
        }
    }

    private fun drawGestureBox(canvas: Canvas) {
        gestureBoxRect?.let { box ->
            val cornerSize = 20f
            val cornerPaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            canvas.drawRect(box.left, box.top, box.left + 4f, box.top + cornerSize, cornerPaint)

            canvas.drawRect(box.right - 4f, box.top, box.right, box.top + cornerSize, cornerPaint)

            canvas.drawRect(
                box.left,
                box.bottom - 4f,
                box.left + cornerSize,
                box.bottom,
                cornerPaint
            )
            canvas.drawRect(
                box.left,
                box.bottom - cornerSize,
                box.left + 4f,
                box.bottom,
                cornerPaint
            )

            canvas.drawRect(
                box.right - cornerSize,
                box.bottom - 4f,
                box.right,
                box.bottom,
                cornerPaint
            )
            canvas.drawRect(
                box.right - 4f,
                box.bottom - cornerSize,
                box.right,
                box.bottom,
                cornerPaint
            )
        }
    }
}
