package com.example.upipaymenttracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upipaymenttracker.TransactionModel
import java.util.*

@Composable
fun HeatmapCalendar(
    transactions: List<TransactionModel>,
    selectedDate: Calendar,
    modifier: Modifier = Modifier
) {
    val calendar = selectedDate.clone() as Calendar
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0-indexed (Sunday = 0)
    
    val dailyTotals = transactions.groupBy {
        val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
        cal.get(Calendar.DAY_OF_MONTH)
    }.mapValues { entry ->
        entry.value.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    }

    val maxTotal = dailyTotals.values.maxOrNull() ?: 1.0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Spending Heatmap",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Days of week header
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Calendar Grid
        var currentDay = 1
        for (i in 0 until 6) { // Max 6 rows
            Row(modifier = Modifier.fillMaxWidth()) {
                for (j in 0 until 7) {
                    val dayIndex = i * 7 + j
                    if (dayIndex < firstDayOfWeek || currentDay > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val total = dailyTotals[currentDay] ?: 0.0
                        val intensity = (total / maxTotal).toFloat().coerceIn(0f, 1f)
                        
                        val bgColor = when {
                            total == 0.0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            intensity < 0.3f -> Color(0xFFF8BBD0) // Light Pink
                            intensity < 0.7f -> Color(0xFFF06292) // Medium Pink
                            else -> Color(0xFFD81B60) // Deep Pink/Red
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .background(bgColor, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentDay.toString(),
                                fontSize = 10.sp,
                                color = if (intensity > 0.5f) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        currentDay++
                    }
                }
            }
            if (currentDay > daysInMonth) break
        }
    }
}