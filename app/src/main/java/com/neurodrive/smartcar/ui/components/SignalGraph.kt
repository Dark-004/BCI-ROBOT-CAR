package com.neurodrive.smartcar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.neurodrive.smartcar.data.model.BioSignal

@Composable
fun SignalGraph(
    signals: List<BioSignal>,          // ← Accept List directly
    modifier: Modifier = Modifier
) {
    val alphaColor = Color(0xFF4CAF50)
    val betaColor = Color(0xFF2196F3)
    val thetaColor = Color(0xFFFF9800)
    val gammaColor = Color(0xFFF44336)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        if (signals.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val maxPoints = signals.size
        val stepX = if (maxPoints > 1) width / (maxPoints - 1) else width

        // Find max value for scaling
        val maxValue = signals.maxOf { signal ->
            maxOf(signal.eegAlpha, signal.eegBeta, signal.eegTheta, signal.eegGamma)
        }.coerceAtLeast(1f)

        // Draw each wave
        fun drawWave(color: Color, valueSelector: (BioSignal) -> Float) {
            for (i in 0 until maxPoints - 1) {
                val x1 = i * stepX
                val y1 = height - (valueSelector(signals[i]) / maxValue * height)
                val x2 = (i + 1) * stepX
                val y2 = height - (valueSelector(signals[i + 1]) / maxValue * height)

                drawLine(
                    color = color,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = 2f
                )
            }
        }

        drawWave(alphaColor) { it.eegAlpha }
        drawWave(betaColor) { it.eegBeta }
        drawWave(thetaColor) { it.eegTheta }
        drawWave(gammaColor) { it.eegGamma }
    }
}