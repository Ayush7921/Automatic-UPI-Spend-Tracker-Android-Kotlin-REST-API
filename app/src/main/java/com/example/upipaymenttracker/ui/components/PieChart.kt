package com.example.upipaymenttracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun PieChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    val total = data.values.sum()
    if (total <= 0) return

    val colors = listOf(
        Color(0xFF6200EE), // Purple
        Color(0xFF03DAC6), // Teal
        Color(0xFFBB86FC), // Light Purple
        Color(0xFF3700B3), // Dark Purple
        Color(0xFF018786), // Dark Teal
        Color(0xFFFF8A65), // Orange
        Color(0xFF81C784)  // Green
    )

    Box(modifier = modifier.size(150.dp)) {
        Canvas(modifier = Modifier.size(150.dp)) {
            var startAngle = 270f
            data.values.forEachIndexed { index, value ->
                val sweepAngle = (value.toFloat() / total.toFloat()) * 360f
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 30f)
                )
                startAngle += sweepAngle
            }
        }
    }
}
