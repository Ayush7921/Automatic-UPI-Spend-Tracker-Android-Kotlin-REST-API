package com.example.upipaymenttracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.upipaymenttracker.TransactionModel

@Composable
fun SplitDialog(
    transaction: TransactionModel,
    onDismiss: () -> Unit,
    onSave: (TransactionModel) -> Unit
) {
    val total = transaction.amount.toDoubleOrNull() ?: 0.0
    var partnerShare by remember { mutableStateOf((total / 2).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Split Bill") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Total Amount: ₹$total")
                OutlinedTextField(
                    value = partnerShare,
                    onValueChange = { partnerShare = it },
                    label = { Text("Partner's Share (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Your Share: ₹${total - (partnerShare.toDoubleOrNull() ?: 0.0)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val share = partnerShare.toDoubleOrNull() ?: 0.0
                onSave(transaction.copy(isShared = true, splitAmount = share))
                onDismiss()
            }) { Text("Split Now") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}