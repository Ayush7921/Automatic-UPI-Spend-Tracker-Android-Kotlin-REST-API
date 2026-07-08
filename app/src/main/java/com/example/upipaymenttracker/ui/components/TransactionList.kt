package com.example.upipaymenttracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upipaymenttracker.TransactionModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionList(
    transactions: List<TransactionModel>, 
    modifier: Modifier = Modifier,
    headerContent: (LazyListScope.() -> Unit)? = null,
    onTransactionClick: (TransactionModel) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (headerContent != null) {
            headerContent()
        }
        
        if (transactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp), 
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(text = "No transactions found")
                }
            }
        } else {
            items(transactions) { transaction ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clickable { onTransactionClick(transaction) }
                ) {
                    TransactionItem(transaction)
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: TransactionModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = transaction.category,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (transaction.isShared) {
                        Text(
                            text = "Split: ₹${transaction.splitAmount} (Partner)", 
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                Text(
                    text = "₹${transaction.amount}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatTimestamp(transaction.timestamp),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}