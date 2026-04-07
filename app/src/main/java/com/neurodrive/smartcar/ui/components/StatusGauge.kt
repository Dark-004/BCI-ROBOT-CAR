package com.neurodrive.smartcar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatusGauge(
    label: String,
    value: Float,
    maxValue: Float,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val sweepAngle = (value / maxValue) * 240f
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(60.dp)) {
            Canvas(modifier = Modifier.size(60.dp)) {
                drawArc(
                    color = color.copy(alpha = 0.2f),
                    startAngle = 150f,
                    sweepAngle = 240f,
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = color,
                    startAngle = 150f,
                    sweepAngle = sweepAngle.coerceAtMost(240f),
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Text(
                "%.0f".format(value),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
