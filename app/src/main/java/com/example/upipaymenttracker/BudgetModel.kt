package com.example.upipaymenttracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetModel(
    @PrimaryKey val category: String,
    val limitAmount: Double
)