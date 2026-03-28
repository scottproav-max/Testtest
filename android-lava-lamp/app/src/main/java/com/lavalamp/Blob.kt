package com.lavalamp

import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

data class Blob(
    var x: Float,
    var y: Float,
    var radius: Float,
    var vx: Float,
    var vy: Float,
    var color: Int,
    var wobblePhase: Float = Random.nextFloat() * Math.PI.toFloat() * 2f,
    var wobbleSpeed: Float = 0.5f + Random.nextFloat() * 1.5f
) {
    fun update(width: Float, height: Float, dt: Float) {
        wobblePhase += wobbleSpeed * dt
        vx = sin(wobblePhase.toDouble()).toFloat() * 40f

        x += vx * dt
        y += vy * dt

        // Bounce off side walls with padding
        val margin = radius * 1.5f
        if (x < margin) {
            x = margin
            vx = abs(vx)
        }
        if (x > width - margin) {
            x = width - margin
            vx = -abs(vx)
        }

        // Reverse at top and bottom
        if (y < margin) {
            y = margin
            vy = abs(vy) * (0.8f + Random.nextFloat() * 0.4f)
        }
        if (y > height - margin) {
            y = height - margin
            vy = -abs(vy) * (0.8f + Random.nextFloat() * 0.4f)
        }
    }
}
