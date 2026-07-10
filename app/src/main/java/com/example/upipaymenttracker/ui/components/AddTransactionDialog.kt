package com.example.upipaymenttracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.upipaymenttracker.LocationHelper
import com.example.upipaymenttracker.TransactionModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (TransactionModel) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Other Expenses") }
    var note by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val categories = listOf("Food & Groceries", "Shopping", "Travel", "Hotels", "Utilities", "Entertainment", "Business", "Personal", "Other Expenses")
    var expanded by remember { mutableStateOf(false) }

    if (showScanner) {
        ReceiptScannerDialog(
            onDismiss = { showScanner = false },
            onResult = { scannedAmount, scannedMerchant ->
                amount = scannedAmount
                note = scannedMerchant
                showScanner = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add Manual Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showScanner = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Receipt")
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Category: $category")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Merchant / Note") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (amount.isNotEmpty()) {
                        scope.launch {
                            val location = withContext(Dispatchers.IO) {
                                LocationHelper.getCurrentLocation(context)
                            }
                            onSave(TransactionModel(
                                amount = amount, 
                                category = category, 
                                note = note,
                                latitude = location?.latitude,
                                longitude = location?.longitude
                            ))
                            onDismiss()
                        }
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}