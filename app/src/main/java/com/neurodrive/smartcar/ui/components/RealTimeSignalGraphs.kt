package com.neurodrive.smartcar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Real-time EEG waveform graph (filtered signal)
 */
@Composable
fun EEGWaveformGraph(
    data: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF2196F3),
    title: String = "EEG Waveform"
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            if (data.isEmpty()) return@Canvas

            val width = size.width
            val height = size.height
            val pointsCount = data.size

            // Auto-scale to data range
            val maxVal = data.maxOrNull() ?: 1f
            val minVal = data.minOrNull() ?: -1f
            val range = maxVal - minVal
            val scale = if (range > 0.001f) height / range else height / 2f

            val path = Path()

            data.forEachIndexed { index, value ->
                val x = (index.toFloat() / pointsCount) * width
                val y = height / 2 - ((value - (maxVal + minVal) / 2) * scale)

                if (index == 0) {
                    path.moveTo(x, y.coerceIn(0f, height))
                } else {
                    path.lineTo(x, y.coerceIn(0f, height))
                }
            }

            // Draw center line
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(0f, height / 2),
                end = Offset(width, height / 2),
                strokeWidth = 1f
            )

            // Draw waveform
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 2f)
            )
        }
    }
}

/**
 * EOG waveform graph (eye movements)
 */
@Composable
fun EOGWaveformGraph(
    data: List<Float>,
    modifier: Modifier = Modifier
) {
    EEGWaveformGraph(
        data = data,
        modifier = modifier,
        color = Color(0xFFFF9800),
        title = "EMG (Eye Movement)"
    )
}

/**
 * Band power graph (Alpha, Beta, Theta, Gamma)
 */
@Composable
fun BandPowerGraph(
    alphaData: List<Float>,
    betaData: List<Float>,
    thetaData: List<Float>,
    gammaData: List<Float>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "EEG Band Powers",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val width = size.width
            val height = size.height

            // Find max across all bands for consistent scaling
            val allData = alphaData + betaData + thetaData + gammaData
            if (allData.isEmpty()) return@Canvas

            val maxValue = allData.maxOrNull()?.coerceAtLeast(1f) ?: 1f

            // Helper to draw band line
            fun drawBandLine(data: List<Float>, color: Color) {
                if (data.isEmpty()) return

                val path = Path()
                val pointsCount = data.size

                data.forEachIndexed { index, value ->
                    val x = (index.toFloat() / pointsCount) * width
                    val y = height - (value / maxValue * height).coerceIn(0f, height)

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 2.5f)
                )
            }

            // Draw each band
            drawBandLine(gammaData, Color(0xFFF44336))  // Red
            drawBandLine(betaData, Color(0xFF2196F3))   // Blue
            drawBandLine(alphaData, Color(0xFF4CAF50))  // Green
            drawBandLine(thetaData, Color(0xFFFF9800))  // Orange
        }

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem("Alpha", Color(0xFF4CAF50))
            LegendItem("Beta", Color(0xFF2196F3))
            LegendItem("Theta", Color(0xFFFF9800))
            LegendItem("Gamma", Color(0xFFF44336))
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row {
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(color = color)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Combined graph layout with all signals
 */
@Composable
fun AllSignalGraphs(
    eegData: List<Float>,
    eogData: List<Float>,
    alphaData: List<Float>,
    betaData: List<Float>,
    thetaData: List<Float>,
    gammaData: List<Float>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Real-time EEG waveform
        EEGWaveformGraph(data = eegData)

        // Real-time EOG waveform
        EOGWaveformGraph(data = eogData)

        // Band powers
        BandPowerGraph(
            alphaData = alphaData,
            betaData = betaData,
            thetaData = thetaData,
            gammaData = gammaData
        )
    }
}