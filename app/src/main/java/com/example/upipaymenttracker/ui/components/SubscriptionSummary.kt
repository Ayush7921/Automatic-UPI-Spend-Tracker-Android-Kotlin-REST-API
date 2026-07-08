package com.example.upipaymenttracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upipaymenttracker.Subscription

@Composable
fun SubscriptionSummary(subscriptions: List<Subscription>, modifier: Modifier = Modifier) {
    if (subscriptions.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = "Recurring Subscriptions",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        val totalAnnual = subscriptions.sumOf { it.annualCost }
        Text(
            text = "Annual Cost Estimate: ₹${"%.0f".format(totalAnnual)}",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(subscriptions) { sub ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = sub.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "₹${"%.0f".format(sub.monthlyAmount)}/mo", fontSize = 12.sp)
                        Text(
                            text = "₹${"%.0f".format(sub.annualCost)}/yr", 
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}