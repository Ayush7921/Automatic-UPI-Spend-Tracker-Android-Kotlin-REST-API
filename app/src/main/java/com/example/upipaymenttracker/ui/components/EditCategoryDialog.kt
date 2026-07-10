package com.example.upipaymenttracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.upipaymenttracker.TransactionModel

@Composable
fun EditCategoryDialog(
    transaction: TransactionModel,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val categories = listOf("Food & Groceries", "Shopping", "Travel", "Hotels", "Utilities", "Entertainment", "Business", "Personal", "Other Expenses")
    var selectedCategory by remember { mutableStateOf(transaction.category) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Edit Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Amount: ₹${transaction.amount}")
                Text(text = "Note: ${transaction.note.ifEmpty { "N/A" }}")
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Category: $selectedCategory")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedCategory)
                    onDismiss()
                }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}