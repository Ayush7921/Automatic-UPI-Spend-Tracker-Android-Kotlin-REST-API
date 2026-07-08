package com.example.upipaymenttracker.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upipaymenttracker.TransactionExporter
import com.example.upipaymenttracker.TransactionModel

@Composable
fun SharedWalletScreen(
    transactions: List<TransactionModel>,
    onImportCode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sharedTransactions = transactions.filter { it.isShared }
    val totalOwed = sharedTransactions.sumOf { it.splitAmount ?: 0.0 }
    
    var showImportDialog by remember { mutableStateOf(false) }

    if (showImportDialog) {
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Partner Data") },
            text = {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Paste Sync Code Here") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onImportCode(code)
                    showImportDialog = false
                }) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Partner's Share", fontSize = 14.sp)
                    Text("₹${"%.2f".format(totalOwed)}", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Shared Expenses", style = MaterialTheme.typography.titleMedium)
            Row {
                IconButton(onClick = {
                    val code = TransactionExporter.generateSyncCode(transactions)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Sync Code", code))
                    Toast.makeText(context, "Sync Code Copied! Share it with partner.", Toast.LENGTH_LONG).show()
                }) {
                    Icon(Icons.Default.Share, "Share Sync Code")
                }
                IconButton(onClick = { showImportDialog = true }) {
                    Icon(Icons.Default.ContentPaste, "Import Sync Code")
                }
            }
        }
        
        if (sharedTransactions.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No shared transactions yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sharedTransactions) { trans ->
                    SharedTransactionItem(trans)
                }
            }
        }
    }
}

@Composable
fun SharedTransactionItem(transaction: TransactionModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = transaction.note.ifEmpty { transaction.category }, fontWeight = FontWeight.Bold)
                Text(text = "Total: ₹${transaction.amount}", fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${"%.2f".format(transaction.splitAmount ?: 0.0)}", 
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(text = "Partner's share", fontSize = 10.sp)
            }
        }
    }
}