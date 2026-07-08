package com.example.upipaymenttracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionModel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: String,
    val category: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isShared: Boolean = false,
    val sharedWith: String? = null, // Name of the partner/flatmate
    val splitAmount: Double? = null // Amount to be paid by the other person
)