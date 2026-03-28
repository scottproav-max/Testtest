package com.lavalamp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class LavaLampView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val blobs = mutableListOf<Blob>()
    private val blobPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint()
    private val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var offscreenBitmap: Bitmap? = null
    private var offscreenCanvas: Canvas? = null

    private var lastFrameTime = System.nanoTime()
    private var initialized = false

    // Lava lamp color palettes — warm glowing colors
    private val blobColors = intArrayOf(
        0xFFFF1744.toInt(), // Red
        0xFFFF9100.toInt(), // Orange
        0xFFFFEA00.toInt(), // Yellow
        0xFFFF4081.toInt(), // Pink
        0xFFE040FB.toInt(), // Purple
        0xFFFF6E40.toInt(), // Deep Orange
        0xFFFFAB40.toInt(), // Amber
        0xFFFF5252.toInt(), // Red accent
    )

    // Glass lamp body parameters
    private var lampLeft = 0f
    private var lampRight = 0f
    private var lampTop = 0f
    private var lampBottom = 0f

    init {
        // Additive blending for glow effect
        blobPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Define lamp body region (centered, narrower than screen)
        val lampWidth = w * 0.65f
        lampLeft = (w - lampWidth) / 2f
        lampRight = lampLeft + lampWidth
        lampTop = h * 0.08f
        lampBottom = h * 0.88f

        offscreenBitmap?.recycle()
        offscreenBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        offscreenCanvas = Canvas(offscreenBitmap!!)

        if (!initialized) {
            initBlobs()
            initialized = true
        }
    }

    private fun initBlobs() {
        blobs.clear()
        val count = 8
        val blobWidth = lampRight - lampLeft
        val blobHeight = lampBottom - lampTop

        for (i in 0 until count) {
            val x = lampLeft + Random.nextFloat() * blobWidth
            val y = lampTop + Random.nextFloat() * blobHeight
            val radius = 60f + Random.nextFloat() * 80f
            val vy = (if (Random.nextBoolean()) 1f else -1f) * (30f + Random.nextFloat() * 50f)
            val color = blobColors[i % blobColors.size]
            blobs.add(Blob(x, y, radius, 0f, vy, color))
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val now = System.nanoTime()
        val dt = min((now - lastFrameTime) / 1_000_000_000f, 0.05f) // cap at 50ms
        lastFrameTime = now

        // Update physics
        for (blob in blobs) {
            blob.update(lampRight, lampBottom, dt)
            // Keep blobs inside lamp body
            blob.x = blob.x.coerceIn(lampLeft + blob.radius * 0.5f, lampRight - blob.radius * 0.5f)
            blob.y = blob.y.coerceIn(lampTop + blob.radius * 0.5f, lampBottom - blob.radius * 0.5f)
        }

        // Draw dark background
        canvas.drawColor(0xFF1A1A2E.toInt())

        // Draw lamp body background (dark glass)
        drawLampBody(canvas)

        // Draw blobs onto offscreen bitmap for blending
        val offCanvas = offscreenCanvas ?: return
        val offBitmap = offscreenBitmap ?: return
        offCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        for (blob in blobs) {
            drawBlob(offCanvas, blob)
        }

        // Clip blobs to lamp body shape and draw
        canvas.save()
        clipLampShape(canvas)
        canvas.drawBitmap(offBitmap, 0f, 0f, null)
        canvas.restore()

        // Draw lamp frame/outline
        drawLampFrame(canvas)

        // Request next frame
        invalidate()
    }

    private fun drawBlob(canvas: Canvas, blob: Blob) {
        val r = blob.radius * 1.8f
        val centerColor = blob.color
        val edgeColor = (centerColor and 0x00FFFFFF) or 0x00000000 // fully transparent edge

        val gradient = RadialGradient(
            blob.x, blob.y, r,
            intArrayOf(centerColor, centerColor, edgeColor),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP
        )
        blobPaint.shader = gradient
        canvas.drawCircle(blob.x, blob.y, r, blobPaint)
    }

    private fun drawLampBody(canvas: Canvas) {
        // Dark semi-transparent lamp interior
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = 0xFF0D0D1A.toInt()

        val cx = width / 2f
        val bodyWidth = lampRight - lampLeft

        // Draw lamp as a rounded rectangle for the body
        val rect = android.graphics.RectF(lampLeft, lampTop, lampRight, lampBottom)
        canvas.drawRoundRect(rect, bodyWidth * 0.15f, bodyWidth * 0.08f, paint)
    }

    private fun clipLampShape(canvas: Canvas) {
        val bodyWidth = lampRight - lampLeft
        val path = android.graphics.Path()
        val rect = android.graphics.RectF(lampLeft, lampTop, lampRight, lampBottom)
        path.addRoundRect(rect, bodyWidth * 0.15f, bodyWidth * 0.08f, android.graphics.Path.Direction.CW)
        canvas.clipPath(path)
    }

    private fun drawLampFrame(canvas: Canvas) {
        val cx = width / 2f
        val bodyWidth = lampRight - lampLeft

        // Glass outline with subtle glow
        framePaint.style = Paint.Style.STROKE
        framePaint.strokeWidth = 3f
        framePaint.color = 0x44FFFFFF.toInt()

        val rect = android.graphics.RectF(lampLeft, lampTop, lampRight, lampBottom)
        canvas.drawRoundRect(rect, bodyWidth * 0.15f, bodyWidth * 0.08f, framePaint)

        // Top cap
        val capPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        capPaint.color = 0xFF2A2A3E.toInt()

        val capHeight = height * 0.06f
        val capWidth = bodyWidth * 0.5f
        val capRect = android.graphics.RectF(
            cx - capWidth / 2, lampTop - capHeight + 10,
            cx + capWidth / 2, lampTop + 10
        )
        canvas.drawRoundRect(capRect, 20f, 20f, capPaint)

        // Top cap highlight
        capPaint.style = Paint.Style.STROKE
        capPaint.strokeWidth = 2f
        capPaint.color = 0x33FFFFFF.toInt()
        canvas.drawRoundRect(capRect, 20f, 20f, capPaint)

        // Bottom base
        val basePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        basePaint.color = 0xFF2A2A3E.toInt()

        val baseHeight = height * 0.08f
        val baseWidth = bodyWidth * 0.7f
        val baseRect = android.graphics.RectF(
            cx - baseWidth / 2, lampBottom - 10,
            cx + baseWidth / 2, lampBottom + baseHeight - 10
        )
        canvas.drawRoundRect(baseRect, 25f, 25f, basePaint)

        // Base highlight
        basePaint.style = Paint.Style.STROKE
        basePaint.strokeWidth = 2f
        basePaint.color = 0x33FFFFFF.toInt()
        canvas.drawRoundRect(baseRect, 25f, 25f, basePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            // Tap to spawn a new blob at touch point (if inside lamp)
            if (event.x in lampLeft..lampRight && event.y in lampTop..lampBottom) {
                if (event.action == MotionEvent.ACTION_DOWN && blobs.size < 20) {
                    val radius = 50f + Random.nextFloat() * 70f
                    val vy = (if (Random.nextBoolean()) 1f else -1f) * (30f + Random.nextFloat() * 50f)
                    val color = blobColors[Random.nextInt(blobColors.size)]
                    blobs.add(Blob(event.x, event.y, radius, 0f, vy, color))
                }

                // Push nearby blobs away from touch
                for (blob in blobs) {
                    val dx = blob.x - event.x
                    val dy = blob.y - event.y
                    val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    if (dist < 200f && dist > 1f) {
                        val force = (200f - dist) / 200f * 100f
                        blob.vx += dx / dist * force
                        blob.vy += dy / dist * force
                    }
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
