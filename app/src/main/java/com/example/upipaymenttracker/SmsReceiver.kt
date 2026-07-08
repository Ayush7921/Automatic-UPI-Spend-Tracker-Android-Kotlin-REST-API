package com.example.upipaymenttracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return

        val pendingResult = goAsync()
        val bundle = intent.extras ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pdus = bundle.get("pdus") as? Array<*> ?: return@launch
                val format = bundle.getString("format")
                
                for (pdu in pdus) {
                    val smsMessage = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        SmsMessage.createFromPdu(pdu as ByteArray, format)
                    } else {
                        @Suppress("DEPRECATION")
                        SmsMessage.createFromPdu(pdu as ByteArray)
                    }
                    
                    val messageBody = smsMessage.messageBody ?: continue
                    Log.d("SMS_DEBUG", "Message: $messageBody")

                    if (isValidTransaction(messageBody)) {
                        val amountRegex = "(?i)(?:Rs|INR|\\u20B9)\\.?\\s?([0-9,]+(?:\\.[0-9]{2})?)".toRegex()
                        val matchResult = amountRegex.find(messageBody)

                        if (matchResult != null) {
                            val amountStr = matchResult.groupValues[1].replace(",", "")
                            val category = classifyTransaction(messageBody)
                            val note = extractMerchant(messageBody)

                            val db = AppDatabase.getDatabase(context)
                            val location = try { LocationHelper.getCurrentLocation(context) } catch (e: Exception) { null }
                            
                            val transaction = TransactionModel(
                                amount = amountStr, 
                                category = category, 
                                note = note,
                                latitude = location?.latitude,
                                longitude = location?.longitude
                            )
                            db.transactionDao().insert(transaction)
                            checkBudgetAndNotify(context, db, category)
                        }
                    } else {
                        Log.d("SMS_DEBUG", "Filtered as SPAM/Marketing: $messageBody")
                    }
                }
            } catch (e: Exception) {
                Log.e("SMS_DEBUG", "Error processing SMS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun isValidTransaction(message: String): Boolean {
        val lowerMsg = message.lowercase()
        
        // Spam/Marketing Keywords (Blacklist)
        val spamKeywords = listOf(
            "pre-approved", "loan", "avail", "win", "offer", "congratulations", 
            "reward", "gift", "luck", "lottery", "increase your limit", 
            "instant credit", "apply now", "limited time", "exclusive offer"
        )
        
        // Real Transaction Indicators (Whitelist)
        val transactionKeywords = listOf(
            "debited", "paid", "spent", "purchased", "sent to", "vpa", "upi:", 
            "a/c", "bank", "txn", "transaction", "payment of"
        )

        // Rule 1: If it contains any spam keyword, it's likely marketing.
        if (spamKeywords.any { lowerMsg.contains(it) }) return false
        
        // Rule 2: It must contain at least one real transaction indicator.
        return transactionKeywords.any { lowerMsg.contains(it) }
    }

    private fun extractMerchant(message: String): String {
        val lowerMsg = message.lowercase()
        val patterns = listOf(
            "to\\s+(.*?)\\s+thru",
            "at\\s+(.*?)\\s+on",
            "paid\\s+to\\s+(.*?)\\s+thru",
            "to\\s+(.*?)\\s+for",
            "to\\s+(.*?)\\s+dt"
        )
        for (p in patterns) {
            val match = p.toRegex().find(lowerMsg)
            if (match != null) return match.groupValues[1].trim().replaceFirstChar { it.uppercase() }
        }
        return "Unknown Merchant"
    }

    private suspend fun checkBudgetAndNotify(context: Context, db: AppDatabase, category: String) {
        val budget = db.budgetDao().getAllBudgets().first().find { it.category == category } ?: return
        val transactions = db.transactionDao().getAllTransactions().first()
        val calNow = Calendar.getInstance()
        
        val totalSpent = transactions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.get(Calendar.MONTH) == calNow.get(Calendar.MONTH) && 
            cal.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) && 
            it.category == category
        }.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }

        if (totalSpent > budget.limitAmount) {
            NotificationHelper(context).showBudgetAlert(category, totalSpent)
        }
    }

    private fun classifyTransaction(message: String): String {
        val lowerMsg = message.lowercase()
        return when {
            lowerMsg.contains("zomato") || lowerMsg.contains("swiggy") || 
            lowerMsg.contains("blinkit") || lowerMsg.contains("instamart") -> "Food & Groceries"
            lowerMsg.contains("flipkart") || lowerMsg.contains("amazon") || 
            lowerMsg.contains("myntra") || lowerMsg.contains("ajio") -> "Shopping"
            lowerMsg.contains("uber") || lowerMsg.contains("ola") || 
            lowerMsg.contains("rapido") || lowerMsg.contains("irctc") -> "Travel"
            lowerMsg.contains("hotel") || lowerMsg.contains("oyo") || 
            lowerMsg.contains("stay") -> "Hotels"
            lowerMsg.contains("bill") || lowerMsg.contains("recharge") || 
            lowerMsg.contains("electricity") || lowerMsg.contains("netflix") ||
            lowerMsg.contains("spotify") || lowerMsg.contains("youtube") -> "Utilities"
            lowerMsg.contains("client") || lowerMsg.contains("project") -> "Business"
            lowerMsg.contains("friend") || lowerMsg.contains("gift") -> "Personal"
            else -> "Other Expenses"
        }
    }
}